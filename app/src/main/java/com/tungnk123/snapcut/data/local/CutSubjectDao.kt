package com.tungnk123.snapcut.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CutSubjectDao {

    @Query("SELECT * FROM cut_subjects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CutSubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CutSubjectEntity): Long

    @Query("DELETE FROM cut_subjects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cut_subjects")
    suspend fun deleteAll()
}
