# GoldEx Companion — Agent Orchestration Guide (`agents.md`)

> **Authoritative Single Source of Truth** for AI agents (implementer, QA, specialist, forensic auditor) and human engineers collaborating on GoldEx Companion.  
> Every agent MUST read this document thoroughly before inspecting, modifying, or creating code in this repository.

---

## 1. Core Invariants (The 6 Non-Negotiable Rules)

All development, refactoring, and maintenance must strictly adhere to these six inviolable rules. Any violation will trigger immediate rejection by the Forensic Auditor.

### Invariant 1: NO LOCAL GRADLE BUILDS
- **Rule**: NEVER execute `./gradlew assembleRelease`, `./gradlew build`, `./gradlew test`, or background daemon builds on the user's host PC.
- **Rationale**: The user's local host environment has strict CPU/RAM limits and cannot run Gradle daemons reliably without severe host system degradation.
- **Enforcement**: All compilation, bytecode verification, APK assembly, and cryptographic signing MUST execute exclusively in the cloud via **GitHub Actions CI/CD** on `ubuntu-latest` runners.

### Invariant 2: PERSISTENT 2048-BIT RSA SIGNING KEYSTORE
- **Rule**: NEVER delete, overwrite, or regenerate `app/keystore/goldex-release.keystore`.
- **Specifications**:
  - **File Path**: `app/keystore/goldex-release.keystore`
  - **Key Algorithm**: RSA 2048-bit
  - **Key Alias**: `goldex`
  - **Store Password**: `goldexcompanion`
  - **Key Password**: `goldexcompanion`
- **Rationale**: Android packages require continuous signature identity. Changing or generating a new keystore breaks cryptographic continuity and causes Android OS to reject updates with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, permanently stranding users on older builds.

### Invariant 3: ZERO-DEPENDENCY LEAN NETWORK STACK
- **Rule**: NEVER add third-party networking or DI frameworks (e.g., `OkHttp`, `Retrofit`, `Ktor`, `Volley`, `Dagger`, `Hilt`, `Room`) to `app/build.gradle.kts`.
- **Implementation**:
  - Use standard JDK `java.net.HttpURLConnection` wrapped in Kotlin Coroutines with `Dispatchers.IO`.
  - Parse payloads using Android SDK `org.json.JSONObject` and `org.json.JSONArray`.
  - Always set required headers:
    ```kotlin
    setRequestProperty("User-Agent", "GoldExCompanion-Android")
    setRequestProperty("Accept", "application/vnd.github.v3+json")
    ```
  - Always close network sockets in a `finally` block: `finally { connection?.disconnect() }`.
- **Rationale**: Keeps the application lightweight (< 5MB), avoids dependency resolution conflicts in cloud runners, and adheres to zero-bloat principles.

### Invariant 4: UNIVERSAL PERSIAN TYPOGRAPHY & RTL
- **Rule**: All user-facing strings, numbers, currencies, and layouts must be fully localized for the Iranian market.
- **Implementation**:
  - **Typography**: The app uses **Vazirmatn** loaded in `Type.kt`. The OpenType feature `ss01` (`fontFeatureSettings = "ss01"`) is enabled across all typography styles so ASCII digits are rendered natively as authentic Persian numerals (۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹).
  - **Formatting**: Format currency and weights using `PersianNumberFormatter` (with thousand-separators) and `PersianWordsFormatter` for monetary sums in Persian words (حروف).
  - **Layout**: Root composables must be wrapped in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.

