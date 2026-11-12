package com.namhs.cashnote.data.local.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.namhs.cashnote.data.local.converter.Converters
import com.namhs.cashnote.data.local.dao.BudgetDao
import com.namhs.cashnote.data.local.dao.CategoryDao
import com.namhs.cashnote.data.local.dao.TransactionDao
import com.namhs.cashnote.data.local.entity.BudgetEntity
import com.namhs.cashnote.data.local.entity.CategoryEntity
import com.namhs.cashnote.data.local.entity.TransactionEntity

/**
 * 앱의 유일한 데이터 소스인 Room 데이터베이스.
 *
 * 스키마가 바뀌면 version을 올리고 migration을 추가한다.
 */
@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // 2: budgets 테이블 추가. 테이블만 늘어나므로 자동 migration으로 충분하다
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class CashNoteDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun budgetDao(): BudgetDao

    companion object {
        /** DB 파일 이름 */
        const val NAME = "cashnote.db"
    }
}
