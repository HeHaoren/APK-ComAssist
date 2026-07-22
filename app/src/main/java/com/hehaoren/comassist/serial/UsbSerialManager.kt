package com.hehaoren.comassist.serial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProlificSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException

/**
 * 支持的 USB 串口驱动类列表
 *
 * 包含常见的 USB 转串口芯片驱动：FTDI、Prolific、CDC ACM、CH340、CP2102
 */
private val DRIVER_CLASSES = listOf(
    FtdiSerialDriver::class.java,
    ProlificSerialDriver::class.java,
    CdcAcmSerialDriver::class.java,
    Ch34xSerialDriver::class.java,
    Cp21xxSerialDriver::class.java
)

/**
 * USB 串口管理器
 *
 * 负责 USB 串口设备的扫描、连接、数据收发和自动重连
 *
 * 支持的 USB 串口芯片：
 * - FTDI (FT232, FT2232 等)
 * - Prolific (PL2303)
 * - CDC ACM (Arduino 等)
 * - CH340/CH341
 * - CP2102/CP2104
 *
 * @param context Android 上下文
 */
class UsbSerialManager(private val context: Context) : SerialConnection {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var usbDevice: UsbDevice? = null
    private var readThread: Thread? = null
    private var dataListener: ((ByteArray) -> Unit)? = null
    private var _isConnected = false

    // 自动重连相关
    private var lastDeviceName: String? = null
    private var lastConfig: SerialConfig? = null
    private var onUsbEvent: ((String) -> Unit)? = null
    private var eventReceiver: BroadcastReceiver? = null

    override val isConnected: Boolean get() = _isConnected

    companion object {
        /** USB 权限请求的 Intent Action */
        private const val ACTION_USB_PERMISSION = "com.hehaoren.comassist.USB_PERMISSION"
    }

