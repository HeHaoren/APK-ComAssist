package com.example.usart_connect.serial

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

class BtSerialManager(private val context: Context) : SerialConnection {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    private var btSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readThread: Thread? = null
    private var dataListener: ((ByteArray) -> Unit)? = null
    private var _isConnected = false
    private var pendingAddress: String? = null

    override val isConnected: Boolean get() = _isConnected

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

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

    fun prepareDevice(address: String): Boolean {
        pendingAddress = address
        return true
    }

    @SuppressLint("MissingPermission")
    override fun connect(config: SerialConfig): Boolean {
        val adapter = bluetoothAdapter ?: return false
        val address = pendingAddress ?: return false
        val device = adapter.getRemoteDevice(address) ?: return false
        try {
            btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
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

    override fun disconnect() {
        _isConnected = false
        readThread?.interrupt()
        readThread = null
        try { inputStream?.close(); outputStream?.close(); btSocket?.close() } catch (_: IOException) {}
        inputStream = null; outputStream = null; btSocket = null
    }

    override fun send(data: ByteArray): Boolean {
        if (!_isConnected) return false
        return try { outputStream?.write(data); outputStream?.flush(); true }
        catch (e: IOException) { e.printStackTrace(); false }
    }

    override fun setDataListener(listener: (ByteArray) -> Unit) {
        dataListener = listener
    }

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