### Invariant 5: IRANIAN GOLD MARKET ACCREDITATION & CALCULATION LOGIC
- **Rule**: All financial calculations must strictly comply with official Iranian Gold and Jewelry Union regulations.
- **Accredited Constants & Standards**:
  - **Standard Benchmark**: 18 Karat raw gold per gram (عیار ۷۵۰ / Purity 0.750).
  - **Mesghal (مظنه آبشده)**: 1 Mesghal = 4.608 grams of 17 Karat gold (عیار ۷۰۵ / Purity 0.705). Conversion factor to 18K: `1 Mesghal 17K = 4.33185 grams 18K`.
  - **Legal VAT Exemption (معافیت اصل طلا)**: Pursuant to Iranian national tax law, Value-Added Tax (9% مالیات بر ارزش افزوده) applies **strictly to workshop wage (اجرت ساخت) and dealer profit (سود فروشنده)**. Raw gold bullion value (اصل طلا) is 100% tax-exempt.
  - **Formula**:
    $$\text{RawGoldValue} = \text{NetWeight} \times \text{Spot18k} \times \frac{\text{Karat}}{750}$$
    $$\text{WageAmount} = \begin{cases} \text{RawGoldValue} \times \frac{\text{Wage\%}}{100} & (\text{Percentage}) \\ \text{NetWeight} \times \text{WagePerGram} & (\text{Toman/Gram}) \end{cases}$$
    $$\text{ProfitAmount} = (\text{RawGoldValue} + \text{WageAmount}) \times \frac{\text{Profit\%}}{100}$$
    $$\text{TaxAmount} = (\text{WageAmount} + \text{ProfitAmount}) \times \frac{9}{100}$$
    $$\text{TotalPayable} = \text{RawGoldValue} + \text{WageAmount} + \text{ProfitAmount} + \text{TaxAmount}$$
  - **Weight Precision**: Gold scales in Iranian bazaars measure to milligram accuracy; inputs and calculation states must support 3 decimal places (0.001g).

### Invariant 6: GOOGLE STITCH "PERSIAN SOVEREIGN AURUM" DESIGN SYSTEM
- **Rule**: All UI components must adhere strictly to the Stitch luxury design system defined in `Color.kt`, `Theme.kt`, and `Type.kt`.
- **Core Design Tokens**:
  - **Canvas (Background)**: `#F6F8FA` (Light porcelain) / `#0E121B` (Dark obsidian)
  - **Surface (Card & Container)**: `#FFFFFF` (Light alabaster) / `#161C28` (Dark slate)
  - **Primary Gold Accent (Champagne)**: `#D4AF37`
  - **Deep Bullion Gold**: `#B8860B` (`goldBullion`)
  - **Text Primary (Obsidian)**: `#141B2B` (Light) / `#F1F5F9` (Dark)
  - **Market Bull / Positive**: `#10B981` (Emerald)
  - **Market Bear / Negative**: `#EF4444` (Ruby)
  - **Hairline Specular Borders**: `0.6.dp` stroke with gold or translucent tint
  - **Glassmorphism**: Translucent frosted containers (`background(surface.copy(alpha = 0.85f))`) with specular edge borders and soft elevation shadows.

### Invariant 7: STREAMING CI RUNNER MONITORING
- **Rule**: NEVER use arbitrary polling timers or sleep loops when waiting for GitHub Actions builds.
- **Enforcement**: Always use native streaming watch commands such as `gh run watch <run-id>` or `gh run view <run-id> --watch`.

---

## 2. Reconciled System Architecture & Directory Layout

### 2.1 System Architecture Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          EXTERNAL DATA SOURCES                              │
│  ┌───────────────────────────────┐     ┌─────────────────────────────────┐  │
│  │     GitHub Releases API       │     │   Live Iranian Gold & FX Feeds  │  │
│  │ (api.github.com/repos/...     │     │  (TGJU / Tala.ir / ISignal)     │  │
│  │  /releases/latest)            │     │  (java.net.HttpURLConnection)   │  │
│  └───────────────┬───────────────┘     └────────────────┬────────────────┘  │
└──────────────────┼──────────────────────────────────────┼───────────────────┘
                   │                                      │
                   ▼                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               DATA LAYER                                    │
