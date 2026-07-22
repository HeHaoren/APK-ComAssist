package com.hehaoren.comassist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hehaoren.comassist.serial.DisplayModes
import com.hehaoren.comassist.serial.LineEndingValues

/**
 * 数据发送卡片组件
 *
 * 提供数据发送功能：
 * - ASCII/HEX 发送模式切换
 * - 行尾追加字符选择
 * - 数据输入和发送
 *
 * @param sendText 当前发送文本
 * @param sendMode 发送显示模式
 * @param lineEnding 行尾追加模式
 * @param isConnected 是否已连接
 * @param onSendTextChange 发送文本变更回调
 * @param onModeChange 发送模式变更回调
 * @param onLineEndingChange 行尾追加模式变更回调
 * @param onSend 发送按钮点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendCard(
    sendText: String,
    sendMode: Int,
    lineEnding: Int,
    isConnected: Boolean,
    onSendTextChange: (String) -> Unit,
    onModeChange: (Int) -> Unit,
    onLineEndingChange: (Int) -> Unit,
    onSend: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 标题行和模式选择
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("发送", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                for (mode in DisplayModes.ALL) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                        RadioButton(selected = sendMode == mode, onClick = { onModeChange(mode) }, modifier = Modifier.size(20.dp))
                        Text(DisplayModes.labelOf(mode), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.width(8.dp))
                var leExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(leExpanded, { leExpanded = it }) {
                    Text("行尾:${LineEndingValues.labelOf(lineEnding)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.menuAnchor().padding(end = 4.dp))
                    ExposedDropdownMenu(leExpanded, { leExpanded = false }) {
                        for (le in LineEndingValues.ALL) DropdownMenuItem(text = { Text(LineEndingValues.labelOf(le)) }, onClick = { onLineEndingChange(le); leExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // 输入框和发送按钮
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = sendText, onValueChange = onSendTextChange, modifier = Modifier.weight(1f),
                    placeholder = { Text(if (sendMode == DisplayModes.HEX) "FF 01 02 03" else "输入发送内容", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSend, enabled = isConnected && sendText.isNotEmpty()) { Text("发送") }
            }
        }
    }
}
