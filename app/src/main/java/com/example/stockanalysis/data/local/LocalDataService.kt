package com.example.stockanalysis.data.local

import com.example.stockanalysis.data.model.*
import com.example.stockanalysis.utils.PinyinSearchHelper
import com.example.stockanalysis.utils.SearchResultItem
import com.example.stockanalysis.utils.TechnicalIndicatorCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 本地数据服务
 * 生成和管理本地模拟股票数据，支持拼音/别名搜索
 * 
 * 股票池包含100+只常见A股（沪深300成分股）、热门港股和美股
 */
class LocalDataService(
    private val kLineDataDao: KLineDataDao
) {
    
    /**
     * 股票池 - 包含A股（沪深300成分股）、港股、美股常见股票
     * 总计100+只股票
     */
    private val stockPool = listOf(
        // ==================== A股 - 金融板块 ====================
        StockInfo("000001", "平安银行", MarketType.A_SHARE),
        StockInfo("600036", "招商银行", MarketType.A_SHARE),
        StockInfo("002142", "宁波银行", MarketType.A_SHARE),
        StockInfo("601166", "兴业银行", MarketType.A_SHARE),
        StockInfo("600016", "民生银行", MarketType.A_SHARE),
        StockInfo("601398", "工商银行", MarketType.A_SHARE),
        StockInfo("601288", "农业银行", MarketType.A_SHARE),
        StockInfo("601988", "中国银行", MarketType.A_SHARE),
        StockInfo("601328", "交通银行", MarketType.A_SHARE),
        StockInfo("600030", "中信证券", MarketType.A_SHARE),
        StockInfo("600837", "海通证券", MarketType.A_SHARE),
        StockInfo("000776", "广发证券", MarketType.A_SHARE),
        
        // ==================== A股 - 白酒/消费 ====================
        StockInfo("600519", "贵州茅台", MarketType.A_SHARE),
        StockInfo("000858", "五粮液", MarketType.A_SHARE),
        StockInfo("000568", "泸州老窖", MarketType.A_SHARE),
        StockInfo("002304", "洋河股份", MarketType.A_SHARE),
        StockInfo("600809", "山西汾酒", MarketType.A_SHARE),
        StockInfo("603288", "海天味业", MarketType.A_SHARE),
        StockInfo("600887", "伊利股份", MarketType.A_SHARE),
        StockInfo("000895", "双汇发展", MarketType.A_SHARE),
        StockInfo("300999", "金龙鱼", MarketType.A_SHARE),
        StockInfo("600600", "青岛啤酒", MarketType.A_SHARE),
        
        // ==================== A股 - 医药/医疗 ====================
        StockInfo("600276", "恒瑞医药", MarketType.A_SHARE),
        StockInfo("300760", "迈瑞医疗", MarketType.A_SHARE),
        StockInfo("603259", "药明康德", MarketType.A_SHARE),
        StockInfo("000538", "云南白药", MarketType.A_SHARE),
        StockInfo("600436", "片仔癀", MarketType.A_SHARE),
        StockInfo("300122", "智飞生物", MarketType.A_SHARE),
        StockInfo("300142", "沃森生物", MarketType.A_SHARE),
        StockInfo("600196", "复星医药", MarketType.A_SHARE),
        StockInfo("603392", "万泰生物", MarketType.A_SHARE),
        StockInfo("300896", "爱美客", MarketType.A_SHARE),
        StockInfo("300003", "乐普医疗", MarketType.A_SHARE),
        StockInfo("600085", "同仁堂", MarketType.A_SHARE),
        
        // ==================== A股 - 新能源/汽车 ====================
        StockInfo("300750", "宁德时代", MarketType.A_SHARE),
        StockInfo("002594", "比亚迪", MarketType.A_SHARE),
        StockInfo("601012", "隆基绿能", MarketType.A_SHARE),
        StockInfo("600438", "通威股份", MarketType.A_SHARE),
        StockInfo("002812", "恩捷股份", MarketType.A_SHARE),
        StockInfo("002709", "天赐材料", MarketType.A_SHARE),
        StockInfo("300014", "亿纬锂能", MarketType.A_SHARE),
        StockInfo("603659", "璞泰来", MarketType.A_SHARE),
        StockInfo("002460", "赣锋锂业", MarketType.A_SHARE),
        StockInfo("002466", "天齐锂业", MarketType.A_SHARE),
        StockInfo("300450", "先导智能", MarketType.A_SHARE),
        StockInfo("002074", "国轩高科", MarketType.A_SHARE),
        StockInfo("600660", "福耀玻璃", MarketType.A_SHARE),
        StockInfo("601633", "长城汽车", MarketType.A_SHARE),
        StockInfo("000625", "长安汽车", MarketType.A_SHARE),
        StockInfo("600104", "上汽集团", MarketType.A_SHARE),
        
        // ==================== A股 - 科技/半导体 ====================
        StockInfo("002415", "海康威视", MarketType.A_SHARE),
        StockInfo("002230", "科大讯飞", MarketType.A_SHARE),
        StockInfo("300124", "汇川技术", MarketType.A_SHARE),
        StockInfo("603501", "韦尔股份", MarketType.A_SHARE),
        StockInfo("688981", "中芯国际", MarketType.A_SHARE),
        StockInfo("002049", "紫光国微", MarketType.A_SHARE),
        StockInfo("603986", "兆易创新", MarketType.A_SHARE),
        StockInfo("300782", "卓胜微", MarketType.A_SHARE),
        StockInfo("688008", "澜起科技", MarketType.A_SHARE),
        StockInfo("688012", "中微公司", MarketType.A_SHARE),
        StockInfo("600584", "长电科技", MarketType.A_SHARE),
        StockInfo("000725", "京东方A", MarketType.A_SHARE),
        StockInfo("000100", "TCL科技", MarketType.A_SHARE),
        StockInfo("300408", "三环集团", MarketType.A_SHARE),
        StockInfo("002371", "北方华创", MarketType.A_SHARE),
        StockInfo("688126", "沪硅产业", MarketType.A_SHARE),
        StockInfo("300661", "圣邦股份", MarketType.A_SHARE),
        StockInfo("603893", "瑞芯微", MarketType.A_SHARE),
        
        // ==================== A股 - 互联网/软件 ====================
        StockInfo("300059", "东方财富", MarketType.A_SHARE),
        StockInfo("600570", "恒生电子", MarketType.A_SHARE),
        StockInfo("600588", "用友网络", MarketType.A_SHARE),
        StockInfo("300033", "同花顺", MarketType.A_SHARE),
        StockInfo("000938", "中芯国际", MarketType.A_SHARE),
        StockInfo("000977", "浪潮信息", MarketType.A_SHARE),
        StockInfo("002236", "大华股份", MarketType.A_SHARE),
        StockInfo("300454", "深信服", MarketType.A_SHARE),
        
        // ==================== A股 - 化工/材料 ====================
        StockInfo("600309", "万华化学", MarketType.A_SHARE),
        StockInfo("002001", "新和成", MarketType.A_SHARE),
        StockInfo("600426", "华鲁恒升", MarketType.A_SHARE),
        StockInfo("002064", "华峰化学", MarketType.A_SHARE),
        StockInfo("601216", "君正集团", MarketType.A_SHARE),
        StockInfo("002092", "中泰化学", MarketType.A_SHARE),
        
        // ==================== A股 - 电力/能源 ====================
        StockInfo("600900", "长江电力", MarketType.A_SHARE),
        StockInfo("601088", "中国神华", MarketType.A_SHARE),
        StockInfo("601857", "中国石油", MarketType.A_SHARE),
        StockInfo("600028", "中国石化", MarketType.A_SHARE),
        StockInfo("601985", "中国核电", MarketType.A_SHARE),
        StockInfo("601727", "上海电气", MarketType.A_SHARE),
        StockInfo("601669", "中国电建", MarketType.A_SHARE),
        StockInfo("601868", "中国能建", MarketType.A_SHARE),
        
        // ==================== A股 - 建筑/基建 ====================
        StockInfo("601668", "中国建筑", MarketType.A_SHARE),
        StockInfo("601390", "中国中铁", MarketType.A_SHARE),
        StockInfo("601800", "中国交建", MarketType.A_SHARE),
        StockInfo("601186", "中国铁建", MarketType.A_SHARE),
        StockInfo("601618", "中国中冶", MarketType.A_SHARE),
        StockInfo("601117", "中国化学", MarketType.A_SHARE),
        
        // ==================== A股 - 军工/航空 ====================
        StockInfo("600893", "航发动力", MarketType.A_SHARE),
        StockInfo("600760", "中航沈飞", MarketType.A_SHARE),
        StockInfo("000768", "中航西飞", MarketType.A_SHARE),
        StockInfo("002179", "中航光电", MarketType.A_SHARE),
        StockInfo("600372", "中航电子", MarketType.A_SHARE),
        StockInfo("600038", "中直股份", MarketType.A_SHARE),
        
        // ==================== A股 - 交通运输 ====================
        StockInfo("601111", "中国国航", MarketType.A_SHARE),
        StockInfo("600029", "南方航空", MarketType.A_SHARE),
        StockInfo("600115", "中国东航", MarketType.A_SHARE),
        StockInfo("601006", "大秦铁路", MarketType.A_SHARE),
        StockInfo("600009", "上海机场", MarketType.A_SHARE),
        
        // ==================== A股 - 零售/免税 ====================
        StockInfo("601888", "中国中免", MarketType.A_SHARE),
        StockInfo("600859", "王府井", MarketType.A_SHARE),
        
        // ==================== A股 - 家电 ====================
        StockInfo("000333", "美的集团", MarketType.A_SHARE),
        StockInfo("000651", "格力电器", MarketType.A_SHARE),
        StockInfo("600690", "海尔智家", MarketType.A_SHARE),
        StockInfo("002032", "苏泊尔", MarketType.A_SHARE),
        StockInfo("002508", "老板电器", MarketType.A_SHARE),
        
        // ==================== A股 - 保险 ====================
        StockInfo("601318", "中国平安", MarketType.A_SHARE),
        StockInfo("601628", "中国人寿", MarketType.A_SHARE),
        StockInfo("601336", "新华保险", MarketType.A_SHARE),
        StockInfo("601601", "中国太保", MarketType.A_SHARE),
        
        // ==================== A股 - 地产 ====================
        StockInfo("000002", "万科A", MarketType.A_SHARE),
        StockInfo("600048", "保利发展", MarketType.A_SHARE),
        StockInfo("001979", "招商蛇口", MarketType.A_SHARE),
        
        // ==================== A股 - 有色金属 ====================
        StockInfo("601899", "紫金矿业", MarketType.A_SHARE),
        StockInfo("600547", "山东黄金", MarketType.A_SHARE),
        StockInfo("600111", "北方稀土", MarketType.A_SHARE),
        StockInfo("603799", "华友钴业", MarketType.A_SHARE),
        StockInfo("300618", "寒锐钴业", MarketType.A_SHARE),
        
        // ==================== A股 - 通信 ====================
        StockInfo("600050", "中国联通", MarketType.A_SHARE),
        StockInfo("000063", "中兴通讯", MarketType.A_SHARE),
        StockInfo("600941", "中国移动", MarketType.A_SHARE),
        StockInfo("601728", "中国电信", MarketType.A_SHARE),
        
        // ==================== 港股 - 科技/互联网 ====================
        StockInfo("00700", "腾讯控股", MarketType.HK),
        StockInfo("09988", "阿里巴巴-SW", MarketType.HK),
        StockInfo("03690", "美团-W", MarketType.HK),
        StockInfo("01810", "小米集团-W", MarketType.HK),
        StockInfo("09618", "京东集团-SW", MarketType.HK),
        StockInfo("01024", "快手-W", MarketType.HK),
        StockInfo("09888", "百度集团-SW", MarketType.HK),
        StockInfo("02015", "理想汽车-W", MarketType.HK),
        StockInfo("09868", "小鹏汽车-W", MarketType.HK),
        StockInfo("06690", "海尔智家", MarketType.HK),
        StockInfo("09626", "哔哩哔哩-W", MarketType.HK),
        StockInfo("09999", "网易-S", MarketType.HK),
        
        // ==================== 港股 - 金融 ====================
        StockInfo("02318", "中国平安", MarketType.HK),
        StockInfo("03968", "招商银行", MarketType.HK),
        StockInfo("01398", "工商银行", MarketType.HK),
        StockInfo("01288", "农业银行", MarketType.HK),
        StockInfo("03988", "中国银行", MarketType.HK),
        StockInfo("06837", "海通证券", MarketType.HK),
        
        // ==================== 港股 - 地产/基建 ====================
        StockInfo("01109", "华润置地", MarketType.HK),
        StockInfo("00688", "中国海外发展", MarketType.HK),
        StockInfo("00960", "龙湖集团", MarketType.HK),
        StockInfo("02007", "碧桂园", MarketType.HK),
        StockInfo("03333", "恒大物业", MarketType.HK),
        StockInfo("03900", "绿城中国", MarketType.HK),
        
        // ==================== 港股 - 消费/零售 ====================
        StockInfo("02331", "李宁", MarketType.HK),
        StockInfo("02020", "安踏体育", MarketType.HK),
        StockInfo("06186", "中国飞鹤", MarketType.HK),
        StockInfo("00291", "华润啤酒", MarketType.HK),
        StockInfo("01898", "中煤能源", MarketType.HK),
        
        // ==================== 港股 - 医药 ====================
        StockInfo("02269", "药明生物", MarketType.HK),
        StockInfo("01093", "石药集团", MarketType.HK),
        StockInfo("02359", "药明康德", MarketType.HK),
        StockInfo("01177", "中国生物制药", MarketType.HK),
        StockInfo("03613", "同仁堂国药", MarketType.HK),
        
        // ==================== 港股 - 能源/公用事业 ====================
        StockInfo("00883", "中国海洋石油", MarketType.HK),
        StockInfo("00857", "中国石油股份", MarketType.HK),
        StockInfo("00386", "中国石油化工股份", MarketType.HK),
        StockInfo("00902", "华能国际电力", MarketType.HK),
        StockInfo("02380", "中国电力", MarketType.HK),
        
        // ==================== 美股 - 科技巨头 ====================
        StockInfo("AAPL", "Apple Inc.", MarketType.US),
        StockInfo("MSFT", "Microsoft Corp.", MarketType.US),
        StockInfo("GOOGL", "Alphabet Inc.", MarketType.US),
        StockInfo("AMZN", "Amazon.com Inc.", MarketType.US),
        StockInfo("META", "Meta Platforms Inc.", MarketType.US),
        StockInfo("NVDA", "NVIDIA Corp.", MarketType.US),
        StockInfo("TSLA", "Tesla Inc.", MarketType.US),
        StockInfo("NFLX", "Netflix Inc.", MarketType.US),
        StockInfo("AMD", "Advanced Micro Devices", MarketType.US),
        StockInfo("INTC", "Intel Corp.", MarketType.US),
        StockInfo("CSCO", "Cisco Systems", MarketType.US),
        StockInfo("ORCL", "Oracle Corp.", MarketType.US),
        StockInfo("IBM", "IBM Corp.", MarketType.US),
        StockInfo("CRM", "Salesforce Inc.", MarketType.US),
        StockInfo("ADBE", "Adobe Inc.", MarketType.US),
        StockInfo("UBER", "Uber Technologies", MarketType.US),
        StockInfo("ABNB", "Airbnb Inc.", MarketType.US),
        
        // ==================== 美股 - 中概股 ====================
        StockInfo("BABA", "Alibaba Group", MarketType.US),
        StockInfo("PDD", "PDD Holdings Inc.", MarketType.US),
        StockInfo("JD", "JD.com Inc.", MarketType.US),
        StockInfo("NIO", "NIO Inc.", MarketType.US),
        StockInfo("LI", "Li Auto Inc.", MarketType.US),
        StockInfo("XPEV", "XPeng Inc.", MarketType.US),
        StockInfo("BIDU", "Baidu Inc.", MarketType.US),
        StockInfo("TCEHY", "Tencent Holdings ADR", MarketType.US),
        StockInfo("DIDI", "DiDi Global Inc.", MarketType.US),
        StockInfo("BEKE", "KE Holdings Inc.", MarketType.US),
        StockInfo("ZTO", "ZTO Express", MarketType.US),
        StockInfo("EDU", "New Oriental Education", MarketType.US),
        StockInfo("TAL", "TAL Education Group", MarketType.US),
        StockInfo("VIPS", "Vipshop Holdings", MarketType.US),
        
        // ==================== 美股 - 金融 ====================
        StockInfo("BRK.B", "Berkshire Hathaway", MarketType.US),
        StockInfo("JPM", "JPMorgan Chase", MarketType.US),
        StockInfo("BAC", "Bank of America", MarketType.US),
        StockInfo("WFC", "Wells Fargo", MarketType.US),
        StockInfo("GS", "Goldman Sachs", MarketType.US),
        StockInfo("MS", "Morgan Stanley", MarketType.US),
        StockInfo("V", "Visa Inc.", MarketType.US),
        StockInfo("MA", "Mastercard Inc.", MarketType.US),
        StockInfo("AXP", "American Express", MarketType.US),
        
        // ==================== 美股 - 半导体/芯片 ====================
        StockInfo("TSM", "Taiwan Semiconductor", MarketType.US),
        StockInfo("ASML", "ASML Holding", MarketType.US),
        StockInfo("AVGO", "Broadcom Inc.", MarketType.US),
        StockInfo("QCOM", "Qualcomm Inc.", MarketType.US),
        StockInfo("TXN", "Texas Instruments", MarketType.US),
        StockInfo("MU", "Micron Technology", MarketType.US),
        StockInfo("LRCX", "Lam Research", MarketType.US),
        
        // ==================== 美股 - 医药/生物 ====================
        StockInfo("JNJ", "Johnson & Johnson", MarketType.US),
        StockInfo("PFE", "Pfizer Inc.", MarketType.US),
        StockInfo("MRK", "Merck & Co.", MarketType.US),
        StockInfo("ABBV", "AbbVie Inc.", MarketType.US),
        StockInfo("TMO", "Thermo Fisher Scientific", MarketType.US),
        StockInfo("UNH", "UnitedHealth Group", MarketType.US),
        StockInfo("LLY", "Eli Lilly", MarketType.US),
        
        // ==================== 美股 - 消费/零售 ====================
        StockInfo("WMT", "Walmart Inc.", MarketType.US),
        StockInfo("COST", "Costco Wholesale", MarketType.US),
        StockInfo("HD", "Home Depot", MarketType.US),
        StockInfo("MCD", "McDonald's Corp.", MarketType.US),
        StockInfo("NKE", "Nike Inc.", MarketType.US),
        StockInfo("DIS", "Walt Disney", MarketType.US),
        StockInfo("SBUX", "Starbucks Corp.", MarketType.US),
        StockInfo("KO", "Coca-Cola Co.", MarketType.US),
        StockInfo("PEP", "PepsiCo Inc.", MarketType.US),
        StockInfo("PG", "Procter & Gamble", MarketType.US),
        StockInfo("TM", "Toyota Motor", MarketType.US)
    )
    
    data class StockInfo(
        val symbol: String,
        val name: String,
        val market: MarketType
    )
    
    /**
     * 获取股票池
     */
    fun getStockPool(): List<Pair<String, String>> {
        return stockPool.map { it.symbol to it.name }
    }
    
    /**
     * 获取股票池数量
     */
    fun getStockPoolSize(): Int = stockPool.size
    
    /**
     * 搜索股票（支持拼音、别名、代码、名称）
     * @param query 搜索查询
     * @param limit 返回结果数量限制
     * @return 匹配的股票列表，按相关度排序
     */
    fun searchStocks(query: String, limit: Int = 20): List<Pair<String, String>> {
        if (query.isBlank()) {
            return emptyList()
        }
        
        // 使用拼音搜索辅助类计算匹配得分
        val results = stockPool.mapNotNull { stock ->
            val score = PinyinSearchHelper.calculateMatchScore(query, stock.symbol, stock.name)
            if (score > 0) {
                SearchResultItem(stock.symbol, stock.name, score)
            } else {
                null
            }
        }.sorted().take(limit)
        
        return results.map { it.symbol to it.name }
    }
    
    /**
     * 获取搜索建议（用于自动完成）
     * @param query 搜索查询
     * @param limit 返回结果数量限制
     * @return 格式化的搜索建议列表
     */
    fun getSearchSuggestions(query: String, limit: Int = 10): List<SearchSuggestion> {
        if (query.isBlank() || query.length < 1) {
            return emptyList()
        }
        
        return stockPool.mapNotNull { stock ->
            val score = PinyinSearchHelper.calculateMatchScore(query, stock.symbol, stock.name)
            if (score > 0) {
                val pinyinAbbr = PinyinSearchHelper.getPinyinAbbreviation(stock.name)
                val aliases = PinyinSearchHelper.getAliases(stock.symbol)
                val matchedAlias = aliases.find { it.contains(query, ignoreCase = true) }
                
                SearchSuggestion(
                    symbol = stock.symbol,
                    name = stock.name,
                    market = stock.market,
                    matchScore = score,
                    pinyinAbbr = pinyinAbbr,
                    highlightAlias = matchedAlias
                )
            } else {
                null
            }
        }.sortedByDescending { it.matchScore }.take(limit)
    }
    
    /**
     * 搜索建议数据类
     */
    data class SearchSuggestion(
        val symbol: String,
        val name: String,
        val market: MarketType,
        val matchScore: Int,
        val pinyinAbbr: String,
        val highlightAlias: String? = null
    ) {
        /**
         * 获取显示文本（包含别名提示）
         */
        fun getDisplayText(): String {
            return if (highlightAlias != null && highlightAlias != name) {
                "$name ($highlightAlias)"
            } else {
                "$name ($pinyinAbbr)"
            }
        }
        
        /**
         * 获取市场类型显示
         */
        fun getMarketDisplay(): String {
            return when (market) {
                MarketType.A_SHARE -> "A股"
                MarketType.HK -> "港股"
                MarketType.US -> "美股"
            }
        }
    }
    
    /**
     * 获取或生成K线数据
     */
    suspend fun getOrGenerateKLineData(symbol: String, days: Int = 90): List<KLineData> = withContext(Dispatchers.IO) {
        val existingData = kLineDataDao.getKLineData(symbol, days)
        
        if (existingData.size >= days) {
            return@withContext existingData.reversed() // 返回正序
        }
        
        // 生成模拟数据
        val generatedData = generateMockKLineData(symbol, days)
        kLineDataDao.insertKLineDataList(generatedData)
        return@withContext generatedData.reversed()
    }
    
    /**
     * 生成模拟K线数据
     */
    private fun generateMockKLineData(symbol: String, days: Int): List<KLineData> {
        val stockInfo = stockPool.find { it.symbol == symbol }
        val basePrice = when (symbol) {
            // A股
            "600519" -> 1700.0 // 茅台
            "300750" -> 180.0  // 宁德时代
            "000858" -> 140.0  // 五粮液
            "601318" -> 45.0   // 中国平安
            "600036" -> 32.0   // 招商银行
            "600276" -> 42.0   // 恒瑞医药
            "002594" -> 240.0  // 比亚迪
            "000001" -> 11.0   // 平安银行
            "300059" -> 15.0   // 东方财富
            // 港股
            "00700" -> 380.0   // 腾讯
            "09988" -> 85.0    // 阿里
            "03690" -> 120.0   // 美团
            // 美股
            "AAPL" -> 180.0    // 苹果
            "TSLA" -> 250.0    // 特斯拉
            "NVDA" -> 480.0    // 英伟达
            "MSFT" -> 380.0    // 微软
            "GOOGL" -> 140.0   // 谷歌
            "AMZN" -> 180.0    // 亚马逊
            "META" -> 500.0    // Meta
            "BABA" -> 75.0     // 阿里
            else -> Random.nextDouble(10.0, 200.0)
        }
        
        val data = mutableListOf<KLineData>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        
        var lastClose = basePrice
        
        for (i in 0 until days) {
            // 跳过周末
            while (calendar.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            // 生成价格波动（带趋势性）
            val trendBias = if (i > days / 2) 0.002 else -0.001 // 后半段趋势向上
            val volatility = 0.02 // 2%波动率
            
            val open = lastClose * (1 + Random.nextDouble(-volatility, volatility) + trendBias)
            val close = open * (1 + Random.nextDouble(-volatility, volatility))
            val high = max(open, close) * (1 + Random.nextDouble(0.0, volatility / 2))
            val low = min(open, close) * (1 - Random.nextDouble(0.0, volatility / 2))
            
            val volume = Random.nextLong(1000000, 10000000)
            val amount = volume * ((high + low) / 2)
            val change = close - lastClose
            val changePercent = (change / lastClose) * 100
            
            data.add(KLineData(
                symbol = symbol,
                timestamp = calendar.time,
                open = round(open, 2),
                high = round(high, 2),
                low = round(low, 2),
                close = round(close, 2),
                volume = volume,
                amount = round(amount, 2),
                change = round(change, 2),
                changePercent = round(changePercent, 2)
            ))
            
            lastClose = close
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return data
    }
    
    /**
     * 获取实时行情（模拟）
     */
    fun getRealtimeQuote(symbol: String): RealtimeQuote? {
        val stockInfo = stockPool.find { it.symbol == symbol } ?: return null
        
        // 基于已存储的K线数据生成实时价格
        val lastClose = Random.nextDouble(10.0, 200.0)
        val changePercent = Random.nextDouble(-3.0, 3.0)
        val change = lastClose * changePercent / 100
        val currentPrice = lastClose + change
        
        return RealtimeQuote(
            symbol = symbol,
            name = stockInfo.name,
            price = round(currentPrice, 2),
            open = round(lastClose * (1 + Random.nextDouble(-0.01, 0.01)), 2),
            high = round(currentPrice * (1 + Random.nextDouble(0.0, 0.02)), 2),
            low = round(currentPrice * (1 - Random.nextDouble(0.0, 0.02)), 2),
            preClose = round(lastClose, 2),
            volume = Random.nextLong(1000000, 50000000),
            amount = Random.nextDouble(100000000.0, 5000000000.0),
            change = round(change, 2),
            changePercent = round(changePercent, 2),
            volumeRatio = Random.nextDouble(0.5, 3.0),
            turnoverRate = Random.nextDouble(0.5, 5.0),
            peRatio = Random.nextDouble(10.0, 50.0),
            pbRatio = Random.nextDouble(1.0, 10.0),
            marketCap = Random.nextDouble(10000000000.0, 2000000000000.0),
            circMarketCap = Random.nextDouble(5000000000.0, 1000000000000.0),
            bidPrice = round(currentPrice - 0.01, 2),
            bidVolume = Random.nextLong(100, 10000),
            askPrice = round(currentPrice + 0.01, 2),
            askVolume = Random.nextLong(100, 10000)
        )
    }
    
    /**
     * 获取筹码分布数据（模拟）
     */
    fun getChipDistribution(symbol: String): ChipDistribution? {
        val stockInfo = stockPool.find { it.symbol == symbol } ?: return null
        val basePrice = Random.nextDouble(10.0, 200.0)
        
        return ChipDistribution(
            symbol = symbol,
            avgCost = round(basePrice * Random.nextDouble(0.95, 1.05), 2),
            profitRatio = Random.nextDouble(0.2, 0.8),
            concentration90 = Random.nextDouble(0.05, 0.3),
            concentration70 = Random.nextDouble(0.03, 0.2),
            peakPrice = round(basePrice * Random.nextDouble(0.98, 1.02), 2),
            supportLevels = listOf(
                round(basePrice * 0.9, 2),
                round(basePrice * 0.85, 2)
            ),
            resistanceLevels = listOf(
                round(basePrice * 1.1, 2),
                round(basePrice * 1.15, 2)
            )
        )
    }
    
    /**
     * 获取技术指标
     */
    suspend fun getTechnicalIndicators(symbol: String): TechnicalIndicators? = withContext(Dispatchers.IO) {
        val klineData = kLineDataDao.getKLineData(symbol, 90)
        if (klineData.size < 60) return@withContext null
        
        return@withContext TechnicalIndicatorCalculator.calculateAllIndicators(klineData.reversed())
    }
    
    /**
     * 获取趋势分析
     */
    suspend fun getTrendAnalysis(symbol: String): TrendAnalysis? = withContext(Dispatchers.IO) {
        val klineData = kLineDataDao.getKLineData(symbol, 60).reversed()
        if (klineData.size < 20) return@withContext null
        
        val currentPrice = klineData.last().close
        return@withContext TechnicalIndicatorCalculator.analyzeTrend(klineData, currentPrice)
    }
    
    /**
     * 获取分析上下文（用于AI分析）
     */
    suspend fun getAnalysisContext(symbol: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        val klineData = kLineDataDao.getKLineData(symbol, 30).reversed()
        val quote = getRealtimeQuote(symbol)
        val chip = getChipDistribution(symbol)
        val trend = if (klineData.size >= 20) {
            TechnicalIndicatorCalculator.analyzeTrend(klineData, klineData.lastOrNull()?.close ?: 0.0)
        } else null
        val indicators = if (klineData.size >= 60) {
            TechnicalIndicatorCalculator.calculateAllIndicators(klineData)
        } else null
        
        mapOf(
            "symbol" to symbol,
            "name" to (quote?.name ?: ""),
            "klineData" to klineData.takeLast(5).map {
                mapOf(
                    "date" to it.timestamp,
                    "open" to it.open,
                    "high" to it.high,
                    "low" to it.low,
                    "close" to it.close,
                    "volume" to it.volume,
                    "change" to it.changePercent
                )
            },
            "realtime" to quote?.let {
                mapOf(
                    "price" to it.price,
                    "change" to it.change,
                    "changePercent" to it.changePercent,
                    "volume" to it.volume,
                    "turnoverRate" to it.turnoverRate,
                    "peRatio" to it.peRatio,
                    "pbRatio" to it.pbRatio
                )
            },
            "chip" to chip?.let {
                mapOf(
                    "avgCost" to it.avgCost,
                    "profitRatio" to it.profitRatio,
                    "concentration90" to it.concentration90,
                    "status" to it.getChipStatus(quote?.price ?: 0.0)
                )
            },
            "trend" to trend?.let {
                mapOf(
                    "status" to it.trendStatus.name,
                    "strength" to it.trendStrength,
                    "buySignal" to it.buySignal.name,
                    "score" to it.signalScore,
                    "biasMa5" to it.biasMa5,
                    "biasMa10" to it.biasMa10,
                    "volumeStatus" to it.volumeStatus.name
                )
            },
            "indicators" to indicators?.let {
                mapOf(
                    "ma5" to it.movingAverages?.ma5,
                    "ma10" to it.movingAverages?.ma10,
                    "ma20" to it.movingAverages?.ma20,
                    "macd" to it.macd?.let { m ->
                        "DIF:${String.format("%.2f", m.dif)} DEA:${String.format("%.2f", m.dea)}"
                    },
                    "rsi6" to it.rsi6,
                    "kdj" to it.kdj?.let { k ->
                        "K:${String.format("%.1f", k.k)} D:${String.format("%.1f", k.d)} J:${String.format("%.1f", k.j)}"
                    }
                )
            }
        )
    }
    
    /**
     * 刷新股票数据
     */
    suspend fun refreshStockData(symbol: String) = withContext(Dispatchers.IO) {
        // 生成今天的数据
        val latestData = kLineDataDao.getLatestKLine(symbol)
        val calendar = Calendar.getInstance()
        
        // 如果最新数据不是今天，生成新数据
        if (latestData == null || !isSameDay(latestData.timestamp, calendar.time)) {
            val lastClose = latestData?.close ?: Random.nextDouble(10.0, 200.0)
            val changePercent = Random.nextDouble(-3.0, 3.0)
            val close = lastClose * (1 + changePercent / 100)
            
            val newData = KLineData(
                symbol = symbol,
                timestamp = calendar.time,
                open = round(lastClose * (1 + Random.nextDouble(-0.01, 0.01)), 2),
                high = round(max(lastClose, close) * (1 + Random.nextDouble(0.0, 0.01)), 2),
                low = round(min(lastClose, close) * (1 - Random.nextDouble(0.0, 0.01)), 2),
                close = round(close, 2),
                volume = Random.nextLong(1000000, 50000000),
                amount = Random.nextDouble(100000000.0, 5000000000.0),
                change = round(close - lastClose, 2),
                changePercent = round(changePercent, 2)
            )
            
            kLineDataDao.insertKLineData(newData)
        }
    }
    
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun round(value: Double, decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return kotlin.math.round(value * factor) / factor
    }
}
