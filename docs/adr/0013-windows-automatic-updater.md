# ADR 0013: Windows automatic portable updates

## Status and context

Accepted at the user's request. Windows must download an available update automatically, then ask before closing, replacing the application and reopening it. The shipped host is a self-contained portable Qirato directory, not an MSI installation. Android owns GitHub latest, so that endpoint cannot discover independent Windows releases.

## Decision

The feature stays in `desktop/update`: `WindowsReleasePolicy` selects the highest numeric version among published, non-prerelease `vX.Y.Z` or `windows-vX.Y.Z` records containing the exact Windows x64 ZIP. `WindowsUpdateNetwork` reads paginated GitHub REST releases at startup and every six hours. A settings action also checks manually. Existing Android version, updater, signing and the selective release workflow remain unchanged.

The trust source is HTTPS metadata for the fixed project repository. Require the GitHub asset's SHA-256 digest, exact project/tag/file URL, uploaded state and a bounded declared size. Only GitHub HTTPS download/redirect hosts are accepted. Verify bytes and size before extraction; unverified/incomplete downloads cannot be installed. This detects corruption and substitution against the repository's digest; it is not an independent Authenticode or offline signing chain. Protect repository release authority. API errors and missing digests fail visibly rather than claiming the current version is latest or silently choosing an older candidate. GitHub API references: [releases](https://docs.github.com/en/rest/releases/releases), [asset digests](https://github.blog/changelog/2025-06-03-releases-now-expose-digests-for-release-assets/).

Stage next to the installed Qirato directory, on the same volume, using a unique `.qirato-update-<uuid>` sibling. Automatic replacement requires the complete portable bundle in a writable directory named Qirato, outside the business data directory and without redirected installation paths. Reject traversal, Windows path aliases, case-insensitive duplicates, excessive entries/expanded size and incomplete/unexpected bundle roots. ZIP metadata never creates symbolic links. The new bundled EXE must pass `--verify-runtime` and report the expected version before readiness. The staged PowerShell script is the resource shipped with the current app, not downloaded release instructions.

`WindowsUpdater` owns immutable phase/progress/confirmation state. It downloads in the background, lets users stop or postpone, retains a ready package for later, and never invokes shutdown without the restart intent. The Persian dialog shows release notes and warns about unsaved fields. Financial saves disable restart. `Main` starts the helper and waits for readiness before cancelling/joining state owners, releasing the store lock and exiting. Failure to start the helper leaves the app open. No new persisted preference/schema is introduced.

The hidden helper validates absolute sibling paths, its own plan/script location, the parent process executable and bundle, and refuses reparse points. It waits up to two minutes for the app's actual process to exit, with no force-kill. It acquires the existing workspace lock to prevent replacing files while another data owner is active. Directory renames refuse existing destinations; bounded retries tolerate transient file locks. The old application becomes a unique `.qirato-previous-<uuid>` sibling, then the staged bundle replaces the original path. Release the lock, start the new app visibly, and observe immediate startup. If replacement/startup fails, retain the failed bundle, restore the previous directory and start it. The helper never deletes directories, financial files, preferences or backups, and never requires elevation or changes machine-wide script policy.

## Compatibility, recovery and consequences

Business data remains `%LOCALAPPDATA%/Qirato/Desktop`; the helper opens its lock only and does not rewrite `workspace.json` or its previous-document backup. Runtime state/calculator drafts are not carried across restart. Previous program directories, failed staging, verification/helper logs and `result.json` are retained for recovery and may consume disk space; no recursive automatic cleanup discards unknown user files in an older directory.

Versions before Windows 0.56.38 require one manual complete ZIP replacement to bootstrap the updater. Thereafter both Windows-only and shared releases are eligible. Keep future storage migrations backward-compatible or explicitly extend the update/rollback plan: preserving files cannot make an older binary understand a new schema. Installer-managed or protected/renamed folders need a later explicit installation strategy; the current UI reports a writable portable-folder requirement.

If automatic rollback cannot restore a locked directory, close all Qirato processes, keep the failed/staging directory, move the current program directory to a fresh sibling name and rename the recorded `.qirato-previous-*` directory to Qirato. Do not delete or replace the data directory. For data corruption or incompatible future schemas, follow ADR 0011's separate backup/rollback procedure. Result/log files contain updater diagnostics only, not business payloads.

## Verification and revisit

Local tests cover independent Windows discovery and pagination, unavailable checksums, malformed ZIPs, corrupt/truncated/cancelled downloads, confirmation/postponement/retry and Persian light/dark UI. Actual Windows PowerShell integration tests use disposable synthetic app/data directories and verify waiting for process exit, successful replacement/relaunch, refusal of an escaped plan and restoration/relaunch after startup failure. Runtime packaging checks verify the updater resource and bundled version. No local APK assembly/signing.

Revisit for signed Windows installers, mandatory/security update policy, extra architectures, enterprise proxies/offline feeds, cache cleanup, durable cross-version UI state or schema migrations that invalidate rollback.
