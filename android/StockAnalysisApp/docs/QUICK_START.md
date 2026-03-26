# 股票智能分析 Android 版 - 快速开始指南

## 📱 项目简介

这是 `daily_stock_analysis` 项目的 Android 客户端实现，提供 AI 驱动的股票智能分析功能。

## 📂 项目位置

```
/Users/chunlin5/opensource/stock-analysis/android/StockAnalysisApp/
```

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Android 设备或模拟器 (Android 7.0+)

### 步骤 1: 打开项目

1. 启动 Android Studio
2. 点击 `File` → `Open`
3. 选择文件夹：`/Users/chunlin5/opensource/stock-analysis/android/StockAnalysisApp`
4. 等待 Gradle 同步完成（首次可能需要几分钟）

### 步骤 2: 配置 API 密钥（可选）

1. 在项目中创建 `local.properties` 文件（如果尚未存在）
2. 添加你的 API 密钥：

```properties
OPENAI_API_KEY=your_openai_key_here
GEMINI_API_KEY=your_gemini_key_here
```

> 注意：也可以在应用的设置页面中配置 API 密钥

### 步骤 3: 运行应用

1. 连接 Android 设备或启动模拟器
2. 点击工具栏的运行按钮 (▶)
3. 选择目标设备
4. 等待安装完成

## 📊 功能模块

### 1. 首页 (Home)
- 搜索股票
- 市场概览
- 快捷操作入口

### 2. 自选股 (Watchlist)
- 管理关注的股票
- 查看实时行情
- 批量分析

### 3. 智能分析 (Analysis)
- AI 驱动的股票分析
- 技术面分析
- 基本面分析
- 舆情分析

### 4. 历史记录 (History)
- 查看过往分析
- 对比历史数据
- 删除记录

### 5. 设置 (Settings)
- AI 模型配置
- 通知设置
- 数据源选择
- 应用主题

## 🏗️ 项目结构

```
StockAnalysisApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/stockanalysis/
│   │   │   ├── data/               # 数据层
│   │   │   │   ├── api/            # Retrofit API
│   │   │   │   ├── local/          # Room 数据库
│   │   │   │   ├── model/          # 数据模型
│   │   │   │   └── repository/     # 数据仓库
│   │   │   ├── di/                 # Hilt 依赖注入
│   │   │   ├── notification/       # 通知服务
│   │   │   ├── ui/                 # UI 层
│   │   │   │   ├── viewmodel/      # ViewModels
│   │   │   │   └── *.kt            # Activities & Fragments
│   │   │   ├── utils/              # 工具类
│   │   │   └── StockAnalysisApplication.kt
│   │   └── res/                    # 资源文件
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## 🔧 核心依赖

| 库 | 版本 | 用途 |
|-----|------|------|
| Kotlin | 1.9.20 | 开发语言 |
| Hilt | 2.48 | 依赖注入 |
| Room | 2.6.1 | 本地数据库 |
| Retrofit | 2.9.0 | 网络请求 |
| WorkManager | 2.9.0 | 后台任务 |
| Material 3 | 1.11.0 | UI 组件 |

## 📝 配置说明

### AI 模型配置

应用支持多种 AI 提供商：

1. **OpenAI** (推荐)
   - 模型：`gpt-3.5-turbo`, `gpt-4`
   - 官网：https://platform.openai.com

2. **Google Gemini**
   - 模型：`gemini-pro`
   - 官网：https://ai.google.dev

3. **Anthropic Claude**
   - 模型：`claude-3-sonnet`
   - 官网：https://anthropic.com

4. **DeepSeek**
   - 模型：`deepseek-chat`
   - 官网：https://platform.deepseek.com

### 通知配置

1. 在系统设置中开启应用通知权限
2. 在应用设置中设置推送时间
3. 可选择开启自动分析

## 🐛 常见问题

### Q: Gradle 同步失败？
A: 检查网络连接，确保可以访问 Maven Central 和 Google 仓库。

### Q: 编译错误？
A: 确保使用 JDK 17，在 Android Studio 的 Settings → Build → Gradle 中配置。

### Q: 无法获取股票数据？
A: 当前版本使用模拟数据，需要接入真实数据源（如东方财富、新浪财经）。

### Q: API 密钥如何安全存储？
A: 生产环境建议使用 Android Keystore 或服务器中转，不要硬编码在客户端。

## 📦 构建 APK

### Debug 版本

```bash
./gradlew assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`

### Release 版本

```bash
./gradlew assembleRelease
```

输出：`app/build/outputs/apk/release/app-release-unsigned.apk`

## 🔒 权限说明

应用需要以下权限：

- `INTERNET` - 访问网络获取数据
- `POST_NOTIFICATIONS` - 发送推送通知
- `RECEIVE_BOOT_COMPLETED` - 开机启动后台任务
- `SCHEDULE_EXACT_ALARM` - 设置精确闹钟

## ⚠️ 免责声明

本应用仅供学习和研究使用，不构成任何投资建议。股市有风险，投资需谨慎。

## 📞 技术支持

如有问题，请通过以下方式联系：

- GitHub Issues: 提交 Issue
- 邮箱: zhuls345@gmail.com

---

**最后更新**: 2026-03-22
**版本**: 1.0.0
