package com.example.legacytodo.data

import android.content.ContentValues
import com.example.legacytodo.model.Todo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 【Step 1 で新設】Step 0 の MainActivity に散らばっていた DB アクセスを、
 * ほぼそのままここへ移した。
 *
 * 変えたこと:
 * - `Thread { }` + `runOnUiThread { }` → `withContext(Dispatchers.IO)`。
 *   呼び出し側は結果を「戻り値」として受け取れるので、コールバック地獄が消える。
 * - DB アクセスが Activity のライフサイクルと無関係になった。
 *
 * 変えていないこと:
 * - SQL も SQLiteOpenHelper もそのまま。「場所を変える」のが今回の目的で、
 *   「仕組みを変える」(Room 化)は Step 3 でやる。
 */
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
