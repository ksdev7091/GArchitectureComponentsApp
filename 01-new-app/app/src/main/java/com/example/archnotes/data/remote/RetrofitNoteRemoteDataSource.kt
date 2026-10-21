package com.example.archnotes.data.remote

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本物のネットワークを使うリモートデータソース(prod flavor)。
 *
 * ここでやっているのは「実装固有の例外を契約の例外に変換する」ことだけ。
 * - 接続失敗・タイムアウトは OkHttp がもともと IOException を投げるのでそのまま。
 * - 4xx/5xx は Retrofit の [HttpException](RuntimeException)。→ IOException に変換。
 * - 応答の JSON が [NetworkNote] の形と違えば [SerializationException]。→ IOException に変換。
 *   (サーバー側の仕様変更などで起きる。アプリをクラッシュさせず「同期に失敗」として扱う)
 *
 * この変換を DataSource の中で済ませるので、Repository と ViewModel は
 * [FakeNoteRemoteDataSource] のときと同じ catch 節のまま動く。
 */
@Singleton
class RetrofitNoteRemoteDataSource @Inject constructor(
    private val api: NotesApi,
) : NoteRemoteDataSource {

    override suspend fun fetchNotes(): List<NetworkNote> =
        try {
            api.getNotes()
        } catch (e: HttpException) {
            throw IOException("HTTP ${e.code()} ${e.message()}", e)
        } catch (e: SerializationException) {
            throw IOException("サーバーの応答を解釈できません", e)
        }
}
