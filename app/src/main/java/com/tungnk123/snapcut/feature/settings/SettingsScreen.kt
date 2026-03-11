package com.tungnk123.snapcut.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tungnk123.snapcut.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var autoSaveToGallery by rememberSaveable { mutableStateOf(true) }
    var highQualityExport by rememberSaveable { mutableStateOf(true) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot events via Channel — never use SharedFlow(replay=0) for events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.StickersCleared -> snackbarHostState.showSnackbar("Sticker history cleared")
                SettingsEvent.ClearFailed -> snackbarHostState.showSnackbar("Failed to clear history")
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Sticker History") },
            text = { Text("This will permanently delete all saved stickers. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showClearDialog = false; viewModel.clearAllStickers() }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionHeader("Export")

            ListItem(
                headlineContent = { Text("Auto-save to Gallery") },
                supportingContent = { Text("Save cut subjects automatically after export") },
                leadingContent = { Icon(Icons.Outlined.ColorLens, contentDescription = null) },
                trailingContent = {
                    Switch(checked = autoSaveToGallery, onCheckedChange = { autoSaveToGallery = it })
                }
            )

            ListItem(
                headlineContent = { Text("High Quality Export") },
                supportingContent = { Text("Export at full resolution (uses more storage)") },
                leadingContent = { Icon(Icons.Outlined.Share, contentDescription = null) },
                trailingContent = {
                    Switch(checked = highQualityExport, onCheckedChange = { highQualityExport = it })
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader("Storage")

            ListItem(
                headlineContent = { Text("Clear Sticker History") },
                supportingContent = { Text("Delete all saved stickers from local storage") },
                leadingContent = { Icon(Icons.Outlined.AutoDelete, contentDescription = null) },
                modifier = Modifier.clickable { showClearDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader("About")

            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
