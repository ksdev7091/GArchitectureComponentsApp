package com.example.archnotes.model

/**
 * アプリ全体で使う「メモ」のモデル。
 *
 * - Room の Entity([com.example.archnotes.data.local.NoteEntity]) や
 *   ネットワークの DTO([com.example.archnotes.data.remote.NetworkNote]) とは別に定義する。
 *   DB やサーバーの都合(カラム名、JSON のキー、サーバー側 ID など)を UI 層に漏らさないため。
 * - 不変(val のみ)にする。状態の変更は必ず新しいインスタンスを作って行うので、
 *   「いつの間にか書き換わっていた」というバグが起きない。
 */
data class Note(
    /** ローカル DB の ID。未保存なら [NEW_NOTE_ID]。 */
    val id: Long,
    val title: String,
    val body: String,
    /** 最終更新日時(エポックミリ秒) */
    val updatedAt: Long,
) {
    companion object {
        /** まだ保存されていないメモの ID。Room の autoGenerate は 0 を「未採番」として扱う。 */
        const val NEW_NOTE_ID = 0L
    }
}
