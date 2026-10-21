package com.example.archnotes.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * サーバーから返ってくるメモの形(DTO)。JSON と 1 対 1 に対応する。
 *
 * - `@SerialName` で JSON のキー名(snake_case)とプロパティ名(camelCase)の違いを吸収する。
 * - サーバーが返さないことがあるフィールドは nullable + デフォルト値にしておく
 *   (prod flavor が使う JSONPlaceholder は updated_at を返さない)。
 * - 未知のキー(JSONPlaceholder の userId など)は Json の ignoreUnknownKeys で無視する。
 *
 * サーバーの都合はこのクラスに閉じ込め、[com.example.archnotes.model.Note] とは分ける。
 */
@Serializable
data class NetworkNote(
    /** サーバー側の ID。ローカル DB の ID とは別物(Repository が文字列化して remote_id に保存する)。 */
    val id: Long,
    val title: String,
    val body: String,
    @SerialName("updated_at")
    val updatedAt: Long? = null,
)

/**
 * リモートデータソースの抽象。
 *
 * データソースは「1 つの出どころ(API、DB、ファイルなど)を扱うことだけ」に責任を持つ。
 * 複数のデータソースをどう組み合わせるか(キャッシュ戦略など)は Repository の仕事。
 *
 * **失敗の契約**: 通信に失敗したら [java.io.IOException] を投げる。
 * Retrofit なら HTTP エラーは `HttpException`(RuntimeException)で返ってくるので、
 * [RetrofitNoteRemoteDataSource] の中で IOException に変換している。
 * 「どんな例外が飛んでくるか」を実装ごとに変えないことで、上の層(Repository / ViewModel)は
 * 実装を差し替えても catch 節を変えずに済む。
 *
 * 実装は 2 つ:
 * - [FakeNoteRemoteDataSource]: demo flavor。ネットワークに出ない
 * - [RetrofitNoteRemoteDataSource]: prod flavor。本物の HTTP
 * どちらを使うかは flavor ごとの Hilt モジュール(src/demo, src/prod の RemoteModule)が決める。
 */
interface NoteRemoteDataSource {
    /**
     * サーバー上の全メモを取得する。main-safe(メインスレッドから呼んでよい)。
     * @throws java.io.IOException 通信失敗時
     */
    suspend fun fetchNotes(): List<NetworkNote>
}
