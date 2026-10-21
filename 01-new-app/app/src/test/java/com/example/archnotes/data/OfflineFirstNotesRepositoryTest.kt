package com.example.archnotes.data

import com.example.archnotes.data.remote.NetworkNote
import com.example.archnotes.data.remote.NoteRemoteDataSource
import com.example.archnotes.model.Note
import com.example.archnotes.testing.FakeNoteDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Repository の単体テスト。DAO とリモートを Fake にすることで JVM だけで動く。
 *
 * ここで守りたい契約:
 * - サーバーのメモは remoteId で同一判定し、2 回同期しても重複しない
 * - ローカルで作ったメモは同期で消えたり上書きされたりしない
 * - saveNote は新規なら採番された ID、更新なら渡した ID を返し、remoteId を失わない
 */
class OfflineFirstNotesRepositoryTest {

    private val dao = FakeNoteDao()
    private var remoteNotes: List<NetworkNote> = emptyList()
    private val remote = object : NoteRemoteDataSource {
        override suspend fun fetchNotes(): List<NetworkNote> = remoteNotes
    }
    private val repository = OfflineFirstNotesRepository(dao, remote)

    @Test
    fun `同期するとサーバーのメモが取り込まれ、再同期しても重複しない`() = runTest {
        remoteNotes = listOf(NetworkNote(id = "srv-1", title = "v1", body = "", updatedAt = 10))

        repository.refresh()
        remoteNotes = listOf(NetworkNote(id = "srv-1", title = "v2", body = "", updatedAt = 20))
        repository.refresh()

        val notes = repository.observeNotes().first()
        assertEquals(1, notes.size)
        assertEquals("v2", notes.single().title) // 同じ remoteId の行が更新された
        assertEquals("srv-1", dao.currentRows.single().remoteId)
    }

    @Test
    fun `応答に同じサーバー ID が重複していても 1 件として取り込む`() = runTest {
        remoteNotes = listOf(
            NetworkNote(id = "srv-1", title = "先", body = "", updatedAt = 10),
            NetworkNote(id = "srv-1", title = "後", body = "", updatedAt = 20),
        )

        repository.refresh()

        assertEquals(1, dao.currentRows.size)
        assertEquals("先", dao.currentRows.single().title) // distinctBy は最初の 1 件を採用する
    }

    @Test
    fun `同期はローカルで作ったメモに影響しない`() = runTest {
        val localId = repository.saveNote(Note(id = Note.NEW_NOTE_ID, title = "ローカル", body = "", updatedAt = 5))
        remoteNotes = listOf(NetworkNote(id = "srv-1", title = "サーバー", body = "", updatedAt = 10))

        repository.refresh()

        val notes = repository.observeNotes().first()
        assertEquals(listOf("サーバー", "ローカル"), notes.map { it.title })
        assertEquals("ローカル", repository.observeNote(localId).first()?.title)
    }

    @Test
    fun `saveNote は新規なら採番した ID、更新なら同じ ID を返す`() = runTest {
        val id = repository.saveNote(Note(id = Note.NEW_NOTE_ID, title = "a", body = "", updatedAt = 1))
        assertEquals(1L, id)

        val updatedId = repository.saveNote(Note(id = id, title = "b", body = "", updatedAt = 2))

        assertEquals(id, updatedId) // DAO の upsert は UPDATE 時に -1 を返すが、Repository が吸収する
        assertEquals("b", repository.observeNote(id).first()?.title)
        assertEquals(1, dao.currentRows.size)
    }

    @Test
    fun `サーバー由来のメモを編集しても remoteId は保たれる`() = runTest {
        remoteNotes = listOf(NetworkNote(id = "srv-1", title = "元", body = "", updatedAt = 10))
        repository.refresh()
        val note = repository.observeNotes().first().single()

        repository.saveNote(note.copy(title = "編集後"))

        val row = dao.currentRows.single()
        assertEquals("編集後", row.title)
        assertEquals("srv-1", row.remoteId) // Note は remoteId を持たないが、Repository が引き継ぐ
    }

    @Test
    fun `削除したメモは observeNote で null になる`() = runTest {
        val id = repository.saveNote(Note(id = Note.NEW_NOTE_ID, title = "x", body = "", updatedAt = 1))

        repository.deleteNote(id)

        assertNull(repository.observeNote(id).first())
    }
}