│  ┌───────────────────────────────┐     ┌─────────────────────────────────┐  │
│  │      AppUpdateChecker         │     │      GoldMarketRepository       │  │
│  │ (Parse tag, notes, APK url)   │     │ (Live rates, sources, caching)  │  │
│  └───────────────┬───────────────┘     └────────────────┬────────────────┘  │
│                  │                                      │                   │
│  ┌───────────────┴───────────────┐     ┌────────────────┴────────────────┐  │
│  │      CustomerRepository       │     │      PortfolioRepository        │  │
│  │ (SharedPreferences JSON)      │     │ (SharedPreferences JSON)        │  │
│  └───────────────┬───────────────┘     └────────────────┬────────────────┘  │
└──────────────────┼──────────────────────────────────────┼───────────────────┘
                   │                                      │
                   ▼                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER: UNIDIRECTIONAL DATA FLOW              │
│                                                                             │
│                     ┌────────────────────────────────┐                      │
│                     │    GoldCalculatorViewModel     │                      │
│                     │      (AndroidViewModel)        │                      │
│                     └───────────────┬────────────────┘                      │
│                                     │                                       │
│                        uiState: StateFlow<CalculatorUiState>                │
│                                     │                                       │
│                                     ▼                                       │
│                     ┌────────────────────────────────┐                      │
│                     │      GoldCalculatorScreen      │                      │
│                     └───────┬───────────────┬────────┘                      │
│                             │               │                               │
│            ┌────────────────┴──────┐ ┌──────┴────────────────┐              │
│            │  LuxuryDrawer (RTL)   │ │  GlassmorphicDock     │              │
│            │  - Customers Hub      │ │  - Spring Sliding     │              │
│            │  - App Settings       │ │    Indicator Pill     │              │
│            │  - Update & About     │ │  - Tab Micro-Bounces  │              │
│            └───────────────────────┘ └───────────────────────┘              │
│                             │                                               │
│            ┌────────────────┴────────────────────────────────┐              │
│            │    5 Swappable Screen Tabs (AnimatedContent)    │              │
│            │  1. JewelryTab (محاسبه طلا و فاکتور)            │              │
│            │  2. MeltTab (مظنه آبشده و تبدیل مثقال)          │              │
│            │  3. CoinBubbleTab (حباب سکه‌های بانکی)           │              │
│            │  4. KaratConvertTab (تبدیل تخصصی عیار)          │              │
│            │  5. PortfolioTab (مدیریت سبد سرمایه‌گذاری)      │              │
│            └─────────────────────────────────────────────────┘              │
│                             │                                               │
│            ┌────────────────┴────────────────────────────────┐              │
│            │        Reusable Composables (ui/components/)    │              │
│            │  LuxuryCard | GoldButton | SectionHeader        │              │
│            │  KaratBadge | PresetPill | UpdateDialog         │              │
│            │  AnimatedPriceTicker | GoldInputField           │              │
│            └─────────────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Complete Repository Layout

