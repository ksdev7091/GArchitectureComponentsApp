package com.example.archnotes.domain

import com.example.archnotes.model.Note
import com.example.archnotes.model.SortOrder
import com.example.archnotes.model.UserPreferences
import com.example.archnotes.testing.FakeNotesRepository
import com.example.archnotes.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Domain 層のテスト。Android にも ViewModel にも依存しないので、最もシンプルに書ける。
 * 2 つの Repository(メモと設定)を Fake にして、組み合わせのロジックだけを検証する。
 */
class GetNotesUseCaseTest {

    private val notesRepository = FakeNotesRepository(
        listOf(
            Note(id = 1, title = "Kotlin の勉強", body = "coroutines", updatedAt = 1),
            Note(id = 2, title = "買い物", body = "牛乳と Kotlin 本", updatedAt = 2),
            Note(id = 3, title = "散歩", body = "", updatedAt = 3),
        ),
    )
    private val preferencesRepository = FakeUserPreferencesRepository()
    private val getNotes = GetNotesUseCase(notesRepository, preferencesRepository)

    @Test
    fun `検索語が空なら全件をそのまま返す`() = runTest {
        val result = getNotes("").first()
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
    }

    @Test
    fun `タイトルか本文に含まれるメモだけを返す(大文字小文字と前後の空白は無視)`() = runTest {
        val result = getNotes("  kotlin ").first()
        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun `設定がタイトル順なら並べ替える`() = runTest {
        preferencesRepository.setSortOrder(SortOrder.TITLE_ASC)

        val result = getNotes("").first()

        assertEquals(listOf("Kotlin の勉強", "散歩", "買い物"), result.map { it.title })
    }

    @Test
    fun `設定を変えると同じ Flow から新しい並びが流れてくる`() = runTest {
        val prefs = FakeUserPreferencesRepository(UserPreferences(sortOrder = SortOrder.TITLE_ASC))
        val useCase = GetNotesUseCase(notesRepository, prefs)
        assertEquals("Kotlin の勉強", useCase().first().first().title)

        prefs.setSortOrder(SortOrder.UPDATED_DESC)

        assertEquals(listOf(1L, 2L, 3L), useCase().first().map { it.id })
    }
}
