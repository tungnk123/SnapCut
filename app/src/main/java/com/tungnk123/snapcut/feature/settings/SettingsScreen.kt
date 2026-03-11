package com.tungnk123.snapcut.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Export section
            SettingsSection(title = "Export") {
                SettingsToggleItem(
                    icon = Icons.Outlined.PhotoLibrary,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "Auto-save to Gallery",
                    subtitle = "Save cut subjects automatically after lifting",
                    checked = autoSaveToGallery,
                    onCheckedChange = { autoSaveToGallery = it },
                )
                SettingsToggleItem(
                    icon = Icons.Outlined.HighQuality,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = "High Quality Export",
                    subtitle = "Export at full resolution (uses more storage)",
                    checked = highQualityExport,
                    onCheckedChange = { highQualityExport = it },
                    showDivider = false,
                )
            }

            // Storage section
            SettingsSection(title = "Storage") {
                SettingsClickItem(
                    icon = Icons.Outlined.AutoDelete,
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    title = "Clear Sticker History",
                    subtitle = "Delete all saved stickers from local storage",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true },
                    showDivider = false,
                )
            }

            // About section
            SettingsSection(title = "About") {
                SettingsInfoItem(
                    icon = Icons.Outlined.Tag,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME,
                )
                SettingsInfoItem(
                    icon = Icons.Outlined.Info,
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "SnapCut",
                    subtitle = "Lift any subject from a photo",
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsIconContainer(
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = Modifier.size(36.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true,
) {
    ListItem(
        leadingContent = { SettingsIconContainer(icon, iconContainerColor, iconTint) },
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    if (showDivider) {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showDivider: Boolean = true,
) {
    ListItem(
        leadingContent = { SettingsIconContainer(icon, iconContainerColor, iconTint) },
        headlineContent = { Text(title, fontWeight = FontWeight.Medium, color = titleColor) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
    if (showDivider) {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    showDivider: Boolean = true,
) {
    ListItem(
        leadingContent = { SettingsIconContainer(icon, iconContainerColor, iconTint) },
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    if (showDivider) {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
