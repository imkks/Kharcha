package com.kharcha.core.common

import android.content.Context
import android.content.SharedPreferences
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BankAccountMergeStoreTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val prefStorage = mutableMapOf<String, Any>()
    private lateinit var store: BankAccountMergeStore

    @Before
    fun setUp() {
        context = mockk()
        prefs = mockk()
        editor = mockk()

        prefStorage.clear()

        every { context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE) } returns prefs

        every { prefs.getStringSet(any(), any()) } answers {
            val key = firstArg<String>()
            val defaultVal = secondArg<Set<String>>()
            @Suppress("UNCHECKED_CAST")
            (prefStorage[key] as? Set<String>) ?: defaultVal
        }

        every { prefs.edit() } returns editor
        every { editor.putStringSet("bank_account_merges", any()) } answers {
            prefStorage["bank_account_merges"] = secondArg<Set<String>>()
            editor
        }
        every { editor.remove(any()) } answers {
            prefStorage.remove(firstArg())
            editor
        }
        every { editor.commit() } returns true
        every { editor.apply() } answers { }

        store = BankAccountMergeStore(context)
    }

    @Test
    fun isShortMaskPair_validatesCorrectly() {
        assertTrue(BankAccountMergeStore.isShortMaskPair("321", "2321"))
        assertTrue(BankAccountMergeStore.isShortMaskPair("001", "9001"))
        assertFalse(BankAccountMergeStore.isShortMaskPair("321", "1234"))
        assertFalse(BankAccountMergeStore.isShortMaskPair("32", "2321"))
        assertFalse(BankAccountMergeStore.isShortMaskPair("1321", "2321"))
    }

    @Test
    fun rememberAndResolve_mapsShortMaskToFull() {
        store.remember(bank = "HDFC", short = "321", full = "2321")

        val resolved = store.resolveSuffix(bank = "HDFC", suffix = "321")
        assertEquals("2321", resolved)

        val unmapped = store.resolveSuffix(bank = "HDFC", suffix = "999")
        assertEquals("999", unmapped)

        val otherBank = store.resolveSuffix(bank = "SBI", suffix = "321")
        assertEquals("321", otherBank)
    }

    @Test
    fun duplicatePairs_identifiesCandidates() {
        val accShort = Account(
            id = 1L,
            name = "HDFC Bank ••321",
            bankName = "HDFC",
            last4 = "321",
            type = AccountType.Savings,
        )
        val accFull = Account(
            id = 2L,
            name = "HDFC Bank ••2321",
            bankName = "HDFC",
            last4 = "2321",
            type = AccountType.Savings,
        )
        val accUnrelated = Account(
            id = 3L,
            name = "SBI Bank ••9999",
            bankName = "SBI",
            last4 = "9999",
            type = AccountType.Savings,
        )

        val duplicates = BankAccountMergeStore.duplicatePairs(listOf(accShort, accFull, accUnrelated))
        assertEquals(1, duplicates.size)
        assertEquals(1L, duplicates[0].first.id)
        assertEquals(2L, duplicates[0].second.id)
    }

    @Test
    fun onMerge_updatesStore() {
        val source = Account(
            id = 1L,
            name = "HDFC Bank ••321",
            bankName = "HDFC",
            last4 = "321",
            type = AccountType.Savings,
        )
        val target = Account(
            id = 2L,
            name = "HDFC Bank ••2321",
            bankName = "HDFC",
            last4 = "2321",
            type = AccountType.Savings,
        )

        store.onMerge(source, target)
        assertEquals("2321", store.resolveSuffix("HDFC", "INR", "321"))
    }
}
