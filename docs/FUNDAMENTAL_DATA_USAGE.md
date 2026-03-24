# 基本面数据系统使用说明

## 概述

基本面数据系统为 StockAnalysisApp 提供完整的股票基本面数据支持，包括财务指标、成长性指标、分红信息和机构持仓数据。

## 架构

```
FundamentalDataSource (数据源)
    ↓
FundamentalRepository (仓库层) ← → FundamentalDao (本地缓存)
    ↓
AnalysisEngine (分析引擎)
    ↓
AnalysisResultViewModel / Activity (UI层)
```

## 数据模型

### 1. FundamentalData
基本面数据汇总实体，存储在 Room 数据库中。

```kotlin
data class FundamentalData(
    val symbol: String,                    // 股票代码
    val name: String,                      // 股票名称
    val updateTime: Date,                  // 更新时间
    val peRatio: Double?,                  // 市盈率
    val pbRatio: Double?,                  // 市净率
    val financialIndicatorsJson: String?,  // 财务指标JSON
    val growthMetricsJson: String?,        // 成长性指标JSON
    val dividendInfoJson: String?,         // 分红信息JSON
    val institutionalHoldingJson: String?, // 机构持仓JSON
    val isCacheValid: Boolean              // 缓存是否有效
)
```

### 2. FinancialIndicators
财务指标，包含盈利能力、偿债能力、运营效率等。

```kotlin
data class FinancialIndicators(
    val roe: Double?,               // 净资产收益率
    val roa: Double?,               // 总资产收益率
    val grossMargin: Double?,       // 毛利率
    val netMargin: Double?,         // 净利率
    val debtToEquity: Double?,      // 资产负债率
    val operatingCashFlow: Double?  // 经营现金流
)
```

### 3. GrowthMetrics
成长性指标，包含营收增长、净利润增长等。

```kotlin
data class GrowthMetrics(
    val revenueGrowthYoY: Double?,      // 营收同比增长
    val netProfitGrowthYoY: Double?,    // 净利润同比增长
    val revenueGrowth3Y: Double?,       // 3年营收复合增长率
    val consecutiveYears: Int           // 连续增长年数
)
```

### 4. DividendInfo
分红信息，包含分红历史和股息率。

```kotlin
data class DividendInfo(
    val dividendPerShare: Double?,      // 每股分红
    val dividendYield: Double?,         // 股息率
    val ttmDividendPerShare: Double?,   // 近12个月每股分红
    val consecutiveYears: Int           // 连续分红年数
)
```

### 5. InstitutionalHolding
机构持仓数据。

```kotlin
data class InstitutionalHolding(
    val institutionCount: Int?,     // 机构数量
    val holdingRatio: Double?,      // 持仓比例
    val holdingChange: Double?,     // 持仓变动
    val top10Holders: List<ShareholderInfo>  // 前十大股东
)
```

## 使用方法

### 1. 获取基本面数据

```kotlin
@Inject
lateinit var fundamentalRepository: FundamentalRepository

// 获取基本面数据（自动使用缓存）
val result = fundamentalRepository.getFundamentalData("600519", "贵州茅台")
result.fold(
    onSuccess = { data ->
        // 使用数据
        val pe = data.peRatio
        val pb = data.pbRatio
    },
    onFailure = { error ->
        // 处理错误
    }
)
```

### 2. 获取详细分析结果

```kotlin
val analysisResult = fundamentalRepository.getFundamentalAnalysis(
    symbol = "600519",
    name = "贵州茅台",
    currentPrice = 1800.0
)

analysisResult.fold(
    onSuccess = { analysis ->
        // 综合评分
        val score = analysis.overallScore
        
        // 各维度评分
        val valuationScore = analysis.valuationScore
        val profitabilityScore = analysis.profitabilityScore
        
        // 投资建议
        val advice = analysis.investmentAdvice
        
        // 风险因素
        val risks = analysis.riskFactors
    }
)
```

### 3. 获取特定指标

```kotlin
// 财务指标
val indicators = fundamentalRepository.getFinancialIndicators("600519")

// 成长性指标
val growth = fundamentalRepository.getGrowthMetrics("600519")

// 分红信息
val dividend = fundamentalRepository.getDividendInfo("600519")

// 机构持仓
val holding = fundamentalRepository.getInstitutionalHolding("600519")
```

### 4. 使用 Flow 观察数据

```kotlin
fundamentalRepository.getFundamentalDataFlow("600519", "贵州茅台")
    .collect { result ->
        result.fold(
            onSuccess = { data ->
                // 更新UI
            }
        )
    }
```

## 缓存策略

- 缓存有效期：1天
- 自动过期检测：`FundamentalData.isExpired()`
- 强制刷新：`fundamentalRepository.refreshFundamentalData(symbol, name)`
- 缓存清理：`fundamentalRepository.clearCache()`

## 数据源

基本面数据从以下 API 获取：
1. **东方财富** (主要)：提供财务指标、分红数据、机构持仓
2. **新浪财经/腾讯财经** (备用)：提供估值数据

数据参照 Python 项目 `fundamental_adapter.py` 实现。

## 分析评分体系

### 综合评分权重
- 估值评分：15%
- 盈利能力：25%
- 成长性：25%
- 财务健康度：15%
- 分红：10%
- 机构关注度：10%

### 评分标准
- 80-100分：优秀
- 60-79分：良好
- 40-59分：一般
- 0-39分：较弱

## 与现有系统集成

### AnalysisEngine
`AnalysisEngine` 已集成基本面数据：
1. 优先使用 `FundamentalRepository` 获取真实数据
2. 降级到 `TushareDataSource`
3. 最后使用模拟数据

### UI展示
`AnalysisResultActivity` 显示：
- 基本面评分卡片
- 各维度评分明细
- 分析结论
- 投资建议
- 风险因素

## 依赖注入

已在 Hilt 模块中注册：
- `DatabaseModule`：提供 `FundamentalDao`
- `DataSourceModule`：提供 `FundamentalDataSource` 和 `FundamentalRepository`
- `AppModule`：注入到 `AnalysisEngine`

## 数据库升级

数据库版本已升级到 6，添加了 `fundamental_data` 表。

## 注意事项

1. 网络请求可能需要较长时间，建议在协程中执行
2. 数据可能不完整，使用时需要做空值检查
3. 缓存过期后会自动从网络刷新
4. 网络失败时会返回过期缓存数据（标记为无效）
