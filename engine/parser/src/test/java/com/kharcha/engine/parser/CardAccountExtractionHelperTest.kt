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

        val rawWells1 = "for NEFT-HDFCH00825101407-WELLS. Avl bal"
        val (sender3, ref3) = CardAccountExtractionHelper.extractNeftSenderAndRef(rawWells1)
        assertEquals("WELLS", sender3)
        assertEquals("HDFCH00825101407", ref3)

        val rawWells2 = "by NEFT-211250238918-WELLS FARGO. Bal"
        val (sender4, ref4) = CardAccountExtractionHelper.extractNeftSenderAndRef(rawWells2)
        assertEquals("WELLS FARGO", sender4)
        assertEquals("211250238918", ref4)
    }

    @Test
    fun testCleanAtmMerchant() {
        assertEquals("SBI ATM", CardAccountExtractionHelper.cleanAtmMerchant("SBI ATM S1BB000454545 from"))
        assertEquals("ICICI ATM", CardAccountExtractionHelper.cleanAtmMerchant("ICI ATM S1CNP941 from"))
        assertEquals("AXIS ATM", CardAccountExtractionHelper.cleanAtmMerchant("AXS ATM BPCN1590 from"))
        assertEquals("UCO ATM", CardAccountExtractionHelper.cleanAtmMerchant("UCO ATM VA207203 from"))
        assertEquals("HDFC ATM", CardAccountExtractionHelper.cleanAtmMerchant("HDF ATM S1ANRN7 from"))
        assertEquals("Cash Withdrawal", CardAccountExtractionHelper.cleanAtmMerchant("18-Aug-25 NFS*CASH WDL*"))
        assertEquals("ATM Withdrawal", CardAccountExtractionHelper.cleanAtmMerchant("04-Jan-24 ATM*S1CNI828*"))
        assertEquals("HDFC ATM", CardAccountExtractionHelper.cleanAtmMerchant("ATM*S1CNI828*", "HDFC"))
    }

    @Test
    fun testCleanCandidateMerchant_stripsVariousDateAndTransferPrefixes() {
        assertEquals(
            "COUNTRYDELIGHT",
            CardAccountExtractionHelper.cleanCandidateMerchant("date 21Feb24 trf to COUNTRYDELIGHT"),
        )
        assertEquals(
            "ASHISH ANAND",
            CardAccountExtractionHelper.cleanCandidateMerchant("13Aug24 transfer from ASHISH ANAND"),
        )
        assertEquals(
            "VIJETHA",
            CardAccountExtractionHelper.cleanCandidateMerchant("08Jul23 transfer to VIJETHA"),
        )
        assertEquals(
            "Udemy India LLP",
            CardAccountExtractionHelper.cleanCandidateMerchant("09-Sep-25 from Udemy India LLP"),
        )
        assertEquals(
            "APOLLO PHARMACY",
            CardAccountExtractionHelper.cleanCandidateMerchant("25-01-24 to APOLLO PHARMACY"),
        )
        assertEquals(
            "IBIBO GROUP PRI",
            CardAccountExtractionHelper.cleanCandidateMerchant("03-Sep-26 at IBIBO GROUP PRI"),
        )
    }

    @Test
    fun testCleanCandidateMerchant_handlesHdfcInfoCodes() {
        assertEquals(
            "Auto Debit",
            CardAccountExtractionHelper.cleanCandidateMerchant("04-Oct-26 InfoATD*Auto Debi"),
        )
        assertEquals(
            "PPF Transfer",
            CardAccountExtractionHelper.cleanCandidateMerchant("04-May-26 InfoTrf to PPF 00"),
        )
        assertEquals(
            "Bill Payment",
            CardAccountExtractionHelper.cleanCandidateMerchant("23-Apr-26 InfoBIL*NEFT*IN12"),
        )
    }

    @Test
    fun testCleanCandidateMerchant_rejectsChannelAndNoise() {
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("02-01-2024 by UCO-UPI"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("HDFC UPI"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("SBI UPI"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("Transaction"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("your"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("clearing"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("dispute"))
        assertNull(CardAccountExtractionHelper.cleanCandidateMerchant("Rs."))
    }
}
