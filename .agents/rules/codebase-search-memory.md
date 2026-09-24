# Codebase Exploration & Search Policy (Codebase Memory MCP & Fallback)

## 1. Core Principle
- **No Repetitive Terminal Grep Loops**: Do NOT run repetitive loops of `git grep`, `rg`, or `findstr` via `run_command` to locate files, symbols, or text across the project. This causes terminal latency, high token usage, and severe task delays.
- **Primary Search Engine**: ALWAYS prioritize **`codebase-memory-mcp`** via `call_mcp_tool`. The knowledge graph is already parsed and provides sub-second AST-enriched search, symbol tracing, and caller/callee graphs.

---

## 2. Step 1: Verify Index Freshness (Re-index if Needed)
Before performing extensive codebase searches on a task:
1. Verify index presence:
   ```json
   {
     "ServerName": "codebase-memory-mcp",
     "ToolName": "list_projects",
     "Arguments": { "include_details": false }
   }
   ```
   The standard project identifier for this workspace is:
   `C-Users-mskho-.gemini-antigravity-scratch-goldex-companion`
   *(Or the matching path if working inside a Git Worktree).*

2. **If the project is not listed or has undergone massive structural changes**:
   Trigger a quick re-index before proceeding:
   ```json
   {
     "ServerName": "codebase-memory-mcp",
     "ToolName": "index_repository",
     "Arguments": {
       "repo_path": "<current-working-directory-path>",
       "mode": "fast"
     }
   }
   ```

---

## 3. Step 2: High-Speed Graph & Code Search
Use the appropriate `codebase-memory-mcp` tool:

- **`search_code` (Fastest for text/keywords & Persian terms)**:
  - Finds any string, identifier, or Persian term (e.g. `"حواله"`, `"فاکتور"`, `"SettlementModal"`) in ~300ms.
  - Returns enclosing functions, classes, line ranges, and call degrees.
  ```json
  {
    "ServerName": "codebase-memory-mcp",
    "ToolName": "search_code",
    "Arguments": {
      "project": "C-Users-mskho-.gemini-antigravity-scratch-goldex-companion",
      "pattern": "حواله"
    }
  }
  ```

- **`search_graph` (Structural symbol discovery)**:
  - Finds composables, classes, functions, and interfaces by name or BM25 ranking.
  ```json
  {
    "ServerName": "codebase-memory-mcp",
    "ToolName": "search_graph",
    "Arguments": {
      "project": "C-Users-mskho-.gemini-antigravity-scratch-goldex-companion",
      "query": "BarterInvoiceViewModel"
    }
  }
  ```

- **`trace_path` (Call hierarchy and blast radius)**:
  - Finds all callers or callees of a function to verify call sites before editing.

- **`get_code_snippet` (Targeted inspection)**:
  - Inspects exact symbol code without reading entire 1,000-line files.

---

## 4. Step 3: Graceful Fallback Strategy
If and only if:
1. `codebase-memory-mcp` is unavailable or throws a server error, OR
2. You are searching non-code files (e.g. Gradle scripts `build.gradle.kts`, Android XML layouts/drawables, markdown files), OR
3. You need to inspect lines in files flagged as skipped or partially parsed in `index_status`:

**Allowed Fallback**:
- Run a single, scoped `git grep` with path narrowing:
  ```powershell
  git grep -n "keyword" app/src/main/res/
  ```
- Or use `view_file` directly on the known package/directory.
- **NEVER** fall back into an unguided loop of consecutive terminal grep commands across the whole repository.
