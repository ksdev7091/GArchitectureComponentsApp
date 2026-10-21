package com.example.archnotes.data.remote

/**
 * サーバーから返ってくるメモの形(DTO)。
 * 実際のアプリでは Retrofit + kotlinx.serialization などで JSON から変換される。
 * サーバーの都合(フィールド名、型)はこのクラスに閉じ込め、[com.example.archnotes.model.Note] とは分ける。
 */
data class NetworkNote(
    val id: Long,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

/**
 * リモートデータソースの抽象。
 *
 * データソースは「1 つの出どころ(API、DB、ファイルなど)を扱うことだけ」に責任を持つ。
 * 複数のデータソースをどう組み合わせるか(キャッシュ戦略など)は Repository の仕事。
 */
interface NoteRemoteDataSource {
    /**
     * サーバー上の全メモを取得する。
     * @throws java.io.IOException 通信失敗時
     */
    suspend fun fetchNotes(): List<NetworkNote>
}
