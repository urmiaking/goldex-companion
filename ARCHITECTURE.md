# GoldEx Companion Architecture

## 1. Purpose and architectural stance

GoldEx Companion is a production-oriented Android application implemented incrementally by vertical feature slices. It is currently a single `app` module using Jetpack Compose, Kotlin coroutines, `StateFlow`, local persistence, and HTTP market-rate integrations.

The architecture must support two truths:

1. Existing features and the Persian Sovereign Aurum UI must keep working unchanged.
2. Each new feature must be isolated enough to migrate from local-only MVP behavior to durable local storage, cloud synchronization, and production operations later.

Do not perform a broad rewrite to satisfy this document. Migrations are incremental and must preserve behavior at every step.

## 2. Current runtime architecture

```text
MainActivity
  -> GoldExCompanionTheme
  -> MainViewModel (App Shell, Market Rates & Calculator Core)
  -> MainScreen
       -> DashboardScreen (DashboardUiState)
       -> LiveRatesScreen (MarketRatesUiState)
       -> JewelryTab (JewelryUiState & JewelryActions)
       -> KaratConvertScreen (KaratConvertViewModel)
       -> CoinBubbleScreen (MarketRates & explicit callbacks)
       -> MeltCalcScreen (MeltUiState & explicit callbacks)
       -> MoreHubScreen (AppSettings & explicit callbacks)
       -> InvoicesTabPreviewCard (InvoiceManagerViewModel)
       -> Dialogs (CustomerPickerDialog, InvoiceManagerDialog, TaxProfitModal, PriceSourceModal, JewelerProfileModal, UpdateDialog)

Feature ViewModels & State Holders:
  -> CustomerManagerViewModel (CustomerStore)
  -> InvoiceManagerViewModel (InvoiceStore)
  -> PortfolioManagerViewModel (PortfolioStore)
  -> SettingsViewModel (SettingsStore)
  -> UpdateViewModel (AppUpdateChecker)
  -> KaratConvertViewModel (GoldCalculationUseCases)
  -> MainViewModel (MarketRatesStore, SettingsStore, Navigation & Calculator Core)

model/ -> domain data types, calculations, formatting, invoice aggregation
data/  -> HTTP integrations and SharedPreferences/JSON persistence via PersistenceJsonCodecs
```

### Current source of truth

- Entry point: `app/src/main/java/com/goldex/companion/MainActivity.kt`
- App shell & coordinator: `ui/main/MainViewModel.kt` & `ui/main/MainScreen.kt` (with backward compatibility bridges in `ui/calculator/`)
- Feature ViewModels: `ui/invoices/`, `ui/portfolio/`, `ui/settings/`, `ui/update/`, `ui/calculator/`
- Dashboard: `ui/dashboard/DashboardScreen.kt`
- Financial models and formatters: `model/`
- Integrations: `data/`
- Design tokens: `ui/theme/`
- Tests: `app/src/test/`
- Release workflow: `.github/workflows/build-and-release.yml`

The code is authoritative when this document and implementation disagree. Update this document when a structural decision changes.

## 3. Feature ownership model

New work must be organized around a feature owner, even while the project remains one Gradle module.

```text
ui/<feature>/       Screens, components, UI state projection
domain/<feature>/   Use cases, policies, pure business calculations
data/<feature>/     Repository implementations, local/remote data sources
```

The current `model/` and `data/` packages remain valid during migration. Do not move files merely for aesthetic consistency. Move a file when ownership is unclear, isolated testing is needed, or a feature is being extracted.

Recommended feature areas:

- `dashboard`: portfolio summary, recent activity, market overview, trend history
- `market`: live rates, source selection, fallback policy, freshness
- `calculator`: jewelry, melt, coin bubble, and karat conversion
- `invoices`: invoice lifecycle, customer association, PDF/export
- `portfolio`: holdings, valuation, profit/loss
- `settings`: user preferences and jeweler profile

## 4. State and UI contracts

The current application uses one `CalculatorUiState`. This is an accepted migration state, not a requirement that all future features share one state object.

Rules for new UI:

- Composables render immutable state and emit events.
- Composables must not call repositories or perform business calculations.
- Screen-local transient state is allowed for purely visual concerns such as selected timeframe or scroll position.
- Business state belongs to a ViewModel or feature state holder.
- Dashboard values, deltas, chart points, and recent transactions must come from state; demo values must be explicitly marked as placeholders and must not be presented as live data in production.
- Prefer feature-specific state projections over passing the entire `CalculatorUiState` to new screens.
- Navigation callbacks may remain simple lambdas until navigation becomes a real multi-screen requirement.

Target direction:

```kotlin
data class DashboardUiState(
    val portfolioSummary: PortfolioSummary?,
    val marketQuotes: List<MarketQuote>,
    val trend: List<PricePoint>,
    val recentInvoices: List<InvoiceSummary>,
    val isLoading: Boolean,
    val error: UiError?
)
```

Extract feature ViewModels incrementally. Do not duplicate state between the global ViewModel and a feature ViewModel without a defined owner.

## 5. Domain and financial correctness

Financial rules belong in pure Kotlin code with no Compose or Android dependencies. Use cases should own policies; ViewModels should translate input, invoke use cases, and publish state.

Required rules:

- Raw gold value, wage, profit, and tax remain separately represented.
- VAT is calculated only on wage plus profit according to the applicable Iranian regulation.
- Weight precision supports 0.001 grams.
- Karat and mesghal conversion constants are named and tested.
- Every financial change needs deterministic unit tests, including rounding and boundary cases.
- New production contracts must not use `Double` as an unexamined monetary representation. Prefer `Long` for whole toman amounts or an explicit money type; use `BigDecimal` only where fractional precision is required.
- Rounding is explicit at the business boundary and covered by tests.

Existing public models may continue using current types during migration. Do not change persisted or invoice-visible values without a compatibility plan.

## 6. Data and repository boundaries

UI and domain code must depend on repository contracts, not HTTP or `SharedPreferences` details.

Example target boundary:

```kotlin
interface MarketRatesRepository {
    val rates: kotlinx.coroutines.flow.Flow<MarketRates>
    suspend fun refresh(): Result<MarketRates>
}
```

Current implementations may remain:

- `HttpURLConnection` plus `org.json` for market providers
- `SharedPreferences` plus JSON for small MVP data

For every new repository:

- Define ownership of cached, pending, and failed data.
- Return structured success/error results rather than silently hiding all failures.
- Keep source-specific parsing separate from repository policy when practical.
- Ensure network connections close in `finally`.
- Make freshness and source information explicit in market data.
- Keep writes atomic from the user's perspective.

### Persistence migration

SharedPreferences JSON is acceptable for early MVP datasets. It rewrites complete collections and has no schema migration mechanism, so it must not become an invisible permanent contract.

Introduce Room or another durable local database when:

- History is large enough that collection rewrites affect UX.
- Partial queries, indexes, relationships, or migrations are required.
- Cloud sync needs stable local identifiers and change tracking.

The migration must include an import path, schema version, backup/rollback behavior, and tests against existing JSON data. No existing user data may be discarded.

## 7. Cloud-sync readiness

Cloud sync is a later feature, but local models must not prevent it.

Before introducing sync, define:

- Account and device identity
- Record ownership
- Stable IDs generated locally
- `createdAt`, `updatedAt`, and deletion/tombstone behavior
- Conflict resolution per entity
- Offline write queue and retry policy
- Authentication and token storage
- Server-authoritative versus local-authoritative fields

Do not add a generic sync layer before at least one feature has a documented conflict policy. Invoices, customers, and portfolio holdings may require different policies.

## 8. Dashboard contract

`DashboardScreen` is a presentation surface. It must not become a second business layer.

The current visual structure and Stitch design may remain while production data progressively replaces hardcoded content:

1. Live market values from market state.
2. Portfolio totals from portfolio domain calculations.
3. Recent invoices from invoice storage.
4. Trend points from a defined market history source.
5. Real deltas with an explicit comparison interval.

Hardcoded demo content must be named as demo content in code and removed or disabled for production releases.

## 9. Design system stability

The Persian Sovereign Aurum design system is a product contract.

- Reuse tokens from `ui/theme/Color.kt`, `Type.kt`, and `Theme.kt`.
- Preserve RTL at the screen boundary.
- Keep Persian number formatting centralized.
- Do not introduce screen-specific colors for existing semantic states.
- Keep reusable components under `ui/components/` unless they are truly feature-specific.
- Visual refactors must not alter calculations, persistence, or navigation behavior.

