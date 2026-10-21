package com.example.legacytodo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 昔ながらの SQLiteOpenHelper。テーブル定義と SQL 文字列を手で管理する。
 * (SQL の列名のタイプミスは実行時までわからない)
 */
class TodoDbHelper(context: Context) : SQLiteOpenHelper(context, "todo.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE todos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                is_done INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS todos")
        onCreate(db)
    }
}
