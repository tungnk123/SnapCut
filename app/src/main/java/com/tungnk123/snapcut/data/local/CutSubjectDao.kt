package com.tungnk123.snapcut.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CutSubjectDao {

    @Query("SELECT * FROM cut_subjects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CutSubjectEntity>>

    // @Upsert preferred over @Insert(onConflict=REPLACE) — handles insert+update atomically (Room 2.5+)
    @Upsert
    suspend fun upsert(entity: CutSubjectEntity): Long

    @Query("DELETE FROM cut_subjects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cut_subjects")
    suspend fun deleteAll()
}
