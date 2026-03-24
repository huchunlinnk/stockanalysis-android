# Agent Memory 系统实现报告

**日期**: 2026-03-23
**状态**: ✅ 已完成
**编译**: ✅ BUILD SUCCESSFUL

---

## 实现概述

实现了完整的 Agent Memory 系统，使应用能够记住用户的偏好、习惯和历史决策，提供更加个性化和智能的分析服务。

---

## 核心功能

### 1. 用户记忆类型

| 类型 | 说明 | 用途 |
|------|------|------|
| **PREFERENCE** | 用户偏好 | 风险偏好、分析深度、通知频率等 |
| **FREQUENT_STOCK** | 常用股票 | 跟踪用户经常分析的股票 |
| **HISTORICAL_DECISION** | 历史决策 | 记录买卖决策及理由 |
| **ANALYSIS_PATTERN** | 分析模式 | 用户常用的分析参数 |
| **MARKET_VIEW** | 市场观点 | 用户对股票/行业的看法 |
| **RISK_TOLERANCE** | 风险承受 | 风险评级和偏好 |
| **INVESTMENT_GOAL** | 投资目标 | 投资目标和期望收益 |
| **CUSTOM_TAG** | 自定义标签 | 用户为股票添加的标签 |
| **LEARNED_PATTERN** | 学习模式 | Agent 从行为中学到的模式 |

---

## 数据模型

### 1. UserMemory (核心实体)

```kotlin
@Entity(tableName = "user_memory")
data class UserMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: MemoryType,          // 记忆类型
    val key: String,                // 记忆键（快速查询）
    val value: String,              // 记忆值
    val description: String = "",   // 描述
    val confidence: Float = 1.0f,   // 置信度 (0.0-1.0)
    val accessCount: Int = 0,       // 访问次数
    val createdAt: Long,            // 创建时间
    val lastAccessedAt: Long,       // 最后访问时间
    val expiresAt: Long? = null     // 过期时间（可选）
)
```

**字段说明**:
- **confidence**: 置信度，表示记忆的可靠程度
  - 1.0 = 用户明确设置
  - 0.5-0.9 = Agent 推断
  - <0.5 = 不确定的推测
- **accessCount**: 访问次数越多，说明越重要
- **expiresAt**: 支持过期机制，自动清理过时数据

---

### 2. UserPreference (用户偏好)

```kotlin
data class UserPreference(
    val key: String,
    val value: String,
    val description: String = ""
)

companion object {
    // 预定义偏好键
    const val PREF_RISK_LEVEL = "risk_level"           // conservative/moderate/aggressive
    const val PREF_INVESTMENT_STYLE = "investment_style" // value/growth/dividend
    const val PREF_TIME_HORIZON = "time_horizon"       // short/medium/long
    const val PREF_ANALYSIS_DEPTH = "analysis_depth"   // basic/detailed/comprehensive
    const val PREF_NOTIFICATION_FREQ = "notification_freq" // realtime/daily/weekly
    const val PREF_FAVORITE_INDICATORS = "favorite_indicators" // 喜欢的指标
    const val PREF_AUTO_ANALYSIS = "auto_analysis"     // 自动分析开关
}
```

---

### 3. StockAccessRecord (股票访问记录)

```kotlin
data class StockAccessRecord(
    val symbol: String,
    val name: String,
    val accessCount: Int,
    val lastAccessTime: Long,
    val avgScore: Float?,      // 平均评分
    val lastDecision: String?  // 最后决策
)
```

---

### 4. DecisionHistory (决策历史)

```kotlin
data class DecisionHistory(
    val stockSymbol: String,
    val stockName: String,
    val decision: String,      // buy/sell/hold
    val reason: String,        // 决策理由
    val timestamp: Long,
    val actualOutcome: String? = null  // 实际结果（用于学习）
)
```

---

## 核心组件

### 1. UserMemoryDao (数据访问层)

**完整的 CRUD 操作**:

```kotlin
@Dao
interface UserMemoryDao {
    // 基础操作
    suspend fun insert(memory: UserMemory): Long
    suspend fun update(memory: UserMemory)
    suspend fun delete(memory: UserMemory)
    suspend fun getById(id: Long): UserMemory?

    // 查询操作
    suspend fun getByKey(key: String): UserMemory?
    suspend fun getByType(type: MemoryType): List<UserMemory>
    suspend fun search(query: String, limit: Int): List<UserMemory>
    suspend fun getRecent(limit: Int): List<UserMemory>
    suspend fun getMostAccessed(limit: Int): List<UserMemory>
    suspend fun getHighConfidence(minConfidence: Float): List<UserMemory>

    // 统计操作
    suspend fun getCount(): Int
    suspend fun getCountByType(type: MemoryType): Int
    suspend fun getAverageConfidence(): Float?

    // 特定查询
    suspend fun getFrequentStocks(limit: Int): List<UserMemory>
    suspend fun getPreferences(): List<UserMemory>
    suspend fun getHistoricalDecisions(limit: Int): List<UserMemory>

    // 维护操作
    suspend fun updateAccessStats(id: Long, timestamp: Long)
    suspend fun updateConfidence(id: Long, confidence: Float)
    suspend fun deleteExpired(currentTime: Long)

    // Flow 观察
    fun getByTypeFlow(type: MemoryType): Flow<List<UserMemory>>
    fun getAllFlow(): Flow<List<UserMemory>>
}
```

**特点**:
- ✅ 完整的 CRUD
- ✅ 丰富的查询方法
- ✅ 统计和分析
- ✅ Flow 响应式编程
- ✅ 自动维护机制

---

### 2. AgentMemoryManager (记忆管理器)

**核心功能**:

#### a. 记录股票访问

```kotlin
suspend fun recordStockAccess(symbol: String, name: String)
```

**行为**:
- 首次访问：创建新记录
- 再次访问：更新访问统计（accessCount++）
- 自动更新 lastAccessedAt

**用途**:
- 跟踪用户最关注的股票
- 生成"常用股票"列表
- 个性化首页推荐

---

#### b. 记录分析决策

```kotlin
suspend fun recordDecision(
    symbol: String,
    name: String,
    decision: String,   // buy/sell/hold
    reason: String,
    score: Float
)
```

**行为**:
- 保存完整的决策上下文
- 记录决策时间和理由
- 存储分析评分作为置信度

**用途**:
- 分析用户决策倾向
- 学习用户投资风格
- 提供历史参考

---

#### c. 保存用户偏好

```kotlin
suspend fun savePreference(preference: UserPreference)
suspend fun getPreference(key: String): String?
suspend fun getAllPreferences(): Map<String, String>
```

**示例**:
```kotlin
// 保存风险偏好
memoryManager.savePreference(
    UserPreference(
        key = UserPreference.PREF_RISK_LEVEL,
        value = "aggressive",
        description = "用户自定义风险偏好"
    )
)

// 获取偏好
val riskLevel = memoryManager.getPreference(UserPreference.PREF_RISK_LEVEL)
// 返回: "aggressive"
```

---

#### d. 获取常用股票

```kotlin
suspend fun getFrequentStocks(limit: Int = 10): List<StockAccessRecord>
```

**返回示例**:
```kotlin
[
    StockAccessRecord(
        symbol = "000001",
        name = "平安银行",
        accessCount = 15,
        lastAccessTime = 1711180800000
    ),
    StockAccessRecord(
        symbol = "600036",
        name = "招商银行",
        accessCount = 12,
        lastAccessTime = 1711094400000
    )
]
```

**用途**:
- 首页快捷访问
- 推荐相关股票
- 自动订阅更新

---

#### e. 获取历史决策

```kotlin
suspend fun getHistoricalDecisions(
    symbol: String? = null,
    limit: Int = 50
): List<DecisionHistory>
```

**功能**:
- 支持全局查询或按股票查询
- 按时间倒序排列
- 返回完整决策上下文

**用途**:
- 分析页面显示历史决策
- 学习用户决策模式
- 生成投资报告

---

#### f. 学习用户行为

```kotlin
suspend fun learnFromHistory()
```

**学习逻辑**:
```kotlin
// 1. 统计历史决策
val buyCount = decisions.count { it.decision == "buy" }
val sellCount = decisions.count { it.decision == "sell" }
val holdCount = decisions.count { it.decision == "hold" }

// 2. 推断风险偏好
val riskLevel = when {
    buyCount > sellCount * 2 -> "aggressive"   // 激进型
    holdCount > buyCount + sellCount -> "conservative" // 保守型
    else -> "moderate"                          // 稳健型
}

// 3. 自动保存推断结果
savePreference(
    UserPreference(
        key = PREF_RISK_LEVEL,
        value = riskLevel,
        description = "从历史决策推断"
    )
)
```

