package com.example.legacytodo.data

import com.example.legacytodo.model.Todo
import kotlinx.coroutines.flow.Flow

/**
 * 【Step 3 で変更】`suspend fun getTodos(): List<Todo>` が `fun observeTodos(): Flow<List<Todo>>` になった。
 *
 * - 「取りに行く(pull)」から「流れてくる(push)」へ。
 * - 書き込み系(add/setDone/delete)は戻り値を持たない。書き込んだ結果は
 *   observeTodos() を通じて自動的に届くので、呼び出し側が読み直す必要がない。
 */
interface TodoRepository {
    fun observeTodos(): Flow<List<Todo>>
    suspend fun addTodo(title: String)
    suspend fun setDone(id: Long, isDone: Boolean)
    suspend fun deleteTodo(id: Long)
}
