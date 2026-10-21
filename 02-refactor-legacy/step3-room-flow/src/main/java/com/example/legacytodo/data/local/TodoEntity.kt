package com.example.legacytodo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.legacytodo.model.Todo

/**
 * 【Step 3 で新設】Room の Entity。
 * Step 0〜2 の `CREATE TABLE` 文字列が、コンパイラにチェックされるクラス定義になった。
 * カラム名は旧テーブルと同じにしてある(実運用なら Migration で移行する)。
 */
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