**触发时机**:
- 应用启动时
- 完成分析后（后台异步）
- 用户主动触发（设置页面）

---

#### g. 生成个性化提示

```kotlin
suspend fun generateContextPrompt(symbol: String): String
```

**生成示例**:
```
用户风险偏好: aggressive
投资风格: growth

该股票历史决策:
- buy: 技术面强势突破，成交量放大
- hold: 等待回调至支撑位
- buy: 基本面改善，估值合理
```

**用途**:
- 传递给 LLM 作为上下文
- 生成更符合用户偏好的分析
- 提供个性化建议

---

#### h. 记忆统计

```kotlin
suspend fun getMemoryStats(): Map<String, Any>
```

**返回示例**:
```kotlin
{
    "total_count" = 125,
    "frequent_stocks_count" = 8,
    "decisions_count" = 45,
    "preferences_count" = 7,
    "average_confidence" = 0.87
}
```

**用途**:
- 设置页面显示统计
- 监控记忆质量
- 调试和优化

---

## 集成示例

### 1. 在分析流程中集成

```kotlin
class AnalysisEngine @Inject constructor(
    private val agentMemoryManager: AgentMemoryManager
) {
    suspend fun analyzeStock(symbol: String, name: String) {
        // 1. 记录访问
        agentMemoryManager.recordStockAccess(symbol, name)

        // 2. 获取个性化上下文
        val context = agentMemoryManager.generateContextPrompt(symbol)

        // 3. 执行分析（包含上下文）
        val result = performAnalysis(symbol, context)

        // 4. 记录决策
        agentMemoryManager.recordDecision(
            symbol = symbol,
            name = name,
            decision = result.decision,
            reason = result.reason,
            score = result.score
        )

        // 5. 后台学习
        launch(Dispatchers.IO) {
            agentMemoryManager.learnFromHistory()
        }
    }
}
```

---

### 2. 在首页显示常用股票

```kotlin
class HomeViewModel @Inject constructor(
    private val agentMemoryManager: AgentMemoryManager
) : ViewModel() {

    val frequentStocks = liveData {
        val stocks = agentMemoryManager.getFrequentStocks(limit = 5)
        emit(stocks)
    }
}

// Fragment/Activity
viewModel.frequentStocks.observe(this) { stocks ->
    stocks.forEach { stock ->
        println("${stock.name} (${stock.symbol}) - 访问 ${stock.accessCount} 次")
    }
}
```

---

### 3. 设置页面管理偏好

```kotlin
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var agentMemoryManager: AgentMemoryManager

    private fun saveRiskPreference(level: String) {
        lifecycleScope.launch {
            agentMemoryManager.savePreference(
                UserPreference(
                    key = UserPreference.PREF_RISK_LEVEL,
                    value = level,
                    description = "用户风险偏好设置"
                )
            )
        }
    }

    private fun loadPreferences() {
        lifecycleScope.launch {
            val prefs = agentMemoryManager.getAllPreferences()
            binding.tvRiskLevel.text = prefs[UserPreference.PREF_RISK_LEVEL] ?: "未设置"
        }
    }

    private fun showMemoryStats() {
        lifecycleScope.launch {
            val stats = agentMemoryManager.getMemoryStats()
            binding.tvTotalMemories.text = "共 ${stats["total_count"]} 条记忆"
            binding.tvAvgConfidence.text = "平均置信度: ${stats["average_confidence"]}"
        }
    }
}
```

---

### 4. 使用 Flow 实时观察

```kotlin
class FrequentStocksFragment : Fragment() {

    @Inject
    lateinit var agentMemoryManager: AgentMemoryManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            agentMemoryManager.observeFrequentStocks()
                .collect { memories ->
                    updateUI(memories)
                }
        }
    }
}
```

---

## 数据库变更

### StockDatabase 更新

**版本**: 4 → 5

**变更**:
```kotlin
@Database(
    entities = [
        // ... 其他实体 ...
        UserMemory::class  // 新增
    ],
    version = 5,  // 版本升级
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    // ... 其他 DAO ...
    abstract fun userMemoryDao(): UserMemoryDao  // 新增
}
```

**迁移策略**: `fallbackToDestructiveMigration()`
- 开发阶段直接重建数据库
- 生产环境需实现 Migration

---

## 使用场景

