package com.hehaoren.comassist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hehaoren.comassist.serial.NetworkDeviceInfo

/**
 * 网卡信息卡片组件
 *
 * 显示设备网络接口的详细信息：
 * - 接口名称和状态
 * - MAC 地址
 * - IPv4/IPv6 地址
 * - 子网掩码、网关、DNS
 * - MTU
 *
 * @param devices 网卡设备信息列表
 * @param onRefresh 刷新按钮点击回调
 */
@Composable
fun NetworkCard(
    devices: List<NetworkDeviceInfo>,
    onRefresh: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 标题行和刷新按钮
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("网卡信息", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRefresh, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("刷新", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (devices.isEmpty()) {
                Text("未发现网卡设备", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            } else {
                for (device in devices) {
                    NetworkDeviceItem(device)
                    if (device != devices.last()) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * 网卡设备信息项组件
 *
 * @param device 网卡设备信息
 */
@Composable
private fun NetworkDeviceItem(device: NetworkDeviceInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 设备名称和状态
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(device.displayName, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(if (device.isUp) "已启用" else "未启用",
                style = MaterialTheme.typography.labelSmall,
                color = if (device.isUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(4.dp))
        // MAC 地址
        InfoRow("MAC", device.macAddress)
        // IPv4 地址
        for ((index, ip) in device.ipAddresses.withIndex()) {
            InfoRow(if (index == 0) "IPv4" else "", ip)
            if (index < device.subnetMasks.size) {
                InfoRow("掩码", device.subnetMasks[index])
            }
        }
        // IPv6 地址
        for (ip in device.ipv6Addresses.take(2)) {
            InfoRow("IPv6", ip)
        }
        // 网关
        if (device.gateway.isNotEmpty()) {
            InfoRow("网关", device.gateway)
        }
        // DNS
        for ((index, dns) in device.dnsServers.withIndex()) {
            InfoRow(if (index == 0) "DNS" else "", dns)
        }
        // MTU
        if (device.mtu > 0) {
            InfoRow("MTU", device.mtu.toString())
        }
    }
}

/**
 * 信息行组件
 *
 * @param label 标签
 * @param value 值
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        if (label.isNotEmpty()) {
            Text("$label: ", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp))
        } else {
            Spacer(Modifier.width(40.dp))
        }
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            fontSize = 11.sp)
    }
}
