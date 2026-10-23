package com.kharcha.core.database.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String,
    val merchantPattern: String,
    val targetCategoryId: String,
)
