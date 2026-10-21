package com.example.archnotes.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Room の DAO(Data Access Object)。SQL はここにだけ書く。
 *
 * - 戻り値を [Flow] にすると、テーブルが変わるたびに Room が自動で再クエリして流してくれる。
 *   これが「データの変化を UI まで自動で伝える」仕組みの出発点。
 * - 書き込みは suspend 関数にする。Room が自動でバックグラウンドスレッドで実行する。
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    /** 主キーが未採番(0)なら INSERT、既存なら UPDATE。戻り値は採番された行 ID。 */
    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
