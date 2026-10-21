package com.example.legacytodo.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.legacytodo.R
import com.example.legacytodo.TodoApplication
import com.example.legacytodo.data.TodoRepository
import com.example.legacytodo.model.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
) {
    val totalCount: Int get() = todos.size
    val doneCount: Int get() = todos.count { it.isDone }
}

/**
 * 【Step 3 で変更】「読み直す」コードが全部消えた。
 *
 * Step 2 では `_uiState` を自分で書き換え、書き込みのたびに `loadTodos()` を呼んでいた。
 * Step 3 では Repository の Flow を **変換して公開するだけ**。
 * - DB が変われば Room が新しいリストを流す
 * - それを combine で UiState に組み立てる
 * - stateIn で StateFlow に変換し、画面が購読している間だけ動かす
 *
 * ViewModel は「状態を所有する」というより「状態を導出する」ものになった。
 * 自分で書き換える状態は、UI 側の都合で生まれる errorMessage だけ。
 */
class TodoViewModel(
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
        viewModelScope.launch {
            repository.addTodo(trimmed)
            // loadTodos() は不要。Room の Flow が自動で新しいリストを流す
        }
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TodoApplication
                TodoViewModel(app.container.todoRepository)
            }
        }
    }
}
