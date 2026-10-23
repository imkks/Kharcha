package com.kharcha.engine.categorizer

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {
    @Test
    fun testNormalizeStripsPrefixes() {
        assertEquals("SWIGGY", MerchantNormalizer.normalize("UPI/SWIGGY"))
        assertEquals("ZOMATO", MerchantNormalizer.normalize("POS/ZOMATO"))
        assertEquals("AMAZON", MerchantNormalizer.normalize("VPA/AMAZON"))
        assertEquals("FLIPKART", MerchantNormalizer.normalize("NEFT/FLIPKART"))
        assertEquals("SWIGGY", MerchantNormalizer.normalize("UPI SWIGGY"))
        assertEquals("AMAZON", MerchantNormalizer.normalize("ECOM AMAZON"))
    }

    @Test
    fun testNormalizeHandlesUpiHandlesAndBankSuffixes() {
        assertEquals("SWIGGY", MerchantNormalizer.normalize("swiggy@icici"))
        assertEquals("ZOMATO", MerchantNormalizer.normalize("zomato@paytm"))
        assertEquals("SWIGGY", MerchantNormalizer.normalize("SWIGGY ICICI"))
        assertEquals("UBER", MerchantNormalizer.normalize("uber.payu@hdfcbank"))
    }

    @Test
    fun testNormalizeRemovesSpecialCharactersAndNumbers() {
        assertEquals("SWIGGY", MerchantNormalizer.normalize("SWIGGY*ORDER 8432"))
        assertEquals("ZOMATO", MerchantNormalizer.normalize("ZOMATO-1234"))
        assertEquals("UBER", MerchantNormalizer.normalize("UBER.COM"))
    }

    @Test
    fun testNormalizeRemovesNoiseWords() {
        assertEquals("RELIANCE", MerchantNormalizer.normalize("RELIANCE PVT LTD"))
        assertEquals("AMAZON INDIA", MerchantNormalizer.normalize("AMAZON INDIA PRIVATE LIMITED"))
        assertEquals("SWIGGY", MerchantNormalizer.normalize("SWIGGY ORDER"))
    }

    @Test
    fun testNormalizeHandlesMultipleCombinations() {
        assertEquals("SWIGGY", MerchantNormalizer.normalize("UPI/SWIGGY*ORDER 8432 PVT LTD"))
    }

    @Test
    fun testNormalizeHandlesNullAndBlank() {
        assertEquals("", MerchantNormalizer.normalize(null))
        assertEquals("", MerchantNormalizer.normalize("   "))
    }
}
