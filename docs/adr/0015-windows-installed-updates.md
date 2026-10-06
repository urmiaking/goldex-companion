# 0015 — Installed Windows releases and five-minute discovery

Status: accepted (2026-10-06).

## Context

Portable ZIP updates require a writable extracted folder and leave rollback copies. Users need registered installation, shortcuts and replacement of a previous installed version. Discovery must work during normal use without manual checks. Existing ZIP clients must continue finding releases.

## Decision

- Keep the verified Kotlin/Compose/JDK pair. Build official MSI from the verified Compose image with pinned WiX 3.14.1 portable binaries (fixed SHA-256, build directory only). Do not publish generated Compose `packageMsi` defaults.
- Preserve upgrade identity `A6B3CF2A-A8F7-4CDE-A49A-F8935C6B8306`. Install per user at `%LOCALAPPDATA%\Programs\Qirato`; create desktop/start-menu shortcuts and installed-app registration. MajorUpgrade runs after InstallInitialize so removal participates in MSI rollback; reject downgrades. Components own only packaged files, shortcuts and installer registry values. No wildcard recursive cleanup or financial-data ownership.
- Keep `%LOCALAPPDATA%\Qirato\Desktop` unchanged. Installation, upgrade and uninstall never enumerate/remove that data directory. MSI upgrades replace the registered old product; arbitrary extracted ZIP folders remain user-owned. Close portable app and install MSI once to migrate, using existing local data.
- Publish MSI plus ZIP from 0.56.40 onward. ZIP remains baseline/discovery compatibility asset for older clients. Finalization also requires MSI for these versions. Pipeline changes release both hosts under existing selective routing.
- Main already starts updater. Check immediately, then wait five minutes after each completed attempt. Serialize checks/downloads and retry failures on next tick. Cancel owned work at shutdown. Download automatically; restart remains explicit user intent. Postponed packages do not reopen on background ticks; manual check can reopen. Ready state is process-local, so exiting before applying redownloads the package next time.
- A marker packaged only in MSI selects MSI discovery; portable clients retain ZIP discovery. No invisible portable migration/deletion. Both channels require published stable matching-version assets from the fixed repository, declared size and GitHub SHA-256.
- MSI preparation verifies hash and COM metadata (name/vendor/version/upgrade identity/installer marker). Hidden shipped helper repeats validation before acknowledgment and installation, checks fixed installation/staging paths and rejects reparse paths, waits up to two minutes for exact app process, then holds business store lock while invoking `msiexec /qn /norestart`. It never forces user processes to exit. Verify installed runtime/version and reopen app after releasing lock.
- MSI installation failures use Windows Installer transaction rollback and reopen remaining app. Runtime failure after successful MSI completion does not promise automatic restoration of an uninstalled product; downloaded installer/result metadata remain for repair. Preserve existing ZIP backup/restore separately.

## Verification and consequences

Local tests exercise startup/ticks/idempotency/no overlapping checks, postponed prompts, package channels, hash/download and portable helper rollback. Local Android compile/unit tests and desktop tests remain required; no local APK assembly/signing. Inspect MSI locally, extract its CAB/file table into a workspace-only image and verify native runtime without installing on the developer account. Disposable Windows CI performs actual install, shortcuts, a lower-version registered fixture upgrade through shipped helper, removal of an obsolete installer-owned file, restart, same-version reinstall, downgrade rejection and uninstall with external data sentinel. Fixture uses current runtime bytes under lower product metadata; it verifies installation ownership rather than old application compatibility. JVM tests remain local.

MSI remains unsigned until Windows Authenticode identity is provisioned. Android signing identity is untouched. Installer uses native Windows dialogs; application retains Persian RTL/Vazirmatn/Aurum. Installer tooling is not a runtime dependency. ICE91 is inapplicable to this explicitly per-user-only product and is excluded; all other MSI validation, including user-profile cleanup/key-path checks ICE64/ICE38, runs during linking. A scoped PR workflow runs only installer/updater changes, exercising installation on disposable Windows before merge; release CI repeats it on tagged bytes. It does not run JVM unit tests in CI.

## Revisit

Revisit for code signing, enterprise/all-users installation, different install roots, automatic portable migration or persistent downloaded-update recovery. Those need explicit compatibility/recovery policy, not deletion of arbitrary previous folders.
