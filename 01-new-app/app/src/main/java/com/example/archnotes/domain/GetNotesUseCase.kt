package com.example.archnotes.domain

import com.example.archnotes.data.NotesRepository
import com.example.archnotes.data.preferences.UserPreferencesRepository
import com.example.archnotes.model.Note
import com.example.archnotes.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Domain 層のユースケース。「検索語に一致するメモを、ユーザーの設定した並び順で取得する」。
 *
 * **ここに Domain 層を置く理由**: このロジックは 2 つの Repository(メモと設定)を組み合わせる。
 * ViewModel に書くと、一覧画面の ViewModel が「メモ」と「設定」の両方の Repository を知り、
 * 組み合わせ方(フィルタ → 並び替え)まで抱えることになる。
 * 同じ組み合わせを別の画面(ウィジェット、共有シートなど)でも使うなら、ここに 1 つあればよい。
 *
 * Domain 層は **省略可能** で、次のどちらかに当てはまるときに置く:
 * 1. 複数の ViewModel で同じロジックを再利用したい
 * 2. 複数の Repository を組み合わせるなど、ViewModel に置くと肥大化するロジックがある
 *
 * 慣例:
 * - クラス名は「動詞 + 名詞 + UseCase」
 * - 公開メソッドは `operator fun invoke` 1 つだけ(関数のように呼べる)
 * - 状態を持たない。Repository に依存し、UI には依存しない。
 */
class GetNotesUseCase @Inject constructor(
    private val notesRepository: NotesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(query: String = ""): Flow<List<Note>> {
        val normalized = query.trim()
        return combine(
            notesRepository.observeNotes(),
            userPreferencesRepository.userPreferences,
        ) { notes, preferences ->
            val filtered = if (normalized.isEmpty()) {
                notes
            } else {
                notes.filter { note ->
                    note.title.contains(normalized, ignoreCase = true) ||
                        note.body.contains(normalized, ignoreCase = true)
                }
            }
            when (preferences.sortOrder) {
                // 既定の並び(更新順)は DAO の SQL が保証している。ここで並べ直すと
                // 「既定の並び順の責任はどこか」が 2 か所に分かれるので、素通しにする
                SortOrder.UPDATED_DESC -> filtered
                SortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            }
        }
    }
}
