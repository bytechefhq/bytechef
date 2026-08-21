# Restore the ai_hub_task_artifact table + sidebar (drop audit-event approach) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Restore the original `ai_hub_task_artifact` table subsystem for task↔artifact relationship tracking (including workflow-open from the sidebar), stop the active `persistent_audit_event` flooding, and restore the sidebar artifact list — WITHOUT the standalone Artifact History page and WITHOUT the audit-event artifact approach.

**Architecture:** The artifact subsystem is restored by reverting phases 1 (`7bbc262`), 2 (`76a6bd8`), and 4 (`3b8d002`) on the server, and the sidebar half of phase 3 (`29bc527`) on the client. Because **only phases 1/2/4 touched the server artifact paths since `7bbc262^`** (verified — the Audit P0/P1/P2 commits are orthogonal), the server restore is a single `git checkout 7bbc262^ -- <files>` plus two removals. The client is a guided restore-from-`29bc527^` that deliberately excludes the history page.

**Baselines:** `7bbc262de10^` (= pre-phase-1, artifact subsystem fully present, tool callbacks record artifacts) for the server; `29bc52785d1^` (= pre-client-removal) for the client.

**Key facts:**
- `AI_HUB_WORKFLOW_REFERENCED` / `AiHubAuditEmitter` are NOT restored — workflows open from the sidebar via the table's `WORKFLOW_REFERENCED` artifact row (recorded by `OpenWorkflowTabToolCallback` → `AiHubTaskArtifactRecorder`), exactly as originally.
- The 9 artifact audit event types (FILE_CREATED, DATA_TABLE_ROW_*, KB_DOCUMENT_*, WORKFLOW_EXECUTION_STARTED) are removed from `AiHubAuditEvent` (restoring it to `7bbc262^`), which stops the flooding.
- The audit engine (Audit P0/P1/P2: `a8b2ccb`/`b95af8e`/`999845e`) is untouched.

---

## Task 1: Restore the server artifact subsystem (one checkout)

**Files (restore to `7bbc262de10^`):** all 27 server files phases 1/2/4 touched.

- [ ] **Step 1: Restore every phase-1/2/4-touched server file to its pre-phase-1 state**

```bash
git checkout 7bbc262de10^ -- \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubTaskArtifactGraphQlController.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubTaskGraphQlController.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-artifact.graphqls \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-task.graphqls \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/test/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubTaskGraphQlControllerTest.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/task/AiHubTaskArtifactRecorderImpl.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/task/AiHubTaskArtifactServiceImpl.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/task/AiHubTaskServiceImpl.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/task/repository/AiHubTaskArtifactRepository.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260423000002_ai_hub_task_artifact_init.xml \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260429140001_ai_hub_task_artifact_add_environment.xml \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditEvent.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/ImageGeneratorConfiguration.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/SlideBuilderConfiguration.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/AddDataTableColumnToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/AddDataTableRowToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/AddKnowledgeBaseDocumentToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/CreateAssetFileToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/CreateBinaryAssetFileToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/DeleteDataTableRowToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/DeleteKnowledgeBaseDocumentToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/RunChatWorkflowToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/UpdateDataTableRowToolCallback.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/config/ImageGeneratorConfigurationTest.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/config/SlideBuilderConfigurationTest.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/task/AiHubTaskArtifactRecorderTest.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/task/AiHubTaskArtifactServiceTest.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/task/AiHubTaskServiceTest.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentResourceKind.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifact.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactKind.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactService.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactStatus.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentResourceKindTest.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactKindWireFormatTest.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/AiHubTaskArtifactRecorder.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/OpenWorkflowTabToolCallback.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/OpenWorkflowTabToolCallbackTest.java \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/toolsearch/ToolSearchCatalogFeederGlobalToolsTest.java
```

- [ ] **Step 2: Remove the phase-2 drop migration (checkout does not delete files absent from the baseline)**

