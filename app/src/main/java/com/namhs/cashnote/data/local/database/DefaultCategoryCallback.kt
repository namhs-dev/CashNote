package com.namhs.cashnote.data.local.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.namhs.cashnote.data.local.converter.Converters
import com.namhs.cashnote.data.local.entity.CategoryEntity

/**
 * DB가 처음 만들어질 때 기본 카테고리를 넣는다.
 *
 * onCreate는 DB 생성과 같은 흐름에서 실행되므로, 첫 화면이 뜨기 전에 항상 데이터가 들어 있다.
 * DAO는 아직 쓸 수 없어서 SQL로 직접 넣는다.
 */
class DefaultCategoryCallback : RoomDatabase.Callback() {
    private val converters = Converters()

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        DefaultCategories.all.forEach { category ->
            db.insert(TABLE_NAME, SQLiteDatabase.CONFLICT_ABORT, category.toContentValues())
        }
    }

    // 컬럼 이름과 저장 형식은 CategoryEntity, Converters와 같아야 한다.
    private fun CategoryEntity.toContentValues() = ContentValues().apply {
        put("id", id)
        put("name", name)
        put("type", converters.fromTransactionType(type))
        put("icon", icon)
        put("color", color)
    }

    private companion object {
        const val TABLE_NAME = "categories"
    }
}
