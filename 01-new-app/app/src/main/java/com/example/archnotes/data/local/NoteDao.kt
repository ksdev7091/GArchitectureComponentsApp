package com.example.archnotes.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Room の DAO(Data Access Object)。SQL はここにだけ書く。
 *
 * - 戻り値を [Flow] にすると、テーブルが変わるたびに Room が自動で再クエリして流してくれる。
 *   これが「データの変化を UI まで自動で伝える」仕組みの出発点。
 * - 書き込みは suspend 関数にする。Room が自動でバックグラウンドスレッドで実行する(main-safe)。
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE remote_id = :remoteId")
    suspend fun getByRemoteId(remoteId: String): NoteEntity?

    /**
     * 主キーが未採番(0)なら INSERT、既存なら UPDATE。
     * 戻り値は INSERT のときだけ採番された行 ID で、UPDATE のときは -1(Room の仕様)。
     * 「保存後の ID」が欲しい呼び出し側のために、この違いは Repository が吸収する。
     */
    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * サーバー由来のメモをまとめて取り込む。
     * 各メモの remoteId でローカル行を探し、あればその行を更新、なければ新規行として挿入する。
     *
     * `@Transaction` で全体を 1 つのトランザクションにしているので、
     * 取り込みの途中で別の書き込みが割り込んで照合結果が古くなることがない。
     * remote_id の UNIQUE インデックスは「万一重複したときの最後の安全網」で、照合の仕組みそのものではない。
     * (Kotlin インターフェースの default メソッドに @Transaction を付ける書き方は Room がサポートしている)
     */
    @Transaction
    suspend fun syncFromRemote(notes: List<NoteEntity>) {
        for (note in notes) {
            val local = note.remoteId?.let { getByRemoteId(it) }
            upsert(note.copy(id = local?.id ?: 0))
        }
    }
}
