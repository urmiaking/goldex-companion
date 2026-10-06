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
