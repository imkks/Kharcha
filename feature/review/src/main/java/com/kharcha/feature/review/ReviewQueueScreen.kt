package com.kharcha.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ReviewQueueScreen(viewModel: ReviewQueueViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    var pendingCategoryAcceptance by remember {
        mutableStateOf<Pair<String, String>?>(null) // Pair(transactionId, categoryId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (uiState.items.isEmpty()) {
            EmptyCompletionState()
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(remainingCount = uiState.remainingCount)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.items) { item ->
                        ReviewItemCard(
                            item = item,
                            onAccept = { categoryId ->
                                pendingCategoryAcceptance = Pair(item.transaction.id.toString(), categoryId)
                            },
                            onIgnore = { viewModel.markIgnored(item.transaction.id.toString()) },
                            onSkip = { viewModel.skip(item.transaction.id.toString()) },
                        )
                    }
                }
            }
        }
    }

    // "Apply to similar" confirmation dialog
    pendingCategoryAcceptance?.let { (transactionId, categoryId) ->
        AlertDialog(
            onDismissRequest = {
                // Cancel
                pendingCategoryAcceptance = null
            },
            title = {
                Text(text = "Apply to similar transactions?")
            },
            text = {
                Text("Do you want to apply this category to all past transactions from this merchant?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.acceptCategory(transactionId, categoryId, applyToPast = true)
                        pendingCategoryAcceptance = null
                    },
                ) {
                    Text("Yes, apply to all")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.acceptCategory(transactionId, categoryId, applyToPast = false)
                        pendingCategoryAcceptance = null
                    },
                ) {
                    Text("Just this one")
                }
            },
        )
    }
}

@Composable
fun Header(remainingCount: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$remainingCount remaining", style = MaterialTheme.typography.titleMedium)
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
fun EmptyCompletionState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("All caught up!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No transactions to review.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ReviewItemCard(
    item: ReviewItemUiState,
    onAccept: (String) -> Unit,
    onIgnore: () -> Unit,
    onSkip: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val titleText = item.transaction.merchantNormalised ?: item.transaction.merchantRaw ?: "Unknown"
            Text(text = titleText, style = MaterialTheme.typography.titleLarge)
            Text(text = "Amount: ${item.transaction.amount}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Date: ${item.transaction.dateTime}", style = MaterialTheme.typography.bodyMedium)

            item.transaction.merchantRaw?.let { sms -> // Using merchant raw as proxy for sms for UI
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = sms,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Suggestions", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                item.topSuggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onAccept(suggestion.category.id) },
                        label = { Text(suggestion.category.name) },
                    )
                }
                SuggestionChip(
                    onClick = { /* Handle "Other" logic */ },
                    label = { Text("Other") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onIgnore) {
                    Text("Not an expense")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSkip) {
                    Text("Skip")
                }
            }
        }
    }
}
