package com.example.legacytodo.di

import com.example.legacytodo.data.RoomTodoRepository
import com.example.legacytodo.data.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 【Step 4 で新設】Step 2〜3 の AppContainer にあった
 * 「TodoRepository を要求されたら RoomTodoRepository を渡す」をアノテーションで宣言する。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindTodoRepository(impl: RoomTodoRepository): TodoRepository
}
