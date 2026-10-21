package com.example.archnotes.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.archnotes.R
import com.example.archnotes.ui.theme.ArchNotesTheme

@Composable
fun NoteEditorScreen(
    onDone: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 「保存完了 → 画面を閉じる」も状態から導く。UI が勝手に閉じるタイミングを決めない。
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onDone()
    }

    NoteEditorScreen(
        uiState = uiState,
        onTitleChange = viewModel::onTitleChange,
        onBodyChange = viewModel::onBodyChange,
        onSave = viewModel::save,
        onBack = onDone,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    uiState: NoteEditorUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isNew) R.string.editor_title_new else R.string.editor_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = uiState.canSave) {
                        Text(stringResource(R.string.editor_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.editor_title_hint)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.body,
                onValueChange = onBodyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp),
                label = { Text(stringResource(R.string.editor_body_hint)) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteEditorScreenPreview() {
    ArchNotesTheme {
        NoteEditorScreen(
            uiState = NoteEditorUiState(title = "タイトル", body = "本文", isNew = false),
            onTitleChange = {},
            onBodyChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
