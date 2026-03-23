# Markdown 转图片功能使用说明

## 概述

本项目实现了将 Markdown 报告转换为图片的功能，支持分享股票分析报告到社交媒体或其他应用。

## 实现文件

### 1. 核心工具类

- **`MarkdownToImageConverter.kt`** - Markdown 转图片转换器
  - 位置: `app/src/main/java/com/example/stockanalysis/util/MarkdownToImageConverter.kt`
  - 功能: 将 Markdown 文本解析并渲染为 Bitmap 图片
  - 支持格式: 标题、列表、表格、加粗、斜体、代码块、引用等

- **`ReportImageGenerator.kt`** - 报告图片生成器
  - 位置: `app/src/main/java/com/example/stockanalysis/util/ReportImageGenerator.kt`
  - 功能: 生成专业美观的股票分析报告图片
  - 包含两种模式: 完整版和简洁版

### 2. UI 相关

- **`AnalysisResultActivity.kt`** - 分析结果页面
  - 添加分享功能按钮
  - 支持分享为图片/文本/保存到相册
  - 位置: `app/src/main/java/com/example/stockanalysis/ui/AnalysisResultActivity.kt`

- **`AnalysisResultViewModel.kt`** - 分析结果 ViewModel
  - 添加 `generateShareImage()` 方法
  - 添加 `generateCompactShareImage()` 方法
  - 位置: `app/src/main/java/com/example/stockanalysis/ui/viewmodel/AnalysisResultViewModel.kt`

### 3. 资源配置

- **`file_paths.xml`** - FileProvider 路径配置
  - 位置: `app/src/main/res/xml/file_paths.xml`
  
- **`AndroidManifest.xml`** - 添加 FileProvider 和存储权限
  - 位置: `app/src/main/AndroidManifest.xml`

- **`strings.xml`** - 添加分享相关字符串
  - 位置: `app/src/main/res/values/strings.xml`

- **`activity_analysis_result.xml`** - 更新布局添加分享按钮
  - 位置: `app/src/main/res/layout/activity_analysis_result.xml`

## 使用方法

### 1. 生成报告图片

```kotlin
// 在 ViewModel 中使用
viewModel.generateShareImage { bitmap ->
    if (bitmap != null) {
        // 使用生成的 Bitmap
        // 例如: 分享、保存等
    }
}

// 生成简洁版报告图片
viewModel.generateCompactShareImage { bitmap ->
    // ...
}
```

### 2. Markdown 转图片

```kotlin
val converter = MarkdownToImageConverter(context)

// 基础使用
val markdown = """
    # 标题
    ## 副标题
    - 列表项 1
    - 列表项 2
    **加粗文本**
    *斜体文本*
""".trimIndent()

val bitmap = converter.convert(markdown)

// 自定义样式
val style = MarkdownToImageConverter.StyleConfig(
    width = 1080,
    padding = 60,
    baseTextSize = 16f,
    backgroundColor = Color.BLACK,
    textColor = Color.WHITE
)
val bitmap = converter.convert(markdown, style, title = "报告标题")
```

### 3. 分享报告

```kotlin
// 在 Activity 中调用
binding.btnShare.setOnClickListener {
    showShareOptions()
}

// 分享为图片
private fun shareAsImage() {
    viewModel.generateShareImage { bitmap ->
        // 保存到缓存并获取 URI
        val file = File(cacheDir, "share_report.png")
        FileOutputStream(file).use { out ->
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        val uri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        
        // 分享
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "分享报告"))
    }
}
```

## 技术特点

### MarkdownToImageConverter
- 使用原生 Canvas 绘制，性能优异
- 支持中文显示
- 支持自定义样式（字体、颜色、背景）
- 支持长文本自动分页
- 遵循 Material Design 风格

### ReportImageGenerator
- 专业美观的报告布局
- 深色主题设计
- 自适应评分颜色
- 支持完整版和简洁版两种模式
- 包含应用 Logo 和水印

## 样式配置

### 默认颜色配置
```kotlin
COLOR_BACKGROUND = 0xFF1A1A2E  // 深蓝背景
COLOR_TEXT_PRIMARY = 0xFFFFFFFF  // 白色主文字
COLOR_ACCENT = 0xFF4A90E2        // 强调色（蓝色）
COLOR_SUCCESS = 0xFF4CAF50       // 成功/买入色
COLOR_WARNING = 0xFFFFA726       // 警告/持有色
COLOR_DANGER = 0xFFEF5350        // 危险/卖出色
```

### 决策颜色映射
- 强烈买入 (STRONG_BUY) - 深绿色
- 买入 (BUY) - 绿色
- 持有 (HOLD) - 黄色/橙色
- 卖出 (SELL) - 浅红色
- 强烈卖出 (STRONG_SELL) - 深红色

## 注意事项

1. **权限**: Android 10 以下需要存储权限来保存图片到相册
2. **FileProvider**: 分享图片需要使用 FileProvider 获取 content:// URI
3. **内存管理**: 生成的 Bitmap 较大，注意在不需要时调用 `recycle()`
4. **图片尺寸**: 默认宽度 1080px，适合大多数社交媒体分享

## 依赖要求

```kotlin
// build.gradle (Module: app)
dependencies {
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // 已有依赖，确保存在
    implementation("androidx.core:core-ktx:1.12.0")
}
```

## 测试建议

1. 测试不同长度的 Markdown 文本
2. 测试包含表格和代码块的复杂报告
3. 测试不同决策类型（买入/卖出/持有）的颜色显示
4. 测试分享功能到不同应用（微信、邮件等）
5. 测试保存到相册功能
