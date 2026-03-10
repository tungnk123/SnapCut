package com.tungnk123.snapcut.feature.sticker

import app.cash.turbine.test
import com.tungnk123.snapcut.data.model.CutSubject
import com.tungnk123.snapcut.data.repository.StickerRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StickerViewModelTest {

    private val repository: StickerRepository = mockk()
    private lateinit var viewModel: StickerViewModel

    @Before
    fun setup() {
        every { repository.observeHistory() } returns flowOf(emptyList())
        viewModel = StickerViewModel(repository)
    }

    @Test
    fun `emits Empty state when no stickers in history`() = runTest {
        viewModel.uiState.test {
            assertEquals(StickerUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Success state with stickers`() = runTest {
        val sticker = mockk<CutSubject>(relaxed = true)
        every { repository.observeHistory() } returns flowOf(listOf(sticker))
        val vm = StickerViewModel(repository)

        vm.uiState.test {
            val state = awaitItem()
            assert(state is StickerUiState.Success)
            assertEquals(1, (state as StickerUiState.Success).stickers.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteSticker calls repository`() = runTest {
        coEvery { repository.deleteCutSubject(any()) } returns Result.success(Unit)

        viewModel.deleteSticker(42L)

        coVerify { repository.deleteCutSubject(42L) }
    }
}
