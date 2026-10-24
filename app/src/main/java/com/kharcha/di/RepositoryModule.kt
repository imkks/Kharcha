package com.kharcha.di

import android.content.Context
import com.kharcha.core.common.DefaultTransactionRepository
import com.kharcha.core.common.DefaultUserPreferences
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.common.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideTransactionRepository(@ApplicationContext context: Context): TransactionRepository {
        return DefaultTransactionRepository.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return DefaultUserPreferences(context)
    }
}
