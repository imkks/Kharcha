package com.kharcha.engine.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardAccountExtractionHelperTest {

    @Test
    fun testExtractLast4_handlesVariousMaskedFormats() {
        assertEquals("6567", CardAccountExtractionHelper.extractLast4("your Card 4620XXXXXXXX6567"))
        assertEquals("8302", CardAccountExtractionHelper.extractLast4("CC 000434155XXXXXX8302 Autopay"))
        assertEquals("5625", CardAccountExtractionHelper.extractLast4("IDFC FIRST Bank Credit Card XX5625"))
        assertEquals("8302", CardAccountExtractionHelper.extractLast4("HDFC Bank Card 8302 At ASSPL"))
        assertEquals("8302", CardAccountExtractionHelper.extractLast4("HDFC Bank Card x8302 at CROMA"))
        assertEquals("2321", CardAccountExtractionHelper.extractLast4("debited from a/c **2321"))
        assertEquals("098", CardAccountExtractionHelper.extractLast4("debited from Acct XXX098"))
        assertEquals("4007", CardAccountExtractionHelper.extractLast4("your ICICI Bank Credit Card XX4007"))
        assertEquals("1234", CardAccountExtractionHelper.extractLast4("OneCard ending with 1234"))
    }

    @Test
    fun testCleanCandidateMerchant_splitsCardAndMerchant() {
        var cardExtracted: String? = null
        val cleaned = CardAccountExtractionHelper.cleanCandidateMerchant("HDFC Bank Card 8302 At ASSPL") {
            cardExtracted = it
        }
        assertEquals("ASSPL", cleaned)
        assertEquals("8302", cardExtracted)
    }

    @Test
    fun testCleanCandidateMerchant_stripsDatePrefix() {
        val cleaned = CardAccountExtractionHelper.cleanCandidateMerchant("04-Oct-26 at GOOGLE STORE")
        assertEquals("GOOGLE STORE", cleaned)
    }

    @Test
    fun testCleanCandidateMerchant_rejectsAccountOrCardNoise() {
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("IDFC FIRST Bank Credit Card XX5625"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("your Card 4620XXXXXXXX6567"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("CC 000434155XXXXXX8302 Autopay"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("HDFC CARD"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("more secure digital banking"))
        assertEquals("a/c **0034", CardAccountExtractionHelper.cleanCandidateMerchant("a/c **0034"))
    }

    @Test
    fun testExtractBankName() {
        assertEquals("HDFC", CardAccountExtractionHelper.extractBankName("XX-HDFCBK", "Spent on card"))
        assertEquals("ONECARD", CardAccountExtractionHelper.extractBankName("XX-ONECRD", "OneCard spend"))
        assertEquals("IDFC", CardAccountExtractionHelper.extractBankName("XX-IDFCFB", "IDFC FIRST Bank"))
        assertEquals("ICICI", CardAccountExtractionHelper.extractBankName("XX-ICICIB", "ICICI Bank"))
        assertEquals("AMEX", CardAccountExtractionHelper.extractBankName("XX-AMEXIN", "American Express"))
    }

    @Test
    fun testExtractNeftSenderAndRef() {
        val raw = "for NEFT Cr-BOFA0CN6215-QUALCOMM INDIA PVT LTD-KUMAR SINGH KRISHNA-BOFAH26272092252.Avl bal INR 3,51,355.22"
        val (sender, ref) = CardAccountExtractionHelper.extractNeftSenderAndRef(raw)
        assertEquals("QUALCOMM INDIA PVT LTD", sender)
        assertEquals("BOFAH26272092252", ref)

        val rawSingle = "for NEFT Cr-ZERODHA BROKING LTD.Avl bal"
        val (sender2, ref2) = CardAccountExtractionHelper.extractNeftSenderAndRef(rawSingle)
        assertEquals("ZERODHA BROKING LTD", sender2)
        assertNull(ref2)
    }
}
