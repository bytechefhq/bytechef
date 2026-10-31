# Version History Workflow Changes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Each version row in the three version history panels lists the workflows that changed against the previous version, and clicking one opens a dialog with a structured change list, a side-by-side JSON diff and a highlighted read-only canvas.

**Architecture:** A platform-layer `WorkflowDefinitionDiffService` parses, masks, normalizes and structurally diffs two `Workflow`s. Owner-specific pairing (which `ProjectWorkflow`/`IntegrationWorkflow` rows to compare, by uuid) lives in an unguarded `ProjectVersionWorkflowChangeService` (CE, shared with the EE automation-workflow facade) and inline in the EE `IntegrationWorkflowFacadeImpl`; guarded facade methods expose it over GraphQL. On the client, shared presentational components under `client/src/shared/components/version-workflow-changes/` are fed by each panel's generated GraphQL hooks; the canvas is a standalone ReactFlow instance built from a pure `buildWorkflowGraph` function extracted from `useLayout`, so it never touches the editor's global stores.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring GraphQL / JUnit 5 + AssertJ + Mockito; React 19 / TypeScript / TanStack Query 5 / graphql-codegen / `@monaco-editor/react` `DiffEditor` / `@xyflow/react` / Vitest.

**Spec:** `docs/superpowers/specs/2026-10-31-version-history-workflow-changes-design.md`

## Global Constraints

- Commit subjects: server `6849 <description>`, client `6849 client - <description>`, docs `6849 docs - <description>`; subject only, no body, no `Co-Authored-By` trailer, no code comments explaining the change.
- Before each server commit: `./gradlew spotlessApply`. Before each client commit: `cd client && npm run format`.
- Generated `client/src/shared/middleware/graphql.ts` / `graphql-types.ts` go in their own commit (`6849 client - Generate`).
- EE files (`server/ee/**`) use the ByteChef Enterprise license header and a `@version ee` Javadoc tag. CE files use the Apache 2.0 header.
- `@PreAuthorize` goes on facade methods only, never on services or controllers. `PreAuthorizeAnnotationTest` enforces this.
- Java: blank line before control statements, blank line between a variable write and its first use, no `_` prefixes, descriptive names, no `TODO:` comments, no `new JsonMapper()` in main code (use `JsonUtils`).
- Java tests: class name `<Class>Test` (unit) or `<Class>IntTest` (integration), method names camelCase without underscores, one test class per source class, `@ExtendWith(ObjectMapperSetupExtension.class)` whenever `JsonUtils`/`MapUtils` are touched.
- Client: object keys sorted ascending (ESLint `sort-keys`), interface names end with `I`/`Props`, lucide icons imported with `Icon` suffix, `twMerge` for class merging, `useRef` names end in `Ref`, hooks ordered `useState → useRef → stores → custom hooks → derived → useEffect → return`.
- The Draft version row (status `DRAFT`) must become expandable in all three panels while staying non-selectable for restore in the Project panel.
- Masked values are rendered as the literal string `••••••` on both sides; expression strings (starting with `${`) are never masked.

## Review Focus

1. A workflow whose stored definition is YAML in one version and JSON in the other must diff as UNCHANGED when the content is the same (Task 1 test `testDiffReportsUnchangedForSameContentInDifferentFormats`).
2. A task renamed between versions appears as REMOVED + ADDED, not MODIFIED, and the summary still counts it once per entry (Task 2 test `testRenamedTaskIsRemovedAndAdded`).
3. A version with a deleted predecessor (V1, V3 exist, V2 deleted) compares V3 against V1 (Task 5 test `testBaseVersionSkipsDeletedVersions`).
4. Opening the dialog for an ADDED workflow renders the Definition tab with an empty original side and the Canvas tab with only the new canvas and no old/new toggle (Task 11 test `rendersAddedWorkflowWithoutBaseline`, Task 13 test `hides the toggle for an added workflow`).
5. Clicking a workflow row inside an expanded Project version card must not select that version for restore (Task 14 test `does not select the version when a changed workflow is clicked`).

---

### Task 1: Platform diff DTOs, service interface, normalization and masking

**Files:**
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/dto/WorkflowChangeType.java` (enum `ADDED, REMOVED, MODIFIED, UNCHANGED`)
- Create: `.../platform-configuration-api/.../dto/WorkflowNodeChangeKind.java` (enum `ADDED, REMOVED, MODIFIED, MOVED`)
- Create: `.../platform-configuration-api/.../dto/WorkflowNodeChangeTarget.java` (enum `WORKFLOW, TRIGGER, TASK, CLUSTER_ELEMENT, INPUT, OUTPUT`)
- Create: `.../platform-configuration-api/.../dto/WorkflowDefinitionValueChange.java` (record `(String path, @Nullable String oldValue, @Nullable String newValue)`)
- Create: `.../platform-configuration-api/.../dto/WorkflowDefinitionChange.java` (record `(WorkflowNodeChangeKind kind, WorkflowNodeChangeTarget target, @Nullable String name, @Nullable String type, @Nullable String label, @Nullable String parentPath, List<WorkflowDefinitionValueChange> valueChanges)`)
- Create: `.../platform-configuration-api/.../dto/WorkflowDefinitionDiff.java` (record `(WorkflowChangeType changeType, List<WorkflowDefinitionChange> changes, @Nullable String oldDefinition, @Nullable String newDefinition)`)
- Create: `.../platform-configuration-api/.../service/WorkflowDefinitionDiffService.java`
- Create: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/workflow/diff/WorkflowDefinitionNormalizer.java`
- Create: `.../platform-configuration-service/.../workflow/diff/SensitiveValueMasker.java`
- Create: `.../platform-configuration-service/.../workflow/diff/WorkflowDefinitionDiffServiceImpl.java` (`@Service`; this task only wires normalization + whole-workflow ADDED/REMOVED/UNCHANGED; structural changes come in Tasks 2–3)
- Test: `.../platform-configuration-service/src/test/java/com/bytechef/platform/configuration/workflow/diff/WorkflowDefinitionNormalizerTest.java`
- Test: `.../platform-configuration-service/src/test/java/com/bytechef/platform/configuration/workflow/diff/SensitiveValueMaskerTest.java`
- Test: `.../platform-configuration-service/src/test/java/com/bytechef/platform/configuration/workflow/diff/WorkflowDefinitionDiffServiceTest.java`

**Interfaces:**
- Consumes: `com.bytechef.atlas.configuration.domain.Workflow` (`getDefinition()`, `getFormat()`), `WorkflowReader.readWorkflowMap(WorkflowResource)`, `WorkflowResource(String id, Map<String,Object> metadata, Resource resource, Workflow.Format format)`, `WorkflowConstants.TASK_NUMBER`, `JsonUtils.write(Object)`, `JsonUtils.writeWithDefaultPrettyPrinter(Object)`.
- Produces:
  - `WorkflowDefinitionDiffService.diff(@Nullable Workflow oldWorkflow, @Nullable Workflow newWorkflow): WorkflowDefinitionDiff`
  - `WorkflowDefinitionNormalizer.normalize(Workflow workflow): Map<String, Object>` — parses by `workflow.getFormat()` through `WorkflowReader`, removes `taskNumber` from every top-level task, applies `SensitiveValueMasker.mask`, returns a `TreeMap`-recursive copy (sorted keys at every level; list order preserved).
  - `WorkflowDefinitionNormalizer.toJson(Map<String, Object> normalized): String` — `JsonUtils.writeWithDefaultPrettyPrinter`.
  - `SensitiveValueMasker.MASK = "••••••"`; `SensitiveValueMasker.mask(Map<String, Object> map): Map<String, Object>` (deep); `SensitiveValueMasker.isSensitiveKey(String key): boolean` — key lower-cased with `_`/`-` removed is in `{password, secret, clientsecret, token, accesstoken, refreshtoken, apikey, apitoken, authorization, privatekey, credentials, passphrase}`; a `String` value starting with `${` is left intact.

