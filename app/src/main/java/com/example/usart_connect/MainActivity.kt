package com.example.usart_connect

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.usart_connect.ui.SerialScreen

class MainActivity : ComponentActivity() {
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

    /** 获取当前屏幕刷新率 (Hz) */
    fun getRefreshRate(): Float {
        return try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.refreshRate
        } catch (_: Exception) {
            60f
        }
    }
}
