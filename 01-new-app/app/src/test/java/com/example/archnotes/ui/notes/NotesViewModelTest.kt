package com.example.archnotes.ui.notes

import com.example.archnotes.R
import com.example.archnotes.domain.GetNotesUseCase
import com.example.archnotes.model.Note
import com.example.archnotes.model.UserPreferences
import com.example.archnotes.testing.FakeNotesRepository
import com.example.archnotes.testing.FakeUserPreferencesRepository
import com.example.archnotes.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ViewModel の単体テスト。Android 端末もエミュレータも不要で、JVM 上で数ミリ秒で終わる。
 *
 * これができるのは、ViewModel が
 * - Android の View/Context に依存していない
 * - Repository をインターフェース経由で受け取っている(Fake を差し込める)
 * からである。アーキテクチャの価値はテストの書きやすさに最もはっきり現れる。
 *
 * 注意: 並び順(更新日時の新しい順)は本物では DAO の SQL が決めるので、ここでは検証しない。
 * 並び順のテストは androidTest の NoteDaoTest にある。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun note(id: Long, title: String, body: String = "") =
        Note(id = id, title = title, body = body, updatedAt = id)

    private fun createViewModel(
        repository: FakeNotesRepository,
        preferences: FakeUserPreferencesRepository = FakeUserPreferencesRepository(),
    ) = NotesViewModel(
        getNotes = GetNotesUseCase(repository, preferences),
        notesRepository = repository,
        userPreferencesRepository = preferences,
    )

    @Test
    fun `Repository のメモがそのまま uiState に反映され、読込中が解除される`() = runTest {
        val repository = FakeNotesRepository(listOf(note(1, "メモ A"), note(2, "メモ B")))
        val viewModel = createViewModel(repository)
        // stateIn(WhileSubscribed) は購読者がいないと動かないので、テスト内で購読しておく
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf("メモ A", "メモ B"), state.notes.map { it.title })
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
    fun `同期に成功すると Repository 経由でサーバーのメモが一覧に現れる`() = runTest {
        val repository = FakeNotesRepository(listOf(note(1, "ローカル")))
        val viewModel = createViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.refresh()

        val state = viewModel.uiState.value
        // ViewModel はサーバーの応答を直接受け取っていない。Repository(SSOT)の変化が流れてきただけ
        assertEquals(listOf("ローカル", "remote"), state.notes.map { it.title })
        assertFalse(state.isRefreshing)
        assertNull(state.userMessageRes)
    }

    @Test
    fun `同期中は isRefreshing が true になり、その間の再要求は無視される`() = runTest {
        val repository = FakeNotesRepository().apply { refreshGate = CompletableDeferred() }
        val viewModel = createViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.refresh()
        assertTrue(viewModel.uiState.value.isRefreshing)

        viewModel.refresh() // 同期中の二度目は無視される
        assertEquals(1, repository.refreshCount)

        repository.refreshGate!!.complete(Unit)
        assertFalse(viewModel.uiState.value.isRefreshing)
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
    fun `本文プレビューの設定が uiState に反映され、設定を変えると追従する`() = runTest {
        val preferences = FakeUserPreferencesRepository(UserPreferences(showBodyPreview = false))
        val viewModel = createViewModel(FakeNotesRepository(), preferences)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertFalse(viewModel.uiState.value.showBodyPreview)

        preferences.setShowBodyPreview(true) // 設定画面で変えた想定

        assertTrue(viewModel.uiState.value.showBodyPreview)
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