- [ ] **Step 1: Write the failing normalizer and masker tests**

```java
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowDefinitionNormalizerTest {
    @Test void testNormalizeSortsKeysAtEveryLevel() // {"tasks":[{"type":"a/v1/b","name":"t1","parameters":{"z":1,"a":2}}],"label":"L"} -> toJson starts with "label", nested parameters keys a before z
    @Test void testNormalizeKeepsListOrder()        // tasks [t2, t1] stay [t2, t1]
    @Test void testNormalizeStripsTaskNumber()      // toJson(normalize(...)) does not contain "taskNumber"
    @Test void testNormalizeParsesYaml()            // new Workflow("label: L\ntasks: []\n", Format.YAML) -> map has label "L"
}

class SensitiveValueMaskerTest {
    @Test void testMasksSensitiveKeysCaseAndSeparatorInsensitive() // "Api_Key", "client-secret", "AUTHORIZATION" -> value startsWith(MASK)
    @Test void testKeepsInsensitiveKeys()                           // "url", "username" untouched
    @Test void testMasksNestedValues()                              // parameters.headers.Authorization -> startsWith(MASK)
    @Test void testKeepsExpressionValues()                          // "token": "${trigger.token}" untouched
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests 'com.bytechef.platform.configuration.workflow.diff.*'`
Expected: compilation failure (classes missing).

- [ ] **Step 3: Implement the DTOs, `SensitiveValueMasker`, `WorkflowDefinitionNormalizer`**

`WorkflowReader.readWorkflowMap` declares `throws Exception`; wrap in `IllegalArgumentException("Unable to parse workflow definition", exception)`.

- [ ] **Step 4: Write the failing diff service tests for whole-workflow outcomes**

```java
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowDefinitionDiffServiceTest {
    private final WorkflowDefinitionDiffService diffService = new WorkflowDefinitionDiffServiceImpl();

    @Test void testDiffReportsAddedWhenOldIsNull()      // changeType ADDED, oldDefinition null, newDefinition pretty JSON
    @Test void testDiffReportsRemovedWhenNewIsNull()    // changeType REMOVED, newDefinition null
    @Test void testDiffReportsUnchangedForReorderedKeys() // same content, different key order/indentation -> UNCHANGED, changes empty, oldDefinition equals newDefinition
    @Test void testDiffReportsUnchangedForSameContentInDifferentFormats() // JSON vs YAML same content -> UNCHANGED
    @Test void testDiffMasksBothDefinitions()           // "password":"x" -> both definitions contain MASK, not "x"
}
```

- [ ] **Step 5: Implement `WorkflowDefinitionDiffServiceImpl.diff`**

Signature: `public WorkflowDefinitionDiff diff(@Nullable Workflow oldWorkflow, @Nullable Workflow newWorkflow)`. Normalize both sides; ADDED/REMOVED when one is null; UNCHANGED when the normalized maps are `equals`; otherwise MODIFIED with `changes` from a `WorkflowStructuralDiffer` that Task 2 adds (for this task return `List.of()` for MODIFIED and leave a test for Task 2).

- [ ] **Step 6: Run tests, then spotless**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests 'com.bytechef.platform.configuration.workflow.diff.*' && ./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:spotlessApply :server:libs:platform:platform-configuration:platform-configuration-api:spotlessApply`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/dto/Workflow*Change*.java server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/dto/WorkflowDefinitionDiff.java server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/service/WorkflowDefinitionDiffService.java server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/workflow/diff server/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/platform/configuration/workflow/diff
git commit -m "6849 Add the workflow definition diff service with normalization and sensitive value masking"
```

---

### Task 2: Structural diff of workflow fields, trigger, top-level tasks, inputs and outputs

**Files:**
- Create: `.../platform-configuration-service/.../workflow/diff/WorkflowNode.java` (record `(WorkflowNodeChangeTarget target, String name, @Nullable String type, @Nullable String label, @Nullable String parentPath, int index, Map<String, Object> attributes)`; `attributes` = the node map without its child-task containers and without `clusterElements`)
- Create: `.../platform-configuration-service/.../workflow/diff/WorkflowNodeCollector.java`
- Create: `.../platform-configuration-service/.../workflow/diff/WorkflowStructuralDiffer.java`
- Modify: `.../platform-configuration-service/.../workflow/diff/WorkflowDefinitionDiffServiceImpl.java` (use the differ for MODIFIED)
- Test: `.../workflow/diff/WorkflowNodeCollectorTest.java`
- Test: `.../workflow/diff/WorkflowStructuralDifferTest.java`
- Modify test: `WorkflowDefinitionDiffServiceTest.java` (add `testDiffReportsModifiedWithChanges`)

**Interfaces:**
- Consumes: Task 1 DTOs and `WorkflowDefinitionNormalizer.normalize`.
- Produces:
  - `WorkflowNodeCollector.collect(Map<String, Object> normalizedDefinition): List<WorkflowNode>` — pre-order: trigger(s) from `triggers` (target TRIGGER), then tasks (target TASK), inputs (`inputs[]`, keyed by `name`, target INPUT), outputs (`outputs[]`, keyed by `name`, target OUTPUT). This task collects **top-level** tasks only; Task 3 adds nesting and cluster elements.
  - `WorkflowStructuralDiffer.diff(Map<String, Object> oldDefinition, Map<String, Object> newDefinition): List<WorkflowDefinitionChange>`.
  - Value change paths use dot/bracket notation relative to the node: `label`, `type`, `parameters.headers.Accept`, `parameters.items[1].name`; values are `JsonUtils.write(value)` (`"abc"` keeps quotes, numbers bare, `null` when absent).
  - Ordering of `changes`: one WORKFLOW entry first (only if `label` or `description` changed; `name` null), then nodes in **new**-definition pre-order, then REMOVED nodes in old-definition pre-order, then INPUT/OUTPUT entries.

- [ ] **Step 1: Write the failing collector and differ tests**

```java
class WorkflowNodeCollectorTest {
    @Test void testCollectsTriggerThenTasksThenInputsAndOutputs() // order and targets
    @Test void testTaskAttributesExcludeClusterElements()         // attributes has no "clusterElements"
    @Test void testTaskIndexFollowsListPosition()
}

class WorkflowStructuralDifferTest {
    @Test void testWorkflowLabelAndDescriptionChange()   // one WORKFLOW change, valueChanges paths "label","description"
    @Test void testTriggerTypeChange()                   // MODIFIED TRIGGER with valueChange path "type" old "\"a/v1/x\"" new "\"b/v1/y\""
    @Test void testTriggerAddedAndRemoved()
    @Test void testTaskAdded()                           // ADDED TASK name "httpClient_2", type set, no valueChanges
    @Test void testTaskRemoved()
    @Test void testTaskParameterChange()                 // valueChange path "parameters.url"
    @Test void testTaskNestedParameterListChange()       // path "parameters.headers[0].value"
    @Test void testRenamedTaskIsRemovedAndAdded()
    @Test void testInputAddedRemovedModified()           // target INPUT, path "required"
    @Test void testOutputValueChange()                   // target OUTPUT, path "value"
    @Test void testChangeOrderIsWorkflowThenNewOrderThenRemoved()
    @Test void testMaskedValueChangeShowsMaskOnBothSides() // two different secrets -> one valueChange with oldValue and newValue both "\"••••••\""
}
```

