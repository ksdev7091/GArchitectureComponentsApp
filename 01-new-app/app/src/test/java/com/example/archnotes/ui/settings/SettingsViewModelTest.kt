package com.example.archnotes.ui.settings

import com.example.archnotes.model.Note
import com.example.archnotes.model.SortOrder
import com.example.archnotes.testing.FakeNotesRepository
import com.example.archnotes.testing.FakeUserPreferencesRepository
import com.example.archnotes.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `設定とメモ件数が uiState に反映される`() = runTest {
        val notes = FakeNotesRepository(listOf(Note(id = 1, title = "a", body = "", updatedAt = 1)))
        val prefs = FakeUserPreferencesRepository()
        val viewModel = SettingsViewModel(prefs, notes)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(SortOrder.UPDATED_DESC, state.sortOrder)
        assertEquals(1, state.noteCount)
    }

    @Test
    fun `並び順を変えると Repository に保存され、uiState にも反映される`() = runTest {
        val prefs = FakeUserPreferencesRepository()
        val viewModel = SettingsViewModel(prefs, FakeNotesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.setSortOrder(SortOrder.TITLE_ASC)
        viewModel.setShowBodyPreview(false)

        assertEquals(SortOrder.TITLE_ASC, prefs.current.sortOrder)
        assertEquals(SortOrder.TITLE_ASC, viewModel.uiState.value.sortOrder)
        assertFalse(viewModel.uiState.value.showBodyPreview)
    }
}
