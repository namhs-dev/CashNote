package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.CategoryEntity
import com.namhs.cashnote.domain.model.Category

// Entity ↔ Domain 변환

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = type,
    icon = icon,
    color = color
)

/** id가 0이면 Room이 새 id를 만든다 */
fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type,
    icon = icon,
    color = color
)
