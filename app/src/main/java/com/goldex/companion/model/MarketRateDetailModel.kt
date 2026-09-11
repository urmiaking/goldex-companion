package com.goldex.companion.model

import com.goldex.companion.data.MarketRates
import kotlin.math.roundToLong

/**
 * Enumeration of all rate items available on the Tehran Gold Bazaar & Coin Market Board.
 */
enum class MarketRateItemType(
    val titleFa: String,
    val shortName: String,
    val defaultPurityKarat: Karat = Karat.K18
) {
    GOLD_18K("طلای ۱۸ عیار (۷۵۰)", "۱۸ عیار", Karat.K18),
    GOLD_24K("طلای ۲۴ عیار (۹۹۹)", "۲۴ عیار", Karat.K24),
    GOLD_MELT("مظنه مثقال آبشده (۱۷ عیار)", "مثقال آبشده", Karat.K17),
    COIN_EMAMI("سکه تمام امامی (طرح جدید)", "سکه امامی"),
    COIN_BAHAR("سکه تمام بهار آزادی", "سکه بهار"),
    COIN_HALF("نیم سکه بهار آزادی", "نیم سکه"),
    COIN_QUARTER("ربع سکه بهار آزادی", "ربع سکه"),
    COIN_GERAMI("سکه گرمی بانک مرکزی", "سکه گرمی"),
    ONS("انس جهانی طلا (XAU)", "انس جهانی"),
    USD("دلار نقدی بازار آزاد", "دلار آزاد")
}

/**
 * Time horizon filter tabs for trend chart analysis.
 */
enum class TimeHorizon(val labelFa: String) {
    TODAY("امروز"),
    ONE_WEEK("۱ هفته"),
    ONE_MONTH("۱ ماه"),
    SIX_MONTHS("۶ ماه"),
    ONE_YEAR("۱ سال")
}

/**
 * Normalized 2D point for trend chart rendering.
 * [points] in normalized range [0f, 1f].
 */
data class TrendChartData(
    val points: List<Pair<Float, Float>>,
    val peakPrice: Long,
    val peakXRatio: Float,
    val peakYRatio: Float,
    val timeLabels: List<String>,
    val fluctuationRangeText: String
)

/**
 * 30-Day technical market indicators.
 */
data class MonthlyMarketStats(
    val dailyRangeText: String,
    val weeklyChangeText: String,
    val thirtyDayHigh: Long,
    val thirtyDayLow: Long,
    val weightedAverage: Long
)

/**
 * Representative or ledger-backed transaction tied to the asset.
 */
data class RateTransactionItem(
    val isBuy: Boolean,
    val title: String,
    val timeText: String,
    val specDetails: String,
    val totalPrice: Long,
    val documentNumber: String = ""
)

/**
 * Comprehensive UI state for the Rate Detail & Trend Chart screen.
 */
