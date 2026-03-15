package com.tungnk123.snapcut.feature.keyboard

<<<<<<< HEAD
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
=======
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
>>>>>>> c2d12f9 (feat: Add StickerKeyboardService and support sharing stickers as WEBP images)
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
<<<<<<< HEAD
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
=======
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
>>>>>>> c2d12f9 (feat: Add StickerKeyboardService and support sharing stickers as WEBP images)
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
<<<<<<< HEAD
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
=======
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
>>>>>>> c2d12f9 (feat: Add StickerKeyboardService and support sharing stickers as WEBP images)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
<<<<<<< HEAD
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
=======

        // Set lifecycle owners on the window's decor view so ComposeView
        // can find them when walking up the view tree during attachment.
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
>>>>>>> c2d12f9 (feat: Add StickerKeyboardService and support sharing stickers as WEBP images)
        }

        return ComposeView(this).apply {
            setContent {
                SnapCutTheme {
<<<<<<< HEAD
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
=======
                    StickerKeyboardContent(
                        stickerRepository = stickerRepository,
                        onStickerClick = ::sendSticker,
                        onSwitchKeyboard = ::switchKeyboard,
                    )
>>>>>>> c2d12f9 (feat: Add StickerKeyboardService and support sharing stickers as WEBP images)
                }
            }
        }
    }

<<<<<<< HEAD
=======
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

>>>>>>> c2d12f9 (feat: Add StickerKeyboardService and support sharing stickers as WEBP images)
    private fun sendSticker(sticker: CutSubject) {
        val file = File(sticker.cutImagePath)
        if (!file.exists()) return

<<<<<<< HEAD
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
=======
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
>>>>>>> c2d12f9 (feat: Add StickerKeyboardService and support sharing stickers as WEBP images)
