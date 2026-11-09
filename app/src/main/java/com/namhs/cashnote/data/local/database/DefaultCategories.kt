package com.namhs.cashnote.data.local.database

import com.namhs.cashnote.data.local.entity.CategoryEntity
import com.namhs.cashnote.domain.model.TransactionType

/**
 * DB 최초 생성 시 넣을 기본 카테고리.
 *
 * id를 고정해 두어 어디서든 같은 id로 기본 카테고리를 찾을 수 있게 한다.
 * 이름은 UI 문자열이 아니라 DB 초기 데이터이므로 strings.xml이 아닌 코드에 둔다.
 * 실제 삽입은 RoomDatabase.Callback.onCreate에서 한다.
 */
object DefaultCategories {
    val all: List<CategoryEntity> = listOf(
        // 지출
        CategoryEntity(1, "식비", TransactionType.EXPENSE, "food", 0xFFFF8A65),
        CategoryEntity(2, "교통", TransactionType.EXPENSE, "transport", 0xFF4FC3F7),
        CategoryEntity(3, "쇼핑", TransactionType.EXPENSE, "shopping", 0xFFBA68C8),
        CategoryEntity(4, "주거·통신", TransactionType.EXPENSE, "housing", 0xFF7986CB),
        CategoryEntity(5, "의료·건강", TransactionType.EXPENSE, "health", 0xFFE57373),
        CategoryEntity(6, "문화·여가", TransactionType.EXPENSE, "leisure", 0xFFFFD54F),
        CategoryEntity(7, "기타 지출", TransactionType.EXPENSE, "etc", 0xFF90A4AE),
        // 수입
        CategoryEntity(8, "급여", TransactionType.INCOME, "salary", 0xFF81C784),
        CategoryEntity(9, "용돈", TransactionType.INCOME, "allowance", 0xFF4DB6AC),
        CategoryEntity(10, "기타 수입", TransactionType.INCOME, "etc", 0xFFA1887F)
    )
}
