# StockAnalysis Android v1.0.0 - Release Notes

## 🎉 首次发布

这是 StockAnalysis Android 应用的第一个正式版本。

## ⚠️ 重要声明

**本应用仅供学习研究使用，不构成任何投资建议。**

- 应用中的所有分析结果、信号、评分仅供参考
- 股票数据可能来自模拟或第三方免费接口，不保证实时性和准确性
- 股市投资有风险，入市需谨慎
- 开发者不对因使用本应用而产生的任何损失负责

详细免责声明请查看 [DISCLAIMER.md](../DISCLAIMER.md)

## 📱 功能特性

### 核心功能
- ✅ 自选股管理 - 添加、删除、管理关注股票
- ✅ 实时行情 - 通过第三方 API 获取行情数据
- ✅ AI 智能分析 - 基于技术指标的股票分析（需自行配置 LLM API）
- ✅ 技术分析 - MA、MACD、KDJ、RSI、BOLL 等指标
- ✅ 历史记录 - 保存和查看分析历史
- ✅ 多数据源 - 支持东方财富、新浪财经、Yahoo Finance
- ✅ 推送通知 - 定时每日分析推送

### 支持的 AI 提供商
- OpenAI (GPT-3.5/GPT-4)
- Google Gemini
- Anthropic Claude
- DeepSeek
- 阿里云通义千问
- Moonshot Kimi
- 讯飞星火
- 智谱 GLM
- Ollama (本地部署)

## 📋 系统要求

- Android 7.0+ (API 24+)
- 最低 2GB RAM
- 需要网络连接获取实时数据

## 🔧 安装方法

1. 下载 `StockAnalysis-v1.0.0.apk`
2. 在 Android 设备上允许安装未知来源应用
3. 点击 APK 文件进行安装

## 📝 配置说明

### 配置 AI 分析
1. 打开应用，进入「设置」
2. 选择「AI 配置」
3. 选择提供商并输入 API Key
4. 返回主界面开始使用

### 配置数据源
1. 进入「设置」-「数据源配置」
2. 启用/禁用特定数据源
3. 设置数据源优先级

## 🔒 权限说明

应用需要以下权限：
- **INTERNET**: 获取股票数据和 AI 分析
- **RECEIVE_BOOT_COMPLETED**: 开机后恢复定时任务
- **POST_NOTIFICATIONS**: 发送分析通知
- **FOREGROUND_SERVICE**: 后台运行分析服务

## 🐛 已知问题

1. 新闻功能尚未完整实现（界面占位）
2. 某些数据源在特定网络环境下可能无法访问
3. DeepSeek 思考模式需要进一步优化

## 🛣️ 未来计划

- [ ] 完善新闻功能展示
- [ ] 添加 K 线图
- [ ] 支持更多技术指标
- [ ] 优化 AI 分析准确性

## 📄 许可证

MIT License - 详见 [LICENSE](../LICENSE)

## 🙏 致谢

感谢 [daily_stock_analysis](https://github.com/ZhuLinsen/daily_stock_analysis) 开源项目提供的参考和灵感。

---

**再次提醒：本应用仅供学习研究使用，不构成投资建议。股市有风险，投资需谨慎！**
