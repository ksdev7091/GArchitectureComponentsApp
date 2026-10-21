package com.example.legacytodo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.legacytodo.model.Todo

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "is_done")
    val isDone: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

fun TodoEntity.asModel(): Todo = Todo(
    id = id,
    title = title,
    isDone = isDone,
    createdAt = createdAt,
)
