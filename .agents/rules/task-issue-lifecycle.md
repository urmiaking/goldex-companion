---
trigger: always_on
description: Mandatory GitHub Issue creation at task start, closing upon completion, and markdown documentation maintenance.
---

# GitHub Issue Lifecycle & Documentation Maintenance

## 1. Mandatory GitHub Issue Lifecycle
Whenever the user assigns a new task, feature request, or bug fix:
1. **Task Inception (شروع هر تسک)**:
   - BEFORE making code changes, create a new GitHub Issue using `gh issue create` or GitHub MCP:
     ```bash
     gh issue create --title "<Title in Persian>" --body "<Detailed description of requirements and plan in Persian>"
     ```
   - Note the issue number created for reference during the task.
2. **Task Completion (پایان هر تسک)**:
   - AFTER the task is implemented, cloud CI build succeeds, and the APK release is published, CLOSE the issue:
     ```bash
     gh issue close <issue-number> --comment "<Summary of implementation and release link>"
     ```

## 2. Continuous Markdown & Documentation Maintenance
After implementing changes in the codebase:
- The agent is explicitly authorized and required to update relevant markdown (`.md`) files in the repository—including `README.md`, `AGENTS.md`, architecture docs, `.agents/workflows/`, and rule definitions—whenever implementation changes affect UI design, navigation, domain formulas, or configuration.