```bash
git rm -q server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260605000001_ai_hub_task_artifact_drop.xml
```

- [ ] **Step 3: Remove the phase-1 audit-design docs (the rejected approach; keep docs clean)**

```bash
git rm -q docs/superpowers/specs/2026-06-05-ai-hub-artifacts-to-audit-engine-design.md docs/superpowers/plans/2026-06-05-ai-hub-artifacts-to-audit-engine.md
```

- [ ] **Step 4: Confirm the audit-emitter bridge files are absent (P4 deleted them; we do NOT want them)**

```bash
ls server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/AiHubAuditEmitter.java 2>&1 | grep -q 'No such file' && echo "OK: AiHubAuditEmitter absent"
ls server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditEmitterImpl.java 2>&1 | grep -q 'No such file' && echo "OK: AiHubAuditEmitterImpl absent"
```
Expected: both "OK: ... absent". (They were created by phase 1 and deleted by phase 4; the `7bbc262^` checkout doesn't recreate them because they don't exist at that baseline.)

- [ ] **Step 5: Verify the Liquibase changelog master includes the restored migrations**

The restored `20260423000002_*` / `20260429140001_*` files must be referenced by the aihub changelog index. Run:
```bash
rg -n 'ai_hub_task_artifact' server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/*.xml | rg -v '20260605000001'
```
Expected: the master/index changelog references `20260423000002` and `20260429140001`. If the index file (e.g. `changelog.xml`/`db.changelog-master`) was edited by phase 2 to drop those `<include>`s, restore it too: `git checkout 7bbc262de10^ -- <index path>`. (The index lives in the same `changelog/automation/aihub/` dir; identify it with `rg -l '20260423000002' <that dir>` at `7bbc262^`.)

- [ ] **Step 6: Compile the three server modules**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:compileJava :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:compileJava :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql:compileJava`
Expected: BUILD SUCCESSFUL. If a restored file references an external API that changed since `7bbc262^`, fix that call site minimally (the artifact subsystem is self-contained, so this is unlikely).

- [ ] **Step 7: Delete stale build/resources copies of the dropped migration (per repo Liquibase note)**

```bash
find server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build -name '20260605000001_ai_hub_task_artifact_drop.xml' -delete 2>/dev/null; echo done
```

- [ ] **Step 8: Run the restored server tests**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:test :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql:test`
Expected: all green (the restored tests are the original artifact tests).

- [ ] **Step 9: Spotless + commit**

```bash
./gradlew spotlessApply -q
git add -A
git commit -m "732 AI Hub: restore ai_hub_task_artifact subsystem; stop audit-event flooding"
```

---

## Task 2: Restore the client sidebar artifact list (exclude the history page)

**Files (restore from `29bc52785d1^`, EXCEPT the history page):**

- [ ] **Step 1: Restore the artifact GraphQL operations + the sidebar/api/hook/runtime files**

```bash
git checkout 29bc52785d1^ -- \
  client/src/graphql/ai/aihub/artifact/ \
  client/src/graphql/ai/aihub/task/aiHubTaskArtifactsByAiHubTask.graphql \
  client/src/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx \
  client/src/pages/automation/ai-hub/tasks/api/tasks.api.ts \
  client/src/pages/automation/ai-hub/tasks/hooks/useTasks.ts \
  client/src/pages/automation/ai-hub/tasks/hooks/useTasks.test.ts \
  client/src/pages/automation/ai-hub/tasks/hooks/useRecordReferencedArtifacts.ts \
  client/src/pages/automation/ai-hub/tasks/tests/useRecordReferencedArtifacts.test.tsx \
  client/src/pages/automation/ai-hub/tasks/tests/AiHubTasksSidebar.test.tsx \
  client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx \
  client/src/pages/automation/ai-hub/runtime-providers/tests/AiHubRuntimeProvider.test.tsx \
  client/src/pages/automation/ai-hub/composer/hooks/useAiHubAttachmentUpload.ts
```
(Adjust the exact paths under `client/src/graphql/ai/aihub/artifact/` and `task/` per `git show 29bc52785d1 --name-status`; the four artifact `.graphql` ops listed in that commit are the source of truth.)

