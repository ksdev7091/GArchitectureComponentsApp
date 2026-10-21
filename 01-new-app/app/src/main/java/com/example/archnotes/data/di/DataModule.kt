package com.example.archnotes.data.di

import com.example.archnotes.data.NotesRepository
import com.example.archnotes.data.OfflineFirstNotesRepository
import com.example.archnotes.data.remote.FakeNoteRemoteDataSource
import com.example.archnotes.data.remote.NoteRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 「インターフェース → 実装」の対応表。
 *
 * アプリ内のどこかで `NotesRepository` が要求されたら `OfflineFirstNotesRepository` を渡す、
 * という対応をここで一元的に宣言する。
 * 実装を差し替えたいとき(例: 本物の API クライアントに切り替える)は、
 * この対応表を 1 行書き換えるだけでアプリ全体に反映される。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindNotesRepository(impl: OfflineFirstNotesRepository): NotesRepository

    @Binds
    abstract fun bindNoteRemoteDataSource(impl: FakeNoteRemoteDataSource): NoteRemoteDataSource
}
