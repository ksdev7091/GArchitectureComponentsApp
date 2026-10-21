package com.example.legacytodo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY created_at DESC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Insert
    suspend fun insert(todo: TodoEntity)

    @Query("UPDATE todos SET is_done = :isDone WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