- [ ] **Step 2: Restore AiHub.tsx WITHOUT the history-page route, by hand**

`git show 29bc52785d1 -- client/src/pages/automation/ai-hub/AiHub.tsx` shows what phase 3 removed (artifact query invalidation + reference-recording hook). Re-apply ONLY those bits to the current `AiHub.tsx` — do NOT add any import of or route to `AiHubArtifactHistoryPage`. Concretely:
  - Re-add the `useRecordReferencedArtifacts()` hook call and the artifact-query invalidation that the diff removed.
  - Leave out anything referencing `AiHubArtifactHistoryPage`.

- [ ] **Step 3: Confirm the history page stays deleted**

```bash
test ! -f client/src/pages/automation/ai-hub/AiHubArtifactHistoryPage.tsx && echo "OK: history page absent"
rg -n 'AiHubArtifactHistoryPage' client/src && echo "FAIL: dangling reference" || echo "OK: no references"
```
Expected: "OK: history page absent" and "OK: no references". If `routes.tsx` gained a reference via any restore, remove that route + import by hand (the page intentionally does not exist).

- [ ] **Step 4: Regenerate GraphQL codegen (do NOT git-checkout graphql.ts/graphql-types.ts)**

Run: `cd client && npm run codegen`
Expected: `graphql.ts` + `graphql-types.ts` regenerate with the artifact operations (the server `ai-hub-artifact.graphqls` restored in Task 1 is on the codegen schema path). No errors.

- [ ] **Step 5: Client check**

Run: `cd client && npm run check`
Expected: prettier + eslint + tsc + vitest all green. Fix any restored-code lint drift (e.g. import order) with `npx eslint --fix` / `npm run format`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "732 client - AI Hub: restore artifact sidebar list (history page stays removed)"
```

---

## Task 3: Manual verification

- [ ] **Step 1: DB reset note** — the restored `20260423000002` init migration re-creates `ai_hub_task_artifact`. On a dev DB that already ran the phase-2 drop, Liquibase will treat the init changeset as already-applied (id match) and will NOT re-create the table. Because the feature is unreleased on `0_732`, reset the dev DB: `docker compose -f server/docker-compose.dev.infra.yml down -v` then restart. Document this in the commit body if not already.
- [ ] **Step 2: Smoke** — start the server + client, open AI Hub, confirm: the sidebar expand-arrow + artifact list appear, opening a workflow from a `WORKFLOW_REFERENCED` row works, and NO new rows land in `persistent_audit_event` for file/data-table/KB/workflow artifact activity (`SELECT event_type, count(*) FROM persistent_audit_event GROUP BY 1` shows only lifecycle events).

---

## Self-Review

**Spec coverage:** Restore table subsystem (Task 1 steps 1-2) ✓; stop audit flooding (Task 1 step 1 restores `AiHubAuditEvent` + tool callbacks to pre-phase-1 → no artifact `publish()`) ✓; sidebar list incl. workflow-open (Task 2) ✓; NO history page (Task 2 steps 2-3) ✓; audit engine P0/P1/P2 untouched (none of the restored paths intersect them — verified) ✓; no `AiHubAuditEmitter`/`AI_HUB_WORKFLOW_REFERENCED` (absent at `7bbc262^`; Task 1 step 4 asserts) ✓.

**Placeholder scan:** Two steps require resolving a path at implementation time (the Liquibase changelog-index file in Task 1 step 5; the exact artifact `.graphql` filenames in Task 2 step 1) — both give the exact command to discover them, not deferred work.

**Risk:** The only non-mechanical edits are Task 2 steps 2-3 (excluding the history page from `AiHub.tsx`/`routes.tsx`). Everything else is `git checkout`/`git rm` + regen, which is deterministic.
