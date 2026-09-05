# Animated Price Ticker & Numeric Transitions Invariant (nimated-price-ticker.md)

## 1. Core Principle
Whenever financial numbers, calculated totals, weights, wage amounts, profit sums, or tax values change in response to user input or live market updates, they MUST NOT snap abruptly. They must transition with a smooth, vertical sliding fintech animation using AnimatedPriceTicker.

## 2. Inviolable Rules

### Rule 1: Dynamic Calculation Labels Must Animate
- In all calculator tabs, monitors, invoice previews, and dashboard metrics:
  - Total payable price (	otalPayable)
  - Raw gold value (awGoldValue)
  - Workshop wage (wageAmount)
  - Retail profit (profitAmount)
  - Tax amount (	axAmount)
  - Net and gross weights (
etWeight, grossWeight)
  - Live spot rates and lot values
  MUST be wrapped in AnimatedPriceTicker(...) or AnimatedContent(...).

### Rule 2: Micro-Interaction Specs
- Motion: Vertical slide with subtle fade (slideInVertically + adeIn together with slideOutVertically + adeOut).
- Physics: Spring dampening (dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium).
- Formatting: All animated values must be pre-formatted using PersianNumberFormatter to guarantee Persian numerals and thousand-separators.

### Rule 3: Visual Stability
- Labels must specify maxLines = 1 and modifier.clipToBounds() to prevent layout jumping or overlapping adjacent elements during transitions.
