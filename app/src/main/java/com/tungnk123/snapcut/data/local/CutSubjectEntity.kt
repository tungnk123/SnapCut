package com.tungnk123.snapcut.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tungnk123.snapcut.data.model.CutSubject
import android.net.Uri

@Entity(tableName = "cut_subjects")
data class CutSubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceImageUri: String,
    val cutImagePath: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CutSubject = CutSubject(
        id = id,
        sourceImageUri = Uri.parse(sourceImageUri),
        cutImagePath = cutImagePath,
        createdAt = createdAt
    )
}

fun CutSubject.toEntity(): CutSubjectEntity = CutSubjectEntity(
    id = id,
    sourceImageUri = sourceImageUri.toString(),
    cutImagePath = cutImagePath,
    createdAt = createdAt
)
