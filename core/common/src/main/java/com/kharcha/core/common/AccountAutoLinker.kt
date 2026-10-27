package com.kharcha.core.common

import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Transaction
import java.util.Locale

object AccountAutoLinker {

    private val KNOWN_BANKS = listOf(
        "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "IDFC",
        "PNB", "BOB", "CANARA", "INDUSIND", "YES", "AMEX",
        "CITI", "RBL", "SCB", "FEDERAL", "STANDARD CHARTERED",
        "HSBC", "BARODA", "UNION",
    )

    // Pattern to extract credit card last 4 from a bill payment / transfer note
    private val CARD_TARGET_PATTERNS = listOf(
        // "towards ICICI Bank Credit Card ending with 8302" or "to credit card 8302"
        Regex("""(?i)(?:towards|for|to|of)\s+(?:your\s+)?(?:[a-z]+\s+){0,3}(?:credit\s*card|card|cc)\s*(?:ending(?:\s+with|\s+in)?|no\.?|number|[*xX.-])*\s*([0-9]{3,4})"""),
        // "Credit card ending 8302 payment"
        Regex("""(?i)(?:credit\s*card|card|cc)\s*(?:ending(?:\s+with|\s+in)?|no\.?|number|[*xX.-])*\s*([0-9]{3,4}).*?(?:payment|bill|autodebit|auto-debit|paid)"""),
        // "Payment of Rs ... for Card ending 8302"
        Regex("""(?i)(?:payment|bill|autodebit|auto-debit|paid).*?(?:towards|for|to)\s+(?:[a-z]+\s+){0,2}(?:credit\s*card|card|cc)?\s*(?:ending(?:\s+with|\s+in)?|no\.?|number|[*xX.-])*\s*([0-9]{3,4})"""),
    )

    // Pattern to extract debited bank account last 4
    private val BANK_SOURCE_PATTERNS = listOf(
        // "from A/c XX1234" or "from ICICI Bank Acc XX1234"
        Regex("""(?i)(?:from|through|via|in)\s+(?:[a-z]+\s+){0,3}(?:a/c|acc|acct|account)\s*(?:no\.?|number)?\s*[*xX.-]*([0-9]{3,4})"""),
        // "A/c XX1234 has been debited" or "Acc XX1234 is debited"
        Regex("""(?i)(?:a/c|acc|acct|account)\s*(?:no\.?|number)?\s*[*xX.-]*([0-9]{3,4})\s*(?:has\s+been|is|was)?\s*debited"""),
    )

    /**
     * Normalizes bank name strings for robust matching (e.g., "ICICI BANK" -> "ICICI").
     */
    fun normalizeBank(bankName: String?): String? {
        if (bankName.isNullOrBlank()) return null
        var clean = bankName.trim().uppercase(Locale.getDefault())
            .replace("BANK", "")
            .replace("LTD", "")
            .replace("LIMITED", "")
            .replace("FINANCIAL", "")
            .replace("SERVICES", "")
            .trim()
        if (clean == "STATE OF INDIA") clean = "SBI"
        if (clean == "OF BARODA") clean = "BOB"
        return clean.ifEmpty { null }
    }

    /**
     * Extracts known bank name from arbitrary account or merchant strings.
     */
    fun extractBankFromName(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val upper = name.uppercase(Locale.getDefault())
        return KNOWN_BANKS.firstOrNull { upper.contains(it) }
    }

