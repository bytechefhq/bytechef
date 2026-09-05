# Data Syncs Under Projects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Every task ends in a commit; a subagent that goes idle for more than 600 seconds is killed, so never leave a task's work uncommitted while waiting on a long build.

**Goal:** Make a Data Sync a member of an ordinary, user-visible Project (next to that project's workflows and agents), replace the standalone Data Syncs / Data Sync Deployments pages with Data Syncs tabs and filters on Projects and Deployments, and dissolve the automation sidebar's Build group.

**Architecture:** `data_sync` loses `project_id` and gains `project_workflow_uuid`, pointing at one generated `project_workflow` row of the new type `ProjectWorkflowType.DATA_SYNC` (ordinal 2). `ProjectWorkflowType.isGenerated()` replaces every `== WORKFLOW` / `== AI_AGENT` test outside the agent module. The data sync module joins project publish, delete, duplicate, export/import and git sync through the SPIs the agents change introduced (`ProjectPublishPreListener`, `ProjectDeleteEventListener`, `ProjectContentContributor`); `AiAgentWorkflowUpdateGuard` becomes `GeneratedProjectWorkflowUpdateGuard`. The client mounts the existing wizard under `/automation/projects/:projectId/data-syncs/:dataSyncId` and adds a third tab everywhere the Agents tab exists.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Liquibase, Spring GraphQL, JUnit 5 + Testcontainers; React 19, TypeScript, react-router, TanStack Query, GraphQL codegen, Vitest.

**Spec:** `docs/superpowers/specs/2026-09-22-data-syncs-under-projects-design.md` (approved in full). Template: `docs/superpowers/plans/2026-09-21-agents-under-projects.md`.

**Working tree:** `/Volumes/Data/bytechef/bytechef/.claude/worktrees/data-syncs-under-projects`, branch `data-syncs-under-projects`. All paths below are relative to it.

---

## Global Constraints

- `ProjectWorkflowType` becomes `WORKFLOW, AI_AGENT, DATA_SYNC` — `DATA_SYNC` appended, never reordered. `project_workflow.type` is already an `INT` with no check constraint, so no configuration changeset is added.
- `data_sync` has never shipped (verified in the spec's Schema section: absent from `v0.33.2`, introducing commit `84987d260d7` is not an ancestor of any `master`). Its init changelog `00000000000001_automation_data_sync_init.xml` is edited **in place**. No changeset anywhere else.
- The local dev database is migrated by hand with the spec's "Migration of local data" SQL. That SQL is documented, never run by a task, and never turned into a changeset.
- Every data sync scope stays (`DATA_SYNC_VIEW` / `CREATE` / `EDIT` / `DELETE` / `PUBLISH`, same ranks). `DataSyncFacadeAuthorizationTest` pins every `@PreAuthorize` by exact signature **and** pins the method count (`testFacadeMethodCountIsPinned`). Every task that changes a facade signature updates that test in the same commit.
- A GraphQL schema field and its controller mapping change in the **same** commit: the schema never declares a field whose facade method is gone, so every server commit leaves `DataSyncGraphQlControllerTest` green. Phase 5 is the additive schema work plus a final audit.
- Server data sync code never calls `ProjectDeploymentService.fetchProjectDeployment(projectId, environment)`: deployments are read with `getAllProjectDeployments(projectId)` and the sync's row is picked with `ProjectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, uuid)`.
- No redirects from removed client routes. No MCP / Copilot / AI Hub tools, no single-sync export/import UI or GraphQL, no environment-promotion tests (spec: Out of scope).
- Java style (CLAUDE.md): blank line before control statements; blank line between a variable modification and its use; no chained calls outside the allowed fluent APIs; descriptive names; no `TODO:`; no trailing blank line in a class body; test method names camelCase without underscores; integration tests end in `IntTest`.
- Client style (CLAUDE.md): object keys sorted ascending (not auto-fixed); interfaces end in `I` or `Props`; `Icon`-suffixed lucide imports; `twMerge`, never `cn`; hook order `useState` → `useRef` → stores → custom hooks → derived/`useMemo`/`useCallback` → `useEffect`; `useRef` names end in `Ref`; store hooks always called with a selector; `vi.hoisted` for refs used in `vi.mock` factories; `fieldset` + `border-0` for form groups; `||` for JSX fallbacks.
- Every `*ListItem` row keeps the shared rhythm: first row `min-h-8`, 8px gap (`gap-y-2` right column / `mt-2` left column), second row `min-h-7`.
- Commit subjects: server plain imperative, client `client - <description>`. **No ticket numbers, no `Co-Authored-By` trailer, no "Generated with" line.**

## Execution Notes

These apply to every task. A subagent reads them before starting.

1. **Commit by path only.** `git add <paths>` or `git commit -m "…" -- <paths>`; never `git add -A` / `git add .` at the repo root, never `git commit -a`. Stage only files the task touched.
2. **Never `git stash`, never `git commit --amend`, never rebase.** The stash stack is shared with other worktrees and sessions. Fix a mistake with a new commit.
3. **No trailers.** After every commit run `git log -1 --format=%B` and confirm the message is exactly the subject (plus an optional body) with no `Co-Authored-By` or "Generated with" line. If one slipped in, stop and report it; do not amend.
4. **Verify the branch before each commit:** `git branch --show-current` must print `data-syncs-under-projects`.
5. **Never boot the server against the shared dev database** (`bootRun`, `docker compose … server`). Server verification is unit tests plus Testcontainers `*IntTest`s, which build the schema from the edited changelog. A manual pass on a throwaway database is the user's call and is not part of this plan.
6. **Gradle is judged by its own exit code.** Never pipe Gradle into `tail`/`grep`. Redirect to a log, read `$?` on its own line, then grep the log for `^> Task .* FAILED`. Always pass `--no-daemon` and `--continue`, and export `DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock` for anything that runs `testIntegration`.
7. **Never run two Gradle builds at once in this worktree.** Tasks marked parallel below have disjoint files; run them concurrently only in separate worktrees, otherwise one after another.
8. **The harness may refuse compound shell commands** (pipes, `&&`, `cd` into computed paths, variables in option position). Put multi-step commands in a zsh script under the gitignored `.superpowers/` directory and run the script. Create these two once (Task 1 does it) and reuse them:

   `.superpowers/gradle-run.sh`:
   ```zsh
   #!/bin/zsh
   # usage: .superpowers/gradle-run.sh <log-name> <gradle task> [<gradle task> ...]
   cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/data-syncs-under-projects || exit 1
   [[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"
   export DOCKER_HOST="unix://$HOME/.orbstack/run/docker.sock"
   mkdir -p .superpowers/logs
   log_file=".superpowers/logs/$1.log"
   shift
   ./gradlew --no-daemon --continue "$@" > "$log_file" 2>&1
   exit_code=$?
   echo "exit=$exit_code log=$log_file"
   grep -E '^> Task .* FAILED' "$log_file"
   exit $exit_code
   ```

   `.superpowers/client-run.sh`:
   ```zsh
   #!/bin/zsh
   # usage: .superpowers/client-run.sh <command> [<args> ...]   e.g. npx vitest run src/foo.test.tsx
   cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/data-syncs-under-projects/client || exit 1
   source "$HOME/.nvm/nvm.sh"
   nvm use 22 > /dev/null
   "$@"
   ```

   `chmod +x` both. Client tests need Node 22–24; the script pins 22. `npm run check` needs a Bash timeout of 600000 ms.
9. **Formatting before every commit.** Server: `.superpowers/gradle-run.sh spotless <module>:spotlessApply` for every touched module (or plain `spotlessApply` when many modules changed), then re-stage. Client: `npx prettier --write <touched files>` and `npx eslint --max-warnings=0 <touched files>` through `client-run.sh`; fix `sort-keys` by hand.
10. **After every server phase** (end of Tasks 4, 8, 10, 11) run the whole-repo compile: `.superpowers/gradle-run.sh compile compileJava compileTestJava`. Git merges and cross-module callers break silently; a module-local green run is not enough.
11. **EE remote-client stubs.** This plan adds no new SPI interface in a `*-api` module that distributed EE apps load: `ProjectWorkflowType.isGenerated()` is a method on an existing enum, and the three new data sync beans live in `automation-data-sync-service`, which only `server-app` depends on (`grep -rln automation-data-sync --include='*.kts' .`). If a task nevertheless adds an interface to a `*-api` module that an EE app carries, add a `@Component @ConditionalOnEEVersion` stub throwing `UnsupportedOperationException` in the matching `*-remote-client` module in the same commit.
12. **Adding a constructor collaborator** to a scanned `@Service`/`@Component` breaks other modules' hand-assembled IntTest contexts. After changing a constructor, `grep -rn "<ClassName>" --include='*IntTestConfiguration.java' --include='*TestConfiguration.java' server` and add mock beans where needed.
13. **Deleted classes linger in build output.** After deleting or renaming a class or resource, delete the module's `build/` classes for it (or run the module's `clean`) before trusting a green run; for renamed Liquibase files, delete stale copies under `build/resources/`.
14. **Prove each new test fails first.** Run it before the implementation and record the failure (compilation failure counts only when the failure is the missing symbol under test).
15. **Stop and report** rather than improvise when a path in this plan does not exist, a test the plan says exists is missing, or a step would touch a file outside the task's list.

## File Structure

Abbreviations:

- `CONF-API` = `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration`
- `CONF-API-TEST` = same module, `src/test/java/com/bytechef/automation/configuration`
- `CONF-SVC` = `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration`
- `CONF-SVC-TEST` = same module, `src/test/java/com/bytechef/automation/configuration`
- `CONF-GQL` / `CONF-GQL-TEST` = `server/libs/automation/automation-configuration/automation-configuration-graphql/src/{main,test}/java/com/bytechef/automation/configuration/web/graphql`
- `DS-API` = `server/libs/automation/automation-data-sync/automation-data-sync-api/src/main/java/com/bytechef/automation/datasync`
- `DS-API-TEST` = same module, `src/test/java/com/bytechef/automation/datasync`
- `DS-SVC` = `server/libs/automation/automation-data-sync/automation-data-sync-service/src/main/java/com/bytechef/automation/datasync`
- `DS-SVC-TEST` = same module, `src/test/java/com/bytechef/automation/datasync`
- `DS-CHANGELOG` = `server/libs/automation/automation-data-sync/automation-data-sync-service/src/main/resources/config/liquibase/changelog/automation/data_sync/00000000000001_automation_data_sync_init.xml`
- `DS-GQL` = `server/libs/automation/automation-data-sync/automation-data-sync-graphql/src/main`
- `DS-GQL-TEST` = `server/libs/automation/automation-data-sync/automation-data-sync-graphql/src/test/java/com/bytechef/automation/datasync/web/graphql`
- `MCP-GQL` / `MCP-GQL-TEST` = `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-graphql/src/{main,test}/java/com/bytechef/automation/ai/mcp/web/graphql`
- `EE-CONF-SVC` / `EE-CONF-SVC-TEST` = `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/{main,test}/java/com/bytechef/ee/automation/configuration`
- `AGENT-SVC` = `server/libs/automation/automation-ai/automation-ai-agent/automation-ai-agent-service/src/main/java/com/bytechef/automation/ai/agent` (read-only reference: the pattern every data sync change copies)
- `C` = `client/src`

Gradle module paths:

- `CONF-API-M` = `:server:libs:automation:automation-configuration:automation-configuration-api`
- `CONF-SVC-M` = `:server:libs:automation:automation-configuration:automation-configuration-service`
- `CONF-GQL-M` = `:server:libs:automation:automation-configuration:automation-configuration-graphql`
- `DS-API-M` = `:server:libs:automation:automation-data-sync:automation-data-sync-api`
- `DS-SVC-M` = `:server:libs:automation:automation-data-sync:automation-data-sync-service`
- `DS-GQL-M` = `:server:libs:automation:automation-data-sync:automation-data-sync-graphql`
- `MCP-GQL-M` = `:server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-graphql`
- `MCP-SVC-M` = `:server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service`
- `EE-CONF-SVC-M` = `:server:ee:libs:automation:automation-configuration:automation-configuration-service`

## Task Order and Parallelism

| # | Phase | Task | Depends on | Parallel with |
|---|---|---|---|---|
| 1 | 1 Server schema + domain | `DATA_SYNC` type and `isGenerated()` | — | — |
| 2 | 1 | `data_sync.project_workflow_uuid`, entity/repository/service, tags removed | 1 | 3, 4 (separate worktree) |
| 3 | 2 Configuration generalization | Generated-workflow guard + `validateNotGeneratedWorkflow` | 1 | 2, 4 (separate worktree) |
| 4 | 2 | `isGenerated()` in every listing | 1 | 2, 3 (separate worktree) |
| 5 | 3 Data sync facade move | Create inside ordinary projects; drop `DATA_SYNC_NAME_PREFIX` | 2, 3, 4 | — |
| 6 | 3 | Publish through the project pre-listener | 5 | — |
| 7 | 3 | Delete rules + project delete listener | 6 | — |
| 8 | 3 | Multi-deployment reads and Run now ownership | 7 | — |
| 9 | 4 Content contributor | Export / import / copy / update-from-export facade methods | 8 | — |
| 10 | 4 | `DataSyncProjectContentContributor` | 9 | — |
| 11 | 5 GraphQL | Additive schema + authorization audit | 10 | — |
| 12 | 6 Client GraphQL | Operations, regenerated types, old pages kept green | 11 | — |
| 13 | 7 Client UI | Path util, actions hook, delete dialog, project-aware dialog | 12 | — |
| 14 | 7 | `ProjectItemSelect`: prefixed values + Data Syncs group | 13 | 15, 17 |
| 15 | 7 | Project sidebar Data Syncs tab (355px fit check) | 13 | 14, 17 |
| 16 | 7 | Sync page inside its project | 14, 15 | 17, 18 |
| 17 | 7 | Projects page Data Syncs tab | 13 | 14, 15, 16 |
| 18 | 7 | Data Syncs filter on Projects; filters mutually exclusive | 17 | 16 |
| 19 | 7 | Deployments page Data Syncs tab + Run now + filter | 18 | 16 |
| 20 | 7 | Remove old pages, routes, nav rows, Build group | 16, 19 | — |
| 21 | 8 Docs | `.agents/data-sync.md`, `.agents/agents.md`, CLAUDE.md, spec pointer | 20 | — |
| 22 | 9 Final verification | Whole-repo compile, checks, every touched module's tests | 21 | — |

Client tasks may run concurrently in one worktree (Vitest and `tsc` do not lock the tree) as long as their file lists are disjoint, which the table guarantees; commit each by path.

---

## Phase 1 — Server schema and domain

### Task 1: `ProjectWorkflowType.DATA_SYNC` and `isGenerated()`

**Files:**
- Modify: `CONF-API/domain/ProjectWorkflowType.java`
- Create: `CONF-API-TEST/domain/ProjectWorkflowTypeTest.java`
- Modify (test): `CONF-SVC-TEST/service/ProjectWorkflowServiceIntTest.java`
- Create (gitignored, not committed): `.superpowers/gradle-run.sh`, `.superpowers/client-run.sh` (Execution Notes §8)

**Interfaces produced:** `ProjectWorkflowType.DATA_SYNC` (ordinal 2); `boolean ProjectWorkflowType.isGenerated()`.

- [ ] **Step 1: Failing tests.** `ProjectWorkflowTypeTest`:
  - `testOrdinalsArePersistedValues` — `WORKFLOW.ordinal() == 0`, `AI_AGENT.ordinal() == 1`, `DATA_SYNC.ordinal() == 2`.
  - `testOnlyWorkflowIsNotGenerated` — `WORKFLOW.isGenerated()` false; `AI_AGENT`, `DATA_SYNC` true.

  `ProjectWorkflowServiceIntTest`: add `testPublishWorkflowKeepsDataSyncTypeOnBothRows`, the `DATA_SYNC` twin of the existing `testPublishWorkflowKeepsAgentTypeOnBothRows` (reads ordinal 2 back through `toEnum`).
- [ ] **Step 2: Run to see them fail** — `.superpowers/gradle-run.sh t1-red CONF-API-M:test --tests "*ProjectWorkflowTypeTest"` → compilation failure on `DATA_SYNC`.
- [ ] **Step 3: Implement.**

  ```java
  /**
   * What a {@code project_workflow} row holds. Generated rows ({@link #isGenerated()}) are produced from a feature's own
   * configuration — an AI agent, a Data Sync — and are edited only through that feature, never through the workflow
   * editor, so workflow listings leave them out.
   *
   * @author Ivica Cardic
   */
  public enum ProjectWorkflowType {

      // Persisted as INT ordinal - append new values at the end only.
      WORKFLOW, AI_AGENT, DATA_SYNC;

      /** Whether rows of this type are generated from a feature's own configuration and edited only through it. */
      public boolean isGenerated() {
          return this != WORKFLOW;
      }
  }
  ```
- [ ] **Step 4: Verify** — `.superpowers/gradle-run.sh t1 CONF-API-M:test CONF-SVC-M:testIntegration --tests "*ProjectWorkflowServiceIntTest"`; exit 0.
- [ ] **Step 5: Commit** — spotlessApply on both modules; `git commit -m "Add the DATA_SYNC project workflow type and the isGenerated predicate" -- <the three files>`.

---

### Task 2: `data_sync.project_workflow_uuid`, entity, repository, service; tags removed

The entity change forces every caller in the data sync module to change in the same commit, so this task also moves the facade, security providers, DTOs and GraphQL onto the new resolution **mechanically** — hidden-project creation stays until Task 5.

**Files:**
- Modify: `DS-CHANGELOG`
- Modify: `DS-API/domain/DataSync.java`; Delete: `DS-API/domain/DataSyncTag.java`
- Modify: `DS-API/dto/DataSyncDTO.java` (drop `tags`), `DS-API/dto/DataSyncDeploymentDTO.java` (drop `tags`)
- Modify: `DS-API/service/DataSyncService.java`, `DS-SVC/service/DataSyncServiceImpl.java`, `DS-SVC/repository/DataSyncRepository.java`
- Modify: `DS-API/facade/DataSyncFacade.java`, `DS-SVC/facade/DataSyncFacadeImpl.java` (remove `getDataSyncTags`, `updateDataSyncTags`, `getDataSyncDeploymentTags`, `updateDataSyncDeploymentTags`, the `TagService` collaborator; replace every `dataSync.getProjectId()`)
- Modify: `DS-SVC/security/DataSyncVisibilityProvider.java`, `DataSyncElementVisibilityProvider.java` (and the two ownership resolvers only if they read `getProjectId`)
- Modify: `DS-GQL/java/com/bytechef/automation/datasync/web/graphql/DataSyncGraphQlController.java`, `DS-GQL/resources/graphql/data-sync.graphqls` (remove `dataSyncTags`, `dataSyncDeploymentTags`, `updateDataSyncTags`, `updateDataSyncDeploymentTags`, `UpdateDataSyncTagsInput`, `UpdateDataSyncDeploymentTagsInput`, `DataSync.tags`, `DataSyncDeployment.tags`; `TagInput` record)
- Tests: `DS-API-TEST/domain/DataSyncTest.java`, `DS-SVC-TEST/service/DataSyncServiceIntTest.java`, `DS-SVC-TEST/facade/DataSyncFacadeIntTest.java`, `DS-SVC-TEST/facade/DataSyncFacadeAuthorizationTest.java`, `DS-SVC-TEST/security/DataSyncVisibilityProvidersTest.java`, `DS-SVC-TEST/security/DataSyncOwnershipResolversTest.java`, `DS-GQL-TEST/DataSyncGraphQlControllerTest.java`, `DS-GQL-TEST/config/*` if they mock tag beans, `server/libs/automation/automation-data-sync/automation-data-sync-graphql/src/test/resources/graphql/test.graphqls` if it declares `Tag`/`TagInput` only for data sync

**Interfaces produced:**
- `UUID DataSync.getProjectWorkflowUuid()`, `void DataSync.setProjectWorkflowUuid(UUID)`; `getProjectId`/`setProjectId`/`getTagIds`/`setTags` gone.
- `DataSyncService`: `long getProjectId(DataSync dataSync)`; `Map<Long, Long> getProjectIds(Collection<DataSync> dataSyncs)` (sync id → project id, one query); `List<DataSync> getProjectDataSyncs(long projectId)`. `update(long id, List<Long> tagIds)` removed.
- `DataSyncRepository`: `findAllByProjectId(long)`, `findProjectIdByProjectWorkflowUuid(UUID)`, `findProjectIdsByProjectWorkflowUuids(Collection<UUID>)` + `ProjectWorkflowProject` record and row mapper — copied from `AGENT-SVC/repository/AiAgentRepository.java` with `ai_agent` → `data_sync`. `findByProjectId` removed.

- [ ] **Step 1: Failing tests.**
  - `DataSyncServiceIntTest`: rewrite the fixture (it names a project `"__DATA_SYNC__" + UUID` at :130 — use a plain name) so each sync is saved with a `project_workflow_uuid` of a `ProjectWorkflow` added to a project. New: `testGetProjectIdResolvesThroughProjectWorkflow`, `testGetProjectIdsBatchesDistinctProjects`, `testGetProjectDataSyncsReturnsOnlyThatProjectsSyncs` (two projects, three syncs), `testProjectWorkflowUuidIsUnique` (second save with the same uuid → `DataIntegrityViolationException`).
  - `DataSyncTest`: drop tag assertions; add a round trip for `projectWorkflowUuid`.
  - `DataSyncVisibilityProvidersTest`: project resolved through `DataSyncService.getProjectId`; `testSyncWhoseWorkflowIsGoneResolvesToNoRecord` (service throws/empty → empty visibility record, fail closed).
  - `DataSyncFacadeAuthorizationTest`: delete the four tag expectations; lower the pinned count by 4.
- [ ] **Step 2: Run to see them fail** — `.superpowers/gradle-run.sh t2-red DS-SVC-M:testIntegration --tests "*DataSyncServiceIntTest"`.
- [ ] **Step 3: Changelog, in place.** In `DS-CHANGELOG`: remove `project_id` and `fk_data_sync_project` from `data_sync`; add
  ```xml
  <column name="project_workflow_uuid" type="${uuid_type}">
      <constraints nullable="false" unique="true" uniqueConstraintName="uk_data_sync_project_workflow_uuid"/>
  </column>
  ```
  Remove `data_sync_tag` entirely — `createTable`, primary key, both foreign keys and its `dropTable` in `<rollback>`. `data_sync_element` unchanged. Delete `server/libs/automation/automation-data-sync/automation-data-sync-service/build/resources/main/config/liquibase/changelog/automation/data_sync/` if present.
- [ ] **Step 4: Entity, repository, service.** `DataSync`: replace `projectId` with `@Column("project_workflow_uuid") private UUID projectWorkflowUuid;`, delete the `@MappedCollection` tags, `getTagIds`, `setTags`, and update `equals`/`toString`. Delete `DataSyncTag.java`. Repository and service as in the interfaces above; `getProjectId` throws `IllegalStateException("Data Sync %d has no project workflow")` when the uuid resolves to nothing (the visibility provider catches it and fails closed); `getProjectIds` returns an empty map for an empty input without querying.
- [ ] **Step 5: Mechanical facade/security/GraphQL moves.**
  - Every `dataSync.getProjectId()` in `DataSyncFacadeImpl` → `dataSyncService.getProjectId(dataSync)`; `visibleDataSyncs` uses `getProjectIds` once.
  - `createDataSync` keeps the hidden project for now, but reorders to the agent shape: create a placeholder workflow (`JsonUtils.write(Map.of("label", name, "tasks", List.of()))`), `projectWorkflowService.addWorkflow(savedProject.getId(), savedProject.getLastProjectVersion(), workflow.getId(), ProjectWorkflowType.DATA_SYNC)`, set `projectWorkflowUuid`, save the row, then `regenerateAndSaveWorkflow(savedDataSync)`.
  - Remove the four tag methods, `toDataSyncDTO`'s tags, the deployment DTO's tags, and `TagService` from the constructor and from `AutomationDataSyncIntTestConfiguration` if it declares it.
  - `DataSyncVisibilityProvider` / `DataSyncElementVisibilityProvider`: resolve the project through `DataSyncService.getProjectId`.
  - Controller: drop the four tag mappings, `DataSyncPayload.tags()`, `TagInput`, both tag inputs; schema edits listed under Files. Controller test: drop tag cases and tag fields from fixtures.
- [ ] **Step 6: Verify.**
  ```
  .superpowers/gradle-run.sh t2 DS-API-M:test DS-SVC-M:test DS-SVC-M:testIntegration DS-GQL-M:test
  ```
  Exit 0. `DataSyncFacadeIntTest` must still pass unchanged in behaviour (hidden project, tags gone).
- [ ] **Step 7: Commit** — spotlessApply on the three data sync modules; commit by path: `"Link data syncs to their generated project workflow and drop data sync tags"`.

---

## Phase 2 — Configuration-layer generalization

The spec's 11 sites: the guard, `ProjectWorkflowFacadeImpl` (validator + two listings), `ProjectFacadeImpl` (three), `ProjectDeploymentFacadeImpl`, `SubflowDataSourceImpl`, `ProjectSearchAssetProvider`, `WorkflowSearchAssetProvider`, `ProjectWorkflowGraphQlController`, `McpProjectWorkflowGraphQlController`, EE `ProjectGitFacadeImpl`. Tasks 3 and 4 split them by file.

### Task 3: `GeneratedProjectWorkflowUpdateGuard` and `validateNotGeneratedWorkflow`

**Files:**
- Rename (`git mv`): `CONF-SVC/workflow/AiAgentWorkflowUpdateGuard.java` → `CONF-SVC/workflow/GeneratedProjectWorkflowUpdateGuard.java`
- Rename (`git mv`): `CONF-SVC-TEST/workflow/AiAgentWorkflowUpdateGuardTest.java` → `CONF-SVC-TEST/workflow/GeneratedProjectWorkflowUpdateGuardTest.java`
- Modify: `CONF-SVC/facade/ProjectWorkflowFacadeImpl.java` (`validateNotAiAgentWorkflow` at :224 and its four call sites :183, :252, :274, :584; listings :421, :440)
- Tests: `CONF-SVC-TEST/facade/ProjectWorkflowFacadeIntTest.java`, `CONF-SVC-TEST/facade/ProjectWorkflowFacadeVisibilityTest.java` (only if its mocks need the new type)

**Interfaces produced:** `public final class GeneratedProjectWorkflowUpdateGuard implements WorkflowUpdateGuard` (becomes `public` so the facade can share its message) with
```java
public static String generatedWorkflowMessage(String workflowId, ProjectWorkflowType type) {
    return switch (type) {
        case AI_AGENT -> "Workflow %s is generated by an AI agent; edit it through the agent".formatted(workflowId);
        case DATA_SYNC -> "Workflow %s is generated by a Data Sync; edit it through the Data Sync".formatted(workflowId);
        case WORKFLOW -> throw new IllegalArgumentException("Workflow %s is not generated".formatted(workflowId));
    };
}
```
The switch is exhaustive on purpose: a future generated type fails compilation here until it has a message. This is the "guard's message table" the spec's grep excludes.

- [ ] **Step 1: Failing tests.** `GeneratedProjectWorkflowUpdateGuardTest`: `testRefusesAiAgentWorkflowNamingTheAgent`, `testRefusesDataSyncWorkflowNamingTheDataSync`, `testAllowsOrdinaryWorkflow`, `testAllowsUnknownWorkflowId`. `ProjectWorkflowFacadeIntTest`: beside the two existing `AI_AGENT` cases (~:473, :491) add `DATA_SYNC` twins for update, delete and duplicate asserting the "Data Sync" message; add `testGetProjectWorkflowsLeavesOutDataSyncWorkflows` (both overloads) and `testGetProjectVersionWorkflowsIncludesGeneratedWorkflows` (a `WORKFLOW`, an `AI_AGENT` and a `DATA_SYNC` row all present).
- [ ] **Step 2: Run to see them fail** — `.superpowers/gradle-run.sh t3-red CONF-SVC-M:test --tests "*GeneratedProjectWorkflowUpdateGuardTest"`.
- [ ] **Step 3: Implement.** Guard body per the spec (`.filter(projectWorkflow -> projectWorkflow.getType().isGenerated())`, throw `IllegalArgumentException(generatedWorkflowMessage(workflowId, projectWorkflow.getType()))`); Javadoc names both owners. Rename `validateNotAiAgentWorkflow` → `validateNotGeneratedWorkflow` with the same filter and `GeneratedProjectWorkflowUpdateGuard.generatedWorkflowMessage`; the two listing filters become `!projectWorkflow.getType().isGenerated()`. `grep -rn "AiAgentWorkflowUpdateGuard" server` must return nothing.
- [ ] **Step 4: Verify** — `.superpowers/gradle-run.sh t3 CONF-SVC-M:test CONF-SVC-M:testIntegration --tests "*ProjectWorkflowFacade*"` then the full `CONF-SVC-M:test`. Exit 0.
- [ ] **Step 5: Commit** — `"Refuse edits to every generated project workflow"`.

---

### Task 4: `isGenerated()` in every remaining listing

**Files (main):**
- `CONF-SVC/facade/ProjectFacadeImpl.java` (:668, :950, :1010)
- `CONF-SVC/facade/ProjectDeploymentFacadeImpl.java` (:537, chat workflows)
- `CONF-SVC/subflow/SubflowDataSourceImpl.java` (:137)
- `CONF-SVC/search/ProjectSearchAssetProvider.java` (:65), `CONF-SVC/search/WorkflowSearchAssetProvider.java` (:63)
- `CONF-GQL/ProjectWorkflowGraphQlController.java` (:120)
- `MCP-GQL/McpProjectWorkflowGraphQlController.java` (:113)
- `EE-CONF-SVC/facade/ProjectGitFacadeImpl.java` (:213)

**Files (test)** — add one `DATA_SYNC` case next to each existing `AI_AGENT` case:
- `CONF-SVC-TEST/facade/ProjectFacadeIntTest.java` (near :1139): `testWorkspaceLatestProjectWorkflowsLeaveOutDataSyncWorkflows`, and the duplicate/export path (`getOrdinaryProjectWorkflows`) leaving it out; `projectWorkflowIds` counts user workflows only
- `CONF-SVC-TEST/facade/ProjectDeploymentFacadeChatWorkflowTest.java` (:191): `testChatWorkflowsLeaveOutDataSyncWorkflows`
- `CONF-SVC-TEST/subflow/SubflowDataSourceTest.java` (:244): `testSubWorkflowsLeaveOutDataSyncWorkflows`
- `CONF-SVC-TEST/search/ProjectSearchAssetProviderTest.java`, `CONF-SVC-TEST/search/WorkflowSearchAssetProviderTest.java`
- `CONF-GQL-TEST/ProjectWorkflowGraphQlControllerErrorWorkflowTest.java` (:128)
- `MCP-GQL-TEST/McpProjectWorkflowGraphQlControllerTest.java` (:56)
- `EE-CONF-SVC-TEST/facade/ProjectGitFacadeTest.java` (:100): a `DATA_SYNC` row is not pushed as a workflow file

- [ ] **Step 1: Failing tests** as listed; run `.superpowers/gradle-run.sh t4-red CONF-SVC-M:test` and confirm the new cases fail.
- [ ] **Step 2: Implement.** Replace each `getType() == ProjectWorkflowType.WORKFLOW` with `!getType().isGenerated()` and `SubflowDataSourceImpl`'s `== AI_AGENT` with `isGenerated()`; drop now-unused imports. Acceptance grep (the spec's): `grep -rn "ProjectWorkflowType.AI_AGENT" server --include='*.java' | grep -v /src/test/ | grep -v automation-ai-agent | grep -v GeneratedProjectWorkflowUpdateGuard` returns nothing (the grep runs in a `.superpowers/` script — it is a pipeline).
- [ ] **Step 3: Verify.**
  ```
  .superpowers/gradle-run.sh t4 CONF-SVC-M:test CONF-SVC-M:testIntegration CONF-GQL-M:test MCP-GQL-M:test EE-CONF-SVC-M:test
  .superpowers/gradle-run.sh t4-compile compileJava compileTestJava
  ```
  Both exit 0.
