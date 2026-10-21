package com.example.archnotes.data.di

import android.content.Context
import androidx.room.Room
import com.example.archnotes.data.local.NoteDao
import com.example.archnotes.data.local.NotesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 自分で `@Inject constructor` を付けられないもの(ライブラリのクラス、ビルダーで作るもの)は
 * `@Provides` で「作り方」を教える。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNotesDatabase(@ApplicationContext context: Context): NotesDatabase =
        Room.databaseBuilder(context, NotesDatabase::class.java, "arch_notes.db").build()

    @Provides
    fun provideNoteDao(database: NotesDatabase): NoteDao = database.noteDao()
}
