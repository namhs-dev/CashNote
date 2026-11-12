package com.namhs.cashnote.di

import com.namhs.cashnote.data.repository.BudgetRepositoryImpl
import com.namhs.cashnote.data.repository.CategoryRepositoryImpl
import com.namhs.cashnote.data.repository.TransactionRepositoryImpl
import com.namhs.cashnote.domain.repository.BudgetRepository
import com.namhs.cashnote.domain.repository.CategoryRepository
import com.namhs.cashnote.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * domain의 Repository 인터페이스에 data의 구현체를 연결한다.
 *
 * 구현체는 상태가 없어 싱글턴으로 두지 않는다. DB·DAO는 DatabaseModule에서 싱글턴으로 제공한다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
}
