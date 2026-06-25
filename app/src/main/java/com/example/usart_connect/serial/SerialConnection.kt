package com.example.usart_connect.serial

/** 串口连接类型 */
object ConnectionTypes {
    const val USB = "USB"
    const val BLUETOOTH = "蓝牙"
}

/** 数据位 */
object DataBitsValues {
    const val FIVE = 5
    const val SIX = 6
    const val SEVEN = 7
    const val EIGHT = 8
    val ALL = listOf(FIVE, SIX, SEVEN, EIGHT)
}

/** 停止位 */
object StopBitsValues {
    const val ONE = 1.0f
    const val ONE_AND_HALF = 1.5f
    const val TWO = 2.0f
    val ALL = listOf(ONE, ONE_AND_HALF, TWO)
    fun labelOf(v: Float): String = when (v) {
        ONE -> "1"
        ONE_AND_HALF -> "1.5"
        TWO -> "2"
        else -> "?"
    }
}

/** 校验位 */
object ParityValues {
    const val NONE = 0
    const val ODD = 1
    const val EVEN = 2
    val ALL = listOf(NONE, ODD, EVEN)
    fun labelOf(v: Int): String = when (v) {
        NONE -> "None"
        ODD -> "Odd"
        EVEN -> "Even"
        else -> "?"
    }
}

/** 行尾追加 */
object LineEndingValues {
    const val NONE = 0
    const val CR = 1
    const val LF = 2
    const val CRLF = 3
    val ALL = listOf(NONE, CR, LF, CRLF)
    fun labelOf(v: Int): String = when (v) {
        NONE -> "无"
        CR -> "CR"
        LF -> "LF"
        CRLF -> "CRLF"
        else -> "?"
    }
    fun bytesOf(v: Int): ByteArray? = when (v) {
        CR -> byteArrayOf(0x0D)
        LF -> byteArrayOf(0x0A)
        CRLF -> byteArrayOf(0x0D, 0x0A)
        else -> null
    }
}

/** 显示模式 */
object DisplayModes {
    const val HEX = 0
    const val ASCII = 1
    val ALL = listOf(HEX, ASCII)
    fun labelOf(v: Int): String = when (v) {
        HEX -> "HEX"
        ASCII -> "ASCII"
        else -> "?"
    }
}

/** 串口配置 */
data class SerialConfig(
    val baudRate: Int = 115200,
    val dataBits: Int = DataBitsValues.EIGHT,
    val stopBits: Float = StopBitsValues.ONE,
    val parity: Int = ParityValues.NONE,
    val lineEnding: Int = LineEndingValues.NONE
)

/** 设备列表项 */
data class DeviceItem(
    val name: String,
    val address: String,
    val connectionType: String
) {
    override fun toString(): String = name
}

/** 串口连接接口 */
interface SerialConnection {
    val isConnected: Boolean
    fun connect(config: SerialConfig): Boolean
    fun disconnect()
    fun send(data: ByteArray): Boolean
    fun setDataListener(listener: (ByteArray) -> Unit)
}
