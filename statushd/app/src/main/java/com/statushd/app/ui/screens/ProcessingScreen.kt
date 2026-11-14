package com.statushd.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.statushd.app.R
import com.statushd.app.data.local.CompressionJobEntity
import com.statushd.app.media.FfmpegCommandBuilder
import com.statushd.app.ui.components.ErrorState
import com.statushd.app.ui.components.StatusHdSecondaryButton
import com.statushd.app.ui.components.StatusHdTopBar
import com.statushd.app.ui.navigation.PickerMode
import com.statushd.app.ui.theme.ShTextMuted
import com.statushd.app.viewmodel.AppViewModel

/**
 * S06 — Memproses: bar kemajuan + peratus + saiz, batal dengan pengesahan.
 * Apabila siap, beralih ke S07; jika gagal, tunjuk ralat + cuba lagi.
 */
@Composable
fun ProcessingScreen(
    vm: AppViewModel,
    jobId: String,
    mode: PickerMode,
    onBack: () -> Unit,
    onDone: (String, PickerMode) -> Unit,
    onRetry: (CompressionJobEntity) -> Unit,
) {
    val job by vm.observeJob(jobId).collectAsState(initial = null)
    var showCancelConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(job?.status) {
        if (job?.status == "delivered") {
            if (mode == PickerMode.SPLIT) {
                onDone(jobId, mode)
            } else {
                onDone(jobId, mode)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StatusHdTopBar(
            title = stringResource(R.string.processing_title),
            onBack = onBack,
        )

        when {
            job == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.common_loading))
                }
            }
            job!!.status == "failed" -> {
                val isCancelled = job!!.error == "cancelled"
                ErrorState(
                    title = if (isCancelled) {
                        stringResource(R.string.processing_cancel)
                    } else {
                        stringResource(R.string.processing_failed)
                    },
                    subtitle = if (isCancelled) {
                        ""
                    } else {
                        stringResource(
                            R.string.processing_failed_sub,
                            job!!.error ?: "",
                        )
                    },
                    onRetry = { onRetry(job!!) },
                )
            }
            else -> {
                val progress = job!!.progress
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (mode == PickerMode.PHOTO) {
                            stringResource(R.string.processing_photo)
                        } else {
                            stringResource(R.string.processing_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            R.string.processing_progress,
                            progress,
                            FfmpegCommandBuilder.formatBytes(job!!.sizeBefore),
                            FfmpegCommandBuilder.formatBytes(
                                vm.estimateBytes(job!!.durationMs, job!!.preset),
                            ),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShTextMuted,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    StatusHdSecondaryButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = { showCancelConfirm = true },
                    )
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(text = stringResource(R.string.processing_cancel)) },
            text = { Text(text = stringResource(R.string.processing_cancel_sub)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirm = false
                    vm.cancelJob(jobId)
                }) {
                    Text(text = stringResource(R.string.processing_cancel_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(text = stringResource(R.string.common_back))
                }
            },
        )
    }
}
