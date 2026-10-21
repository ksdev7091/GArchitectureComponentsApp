package com.example.archnotes.testing

import com.example.archnotes.data.local.NoteDao
import com.example.archnotes.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Room の DAO の Fake。Repository を JVM だけでテストするために使う。
 *
 * 本物の挙動のうち、Repository の正しさに関わる部分だけを真似ている:
 * - id が 0 なら採番して INSERT、それ以外は同じ id の行を UPDATE
 * - [upsert] の戻り値は INSERT 時のみ行 ID、UPDATE 時は -1(Room の仕様)
 * - 並び順は updated_at の降順
 */
class FakeNoteDao : NoteDao {

    private val rows = MutableStateFlow<List<NoteEntity>>(emptyList())
    private var nextId = 1L

    val currentRows: List<NoteEntity> get() = rows.value

    override fun observeAll(): Flow<List<NoteEntity>> =
        rows.map { list -> list.sortedByDescending { it.updatedAt } }

    override fun observeById(id: Long): Flow<NoteEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): NoteEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun getByRemoteId(remoteId: String): NoteEntity? =
        rows.value.firstOrNull { it.remoteId == remoteId }

    override suspend fun upsert(note: NoteEntity): Long {
        val isInsert = note.id == 0L
        val stored = if (isInsert) note.copy(id = nextId++) else note
        rows.update { list -> list.filterNot { it.id == stored.id } + stored }
        return if (isInsert) stored.id else -1L
    }

    override suspend fun upsertAll(notes: List<NoteEntity>) {
        notes.forEach { upsert(it) }
    }

    override suspend fun deleteById(id: Long) {
        rows.update { list -> list.filterNot { it.id == id } }
    }
}
