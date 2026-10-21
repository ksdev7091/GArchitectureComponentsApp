package com.example.archnotes.data.di

import com.example.archnotes.BuildConfig
import com.example.archnotes.data.remote.NotesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

/**
 * ネットワーク関連の「作り方」。
 *
 * - [Json]: 未知のキーを無視する設定にしておく。サーバーがフィールドを増やしてもアプリが壊れない。
 * - [OkHttpClient]: タイムアウトやログなど、HTTP 全体に関わる設定はここ 1 か所。
 * - [NotesApi]: Retrofit がインターフェースから実装を生成する。
 *
 * ベース URL は flavor ごとの BuildConfig から取る(demo では使われない)。
 * 単体テストでは MockWebServer の URL で同じ組み立てをやり直す
 * (→ src/test の RetrofitNoteRemoteDataSourceTest)。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // デバッグビルドだけリクエスト/レスポンスの要約を Logcat に出す
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()

    @Provides
    @Singleton
    fun provideNotesApi(json: Json, client: OkHttpClient): NotesApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NotesApi::class.java)
}
