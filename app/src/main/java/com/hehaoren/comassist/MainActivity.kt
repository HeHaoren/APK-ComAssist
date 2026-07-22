package com.hehaoren.comassist

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hehaoren.comassist.ui.SerialScreen

/**
 * 主 Activity
 *
 * 应用的入口 Activity，负责初始化 UI 和提供系统服务
 */
class MainActivity : ComponentActivity() {

    /**
     * Activity 创建时的初始化
     *
     * @param savedInstanceState 保存的实例状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SerialScreen()
                }
            }
        }
    }

    /**
     * 获取当前屏幕刷新率
     *
     * @return 屏幕刷新率 (Hz)，获取失败返回 60f
     */
    fun getRefreshRate(): Float {
        return try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.refreshRate
        } catch (_: Exception) {
            60f
        }
    }
}
