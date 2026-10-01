---
trigger: always_on
description: Route each task between Graphify architecture exploration and precise codebase-memory MCP inspection; maintain safe, current graphs.
---

# Complementary Graph Exploration

## 1. Route every task

Evaluate the task scope before repository exploration. Agents must use Graphify when architecture, relationships across features/modules, persistence or sync migrations, or comparisons between code and documentation affect the answer or implementation boundary. Use codebase-memory MCP for exact symbols, call sites, tests, literal/Persian text, and source snippets. Use both when both kinds of evidence are needed.

| Task | Required approach |
| --- | --- |
| Localized bug, label, or component adjustment | Locate the owner with codebase-memory MCP; consult an existing Graphify graph if impact crosses that owner. No full graph build required. |
| Architecture question, feature extraction, multi-feature change | Query Graphify for ownership and connections, then verify relevant symbols/callers and source with codebase-memory MCP. |
| Persistence, onboarding, invoice/ledger, sync, or module migration | Use Graphify to map affected boundaries and docs/ADRs; verify actual transaction, calculation, and call behavior with codebase-memory MCP and tests. |
| Local documentation or rules edit | Read the relevant documents directly; consult an existing Graphify graph when relevant. No full graph build required unless the task also compares implementation/architecture or changes ownership, in which case use the architecture route above. |
| Build/resource/configuration change | Inspect the scoped non-code assets directly; use Graphify if the change affects broader architecture or release relationships. |

Do not run both tools merely to duplicate the same lookup. Existing implementation and tests remain authoritative; `ARCHITECTURE.md` documents current ownership.

## 2. Graph presence, scope, and freshness

1. Load the installed `graphify` skill before using its workflows. Follow that installed version for commands, query vocabulary expansion, extraction, and validation; do not assume every host exposes the same CLI/MCP interface.
2. Check `graphify-out/graph.json` relative to the **current checkout/worktree**. Confirm its recorded scan root and covered inputs match this checkout and task. Do not silently query the main checkout's graph for changes in a feature worktree.
3. If a relevant graph exists, reuse it. Compare its manifest with changed code/docs (including uncommitted work); do not judge freshness only by the graph file's timestamp. Incrementally update stale covered inputs through the skill's `--update` workflow before relying on them. Changes to scope/exclusions require a deliberate rebuild.
4. If no graph exists and the routing above requires Graphify, build one for the relevant source and documentation scope using the skill. Do not ask the user to run a routine build themselves. For oversized corpora, follow the skill's scope/cost limits and continue independent targeted inspection while required clarification is pending.
5. Preserve existing graph artifacts if extraction fails. Report stale, partial, or missing coverage and continue with the available tool and scoped source/doc reads. Tool unavailability alone must not block an otherwise verifiable task.

Keep graph outputs local to each worktree. Do not commit generated HTML/JSON, extraction caches, saved answers, or reflection files unless explicitly requested. Do not install hooks, background watchers, or extra export integrations merely to satisfy this rule.

## 3. Query and verify

- Use `query` for broad context, `explain` for a concept, and `path` for connections between known graph nodes, through the installed skill's supported workflow. Expand wording against actual graph labels when required, including Persian/English terminology.
- Start with the owning feature and relevant shared policies/ports. Useful GoldEx starting points include `GoldCalculationUseCases`, `InvoiceDeletionUseCase`, `CompleteOnboardingUseCase`, repository contracts, and `AndroidAppContainer`; verify the actual node names before querying.
- Inspect source locations and edge confidence. Preserve the distinction between extracted, inferred, and ambiguous relationships. Clusters identify investigation candidates, not proven ownership or defects.
- A path in an undirected graph proves connectivity only; it does not establish call direction, data flow, or execution order. Verify those with codebase-memory MCP `trace_path` and current source snippets.
- Check MCP index health/coverage and paginate when necessary, as described in [precise code search](codebase-search-memory.md). Graph gaps are not evidence that a dependency or behavior does not exist.
- Before editing, state one falsifiable hypothesis and one focused validation grounded in the verified owner and nearby tests. Graph exploration does not replace compilation, financial/persistence tests, or migration verification.

## 4. Safe inputs and retained knowledge

Before extraction, exclude release keystores, credentials/private keys, environment/local signing configuration, customer records, database dumps/backups, generated build/cache directories, APKs, and unrelated or nested worktrees. Check the installed scanner's exclusion behavior; do not assume default filtering covers every sensitive file. Graphify outputs and saved answers must not contain secrets, customer data, or complete financial/external payloads.

Use code and non-sensitive design/architecture documentation. Do not introduce an external extraction provider, upload repository inputs, or request an API key just to enable this rule; use the installed skill's local structural extraction and host-agent semantic workflow. Existing permission and secret rules still apply.

If the installed skill supports saved findings or reflections, retain only source-backed, non-sensitive findings and label corrections/dead ends accurately. Saved agent answers and lessons are investigation hints; re-check them against current source before reusing them as evidence.

## 5. After changes

When a graph already covers touched code or documentation, run the installed skill's incremental update workflow after focused validation. Code-only hook/watch refreshes do not substitute for documentation/ADR semantic refreshes. If refresh fails or a required expensive rebuild is deferred, record that the affected graph is stale and explain the limitation in the task report.

Do not bootstrap a new full graph solely for a localized change. Report the relevant graph findings, actual validation, and any coverage/freshness limitation concisely; never claim a query, refresh, or test succeeded without its output.
