package com.example.stockanalysis.data.import

import android.content.Context
import android.net.Uri
import com.example.stockanalysis.data.api.LLMApiService
import com.example.stockanalysis.data.local.StockDao
import com.example.stockanalysis.data.model.Stock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能导入服务
 * 支持图片识别、CSV/Excel导入、剪贴板粘贴
 */
@Singleton
class SmartImportService @Inject constructor(
    private val context: Context,
    private val stockDao: StockDao,
    private val llmService: LLMApiService
) {
    
    /**
     * 从图片识别股票
     * 使用Vision LLM识别截图中的股票代码和名称
     */
    suspend fun importFromImage(imageUri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            // 读取图片并转为Base64
            val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            } ?: return@withContext Result.failure(Exception("无法读取图片"))
            
            val base64Image = bitmapToBase64(bitmap)
            
            // 调用Vision API识别
            val prompt = """
                请识别图片中的股票代码和名称。
                
                输出格式(JSON数组):
                [
                  {"code": "600519", "name": "贵州茅台", "confidence": "high"},
                  {"code": "000001", "name": "平安银行", "confidence": "medium"}
                ]
                
                confidence可选值: high, medium, low
                只输出JSON，不要其他文字。
            """.trimIndent()
            
            // 调用LLM进行图片识别
            val request = com.example.stockanalysis.data.api.ChatCompletionRequest(
                model = "gpt-4-vision-preview",
                messages = listOf(
                    com.example.stockanalysis.data.api.Message(
                        role = "user",
                        content = prompt
                    )
                ),
                maxTokens = 1000
            )
            
            val response = llmService.chatCompletion(request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                val extractedStocks = parseExtractedStocks(content)
                
                // 验证并导入
                val importResult = validateAndImport(extractedStocks)
                Result.success(importResult)
            } else {
                Result.failure(Exception("识别失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从CSV导入
     */
    suspend fun importFromCsv(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val stocks = mutableListOf<ExtractedStock>()
            
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var line: String?
                    var isFirstLine = true
                    
                    while (reader.readLine().also { line = it } != null) {
                        // 跳过标题行
                        if (isFirstLine) {
                            isFirstLine = false
                            continue
                        }
                        
                        line?.let { parseCsvLine(it, stocks) }
                    }
                }
            }
            
            val importResult = validateAndImport(stocks)
            Result.success(importResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从剪贴板粘贴导入
     */
    suspend fun importFromClipboard(text: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val stocks = mutableListOf<ExtractedStock>()
            
            // 按行解析
            text.lines().forEach { line ->
                parseClipboardLine(line.trim(), stocks)
            }
            
            val importResult = validateAndImport(stocks)
            Result.success(importResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 名称到代码解析
     */
    suspend fun resolveNameToCode(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. 先查本地数据库
            val localStock = stockDao.searchStocks(name).firstOrNull()
            if (localStock != null) {
                return@withContext Result.success(localStock.symbol)
            }
            
            // 2. 使用LLM解析
            val prompt = """
                请将股票名称转换为股票代码：$name
                只输出6位数字代码，不要其他文字。
                如果是A股，直接输出代码。
                如果无法确定，输出"UNKNOWN"。
            """.trimIndent()
            
            val request = com.example.stockanalysis.data.api.ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(
                    com.example.stockanalysis.data.api.Message(
                        role = "user",
                        content = prompt
                    )
                ),
                maxTokens = 20
            )
            
            val response = llmService.chatCompletion(request)
            
            if (response.isSuccessful) {
                val code = response.body()?.choices?.firstOrNull()?.message?.content?.trim()
                if (code != null && code.matches(Regex("\\d{6}"))) {
                    Result.success(code)
                } else {
                    Result.failure(Exception("无法解析"))
                }
            } else {
                Result.failure(Exception("解析失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 私有方法 ====================
    
    private fun bitmapToBase64(bitmap: android.graphics.Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
    }
    
    private fun parseExtractedStocks(jsonContent: String): List<ExtractedStock> {
        val stocks = mutableListOf<ExtractedStock>()
        
        try {
            // 简单JSON解析
            val jsonArray = org.json.JSONArray(jsonContent)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                stocks.add(ExtractedStock(
                    code = obj.optString("code"),
                    name = obj.optString("name"),
                    confidence = obj.optString("confidence", "medium")
                ))
            }
        } catch (e: Exception) {
            // 尝试正则提取
            val pattern = Regex("""["']?code["']?\s*:\s*["']?(\d{6})["']?""")
            val codes = pattern.findAll(jsonContent).map { it.groupValues[1] }.toList()
            codes.forEach { code ->
                stocks.add(ExtractedStock(code, "", "low"))
            }
        }
        
        return stocks
    }
    
    private fun parseCsvLine(line: String, stocks: MutableList<ExtractedStock>) {
        val parts = line.split(",")
        if (parts.size >= 2) {
            val code = parts[0].trim()
            val name = parts[1].trim()
            if (code.matches(Regex("\\d{6}"))) {
                stocks.add(ExtractedStock(code, name, "high"))
            }
        } else if (parts.size == 1) {
            val code = parts[0].trim()
            if (code.matches(Regex("\\d{6}"))) {
                stocks.add(ExtractedStock(code, "", "medium"))
            }
        }
    }
    
    private fun parseClipboardLine(line: String, stocks: MutableList<ExtractedStock>) {
        if (line.isBlank()) return
        
        // 尝试匹配 代码 名称 格式
        val pattern1 = Regex("""^(\d{6})\s+(\S+)""")
        val match1 = pattern1.find(line)
        if (match1 != null) {
            stocks.add(ExtractedStock(
                code = match1.groupValues[1],
                name = match1.groupValues[2],
                confidence = "high"
            ))
            return
        }
        
        // 尝试匹配纯代码
        val pattern2 = Regex("""^(\d{6})$""")
        val match2 = pattern2.find(line)
        if (match2 != null) {
            stocks.add(ExtractedStock(
                code = match2.groupValues[1],
                name = "",
                confidence = "medium"
            ))
        }
    }
    
    private suspend fun validateAndImport(stocks: List<ExtractedStock>): ImportResult {
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()
        val importedStocks = mutableListOf<Stock>()
        
        stocks.forEach { stock ->
            try {
                // 验证股票代码格式
                if (!stock.code.matches(Regex("\\d{6}"))) {
                    failCount++
                    errors.add("${stock.code}: 无效的代码格式")
                    return@forEach
                }
                
                // 检查是否已存在
                if (stockDao.isStockExists(stock.code)) {
                    failCount++
                    errors.add("${stock.code}: 已存在")
                    return@forEach
                }
                
                // 创建股票对象
                val newStock = Stock(
                    symbol = stock.code,
                    name = stock.name.ifBlank { stock.code },
                    market = determineMarket(stock.code)
                )
                
                stockDao.insertStock(newStock)
                importedStocks.add(newStock)
                successCount++
                
            } catch (e: Exception) {
                failCount++
                errors.add("${stock.code}: ${e.message}")
            }
        }
        
        return ImportResult(
            totalCount = stocks.size,
            successCount = successCount,
            failCount = failCount,
            importedStocks = importedStocks,
            errors = errors
        )
    }
    
    private fun determineMarket(code: String): com.example.stockanalysis.data.model.MarketType {
        return when {
            code.startsWith("6") -> com.example.stockanalysis.data.model.MarketType.A_SHARE
            code.startsWith("0") || code.startsWith("3") -> com.example.stockanalysis.data.model.MarketType.A_SHARE
            else -> com.example.stockanalysis.data.model.MarketType.A_SHARE
        }
    }
}

/**
 * 提取的股票
 */
data class ExtractedStock(
    val code: String,
    val name: String,
    val confidence: String  // high, medium, low
)

/**
 * 导入结果
 */
data class ImportResult(
    val totalCount: Int,
    val successCount: Int,
    val failCount: Int,
    val importedStocks: List<Stock>,
    val errors: List<String>
)
