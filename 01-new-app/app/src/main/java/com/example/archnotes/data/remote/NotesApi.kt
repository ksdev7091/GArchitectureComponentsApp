package com.example.archnotes.data.remote

import retrofit2.http.GET

/**
 * Retrofit の API 定義。HTTP のエンドポイントを Kotlin のインターフェースとして書く。
 *
 * - suspend 関数にすると Retrofit がバックグラウンドで実行し、結果を返す(main-safe)。
 * - 非 2xx の応答は `retrofit2.HttpException` として投げられる。
 * - JSON → [NetworkNote] の変換は Retrofit に登録した kotlinx.serialization のコンバータが行う。
 *
 * このクラスは「HTTP の形」だけを知っていて、エラーの変換や DB への保存は知らない。
 * それらは [RetrofitNoteRemoteDataSource] と Repository の仕事。
 */
interface NotesApi {

    /**
     * メモ一覧を取得する。
     * prod flavor は学習用に JSONPlaceholder(https://jsonplaceholder.typicode.com)の
     * `/posts` をメモ一覧として使っているため、パスが "posts" になっている。
     */
    @GET("posts")
    suspend fun getNotes(): List<NetworkNote>
}
