# 导出分享功能架构图

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                    AnalysisResultActivity                       │
│                  (实现 ExportDialog.ExportListener)              │
│                                                                 │
│  ┌──────────────┐                    ┌───────────────────┐    │
│  │ Share Button │ ──────────────────> │ ExportDialog      │    │
│  └──────────────┘                    │                   │    │
│                                       │ - Markdown        │    │
│  ┌──────────────┐                    │ - Image           │    │
│  │MarkdownExport│                    │ - Text            │    │
│  │    er        │                    │ - Share to Others │    │
│  └──────────────┘                    │ - Save to Local   │    │
│                                       └───────────────────┘    │
│  ┌──────────────┐                                              │
│  │ReportImage   │                                              │
│  │  Generator   │                                              │
│  └──────────────┘                                              │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  AnalysisResultViewModel                         │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ State Management                                         │ │
│  │  - analysisResult: StateFlow<AnalysisResult?>            │ │
│  │  - shareEvent: SharedFlow<ShareEvent>                    │ │
│  │  - shareImageState: StateFlow<ShareImageState>           │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Export Methods                                           │ │
│  │  - exportToMarkdown(): String?                           │ │
│  │  - exportToCompactMarkdown(): String?                    │ │
│  │  - generateShareImage(callback)                          │ │
│  │  - generateCompactShareImage(callback)                   │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Data & Repository Layer                       │
│                                                                 │
│  ┌──────────────────┐              ┌────────────────────────┐ │
│  │ AnalysisRepository│             │ FundamentalRepository  │ │
│  │                  │              │                        │ │
│  │ - getResultById  │              │ - getFundamentalData   │ │
│  │ - analyzeStock   │              │ - getValuationMetrics  │ │
│  │ - deleteResult   │              │ - getFinancialIndicators││
│  └──────────────────┘              └────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## 组件交互流程

### 1. 用户点击分享按钮

```
User Click Share Button
         │
         ▼
┌─────────────────────────┐
│AnalysisResultActivity   │
│  setupButtons()         │
│    btnShare.onClick {   │
│      showExportDialog() │
│    }                    │
└─────────────────────────┘
         │
         ▼
┌─────────────────────────┐
│   ExportDialog          │
│   show(supportFragment  │
│        Manager)         │
└─────────────────────────┘
```

### 2. 用户选择导出选项

```
User Selects Option
         │
         ├─────────────┬─────────────┬─────────────┬─────────────┐
         │             │             │             │             │
         ▼             ▼             ▼             ▼             ▼
  onExportMarkdown  onExportImage  onExportText  onShareToOthers  onSaveToLocal
         │             │             │             │             │
         ▼             ▼             ▼             ▼             ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│MarkdownExport│ │ReportImage  │ │buildShare   │ │showShare    │ │showSave     │
│   er.export  │ │Generator.   │ │ Content()   │ │ Options     │ │ Options     │
│ ToMarkdown() │ │generate()   │ └─────────────┘ │ Dialog()    │ │ Dialog()    │
└─────────────┘ └─────────────┘                  └─────────────┘ └─────────────┘
         │             │                                │             │
         └─────────────┴────────────────────────────────┴─────────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │ File Creation &     │
                   │ Sharing via Intent  │
                   └─────────────────────┘
```

### 3. Markdown导出流程

```
┌──────────────────────────────────────────────────────────────┐
│ onExportMarkdown()                                           │
│   │                                                          │
│   ├─> viewModel.analysisResult.value                        │
│   │   (获取分析结果)                                         │
│   │                                                          │
│   ├─> markdownExporter.exportToMarkdown(result)             │
│   │   (生成Markdown内容)                                     │
│   │                                                          │
│   ├─> File Creation                                         │
│   │   - filename: stock_report_{symbol}_{timestamp}.md     │
│   │   - path: cacheDir/                                     │
│   │   - content: markdown string                            │
│   │                                                          │
│   ├─> FileProvider.getUriForFile()                          │
│   │   (获取安全的文件URI)                                    │
│   │                                                          │
│   └─> Intent.ACTION_SEND                                    │
│       - type: "text/markdown"                               │
│       - EXTRA_STREAM: uri                                   │
│       - EXTRA_SUBJECT: "股票分析报告"                        │
│       - FLAG_GRANT_READ_URI_PERMISSION                      │
│                                                              │
│       startActivity(Intent.createChooser())                 │
└──────────────────────────────────────────────────────────────┘
```

