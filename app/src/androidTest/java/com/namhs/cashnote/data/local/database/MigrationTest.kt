package com.namhs.cashnote.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 버전별 schemas JSON을 기준으로 migration을 검증한다.
 * 기존 데이터가 남는지와 새 스키마가 Room이 기대하는 모양과 같은지를 확인한다.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CashNoteDatabase::class.java
    )

    @Test
    fun migrate1To2_keepsTransactionsAndAddsBudgets() {
        // v1: 카테고리 1개와 그 카테고리를 쓰는 거래 1건
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, name, type, icon, color) " +
                    "VALUES (1, '식비', 'EXPENSE', 'food', 4293227379)"
            )
            db.execSQL(
                "INSERT INTO transactions " +
                    "(id, amount, type, category_id, memo, date, created_at) " +
                    "VALUES (1, 12500, 'EXPENSE', 1, '점심', 20401, 1762657200000)"
            )
        }

        // v2로 올리면서 스키마가 2.json과 같은지 검증한다
        helper.runMigrationsAndValidate(TEST_DB, 2, true).use { db ->
            db.query("SELECT amount, memo FROM transactions WHERE id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(12_500L, cursor.getLong(0))
                assertEquals("점심", cursor.getString(1))
            }
            db.query("SELECT COUNT(*) FROM budgets").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate2To3_keepsBudgetsAndAddsExchangeRates() {
        // v2: 예산 1건 (2026-11 → epoch month 2026 × 12 + 11 − 1 = 24322)
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL("INSERT INTO budgets (start_month, amount) VALUES (24322, 500000)")
        }

        // v3로 올리면서 스키마가 3.json과 같은지 검증한다
        helper.runMigrationsAndValidate(TEST_DB, 3, true).use { db ->
            db.query("SELECT amount FROM budgets WHERE start_month = 24322").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(500_000L, cursor.getLong(0))
            }
            db.query("SELECT COUNT(*) FROM exchange_rates").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate3To4_keepsTransactionsAsKrwAndAddsForeignColumns() {
        // v3: 카테고리 1개와 원화 거래 1건
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, name, type, icon, color) " +
                    "VALUES (1, '식비', 'EXPENSE', 'food', 4293227379)"
            )
            db.execSQL(
                "INSERT INTO transactions " +
                    "(id, amount, type, category_id, memo, date, created_at) " +
                    "VALUES (1, 12500, 'EXPENSE', 1, '점심', 20401, 1762657200000)"
            )
        }

        // v4로 올리면서 스키마가 4.json과 같은지 검증한다. 기존 거래의 외화 컬럼은 null이어야 한다
        helper.runMigrationsAndValidate(TEST_DB, 4, true).use { db ->
            db.query(
                "SELECT amount, currency_code, foreign_amount, applied_rate " +
                    "FROM transactions WHERE id = 1"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(12_500L, cursor.getLong(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
