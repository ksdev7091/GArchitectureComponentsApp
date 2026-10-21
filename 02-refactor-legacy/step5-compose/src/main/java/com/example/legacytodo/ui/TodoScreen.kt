package com.example.legacytodo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.legacytodo.R
import com.example.legacytodo.model.Todo
import kotlinx.coroutines.launch

/**
 * 【Step 5 で新設】activity_main.xml + item_todo.xml + TodoAdapter + MainActivity.render() が
 * この 1 ファイルに置き換わった。
 *
 * 状態あり版(ViewModel と接続)と状態なし版(描くだけ)に分けるのは新規アプリ版(01)と同じ。
 */
@Composable
fun TodoScreen(viewModel: TodoViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TodoScreen(
        uiState = uiState,
        onAdd = viewModel::addTodo,
        onDoneChange = viewModel::setDone,
        onDelete = viewModel::deleteTodo,
        onErrorShown = viewModel::errorMessageShown,
    )
}

@Composable
fun TodoScreen(
    uiState: TodoUiState,
    onAdd: (String) -> Unit,
    onDoneChange: (id: Long, isDone: Boolean) -> Unit,
    onDelete: (id: Long) -> Unit,
    onErrorShown: () -> Unit,
) {
    // 入力中のテキストは「ViewModel が保持する必要のない UI 要素の状態」なので UI 側で持つ
    // (追加ボタンでイベントの引数として一度渡すだけ)。rememberSaveable なら回転でも消えない。
    var inputText by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 「表示すべきメッセージがある」状態を受け取ったら、まず ViewModel に「受け取った」と伝えて
    // 状態を消し、表示そのものは画面のスコープで行う。
    // 表示が終わるまで消さずにいると、その間の回転で再表示されたり、同じメッセージの再発生を
    // 取りこぼしたりする(StateFlow は同じ値を流さない)。
    val errorMessage = uiState.errorMessageRes?.let { stringResource(it) }
    LaunchedEffect(uiState.errorMessageRes) {
        if (errorMessage != null) {
            onErrorShown()
            scope.launch { snackbarHostState.showSnackbar(errorMessage) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = if (uiState.isLoading) {
                    stringResource(R.string.loading)
                } else {
                    stringResource(R.string.summary_format, uiState.totalCount, uiState.doneCount)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.input_hint)) },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onAdd(inputText)
                        inputText = ""
                    },
                ) {
                    Text(stringResource(R.string.add))
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.todos, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onDoneChange = { isDone -> onDoneChange(todo.id, isDone) },
                        onDelete = { onDelete(todo.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    onDoneChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = todo.isDone, onCheckedChange = onDoneChange)
        Text(text = todo.title, modifier = Modifier.weight(1f))
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.delete))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoScreenPreview() {
    MaterialTheme {
        TodoScreen(
            uiState = TodoUiState(
                isLoading = false,
                todos = listOf(
                    Todo(id = 1, title = "牛乳を買う", isDone = true, createdAt = 0),
                    Todo(id = 2, title = "アーキテクチャを学ぶ", isDone = false, createdAt = 0),
                ),
            ),
            onAdd = {},
            onDoneChange = { _, _ -> },
            onDelete = {},
            onErrorShown = {},
        )
    }
}
