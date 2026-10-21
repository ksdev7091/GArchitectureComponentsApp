package com.example.legacytodo

import android.app.Application
import android.content.Context
import com.example.legacytodo.data.SqliteTodoRepository
import com.example.legacytodo.data.TodoDbHelper
import com.example.legacytodo.data.TodoRepository

/**
 * 【Step 2 で新設】手動 DI(依存性の注入)のための「コンテナ」。
 *
 * Step 1 では MainActivity が `SqliteTodoRepository(TodoDbHelper(...))` と自分で組み立てていた。
 * これだと
 * - Activity が「Repository の作り方」を知ってしまう(本来知らなくていい知識)
 * - 画面が増えるたびに同じ組み立てコードが複製される
 * - Repository のインスタンスが画面ごとにバラバラになる
 *
 * そこで「作る役」をアプリに 1 つだけあるここへ集める。
 * Hilt のようなライブラリは、これを自動生成してくれるものだと理解すればよい(Step 4)。
 */
class AppContainer(context: Context) {
    val todoRepository: TodoRepository by lazy {
        SqliteTodoRepository(TodoDbHelper(context.applicationContext))
    }
}

class TodoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
