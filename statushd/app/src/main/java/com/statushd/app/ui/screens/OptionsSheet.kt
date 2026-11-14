package com.statushd.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.statushd.app.R
import com.statushd.app.data.media.MediaItem
import com.statushd.app.media.FfmpegCommandBuilder
import com.statushd.app.ui.components.SegmentedControl
import com.statushd.app.ui.components.StatusHdButton
import com.statushd.app.ui.navigation.PickerMode
import com.statushd.app.ui.theme.ShTextMuted
import com.statushd.app.viewmodel.AppViewModel

/**
 * S05 — Helaian pilihan mampatan: kualiti, nisbah, kapsyen, anggaran,
 * butang mula. Anggaran saiz dilabel sebagai anggaran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheet(
    vm: AppViewModel,
    item: MediaItem,
    mode: PickerMode,
    defaultQuality: String,
    onDismiss: () -> Unit,
    onStart: (preset: String, aspect: String, caption: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var preset by remember { mutableStateOf(defaultQuality) }
    var aspect by remember { mutableStateOf("original") }
    var caption by remember { mutableStateOf("") }

    val durationMs = if (item.type == "photo") {
        5_000L
    } else {
        item.durationMs
    }
    val estimate = remember(preset, durationMs) {
        FfmpegCommandBuilder.estimateOutputBytes(
            durationMs,
            com.statushd.app.media.Preset.fromName(preset),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.options_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = stringResource(R.string.options_quality))
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedControl(
                options = listOf("720p", "1080p", "4k"),
                selected = preset,
                onSelect = { preset = it },
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = stringResource(R.string.options_aspect))
            Spacer(modifier = Modifier.height(4.dp))
            AspectRow(
                selected = aspect == "original",
                title = stringResource(R.string.options_aspect_original),
                subtitle = stringResource(R.string.options_aspect_original_sub),
                onClick = { aspect = "original" },
            )
            AspectRow(
                selected = aspect == "9:16",
                title = stringResource(R.string.options_aspect_916),
                subtitle = stringResource(R.string.options_aspect_916_sub),
                onClick = { aspect = "9:16" },
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.options_caption)) },
                placeholder = { Text(text = stringResource(R.string.options_caption_hint)) },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(
                    R.string.options_estimate,
                    FfmpegCommandBuilder.formatBytes(item.sizeBytes),
                    FfmpegCommandBuilder.formatBytes(estimate),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = ShTextMuted,
            )
            Spacer(modifier = Modifier.height(16.dp))

            StatusHdButton(
                text = stringResource(R.string.options_start),
                onClick = { onStart(preset, aspect, caption.ifBlank { null }) },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AspectRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ShTextMuted,
            )
        }
    }
}
