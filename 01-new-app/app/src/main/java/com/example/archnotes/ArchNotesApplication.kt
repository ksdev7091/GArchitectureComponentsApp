package com.example.archnotes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * アプリのエントリポイント。
 *
 * [HiltAndroidApp] を付けると、Hilt がアプリ全体の依存関係グラフ(SingletonComponent)を生成する。
 * 「誰が何を new するか」をアプリ全体で一元管理するのが DI の目的であり、
 * 各クラスは自分が必要とするものをコンストラクタで宣言するだけでよくなる。
 */
@HiltAndroidApp
class ArchNotesApplication : Application()
