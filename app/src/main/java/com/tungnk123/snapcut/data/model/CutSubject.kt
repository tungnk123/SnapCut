package com.tungnk123.snapcut.data.model

import android.net.Uri

data class CutSubject(
    val id: Long = 0,
    val sourceImageUri: Uri,
    val cutImagePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