Masking happens before diffing, so two different secrets would both become `MASK` and look equal. Decision: `SensitiveValueMasker.mask` replaces a sensitive value with `MASK + ":" + sha256Hex(JsonUtils.write(value)).substring(0, 16)` (so the normalized maps still differ), and `WorkflowDefinitionNormalizer.toJson` plus `WorkflowStructuralDiffer` print any string starting with `MASK` as plain `MASK`, so nothing hashed ever leaves the server. Add `SensitiveValueMaskerTest.testMaskPreservesInequalityOfDifferentSecrets` and `WorkflowDefinitionNormalizerTest.testToJsonPrintsPlainMaskForMaskedValues` in this task.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests 'com.bytechef.platform.configuration.workflow.diff.*'`
Expected: compilation failure.

- [ ] **Step 3: Implement `WorkflowNodeCollector`, `WorkflowStructuralDiffer`, wire into `WorkflowDefinitionDiffServiceImpl`**

Node identity key = `target + ":" + name` (trigger and task names cannot collide in practice; the key keeps them apart anyway). For a node present on both sides, compare `attributes` deeply (`compareValues(path, oldValue, newValue, List<WorkflowDefinitionValueChange>)`: maps recurse by key union, lists recurse by index up to max length, scalars compare with `Objects.equals`).

- [ ] **Step 4: Run tests, spotless**

Run: same test command, then `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:spotlessApply`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-configuration/platform-configuration-service/src
git commit -m "6849 Report workflow, trigger, task, input and output changes in the workflow definition diff"
```

---

### Task 3: Nested tasks, cluster elements and MOVED detection

**Files:**
- Modify: `WorkflowNodeCollector.java`, `WorkflowStructuralDiffer.java`
- Modify tests: `WorkflowNodeCollectorTest.java`, `WorkflowStructuralDifferTest.java`

**Interfaces:**
- Produces (collector): recursion through every value of a task map except `name`, `type`, `label`, `description`, `metadata`: any `Map` with a `String name` **and** `String type` is a nested TASK node; `parentPath` = `<parentTaskName>.<path to the containing list>` using the same notation as value paths (`condition_1.caseTrue`, `loop_1.iteratee`, `branch_1.cases[1].tasks`, `branch_1.default`, `forkJoin_1.branches[0]`, `parallel_1.tasks`). A nested task's `attributes` exclude its own child-task containers (any list/map that resolved to task nodes). Cluster elements: for a task map with `clusterElements` (map of type → element map or list of element maps), each element map is a CLUSTER_ELEMENT node with `parentPath = <taskName>.clusterElements.<type>`, recursing into nested `clusterElements`.
- Produces (differ): a node present on both sides is MOVED when `parentPath` differs, or when its position among the siblings common to both sides (same `parentPath`, same target) differs between old and new ordering. MOVED may carry `valueChanges` if attributes also changed; kind stays MOVED (highlight priority: moved over modified).

- [ ] **Step 1: Write the failing tests**

```java
// WorkflowNodeCollectorTest
@Test void testCollectsConditionCaseTrueAndCaseFalseTasks()   // parentPath "condition_1.caseTrue" / "condition_1.caseFalse"
@Test void testCollectsLoopIterateeTasks()                     // "loop_1.iteratee"
@Test void testCollectsBranchCasesAndDefaultTasks()            // "branch_1.cases[0].tasks", "branch_1.default"
@Test void testCollectsForkJoinBranchesListOfLists()           // "forkJoin_1.branches[1]"
@Test void testCollectsParallelTasks()                         // "parallel_1.tasks"
@Test void testNestedTaskAttributesExcludeChildTaskLists()     // condition attributes has parameters without caseTrue/caseFalse
@Test void testCollectsClusterElementsSingleAndList()          // "aiAgent_1.clusterElements.model", "aiAgent_1.clusterElements.tools"
@Test void testCollectsNestedClusterElements()
// WorkflowStructuralDifferTest
@Test void testNestedTaskAddedInsideCondition()
@Test void testNestedTaskParameterChangeInsideLoop()
@Test void testTaskMovedBetweenConditionBranches()              // MOVED, parentPath = new parentPath
@Test void testTaskMovedToTopLevel()
@Test void testReorderedSiblingsAreMoved()                      // [a,b,c] -> [a,c,b]: b and c MOVED, a unchanged
@Test void testInsertingSiblingDoesNotMoveOthers()              // [a,c] -> [a,b,c]: only b ADDED
@Test void testClusterElementAddedRemovedModified()
@Test void testMovedTaskWithParameterChangeStaysMoved()
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests 'com.bytechef.platform.configuration.workflow.diff.*'`
Expected: FAIL on the new tests.

- [ ] **Step 3: Implement recursion, cluster element collection and MOVED detection**

- [ ] **Step 4: Run tests, spotless, commit**

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests 'com.bytechef.platform.configuration.workflow.diff.*' :server:libs:platform:platform-configuration:platform-configuration-service:spotlessApply
git add server/libs/platform/platform-configuration/platform-configuration-service/src
git commit -m "6849 Diff nested task dispatcher tasks and cluster elements and detect moved nodes"
```

---

### Task 4: Shared version-change DTOs and GraphQL schema types

**Files:**
- Create: `.../platform-configuration-api/.../dto/VersionWorkflowChangeSummary.java` (record `(String workflowUuid, @Nullable String label, WorkflowChangeType changeType, int changeCount)`)
- Create: `.../platform-configuration-api/.../dto/VersionWorkflowChanges.java` (record `(@Nullable Integer baseVersion, int version, List<VersionWorkflowChangeSummary> workflows)`)
- Create: `.../platform-configuration-api/.../dto/VersionWorkflowChange.java` (record `(String workflowUuid, @Nullable String label, WorkflowChangeType changeType, @Nullable Integer baseVersion, int version, List<WorkflowDefinitionChange> changes, @Nullable String oldDefinition, @Nullable String newDefinition)`)
- Create: `server/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/workflow-version-change.graphqls`
- Create: `.../platform-configuration-api/.../dto/VersionWorkflowChangeFactory.java` — static helpers both owners use: `summary(String workflowUuid, @Nullable Workflow oldWorkflow, @Nullable Workflow newWorkflow, WorkflowDefinitionDiff diff): VersionWorkflowChangeSummary` (label = `newWorkflow` label, or old when removed) and `change(String workflowUuid, @Nullable Integer baseVersion, int version, @Nullable Workflow oldWorkflow, @Nullable Workflow newWorkflow, WorkflowDefinitionDiff diff): VersionWorkflowChange`.
- Test: `.../platform-configuration-api/src/test/java/com/bytechef/platform/configuration/dto/VersionWorkflowChangeFactoryTest.java`

**Interfaces:**
- Produces the GraphQL SDL (no `extend type Query` here; owners add queries):

```graphql
enum WorkflowChangeType { ADDED REMOVED MODIFIED }
enum WorkflowNodeChangeKind { ADDED REMOVED MODIFIED MOVED }
enum WorkflowNodeChangeTarget { WORKFLOW TRIGGER TASK CLUSTER_ELEMENT INPUT OUTPUT }

type WorkflowDefinitionValueChange { path: String!  oldValue: String  newValue: String }
type WorkflowDefinitionChange {
    kind: WorkflowNodeChangeKind!  target: WorkflowNodeChangeTarget!  name: String  type: String
    label: String  parentPath: String  valueChanges: [WorkflowDefinitionValueChange!]!
}
type VersionWorkflowChangeSummary { workflowUuid: String!  label: String  changeType: WorkflowChangeType!  changeCount: Int! }
type VersionWorkflowChanges { baseVersion: Int  version: Int!  workflows: [VersionWorkflowChangeSummary!]! }
type VersionWorkflowChange {
    workflowUuid: String!  label: String  changeType: WorkflowChangeType!  baseVersion: Int  version: Int!
    changes: [WorkflowDefinitionChange!]!  oldDefinition: String  newDefinition: String
}
```

- [ ] **Step 1: Write the failing factory test**

```java
class VersionWorkflowChangeFactoryTest {
    @Test void testSummaryUsesNewLabelAndChangeCount()
    @Test void testSummaryUsesOldLabelWhenRemoved()
    @Test void testChangeCarriesDefinitionsAndVersions()
}
```

- [ ] **Step 2: Run to verify failure, implement, run, spotless**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:test --tests '*VersionWorkflowChangeFactoryTest' && ./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:spotlessApply`

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-configuration/platform-configuration-api/src server/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/workflow-version-change.graphqls
git commit -m "6849 Add the shared version workflow change types and GraphQL schema"
```

---

### Task 5: CE pairing service for project versions

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectVersionWorkflowChangeService.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectVersionWorkflowChangeServiceImpl.java` (`@Service`, `@Transactional(readOnly = true)`, unguarded, Javadoc states callers must be guarded facades)
- Test: `.../automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/ProjectVersionWorkflowChangeServiceTest.java` (Mockito unit test)

