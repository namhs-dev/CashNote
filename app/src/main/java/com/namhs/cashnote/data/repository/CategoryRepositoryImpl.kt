package com.namhs.cashnote.data.repository

import com.namhs.cashnote.data.local.dao.CategoryDao
import com.namhs.cashnote.data.repository.mapper.toDomain
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl @Inject constructor(private val categoryDao: CategoryDao) :
    CategoryRepository {
    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeCategories(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeByType(type).map { entities -> entities.map { it.toDomain() } }
}
