package com.example.archnotes.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.archnotes.ui.editor.navigateToNoteEditor
import com.example.archnotes.ui.editor.noteEditorScreen
import com.example.archnotes.ui.notes.NotesRoute
import com.example.archnotes.ui.notes.notesScreen

/**
 * 画面遷移の定義。
 *
 * - 各画面の「住所」(ルート)と NavGraph への登録関数は、その画面のパッケージが持つ
 *   (`ui/notes/NotesNavigation.kt`, `ui/editor/NoteEditorNavigation.kt`)。
 *   NavHost は画面パッケージに依存するが、画面パッケージは NavHost を知らない。
 *   こうしておくと、後で画面ごとにモジュール分割しても依存の向きが崩れない。
 * - 各画面(Screen)は「どこへ遷移するか」を知らない。`onNoteClick` のようなコールバックを
 *   受け取るだけで、実際の遷移はここで決める。
 */
@Composable
fun ArchNotesNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NotesRoute) {
        notesScreen(
            onNoteClick = { noteId -> navController.navigateToNoteEditor(noteId) },
            onAddClick = { navController.navigateToNoteEditor() },
        )
        noteEditorScreen(
            onDone = { navController.popBackStack() },
        )
    }
}