```
C:\Users\Masoud\.gemini\antigravity\scratch\goldex-companion\
├── .github/
│   └── workflows/
│       └── build-and-release.yml        # Cloud CI/CD pipeline: build, sign, publish APK & releases
├── app/
│   ├── keystore/
│   │   └── goldex-release.keystore      # 2048-bit RSA release signing keystore (CRITICAL: NEVER DELETE)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml      # App permissions (INTERNET) and Application declarations
│   │   │   ├── java/com/goldex/companion/
│   │   │   │   ├── MainActivity.kt      # Edge-to-edge root activity hosting theme & root screen
│   │   │   │   ├── data/                # Data Layer (Network, Persistence, Domain Models)
│   │   │   │   │   ├── AppUpdateChecker.kt     # GitHub release checker, SemVer comparator, APK asset extractor
│   │   │   │   │   ├── CustomerRepository.kt   # Local customer directory backed by SharedPreferences JSON
│   │   │   │   │   ├── GoldMarketRepository.kt # Live rate fetcher (TGJU, Tala.ir, ISignal) via HttpURLConnection
│   │   │   │   │   ├── MarketRates.kt          # Immutable data models for live market rates & price sources
│   │   │   │   │   └── PortfolioRepository.kt  # User holding portfolio persistence & PnL calculation
│   │   │   │   ├── model/               # Pure Business Logic (100% Android-free, unit-testable)
│   │   │   │   │   ├── Customer.kt             # Customer entity (id, name, phone, nationalId, note)
│   │   │   │   │   ├── GoldCalculation.kt      # Mathematical formulas: Jewelry, Melt, Coin Bubble, Karat Convert
│   │   │   │   │   ├── Invoice.kt              # Invoice item, invoice summary, VAT, and aggregate totals
│   │   │   │   │   └── PersianWordsFormatter.kt# Numerical amount to Persian written words converter (حروف)
│   │   │   │   └── ui/                  # Presentation Layer (Jetpack Compose Material 3)
│   │   │   │       ├── calculator/
│   │   │   │       │   ├── GoldCalculatorScreen.kt    # Root Scaffold, Drawer wrapper, TopBar, and tab switcher
│   │   │   │       │   ├── GoldCalculatorViewModel.kt # Central AndroidViewModel with StateFlow<CalculatorUiState>
│   │   │   │       │   └── tabs/
│   │   │   │       │       ├── JewelryTab.kt          # طلا و جواهر: Weight, Wage, Profit, Tax, Multi-item cart
│   │   │   │       │       ├── MeltTab.kt             # مظنه آبشده: Mesghal-to-gram conversion, lot calculation
│   │   │   │       │       ├── CoinBubbleTab.kt       # حباب سکه: Emami, Bahar, Half, Quarter, Gerami
│   │   │   │       │       ├── KaratConvertTab.kt     # تبدیل عیار: 750, 875, 900, 999 karat equivalence
│   │   │   │       │       └── PortfolioTab.kt        # سبد دارایی: Asset holdings, current value, total profit
│   │   │   │       ├── components/      # Modular, Reusable UI Components
│   │   │   │       │   ├── AnimatedPriceTicker.kt     # Spring-animated sliding fintech number transition
│   │   │   │       │   ├── CustomerDialogs.kt         # Modal dialogs for adding, editing, and managing customers
│   │   │   │       │   ├── GlassmorphicDock.kt        # iOS 26 frosted glass floating dock with sliding spring pill
│   │   │   │       │   ├── GoldButton.kt              # Sovereign Aurum gradient primary & surface secondary buttons
│   │   │   │       │   ├── GoldInputField.kt          # Persian digit input field with thousand separators
│   │   │   │       │   ├── KaratBadge.kt              # Standardized 18K (۷۵۰), 21K (۸۷۵), 24K (۹۹۹) badges
│   │   │   │       │   ├── LiveRatesTicker.kt         # Horizontal auto-scrolling live market ticker pills
│   │   │   │       │   ├── LuxuryCard.kt              # Elevated card with hairline border & subtle gradient header
│   │   │   │       │   ├── LuxuryDrawer.kt            # Persian RTL ModalNavigationDrawer sheet content
│   │   │   │       │   ├── PresetPill.kt              # Quick preset chips for weight, profit, and tax percentages
│   │   │   │       │   ├── ResultRow.kt               # Key-value financial row with copy-to-clipboard action
│   │   │   │       │   ├── SectionHeader.kt           # Standardized icon container with Persian title & subtitle
│   │   │   │       │   └── UpdateDialog.kt            # Modal dialog prompting in-app GitHub update installation
│   │   │   │       ├── theme/           # Design System & Theme Tokens
│   │   │   │       │   ├── Color.kt                   # Stitch Sovereign Aurum palettes, gradients, and hairline tokens
│   │   │   │       │   ├── Theme.kt                   # MaterialTheme provider and custom LocalGoldExAppColors
│   │   │   │       │   └── Type.kt                    # Vazirmatn typography with OpenType ss01 Persian numerals
│   │   │   │       └── util/            # Utilities
│   │   │   │           ├── PdfInvoiceGenerator.kt     # A4 Iranian Gold Union standard PDF invoice generator
│   │   │   │           └── ThousandsSeparatorVisualTransformation.kt
│   │   │   └── res/
│   │   │       └── font/
│   │   │           └── vazirmatn_*.ttf                # Vazirmatn font family assets
│   │   └── test/java/com/goldex/companion/
│   │       ├── GoldCalculationTest.kt   # Unit tests for jewelry math, melt conversion, bubble, and karat
│   │       └── InvoiceTest.kt           # Unit tests for multi-item invoice aggregation and tax laws
│   ├── build.gradle.kts                 # App build config (versionCode, versionName, signingConfigs)
│   └── proguard-rules.pro               # ProGuard / R8 rules
├── agents.md                            # Authoritative orchestration guide (THIS FILE)
├── ARCHITECTURE.md                      # Architectural overview document
├── ORIGINAL_REQUEST.md                  # User project charter and requirements
├── README.md                            # Public repository overview
├── build.gradle.kts                     # Root build configuration
├── gradle.properties                    # JVM arguments & AndroidX flags
└── settings.gradle.kts                  # Module includes
```