## 10. Security and release readiness

- Never commit new credentials, API tokens, passwords, or private keys.
- Keep the release keystore identity stable; signing credentials must move to protected CI secrets before public production distribution.
- Do not log customer identity, financial records, tokens, or full network payloads in production.
- Validate external data before using it in financial calculations.
- Release builds must be reproducible by `.github/workflows/build-and-release.yml`.
- Every release must have human-readable Persian notes and a verified artifact.

## 11. Testing strategy

The test pyramid is:

1. Pure domain tests for calculations, formatting, rounding, and policies.
2. Repository tests for serialization, migration, fallback, and error behavior.
3. ViewModel tests for event-to-state transitions.
4. Compose tests for critical workflows and accessibility semantics.
5. Cloud release verification for signed artifacts.

Every vertical feature ships with its smallest meaningful test slice. Do not wait for a complete database or backend before testing production behavior.

## 12. Architectural change protocol

Before changing a shared contract, an agent must:

1. Identify the current owner and all call sites.
2. State the compatibility impact.
3. Make the smallest migration step that can be tested.
4. Preserve existing persisted data and UI behavior.
5. Update this document and the relevant ADR when the boundary changes.

Use an Architecture Decision Record for decisions involving persistence, money representation, navigation, cloud sync, authentication, or module boundaries.

## 13. Completed migration slices

- Calculation policies live in `domain/calculator/GoldCalculationUseCases.kt`; the ViewModel parses UI input and publishes results without owning the formulas.
- Portfolio aggregation lives in `domain/portfolio/PortfolioValuation.kt`.
- Existing concrete repositories implement contracts in `data/RepositoryContracts.kt` (`CustomerStore`, `InvoiceStore`, `PortfolioStore`, `SettingsStore`, `MarketRatesStore`).
- `DashboardScreen` consumes `DashboardUiState` instead of the global ViewModel.
- `LiveRatesScreen` consumes `MarketRatesUiState` instead of the aggregate calculator state.
- `PortfolioTab` receives explicit mutation callbacks and items/rates, decoupled from ViewModels.
- Karat conversion owns its input and event state in `ui/calculator/KaratConvertViewModel.kt`.
- Customer, portfolio, and invoice JSON compatibility is centralized in `data/PersistenceJsonCodecs.kt` and covered by pure compatibility tests.
- Independent feature ViewModels (`CustomerManagerViewModel`, `InvoiceManagerViewModel`, `PortfolioManagerViewModel`, `SettingsViewModel`, `UpdateViewModel`, `KaratConvertViewModel`) are fully wired into the UI and coordinate directly with dialogs and preview cards.
- The root composition shell has been extracted into `MainViewModel` and `MainScreen`, dropping the primary coordinator size by ~50% (from ~810 to ~420 lines).
- Calculator and tool composables (`JewelryTab`, `CoinBubbleScreen`, `MeltCalcScreen`, `MoreHubScreen`) are decoupled from concrete ViewModels, depending only on focused UI state data classes and callback interfaces (`JewelryActions`).
- Backward compatibility typealiases (`GoldCalculatorViewModel`, `GoldCalculatorScreen`, `CalculatorUiState`) ensure zero external breakage.
- Release signing credentials are securely configured using protected GitHub Actions repository secrets.
- Phase 2 formally completed: Dashboard, Live Rates, and dedicated Stitch calculator screens (`KaratConvertScreen`, `CoinBubbleScreen`, `MeltCalcScreen`, `StandardFormulasScreen`) are fully established.
- Standardized two-action dialog button layout across all modals and dialogs (Cancel on right, Save/Confirm on left in RTL) governed by `.agents/rules/dialog-button-layout.md`.
- Weight and price-based wage inputs unified to 18sp with strict LTR decimal entry semantics under RTL layouts.

## 14. Known current compromises

- Some persistence is collection-level SharedPreferences JSON, now behind tested compatibility codecs (to be migrated to Room when dataset scale justifies).
- The market layer contains provider-specific HTTP and parsing code.
- Dashboard visual content is partly static while the feature is being migrated from Stitch designs.

These are tracked migration items, not reasons to break existing features through a broad rewrite.
