package com.tungnk123.snapcut.feature.editor

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tungnk123.snapcut.feature.editor.components.ActionSheet
import com.tungnk123.snapcut.feature.editor.components.SubjectLiftCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showActionSheet by remember { mutableStateOf(false) }

    // Load image when screen is first shown
    LaunchedEffect(imageUri) {
        viewModel.loadImage(imageUri)
    }

    // Collect one-shot events (snackbars, navigation triggers)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.ShowMessage ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is EditorEvent.SubjectSaved -> showActionSheet = false
                is EditorEvent.SubjectShared -> showActionSheet = false
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
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Sticker history")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is EditorUiState.Idle -> Unit

            is EditorUiState.ImageLoaded -> {
                SubjectLiftCanvas(
                    sourceBitmap = state.sourceBitmap,
                    subjectBitmap = null,
                    outlinePath = Path(),
                    isLifted = false,
                    onLongPress = { viewModel.onLongPressDetected() },
                    onTap = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            is EditorUiState.Segmenting -> {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is EditorUiState.SubjectLifted -> {
                SubjectLiftCanvas(
                    sourceBitmap = state.sourceBitmap,
                    subjectBitmap = state.subjectBitmap,
                    outlinePath = state.outlinePath.asComposePath(),
                    isLifted = state.isLifted,
                    onLongPress = {},
                    onTap = { showActionSheet = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )

                if (showActionSheet) {
                    ActionSheet(
                        onDismiss = { showActionSheet = false },
                        onCopy = {
                            // Copy transparent PNG to clipboard
                            showActionSheet = false
                        },
                        onShare = {
                            showActionSheet = false
                        },
                        onSave = {
                            viewModel.saveToGallery()
                        }
                    )
                }
            }

            is EditorUiState.Error -> {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message)
                }
            }
        }
    }
}
