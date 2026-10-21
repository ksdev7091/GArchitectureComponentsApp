package com.example.archnotes.data

import com.example.archnotes.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Data 層の公開 API。アプリの他の層は、このインターフェースだけを通じてメモを読み書きする。
 *
 * 設計のポイント:
 * - **読み取りは Flow**: データが変わったら自動的に新しいリストが流れてくる。
 *   UI 側は「いつ再取得するか」を考えなくてよい。
 * - **書き込みは suspend**: 呼び出し側はコルーチンから呼ぶだけ。スレッドの切り替えは
 *   Repository の責任(main-safe)なので、UI 層は Dispatchers を意識しない。
 * - **インターフェースにする理由**: テストでは [NotesRepository] の偽物(Fake)を差し込める。
 *   また、実装が Room から別の仕組みに変わっても UI 層は影響を受けない。
 * - **データの出どころを隠す**: ローカル DB とサーバーのどちらから来たかは呼び出し側に見せない。
 */
interface NotesRepository {

    /** すべてのメモを更新日時の新しい順で監視する。 */
    fun observeNotes(): Flow<List<Note>>

    /** 指定 ID のメモを監視する。存在しなければ null。 */
    fun observeNote(id: Long): Flow<Note?>

    /**
     * メモを保存する。[Note.id] が [Note.NEW_NOTE_ID] なら新規作成、それ以外は更新。
     * @return 保存後の ID(新規なら採番された ID、更新なら渡した ID)
     */
    suspend fun saveNote(note: Note): Long

    suspend fun deleteNote(id: Long)

    /**
     * サーバーから最新のメモを取得してローカル DB に取り込む。
     * 同じサーバー ID のメモが既にあれば上書きする(「サーバーが正」の単純な同期)。
     * 結果は [observeNotes] を通じて自動的に UI に届くので、戻り値は持たない。
     * @throws java.io.IOException 通信に失敗した場合
     */
    suspend fun refresh()
}
