package com.example.archnotes.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.example.archnotes.model.Note
import com.example.archnotes.testing.FakeNotesRepository
import com.example.archnotes.testing.MainDispatcherRule
import com.example.archnotes.ui.navigation.NoteEditorRoute
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
    private fun savedStateHandleFor(noteId: Long?) =
        SavedStateHandle(if (noteId == null) emptyMap() else mapOf(NoteEditorRoute::noteId.name to noteId))

    @Test
    fun `新規作成のときは空の状態で始まり、タイトルが空なら保存できない`() = runTest {
        val repository = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(savedStateHandleFor(null), repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertTrue(state.isNew)
        assertFalse(state.isLoading)
        assertEquals("", state.title)
        assertFalse(state.canSave)
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
        assertEquals("既存", state.title)
        assertEquals("本文", state.body)
    }

    @Test
    fun `保存すると Repository に反映され isSaved が true になる`() = runTest {
        val repository = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(savedStateHandleFor(null), repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onTitleChange("  新しいメモ  ")
        viewModel.onBodyChange("内容")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.save()

        assertTrue(viewModel.uiState.value.isSaved)
        val saved = repository.currentNotes.single()
        assertEquals("新しいメモ", saved.title) // trim される
        assertEquals("内容", saved.body)
    }
}