---

## 3. Module Responsibilities & Component Inventory

### 3.1 Module Breakdown

| Module / Package | Primary Responsibility | Dependencies | Threading / Execution Model |
|---|---|---|---|
| `com.goldex.companion.model` | Pure business rules, Iranian gold Union formulas, tax exemptions, invoice calculations. | None (Pure Kotlin standard library) | Synchronous / CPU-bound |
| `com.goldex.companion.data` | Network I/O to GitHub API & live market scraping, SharedPreferences JSON persistence. | Android SDK (`Context`, `SharedPreferences`, `HttpURLConnection`, `org.json`) | Asynchronous (`Dispatchers.IO` via Coroutines) |
| `com.goldex.companion.ui.calculator` | State orchestration, screen rendering, tab swapping, drawer integration. | `AndroidViewModel`, Compose Foundation, Material 3, `StateFlow` | Main Thread (`Dispatchers.Main`) |
| `com.goldex.companion.ui.components` | Reusable atomic and molecular UI composables adhering to Stitch design system. | Compose UI, Material 3, Custom design tokens | UI thread rendering |
| `com.goldex.companion.ui.theme` | Color palettes, typographic definitions, theme providers, hairline strokes. | Compose Material 3 | Static token composition |
| `com.goldex.companion.ui.util` | Text transformations, Persian number formatting, Android PDF printing. | Android SDK (`android.graphics.pdf`, Compose UI) | Main & IO |

### 3.2 Reusable Component Inventory (`ui/components/`)