    /**
     * Analyzes transaction notes and bodies to find which bank account paid which credit card.
     * Returns a map of creditCardAccountId -> payingPrimaryAccountId.
     */
    fun findBillPaymentLinks(
        accounts: List<Account>,
        transactions: List<Transaction>,
    ): Map<Long, Long> {
        val links = mutableMapOf<Long, Long>()

        val creditCards = accounts.filter { it.type == AccountType.CreditCard }
        val bankAccounts = accounts.filter { it.type == AccountType.Savings || it.type == AccountType.Current }

        if (creditCards.isEmpty() || bankAccounts.isEmpty()) return emptyMap()

        for (txn in transactions) {
            val text = (txn.note ?: txn.merchantRaw ?: "")
            if (text.isBlank()) continue

            var targetCardLast4: String? = null
            for (p in CARD_TARGET_PATTERNS) {
                val m = p.find(text)
                if (m != null) {
                    targetCardLast4 = m.groupValues[1]
                    break
                }
            }
            if (targetCardLast4 == null) continue

            val targetCard = creditCards.find { it.last4?.endsWith(targetCardLast4) == true } ?: continue

            var sourceBankLast4: String? = null
            for (p in BANK_SOURCE_PATTERNS) {
                val m = p.find(text)
                if (m != null) {
                    sourceBankLast4 = m.groupValues[1]
                    break
                }
            }

            val targetBank = if (sourceBankLast4 != null) {
                bankAccounts.find { it.last4?.endsWith(sourceBankLast4) == true }
                    ?: bankAccounts.find { it.id == txn.accountId }
            } else {
                bankAccounts.find { it.id == txn.accountId }
            }

            if (targetBank != null && targetCard.id != targetBank.id) {
                links[targetCard.id] = targetBank.id
            }
        }

        return links
    }

    /**
     * Finds same-bank relationships between primary bank accounts (Savings/Current)
     * and sub-accounts (CreditCard, Wallet).
     * Multiple credit cards from the same bank will all map to the primary bank account.
     */
    fun findSameBankLinks(accounts: List<Account>): Map<Long, Long> {
        val links = mutableMapOf<Long, Long>()

        val primaryBanks = accounts.filter { it.type == AccountType.Savings || it.type == AccountType.Current }
        val subAccounts = accounts.filter { it.type == AccountType.CreditCard || it.type == AccountType.Wallet }

        if (primaryBanks.isEmpty() || subAccounts.isEmpty()) return emptyMap()

        for (sub in subAccounts) {
            val subBankNorm = normalizeBank(sub.bankName) ?: extractBankFromName(sub.name)
            val matchedPrimary = primaryBanks.find { primary ->
                val primaryBankNorm = normalizeBank(primary.bankName) ?: extractBankFromName(primary.name)
                when {
                    subBankNorm != null && primaryBankNorm != null &&
                        subBankNorm.equals(primaryBankNorm, ignoreCase = true) -> true
                    subBankNorm != null && primary.name.contains(subBankNorm, ignoreCase = true) -> true
                    primaryBankNorm != null && sub.name.contains(primaryBankNorm, ignoreCase = true) -> true
                    else -> false
                }
            }

            if (matchedPrimary != null && sub.id != matchedPrimary.id) {
                links[sub.id] = matchedPrimary.id
            }
        }

        return links
    }

    /**
     * Auto-links accounts by combining:
     * 1. Bill payment transaction evidence from SMS data
     * 2. Same-bank matching (e.g. ICICI Savings -> all ICICI Credit Cards)
     *
     * Respects user explicit unlinking preference (explicitlyUnlinkedIds).
     * Returns the updated accounts list with linkedPrimaryAccountId set.
     */
    fun autoLinkAccounts(
        accounts: List<Account>,
        transactions: List<Transaction> = emptyList(),
        explicitlyUnlinkedIds: Set<Long> = emptySet(),
    ): List<Account> {
        if (accounts.isEmpty()) return accounts

        val billPaymentLinks = findBillPaymentLinks(accounts, transactions)
        val sameBankLinks = findSameBankLinks(accounts)

        return accounts.map { acc ->
            // If already linked, or explicitly unlinked by user, keep as is
            if (acc.id in explicitlyUnlinkedIds) {
                if (acc.linkedPrimaryAccountId != null) acc.copy(linkedPrimaryAccountId = null) else acc
            } else if (acc.linkedPrimaryAccountId != null) {
                acc
            } else {
                // Priority 1: Direct bill payment evidence from SMS data
                // Priority 2: Same bank matching (multiple cards per bank)
                val linkedPrimaryId = billPaymentLinks[acc.id] ?: sameBankLinks[acc.id]
                if (linkedPrimaryId != null && linkedPrimaryId != acc.id) {
                    acc.copy(linkedPrimaryAccountId = linkedPrimaryId)
                } else {
                    acc
                }
            }
        }
    }
}
