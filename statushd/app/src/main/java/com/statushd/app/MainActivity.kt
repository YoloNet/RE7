package com.statushd.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.statushd.app.data.local.SettingsStore
import com.statushd.app.data.media.MediaItem
import com.statushd.app.ui.navigation.PickerMode
import com.statushd.app.ui.navigation.Screen
import com.statushd.app.ui.screens.HomeScreen
import com.statushd.app.ui.screens.OptionsSheet
import com.statushd.app.ui.screens.PickerScreen
import com.statushd.app.ui.screens.ProcessingScreen
import com.statushd.app.ui.screens.ResultScreen
import com.statushd.app.ui.screens.SettingsScreen
import com.statushd.app.ui.screens.SplitterScreen
import com.statushd.app.ui.screens.SupportScreen
import com.statushd.app.ui.theme.Accent
import com.statushd.app.ui.theme.StatusHdTheme
import com.statushd.app.viewmodel.AppViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * StatusHD v1.0.0 — Activity utama.
 *
 * - Navigasi berasaskan keadaan (timbunan Screen), seperti KebunKu native.
 * - Edge-to-edge + insets dari skrin pertama (pengajaran pk2).
 * - Bahasa: BM lalai; ditukar dalam Tetapan (recreate).
 * - Tiada WebView, tiada iklan, tiada akaun.
 */
class MainActivity : ComponentActivity() {

    private lateinit var vm: AppViewModel

    override fun attachBaseContext(newBase: Context) {
        val language = try {
            runBlocking { SettingsStore(newBase).settings.first().language }
        } catch (_: Exception) {
            "ms"
        }
        super.attachBaseContext(applyLocale(newBase, language))
    }

    private fun applyLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        vm = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(applicationContext) as T
                }
            },
        )[AppViewModel::class.java]
        setContent {
            StatusHdTheme {
                StatusHdShell(vm, onRecreate = { recreate() })
            }
        }
    }
}

@Composable
private fun StatusHdShell(vm: AppViewModel, onRecreate: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val settingsReady = remember { mutableStateOf(false) }
    val recentJobs by vm.recentJobs.collectAsState()
    val toast by vm.toast.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Timbunan navigasi (Home di bawah sekali).
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = backStack.last()

    // Helaian pilihan (S05) — muncul atas Picker.
    var pendingOptions by remember {
        mutableStateOf<Triple<MediaItem, PickerMode, String>?>(null)
    }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.clearToast()
        }
    }

    LaunchedEffect(settings) {
        settingsReady.value = true
    }

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun goHome() {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    if (!settingsReady.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            when (val screen = current) {
                is Screen.Home -> HomeScreen(
                    vm = vm,
                    settings = settings,
                    recentJobs = recentJobs,
                    onPick = { mode -> push(Screen.Picker(mode)) },
                    onOpenResult = { jobId, mode ->
                        if (mode == PickerMode.SPLIT) {
                            push(Screen.Splitter(jobId))
                        } else {
                            push(Screen.Result(jobId, mode))
                        }
                    },
                    onOpenSupport = { push(Screen.Support) },
                    onOpenSettings = { push(Screen.Settings) },
                )
                is Screen.Picker -> PickerScreen(
                    vm = vm,
                    mode = screen.mode,
                    onBack = { pop() },
                    onPick = { item ->
                        scope.launch {
                            val mediaId = vm.registerMedia(item)
                            pendingOptions = Triple(item, screen.mode, mediaId)
                        }
                    },
                )
                is Screen.Processing -> ProcessingScreen(
                    vm = vm,
                    jobId = screen.jobId,
                    mode = screen.mode,
                    onBack = { goHome() },
                    onDone = { jobId, mode ->
                        backStack.removeAt(backStack.lastIndex)
                        if (mode == PickerMode.SPLIT) {
                            push(Screen.Splitter(jobId))
                        } else {
                            push(Screen.Result(jobId, mode))
                        }
                    },
                    onRetry = { job ->
                        val newId = vm.startJob(
                            job.mediaId,
                            screen.mode,
                            job.preset,
                            job.aspect,
                            job.caption,
                        )
                        backStack.removeAt(backStack.lastIndex)
                        push(Screen.Processing(newId, screen.mode))
                    },
                )
                is Screen.Result -> ResultScreen(
                    vm = vm,
                    jobId = screen.jobId,
                    mode = screen.mode,
                    onBack = { goHome() },
                )
                is Screen.Splitter -> SplitterScreen(
                    vm = vm,
                    jobId = screen.jobId,
                    onBack = { goHome() },
                )
                is Screen.Support -> SupportScreen(onBack = { pop() })
                is Screen.Settings -> SettingsScreen(
                    vm = vm,
                    settings = settings,
                    onBack = { pop() },
                    onLanguageChanged = { language ->
                        if (language != settings.language) {
                            vm.setLanguage(language)
                            onRecreate()
                        }
                    },
                )
                is Screen.Options -> {
                    // Tidak digunakan — S05 ditunjuk sebagai helaian, bukan skrin.
                    pop()
                }
            }
        }
    }

    pendingOptions?.let { (item, mode, mediaId) ->
        OptionsSheet(
            vm = vm,
            item = item,
            mode = mode,
            defaultQuality = settings.qualityDefault,
            onDismiss = { pendingOptions = null },
            onStart = { preset, aspect, caption ->
                pendingOptions = null
                val jobId = vm.startJob(mediaId, mode, preset, aspect, caption)
                push(Screen.Processing(jobId, mode))
            },
        )
    }
}
