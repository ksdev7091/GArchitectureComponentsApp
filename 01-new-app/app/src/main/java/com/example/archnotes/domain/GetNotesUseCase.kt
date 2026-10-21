package com.example.archnotes.domain

import com.example.archnotes.data.NotesRepository
import com.example.archnotes.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain 層のユースケース。「検索語に一致するメモを取得する」という 1 つの業務ロジックを表す。
 *
 * Domain 層は **省略可能** な層で、次のどちらかに当てはまるときに置く価値がある:
 * 1. 複数の ViewModel で同じロジックを再利用したい
 * 2. ViewModel が肥大化してきたので、ロジックを切り出して単体テストしたい
 *
 * このアプリの検索は単純なので本来は ViewModel 内でも書けるが、
 * 「Domain 層を置くとこういう形になる」を示すためにあえて切り出している。
 *
 * 慣例:
 * - クラス名は「動詞 + 名詞 + UseCase」
 * - 公開メソッドは `operator fun invoke` 1 つだけ(関数のように呼べる)
 * - 状態を持たない。Repository に依存し、UI には依存しない。
 */
class GetNotesUseCase @Inject constructor(
    private val notesRepository: NotesRepository,
) {
    operator fun invoke(query: String = ""): Flow<List<Note>> {
        val normalized = query.trim()
        return notesRepository.observeNotes().map { notes ->
            if (normalized.isEmpty()) {
                notes
            } else {
                notes.filter { note ->
                    note.title.contains(normalized, ignoreCase = true) ||
                        note.body.contains(normalized, ignoreCase = true)
                }
            }
        }
    }
}
