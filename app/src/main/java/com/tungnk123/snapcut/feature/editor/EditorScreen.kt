package com.tungnk123.snapcut.feature.editor

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tungnk123.snapcut.feature.editor.components.ActionSheet
import com.tungnk123.snapcut.feature.editor.components.SubjectLiftCanvas

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditorScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onEditSticker: (stickerId: Long, path: String) -> Unit = { _, _ -> },
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(imageUri) {
        viewModel.loadImage(imageUri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.ShowMessage ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is EditorEvent.SubjectSaved -> Unit
                is EditorEvent.SubjectShared -> context.startActivity(
                    android.content.Intent.createChooser(
                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "image/webp"
                            putExtra(android.content.Intent.EXTRA_STREAM, event.uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Share sticker"
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is EditorUiState.Idle -> Unit

            is EditorUiState.ImageLoaded -> {
                SubjectLiftCanvas(
                    sourceBitmap = state.sourceBitmap,
                    subjectBitmap = null,
                    isLifted = false,
                    onLongPress = { x, y -> viewModel.onLongPressDetected(x, y) },
                    onTap = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            is EditorUiState.Segmenting -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        // Layered circles with loading indicator in front
                        Box(modifier = Modifier.size(160.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(88.dp).align(Alignment.BottomStart),
                            ) {}
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(88.dp).align(Alignment.BottomEnd),
                            ) {}
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(112.dp).align(Alignment.TopCenter),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ContainedLoadingIndicator(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerShape = CircleShape,
                                    )
                                }
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Lifting subject",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Analyzing with ML Kit…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            is EditorUiState.SubjectLifted -> {
                SubjectLiftCanvas(
                    sourceBitmap = state.sourceBitmap,
                    subjectBitmap = state.rawSubjectBitmap,
                    outlinePath = state.outlinePath.asComposePath(),
                    isLifted = state.isLifted,
                    onLongPress = { _, _ -> },
                    onTap = {
                        if (state.savedStickerId != -1L) {
                            onEditSticker(state.savedStickerId, state.savedStickerPath)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            is EditorUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
