package com.tungnk123.snapcut.feature.keyboard

import android.content.ClipDescription
import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.scale
import java.io.FileOutputStream

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StickerKeyboardEntryPoint {
    fun stickerRepository(): StickerRepository
}

private const val STICKER_MIME_TYPE = "image/png"
private const val STICKER_CACHE_PREFIX = "sticker_send_"
private const val STICKER_MAX_DIM = 512
private const val GRID_COLUMNS = 4
private const val GRID_HEIGHT_DP = 280
private const val GRID_PADDING_DP = 8
private const val ITEM_SIZE_DP = 80
private const val ITEM_PADDING_DP = 4

class StickerKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val stickers = mutableStateListOf<CutSubject>()

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val repository = EntryPointAccessors.fromApplication(
            applicationContext,
            StickerKeyboardEntryPoint::class.java
        ).stickerRepository()

        serviceScope.launch {
            repository.observeHistory().collect { list ->
                stickers.clear()
                stickers.addAll(list)
            }
        }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setContent {
                SnapCutTheme {
                    Surface {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(GRID_COLUMNS),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(GRID_HEIGHT_DP.dp)
                                .padding(GRID_PADDING_DP.dp)
                        ) {
                            items(stickers, key = { it.id }) { sticker ->
                                val checkLight = MaterialTheme.colorScheme.surfaceContainerLow
                                val checkDark = MaterialTheme.colorScheme.surfaceContainer
                                AsyncImage(
                                    model = File(sticker.cutImagePath),
                                    contentDescription = "Sticker",
                                    modifier = Modifier
                                        .size(ITEM_SIZE_DP.dp)
                                        .clickable { sendSticker(sticker) }
                                        .padding(ITEM_PADDING_DP.dp)
                                        .checkerboard(checkLight, checkDark, 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendSticker(sticker: CutSubject) {
        val file = File(sticker.cutImagePath)
        if (!file.exists()) return

        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo ?: return

        serviceScope.launch {
            val sendFile = withContext(Dispatchers.IO) { scaledStickerFile(file) } ?: file
            val uri = FileProvider.getUriForFile(
                this@StickerKeyboardService,
                "${packageName}.fileprovider",
                sendFile
            )
            InputConnectionCompat.commitContent(
                ic,
                editorInfo,
                InputContentInfoCompat(
                    uri,
                    ClipDescription("sticker", arrayOf(STICKER_MIME_TYPE)),
                    null
                ),
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                null
            )
        }
    }

    private fun scaledStickerFile(source: File): File? {
        val original = android.graphics.BitmapFactory.decodeFile(source.absolutePath)
            ?: return null
        return runCatching {
            val scaled = if (original.width <= STICKER_MAX_DIM && original.height <= STICKER_MAX_DIM) {
                original
            } else {
                val ratio = STICKER_MAX_DIM.toFloat() / maxOf(original.width, original.height)
                original.scale((original.width * ratio).toInt(), (original.height * ratio).toInt())
            }
            val cacheFile = File(cacheDir, "$STICKER_CACHE_PREFIX${source.nameWithoutExtension}.png")
            FileOutputStream(cacheFile).use {
                scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            if (scaled !== original) scaled.recycle()
            cacheFile
        }.also {
            original.recycle()
        }.getOrNull()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        super.onDestroy()
    }
}

private fun Modifier.checkerboard(color1: Color, color2: Color, tileSize: Dp): Modifier =
    drawBehind {
        val tilePx = tileSize.toPx()
        val cols = (size.width / tilePx).toInt() + 1
        val rows = (size.height / tilePx).toInt() + 1
        for (row in 0..rows) {
            for (col in 0..cols) {
                drawRect(
                    color = if ((row + col) % 2 == 0) color1 else color2,
                    topLeft = Offset(col * tilePx, row * tilePx),
                    size = Size(tilePx, tilePx),
                )
            }
        }
    }
