package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagParserTest {

    @Test
    fun testExtractTagsFromNote() {
        val note = "Dinner with colleagues #Food #Party #Weekend"
        val tags = TagParser.extractTags(note)

        assertEquals(listOf("#Food", "#Party", "#Weekend"), tags)
    }

    @Test
    fun testExtractTagsFromNullOrBlankNote() {
        assertTrue(TagParser.extractTags(null).isEmpty())
        assertTrue(TagParser.extractTags("").isEmpty())
        assertTrue(TagParser.extractTags("   ").isEmpty())
        assertTrue(TagParser.extractTags("Regular note with no tags").isEmpty())
    }

    @Test
    fun testAutoOnlineTagForChannel() {
        // Channel UPI auto-injects #Online if not present
        val upiTags = TagParser.extractTags("Grocery shopping", Channel.UPI)
        assertEquals(listOf("#Online"), upiTags)

        // If #Online is already present in note, it should not duplicate
        val existingOnlineTags = TagParser.extractTags("Shopping #Online #Vacation", Channel.CARD)
        assertEquals(listOf("#Online", "#Vacation"), existingOnlineTags)

        // Offline channels like ATM should not auto-inject #Online
        val atmTags = TagParser.extractTags("Cash withdrawal", Channel.ATM)
        assertTrue(atmTags.isEmpty())

        // NetBanking (NEFT/IMPS) should auto-inject #Online
        val neftTags = TagParser.extractTags("Rent transfer", Channel.NEFT)
        assertEquals(listOf("#Online"), neftTags)
    }

    @Test
    fun testAddTagToEmptyNote() {
        val updated = TagParser.addTag("", "Vacation")
        assertEquals("#Vacation", updated)

        val updatedWithHash = TagParser.addTag(null, "#TaxDeductible")
        assertEquals("#TaxDeductible", updatedWithHash)
    }

    @Test
    fun testAddTagToExistingNote() {
        val note = "Monthly team lunch"
        val updated = TagParser.addTag(note, "Party")
        assertEquals("Monthly team lunch #Party", updated)

        // Adding tag with leading hash
        val reUpdated = TagParser.addTag(updated, "#Reimbursable")
        assertEquals("Monthly team lunch #Party #Reimbursable", reUpdated)
    }

    @Test
    fun testAddDuplicateTagIsIdempotent() {
        val note = "Flight tickets #Vacation"
        val updated = TagParser.addTag(note, "#Vacation")
        assertEquals("Flight tickets #Vacation", updated)

        // Case-insensitive duplicate prevention
        val updatedLower = TagParser.addTag(note, "vacation")
        assertEquals("Flight tickets #Vacation", updatedLower)
    }

    @Test
    fun testRemoveTag() {
        val note = "Project dinner #Online #Party #Reimbursable"
        val updated = TagParser.removeTag(note, "#Party")
        assertEquals("Project dinner #Online #Reimbursable", updated)

        // Removing tag with or without leading hash
        val reUpdated = TagParser.removeTag(updated, "Reimbursable")
        assertEquals("Project dinner #Online", reUpdated)
    }

    @Test
    fun testRemoveTagSoleContent() {
        val note = "#Vacation"
        val updated = TagParser.removeTag(note, "#Vacation")
        assertEquals("", updated)
    }

    @Test
    fun testRemoveNonExistentTagDoesNotModifyNote() {
        val note = "Just a note #Online"
        val updated = TagParser.removeTag(note, "#Offline")
        assertEquals("Just a note #Online", updated)
    }

    @Test
    fun testFormatTag() {
        assertEquals("#Vacation", TagParser.formatTag("Vacation"))
        assertEquals("#Vacation", TagParser.formatTag("#Vacation"))
        assertEquals("#TaxDeductible", TagParser.formatTag("  #TaxDeductible  "))
        assertEquals("", TagParser.formatTag(""))
        assertEquals("", TagParser.formatTag("   "))
        assertEquals("", TagParser.formatTag("#"))
    }

    @Test
    fun testPresetTagsIntegrity() {
        val expectedPresets = listOf(
            "#Online",
            "#Offline",
            "#Vacation",
            "#TaxDeductible",
            "#Reimbursable",
            "#Party",
        )
        assertEquals(expectedPresets, TagParser.PRESET_TAGS)
        expectedPresets.forEach { tag ->
            assertTrue(tag.startsWith("#"))
            assertFalse(tag.contains(" "))
        }
    }
}
