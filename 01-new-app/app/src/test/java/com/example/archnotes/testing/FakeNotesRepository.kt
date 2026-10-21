package com.example.archnotes.testing

import com.example.archnotes.data.NotesRepository
import com.example.archnotes.model.Note
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

/**
 * テスト用の Repository。本物(Room + ネットワーク)の代わりにメモリ上のリストで動く。
 *
 * モックライブラリではなく「Fake(偽物の本物らしい実装)」を使うのが Google の推奨。
 * - テストが実装の呼び出し順などに縛られず、振る舞いだけを検証できる
 * - 複数のテストで同じ Fake を使い回せる
 *
 * これが可能なのは [NotesRepository] がインターフェースだから。
 *
 * [refreshGate] / [saveGate] に [CompletableDeferred] を入れると、その処理が完了を待つようになる。
 * 「同期中」「保存中」のような途中の状態をテストから観察するための仕組み。
 */
class FakeNotesRepository(
    initialNotes: List<Note> = emptyList(),
) : NotesRepository {

    private val notes = MutableStateFlow(initialNotes)

    /** true にすると [refresh] が通信エラーを投げる。 */
    var shouldFailRefresh = false

    /** セットすると [refresh] は完了する前にこれを待つ。 */
    var refreshGate: CompletableDeferred<Unit>? = null

    /** セットすると [saveNote] は書き込む前にこれを待つ。 */
    var saveGate: CompletableDeferred<Unit>? = null

    var refreshCount = 0
        private set

    val currentNotes: List<Note> get() = notes.value

    // 並び順は本物では DAO の SQL が決める。ここでは「渡した順」をそのまま返す
    override fun observeNotes(): Flow<List<Note>> = notes

    override fun observeNote(id: Long): Flow<Note?> =
        notes.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun saveNote(note: Note): Long {
        saveGate?.await()
        val id = if (note.id == Note.NEW_NOTE_ID) {
            (notes.value.maxOfOrNull { it.id } ?: 0L) + 1
        } else {
            note.id
        }
        notes.update { list -> list.filterNot { it.id == id } + note.copy(id = id) }
        return id
    }

    override suspend fun deleteNote(id: Long) {
        notes.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun refresh() {
        refreshCount++
        refreshGate?.await()
        if (shouldFailRefresh) throw IOException("fake network error")
        notes.update { list ->
            list + Note(id = 1_000_001, title = "remote", body = "from server", updatedAt = Long.MAX_VALUE)
        }
    }
}
