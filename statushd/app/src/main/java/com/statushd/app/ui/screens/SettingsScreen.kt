package com.statushd.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.statushd.app.R
import com.statushd.app.config.AppConfig
import com.statushd.app.data.local.Settings
import com.statushd.app.ui.components.SegmentedControl
import com.statushd.app.ui.components.StatusHdTopBar
import com.statushd.app.ui.theme.ShTextMuted
import com.statushd.app.viewmodel.AppViewModel

/** S15 — Tetapan: bahasa, kualiti lalai, privasi, tentang. */
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    settings: Settings,
    onBack: () -> Unit,
    onLanguageChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    var showPrivacyFull by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        StatusHdTopBar(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LanguageRow(
                selected = settings.language == "ms",
                title = stringResource(R.string.settings_language_ms),
                onClick = { onLanguageChanged("ms") },
            )
            LanguageRow(
                selected = settings.language == "en",
                title = stringResource(R.string.settings_language_en),
                onClick = { onLanguageChanged("en") },
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.settings_quality),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedControl(
                options = listOf("720p", "1080p", "4k"),
                selected = settings.qualityDefault,
                onSelect = { vm.setQualityDefault(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.settings_privacy),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_privacy_policy),
                style = MaterialTheme.typography.titleMedium,
                color = com.statushd.app.ui.theme.Accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickableNoRipple { showPrivacyFull = true },
            )
            Text(
                text = stringResource(R.string.settings_privacy_show),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickableNoRipple {
                        vm.resetPrivacyNotice()
                        vm.showToast(
                            context.getString(R.string.settings_privacy_show_done),
                        )
                    },
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.settings_about_text,
                    AppConfig.VERSION_NAME,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = ShTextMuted,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPrivacyFull) {
        AlertDialog(
            onDismissRequest = { showPrivacyFull = false },
            title = { Text(text = stringResource(R.string.privacy_title)) },
            text = { Text(text = stringResource(R.string.privacy_full)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyFull = false }) {
                    Text(text = stringResource(R.string.common_close))
                }
            },
        )
    }
}

@Composable
private fun LanguageRow(selected: Boolean, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Klik tanpa kesan riak (untuk baris teks pautan). */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return androidx.compose.foundation.clickable(
        indication = null,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        onClick = onClick,
    )
}
