package com.example.archnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.archnotes.ui.navigation.ArchNotesNavHost
import com.example.archnotes.ui.theme.ArchNotesTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * アプリ唯一の Activity。
 *
 * 推奨アーキテクチャでは Activity は「OS との接点」でしかない。
 * 画面の内容やロジックは持たず、Compose のツリーを立ち上げるだけにする。
 * こうしておくと、Activity の再生成(画面回転など)が UI の状態に影響しなくなる。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArchNotesTheme {
                ArchNotesNavHost()
            }
        }
    }
}
