package com.hehaoren.comassist.serial

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * 蓝牙串口管理器
 *
 * 负责蓝牙 SPP (Serial Port Profile) 串口连接的管理
 *
 * 功能：
 * - 扫描已配对的蓝牙设备
 * - 建立蓝牙串口连接
 * - 数据收发
 * - 连接状态管理
 *
 * @param context Android 上下文
 */
class BtSerialManager(private val context: Context) : SerialConnection {

    /** 蓝牙适配器 */
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    /** 蓝牙 Socket */
    private var btSocket: BluetoothSocket? = null

    /** 输入流 */
    private var inputStream: InputStream? = null

    /** 输出流 */
    private var outputStream: OutputStream? = null

    /** 数据读取线程 */
    private var readThread: Thread? = null

    /** 数据接收监听器 */
    private var dataListener: ((ByteArray) -> Unit)? = null

    /** 连接状态 */
    private var _isConnected = false

    /** 待连接的设备地址 */
    private var pendingAddress: String? = null

    override val isConnected: Boolean get() = _isConnected

    companion object {
        /** SPP (Serial Port Profile) UUID */
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    /**
     * 检查蓝牙是否可用
     *
     * @return 蓝牙适配器存在返回 true
     */
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    /**
     * 检查蓝牙是否已开启
     *
     * @return 蓝牙已开启返回 true
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * 扫描已配对的蓝牙设备
     *
     * @return 已配对设备列表
     */
    @SuppressLint("MissingPermission")
    fun scanDevices(): List<DeviceItem> {
        val devices = mutableListOf<DeviceItem>()
        val adapter = bluetoothAdapter ?: return devices
        for (device in adapter.bondedDevices) {
            devices.add(DeviceItem(
                name = "${device.name ?: "未知设备"} [${device.address}]",
                address = device.address,
                connectionType = ConnectionTypes.BLUETOOTH
            ))
        }
        return devices
    }

    /**
     * 准备设备连接
     *
     * 保存待连接设备的地址
     *
     * @param address 蓝牙设备 MAC 地址
     * @return 总是返回 true
     */
    fun prepareDevice(address: String): Boolean {
        pendingAddress = address
        return true
    }

    /**
     * 建立蓝牙串口连接
     *
     * @param config 串口配置（蓝牙连接中未使用，保留接口一致性）
     * @return 连接是否成功
     */
    @SuppressLint("MissingPermission")
    override fun connect(config: SerialConfig): Boolean {
        val adapter = bluetoothAdapter ?: return false
        val address = pendingAddress ?: return false
        val device = adapter.getRemoteDevice(address) ?: return false
        try {
            btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            // 取消发现以加速连接
            adapter.cancelDiscovery()
            btSocket?.connect()
            inputStream = btSocket?.inputStream
            outputStream = btSocket?.outputStream
            _isConnected = true
            startReading()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            try { btSocket?.close() } catch (_: IOException) {}
            btSocket = null
            return false
        }
    }

    /**
     * 断开蓝牙连接
     */
    override fun disconnect() {
        _isConnected = false
        readThread?.interrupt()
        readThread = null
        try { inputStream?.close(); outputStream?.close(); btSocket?.close() } catch (_: IOException) {}
        inputStream = null; outputStream = null; btSocket = null
    }

    /**
     * 发送数据
     *
     * @param data 要发送的字节数组
     * @return 发送是否成功
     */
    override fun send(data: ByteArray): Boolean {
        if (!_isConnected) return false
        return try { outputStream?.write(data); outputStream?.flush(); true }
        catch (e: IOException) { e.printStackTrace(); false }
    }

    /**
     * 设置数据接收监听器
     *
     * @param listener 接收到数据时的回调函数
     */
    override fun setDataListener(listener: (ByteArray) -> Unit) {
        dataListener = listener
    }

    /**
     * 启动数据读取线程
     */
    private fun startReading() {
        readThread = Thread {
            val buf = ByteArray(1024)
            while (_isConnected && !Thread.currentThread().isInterrupted) {
                try {
                    val stream = inputStream ?: break
                    val len = stream.read(buf)
                    if (len > 0) dataListener?.invoke(buf.copyOf(len))
                } catch (_: IOException) {
                    if (_isConnected) _isConnected = false
                    break
                }
            }
        }.apply { isDaemon = true; start() }
    }
}
