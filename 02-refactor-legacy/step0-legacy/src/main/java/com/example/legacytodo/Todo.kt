package com.example.legacytodo

data class Todo(
    val id: Long,
    val title: String,
    val isDone: Boolean,
    val createdAt: Long,
)
