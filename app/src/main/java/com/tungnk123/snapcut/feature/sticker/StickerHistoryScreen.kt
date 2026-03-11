package com.tungnk123.snapcut.feature.sticker

import android.content.Intent
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tungnk123.snapcut.data.model.CutSubject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerHistoryScreen(
    viewModel: StickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedSticker by remember { mutableStateOf<CutSubject?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StickerEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is StickerEvent.Share -> {
                    selectedSticker = null
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share sticker"))
                }
            }
        }
    }

    selectedSticker?.let { sticker ->
        ModalBottomSheet(
            onDismissRequest = { selectedSticker = null },
            sheetState = bottomSheetState,
        ) {
            StickerActionSheet(
                onShare = { viewModel.shareSticker(sticker) },
                onCopy = { viewModel.copyToClipboard(sticker); selectedSticker = null },
                onSaveToGallery = { viewModel.saveToGallery(sticker); selectedSticker = null },
                onDelete = { viewModel.deleteSticker(sticker.id); selectedSticker = null },
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stickers") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is StickerUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            is StickerUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("No stickers yet. Lift a subject to get started!") }
            }

            is StickerUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.stickers, key = { it.id }) { sticker ->
                        // Fix bug 1: aspectRatio(1f) gives a real height so the
                        // IconButton's 48dp min touch target can't cover the whole card.
                        // Fix bug 2: pass File — raw absolute path has no scheme,
                        // Coil can't resolve it; File is handled natively.
                        AsyncImage(
                            model = File(sticker.cutImagePath),
                            contentDescription = "Sticker",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSticker = sticker }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerActionSheet(
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onSaveToGallery: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Sticker options",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        SheetAction(icon = { Icon(Icons.Outlined.Share, null) }, label = "Share", onClick = onShare)
        SheetAction(icon = { Icon(Icons.Outlined.ContentCopy, null) }, label = "Copy to clipboard", onClick = onCopy)
        SheetAction(icon = { Icon(Icons.Outlined.SaveAlt, null) }, label = "Save to Gallery", onClick = onSaveToGallery)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SheetAction(
            icon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            label = "Delete",
            labelColor = MaterialTheme.colorScheme.error,
            onClick = onDelete,
        )
    }
}

@Composable
private fun SheetAction(
    icon: @Composable () -> Unit,
    label: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(24.dp)) { icon() }
            Text(text = label, color = labelColor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
