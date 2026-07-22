package com.hehaoren.comassist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hehaoren.comassist.serial.QuickCommand

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickCommandCard(
    commands: List<QuickCommand>,
    isConnected: Boolean,
    onSend: (QuickCommand) -> Unit,
    onAdd: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit,
    onExport: () -> String,
    onImport: (String) -> Int,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editingCommand by remember { mutableStateOf<QuickCommand?>(null) }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("指令", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAddDialog = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("+", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { showImportDialog = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("导入", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            if (commands.isEmpty()) {
                Text("暂无指令", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(commands, key = { it.id }) { cmd ->
                        var showMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.wrapContentSize()) {
                            FilterChip(
                                selected = false,
                                onClick = { showMenu = true },
                                label = { Text(cmd.name, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                                trailingIcon = {
                                    Text(
                                        if (cmd.isHex) "HEX" else "TXT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("发送") },
                                    onClick = { if (isConnected) onSend(cmd); showMenu = false }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                    onClick = { onRemove(cmd.id); showMenu = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加指令对话框
    if (showAddDialog) {
        QuickCommandDialog(
            title = "添加快捷指令",
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { cmd ->
                onAdd(cmd)
                showAddDialog = false
            }
        )
    }

    // 编辑/删除对话框
    editingCommand?.let { cmd ->
        AlertDialog(
            onDismissRequest = { editingCommand = null },
            title = { Text("指令操作") },
            text = { Text("${cmd.name}\n${cmd.command}") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(cmd.id)
                    editingCommand = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { editingCommand = null }) { Text("取消") }
            }
        )
    }

    // 导入对话框
    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json ->
                val count = onImport(json)
                showImportDialog = false
                count
            }
        )
    }
}

@Composable
private fun QuickCommandDialog(
    title: String,
    initial: QuickCommand?,
    onDismiss: () -> Unit,
    onConfirm: (QuickCommand) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var command by remember { mutableStateOf(initial?.command ?: "") }
    var isHex by remember { mutableStateOf(initial?.isHex ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("名称") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = command, onValueChange = { command = it },
                    label = { Text("指令内容") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("格式:", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = !isHex, onClick = { isHex = false }, label = { Text("ASCII") })
                    Spacer(Modifier.width(4.dp))
                    FilterChip(selected = isHex, onClick = { isHex = true }, label = { Text("HEX") })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotEmpty() && command.isNotEmpty()) onConfirm(QuickCommand(name = name, command = command, isHex = isHex)) },
                enabled = name.isNotEmpty() && command.isNotEmpty()
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Int
) {
    var jsonText by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入快捷指令") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("粘贴 JSON 格式指令:", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = jsonText, onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("""[{"name":"查询","command":"AT?","isHex":false}]""", style = MaterialTheme.typography.bodySmall) }
                )
                if (resultMessage.isNotEmpty()) {
                    Text(resultMessage, style = MaterialTheme.typography.bodySmall,
                        color = if (resultMessage.startsWith("成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val count = onImport(jsonText)
                resultMessage = if (count >= 0) "成功导入 $count 条指令" else "JSON 格式错误"
            }) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
