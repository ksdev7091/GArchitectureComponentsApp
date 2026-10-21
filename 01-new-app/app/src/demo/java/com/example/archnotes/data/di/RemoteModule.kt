package com.example.archnotes.data.di

import com.example.archnotes.data.remote.FakeNoteRemoteDataSource
import com.example.archnotes.data.remote.NoteRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * demo flavor のリモートデータソース: ネットワークに出ない疑似実装。
 * (src/prod には同名のモジュールがあり、Retrofit 実装を選んでいる)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {

    @Binds
    abstract fun bindNoteRemoteDataSource(impl: FakeNoteRemoteDataSource): NoteRemoteDataSource
}
