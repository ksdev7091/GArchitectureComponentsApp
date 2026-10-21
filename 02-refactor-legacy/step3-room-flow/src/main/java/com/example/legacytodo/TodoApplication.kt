package com.example.legacytodo

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.legacytodo.data.RoomTodoRepository
import com.example.legacytodo.data.TodoRepository
import com.example.legacytodo.data.local.TodoDatabase

/**
 * 【Step 3 で変更】Repository の実装が Room 版に変わった。
 *
 * 変更したのはこのファイルの組み立て部分だけで、MainActivity は一切触っていない。
 * Step 2 で依存の組み立てを 1 か所に集めておいたおかげ。
 */
class AppContainer(context: Context) {
    private val database: TodoDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, TodoDatabase::class.java, "todo_room.db").build()
    }

    val todoRepository: TodoRepository by lazy {
        RoomTodoRepository(database.todoDao())
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
