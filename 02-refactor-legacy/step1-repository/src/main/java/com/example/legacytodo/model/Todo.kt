package com.example.legacytodo.model

/**
 * アプリ全体で使う Todo のモデル。
 * Step 1 で `model` パッケージに移動した。DB の行の形(カラム名など)とは独立した
 * 「アプリにとっての Todo」を表す。
 */
data class Todo(
    val id: Long,
    val title: String,
    val isDone: Boolean,
    val createdAt: Long,
)
