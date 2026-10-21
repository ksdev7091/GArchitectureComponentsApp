package com.example.legacytodo.data

import com.example.legacytodo.model.Todo

interface TodoRepository {
    suspend fun getTodos(): List<Todo>
    suspend fun addTodo(title: String)
    suspend fun setDone(id: Long, isDone: Boolean)
    suspend fun deleteTodo(id: Long)
}
