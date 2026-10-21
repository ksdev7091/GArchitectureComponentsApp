package com.example.legacytodo.data

import com.example.legacytodo.model.Todo

/**
 * 【Step 1 で新設】Data 層の入口。
 *
 * UI 層から見た「Todo データの読み書き」はこのインターフェースがすべて。
 * - どこに保存されているか(SQLite? Room? サーバー?)は呼び出し側に見せない
 * - すべて suspend 関数。**どのスレッドで実行するかは実装側の責任**(main-safe)。
 *   呼び出し側はメインスレッドから気軽に呼んでよい。
 * - インターフェースにしておくと、後のステップで実装を Room に入れ替えたり、
 *   テスト用の偽物に差し替えたりできる。
 */
interface TodoRepository {
    suspend fun getTodos(): List<Todo>
    suspend fun addTodo(title: String)
    suspend fun setDone(id: Long, isDone: Boolean)
    suspend fun deleteTodo(id: Long)
}
