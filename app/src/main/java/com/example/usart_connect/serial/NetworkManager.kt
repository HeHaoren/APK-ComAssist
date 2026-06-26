package com.example.usart_connect.serial

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import java.net.NetworkInterface

/** 网卡设备信息 */
data class NetworkDeviceInfo(
    val name: String,           // 接口名 (eth0, wlan0 等)
    val displayName: String,    // 显示名称
    val macAddress: String,     // MAC 地址
    val ipAddresses: List<String>,  // IPv4 地址
    val ipv6Addresses: List<String>, // IPv6 地址
    val subnetMasks: List<String>,  // 子网掩码
    val gateway: String,        // 网关
    val dnsServers: List<String>,   // DNS 服务器
    val isUp: Boolean,          // 是否启用
    val isLoopback: Boolean,    // 是否回环
    val mtu: Int,               // MTU
    val vendorId: String,       // USB 设备 VID (如果是 USB 网卡)
    val productId: String       // USB 设备 PID (如果是 USB 网卡)
)

class NetworkManager(private val context: Context) {

    /** 扫描所有网络接口 */
    fun scanNetworkDevices(): List<NetworkDeviceInfo> {
        val devices = mutableListOf<NetworkDeviceInfo>()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return devices

            for (iface in interfaces) {
                // 跳过回环接口
                if (iface.isLoopback) continue
                // 跳过未启用的接口
                if (!iface.isUp) continue

                val name = iface.name
                val displayName = getDisplayName(name)
                val macAddress = getMacAddress(iface)
                val ipAddresses = mutableListOf<String>()
                val ipv6Addresses = mutableListOf<String>()
                val subnetMasks = mutableListOf<String>()

                // 获取 IP 地址和子网掩码
                for (address in iface.interfaceAddresses) {
                    val ip = address.address
                    val prefixLength = address.networkPrefixLength

                    if (ip is java.net.Inet4Address) {
                        ipAddresses.add(ip.hostAddress ?: "")
                        subnetMasks.add(prefixLengthToSubnetMask(prefixLength.toInt()))
                    } else if (ip is java.net.Inet6Address && !ip.isLoopbackAddress) {
                        ipv6Addresses.add(ip.hostAddress ?: "")
                    }
                }

                // 获取网关和 DNS
                var gateway = ""
                val dnsServers = mutableListOf<String>()

                if (connectivityManager != null) {
                    try {
                        val network = connectivityManager.boundNetworkForProcess
                            ?: connectivityManager.activeNetwork
                        if (network != null) {
                            val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(network)
                            if (linkProperties != null && linkProperties.interfaceName == name) {
                                // 网关
                                for (route in linkProperties.routes) {
                                    if (route.isDefaultRoute && route.gateway is java.net.Inet4Address) {
                                        gateway = route.gateway?.hostAddress ?: ""
                                    }
                                }
                                // DNS
                                for (dns in linkProperties.dnsServers) {
                                    dnsServers.add(dns.hostAddress ?: "")
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                val mtu = try { iface.mtu } catch (_: Exception) { 0 }

                devices.add(NetworkDeviceInfo(
                    name = name,
                    displayName = displayName,
                    macAddress = macAddress,
                    ipAddresses = ipAddresses,
                    ipv6Addresses = ipv6Addresses,
                    subnetMasks = subnetMasks,
                    gateway = gateway,
                    dnsServers = dnsServers,
                    isUp = iface.isUp,
                    isLoopback = iface.isLoopback,
                    mtu = mtu,
                    vendorId = "",
                    productId = ""
                ))
            }
        } catch (_: Exception) {}

        return devices
    }

    private fun getDisplayName(name: String): String = when {
        name.startsWith("wlan") -> "WiFi ($name)"
        name.startsWith("eth") -> "以太网 ($name)"
        name.startsWith("rmnet") -> "移动数据 ($name)"
        name.startsWith("usb") -> "USB 网卡 ($name)"
        name.startsWith("rndis") -> "USB 网络共享 ($name)"
        name.startsWith("lo") -> "回环 ($name)"
        else -> name
    }

    private fun getMacAddress(iface: NetworkInterface): String {
        return try {
            val mac = iface.hardwareAddress ?: return "未知"
            mac.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) { "未知" }
    }

    private fun prefixLengthToSubnetMask(prefixLength: Int): String {
        if (prefixLength < 0 || prefixLength > 32) return "未知"
        val mask = (0xFFFFFFFF.toInt() shl (32 - prefixLength))
        return "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"
    }
}
