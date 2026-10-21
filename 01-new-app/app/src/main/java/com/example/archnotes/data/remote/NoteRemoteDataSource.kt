package com.example.archnotes.data.remote

/**
 * サーバーから返ってくるメモの形(DTO)。
 * 実際のアプリでは Retrofit + kotlinx.serialization などで JSON から変換される。
 * サーバーの都合(フィールド名、型、サーバー側 ID)はこのクラスに閉じ込め、
 * [com.example.archnotes.model.Note] とは分ける。
 */
data class NetworkNote(
    /** サーバー側の ID。ローカル DB の ID とは別物。 */
    val id: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

/**
 * リモートデータソースの抽象。
 *
 * データソースは「1 つの出どころ(API、DB、ファイルなど)を扱うことだけ」に責任を持つ。
 * 複数のデータソースをどう組み合わせるか(キャッシュ戦略など)は Repository の仕事。
 *
 * **失敗の契約**: 通信に失敗したら [java.io.IOException] を投げる。
 * Retrofit なら HTTP エラーは `HttpException`(RuntimeException)で返ってくるので、
 * 本物の実装ではこのクラスの中で IOException に変換する。
 * 「どんな例外が飛んでくるか」を実装ごとに変えないことで、上の層(Repository / ViewModel)は
 * 実装を差し替えても catch 節を変えずに済む。
 */
interface NoteRemoteDataSource {
    /**
     * サーバー上の全メモを取得する。main-safe(メインスレッドから呼んでよい)。
     * @throws java.io.IOException 通信失敗時
     */
    suspend fun fetchNotes(): List<NetworkNote>
}