### 1. 个性化首页

**场景**: 用户打开应用，首页显示常用股票

```kotlin
// 显示最近访问的 5 只股票
val frequentStocks = agentMemoryManager.getFrequentStocks(5)

// UI 展示
频繁访问:
  平安银行 (000001) - 访问 15 次
  招商银行 (600036) - 访问 12 次
  贵州茅台 (600519) - 访问 10 次
```

---

### 2. 智能推荐

**场景**: 基于历史决策推荐相似股票

```kotlin
// 1. 分析历史决策
val decisions = agentMemoryManager.getHistoricalDecisions()
val boughtStocks = decisions.filter { it.decision == "buy" }
    .map { it.stockSymbol }

// 2. 推荐同板块股票
val recommendations = findSimilarStocks(boughtStocks)

// 3. 展示推荐
根据您的投资偏好，推荐:
  - 宁波银行 (与已买入的平安银行相似)
  - 兴业银行 (与已买入的招商银行相似)
```

---

### 3. 分析报告生成

**场景**: 生成个性化的分析报告

```kotlin
val context = agentMemoryManager.generateContextPrompt("000001")

// LLM 提示词
"""
用户画像:
$context

请基于用户的风险偏好和投资风格，为平安银行 (000001) 生成分析报告。
"""

// 生成的报告会更符合用户偏好
```

---

### 4. 投资复盘

**场景**: 用户查看历史决策和结果

```kotlin
val history = agentMemoryManager.getHistoricalDecisions(symbol = "000001")

// 展示时间线
2026-03-20: buy - 技术面突破，买入 ✅ 盈利 +5.2%
2026-03-15: hold - 等待回调
2026-03-10: sell - 短期超买，卖出 ✅ 盈利 +3.1%
```

---

## 隐私和安全

### 1. 数据存储

**位置**: 本地 SQLite 数据库
- ✅ 不上传到服务器
- ✅ 仅存储在设备本地
- ✅ 应用卸载后自动清除

---

### 2. 敏感信息处理

**不存储的信息**:
- ❌ 真实资金数额
- ❌ 银行账户信息
- ❌ 个人身份信息 (PII)

**仅存储的信息**:
- ✅ 股票代码和名称
- ✅ 分析决策 (buy/sell/hold)
- ✅ 用户偏好设置
- ✅ 访问统计

---

### 3. 用户控制

```kotlin
// 清空所有记忆
agentMemoryManager.clearAllMemories()

// 删除特定类型
userMemoryDao.deleteByType(MemoryType.HISTORICAL_DECISION)

// 删除过期数据
agentMemoryManager.cleanupExpiredMemories()
```

**设置页面功能**:
- 查看记忆统计
- 清空所有记忆
- 导出记忆数据
- 导入记忆数据

---

## 性能优化

### 1. 索引优化

```kotlin
@Entity(
    tableName = "user_memory",
    indices = [
        Index(value = ["type"]),
        Index(value = ["key"]),
        Index(value = ["lastAccessedAt"]),
        Index(value = ["accessCount"])
    ]
)
```

**优化查询**:
- 按类型查询: O(log n)
- 按键查询: O(log n)
- 排序查询: O(log n)

---

### 2. 批量操作

```kotlin
// 批量插入
userMemoryDao.insertAll(memories)

// 使用事务
database.runInTransaction {
    memories.forEach { memory ->
        userMemoryDao.insert(memory)
    }
}
```

---

### 3. 后台执行

```kotlin
// 使用 Dispatchers.IO
withContext(Dispatchers.IO) {
    agentMemoryManager.recordStockAccess(symbol, name)
}

// 异步学习
lifecycleScope.launch(Dispatchers.IO) {
    agentMemoryManager.learnFromHistory()
}
```

---

## 未来扩展

### 1. 记忆同步

```kotlin
// 同步到云端（可选）
suspend fun syncToCloud() {
    val memories = userMemoryDao.getAll()
    val json = gson.toJson(memories)
    cloudStorage.upload("user_memory.json", json)
}

// 从云端恢复
suspend fun restoreFromCloud() {
    val json = cloudStorage.download("user_memory.json")
    val memories = gson.fromJson(json, Array<UserMemory>::class.java)
    userMemoryDao.insertAll(memories.toList())
}
```

---

### 2. 高级学习算法

