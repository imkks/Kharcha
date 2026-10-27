package com.kharcha.core.designsystem.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineMerchantTagTest {
    private val onlineKeywords = listOf(
        "swiggy", "blinkit", "zomato", "amazon", "flipkart", "uber", "ola", "zepto",
        "hunger box", "hungerbox", "bigbasket", "bbdaily", "myntra", "dunzo", "instamart"
    )

    private fun isOnlineMerchant(merchant: String?): Boolean {
        if (merchant == null) return false
        val lower = merchant.lowercase()
        return onlineKeywords.any { lower.contains(it) }
    }

    @Test
    fun testKnownOnlineMerchantsMatch() {
        assertTrue(isOnlineMerchant("Swiggy"))
        assertTrue(isOnlineMerchant("Blinkit Commerce"))
        assertTrue(isOnlineMerchant("Zomato Limited"))
        assertTrue(isOnlineMerchant("Amazon Pay India"))
        assertTrue(isOnlineMerchant("Hunger Box Cafeteria"))
        assertTrue(isOnlineMerchant("Zepto Marketplace"))
    }

    @Test
    fun testOfflineMerchantsDoNotMatch() {
        assertFalse(isOnlineMerchant("Vijetha Super Market"))
        assertFalse(isOnlineMerchant("Chai Point POS"))
        assertFalse(isOnlineMerchant("HP Petrol Pump"))
        assertFalse(isOnlineMerchant(null))
        assertFalse(isOnlineMerchant(""))
    }
}
