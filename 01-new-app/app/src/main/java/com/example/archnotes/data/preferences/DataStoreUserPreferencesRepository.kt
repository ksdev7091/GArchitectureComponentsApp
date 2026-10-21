package com.example.archnotes.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.archnotes.model.SortOrder
import com.example.archnotes.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Jetpack DataStore(Preferences)で設定を永続化する実装。
 *
 * SharedPreferences との違い:
 * - 読み取りが [Flow] なので、変更が自動で UI に届く(SharedPreferences はリスナー登録が必要)
 * - 書き込みが suspend で、メインスレッドをブロックしない
 * - 読み書きがトランザクショナルで、途中で壊れない
 *
 * DataStore のキーと保存形式(enum を文字列で保存)はこのクラスに閉じ込める。
 */
@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val SHOW_BODY_PREVIEW = booleanPreferencesKey("show_body_preview")
    }

    override val userPreferences: Flow<UserPreferences> =
        dataStore.data
            // ファイルが読めないときは「設定なし」として既定値で続行する(アプリを落とさない)
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                UserPreferences(
                    sortOrder = prefs[Keys.SORT_ORDER]
                        ?.let { name -> SortOrder.entries.firstOrNull { it.name == name } }
                        ?: SortOrder.UPDATED_DESC, // 未知の値(古いバージョンの名前など)は既定に戻す
                    showBodyPreview = prefs[Keys.SHOW_BODY_PREVIEW] ?: true,
                )
            }

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        dataStore.edit { it[Keys.SORT_ORDER] = sortOrder.name }
    }

    override suspend fun setShowBodyPreview(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_BODY_PREVIEW] = show }
    }
}
