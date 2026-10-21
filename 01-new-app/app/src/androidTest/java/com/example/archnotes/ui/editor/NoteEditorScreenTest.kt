package com.example.archnotes.ui.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.archnotes.ui.theme.ArchNotesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 状態なし版 [NoteEditorScreen] の UI テスト。 */
class NoteEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveButton_isDisabledWhenCanSaveIsFalse() {
        composeRule.setContent {
            ArchNotesTheme {
                NoteEditorScreen(
                    title = "", body = "", canSave = false, uiState = NoteEditorUiState(isNew = true),
                    onTitleChange = {}, onBodyChange = {}, onSave = {}, onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("新しいメモ").assertIsDisplayed()
        composeRule.onNodeWithText("保存").assertIsNotEnabled()
    }

    @Test
    fun typingTitle_invokesOnTitleChange_andSaveCallsOnSave() {
        var typed = ""
        var saved = false
        composeRule.setContent {
            ArchNotesTheme {
                NoteEditorScreen(
                    title = "", body = "", canSave = true, uiState = NoteEditorUiState(isNew = false),
                    onTitleChange = { typed = it }, onBodyChange = {}, onSave = { saved = true }, onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("タイトル").performTextInput("abc")
        assertEquals("abc", typed)

        composeRule.onNodeWithText("保存").assertIsEnabled().performClick()
        assertTrue(saved)
    }

    @Test
    fun loadingState_showsNoTextFields() {
        composeRule.setContent {
            ArchNotesTheme {
                NoteEditorScreen(
                    title = "", body = "", canSave = false, uiState = NoteEditorUiState(isNew = false, isLoading = true),
                    onTitleChange = {}, onBodyChange = {}, onSave = {}, onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("メモを編集").assertIsDisplayed()
        composeRule.onNodeWithText("タイトル").assertDoesNotExist()
    }
}