**Interfaces:**
- Consumes: `ProjectService.getProject(long)`, `Project.fetchProjectVersion(int)`, `Project.getProjectVersions()`, `ProjectWorkflowService.getProjectWorkflows(long projectId, int projectVersion)`, `ProjectWorkflow.getUuidAsString()/getWorkflowId()`, `WorkflowService.getWorkflow(String)`, `WorkflowDefinitionDiffService.diff`, `VersionWorkflowChangeFactory`.
- Produces:
  - `VersionWorkflowChanges getVersionWorkflowChanges(long projectId, int projectVersion)`
  - `VersionWorkflowChange getVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid)`
  - Baseline rule: `baseVersion` = max `ProjectVersion.getVersion()` strictly below `projectVersion` among `project.getProjectVersions()`, or `null`. Unknown `projectVersion` → `ConfigurationException(..., ProjectErrorType.PROJECT_VERSION_NOT_FOUND)`. Unknown uuid in both versions → `IllegalArgumentException("Workflow uuid=%s not found in project version v=%s")`.
  - Summary lists only `changeType != UNCHANGED`, ordered: ADDED, MODIFIED, REMOVED, then by label (case-insensitive, nulls last).

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class ProjectVersionWorkflowChangeServiceTest {
    @Test void testBaseVersionIsPreviousExistingVersion()      // versions 1,2,3 -> V3 base 2
    @Test void testBaseVersionSkipsDeletedVersions()           // versions 1,3 -> V3 base 1
    @Test void testFirstVersionHasNoBaseAndAllWorkflowsAdded() // base null; every workflow ADDED
    @Test void testPairsWorkflowsByUuidAndOmitsUnchanged()
    @Test void testOrdersSummariesByChangeTypeThenLabel()
    @Test void testUnknownVersionThrows()
    @Test void testChangeForRemovedWorkflowHasOnlyOldDefinition()
    @Test void testUnknownWorkflowUuidThrows()
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectVersionWorkflowChangeServiceTest'`
Expected: compilation failure.

- [ ] **Step 3: Implement the interface and impl**

- [ ] **Step 4: Run tests, spotless, commit**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectVersionWorkflowChangeServiceTest' :server:libs:automation:automation-configuration:automation-configuration-service:spotlessApply :server:libs:automation:automation-configuration:automation-configuration-api:spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectVersionWorkflowChangeService.java server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectVersionWorkflowChangeServiceImpl.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/ProjectVersionWorkflowChangeServiceTest.java
git commit -m "6849 Pair project version workflows by uuid and diff them against the previous version"
```

---

### Task 6: CE facade methods, authorization and GraphQL queries

**Files:**
- Modify: `.../automation-configuration-api/.../facade/ProjectWorkflowFacade.java`
- Modify: `.../automation-configuration-service/.../facade/ProjectWorkflowFacadeImpl.java` (inject `ProjectVersionWorkflowChangeService`)
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/facade/RemoteProjectWorkflowFacadeClient.java` (two stubs throwing `UnsupportedOperationException`)
- Modify: `.../automation-configuration-graphql/src/main/resources/graphql/project-workflow.graphqls`
- Modify: `.../automation-configuration-graphql/.../web/graphql/ProjectWorkflowGraphQlController.java`
- Modify: `.../automation-configuration-graphql/src/test/resources/graphql/test.graphqls` (add the Task 4 types as stubs)
- Modify test: `.../automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectWorkflowFacadeIntTest.java` (nested `PreAuthorizeEnforcement`)
- Modify test: `.../automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/web/graphql/ProjectWorkflowGraphQlControllerIntTest.java`

**Interfaces:**
- Produces (facade, both `@PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')") @Transactional(readOnly = true)`):
  - `VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion)`
  - `VersionWorkflowChange getProjectVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid)`
- Produces (GraphQL, `extend type Query` in `project-workflow.graphqls`):
  - `projectVersionWorkflowChanges(projectId: ID!, projectVersion: Int!): VersionWorkflowChanges!`
  - `projectVersionWorkflowChange(projectId: ID!, projectVersion: Int!, workflowUuid: String!): VersionWorkflowChange!`
  - Controller methods `projectVersionWorkflowChanges(@Argument long projectId, @Argument int projectVersion)` and `projectVersionWorkflowChange(@Argument long projectId, @Argument int projectVersion, @Argument String workflowUuid)` delegating to `projectWorkflowFacade`.

- [ ] **Step 1: Write the failing authorization and GraphQL tests**

```java
// ProjectWorkflowFacadeIntTest.PreAuthorizeEnforcement
@Test void testVersionWorkflowChangesReadIsProtected() {
    assertRequiresScope(PROJECT_ID, "Project", "WORKFLOW_VIEW",
        () -> projectWorkflowFacade.getProjectVersionWorkflowChanges(PROJECT_ID, 1));
    assertRequiresScope(PROJECT_ID, "Project", "WORKFLOW_VIEW",
        () -> projectWorkflowFacade.getProjectVersionWorkflowChange(PROJECT_ID, 1, UUID_STRING));
}
// ProjectWorkflowGraphQlControllerIntTest
@Test void testProjectVersionWorkflowChanges() // when(facade.getProjectVersionWorkflowChanges(1L, 2)).thenReturn(new VersionWorkflowChanges(1, 2, List.of(new VersionWorkflowChangeSummary("u1","Flow",MODIFIED,3)))); path("projectVersionWorkflowChanges.workflows[0].changeCount").entity(Integer.class).isEqualTo(3)
@Test void testProjectVersionWorkflowChange()  // path("projectVersionWorkflowChange.changes[0].kind").entity(String.class).isEqualTo("ADDED")
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:testIntegration --tests '*ProjectWorkflowGraphQlControllerIntTest'`
Expected: compilation failure.

- [ ] **Step 3: Implement facade methods, remote stub, schema, controller, test stubs**

- [ ] **Step 4: Run both IntTests, spotless**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:testIntegration --tests '*ProjectWorkflowGraphQlControllerIntTest' :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*ProjectWorkflowFacadeIntTest' && ./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL (the facade IntTest needs Docker).

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-configuration server/ee/libs/automation/automation-configuration/automation-configuration-remote-client
git commit -m "6849 Expose project version workflow changes through the project workflow facade and GraphQL"
```

---

### Task 7: EE integration version workflow changes

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/IntegrationWorkflowFacade.java`
- Modify: `.../embedded-configuration-service/.../facade/IntegrationWorkflowFacadeImpl.java` (inject `WorkflowDefinitionDiffService`)
- Modify: `.../embedded-configuration-remote-client/.../facade/RemoteIntegrationWorkflowFacadeClient.java`
- Modify: `.../embedded-configuration-graphql/src/main/resources/graphql/integration-workflow.graphqls`
- Modify: `.../embedded-configuration-graphql/.../web/graphql/IntegrationWorkflowGraphQlController.java`
- Modify: `.../embedded-configuration-graphql/src/test/resources/graphql/test.graphqls`
- Create test: `.../embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/IntegrationWorkflowFacadeAuthorizationIntTest.java` (follow `IntegrationTagFacadeIntTest`: `MethodSecurityConfiguration` with `@EnableMethodSecurity` + `@ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)` and the `permissionService` bean answering `isTenantAdmin()` from `AuthorityConstants.ADMIN`) — only if no authorization IntTest for this facade exists yet; otherwise add the tests there.
- Modify test: `.../embedded-configuration-graphql/src/test/java/.../web/graphql/IntegrationWorkflowGraphQlControllerIntTest.java`

**Interfaces:**
- Consumes: `IntegrationService.getIntegration(long)`, `Integration.getIntegrationVersions()`, `IntegrationVersion.getVersion()`, `IntegrationWorkflowService.getIntegrationWorkflows(long, int)`, `IntegrationWorkflow.getUuidAsString()/getWorkflowId()`, `WorkflowService.getWorkflow`, `WorkflowDefinitionDiffService`, `VersionWorkflowChangeFactory`.
- Produces (facade, `@PreAuthorize("isTenantAdmin()")`):
  - `VersionWorkflowChanges getIntegrationVersionWorkflowChanges(long integrationId, int integrationVersion)`
  - `VersionWorkflowChange getIntegrationVersionWorkflowChange(long integrationId, int integrationVersion, String workflowUuid)`
  - Same baseline, ordering and error rules as Task 5 (unknown version → `IllegalArgumentException("Integration version v=%s not found")`).
- Produces (GraphQL): `integrationVersionWorkflowChanges(integrationId: ID!, integrationVersion: Int!): VersionWorkflowChanges!`, `integrationVersionWorkflowChange(integrationId: ID!, integrationVersion: Int!, workflowUuid: String!): VersionWorkflowChange!`.

- [ ] **Step 1: Write the failing tests**

```java
// IntegrationWorkflowFacadeAuthorizationIntTest
@Test void testVersionWorkflowChangesDeniedForNonAdmin()      // ROLE_USER -> AccessDeniedException on both methods
@Test void testVersionWorkflowChangesDeniedForConnectedUser() // EmbeddedApiKeyAuthenticationToken -> AccessDeniedException
@Test void testVersionWorkflowChangesAllowedForAdmin()        // ROLE_ADMIN -> no AccessDeniedException
// IntegrationWorkflowGraphQlControllerIntTest
@Test void testIntegrationVersionWorkflowChanges()
@Test void testIntegrationVersionWorkflowChange()
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration --tests '*IntegrationWorkflowGraphQlControllerIntTest'`

- [ ] **Step 3: Implement**

The pairing code in `IntegrationWorkflowFacadeImpl` mirrors `ProjectVersionWorkflowChangeServiceImpl`; keep it in two private methods (`resolveBaseIntegrationVersion`, `pairIntegrationWorkflows`).

- [ ] **Step 4: Run tests, spotless, commit**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration --tests '*IntegrationWorkflowGraphQlControllerIntTest' :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests '*IntegrationWorkflowFacadeAuthorizationIntTest' spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "6849 Expose integration version workflow changes through the integration workflow facade and GraphQL"
```

---

### Task 8: EE automation workflow project version workflow changes

**Files:**
- Modify: `.../embedded-configuration-api/.../facade/AutomationWorkflowProjectFacade.java` and `AutomationWorkflowProjectAdminFacade.java`
- Modify: `.../embedded-configuration-service/.../facade/AutomationWorkflowProjectFacadeImpl.java` (inject `ProjectVersionWorkflowChangeService`; validate with `getProjectOfAutomationWorkflowProject(projectId)` first) and `AutomationWorkflowProjectAdminFacadeImpl.java` (delegate; class-level `isTenantAdmin()` covers it)
- Modify: `.../embedded-configuration-remote-client/.../facade/RemoteAutomationWorkflowProjectFacadeClient.java` (and the admin remote client if one exists)
- Modify: `.../embedded-configuration-graphql/src/main/resources/graphql/automation-workflow-project.graphqls`
- Modify: `.../embedded-configuration-graphql/.../web/graphql/AutomationWorkflowProjectGraphQlController.java`
- Modify tests: `AutomationWorkflowProjectGraphQlControllerIntTest.java`; the existing admin facade authorization IntTest (add denial tests for the two new methods) or a new `AutomationWorkflowProjectAdminFacadeAuthorizationIntTest` following Task 7's pattern.

**Interfaces:**
- Produces (both facades): `VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion)`, `VersionWorkflowChange getProjectVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid)`.
- Produces (GraphQL): `automationWorkflowProjectVersionWorkflowChanges(id: ID!, projectVersion: Int!): VersionWorkflowChanges!`, `automationWorkflowProjectVersionWorkflowChange(id: ID!, projectVersion: Int!, workflowUuid: String!): VersionWorkflowChange!` (controller parses `id` with `Long.parseLong`, like `automationWorkflowProjectVersions`).

- [ ] **Step 1: Write the failing tests** (`testAutomationWorkflowProjectVersionWorkflowChanges`, `testAutomationWorkflowProjectVersionWorkflowChange`, admin denial tests)
- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration --tests '*AutomationWorkflowProjectGraphQlControllerIntTest'`

- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests, then the full server check for touched modules, spotless, commit**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "6849 Expose automation workflow project version workflow changes through the admin facade and GraphQL"
```

- [ ] **Step 5: Server-wide verification**

Run: `./gradlew compileJava compileTestJava :server:libs:platform:platform-configuration:platform-configuration-service:test :server:libs:automation:automation-configuration:automation-configuration-service:test` and the `PreAuthorizeAnnotationTest` module test (`./gradlew test --tests '*PreAuthorizeAnnotationTest'`).
Expected: BUILD SUCCESSFUL. Also boot the app once (`./gradlew -p server/apps/server-app bootRun`) and run `projectVersionWorkflowChanges` against a local project from the GraphiQL/HTTP client to confirm the schema merges.

---

### Task 9: Client GraphQL operations and generated code

**Files:**
- Create: `client/src/graphql/automation/configuration/projectVersionWorkflowChanges.graphql`, `projectVersionWorkflowChange.graphql`
- Create: `client/src/graphql/embedded/configuration/integrationVersionWorkflowChanges.graphql`, `integrationVersionWorkflowChange.graphql`, `automationWorkflowProjectVersionWorkflowChanges.graphql`, `automationWorkflowProjectVersionWorkflowChange.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`, `client/src/shared/middleware/graphql-types.ts`

**Interfaces:**
- Produces hooks: `useProjectVersionWorkflowChangesQuery({projectId, projectVersion}, options)`, `useProjectVersionWorkflowChangeQuery({projectId, projectVersion, workflowUuid}, options)`, `useIntegrationVersionWorkflowChangesQuery`, `useIntegrationVersionWorkflowChangeQuery`, `useAutomationWorkflowProjectVersionWorkflowChangesQuery({id, projectVersion})`, `useAutomationWorkflowProjectVersionWorkflowChangeQuery({id, projectVersion, workflowUuid})`.
- Produces schema types (from `graphql-types.ts`): `VersionWorkflowChanges`, `VersionWorkflowChangeSummary`, `VersionWorkflowChange`, `WorkflowDefinitionChange`, `WorkflowDefinitionValueChange`, `WorkflowChangeType`, `WorkflowNodeChangeKind`, `WorkflowNodeChangeTarget`.
- Selection sets: summaries select `baseVersion version workflows { workflowUuid label changeType changeCount }`; details select `workflowUuid label changeType baseVersion version oldDefinition newDefinition changes { kind target name type label parentPath valueChanges { path oldValue newValue } }`.

- [ ] **Step 1: Write the six operation files**
- [ ] **Step 2: Generate**

Run: `cd client && npx graphql-codegen && npm run typecheck`
Expected: generated hooks exist; typecheck passes.

- [ ] **Step 3: Commit operations, then generated files separately**

```bash
git add client/src/graphql/automation/configuration/projectVersionWorkflowChange*.graphql client/src/graphql/embedded/configuration/integrationVersionWorkflowChange*.graphql client/src/graphql/embedded/configuration/automationWorkflowProjectVersionWorkflowChange*.graphql
git commit -m "6849 client - Add version workflow change GraphQL operations"
git add client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "6849 client - Generate"
```

---

### Task 10: Shared changed-workflows list and summary utils

**Files:**
- Create: `client/src/shared/components/version-workflow-changes/versionWorkflowChangesUtils.ts`
- Create: `client/src/shared/components/version-workflow-changes/VersionWorkflowChangesList.tsx`
- Test: `client/src/shared/components/version-workflow-changes/tests/versionWorkflowChangesUtils.test.ts`
- Test: `client/src/shared/components/version-workflow-changes/tests/VersionWorkflowChangesList.test.tsx`

**Interfaces:**
- Produces:
  - `formatVersionWorkflowChangesSummary(workflows: VersionWorkflowChangeSummary[]): string` → `"No workflow changes."` for empty; `"1 workflow changed (1 added)"`; `"3 workflows changed (1 added, 2 modified)"`; parts in order added, modified, removed, omitting zero counts.
  - `getWorkflowChangeTypeBadgeStyleType(changeType: WorkflowChangeType): BadgeStyleType` → ADDED `success-outline`, MODIFIED `warning-outline`, REMOVED `destructive-outline` (export the badge prop type from `Badge.tsx` if not already exported).
  - `formatVersionLabel(version: number | null | undefined, draft: boolean): string` → `"Draft"` when `draft`, else `` `V${version}` ``; `formatVersionRange(baseVersion, version, draft)` → `"V3 → V4"`, `"V4 → Draft"`, or just `"V1"` when `baseVersion` is null.
  - `VersionWorkflowChangesList` props: `{changes?: VersionWorkflowChanges; isLoading: boolean; onWorkflowClick: (workflowUuid: string) => void}`. Renders the summary line (`<p>`), then one `<button type="button">` per workflow with `aria-label={`Open changes of ${label}`}`, the label (or `workflowUuid` when null), a `Badge` with the change type label (`Added`/`Modified`/`Removed`) and `"N changes"` text (singular `"1 change"`); REMOVED rows get no change count. Loading renders `LoadingIcon`.

- [ ] **Step 1: Write the failing tests**

```ts
// versionWorkflowChangesUtils.test.ts
it('formats the empty summary') ; it('formats singular and plural summaries'); it('omits zero counts and orders added, modified, removed'); it('formats version ranges with draft')
// VersionWorkflowChangesList.test.tsx (render from @/shared/util/test-utils)
it('renders the summary and one row per changed workflow with badges');
it('calls onWorkflowClick with the workflow uuid');
it('shows the loading icon while loading');
it('renders the empty state');
```

- [ ] **Step 2: Run to verify failure**

Run: `cd client && npx vitest run src/shared/components/version-workflow-changes`
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests, format, commit**

```bash
cd client && npx vitest run src/shared/components/version-workflow-changes && npm run format && cd ..
git add client/src/shared/components/version-workflow-changes
git commit -m "6849 client - Add the changed workflows list for version history panels"
```

---

### Task 11: Monaco diff wrapper, structured change list and the dialog (Changes + Definition tabs)

**Files:**
- Create: `client/src/shared/components/monacoSetup.ts` — move `window.MonacoEnvironment`, `ensureMonacoConfigured`, `monacoConfigured`, `monacoInstance` out of `MonacoEditorWrapper.tsx`; export `ensureMonacoConfigured(): Promise<void>`, `isMonacoConfigured(): boolean`, `getMonacoInstance(): Monaco | undefined`.
- Modify: `client/src/shared/components/MonacoEditorWrapper.tsx` (import from `monacoSetup`; behaviour unchanged)
- Create: `client/src/shared/components/MonacoDiffEditorWrapper.tsx` — default export; props `{className?: string; language: string; modified: string; original: string}`; renders `DiffEditor` from `@monaco-editor/react` with `options={{readOnly: true, renderSideBySide: true, originalEditable: false, minimap: {enabled: false}}}`, same theme logic as the editor wrapper, `MonacoEditorLoader` while loading.
- Create: `client/src/shared/components/version-workflow-changes/WorkflowDefinitionChangesList.tsx` — props `{changes: WorkflowDefinitionChange[]}`; one section per change: header row with kind `Badge` (`Added` success, `Removed` destructive, `Modified` warning, `Moved` primary-outline), `label || name`, muted `type` and `parentPath` (prefixed `in `), then a two-column list of `valueChanges` (`path`, `oldValue → newValue`, `—` for null). Empty: `"No structural changes."`.
- Create: `client/src/shared/components/version-workflow-changes/WorkflowVersionChangesDialog.tsx` — props `{baseVersionLabel?: string; change?: VersionWorkflowChange; isLoading: boolean; onClose: () => void; open: boolean; versionLabel: string}`; `Dialog`/`DialogContent size="xl"`/`DialogMain`/`DialogHeader title={change.label || change.workflowUuid} description={range}`/`DialogBody`; `Tabs` with triggers `Changes`, `Definition`, `Canvas` (Canvas content added in Task 13; render the trigger disabled with text `Canvas` until then). Definition tab lazy-loads `MonacoDiffEditorWrapper` with `original={change.oldDefinition ?? ''}`, `modified={change.newDefinition ?? ''}`, `language="json"`, inside a `h-[60vh]` container. Range text: `baseVersionLabel ? `${baseVersionLabel} → ${versionLabel}` : versionLabel`.
- Test: `client/src/shared/components/version-workflow-changes/tests/WorkflowDefinitionChangesList.test.tsx`
- Test: `client/src/shared/components/version-workflow-changes/tests/WorkflowVersionChangesDialog.test.tsx` (mock `@/shared/components/MonacoDiffEditorWrapper` with a `<div data-testid="mock-diff-editor" data-original data-modified/>`)

- [ ] **Step 1: Write the failing tests**

```ts
// WorkflowDefinitionChangesList.test.tsx
it('renders a section per change with kind badge, name, type and parent path');
it('renders value changes with old and new values and a dash for missing sides');
it('renders the empty state');
// WorkflowVersionChangesDialog.test.tsx
it('shows the workflow label and the version range in the header');
it('renders the structured changes on the Changes tab');
it('passes old and new definitions to the diff editor on the Definition tab');
it('rendersAddedWorkflowWithoutBaseline'); // original '' and header shows only versionLabel
it('shows the loading icon while loading');
```

- [ ] **Step 2: Run to verify failure**

Run: `cd client && npx vitest run src/shared/components/version-workflow-changes`
- [ ] **Step 3: Implement (extract `monacoSetup.ts` first, run existing Monaco-related tests to confirm no regression: `npx vitest run src/shared/components`)**
- [ ] **Step 4: Run tests, typecheck, format, commit**

```bash
cd client && npx vitest run src/shared/components && npm run typecheck && npm run format && cd ..
git add client/src/shared/components/monacoSetup.ts client/src/shared/components/MonacoEditorWrapper.tsx client/src/shared/components/MonacoDiffEditorWrapper.tsx client/src/shared/components/version-workflow-changes
git commit -m "6849 client - Add the workflow version changes dialog with structured changes and a definition diff"
```

---

### Task 12: Extract `buildWorkflowGraph` from `useLayout`

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/buildWorkflowGraph.ts` (`.tsx` if JSX is needed; `convertTaskToNode` already lives in a `.tsx` util)
- Modify: `client/src/pages/platform/workflow-editor/hooks/useLayout.tsx` (lines ~287–704: replace the inline assembly with a `useMemo` call of `buildWorkflowGraph`)
- Test: `client/src/pages/platform/workflow-editor/utils/buildWorkflowGraph.test.ts`

