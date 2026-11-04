package com.kharcha.core.common.statement

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.Duration
import java.time.Instant
import java.util.Locale

object StatementTransactionEnricher {
    private val genericValues = setOf(
        "",
        "unknown",
        "unknown merchant",
        "upi",
        "upi transaction",
        "upi payment",
        "upi credit",
        "payment",
        "google pay",
        "phonepe",
        "paytm",
    )

    fun isStatementMatch(existing: Transaction, statement: Transaction): Boolean {
        val existingRef = normalizedReference(existing.refNumber)
        val statementRef = normalizedReference(statement.refNumber)
        if (existingRef.isNullOrBlank() || existingRef != statementRef) return false
        return hasSameTransactionDetails(existing, statement)
    }

    private fun hasSameTransactionDetails(existing: Transaction, statement: Transaction): Boolean {
        if (existing.amount != statement.amount) return false
        if (existing.currency != statement.currency) return false
        if (!sameDirection(existing, statement)) return false
        return accountsMatch(existing.accountLast4, statement.accountLast4)
    }

    fun isReferenceMatch(existing: Transaction, statement: Transaction, window: Duration): Boolean {
        val ref = normalizedReference(statement.refNumber) ?: return false
        if (normalizedReference(existing.refNumber) != ref) return false
        if (existing.amount != statement.amount) return false
        if (existing.currency != statement.currency || !sameDirection(existing, statement)) return false
        val diffMs = kotlin.math.abs(existing.dateTime - statement.dateTime)
        return Duration.ofMillis(diffMs) <= window
    }

    fun isPossibleAmountDateMatch(existing: Transaction, statement: Transaction): Boolean =
        existing.amount == statement.amount &&
            existing.currency == statement.currency &&
            sameDirection(existing, statement) &&
            accountsMatch(existing.accountLast4, statement.accountLast4)

    fun isAmountDateFallbackEnrichmentCandidate(existing: Transaction, statement: Transaction): Boolean {
        if (!isPossibleAmountDateMatch(existing, statement)) return false
        return hasUsableStatementDetails(existing, statement)
    }

    fun normalizedReference(reference: String?): String? =
        reference?.trim()?.trimStart('0')?.takeIf { it.isNotEmpty() }

    private fun sameDirection(existing: Transaction, statement: Transaction): Boolean =
        (existing.type == TransactionType.Income) == (statement.type == TransactionType.Income)

    private fun accountsMatch(a: String?, b: String?): Boolean {
        if (a.isNullOrBlank() || b.isNullOrBlank()) return true
        return a.takeLast(4) == b.takeLast(4)
    }

    private fun hasUsableStatementDetails(existing: Transaction, statement: Transaction): Boolean {
        return canUseStatementValue(existing.merchantNormalised, statement.merchantNormalised) ||
            canUseStatementValue(existing.merchantRaw, statement.merchantRaw) ||
            (existing.refNumber.isNullOrBlank() && !statement.refNumber.isNullOrBlank()) ||
            (existing.balanceAfter == null && statement.balanceAfter != null)
    }

    fun enrich(existing: Transaction, statement: Transaction): Transaction {
        var changed = false

        val merchant = if (canUseStatementValue(existing.merchantNormalised, statement.merchantNormalised)) {
            changed = true
            statement.merchantNormalised
        } else {
            existing.merchantNormalised
        }

        val ref = if (existing.refNumber.isNullOrBlank() && !statement.refNumber.isNullOrBlank()) {
            changed = true
            statement.refNumber
        } else {
            existing.refNumber
        }

        val balance = if (existing.balanceAfter == null && statement.balanceAfter != null) {
            changed = true
            statement.balanceAfter
        } else {
            existing.balanceAfter
        }

        val bank = if (existing.bankName.isNullOrBlank() && !statement.bankName.isNullOrBlank()) {
            changed = true
            statement.bankName
        } else {
            existing.bankName
        }

        val last4 = if (existing.accountLast4.isNullOrBlank() && !statement.accountLast4.isNullOrBlank()) {
            changed = true
            statement.accountLast4
        } else {
            existing.accountLast4
        }

        return if (changed) {
            existing.copy(
                merchantNormalised = merchant,
                merchantRaw = if (canUseStatementValue(existing.merchantRaw, statement.merchantRaw)) statement.merchantRaw else existing.merchantRaw,
                refNumber = ref,
                balanceAfter = balance,
                bankName = bank,
                accountLast4 = last4,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            existing
        }
    }

    fun hasEnrichment(existing: Transaction, enriched: Transaction): Boolean =
        existing.merchantNormalised != enriched.merchantNormalised ||
            existing.merchantRaw != enriched.merchantRaw ||
            existing.refNumber != enriched.refNumber ||
            existing.balanceAfter != enriched.balanceAfter ||
            existing.bankName != enriched.bankName ||
            existing.accountLast4 != enriched.accountLast4

    private fun canUseStatementValue(existing: String?, statement: String?): Boolean {
        val incoming = statement?.trim().orEmpty()
        if (incoming.isBlank() || isGeneric(incoming)) return false
        val current = existing?.trim().orEmpty()
        return current.isBlank() || isGeneric(current) || (incoming.length > current.length && current in incoming)
    }

    private fun isGeneric(value: String): Boolean =
        value.lowercase(Locale.ROOT).trim() in genericValues
}
