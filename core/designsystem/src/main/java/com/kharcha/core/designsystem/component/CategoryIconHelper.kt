package com.kharcha.core.designsystem.component

import androidx.annotation.DrawableRes
import com.kharcha.core.designsystem.R
import com.kharcha.core.model.Channel

object CategoryIconHelper {
    @DrawableRes
    fun getCategoryIcon(categoryName: String?): Int {
        val lower = categoryName?.lowercase()?.trim() ?: return R.drawable.ic_category_other
        return when {
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("diesel") ->
                R.drawable.ic_category_fuel

            lower.contains("donation") || lower.contains("charity") || lower.contains("trust") ->
                R.drawable.ic_category_donation

            lower.contains("emi") || lower.contains("loan") ->
                R.drawable.ic_category_emi

            lower.contains("shared") || lower.contains("split") ->
                R.drawable.ic_category_shared

            lower.contains("business") ->
                R.drawable.ic_category_business

            lower.contains("interest") || lower.contains("savings") ->
                R.drawable.ic_category_savings

            lower.contains("recharge") ->
                R.drawable.ic_category_recharge

            lower.contains("refund") ->
                R.drawable.ic_category_refund

            lower.contains("reimburse") ->
                R.drawable.ic_category_reimbursement

            lower.contains("reward") || lower.contains("cashback") ->
                R.drawable.ic_category_rewards

            lower.contains("credit") ->
                R.drawable.ic_category_credit

            lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") ||
                lower.contains("cafe") -> R.drawable.ic_category_food

            lower.contains("grocer") || lower.contains("supermarket") ||
                lower.contains("mart") -> R.drawable.ic_category_groceries

            lower.contains("shop") || lower.contains("store") || lower.contains("fashion") ||
                lower.contains("retail") -> R.drawable.ic_category_shopping

            lower.contains("transport") || lower.contains("travel") || lower.contains("cab") ->
                R.drawable.ic_category_transport

            lower.contains("bill") || lower.contains("utility") || lower.contains("utilities") ||
                lower.contains("electricity") -> R.drawable.ic_category_bills

            lower.contains("entertain") || lower.contains("movie") || lower.contains("music") ||
                lower.contains("ott") -> R.drawable.ic_category_entertainment

            lower.contains("health") || lower.contains("medical") || lower.contains("pharmacy") ||
                lower.contains("doctor") -> R.drawable.ic_category_health

            lower.contains("invest") || lower.contains("mutual fund") || lower.contains("stock") ||
                lower.contains("share") -> R.drawable.ic_category_investments

            lower.contains("salary") -> R.drawable.ic_category_salary

            lower.contains("deposit") -> R.drawable.ic_bank

            lower.contains("transfer") || lower.contains("a/c transfer") ->
                R.drawable.ic_category_transfer

            else -> R.drawable.ic_category_other
        }
    }

    @DrawableRes
    fun getChannelIcon(channel: Channel?): Int {
        return when (channel) {
            Channel.UPI -> R.drawable.ic_channel_upi
            Channel.CARD -> R.drawable.ic_channel_card
            Channel.ATM -> R.drawable.ic_channel_atm
            Channel.WALLET -> R.drawable.ic_channel_wallet
            Channel.NEFT, Channel.IMPS -> R.drawable.ic_category_transfer
            else -> R.drawable.ic_category_other
        }
    }
}
