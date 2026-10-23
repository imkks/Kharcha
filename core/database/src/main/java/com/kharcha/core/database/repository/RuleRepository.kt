package com.kharcha.core.database.repository

import com.kharcha.core.model.Rule

interface RuleRepository {
    suspend fun insertRule(rule: Rule)
}
