package com.kharcha.core.database.di

import android.content.Context
import androidx.room.Room
import com.kharcha.core.database.KharchaDatabase
import com.kharcha.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideKharchaDatabase(
        @ApplicationContext context: Context,
    ): KharchaDatabase {
        return Room.databaseBuilder(
            context,
            KharchaDatabase::class.java,
            "kharcha_db",
        ).build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: KharchaDatabase): TransactionDao = database.transactionDao()
}