    /**
     * 注册 USB 设备插拔事件监听
     *
     * @param callback 事件回调，参数为 "attached" 或 "detached"
     */
    fun registerUsbEvents(callback: (String) -> Unit) {
        onUsbEvent = callback
        eventReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        val d = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        if (d != null && d.deviceName == lastDeviceName) {
                            silentClose()
                            callback("detached")
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        val d = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        if (d != null && d.deviceName == lastDeviceName) {
                            callback("attached")
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        context.registerReceiver(eventReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    /**
     * 注销 USB 设备插拔事件监听
     */
    fun unregisterUsbEvents() {
        eventReceiver?.let { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
        eventReceiver = null
    }

    /**
     * 静默关闭连接
     *
     * 关闭串口但保留设备信息用于自动重连
     */
    private fun silentClose() {
        _isConnected = false
        readThread?.interrupt()
        readThread = null
        try { serialPort?.close() } catch (_: IOException) {}
        serialPort = null
    }

    /**
     * 重新连接设备
     *
     * 使用上次连接的设备名和配置尝试重连
     *
     * @return 重连是否成功
     */
    fun reconnect(): Boolean {
        val name = lastDeviceName ?: return false
        val cfg = lastConfig ?: return false
        if (!prepareDevice(name)) return false
        return connect(cfg)
    }

    /**
     * 扫描可用的 USB 串口设备
     *
     * 扫描策略：
     * 1. 使用默认探针扫描已知设备
     * 2. 扫描所有 USB 设备，尝试识别未被默认探针找到的串口芯片
     * 3. 如果还没找到，列出可能是串口的 USB 设备方便调试
     *
     * @return 发现的设备列表
     */
    fun scanDevices(): List<DeviceItem> {
        val devices = mutableListOf<DeviceItem>()
        val foundDeviceNames = mutableSetOf<String>()

        // 1. 默认探针扫描已知设备
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        for (driver in availableDrivers) {
            val device = driver.device
            if (!mightBeSerialDevice(device)) continue
            foundDeviceNames.add(device.deviceName)
            val name = device.productName ?: device.deviceName
            val vidPid = "VID:%04X PID:%04X".format(device.vendorId, device.productId)
            devices.add(DeviceItem(
                name = "$name ($vidPid) [${device.deviceName}]",
                address = device.deviceName,
                connectionType = ConnectionTypes.USB
            ))
        }

        // 2. 扫描所有 USB 设备，尝试识别未被默认探针找到的串口芯片
        for ((_, usbDevice) in usbManager.deviceList) {
            if (foundDeviceNames.contains(usbDevice.deviceName)) continue
            // 跳过非串口设备（网卡、HID等）
            if (!mightBeSerialDevice(usbDevice)) continue
            val driver = probeDevice(usbDevice)
            if (driver != null) {
                foundDeviceNames.add(usbDevice.deviceName)
                val name = usbDevice.productName ?: usbDevice.deviceName
                val vidPid = "VID:%04X PID:%04X".format(usbDevice.vendorId, usbDevice.productId)
                devices.add(DeviceItem(
                    name = "$name ($vidPid) [${usbDevice.deviceName}]",
                    address = usbDevice.deviceName,
                    connectionType = ConnectionTypes.USB
                ))
            }
        }

        // 3. 如果还没找到，列出可能是串口的 USB 设备（VID/PID）方便调试
        if (devices.isEmpty()) {
            for ((_, usbDevice) in usbManager.deviceList) {
                if (!mightBeSerialDevice(usbDevice)) continue
                val name = usbDevice.productName ?: usbDevice.deviceName
                val vidPid = "VID:%04X PID:%04X".format(usbDevice.vendorId, usbDevice.productId)
                devices.add(DeviceItem(
                    name = "$name ($vidPid) [${usbDevice.deviceName}]",
                    address = usbDevice.deviceName,
                    connectionType = ConnectionTypes.USB
                ))
            }
        }

        return devices
    }

    /**
     * 已知非串口设备 VID 黑名单
     *
     * 包含常见的网卡、Hub、HID 设备厂商 ID
     */
    private val BLACKLIST_VIDS = setOf(
        0x0BDA,  // Realtek（网卡）
        0x0424,  // Microchip/SMSC（Hub/网卡）
        0x0A5C,  // Broadcom（蓝牙/网卡）
        0x0846,  // NetGear（网卡）
        0x0B95,  // ASIX（USB网卡芯片）
        0x07D1,  // D-Link（网卡）
        0x050D,  // Belkin（网卡）
        0x17EF,  // Lenovo（部分设备）
        0x046D,  // Logitech（HID设备）
    )

    /**
     * 判断 USB 设备是否可能是串口设备
     *
     * 通过 VID 黑名单和接口类进行判断
     *
     * @param device USB 设备
     * @return 可能是串口设备返回 true
     */
    private fun mightBeSerialDevice(device: UsbDevice): Boolean {
        // 黑名单直接排除
        if (device.vendorId in BLACKLIST_VIDS) return false

        // 检查是否有串口相关的接口类
        var hasSerialInterface = false
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            val cls = iface.interfaceClass
            // 0x0A = CDC Data（串口数据接口），0xFF = Vendor Specific（CH340等国产芯片）
            if (cls == 0x0A || cls == 0xFF) hasSerialInterface = true
            // 0x0D = RNDIS（USB网络共享），0x0E = CDC ECM（USB以太网）
            if (cls == 0x0D || cls == 0x0E) return false
        }
        return hasSerialInterface
    }

    /**
     * 尝试为设备找到合适的驱动
     *
     * @param device USB 设备
     * @return 匹配的驱动，未找到返回 null
     */
    private fun probeDevice(device: UsbDevice): UsbSerialDriver? {
        for (cls in DRIVER_CLASSES) {
            try {
                val ctor = cls.getConstructor(UsbDevice::class.java)
                val driver = ctor.newInstance(device)
                if (driver.ports.isNotEmpty()) {
                    android.util.Log.d("UsbSerial", "Found driver ${cls.simpleName} for ${device.deviceName} VID:${"%04X".format(device.vendorId)} PID:${"%04X".format(device.productId)}")
                    return driver
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * 检查是否已获得设备权限
     *
     * @param deviceName 设备名称
     * @return 已有权限返回 true
     */
    fun hasPermission(deviceName: String): Boolean {
        val device = findDevice(deviceName) ?: return false
        return usbManager.hasPermission(device)
    }

    /**
     * 请求 USB 设备权限
     *
     * @param deviceName 设备名称
     * @param onResult 权限请求结果回调
     */
    fun requestPermission(deviceName: String, onResult: (Boolean) -> Unit) {
        val device = findDevice(deviceName) ?: run { onResult(false); return }
        if (usbManager.hasPermission(device)) { onResult(true); return }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    try { context.unregisterReceiver(this) } catch (_: Exception) {}
                    onResult(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                }
            }
        }
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE)
        usbManager.requestPermission(device, pi)
    }

    /**
     * 准备设备连接
     *
     * 查找设备驱动并获取串口实例
     *
     * @param deviceName 设备名称
     * @return 准备成功返回 true
     */
    fun prepareDevice(deviceName: String): Boolean {
        val device = findDevice(deviceName) ?: return false
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val driver = drivers.find { it.device == device } ?: probeDevice(device) ?: return false
        serialPort = driver.ports.firstOrNull() ?: return false
        usbDevice = device
        return true
    }

    /**
     * 建立串口连接
     *
     * @param config 串口配置参数
     * @return 连接是否成功
     */
    override fun connect(config: SerialConfig): Boolean {
        val port = serialPort ?: return false
        val device = usbDevice ?: return false
        try {
            val connection: UsbDeviceConnection = usbManager.openDevice(device) ?: return false
            port.open(connection)
            port.setParameters(
                config.baudRate,
                config.dataBits,
                config.stopBits.toInt(),
                when (config.parity) {
                    ParityValues.ODD -> UsbSerialPort.PARITY_ODD
                    ParityValues.EVEN -> UsbSerialPort.PARITY_EVEN
                    else -> UsbSerialPort.PARITY_NONE
                }
            )
            _isConnected = true
            lastDeviceName = device.deviceName
            lastConfig = config
            startReading()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 断开串口连接
     */
    override fun disconnect() {
        _isConnected = false
        readThread?.interrupt()
        readThread = null
        try { serialPort?.close() } catch (_: IOException) {}
        serialPort = null
        usbDevice = null
        lastDeviceName = null
        lastConfig = null
    }

    /**
     * 发送数据
     *
     * @param data 要发送的字节数组
     * @return 发送是否成功
     */
    override fun send(data: ByteArray): Boolean {
        val port = serialPort ?: return false
        if (!_isConnected) return false
        return try { port.write(data, 1000); true } catch (e: IOException) { e.printStackTrace(); false }
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
                    val port = serialPort ?: break
                    val len = port.read(buf, 1000)
                    if (len > 0) dataListener?.invoke(buf.copyOf(len))
                } catch (_: IOException) {
                    if (_isConnected) _isConnected = false
                    break
                }
            }
        }.apply { isDaemon = true; start() }
    }

    /**
     * 根据设备名查找 USB 设备
     *
     * @param name 设备名称
     * @return 找到的设备，未找到返回 null
     */
    private fun findDevice(name: String): UsbDevice? =
        usbManager.deviceList.values.find { it.deviceName == name }
}