| Component | Path | Purpose | Key Parameters |
|---|---|---|---|
| `LuxuryCard` | `ui/components/LuxuryCard.kt` | Uniform card with alabaster surface, `0.6.dp` gold hairline border, and subtle shadow. | `modifier: Modifier`, `shape: Shape`, `border: BorderStroke?`, `content: @Composable () -> Unit` |
| `GoldButton` | `ui/components/GoldButton.kt` | Luxury styled primary button with gold gradient or secondary surface button. | `text: String`, `onClick: () -> Unit`, `isPrimary: Boolean`, `leadingIcon: ImageVector?`, `enabled: Boolean` |
| `SectionHeader` | `ui/components/SectionHeader.kt` | Standardized header featuring a 38dp rounded icon container, Persian title, and optional badge/action. | `title: String`, `subtitle: String?`, `icon: ImageVector`, `action: @Composable (() -> Unit)?` |
| `KaratBadge` | `ui/components/KaratBadge.kt` | Interactive or static badge displaying karat purity (18K ۷۵۰, 21K ۸۷۵, 24K ۹۹۹). | `karat: Karat`, `selected: Boolean`, `onClick: (() -> Unit)?` |
| `PresetPill` | `ui/components/PresetPill.kt` | Quick-tap preset chips for weights (5g, 10g), profit margins (5%, 7%), or taxes (0%, 9%). | `label: String`, `selected: Boolean`, `onClick: () -> Unit` |
| `GlassmorphicDock` | `ui/components/GlassmorphicDock.kt` | iOS 26 / Telegram style frosted floating island with spring-animated sliding indicator. | `selectedTab: AppTab`, `onTabSelected: (AppTab) -> Unit`, `modifier: Modifier` |
| `LuxuryDrawer` | `ui/components/LuxuryDrawer.kt` | Right-to-Left `ModalNavigationDrawer` housing Customer Directory, Settings, and Updates. | `rates: MarketRates`, `customerCount: Int`, `onNavigateCustomers: () -> Unit`, `onNavigateSettings: () -> Unit`, `onCheckForUpdates: () -> Unit` |
| `UpdateDialog` | `ui/components/UpdateDialog.kt` | Modal alert displaying release notes, version tags, and direct APK download intent. | `updateInfo: UpdateInfo`, `onDismiss: () -> Unit` |
| `AnimatedPriceTicker` | `ui/components/AnimatedPriceTicker.kt` | Smooth vertical sliding transition for fluctuating prices and sums. | `amount: Long`, `formatter: (Long) -> String`, `style: TextStyle`, `color: Color` |
| `GoldInputField` | `ui/components/GoldInputField.kt` | Input field with Persian digit support, thousand-separators, and trailing units (گرم, درصد, تومان). | `value: String`, `onValueChange: (String) -> Unit`, `label: String`, `trailingUnit: String` |
| `ResultRow` | `ui/components/ResultRow.kt` | Key-value display row for financial summaries with quick copy-to-clipboard interaction. | `label: String`, `value: String`, `valueColor: Color`, `isHighlighted: Boolean`, `copyableText: String?` |
| `LiveRatesTicker` | `ui/components/LiveRatesTicker.kt` | Auto-scrolling horizontal marquee showing real-time gold, dollar, coin, and melt rates. | `rates: MarketRates`, `isRefreshing: Boolean`, `onRefreshClick: () -> Unit` |
| `CustomerDialogs` | `ui/components/CustomerDialogs.kt` | Add Customer, Edit Customer, and Customer Selector modal dialogs. | `customers: List<Customer>`, `onSelect: (Customer) -> Unit`, `onAdd: (Customer) -> Unit`, `onDismiss: () -> Unit` |

---

## 4. State Management Conventions (UDF & AndroidViewModel)

### 4.1 Unidirectional Data Flow (UDF) Architecture
The presentation layer follows a strict Unidirectional Data Flow pattern:
1. **Single Immutable State Stream**: The UI observes a single `StateFlow<CalculatorUiState>` exposed by `GoldCalculatorViewModel`.
2. **State Consumption**: Composables receive immutable state slices or lambdas. They NEVER hold internal business state or mutate ViewModel properties directly.
3. **Event Dispatching**: User interactions (typing digits, clicking tabs, picking customers) trigger explicit public intent methods on `GoldCalculatorViewModel`.
4. **Atomic Updates**: State changes are applied using Kotlin Flow's thread-safe atomic operator:
   ```kotlin
   _uiState.update { currentState ->
       currentState.copy(...)
   }
   ```

### 4.2 ViewModel Lifecycle & No Context Leaks
- **Rule**: `GoldCalculatorViewModel` must inherit from `AndroidViewModel(application: Application)`.
- **Context Leak Elimination**: Never pass `Context` as a parameter to ViewModel methods (e.g. `loadCustomers(context: Context)` is strictly forbidden). Repositories (`CustomerRepository`, `PortfolioRepository`) must be instantiated using `getApplication<Application>().applicationContext` within the ViewModel constructor or injected cleanly.
- **Coroutines Scoping**: All asynchronous operations (HTTP checks, SharedPreferences serialization) must launch on `viewModelScope` with appropriate dispatchers (`Dispatchers.IO`).

### 4.3 In-App Auto-Updater State Contracts
The central `CalculatorUiState` coordinates in-app updates with the following state fields:
```kotlin
data class CalculatorUiState(
    // ... Existing feature fields ...
    
    // In-App GitHub Auto-Updater State
    val updateInfo: UpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val isCheckingForUpdate: Boolean = false,
    val manualUpdateMessage: String? = null
)
```