**Interfaces:**
- Produces: `buildWorkflowGraph({canvasWidth, componentDefinitions, taskDispatcherDefinitions, tasks, triggers}: BuildWorkflowGraphProps): {edges: Edge[]; nodes: Node[]}` where `nodes` = `[...triggerNodes, triggerPlaceholderNode, ...taskNodes (incl. ghost nodes), finalPlaceholderNode]` and `edges` = `[...triggerFanInEdges, ...taskEdges]` — exactly what `useLayout` currently computes as `allNodes` / `taskEdges` after line 704. The function is pure (no hooks, no stores).

- [ ] **Step 1: Write the failing test**

```ts
describe('buildWorkflowGraph', () => {
    it('builds trigger, task and placeholder nodes in order for a linear workflow');   // ids: trigger name, TRIGGER_PLACEHOLDER_NODE_ID, 't1', 't2', FINAL_PLACEHOLDER_NODE_ID
    it('creates an edge between consecutive tasks and a fan-in edge from the trigger');
    it('builds condition child nodes with ghost nodes');                                 // ids include 'condition_1' and a bottom ghost node
    it('returns only placeholders when there are no tasks');
});
```

- [ ] **Step 2: Run to verify failure**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/buildWorkflowGraph.test.ts`
- [ ] **Step 3: Move the block, call it from `useLayout` via `useMemo(() => buildWorkflowGraph({...}), [canvasWidth, componentDefinitions, taskDispatcherDefinitions, tasks, triggers])`, keep all later code using `allNodes`/`taskEdges` names**
- [ ] **Step 4: Run the whole workflow-editor test suite to prove the refactor is behaviour-preserving**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor && npm run typecheck`
Expected: all green.

