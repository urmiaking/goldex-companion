# Segmented Switch & Selector Invariant (`segmented-switch-design-system.md`)

## 1. Core Principle
Whenever the user can select between two, three, or more mutually exclusive modes, timeframes, or categories (e.g., Benchmark Rate in Calculator, Wage Mode, Trend Chart Timeframe, Karat Convert Mode), a dedicated animated segmented control (`LuxurySegmentedControl`) MUST be used.

## 2. Inviolable Design & Motion Rules
- **Sliding Indicator Transition**: The active selection indicator must glide smoothly between options using a physical spring transition (`spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)`). Instant snapping without motion is prohibited.
- **No Harsh Dark Borders**: Outer containers and pills must NEVER use black or dark-gray solid borders (`colors.border` with dark alpha). Always use delicate golden borders: `BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.35f))`.
- **RTL Awareness**: The offset and sliding calculations must natively support Right-to-Left (RTL) layout direction, ensuring the pill moves correctly towards the selected Persian label. In Jetpack Compose, `Modifier.offset(x)` is layout-direction aware (positive `x` offsets away from Start/Right edge towards End/Left edge in RTL); hence `targetOffset = itemWidth * selectedIndex` is the correct formula for both RTL and LTR without manual inversion.
- **Typography**: Text inside all segmented options must use `VazirmatnFamily` with `fontFeatureSettings = "ss01"`, ensuring Persian numerals (۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹).
- **Colors**:
  - Container Background: `colors.surfaceElevated`
  - Active Pill: `colors.surface` with `BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.5f))` and soft elevation (`1.dp` to `1.5.dp`)
  - Active Text Color: `colors.goldPrimary` (Bold)
  - Inactive Text Color: `colors.textMuted` (Medium)
