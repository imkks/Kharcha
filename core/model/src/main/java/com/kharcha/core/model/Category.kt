package com.kharcha.core.model

data class Category(
    val id: String,
    val name: String,
    val parentId: String?,
    val icon: String,
    val type: TransactionType,
)
