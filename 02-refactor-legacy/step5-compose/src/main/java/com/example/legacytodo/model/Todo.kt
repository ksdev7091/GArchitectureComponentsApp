package com.example.legacytodo.model

data class Todo(
    val id: Long,
    val title: String,
    val isDone: Boolean,
    val createdAt: Long,
)
