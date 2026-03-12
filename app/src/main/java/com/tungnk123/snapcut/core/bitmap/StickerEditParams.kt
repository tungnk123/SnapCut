package com.tungnk123.snapcut.core.bitmap

data class StickerEditParams(
    val outlineEnabled: Boolean = true,
    val outlineColor: Int = android.graphics.Color.WHITE,
    val outlineWidth: Int = 32,
    val tintEnabled: Boolean = false,
    val tintColor: Int = android.graphics.Color.parseColor("#9C27B0"),
    val tintAlpha: Int = 120,
    val shadowEnabled: Boolean = false,
    val shadowRadius: Float = 28f,
    val shadowDx: Float = 6f,
    val shadowDy: Float = 12f,
)
