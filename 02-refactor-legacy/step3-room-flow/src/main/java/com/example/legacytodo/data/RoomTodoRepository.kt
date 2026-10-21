package com.example.legacytodo.data

import com.example.legacytodo.data.local.TodoDao
import com.example.legacytodo.data.local.TodoEntity
import com.example.legacytodo.data.local.asModel
import com.example.legacytodo.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 【Step 3 で新設】[SqliteTodoRepository] を置き換える Room 版の実装。
 *
 * Step 2 までの `withContext(Dispatchers.IO)` が消えたことに注目。
 * Room の suspend 関数と Flow は内部で適切なスレッドを使うので、
 * Repository がスレッドを意識する必要がなくなった(それでも main-safe は保たれている)。
 *
 * Repository は「DB の行(TodoEntity)」を「アプリのモデル(Todo)」に変換して返す。
 * UI 層は Room の存在を知らない。
 */
class RoomTodoRepository(
    private val todoDao: TodoDao,
) : TodoRepository {

    override fun observeTodos(): Flow<List<Todo>> =
        todoDao.observeAll().map { entities -> entities.map(TodoEntity::asModel) }

    override suspend fun addTodo(title: String) {
        todoDao.insert(TodoEntity(title = title, createdAt = System.currentTimeMillis()))
    }

    override suspend fun setDone(id: Long, isDone: Boolean) = todoDao.setDone(id, isDone)

    override suspend fun deleteTodo(id: Long) = todoDao.deleteById(id)
}
