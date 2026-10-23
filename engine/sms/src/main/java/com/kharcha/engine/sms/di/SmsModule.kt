package com.kharcha.engine.sms.di

import android.content.ContentResolver
import android.content.Context
import com.kharcha.engine.sms.DefaultSmsRepository
import com.kharcha.engine.sms.SmsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmsModule {
    @Binds
    @Singleton
    abstract fun bindSmsRepository(impl: DefaultSmsRepository): SmsRepository

    companion object {
        @Provides
        @Singleton
        fun provideContentResolver(
            @ApplicationContext context: Context,
        ): ContentResolver {
            return context.contentResolver
        }
    }
}
