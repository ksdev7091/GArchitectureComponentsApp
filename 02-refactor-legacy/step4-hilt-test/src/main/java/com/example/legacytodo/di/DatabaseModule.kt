package com.example.legacytodo.di

import android.content.Context
import androidx.room.Room
import com.example.legacytodo.data.local.TodoDao
import com.example.legacytodo.data.local.TodoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 【Step 4 で新設】Room のようにビルダーで作るものは `@Provides` で作り方を教える。
 * Step 3 の AppContainer の `Room.databaseBuilder(...)` がそのまま移ってきた。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTodoDatabase(@ApplicationContext context: Context): TodoDatabase =
        Room.databaseBuilder(context, TodoDatabase::class.java, "todo_room.db").build()

    @Provides
    fun provideTodoDao(database: TodoDatabase): TodoDao = database.todoDao()
}
