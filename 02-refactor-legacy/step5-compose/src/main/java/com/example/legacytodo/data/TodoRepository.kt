package com.example.legacytodo.data

import com.example.legacytodo.model.Todo
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun observeTodos(): Flow<List<Todo>>
    suspend fun addTodo(title: String)
    suspend fun setDone(id: Long, isDone: Boolean)
    suspend fun deleteTodo(id: Long)
}
