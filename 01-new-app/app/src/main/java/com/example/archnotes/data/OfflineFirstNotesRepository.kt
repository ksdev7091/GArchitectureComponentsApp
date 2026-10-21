package com.example.archnotes.data

import com.example.archnotes.data.di.IoDispatcher
import com.example.archnotes.data.local.NoteDao
import com.example.archnotes.data.local.NoteEntity
import com.example.archnotes.data.local.asEntity
import com.example.archnotes.data.local.asModel
import com.example.archnotes.data.remote.NoteRemoteDataSource
import com.example.archnotes.model.Note
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 「オフラインファースト」な Repository の実装。
 *
 * 考え方: **ローカル DB(Room)を唯一の信頼できる情報源(Single Source of Truth)にする。**
 * - UI が表示するのは常に Room の内容。サーバーの応答を直接 UI に流すことはしない。
 * - サーバーから取得したデータは一度 Room に書き込み、その変更が Flow を通じて UI に届く。
 * - こうすると「オフラインでも直前の内容が見える」「どこから見ても同じデータ」が自然に実現する。
 *
 * 依存するものはすべてコンストラクタで受け取る(コンストラクタインジェクション)。
 * `NoteDao` / `NoteRemoteDataSource` をインターフェースで受け取るので、
 * テストでは偽物に差し替えられる。
 */
@Singleton
class OfflineFirstNotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val remoteDataSource: NoteRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NotesRepository {

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeAll()
            .map { entities -> entities.map(NoteEntity::asModel) }
            .distinctUntilChanged()

    override fun observeNote(id: Long): Flow<Note?> =
        noteDao.observeById(id).map { it?.asModel() }

    // Room の suspend 関数は内部で適切なスレッドに切り替えてくれる(main-safe)ので、
    // ここで withContext を書く必要はない。
    override suspend fun saveNote(note: Note): Long = noteDao.upsert(note.asEntity())

    override suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

    // ネットワーク応答の変換のように CPU/IO を使う処理は、Repository が責任を持って
    // バックグラウンドに逃がす。呼び出し側(ViewModel)はメインスレッドから安全に呼べる。
    override suspend fun refresh() = withContext(ioDispatcher) {
        val remoteNotes = remoteDataSource.fetchNotes()
        noteDao.upsertAll(
            remoteNotes.map { network ->
                NoteEntity(
                    id = network.id,
                    title = network.title,
                    body = network.body,
                    updatedAt = network.updatedAt,
                )
            },
        )
    }
}
