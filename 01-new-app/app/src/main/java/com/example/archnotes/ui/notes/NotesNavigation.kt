package com.example.archnotes.ui.notes

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * 一覧画面の「住所」。Navigation Compose の型安全ルート機能を使う。
 * 文字列 "notes" のような組み立てをしなくて済み、引数の型ミスがコンパイル時に分かる。
 */
@Serializable
object NotesRoute

/** 一覧画面を NavGraph に登録する。遷移先の決定は呼び出し側(NavHost)に委ねる。 */
fun NavGraphBuilder.notesScreen(
    onNoteClick: (Long) -> Unit,
    onAddClick: () -> Unit,
) {
    composable<NotesRoute> {
        NotesScreen(
            onNoteClick = onNoteClick,
            onAddClick = onAddClick,
        )
    }
}
