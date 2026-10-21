package com.example.archnotes.testing

import com.example.archnotes.data.preferences.UserPreferencesRepository
import com.example.archnotes.model.SortOrder
import com.example.archnotes.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** DataStore の代わりにメモリ上で設定を持つ Fake。 */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {

    private val preferences = MutableStateFlow(initial)

    val current: UserPreferences get() = preferences.value

    override val userPreferences: Flow<UserPreferences> = preferences

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        preferences.update { it.copy(sortOrder = sortOrder) }
    }

    override suspend fun setShowBodyPreview(show: Boolean) {
        preferences.update { it.copy(showBodyPreview = show) }
    }
}
