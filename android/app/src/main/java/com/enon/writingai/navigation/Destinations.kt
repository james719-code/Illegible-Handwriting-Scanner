package com.enon.writingai.navigation

sealed class Destination(
    val route: String,
    val title: String,
) {
    companion object {
        const val HISTORY_ENTRY_ID_ARG = "entryId"
    }

    data object Splash : Destination("splash", "Splash")
    data object Home : Destination("home", "Home")
    data object Capture : Destination("capture", "Capture")
    data object Gallery : Destination("gallery", "Import")
    data object Preprocess : Destination("preprocess", "Preprocess")
    data object Scan : Destination("scan", "Scan")
    data object Result : Destination("result", "Result")
    data object History : Destination("history", "History")
    data object HistoryDetail : Destination("history/{$HISTORY_ENTRY_ID_ARG}", "Saved Scan") {
        fun createRoute(entryId: String): String = "history/$entryId"
    }
    data object Settings : Destination("settings", "Settings")
    data object About : Destination("about", "About")
}
