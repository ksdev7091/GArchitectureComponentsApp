package com.example.archnotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room データベース本体。インスタンスの生成は DI モジュール
 * ([com.example.archnotes.data.di.DatabaseModule])が担当する。
 *
 * 実運用では exportSchema = true にしてスキーマを Git 管理し、
 * version を上げるときに Migration を書く。学習用なので省略している。
 */
@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
