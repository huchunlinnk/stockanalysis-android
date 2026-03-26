# 股票分析助手 v1.2.0

## 版本信息
- 版本号: 1.2.0
- 版本代码: 3
- 发布日期: 2026-03-26

## 主要功能

### 核心功能
- 📈 **股票数据管理** - 支持添加、删除、查看自选股
- 📊 **技术分析** - K线图、均线、MACD、RSI、布林带等技术指标
- 🤖 **多Agent智能分析** - 技术面、基本面、消息面、风险评估、决策Agent
- 📰 **新闻分析** - 支持多种新闻源（Bocha、Brave、SerpApi等）
- 📋 **回测系统** - 支持历史数据回测和策略验证
- 📉 **历史对比** - 信号准确性追踪和验证

### 数据源支持
- 东方财富 (EFinance)
- Yahoo Finance
- TuShare
- Baostock
- AkShare

### LLM支持
- OpenAI API兼容接口
- 多Key轮询负载均衡
- 支持自定义API Base URL

### 通知功能
- 微信企业号
- 飞书机器人
- Telegram
- 钉钉
- Email (SMTP)
- Slack/Discord
- PushPlus/Server酱

### 数据导入
- CSV文件导入
- 剪贴板导入
- 图片OCR识别导入

## 技术栈
- Kotlin + Android Architecture Components
- Hilt 依赖注入
- Room 数据库
- Retrofit + OkHttp 网络请求
- MPAndroidChart 图表
- WorkManager 后台任务
- Firebase Crashlytics

## 安装说明
1. 下载 `StockAnalysis-v1.2.0.apk`
2. 在Android设备上安装（需要Android 7.0+）
3. 首次启动配置数据源和LLM API密钥

## 注意事项
- 本应用需要网络连接获取股票数据
- LLM分析功能需要用户自行配置API密钥
- 所有数据存储在本地，保护隐私

## 开源协议
MIT License
