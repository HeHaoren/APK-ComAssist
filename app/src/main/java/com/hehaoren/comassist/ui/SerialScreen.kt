package com.hehaoren.comassist.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hehaoren.comassist.SerialViewModel
import com.hehaoren.comassist.serial.ConnectionTypes

/**
 * 串口调试助手主屏幕
 *
 * 根据屏幕方向自动切换布局：
 * - 横屏：左右布局（左侧控制区，右侧接收区）
 * - 竖屏：上下布局
 *
 * @param vm SerialViewModel 实例
 */
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

    // 收集 ViewModel 状态
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

    // 蓝牙权限请求
    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) vm.scanDevices()
        else vm.setStatusMessage("蓝牙权限被拒绝")
    }

    /**
     * 检查蓝牙权限并扫描设备
     */
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

    // 接收数据时自动滚动到底部
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
