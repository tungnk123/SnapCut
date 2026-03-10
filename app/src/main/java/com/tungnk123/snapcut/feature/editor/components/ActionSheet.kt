package com.tungnk123.snapcut.feature.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Subject lifted! What do you want to do?")
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") },
                    label = "Copy",
                    onClick = onCopy
                )
                ActionButton(
                    icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                    label = "Share",
                    onClick = onShare
                )
                ActionButton(
                    icon = { Icon(Icons.Default.Download, contentDescription = "Save") },
                    label = "Save",
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label)
        }
    }
}
