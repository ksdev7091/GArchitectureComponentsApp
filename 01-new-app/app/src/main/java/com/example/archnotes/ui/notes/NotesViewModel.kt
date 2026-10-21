package com.example.archnotes.ui.notes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.archnotes.R
import com.example.archnotes.data.NotesRepository
import com.example.archnotes.data.preferences.UserPreferencesRepository
import com.example.archnotes.domain.GetNotesUseCase
import com.example.archnotes.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * 一覧画面の UI 状態。「画面に描くために必要な情報のすべて」を 1 つの不変オブジェクトにまとめる。
 *
 * - UI はこのオブジェクトを受け取って描画するだけ。UI 自身は状態を持たない。
 * - ユーザーへの一度きりの通知(Snackbar など)も状態として表現する([userMessageRes])。
 *   「イベント」として別チャネルで流すと、画面回転中に取りこぼすことがあるため。
 */
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** 設定画面で変えられる。設定の Flow から導くので、画面間で値を受け渡すコードはない */
    val showBodyPreview: Boolean = true,
    @StringRes val userMessageRes: Int? = null,
)

/**
 * 一覧画面の状態ホルダー(ViewModel)。
 *
 * 役割:
 * 1. Data/Domain 層から受け取ったデータを **UI 状態に変換** して公開する
 * 2. UI からの **イベント**(検索語の変更、削除、同期)を受け取り、適切な層に委譲する
 *
 * やらないこと:
 * - Android の View/Context を参照しない(だから JVM だけで単体テストできる)
 * - UI の描画方法を知らない(Compose でも View でも同じ ViewModel が使える)
 *
 * これが「単方向データフロー(UDF)」:
 *   状態は ViewModel → UI へ、イベントは UI → ViewModel へ、一方向にだけ流れる。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getNotes: GetNotesUseCase,
    private val notesRepository: NotesRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    // UI から変更される「入力」はそれぞれ独立した StateFlow として持つ
    private val query = MutableStateFlow("")
    private val isRefreshing = MutableStateFlow(false)
    private val userMessageRes = MutableStateFlow<Int?>(null)

    // 検索語が変わるたびに、Domain 層から新しい Flow を取り直す。
    // (1 文字ごとに Room のクエリを張り直すことになる。この規模では問題ないが、
    //  大きなテーブルなら observeNotes() と query を combine してメモリ上で絞る方が軽い)
    private val notes = query.flatMapLatest { getNotes(it) }

    /**
     * 画面が購読する唯一の状態。
     * 複数の Flow を combine して 1 つの UiState に合成する。
     *
     * `stateIn` の `WhileSubscribed(5_000)`:
     *   画面が購読をやめて 5 秒経つと上流の Flow(Room のクエリなど)を止める。
     *   画面回転のような短い中断では止めないので、再購読時に即座に最新値が返る。
     */
    val uiState: StateFlow<NotesUiState> =
        combine(
            notes,
            query,
            isRefreshing,
            userMessageRes,
            userPreferencesRepository.userPreferences,
        ) { notes, query, refreshing, messageRes, preferences ->
            NotesUiState(
                notes = notes,
                query = query,
                isLoading = false,
                isRefreshing = refreshing,
                showBodyPreview = preferences.showBodyPreview,
                userMessageRes = messageRes,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState(),
        )

    // ---- 以下、UI からのイベント ----

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true // launch の外で同期的に立て、連打の 2 回目を確実に弾く
        viewModelScope.launch {
            try {
                notesRepository.refresh()
            } catch (e: IOException) {
                // 例外は UI に投げず「状態」に変換する。UI はこの状態を見て Snackbar を出す。
                // catch する型は Data 層の契約(通信失敗 = IOException)に合わせる。
                // `catch (e: Exception)` と書くとコルーチンのキャンセルまで握りつぶしてしまう。
                userMessageRes.value = R.string.notes_refresh_failed
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            notesRepository.deleteNote(id)
            userMessageRes.value = R.string.notes_deleted
        }
    }

    /** UI がメッセージを受け取ったら(表示を始める前に)呼ぶ。同じメッセージを二度出さないため。 */
    fun userMessageShown() {
        userMessageRes.value = null
    }
}