### 4. 图片导出流程

```
┌──────────────────────────────────────────────────────────────┐
│ onExportImage() / shareAsImage()                             │
│   │                                                          │
│   ├─> viewModel.generateShareImage { bitmap ->              │
│   │   │                                                      │
│   │   ├─> ReportImageGenerator(context)                     │
│   │   │   (创建图片生成器)                                   │
│   │   │                                                      │
│   │   ├─> generator.generateReportImage(result)             │
│   │   │   │                                                  │
│   │   │   ├─> Calculate Image Height                        │
│   │   │   │   - Header: 200px                               │
│   │   │   │   - Stock Info: 180px                           │
│   │   │   │   - Decision: 200px                             │
│   │   │   │   - Score: 160px                                │
│   │   │   │   - Summary: dynamic                            │
│   │   │   │   - Technical: 250px                            │
│   │   │   │   - Fundamental: 180px                          │
│   │   │   │   - Risk: 160px                                 │
│   │   │   │   - Action Plan: 300px                          │
│   │   │   │   - Footer: 120px                               │
│   │   │   │                                                  │
│   │   │   ├─> Create Bitmap(1080, height, ARGB_8888)       │
│   │   │   │                                                  │
│   │   │   ├─> Draw Background                               │
│   │   │   │                                                  │
│   │   │   ├─> Render Content                                │
│   │   │   │   - renderHeader()                              │
│   │   │   │   - renderStockInfo()                           │
│   │   │   │   - renderDecision()                            │
│   │   │   │   - renderScore()                               │
│   │   │   │   - renderSummary()                             │
│   │   │   │   - renderTechnicalAnalysis()                   │
│   │   │   │   - renderFundamentalAnalysis()                 │
│   │   │   │   - renderRiskAssessment()                      │
│   │   │   │   - renderActionPlan()                          │
│   │   │   │   - renderFooter()                              │
│   │   │   │                                                  │
│   │   │   └─> return Bitmap                                 │
│   │   │                                                      │
│   │   └─> callback(bitmap)                                  │
│   │                                                          │
│   ├─> Save to File                                          │
│   │   - compress to PNG                                     │
│   │   - filename: share_report_{timestamp}.png             │
│   │                                                          │
│   ├─> FileProvider.getUriForFile()                          │
│   │                                                          │
│   └─> Intent.ACTION_SEND                                    │
│       - type: "image/png"                                   │
│       - EXTRA_STREAM: uri                                   │
└──────────────────────────────────────────────────────────────┘
```

### 5. 文本导出流程

```
┌──────────────────────────────────────────────────────────────┐
│ onExportText() / shareAsText()                               │
│   │                                                          │
│   ├─> viewModel.shareResult()                               │
│   │   │                                                      │
│   │   ├─> buildShareContent(result)                         │
│   │   │   │                                                  │
│   │   │   ├─> Format Stock Info                             │
│   │   │   ├─> Format Decision                               │
│   │   │   ├─> Format Score                                  │
│   │   │   ├─> Format Summary                                │
│   │   │   ├─> Format Technical Analysis                     │
│   │   │   ├─> Format Fundamental Analysis                   │
│   │   │   ├─> Format Risk Assessment                        │
│   │   │   ├─> Format Action Plan                            │
│   │   │   └─> return String                                 │
│   │   │                                                      │
│   │   └─> emit ShareEvent.Success(content)                  │
│   │                                                          │
│   ├─> shareText(content)                                    │
│   │                                                          │
│   └─> Intent.ACTION_SEND                                    │
│       - type: "text/plain"                                  │
│       - EXTRA_TEXT: content                                 │
│       - EXTRA_SUBJECT: "股票分析报告"                        │
└──────────────────────────────────────────────────────────────┘
```

## 数据流图