**Startup & Manual Update Triggers**:
- On ViewModel `init`: Launch `checkForUpdates(isStartup = true)` in `viewModelScope.launch(Dispatchers.IO)`.
- On Drawer "بررسی بروزرسانی" click: Trigger `checkForUpdates(isStartup = false)` to inform the user if they are already on the latest version.

---

## 5. Keystore & Cryptographic Signing Specifications

To safeguard existing user installations from signature mismatch bricking, every release APK must be signed using the repository's persistent release keystore.

### 5.1 Keystore Metadata & Configuration
| Parameter | Value |
|---|---|
| **Location** | `app/keystore/goldex-release.keystore` |
| **Keystore Format** | Java Keystore (JKS) |
| **Key Size & Algorithm** | RSA 2048-bit |
| **Key Alias** | `goldex` |
| **Keystore Password** | `goldexcompanion` |
| **Key Password** | `goldexcompanion` |
| **Certificate Signature** | SHA-256 with RSA encryption |

### 5.2 Gradle Signing Block (`app/build.gradle.kts`)
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("keystore/goldex-release.keystore")
            storePassword = "goldexcompanion"
            keyAlias = "goldex"
            keyPassword = "goldexcompanion"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 5.3 Signature Continuity Invariant
Never generate a new keystore or alter keystore passwords. Doing so will generate a different SHA-256 certificate fingerprint, causing the Android OS package manager to reject app updates with:
`INSTALL_FAILED_UPDATE_INCOMPATIBLE: Package com.goldex.companion signatures do not match previously installed version.`

---

## 6. GitHub Actions CI/CD Deployment Loop

### 6.1 Cloud Pipeline Overview
All production APKs are built, verified, signed, and released exclusively by GitHub Actions.
- **Workflow File**: `.github/workflows/build-and-release.yml`
- **Workflow Name**: `Build and Release`
- **Trigger**: Pushing a git tag matching `v*` (e.g. `v0.8.0`).
- **Runner**: `ubuntu-latest` with JDK 17 (`temurin`) and automated Gradle dependency caching.

### 6.2 Pipeline Execution Stages
1. **Checkout Code**: `actions/checkout@v4` fetches the commit and tag ref.
2. **JDK 17 Setup**: `actions/setup-java@v4` configures Temurin OpenJDK 17 with Gradle cache.
3. **Grant Execution Rights**: `chmod +x gradlew` ensures wrapper executable permissions.
4. **Cloud Assembly & Signing**:
   ```bash
   set -e -o pipefail
   touch build.log
   ./gradlew assembleRelease --stacktrace 2>&1 | tee build.log
   ```
   Gradle compiles Kotlin code, merges resources, generates DEX bytecode, and automatically signs `app/build/outputs/apk/release/app-release.apk` with `goldex-release.keystore`.
5. **Automated Release Publishing (`softprops/action-gh-release@v2`)**:
   - Publishes a formal GitHub Release tagged with `vX.Y.Z`.
   - Attaches `app-release.apk` as a downloadable asset.
   - Automatically generates markdown changelogs from merged commits.
6. **Failure Recovery & Issue Creation**:
   - If the build fails, an automated step executes using `GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}`.
   - Files an issue titled `"CI Build Failure on ${{ github.ref_name }}"` with the label `ci-build-failure` and dumps the last 80 lines of `build.log`.

### 6.3 Standard Release Command Protocol
To ship a new release (e.g., version `0.8.0`):

```bash
# Step 1: Bump version in app/build.gradle.kts
#   versionCode = 15
#   versionName = "0.8.0"

# Step 2: Stage all repository changes
git add -A

# Step 3: Commit with semantic commit message
git commit -m "feat(release): modernize architecture, auto-updater, dock, and drawer v0.8.0"

# Step 4: Push commit to GitHub origin/main
git push origin main

# Step 5: Create and push semantic version tag to trigger cloud release
git tag v0.8.0
git push origin v0.8.0
```

---

## 7. Rules to Prevent Agent Hallucinations (Anti-Pattern Catalog)

The following table catalogs documented errors, stale assumptions, and hallucinations observed in past workflows. Agents must strictly adhere to the verified realities.

