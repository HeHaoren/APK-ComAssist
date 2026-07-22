package com.hehaoren.comassist.serial

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import java.net.NetworkInterface

/**
 * 网卡设备信息数据类
 *
 * 包含网络接口的详细配置信息
 *
 * @property name 接口名称（如 eth0, wlan0）
 * @property displayName 用户友好的显示名称
 * @property macAddress MAC 地址
 * @property ipAddresses IPv4 地址列表
 * @property ipv6Addresses IPv6 地址列表
 * @property subnetMasks 子网掩码列表
 * @property gateway 默认网关地址
 * @property dnsServers DNS 服务器列表
 * @property isUp 接口是否启用
 * @property isLoopback 是否为回环接口
 * @property mtu 最大传输单元
 * @property vendorId USB 设备厂商 ID（仅 USB 网卡）
 * @property productId USB 设备产品 ID（仅 USB 网卡）
 */
data class NetworkDeviceInfo(
    val name: String,
    val displayName: String,
    val macAddress: String,
    val ipAddresses: List<String>,
    val ipv6Addresses: List<String>,
    val subnetMasks: List<String>,
    val gateway: String,
    val dnsServers: List<String>,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val mtu: Int,
    val vendorId: String,
    val productId: String
)

/**
 * 网络管理器
 *
 * 负责扫描和获取设备网络接口信息
 *
 * @param context Android 上下文
 */
class NetworkManager(private val context: Context) {

    /**
     * 扫描所有网络接口
     *
     * 遍历设备上所有活跃的网络接口，收集其配置信息
     *
     * @return 网络设备信息列表
     */
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

    /**
     * 获取网络接口的用户友好显示名称
     *
     * @param name 接口名称
     * @return 用户友好的显示名称
     */
    private fun getDisplayName(name: String): String = when {
        name.startsWith("wlan") -> "WiFi ($name)"
        name.startsWith("eth") -> "以太网 ($name)"
        name.startsWith("rmnet") -> "移动数据 ($name)"
        name.startsWith("usb") -> "USB 网卡 ($name)"
        name.startsWith("rndis") -> "USB 网络共享 ($name)"
        name.startsWith("lo") -> "回环 ($name)"
        else -> name
    }

    /**
     * 获取网络接口的 MAC 地址
     *
     * @param iface 网络接口
     * @return 格式化的 MAC 地址字符串，获取失败返回 "未知"
     */
    private fun getMacAddress(iface: NetworkInterface): String {
        return try {
            val mac = iface.hardwareAddress ?: return "未知"
            mac.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) { "未知" }
    }

    /**
     * 将前缀长度转换为子网掩码
     *
     * @param prefixLength CIDR 前缀长度（0-32）
     * @return 点分十进制格式的子网掩码
     */
    private fun prefixLengthToSubnetMask(prefixLength: Int): String {
        if (prefixLength < 0 || prefixLength > 32) return "未知"
        val mask = (0xFFFFFFFF.toInt() shl (32 - prefixLength))
        return "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"
    }
}
