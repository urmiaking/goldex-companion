package com.goldex.companion.desktop

import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.TimeHorizon
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import java.time.Instant
import kotlin.test.*

class DesktopDashboardTest {
    private val now = Instant.parse("2026-10-06T16:00:00Z").toEpochMilli()
    private val today = """{"data":[["60,000,000","18:00:00"],["59,000,000","09:00:00"],["59,500,000","10:00:00"]]}"""
    private fun sample(horizon: TimeHorizon) = GoldHistorySnapshot(horizon, listOf(GoldHistoryPoint(now - 1000, 5_900_000, "09:00"), GoldHistoryPoint(now, 6_000_000, "10:00")), now)

    @Test fun intradayUsesRealTimesChronologicalOrderAndWholeTomans() {
        val result = DesktopGoldHistoryRepository.decode(today, TimeHorizon.TODAY, now)
        assertEquals(listOf(5_900_000L, 5_950_000L, 6_000_000L), result.points.map { it.price })
        assertEquals(100_000, result.change)
        assertEquals("09:00:00", result.points.first().label)
        assertTrue(result.points.zipWithNext().all { (a,b) -> a.at < b.at })
    }

    @Test fun summaryDatesAreOrderedAndUnitConversionMatchesIntraday() {
        val rows = """{"data":[["0","0","0","60,000,000","","","2026/10/05","1405/07/13"],["0","0","0","59,000,000","","","2026/10/04","1405/07/12"]]}"""
        val result = DesktopGoldHistoryRepository.decode(rows, TimeHorizon.ONE_WEEK, now)
        assertEquals(5_900_000, result.points.first().price)
        assertEquals(6_000_000, result.points.last().price)
    }

    @Test fun malformedNegativeFutureAndInsufficientHistoryAreUnavailable() {
        for (body in listOf("{}", today.replace("60,000,000", "bad"), today.replace("60,000,000", "-1"), today.replace("18:00:00", "23:59:59"), """{"data":[["60","09:00:00"]]}"""))
            assertFails { DesktopGoldHistoryRepository.decode(body, TimeHorizon.TODAY, now) }
    }

    @Test fun yesterdayIntradayCacheIsNeverDisplayedAsToday() {
        assertNotNull(DesktopDashboardState(history = mapOf(TimeHorizon.TODAY to sample(TimeHorizon.TODAY))).active(now))
        assertNull(DesktopDashboardState(history = mapOf(TimeHorizon.TODAY to sample(TimeHorizon.TODAY))).active(now + 86_400_000))
        assertEquals("عصر بخیر", DesktopDashboard.greeting(now))
    }

    @Test fun failedRefreshRetainsActualHistoryWithVisibleFailure() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var fail = false; var calls = 0
        val model = DesktopDashboard(DesktopGoldHistoryGateway { calls++; if (fail) error("offline"); sample(it) }, scope) { now }
        try {
            model.select(TimeHorizon.TODAY)
            withTimeout(2000) { model.state.first { !it.loading && it.active(now) != null } }
            model.select(TimeHorizon.TODAY)
            assertEquals(1, calls)
            fail = true; model.select(TimeHorizon.TODAY, force = true)
            withTimeout(2000) { model.state.first { it.error != null } }
            assertEquals(6_000_000, model.state.value.active(now)!!.points.last().price)
        } finally { scope.cancel() }
    }

    @Test fun obsoleteRequestCannotOverwriteNewHorizonOrItsFailure() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>(); val finished = CompletableDeferred<Unit>()
        val model = DesktopDashboard(DesktopGoldHistoryGateway {
            if (it == TimeHorizon.TODAY) withContext(NonCancellable) { entered.complete(Unit); release.await(); finished.complete(Unit); sample(it) }
            else error("offline")
        }, scope) { now }
        try {
            model.select(TimeHorizon.TODAY); withTimeout(2000) { entered.await() }
            model.select(TimeHorizon.ONE_WEEK)
            withTimeout(2000) { model.state.first { it.horizon == TimeHorizon.ONE_WEEK && it.error != null } }
            release.complete(Unit); withTimeout(2000) { finished.await() }
            withTimeout(2000) { scope.coroutineContext[Job]!!.children.toList().joinAll() }
            assertEquals(TimeHorizon.ONE_WEEK, model.state.value.horizon)
            assertNotNull(model.state.value.error)
            assertFalse(model.state.value.loading)
        } finally { release.complete(Unit); scope.cancel() }
    }

    @Test fun largeHistoryRenderingPreservesActualExtremaAndEndpoints() {
        val points = (0..9999).map { GoldHistoryPoint(it.toLong(), if (it == 5101) 1 else if (it == 5102) 10_000 else 500, it.toString()) }
        val snapshot = GoldHistorySnapshot(TimeHorizon.TODAY, points, now)
        assertTrue(snapshot.renderedPoints.size <= 402)
        assertEquals(points.first(), snapshot.renderedPoints.first())
        assertEquals(points.last(), snapshot.renderedPoints.last())
        assertTrue(snapshot.renderedPoints.contains(points[5101]))
        assertTrue(snapshot.renderedPoints.contains(points[5102]))
    }
}
