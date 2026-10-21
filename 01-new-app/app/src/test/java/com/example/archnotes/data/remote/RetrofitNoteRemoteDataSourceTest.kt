package com.example.archnotes.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

/**
 * Retrofit 実装の単体テスト。MockWebServer がローカルで本物の HTTP サーバーとして振る舞うので、
 * JSON の解釈からエラーの変換まで、ネットワーク層の責任を端末なしで検証できる。
 *
 * 検証したい契約:
 * - サーバーの JSON(snake_case、未知のキー、省略されたフィールド)を [NetworkNote] に変換できる
 * - 失敗はすべて IOException として報告する(HTTP エラー、壊れた JSON、接続失敗)
 */
class RetrofitNoteRemoteDataSourceTest {

    private val server = MockWebServer()
    private lateinit var dataSource: RetrofitNoteRemoteDataSource

    @Before
    fun setUp() {
        server.start()
        // NetworkModule と同じ組み立てを、ベース URL だけ MockWebServer に向けて行う
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NotesApi::class.java)
        dataSource = RetrofitNoteRemoteDataSource(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `JSON を DTO に変換する(未知のキーは無視、updated_at は省略可)`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  {"userId": 1, "id": 1, "title": "first", "body": "b1"},
                  {"id": 2, "title": "second", "body": "b2", "updated_at": 1700000000000}
                ]
                """.trimIndent(),
            ),
        )

        val notes = dataSource.fetchNotes()

        assertEquals(listOf(1L, 2L), notes.map { it.id })
        assertNull(notes[0].updatedAt)
        assertEquals(1_700_000_000_000L, notes[1].updatedAt)
        assertEquals("/posts", server.takeRequest().path) // NotesApi の @GET と一致
    }

    @Test
    fun `HTTP エラー(5xx)は IOException に変換される`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        assertFetchFailsWithIOException()
    }

    @Test
    fun `壊れた JSON は IOException に変換される`() = runTest {
        server.enqueue(MockResponse().setBody("this is not json"))

        assertFetchFailsWithIOException()
    }

    @Test
    fun `接続できないときは IOException になる`() = runTest {
        server.shutdown() // サーバーを落としてから呼ぶ

        assertFetchFailsWithIOException()
    }

    private suspend fun assertFetchFailsWithIOException() {
        try {
            dataSource.fetchNotes()
            fail("IOException が投げられるはず")
        } catch (e: IOException) {
            // 期待どおり: ViewModel はこの型だけを catch すればよい
        }
    }
}
