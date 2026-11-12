package com.namhs.cashnote.di

import android.content.Context
import androidx.room.Room
import com.namhs.cashnote.data.local.dao.BudgetDao
import com.namhs.cashnote.data.local.dao.CategoryDao
import com.namhs.cashnote.data.local.dao.TransactionDao
import com.namhs.cashnote.data.local.database.CashNoteDatabase
import com.namhs.cashnote.data.local.database.DefaultCategoryCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room 데이터베이스와 DAO를 제공한다.
 *
 * 스키마 변경 시 데이터를 지우지 않도록 fallbackToDestructiveMigration은 쓰지 않고,
 * 버전마다 migration을 추가한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CashNoteDatabase =
        Room.databaseBuilder(context, CashNoteDatabase::class.java, CashNoteDatabase.NAME)
            .addCallback(DefaultCategoryCallback())
            .build()

    @Provides
    fun provideTransactionDao(database: CashNoteDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideCategoryDao(database: CashNoteDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideBudgetDao(database: CashNoteDatabase): BudgetDao = database.budgetDao()
}
