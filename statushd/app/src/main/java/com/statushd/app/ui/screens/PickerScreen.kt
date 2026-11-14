package com.statushd.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.statushd.app.R
import com.statushd.app.data.media.MediaItem
import com.statushd.app.media.FfmpegCommandBuilder
import com.statushd.app.ui.components.EmptyState
import com.statushd.app.ui.components.StatusHdButton
import com.statushd.app.ui.components.StatusHdTopBar
import com.statushd.app.ui.navigation.PickerMode
import com.statushd.app.ui.theme.ShSurface
import com.statushd.app.ui.theme.ShTextMuted
import com.statushd.app.viewmodel.AppViewModel

/** S03 — Pemilih media: grid galeri, carian, tapis, keadaan izin. */
@Composable
fun PickerScreen(
    vm: AppViewModel,
    mode: PickerMode,
    onBack: () -> Unit,
    onPick: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    val mediaList by vm.mediaList.collectAsState()
    val loading by vm.mediaLoading.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") } // all | video | photo

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var hasPermission by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
            },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        hasPermission = granted.values.all { it }
        if (hasPermission) vm.loadMedia()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) vm.loadMedia()
    }

    val filtered = remember(mediaList, query, filter, mode) {
        mediaList.filter { item ->
            val modeOk = when (mode) {
                PickerMode.PHOTO -> item.type == "photo"
                else -> item.type == "video"
            }
            val filterOk = when (filter) {
                "video" -> item.type == "video"
                "photo" -> item.type == "photo"
                else -> true
            }
            val queryOk = query.isBlank() ||
                item.displayName.contains(query, ignoreCase = true)
            modeOk && filterOk && queryOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StatusHdTopBar(
            title = stringResource(
                if (mode == PickerMode.PHOTO) {
                    R.string.picker_title_photo
                } else {
                    R.string.picker_title_video
                },
            ),
            onBack = onBack,
        )

        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.picker_denied),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.picker_denied_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShTextMuted,
                )
                Spacer(modifier = Modifier.height(20.dp))
                StatusHdButton(
                    text = stringResource(R.string.picker_allow),
                    onClick = { permissionLauncher.launch(permissions) },
                )
            }
            return
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(text = stringResource(R.string.picker_search)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filter == "all",
                onClick = { filter = "all" },
                label = { Text(text = stringResource(R.string.picker_all)) },
            )
            FilterChip(
                selected = filter == "video",
                onClick = { filter = "video" },
                label = { Text(text = stringResource(R.string.picker_videos)) },
            )
            FilterChip(
                selected = filter == "photo",
                onClick = { filter = "photo" },
                label = { Text(text = stringResource(R.string.picker_photos)) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.common_loading))
            }
        } else if (filtered.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.picker_empty),
                subtitle = stringResource(R.string.picker_empty_sub),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filtered, key = { it.id }) { item ->
                    MediaGridCell(item = item, vm = vm, onClick = { onPick(item) })
                }
            }
        }
    }
}

@Composable
private fun MediaGridCell(item: MediaItem, vm: AppViewModel, onClick: () -> Unit) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.id) {
        bitmap = vm.loadThumbnail(item)
    }
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(8.dp))
            .background(ShSurface)
            .clickable(onClick = onClick),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = if (item.type == "video") {
                    Icons.Default.PlayArrow
                } else {
                    Icons.Default.Image
                },
                contentDescription = null,
                tint = ShTextMuted,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center),
            )
        }
        if (item.type == "video" && item.durationMs > 0) {
            Text(
                text = FfmpegCommandBuilder.formatDuration(item.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