| # | Hallucination / Anti-Pattern | Verified Codebase Reality | Enforcement Rule |
|---|---|---|---|
| 1 | **Running local Gradle commands** (`./gradlew assembleRelease`, `./gradlew build`, daemon commands). | Host PC has resource constraints; Gradle daemons cause system freeze. | **FORBIDDEN**. Push all code changes to GitHub; let GitHub Actions build and sign in the cloud. |
| 2 | **Assuming OkHttp, Retrofit, or Ktor are installed** (e.g. stale claim in ARCHITECTURE.md line 15). | `app/build.gradle.kts` has **zero** third-party network libraries. | Use standard `java.net.HttpURLConnection` with `Dispatchers.IO` and `org.json.JSONObject`. |
| 3 | **Referencing `.github/workflows/build-release.yml`** | The actual workflow file is `.github/workflows/build-and-release.yml`. | Always use the exact filename `build-and-release.yml`. |
| 4 | **Naming state `GoldCalculatorUiState`** | The actual data class in `GoldCalculatorViewModel.kt` is `CalculatorUiState`. | Use `CalculatorUiState` consistently across all presentation code. |
| 5 | **Adding Dagger / Hilt dependency injection** (`@HiltViewModel`, `@AndroidEntryPoint`, `@Inject`). | Project uses standard Android Architecture Components without Hilt. | Use `AndroidViewModel` or vanilla Compose `viewModel()` factory. |
| 6 | **Calculating VAT (9%) on gross gold value** | Iranian tax law explicitly exempts raw gold (معافیت اصل طلا). | Calculate VAT strictly on `(Wage + Profit)`: `tax = (wageAmount + profitAmount) * 0.09`. |
| 7 | **Rendering raw ASCII/English digits in UI** | Persian gold traders require authentic Persian numerals (۱۲۳۴۵۶۷۸۹۰). | Use `PersianNumberFormatter` or rely on `Type.kt` OpenType `ss01` font feature. |
| 8 | **Passing `Context` into ViewModel public methods** (`loadCustomers(context)`, `addCustomer(context)`). | Violates clean architecture and risks memory leaks during configuration changes. | Inherit from `AndroidViewModel(application)` and use `getApplication<Application>()`. |
| 9 | **Omitting `User-Agent` header in GitHub API requests** | GitHub API returns `HTTP 403 Forbidden` for requests without a valid `User-Agent`. | Always set `setRequestProperty("User-Agent", "GoldExCompanion-Android")`. |
| 10 | **Omitting `connection.disconnect()` in HTTP calls** | Causes socket leak and connection pool exhaustion on mobile devices. | Always wrap HTTP calls in `try ... finally { connection?.disconnect() }`. |
| 11 | **Creating a new signing keystore** | Breaks Android package cryptographic continuity. | Use ONLY `app/keystore/goldex-release.keystore` with alias `goldex` and password `goldexcompanion`. |
| 12 | **Assuming Android navigation library is used** | Project uses a custom single-activity multi-tab architecture with `AppTab` and `AnimatedContent`. | Do not add `androidx.navigation:navigation-compose`. Manage screens via `AppTab` state. |

---

## 8. Verification & Forensic Audit Checklist

Before any milestone is declared complete, the implementing agent and auditor must verify:
- [ ] **No Local Gradle Execution**: Verify no `./gradlew` commands were executed locally.
- [ ] **Accurate File References**: All file paths cited in documentation and code correspond to real files on disk.
- [ ] **State Flow Compliance**: Every UI element is backed by immutable properties in `CalculatorUiState`.
- [ ] **Design Token Strictness**: All colors and borders are referenced from `GoldExAppColors` / `Color.kt`.
- [ ] **Persian Digits & Typography**: No unformatted English numerals appear in user-visible currency or weight displays.
- [ ] **Zero DEX Bloat**: No unapproved dependencies added to `app/build.gradle.kts`.
- [ ] **Cloud CI Readiness**: Keystore configuration in `app/build.gradle.kts` matches `app/keystore/goldex-release.keystore`.
