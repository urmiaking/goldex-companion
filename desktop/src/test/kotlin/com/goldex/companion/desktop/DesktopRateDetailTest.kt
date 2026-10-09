package com.goldex.companion.desktop

import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.TimeHorizon
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.time.*
import kotlin.test.*

class DesktopRateDetailTest {
    @get:Rule val temporary=TemporaryFolder()
    private val zone=ZoneId.of("Asia/Tehran")
    private val now=LocalDate.of(2026,10,9).atTime(16,0).atZone(zone).toInstant().toEpochMilli()
    private fun daily(instrument: BoardInstrument=BoardInstrument.GOLD18,horizon: TimeHorizon=TimeHorizon.ONE_YEAR)=RateHistorySnapshot(instrument,horizon,
        (0..240).map { i -> val at=LocalDate.of(2026,2,11).plusDays(i.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
            RateHistoryPoint(at,10_000+i*10L,9_900+i*10L,10_100+i*10L,"۱۴۰۵/۰۷/${i%30+1}") },now)
    private val intraday="""{"data":[["262,581,000","12:00:00"],["263,000,000","14:00:00"]]}"""
    @Test fun iranianRatesBecomeWholeTomanAndChronologicalPoints() {
        val h=DesktopRateHistoryRepository.decode(intraday,BoardInstrument.GOLD18,TimeHorizon.TODAY,now)
        assertEquals(26_258_100,h.points.first().price);assertEquals(26_300_000,h.high)
        assertEquals(listOf("12:00:00","14:00:00"),h.points.map { it.label })
    }
    @Test fun ounceUsesUSCentsAndDailyOHLCPreserveActualExtrema() {
        val data="""{"data":[["4,110.08","4,101","4,144.67","4,134.87","","","2026/10/08","1405/07/16"],["4,162.84","4,069.53","4,169.49","4,111.70","","","2026/10/07","1405/07/15"]]}"""
        val h=DesktopRateHistoryRepository.decode(data,BoardInstrument.OUNCE,TimeHorizon.ONE_MONTH,now)
        assertEquals(411170,h.points.first().price);assertEquals(406953,h.low);assertEquals(416949,h.high)
        assertEquals("۴,۱۳۴.۸۷",com.goldex.companion.desktop.ui.historyAmount(h.points.last().price,h.instrument))
    }
    @Test fun malformedNegativeFutureAndInsufficientDataNeverBecomeAChart() {
        listOf(intraday.replace("262,581,000","-5"),intraday.replace("14:00:00","23:59:00"),"""{"data":[["200","12:00:00"]]}""",
            intraday.replace("263,000,000","NaN"),intraday.replace("263,000,000","0")).forEach {
            assertFails { DesktopRateHistoryRepository.decode(it,BoardInstrument.GOLD18,TimeHorizon.TODAY,now) }
        }
    }
    @Test fun statsUseSimpleCloseAverageOHLCAndPersianYearBoundary() {
        val h=daily();val stats=DesktopRateStatisticsPolicy.calculate(h,now)
        val last30=h.points.filter { RateHistorySnapshot.day(it.at)>=LocalDate.of(2026,9,10) }
        assertEquals(last30.map { it.price }.average().toLong(),stats.average30)
        assertEquals(last30.maxOf { it.high },stats.high30);assertEquals(last30.minOf { it.low },stats.low30)
        val march20=h.points.last { RateHistorySnapshot.day(it.at)<=LocalDate.of(2026,3,20) }
        assertEquals(h.points.last().price-march20.price,stats.yearChange)
        assertEquals(last30.size,stats.samples30)
    }
    @Test fun partialMonthlyAndMissingPersianYearBaselineRemainUnavailable() {
        val h=daily().let { it.copy(points=it.points.takeLast(7)) }
        val s=DesktopRateStatisticsPolicy.calculate(h,now)
        assertNull(s.average30);assertNull(s.yearChange);assertNull(s.yearChangePercent)
    }
    @Test fun diskCacheSurvivesRestartRetainsUnknownFieldsAndKeepsInstrumentIsolation()=runBlocking {
        val directory=temporary.newFolder().toPath()
        val repo=DesktopRateHistoryRepository(directory,{now},{intraday})
        val h=repo.load(BoardInstrument.GOLD18,TimeHorizon.TODAY)
        val path=directory.resolve("rate-history-v1/GOLD18-TODAY.json")
        val doc=JSONObject(Files.readString(path)).put("futureField","preserved");Files.writeString(path,doc.toString())
        repo.load(BoardInstrument.GOLD18,TimeHorizon.TODAY)
        assertEquals("preserved",JSONObject(Files.readString(path)).getString("futureField"))
        val restarted=DesktopRateHistoryRepository(directory,{now},{error("offline")})
        assertEquals(h,restarted.cached(BoardInstrument.GOLD18,TimeHorizon.TODAY))
        assertNull(restarted.cached(BoardInstrument.EMAMI,TimeHorizon.TODAY))
        assertFails { restarted.load(BoardInstrument.GOLD18,TimeHorizon.TODAY) }
        assertEquals(h,restarted.cached(BoardInstrument.GOLD18,TimeHorizon.TODAY))
    }
    @Test fun corruptAndNewerCachesArePreservedWithoutOverwritingBytes()=runBlocking {
        val dir=temporary.newFolder().toPath();val path=dir.resolve("rate-history-v1/GOLD18-TODAY.json");Files.createDirectories(path.parent)
        for(text in listOf("broken-json",DesktopRateHistoryRepository.encodeCache(daily(horizon=TimeHorizon.TODAY)).put("schemaVersion",2).toString())) {
            Files.writeString(path,text)
            val repo=DesktopRateHistoryRepository(dir,{now},{intraday})
            assertFails { repo.load(BoardInstrument.GOLD18,TimeHorizon.TODAY) };assertEquals(text,Files.readString(path))
        }
    }
    @Test fun olderCacheRowsDefaultLowHighAndUnknownFieldsAreTolerated() {
        val doc=DesktopRateHistoryRepository.encodeCache(daily())
        val rows=doc.getJSONArray("points");(0 until rows.length()).forEach { rows.getJSONObject(it).remove("low");rows.getJSONObject(it).remove("high") }
        val h=DesktopRateHistoryRepository.decodeCache(doc.put("future","ignored"))
        assertEquals(h.points.first().price,h.points.first().low);assertEquals(h.points.last().price,h.points.last().high)
    }
    @Test fun lateCancelledResponseCannotReplaceAnotherInstrumentOrTimeframe()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        val started=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val model=DesktopRateDetail(object : DesktopRateHistoryGateway {
            override suspend fun load(instrument: BoardInstrument,horizon: TimeHorizon): RateHistorySnapshot {
                if(instrument==BoardInstrument.GOLD18) withContext(NonCancellable) { started.complete(Unit);release.await() }
                return daily(instrument,horizon)
            }
        },scope,{now})
        try {
            model.select(BoardInstrument.GOLD18,TimeHorizon.ONE_MONTH);started.await()
            model.select(BoardInstrument.OUNCE,TimeHorizon.ONE_YEAR)
            withTimeout(5000) { model.state.first { !it.loading && !it.statsLoading } }
            release.complete(Unit)
            scope.coroutineContext[Job]!!.children.toList().joinAll()
            assertEquals(BoardInstrument.OUNCE,model.state.value.instrument)
            assertEquals(BoardInstrument.OUNCE,model.state.value.active!!.instrument)
            assertFalse(model.state.value.histories.keys.any { it.instrument==BoardInstrument.GOLD18 })
        } finally { release.complete(Unit);scope.cancel() }
    }
    @Test fun offlineStateKeepsRealCachedChartAndOriginalTimestamp()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val h=daily(horizon=TimeHorizon.ONE_MONTH)
        val model=DesktopRateDetail(object : DesktopRateHistoryGateway {
            override fun cached(instrument: BoardInstrument,horizon: TimeHorizon)=h.takeIf { it.instrument==instrument && it.horizon==horizon }
            override suspend fun load(instrument: BoardInstrument,horizon: TimeHorizon): RateHistorySnapshot=error("offline")
        },scope,{now})
        try { model.select(BoardInstrument.GOLD18,TimeHorizon.ONE_MONTH)
            withTimeout(5000) { model.state.first { !it.loading && !it.statsLoading } }
            assertEquals(h,model.state.value.active);assertTrue(model.state.value.cached);assertNotNull(model.state.value.error)
            model.select(BoardInstrument.USD,TimeHorizon.ONE_MONTH)
            withTimeout(5000) { model.state.first { !it.loading && !it.statsLoading } }
            assertNull(model.state.value.active)
        } finally { scope.cancel() }
    }
    @Test fun nominalPremiumUsesSameQuoteExplicitRoundingAndExistingGoldBasisConversion() {
        val rates=DesktopMarketRepository.emptyRates().copy(gold18=100_000,gold24=133_333,usd=100_000,ons=31.1035)
        val quote=MarketSnapshot(rates,now,QuoteKind.ONLINE)
        assertEquals(25_000L,DesktopRateStatisticsPolicy.goldPremium(quote,BoardInstrument.GOLD18)!!.first)
        assertEquals(25_000L,DesktopRateStatisticsPolicy.goldPremium(quote,BoardInstrument.GOLD24)!!.first)
        assertEquals(100.0/3,DesktopRateStatisticsPolicy.goldPremium(quote,BoardInstrument.GOLD18)!!.second,0.0001)
        assertNull(DesktopRateStatisticsPolicy.goldPremium(quote,BoardInstrument.USD))
        assertNull(DesktopRateStatisticsPolicy.goldPremium(quote.copy(rates=rates.copy(usd=0)),BoardInstrument.GOLD18))
    }
    @Test fun mismatchedProviderResponseIsRejected(): Unit=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        val model=DesktopRateDetail(object : DesktopRateHistoryGateway {
            override suspend fun load(instrument: BoardInstrument,horizon: TimeHorizon)=daily(BoardInstrument.EMAMI,horizon)
        },scope,{now})
        try { model.select(BoardInstrument.GOLD18,TimeHorizon.ONE_YEAR)
            withTimeout(5000) { model.state.first { !it.loading && !it.statsLoading } }
            assertNull(model.state.value.active);assertNotNull(model.state.value.error)
        } finally { scope.cancel() }
    }
}
