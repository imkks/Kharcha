package com.kharcha.feature.onboarding

import android.content.Context
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.model.RawSms
import com.kharcha.engine.sms.Deduplicator
import com.kharcha.engine.sms.InboxReader
import com.kharcha.engine.sms.SenderFilter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val inboxReader: InboxReader = mockk()
    private val senderFilter: SenderFilter = mockk()
    private val deduplicator: Deduplicator = mockk()
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private lateinit var tempDir: File

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tempDir = File(System.getProperty("java.io.tmpdir"), "onboarding_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        every { context.filesDir } returns tempDir
        every { context.applicationContext } returns context
        viewModel =
            OnboardingViewModel(
                inboxReader,
                senderFilter,
                deduplicator,
                transactionRepository,
                context,
                testDispatcher,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    @Test
    fun initialStateIsWelcome() {
        assertEquals(OnboardingState.WELCOME, viewModel.uiState.value.currentState)
    }

    @Test
    fun onPermissionGrantedStartsImport() =
        runTest(testDispatcher) {
            coEvery { inboxReader.getSmsCount(any()) } returns 0
            viewModel.onPermissionResult(true)
            advanceUntilIdle()
            assertEquals(OnboardingState.IMPORT_PROGRESS, viewModel.uiState.value.currentState)
            assertEquals(false, viewModel.uiState.value.isPermissionDenied)
            assertTrue(viewModel.uiState.value.isImportComplete)
        }

    @Test
    fun onPermissionDeniedSetsFlag() {
        viewModel.onPermissionResult(false)
        assertEquals(true, viewModel.uiState.value.isPermissionDenied)
    }

    @Test
    fun startImportTransitionsToImportProgressAndCompletes() =
        runTest(testDispatcher) {
            coEvery { inboxReader.getSmsCount(any()) } returns 0

            viewModel.startImport("30 days", false)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(OnboardingState.IMPORT_PROGRESS, state.currentState)
            assertTrue(state.isImportComplete)
            assertEquals(1.0f, state.progress, 0.01f)
        }
}
