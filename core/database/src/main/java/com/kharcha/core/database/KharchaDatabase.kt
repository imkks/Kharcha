package com.kharcha.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kharcha.core.database.dao.RuleDao
import com.kharcha.core.database.dao.RuleEntity
import com.kharcha.core.database.dao.TransactionDao
import com.kharcha.core.database.dao.TransactionEntity

@Database(
    entities = [TransactionEntity::class, RuleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class KharchaDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun ruleDao(): RuleDao
}
