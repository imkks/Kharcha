package com.kharcha.core.database.di

import android.content.Context
import androidx.room.Room
import com.kharcha.core.database.KharchaDatabase
import com.kharcha.core.database.dao.RuleDao
import com.kharcha.core.database.dao.TransactionDao
import com.kharcha.core.database.repository.RuleRepository
import com.kharcha.core.database.repository.RuleRepositoryImpl
import com.kharcha.core.database.repository.TransactionRepository
import com.kharcha.core.database.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindsModule {
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseProvidesModule {
    @Provides
    @Singleton
    fun provideKharchaDatabase(
        @ApplicationContext context: Context,
    ): KharchaDatabase {
        return Room.databaseBuilder(
            context,
            KharchaDatabase::class.java,
            "kharcha_database",
        ).build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: KharchaDatabase): TransactionDao = database.transactionDao()

    @Provides
    @Singleton
    fun provideRuleDao(database: KharchaDatabase): RuleDao = database.ruleDao()
}
