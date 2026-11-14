package com.statushd.app.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.statushd.app.R
import com.statushd.app.media.FfmpegCommandBuilder
import com.statushd.app.ui.components.SegmentedControl
import com.statushd.app.ui.components.StatusHdButton
import com.statushd.app.ui.components.StatusHdSecondaryButton
import com.statushd.app.ui.components.StatusHdTopBar
import com.statushd.app.ui.navigation.PickerMode
import com.statushd.app.ui.theme.ShTextMuted
import com.statushd.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch

/**
 * S07 — Hasil: pratonton video (VideoView), togol Sebelum/Selepas,
 * saiz, kongsi ke WhatsApp, simpan ke galeri, hantar ke nombor sendiri.
 */
@Composable
fun ResultScreen(
    vm: AppViewModel,
    jobId: String,
    mode: PickerMode,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val job by vm.observeJob(jobId).collectAsState(initial = null)
    val settings by vm.settings.collectAsState()
    var showAfter by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var showNumberDialog by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf(settings.whatsappNumber) }
    var numberError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        StatusHdTopBar(
            title = stringResource(R.string.result_title),
            onBack = onBack,
        )

        val currentJob = job
        if (currentJob == null || currentJob.outputUri == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.common_loading))
            }
            return
        }

        // Pratonton: video output / asal, atau foto asal (mod foto).
        var videoView by remember { mutableStateOf<VideoView?>(null) }
        val beforeUri = vm.mediaUriFor(currentJob.mediaId)
        if (mode == PickerMode.PHOTO && !showAfter && beforeUri != null) {
            PhotoPreview(uri = beforeUri)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.BottomEnd,
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).also { view ->
                            videoView = view
                            view.setOnPreparedListener { mp ->
                                mp.isLooping = true
                                if (isPlaying) view.start()
                            }
                        }
                    },
                    update = { view ->
                        val uri = if (showAfter) {
                            Uri.parse("file://${currentJob.outputUri}")
                        } else {
                            Uri.parse(beforeUri ?: "")
                        }
                        if (view.tag != uri.toString()) {
                            view.tag = uri.toString()
                            view.setVideoURI(uri)
                        }
                        if (isPlaying) view.start() else view.pause()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        if (isPlaying) videoView?.start() else videoView?.pause()
                    },
                ) {
                    Icon(
                        imageVector = if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SegmentedControl(
            options = listOf(
                stringResource(R.string.result_before),
                stringResource(R.string.result_after),
            ),
            selected = if (showAfter) {
                stringResource(R.string.result_after)
            } else {
                stringResource(R.string.result_before)
            },
            onSelect = { showAfter = it == stringResource(R.string.result_after) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(
                R.string.result_sizes,
                FfmpegCommandBuilder.formatBytes(currentJob.sizeBefore),
                FfmpegCommandBuilder.formatBytes(currentJob.sizeAfter),
                currentJob.preset,
                FfmpegCommandBuilder.formatDuration(currentJob.durationMs),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = ShTextMuted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (mode == PickerMode.PHOTO) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.result_photo_note),
                style = MaterialTheme.typography.bodyMedium,
                color = ShTextMuted,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        val whatsappInstalled = vm.installedWhatsapp() != null
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (!whatsappInstalled) {
                Text(
                    text = stringResource(R.string.result_no_whatsapp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShTextMuted,
                )
                Text(
                    text = stringResource(R.string.result_no_whatsapp_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShTextMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            StatusHdButton(
                text = stringResource(R.string.result_share),
                enabled = whatsappInstalled,
                onClick = {
                    val intent = vm.buildShareIntent(currentJob)
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                            vm.showToast(
                                context.getString(R.string.result_open_whatsapp),
                            )
                        } catch (_: Exception) {
                            vm.showToast(context.getString(R.string.common_error))
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusHdSecondaryButton(
                text = stringResource(R.string.result_save),
                onClick = {
                    scope.launch {
                        val uri = vm.saveToGallery(currentJob)
                        vm.showToast(
                            if (uri != null) {
                                context.getString(R.string.result_saved)
                            } else {
                                context.getString(R.string.common_error)
                            },
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusHdSecondaryButton(
                text = stringResource(R.string.result_send_number),
                onClick = { showNumberDialog = true },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showNumberDialog) {
        AlertDialog(
            onDismissRequest = { showNumberDialog = false },
            title = { Text(text = stringResource(R.string.result_number_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.result_number_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShTextMuted,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = number,
                        onValueChange = {
                            number = it
                            numberError = false
                        },
                        placeholder = { Text(text = stringResource(R.string.result_number_hint)) },
                        singleLine = true,
                        isError = numberError,
                        supportingText = if (numberError) {
                            {
                                Text(text = stringResource(R.string.result_number_invalid))
                            }
                        } else {
                            null
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = vm.buildMyNumberIntent(number)
                    if (intent == null) {
                        numberError = true
                        return@TextButton
                    }
                    vm.setWhatsappNumber(number)
                    showNumberDialog = false
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        vm.showToast(context.getString(R.string.common_error))
                    }
                }) {
                    Text(text = stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNumberDialog = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** Pratonton foto asal (mod foto, togol "Sebelum"). */
@Composable
private fun PhotoPreview(uri: String) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(uri) {
        bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                    ?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
