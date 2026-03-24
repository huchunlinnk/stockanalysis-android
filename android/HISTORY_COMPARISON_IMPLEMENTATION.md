# 历史信号对比服务实现文档

## 概述
为 Android StockAnalysisApp 实现了历史信号对比服务，参考 Python backtest_engine.py 的功能，提供了完整的信号记录、验证、对比和报告生成功能。

## 创建的文件

### 1. 核心服务
**文件**: `app/src/main/java/com/example/stockanalysis/data/analysis/HistoryComparisonService.kt`

主要功能：
- 记录每次分析的买入信号和决策 (recordSignal)
- 对比预测价格和实际价格 (validateSignal)
- 计算信号准确率 (calculateAccuracy)
- 追踪信号胜率统计 (SignalAccuracy)
- 生成信号准确性报告 (generateReport)
- 流式API获取对比结果 (comparePredictedVsActual)

数据模型：
- `SignalRecord`: 信号记录
- `ComparisonResult`: 对比结果
- `SignalAccuracy`: 准确率统计
- `SignalReport`: 信号报告
- `Direction`: 方向枚举 (UP/DOWN/FLAT/NOT_DOWN)
- `Outcome`: 结果枚举 (WIN/LOSS/NEUTRAL/PENDING)

### 2. 后台 Worker
**文件**: `app/src/main/java/com/example/stockanalysis/notification/SignalTrackingWorker.kt`

功能：
- 定期后台任务验证历史信号准确性
- 比较预测价格与实际价格
- 更新准确率统计
- 支持三种模式：单只股票、所有股票、批量更新
- 准确率变化通知

**文件**: `app/src/main/java/com/example/stockanalysis/notification/SignalTrackingScheduler.kt`

功能：
- 调度定期信号追踪任务
- 立即执行单次追踪
- 批量更新待验证信号
- 追踪状态查询

### 3. DAO 扩展
**修改文件**: `app/src/main/java/com/example/stockanalysis/data/local/AnalysisResultDao.kt`

新增查询：
- `getAnalysisHistoryPaged()`: 分页获取历史记录
- `getResultsByTimeRange(symbol, start, end)`: 获取股票在时间段内的分析结果
- `getAllResultsByTimeRange()`: 获取所有股票时间段内的结果（Flow）
- `getDecisionStatsBySymbol()`: 获取决策统计信息
- `getRecentTradeSignals()`: 获取最近交易信号
- `getSignalsNeedingValidation()`: 获取需要验证的信号
- `getSignalCount()`: 获取信号数量
- `getResultsByDecision()`: 按决策类型获取结果
- `getRecentResults()`: 获取最近的分析结果

新增数据类：
- `DecisionStat`: 决策统计

### 4. 回测引擎集成
**修改文件**: `app/src/main/java/com/example/stockanalysis/data/backtest/BacktestEngine.kt`

新增功能：
- `validateSignalsWithComparison()`: 使用 HistoryComparisonService 验证信号
- `getSignalAccuracy()`: 获取信号准确率
- `compareBacktestWithSignalValidation()`: 对比回测与信号验证结果
- `comparePredictedVsActualFlow()`: 流式获取对比结果
- `generateEnhancedReport()`: 生成增强版回测报告

新增数据类：
- `BacktestValidationComparison`: 回测与验证对比
- `EnhancedBacktestReport`: 增强版回测报告

### 5. UI 层
**文件**: `app/src/main/java/com/example/stockanalysis/ui/HistoryComparisonFragment.kt`

功能：
- 显示信号准确率统计（卡片式布局）
- 显示历史信号列表（RecyclerView）
- 显示预测 vs 实际对比图表（MPAndroidChart）
  - 准确率饼图
  - 收益对比柱状图
  - 准确率趋势线图
- 筛选功能（全部/正确/错误/买入/卖出等）
- 排序功能（日期/收益/评分）
- 时间范围选择（7天/30天/90天/全部）
- 股票切换
- 信号详情弹窗
- 生成报告

