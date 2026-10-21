package com.example.archnotes.domain

import com.example.archnotes.model.Note
import com.example.archnotes.testing.FakeNotesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Domain 層のテスト。Android にも ViewModel にも依存しないので、最もシンプルに書ける。
 */
class GetNotesUseCaseTest {

    private val repository = FakeNotesRepository(
        listOf(
            Note(id = 1, title = "Kotlin の勉強", body = "coroutines", updatedAt = 1),
            Note(id = 2, title = "買い物", body = "牛乳と Kotlin 本", updatedAt = 2),
            Note(id = 3, title = "散歩", body = "", updatedAt = 3),
        ),
    )
    private val getNotes = GetNotesUseCase(repository)

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
}
