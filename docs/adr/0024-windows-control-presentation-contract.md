# ADR 0024: Context-specific Windows controls

## Context

Windows customer cards reused the shell's 24dp capsule/compact 11sp control while other screens used shared 12dp buttons or Material defaults. Rule documents simultaneously required 24dp primary buttons and universal 12dp curvature; all mutually exclusive choices were treated as segmented switches. The user selected existing calculator body controls, Android customer filter options, Android banking quick access and the desktop header as distinct references.

## Decision

`desktop/ui/controls/DesktopControls.kt` owns Windows presentation adapters using shared Aurum tokens. Body actions/outlined fields/search use 12dp, header/nav controls 24dp, independent record/category/instrument filters use Android's reference capsules and counts. Body actions require an explicit meaningful icon and 14sp one-line label. Form choices use independent 12dp options. Customer type is not rendered or selected in Windows; creating uses the compatible model default and editing preserves the exact legacy role. Dashboard and rate-chart horizons retain the previous shared sliding selector; inventory location uses a dropdown. Approved calculator mode controls remain local. Both dashboard card shortcuts are transparent directional text navigation links. Each footer item opens its explicit rate instrument via the existing detail state owner. Wide settings align the bottom of the stretching rate-source card with the profile card; narrow settings retain natural heights. Dashboard shortcuts use icon-over-label tiles. WorkspaceHeader owns rate-detail identity/back, as it already owns customer statement identity/back/registration.

`DesktopDialogs.kt` owns presentation lifetime for app-owned Windows dialogs, confirmations and modal sheets. `DesktopDialogHost` retains an outgoing display snapshot until the existing LuxuryMotion transition ends while feature save/cancel state changes remain immediate. Native dialog adapters apply layer-only fade/scale or translation without changing fields, surface layout or domain callbacks. Outgoing windows block pointer, keyboard and action input and hide accessibility semantics; reduced motion sets host duration to zero. Customer cards show contact with optional parenthesized account code under the name/status row; full action labels reflow into two rows. User-entered metadata, financial balances, stored role and deletion safeguards remain unchanged.

The mandatory rule and detailed specification are `.agents/rules/windows-design-system.md` and `docs/design/windows-controls.md`. Existing generic button/selector/input rules explicitly route Windows to this contract; historical architecture statements no longer claim all controls share one radius. Guard tests prevent raw action/filter/input variants from reappearing without a reviewed exception.

## Consequences

The user's later visual review explicitly retains compact footer preview capsules and a small rounded rate-details navigation action. Both dashboard card destinations share the transparent navigation link. Icon-only controls use circular Material state layers without a rectangular hover background. Customer filters wrap their content while search fills the remaining toolbar width. Rate statistics retain their policies and use readable table typography with a compact explanatory footer. The settings source explanation is bottom-aligned with excess space distributed among natural child measurements. Exact exceptions and dimensions are recorded in the detailed specification; none changes a save/delete action or a financial contract.

- Android UI and shared tokens remain unchanged; this is a Windows-only release.
- Feature state holders, domain policies, navigation destinations, persisted shape, calculation precision and transactional delete/write guards remain unchanged.
- All action icons are compile-time required, including optional/empty/error/dialog actions. Disabled invoice placeholders stay disabled.
- Overflow uses scrolling or responsive reflow, not compact text or hidden icons. Reduced-motion preference is supplied to the Windows control boundary.
- These host-owned wrappers intentionally avoid a shared token migration affecting Android. Shared components can adopt a common context API later if both hosts require the same change.

## Revisit

Revisit when Android and Windows jointly change control geometry or accessibility APIs, or when meaningful behavior needs another control role. Any exception must be recorded in the detailed contract and verified at minimum width in both themes.
