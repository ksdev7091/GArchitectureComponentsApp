package com.example.archnotes.data.di

import com.example.archnotes.data.NotesRepository
import com.example.archnotes.data.OfflineFirstNotesRepository
import com.example.archnotes.data.preferences.DataStoreUserPreferencesRepository
import com.example.archnotes.data.preferences.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 「インターフェース → 実装」の対応表。
 *
 * アプリ内のどこかで `NotesRepository` が要求されたら `OfflineFirstNotesRepository` を渡す、
 * という対応をここで一元的に宣言する。
 *
 * `NoteRemoteDataSource` の対応は flavor ごとに違うので、ここではなく
 * src/demo と src/prod にある `RemoteModule` が宣言している。
 * 実装を差し替えたいとき、変更はその 1 行だけで済み、Repository や ViewModel には手を入れない。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindNotesRepository(impl: OfflineFirstNotesRepository): NotesRepository

    @Binds
    abstract fun bindUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository
}