- [ ] **Step 5: Format, commit**

```bash
cd client && npm run format && cd ..
git add client/src/pages/platform/workflow-editor/utils/buildWorkflowGraph.ts* client/src/pages/platform/workflow-editor/utils/buildWorkflowGraph.test.ts client/src/pages/platform/workflow-editor/hooks/useLayout.tsx
git commit -m "6849 client - Extract the pure workflow graph assembly from useLayout"
```

---

### Task 13: Highlight context, standalone change canvas and the Canvas tab

**Files:**
- Create: `client/src/pages/platform/workflow-editor/providers/workflowChangeHighlightContext.ts` — `WorkflowChangeHighlightContext = createContext<Record<string, WorkflowNodeChangeKind> | null>(null)`, `useWorkflowChangeHighlight(): Record<string, WorkflowNodeChangeKind> | null`.
- Create: `client/src/pages/platform/workflow-editor/utils/getChangeHighlightBorderClassName.ts` — `(kind?: WorkflowNodeChangeKind): string | undefined` → ADDED `'border-green-500 hover:border-green-500'`, MODIFIED `'border-amber-500 hover:border-amber-500'`, MOVED `'border-blue-500 hover:border-blue-500'`, REMOVED `'border-red-500 hover:border-red-500'`.
- Modify: `client/src/pages/platform/workflow-editor/nodes/ReadOnlyNode.tsx` and `ReadOnlyClusterElementNode.tsx` — read the highlight context; when it is non-null, the change border class wins over the test-state border class; add `data-change-kind={kind}` on the node root.
- Create: `client/src/shared/components/version-workflow-changes/parseWorkflowDefinition.ts` — `parseWorkflowDefinition(definition: string): Workflow` → `{definition, description, label, tasks: parsed.tasks ?? [], triggers: parsed.triggers ?? []}`; `collectComponentNames(workflow: Workflow): string[]` (distinct `type.split('/')[0]` of tasks, nested tasks found by the same name+type rule, triggers).
- Create: `client/src/shared/components/version-workflow-changes/WorkflowChangeCanvas.tsx` — props `{componentDefinitions: ComponentDefinitionBasic[]; highlights: Record<string, WorkflowNodeChangeKind>; taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[]; workflow: Workflow}`. Own `ReactFlowProvider` + `ReactFlow` (`nodesDraggable={false} nodesConnectable={false} elementsSelectable={false} fitView proOptions={{hideAttribution: true}}`, `Background` dots like `WorkflowEditor`). Builds elements with `buildWorkflowGraph` (canvasWidth from a `ResizeObserver`/container width, default 756), maps node types `workflow`/`clusterRoot` → `readonly`, `placeholder` → `readonlyPlaceholder`, drops `TRIGGER_PLACEHOLDER_NODE_ID` and `FINAL_PLACEHOLDER_NODE_ID`, sets every edge `type: 'smoothstep'`, then `await getLayoutElements({canvasWidth, direction: 'TB', edges, nodes})`. `nodeTypes = {readonly: ReadOnlyNode, readonlyClusterElement: ReadOnlyClusterElementNode, readonlyPlaceholder: ReadOnlyPlaceholderNode, taskDispatcherBottomGhostNode, taskDispatcherLeftGhostNode, taskDispatcherTopGhostNode}`, `edgeTypes = {smoothstep: RoundedSmoothStepEdge}`. Wrap in `WorkflowChangeHighlightContext.Provider value={highlights}` and `WorkflowEditorReadOnlyContext.Provider value={true}`.
- Create: `client/src/shared/components/version-workflow-changes/WorkflowChangeCanvasTab.tsx` — props `{baseVersionLabel?: string; change: VersionWorkflowChange; versionLabel: string}`; derives `highlights` from `change.changes` (TASK/TRIGGER/CLUSTER_ELEMENT entries keyed by `name`; for the new side: ADDED/MODIFIED/MOVED; for the old side: REMOVED/MODIFIED/MOVED); a two-button segmented toggle (`Button variant="outline"`/`"default"`) labelled `baseVersionLabel` and `versionLabel`, hidden when one side is missing; fetches `useGetComponentDefinitionsQuery({include: componentNames})` and `useGetTaskDispatcherDefinitionsQuery()`; renders `WorkflowChangeCanvas` for the selected side inside a `h-[60vh]` container; a legend row with the four colours.
- Modify: `WorkflowVersionChangesDialog.tsx` — enable the Canvas tab with `WorkflowChangeCanvasTab`.
- Test: `client/src/pages/platform/workflow-editor/utils/getChangeHighlightBorderClassName.test.ts`
- Test: `client/src/shared/components/version-workflow-changes/tests/parseWorkflowDefinition.test.ts`
- Test: `client/src/shared/components/version-workflow-changes/tests/WorkflowChangeCanvasTab.test.tsx` (mock `../WorkflowChangeCanvas` to render `data-testid="mock-change-canvas"` with `data-highlights={JSON.stringify(highlights)}` and `data-label={workflow.label}`; mock the two definition queries)
- Test: `client/src/pages/platform/workflow-editor/nodes/ReadOnlyNode.test.tsx` (add: `applies the change highlight border when a highlight context is present`)

- [ ] **Step 1: Write the failing tests**

```ts
// parseWorkflowDefinition.test.ts
it('parses label, description, tasks and triggers'); it('defaults tasks and triggers to empty arrays'); it('collects distinct component names including nested tasks and triggers');
// WorkflowChangeCanvasTab.test.tsx
it('shows the new canvas with added, modified and moved highlights by default');
it('switches to the old canvas with removed, modified and moved highlights');
it('hides the toggle for an added workflow');          // rendersAddedWorkflowWithoutBaseline counterpart
it('hides the toggle and shows the old canvas for a removed workflow');
// ReadOnlyNode.test.tsx
it('applies the change highlight border when a highlight context is present');
```

