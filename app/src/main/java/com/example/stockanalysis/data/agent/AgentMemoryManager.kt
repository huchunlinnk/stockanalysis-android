package com.example.stockanalysis.data.agent

import android.util.Log
import com.example.stockanalysis.data.local.UserMemoryDao
import com.example.stockanalysis.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent 记忆管理器
 *
 * 功能：
 * 1. 记录用户偏好和习惯
 * 2. 跟踪常用股票
 * 3. 保存历史决策
 * 4. 学习用户行为模式
 * 5. 提供个性化建议
 */
@Singleton
class AgentMemoryManager @Inject constructor(
    private val userMemoryDao: UserMemoryDao,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "AgentMemoryManager"
    }

    /**
     * 记录股票访问
     *
     * @param symbol 股票代码
     * @param name 股票名称
     */
    suspend fun recordStockAccess(symbol: String, name: String) = withContext(Dispatchers.IO) {
        try {
            val key = "stock:$symbol"
            val existing = userMemoryDao.getByKey(key)

            if (existing != null) {
                // 更新访问统计
                userMemoryDao.updateAccessStats(existing.id)
            } else {
                // 创建新记录
                val memory = UserMemory(
                    type = MemoryType.FREQUENT_STOCK,
                    key = key,
                    value = name,
                    description = "用户访问股票: $name ($symbol)",
                    confidence = 1.0f,
                    accessCount = 1
                )
                userMemoryDao.insert(memory)
            }

            Log.d(TAG, "Recorded stock access: $symbol")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record stock access", e)
        }
    }

    /**
     * 记录分析决策
     *
     * @param symbol 股票代码
     * @param name 股票名称
     * @param decision 决策（buy/sell/hold）
     * @param reason 决策理由
     * @param score 分析评分
     */
    suspend fun recordDecision(
        symbol: String,
        name: String,
        decision: String,
        reason: String,
        score: Float
    ) = withContext(Dispatchers.IO) {
        try {
            val decisionData = DecisionHistory(
                stockSymbol = symbol,
                stockName = name,
                decision = decision,
                reason = reason,
                timestamp = System.currentTimeMillis()
            )

            val memory = UserMemory(
                type = MemoryType.HISTORICAL_DECISION,
                key = "decision:$symbol:${System.currentTimeMillis()}",
                value = gson.toJson(decisionData),
                description = "决策: $decision - $name ($symbol)",
                confidence = score,
                accessCount = 0
            )

            userMemoryDao.insert(memory)
            Log.d(TAG, "Recorded decision: $decision for $symbol")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record decision", e)
        }
    }

    /**
     * 保存用户偏好
     *
     * @param preference 偏好信息
     */
    suspend fun savePreference(preference: UserPreference) = withContext(Dispatchers.IO) {
        try {
            val key = "pref:${preference.key}"
            val existing = userMemoryDao.getByKey(key)

            val memory = UserMemory(
                id = existing?.id ?: 0,
                type = MemoryType.PREFERENCE,
                key = key,
                value = preference.value,
                description = preference.description,
                confidence = 1.0f,
                accessCount = (existing?.accessCount ?: 0) + 1
            )

            if (existing != null) {
                userMemoryDao.update(memory)
            } else {
                userMemoryDao.insert(memory)
            }

            Log.d(TAG, "Saved preference: ${preference.key} = ${preference.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save preference", e)
        }
    }

    /**
     * 获取用户偏好
     *
     * @param key 偏好键
     * @return 偏好值，如果不存在返回 null
     */
    suspend fun getPreference(key: String): String? = withContext(Dispatchers.IO) {
        try {
            val memory = userMemoryDao.getByKey("pref:$key")
            memory?.value
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get preference", e)
            null
        }
    }

    /**
     * 获取所有用户偏好
     */
    suspend fun getAllPreferences(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val memories = userMemoryDao.getByType(MemoryType.PREFERENCE)
            memories.associate { memory ->
                memory.key.removePrefix("pref:") to memory.value
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all preferences", e)
            emptyMap()
        }
    }

    /**
     * 获取常用股票列表
     *
     * @param limit 返回数量限制
     */
    suspend fun getFrequentStocks(limit: Int = 10): List<StockAccessRecord> =
        withContext(Dispatchers.IO) {
            try {
                val memories = userMemoryDao.getFrequentStocks(limit)
                memories.map { memory ->
                    val symbol = memory.key.removePrefix("stock:")
                    StockAccessRecord(
                        symbol = symbol,
                        name = memory.value,
                        accessCount = memory.accessCount,
                        lastAccessTime = memory.lastAccessedAt,
                        avgScore = null,
                        lastDecision = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get frequent stocks", e)
                emptyList()
            }
        }

    /**
     * 获取历史决策
     *
     * @param symbol 股票代码（可选）
     * @param limit 返回数量限制
     */
    suspend fun getHistoricalDecisions(
        symbol: String? = null,
        limit: Int = 50
    ): List<DecisionHistory> = withContext(Dispatchers.IO) {
        try {
            val memories = if (symbol != null) {
                userMemoryDao.search("decision:$symbol:", limit)
            } else {
                userMemoryDao.getHistoricalDecisions(limit)
            }

            memories.mapNotNull { memory ->
                try {
                    gson.fromJson(memory.value, DecisionHistory::class.java)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse decision: ${memory.value}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get historical decisions", e)
            emptyList()
        }
    }

    /**
     * 记录分析模式
     *
     * @param pattern 分析模式描述
     */
    suspend fun recordAnalysisPattern(pattern: String) = withContext(Dispatchers.IO) {
        try {
            val key = "pattern:${pattern.hashCode()}"
            val existing = userMemoryDao.getByKey(key)

            if (existing != null) {
                userMemoryDao.updateAccessStats(existing.id)
                // 增加置信度
                val newConfidence = (existing.confidence + 0.1f).coerceAtMost(1.0f)
                userMemoryDao.updateConfidence(existing.id, newConfidence)
            } else {
                val memory = UserMemory(
                    type = MemoryType.ANALYSIS_PATTERN,
                    key = key,
                    value = pattern,
                    description = "用户分析模式",
                    confidence = 0.5f,
                    accessCount = 1
                )
                userMemoryDao.insert(memory)
            }

            Log.d(TAG, "Recorded analysis pattern")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record analysis pattern", e)
        }
    }

    /**
     * 学习用户行为
     *
     * 从用户的历史操作中学习规律
     */
    suspend fun learnFromHistory() = withContext(Dispatchers.IO) {
        try {
            val decisions = getHistoricalDecisions(limit = 100)

            // 分析决策倾向
            val buyCount = decisions.count { it.decision == "buy" }
            val sellCount = decisions.count { it.decision == "sell" }
            val holdCount = decisions.count { it.decision == "hold" }

            // 推断风险偏好
            val riskLevel = when {
                buyCount > sellCount * 2 -> "aggressive"
                holdCount > buyCount + sellCount -> "conservative"
                else -> "moderate"
            }

            savePreference(
                UserPreference(
                    key = UserPreference.PREF_RISK_LEVEL,
                    value = riskLevel,
                    description = "从历史决策推断的风险偏好"
                )
            )

            Log.d(TAG, "Learned risk level: $riskLevel (buy=$buyCount, sell=$sellCount, hold=$holdCount)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to learn from history", e)
        }
    }

    /**
     * 生成个性化建议提示
     *
     * 基于用户记忆生成分析时的上下文提示
     */
    suspend fun generateContextPrompt(symbol: String): String = withContext(Dispatchers.IO) {
        try {
            val context = StringBuilder()

            // 获取风险偏好
            val riskLevel = getPreference(UserPreference.PREF_RISK_LEVEL)
            if (riskLevel != null) {
                context.append("用户风险偏好: $riskLevel\n")
            }

            // 获取投资风格
            val investmentStyle = getPreference(UserPreference.PREF_INVESTMENT_STYLE)
            if (investmentStyle != null) {
                context.append("投资风格: $investmentStyle\n")
            }

            // 获取历史决策
            val decisions = getHistoricalDecisions(symbol, limit = 5)
            if (decisions.isNotEmpty()) {
                context.append("\n该股票历史决策:\n")
                decisions.forEach { decision ->
                    context.append("- ${decision.decision}: ${decision.reason}\n")
                }
            }

            context.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate context prompt", e)
            ""
        }
    }

    /**
     * 清理过期记忆
     */
    suspend fun cleanupExpiredMemories() = withContext(Dispatchers.IO) {
        try {
            userMemoryDao.deleteExpired()
            Log.d(TAG, "Cleaned up expired memories")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup expired memories", e)
        }
    }

    /**
     * 获取记忆统计
     */
    suspend fun getMemoryStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            mapOf(
                "total_count" to userMemoryDao.getCount(),
                "frequent_stocks_count" to userMemoryDao.getCountByType(MemoryType.FREQUENT_STOCK),
                "decisions_count" to userMemoryDao.getCountByType(MemoryType.HISTORICAL_DECISION),
                "preferences_count" to userMemoryDao.getCountByType(MemoryType.PREFERENCE),
                "average_confidence" to (userMemoryDao.getAverageConfidence() ?: 0f)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory stats", e)
            emptyMap()
        }
    }

    /**
     * 清空所有记忆
     */
    suspend fun clearAllMemories() = withContext(Dispatchers.IO) {
        try {
            userMemoryDao.deleteAll()
            Log.d(TAG, "Cleared all memories")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear memories", e)
        }
    }

    /**
     * 观察常用股票变化
     */
    fun observeFrequentStocks(): Flow<List<UserMemory>> {
        return userMemoryDao.getByTypeFlow(MemoryType.FREQUENT_STOCK)
    }

    /**
     * 观察所有记忆变化
     */
    fun observeAllMemories(): Flow<List<UserMemory>> {
        return userMemoryDao.getAllFlow()
    }
}
