package com.example.legacytodo.data

import android.content.ContentValues
import com.example.legacytodo.model.Todo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteTodoRepository(
    private val dbHelper: TodoDbHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TodoRepository {

    override suspend fun getTodos(): List<Todo> = withContext(ioDispatcher) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("todos", null, null, null, null, null, "created_at DESC")
        val result = ArrayList<Todo>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    Todo(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        isDone = it.getInt(it.getColumnIndexOrThrow("is_done")) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                    ),
                )
            }
        }
        result
    }

    override suspend fun addTodo(title: String) = withContext(ioDispatcher) {
        val values = ContentValues().apply {
            put("title", title)
            put("is_done", 0)
            put("created_at", System.currentTimeMillis())
        }
        dbHelper.writableDatabase.insert("todos", null, values)
        Unit
    }

    override suspend fun setDone(id: Long, isDone: Boolean) = withContext(ioDispatcher) {
        val values = ContentValues().apply { put("is_done", if (isDone) 1 else 0) }
        dbHelper.writableDatabase.update("todos", values, "id = ?", arrayOf(id.toString()))
        Unit
    }

    override suspend fun deleteTodo(id: Long) = withContext(ioDispatcher) {
        dbHelper.writableDatabase.delete("todos", "id = ?", arrayOf(id.toString()))
        Unit
    }
}
