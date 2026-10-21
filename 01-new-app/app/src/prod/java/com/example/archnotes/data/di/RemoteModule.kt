package com.example.archnotes.data.di

import com.example.archnotes.data.remote.NoteRemoteDataSource
import com.example.archnotes.data.remote.RetrofitNoteRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * prod flavor のリモートデータソース: Retrofit で本物のサーバーに接続する。
 * (src/demo には同名のモジュールがあり、疑似実装を選んでいる)
 *
 * demo と prod で違うのはこのファイルの 1 行だけ。Repository / ViewModel / UI は共通。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {

    @Binds
    abstract fun bindNoteRemoteDataSource(impl: RetrofitNoteRemoteDataSource): NoteRemoteDataSource
}
