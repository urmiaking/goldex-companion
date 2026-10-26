package com.goldex.companion.desktop

import com.goldex.companion.data.sync.SyncJson
import com.goldex.companion.desktop.data.db.DesktopSqliteDatabase
import com.goldex.companion.desktop.license.DesktopLicenseRepository
import com.goldex.companion.desktop.sync.*
import com.goldex.companion.model.Customer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.*

class DesktopCloudRecoveryTest {
    @get:Rule val temporary = TemporaryFolder()

    private class Cloud : DesktopCloudRepository {
        var badHash = false
        var unavailable = false
        var pagesRead = 0
        var duringPage: () -> Unit = {}
        val records = JSONArray().put(JSONObject().put("type", "customer").put("id", "remote")
            .put("version", 3).put("deleted", false)
            .put("payload", SyncJson.record(Customer(id = "remote", name = "بازیابی / حساب", createdAt = 1))))
        override suspend fun request(path: String, body: JSONObject?): JSONObject {
            if (unavailable) throw DesktopCloudException("CLOUD_UNAVAILABLE", 503)
            return when (path) {
                "cloud/bootstrap" -> JSONObject().put("workspaceId", "workspace").put("initialized", true).put("writerEpoch", 1).put("lastSequence", 7)
                "cloud/snapshot/start" -> JSONObject().put("snapshotId", "snapshot").put("revision", 12).put("pages", 1)
                "cloud/snapshot/page" -> {
                    pagesRead++
                    duringPage()
                    val hash = MessageDigest.getInstance("SHA-256").digest(SyncJson.canonical(records).toByteArray()).joinToString("") { "%02x".format(it) }
                    JSONObject().put("records", records).put("hash", if (badHash) "corrupt" else hash)
                }
                else -> error("Unexpected request: $path")
            }
        }
        override suspend fun requestCode(phone: String) = JSONObject()
        override suspend fun verifyCode(challenge: JSONObject, code: String, phone: String) = JSONObject().put("phone", phone).put("accessToken", "test")
        override suspend fun logout() {}
    }

    @Test fun loginFindsExistingAccountAndWaitsForReplacementConsent() = runBlocking {
        val directory = temporary.newFolder().toPath()
        DesktopSqliteDatabase(directory.resolve("active.db")).use { db ->
            db.addCustomer(Customer(id = "local", name = "محلی", createdAt = 1))
            val cloud = Cloud()
            val coordinator = DesktopSyncCoordinator(directory, DesktopLicenseRepository(directory), database = db, apiOverride = cloud)
            assertTrue(coordinator.verifyOtp(JSONObject(), "123456", "09120000000").isSuccess)
            assertEquals(DesktopSyncStatus.RESTORE_REQUIRED, coordinator.state.value.status)
            assertEquals(0, cloud.pagesRead)
            assertTrue(coordinator.restore(false).isFailure)
            assertEquals("local", db.getCustomers().single().id)
            assertTrue(coordinator.restore(true).isSuccess)
            assertEquals("remote", db.getCustomers().single().id)
            assertEquals(8, db.checkpoint().nextSequence)
            assertEquals(1, coordinator.state.value.restoredGeneration)
            assertTrue(db.hasSeededOpeningInventory())
            val backup = Files.list(directory.resolve("cloud-backups")).use { it.findFirst().orElseThrow() }
            DesktopSqliteDatabase(backup).use { saved ->
                assertEquals("local", saved.getCustomers().single().id)
                assertTrue(saved.outboxCount() > 0)
            }
        }
    }

    @Test fun failedHashOrConcurrentLocalEditNeverActivatesOrErasesTheQueue() = runBlocking {
        for (concurrent in listOf(false, true)) {
            val directory = temporary.newFolder().toPath()
            DesktopSqliteDatabase(directory.resolve("active.db")).use { db ->
                db.addCustomer(Customer(id = "local", name = "محلی", createdAt = 1))
                val cloud = Cloud().apply {
                    badHash = !concurrent
                    if (concurrent) duringPage = { db.addCustomer(Customer(id = "edited", name = "ویرایش تازه", createdAt = 2)) }
                }
                val coordinator = DesktopSyncCoordinator(directory, DesktopLicenseRepository(directory), database = db, apiOverride = cloud)
                coordinator.verifyOtp(JSONObject(), "123456", "09120000000")
                val result = coordinator.restore(true)
                assertTrue(result.isFailure)
                assertEquals(if (concurrent) "LOCAL_CHANGED_DURING_RESTORE" else "SNAPSHOT_HASH_MISMATCH", (result.exceptionOrNull() as DesktopCloudException).code)
                assertTrue(db.getCustomers().any { it.id == "local" })
                assertFalse(db.getCustomers().any { it.id == "remote" })
                assertTrue(db.outboxCount() > 0)
                assertFalse(db.checkpoint().initialized)
                assertEquals(0, coordinator.state.value.restoredGeneration)
                assertEquals(DesktopSyncStatus.RESTORE_REQUIRED, coordinator.state.value.status)
            }
        }
    }

    @Test fun loginDoesNotReportSuccessWhenCloudBootstrapFails() = runBlocking {
        val directory = temporary.newFolder().toPath()
        DesktopSqliteDatabase(directory.resolve("active.db")).use { db ->
            val coordinator = DesktopSyncCoordinator(directory, DesktopLicenseRepository(directory), database = db, apiOverride = Cloud().apply { unavailable = true })
            assertTrue(coordinator.verifyOtp(JSONObject(), "123456", "09120000000").isFailure)
            assertEquals(DesktopSyncStatus.ERROR, coordinator.state.value.status)
            assertEquals(0, coordinator.state.value.lastSuccessAt)
        }
    }

    @Test fun logoutKeepsLocalRecordsAndPendingOperationsForNextLogin() = runBlocking {
        val directory = temporary.newFolder().toPath()
        DesktopSqliteDatabase(directory.resolve("active.db")).use { db ->
            db.addCustomer(Customer(id = "local", name = "محلی", createdAt = 1))
            val pending = db.outboxCount()
            val coordinator = DesktopSyncCoordinator(directory, DesktopLicenseRepository(directory), database = db, apiOverride = Cloud())
            coordinator.verifyOtp(JSONObject(), "123456", "09120000000")
            coordinator.logout()
            assertEquals(pending, db.outboxCount())
            assertEquals("local", db.getCustomers().single().id)
            assertNull(coordinator.keys.read())
            assertEquals(DesktopSyncStatus.DISABLED, coordinator.state.value.status)
        }
    }
}
