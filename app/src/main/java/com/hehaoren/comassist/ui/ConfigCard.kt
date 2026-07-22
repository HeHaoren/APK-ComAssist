package com.hehaoren.comassist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hehaoren.comassist.serial.*

/**
 * 串口参数配置卡片组件
 *
 * 显示当前串口配置摘要，并提供配置弹窗修改参数：
 * - 波特率（支持手动输入和预设选择）
 * - 数据位
 * - 停止位
 * - 校验位
 *
 * @param config 当前串口配置
 * @param isConnected 是否已连接
 * @param expanded 是否展开配置弹窗
 * @param onExpandedChange 配置弹窗展开状态变更回调
 * @param modifier Modifier
 * @param onConfigChange 配置变更回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigCard(
    config: SerialConfig,
    isConnected: Boolean,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    onConfigChange: (SerialConfig) -> Unit
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 标题行：串口参数 + 设置按钮
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("串口参数", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onExpandedChange(true) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("设置", style = MaterialTheme.typography.bodySmall)
                }
            }
            // 当前配置摘要
            Text("${config.baudRate}/${config.dataBits}/${StopBitsValues.labelOf(config.stopBits)}/${ParityValues.labelOf(config.parity)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // 设置弹窗
    if (expanded) {
        AlertDialog(
            onDismissRequest = { onExpandedChange(false) },
            title = { Text("串口参数设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 波特率
                    var baudExpanded by remember { mutableStateOf(false) }
                    var baudText by remember { mutableStateOf(config.baudRate.toString()) }
                    val baudRates = listOf(300, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = baudText,
                            onValueChange = { new ->
                                baudText = new
                                new.toIntOrNull()?.let { v -> onConfigChange(config.copy(baudRate = v)) }
                            },
                            label = { Text("波特率") }, enabled = !isConnected, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                IconButton(onClick = { baudExpanded = !baudExpanded }, enabled = !isConnected) {
                                    Text("▼", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        DropdownMenu(expanded = baudExpanded, onDismissRequest = { baudExpanded = false }) {
                            for (r in baudRates) DropdownMenuItem(text = { Text(r.toString()) }, onClick = {
                                baudText = r.toString()
                                onConfigChange(config.copy(baudRate = r))
                                baudExpanded = false
                            })
                        }
                    }
                    // 数据位
                    var dbExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(dbExpanded, { dbExpanded = it && !isConnected }, Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = config.dataBits.toString(), onValueChange = {}, readOnly = true, label = { Text("数据位") }, enabled = !isConnected,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dbExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall)
                        ExposedDropdownMenu(dbExpanded, { dbExpanded = false }) {
                            for (v in DataBitsValues.ALL) DropdownMenuItem(text = { Text(v.toString()) }, onClick = { onConfigChange(config.copy(dataBits = v)); dbExpanded = false })
                        }
                    }
                    // 停止位
                    var sbExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(sbExpanded, { sbExpanded = it && !isConnected }, Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = StopBitsValues.labelOf(config.stopBits), onValueChange = {}, readOnly = true, label = { Text("停止位") }, enabled = !isConnected,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sbExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall)
                        ExposedDropdownMenu(sbExpanded, { sbExpanded = false }) {
                            for (v in StopBitsValues.ALL) DropdownMenuItem(text = { Text(StopBitsValues.labelOf(v)) }, onClick = { onConfigChange(config.copy(stopBits = v)); sbExpanded = false })
                        }
                    }
                    // 校验位
                    var pExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(pExpanded, { pExpanded = it && !isConnected }, Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = ParityValues.labelOf(config.parity), onValueChange = {}, readOnly = true, label = { Text("校验位") }, enabled = !isConnected,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(pExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall)
                        ExposedDropdownMenu(pExpanded, { pExpanded = false }) {
                            for (v in ParityValues.ALL) DropdownMenuItem(text = { Text(ParityValues.labelOf(v)) }, onClick = { onConfigChange(config.copy(parity = v)); pExpanded = false })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onExpandedChange(false) }) {
                    Text("确定")
                }
            }
        )
    }
}
