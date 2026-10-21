package com.example.archnotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.archnotes.model.Note

/**
 * Room のテーブル定義。DB の都合(テーブル名、カラム名)はこのファイルに閉じ込める。
 *
 * UI 層や Domain 層は [Note] しか知らない。もし DB のカラム名を変えたくなっても、
 * 変更はこのファイルと変換関数だけで済む。
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

/** DB の行 → アプリのモデル */
fun NoteEntity.asModel(): Note = Note(
    id = id,
    title = title,
    body = body,
    updatedAt = updatedAt,
)

/** アプリのモデル → DB の行 */
fun Note.asEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    body = body,
    updatedAt = updatedAt,
)
