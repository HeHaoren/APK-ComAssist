package com.example.usart_connect

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.usart_connect.serial.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SerialViewModel(application: Application) : AndroidViewModel(application) {

    private val usbManager = UsbSerialManager(application)
    private val btManager = BtSerialManager(application)
    private val networkManager = NetworkManager(application)
    private var activeConnection: SerialConnection? = null
    private val prefs: SharedPreferences = application.getSharedPreferences("quick_commands", Context.MODE_PRIVATE)

    // 屏幕刷新率自适应
    private var refreshRateHz: Float = 60f
    private val frameIntervalMs: Long get() = (1000f / refreshRateHz).toLong().coerceIn(8, 33)

    // 接收数据缓冲区（按帧率批量刷新UI）
    private val pendingData = StringBuilder()
    private var flushScheduled = false
    private var droppedPackets = 0  // 丢弃的数据包计数
    private val maxBufferSize = 102400  // 缓冲区最大 100KB

    // 状态
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

    // 快捷指令
    private val _quickCommands = MutableStateFlow<List<QuickCommand>>(emptyList())
    val quickCommands: StateFlow<List<QuickCommand>> = _quickCommands.asStateFlow()

    // 网卡设备
    private val _networkDevices = MutableStateFlow<List<NetworkDeviceInfo>>(emptyList())
    val networkDevices: StateFlow<List<NetworkDeviceInfo>> = _networkDevices.asStateFlow()

    private val _showNetworkPanel = MutableStateFlow(false)
    val showNetworkPanel: StateFlow<Boolean> = _showNetworkPanel.asStateFlow()

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
        loadQuickCommands()
    }

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
            // 缓冲区超限时丢弃最早的数据
            if (pendingData.length > maxBufferSize) {
                val dropLen = pendingData.length - maxBufferSize / 2
                pendingData.delete(0, dropLen)
                droppedPackets++
            }
            pendingData.append("[$ts] $content\n\n")
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
        val dropped: Int
        synchronized(pendingData) {
            batch = pendingData.toString()
            pendingData.clear()
            flushScheduled = false
            dropped = droppedPackets
            droppedPackets = 0
        }
        if (batch.isNotEmpty()) {
            val sb = StringBuilder(_receivedText.value.length + batch.length + 50)
            sb.append(_receivedText.value)
            sb.append(batch)
            // 丢包提示
            if (dropped > 0) {
                sb.append("[!] 数据过快，已丢弃 $dropped 批缓冲数据\n\n")
            }
            // 总文本超限截断
            val text = sb.toString()
            _receivedText.value = if (text.length > 50000) text.takeLast(40000) else text
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

    // ==================== 快捷指令管理 ====================

    private fun loadQuickCommands() {
        val json = prefs.getString("commands", "[]") ?: "[]"
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<QuickCommand>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(QuickCommand(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    name = obj.getString("name"),
                    command = obj.getString("command"),
                    isHex = obj.optBoolean("isHex", false)
                ))
            }
            _quickCommands.value = list
        } catch (_: Exception) {
            _quickCommands.value = emptyList()
        }
    }

    private fun saveQuickCommands() {
        val arr = JSONArray()
        for (cmd in _quickCommands.value) {
            val obj = JSONObject()
            obj.put("id", cmd.id)
            obj.put("name", cmd.name)
            obj.put("command", cmd.command)
            obj.put("isHex", cmd.isHex)
            arr.put(obj)
        }
        prefs.edit().putString("commands", arr.toString()).apply()
    }

    fun addQuickCommand(name: String, command: String, isHex: Boolean) {
        val cmd = QuickCommand(name = name, command = command, isHex = isHex)
        _quickCommands.value = _quickCommands.value + cmd
        saveQuickCommands()
    }

    fun removeQuickCommand(id: Long) {
        _quickCommands.value = _quickCommands.value.filter { it.id != id }
        saveQuickCommands()
    }

    fun sendQuickCommand(cmd: QuickCommand) {
        val conn = activeConnection
        if (conn == null) { _statusMessage.value = "未连接"; return }
        if (!conn.isConnected) { _statusMessage.value = "连接已断开"; return }
        viewModelScope.launch(Dispatchers.IO) {
            val data = if (cmd.isHex) {
                try {
                    cmd.command.replace(" ", "").replace("\n", "")
                        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                } catch (_: Exception) { _statusMessage.value = "无效的 HEX 格式"; return@launch }
            } else {
                cmd.command.toByteArray()
            }
            val ok = conn.send(data)
            _statusMessage.value = if (ok) "已发送: ${cmd.name}" else "发送失败"
        }
    }

    fun importCommands(json: String): Int {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<QuickCommand>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(QuickCommand(
                    name = obj.getString("name"),
                    command = obj.getString("command"),
                    isHex = obj.optBoolean("isHex", false)
                ))
            }
            _quickCommands.value = _quickCommands.value + list
            saveQuickCommands()
            list.size
        } catch (_: Exception) { -1 }
    }

    fun exportCommands(): String {
        val arr = JSONArray()
        for (cmd in _quickCommands.value) {
            val obj = JSONObject()
            obj.put("name", cmd.name)
            obj.put("command", cmd.command)
            obj.put("isHex", cmd.isHex)
            arr.put(obj)
        }
        return arr.toString(2)
    }

    // ==================== 网卡设备管理 ====================

    fun toggleNetworkPanel() {
        _showNetworkPanel.value = !_showNetworkPanel.value
        if (_showNetworkPanel.value) scanNetworkDevices()
    }

    fun scanNetworkDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            val devices = networkManager.scanNetworkDevices()
            _networkDevices.value = devices
            _statusMessage.value = if (devices.isEmpty()) "未发现网卡设备" else "发现 ${devices.size} 个网卡"
        }
    }

    override fun onCleared() {
        super.onCleared()
        usbManager.unregisterUsbEvents()
        activeConnection?.disconnect()
    }
}
