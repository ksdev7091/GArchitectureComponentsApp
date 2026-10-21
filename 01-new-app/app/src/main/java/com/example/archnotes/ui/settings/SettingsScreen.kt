package com.example.archnotes.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.archnotes.R
import com.example.archnotes.model.SortOrder
import com.example.archnotes.ui.theme.ArchNotesTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onSortOrderChange = viewModel::setSortOrder,
        onShowBodyPreviewChange = viewModel::setShowBodyPreview,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSortOrderChange: (SortOrder) -> Unit,
    onShowBodyPreviewChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_sort_order),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // RadioButton は「選ばれている値」を引数で受け取り、選択をコールバックで返すだけ(状態を持たない)
            Column(modifier = Modifier.selectableGroup()) {
                SortOrderOption(
                    label = stringResource(R.string.settings_sort_updated),
                    selected = uiState.sortOrder == SortOrder.UPDATED_DESC,
                    onClick = { onSortOrderChange(SortOrder.UPDATED_DESC) },
                )
                SortOrderOption(
                    label = stringResource(R.string.settings_sort_title),
                    selected = uiState.sortOrder == SortOrder.TITLE_ASC,
                    onClick = { onSortOrderChange(SortOrder.TITLE_ASC) },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            // 行全体をタップ対象にする(文字をタップしても切り替わる)。Switch 自体のクリックは null にして二重処理を避ける
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = uiState.showBodyPreview,
                        role = Role.Switch,
                        onValueChange = onShowBodyPreviewChange,
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_show_body_preview),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = uiState.showBodyPreview,
                    onCheckedChange = null,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (!uiState.isLoading) {
                Text(
                    text = stringResource(R.string.settings_note_count, uiState.noteCount),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SortOrderOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null) // クリックは Row 全体で受ける
        Text(text = label, modifier = Modifier.padding(start = 12.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ArchNotesTheme {
        SettingsScreen(
            uiState = SettingsUiState(sortOrder = SortOrder.TITLE_ASC, showBodyPreview = false, noteCount = 5, isLoading = false),
            onSortOrderChange = {},
            onShowBodyPreviewChange = {},
            onBack = {},
        )
    }
}
