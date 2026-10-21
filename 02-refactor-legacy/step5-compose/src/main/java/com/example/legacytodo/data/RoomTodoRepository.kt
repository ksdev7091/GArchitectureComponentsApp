package com.example.legacytodo.data

import com.example.legacytodo.data.local.TodoDao
import com.example.legacytodo.data.local.TodoEntity
import com.example.legacytodo.data.local.asModel
import com.example.legacytodo.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 【Step 4 で変更】`@Inject constructor` を付けた。
 * これだけで Hilt は「TodoDao をくれれば RoomTodoRepository を作れる」と理解する。
 * `@Singleton` でアプリ内に 1 つだけ存在することを保証する。
 */
@Singleton
class RoomTodoRepository @Inject constructor(
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
