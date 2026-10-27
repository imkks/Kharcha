package com.kharcha.engine.parser

/**
 * Isolates the active transaction event clause from static balance, credit limit,
 * greeting, or regulatory clauses in financial SMS messages.
 *
 * This prevents amount extraction patterns from conflating the transaction amount
 * with the available balance, ledger balance, or credit limit.
 */
object ClauseSemanticScoper {

    private val BALANCE_KEYWORDS_REGEX =
        Regex(
            """\b(?:passbook\s*balance|available\s*balance|avail\s*bal|avl\s*bal|account\s*balance|""" +
                """total\s*balance|remaining\s*balance|ledger\s*balance|updated\s*balance|""" +
                """net\s*balance|clear\s*bal|available\s*limit|avail\s*limit|avail\s*lmt|""" +
                """limit\s*available|avail\.\s*lmt|bal(?::|\b))\b""",
            RegexOption.IGNORE_CASE,
        )

    private val TRANSACTION_KEYWORDS_REGEX =
        Regex(
            """\b(?:debited|spent|paid|withdrawn|transferred|sent|charged|deducted|""" +
                """credited|deposited|received|refunded|refund|reversed|salary|""" +
                """contribution|disbursed|using|used|processed|sip|installment)\b""",
            RegexOption.IGNORE_CASE,
        )

    // Sentence splitter that requires sentence-ending dot/exclamation followed by space and uppercase letter,
    // excluding currency prefixes and common abbreviations.
    private val SENTENCE_SPLITTER =
        Regex(
            """(?<!\bRs|\bINR|\bRe|\bdr|\bmr|\bms|\bA\/c|\bNo)(?:\.|\!)\s+(?=[A-Z])""",
            RegexOption.IGNORE_CASE,
        )

    /**
     * Splits raw SMS into distinct clauses while preserving currency decimals and abbreviations.
     */
    fun splitIntoClauses(rawSms: String): List<String> {
        val clean = rawSms.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (clean.isEmpty()) return emptyList()

        val lineSegments = clean.split(Regex("""\n+|;\s*""")).map { it.trim() }.filter { it.isNotEmpty() }
        val clauses = mutableListOf<String>()

        for (segment in lineSegments) {
            val parts = segment.split(SENTENCE_SPLITTER)
                .map { it.trim().trimEnd('.', ';').trim() }
                .filter { it.isNotEmpty() }
            clauses.addAll(parts)
        }

        return clauses
    }

    /**
     * Returns true if the clause is strictly a balance statement and does not describe
     * an active debit/credit event.
     */
    fun isPureBalanceClause(clause: String): Boolean {
        val hasBalance = BALANCE_KEYWORDS_REGEX.containsMatchIn(clause)
        if (!hasBalance) return false

        val hasTxn = TRANSACTION_KEYWORDS_REGEX.containsMatchIn(clause)
        if (!hasTxn) return true

        val lower = clause.lowercase()
        val hasActiveVerb =
            lower.contains("debited") || lower.contains("credited") ||
                lower.contains("spent") || lower.contains("withdrawn")

        return !hasActiveVerb
    }

    /**
     * Isolates and returns the primary transaction event clause from the raw SMS text,
     * stripping out standalone balance, credit limit, or greeting clauses.
     */
    fun isolateTransactionClause(rawSms: String): String {
        val clauses = splitIntoClauses(rawSms)
        if (clauses.size <= 1) return rawSms.trim().trimEnd('.', ';').trim()

        val txnClauses = mutableListOf<String>()
        val nonBalanceClauses = mutableListOf<String>()

        for (clause in clauses) {
            val isBalance = isPureBalanceClause(clause)
            if (!isBalance) {
                nonBalanceClauses.add(clause)
                val hasTxn = TRANSACTION_KEYWORDS_REGEX.containsMatchIn(clause)
                if (hasTxn) {
                    txnClauses.add(clause)
                }
            }
        }

        return when {
            txnClauses.size == 1 -> txnClauses.first()
            txnClauses.isNotEmpty() -> txnClauses.joinToString(". ")
            nonBalanceClauses.isNotEmpty() -> nonBalanceClauses.joinToString(". ")
            else -> rawSms.trim().trimEnd('.', ';').trim()
        }
    }
}
