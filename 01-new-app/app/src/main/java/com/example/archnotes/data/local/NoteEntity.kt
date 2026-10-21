package com.example.archnotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.archnotes.model.Note

/**
 * Room のテーブル定義。DB の都合(テーブル名、カラム名、サーバー側 ID)はこのファイルに閉じ込める。
 *
 * **ID を 2 種類持つ理由**:
 * - [id] はローカル DB が自動採番する主キー。UI 層が扱うのはこちらだけ。
 * - [remoteId] はサーバーが付けた ID。サーバーのメモを取り込むときに「同じメモか」を判定するのに使う。
 * 1 つの ID 列にサーバーの ID とローカルの自動採番を混ぜると、いつか必ず衝突して
 * ユーザーのメモが黙って上書きされる。オフラインファーストでは ID を分けるのが定石。
 *
 * UI 層や Domain 層は [Note] しか知らない。DB のカラム名を変えても、
 * 変更はこのファイルと変換関数だけで済む。
 */
@Entity(
    tableName = "notes",
    indices = [Index(value = ["remote_id"], unique = true)],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** サーバー側の ID。ローカルで作ったメモは null。 */
    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,
    val title: String,
    val body: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

/** DB の行 → アプリのモデル(サーバー側 ID は UI に見せない) */
fun NoteEntity.asModel(): Note = Note(
    id = id,
    title = title,
    body = body,
    updatedAt = updatedAt,
)

/**
 * アプリのモデル → DB の行。
 * モデルは remoteId を持たないので null になる。既存行の remoteId を保つのは Repository の責任。
 */
fun Note.asEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    body = body,
    updatedAt = updatedAt,
)
