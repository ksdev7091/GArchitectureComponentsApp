package com.example.legacytodo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.legacytodo.ui.TodoAdapter
import com.example.legacytodo.ui.TodoUiState
import com.example.legacytodo.ui.TodoViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 【Step 4: Hilt + テスト】
 * 変更点は `@AndroidEntryPoint` を付けたことと、`by viewModels()` に Factory を渡さなくなったことだけ。
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: TodoViewModel by viewModels()

    private lateinit var summaryText: TextView
    private lateinit var input: EditText
    private lateinit var adapter: TodoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        summaryText = findViewById(R.id.summary_text)
        input = findViewById(R.id.input)
        adapter = TodoAdapter(
            onDoneChange = { id, isDone -> viewModel.setDone(id, isDone) },
            onDelete = { id -> viewModel.deleteTodo(id) },
        )
        findViewById<ListView>(R.id.todo_list).adapter = adapter

        findViewById<Button>(R.id.add_button).setOnClickListener {
            viewModel.addTodo(input.text.toString())
            input.text.clear() // 成否を待たずにクリアする(空文字のときは消しても同じ)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: TodoUiState) {
        adapter.submit(state.todos)
        summaryText.text = if (state.isLoading) {
            getString(R.string.loading)
        } else {
            getString(R.string.summary_format, state.totalCount, state.doneCount)
        }

        state.errorMessageRes?.let { messageRes ->
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
            viewModel.errorMessageShown() // 受け取ったことを伝えて状態を消し、二重表示を防ぐ
        }
    }
}
