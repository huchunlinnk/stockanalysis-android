package com.example.stockanalysis.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import java.util.regex.Pattern

/**
 * Markdown 转图片转换器
 * 支持基本的 Markdown 语法：标题、列表、加粗、斜体、表格
 */
class MarkdownToImageConverter(private val context: Context) {

    companion object {
        const val DEFAULT_WIDTH = 1080
        const val DEFAULT_PADDING = 60
        const val DEFAULT_TEXT_SIZE = 16f
        const val DEFAULT_LINE_SPACING = 1.5f
        
        const val COLOR_BACKGROUND = 0xFF1A1A2E.toInt()
        const val COLOR_TEXT_PRIMARY = 0xFFFFFFFF.toInt()
        const val COLOR_TEXT_SECONDARY = 0xFFB0B0B0.toInt()
        const val COLOR_ACCENT = 0xFF4A90E2.toInt()
        const val COLOR_DIVIDER = 0xFF333333.toInt()
    }

    data class StyleConfig(
        val width: Int = DEFAULT_WIDTH,
        val padding: Int = DEFAULT_PADDING,
        val baseTextSize: Float = DEFAULT_TEXT_SIZE,
        val lineSpacing: Float = DEFAULT_LINE_SPACING,
        val backgroundColor: Int = COLOR_BACKGROUND,
        val textColor: Int = COLOR_TEXT_PRIMARY,
        val accentColor: Int = COLOR_ACCENT
    )

    /**
     * 将 Markdown 转换为图片
     */
    fun convert(markdown: String, style: StyleConfig = StyleConfig(), title: String? = null): Bitmap {
        val elements = parseMarkdown(markdown, title)
        return renderToBitmap(elements, style)
    }

