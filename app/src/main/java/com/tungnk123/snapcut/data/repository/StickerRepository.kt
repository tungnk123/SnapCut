package com.tungnk123.snapcut.data.repository

import android.net.Uri
import com.tungnk123.snapcut.data.model.CutSubject
import kotlinx.coroutines.flow.Flow

interface StickerRepository {
    fun observeHistory(): Flow<List<CutSubject>>
    suspend fun saveCutSubject(sourceUri: Uri, cutImagePath: String): Result<CutSubject>
    suspend fun deleteCutSubject(id: Long): Result<Unit>
    suspend fun deleteAll(): Result<Unit>
}
