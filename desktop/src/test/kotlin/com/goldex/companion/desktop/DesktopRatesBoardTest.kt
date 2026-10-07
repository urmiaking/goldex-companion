package com.goldex.companion.desktop

import com.goldex.companion.data.PriceSource
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.CoinType
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import kotlin.test.*

class DesktopRatesBoardTest {
    @get:Rule val temporary = TemporaryFolder()
    private val now = System.currentTimeMillis()
    private val current = """{"current":{"geram18":{"p":"60,000,000","l":"59,000,000","h":"61,000,000","d":"1,000,000","dp":1.67,"dt":"high"},"ons":{"p":"2,650.80","l":"2,640.40","h":"2,660.90","d":"5.40","dp":0.2,"dt":"low"},"price_aed":{"p":"250,000","l":"249,000","h":"251,000","d":"1,000","dp":0.4,"dt":"high"}}}"""
    private val history = """{"data":[["60,000,000","10:30:00"],["59,000,000","09:00:00"]]}"""

    @Test fun providerParsingPreservesTomanAndOuncePrecisionWithoutSyntheticRanges() {
        val quotes = DesktopRatesBoardRepository.decodeCurrent(current)
        assertEquals(6_000_000.0,quotes[BoardInstrument.GOLD18]!!.price)
        assertEquals(100_000.0,quotes[BoardInstrument.GOLD18]!!.change)
        assertEquals(2650.8,quotes[BoardInstrument.OUNCE]!!.price)
        assertEquals(-5.4,quotes[BoardInstrument.OUNCE]!!.change)
        assertEquals(25_000.0,quotes[BoardInstrument.AED]!!.price)
        val invalid = DesktopRatesBoardRepository.decodeCurrent("""{"current":{"geram18":{"p":"NaN"},"geram24":{"p":"30,000,000","l":"40,000,000","h":"20,000,000","d":"10","dp":1}}}""")
        assertFalse(BoardInstrument.GOLD18 in invalid)
        assertNull(invalid[BoardInstrument.GOLD24]!!.low)
        assertNull(invalid[BoardInstrument.GOLD24]!!.change)
        assertEquals(listOf(5_900_000.0,6_000_000.0),DesktopRatesBoardRepository.decodeHistory(history,BoardInstrument.GOLD18))
        assertFails { DesktopRatesBoardRepository.decodeHistory("""{"data":[["0","09:00:00"],["10","10:00:00"]]}""",BoardInstrument.GOLD18) }
    }

    @Test fun cacheReopensOfflineAndPreservesUnknownFieldsAndMalformedFile() = runBlocking {
        val dir = temporary.newFolder().toPath()
        val cache = dir.resolve("rates-board-v1.json")
        val old = DesktopRatesBoardRepository.encodeCache(BoardSnapshot(now,emptyMap()))
            .put("futureField","retained")
        old.getJSONObject("quotes").put("FUTURE_INSTRUMENT",JSONObject().put("unrecognized",42))
        old.getJSONObject("quotes").put("TETHER",JSONObject().put("price",93_150))
        Files.writeString(cache,old.toString())
        val repository = DesktopRatesBoardRepository(dir,{ now }) { url -> if (url.contains("ajax")) current else history }
        repository.cached()
        val result = repository.load(null)
        assertTrue(result.history.isNotEmpty())
        val reopened = DesktopRatesBoardRepository(dir,{ now }) { error("offline") }
        assertEquals(result,reopened.cached())
        val saved = JSONObject(Files.readString(cache))
        assertEquals("retained",saved.getString("futureField"))
        assertEquals(42,saved.getJSONObject("quotes").getJSONObject("FUTURE_INSTRUMENT").getInt("unrecognized"))
        assertFalse(saved.getJSONObject("quotes").has("TETHER"))
        Files.writeString(cache,"bad cache")
        val corrupt = DesktopRatesBoardRepository(dir,{ now }) { current }
        assertFails { corrupt.cached() }
        assertEquals("bad cache",Files.readString(cache))
    }

    @Test fun manualOrDifferentProviderNeverGetsUnrelatedDailyChangeAndHistory() {
        val quotes = DesktopRatesBoardRepository.decodeCurrent(current)
        val board = DesktopRatesBoardState(BoardSnapshot(now,quotes,mapOf(BoardInstrument.GOLD18 to BoardHistory(listOf(5_900_000.0,6_000_000.0),now))))
        val rates = DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000,coinEmami = 60_000_000,usd = 100_000,ons = 2650.8)
        val manual = MarketSnapshot(rates,now,QuoteKind.MANUAL)
        val row = DesktopRatesBoardPolicy.row(BoardInstrument.GOLD18,manual,board,now)
        assertEquals(6_000_000.0,row.value); assertNull(row.daily); assertTrue(row.history.isEmpty())
        assertNull(DesktopRatesBoardPolicy.row(BoardInstrument.GOLD18,manual.copy(kind = QuoteKind.ONLINE,rates = rates.copy(source = PriceSource.TALA_IR)),board,now).daily)
        assertNotNull(DesktopRatesBoardPolicy.row(BoardInstrument.GOLD18,manual.copy(kind = QuoteKind.ONLINE),board,now).daily)
        assertNull(DesktopRatesBoardPolicy.row(BoardInstrument.GOLD18,manual.copy(kind = QuoteKind.ONLINE),board,now+86_400_000).daily)
        assertNull(DesktopRatesBoardPolicy.row(BoardInstrument.GOLD24,manual,board,now).value)
        val coin = DesktopRatesBoardPolicy.row(BoardInstrument.EMAMI,manual,board,now)
        assertEquals(GoldCalculationUseCases.calculateCoinBubble(CoinType.EMAMI,60_000_000.0,100_000,2650.8),coin.bubble)
        assertNull(DesktopRatesBoardPolicy.row(BoardInstrument.EMAMI,manual.copy(rates = rates.copy(usd = 0)),board,now).bubble)
    }

    @Test fun failedRefreshRetainsOriginalCacheTimeAndRejectsConcurrentRequests() = runBlocking {
        val cached = BoardSnapshot(now-10_000,DesktopRatesBoardRepository.decodeCurrent(current))
        var calls = 0
        val ready = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob()+Dispatchers.Default)
        val board = DesktopRatesBoard(object : DesktopRatesBoardGateway {
            override fun cached() = cached
            override suspend fun load(previous: BoardSnapshot?): BoardSnapshot { calls++; ready.complete(Unit); finish.await(); error("offline") }
        },scope,{ now })
        try {
            board.refresh(); ready.await(); board.refresh(true)
            assertEquals(1,calls)
            finish.complete(Unit)
            withTimeout(5000) { while (board.state.value.loading) delay(10) }
            assertEquals(cached,board.state.value.snapshot)
            assertTrue(board.state.value.cached); assertNotNull(board.state.value.error)
        } finally { scope.cancel() }
        Unit
    }
}
