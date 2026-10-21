package com.example.legacytodo.di

import com.example.legacytodo.data.RoomTodoRepository
import com.example.legacytodo.data.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindTodoRepository(impl: RoomTodoRepository): TodoRepository
}
