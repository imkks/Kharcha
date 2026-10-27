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
    fun testNormalizeStripsPaymentGatewayPrefixes() {
        assertEquals("SWIGGY", MerchantNormalizer.normalize("RSP*SWIGGY PVT"))
        assertEquals("OPTICK LABS", MerchantNormalizer.normalize("RAZ*Optick Labs Inc"))
        assertEquals("MYNTRA", MerchantNormalizer.normalize("Cashfree*Myntra"))
        assertEquals("SWIGGY FOOD", MerchantNormalizer.normalize("Payu*Swiggy Food"))
        assertEquals("ZEPTO", MerchantNormalizer.normalize("PHP*Zepto"))
        assertEquals("SWIGGY", MerchantNormalizer.normalize("PPSL*SWIGGY LIM"))
    }

    @Test
    fun testNormalizeStripsBankNarrativePrefixes() {
        assertEquals("COUNTRYDELIGHT", MerchantNormalizer.normalize("date 21Feb24 trf to COUNTRYDELIGHT"))
        assertEquals("VIJETHA", MerchantNormalizer.normalize("08Jul23 transfer to VIJETHA"))
        assertEquals("VJ82 MY HOME MANGALA", MerchantNormalizer.normalize("14-01-24 to VJ82 MY HOME MANGALA"))
        assertEquals("GOOGLE STORE", MerchantNormalizer.normalize("04-Oct-26 at GOOGLE STORE"))
        assertEquals("BUNDL TECHNOLOG", MerchantNormalizer.normalize("20-Jul-24 at BUNDL TECHNOLOG"))
        assertEquals("UNICORN INFOSOL", MerchantNormalizer.normalize("28-Apr-24 at UNICORN INFOSOL"))
    }

    @Test
    fun testNormalizePreservesShortNumbersAndEmbeddedIdentifiers() {
        assertEquals("VJ82 MY HOME MANGALA", MerchantNormalizer.normalize("VJ82 MY HOME MANGALA"))
        assertEquals("VJ80 MY HOME MAN", MerchantNormalizer.normalize("VJ80 MY HOME MAN"))
        assertEquals("MS 7 NINE", MerchantNormalizer.normalize("MS 7 NINE"))
        assertEquals("PUMP 25 TS POLICE WELFARE", MerchantNormalizer.normalize("PUMP 25 TS POLICE WELFARE"))
    }

    @Test
    fun testNormalizeCleansWebUrlDomains() {
        assertEquals("SWIGGY", MerchantNormalizer.normalize("WWW SWIGGY COM"))
        assertEquals("BIGBASKET", MerchantNormalizer.normalize("WWWBIGBASKETCOM"))
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
