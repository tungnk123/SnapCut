package com.tungnk123.snapcut.di

import android.content.Context
import com.tungnk123.snapcut.core.bitmap.BitmapProcessor
import com.tungnk123.snapcut.core.ml.SubjectSegmentationManager
import com.tungnk123.snapcut.data.repository.StickerRepository
import com.tungnk123.snapcut.data.repository.StickerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

// @Binds is more efficient than @Provides for interface-impl bindings
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStickerRepository(
        impl: StickerRepositoryImpl
    ): StickerRepository
}

// @Provides for objects we can't annotate with @Inject directly
@Module
@InstallIn(SingletonComponent::class)
object AppProviderModule {

    @Provides
    @Singleton
    fun provideSubjectSegmentationManager(): SubjectSegmentationManager =
        SubjectSegmentationManager()

    @Provides
    @Singleton
    fun provideBitmapProcessor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): BitmapProcessor = BitmapProcessor(context, ioDispatcher)
}