**文件**: `app/src/main/java/com/example/stockanalysis/ui/viewmodel/HistoryComparisonViewModel.kt`

功能：
- 管理UI状态（Loading/Success/Error）
- 股票列表加载
- 信号数据加载
- 筛选和排序逻辑
- 统计计算
- 报告生成

状态管理：
- `HistoryComparisonUiState`: UI状态密封类
- `SignalFilterType`: 筛选类型枚举
- `SortType`: 排序类型枚举
- `TimeRange`: 时间范围枚举
- `ComparisonStatistics`: 对比统计

### 6. 布局文件
**文件**: `app/src/main/res/layout/fragment_history_comparison.xml`

布局结构：
- 顶部工具栏（股票选择、刷新）
- 筛选栏（时间范围、筛选类型、排序）
- 统计概览卡片
- 准确率饼图卡片
- 收益对比柱状图卡片
- 准确率趋势线图卡片
- 历史信号列表卡片

**文件**: `app/src/main/res/layout/item_signal_comparison.xml`

列表项布局：
- 日期
- 信号类型
- 预测方向
- 实际方向
- 收益率
- 状态图标

### 7. 资源文件
**图标**:
- `ic_check_circle.xml`: 正确图标
- `ic_close_circle.xml`: 错误图标
- `ic_refresh.xml`: 刷新图标
- `ic_analytics.xml`: 分析图标

**背景**:
- `bg_spinner.xml`: Spinner 背景

**菜单**:
- `menu_history.xml`: 历史页面菜单（信号对比入口）

**字符串**:
- 在 `strings.xml` 中添加历史信号对比相关字符串

**颜色**:
- 在 `colors.xml` 中添加趋势颜色别名

### 8. 导航
**修改文件**: `app/src/main/res/navigation/nav_graph.xml`

新增：
- `nav_history_comparison` Fragment
- `action_history_to_comparison` Action

### 9. DI 模块
**修改文件**: `app/src/main/java/com/example/stockanalysis/di/BacktestModule.kt`

新增：
- `provideHistoryComparisonService()`: 提供 HistoryComparisonService
- 更新 `provideBacktestEngine()`: 注入 HistoryComparisonService

### 10. 历史页面集成
**修改文件**: `app/src/main/java/com/example/stockanalysis/ui/HistoryFragment.kt`

新增：
- 菜单设置
- 信号对比入口

## 技术实现细节

### 准确率计算逻辑
参考 Python backtest_engine.py:
1. 推断决策方向（买入=UP，卖出=DOWN，持有=FLAT）
2. 计算评估窗口期内的价格变化
3. 判断是否预测正确
4. 分类结果（WIN/LOSS/NEUTRAL）
5. 计算胜率、方向准确率、平均收益

### 止损止盈评估
- 评估期间检查是否触发止损或止盈
- 记录首次触发类型
- 计算模拟退出价格

### 数据流
```
AnalysisResult -> SignalRecord -> ComparisonResult -> SignalAccuracy/SignalReport
```

### WorkManager 调度
- 每天上午9:30执行定期追踪
- 网络连接要求
- 电量充足要求

## 使用方法

### 在 ViewModel 中使用
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val historyComparisonService: HistoryComparisonService
) : ViewModel() {
    
    fun validateSignals(symbol: String) {
        viewModelScope.launch {
            val accuracy = historyComparisonService.validateSignals(symbol)
            // 使用准确率数据
        }
    }
}
```

### 调度后台任务
```kotlin
// 定期追踪
SignalTrackingScheduler.schedulePeriodicTracking(context)

// 立即执行
SignalTrackingScheduler.runImmediateTracking(context, symbol)
```

### 导航到对比页面
```kotlin
findNavController().navigate(R.id.nav_history_comparison)
```

## 依赖要求
- Room 数据库
- MPAndroidChart（图表）
- WorkManager（后台任务）
- Kotlin 协程
- Hilt（依赖注入）