- [ ] **Step 2: Run to verify failure**

Run: `cd client && npx vitest run src/shared/components/version-workflow-changes src/pages/platform/workflow-editor/nodes src/pages/platform/workflow-editor/utils`
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests, typecheck, lint, format**

Run: `cd client && npx vitest run src/shared/components/version-workflow-changes src/pages/platform/workflow-editor && npm run typecheck && npx eslint src/shared/components/version-workflow-changes src/pages/platform/workflow-editor/providers src/pages/platform/workflow-editor/nodes src/pages/platform/workflow-editor/utils && npm run format`
- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/providers/workflowChangeHighlightContext.ts client/src/pages/platform/workflow-editor/utils/getChangeHighlightBorderClassName.ts client/src/pages/platform/workflow-editor/utils/getChangeHighlightBorderClassName.test.ts client/src/pages/platform/workflow-editor/nodes client/src/shared/components/version-workflow-changes
git commit -m "6849 client - Show changed nodes on a standalone read-only canvas in the workflow version changes dialog"
```

---

### Task 14: Project version history panel integration

**Files:**
- Create: `client/src/pages/automation/project/components/ProjectVersionWorkflowChanges.tsx` — props `{draft: boolean; expanded: boolean; projectId: number; projectVersion: number}`; `useProjectVersionWorkflowChangesQuery({projectId, projectVersion}, {enabled: expanded})`; renders `VersionWorkflowChangesList`; on row click sets `selectedWorkflowUuid` and renders `ProjectWorkflowVersionChangeDialog`.
- Create: `client/src/pages/automation/project/components/ProjectWorkflowVersionChangeDialog.tsx` — props `{draft: boolean; onClose: () => void; projectId: number; projectVersion: number; workflowUuid: string}`; `useProjectVersionWorkflowChangeQuery(...)`; computes `versionLabel = formatVersionLabel(version, draft)`, `baseVersionLabel = baseVersion == null ? undefined : formatVersionLabel(baseVersion, false)`; renders `WorkflowVersionChangesDialog open`.
- Modify: `client/src/pages/automation/project/components/ProjectVersionHistorySheet.tsx` — `expandedVersions: Set<number>` state; a chevron `Button size="icon" variant="ghost" aria-label={`Toggle changes of version ${version}`}` next to the actions menu (`event.stopPropagation()` on click and keydown); when expanded, render `<ProjectVersionWorkflowChanges … />` below the description inside a `div` whose `onClick`/`onKeyDown` call `event.stopPropagation()`. Draft row: expandable, still `aria-disabled` and non-selectable.
- Modify test: `client/src/pages/automation/project/components/tests/ProjectVersionHistorySheet.test.tsx` — switch to `render` from `@/shared/util/test-utils`; `vi.mock('@/shared/middleware/graphql', …)` with `useProjectVersionWorkflowChangesQuery` and `useProjectVersionWorkflowChangeQuery` spies from `vi.hoisted`; mock `@/shared/components/version-workflow-changes/WorkflowVersionChangesDialog` to a `data-testid="mock-changes-dialog"` element that prints `versionLabel`/`baseVersionLabel`.

- [ ] **Step 1: Write the failing tests (added to the existing file)**

```ts
it('loads the changed workflows only when a version is expanded');            // query enabled false until chevron click
it('shows the changed workflows of a version after expanding it');
it('does not select the version when a changed workflow is clicked');         // aria-selected stays false after clicking a row inside V2
it('opens the changes dialog with V1 → V2 labels for a published version');
it('opens the changes dialog with V2 → Draft labels for the draft version');
```

- [ ] **Step 2: Run to verify failure**

Run: `cd client && npx vitest run src/pages/automation/project/components/tests/ProjectVersionHistorySheet.test.tsx`
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run tests, format, commit**

```bash
cd client && npx vitest run src/pages/automation/project && npm run typecheck && npm run format && cd ..
git add client/src/pages/automation/project/components/ProjectVersionWorkflowChanges.tsx client/src/pages/automation/project/components/ProjectWorkflowVersionChangeDialog.tsx client/src/pages/automation/project/components/ProjectVersionHistorySheet.tsx client/src/pages/automation/project/components/tests/ProjectVersionHistorySheet.test.tsx
git commit -m "6849 client - Show changed workflows per version in the project version history sheet"
```

---

### Task 15: Integration and Automation Workflows panel integration

**Files:**
- Create: `client/src/ee/pages/embedded/integration/components/IntegrationVersionWorkflowChanges.tsx` (props `{draft: boolean; expanded: boolean; integrationId: number; integrationVersion: number}`; uses `useIntegrationVersionWorkflowChangesQuery`) and `IntegrationWorkflowVersionChangeDialog.tsx` (uses `useIntegrationVersionWorkflowChangeQuery`).
- Modify: `client/src/ee/pages/embedded/integration/components/IntegrationVersionHistorySheet.tsx` — track open accordion values in state (`value`/`onValueChange`, initial = current `defaultValue`); remove `disabled` on the Draft trigger; `AccordionContent` renders the description `<p>` then `<IntegrationVersionWorkflowChanges expanded={openValues.includes(…)} … />`.
- Create: `client/src/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowProjectVersionWorkflowChanges.tsx` (props `{draft: boolean; expanded: boolean; projectId: string; projectVersion: number}`; uses `useAutomationWorkflowProjectVersionWorkflowChangesQuery({id: projectId, projectVersion})`) and `AutomationWorkflowProjectWorkflowVersionChangeDialog.tsx`.
- Modify: `.../AutomationWorkflowProjectVersionHistorySheet.tsx` — same accordion changes as the integration sheet.
- Create test: `client/src/ee/pages/embedded/integration/components/tests/IntegrationVersionHistorySheet.test.tsx` (mock `@/ee/shared/queries/embedded/integrationVersions.queries` and the GraphQL hooks).
- Modify test: `.../components/tests/AutomationWorkflowProjectVersionHistorySheet.test.tsx`.

- [ ] **Step 1: Write the failing tests**

```ts
// both sheets
it('expands the draft version and loads its changed workflows');
it('loads changed workflows when a published version is expanded');
it('opens the changes dialog with the version range for the clicked workflow');
```

- [ ] **Step 2: Run to verify failure**

Run: `cd client && npx vitest run src/ee/pages/embedded/integration src/ee/pages/embedded/automation-workflow`
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run the full client check, commit**

```bash
cd client && npm run format && npm run check && cd ..
git add client/src/ee/pages/embedded/integration/components client/src/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components
git commit -m "6849 client - Show changed workflows per version in the integration and automation workflow version history sheets"
```

---

### Task 16: End-to-end verification and docs

**Files:**
- Modify: `docs/src/content/docs/**` only if a page documents the version history panels (search for "Version History"); add one short paragraph on the changed-workflows list and the changes dialog.

- [ ] **Step 1: Full server and client checks**

Run: `./gradlew spotlessApply check` and `cd client && npm run check`
Expected: BUILD SUCCESSFUL; lint, typecheck and tests pass.

- [ ] **Step 2: Manual check against the running app**

Start infra and server (`cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd .. && ./gradlew -p server/apps/server-app bootRun`) and client (`cd client && npm run dev`). In a project with two published versions and a draft with an edited task: expand each version row, open a modified workflow, verify the three tabs (changes list, side-by-side JSON diff, canvas with the amber node), toggle old/new, confirm a parameter named `password` shows `••••••` on both tabs, confirm the editable editor behind the dialog is unchanged after closing. Repeat for an Integration and an embedded Automation Workflow project.

- [ ] **Step 3: Commit docs (if any) and finish**

```bash
git add docs
git commit -m "6849 docs - Describe the changed workflows list in the version history panels"
```
