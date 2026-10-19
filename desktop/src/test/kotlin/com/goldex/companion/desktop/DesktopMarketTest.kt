package com.goldex.companion.desktop

import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.DesktopPortfolioPolicy
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class DesktopMarketTest {
    @Test fun unknownPurchasePriceIsNotPresentedAsProfitAndDatesUseTehranShamsiCalendar() {
        val item = PortfolioItem(title = "طلا", category = PortfolioCategory.GOLD, weightGrams = 2.0)
        val projection = DesktopPortfolioPolicy.project(listOf(item), DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000))
        assertNull(projection.rows.single().profit)
        assertFalse(projection.knownPurchaseBasis)
        assertEquals("۱۴۰۵/۰۷/۱۴ ۰۰:۳۰:۰۰", DesktopPortfolioPolicy.observedTime(java.time.Instant.parse("2026-10-05T21:00:00Z").toEpochMilli()))
    }
    @get:Rule val temporary = TemporaryFolder()

    @Test fun providerPricesConvertRialsAndMissingCoinsStayUnavailable() {
        val parsed = DesktopMarketRepository.parseProvider(PriceSource.TGJU, JSONObject("""{"current":{"geram18":{"p":"60,000,000"},"nim":{"p":"-123"}}}"""))
        assertEquals(6_000_000, parsed.gold18)
        assertEquals(0, parsed.coinEmami)
        assertEquals(0, parsed.coinHalf)
        assertEquals(0.0, parsed.ons)
        val coin = PortfolioItem(title = "سکه", category = PortfolioCategory.COIN)
        val projection = DesktopPortfolioPolicy.project(listOf(coin), parsed)
        assertNull(projection.rows.single().value)
        assertNull(projection.summary)
    }

    @Test fun isignalProviderParsesCoinsAndFreeCurrency() {
        val json = JSONObject("""
            {
                "data": {
                    "gold": { "data": [ {"name": "geram18", "close": 261713000}, {"name": "geram24", "close": 348948000}, {"name": "mazanne", "close": 1132280000} ] },
                    "coin": { "data": [ {"name": "sekeEmam", "close": 2667300000}, {"name": "sekeBaharAzadi", "close": 2571550000}, {"name": "nim", "close": 1405700000}, {"name": "rob", "close": 744900000}, {"name": "gerami", "close": 380000000} ] },
                    "freeCurrency": { "data": [ {"name": "usDollar", "close": 2629900} ] }
                }
            }
        """.trimIndent())
        val parsed = DesktopMarketRepository.parseProvider(PriceSource.ISIGNAL, json)
        assertEquals(26_171_300, parsed.gold18)
        assertEquals(34_894_800, parsed.gold24)
        assertEquals(113_228_000, parsed.goldMelt)
        assertEquals(266_730_000, parsed.coinEmami)
        assertEquals(257_155_000, parsed.coinBahar)
        assertEquals(140_570_000, parsed.coinHalf)
        assertEquals(74_490_000, parsed.coinQuarter)
        assertEquals(38_000_000, parsed.coinGerami)
        assertEquals(262_990, parsed.usd)
    }

    @Test fun talaProviderCalculatesUsdFromDerhamFallbackWhenDollarIsZero() {
        val json = JSONObject("""
            {
                "gold": { "gold_18k": { "v": "26,150,000" }, "gold_24k": { "v": "34,863,300" }, "gold_bazartehran": { "v": "113,277,000" }, "gold_ounce": { "v": "4486.99" } },
                "sekke": { "sekke-nim": { "v": "140,500,000" } },
                "arz": { "arz_dolar": { "v": "0" }, "arz_derham": { "v": "72,400" } }
            }
        """.trimIndent())
        val parsed = DesktopMarketRepository.parseProvider(PriceSource.TALA_IR, json)
        assertEquals(26_150_000, parsed.gold18)
        assertEquals(140_500_000, parsed.coinHalf)
        assertEquals(265_889, parsed.usd)
        assertEquals(4486.99, parsed.ons)
    }

    @Test fun fallbackReportsActualSourceAndPreservesObservationOnFailure() = runBlocking {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            store.saveSettings(AppSettings(priceSource = PriceSource.TGJU))
            var failed = false
            val calls = mutableListOf<PriceSource>()
            val repository = DesktopMarketRepository(store, { source ->
                calls += source
                if (failed || source == PriceSource.TGJU) error("synthetic failure")
                DesktopMarketRepository.emptyRates(source).copy(gold18 = 6_000_000)
            }, { 100_000L })
            repository.refreshRates()
            assertEquals(listOf(PriceSource.TGJU, PriceSource.ISIGNAL), calls)
            assertEquals(PriceSource.ISIGNAL, repository.snapshot.value!!.rates.source)
            assertTrue(repository.snapshot.value!!.isFresh(100_100))
            assertFalse(repository.snapshot.value!!.isFresh(221_000))
            failed = true
            assertFails { repository.refreshRates() }
            assertEquals(100_000, repository.snapshot.value!!.observedAt)
            assertEquals(QuoteKind.CACHED, repository.snapshot.value!!.kind)
            assertFalse(repository.snapshot.value!!.isFresh(100_100))
        }
    }

    @Test fun manualRatesSurviveRestartWithoutBecomingLive() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store -> DesktopMarketRepository(store, clock = { 200_000 }).useManual(DesktopMarketRepository.emptyRates().copy(gold18 = 7_000_000)) }
        DesktopDataStore(path).use { store ->
            val snapshot = DesktopMarketRepository(store).snapshot.value!!
            assertEquals(QuoteKind.MANUAL, snapshot.kind)
            assertFalse(snapshot.isFresh(200_000))
            assertEquals(0, snapshot.rates.coinEmami)
        }
    }

    @Test fun initialStateHasNoManufacturedMarketPrice() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val repository = DesktopMarketRepository(store)
            assertNull(repository.snapshot.value)
            assertEquals(0, repository.rates.value.gold18)
            assertFalse(repository.rates.value.isLive)
        }
    }

    @Test fun portfolioUsesEstablishedValuationAndRefusesOverflowTotals() {
        val item = PortfolioItem(title = "طلا", category = PortfolioCategory.GOLD, weightGrams = 2.0, purchasePriceTotal = 10_000_000)
        val rates = DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000)
        val result = DesktopPortfolioPolicy.project(listOf(item), rates)
        assertEquals(12_000_000, result.summary!!.currentValue)
        assertEquals(2_000_000, result.summary!!.profit)
        val huge = item.copy(weightGrams = 1_000_000.0)
        assertNull(DesktopPortfolioPolicy.project(List(2) { huge }, rates.copy(gold18 = 9_000_000_000_000)).summary)
    }
}
