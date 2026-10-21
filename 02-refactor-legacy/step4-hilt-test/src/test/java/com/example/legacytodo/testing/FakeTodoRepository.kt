package com.example.legacytodo.testing

import com.example.legacytodo.data.TodoRepository
import com.example.legacytodo.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 【Step 4 で新設】テスト用の Repository。Room の代わりにメモリ上のリストで動く。
 *
 * Step 0 ではこれが書けなかった: DB アクセスが Activity に直書きされていて、
 * 差し替える「継ぎ目」が存在しなかったから。
 * Step 1 で TodoRepository をインターフェースにした時点で、この Fake は書けるようになっていた。
 */
class FakeTodoRepository(
    initialTodos: List<Todo> = emptyList(),
) : TodoRepository {

    private val todos = MutableStateFlow(initialTodos)
    private var nextId = (initialTodos.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeTodos(): Flow<List<Todo>> = todos

    override suspend fun addTodo(title: String) {
        todos.update { it + Todo(id = nextId++, title = title, isDone = false, createdAt = 0) }
    }

    override suspend fun setDone(id: Long, isDone: Boolean) {
        todos.update { list -> list.map { if (it.id == id) it.copy(isDone = isDone) else it } }
    }

    override suspend fun deleteTodo(id: Long) {
        todos.update { list -> list.filterNot { it.id == id } }
    }
}
