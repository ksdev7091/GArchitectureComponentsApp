package com.example.archnotes.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * `Dispatchers.IO` を直接書かずに注入する理由:
 * テストでは `StandardTestDispatcher` などに差し替えたい。
 * ハードコードすると、テストで時間の進み方を制御できなくなる。
 *
 * 同じ型(CoroutineDispatcher)を複数登録するので、Qualifier で区別する。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
