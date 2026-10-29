package com.kharcha.engine.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kharcha.core.common.DefaultAccountRepository
import com.kharcha.core.common.DefaultTransactionRepository
import com.kharcha.core.common.DefaultUserPreferences
import com.kharcha.core.common.LocationCaptureHelper
import com.kharcha.core.common.TransactionClassifierHelper
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.RawSms
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.engine.parser.TemplateLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    private val senderFilter = SenderFilter()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages =
                try {
                    Telephony.Sms.Intents.getMessagesFromIntent(intent)
                } catch (_: Exception) {
                    null
                }
            if (messages.isNullOrEmpty()) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = DefaultTransactionRepository.getInstance(context)
                    val parser = TemplateLoader.createParser(context)
                    for (msg in messages) {
                        val sender = msg.displayOriginatingAddress ?: msg.originatingAddress ?: continue
                        val body = msg.displayMessageBody ?: msg.messageBody ?: continue
                        val receivedAt = msg.timestampMillis

                        if (!senderFilter.isFinancialSender(sender)) continue

                        val rawSms =
                            RawSms(
                                id = System.currentTimeMillis() + (0..999).random(),
                                sender = sender,
                                body = body,
                                receivedAt = receivedAt,
                                subId = null,
                            )

                        val parseResult = parser.parse(rawSms)
                        if (parseResult !is ParseResult.Success) continue
                        val parsed = parseResult.txn
                        val isCredit = parsed.direction == Direction.CREDIT
                        val rawMerchant = parsed.merchantRaw ?: parsed.upiId
                        val normMerchant =
                            TransactionClassifierHelper.cleanMerchantDisplay(
                                rawMerchant,
                                parsed.bankName,
                                parsed.channel?.name,
                            )
                        val type =
                            TransactionClassifierHelper.classifyType(
                                direction = parsed.direction,
                                body = body,
                                merchant = normMerchant,
                                amountMinor = parsed.amountMinor,
                                channel = parsed.channel,
                            )
                        val isCardBill = TransactionClassifierHelper.isCreditCardBillPayment(body, normMerchant)
                        val targetCardLast4 = if (isCardBill) TransactionClassifierHelper.extractTargetCardLast4(body) else null
                        val resolvedAccountLast4 = targetCardLast4 ?: parsed.accountLast4
                        val resolvedChannel = if (isCardBill) Channel.CARD else parsed.channel
                        val isTransferNarrative = TransactionClassifierHelper.isTransferNarrative(
                            body = body,
                            merchant = normMerchant,
                            amountMinor = parsed.amountMinor,
                            channel = parsed.channel,
                        )
                        val userRules = DefaultUserPreferences(context).getMerchantCategoryRules()
                        val isUserCategorized = userRules.containsKey(normMerchant.trim().lowercase())
                        val category =
                            TransactionClassifierHelper.guessCategory(
                                merchant = normMerchant,
                                isCredit = isCredit,
                                isTransfer = isTransferNarrative,
                                body = body,
                                userRules = userRules,
                            )
                        val isTransfer = !isCardBill && (category.equals("Transfer", ignoreCase = true) || category.equals("A/C Transfer", ignoreCase = true))
                        val isCounted = !isCardBill && !isTransfer

                        val accountId =
                            try {
                                val accountRepo = DefaultAccountRepository.getInstance(context)
                                val account =
                                    accountRepo.resolveOrCreateAccount(
                                        bankName = parsed.bankName,
                                        last4 = resolvedAccountLast4,
                                        channel = resolvedChannel,
                                        balanceMinor = parsed.balanceAfterMinor,
                                        timestamp = parsed.dateTime ?: receivedAt,
                                        smsBody = body,
                                    )
                                account.id
                            } catch (_: Throwable) {
                                1L
                            }

                        val capturedLocation = LocationCaptureHelper.getLastKnownLocation(context)

                        val txn =
                            Transaction(
                                id = rawSms.id,
                                smsHash = "$sender|$body|$receivedAt",
                                accountId = accountId,
                                type = type,
                                amount = parsed.amountMinor,
                                currency = parsed.currency,
                                dateTime = parsed.dateTime ?: receivedAt,
                                merchantRaw = rawMerchant ?: normMerchant,
                                merchantNormalised = normMerchant,
                                categorySource = category,
                                channel = resolvedChannel,
                                refNumber = parsed.refNumber,
                                upiId = parsed.upiId,
                                balanceAfter = parsed.balanceAfterMinor,
                                note = body.trim(),
                                parseConfidence = parsed.confidence,
                                isReviewed = isUserCategorized,
                                latitude = capturedLocation?.latitude,
                                longitude = capturedLocation?.longitude,
                                locationLabel = capturedLocation?.label,
                                templateId = parsed.templateId ?: "generic_fallback",
                                accountLast4 = resolvedAccountLast4,
                                bankName = parsed.bankName,
                                sender = sender,
                                isCounted = isCounted,
                            )
                        repo.insertTransaction(txn)
                    }
                } catch (_: Throwable) {
                    // Fail gracefully
                } finally {
                    try {
                        pendingResult.finish()
                    } catch (_: Exception) {
                    }
                }
            }

            try {
                val workRequest =
                    OneTimeWorkRequestBuilder<ImportWorker>()
                        .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            } catch (_: Throwable) {
            }
        }
    }
}
