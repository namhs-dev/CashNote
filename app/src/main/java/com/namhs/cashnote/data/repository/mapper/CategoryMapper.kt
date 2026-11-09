package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.CategoryEntity
import com.namhs.cashnote.domain.model.Category

// Entity → Domain 변환. 카테고리 쓰기 기능이 생기면 toEntity를 추가한다.

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = type,
    icon = icon,
    color = color
)
