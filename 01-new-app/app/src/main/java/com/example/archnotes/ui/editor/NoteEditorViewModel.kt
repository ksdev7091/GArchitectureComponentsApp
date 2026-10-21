package com.example.archnotes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.example.archnotes.data.NotesRepository
import com.example.archnotes.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 編集画面の状態のうち、テキスト以外。テキストは [NoteEditorViewModel.title] / [NoteEditorViewModel.body]。 */
data class NoteEditorUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /** 保存が完了したら true。UI はこれを見て前の画面に戻る。 */
    val isSaved: Boolean = false,
)

/**
 * 編集画面の ViewModel。
 *
 * ここで学べるポイント:
 * 1. **画面引数の受け取り**: Navigation が渡した引数を [SavedStateHandle] から取り出す。
 *    Activity/Fragment の Intent や Bundle を ViewModel が触らないので、JVM だけでテストできる。
 * 2. **入力中のテキストの持ち方**: [title] / [body] は StateFlow ではなく
 *    Compose のスナップショット状態(`mutableStateOf`)で持つ。
 *    TextField は「入力 → onValueChange → 新しい value」が同期的に反映されることを前提にしており、
 *    StateFlow → collect → 再コンポーズという非同期の経路を挟むと、速い入力や日本語の変換中に
 *    文字が欠ける・重複することがある(Compose 公式の TextField ガイダンス)。
 *    `savedStateHandle.saveable` で包んでおけば、プロセス再生成後も復元される
 *    (SavedStateHandle は Bundle に入るので、ここに置く状態は小さく保つ)。
 * 3. **ロードは一度だけ**: 既存メモを開いたとき、Repository から読み込むのは最初の一度だけ。
 *    プロセス再生成後に再ロードすると、ユーザーの編集内容を上書きしてしまうため、
 *    「ロード済み」フラグも SavedStateHandle に残す。
 * 4. **二重保存の防止**: 保存ボタンの連打で同じメモが 2 つできないよう、保存中フラグで弾く。
 *
 * テキストはスナップショット状態、それ以外は StateFlow と、2 種類の状態が同居している。
 * 両方から導く「保存できるか」は画面側で計算する(理由は [canSaveNow] のコメント)。
 */
@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val notesRepository: NotesRepository,
) : ViewModel() {

    /**
     * 画面引数(noteId)の取り出し。
     *
     * 型安全ルートなら `savedStateHandle.toRoute<NoteEditorRoute>().noteId` と書けるが、
     * toRoute() は内部で android.os.Bundle を使うため、JVM の単体テストでは
     * "Method putLong in android.os.BaseBundle not mocked" で落ちる(Robolectric が必要になる)。
     * ここでは「ViewModel を素の JVM テストで検証できること」を優先し、
     * ルートのプロパティ名をキーにして SavedStateHandle から直接読んでいる。
     */
    private val noteId: Long =
        savedStateHandle.get<Long>(NoteEditorRoute::noteId.name) ?: Note.NEW_NOTE_ID
    private val isNew: Boolean = noteId == Note.NEW_NOTE_ID

    // ---- テキスト: スナップショット状態 + SavedStateHandle(プロセス再生成を生き延びる) ----

    var title: String by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var body: String by savedStateHandle.saveable { mutableStateOf("") }
        private set

    // ---- テキスト以外: StateFlow ----

    private val isLoading = MutableStateFlow(!isNew && !savedStateHandle.contains(KEY_LOADED))
    private val isSaving = MutableStateFlow(false)
    private val isSaved = MutableStateFlow(false)

    val uiState: StateFlow<NoteEditorUiState> =
        combine(isLoading, isSaving, isSaved) { loading, saving, saved ->
            NoteEditorUiState(isNew = isNew, isLoading = loading, isSaving = saving, isSaved = saved)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NoteEditorUiState(isNew = isNew, isLoading = isLoading.value),
        )

    /**
     * 保存できるか。[title](スナップショット状態)と [uiState](StateFlow)の両方から導く値なので、
     * 両方を購読している画面側で同じ式を計算する(NoteEditorScreen の状態あり版)。
     * ViewModel 側はイベントの入口で「今」の値を見て弾くだけに使う。
     * Compose が追跡するのはスナップショット状態だけで、StateFlow の `.value` を Composable から
     * 直接読んでも再コンポーズの対象にならない。両方が混ざる導出値を公開プロパティにしない。
     */
    private fun canSaveNow(): Boolean =
        title.isNotBlank() && !isLoading.value && !isSaving.value && !isSaved.value

    init {
        if (isLoading.value) {
            loadExistingNote()
        }
    }

    private fun loadExistingNote() {
        viewModelScope.launch {
            val note = notesRepository.observeNote(noteId).first()
            // 他の画面で削除済みなど、存在しない ID なら空のまま編集を続ける
            // (保存すると同じ ID で復活する。このアプリの遷移では起きない)
            if (note != null) {
                title = note.title
                body = note.body
            }
            savedStateHandle[KEY_LOADED] = true
            isLoading.value = false
        }
    }

    fun onTitleChange(value: String) {
        title = value
    }

    fun onBodyChange(value: String) {
        body = value
    }

    fun save() {
        if (!canSaveNow()) return
        // 保存中フラグは launch の外で同期的に立てる。ディスパッチャの都合に依存せず、
        // 連打の 2 回目が必ず弾かれることをコードの形で保証するため
        isSaving.value = true
        viewModelScope.launch {
            try {
                notesRepository.saveNote(
                    Note(
                        id = noteId,
                        title = title.trim(),
                        body = body,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                isSaved.value = true
            } finally {
                isSaving.value = false
            }
        }
    }

    private companion object {
        const val KEY_LOADED = "loaded"
    }
}