```
┌──────────────┐
│   UI Event   │  User clicks share button
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ ExportDialog │  Show export options
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Listener    │  onExport* method called
│  Callback    │
└──────┬───────┘
       │
       ├─────────────────────┬─────────────────────┐
       │                     │                     │
       ▼                     ▼                     ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  Markdown    │      │    Image     │      │     Text     │
│  Exporter    │      │  Generator   │      │   Builder    │
└──────┬───────┘      └──────┬───────┘      └──────┬───────┘
       │                     │                     │
       ▼                     ▼                     ▼
┌───────────────────────────────────────────────────────┐
│              AnalysisResult Data                       │
│  - stockName, stockSymbol                             │
│  - decision, score, confidence                        │
│  - technicalAnalysis                                  │
│  - fundamentalAnalysis                                │
│  - riskAssessment                                     │
│  - actionPlan                                         │
│  - newsHeadlines                                      │
└───────────┬────────────────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────────────────────────┐
│            Export Processing                          │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐    │
│  │  Format    │  │  Render    │  │   Build    │    │
│  │    Text    │  │   Canvas   │  │   String   │    │
│  └────────────┘  └────────────┘  └────────────┘    │
└───────────┬────────────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────────────────────────┐
│            File Creation                              │
│  - Create temporary file in cache/external dir       │
│  - Write content (Markdown/Image/Text)                │
│  - Get FileProvider URI                               │
└───────────┬────────────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────────────────────────┐
│            Android Sharing                            │
│  Intent.ACTION_SEND + createChooser()                 │
│  - Share to WeChat, QQ, Email, etc.                  │
└──────────────────────────────────────────────────────┘
```

## 类图

```
┌─────────────────────────────────────────┐
│      AnalysisResultActivity             │
├─────────────────────────────────────────┤
│ - binding: ActivityAnalysisResultBinding│
│ - viewModel: AnalysisResultViewModel    │
│ - generatedBitmap: Bitmap?              │
│ - markdownExporter: MarkdownExporter    │
├─────────────────────────────────────────┤
│ + onCreate()                            │
│ + setupButtons()                        │
│ + showExportDialog()                    │
│ + onExportMarkdown()                    │
│ + onExportImage()                       │
│ + onExportText()                        │
│ + onShareToOthers()                     │
│ + onSaveToLocal()                       │
│ - shareAsImage()                        │
│ - shareAsText()                         │
│ - saveToGallery()                       │
│ - saveMarkdownToLocal()                 │
│ - saveTextToLocal()                     │
│ - buildShareContent()                   │
└─────────────────────────────────────────┘
                 │
                 │ implements
                 ▼
┌─────────────────────────────────────────┐
│    ExportDialog.ExportListener          │
├─────────────────────────────────────────┤
│ + onExportMarkdown()                    │
│ + onExportImage()                       │
│ + onExportText()                        │
│ + onShareToOthers()                     │
│ + onSaveToLocal()                       │
└─────────────────────────────────────────┘


┌─────────────────────────────────────────┐
│          ExportDialog                    │
├─────────────────────────────────────────┤
│ - listener: ExportListener?             │
├─────────────────────────────────────────┤
│ + onAttach(context: Context)            │
│ + onCreateDialog(): Dialog              │
│ + onDetach()                            │
│ + newInstance(): ExportDialog [static]  │
└─────────────────────────────────────────┘


┌─────────────────────────────────────────┐
│       AnalysisResultViewModel           │
├─────────────────────────────────────────┤
│ - analysisRepository: AnalysisRepository│
│ - fundamentalRepository: FundamentalRepo│
│ - markdownExporter: MarkdownExporter    │
│ - _analysisResult: MutableStateFlow     │
│ - _shareEvent: MutableSharedFlow        │
│ - _shareImageState: MutableStateFlow    │
├─────────────────────────────────────────┤
│ + loadAnalysisResult(id: String)        │
│ + startNewAnalysis(symbol, name)        │
│ + shareResult()                         │
│ + generateShareImage(callback)          │
│ + generateCompactShareImage(callback)   │
│ + exportToMarkdown(): String?           │
│ + exportToCompactMarkdown(): String?    │
│ + refreshAnalysis()                     │
│ + deleteAnalysisResult()                │
└─────────────────────────────────────────┘


┌─────────────────────────────────────────┐
│         MarkdownExporter                 │
├─────────────────────────────────────────┤
│ + exportToMarkdown(result): String      │
│ + exportCompactMarkdown(result): String │
│ - formatDate(timestamp): String         │
│ - formatDecision(decision): String      │
│ - formatDecisionEmoji(decision): String │
│ - formatConfidence(level): String       │
│ - formatRiskLevel(level): String        │
│ - formatVolume(volume): String          │
└─────────────────────────────────────────┘


┌─────────────────────────────────────────┐
│       ReportImageGenerator               │
├─────────────────────────────────────────┤
│ - context: Context                      │
├─────────────────────────────────────────┤
│ + generateReportImage(result): Bitmap   │
│ + generateCompactReportImage(): Bitmap  │
│ - drawBackground(canvas: Canvas)        │
└─────────────────────────────────────────┘
         │
         ├── HeightCalculator
         ├── CompactHeightCalculator
         ├── ReportRenderer
         └── CompactReportRenderer
```

