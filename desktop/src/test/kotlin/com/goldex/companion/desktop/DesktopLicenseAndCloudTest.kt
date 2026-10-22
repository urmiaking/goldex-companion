package com.goldex.companion.desktop

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.license.LicenseInfo
import com.goldex.companion.data.license.LicenseStatus
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.desktop.data.DesktopMarketRepository
import com.goldex.companion.desktop.license.DesktopDeviceIdentity
import com.goldex.companion.desktop.license.DesktopLicenseRepository
import com.goldex.companion.desktop.state.DesktopWorkspace
import com.goldex.companion.desktop.sync.DesktopCloudKeys
import com.goldex.companion.desktop.sync.DesktopSyncCoordinator
import com.goldex.companion.desktop.sync.DesktopSyncStatus
import com.goldex.companion.domain.onboarding.OpeningInventoryInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopLicenseAndCloudTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace

    @Before
    fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        val market = DesktopMarketRepository(store, fetch = { error("offline test") })
        workspace = DesktopWorkspace(store, market)
    }

    @After
    fun cleanup() {
        workspace.close()
        store.close()
    }

    @Test
    fun deviceIdentityIsStableAndPersistent() {
        val dir = temporary.newFolder().toPath()
        val id1 = DesktopDeviceIdentity.get(dir, "0.56.74")
        assertTrue(id1.fingerprint.startsWith("WIN-"))
        assertEquals("0.56.74", id1.appVersion)

        val id2 = DesktopDeviceIdentity.get(dir, "0.56.74")
        assertEquals(id1.fingerprint, id2.fingerprint)
        assertTrue(Files.exists(dir.resolve("device_identity.json")))
    }

    @Test
    fun licenseRepositoryDefaultsToNoneAndSurvivesCacheReload() {
        val dir = temporary.newFolder().toPath()
        val repo = DesktopLicenseRepository(dir)
        assertEquals(LicenseStatus.NONE, repo.getCachedInfo().status)

        // Write a mock license
        val json = JSONObject().apply {
            put("status", LicenseStatus.TRIAL_ACTIVE.name)
            put("token", "tok_test_123")
            put("remainingDays", 14)
        }
        Files.writeString(dir.resolve("license.json"), json.toString())
        repo.reload()

        val reloaded = repo.getCachedInfo()
        assertEquals(LicenseStatus.TRIAL_ACTIVE, reloaded.status)
        assertEquals("tok_test_123", reloaded.token)
        assertEquals(14, reloaded.remainingDays)
    }

    @Test
    fun cloudKeysGenerateEcdsaKeyPairAndEncryptLocalSession() {
        val dir = temporary.newFolder().toPath()
        val keys = DesktopCloudKeys(dir)
        val pub = keys.publicKey()
        assertTrue(pub.isNotBlank())

        // Sign a challenge payload with ECDSA
        val challenge = "random_server_challenge_${System.currentTimeMillis()}"
        val signature = keys.sign(challenge)
        assertTrue(signature.isNotBlank())

        // Encrypt and store local cloud session
        val sessionJson = JSONObject().put("phone", "09121111111").put("token", "cloud_tok_xyz")
        keys.write(sessionJson)

        val retrieved = keys.read()
        assertNotNull(retrieved)
        assertEquals("09121111111", retrieved.getString("phone"))
        assertEquals("cloud_tok_xyz", retrieved.getString("token"))
    }

    @Test
    fun cloudCoordinatorManagesStateAndLogout() = runTest {
        val dir = temporary.newFolder().toPath()
        val licenseRepo = DesktopLicenseRepository(dir)
        val coordinator = DesktopSyncCoordinator(dir, licenseRepo)

        assertEquals(DesktopSyncStatus.DISABLED, coordinator.state.value.status)
        coordinator.setEnabled(true)
        assertEquals(DesktopSyncStatus.AUTH_REQUIRED, coordinator.state.value.status)

        // Simulate successful login
        coordinator.keys.write(JSONObject().put("phone", "09120000000"))
        coordinator.reload()
        coordinator.setEnabled(true)
        assertEquals(DesktopSyncStatus.SYNCED, coordinator.state.value.status)

        // Logout
        coordinator.logout()
        assertEquals(DesktopSyncStatus.DISABLED, coordinator.state.value.status)
        assertEquals("", coordinator.state.value.phone)
    }

    @Test
    fun workspaceClearAllLocalDataResetsDatabaseAndReturnsToWizard() = runTest {
        // Complete wizard with initial inventory
        val initialSettings = store.loadSettings().copy(galleryName = "گالری تست")
        val success = workspace.completeWizard(
            initialSettings,
            OpeningInventoryInput(vitrinWeight = "100.0", meltWeight = "50.0")
        )
        assertTrue(success)
        assertFalse(workspace.state.value.isWizardOpen)
        assertTrue(workspace.inventory.state.value.items.isNotEmpty())

        // Clear all data
        workspace.clearAllLocalData().join()

        // Verify everything was reset
        assertTrue(workspace.state.value.isWizardOpen)
        assertEquals(0, workspace.inventory.state.value.items.size)
        assertFalse(store.loadSettings().hasCompletedOnboarding)
    }

    @Test
    fun cloudPullRemoteCustomerAppearsInWorkspace() = runTest {
        val pullResult = JSONObject().apply {
            put("cursor", 1L)
            put("upperRevision", 1L)
            put("hasMore", false)
            val groups = org.json.JSONArray().apply {
                val group = JSONObject().apply {
                    put("revision", 1L)
                    val changes = org.json.JSONArray().apply {
                        val change = JSONObject().apply {
                            put("action", "create")
                            put("type", "customer")
                            put("id", "cust_remote_101")
                            put("version", 1L)
                            put("baseVersion", 0L)
                            put("fields", JSONObject().apply {
                                put("id", "cust_remote_101")
                                put("name", "حاج رضا طلافروش")
                                put("phone", "09123456789")
                                put("role", "بنکدار و همکار بازار")
                                put("goldDebtGrams", "12.345")
                                put("cashDebtTomans", "50000000")
                                put("createdAt", "1729000000000")
                                put("accountCode", "C101")
                                put("cityOrMarket", "بازار زرگرها")
                                put("lastActivityTime", "لحظاتی پیش")
                            })
                        }
                        put(change)
                    }
                    put("changes", changes)
                }
                put(group)
            }
            put("groups", groups)
        }

        // Apply remote pull into desktop SQLite
        store.database.applyPull(pullResult)

        // Trigger workspace sync reload
        workspace.reloadAfterSync()

        // Wait for customer list to be updated
        var found = false
        for (i in 0 until 50) {
            val list = workspace.customers.manager.uiState.value.customerList
            if (list.any { it.id == "cust_remote_101" }) {
                found = true
                val c = list.first { it.id == "cust_remote_101" }
                assertEquals("حاج رضا طلافروش", c.name)
                assertEquals(12.345, c.goldDebtGrams, 0.001)
                assertEquals(50_000_000L, c.cashDebtTomans)
                break
            }
            kotlinx.coroutines.delay(50)
        }
        assertTrue(found, "Remote customer should be loaded in workspace after sync")
    }

    @Test
    fun unlicensedStateLocksInvoiceAndPaymentFeatures() {
        val customer = com.goldex.companion.model.Customer(id = "c_unlicensed", name = "تست لایسنس")
        assertFalse(workspace.license.getCachedInfo().isLicensed)

        // Attempting to open invoice for customer while unlicensed
        workspace.openCustomerInvoice(customer)
        assertTrue(workspace.showLicenseDialog.value)
        assertEquals("جهت صدور فاکتور نیاز به فعال‌سازی اشتراک دارید.", workspace.state.value.notice)
        assertFalse(workspace.invoices.hasDraft)

        workspace.closeLicenseDialog()
        assertFalse(workspace.showLicenseDialog.value)

        // Attempting to open payment while unlicensed
        workspace.customers.openPayment(customer)
        assertTrue(workspace.showLicenseDialog.value)
        assertEquals("ثبت پرداخت تسویه نیازمند اشتراک معتبر است.", workspace.state.value.notice)
    }
}
