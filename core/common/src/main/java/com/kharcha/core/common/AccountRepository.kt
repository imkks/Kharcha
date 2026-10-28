package com.kharcha.core.common

import com.kharcha.core.model.Account
import com.kharcha.core.model.Channel
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    fun getAccountsSync(): List<Account>
    suspend fun getAccountById(id: Long): Account?
    suspend fun resolveOrCreateAccount(
        bankName: String?,
        last4: String?,
        channel: Channel?,
        balanceMinor: Long? = null,
        timestamp: Long? = null,
        smsBody: String? = null,
    ): Account
    suspend fun saveAccount(account: Account)
    suspend fun clearAccounts()
    suspend fun autoLinkAccounts(
        transactions: List<com.kharcha.core.model.Transaction> = emptyList(),
        explicitlyUnlinkedIds: Set<Long> = emptySet(),
    )
    suspend fun linkSubAccounts(primaryAccountId: Long, linkedSubAccountIds: Set<Long>)
    suspend fun mergeAccounts(
        sourceAccountId: Long,
        targetAccountId: Long,
        transactionRepository: TransactionRepository? = null,
    ): Boolean
}
