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
            // 左侧：连接 + 参数 + 发送（可滚动）
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                ConnectionCard(connectionType, devices, selectedDevice, isConnected, statusMessage,
                    { if (connectionType == ConnectionTypes.BLUETOOTH) checkBtAndScan() else vm.scanDevices() },
                    { vm.setConnectionType(it) }, { vm.selectDevice(it) }, { vm.connect() }, { vm.disconnect() })
                Spacer(Modifier.height(cardSpacing))
                ConfigCard(serialConfig, isConnected) { vm.updateConfig(it) }
                Spacer(Modifier.height(cardSpacing))
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
                { vm.setConnectionType(it) }, { vm.selectDevice(it) }, { vm.connect() }, { vm.disconnect() })
            Spacer(Modifier.height(cardSpacing))
            ConfigCard(serialConfig, isConnected) { vm.updateConfig(it) }
            Spacer(Modifier.height(cardSpacing))
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
    onDisconnect: () -> Unit
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
private fun ConfigCard(config: SerialConfig, isConnected: Boolean, onConfigChange: (SerialConfig) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("串口参数", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                var baudExpanded by remember { mutableStateOf(false) }
                val baudRates = listOf(300, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600)
                ExposedDropdownMenuBox(baudExpanded, { baudExpanded = it && !isConnected }, Modifier.weight(1f)) {
                    OutlinedTextField(value = config.baudRate.toString(), onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(baudRate = v)) } },
                        label = { Text("波特率") }, enabled = !isConnected, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(baudExpanded) }, modifier = Modifier.menuAnchor(), textStyle = MaterialTheme.typography.bodySmall)
                    ExposedDropdownMenu(baudExpanded, { baudExpanded = false }) {
                        for (r in baudRates) DropdownMenuItem(text = { Text(r.toString()) }, onClick = { onConfigChange(config.copy(baudRate = r)); baudExpanded = false })
                    }
                }
                var dbExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(dbExpanded, { dbExpanded = it && !isConnected }, Modifier.weight(1f)) {
                    OutlinedTextField(value = config.dataBits.toString(), onValueChange = {}, readOnly = true, label = { Text("数据位") }, enabled = !isConnected,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dbExpanded) }, modifier = Modifier.menuAnchor(), textStyle = MaterialTheme.typography.bodySmall)
                    ExposedDropdownMenu(dbExpanded, { dbExpanded = false }) {
                        for (v in DataBitsValues.ALL) DropdownMenuItem(text = { Text(v.toString()) }, onClick = { onConfigChange(config.copy(dataBits = v)); dbExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                var sbExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(sbExpanded, { sbExpanded = it && !isConnected }, Modifier.weight(1f)) {
                    OutlinedTextField(value = StopBitsValues.labelOf(config.stopBits), onValueChange = {}, readOnly = true, label = { Text("停止位") }, enabled = !isConnected,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sbExpanded) }, modifier = Modifier.menuAnchor(), textStyle = MaterialTheme.typography.bodySmall)
                    ExposedDropdownMenu(sbExpanded, { sbExpanded = false }) {
                        for (v in StopBitsValues.ALL) DropdownMenuItem(text = { Text(StopBitsValues.labelOf(v)) }, onClick = { onConfigChange(config.copy(stopBits = v)); sbExpanded = false })
                    }
                }
                var pExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(pExpanded, { pExpanded = it && !isConnected }, Modifier.weight(1f)) {
                    OutlinedTextField(value = ParityValues.labelOf(config.parity), onValueChange = {}, readOnly = true, label = { Text("校验位") }, enabled = !isConnected,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(pExpanded) }, modifier = Modifier.menuAnchor(), textStyle = MaterialTheme.typography.bodySmall)
                    ExposedDropdownMenu(pExpanded, { pExpanded = false }) {
                        for (v in ParityValues.ALL) DropdownMenuItem(text = { Text(ParityValues.labelOf(v)) }, onClick = { onConfigChange(config.copy(parity = v)); pExpanded = false })
                    }
                }
            }
        }
    }
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
