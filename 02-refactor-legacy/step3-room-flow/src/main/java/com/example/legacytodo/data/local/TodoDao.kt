package com.example.legacytodo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 【Step 3 で新設】DAO。SQL はここに集約され、列名の typo はコンパイル時に検出される。
 *
 * 一番大事なのは [observeAll] の戻り値が **Flow** であること。
 * テーブルが変わるたびに Room が自動で再クエリして新しいリストを流す。
 * これで「書き込んだら手で読み直す」が不要になる。
 */
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
