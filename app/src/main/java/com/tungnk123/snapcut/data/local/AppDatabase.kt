package com.tungnk123.snapcut.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CutSubjectEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cutSubjectDao(): CutSubjectDao
}
