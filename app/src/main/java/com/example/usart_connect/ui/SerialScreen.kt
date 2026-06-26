package com.example.usart_connect.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.usart_connect.SerialViewModel
import com.example.usart_connect.serial.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerialScreen(vm: SerialViewModel = viewModel()) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 获取屏幕刷新率并传递给 ViewModel
    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        val hz = activity?.windowManager?.defaultDisplay?.refreshRate ?: 60f
        vm.setRefreshRate(hz)
    }

    val connectionType by vm.connectionType.collectAsState()
    val devices by vm.devices.collectAsState()
    val selectedDevice by vm.selectedDevice.collectAsState()
    val isConnected by vm.isConnected.collectAsState()
    val serialConfig by vm.config.collectAsState()
    val receivedText by vm.receivedText.collectAsState()
    val sendText by vm.sendText.collectAsState()
    val receiveMode by vm.receiveMode.collectAsState()
    val sendMode by vm.sendMode.collectAsState()
    val lineEnding by vm.lineEnding.collectAsState()
    val statusMessage by vm.statusMessage.collectAsState()
    val quickCommands by vm.quickCommands.collectAsState()
    val networkDevices by vm.networkDevices.collectAsState()
    val showNetworkPanel by vm.showNetworkPanel.collectAsState()
    var showConfigPanel by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) vm.scanDevices()
        else vm.setStatusMessage("蓝牙权限被拒绝")
    }

    fun checkBtAndScan() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            vm.scanDevices()
        } else {
            btPermissionLauncher.launch(perms)
        }
    }

    LaunchedEffect(receivedText) { scrollState.animateScrollTo(scrollState.maxValue) }

    val padding = if (isLandscape) 8.dp else 12.dp
    val cardSpacing = if (isLandscape) 4.dp else 8.dp

    if (isLandscape) {
        // 横屏：左右布局
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 左侧：连接 + 参数&快捷指令 + 网卡 + 发送（可滚动）
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                ConnectionCard(connectionType, devices, selectedDevice, isConnected, statusMessage,
                    { if (connectionType == ConnectionTypes.BLUETOOTH) checkBtAndScan() else vm.scanDevices() },
                    { vm.setConnectionType(it) }, { vm.selectDevice(it) }, { vm.connect() }, { vm.disconnect() },
                    showNetworkPanel, { vm.toggleNetworkPanel() })
                Spacer(Modifier.height(cardSpacing))
                // 串口参数和快捷指令左右并排
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cardSpacing)) {
                    ConfigCard(serialConfig, isConnected, showConfigPanel, { showConfigPanel = it },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 120.dp)) { vm.updateConfig(it) }
                    QuickCommandCard(quickCommands, isConnected,
                        { vm.sendQuickCommand(it) }, { vm.addQuickCommand(it.name, it.command, it.isHex) }, { vm.removeQuickCommand(it) },
                        { vm.exportCommands() }, { vm.importCommands(it) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 120.dp))
                }
                Spacer(Modifier.height(cardSpacing))
                if (showNetworkPanel) {
                    NetworkCard(networkDevices) { vm.scanNetworkDevices() }
                    Spacer(Modifier.height(cardSpacing))
                }
                SendCard(sendText, sendMode, lineEnding, isConnected,
                    { vm.setSendText(it) }, { vm.setSendMode(it) }, { vm.setLineEnding(it) }, { vm.send() })
            }
            Spacer(Modifier.width(cardSpacing))
            // 右侧：接收区
            ReceiveCard(receivedText, receiveMode, scrollState, { vm.setReceiveMode(it) }, { vm.clearReceived() },
                modifier = Modifier.weight(0.6f).fillMaxHeight())
        }
    } else {
        // 竖屏：上下布局
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ConnectionCard(connectionType, devices, selectedDevice, isConnected, statusMessage,
                { if (connectionType == ConnectionTypes.BLUETOOTH) checkBtAndScan() else vm.scanDevices() },
                { vm.setConnectionType(it) }, { vm.selectDevice(it) }, { vm.connect() }, { vm.disconnect() },
                showNetworkPanel, { vm.toggleNetworkPanel() })
            Spacer(Modifier.height(cardSpacing))
            // 串口参数和快捷指令左右并排
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cardSpacing)) {
                ConfigCard(serialConfig, isConnected, showConfigPanel, { showConfigPanel = it },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 120.dp)) { vm.updateConfig(it) }
                QuickCommandCard(quickCommands, isConnected,
                    { vm.sendQuickCommand(it) }, { vm.addQuickCommand(it.name, it.command, it.isHex) }, { vm.removeQuickCommand(it) },
                    { vm.exportCommands() }, { vm.importCommands(it) },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 120.dp))
            }
            Spacer(Modifier.height(cardSpacing))
            if (showNetworkPanel) {
                NetworkCard(networkDevices) { vm.scanNetworkDevices() }
                Spacer(Modifier.height(cardSpacing))
            }
            ReceiveCard(receivedText, receiveMode, scrollState, { vm.setReceiveMode(it) }, { vm.clearReceived() },
                modifier = Modifier.weight(1f))
            Spacer(Modifier.height(cardSpacing))
            SendCard(sendText, sendMode, lineEnding, isConnected,
                { vm.setSendText(it) }, { vm.setSendMode(it) }, { vm.setLineEnding(it) }, { vm.send() })
        }
    }
}

// ==================== 连接控制卡片 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionCard(
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
            if (statusMessage.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ==================== 串口参数卡片 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigCard(
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

// ==================== 网卡信息卡片 ====================

@Composable
private fun NetworkCard(
    devices: List<NetworkDeviceInfo>,
    onRefresh: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
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

@Composable
private fun NetworkDeviceItem(device: NetworkDeviceInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 设备名称和状态
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(device.displayName, style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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

// ==================== 快捷指令卡片 ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun QuickCommandCard(
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

// ==================== 接收卡片 ====================

@Composable
private fun ReceiveCard(
    receivedText: String,
    receiveMode: Int,
    scrollState: androidx.compose.foundation.ScrollState,
    onModeChange: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
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
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small).padding(8.dp)) {
                if (receivedText.isEmpty()) Text("等待数据...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text(receivedText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(scrollState))
            }
        }
    }
}

// ==================== 发送卡片 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendCard(
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
