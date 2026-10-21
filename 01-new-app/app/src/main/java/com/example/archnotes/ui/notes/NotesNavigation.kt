package com.example.archnotes.ui.notes

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * 一覧画面の「住所」。Navigation Compose の型安全ルート機能を使う。
 * 文字列 "notes" のような組み立てをしなくて済み、引数の型ミスがコンパイル時に分かる。
 */
@Serializable
object NotesRoute

/**
 * 一覧画面を NavGraph に登録する。遷移先の決定は呼び出し側(NavHost)に委ねる。
 *
 * 連打対策: 遷移を要求するのは、この画面のエントリが RESUMED のときだけ。
 * 1 回目のタップで遷移が始まると、この画面は RESUMED でなくなるので、2 回目のタップは無視される
 * (公式サンプル JetNews / Jetsnack と同じやり方)。引数が違っても正しく弾ける。
 */
fun NavGraphBuilder.notesScreen(
    onNoteClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    composable<NotesRoute> { entry ->
        fun isResumed() = entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        NotesScreen(
            onNoteClick = { noteId -> if (isResumed()) onNoteClick(noteId) },
            onAddClick = { if (isResumed()) onAddClick() },
            onSettingsClick = { if (isResumed()) onSettingsClick() },
        )
    }
}
