package com.kharcha.engine.sms

import javax.inject.Inject

class SenderFilter
    @Inject
    constructor() {
        fun isFinancialSender(sender: String): Boolean {
            // Drop standard 10 digit numbers (with or without +91)
            if (sender.matches(Regex("^(\\+91)?[0-9]{10}$"))) return false
            // Financial senders usually have alphabets e.g., XX-HDFCBK
            if (!sender.contains(Regex("[a-zA-Z]"))) return false
            return true
        }
    }
