# Qirato Gradle/Test Performance

Use this skill when changing Gradle, unit tests, UI test sources or CI of GoldEx Companion.

- Separate expensive Compose Desktop test classes into desktop/src/uiTest/kotlin; the normal desktop/src/test/kotlin is for business-focused JVM tests.
- Run scripts/dev/verify.ps1 -Target windows/android/core/shared for local checks. Do not run full UI suites by default.
- Never create screenshots or PNG artifacts in automated test runs. UI regression tests are opt-in through :desktop:uiTest.
- Gradle daemon, local build cache, parallelism (maximum 4 workers), and configuration cache are enabled; typed tasks must use task input properties rather than captured Gradle scripts.
- CI releases remain platform-selective and signed, with Windows packaged-runtime and installer validation retained.
- Preserve all financial calculation, invoice, storage and synchronization assertions even when reducing the graphical test workload.
- When benchmarking, distinguish cold compilation, task execution time, and a warm build with UP-TO-DATE/cache hits. Do not compare cold and warm as if they were identical conditions.
- Do not touch user changes on main; use an isolated worktree.
