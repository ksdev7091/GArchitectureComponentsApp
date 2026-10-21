package com.example.legacytodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.legacytodo.ui.TodoScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * 【Step 5: Compose 化】Activity は Compose のツリーを立ち上げるだけになった。
 * Step 0 で 150 行あった MainActivity が、最終的にこの大きさになる。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TodoScreen()
            }
        }
    }
}
