package com.example.archnotes.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** 設定画面の「住所」。引数なし。 */
@Serializable
object SettingsRoute

fun NavController.navigateToSettings() {
    navigate(SettingsRoute)
}

/** 設定画面を NavGraph に登録する。 */
fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable<SettingsRoute> {
        SettingsScreen(onBack = onBack)
    }
}
