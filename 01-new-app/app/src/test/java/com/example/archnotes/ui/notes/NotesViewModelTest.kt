package com.example.archnotes.ui.notes

import com.example.archnotes.R
import com.example.archnotes.domain.GetNotesUseCase
import com.example.archnotes.model.Note
import com.example.archnotes.testing.FakeNotesRepository
import com.example.archnotes.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * ViewModel の単体テスト。Android 端末もエミュレータも不要で、JVM 上で数ミリ秒で終わる。
 *
 * これができるのは、ViewModel が
 * - Android の View/Context に依存していない
 * - Repository をインターフェース経由で受け取っている(Fake を差し込める)
 * からである。アーキテクチャの価値はテストの書きやすさに最もはっきり現れる。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun note(id: Long, title: String, body: String = "") =
        Note(id = id, title = title, body = body, updatedAt = id)

    private fun createViewModel(repository: FakeNotesRepository) =
        NotesViewModel(getNotes = GetNotesUseCase(repository), notesRepository = repository)

    @Test
    fun `Repository のメモが更新日時の新しい順で uiState に反映される`() = runTest {
        val repository = FakeNotesRepository(listOf(note(1, "古いメモ"), note(2, "新しいメモ")))
        val viewModel = createViewModel(repository)
        // stateIn(WhileSubscribed) は購読者がいないと動かないので、テスト内で購読しておく
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf("新しいメモ", "古いメモ"), state.notes.map { it.title })
    }

    @Test
    fun `検索語を入力すると一致するメモだけになる`() = runTest {
        val repository = FakeNotesRepository(listOf(note(1, "買い物", "牛乳"), note(2, "勉強", "Kotlin")))
        val viewModel = createViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onQueryChange("kotlin")

        val state = viewModel.uiState.value
        assertEquals("kotlin", state.query)
        assertEquals(listOf("勉強"), state.notes.map { it.title })
    }

    @Test
    fun `同期に失敗するとエラーメッセージが状態に入り、表示後に消せる`() = runTest {
        val repository = FakeNotesRepository().apply { shouldFailRefresh = true }
        val viewModel = createViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.refresh()

        assertEquals(1, repository.refreshCount)
        assertEquals(R.string.notes_refresh_failed, viewModel.uiState.value.userMessageRes)
        assertFalse(viewModel.uiState.value.isRefreshing)

        viewModel.userMessageShown()
        assertNull(viewModel.uiState.value.userMessageRes)
    }

    @Test
    fun `削除すると一覧から消え、削除メッセージが出る`() = runTest {
        val repository = FakeNotesRepository(listOf(note(1, "消すメモ"), note(2, "残すメモ")))
        val viewModel = createViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.deleteNote(1)

        val state = viewModel.uiState.value
        assertEquals(listOf("残すメモ"), state.notes.map { it.title })
        assertEquals(R.string.notes_deleted, state.userMessageRes)
    }
}
