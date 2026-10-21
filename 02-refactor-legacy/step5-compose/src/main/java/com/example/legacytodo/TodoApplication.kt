package com.example.legacytodo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 【Step 4 で変更】手書きの AppContainer が消え、アノテーション 1 つになった。
 * Hilt が AppContainer 相当のコードを生成する。
 */
@HiltAndroidApp
class TodoApplication : Application()