## 状态机图

```
                    ┌─────────┐
                    │  Idle   │
                    └────┬────┘
                         │ User clicks share
                         ▼
                   ┌──────────┐
                   │ Dialog   │
                   │ Showing  │
                   └────┬─────┘
                        │ User selects option
                        ▼
               ┌────────────────┐
               │  Processing    │
               │   Export       │
               └────┬───────────┘
                    │
         ┌──────────┼──────────┐
         │          │          │
         ▼          ▼          ▼
    ┌────────┐  ┌──────┐  ┌──────┐
    │Markdown│  │Image │  │ Text │
    │Generate│  │Gen   │  │Build │
    └────┬───┘  └───┬──┘  └───┬──┘
         │          │          │
         └──────────┼──────────┘
                    ▼
           ┌────────────────┐
           │  File Created  │
           └────────┬───────┘
                    │
         ┌──────────┼──────────┐
         │                     │
         ▼                     ▼
    ┌────────┐           ┌─────────┐
    │ Share  │           │  Save   │
    │Intent  │           │ Local   │
    └────┬───┘           └────┬────┘
         │                    │
         └──────────┬─────────┘
                    ▼
              ┌──────────┐
              │ Success  │
              │  Toast   │
              └──────────┘
                    │
                    ▼
              ┌──────────┐
              │   Idle   │
              └──────────┘
```

## 文件结构树

```
app/src/main/
├── java/com/example/stockanalysis/
│   ├── ui/
│   │   ├── AnalysisResultActivity.kt  ⭐ [修改]
│   │   └── dialog/
│   │       └── ExportDialog.kt         ⭐ [新增]
│   │
│   ├── ui/viewmodel/
│   │   └── AnalysisResultViewModel.kt ⭐ [修改]
│   │
│   └── util/
│       ├── MarkdownExporter.kt         ⭐ [新增]
│       └── ReportImageGenerator.kt     ✓ [已有]
│
└── res/
    ├── layout/
    │   └── activity_analysis_result.xml ✓ [已有]
    │
    ├── values/
    │   └── strings.xml                  ⭐ [修改]
    │
    └── xml/
        └── file_paths.xml               ✓ [已有]

AndroidManifest.xml                      ✓ [已有FileProvider配置]
```

图例:
- ⭐ 本次实现修改的文件
- ✓ 已存在无需修改的文件

## 依赖关系图

```
AnalysisResultActivity
  │
  ├─> ExportDialog (依赖)
  ├─> AnalysisResultViewModel (依赖)
  ├─> MarkdownExporter (依赖)
  ├─> ReportImageGenerator (依赖)
  └─> FileProvider (系统组件)
      └─> file_paths.xml (配置)

AnalysisResultViewModel
  │
  ├─> AnalysisRepository (依赖)
  ├─> FundamentalRepository (依赖)
  ├─> MarkdownExporter (依赖)
  └─> ReportImageGenerator (依赖)

MarkdownExporter
  └─> AnalysisResult (数据模型)

ReportImageGenerator
  └─> AnalysisResult (数据模型)

ExportDialog
  └─> ExportListener (接口)
```

---

**说明**: 本架构图展示了导出分享功能的完整技术架构，包括组件交互、数据流、类图、状态机和文件结构。所有组件遵循MVVM架构模式，使用Hilt进行依赖注入，符合Android开发最佳实践。
