package com.statushd.app.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.statushd.app.R
import com.statushd.app.data.local.CompressionJobEntity
import com.statushd.app.data.local.Settings
import com.statushd.app.ui.components.EmptyState
import com.statushd.app.ui.components.ModeCard
import com.statushd.app.ui.navigation.PickerMode
import com.statushd.app.ui.theme.Accent
import com.statushd.app.ui.theme.ShTextMuted
import com.statushd.app.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** S01 — Home: kad mod + kerja terkini + notis privasi (S02). */
@Composable
fun HomeScreen(
    vm: AppViewModel,
    settings: Settings,
    recentJobs: List<CompressionJobEntity>,
    onPick: (PickerMode) -> Unit,
    onOpenResult: (String, PickerMode) -> Unit,
    onOpenSupport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var showPrivacy by remember { mutableStateOf(!settings.privacySeen) }
    var showPrivacyFull by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
    ) {
        // Header melekit: wordmark + tetapan.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Status")
                    withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Bold)) {
                        append("HD")
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                )
            }
        }

        Text(
            text = stringResource(R.string.home_hero),
            style = MaterialTheme.typography.bodyLarge,
            color = ShTextMuted,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Kad mod 2x2.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(
                icon = Icons.Default.Videocam,
                title = stringResource(R.string.home_mode_video),
                subtitle = stringResource(R.string.home_mode_video_sub),
                onClick = { onPick(PickerMode.VIDEO) },
                modifier = Modifier.weight(1f),
            )
            ModeCard(
                icon = Icons.Default.Image,
                title = stringResource(R.string.home_mode_photo),
                subtitle = stringResource(R.string.home_mode_photo_sub),
                onClick = { onPick(PickerMode.PHOTO) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(
                icon = Icons.Default.ContentCut,
                title = stringResource(R.string.home_mode_splitter),
                subtitle = stringResource(R.string.home_mode_splitter_sub),
                onClick = { onPick(PickerMode.SPLIT) },
                modifier = Modifier.weight(1f),
            )
            ModeCard(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.home_mode_ai),
                subtitle = stringResource(R.string.home_mode_ai_sub),
                badge = stringResource(R.string.home_mode_soon),
                onClick = { vm.showToast(stringResource(R.string.home_mode_soon)) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_recent),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (recentJobs.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.home_recent),
                subtitle = stringResource(R.string.home_footer),
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recentJobs) { job ->
                    RecentJobThumb(
                        job = job,
                        onClick = {
                            val mode = if (job.clipLengthMs > 0) {
                                PickerMode.SPLIT
                            } else {
                                PickerMode.VIDEO
                            }
                            onOpenResult(job.id, mode)
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_footer),
            style = MaterialTheme.typography.bodyMedium,
            color = ShTextMuted,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.support_title),
            style = MaterialTheme.typography.bodyMedium,
            color = Accent,
            modifier = Modifier.clickable(onClick = onOpenSupport),
        )
        Spacer(modifier = Modifier.height(32.dp))
    }

    // S02 — notis privasi (bukan halangan; boleh ditutup).
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = {
                showPrivacy = false
                vm.setPrivacySeen()
            },
            title = { Text(text = stringResource(R.string.privacy_title)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.privacy_line1))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_line2))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_line3))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPrivacy = false
                    vm.setPrivacySeen()
                }) {
                    Text(text = stringResource(R.string.common_understand))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyFull = true }) {
                    Text(text = stringResource(R.string.privacy_read))
                }
            },
        )
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
private fun RecentJobThumb(job: CompressionJobEntity, onClick: () -> Unit) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(job.outputUri) {
        bitmap = withContext(Dispatchers.IO) {
            val path = job.outputUri ?: return@withContext null
            try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(path)
                    retriever.getFrameAtTime(
                        1_000_000,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    )
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(
        modifier = Modifier
            .size(96.dp, 160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(com.statushd.app.ui.theme.ShSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = ShTextMuted,
            )
        }
    }
}
