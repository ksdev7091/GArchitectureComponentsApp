package com.example.legacytodo.ui

import com.example.legacytodo.R
import com.example.legacytodo.model.Todo
import com.example.legacytodo.testing.FakeTodoRepository
import com.example.legacytodo.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * 【Step 4 で新設】Step 0 では不可能だった「画面ロジックの自動テスト」。
 *
 * - エミュレータ不要。JVM 上で数ミリ秒。
 * - 「追加したら件数が増える」「完了にすると完了数が増える」「空文字はエラー」といった
 *   仕様をコードとして残せる。次のリファクタリング(Step 5: Compose 化)の安全網になる。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun todo(id: Long, title: String, isDone: Boolean = false) =
        Todo(id = id, title = title, isDone = isDone, createdAt = 0)

    @Test
    fun `Repository の内容が uiState に反映され、完了数が集計される`() = runTest {
        val repository = FakeTodoRepository(listOf(todo(1, "a", isDone = true), todo(2, "b")))
        val viewModel = TodoViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertEquals(2, state.totalCount)
        assertEquals(1, state.doneCount)
    }

    @Test
    fun `追加すると一覧に増える(前後の空白は除去)`() = runTest {
        val repository = FakeTodoRepository()
        val viewModel = TodoViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.addTodo("  牛乳を買う ")

        assertEquals(listOf("牛乳を買う"), viewModel.uiState.value.todos.map { it.title })
        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `空文字を追加しようとするとエラーメッセージが状態に入る`() = runTest {
        val repository = FakeTodoRepository()
        val viewModel = TodoViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.addTodo("   ")

        assertEquals(R.string.empty_title_error, viewModel.uiState.value.errorMessageRes)
        assertEquals(0, viewModel.uiState.value.totalCount)

        viewModel.errorMessageShown()
        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `完了にすると doneCount が増え、削除すると totalCount が減る`() = runTest {
        val repository = FakeTodoRepository(listOf(todo(1, "a"), todo(2, "b")))
        val viewModel = TodoViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.setDone(1, true)
        assertEquals(1, viewModel.uiState.value.doneCount)

        viewModel.deleteTodo(2)
        assertEquals(1, viewModel.uiState.value.totalCount)
        assertEquals(listOf("a"), viewModel.uiState.value.todos.map { it.title })
    }
}