data class MarketRateDetailState(
    val type: MarketRateItemType,
    val title: String,
    val subtitle: String,
    val categoryBadge: String,
    val currentPrice: Long,
    val currencyUnit: String,
    val changeAmount: Long,
    val changePercent: Double,
    val isPositive: Boolean,
    val dayLow: Long,
    val dayHigh: Long,
    val openPrice: Long,
    val bubbleOrSpread: Long,
    val bubbleOrSpreadLabel: String,
    val referenceIndexText: String,
    val referenceIndexChange: String,
    val monthlyStats: MonthlyMarketStats,
    val recentTransactions: List<RateTransactionItem>,
    val chartDataByHorizon: Map<TimeHorizon, TrendChartData>
) {
    companion object {
        /**
         * Factory function to generate rich, accurate detail state for any market rate item.
         */
        fun create(type: MarketRateItemType, rates: MarketRates): MarketRateDetailState {
            return when (type) {
                MarketRateItemType.GOLD_18K -> buildGold18State(rates)
                MarketRateItemType.GOLD_24K -> buildGold24State(rates)
                MarketRateItemType.GOLD_MELT -> buildGoldMeltState(rates)
                MarketRateItemType.COIN_EMAMI -> buildCoinEmamiState(rates)
                MarketRateItemType.COIN_BAHAR -> buildCoinBaharState(rates)
                MarketRateItemType.COIN_HALF -> buildCoinHalfState(rates)
                MarketRateItemType.COIN_QUARTER -> buildCoinQuarterState(rates)
                MarketRateItemType.COIN_GERAMI -> buildCoinGeramiState(rates)
                MarketRateItemType.ONS -> buildOnsState(rates)
                MarketRateItemType.USD -> buildUsdState(rates)
            }
        }

        private fun buildGold18State(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.gold18 > 0) rates.gold18 else 4_285_000L
            val change = 32_000L
            val changePct = 0.84
            val dayLow = (basePrice * 0.9918).roundToLong()
            val dayHigh = (basePrice * 1.0023).roundToLong()
            val openPrice = (basePrice * 0.9925).roundToLong()
            val spread = -12_000L

            return MarketRateDetailState(
                type = MarketRateItemType.GOLD_18K,
                title = "طلای ۱۸ عیار (۷۵۰)",
                subtitle = "مظنه مرجع اتحادیه تهران • بازار باز است",
                categoryBadge = "نرخ رسمی",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = spread,
                bubbleOrSpreadLabel = "حباب / اسپرد",
                referenceIndexText = "شاخص پایه: انس جهانی ${PersianNumberFormatter.formatWithCommas(rates.ons.toLong())} $",
                referenceIndexChange = "+۰.۸۴٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۴۵,۰۰۰ تومان (۱.۱٪)",
                    weeklyChangeText = "+۱۰۱,۰۰۰ تومان (+۲.۴٪)",
                    thirtyDayHigh = (basePrice * 1.029).roundToLong(),
                    thirtyDayLow = (basePrice * 0.976).roundToLong(),
                    weightedAverage = (basePrice * 0.984).roundToLong()
                ),
                recentTransactions = listOf(
                    RateTransactionItem(
                        isBuy = true,
                        title = "فاکتور رسمی #۸۸۴۲",
                        timeText = "امروز ۱۵:۴۰",
                        specDetails = "۲۴.۳۵۰ گرم • بنکداری طلا",
                        totalPrice = (24.35 * basePrice).roundToLong(),
                        documentNumber = "۸۸۴۲"
                    ),
                    RateTransactionItem(
                        isBuy = false,
                        title = "سند معامله #۸۸۳۹",
                        timeText = "امروز ۱۲:۱۵",
                        specDetails = "۱۲.۱۸۰ گرم • تسویه نقدی",
                        totalPrice = (12.18 * basePrice).roundToLong(),
                        documentNumber = "۸۸۳۹"
                    )
                ),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildGold24State(rates: MarketRates): MarketRateDetailState {
            val gold18 = if (rates.gold18 > 0) rates.gold18 else 4_285_000L
            val basePrice = if (rates.gold24 > 0) rates.gold24 else (gold18 * 1000L) / 750L
            val change = 45_000L
            val changePct = 0.82
            val dayLow = (basePrice * 0.992).roundToLong()
            val dayHigh = (basePrice * 1.003).roundToLong()
            val openPrice = (basePrice * 0.993).roundToLong()
            val spread = -16_000L

            return MarketRateDetailState(
                type = MarketRateItemType.GOLD_24K,
                title = "طلای ۲۴ عیار (۹۹۹)",
                subtitle = "شمش استاندارد خلوص کامل • بازار باز است",
                categoryBadge = "شمش خالص",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = spread,
                bubbleOrSpreadLabel = "اسپرد شمش",
                referenceIndexText = "شاخص پایه: انس جهانی ${PersianNumberFormatter.formatWithCommas(rates.ons.toLong())} $",
                referenceIndexChange = "+۰.۸۲٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۶۰,۰۰۰ تومان (۱.۰٪)",
                    weeklyChangeText = "+۱۳۵,۰۰۰ تومان (+۲.۵٪)",
                    thirtyDayHigh = (basePrice * 1.031).roundToLong(),
                    thirtyDayLow = (basePrice * 0.974).roundToLong(),
                    weightedAverage = (basePrice * 0.985).roundToLong()
                ),
                recentTransactions = listOf(
                    RateTransactionItem(
                        isBuy = true,
                        title = "حواله شمش طلا #۹۱۰۲",
                        timeText = "امروز ۱۶:۲۰",
                        specDetails = "۵۰.۰۰۰ گرم • شمش سوئیسی ۹۹۹",
                        totalPrice = (50.0 * basePrice).roundToLong(),
                        documentNumber = "۹۱۰۲"
                    ),
                    RateTransactionItem(
                        isBuy = false,
                        title = "تسویه کارگاهی #۹۰۸۸",
                        timeText = "امروز ۱۱:۳۰",
                        specDetails = "۲۰.۵۰۰ گرم • تسویه آبشده کارگاهی",
                        totalPrice = (20.5 * basePrice).roundToLong(),
                        documentNumber = "۹۰۸۸"
                    )
                ),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildGoldMeltState(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.goldMelt > 0) rates.goldMelt else 18_550_000L
            val change = 140_000L
            val changePct = 0.76
            val dayLow = (basePrice * 0.993).roundToLong()
            val dayHigh = (basePrice * 1.004).roundToLong()
            val openPrice = (basePrice * 0.994).roundToLong()
            val spread = -34_000L

            return MarketRateDetailState(
                type = MarketRateItemType.GOLD_MELT,
                title = "مظنه مثقال آبشده (۱۷ عیار)",
                subtitle = "مبنای سنتی و بنکداری بازار تهران • بازار باز است",
                categoryBadge = "مظنه پایه",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = spread,
                bubbleOrSpreadLabel = "حباب مثقال",
                referenceIndexText = "نسبت تبدیل: ۴.۳۳۱۸ به هر گرم ۱۸ عیار",
                referenceIndexChange = "+۰.۷۶٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۱۹۰,۰۰۰ تومان (۱.۰٪)",
                    weeklyChangeText = "+۴۴۰,۰۰۰ تومان (+۲.۳٪)",
                    thirtyDayHigh = (basePrice * 1.030).roundToLong(),
                    thirtyDayLow = (basePrice * 0.972).roundToLong(),
                    weightedAverage = (basePrice * 0.986).roundToLong()
                ),
                recentTransactions = listOf(
                    RateTransactionItem(
                        isBuy = true,
                        title = "حواله آبشده سبزه میدان #۷۷۱۱",
                        timeText = "امروز ۱۶:۵۰",
                        specDetails = "۲ مثقال • انگ دار آزمایشگاهی",
                        totalPrice = (2.0 * basePrice).roundToLong(),
                        documentNumber = "۷۷۱۱"
                    ),
                    RateTransactionItem(
                        isBuy = false,
                        title = "سند تسویه بنکدار #۷۶۹۵",
                        timeText = "امروز ۱۳:۴۰",
                        specDetails = "۱.۵ مثقال • ری‌گیری تهران",
                        totalPrice = (1.5 * basePrice).roundToLong(),
                        documentNumber = "۷۶۹۵"
                    )
                ),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildCoinEmamiState(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.coinEmami > 0) rates.coinEmami else 49_100_000L
            val change = 580_000L
            val changePct = 1.2
            val dayLow = (basePrice * 0.990).roundToLong()
            val dayHigh = (basePrice * 1.006).roundToLong()
            val openPrice = (basePrice * 0.991).roundToLong()
            val bubble = 8_150_000L

            return MarketRateDetailState(
                type = MarketRateItemType.COIN_EMAMI,
                title = "سکه تمام امامی (طرح جدید)",
                subtitle = "وزن ۸.۱۳۳ گرم • عیار ۹۰۰ (۲۱.۶ عیار) • ضرب بانک مرکزی",
                categoryBadge = "حباب بالا",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = bubble,
                bubbleOrSpreadLabel = "حباب قیمتی",
                referenceIndexText = "ارزش ذاتی: ${PersianNumberFormatter.formatWithCommas(basePrice - bubble)} تومان (۱۶.۶٪ حباب)",
                referenceIndexChange = "+۱.۲٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۷۵۰,۰۰۰ تومان (۱.۵٪)",
                    weeklyChangeText = "+۱,۸۰۰,۰۰۰ تومان (+۳.۸٪)",
                    thirtyDayHigh = (basePrice * 1.042).roundToLong(),
                    thirtyDayLow = (basePrice * 0.965).roundToLong(),
                    weightedAverage = (basePrice * 0.982).roundToLong()
                ),
                recentTransactions = listOf(
                    RateTransactionItem(
                        isBuy = true,
                        title = "فاکتور خرید سکه #۵۵۳۰",
                        timeText = "امروز ۱۷:۱۰",
                        specDetails = "۲ قطعه سکه تمام امامی طرح ۸۶",
                        totalPrice = 2 * basePrice,
                        documentNumber = "۵۵۳۰"
                    ),
                    RateTransactionItem(
                        isBuy = false,
                        title = "فروش سکه صرافی #۵۵۱۸",
                        timeText = "امروز ۱۴:۳۰",
                        specDetails = "۱ قطعه سکه تمام با پلمپ معتبر",
                        totalPrice = basePrice,
                        documentNumber = "۵۵۱۸"
                    )
                ),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildCoinBaharState(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.coinBahar > 0) rates.coinBahar else 44_200_000L
            val change = 410_000L
            val changePct = 0.94
            val dayLow = (basePrice * 0.991).roundToLong()
            val dayHigh = (basePrice * 1.005).roundToLong()
            val openPrice = (basePrice * 0.992).roundToLong()
            val bubble = 4_850_000L

            return MarketRateDetailState(
                type = MarketRateItemType.COIN_BAHAR,
                title = "سکه تمام بهار آزادی",
                subtitle = "طرح قدیم • وزن ۸.۱۳۳ گرم • عیار ۹۰۰",
                categoryBadge = "طرح قدیم",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = bubble,
                bubbleOrSpreadLabel = "حباب قیمتی",
                referenceIndexText = "ارزش ذاتی: ${PersianNumberFormatter.formatWithCommas(basePrice - bubble)} تومان (۱۱.۰٪ حباب)",
                referenceIndexChange = "+۰.۹۴٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۵۵۰,۰۰۰ تومان (۱.۲٪)",
                    weeklyChangeText = "+۱,۲۵۰,۰۰۰ تومان (+۲.۹٪)",
                    thirtyDayHigh = (basePrice * 1.035).roundToLong(),
                    thirtyDayLow = (basePrice * 0.968).roundToLong(),
                    weightedAverage = (basePrice * 0.985).roundToLong()
                ),
                recentTransactions = listOf(
                    RateTransactionItem(
                        isBuy = true,
                        title = "خرید سکه بهار #۴۴۱۰",
                        timeText = "امروز ۱۲:۵۰",
                        specDetails = "۱ قطعه سکه تمام بهار آزادی",
                        totalPrice = basePrice,
                        documentNumber = "۴۴۱۰"
                    )
                ),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildCoinHalfState(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.coinHalf > 0) rates.coinHalf else 25_300_000L
            val change = 220_000L
            val changePct = 0.88
            val dayLow = (basePrice * 0.992).roundToLong()
            val dayHigh = (basePrice * 1.005).roundToLong()
            val openPrice = (basePrice * 0.993).roundToLong()
            val bubble = 4_350_000L

            return MarketRateDetailState(
                type = MarketRateItemType.COIN_HALF,
                title = "نیم سکه بهار آزادی",
                subtitle = "وزن ۴.۰۶۶ گرم • عیار ۹۰۰ • تقاضای متوازن",
                categoryBadge = "نیم بهار",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = bubble,
                bubbleOrSpreadLabel = "حباب قیمتی",
                referenceIndexText = "ارزش ذاتی: ${PersianNumberFormatter.formatWithCommas(basePrice - bubble)} تومان (۱۷.۲٪ حباب)",
                referenceIndexChange = "+۰.۸۸٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۳۰۰,۰۰۰ تومان (۱.۲٪)",
                    weeklyChangeText = "+۶۵۰,۰۰۰ تومان (+۲.۶٪)",
                    thirtyDayHigh = (basePrice * 1.038).roundToLong(),
                    thirtyDayLow = (basePrice * 0.965).roundToLong(),
                    weightedAverage = (basePrice * 0.984).roundToLong()
                ),
                recentTransactions = listOf(
                    RateTransactionItem(
                        isBuy = true,
                        title = "فاکتور نیم سکه #۳۳۰۵",
                        timeText = "امروز ۱۳:۱۰",
                        specDetails = "۲ قطعه نیم سکه بهار آزادی",
                        totalPrice = 2 * basePrice,
                        documentNumber = "۳۳۰۵"
                    )
                ),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildCoinQuarterState(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.coinQuarter > 0) rates.coinQuarter else 15_400_000L
            val change = 230_000L
            val changePct = 1.51
            val dayLow = (basePrice * 0.989).roundToLong()
            val dayHigh = (basePrice * 1.008).roundToLong()
            val openPrice = (basePrice * 0.990).roundToLong()
            val bubble = 4_900_000L

            return MarketRateDetailState(
                type = MarketRateItemType.COIN_QUARTER,
                title = "ربع سکه بهار آزادی",
                subtitle = "وزن ۲.۰۳۳ گرم • عیار ۹۰۰ • تقاضای خرد بالا",
                categoryBadge = "تقاضای بالا",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = bubble,
                bubbleOrSpreadLabel = "حباب قیمتی",
                referenceIndexText = "ارزش ذاتی: ${PersianNumberFormatter.formatWithCommas(basePrice - bubble)} تومان (۳۱.۸٪ حباب)",
                referenceIndexChange = "+۱.۵۱٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۲۸۰,۰۰۰ تومان (۱.۸٪)",
                    weeklyChangeText = "+۵۹۰,۰۰۰ تومان (+۴.۰٪)",
                    thirtyDayHigh = (basePrice * 1.048).roundToLong(),
                    thirtyDayLow = (basePrice * 0.958).roundToLong(),
                    weightedAverage = (basePrice * 0.978).roundToLong()
                ),
                recentTransactions = listOf(
                    RateTransactionItem(
                        isBuy = false,
                        title = "فروش ربع سکه #۲۲۰۱",
                        timeText = "امروز ۱۰:۴۵",
                        specDetails = "۱ قطعه ربع سکه بهار",
                        totalPrice = basePrice,
                        documentNumber = "۲۲۰۱"
                    )
                ),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildCoinGeramiState(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.coinGerami > 0) rates.coinGerami else 7_200_000L
            val change = 70_000L
            val changePct = 0.98
            val dayLow = (basePrice * 0.992).roundToLong()
            val dayHigh = (basePrice * 1.006).roundToLong()
            val openPrice = (basePrice * 0.993).roundToLong()
            val bubble = 2_650_000L

            return MarketRateDetailState(
                type = MarketRateItemType.COIN_GERAMI,
                title = "سکه گرمی بانک مرکزی",
                subtitle = "وزن ۱.۰۱ گرم • عیار ۹۰۰ • دارای وکیوم امنیتی",
                categoryBadge = "بانکی",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = bubble,
                bubbleOrSpreadLabel = "حباب قیمتی",
                referenceIndexText = "ارزش ذاتی: ${PersianNumberFormatter.formatWithCommas(basePrice - bubble)} تومان (۳۶.۸٪ حباب)",
                referenceIndexChange = "+۰.۹۸٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۹۵,۰۰۰ تومان (۱.۳٪)",
                    weeklyChangeText = "+۲۱۰,۰۰۰ تومان (+۳.۰٪)",
                    thirtyDayHigh = (basePrice * 1.040).roundToLong(),
                    thirtyDayLow = (basePrice * 0.962).roundToLong(),
                    weightedAverage = (basePrice * 0.980).roundToLong()
                ),
                recentTransactions = listOf(
                    RateTransactionItem(
                        isBuy = true,
                        title = "خرید سکه گرمی #۱۱۰۹",
                        timeText = "امروز ۰۹:۳۰",
                        specDetails = "۱ قطعه سکه گرمی با کارت بانک مرکزی",
                        totalPrice = basePrice,
                        documentNumber = "۱۱۰۹"
                    )
                ),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildOnsState(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.ons > 0) rates.ons.toLong() else 2_684L
            val change = 18L
            val changePct = 0.68
            val dayLow = (basePrice * 0.994).roundToLong()
            val dayHigh = (basePrice * 1.005).roundToLong()
            val openPrice = (basePrice * 0.995).roundToLong()
            val spread = 2L

            return MarketRateDetailState(
                type = MarketRateItemType.ONS,
                title = "انس جهانی طلا (XAU)",
                subtitle = "نرخ برابری هر تروا اونس (۳۱.۱۰۳۵ گرم طلا ۲۴ عیار)",
                categoryBadge = "بازار جهانی",
                currentPrice = basePrice,
                currencyUnit = "$",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = spread,
                bubbleOrSpreadLabel = "اسپرد جهانی",
                referenceIndexText = "نقره جهانی: ۳۱.۵۰ $ • شاخص دلار DXY: ۱۰۴.۲",
                referenceIndexChange = "+۰.۶۸٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۲۴ $ (۰.۹٪)",
                    weeklyChangeText = "+۵۲ $ (+۱.۹٪)",
                    thirtyDayHigh = (basePrice * 1.025).roundToLong(),
                    thirtyDayLow = (basePrice * 0.978).roundToLong(),
                    weightedAverage = (basePrice * 0.988).roundToLong()
                ),
                recentTransactions = emptyList(),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        private fun buildUsdState(rates: MarketRates): MarketRateDetailState {
            val basePrice = if (rates.usd > 0) rates.usd else 92_500L
            val change = 350L
            val changePct = 0.38
            val dayLow = (basePrice * 0.995).roundToLong()
            val dayHigh = (basePrice * 1.003).roundToLong()
            val openPrice = (basePrice * 0.996).roundToLong()
            val spread = 600L

            return MarketRateDetailState(
                type = MarketRateItemType.USD,
                title = "دلار نقدی بازار آزاد",
                subtitle = "اسکناس نقدی تهران سبزه میدان • بازار باز است",
                categoryBadge = "ارز آزاد",
                currentPrice = basePrice,
                currencyUnit = "تومان",
                changeAmount = change,
                changePercent = changePct,
                isPositive = true,
                dayLow = dayLow,
                dayHigh = dayHigh,
                openPrice = openPrice,
                bubbleOrSpread = spread,
                bubbleOrSpreadLabel = "اختلاف حواله",
                referenceIndexText = "حواله درهم دبی: ۲۵,۴۰۰ تومان • تتر: ۹۳,۱۰۰ تومان",
                referenceIndexChange = "+۰.۳۸٪ امروز",
                monthlyStats = MonthlyMarketStats(
                    dailyRangeText = "+۶۰۰ تومان (۰.۷٪)",
                    weeklyChangeText = "+۱,۲۰۰ تومان (+۱.۳٪)",
                    thirtyDayHigh = (basePrice * 1.022).roundToLong(),
                    thirtyDayLow = (basePrice * 0.982).roundToLong(),
                    weightedAverage = (basePrice * 0.990).roundToLong()
                ),
                recentTransactions = emptyList(),
                chartDataByHorizon = generateHorizonCharts(basePrice, dayHigh)
            )
        }

        /**
         * Generates normalized smooth curve points and labels for all 5 horizons.
         */
        private fun generateHorizonCharts(currentPrice: Long, peakPrice: Long): Map<TimeHorizon, TrendChartData> {
            return mapOf(
                TimeHorizon.TODAY to TrendChartData(
                    points = listOf(
                        0.00f to 0.22f,
                        0.12f to 0.20f,
                        0.25f to 0.38f,
                        0.45f to 0.55f,
                        0.65f to 0.85f, // Summit / Peak
                        0.80f to 0.32f,
                        0.90f to 0.34f,
                        1.00f to 0.48f  // Live closing point
                    ),
                    peakPrice = peakPrice,
                    peakXRatio = 0.65f,
                    peakYRatio = 0.85f,
                    timeLabels = listOf("۱۰:۰۰", "۱۲:۰۰", "۱۴:۰۰", "۱۶:۰۰", "۱۸:۰۰"),
                    fluctuationRangeText = "+۱.۱٪"
                ),
                TimeHorizon.ONE_WEEK to TrendChartData(
                    points = listOf(
                        0.00f to 0.30f,
                        0.18f to 0.25f,
                        0.35f to 0.45f,
                        0.52f to 0.88f, // Peak mid-week
                        0.70f to 0.60f,
                        0.85f to 0.50f,
                        1.00f to 0.68f
                    ),
                    peakPrice = (peakPrice * 1.008).roundToLong(),
                    peakXRatio = 0.52f,
                    peakYRatio = 0.88f,
                    timeLabels = listOf("شنبه", "دوشنبه", "چهارشنبه", "جمعه", "امروز"),
                    fluctuationRangeText = "+۲.۴٪"
                ),
                TimeHorizon.ONE_MONTH to TrendChartData(
                    points = listOf(
                        0.00f to 0.18f,
                        0.20f to 0.32f,
                        0.40f to 0.48f,
                        0.60f to 0.62f,
                        0.78f to 0.90f, // Peak late month
                        0.90f to 0.72f,
                        1.00f to 0.78f
                    ),
                    peakPrice = (peakPrice * 1.025).roundToLong(),
                    peakXRatio = 0.78f,
                    peakYRatio = 0.90f,
                    timeLabels = listOf("۴ هفته پیش", "۳ هفته پیش", "۲ هفته پیش", "۱ هفته پیش", "امروز"),
                    fluctuationRangeText = "+۴.۵٪"
                ),
                TimeHorizon.SIX_MONTHS to TrendChartData(
                    points = listOf(
                        0.00f to 0.12f,
                        0.22f to 0.28f,
                        0.42f to 0.40f,
                        0.60f to 0.58f,
                        0.75f to 0.75f,
                        0.88f to 0.92f, // Peak near end
                        1.00f to 0.86f
                    ),
                    peakPrice = (peakPrice * 1.065).roundToLong(),
                    peakXRatio = 0.88f,
                    peakYRatio = 0.92f,
                    timeLabels = listOf("۶ ماه پیش", "۴ ماه پیش", "۳ ماه پیش", "۲ ماه پیش", "امروز"),
                    fluctuationRangeText = "+۱۴.۲٪"
                ),
                TimeHorizon.ONE_YEAR to TrendChartData(
                    points = listOf(
                        0.00f to 0.08f,
                        0.25f to 0.22f,
                        0.50f to 0.45f,
                        0.70f to 0.68f,
                        0.85f to 0.94f,
                        1.00f to 0.88f
                    ),
                    peakPrice = (peakPrice * 1.150).roundToLong(),
                    peakXRatio = 0.85f,
                    peakYRatio = 0.94f,
                    timeLabels = listOf("۱ سال پیش", "۹ ماه پیش", "۶ ماه پیش", "۳ ماه پیش", "امروز"),
                    fluctuationRangeText = "+۳۸.۶٪"
                )
            )
        }
    }
}
