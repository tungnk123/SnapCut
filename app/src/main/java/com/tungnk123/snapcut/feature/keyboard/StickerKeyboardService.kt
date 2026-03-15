package com.tungnk123.snapcut.feature.keyboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.tungnk123.snapcut.data.model.CutSubject
import com.tungnk123.snapcut.data.repository.StickerRepository
import com.tungnk123.snapcut.ui.theme.SnapCutTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

class StickerKeyboardService :
    InputMethodService(),
    androidx.lifecycle.LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var stickerRepository: StickerRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface KeyboardEntryPoint {
        fun stickerRepository(): StickerRepository
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            KeyboardEntryPoint::class.java
        )
        stickerRepository = entryPoint.stickerRepository()
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        // Set lifecycle owners on the window's decor view so ComposeView
        // can find them when walking up the view tree during attachment.
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setContent {
                SnapCutTheme {
                    StickerKeyboardContent(
                        stickerRepository = stickerRepository,
                        onStickerClick = ::sendSticker,
                        onSwitchKeyboard = ::switchKeyboard,
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    private fun sendSticker(sticker: CutSubject) {
        val file = File(sticker.cutImagePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        val mimeType = if (file.extension == "webp") "image/webp" else "image/png"

        // Check if the target app supports image content via keyboard
        val supportedMimes = EditorInfoCompat.getContentMimeTypes(currentInputEditorInfo)
        val isImageSupported = supportedMimes.any {
            ClipDescription.compareMimeTypes(it, "image/*")
        }

        if (isImageSupported && currentInputConnection != null) {
            // Send via commitContent — app receives it as inline sticker
            val description = ClipDescription("Sticker", arrayOf(mimeType))
            val inputContentInfo = InputContentInfoCompat(uri, description, null)

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            } else {
                0
            }

            InputConnectionCompat.commitContent(
                currentInputConnection,
                currentInputEditorInfo,
                inputContentInfo,
                flags,
                null
            )
        } else {
            // Fallback: copy to clipboard so user can paste manually
            val clip = ClipData.newUri(contentResolver, "Sticker", uri)
            getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
        }
    }

    @Suppress("DEPRECATION")
    private fun switchKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToPreviousInputMethod()
        } else {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val token = window?.window?.attributes?.token
            imm.switchToLastInputMethod(token)
        }
    }
}

@Composable
private fun StickerKeyboardContent(
    stickerRepository: StickerRepository,
    onStickerClick: (CutSubject) -> Unit,
    onSwitchKeyboard: () -> Unit,
) {
    val stickers by stickerRepository.observeHistory()
        .collectAsState(initial = emptyList())

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "SnapCut Stickers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
                IconButton(
                    onClick = onSwitchKeyboard,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Switch keyboard",
                    )
                }
            }

            if (stickers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No stickers yet\nCreate stickers in SnapCut app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(stickers, key = { it.id }) { sticker ->
                        AsyncImage(
                            model = File(sticker.cutImagePath),
                            contentDescription = "Sticker",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onStickerClick(sticker) }
                                .padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}
