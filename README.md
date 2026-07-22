# APK-ComAssist

安卓串口 & 蓝牙调试助手

## 功能特性

- **USB 串口通信**：支持 CH340、CP2102、FTDI、PL2303 等常见 USB 转串口芯片
- **蓝牙串口通信**：支持经典蓝牙 SPP 协议
- **网卡信息查看**：扫描设备网络接口，显示 IP、MAC、网关等信息
- **数据收发**：支持 ASCII 和 HEX 两种数据格式
- **快捷指令**：支持添加、删除、导入/导出常用指令
- **串口参数配置**：波特率、数据位、停止位、校验位可自由设置
- **自适应布局**：横屏/竖屏自动切换布局

## 技术栈

- **语言**：Kotlin 100%
- **UI 框架**：Jetpack Compose
- **构建工具**：Gradle (Kotlin DSL)
- **最低 SDK**：Android 8.0 (API 26)
- **目标 SDK**：Android 15 (API 35)

## 项目结构

```
app/src/main/java/com/example/usart_connect/
├── MainActivity.kt              # 主入口
├── SerialViewModel.kt           # 业务逻辑
├── serial/
│   ├── SerialConnection.kt      # 连接接口定义
│   ├── UsbSerialManager.kt      # USB 串口管理
│   ├── BtSerialManager.kt       # 蓝牙串口管理
│   └── NetworkManager.kt        # 网卡扫描
└── ui/
    ├── SerialScreen.kt          # 主屏幕布局
    ├── ConnectionCard.kt        # 连接控制组件
    ├── ConfigCard.kt            # 串口参数组件
    ├── QuickCommandCard.kt      # 快捷指令组件
    ├── NetworkCard.kt           # 网卡信息组件
    ├── ReceiveCard.kt           # 数据接收组件
    └── SendCard.kt              # 数据发送组件
```

## 使用说明

### 连接设备

1. **USB 串口**：选择 USB 模式，点击扫描，从列表中选择设备，点击连接
2. **蓝牙串口**：选择蓝牙模式，确保蓝牙已开启，扫描并连接已配对设备

### 发送数据

1. 在发送区输入数据（ASCII 或 HEX 格式）
2. 选择行尾追加（无/CR/LF/CRLF）
3. 点击发送按钮

### 快捷指令

1. 点击 "+" 按钮添加新指令
2. 输入指令名称和内容
3. 选择数据格式（ASCII/HEX）
4. 点击指令芯片可快速发送

## 构建说明

### 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 21
- Android SDK 35

### 构建步骤

```bash
# 克隆项目
git clone https://github.com/HeHaoren/APK-ComAssist.git

# 进入项目目录
cd APK-ComAssist

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

## 更新日志

### 2026.6.26
- 新增网卡扫描及数据接收功能
- 实现串口参数设置，支持手动输入波特率
- 增加快捷指令的增删功能，支持点击发送和删除
- 优化 UI 格式和布局

## 许可证

本项目仅供学习交流使用。
