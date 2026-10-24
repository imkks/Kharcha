package com.kharcha.core.designsystem.component

import com.kharcha.core.designsystem.R
import com.kharcha.core.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryIconHelperTest {
    @Test
    fun testCategoryIconMapping() {
        assertEquals(R.drawable.ic_category_food, CategoryIconHelper.getCategoryIcon("Food & Dining"))
        assertEquals(R.drawable.ic_category_groceries, CategoryIconHelper.getCategoryIcon("Groceries"))
        assertEquals(R.drawable.ic_category_shopping, CategoryIconHelper.getCategoryIcon("Shopping"))
        assertEquals(R.drawable.ic_category_transport, CategoryIconHelper.getCategoryIcon("Transport"))
        assertEquals(R.drawable.ic_category_bills, CategoryIconHelper.getCategoryIcon("Bills & Utilities"))
        assertEquals(R.drawable.ic_category_entertainment, CategoryIconHelper.getCategoryIcon("Entertainment"))
        assertEquals(R.drawable.ic_category_health, CategoryIconHelper.getCategoryIcon("Health"))
        assertEquals(R.drawable.ic_category_investments, CategoryIconHelper.getCategoryIcon("Investments"))
        assertEquals(R.drawable.ic_category_salary, CategoryIconHelper.getCategoryIcon("Salary"))
        assertEquals(R.drawable.ic_category_transfer, CategoryIconHelper.getCategoryIcon("Transfer"))
        assertEquals(R.drawable.ic_category_other, CategoryIconHelper.getCategoryIcon("Miscellaneous"))
        assertEquals(R.drawable.ic_category_other, CategoryIconHelper.getCategoryIcon(null))
    }

    @Test
    fun testChannelIconMapping() {
        assertEquals(R.drawable.ic_channel_upi, CategoryIconHelper.getChannelIcon(Channel.UPI))
        assertEquals(R.drawable.ic_channel_card, CategoryIconHelper.getChannelIcon(Channel.CARD))
        assertEquals(R.drawable.ic_channel_atm, CategoryIconHelper.getChannelIcon(Channel.ATM))
        assertEquals(R.drawable.ic_channel_wallet, CategoryIconHelper.getChannelIcon(Channel.WALLET))
        assertEquals(R.drawable.ic_category_transfer, CategoryIconHelper.getChannelIcon(Channel.NEFT))
        assertEquals(R.drawable.ic_category_transfer, CategoryIconHelper.getChannelIcon(Channel.IMPS))
        assertEquals(R.drawable.ic_category_other, CategoryIconHelper.getChannelIcon(null))
    }
}