- [ ] **Step 4: Commit** — spotlessApply on the five modules; `"Leave every generated workflow out of workflow listings"`.

---

## Phase 3 — Data sync service and facade move

`AutomationDataSyncIntTestConfiguration` deliberately does not load `ProjectFacadeImpl`. Tests that need a project publish use a private helper replicating `ProjectFacadeImpl.publishProject`'s loop (the `publishProject(…)` helper in the agents plan's Task 12, Step 3); tests of project delete call `deleteProjectDataSyncs` directly and test the listener by unit test. `ProjectServiceImpl` finds pre-publish listeners through the application context, so the slice's component scan of `com.bytechef.automation.datasync` wires `DataSyncProjectPublishPreListener` for real.

### Task 5: Create data syncs inside ordinary projects

**Files:**
- Modify: `DS-API/facade/DataSyncFacade.java`, `DS-SVC/facade/DataSyncFacadeImpl.java` (`createDataSync`, `regenerateAndSaveWorkflow`, `toDataSyncDTO`, `getDataSyncVersions`; new private `createDataSyncProject`, `getWorkspaceProject`, `uniqueProjectName`, `draftProjectWorkflow`)
- Modify: `DS-API/dto/DataSyncDTO.java` — add `long projectId`, `@Nullable Instant lastModifiedDate`
- Modify: `DS-GQL/java/.../DataSyncGraphQlController.java` — `createDataSync` passes `null` for the project (wired in Task 11); `DataSyncPayload.projectId()` / `lastModifiedDate()` read the DTO
- Modify: `CONF-API/domain/SystemProjects.java` (delete `DATA_SYNC_NAME_PREFIX`, drop it from `NAME_PREFIXES`, reword "a uuid for Data Sync" in the class Javadoc), `CONF-API/facade/ProjectFacade.java` (:63 Javadoc), `CONF-SVC/facade/ProjectWorkflowFacadeImpl.java` (:213 Javadoc)
- Tests switching the sample prefix to `KNOWLEDGE_BASE_NAME_PREFIX` or `CONTEXT_STORE_NAME_PREFIX`: `CONF-API-TEST/domain/SystemProjectsTest.java`, `CONF-SVC-TEST/facade/ProjectDeploymentFacadeTest.java` (:324), `CONF-SVC-TEST/facade/ProjectFacadeRowVisibilityTest.java` (:117), `CONF-SVC-TEST/service/ProjectDeploymentServiceSystemProjectIntTest.java` (:93), `CONF-SVC-TEST/service/ProjectServiceIntTest.java` (:175-184), `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-service/src/test/java/com/bytechef/automation/ai/mcp/facade/WorkflowDeleteCascadeIntTest.java` (:186)
- Tests: `DS-SVC-TEST/facade/DataSyncFacadeIntTest.java`, `DS-SVC-TEST/facade/DataSyncFacadeAuthorizationTest.java`, `DS-GQL-TEST/DataSyncGraphQlControllerTest.java`

