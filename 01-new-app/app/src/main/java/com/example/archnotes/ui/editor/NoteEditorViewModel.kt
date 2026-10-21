package com.example.archnotes.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.archnotes.data.NotesRepository
import com.example.archnotes.model.Note
import com.example.archnotes.ui.navigation.NoteEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteEditorUiState(
    val title: String = "",
    val body: String = "",
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    /** 保存が完了したら true。UI はこれを見て前の画面に戻る。 */
    val isSaved: Boolean = false,
) {
    /** 「保存できるか」は title から導出できるので、別のフィールドにせずプロパティで計算する。 */
    val canSave: Boolean get() = title.isNotBlank() && !isLoading && !isSaved
}

/**
 * 編集画面の ViewModel。
 *
 * ここで学べるポイント:
 * 1. **画面引数の受け取り**: Navigation が渡した引数を [SavedStateHandle] から取り出す。
 *    Activity/Fragment の Intent や Bundle を ViewModel が触らないので、JVM だけでテストできる。
 * 2. **入力途中のテキストの保存**: `title` / `body` は [SavedStateHandle] に置く。
 *    ViewModel はプロセスが OS に kill されると消えるが、SavedStateHandle の中身は復元される。
 *    「入力途中の内容」のような、失うとユーザーが困る軽い状態はここに置く。
 * 3. **ロードは一度だけ**: 既存メモを開いたとき、Repository から読み込むのは最初の一度だけ。
 *    プロセス再生成後に再ロードすると、ユーザーの編集内容を上書きしてしまうため。
 */
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

    // SavedStateHandle をそのまま StateFlow として扱える
    private val title = savedStateHandle.getStateFlow(KEY_TITLE, "")
    private val body = savedStateHandle.getStateFlow(KEY_BODY, "")

    private val isLoading = MutableStateFlow(!isNew && !savedStateHandle.contains(KEY_LOADED))
    private val isSaved = MutableStateFlow(false)

    val uiState: StateFlow<NoteEditorUiState> =
        combine(title, body, isLoading, isSaved) { title, body, loading, saved ->
            NoteEditorUiState(
                title = title,
                body = body,
                isNew = isNew,
                isLoading = loading,
                isSaved = saved,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NoteEditorUiState(isNew = isNew, isLoading = isLoading.value),
        )

    init {
        if (isLoading.value) {
            loadExistingNote()
        }
    }

    private fun loadExistingNote() {
        viewModelScope.launch {
            val note = notesRepository.observeNote(noteId).first()
            savedStateHandle[KEY_TITLE] = note?.title.orEmpty()
            savedStateHandle[KEY_BODY] = note?.body.orEmpty()
            savedStateHandle[KEY_LOADED] = true
            isLoading.value = false
        }
    }

    fun onTitleChange(value: String) {
        savedStateHandle[KEY_TITLE] = value
    }

    fun onBodyChange(value: String) {
        savedStateHandle[KEY_BODY] = value
    }

    fun save() {
        val current = uiState.value
        if (!current.canSave) return
        viewModelScope.launch {
            notesRepository.saveNote(
                Note(
                    id = noteId,
                    title = current.title.trim(),
                    body = current.body,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            isSaved.value = true
        }
    }

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_LOADED = "loaded"
    }
}
