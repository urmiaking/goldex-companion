package com.goldex.companion.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.desktop.data.DesktopMarketRepository
import com.goldex.companion.desktop.state.DesktopDestination
import com.goldex.companion.desktop.state.DesktopWorkspace
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.desktop.wizard.DesktopOnboardingWizardScreen
import com.goldex.companion.domain.onboarding.OpeningInventoryInput
import com.goldex.companion.model.Karat
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopOnboardingWizardTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace

    @Before
    fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        val market = DesktopMarketRepository(store, fetch = { error("offline test") })
        market.useManual(
            DesktopMarketRepository.emptyRates().copy(
                gold18 = 6_000_000,
                coinEmami = 60_000_000,
                goldMelt = 25_991_100
            )
        )
        workspace = DesktopWorkspace(store, market)
    }

    @After
    fun cleanup() {
        workspace.close()
        store.close()
    }

    private fun renderScreen() = compose.setContent {
        val state by workspace.state.collectAsState()
        GoldExCompanionTheme(isDarkTheme = state.dark) {
            DesktopWorkspaceScreen(workspace, {}, "0.56.70")
        }
    }

    private fun renderStandaloneWizard() = compose.setContent {
        val state by workspace.state.collectAsState()
        GoldExCompanionTheme(isDarkTheme = state.dark) {
            DesktopOnboardingWizardScreen(
                workspace = workspace,
                onClose = workspace::closeWizard
            )
        }
    }

    private fun field(tag: String) = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))

    @Test
    fun dashboardShowsWelcomeBannerWhenOnboardingIncomplete() {
        renderScreen()
        assertFalse(store.loadSettings().hasCompletedOnboarding)
        compose.onNodeWithTag("dashboard-wizard-banner").assertIsDisplayed()
        compose.onNodeWithTag("dashboard-open-wizard").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertTrue(workspace.state.value.isWizardOpen)
        }
    }

    @Test
    fun settingsShowsWizardCardAndCanLaunch() {
        renderScreen()
        compose.onNodeWithTag("nav-SETTINGS").performClick()
        compose.onNodeWithTag("settings-wizard-card").assertExists()
        compose.onNodeWithTag("run-onboarding-wizard").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(workspace.state.value.isWizardOpen)
        }
    }

    @Test
    fun skipOnboardingMarksCompletedAndCloses() {
        renderStandaloneWizard()
        compose.onNodeWithTag("wizard-skip-btn").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertTrue(store.loadSettings().hasCompletedOnboarding)
            assertFalse(workspace.state.value.isWizardOpen)
        }
    }

    @Test
    fun wizardStepNavigationAndProfileValidation() {
        renderStandaloneWizard()
        // Step 1: Intro
        compose.onNodeWithTag("wizard-step-header").assertIsDisplayed()
        compose.onNodeWithTag("wizard-intro-pane").assertIsDisplayed()

        // Move to Step 2: Profile
        compose.onNodeWithTag("wizard-next-btn").performClick()
        compose.onNodeWithTag("wizard-profile-pane").assertIsDisplayed()

        // Try to proceed with empty profile -> Should fail validation
        compose.onNodeWithTag("wizard-next-btn").performClick()
        compose.onNodeWithTag("wizard-profile-pane").assertIsDisplayed() // Remains on profile
        compose.onNodeWithText("وارد کردن نام تجاری طلافروشی / بنکداری الزامی است.").assertIsDisplayed()

        // Fill in required profile fields
        field("profile-gallery-name").performTextReplacement("طلا و جواهری زمرد")
        field("profile-manager-name").performTextReplacement("حاج محمد زرگر")
        field("profile-union-code").performTextReplacement("۱۲۳۴۵")
        field("profile-phone").performTextReplacement("۰۹۱۲۱۱۱۱۱۱۱")
        field("profile-address").performTextReplacement("بازار بزرگ تهران، سرای زرگرها، پلاک ۱")

        // Move to Step 3: Financial Defaults
        compose.onNodeWithTag("wizard-next-btn").performClick()
        compose.onNodeWithTag("wizard-financial-pane").assertIsDisplayed()

        // Move to Step 4: Inventory
        compose.onNodeWithTag("wizard-next-btn").performClick()
        compose.onNodeWithTag("wizard-inventory-pane").assertIsDisplayed()

        // Fill in inventory values
        field("inventory-showcase-weight").performTextReplacement("۲۵۰٫۵۰۰")
        field("inventory-melt-weight").performTextReplacement("۱۰۰٫۰۰۰")
        field("inventory-cash-balance").performTextReplacement("۵۰۰۰۰۰۰۰۰")

        // Move to Step 5: Completion
        compose.onNodeWithTag("wizard-next-btn").performClick()
        compose.onNodeWithTag("wizard-completion-pane").assertIsDisplayed()

        // Assert review summary rows contain entered information
        compose.onNodeWithText("طلا و جواهری زمرد").assertIsDisplayed()
        compose.onNodeWithText("حاج محمد زرگر").assertIsDisplayed()

        // Complete onboarding
        compose.onNodeWithTag("wizard-finish-btn").performClick()

        compose.runOnIdle {
            val settings = store.loadSettings()
            assertTrue(settings.hasCompletedOnboarding)
            assertEquals("طلا و جواهری زمرد", settings.galleryName)
            assertEquals("حاج محمد زرگر", settings.managerName)
            assertEquals("۱۲۳۴۵", settings.unionCode)
            assertEquals("۰۹۱۲۱۱۱۱۱۱۱", settings.galleryPhone)
            assertEquals("بازار بزرگ تهران، سرای زرگرها، پلاک ۱", settings.galleryAddress)
            assertFalse(workspace.state.value.isWizardOpen)

            // Verify inventory and portfolio items were seeded
            val inventoryItems = workspace.inventory.state.value.items
            assertTrue(inventoryItems.isNotEmpty(), "Inventory items should be seeded upon onboarding completion")
            assertTrue(store.hasSeededOpeningInventory(), "Opening inventory checkpoint should be marked as seeded")
        }
    }

    @Test
    fun completingOnboardingTwiceDoesNotDuplicateInventory() {
        val input = OpeningInventoryInput(
            vitrinWeight = "100.0",
            meltWeight = "50.0",
            cashTankhah = "100000000"
        )
        val initialSettings = store.loadSettings().copy(
            galleryName = "گالری تست"
        )

        // First completion
        val firstSuccess = workspace.completeWizard(initialSettings, input)
        assertTrue(firstSuccess)
        val countAfterFirst = workspace.inventory.state.value.items.size
        assertTrue(countAfterFirst > 0)

        // Second completion (e.g. re-run from settings)
        val secondSuccess = workspace.completeWizard(initialSettings, input)
        assertTrue(secondSuccess)
        val countAfterSecond = workspace.inventory.state.value.items.size

        // Invariant: Opening inventory must never duplicate on subsequent runs
        assertEquals(countAfterFirst, countAfterSecond, "Opening inventory items must not duplicate when wizard re-runs")
    }
}
