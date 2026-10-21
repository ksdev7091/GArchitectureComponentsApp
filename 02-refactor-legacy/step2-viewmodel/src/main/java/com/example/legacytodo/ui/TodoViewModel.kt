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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 【Step 2 で新設】画面の状態。「画面を描くのに必要な情報」を 1 つの不変オブジェクトにまとめる。
 *
 * Step 0〜1 で Activity のフィールドに散っていた `todos` と、`updateSummary()` の中にあった
 * 集計ロジック(④)がここに来た。`doneCount` は todos から導出できるので
 * 別フィールドにせず計算プロパティにする(二重管理を避ける)。
 */
data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
) {
    val totalCount: Int get() = todos.size
    val doneCount: Int get() = todos.count { it.isDone }
}

/**
 * 【Step 2 で新設】状態ホルダー。
 *
 * Step 1 からの変化:
 * - 画面の状態は ViewModel が持つ。画面回転で Activity が作り直されても ViewModel は生き残るので、
 *   回転のたびに DB を読み直さなくなった(③を解決)。
 * - 入力チェックと完了数の集計が UI から出てきた(④を解決)。
 * - Activity は「状態を描く」「イベントを渡す」だけになった(①を解決)。
 *
 * まだ残っている問題:
 * - 書き込みのたびに `loadTodos()` を呼んで読み直している。呼び忘れると画面が古いままになる。
 *   → Step 3 で Room の Flow に置き換えて「DB が変わったら勝手に流れてくる」形にする。
 * - 依存の組み立てを手書きの Factory でやっている(⑤は半分解決)。→ Step 4 で Hilt に。
 */
class TodoViewModel(
    private val repository: TodoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch {
            val todos = repository.getTodos()
            _uiState.update { it.copy(todos = todos, isLoading = false) }
        }
    }

    fun addTodo(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            // 「エラーを表示したい」も状態として表現する。Toast を直接出すのは UI の仕事
            _uiState.update { it.copy(errorMessageRes = R.string.empty_title_error) }
            return
        }
        viewModelScope.launch {
            repository.addTodo(trimmed)
            loadTodos() // まだ手動で読み直している(Step 3 で解消)
        }
    }

    fun setDone(id: Long, isDone: Boolean) {
        viewModelScope.launch {
            repository.setDone(id, isDone)
            loadTodos()
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch {
            repository.deleteTodo(id)
            loadTodos()
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }

    companion object {
        /**
         * ViewModel は OS が生成・保持するため、コンストラクタ引数を直接渡せない。
         * Factory で「どう作るか」を教える。Hilt を使うとこの Factory が不要になる(Step 4)。
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TodoApplication
                TodoViewModel(app.container.todoRepository)
            }
        }
    }
}
