package com.tungnk123.snapcut.data.repository

import android.net.Uri
import com.tungnk123.snapcut.data.local.CutSubjectDao
import com.tungnk123.snapcut.data.local.CutSubjectEntity
import com.tungnk123.snapcut.data.model.CutSubject
import com.tungnk123.snapcut.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StickerRepositoryImpl @Inject constructor(
    private val dao: CutSubjectDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : StickerRepository {

    // Room as single source of truth — expose Flow from DAO for reactive UI
    override fun observeHistory(): Flow<List<CutSubject>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveCutSubject(
        sourceUri: Uri,
        cutImagePath: String
    ): Result<CutSubject> = withContext(ioDispatcher) {
        runCatching {
            val entity = CutSubjectEntity(
                sourceImageUri = sourceUri.toString(),
                cutImagePath = cutImagePath
            )
            val id = dao.upsert(entity)
            entity.copy(id = id).toDomain()
        }
    }

    override suspend fun deleteCutSubject(id: Long): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { dao.deleteById(id) }
        }

    override suspend fun deleteAll(): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { dao.deleteAll() }
        }
}
