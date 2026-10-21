package com.example.legacytodo.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.legacytodo.R
import com.example.legacytodo.data.TodoRepository
import com.example.legacytodo.model.Todo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
) {
    val totalCount: Int get() = todos.size
    val doneCount: Int get() = todos.count { it.isDone }
}

/**
 * 【Step 5: Compose 化】ViewModel は Step 4 から **1 文字も変わっていない**。
 *
 * View(XML) から Compose へ UI の作り方が丸ごと変わっても、状態ホルダーには影響しない。
 * UiState と ViewModel を「UI 技術に依存しない形」で設計しておいた見返り。
 * 単体テスト(src/test)もそのまま通る。
 */
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository,
) : ViewModel() {

    private val errorMessageRes = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<TodoUiState> =
        combine(repository.observeTodos(), errorMessageRes) { todos, errorRes ->
            TodoUiState(todos = todos, isLoading = false, errorMessageRes = errorRes)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodoUiState(),
        )

    fun addTodo(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            errorMessageRes.value = R.string.empty_title_error
            return
        }
        viewModelScope.launch { repository.addTodo(trimmed) }
    }

    fun setDone(id: Long, isDone: Boolean) {
        viewModelScope.launch { repository.setDone(id, isDone) }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch { repository.deleteTodo(id) }
    }

    fun errorMessageShown() {
        errorMessageRes.value = null
    }
}
