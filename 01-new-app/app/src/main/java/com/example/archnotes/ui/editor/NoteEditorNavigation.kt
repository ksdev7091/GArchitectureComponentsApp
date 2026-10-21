package com.example.archnotes.ui.editor

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.archnotes.model.Note
import kotlinx.serialization.Serializable

/** 編集画面の「住所」。引数 noteId を省略すると新規作成。 */
@Serializable
data class NoteEditorRoute(val noteId: Long = Note.NEW_NOTE_ID)

/**
 * 編集画面へ遷移する。
 *
 * 連打対策はここではなく、遷移元の画面(NotesNavigation.kt)で「自分が RESUMED のときだけ遷移する」
 * と判定している。`launchSingleTop` は同じ引数の重複しか防げず、引数が違う場合は
 * 既存のエントリ(と ViewModel)を残したまま引数だけ差し替えてしまうため、ここでは使わない。
 */
fun NavController.navigateToNoteEditor(noteId: Long = Note.NEW_NOTE_ID) {
    navigate(NoteEditorRoute(noteId))
}

/** 編集画面を NavGraph に登録する。 */
fun NavGraphBuilder.noteEditorScreen(onDone: () -> Unit) {
    composable<NoteEditorRoute> {
        NoteEditorScreen(onDone = onDone)
    }
}
