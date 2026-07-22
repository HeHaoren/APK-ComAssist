package com.hehaoren.comassist.serial

/**
 * 串口连接类型常量定义
 *
 * 定义了应用支持的两种串口连接方式：USB 和蓝牙
 */
object ConnectionTypes {
    /** USB 串口连接 */
    const val USB = "USB"

    /** 蓝牙串口连接 */
    const val BLUETOOTH = "蓝牙"
}

/**
 * 数据位配置选项
 *
 * 串口通信中每个数据帧的数据位数，常见值为 8 位
 */
object DataBitsValues {
    const val FIVE = 5
    const val SIX = 6
    const val SEVEN = 7
    const val EIGHT = 8

    /** 所有可用的数据位选项 */
    val ALL = listOf(FIVE, SIX, SEVEN, EIGHT)
}

/**
 * 停止位配置选项
 *
 * 串口通信中标识数据帧结束的位数，支持 1、1.5、2 位
 */
object StopBitsValues {
    const val ONE = 1.0f
    const val ONE_AND_HALF = 1.5f
    const val TWO = 2.0f

    /** 所有可用的停止位选项 */
    val ALL = listOf(ONE, ONE_AND_HALF, TWO)

    /**
     * 获取停止位的显示标签
     *
     * @param v 停止位值
     * @return 对应的显示标签，未知值返回 "?"
     */
    fun labelOf(v: Float): String = when (v) {
        ONE -> "1"
        ONE_AND_HALF -> "1.5"
        TWO -> "2"
        else -> "?"
    }
}

/**
 * 校验位配置选项
 *
 * 串口通信中用于数据校验的位，支持无校验、奇校验、偶校验
 */
object ParityValues {
    /** 无校验 */
    const val NONE = 0

    /** 奇校验 */
    const val ODD = 1

    /** 偶校验 */
    const val EVEN = 2

    /** 所有可用的校验位选项 */
    val ALL = listOf(NONE, ODD, EVEN)

    /**
     * 获取校验位的显示标签
     *
     * @param v 校验位值
     * @return 对应的显示标签，未知值返回 "?"
     */
    fun labelOf(v: Int): String = when (v) {
        NONE -> "None"
        ODD -> "Odd"
        EVEN -> "Even"
        else -> "?"
    }
}

/**
 * 行尾追加字符配置
 *
 * 发送数据时在末尾追加的换行符类型
 */
object LineEndingValues {
    /** 不追加 */
    const val NONE = 0

    /** 回车符 (0x0D) */
    const val CR = 1

    /** 换行符 (0x0A) */
    const val LF = 2

    /** 回车换行符 (0x0D 0x0A) */
    const val CRLF = 3

    /** 所有可用的行尾追加选项 */
    val ALL = listOf(NONE, CR, LF, CRLF)

    /**
     * 获取行尾追加的显示标签
     *
     * @param v 行尾追加值
     * @return 对应的显示标签，未知值返回 "?"
     */
    fun labelOf(v: Int): String = when (v) {
        NONE -> "无"
        CR -> "CR"
        LF -> "LF"
        CRLF -> "CRLF"
        else -> "?"
    }

    /**
     * 获取行尾追加的字节数组
     *
     * @param v 行尾追加值
     * @return 对应的字节数组，NONE 返回 null
     */
    fun bytesOf(v: Int): ByteArray? = when (v) {
        CR -> byteArrayOf(0x0D)
        LF -> byteArrayOf(0x0A)
        CRLF -> byteArrayOf(0x0D, 0x0A)
        else -> null
    }
}

/**
 * 数据显示模式
 *
 * 接收和发送数据时的显示格式
 */
object DisplayModes {
    /** 十六进制显示模式 */
    const val HEX = 0

    /** ASCII 文本显示模式 */
    const val ASCII = 1

    /** 所有可用的显示模式 */
    val ALL = listOf(HEX, ASCII)

    /**
     * 获取显示模式的标签
     *
     * @param v 显示模式值
     * @return 对应的标签，未知值返回 "?"
     */
    fun labelOf(v: Int): String = when (v) {
        HEX -> "HEX"
        ASCII -> "ASCII"
        else -> "?"
    }
}

/**
 * 串口配置数据类
 *
 * 包含串口通信所需的所有配置参数
 *
 * @property baudRate 波特率，默认 115200
 * @property dataBits 数据位，默认 8 位
 * @property stopBits 停止位，默认 1 位
 * @property parity 校验位，默认无校验
 * @property lineEnding 行尾追加，默认不追加
 */
data class SerialConfig(
    val baudRate: Int = 115200,
    val dataBits: Int = DataBitsValues.EIGHT,
    val stopBits: Float = StopBitsValues.ONE,
    val parity: Int = ParityValues.NONE,
    val lineEnding: Int = LineEndingValues.NONE
)

/**
 * 设备列表项数据类
 *
 * 表示扫描到的串口设备信息
 *
 * @property name 设备显示名称
 * @property address 设备地址（USB 设备名或蓝牙 MAC 地址）
 * @property connectionType 连接类型（USB 或蓝牙）
 */
data class DeviceItem(
    val name: String,
    val address: String,
    val connectionType: String
) {
    override fun toString(): String = name
}

/**
 * 串口连接接口
 *
 * 定义了串口连接的标准操作，USB 和蓝牙连接都实现此接口
 */
interface SerialConnection {
    /** 当前是否已连接 */
    val isConnected: Boolean

    /**
     * 建立连接
     *
     * @param config 串口配置参数
     * @return 连接是否成功
     */
    fun connect(config: SerialConfig): Boolean

    /** 断开连接 */
    fun disconnect()

    /**
     * 发送数据
     *
     * @param data 要发送的字节数组
     * @return 发送是否成功
     */
    fun send(data: ByteArray): Boolean

    /**
     * 设置数据接收监听器
     *
     * @param listener 接收到数据时的回调函数
     */
    fun setDataListener(listener: (ByteArray) -> Unit)
}

/**
 * 快捷指令数据类
 *
 * 表示用户保存的快捷发送指令
 *
 * @property id 指令唯一标识，默认为当前时间戳
 * @property name 指令显示名称
 * @property command 指令内容
 * @property isHex 是否为十六进制格式，默认为 false（ASCII 格式）
 */
data class QuickCommand(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val command: String,
    val isHex: Boolean = false
)
