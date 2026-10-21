package com.example.archnotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * 画面からデータ層までを通す End-to-End テスト。
 *
 * - [HiltAndroidRule] がテスト用の Hilt グラフを組み立てる([com.example.archnotes.di.TestRemoteModule] で
 *   リモートが差し替わる)。Room と DataStore は本物が動く
 * - `createAndroidComposeRule<MainActivity>()` が本物の Activity を起動する
 * - 操作と検証は Compose のセマンティクス(contentDescription / text)で行う
 *
 * 本番コードは変えていないので、このテストが通れば「同期ボタン → Repository → Room → 一覧」の
 * 配線が実機で正しいことが分かる。ネットワーク層だけが差し替わっているので、結果は決定的で速い。
 */
@HiltAndroidTest
class SyncFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun tappingSync_showsNoteFromReplacedRemote() {
        composeRule.onNodeWithContentDescription("サーバーと同期").performClick()

        // Room への書き込み → Flow → 再コンポーズを待つ
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("テスト用のサーバーメモ").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("テスト用のサーバーメモ").assertIsDisplayed()
    }
}
