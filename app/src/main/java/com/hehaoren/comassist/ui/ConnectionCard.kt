package com.hehaoren.comassist.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hehaoren.comassist.serial.ConnectionTypes
import com.hehaoren.comassist.serial.DeviceItem

/**
 * 连接控制卡片组件
 *
 * 提供设备连接相关的 UI 控件：
 * - 连接类型选择（USB/蓝牙）
 * - 设备扫描和选择
 * - 连接/断开操作
 * - 网卡面板切换
 *
 * @param connectionType 当前连接类型
 * @param devices 可用设备列表
 * @param selectedDevice 选中的设备
 * @param isConnected 是否已连接
 * @param statusMessage 状态消息
 * @param onScan 扫描按钮点击回调
 * @param onTypeChange 连接类型变更回调
 * @param onDeviceSelect 设备选择回调
 * @param onConnect 连接按钮点击回调
 * @param onDisconnect 断开按钮点击回调
 * @param showNetworkPanel 是否显示网卡面板
 * @param onToggleNetwork 网卡面板切换回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionCard(
    connectionType: String,
    devices: List<DeviceItem>,
    selectedDevice: DeviceItem?,
    isConnected: Boolean,
    statusMessage: String,
    onScan: () -> Unit,
    onTypeChange: (String) -> Unit,
    onDeviceSelect: (DeviceItem) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    showNetworkPanel: Boolean = false,
    onToggleNetwork: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 连接类型选择和操作按钮
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("连接:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = connectionType == ConnectionTypes.USB, onClick = { onTypeChange(ConnectionTypes.USB) }, enabled = !isConnected)
                    Text("USB", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                    RadioButton(selected = connectionType == ConnectionTypes.BLUETOOTH, onClick = { onTypeChange(ConnectionTypes.BLUETOOTH) }, enabled = !isConnected)
                    Text("蓝牙", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onToggleNetwork) { Text(if (showNetworkPanel) "隐藏网卡" else "网卡") }
                TextButton(onClick = onScan, enabled = !isConnected) { Text("扫描") }
            }
            Spacer(Modifier.height(6.dp))
            // 设备选择和连接按钮
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded, { expanded = it && !isConnected }, Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedDevice?.name ?: "请先扫描", onValueChange = {}, readOnly = true, enabled = !isConnected,
                        label = { Text("设备") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(), textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        if (devices.isEmpty()) DropdownMenuItem(text = { Text("无可用设备") }, onClick = { expanded = false })
                        for (d in devices) DropdownMenuItem(text = { Text(d.name, style = MaterialTheme.typography.bodySmall) }, onClick = { onDeviceSelect(d); expanded = false })
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (isConnected) onDisconnect() else onConnect() },
                    colors = if (isConnected) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) { Text(if (isConnected) "断开" else "连接") }
            }
            // 状态消息
            if (statusMessage.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
