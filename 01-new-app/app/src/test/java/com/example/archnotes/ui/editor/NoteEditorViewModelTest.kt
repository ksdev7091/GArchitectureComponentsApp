package com.example.archnotes.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.example.archnotes.model.Note
import com.example.archnotes.testing.FakeNotesRepository
import com.example.archnotes.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 画面引数は SavedStateHandle に入ってくるので、テストでも同じ形で渡す。 */
    private fun savedStateHandleFor(noteId: Long?, vararg extra: Pair<String, Any>) =
        SavedStateHandle(
            buildMap {
                if (noteId != null) put(NoteEditorRoute::noteId.name, noteId)
                putAll(extra)
            },
        )

    @Test
    fun `新規作成のときは空の状態で始まり、タイトルが空なら保存できない`() = runTest {
        val repository = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(savedStateHandleFor(null), repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertTrue(state.isNew)
        assertFalse(state.isLoading)
        assertEquals("", viewModel.title)
        assertFalse(viewModel.canSave)
    }

    @Test
    fun `既存メモを開くと内容が読み込まれる`() = runTest {
        val repository = FakeNotesRepository(
            listOf(Note(id = 7, title = "既存", body = "本文", updatedAt = 1)),
        )
        val viewModel = NoteEditorViewModel(savedStateHandleFor(7), repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertFalse(state.isNew)
        assertFalse(state.isLoading)
        assertEquals("既存", viewModel.title)
        assertEquals("本文", viewModel.body)
    }

    @Test
    fun `ロード済みフラグがあれば Repository から読み直さない(編集内容を上書きしないため)`() = runTest {
        // プロセス再生成後は SavedStateHandle に "loaded" が残っている。
        // (title/body の復元は Bundle 経由なので JVM テストでは再現できず、ここではフラグの効果だけを見る)
        val repository = FakeNotesRepository(
            listOf(Note(id = 7, title = "DB の内容", body = "", updatedAt = 1)),
        )
        val viewModel = NoteEditorViewModel(savedStateHandleFor(7, "loaded" to true), repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.title) // DB の内容で上書きされていない
    }

    @Test
    fun `保存すると Repository に反映され isSaved が true になる`() = runTest {
        val repository = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(savedStateHandleFor(null), repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onTitleChange("  新しいメモ  ")
        viewModel.onBodyChange("内容")
        assertTrue(viewModel.canSave)

        viewModel.save()

        assertTrue(viewModel.uiState.value.isSaved)
        val saved = repository.currentNotes.single()
        assertEquals("新しいメモ", saved.title) // trim される
        assertEquals("内容", saved.body)
    }

    @Test
    fun `既存メモを保存しても ID は変わらず、件数も増えない`() = runTest {
        val repository = FakeNotesRepository(
            listOf(Note(id = 7, title = "前", body = "", updatedAt = 1)),
        )
        val viewModel = NoteEditorViewModel(savedStateHandleFor(7), repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onTitleChange("後")
        viewModel.save()

        val saved = repository.currentNotes.single()
        assertEquals(7L, saved.id)
        assertEquals("後", saved.title)
    }

    @Test
    fun `保存ボタンを連打しても 1 件しか作られない`() = runTest {
        val repository = FakeNotesRepository().apply { saveGate = CompletableDeferred() }
        val viewModel = NoteEditorViewModel(savedStateHandleFor(null), repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.onTitleChange("連打")

        viewModel.save() // 1 回目: saveGate で待たされる
        assertTrue(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.canSave)
        viewModel.save() // 2 回目: 保存中なので無視される

        repository.saveGate!!.complete(Unit)

        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(1, repository.currentNotes.size)
    }
}
