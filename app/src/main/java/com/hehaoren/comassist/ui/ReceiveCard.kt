package com.hehaoren.comassist.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hehaoren.comassist.serial.DisplayModes

/**
 * 数据接收卡片组件
 *
 * 显示接收到的串口数据，支持：
 * - ASCII/HEX 显示模式切换
 * - 自动滚动
 * - 清屏操作
 *
 * @param receivedText 接收到的文本
 * @param receiveMode 当前接收显示模式
 * @param scrollState 滚动状态
 * @param onModeChange 显示模式变更回调
 * @param onClear 清屏按钮点击回调
 * @param modifier Modifier
 */
@Composable
fun ReceiveCard(
    receivedText: String,
    receiveMode: Int,
    scrollState: ScrollState,
    onModeChange: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 标题行和模式选择
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("接收", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                for (mode in DisplayModes.ALL) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                        RadioButton(selected = receiveMode == mode, onClick = { onModeChange(mode) }, modifier = Modifier.size(20.dp))
                        Text(DisplayModes.labelOf(mode), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("清屏", style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.height(4.dp))
            // 数据显示区域
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small).padding(8.dp)) {
                if (receivedText.isEmpty()) Text("等待数据...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text(receivedText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(scrollState))
            }
        }
    }
}