**Interfaces produced:** `DataSyncDTO createDataSync(String title, @Nullable String description, long workspaceId, @Nullable Long projectId)`.

- [ ] **Step 1: Failing tests** (`DataSyncFacadeIntTest`; replace `:162`'s prefix assertion):
  - `testCreateWithoutProjectCreatesOrdinaryProjectNamedAfterTitle` — no prefix, description is the sync's, `SystemProjects.isSystemProject` false, project workflow type `DATA_SYNC`.
  - `testCreateWithoutProjectSuffixesCollidingProjectName` — second "Orders" → "Orders (2)".
  - `testCreateInExistingProjectAddsDataSyncWorkflow` — project keeps its ordinary workflow; `getProjectWorkflowIds` has both.
  - `testCreateInProjectOfAnotherWorkspaceIsRefused` → `IllegalArgumentException`.
  - `testTwoSyncsInOneProjectResolveTheirOwnDraftWorkflows` — two syncs + one `AI_AGENT`-typed row + one ordinary workflow; distinct `draftWorkflowId`s; each regenerated definition carries its own `metadata.dataSyncId`.
  - `testSyncAddedAfterPublishReportsUnpublished` — publish the project (helper), add a second sync: `lastPublishedVersion == 0`, `unpublishedChanges`.
  - `testLastModifiedDateFollowsDraftWorkflow` — an element edit moves `lastModifiedDate` though the `data_sync` row is untouched.
  - `ProjectServiceIntTest`: `testFormerDataSyncPrefixIsNoLongerSystem` — a project named `__DATA_SYNC__Sync` appears in listings.
  - Authorization test: `createDataSync` pinned with `(String, String, long, Long)`.
- [ ] **Step 2: Run to see them fail** — `.superpowers/gradle-run.sh t5-red DS-SVC-M:testIntegration --tests "*DataSyncFacadeIntTest"`.
- [ ] **Step 3: Implement.** Copy `AGENT-SVC/facade/AiAgentFacadeImpl.java`'s `createAgentProject`, `getWorkspaceProject` and `uniqueProjectName` (:323-362) as private methods — no shared helper: the two facades live in unrelated modules and the natural shared home (`ProjectService`) would widen a released API for three short methods. The draft workflow is `projectWorkflowService.fetchProjectWorkflow(projectId, project.getLastProjectVersion(), uuid)`; `toDataSyncDTO` compares the draft with `fetchProjectWorkflow(projectId, lastPublishedVersion, uuid)` and reports `lastPublishedVersion = 0`, `unpublishedChanges = true` when the published version lacks the uuid (`toAgentDTO`'s rule). `lastModifiedDate` = draft workflow's `getLastModifiedDate()`, falling back to the row's. `getVersionWorkflowId` is no longer called from create/regenerate/read paths (it survives only in `runDataSyncDeployment`/`toDeploymentDTO` until Task 8). Enforce `data_sync.workspace_id == project.workspaceId` (it is, by construction; assert it in `getWorkspaceProject`'s use).
- [ ] **Step 4: Verify.**
  ```
  .superpowers/gradle-run.sh t5 CONF-API-M:test CONF-SVC-M:test CONF-SVC-M:testIntegration MCP-SVC-M:testIntegration DS-SVC-M:test DS-SVC-M:testIntegration DS-GQL-M:test
  ```
  Exit 0; `grep -rn "DATA_SYNC_NAME_PREFIX\|__DATA_SYNC__" server --include='*.java'` returns only `ProjectServiceIntTest`'s literal.
- [ ] **Step 5: Commit** — `"Create data syncs inside ordinary projects"`.

---

### Task 6: Publish through the project

**Files:**
- Create: `DS-SVC/event/DataSyncProjectPublishPreListener.java`
- Create: `DS-SVC-TEST/event/DataSyncProjectPublishPreListenerTest.java`
- Modify: `DS-API/facade/DataSyncFacade.java`, `DS-SVC/facade/DataSyncFacadeImpl.java` (remove `publishDataSync` and `publishProjectVersion`; add `prepareProjectPublish`; `validateForPublish` messages name the sync)
- Modify: `DS-GQL/java/.../DataSyncGraphQlController.java`, `DS-GQL/resources/graphql/data-sync.graphqls` (remove `publishDataSync`)
- Tests: `DataSyncFacadeIntTest`, `DataSyncFacadeAuthorizationTest`, `DataSyncGraphQlControllerTest`

**Interfaces produced:** `void DataSyncFacade.prepareProjectPublish(long projectId)`, gated `@PreAuthorize("hasPermission(#projectId, 'Project', 'DATA_SYNC_PUBLISH')")`, `@Transactional`.

- [ ] **Step 1: Failing tests.**
  - `DataSyncProjectPublishPreListenerTest` (Mockito): `testSkipsProjectWithoutDataSyncs` (`getProjectDataSyncs` empty → `verifyNoInteractions(dataSyncFacade)`), `testPreparesProjectWithDataSyncs`.
  - `DataSyncFacadeIntTest`: `testUnfinishedSyncBlocksProjectPublishNamingIt` — `projectService.publishProject` throws `ConfigurationException` with `SOURCE_MISSING` and a message containing `"Data Sync 'Orders to warehouse' has no source."`; the same for destination, connection (`"Element SOURCE of Data Sync '…' needs a connection."`) and cron (`"Data Sync '…' is scheduled but has no cron expression."`). `testProjectPublishRegeneratesSyncsInPlace` — publish adds no `project_workflow` rows beyond the helper's duplicates, and the published copy carries the latest trigger. Replace every `publishDataSync` call with the publish helper.
  - Authorization test: `prepareProjectPublish(long)` → `DATA_SYNC_PUBLISH` on `Project`; `publishDataSync` removed; count unchanged (−1 +1).
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.** Listener mirrors `AGENT-SVC/event/AiAgentProjectPublishPreListener.java` with a `DataSyncService` for the emptiness check and `@Lazy DataSyncFacade`:
  ```java
  @Override
  public void onBeforePublishProject(long projectId) {
      if (dataSyncService.getProjectDataSyncs(projectId)
          .isEmpty()) {

          return;
      }

      dataSyncFacade.prepareProjectPublish(projectId);
  }
  ```
  `prepareProjectPublish` loops `getProjectDataSyncs`: `validateForPublish` then `regenerateAndSaveWorkflow`, never adding or removing rows. Messages use `"Data Sync '" + dataSync.getTitle() + "'"`. Add `@SuppressFBWarnings("EI_EXPOSE_REP2")` on the listener constructor if SpotBugs asks.
- [ ] **Step 4: Verify** — `.superpowers/gradle-run.sh t6 DS-SVC-M:test DS-SVC-M:testIntegration DS-GQL-M:test`; exit 0.
- [ ] **Step 5: Commit** — `"Publish data syncs with their project"`.

---

### Task 7: Delete within and with the project

**Files:**
- Create: `DS-SVC/event/DataSyncProjectDeleteEventListener.java`, `DS-SVC-TEST/event/DataSyncProjectDeleteEventListenerTest.java`
- Modify: `DS-API/facade/DataSyncFacade.java`, `DS-SVC/facade/DataSyncFacadeImpl.java` (`deleteDataSync`; remove `hasAnyDeployment`; add `deleteProjectDataSyncs`, private `isEnabledInAnyDeployment`, `projectDeployments`, `dataSyncDeploymentWorkflows`)
- Modify: `DS-API/exception/DataSyncErrorType.java` only if it carries message text for `DATA_SYNC_HAS_DEPLOYMENTS`
- Tests: `DataSyncFacadeIntTest`, `DataSyncFacadeAuthorizationTest`

**Interfaces produced:** `void DataSyncFacade.deleteProjectDataSyncs(long projectId)` — `@PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_DELETE')")`, `@Transactional`.

- [ ] **Step 1: Failing tests** (`DataSyncFacadeIntTest`):
  - `testDeleteKeepsProjectSiblingSyncAndWorkflows` — project with two syncs and an ordinary workflow, published once; delete one sync: project present, sibling sync and workflow intact, every version's row for the deleted sync gone, its test configurations gone.
  - `testDeleteRefusedWhileEnabledInADeployment` → `DATA_SYNC_HAS_DEPLOYMENTS`, message `"… while it is enabled in a deployment"`.
  - `testDeleteAllowedOnceDisabledRemovesDisabledDeploymentRow` — the disabled `project_deployment_workflow` row is gone afterwards.
  - `testDeleteChecksEveryDeploymentOfAnEnvironment` — two deployments in one environment, only the second enabled → refused; no `IncorrectResultSizeDataAccessException`.
  - `testDeleteProjectDataSyncsRemovesTheProjectsRows` — leaves another project's sync.
  - `DataSyncProjectDeleteEventListenerTest`: delegates to `deleteProjectDataSyncs(projectId)`.
  - Authorization test: `deleteProjectDataSyncs(long)` → `PROJECT_DELETE` on `Project`; count +1.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.** Copy `AiAgentFacadeImpl.deleteAgent`'s body (:366-419, minus the sub-agent guard) and `isEnabledInAnyDeployment` / `projectDeployments` / `agentDeploymentWorkflows` (:453-503), renamed. The project is never deleted. `deleteProjectDataSyncs` deletes the rows only (elements cascade); `ProjectFacadeImpl.deleteProject` removes workflows and deployments afterwards. Listener: `@Lazy DataSyncFacade`, same shape as `AGENT-SVC/event/AiAgentProjectDeleteEventListener.java`.
- [ ] **Step 4: Verify** — `.superpowers/gradle-run.sh t7 DS-SVC-M:test DS-SVC-M:testIntegration`; exit 0.
- [ ] **Step 5: Commit** — `"Delete data syncs within their project and with their project"`.

---

### Task 8: Multi-deployment reads and Run now ownership

**Files:**
- Modify: `DS-API/dto/DataSyncDeploymentDTO.java` (field order per the spec: `id`, `name`, `dataSyncId`, `dataSyncTitle`, `projectId`, `environmentId`, `enabled`, `projectVersion`, `workflowId`, `triggerType`, `lastExecutionDate`)
- Modify: `DS-SVC/facade/DataSyncFacadeImpl.java` (`getDataSyncDeployments`, `toDeploymentDTO`, `runDataSyncDeployment`; delete `getVersionWorkflowId`)
- Modify: `DS-GQL/java/.../DataSyncGraphQlController.java` only if it reads DTO components by position
- Modify: `EE-CONF-SVC-TEST/security/ResourceEnvironmentResolverCoverageTest.java` (:121 justification: name `isEnabledInAnyDeployment` and `syncTestConnections`, not `hasAnyDeployment`/`getDataSyncDeployments`)
- Tests: `DataSyncFacadeIntTest`, `DS-GQL-TEST/DataSyncGraphQlControllerTest.java`

- [ ] **Step 1: Failing tests** (`DataSyncFacadeIntTest`; `ProjectDeploymentFacade` is already a mock in the slice — reuse it):
  - `testTwoDeploymentsInOneEnvironmentAreBothListed` — no exception, two DTOs with their own ids and the sync's own `workflowId`.
  - `testDeploymentsListOnlyVersionsContainingTheSync` — a deployment pinned to a version before the sync existed is not listed.
  - `testDeploymentTriggerTypeIsTheDeployedTrigger` — deploy while `MANUAL`, switch the row to `SCHEDULE`: DTO still `MANUAL`.
  - `testRunResolvesTheSyncsWorkflowAmongSeveral` — verify `createProjectDeploymentWorkflowJob(deploymentId, <sync's deployed workflow id>)`.
  - `testRunRefusesForeignDeployment` and `testRunRefusesDeploymentPinnedBeforeTheSync` → `DEPLOYMENT_NOT_OWNED`, message `"Deployment N does not deploy Data Sync 'title'"`.
  - `testRunRefusesDisabledDeployment` and `testRunRefusesDisabledSyncRow` → `DEPLOYMENT_DISABLED`.
  - `testRunIsCalledOutsideATransaction` — the mock's `Answer` records `TransactionSynchronizationManager.isActualTransactionActive()`; assert false.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.** `getDataSyncDeployments`: for each visible sync, `projectDeploymentService.getAllProjectDeployments(projectId)`; per deployment, `fetchProjectWorkflowWorkflowId(deploymentId, uuid)` → skip when empty or when no `project_deployment_workflow` row carries that workflow id. `triggerType` = `SCHEDULE` iff the deployed workflow's trigger type is `schedule/v1/cron` (read the definition's trigger through `WorkflowService.getWorkflow(workflowId)`), else `MANUAL`. `runDataSyncDeployment` follows the spec's three steps, stays un-`@Transactional`, and keeps its Javadoc on why. `getLastExecutionDate` uses the sync's own row only. `grep -n "fetchProjectDeployment(" DS-SVC` returns nothing.
- [ ] **Step 4: Verify.**
  ```
  .superpowers/gradle-run.sh t8 DS-SVC-M:test DS-SVC-M:testIntegration DS-GQL-M:test EE-CONF-SVC-M:test --tests "*ResourceEnvironmentResolverCoverageTest"
  .superpowers/gradle-run.sh t8-compile compileJava compileTestJava
  ```
  (Split the first line into two invocations if `--tests` applying to all listed tasks filters too much.) Both exit 0.
- [ ] **Step 5: Commit** — `"Resolve data sync deployments and Run now through the sync's own workflow"`.

---

## Phase 4 — Content contributor

### Task 9: Export, import, copy and update-from-export

**Files:**
- Modify: `DS-API/facade/DataSyncFacade.java`, `DS-SVC/facade/DataSyncFacadeImpl.java`
- Tests: `DataSyncFacadeIntTest`, `DataSyncFacadeAuthorizationTest`

**Interfaces produced** (Javadoc on the interface states that connections are never exported):
- `String exportDataSync(long id)` — `hasPermission(#id, 'DataSync', 'DATA_SYNC_VIEW')`, read-only
- `DataSyncDTO importDataSync(long workspaceId, String json, @Nullable Long projectId)` — `hasPermission(#workspaceId, 'Workspace', 'DATA_SYNC_CREATE')`; `null` project creates one, as `importAgent` does
- `List<DataSyncDTO> copyProjectDataSyncs(long sourceProjectId, long targetProjectId, long workspaceId)` — `DATA_SYNC_CREATE` on `Workspace`
- `DataSyncDTO updateDataSyncFromExport(long id, String json)` — `DATA_SYNC_EDIT` by id

- [ ] **Step 1: Failing tests.**
  - `testExportDocumentShape` — `exportVersion: 1`, `name`, `title`, `description`, `triggerType`/`triggerParameters`, elements ordered SOURCE, DESTINATION, PROCESSOR, enums by name, pretty-printed, **no** `connectionId` anywhere.
  - `testImportRecreatesWithoutConnections` — exported name seeds `uniqueName`; elements created with null `connection_id`; draft generated; a connection-requiring component blocks the next project publish naming the sync.
  - `testImportRejectsNonFieldMapperProcessor` → `PROCESSOR_NOT_FIELD_MAPPER`.
  - `testCopyProjectDataSyncsKeepsConnections` — new rows, new names, new `DATA_SYNC` workflows in the target draft, test connections synced.
  - `testUpdateFromExportUpsertsByKind` — title/description/trigger replaced; unchanged element keeps its `connection_id`; changed component/version/operation → null; a kind missing from the file is deleted.
  - Authorization test: four new pins; count +4 (17 total now).
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement**, shaped on `AiAgentFacadeImpl.exportAgent` / `importAgent` / `copyProjectAgents` / `updateAgentFromExport` (:548-762). Import goes through the private `createDataSync` path so the project and draft are created like any other sync; an unsupported `exportVersion` → `IllegalArgumentException`. `workspace_id` of the created row equals the target project's workspace (`getWorkspaceProject`).
- [ ] **Step 4: Verify** — `.superpowers/gradle-run.sh t9 DS-SVC-M:test DS-SVC-M:testIntegration`; exit 0.
- [ ] **Step 5: Commit** — `"Add data sync export, import, copy and update-from-export"`.

---

### Task 10: `DataSyncProjectContentContributor`

**Files:**
- Create: `DS-SVC/event/DataSyncProjectContentContributor.java`
- Create: `DS-SVC-TEST/event/DataSyncProjectContentContributorIntTest.java`
- Modify (test): `EE-CONF-SVC-TEST/facade/ProjectGitFacadeTest.java`

- [ ] **Step 1: Failing tests.** `DataSyncProjectContentContributorIntTest` (use `AGENT-SVC`'s `AiAgentProjectContentContributorIntTest` as the model):
  - `testContentDirectoryIsDataSyncs` → `"data-syncs/"`.
  - `testDuplicateCopiesSyncsWithConnections`.
  - `testExportWritesOneFilePerSyncWithoutConnectionIds` — keys `data-syncs/<name>.json`.
  - `testImportRecreatesSyncsWithoutConnections`; nested paths (`data-syncs/x/y.json`) and non-`.json` entries ignored.
  - `testPullUpdatesByNameThenUniqueTitle`, `testPullKeepsConnectionsOfUnchangedElements`, `testPullImportsUnmatchedFiles`, `testPullLeavesSyncsWithoutAFileAlone`.
  - `ProjectGitFacadeTest`: `testPushWritesDataSyncContributorFiles` and `testPullHandsDataSyncFilesToTheContributor` with a mocked contributor whose directory is `data-syncs/`.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.** `@Component`, collaborators `@Lazy` (the reason `AiAgentProjectContentContributor`'s Javadoc gives), never depending on `ProjectFacade`. `onProjectDuplicated` → `copyProjectDataSyncs(source, duplicate, workspaceId)` with the workspace read from the duplicate project; `importProjectContent` → `importDataSync(workspaceId, json, projectId)` per file; `pullProjectContent` matches as the agent contributor's `findAgent` does, then `updateDataSyncFromExport` or `importDataSync`.
- [ ] **Step 4: Verify.**
  ```
  .superpowers/gradle-run.sh t10 DS-SVC-M:test DS-SVC-M:testIntegration EE-CONF-SVC-M:test
  .superpowers/gradle-run.sh t10-compile compileJava compileTestJava
  ```
- [ ] **Step 5: Commit** — `"Carry data syncs through project duplicate, export/import and git"`.

---

## Phase 5 — GraphQL schema and authorization

### Task 11: Additive schema and the authorization audit

**Files:**
- Modify: `DS-GQL/resources/graphql/data-sync.graphqls` — `DataSync.projectWorkflowUuid: String!`; `CreateDataSyncInput.projectId: ID`; confirm `projectId: ID!` stays and is now resolved; confirm no `tags`, `publishDataSync`, tag queries/mutations/inputs remain
- Modify: `DS-GQL/java/.../DataSyncGraphQlController.java` — `CreateDataSyncInput(String title, @Nullable String description, long workspaceId, @Nullable Long projectId)` passed through; `DataSyncPayload.projectWorkflowUuid()`
- Tests: `DS-GQL-TEST/DataSyncGraphQlControllerTest.java`, `DS-SVC-TEST/facade/DataSyncFacadeAuthorizationTest.java`

- [ ] **Step 1: Failing tests.** Controller: `testCreateDataSyncPassesProjectId`, `testCreateDataSyncWithoutProjectPassesNull`, `testDataSyncResolvesProjectWorkflowUuid`, `testDataSyncDeploymentsHaveNoTags` (querying `tags` is a validation error). Authorization: extend `testFacadeMethodCountIsPinned` to also assert the declared method **names** equal the spec's 17 (`createDataSync`, `updateDataSync`, `deleteDataSync`, `getDataSync`, `getDataSyncs`, `updateDataSyncTrigger`, `setDataSyncElement`, `updateDataSyncElement`, `getDataSyncVersions`, `getDataSyncDeployments`, `runDataSyncDeployment`, `prepareProjectPublish`, `deleteProjectDataSyncs`, `copyProjectDataSyncs`, `exportDataSync`, `importDataSync`, `updateDataSyncFromExport`), and that `runDataSyncDeployment` carries no `@Transactional`.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Verify.**
  ```
  .superpowers/gradle-run.sh t11 DS-API-M:test DS-SVC-M:test DS-SVC-M:testIntegration DS-GQL-M:test
  .superpowers/gradle-run.sh t11-compile compileJava compileTestJava
  ```
- [ ] **Step 5: Commit** — `"Expose the data sync project workflow and target project in GraphQL"`.

---

## Phase 6 — Client GraphQL

Run client commands through `.superpowers/client-run.sh`. Codegen validates operations against the server schema files, so every operation change happens in one run; the regenerated file is committed separately from the operations.

### Task 12: Operations, regenerated types, old pages kept green

**Files:**
- Delete: `C/graphql/automation/data-sync/{dataSyncTags,dataSyncDeploymentTags,publishDataSync,updateDataSyncTags,updateDataSyncDeploymentTags}.graphql`
- Modify: `C/graphql/automation/data-sync/createDataSync.graphql` (input carries `projectId`; select `id projectId`), `dataSync.graphql` and `dataSyncs.graphql` (add `projectWorkflowUuid`, `lastModifiedDate` if missing; drop `tags`), `dataSyncDeployments.graphql` (drop `tags`)
- Regenerate: `C/shared/middleware/graphql.ts` (and `graphql-types.ts` if codegen touches it)
- Keep-green edits (the files Task 20 deletes, plus the header Task 16 rebuilds): `C/pages/automation/data-syncs/DataSyncs.tsx`, `components/DataSyncsLeftSidebarNav.tsx`, `components/data-sync-list/DataSyncListItem.tsx` (+ `.test.tsx`), `components/detail/DataSyncDetailHeader.tsx`, `C/pages/automation/data-sync-deployments/DataSyncDeployments.tsx`, `components/DataSyncDeploymentListItem.tsx` (+ `.test.tsx`)

- [ ] **Step 1: Operations** — edit/delete as listed. Commit: `git commit -m "client - Update data sync GraphQL operations" -- client/src/graphql/automation/data-sync`.
- [ ] **Step 2: Codegen** — `.superpowers/client-run.sh npx graphql-codegen`. Confirm the diff only touches data sync types. Commit: `"client - Regenerate GraphQL types"` — the generated file(s) only.
- [ ] **Step 3: Keep the old pages compiling.** `.superpowers/client-run.sh npm run typecheck` lists the breakages. Remove tag filters/badges/tag mutations from the list and deployments pages; in `DataSyncDetailHeader` replace `usePublishDataSyncMutation` with `usePublishProjectMutation` from `C/shared/mutations/automation/projects.mutations.ts`, called exactly as `useProjectHeader.ts` calls it, with the sync's `projectId`. No new behaviour — these files are replaced in Tasks 16 and 20.
- [ ] **Step 4: Verify** — `npm run typecheck`; `npx vitest run src/pages/automation/data-syncs src/pages/automation/data-sync-deployments`; eslint + prettier on touched files. All green.
- [ ] **Step 5: Commit** — `"client - Drop data sync tags and per-sync publish"`.

---

## Phase 7 — Client UI

### Task 13: Path util, actions hook, delete dialog, project-aware dialog

**Files:**
- Create: `C/pages/automation/data-syncs/utils/getDataSyncPath.ts`, `C/pages/automation/data-syncs/utils/tests/getDataSyncPath.test.ts`
- Create: `C/pages/automation/data-syncs/hooks/useDataSyncActions.ts` (+ `useDataSyncActions.test.ts`) — the `C/pages/automation/agents/hooks/useAgentActions.ts` twin
- Create: `C/pages/automation/data-syncs/components/DeleteDataSyncAlertDialog.tsx` (+ test) — the `DeleteAgentAlertDialog.tsx` twin, text "This will permanently delete the data sync <title>."
- Modify: `C/pages/automation/data-syncs/components/DataSyncsLeftSidebarDropdownMenu.tsx` (Delete opens the dialog), `C/pages/automation/data-syncs/components/DataSyncDialog.tsx` (project select as in `C/pages/automation/agents/components/AgentDialog.tsx`: hidden when `projectId` is locked, default "New project named after this data sync"; on success navigate to `getDataSyncPath` and invalidate project queries when a project was created), `C/pages/automation/data-syncs/utils/invalidateDataSyncQueries.ts` (also `ProjectKeys` on create and delete)
- Tests: new `DataSyncDialog.test.tsx` and `DataSyncsLeftSidebarDropdownMenu.test.tsx` next to their components

**Interfaces produced:** `getDataSyncPath(dataSync: {id: string; projectId: string}): string` → `/automation/projects/${projectId}/data-syncs/${id}`; `DataSyncDialogProps.projectId?: number`; `useDataSyncActions({dataSync, onDeleted?})` returning `{deleteDataSync, isDeleting, openEditDialog, …}` (mirror `useAgentActions`' return shape).

- [ ] **Step 1: Failing tests** — path shape; dialog shows the project select only when unlocked, sends `projectId` (or none) in `createDataSync`, navigates to the new path; menu's Delete opens the confirmation and only the confirm button calls `deleteDataSync`.
- [ ] **Step 2: Run to see them fail** — `npx vitest run src/pages/automation/data-syncs`.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Verify** — the vitest run, `npm run typecheck`, eslint + prettier on touched files.
- [ ] **Step 5: Commit** — `"client - Add project-aware data sync dialog, delete confirmation and actions"`.

---

### Task 14: `ProjectItemSelect` — prefixed values and a Data Syncs group

**Files:**
- Modify: `C/pages/automation/project/components/project-header/components/ProjectItemSelect.tsx`
- Test: `C/pages/automation/project/components/project-header/tests/ProjectItemSelect.test.tsx`

- [ ] **Step 1: Failing tests** — three groups with `WorkflowIcon`, `BotIcon`, `ArrowLeftRightIcon`; `currentDataSyncId` selects the sync; an agent and a workflow with the same numeric id (`projectWorkflowId === agent.id === '1050'`) each navigate to their own target; picking a sync navigates to `getDataSyncPath`.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.** Radio values `workflow:<projectWorkflowId>`, `agent:<id>`, `dataSync:<id>`; `onValueChange` splits on the first `:` and dispatches by kind. Syncs from `useDataSyncs()` filtered by `+dataSync.projectId === projectId`.
- [ ] **Step 4: Verify** — vitest on the test file, typecheck, eslint/prettier.
- [ ] **Step 5: Commit** — `"client - Add a Data Syncs group to the project item switcher and prefix its values"`.

---

### Task 15: Project sidebar Data Syncs tab

**Files:**
- Modify: `C/pages/automation/project/components/projects-sidebar/ProjectsLeftSidebar.tsx` (+ `ProjectsLeftSidebar.test.tsx`)
- Create: `C/pages/automation/project/components/projects-sidebar/components/ProjectDataSyncsList.tsx`
- Create: `C/pages/automation/project/components/projects-sidebar/tests/ProjectDataSyncsList.test.tsx`

**Interfaces produced:** `ProjectsLeftSidebarProps.currentDataSyncId?: number`.

- [ ] **Step 1: Failing tests** — tabs **Workflows (N) | Agents (M) | Data Syncs (K)**; initial tab is `dataSyncs` when `currentDataSyncId` is set; full-width **New Data Sync** button above the list opens `DataSyncDialog` locked to the browsed project; `ProjectDataSyncsList` rows show a badge row with the source and destination component icons, the title linking to `getDataSyncPath`, "Edited <date>" from `lastModifiedDate`, the row menu, and **no** leading type icon; empty list message.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement**, shaped on `ProjectAgentsList.tsx`. Component icons via the same component-definition lookup `WorkflowComponentsIcon.tsx` uses.
- [ ] **Step 4: 355px fit check.** Tests cannot measure layout and the app must not be booted against the shared dev DB. Build a throwaway `.superpowers/sidebar-tabs.html` that loads the built CSS (`npm run build`, then reference the emitted `dist/assets/*.css`) and reproduces the `TabsList`/`TabsTrigger` markup and classes at a 355px container with `px-*` matching the sidebar, labels "Workflows (12)", "Agents (3)", "Data Syncs (4)". Open it in the Browser pane (`file://…`) and read each trigger's `scrollWidth > clientWidth` with `javascript_tool`. If any label truncates — or the check cannot be made — apply the spec's fallback to **all three** tabs: plain labels with the count as a small badge inside each trigger (never a shorter label for one tab). Record the measurement or the reason in the commit body.
- [ ] **Step 5: Verify** — vitest on both test files, typecheck, eslint/prettier.
- [ ] **Step 6: Commit** — `"client - Add a Data Syncs tab to the project sidebar"`.

---

### Task 16: The sync page inside its project

**Files:**
- Create: `C/pages/automation/project/ProjectDataSync.tsx`, `C/pages/automation/project/ProjectDataSync.test.tsx` (shaped on `ProjectAgent.tsx` / `ProjectAgent.test.tsx`)
- Rewrite: `C/pages/automation/data-syncs/components/detail/DataSyncDetailHeader.tsx` on `C/pages/automation/agents/components/detail/AgentDetailHeader.tsx`'s shape; add `DataSyncDetailHeader.test.tsx`
- Modify: `C/routes.tsx` — add `projects/:projectId/data-syncs/:dataSyncId` beside `projects/:projectId/agents/:agentId` (:983) with a lazy import; the old `data-syncs/:dataSyncId` route stays until Task 20
- Modify: `C/pages/automation/data-syncs/DataSyncDetail.tsx` only to redirect its internal links through `getDataSyncPath` if it still links (it is deleted in Task 20)

- [ ] **Step 1: Failing tests** — 355px `ProjectsLeftSidebar` with `currentDataSyncId`, then header and `DataSyncWizard` full width; breadcrumb with `ProjectItemSelect` showing the sync title; Publish (`PublishPopover` → `usePublishProjectMutation` with the project id) and Deploy (`ProjectDeploymentDialog` for the project) as a segmented group; version pill + history sheet from `dataSyncVersions`; `SettingsMenu` with `firstTab={{ariaLabel: 'Data Sync tab', label: 'Data Sync', value: 'dataSync', content}}` holding Edit and Delete; no Run now; deleting the current sync navigates to the project's first workflow, else `/automation/projects`. Store and router mocks through `vi.hoisted`.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Verify** — vitest on the new tests plus `src/pages/automation/data-syncs`, typecheck, eslint/prettier.
- [ ] **Step 5: Commit** — `"client - Open data syncs inside their project"`.

---

### Task 17: Projects page Data Syncs tab

**Files:**
- Modify: `C/pages/automation/projects/components/project-list/ProjectList.tsx` (+ `ProjectList.test.tsx`), `ProjectListItem.tsx`
- Create: `C/pages/automation/projects/components/project-data-sync-list/ProjectDataSyncList.tsx`, `ProjectDataSyncListItem.tsx`, `ProjectDataSyncCreationActions.tsx` and a test for each (shaped on `project-agent-list/`)

**Interfaces produced:** `ProjectListTabType = 'agents' | 'dataSyncs' | 'workflows'`; `ProjectDataSyncCreationActionsProps.placement: 'emptyState' | 'tabRow'`.

- [ ] **Step 1: Failing tests** — tabs **Workflows (N) | Agents (M) | Data Syncs (K)** with K from `useDataSyncs()` grouped by `projectId`; the Data Syncs tab is present with zero syncs; the tab-row create button shows only while the active tab lists something; empty state (`ArrowLeftRightIcon` size-24, "No data syncs in this project", creation action); list rows carry a leading size-4 `text-content-neutral-secondary` `ArrowLeftRightIcon`, title link, "Source → Destination" line, trigger summary via `describeTrigger` (`C/pages/automation/data-syncs/utils/dataSyncTrigger.ts`), published version, row menu, and the shared row rhythm; `ProjectListItem` shows the sync count beside the agent count.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.** A single **New Data Sync** / **Create Data Sync** button (no import dropdown).
- [ ] **Step 4: Verify** — vitest on `src/pages/automation/projects`, typecheck, eslint/prettier.
- [ ] **Step 5: Commit** — `"client - Add a Data Syncs tab to Projects page rows"`.

---

### Task 18: Data Syncs filter on Projects; filters mutually exclusive

**Files:**
- Create: `C/pages/automation/data-syncs/components/DataSyncsFilterLeftSidebarNav.tsx` (+ test), exporting `getDataSyncsFilter(searchParams)` and the nav, built on `C/shared/layout/LeftSidebarFilterNav.tsx` like `AgentsFilterLeftSidebarNav.tsx`
- Modify: `C/pages/automation/agents/components/AgentsFilterLeftSidebarNav.tsx` (its links delete `dataSyncs`), `C/pages/automation/projects/Projects.tsx` (+ `Projects.test.tsx`), `C/pages/automation/projects/components/ProjectsFilterTitle.tsx`

- [ ] **Step 1: Failing tests** — nav title "Data Syncs", items **All Data Syncs** / **Scheduled**, `?dataSyncs=all|scheduled`, toggling off, preserving unrelated params, clearing `agents`; the agents nav clears `dataSyncs`; on Projects the filter keeps projects holding a (scheduled) sync and opens rows on the Data Syncs tab; `ProjectsFilterTitle` and `preservedSearchParams` carry whichever content filter is active; with no matching project the empty state offers **Create Data Sync** opening an unlocked `DataSyncDialog`.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.** Scheduled = the sync's current `triggerType === 'SCHEDULE'`.
- [ ] **Step 4: Verify** — vitest on `src/pages/automation/projects`, `src/pages/automation/data-syncs`, `src/pages/automation/agents/components`; typecheck; eslint/prettier.
- [ ] **Step 5: Commit** — `"client - Add a Data Syncs filter to the Projects page"`.

---

### Task 19: Deployments page Data Syncs tab, Run now, filter

**Files:**
- Move (`git mv`): `C/pages/automation/data-sync-deployments/hooks/useDataSyncDeployments.ts` → `C/pages/automation/project-deployments/hooks/useDataSyncDeployments.ts` (fix the one old importer)
- Create: `C/pages/automation/project-deployments/components/project-deployment-data-sync-list/ProjectDeploymentDataSyncList.tsx`, `ProjectDeploymentDataSyncListItem.tsx`, `ProjectDeploymentDataSyncListItem.test.tsx`, `ProjectDeploymentDataSyncList.test.tsx`
- Modify: `C/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentList.tsx` (+ test), `ProjectDeploymentListItem.tsx` (+ test), `C/pages/automation/project-deployments/ProjectDeployments.tsx` (+ test), `C/pages/automation/project-deployments/components/ProjectDeploymentFilterTitle.tsx`

**Interfaces produced:** `ProjectDeploymentListTabType = 'agents' | 'dataSyncs' | 'workflows'`.

- [ ] **Step 1: Failing tests** — tabs **Workflows | Agents | Data Syncs (K)**, K from `useDataSyncDeployments()` grouped by deployment id; the Workflows tab leaves out syncs' workflows (`workflowUuid` against the syncs' `projectWorkflowUuid` set) beside the agent exclusion; counter "N workflows · M agents · K data syncs"; the sync row shows `ArrowLeftRightIcon`, title, trigger summary, the enable `Switch` on the sync's `ProjectDeploymentWorkflow` (found in the deployment's `projectDeploymentWorkflows` by `workflowUuid === dataSync.projectWorkflowUuid`, toggled with `useEnableProjectDeploymentWorkflowMutation` from `C/shared/mutations/automation/projectDeploymentWorkflows.mutations.ts`), last execution, and **Run now** (`PlayIcon`) enabled only while both the deployment and that row are enabled, calling `runDataSyncDeployment({id, projectDeploymentId})`; empty "This deployment has no data syncs."; the Data Syncs filter keeps deployments deploying a (deployed-`SCHEDULE`) sync and opens rows on the tab; `ProjectDeploymentFilterTitle` shows it.
- [ ] **Step 2: Run to see them fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Verify** — vitest on `src/pages/automation/project-deployments`, typecheck, eslint/prettier.
- [ ] **Step 5: Commit** — `"client - Add a Data Syncs tab with Run now to Deployments page rows"`.

---

### Task 20: Remove the old pages, routes, nav rows and the automation Build group

**Files:**
- Delete: `C/pages/automation/data-syncs/DataSyncs.tsx` (+ test), `DataSyncDetail.tsx` (+ test), `components/data-sync-list/` (all), `components/DataSyncsLeftSidebarNav.tsx`, `components/DataSyncsFilterTitle.tsx`; `C/pages/automation/data-sync-deployments/` (all remaining)
- Modify: `C/routes.tsx` (remove the three lazy imports :64-66 and the `data-syncs`, `data-syncs/:dataSyncId`, `data-sync-deployments` route objects)
- Modify: `C/shared/navigation/navigationItems.ts` — remove `group: 'Build'` from Projects (it stays after Approval Tasks, before Connections, `FolderIcon`, same href), delete the Data Syncs row (:56) and the Deploy group's Data Syncs row (:66); drop `ArrowLeftRightIcon` / `RefreshCwIcon` imports if unused; keep `NAVIGATION_GROUP_ICONS.Build` (`HammerIcon`) for embedded
- Modify: `C/shared/navigation/developmentOnlyRoutes.ts` (delete `{fallbackHref: '/automation/data-sync-deployments', href: '/automation/data-syncs'}`), `C/shared/navigation/tests/developmentOnlyRoutes.test.ts` (add `['/automation/projects/12/data-syncs/34', '/automation/deployments']`; assert no `/automation/data-syncs` entry), and `useDevelopmentOnlyRouteGuard` tests if they name the old route
- Modify: `C/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStep.tsx` and `ProjectDeploymentDialog.tsx` — remove `agentOptions`, `agentOptionsLabel`, `DeployableAgentI`

- [ ] **Step 1: Pre-checks.** `grep -rn "'Build'" client/src --include='*.test.*'` and `grep -rln "data-syncs\b\|data-sync-deployments\|DataSyncDetail\|DataSyncs'" client/src` (in a `.superpowers/` script); every hit is either removed here or deliberate (the new `/data-syncs/` project route). `AppSidebar.test.tsx` uses its own fixture — confirm it asserts no real automation Build group.
- [ ] **Step 2: Failing tests** — `developmentOnlyRoutes.test.ts` cases above; a `navigationItems` assertion (existing nav test or a new `C/shared/navigation/tests/navigationItems.test.ts`) that the automation array has no `group: 'Build'`, Projects has no group and sits between Approval Tasks and Connections, and no href starts with `/automation/data-sync`.
- [ ] **Step 3: Delete and edit** as listed.
- [ ] **Step 4: Verify** — `.superpowers/client-run.sh npm run check` with a 600000 ms timeout. Green.
- [ ] **Step 5: Commit** — `"client - Remove the Data Syncs and Data Sync Deployments pages and the automation Build group"`.

---

## Phase 8 — Documentation

### Task 21: Agent guidance and docs

**Files:**
- `.agents/data-sync.md` — rewrite the load comment's paths (drop `data-sync-deployments`, add `pages/automation/project/ProjectDataSync.tsx`), Entity model (`project_workflow_uuid`, no `project_id`, no `data_sync_tag`, `workspace_id` equals the project's), placement in ordinary projects, publish via `DataSyncProjectPublishPreListener` and the `DATA_SYNC_PUBLISH` gate on `prepareProjectPublish`, the regenerate-in-place rule, delete rules (enabled-in-any-deployment refusal; project delete listener), the `data-syncs/<name>.json` contributor format and pull rules, Run now's ownership rule and the disabled-row refusal, multi-deployment reads, and the new client surfaces (three tabs, sidebar rows, filter, Deployments Run now). Point to the spec for the dev-DB migration SQL; do not copy it.
- `.agents/agents.md` — the "Client surfaces" section (:558-565) and the summary paragraph (:809-813): describe **Workflows | Agents | Data Syncs** tabs where two are implied; fix the stale `/automation/projects/agents` route wording to the Agents tab/filter reality while there.
- `CLAUDE.md`, "Sidebar navigation groups": "Current groups: automation Build / Deploy / Monitor / AI / Resources; …" → "automation Deploy / Monitor / AI / Resources; embedded Build / Configure / Monitor / Resources"; ungrouped rows become "automation AI Hub (Chats in CE — the two are edition-exclusive), Approval Tasks, Projects and Connections; embedded Connect alone"; "Both Build groups are Development-only" → "The embedded Build group and the automation Projects row are Development-only"; the pairs list drops "Data Syncs → Data Sync Deployments" and reads "Projects — including the Agents and Data Syncs tabs and the agents and data syncs inside a project — → Deployments". Also the `.agents/data-sync.md` row in the deep-dive table: "Data Sync: rows-as-truth, generated draft in its project, fixed node names, publish/delete with the project, Run now path, form-mode wizard".
- `docs/superpowers/specs/2026-09-05-data-sync-design.md` — a one-line note at the top pointing to the new spec for placement, publish, deployments and tags.
- `docs/content/` — `grep -rli "data sync" docs/content` returned nothing when this plan was written; re-run it and update any hit.
- Check: `grep -rn "DATA_SYNC_NAME_PREFIX\|__DATA_SYNC__\|data-sync-deployments\|Data Sync Deployments" .agents CLAUDE.md docs/content` returns nothing (spec files excepted).

- [ ] **Step 1: Edit.** **Step 2:** `.superpowers/client-run.sh npx prettier --check ../.agents/data-sync.md ../.agents/agents.md ../CLAUDE.md` (the client prettier config covers `*.md`). **Step 3: Commit** by path — `"Describe data syncs as members of ordinary projects in agent guidance and docs"`.

---

## Phase 9 — Final verification

### Task 22: Whole-branch verification

- [ ] **Step 1: Compile everything** — `.superpowers/gradle-run.sh final-compile compileJava compileTestJava`; exit 0.
- [ ] **Step 2: Formatting** — `.superpowers/gradle-run.sh final-spotless CONF-API-M:spotlessCheck CONF-SVC-M:spotlessCheck CONF-GQL-M:spotlessCheck MCP-GQL-M:spotlessCheck MCP-SVC-M:spotlessCheck EE-CONF-SVC-M:spotlessCheck DS-API-M:spotlessCheck DS-SVC-M:spotlessCheck DS-GQL-M:spotlessCheck`; exit 0.
- [ ] **Step 3: Every touched module's tests** — one invocation (Gradle keeps going with `--continue`):
  ```
  .superpowers/gradle-run.sh final-tests \
    CONF-API-M:test CONF-SVC-M:test CONF-SVC-M:testIntegration CONF-GQL-M:test \
    MCP-GQL-M:test MCP-SVC-M:testIntegration EE-CONF-SVC-M:test \
    DS-API-M:test DS-SVC-M:test DS-SVC-M:testIntegration DS-GQL-M:test \
    :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-service:test \
    :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-service:testIntegration
  ```
  (the agent module is included because its guard test moved and its listings share `isGenerated()`). If this exceeds the 600 s tool cap, run it with `run_in_background` and wait with Monitor, or split it per module. Exit 0; `grep -E '^> Task .* FAILED'` on the log is empty.
- [ ] **Step 4: Client** — `.superpowers/client-run.sh npm run check` with a 600000 ms timeout; green.
- [ ] **Step 5: Acceptance greps** (in a `.superpowers/` script): no `ProjectWorkflowType.AI_AGENT` in main code outside the agent module and the guard; no `fetchProjectDeployment(` in `automation-data-sync`; no `DATA_SYNC_NAME_PREFIX`; no `/automation/data-syncs` or `/automation/data-sync-deployments` in `client/src` outside the new project route; `git log --format=%B <base>..HEAD` contains no `Co-Authored-By` / `Generated with`.
- [ ] **Step 6:** Commit only if a fix was needed (by path, a subject naming the fix). Report the log paths and results.

---

## Outstanding against the spec

Intentionally not covered here, as the spec records:

- Environment promotion of a generated workflow (`automation-promotion`) — no test is added; the gap is recorded in the spec's Risks.
- A manual create → publish → deploy → run pass on a live server. Every lifecycle edge is covered by Testcontainers IntTests; a throwaway-database run is the user's decision.
- The dev-DB migration SQL stays in the spec and is run by hand, if at all.
