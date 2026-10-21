package com.example.archnotes.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.archnotes.R
import com.example.archnotes.model.Note
import com.example.archnotes.ui.theme.ArchNotesTheme
import kotlinx.coroutines.launch

/**
 * 一覧画面(状態あり版)。ViewModel と接続する「入口」。
 *
 * この関数の責任は「ViewModel から状態を取り出し、イベントを ViewModel に渡す」ことだけ。
 * 描画の中身は下の状態なし版 [NotesScreen] に委ねる。
 */
@Composable
fun NotesScreen(
    onNoteClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle: 画面がバックグラウンドにある間は購読を止める(リソース節約)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotesScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onRefresh = viewModel::refresh,
        onDelete = viewModel::deleteNote,
        onUserMessageShown = viewModel::userMessageShown,
        onNoteClick = onNoteClick,
        onAddClick = onAddClick,
    )
}

/**
 * 一覧画面(状態なし版)。引数で受け取った [uiState] を描くだけ。
 *
 * ViewModel を知らないので、Preview で任意の状態を表示したり、UI テストで
 * 「空のとき」「読込中」などの見た目を直接検証したりできる。
 * これが「状態ホイスティング(state hoisting)」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    uiState: NotesUiState,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onDelete: (Long) -> Unit,
    onUserMessageShown: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onAddClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 「一度きりの通知」も状態として受け取る。
    // 受け取ったらまず ViewModel に「受け取った」と伝えて状態を消し、表示そのものは画面のスコープで行う。
    // 表示が終わるまで消さずにいると、その間の回転で再表示されたり、同じメッセージの再発生を
    // 取りこぼしたりする(StateFlow は同じ値を流さない)。
    // 同じメッセージが連続して出るケースを厳密に扱うなら、公式ガイドのように ID 付きメッセージの
    // リストにする。
    val userMessage = uiState.userMessageRes?.let { stringResource(it) }
    LaunchedEffect(uiState.userMessageRes) {
        if (userMessage != null) {
            onUserMessageShown()
            scope.launch { snackbarHostState.showSnackbar(userMessage) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_title)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.notes_sync))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.notes_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (uiState.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.notes_search_hint)) },
                singleLine = true,
            )
            when {
                uiState.isLoading -> CenteredBox { CircularProgressIndicator() }
                uiState.notes.isEmpty() -> CenteredBox {
                    Text(
                        text = stringResource(R.string.notes_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(32.dp),
                    )
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteRow(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onDelete = { onDelete(note.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.notes_delete))
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

// ---- Preview: 状態なし版なので、任意の UiState を渡して見た目を確認できる ----

@Preview(showBackground = true)
@Composable
private fun NotesScreenPreview() {
    ArchNotesTheme {
        NotesScreen(
            uiState = NotesUiState(
                isLoading = false,
                notes = listOf(
                    Note(id = 1, title = "買い物リスト", body = "牛乳、卵、パン", updatedAt = 0),
                    Note(id = 2, title = "アーキテクチャ", body = "UI → Domain → Data", updatedAt = 0),
                ),
            ),
            onQueryChange = {},
            onRefresh = {},
            onDelete = {},
            onUserMessageShown = {},
            onNoteClick = {},
            onAddClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesScreenEmptyPreview() {
    ArchNotesTheme {
        NotesScreen(
            uiState = NotesUiState(isLoading = false),
            onQueryChange = {},
            onRefresh = {},
            onDelete = {},
            onUserMessageShown = {},
            onNoteClick = {},
            onAddClick = {},
        )
    }
}
