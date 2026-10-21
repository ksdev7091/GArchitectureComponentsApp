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
 * `launchSingleTop` は、行の連打で編集画面が 2 枚積まれるのを防ぐ。
 */
fun NavController.navigateToNoteEditor(noteId: Long = Note.NEW_NOTE_ID) {
    navigate(NoteEditorRoute(noteId)) { launchSingleTop = true }
}

/** 編集画面を NavGraph に登録する。 */
fun NavGraphBuilder.noteEditorScreen(onDone: () -> Unit) {
    composable<NoteEditorRoute> {
        NoteEditorScreen(onDone = onDone)
    }
}
