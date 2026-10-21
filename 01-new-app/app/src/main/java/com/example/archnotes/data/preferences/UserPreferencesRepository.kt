package com.example.archnotes.data.preferences

import com.example.archnotes.model.SortOrder
import com.example.archnotes.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * ユーザー設定の Data 層 API。
 *
 * [com.example.archnotes.data.NotesRepository] と同じ設計:
 * - 読み取りは [Flow]。設定が変わればすべての購読者(一覧画面、設定画面)に同時に届く
 * - 書き込みは suspend
 * - インターフェースにしてテストで Fake に差し替える
 *
 * 「メモ」と「設定」は別の種類のデータなので Repository も分ける。
 * 2 つを組み合わせるのは Domain 層([com.example.archnotes.domain.GetNotesUseCase])の仕事。
 */
interface UserPreferencesRepository {

    val userPreferences: Flow<UserPreferences>

    suspend fun setSortOrder(sortOrder: SortOrder)

    suspend fun setShowBodyPreview(show: Boolean)
}
