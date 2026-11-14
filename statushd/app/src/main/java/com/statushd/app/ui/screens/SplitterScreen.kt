package com.statushd.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.statushd.app.R
import com.statushd.app.data.local.SplitPartEntity
import com.statushd.app.media.FfmpegCommandBuilder
import com.statushd.app.ui.components.EmptyState
import com.statushd.app.ui.components.StatusHdButton
import com.statushd.app.ui.components.StatusHdSecondaryButton
import com.statushd.app.ui.components.StatusHdTopBar
import com.statushd.app.ui.theme.ShSuccess
import com.statushd.app.ui.theme.ShSurface
import com.statushd.app.ui.theme.ShTextMuted
import com.statushd.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch

/**
 * S09 — Pemotong: senarai bahagian 30 saat dengan status setiap satu,
 * kongsi per bahagian, simpan semua ke galeri.
 *
 * Nota v1: WhatsApp tidak menyokong kongsi berbilang video serentak ke
 * Status, jadi "Kongsi semua" berkongsi bahagian pertama dan memberitahu
 * pengguna supaya mengulang mengikut turutan. Lihat build-log.md.
 */
@Composable
fun SplitterScreen(
    vm: AppViewModel,
    jobId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val job by vm.observeJob(jobId).collectAsState(initial = null)
    val parts by vm.observeParts(jobId).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        StatusHdTopBar(
            title = stringResource(R.string.splitter_title),
            onBack = onBack,
        )

        if (parts.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.splitter_no_parts),
                subtitle = stringResource(R.string.common_loading),
            )
            return
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            items(parts, key = { it.id }) { part ->
                PartRow(
                    part = part,
                    onShare = {
                        val currentJob = job ?: return@PartRow
                        val intent = vm.buildShareIntent(
                            currentJob,
                            partPath = part.outputUri,
                        )
                        if (intent != null) {
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                vm.showToast(context.getString(R.string.common_error))
                            }
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            val delivered = parts.filter { it.status == "delivered" }
            StatusHdSecondaryButton(
                text = stringResource(R.string.splitter_save_all),
                enabled = delivered.isNotEmpty(),
                onClick = {
                    scope.launch {
                        val currentJob = job ?: return@launch
                        var saved = 0
                        for (part in delivered) {
                            val uri = vm.saveToGallery(currentJob, part.outputUri)
                            if (uri != null) saved++
                        }
                        vm.showToast(
                            if (saved == delivered.size) {
                                context.getString(R.string.splitter_saved_all)
                            } else {
                                context.getString(R.string.common_error)
                            },
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusHdButton(
                text = stringResource(R.string.splitter_share_all),
                enabled = delivered.isNotEmpty(),
                onClick = {
                    val currentJob = job ?: return@StatusHdButton
                    val first = delivered.firstOrNull() ?: return@StatusHdButton
                    val intent = vm.buildShareIntent(currentJob, part.outputUri)
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
        }
    }
}

@Composable
private fun PartRow(part: SplitPartEntity, onShare: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ShSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (part.status) {
                "delivered" -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ShSuccess,
                )
                "failed" -> Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = com.statushd.app.ui.theme.ShDanger,
                )
                else -> Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = ShTextMuted,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = stringResource(
                        R.string.splitter_part,
                        part.partIndex + 1,
                        FfmpegCommandBuilder.formatDuration(part.startMs),
                        FfmpegCommandBuilder.formatDuration(part.endMs),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = part.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShTextMuted,
                )
            }
            if (part.status == "delivered") {
                IconButton(onClick = onShare) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                }
            }
        }
    }
}
