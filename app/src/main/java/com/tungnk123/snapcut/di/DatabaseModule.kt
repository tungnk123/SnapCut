package com.tungnk123.snapcut.di

import android.content.Context
import androidx.room.Room
import com.tungnk123.snapcut.data.local.AppDatabase
import com.tungnk123.snapcut.data.local.CutSubjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "snapcut_database"
    ).build()

    @Provides
    fun provideCutSubjectDao(database: AppDatabase): CutSubjectDao =
        database.cutSubjectDao()
}
