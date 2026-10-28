package com.kharcha.core.common

import android.content.Context
import com.kharcha.core.model.Account

/**
 * Persists and applies user-confirmed bank account aliases (e.g. 3-digit UPI mask XX321 to
 * 4-digit bank account mask XX2321), scoped to the bank and currency.
 */
class BankAccountMergeStore(context: Context) {
    private val prefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    fun mappings(): Map<String, String> =
        prefs.getStringSet("bank_account_merges", emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val parts = entry.split(':')
                if (parts.size == 2 && validMapping(parts[0], parts[1])) parts[0] to parts[1] else null
            }
            .toMap()

    fun remember(
        bank: String,
        currency: String = "INR",
        short: String,
        full: String,
    ) = synchronized(writeLock) {
        val key = mappingKey(bank, currency, short)
        require(validMapping(key, full)) { "Invalid mapping: key=$key, full=$full" }
        write(mappings() + (key to full))
    }

    fun restore(imported: Map<String, String>) = synchronized(writeLock) {
        write(imported.filter { (key, full) -> validMapping(key, full) } + mappings())
    }

    fun onMerge(source: Account, target: Account) = synchronized(writeLock) {
        write(mappingsAfterMerge(mappings(), source, target))
    }

    fun forgetAccount(bank: String, suffix: String) = synchronized(writeLock) {
        write(
            mappings().filterNot { (key, full) ->
                val parts = key.split('|')
                parts[0].equals(bank, ignoreCase = true) && (parts[2] == suffix || full == suffix)
            },
        )
    }

    fun clear() = synchronized(writeLock) {
        prefs.edit().remove("bank_account_merges").apply()
    }

    private fun write(mappings: Map<String, String>) {
        val set = mappings.map { "${it.key}:${it.value}" }.toSet()
        check(prefs.edit().putStringSet("bank_account_merges", set).commit())
    }

    fun resolveSuffix(bank: String, currency: String = "INR", suffix: String): String =
        resolveSuffix(bank, currency, suffix, mappings())

    companion object {
        private val writeLock = Any()
        private val shortSuffixPattern = Regex("""\d{3}""")
        private val fullSuffixPattern = Regex("""\d{4}""")
        private val currencyPattern = Regex("""[A-Z]{3}""")

        fun mappingKey(bank: String, currency: String, suffix: String): String =
            "${bank.trim().uppercase()}|${currency.trim().uppercase()}|$suffix"

        fun isShortMaskPair(short: String, full: String): Boolean =
            short.matches(shortSuffixPattern) && full.matches(fullSuffixPattern) && full.endsWith(short)

        private fun validMapping(key: String, full: String): Boolean {
            val parts = key.split('|')
            return parts.size == 3 &&
                parts[0].isNotBlank() &&
                ':' !in parts[0] &&
                parts[1].matches(currencyPattern) &&
                isShortMaskPair(parts[2], full)
        }

        fun resolveSuffix(
            bank: String,
            currency: String = "INR",
            suffix: String,
            mappings: Map<String, String>,
        ): String {
            val full = mappings[mappingKey(bank, currency, suffix)] ?: return suffix
            return if (isShortMaskPair(suffix, full)) full else suffix
        }

        fun mappingsAfterMerge(
            mappings: Map<String, String>,
            source: Account,
            target: Account,
        ): Map<String, String> {
            val sourceBank = source.bankName.orEmpty().trim().uppercase()
            val targetBank = target.bankName.orEmpty().trim().uppercase()
            val sourceLast4 = source.last4.orEmpty()
            val targetLast4 = target.last4.orEmpty()
            val currency = "INR"

            val updated = mappings.mapNotNull { (key, full) ->
                if (!validMapping(key, full)) return@mapNotNull null
                val parts = key.split('|')
                if (parts[0] != sourceBank || parts[1] != currency || full != sourceLast4) {
                    key to full
                } else if (sourceBank == targetBank && isShortMaskPair(parts[2], targetLast4)) {
                    key to targetLast4
                } else {
                    null
                }
            }.toMap()

            return if (sourceBank == targetBank && isShortMaskPair(sourceLast4, targetLast4)) {
                updated + (mappingKey(sourceBank, currency, sourceLast4) to targetLast4)
            } else {
                updated
            }
        }

        /**
         * Identifies candidate pairs of duplicate accounts where one has 3 digits and
         * the other has 4 digits ending with the 3 digits.
         */
        fun duplicatePairs(accounts: List<Account>): List<Pair<Account, Account>> {
            val unique = accounts.distinctBy { Pair(it.bankName?.uppercase(), it.last4) }
            return unique.filter {
                val last4 = it.last4.orEmpty()
                last4.matches(shortSuffixPattern) && !it.bankName.isNullOrBlank()
            }.mapNotNull { source ->
                val sourceLast4 = source.last4.orEmpty()
                val targets = unique.filter { target ->
                    val targetLast4 = target.last4.orEmpty()
                    target.id != source.id &&
                        target.bankName.equals(source.bankName, ignoreCase = true) &&
                        target.type == source.type &&
                        isShortMaskPair(sourceLast4, targetLast4)
                }
                targets.singleOrNull()?.let { source to it }
            }
        }
    }
}
