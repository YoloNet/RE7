package com.statushd.app.ui.navigation

/** Mod pemilih media (S03) dan aliran kerja. */
enum class PickerMode { VIDEO, PHOTO, SPLIT }

/**
 * StatusHD — navigasi berasaskan keadaan (seperti KebunKu native):
 * satu senarai timbunan skrin dalam MainActivity, tiada navigation-compose.
 * Ini mengelakkan satu lagi kebergantungan yang belum terbukti.
 */
sealed class Screen {
    object Home : Screen()
    data class Picker(val mode: PickerMode) : Screen()
    data class Options(val mediaId: String, val mode: PickerMode) : Screen()
    data class Processing(val jobId: String, val mode: PickerMode) : Screen()
    data class Result(val jobId: String, val mode: PickerMode) : Screen()
    data class Splitter(val jobId: String) : Screen()
    object Support : Screen()
    object Settings : Screen()
}
