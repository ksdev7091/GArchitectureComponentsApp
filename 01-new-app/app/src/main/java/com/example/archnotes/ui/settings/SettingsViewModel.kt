package com.example.archnotes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.archnotes.data.NotesRepository
import com.example.archnotes.data.preferences.UserPreferencesRepository
import com.example.archnotes.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sortOrder: SortOrder = SortOrder.UPDATED_DESC,
    val showBodyPreview: Boolean = true,
    /** 保存されているメモの件数。設定画面も同じ Repository(SSOT)を見ている例 */
    val noteCount: Int = 0,
    val isLoading: Boolean = true,
)

/**
 * 設定画面の ViewModel。
 *
 * 一覧画面と同じ [UserPreferencesRepository] と [NotesRepository] を購読しているだけで、
 * 画面同士は互いを知らない。設定画面で並び順を変えると、一覧画面は Flow を通じて
 * 自動的に並び替わる。「画面間で状態を受け渡す」コードはどこにもない。これが SSOT の効果。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    notesRepository: NotesRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        combine(
            userPreferencesRepository.userPreferences,
            notesRepository.observeNotes(),
        ) { preferences, notes ->
            SettingsUiState(
                sortOrder = preferences.sortOrder,
                showBodyPreview = preferences.showBodyPreview,
                noteCount = notes.size,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch { userPreferencesRepository.setSortOrder(sortOrder) }
    }

    fun setShowBodyPreview(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowBodyPreview(show) }
    }
}
