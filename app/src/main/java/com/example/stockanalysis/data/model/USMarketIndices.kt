package com.example.stockanalysis.data.model

/**
 * 美股指数枚举类
 * 定义主要美股指数及其与 Yahoo Finance 符号的映射
 */
enum class USMarketIndex(
    val displayName: String,           // 显示名称
    val chineseName: String,           // 中文名称
    val yahooSymbol: String,           // Yahoo Finance 符号
    val aliases: List<String>          // 别名列表（用户可能输入的各种形式）
) {
    SPX(
        displayName = "S&P 500",
        chineseName = "标普500指数",
        yahooSymbol = "^GSPC",
        aliases = listOf("SPX", "^GSPC", "GSPC", "S&P500", "SP500")
    ),
    DJI(
        displayName = "Dow Jones",
        chineseName = "道琼斯工业指数",
        yahooSymbol = "^DJI",
        aliases = listOf("DJI", "^DJI", "DJIA", "DOW", "DOWJONES")
    ),
    IXIC(
        displayName = "NASDAQ Composite",
        chineseName = "纳斯达克综合指数",
        yahooSymbol = "^IXIC",
        aliases = listOf("IXIC", "^IXIC", "NASDAQ", "NASDAQCOMPOSITE")
    ),
    NDX(
        displayName = "NASDAQ 100",
        chineseName = "纳斯达克100指数",
        yahooSymbol = "^NDX",
        aliases = listOf("NDX", "^NDX", "NASDAQ100", "NDX100")
    ),
    VIX(
        displayName = "VIX",
        chineseName = "VIX恐慌指数",
        yahooSymbol = "^VIX",
        aliases = listOf("VIX", "^VIX", "VOLATILITY", "FearIndex")
    ),
    RUT(
        displayName = "Russell 2000",
        chineseName = "罗素2000指数",
        yahooSymbol = "^RUT",
        aliases = listOf("RUT", "^RUT", "RUSSELL", "RUSSELL2000")
    );

    companion object {
        /**
         * 用户输入 -> 美股指数 的映射表
         */
        private val CODE_MAPPING: Map<String, USMarketIndex> by lazy {
            val map = mutableMapOf<String, USMarketIndex>()
            values().forEach { index ->
                index.aliases.forEach { alias ->
                    map[alias.uppercase()] = index
                }
            }
            map
        }

        /**
         * 判断代码是否为美股指数符号
         *
         * @param code 股票/指数代码，如 "SPX", "DJI"
         * @return True 表示是已知美股指数符号
         */
        fun isUSIndexCode(code: String): Boolean {
            return CODE_MAPPING.containsKey(code.trim().uppercase())
        }

        /**
         * 获取美股指数的 Yahoo Finance 符号
         *
         * @param code 用户输入，如 "SPX", "^GSPC", "DJI"
         * @return Yahoo Finance 符号，未找到时返回 null
         */
        fun getYahooSymbol(code: String): String? {
            return CODE_MAPPING[code.trim().uppercase()]?.yahooSymbol
        }

        /**
         * 获取美股指数的中文名称
         *
         * @param code 用户输入，如 "SPX", "DJI"
         * @return 中文名称，未找到时返回 null
         */
        fun getChineseName(code: String): String? {
            return CODE_MAPPING[code.trim().uppercase()]?.chineseName
        }

        /**
         * 获取美股指数信息
         *
         * @param code 用户输入，如 "SPX", "DJI"
         * @return 美股指数枚举，未找到时返回 null
         */
        fun fromCode(code: String): USMarketIndex? {
            return CODE_MAPPING[code.trim().uppercase()]
        }

        /**
         * 将 Yahoo Finance 符号转换为标准代码
         *
         * @param yahooSymbol Yahoo Finance 符号，如 "^GSPC"
         * @return 标准代码（如 "SPX"），未找到时返回 null
         */
        fun fromYahooSymbol(yahooSymbol: String): String? {
            return values().find { it.yahooSymbol == yahooSymbol.uppercase() }?.name
        }

        /**
         * 美股代码正则：1-5 个大写字母，可选 .X 后缀（如 BRK.B）
         * 排除美股指数代码
         */
        private val US_STOCK_PATTERN = Regex("^[A-Z]{1,5}(\\.[A-Z])?$")

        /**
         * 判断代码是否为美股股票符号（排除美股指数）
         *
         * 美股股票代码为 1-5 个大写字母，可选 .X 后缀如 BRK.B。
         * 美股指数（SPX、DJI 等）明确排除。
         *
         * @param code 股票代码，如 "AAPL", "TSLA", "BRK.B"
         * @return True 表示是美股股票符号，否则 False
         */
        fun isUSStockCode(code: String): Boolean {
            val normalized = code.trim().uppercase()
            // 美股指数不是股票
            if (isUSIndexCode(normalized)) {
                return false
            }
            return US_STOCK_PATTERN.matches(normalized)
        }

        /**
         * 将用户输入的代码转换为 Yahoo Finance 格式
         * - 美股指数：转换为对应的 ^GSPC, ^DJI 等
         * - 美股股票：直接返回大写形式
         *
         * @param code 用户输入代码
         * @return Yahoo Finance 格式的代码
         */
        fun toYahooFinanceSymbol(code: String): String {
            val normalized = code.trim().uppercase()
            // 先检查是否是指数
            return getYahooSymbol(normalized) ?: normalized
        }

        /**
         * 判断是否为有效的美股代码（股票或指数）
         *
         * @param code 代码
         * @return True 表示是有效的美股代码
         */
        fun isValidUSCode(code: String): Boolean {
            return isUSIndexCode(code) || isUSStockCode(code)
        }

        /**
         * 获取所有美股指数的 Yahoo Finance 符号列表
         */
        fun getAllIndexSymbols(): List<String> {
            return values().map { it.yahooSymbol }
        }

        /**
         * 获取主要美股指数（用于市场概览）
         */
        fun getMajorIndices(): List<USMarketIndex> {
            return listOf(SPX, DJI, IXIC)
        }
    }
}

/**
 * 美股市场类型扩展
 */
fun MarketType.isUSMarket(): Boolean = this == MarketType.US

/**
 * 代码类型判断
 */
fun String.isUSIndex(): Boolean = USMarketIndex.isUSIndexCode(this)
fun String.isUSStock(): Boolean = USMarketIndex.isUSStockCode(this)
fun String.isValidUSCode(): Boolean = USMarketIndex.isValidUSCode(this)
fun String.toYahooSymbol(): String = USMarketIndex.toYahooFinanceSymbol(this)
