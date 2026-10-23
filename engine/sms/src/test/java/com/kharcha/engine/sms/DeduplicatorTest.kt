package com.kharcha.engine.sms

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

class DeduplicatorTest {
    private lateinit var deduplicator: Deduplicator
    private lateinit var repository: SmsRepository

    @Before
    fun setUp() {
        repository = mockk()
        deduplicator = Deduplicator(repository)
    }

    @Test
    fun generateHashproducescorrectSHATwoFiftySixhash() {
        val sender = "VM-SBIUPI"
        val body = "Rs 100 spent"
        val timestamp = 1634567890123L
        val expectedInput = "$sender|$body|$timestamp"
        val expectedHash =
            MessageDigest.getInstance("SHA-256")
                .digest(expectedInput.toByteArray())
                .joinToString("") { "%02x".format(it) }

        val actualHash = deduplicator.generateHash(sender, body, timestamp)

        assertEquals(expectedHash, actualHash)
    }

    @Test
    fun isDuplicatereturnstrueifhashexistsinrepository() =
        runTest {
            val hash = deduplicator.generateHash("Sender", "Body", 1L)
            coEvery { repository.isDuplicate(hash) } returns true

            val result = deduplicator.isDuplicate("Sender", "Body", 1L)

            assertTrue(result)
        }

    @Test
    fun isDuplicatereturnsfalseifhashdoesnotexistinrepository() =
        runTest {
            val hash = deduplicator.generateHash("Sender", "Body", 1L)
            coEvery { repository.isDuplicate(hash) } returns false

            val result = deduplicator.isDuplicate("Sender", "Body", 1L)

            assertFalse(result)
        }
}
