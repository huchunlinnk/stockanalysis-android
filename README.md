# StockAnalysis App - 智能股票分析系统

> ⚠️ **重要声明：本应用仅供学习研究使用，不构成任何投资建议。股市有风险，投资需谨慎。详见 [免责声明](./DISCLAIMER.md)。**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)

## 📱 项目简介

这是一款基于 Kotlin + Jetpack Compose + Hilt + Room 构建的 Android 股票分析应用，参考 [daily_stock_analysis](https://github.com/ZhuLinsen/daily_stock_analysis) 项目逻辑实现。

**项目性质**: 学习/教育用途，用于展示如何在 Android 平台实现股票分析应用

### 截图预览

*(截图将在后续版本中添加)*

## ⚠️ 重要提示

**本应用仅供学习研究使用，不构成任何投资建议。**

- 应用中的所有分析结果、信号、评分仅供参考
- 股票数据可能来自模拟或第三方免费接口，不保证实时性和准确性
- 股市投资有风险，入市需谨慎
- 开发者不对因使用本应用而产生的任何损失负责

详细免责声明请查看 [DISCLAIMER.md](./DISCLAIMER.md)

## 🌟 功能特性

### 核心功能
- **📊 自选股管理**：添加、删除、管理关注股票
- **📈 实时行情**：通过第三方 API 获取行情数据
- **🤖 AI 智能分析**：基于技术指标和量化模型的股票分析（需自行配置 LLM API）
- **📜 历史记录**：保存和查看分析历史
- **🔔 推送通知**：定时每日分析推送
- **⚙️ 灵活配置**：支持多种 AI 模型和数据源配置

### 技术指标
- 移动平均线 (MA5, MA10, MA20, MA60)
- MACD (DIF, DEA, MACD柱状图)
- KDJ (K值, D值, J值)
- RSI (6日, 12日, 24日)
- 布林带 (上轨, 中轨, 下轨)
- 乖离率 (BIAS)

### 科技感 UI 设计
- 深色科技感主题（深蓝紫色系）
- 霓虹渐变色彩
- 卡片式布局与发光效果
- 流畅动画与数据可视化

## 🏗️ 技术架构

### 数据层
- **Room 数据库**：股票列表、K线数据、分析结果
- **本地数据服务**：模拟股票数据生成和管理
- **技术指标计算器**：完整的技术分析算法

### 业务逻辑层
- **Repository 模式**：数据访问抽象
- **AnalysisEngine**：核心分析引擎
- **TechnicalIndicatorCalculator**：技术指标计算

### UI 层
- **MVVM 架构**：ViewModel + LiveData/Flow
- **Hilt 依赖注入**：组件解耦
- **Navigation**：页面导航
- **Material Design 3**：现代 UI 组件

## 📋 与原版项目的对应关系

本应用参考 daily_stock_analysis 项目实现，对应关系如下：

| daily_stock_analysis (原版) | StockAnalysis App (本复刻) |
|---------------------------|--------------------------|
| StockAnalysisPipeline | AnalysisEngine |
| DataFetcherManager | DataSourceManager + LocalDataService |
| GeminiAnalyzer | AnalysisEngine.analyzeStock |
| StockTrendAnalyzer | TechnicalIndicatorCalculator.analyzeTrend |
| storage.get_analysis_context | LocalDataService.getAnalysisContext |
| config.stock_list | StockRepository (本地数据库) |
| notifier.send | NotificationHelper + WorkManager |

### 主要差异

| 特性 | 原版 | 本复刻 |
|------|------|--------|
| 平台 | Python 后端 + Web 前端 | Android 原生应用 |
| 部署 | 需要服务器 | 纯客户端，无需服务器 |
| 数据存储 | SQLite/PostgreSQL | Android Room 本地数据库 |
| AI 分析 | 完整 LLM 集成 | 基础框架，需自行配置 API |
| 数据源 | 多源实时数据 | 本地模拟 + 第三方 API |

## 🚀 快速开始

### 环境要求
- JDK 17
- Android SDK 34
- Kotlin 1.9.22
- Gradle 8.2

### 编译步骤

```bash
# 克隆仓库
git clone https://github.com/yourusername/stockanalysis-android.git
cd stockanalysis-android

# 编译 Debug 版本
./gradlew clean assembleDebug

# APK 输出位置
app/build/outputs/apk/debug/app-debug.apk
```

### 安装到设备

```bash
# 连接设备并安装
adb install -t -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.example.stockanalysis/.ui.MainActivity
```

或使用便捷脚本：
```bash
./build-and-install.sh
```

## ⚙️ 配置说明

### AI 模型配置

1. 打开应用，进入「设置」页面
2. 选择 AI 提供商：
   - OpenAI (GPT-3.5/GPT-4)
   - Google Gemini
   - Anthropic Claude
   - DeepSeek
   - 阿里云通义千问
   - Moonshot Kimi
   - 讯飞星火
   - 智谱 GLM
   - Ollama (本地)
   - 自定义 API
3. 输入 API Key（从对应提供商网站获取）
4. 选择模型并开始使用

### 数据源配置

应用支持多种数据源：
- 东方财富 (EFinance)
- 新浪财经 (AkShare)
- Yahoo Finance
- Baostock

在「数据源配置」页面可以设置优先级和启用/禁用特定数据源。

## 📂 项目结构

```
app/src/main/java/com/example/stockanalysis/
├── data/
│   ├── api/             # API 接口 (Retrofit)
│   ├── local/           # Room 数据库和 DAO
│   ├── model/           # 数据模型
│   ├── repository/      # 仓库层
│   ├── agent/           # AI Agent 系统
│   ├── datasource/      # 数据源实现
│   └── backtest/        # 回测引擎
├── di/                  # Hilt 依赖注入模块
├── ui/                  # Activity/Fragment/ViewModel
├── utils/               # 工具类
└── notification/        # 通知相关
```

## 🛣️ 路线图

- [ ] 接入真实股票数据源
- [ ] 完善新闻功能展示
- [ ] 支持 DeepSeek 思考模式
- [ ] 添加 K 线图展示
- [ ] 支持美股、港股实时数据
- [ ] 添加更多技术指标

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

请查看 [CONTRIBUTING.md](./CONTRIBUTING.md) 了解如何参与贡献。

## 📄 许可证

本项目采用 [MIT License](./LICENSE) 开源许可证。

**再次强调：本应用仅供学习研究使用，不构成任何投资建议。股市有风险，投资需谨慎。**

## 🙏 致谢

感谢 [daily_stock_analysis](https://github.com/ZhuLinsen/daily_stock_analysis) 开源项目提供的参考和灵感。

---

*本项目是学习性质的复刻项目，旨在学习 Android 开发和金融数据分析技术。*
