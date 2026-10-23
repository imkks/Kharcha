package com.kharcha.core.database.repository

import com.kharcha.core.database.dao.RuleDao
import com.kharcha.core.database.dao.RuleEntity
import com.kharcha.core.model.Rule
import javax.inject.Inject

class RuleRepositoryImpl
    @Inject
    constructor(
        private val ruleDao: RuleDao,
    ) : RuleRepository {
        override suspend fun insertRule(rule: Rule) {
            ruleDao.insertRule(
                RuleEntity(
                    id = rule.id,
                    merchantPattern = rule.merchantPattern,
                    targetCategoryId = rule.targetCategoryId,
                ),
            )
        }
    }
