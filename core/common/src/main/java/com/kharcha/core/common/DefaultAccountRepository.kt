package com.kharcha.core.common

import android.content.Context
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Channel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DefaultAccountRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountRepository {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    private val file: File
        get() {
            val dir =
                try {
                    context.filesDir
                } catch (_: Exception) {
                    null
                } ?: File(System.getProperty("java.io.tmpdir") ?: ".")
            return File(dir, "accounts.json")
        }

    init {
        loadFromDisk()
    }

    override fun getAccounts(): Flow<List<Account>> = _accounts.asStateFlow()

    override fun getAccountsSync(): List<Account> = _accounts.value

    override suspend fun getAccountById(id: Long): Account? {
        return _accounts.value.find { it.id == id }
    }

    override suspend fun resolveOrCreateAccount(
        bankName: String?,
        last4: String?,
        channel: Channel?,
        balanceMinor: Long?,
        timestamp: Long?,
        smsBody: String?,
    ): Account {
        return withContext(ioDispatcher) {
            val currentList = _accounts.value.toMutableList()

            val targetCardLast4 = smsBody?.let { TransactionClassifierHelper.extractTargetCardLast4(it) }
            val sourceAccountLast4 = smsBody?.let { TransactionClassifierHelper.extractSourceAccountLast4(it) }
            val isBillPayment = smsBody?.let { TransactionClassifierHelper.isCreditCardBillPayment(it) } == true

            val isCardChannel = channel == Channel.CARD
            val hasCardText =
                smsBody?.let { body ->
                    val b = body.lowercase()
                    (b.contains("credit card") || b.contains("card ending") ||
                        b.contains("spent on card") || b.contains("in your credit card") ||
                        b.contains("spent on hdfc bank card") || b.contains("spent on icici bank card") ||
                        b.contains("spent using") || b.contains("card xx") || b.contains("card x") ||
                        b.contains("cardmember") || b.contains("credit limit") || b.contains("available limit") ||
                        (b.contains("card") && targetCardLast4 != null)) &&
                        !b.contains("towards credit card") && !b.contains("towards your credit card") &&
                        !b.contains("towards card") && !b.contains("for credit card") &&
                        !b.contains("credit card bil") && !b.contains("credit card payment")
                } == true

            val type =
                when {
                    sourceAccountLast4 != null -> AccountType.Savings
                    isCardChannel -> AccountType.CreditCard
                    hasCardText -> AccountType.CreditCard
                    channel == Channel.WALLET -> AccountType.Wallet
                    isBillPayment && targetCardLast4 != null -> AccountType.CreditCard
                    else -> AccountType.Savings
                }

            val resolvedBank =
                when {
                    !bankName.isNullOrBlank() -> bankName.trim().uppercase()
                    smsBody != null -> extractBankFromText(smsBody)
                    else -> null
                }

            val resolvedLast4 =
                when {
                    type == AccountType.CreditCard && targetCardLast4 != null -> targetCardLast4
                    type == AccountType.Savings && sourceAccountLast4 != null -> sourceAccountLast4
                    !last4.isNullOrBlank() -> {
                        val digits = last4.filter { it.isDigit() }
                        if (digits.length in 3..4) {
                            digits
                        } else if (digits.length > 4) {
                            digits.takeLast(4)
                        } else {
                            last4.trim()
                        }
                    }
                    smsBody != null -> extractLast4FromText(smsBody, type)
                    else -> null
                }

            val name =
                when (type) {
                    AccountType.CreditCard -> {
                        val bankPart = resolvedBank ?: "Credit"
                        val last4Part = resolvedLast4?.let { " ••$it" } ?: ""
                        "$bankPart Card$last4Part"
                    }
                    AccountType.Wallet -> {
                        val walletPart = resolvedBank ?: "Digital"
                        "$walletPart Wallet"
                    }
                    else -> {
                        val bankPart = resolvedBank ?: "Bank"
                        val last4Part = resolvedLast4?.let { " ••$it" } ?: ""
                        "$bankPart$last4Part"
                    }
                }

            val existingIndex =
                currentList.indexOfFirst { acc ->
                    val sameType = acc.type == type
                    val sameLast4 =
                        resolvedLast4 != null && acc.last4 != null &&
                            acc.last4.equals(resolvedLast4, ignoreCase = true)
                    val sameBank =
                        resolvedBank != null && acc.bankName != null &&
                            acc.bankName.equals(resolvedBank, ignoreCase = true)

                    if (resolvedLast4 != null && acc.last4 != null) {
                        sameType && sameLast4 && (sameBank || acc.bankName == null || resolvedBank == null)
                    } else if (resolvedBank != null && acc.bankName != null) {
                        sameType && sameBank
                    } else {
                        false
                    }
                }

            val now = timestamp ?: System.currentTimeMillis()
            val primaryAccount: Account
            if (existingIndex != -1) {
                val existing = currentList[existingIndex]
                val lastUpdated = existing.balanceUpdatedAt
                val shouldUpdateBalance =
                    balanceMinor != null && (lastUpdated == null || now >= lastUpdated)

                val updated =
                    if (shouldUpdateBalance) {
                        existing.copy(
                            currentBalance = balanceMinor,
                            balanceUpdatedAt = now,
                        )
                    } else {
                        existing
                    }
                currentList[existingIndex] = updated
                primaryAccount = updated
            } else {
                val nextId = (currentList.maxOfOrNull { it.id } ?: 0L) + 1L

                val autoLinkedPrimaryId =
                    if (type == AccountType.CreditCard || type == AccountType.Wallet) {
                        currentList.find { primary ->
                            (primary.type == AccountType.Savings || primary.type == AccountType.Current) &&
                                AccountAutoLinker.normalizeBank(primary.bankName)?.equals(
                                    AccountAutoLinker.normalizeBank(resolvedBank),
                                    ignoreCase = true,
                                ) == true
                        }?.id
                    } else {
                        null
                    }

                val newAccount =
                    Account(
                        id = nextId,
                        name = name,
                        bankName = resolvedBank,
                        last4 = resolvedLast4,
                        type = type,
                        currentBalance = balanceMinor,
                        balanceUpdatedAt = if (balanceMinor != null) now else null,
                        linkedPrimaryAccountId = autoLinkedPrimaryId,
                    )
                currentList.add(newAccount)

                if (type == AccountType.Savings || type == AccountType.Current) {
                    for (i in currentList.indices) {
                        val acc = currentList[i]
                        if ((acc.type == AccountType.CreditCard || acc.type == AccountType.Wallet) &&
                            acc.linkedPrimaryAccountId == null &&
                            AccountAutoLinker.normalizeBank(acc.bankName)?.equals(
                                AccountAutoLinker.normalizeBank(resolvedBank),
                                ignoreCase = true,
                            ) == true
                        ) {
                            currentList[i] = acc.copy(linkedPrimaryAccountId = newAccount.id)
                        }
                    }
                }
                primaryAccount = newAccount
            }

            // If SMS has both a source bank account and a target credit card (e.g. Auto Debit for credit card),
            // ensure the target credit card also exists and is explicitly linked to the debited primary account.
            if (sourceAccountLast4 != null && targetCardLast4 != null && sourceAccountLast4 != targetCardLast4) {
                val cardBank = resolvedBank ?: "Credit"
                val existingCardIndex = currentList.indexOfFirst {
                    it.type == AccountType.CreditCard && it.last4 == targetCardLast4 &&
                        (it.bankName == null || it.bankName.equals(cardBank, ignoreCase = true))
                }
                if (existingCardIndex != -1) {
                    val cardAcc = currentList[existingCardIndex]
                    if (cardAcc.linkedPrimaryAccountId == null) {
                        currentList[existingCardIndex] = cardAcc.copy(linkedPrimaryAccountId = primaryAccount.id)
                    }
                } else {
                    val cardNextId = (currentList.maxOfOrNull { it.id } ?: 0L) + 1L
                    val newCard = Account(
                        id = cardNextId,
                        name = "$cardBank Card ••$targetCardLast4",
                        bankName = cardBank,
                        last4 = targetCardLast4,
                        type = AccountType.CreditCard,
                        linkedPrimaryAccountId = primaryAccount.id,
                    )
                    currentList.add(newCard)
                }
            }

            _accounts.value = currentList
            saveToDisk(currentList)
            primaryAccount
        }
    }

    override suspend fun saveAccount(account: Account) {
        withContext(ioDispatcher) {
            val currentList = _accounts.value.toMutableList()
            val targetId = if (account.id == 0L) (currentList.maxOfOrNull { it.id } ?: 0L) + 1L else account.id
            val finalAccount = if (account.id == 0L) account.copy(id = targetId) else account
            val index = currentList.indexOfFirst { it.id == finalAccount.id }
            if (index != -1) {
                currentList[index] = finalAccount
            } else {
                currentList.add(finalAccount)
            }
            _accounts.value = currentList
            saveToDisk(currentList)
        }
    }

    override suspend fun clearAccounts() {
        withContext(ioDispatcher) {
            _accounts.value = emptyList()
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override suspend fun autoLinkAccounts(
        transactions: List<com.kharcha.core.model.Transaction>,
        explicitlyUnlinkedIds: Set<Long>,
    ) {
        withContext(ioDispatcher) {
            val current = _accounts.value
            val updated = AccountAutoLinker.autoLinkAccounts(current, transactions, explicitlyUnlinkedIds)
            if (updated != current) {
                _accounts.value = updated
                saveToDisk(updated)
            }
        }
    }

    override suspend fun linkSubAccounts(primaryAccountId: Long, linkedSubAccountIds: Set<Long>) {
        withContext(ioDispatcher) {
            val currentList = _accounts.value.toMutableList()
            for (i in currentList.indices) {
                val acc = currentList[i]
                if (acc.id in linkedSubAccountIds) {
                    currentList[i] = acc.copy(linkedPrimaryAccountId = primaryAccountId)
                } else if (acc.linkedPrimaryAccountId == primaryAccountId && acc.id !in linkedSubAccountIds) {
                    currentList[i] = acc.copy(linkedPrimaryAccountId = null)
                }
            }
            _accounts.value = currentList
            saveToDisk(currentList)
        }
    }

    private fun extractBankFromText(body: String): String? {
        val banks =
            listOf(
                "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "IDFC",
                "PNB", "BOB", "CANARA", "INDUSIND", "YES", "AMEX",
                "CITI", "RBL", "SCB", "FEDERAL", "STANDARD CHARTERED",
                "HSBC", "BARODA", "UNION", "UCO", "BANDHAN", "AU",
                "SLICE", "ONECARD", "JUPITER", "FI", "PAYTM", "AIRTEL",
            )
        val upper = body.uppercase()
        return banks.firstOrNull { upper.contains(it) }
    }

    private fun extractLast4FromText(body: String, type: AccountType): String? {
        if (type == AccountType.CreditCard) {
            val target = TransactionClassifierHelper.extractTargetCardLast4(body)
            if (target != null) return target
            val cardRegex =
                Regex(
                    """(?i)(?:credit\s*card|card|cc)(?:\s+(?:account|a/c|acc))?\s*(?:ending(?:\s+with|\s+in)?|no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b""",
                )
            val match = cardRegex.find(body)
            if (match != null) return match.groupValues[1]
        } else {
            val source = TransactionClassifierHelper.extractSourceAccountLast4(body)
            if (source != null) return source
        }

        val acctRegex =
            Regex(
                """(?i)(?:a/c|acct|account|acc)\s*(?:no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b""",
            )
        val acctMatch = acctRegex.find(body)
        if (acctMatch != null) return acctMatch.groupValues[1]

        val maskedMatch = Regex("""(?i)(?:[xX*]{1,12}|\b\d{2,9}[xX*]+)(\d{3,4})\b""").find(body)
        if (maskedMatch != null) return maskedMatch.groupValues[1]

        val fallback = Regex("""(?:\*{1,}|x{2,}|xx)(\d{3,4})""", RegexOption.IGNORE_CASE)
        return fallback.find(body)?.groupValues?.get(1)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadFromDisk() {
        try {
            if (!file.exists()) return
            val text = file.readText()
            if (text.isBlank()) return
            val jsonArray = JSONArray(text)
            val list = mutableListOf<Account>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(jsonToAccount(obj))
            }
            val autoLinked = AccountAutoLinker.autoLinkAccounts(list)
            _accounts.value = autoLinked
            if (autoLinked != list) {
                saveToDisk(autoLinked)
            }
        } catch (_: Exception) {
            // Keep empty list if corrupted
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun saveToDisk(list: List<Account>) {
        try {
            val jsonArray = JSONArray()
            for (acc in list) {
                jsonArray.put(accountToJson(acc))
            }
            file.writeText(jsonArray.toString())
        } catch (_: Exception) {
            // Log or ignore disk errors gracefully
        }
    }

    private fun accountToJson(account: Account): JSONObject {
        val obj = JSONObject()
        obj.put("id", account.id)
        obj.put("name", account.name)
        obj.put("bankName", account.bankName)
        obj.put("last4", account.last4)
        obj.put("type", account.type.name)
        account.currentBalance?.let { obj.put("currentBalance", it) }
        account.balanceUpdatedAt?.let { obj.put("balanceUpdatedAt", it) }
        obj.put("isArchived", account.isArchived)
        obj.put("isCountedAsExpense", account.isCountedAsExpense)
        account.linkedPrimaryAccountId?.let { obj.put("linkedPrimaryAccountId", it) }
        return obj
    }

    @Suppress("TooGenericExceptionCaught")
    private fun jsonToAccount(json: JSONObject): Account {
        val typeStr = json.optString("type", AccountType.Savings.name)
        val accType =
            try {
                AccountType.valueOf(typeStr)
            } catch (_: Exception) {
                AccountType.Savings
            }

        val bal =
            if (json.has("currentBalance") && !json.isNull("currentBalance")) {
                json.getLong("currentBalance")
            } else {
                null
            }

        val updated =
            if (json.has("balanceUpdatedAt") && !json.isNull("balanceUpdatedAt")) {
                json.getLong("balanceUpdatedAt")
            } else {
                null
            }

        val linkedPrimary =
            if (json.has("linkedPrimaryAccountId") && !json.isNull("linkedPrimaryAccountId")) {
                json.getLong("linkedPrimaryAccountId")
            } else {
                null
            }

        return Account(
            id = json.optLong("id"),
            name = json.optString("name", "Account"),
            bankName = if (json.has("bankName") && !json.isNull("bankName")) json.getString("bankName") else null,
            last4 = if (json.has("last4") && !json.isNull("last4")) json.getString("last4") else null,
            type = accType,
            currentBalance = bal,
            balanceUpdatedAt = updated,
            isArchived = json.optBoolean("isArchived", false),
            isCountedAsExpense = json.optBoolean("isCountedAsExpense", true),
            linkedPrimaryAccountId = linkedPrimary,
        )
    }

    companion object {
        @Volatile
        private var instance: DefaultAccountRepository? = null

        fun getInstance(context: Context): DefaultAccountRepository {
            return instance ?: synchronized(this) {
                val appContext =
                    try {
                        context.applicationContext ?: context
                    } catch (_: Exception) {
                        context
                    }
                instance ?: DefaultAccountRepository(appContext).also {
                    instance = it
                }
            }
        }
    }
}