    /**
     * 解析 Markdown 为元素列表
     */
    private fun parseMarkdown(markdown: String, title: String?): List<MarkdownElement> {
        val elements = mutableListOf<MarkdownElement>()
        
        // 添加标题
        title?.let {
            elements.add(MarkdownElement.Title(it, 1))
            elements.add(MarkdownElement.Spacer(20))
        }
        
        val lines = markdown.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            when {
                // 标题
                line.startsWith("# ") -> {
                    elements.add(MarkdownElement.Title(line.substring(2), 1))
                    elements.add(MarkdownElement.Spacer(16))
                }
                line.startsWith("## ") -> {
                    elements.add(MarkdownElement.Title(line.substring(3), 2))
                    elements.add(MarkdownElement.Spacer(12))
                }
                line.startsWith("### ") -> {
                    elements.add(MarkdownElement.Title(line.substring(4), 3))
                    elements.add(MarkdownElement.Spacer(8))
                }
                // 分隔线
                line.trim() == "---" || line.trim() == "***" -> {
                    elements.add(MarkdownElement.Divider)
                    elements.add(MarkdownElement.Spacer(16))
                }
                // 列表项
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val content = line.trimStart().substring(2)
                    elements.add(MarkdownElement.ListItem(parseInline(content), 0))
                }
                line.trimStart().matches(Regex("^\\d+\\. .*")) -> {
                    val content = line.trimStart().substringAfter(". ")
                    elements.add(MarkdownElement.ListItem(parseInline(content), 0, true))
                }
                // 引用
                line.trimStart().startsWith("> ") -> {
                    elements.add(MarkdownElement.Quote(line.trimStart().substring(2)))
                }
                // 表格
                line.contains("|") && i + 1 < lines.size && lines[i + 1].contains("-") -> {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].contains("|")) {
                        tableLines.add(lines[i])
                        i++
                    }
                    i--
                    elements.add(parseTable(tableLines))
                    elements.add(MarkdownElement.Spacer(16))
                }
                // 代码块
                line.trimStart().startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    elements.add(MarkdownElement.CodeBlock(codeLines.joinToString("\n")))
                    elements.add(MarkdownElement.Spacer(16))
                }
                // 普通段落
                line.isNotBlank() -> {
                    elements.add(MarkdownElement.Paragraph(parseInline(line)))
                    elements.add(MarkdownElement.Spacer(8))
                }
                // 空行
                else -> {
                    elements.add(MarkdownElement.Spacer(8))
                }
            }
            i++
        }
        
        return elements
    }

    /**
     * 解析行内元素
     */
    private fun parseInline(text: String): List<InlineElement> {
        val elements = mutableListOf<InlineElement>()
        var remaining = text
        
        // 加粗 **text**
        val boldPattern = Pattern.compile("\\*\\*(.+?)\\*\\*")
        var matcher = boldPattern.matcher(remaining)
        var lastEnd = 0
        
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                elements.add(InlineElement.Text(remaining.substring(lastEnd, matcher.start())))
            }
            elements.add(InlineElement.Bold(matcher.group(1)))
            lastEnd = matcher.end()
        }
        
        if (lastEnd < remaining.length) {
            elements.add(InlineElement.Text(remaining.substring(lastEnd)))
        }
        
        return if (elements.isEmpty()) listOf(InlineElement.Text(text)) else elements
    }

    /**
     * 解析表格
     */
    private fun parseTable(lines: List<String>): MarkdownElement.Table {
        val rows = lines.filter { it.contains("|") && !it.contains("---") }
            .map { line ->
                line.split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        return MarkdownElement.Table(rows)
    }

    /**
     * 渲染为 Bitmap
     */
    private fun renderToBitmap(elements: List<MarkdownElement>, style: StyleConfig): Bitmap {
        val tempBitmap = Bitmap.createBitmap(style.width, 1000, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)
        
        var currentY = style.padding.toFloat()
        
        // 第一遍：计算总高度
        for (element in elements) {
            val height = measureElementHeight(element, style)
            currentY += height
        }
        
        val totalHeight = (currentY + style.padding).toInt()
        
        // 创建最终 Bitmap
        val bitmap = Bitmap.createBitmap(style.width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制背景
        canvas.drawColor(style.backgroundColor)
        
        // 第二遍：实际绘制
        currentY = style.padding.toFloat()
        for (element in elements) {
            currentY += drawElement(canvas, element, style, currentY)
        }
        
        return bitmap
    }

    /**
     * 测量元素高度
     */
    private fun measureElementHeight(element: MarkdownElement, style: StyleConfig): Float {
        return when (element) {
            is MarkdownElement.Title -> getTitleHeight(element.level, style)
            is MarkdownElement.Paragraph -> getParagraphHeight(element.content, style)
            is MarkdownElement.ListItem -> getListItemHeight(element.content, style)
            is MarkdownElement.Quote -> getQuoteHeight(element.text, style)
            is MarkdownElement.CodeBlock -> getCodeBlockHeight(element.code, style)
            is MarkdownElement.Table -> getTableHeight(element.rows, style)
            is MarkdownElement.Divider -> 2f + 16f
            is MarkdownElement.Spacer -> element.height.toFloat()
        }
    }

    /**
     * 绘制元素
     */
    private fun drawElement(canvas: Canvas, element: MarkdownElement, style: StyleConfig, y: Float): Float {
        return when (element) {
            is MarkdownElement.Title -> drawTitle(canvas, element, style, y)
            is MarkdownElement.Paragraph -> drawParagraph(canvas, element, style, y)
            is MarkdownElement.ListItem -> drawListItem(canvas, element, style, y)
            is MarkdownElement.Quote -> drawQuote(canvas, element, style, y)
            is MarkdownElement.CodeBlock -> drawCodeBlock(canvas, element, style, y)
            is MarkdownElement.Table -> drawTable(canvas, element, style, y)
            is MarkdownElement.Divider -> drawDivider(canvas, style, y)
            is MarkdownElement.Spacer -> element.height.toFloat()
        }
    }

    // ===== 绘制方法 =====

    private fun drawTitle(canvas: Canvas, element: MarkdownElement.Title, style: StyleConfig, y: Float): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textColor
            textSize = spToPx(when (element.level) {
                1 -> style.baseTextSize * 1.8f
                2 -> style.baseTextSize * 1.5f
                else -> style.baseTextSize * 1.2f
            })
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val contentWidth = style.width - style.padding * 2
        val layout = StaticLayout.Builder.obtain(
            element.text, 0, element.text.length, paint, contentWidth
        ).build()
        
        canvas.save()
        canvas.translate(style.padding.toFloat(), y)
        layout.draw(canvas)
        canvas.restore()
        
        return layout.height + when (element.level) {
            1 -> 16f
            2 -> 12f
            else -> 8f
        }
    }

    private fun drawParagraph(canvas: Canvas, element: MarkdownElement.Paragraph, style: StyleConfig, y: Float): Float {
        val contentWidth = style.width - style.padding * 2
        var totalHeight = 0f
        
        var currentX = style.padding.toFloat()
        
        for (inline in element.content) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = style.textColor
                textSize = spToPx(style.baseTextSize)
                typeface = when (inline) {
                    is InlineElement.Bold -> Typeface.DEFAULT_BOLD
                    else -> Typeface.DEFAULT
                }
            }
            
            val text = when (inline) {
                is InlineElement.Text -> inline.text
                is InlineElement.Bold -> inline.text
                else -> ""
            }
            
            val layout = StaticLayout.Builder.obtain(
                text, 0, text.length, paint, contentWidth.toInt()
            ).build()
            
            canvas.save()
            canvas.translate(currentX, y + totalHeight)
            layout.draw(canvas)
            canvas.restore()
            
            totalHeight += layout.height.toFloat()
        }
        
        return totalHeight + 8f
    }

    private fun drawListItem(canvas: Canvas, element: MarkdownElement.ListItem, style: StyleConfig, y: Float): Float {
        val bullet = if (element.ordered) "${element.index + 1}. " else "• "
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.accentColor
            textSize = spToPx(style.baseTextSize)
        }
        
        canvas.drawText(bullet, style.padding.toFloat() + 20f, y + paint.textSize, paint)
        
        val contentWidth = style.width - style.padding * 2 - 40f
        val layout = StaticLayout.Builder.obtain(
            element.content.joinToString("") { 
                when (it) {
                    is InlineElement.Text -> it.text
                    is InlineElement.Bold -> it.text
                    else -> ""
                }
            },
            0, 
            element.content.joinToString("").length,
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = style.textColor
                textSize = spToPx(style.baseTextSize)
            },
            contentWidth.toInt()
        ).build()
        
        canvas.save()
        canvas.translate(style.padding.toFloat() + 40f, y)
        layout.draw(canvas)
        canvas.restore()
        
        return layout.height + 4f
    }

    private fun drawQuote(canvas: Canvas, element: MarkdownElement.Quote, style: StyleConfig, y: Float): Float {
        // 左边框
        val linePaint = Paint().apply {
            color = style.accentColor
            strokeWidth = 4f
        }
        canvas.drawLine(
            style.padding.toFloat() + 10f, y,
            style.padding.toFloat() + 10f, y + 60f,
            linePaint
        )
        
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = spToPx(style.baseTextSize)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        
        val contentWidth = style.width - style.padding * 2 - 30
        val layout = StaticLayout.Builder.obtain(
            element.text, 0, element.text.length, paint, contentWidth
        ).build()
        
        canvas.save()
        canvas.translate(style.padding.toFloat() + 25f, y)
        layout.draw(canvas)
        canvas.restore()
        
        return layout.height + 12f
    }

    private fun drawCodeBlock(canvas: Canvas, element: MarkdownElement.CodeBlock, style: StyleConfig, y: Float): Float {
        val contentWidth = style.width - style.padding * 2
        val height = measureCodeBlockHeight(element.code, style)
        
        // 背景
        val bgPaint = Paint().apply {
            color = 0xFF252540.toInt()
        }
        canvas.drawRect(
            style.padding.toFloat(), y,
            style.width - style.padding.toFloat(), y + height,
            bgPaint
        )
        
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = spToPx(style.baseTextSize * 0.9f)
            typeface = Typeface.MONOSPACE
        }
        
        val layout = StaticLayout.Builder.obtain(
            element.code, 0, element.code.length, paint, contentWidth - 20
        ).build()
        
        canvas.save()
        canvas.translate(style.padding.toFloat() + 10f, y + 10f)
        layout.draw(canvas)
        canvas.restore()
        
        return height + 16f
    }

    private fun drawTable(canvas: Canvas, element: MarkdownElement.Table, style: StyleConfig, y: Float): Float {
        if (element.rows.isEmpty()) return 0f
        
        val rowHeight = 50f
        val colCount = element.rows.firstOrNull()?.size ?: 0
        val colWidth = (style.width - style.padding * 2) / colCount.toFloat()
        
        val paint = Paint().apply {
            color = COLOR_DIVIDER
            strokeWidth = 1f
        }
        
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textColor
            textSize = spToPx(style.baseTextSize * 0.9f)
        }
        
        // 绘制单元格
        element.rows.forEachIndexed { rowIndex, row ->
            val rowY = y + rowIndex * rowHeight
            
            // 横线
            canvas.drawLine(
                style.padding.toFloat(), rowY,
                style.width - style.padding.toFloat(), rowY,
                paint
            )
            
            row.forEachIndexed { colIndex, cell ->
                val colX = style.padding + colIndex * colWidth
                
                // 竖线
                canvas.drawLine(
                    colX.toFloat(), rowY,
                    colX.toFloat(), rowY + rowHeight,
                    paint
                )
                
                // 文本
                canvas.drawText(
                    cell.take(15),
                    colX + 10f,
                    rowY + rowHeight / 2 + textPaint.textSize / 2,
                    textPaint
                )
            }
        }
        
        return element.rows.size * rowHeight + 16f
    }

    private fun drawDivider(canvas: Canvas, style: StyleConfig, y: Float): Float {
        val paint = Paint().apply {
            color = COLOR_DIVIDER
            strokeWidth = 2f
        }
        canvas.drawLine(
            style.padding.toFloat(), y,
            style.width - style.padding.toFloat(), y,
            paint
        )
        return 18f
    }

    // ===== 测量方法 =====

    private fun getTitleHeight(level: Int, style: StyleConfig): Float {
        return when (level) {
            1 -> style.baseTextSize * 2.5f + 16f
            2 -> style.baseTextSize * 2f + 12f
            else -> style.baseTextSize * 1.5f + 8f
        }
    }

    private fun getParagraphHeight(content: List<InlineElement>, style: StyleConfig): Float {
        return style.baseTextSize * 1.5f + 8f
    }

    private fun getListItemHeight(content: List<InlineElement>, style: StyleConfig): Float {
        return style.baseTextSize * 1.5f + 4f
    }

    private fun getQuoteHeight(text: String, style: StyleConfig): Float {
        return style.baseTextSize * 1.5f + 12f
    }

    private fun getCodeBlockHeight(code: String, style: StyleConfig): Float {
        val lines = code.lines().size
        return lines * style.baseTextSize * 1.2f + 20f + 16f
    }

    private fun measureCodeBlockHeight(code: String, style: StyleConfig): Float {
        return getCodeBlockHeight(code, style)
    }

    private fun getTableHeight(rows: List<List<String>>, style: StyleConfig): Float {
        return rows.size * 50f + 16f
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics
        )
    }
}

// ===== 数据类 =====

sealed class MarkdownElement {
    data class Title(val text: String, val level: Int) : MarkdownElement()
    data class Paragraph(val content: List<InlineElement>) : MarkdownElement()
    data class ListItem(val content: List<InlineElement>, val index: Int, val ordered: Boolean = false) : MarkdownElement()
    data class Quote(val text: String) : MarkdownElement()
    data class CodeBlock(val code: String) : MarkdownElement()
    data class Table(val rows: List<List<String>>) : MarkdownElement()
    object Divider : MarkdownElement()
    data class Spacer(val height: Int) : MarkdownElement()
}

sealed class InlineElement {
    data class Text(val text: String) : InlineElement()
    data class Bold(val text: String) : InlineElement()
    data class Italic(val text: String) : InlineElement()
    data class Code(val text: String) : InlineElement()
    data class Link(val text: String, val url: String) : InlineElement()
}
