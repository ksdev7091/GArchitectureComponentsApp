package com.example.archnotes.data

import com.example.archnotes.data.local.NoteDao
import com.example.archnotes.data.local.NoteEntity
import com.example.archnotes.data.local.asEntity
import com.example.archnotes.data.local.asModel
import com.example.archnotes.data.remote.NoteRemoteDataSource
import com.example.archnotes.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
 * **スレッドについて**: Room の suspend 関数 / Flow も、[NoteRemoteDataSource] の契約も main-safe
 * なので、この Repository に `withContext(Dispatchers.IO)` は不要。
 * ブロッキングな API(旧来の HTTP クライアントなど)を使う場合は、それを包む DataSource の中で
 * `withContext` を書き、Repository より上には漏らさない。
 *
 * 依存するものはすべてコンストラクタで受け取る(コンストラクタインジェクション)。
 * `NoteDao` / `NoteRemoteDataSource` はインターフェースなので、テストでは偽物に差し替えられる
 * (→ src/test の OfflineFirstNotesRepositoryTest)。
 */
@Singleton
class OfflineFirstNotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val remoteDataSource: NoteRemoteDataSource,
) : NotesRepository {

    // Room の Flow はテーブルのどの行が変わっても再発行するので、
    // 中身が同じなら流さない distinctUntilChanged を挟んで UI の無駄な再描画を避ける
    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeAll()
            .map { entities -> entities.map(NoteEntity::asModel) }
            .distinctUntilChanged()

    override fun observeNote(id: Long): Flow<Note?> =
        noteDao.observeById(id)
            .map { it?.asModel() }
            .distinctUntilChanged()

    override suspend fun saveNote(note: Note): Long {
        // モデルはサーバー側 ID を持たないので、既存行の remoteId を引き継ぐ
        val existing = if (note.id == Note.NEW_NOTE_ID) null else noteDao.getById(note.id)
        val rowId = noteDao.upsert(note.asEntity().copy(remoteId = existing?.remoteId))
        // upsert の戻り値は INSERT 時のみ行 ID(UPDATE 時は -1)なので、ここで契約をそろえる
        return if (note.id == Note.NEW_NOTE_ID) rowId else note.id
    }

    override suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

    override suspend fun refresh() {
        val remoteNotes = remoteDataSource.fetchNotes()
        val entities = remoteNotes.map { network ->
            // サーバー ID でローカルの行を探し、あればその行を更新、なければ新規行として挿入する
            val local = noteDao.getByRemoteId(network.id)
            NoteEntity(
                id = local?.id ?: 0,
                remoteId = network.id,
                title = network.title,
                body = network.body,
                updatedAt = network.updatedAt,
            )
        }
        noteDao.upsertAll(entities)
    }
}
