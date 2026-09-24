# Git Worktree and Feature Branching Workflow

## 1. Core Principles
- **No Direct Development on `main`**: All features, bug fixes, UI overhauls, and experimental tasks must be developed on dedicated feature branches (e.g. `feature/<name>` or `fix/<name>`).
- **Parallel Agent Isolation (Git Worktree)**: When running multiple AI agents or chats simultaneously on this repository, **NEVER** share the same physical working directory. A single Git working directory can only have one active `HEAD` and will suffer from file overwrites, `.git/index.lock` collisions, and Gradle build lock issues.
- Use **Git Worktree** to provide each agent with its own physical folder and branch while sharing the same underlying Git repository.

---

## 2. Git Worktree Lifecycle for Parallel Agents

### Step 1: Create a Worktree for a New Feature/Task
From the root repository (on `main`):
```powershell
# Create an isolated worktree in a sibling folder with a new feature branch
git worktree add ../goldex-<feature-name> -b feature/<feature-name> main
```
*Note: Storing worktrees in sibling directories (e.g. `../goldex-<feature-name>`) or in `.worktrees/<feature-name>` ensures Gradle and IDEs do not index nested repositories.*

### Step 2: Agent Operates in the Worktree
The agent or chat session operates entirely within `../goldex-<feature-name>`:
1. Make code changes and additions.
2. Verify Kotlin syntax:
   ```powershell
   .\gradlew compileDebugKotlin --no-build-cache --no-daemon -q
   ```
3. Verify all unit tests locally:
   ```powershell
   .\gradlew testDebugUnitTest --no-daemon -q
   ```
4. Commit changes:
   ```powershell
   git add .
   git commit -m "feat(<feature-name>): <clear description in English or Persian>"
   ```

### Step 3: Push and Merge to `main`
Push the feature branch to remote:
```powershell
git push -u origin feature/<feature-name>
```

Merge the feature into `main`:
- **Via GitHub CLI (Recommended for tracking)**:
  ```powershell
  gh pr create --title "<Title in Persian>" --body "<Summary in Persian>" --base main
  gh pr merge --squash --delete-branch
  ```
- **Or via Local Git Merge**:
  ```powershell
  # Inside the main repository root
  git checkout main
  git pull origin main
  git merge --no-ff feature/<feature-name>
  git push origin main
  ```

### Step 4: Tag & Release
When ready to publish an APK release:
```powershell
git tag v1.X.Y
git push origin v1.X.Y
```
Cloud CI (`build-and-release.yml`) automatically triggers on the tag push, compiles the release APK, signs it with the release keystore, and creates the GitHub Release.

### Step 5: Clean Up Worktree
Once merged and verified:
```powershell
git worktree remove ../goldex-<feature-name>
# Clean up local branch if not automatically deleted
git branch -d feature/<feature-name>
```

---

## 3. Worktree Safety Rules
1. **Never copy keystores**: Never copy `goldex-release.keystore` or private keys into temporary worktrees.
2. **Memory management**: On Windows, run Gradle tasks with `--no-daemon` when working across multiple worktrees to avoid lingering daemon processes consuming RAM.
3. **List active worktrees**: You can view all active worktrees at any time using:
   ```powershell
   git worktree list
   ```
