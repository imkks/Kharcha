package com.kharcha.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AccountCardModel(
    val id: Long,
    val bankName: String,
    val accountNumberMasked: String,
    val balanceFormatted: String,
    val isWallet: Boolean = false,
    val lastTxnInfo: String? = null,
)

/**
 * Snapping horizontal carousel of AccountCard items with 12dp spacing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCarousel(
    accounts: List<AccountCardModel>,
    onAccountClick: (AccountCardModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(accounts, key = { it.id }) { acc ->
            AccountCard(
                bankName = acc.bankName,
                accountNumberMasked = acc.accountNumberMasked,
                balanceFormatted = acc.balanceFormatted,
                isWallet = acc.isWallet,
                lastTxnInfo = acc.lastTxnInfo,
                onClick = { onAccountClick(acc) },
            )
        }
    }
}
