package com.kharcha.engine.classifier

import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Channel

class AccountResolver {
    fun resolveAccount(
        bankName: String?,
        last4: String?,
        channel: Channel?,
        existingAccounts: List<Account>,
        parsedTxnBalance: Long? = null,
        txnDateTime: Long? = null,
    ): Account {
        val type =
            when {
                channel == Channel.CARD -> AccountType.CreditCard
                channel == Channel.WALLET -> AccountType.Wallet
                else -> AccountType.Savings
            }

        val name = "${bankName ?: "Unknown"} ${last4 ?: ""}".trim()

        val existing =
            existingAccounts.find {
                it.bankName == bankName && it.last4 == last4 && it.type == type
            }

        return if (existing != null) {
            // Update balance if newer
            val shouldUpdateBalance =
                parsedTxnBalance != null &&
                    txnDateTime != null &&
                    (existing.balanceUpdatedAt == null || txnDateTime > existing.balanceUpdatedAt)

            if (shouldUpdateBalance) {
                existing.copy(
                    currentBalance = parsedTxnBalance,
                    balanceUpdatedAt = txnDateTime,
                )
            } else {
                existing
            }
        } else {
            Account(
                name = name,
                bankName = bankName,
                last4 = last4,
                type = type,
                currentBalance = parsedTxnBalance,
                balanceUpdatedAt = txnDateTime,
            )
        }
    }
}
