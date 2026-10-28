package com.pennywiseai.parser.core.test

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.BankParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import java.math.BigDecimal

data class ExpectedTransaction(
    val amount: BigDecimal,
    val currency: String = "INR",
    val type: TransactionType,
    val merchant: String? = null,
    val reference: String? = null,
    val accountLast4: String? = null,
    val balance: BigDecimal? = null,
    val creditLimit: BigDecimal? = null,
    val isFromCard: Boolean? = null,
)

data class ParserTestCase(
    val name: String,
    val message: String,
    val sender: String,
    val expected: ExpectedTransaction? = null,
    val shouldParse: Boolean = true,
    val description: String = "",
)

object ParserTestHelper {
    fun assertCases(parser: BankParser, cases: List<ParserTestCase>) {
        for (case in cases) {
            val result = parser.parse(case.message, case.sender, 0L)
            if (!case.shouldParse) {
                assertNull("Expected null for '${case.name}', but got $result", result)
                continue
            }
            assertNotNull("Expected parse success for '${case.name}'", result)
            val expected = case.expected ?: continue
            assertEquals("Amount mismatch in '${case.name}'", expected.amount, result!!.amount)
            assertEquals("Type mismatch in '${case.name}'", expected.type, result.type)
            if (expected.merchant != null) {
                assertEquals("Merchant mismatch in '${case.name}'", expected.merchant, result.merchant)
            }
            if (expected.accountLast4 != null) {
                assertEquals("Account mismatch in '${case.name}'", expected.accountLast4, result.accountLast4)
            }
            if (expected.reference != null) {
                assertEquals("Reference mismatch in '${case.name}'", expected.reference, result.reference)
            }
            if (expected.balance != null) {
                assertEquals("Balance mismatch in '${case.name}'", expected.balance, result.balance)
            }
        }
    }
}
