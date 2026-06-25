package com.example.usart_connect.serial

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

private val DRIVER_CLASSES = listOf(
    FtdiSerialDriver::class.java,
    ProlificSerialDriver::class.java,
    CdcAcmSerialDriver::class.java,
    Ch34xSerialDriver::class.java,
    Cp21xxSerialDriver::class.java
)


class UsbSerialManager(private val context: Context) : SerialConnection {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var usbDevice: UsbDevice? = null
    private var readThread: Thread? = null
    private var dataListener: ((ByteArray) -> Unit)? = null
    private var _isConnected = false

    // 自动重连
    private var lastDeviceName: String? = null
    private var lastConfig: SerialConfig? = null
    private var onUsbEvent: ((String) -> Unit)? = null  // "attached" / "detached"
    private var eventReceiver: BroadcastReceiver? = null

    override val isConnected: Boolean get() = _isConnected

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.usart_connect.USB_PERMISSION"
    }

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

    fun unregisterUsbEvents() {
        eventReceiver?.let { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
        eventReceiver = null
    }

    /** 静默关闭，保留 lastDeviceName/lastConfig 用于重连 */
    private fun silentClose() {
        _isConnected = false
        readThread?.interrupt()
        readThread = null
        try { serialPort?.close() } catch (_: IOException) {}
        serialPort = null
    }

    /** 重连：重新 prepare + connect */
    fun reconnect(): Boolean {
        val name = lastDeviceName ?: return false
        val cfg = lastConfig ?: return false
        if (!prepareDevice(name)) return false
        return connect(cfg)
    }

    fun scanDevices(): List<DeviceItem> {
        val devices = mutableListOf<DeviceItem>()
        val foundDeviceNames = mutableSetOf<String>()

        // 1. 默认探针扫描已知设备
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        for (driver in availableDrivers) {
            val device = driver.device
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

        // 3. 如果还没找到，列出所有 USB 设备（VID/PID）方便调试
        if (devices.isEmpty()) {
            for ((_, usbDevice) in usbManager.deviceList) {
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

    fun hasPermission(deviceName: String): Boolean {
        val device = findDevice(deviceName) ?: return false
        return usbManager.hasPermission(device)
    }

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

    fun prepareDevice(deviceName: String): Boolean {
        val device = findDevice(deviceName) ?: return false
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val driver = drivers.find { it.device == device } ?: probeDevice(device) ?: return false
        serialPort = driver.ports.firstOrNull() ?: return false
        usbDevice = device
        return true
    }

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

    override fun send(data: ByteArray): Boolean {
        val port = serialPort ?: return false
        if (!_isConnected) return false
        return try { port.write(data, 1000); true } catch (e: IOException) { e.printStackTrace(); false }
    }

    override fun setDataListener(listener: (ByteArray) -> Unit) {
        dataListener = listener
    }

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

    private fun findDevice(name: String): UsbDevice? =
        usbManager.deviceList.values.find { it.deviceName == name }
}
