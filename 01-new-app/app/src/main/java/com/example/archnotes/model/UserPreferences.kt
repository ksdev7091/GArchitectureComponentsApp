package com.example.archnotes.model

/** 一覧の並び順。 */
enum class SortOrder {
    /** 更新日時の新しい順(既定。DAO の SQL がこの順で返す) */
    UPDATED_DESC,

    /** タイトルの昇順 */
    TITLE_ASC,
}

/**
 * ユーザーが変更できる設定。DataStore に保存され、アプリ全体で共有される。
 *
 * [Note] と同じく、DataStore のキー名や保存形式(文字列)はここに漏らさない。
 * UI 層と Domain 層はこの不変オブジェクトだけを見る。
 */
data class UserPreferences(
    val sortOrder: SortOrder = SortOrder.UPDATED_DESC,
    /** 一覧に本文の先頭を表示するか */
    val showBodyPreview: Boolean = true,
)
