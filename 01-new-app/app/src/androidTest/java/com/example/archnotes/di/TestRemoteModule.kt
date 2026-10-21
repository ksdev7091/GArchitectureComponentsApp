package com.example.archnotes.di

import com.example.archnotes.data.di.RemoteModule
import com.example.archnotes.data.remote.NetworkNote
import com.example.archnotes.data.remote.NoteRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * テストだけで使うリモートデータソース。遅延も失敗もなく、決まった 1 件を返す。
 */
@Singleton
class TestNoteRemoteDataSource @Inject constructor() : NoteRemoteDataSource {
    override suspend fun fetchNotes(): List<NetworkNote> =
        listOf(NetworkNote(id = 99, title = "テスト用のサーバーメモ", body = "TestRemoteModule から", updatedAt = 1))
}

/**
 * 本番の `RemoteModule`(demo なら Fake、prod なら Retrofit)を、テスト実行中だけこのモジュールに置き換える。
 *
 * `replaces` に指定したモジュールは Hilt のグラフから外れ、代わりにここで宣言した対応表が使われる。
 * 本番コードは 1 文字も変えずに、「ネットワークの向こう側」をテストの都合で差し替えられる。
 * これが「依存をインターフェースで受け取り、組み立てを DI に任せる」設計の最終的な見返り。
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RemoteModule::class],
)
abstract class TestRemoteModule {

    @Binds
    abstract fun bindNoteRemoteDataSource(impl: TestNoteRemoteDataSource): NoteRemoteDataSource
}
