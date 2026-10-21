package com.example.archnotes.ui.notes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.archnotes.model.Note
import com.example.archnotes.ui.theme.ArchNotesTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 状態なし版 [NotesScreen] の UI テスト。
 *
 * ViewModel も Hilt も Room も不要。任意の [NotesUiState] を渡して
 * 「この状態ならこう描かれる」「この操作でこのコールバックが呼ばれる」だけを検証する。
 * 状態あり版と状態なし版に分けた(状態ホイスティング)から、こういうテストが書ける。
 */
class NotesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun note(id: Long, title: String, body: String = "") =
        Note(id = id, title = title, body = body, updatedAt = id)

    private fun setContent(
        uiState: NotesUiState,
        onDelete: (Long) -> Unit = {},
        onNoteClick: (Long) -> Unit = {},
        onRefresh: () -> Unit = {},
    ) {
        composeRule.setContent {
            ArchNotesTheme {
                NotesScreen(
                    uiState = uiState,
                    onQueryChange = {},
                    onRefresh = onRefresh,
                    onDelete = onDelete,
                    onUserMessageShown = {},
                    onNoteClick = onNoteClick,
                    onAddClick = {},
                    onSettingsClick = {},
                )
            }
        }
    }

    @Test
    fun emptyState_showsGuidanceText() {
        setContent(NotesUiState(isLoading = false))

        composeRule.onNodeWithText("メモがありません", substring = true).assertIsDisplayed()
    }

    @Test
    fun notes_areListedWithBodyPreview() {
        setContent(NotesUiState(isLoading = false, notes = listOf(note(1, "買い物", "牛乳"), note(2, "勉強"))))

        composeRule.onNodeWithText("買い物").assertIsDisplayed()
        composeRule.onNodeWithText("牛乳").assertIsDisplayed()
        composeRule.onNodeWithText("勉強").assertIsDisplayed()
    }

    @Test
    fun bodyPreview_isHiddenWhenSettingIsOff() {
        setContent(NotesUiState(isLoading = false, showBodyPreview = false, notes = listOf(note(1, "買い物", "牛乳"))))

        composeRule.onNodeWithText("買い物").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("牛乳").fetchSemanticsNodes().size)
    }

    @Test
    fun deleteButton_invokesCallbackWithNoteId() {
        var deleted: Long? = null
        setContent(NotesUiState(isLoading = false, notes = listOf(note(7, "消すメモ"))), onDelete = { deleted = it })

        composeRule.onNodeWithContentDescription("削除").performClick()

        assertEquals(7L, deleted)
    }

    @Test
    fun syncButton_isDisabledWhileRefreshing() {
        setContent(NotesUiState(isLoading = false, isRefreshing = true))

        composeRule.onNodeWithContentDescription("サーバーと同期").assertIsNotEnabled()
    }

    @Test
    fun syncButton_isEnabledWhenIdle() {
        setContent(NotesUiState(isLoading = false))

        composeRule.onNodeWithContentDescription("サーバーと同期").assertIsEnabled()
    }
}
