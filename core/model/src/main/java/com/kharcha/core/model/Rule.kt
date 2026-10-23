package com.kharcha.core.model

data class Rule(
    val id: String,
    val merchantPattern: String,
    val targetCategoryId: String,
)
