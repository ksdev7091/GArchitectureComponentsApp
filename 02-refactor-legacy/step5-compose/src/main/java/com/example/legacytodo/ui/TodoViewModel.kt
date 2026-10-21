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
 * 【Step 4 で変更】`@HiltViewModel` + `@Inject constructor` にして、手書きの Factory を削除した。
 *
 * ロジックは Step 3 から変わっていない。
 * そして `TodoRepository` をインターフェースで受け取っているので、
 * テストでは [FakeTodoRepository](src/test) を渡して JVM 上で検証できる。
 * → `src/test/java/com/example/legacytodo/ui/TodoViewModelTest.kt`
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
