package com.kharcha.core.common.statement

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class StatementImportProcessor(
    private val transactionStore: TransactionStore,
) {
    interface TransactionStore {
        suspend fun getTransactionsByHashes(hashes: List<String>): List<Transaction>
        suspend fun getTransactionsBetween(startEpochMs: Long, endEpochMs: Long): List<Transaction>
        suspend fun updateTransaction(transaction: Transaction)
        suspend fun insertTransactions(transactions: List<Transaction>)
    }

    suspend fun process(parsedTransactions: List<Transaction>): StatementImportResult.Success =
        apply(plan(parsedTransactions))

    suspend fun plan(parsedTransactions: List<Transaction>): StatementImportPlan {
        if (parsedTransactions.isEmpty()) return StatementImportPlan(emptyList())
        val pending = HashMap<Long, Transaction>()
        val claimed = HashSet<Long>()
        fun Transaction.current() = pending[id] ?: this
        fun claim(existing: Transaction) = claimed.add(existing.id)
        fun enrichDecision(existing: Transaction, merged: Transaction): StatementRowDecision {
            claim(existing)
            pending[existing.id] = merged
            return StatementRowDecision.Enrich(merged)
        }
        fun matchDecision(existing: Transaction, statement: Transaction, reason: StatementRowDecision.SkipReason): StatementRowDecision {
            val merged = StatementTransactionEnricher.enrich(existing, statement)
            return if (StatementTransactionEnricher.hasEnrichment(existing, merged)) {
                enrichDecision(existing, merged)
            } else {
                claim(existing)
                StatementRowDecision.Skip(reason)
            }
        }

        val entities = parsedTransactions.map { it.copy(balanceAfter = null) }
        val zoneId = ZoneId.systemDefault()
        val days = entities.map { Instant.ofEpochMilli(it.dateTime).atZone(zoneId).toLocalDate() }

        val hashes = parsedTransactions.mapNotNull { it.smsHash }.filter { it.isNotBlank() }
        val byHash = transactionStore.getTransactionsByHashes(hashes.distinct()).associateBy { it.smsHash }

        val minDay = days.minOrNull() ?: LocalDate.now()
        val maxDay = days.maxOrNull() ?: LocalDate.now()
        val startEpoch = minDay.minusDays(REFERENCE_WINDOW.toDays()).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endEpoch = maxDay.plusDays(REFERENCE_WINDOW.toDays() + 1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        val nearby = transactionStore.getTransactionsBetween(startEpoch, endEpoch)
        val byReference = nearby.groupBy { StatementTransactionEnricher.normalizedReference(it.refNumber) }

        fun amountKey(t: Transaction) = t.amount to (t.type == TransactionType.Income)
        val byAmount = nearby.groupBy(::amountKey)

        fun amountCandidates(index: Int, from: LocalDate, to: LocalDate): List<Transaction> =
            byAmount[amountKey(entities[index])].orEmpty()
                .map { it.current() }
                .filter {
                    val date = Instant.ofEpochMilli(it.dateTime).atZone(zoneId).toLocalDate()
                    it.id !in claimed && date in from..to &&
                        StatementTransactionEnricher.isPossibleAmountDateMatch(it, entities[index])
                }
                .sortedBy { it.dateTime }

        fun sameDayMatch(index: Int, candidates: List<Transaction>): StatementRowDecision {
            val only = candidates.singleOrNull()
            if (only != null && StatementTransactionEnricher.isAmountDateFallbackEnrichmentCandidate(only, entities[index])) {
                return enrichDecision(only, StatementTransactionEnricher.enrich(only, entities[index]))
            }
            claim(candidates.first())
            return StatementRowDecision.Skip(StatementRowDecision.SkipReason.AMOUNT_DATE)
        }

        val decisions = arrayOfNulls<StatementRowDecision>(parsedTransactions.size)

        // 1. Exact re-imports
        hashes.forEachIndexed { i, hash ->
            byHash[hash]?.current()?.let { hashMatch ->
                decisions[i] = matchDecision(hashMatch, entities[i], StatementRowDecision.SkipReason.HASH)
            }
        }

        // 2. References
        entities.forEachIndexed { i, statement ->
            if (decisions[i] != null) return@forEachIndexed
            val ref = StatementTransactionEnricher.normalizedReference(statement.refNumber) ?: return@forEachIndexed
            byReference[ref].orEmpty()
                .map { it.current() }
                .filter { it.id !in claimed && StatementTransactionEnricher.isReferenceMatch(it, statement, REFERENCE_WINDOW) }
                .minByOrNull { kotlin.math.abs(it.dateTime - statement.dateTime) }
                ?.let { decisions[i] = matchDecision(it, statement, StatementRowDecision.SkipReason.REFERENCE) }
        }

        // 3. Same amount on row's own day
        val dateOnly = parsedTransactions.map { it.sender in DATE_ONLY_SENDERS }
        for (i in decisions.indices) {
            if (decisions[i] != null || dateOnly[i]) continue
            val candidates = amountCandidates(i, days[i], days[i])
            if (candidates.isNotEmpty()) decisions[i] = sameDayMatch(i, candidates)
        }

        // 4. Date-only fallback
        val nextDay = HashMap<Int, Transaction>()
        for (i in decisions.indices.filter { dateOnly[it] }.sortedByDescending { days[it] }) {
            if (decisions[i] != null) continue
            val candidates = amountCandidates(i, days[i], days[i].plusDays(1))
            val latest = candidates.lastOrNull() ?: continue
            val latestDate = Instant.ofEpochMilli(latest.dateTime).atZone(zoneId).toLocalDate()
            decisions[i] = if (latestDate == days[i]) {
                sameDayMatch(i, candidates)
            } else {
                claim(latest)
                nextDay[i] = latest
                StatementRowDecision.Skip(StatementRowDecision.SkipReason.AMOUNT_DATE)
            }
        }

        for ((i, taken) in nextDay) {
            val sameDay = amountCandidates(i, days[i], days[i])
            if (sameDay.isEmpty()) continue
            claimed.remove(taken.id)
            decisions[i] = sameDayMatch(i, sameDay)
        }

        return StatementImportPlan(
            parsedTransactions.indices.map { i ->
                StatementPlanRow(parsedTransactions[i], decisions[i] ?: StatementRowDecision.Insert(entities[i]))
            },
        )
    }

    suspend fun apply(plan: StatementImportPlan, excluded: Set<Int> = emptySet()): StatementImportResult.Success {
        plan.rows.mapNotNull { (it.decision as? StatementRowDecision.Enrich)?.merged }
            .associateBy { it.id }
            .values
            .forEach { transactionStore.updateTransaction(it) }

        val toInsert = plan.rows.mapIndexedNotNull { i, row ->
            (row.decision as? StatementRowDecision.Insert)?.entity?.takeIf { i !in excluded }
        }
        if (toInsert.isNotEmpty()) {
            transactionStore.insertTransactions(toInsert)
        }

        val skippedByHash = plan.count(StatementRowDecision.SkipReason.HASH)
        val skippedByReference = plan.count(StatementRowDecision.SkipReason.REFERENCE)
        val skippedByAmountDate = plan.count(StatementRowDecision.SkipReason.AMOUNT_DATE)

        return StatementImportResult.Success(
            imported = toInsert.size,
            skippedDuplicates = skippedByHash + skippedByReference + skippedByAmountDate,
            skippedByReference = skippedByReference,
            skippedByAmountDate = skippedByAmountDate,
            skippedByHash = skippedByHash,
            enriched = plan.rows.count { it.decision is StatementRowDecision.Enrich },
            totalParsed = plan.rows.size,
        )
    }

    private companion object {
        val REFERENCE_WINDOW: Duration = Duration.ofDays(3)
        val DATE_ONLY_SENDERS = setOf(StatementTableParser.STATEMENT_SENDER, "ICICI_PDF")
    }
}

sealed interface StatementRowDecision {
    data class Insert(val entity: Transaction) : StatementRowDecision
    data class Enrich(val merged: Transaction) : StatementRowDecision
    data class Skip(val reason: SkipReason) : StatementRowDecision

    enum class SkipReason { HASH, REFERENCE, AMOUNT_DATE }
}

data class StatementPlanRow(val parsed: Transaction, val decision: StatementRowDecision)

data class StatementImportPlan(val rows: List<StatementPlanRow>) {
    val matchedCount: Int get() = rows.count { it.decision !is StatementRowDecision.Insert }
    val newIndices: List<Int> get() = rows.indices.filter { rows[it].decision is StatementRowDecision.Insert }

    internal fun count(reason: StatementRowDecision.SkipReason) =
        rows.count { (it.decision as? StatementRowDecision.Skip)?.reason == reason }
}