```kotlin
// 协同过滤推荐
suspend fun recommendBasedOnSimilarUsers() {
    val myDecisions = getHistoricalDecisions()
    val similarUsers = findSimilarUsers(myDecisions)
    val recommendations = aggregateRecommendations(similarUsers)
    return recommendations
}

// 时间序列分析
suspend fun predictFutureDecision(symbol: String): String {
    val history = getHistoricalDecisions(symbol)
    val pattern = analyzeTimePattern(history)
    return pattern.predict()
}
```

---

### 3. 记忆压缩

```kotlin
// 合并相似记忆
suspend fun compressMemories() {
    val memories = userMemoryDao.getAll()
    val compressed = memories.groupBy { it.key }
        .map { (key, group) ->
            group.maxByOrNull { it.accessCount }!!
        }
    userMemoryDao.deleteAll()
    userMemoryDao.insertAll(compressed)
}
```

---

## 代码统计

### 文件统计

| 文件 | 操作 | 行数 | 说明 |
|------|------|------|------|
| `UserMemory.kt` | 新增 | 180 | 数据模型定义 |
| `UserMemoryDao.kt` | 新增 | 180 | DAO 接口 |
| `AgentMemoryManager.kt` | 新增 | 320 | 记忆管理器 |
| `StockDatabase.kt` | 修改 | +3 | 添加 UserMemory 和 DAO |
| `DatabaseModule.kt` | 修改 | +6 | Hilt 依赖注入 |

**总计**:
- 新增文件: 3 个
- 修改文件: 2 个
- 新增代码: ~680 行

---

## 测试建议

### 1. 单元测试

```kotlin
@Test
fun testRecordStockAccess() = runTest {
    // 首次访问
    memoryManager.recordStockAccess("000001", "平安银行")
    val memory1 = userMemoryDao.getByKey("stock:000001")
    assertEquals(1, memory1?.accessCount)

    // 再次访问
    memoryManager.recordStockAccess("000001", "平安银行")
    val memory2 = userMemoryDao.getByKey("stock:000001")
    assertEquals(2, memory2?.accessCount)
}

@Test
fun testLearnFromHistory() = runTest {
    // 模拟 10 次买入决策
    repeat(10) {
        memoryManager.recordDecision("00000$it", "股票$it", "buy", "理由", 0.8f)
    }

    // 学习
    memoryManager.learnFromHistory()

    // 验证推断结果
    val riskLevel = memoryManager.getPreference(UserPreference.PREF_RISK_LEVEL)
    assertEquals("aggressive", riskLevel)
}
```

---

### 2. 集成测试

```kotlin
@Test
fun testEndToEndWorkflow() = runTest {
    // 1. 用户分析股票
    memoryManager.recordStockAccess("000001", "平安银行")

    // 2. 做出决策
    memoryManager.recordDecision("000001", "平安银行", "buy", "技术突破", 0.9f)

    // 3. 获取常用股票
    val frequentStocks = memoryManager.getFrequentStocks()
    assertTrue(frequentStocks.any { it.symbol == "000001" })

    // 4. 生成上下文
    val context = memoryManager.generateContextPrompt("000001")
    assertTrue(context.contains("buy"))
}
```

---

## 总结

### ✅ 已实现

1. **UserMemory 数据模型** - 完整的记忆实体定义
2. **UserMemoryDao** - 丰富的数据库操作接口
3. **AgentMemoryManager** - 高级记忆管理功能
4. **数据库集成** - 无缝集成到 Room 数据库
5. **Hilt 依赖注入** - 自动依赖管理

### 🎯 核心价值

**智能化提升**:
- 从 **通用分析** → **个性化分析**
- 从 **被动服务** → **主动推荐**
- 从 **单次分析** → **持续学习**

**用户体验**:
- ✅ 记住常用股票
- ✅ 理解用户偏好
- ✅ 提供个性化建议
- ✅ 学习投资模式

**技术特点**:
- ✅ 完整的 CRUD 操作
- ✅ 响应式编程 (Flow)
- ✅ 自动学习机制
- ✅ 隐私保护

---

**报告时间**: 2026-03-23
**编译状态**: ✅ BUILD SUCCESSFUL
**生产就绪**: ✅ 核心功能完成，可进一步集成

**推荐下一步**:
1. 在分析流程中集成 recordStockAccess
2. 在首页展示常用股票
3. 在设置页面添加记忆管理
4. 实现 LLM 上下文注入
