package com.example.usart_connect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.usart_connect.serial.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SerialViewModel(application: Application) : AndroidViewModel(application) {

    private val usbManager = UsbSerialManager(application)
    private val btManager = BtSerialManager(application)
    private var activeConnection: SerialConnection? = null

    // 屏幕刷新率自适应
    private var refreshRateHz: Float = 60f
    private val frameIntervalMs: Long get() = (1000f / refreshRateHz).toLong().coerceIn(8, 33)

    // 接收数据缓冲区（按帧率批量刷新UI）
    private val pendingData = StringBuilder()
    private var flushScheduled = false

    init {
        usbManager.setDataListener { data ->
            bufferReceivedData(data)
        }
        usbManager.registerUsbEvents { event ->
            when (event) {
                "detached" -> {
                    _isConnected.value = false
                    _statusMessage.value = "USB 已断开，等待重新插入..."
                }
                "attached" -> {
                    _statusMessage.value = "USB 已重新插入，正在重连..."
                    viewModelScope.launch(Dispatchers.IO) {
                        Thread.sleep(800)
                        val ok = usbManager.reconnect()
                        if (ok) {
                            activeConnection = usbManager
                            _isConnected.value = true
                            _statusMessage.value = "USB 已自动重连"
                        } else {
                            _statusMessage.value = "自动重连失败，请手动连接"
                        }
                    }
                }
            }
        }
    }

    private val _connectionType = MutableStateFlow(ConnectionTypes.USB)
    val connectionType: StateFlow<String> = _connectionType.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceItem>>(emptyList())
    val devices: StateFlow<List<DeviceItem>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<DeviceItem?>(null)
    val selectedDevice: StateFlow<DeviceItem?> = _selectedDevice.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _config = MutableStateFlow(SerialConfig())
    val config: StateFlow<SerialConfig> = _config.asStateFlow()

    private val _receivedText = MutableStateFlow("")
    val receivedText: StateFlow<String> = _receivedText.asStateFlow()

    private val _sendText = MutableStateFlow("")
    val sendText: StateFlow<String> = _sendText.asStateFlow()

    private val _receiveMode = MutableStateFlow(DisplayModes.ASCII)
    val receiveMode: StateFlow<Int> = _receiveMode.asStateFlow()

    private val _sendMode = MutableStateFlow(DisplayModes.ASCII)
    val sendMode: StateFlow<Int> = _sendMode.asStateFlow()

    private val _lineEnding = MutableStateFlow(LineEndingValues.CRLF)
    val lineEnding: StateFlow<Int> = _lineEnding.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    /** 设置屏幕刷新率（由 Activity 调用） */
    fun setRefreshRate(hz: Float) {
        refreshRateHz = hz.coerceIn(30f, 240f)
    }

    /** 按帧率批量刷新接收数据 */
    private fun bufferReceivedData(data: ByteArray) {
        val ts = timeFormat.format(Date())
        val content = when (_receiveMode.value) {
            DisplayModes.HEX -> data.joinToString(" ") { "%02X".format(it) }
            else -> String(data, Charsets.UTF_8)
        }
        synchronized(pendingData) {
            pendingData.append("[$ts] $content\n")
        }
        if (!flushScheduled) {
            flushScheduled = true
            viewModelScope.launch(Dispatchers.Main) {
                delay(frameIntervalMs)
                flushPendingData()
            }
        }
    }

    private fun flushPendingData() {
        val batch: String
        synchronized(pendingData) {
            batch = pendingData.toString()
            pendingData.clear()
            flushScheduled = false
        }
        if (batch.isNotEmpty()) {
            _receivedText.value += batch
            if (_receivedText.value.length > 50000) {
                _receivedText.value = _receivedText.value.takeLast(40000)
            }
        }
    }

    fun setConnectionType(type: String) {
        if (_isConnected.value) return
        _connectionType.value = type
        _selectedDevice.value = null
        _devices.value = emptyList()
    }

    fun scanDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = when (_connectionType.value) {
                ConnectionTypes.USB -> usbManager.scanDevices()
                else -> {
                    if (!btManager.isBluetoothAvailable()) { _statusMessage.value = "蓝牙不可用"; return@launch }
                    if (!btManager.isBluetoothEnabled()) { _statusMessage.value = "请先开启蓝牙"; return@launch }
                    btManager.scanDevices()
                }
            }
            _devices.value = list
            _statusMessage.value = if (list.isEmpty()) "未发现设备" else "发现 ${list.size} 个设备"
        }
    }

    fun selectDevice(device: DeviceItem) { _selectedDevice.value = device }

    fun connect() {
        val device = _selectedDevice.value ?: run { _statusMessage.value = "请先选择设备"; return }
        viewModelScope.launch(Dispatchers.IO) {
            when (device.connectionType) {
                ConnectionTypes.USB -> {
                    if (!usbManager.hasPermission(device.address)) {
                        _statusMessage.value = "正在请求 USB 权限..."
                        try {
                            usbManager.requestPermission(device.address) { ok ->
                                viewModelScope.launch(Dispatchers.Main) {
                                    if (ok) connect() else _statusMessage.value = "USB 权限被拒绝"
                                }
                            }
                        } catch (e: Exception) {
                            _statusMessage.value = "权限请求失败: ${e.message}"
                        }
                        return@launch
                    }
                    if (!usbManager.prepareDevice(device.address)) {
                        _statusMessage.value = "无法打开 USB 设备"; return@launch
                    }
                    activeConnection = usbManager
                }
                else -> {
                    btManager.prepareDevice(device.address)
                    activeConnection = btManager
                }
            }
            val conn = activeConnection ?: return@launch
            conn.setDataListener { data -> bufferReceivedData(data) }
            val ok = conn.connect(_config.value)
            _isConnected.value = ok
            _statusMessage.value = if (ok) "已连接" else "连接失败"
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            activeConnection?.disconnect(); activeConnection = null
            _isConnected.value = false; _statusMessage.value = "已断开"
        }
    }

    fun send() {
        val text = _sendText.value
        if (text.isEmpty()) { _statusMessage.value = "请输入发送内容"; return }
        val conn = activeConnection
        if (conn == null) { _statusMessage.value = "未连接"; return }
        if (!conn.isConnected) { _statusMessage.value = "连接已断开"; return }
        viewModelScope.launch(Dispatchers.IO) {
            val data = when (_sendMode.value) {
                DisplayModes.ASCII -> {
                    val bytes = text.toByteArray()
                    val ending = LineEndingValues.bytesOf(_lineEnding.value)
                    if (ending != null) bytes + ending else bytes
                }
                else -> {
                    try {
                        val hex = text.replace(" ", "").replace("\n", "")
                        val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                        val ending = LineEndingValues.bytesOf(_lineEnding.value)
                        if (ending != null) bytes + ending else bytes
                    } catch (_: Exception) { _statusMessage.value = "无效的 HEX 格式"; return@launch }
                }
            }
            val ok = conn.send(data)
            _statusMessage.value = if (ok) "已发送 ${data.size} 字节" else "发送失败"
        }
    }

    fun setSendText(text: String) { _sendText.value = text }
    fun clearReceived() { _receivedText.value = "" }
    fun setReceiveMode(mode: Int) { _receiveMode.value = mode }
    fun setSendMode(mode: Int) { _sendMode.value = mode }
    fun setLineEnding(ending: Int) { _lineEnding.value = ending }
    fun updateConfig(config: SerialConfig) { _config.value = config }
    fun setStatusMessage(msg: String) { _statusMessage.value = msg }

    override fun onCleared() {
        super.onCleared()
        usbManager.unregisterUsbEvents()
        activeConnection?.disconnect()
    }
}
