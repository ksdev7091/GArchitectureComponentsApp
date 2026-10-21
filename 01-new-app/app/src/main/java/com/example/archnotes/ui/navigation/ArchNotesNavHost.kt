package com.example.archnotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.archnotes.model.Note
import com.example.archnotes.ui.editor.NoteEditorScreen
import com.example.archnotes.ui.notes.NotesScreen
import kotlinx.serialization.Serializable

/**
 * 画面の「住所」。Navigation Compose の型安全ルート機能を使う。
 * 文字列 "editor/{noteId}" のような組み立てをしなくて済み、引数の型ミスがコンパイル時に分かる。
 */
@Serializable
object NotesRoute

@Serializable
data class NoteEditorRoute(val noteId: Long = Note.NEW_NOTE_ID)

/**
 * 画面遷移の定義。
 *
 * 各画面(Screen)は「どこへ遷移するか」を知らない。`onNoteClick` のようなコールバックを
 * 受け取るだけで、実際の遷移はここ(NavHost)で決める。
 * 画面を別の場所で再利用したり、プレビュー/テストで単独表示したりしやすくなる。
 */
@Composable
fun ArchNotesNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NotesRoute) {
        composable<NotesRoute> {
            NotesScreen(
                onNoteClick = { noteId -> navController.navigate(NoteEditorRoute(noteId)) },
                onAddClick = { navController.navigate(NoteEditorRoute()) },
            )
        }
        composable<NoteEditorRoute> {
            NoteEditorScreen(
                onDone = { navController.popBackStack() },
            )
        }
    }
}
