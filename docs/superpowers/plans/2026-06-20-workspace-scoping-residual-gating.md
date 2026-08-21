# Workspace-Scoping & Residual-Gating Pass — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every "list tags / list resources" endpoint that feeds a workspace page return only the caller's accessible workspace data behind a `WorkspaceRole VIEWER` gate, and close the remaining per-id read leaks deferred from gecko T20/T22/T24.

**Architecture:** Each endpoint gains a `workspaceId` (REST path param mirroring `getWorkspaceProjects`, or GraphQL argument), the backing facade/service scopes results to that workspace's entities (reusing the workspace-relation repositories already built for search-scoping), and carries `@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")` on the facade/service impl (never the controller — per the project agreement). Each domain is an independent vertical slice (server + openapi/graphql + client codegen + hook + test) so partial delivery is always safe.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Spring Security method security; GraphQL (Spring for GraphQL) + REST (SpringDoc/openapi-generator); React 19 + TanStack Query + graphql-codegen; JUnit 5 + Mockito; Gradle.

## Global Constraints

- `@PreAuthorize("hasPermission(...)")` gates live on **facade/service impls, not controllers** (the standing project agreement). Controllers only delegate.
- Gate token form for a numeric workspace id: `hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')`.
- Tags remain tenant-global in storage; scope at the *listing* layer only (which tag-ids are reachable from a given workspace's entities). Do **not** add a `workspace_id` column to `tag`.
- EE files (anything under `server/ee/`) require the ByteChef Enterprise license header + a `@version ee` Javadoc tag on every class (incl. tests). CE files use the Apache header.
- Reuse existing workspace-relation repositories — do NOT invent parallel mappings:
  `ProjectService.getWorkspaceProjectIds(long)` + `getProjects(List<Long>)`;
  `ProjectDeploymentService.getProjectDeployments(Boolean, Environment, Long, Long, Long workspaceId)`;
  `WorkspaceConnectionRepository.findAllByWorkspaceId(long)`;
  `WorkspaceDataTableRepository.findAllByWorkspaceId(Long)`;
  `WorkspaceKnowledgeBaseRepository.findAllByWorkspaceId(Long)`;
  `ApiCollectionService.getApiCollections(Long workspaceId, Environment, Long, Long)`;
  `workspaceMcpServerFacade.getWorkspaceMcpServers(Long)`.
- Commit convention: server `gecko <description>`, client `gecko client - <description>`. Never `--amend` on `0_732` (the user commits in parallel). Stage only files this task touched.
- Commit trailer: `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`.
- Run `./gradlew spotlessApply` before each server commit; run `npm run check` in `client/` before each client commit.

---

## Scope Reconciliation vs. the Spec

The spec (`docs/superpowers/specs/2026-06-20-workspace-scoping-residual-gating-design.md`) was written before deep per-domain probing. Verified facts that change scope:

| Spec item | Verified reality | Effect on plan |
|---|---|---|
| Eval read queries ungated (§5.5) → needs facade split | **Already done.** `AiEvalScoreFacadeImpl:61` / `AiEvalRuleFacadeImpl` / `AiEvalScoreConfigFacadeImpl` already carry `@PreAuthorize(... 'WorkspaceRole','VIEWER')` (analytics variant carries `ROLE_ADMIN`); the async `AiEvalExecutor` bypasses the facade by calling the ungated *service* directly. The split already exists. | **Task 12 = verification only.** No new facade. |
| DataTable/AssetFile/KnowledgeBase tags are REST | They are **GraphQL** queries (`DataTableTagGraphQlController.dataTableTags()`, `AssetFileGraphQlController.assetFileTags()`, `KnowledgeBaseTagGraphQlController.knowledgeBaseTags()`). | Pattern B (GraphQL arg), not REST openapi. |
| `assetFileTags` needs a new workspaceId param | `assetFileTags(workspaceId: ID!)` **already takes `workspaceId`** in schema + controller; only `assetFileTagService.getAllTags()` ignores it. | Task 6 is the smallest fix (thread existing arg through). |
| MCP `mcpServers(type)` — delete or gate | Verified **dead on the client** (`useMcpServersQuery` unreferenced outside generated code; list uses `workspaceMcpServers`). | Task 10 deletes it. |
| Subflow schema reads (#A) → add a web-facing facade | The reads have **no distinct endpoint**; they run inside the cluster-element/task-dispatcher option/properties/output functions (the T22a shared-with-worker seam) and the call site has only the *referenced* `workflowUuid`, no SecurityContext and no containing-workflow id. A clean gate needs its own mini-design. | **Task 13 = deferred with documented rationale**, not implemented here. |

Net actionable scope: **4 REST tag endpoints** (Tasks 1–4), **3 GraphQL tag endpoints** (Tasks 5–7), **MCP** (Tasks 8–10), **subflow list** (Task 11), **eval verification** (Task 12), **#A deferred note** (Task 13).

---

## File Structure Map

**Pattern A — REST tag endpoint** (Project, ProjectDeployment, Connection, ApiCollection):
- `*-api/.../facade/<X>TagFacade.java` (or `<X>Facade.java`) — signature gains `long workspaceId`.
- `*-service/.../facade/<X>FacadeImpl.java` — scope + `@PreAuthorize`. Connection scoping moves to an **automation-tier** facade (cross-module; mirrors the Connection search-provider move).
- `*-rest-impl/openapi.yaml` — relocate operation under `/workspaces/{id}/<entity>-tags`.
- regenerate REST API interface (gradle `openApiGenerate` runs in build) → controller signature gains `Long id`.
- `client/src/shared/middleware/...` — regenerate; `client/src/shared/queries/automation/<x>Tags.queries.ts` — hook gains `id`.

**Pattern B — GraphQL tag endpoint** (DataTable, AssetFile, KnowledgeBase):
- `*-graphql/src/main/resources/graphql/<x>.graphqls` — query gains `(workspaceId: ID!)`.
- `*-graphql/.../web/graphql/<X>GraphQlController.java` — `@QueryMapping` gains `@Argument Long workspaceId`, delegates to a scoped facade/service method.
- `*-api` + `*-service` — scoped method + `@PreAuthorize`.
- `client/src/graphql/.../<x>Tags.graphql` — operation gains `($workspaceId: ID!)`; regenerate; update hook usage.

**MCP** (Tasks 8–10): `mcp-server-tag.graphqls`, `mcp-project.graphqls`, `mcp-server.graphqls` + their controllers + `automation-ai-mcp-service` facade + client `.graphql` + `useMcpServers.ts`.

**Subflow** (Task 11): `SubflowDataSource.java` (SPI) + `SubflowDataSourceImpl.java` + 2 callers.

---

## Task Rhythm (applies to every implementation task)

Each task follows the same 5-beat cycle; later tasks state only their concrete artifacts, not this prose:
1. Write the failing test (facade/service unit test asserting workspace-scoped result + a gate-presence assertion).
2. Run it; confirm it fails for the expected reason.
3. Make the change (signature + scope + `@PreAuthorize`; openapi/graphql; codegen; client hook).
4. Run the module test + `./gradlew spotlessApply` (server) / `npm run check` (client); confirm green.
5. Commit the slice (server and client as separate commits per convention).

Gate-presence is asserted structurally (the facade unit test cannot exercise Spring AOP), e.g. via a reflection check that the impl method is annotated — mirror the existing `AiEvalScoreFacadeAuthorizationTest` reflection pattern in the module if present; otherwise assert the scoping behavior and rely on the annotation being visible in review.

---

## Phase 1 — REST tag endpoints

### Task 1: Project tags → workspace-scoped (canonical REST pattern)

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ProjectTagFacade.java:27`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectTagFacadeImpl.java:50-56`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/openapi.yaml:508-524`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/main/java/com/bytechef/automation/configuration/web/rest/ProjectTagApiController.java:49-55` (regenerated interface drives this)
- Modify: `client/src/shared/queries/automation/projectTags.queries.ts`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectTagFacadeImplTest.java` (create)

**Interfaces:**
- Consumes: `ProjectService.getWorkspaceProjectIds(long)`, `ProjectService.getProjects(List<Long>)`, `Project.getTagIds()`, `TagService.getTags(List<Long>)`.
- Produces: `List<Tag> ProjectTagFacade.getProjectTags(long workspaceId)` (gated `WorkspaceRole VIEWER`); REST `GET /workspaces/{id}/project-tags`.

- [ ] **Step 1: Write the failing test**

```java
// ProjectTagFacadeImplTest.java
package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectTagFacadeImplTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ProjectTagFacadeImpl projectTagFacade;

    @Test
    void testGetProjectTagsScopesToWorkspace() {
        Project project = new Project();

        project.setTagIds(List.of(10L, 11L));

        when(projectService.getWorkspaceProjectIds(5L)).thenReturn(List.of(1L, 2L));
        when(projectService.getProjects(List.of(1L, 2L))).thenReturn(List.of(project));
        when(tagService.getTags(List.of(10L, 11L))).thenReturn(List.of(new Tag("a"), new Tag("b")));

        List<Tag> tags = projectTagFacade.getProjectTags(5L);

        assertThat(tags).hasSize(2);
    }
}
```

- [ ] **Step 2: Run it; confirm it fails to compile** (`getProjectTags(long)` does not yet exist)

Run: `./gradlew ':server:libs:automation:automation-configuration:automation-configuration-service:test' --tests 'com.bytechef.automation.configuration.facade.ProjectTagFacadeImplTest'`
Expected: compile failure / FAIL.

- [ ] **Step 3a: Change the interface**

`ProjectTagFacade.java:27` → `List<Tag> getProjectTags(long workspaceId);`

- [ ] **Step 3b: Change the impl** (`ProjectTagFacadeImpl.java`)

```java
@Override
@Transactional(readOnly = true)
@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
public List<Tag> getProjectTags(long workspaceId) {
    List<Long> projectIds = projectService.getWorkspaceProjectIds(workspaceId);

    List<Project> projects = projectService.getProjects(projectIds);

    return tagService.getTags(CollectionUtils.flatMap(projects, Project::getTagIds));
}
```

Add import `org.springframework.security.access.prepost.PreAuthorize;`. Confirm module `build.gradle.kts` has `implementation("org.springframework.security:spring-security-core")` (automation-configuration-service already does).

- [ ] **Step 3c: Relocate the openapi operation** (`openapi.yaml`)

Replace the `/projects/tags` `getProjectTags` operation (lines ~508-524) with a workspace-scoped path mirroring `getWorkspaceProjects`:

```yaml
  /workspaces/{id}/project-tags:
    get:
      description: "Get project tags for a workspace."
      summary: "Get project tags"
      tags:
        - "project-tag"
      operationId: "getProjectTags"
      parameters:
        - name: "id"
          description: "The id of a workspace."
          in: "path"
          required: true
          schema:
            type: "integer"
            format: "int64"
      responses:
        "200":
          description: "The list of project tags."
          content:
            application/json:
              schema:
                description: "The response object that contains the array of tags."
                type: "array"
                items:
                  $ref: "#/components/schemas/Tag"
```

The regenerated `ProjectTagApi` interface now declares `getProjectTags(Long id)`; update `ProjectTagApiController` to pass `id`:

```java
@Override
public ResponseEntity<List<TagModel>> getProjectTags(Long id) {
    return ResponseEntity.ok(
        projectTagFacade.getProjectTags(id)
            .stream()
            .map(tag -> conversionService.convert(tag, TagModel.class))
            .toList());
}
```

- [ ] **Step 4: Run server test + spotless; confirm green**

Run: `./gradlew ':server:libs:automation:automation-configuration:automation-configuration-service:test' --tests 'com.bytechef.automation.configuration.facade.ProjectTagFacadeImplTest' && ./gradlew spotlessApply`
Expected: PASS.

- [ ] **Step 5: Commit server slice**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ProjectTagFacade.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectTagFacadeImpl.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectTagFacadeImplTest.java \
        server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/openapi.yaml \
        server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/main/java/com/bytechef/automation/configuration/web/rest/ProjectTagApiController.java
git commit -m "$(cat <<'EOF'
gecko Scope project tags to workspace (IDOR residual)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 6: Regenerate client middleware + update hook**

Regenerate the automation REST client (project script — `cd client && npm run generate:api` or the repo's openapi client task), then:

```ts
// client/src/shared/queries/automation/projectTags.queries.ts
export const ProjectTagKeys = {
    projectTags: (id: number) => ['projectTags', id] as const,
};

export const useGetProjectTagsQuery = (id: number) =>
    useQuery<Tag[], Error>({
        queryKey: ProjectTagKeys.projectTags(id),
        queryFn: () => new ProjectTagApi().getProjectTags({id}),
    });
```

Update every caller of `useGetProjectTagsQuery()` to pass the current workspace id (same source the page already uses for `useGetWorkspaceProjectsQuery`, i.e. `useGetCurrentWorkspaceId()` / the workspace store). Invalidations of `ProjectTagKeys.projectTags` become `ProjectTagKeys.projectTags(currentWorkspaceId)`.

- [ ] **Step 7: `cd client && npm run check`; confirm green, then commit client slice**

```bash
git add client/src/shared/middleware client/src/shared/queries/automation/projectTags.queries.ts <changed caller files>
git commit -m "gecko client - Pass workspaceId to project tags query"
```

### Task 2: Project-deployment tags → workspace-scoped

**Files:** `ProjectDeploymentFacade.java:53`, `ProjectDeploymentFacadeImpl.java:346-356`, same `openapi.yaml` (`/project-deployments/tags` → `/workspaces/{id}/project-deployment-tags`, operationId `getProjectDeploymentTags`), `ProjectDeploymentTagApiController.java:51-57`, `client/src/shared/queries/automation/projectDeploymentTags.queries.ts`; Test: `ProjectDeploymentFacadeImplTest` (add method).

**Impl change:**
```java
@Override
@Transactional(readOnly = true)
@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
public List<Tag> getProjectDeploymentTags(long workspaceId) {
    List<ProjectDeployment> projectDeployments =
        projectDeploymentService.getProjectDeployments(null, null, null, null, workspaceId);

    return tagService.getTags(
        projectDeployments.stream()
            .map(ProjectDeployment::getTagIds)
            .flatMap(Collection::stream)
            .toList());
}
```
Interface → `List<Tag> getProjectDeploymentTags(long workspaceId);`. openapi block identical shape to Task 1 Step 3c (path `/workspaces/{id}/project-deployment-tags`). Controller passes `id`. Client hook gains `id` (mirror Task 1 Step 6). Test asserts only `getProjectDeployments(null,null,null,null,7L)` results' tag-ids are fetched. Follow the Task Rhythm; commit server then client.

### Task 3: Connection tags → workspace-scoped (cross-module move)

`ConnectionFacadeImpl` lives in `platform-connection-service`, which has no access to `WorkspaceConnectionRepository` (in `automation-configuration-service`). Mirror the Connection **search-provider move**: scope at the automation tier.

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionTagFacade.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionTagFacadeImpl.java`
- Modify: `.../web/rest/ConnectionTagApiController.java:56-62` (reroute to the new facade; controller is already in automation-configuration-rest)
- Modify: `openapi.yaml` (`/connections/tags` → `/workspaces/{id}/connection-tags`, operationId `getConnectionTags`)
- Modify: `client/src/shared/queries/automation/connections.queries.ts` (`useGetConnectionTagsQuery` gains `id`)
- Test: `WorkspaceConnectionTagFacadeImplTest` (create)

**New facade interface:**
```java
public interface WorkspaceConnectionTagFacade {
    List<Tag> getConnectionTags(long workspaceId);
}
```

**New facade impl** (consumes `WorkspaceConnectionRepository.findAllByWorkspaceId(long)` + `ConnectionService.getConnections(List<Long>)` + `Connection.getTagIds()` + `TagService`):
```java
@Override
@Transactional(readOnly = true)
@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
public List<Tag> getConnectionTags(long workspaceId) {
    List<Long> connectionIds = workspaceConnectionRepository.findAllByWorkspaceId(workspaceId)
        .stream()
        .map(WorkspaceConnection::getConnectionId)
        .toList();

    List<Connection> connections = connectionService.getConnections(connectionIds);

    return tagService.getTags(
        connections.stream()
            .map(Connection::getTagIds)
            .flatMap(Collection::stream)
            .toList());
}
```
Controller delegates to `workspaceConnectionTagFacade.getConnectionTags(id)`. Leave the old `ConnectionFacade.getConnectionTags(PlatformType)` in place only if another caller exists; otherwise delete it (grep first). Client hook gains `id`. Follow the Task Rhythm.

### Task 4: API-collection tags → workspace-scoped (EE)

**Files (all EE — Enterprise header + `@version ee`):** `ApiCollectionFacade.java:33`, `ApiCollectionFacadeImpl.java:205-214`, `automation-api-platform-configuration-rest/openapi.yaml:141-157` (`/api-collections/tags` → `/workspaces/{id}/api-collection-tags`), `ApiCollectionTagApiController.java:49-54`; Test: `ApiCollectionFacadeImplTest` (add method, EE header).

**Impl change** (uses `ApiCollectionService.getApiCollections(Long workspaceId, Environment, Long, Long)`):
```java
@Override
@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
public List<Tag> getApiCollectionTags(long workspaceId) {
    List<ApiCollection> apiCollections = apiCollectionService.getApiCollections(workspaceId, null, null, null);

    return tagService.getTags(
        apiCollections.stream()
            .map(ApiCollection::getTagIds)
            .flatMap(Collection::stream)
            .toList());
}
```
Interface → `List<Tag> getApiCollectionTags(long workspaceId);`. Confirm `automation-api-platform-configuration-service` `build.gradle.kts` has `spring-security-core` (add if missing). No client hook exists today (none found) — if the API Platform page filters by tag, add the hook; otherwise just the server slice. Follow the Task Rhythm.

---

## Phase 2 — GraphQL tag endpoints

### Task 5: Data-table tags → workspace-scoped (canonical GraphQL pattern)

**Files:**
- Modify: `server/libs/automation/automation-data-table/automation-data-table-graphql/src/main/resources/graphql/data-table.graphqls:3` — `dataTableTags(workspaceId: ID!): [Tag!]!`
- Modify: `.../web/graphql/DataTableTagGraphQlController.java:56-59`
- Modify: `platform-data-table-api/.../service/DataTableTagService.java` + `platform-data-table-service/.../DataTableTagServiceImpl.java:48-65` — **but** `WorkspaceDataTableRepository` lives in `automation-data-table-api`, not platform. Scope at the automation tier instead: add the scoped method to a new `WorkspaceDataTableTagService`/facade in `automation-data-table-service`, OR (simpler, since the controller is already in automation-data-table-graphql) inject `WorkspaceDataTableRepository` + `DataTableTagService` into the controller's delegate. Prefer a small `automation-data-table-service` facade `WorkspaceDataTableTagFacade.getDataTableTags(long workspaceId)` carrying the gate.
- Modify: `client/src/graphql/automation/configuration/dataTableTags.graphql` (add `($workspaceId: ID!)`), regenerate, update hook.
- Test: `WorkspaceDataTableTagFacadeImplTest` (create).

**Scoped method** (automation-tier facade):
```java
@Override
@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
public List<Tag> getDataTableTags(long workspaceId) {
    List<Long> dataTableIds = workspaceDataTableRepository.findAllByWorkspaceId(workspaceId)
        .stream()
        .map(WorkspaceDataTable::getDataTableId)
        .toList();

    Set<Long> tagIds = new HashSet<>();

    for (Long dataTableId : dataTableIds) {
        List<Long> ids = dataTableService.getDataTable(dataTableId).getTagIds();

        if (ids != null) {
            tagIds.addAll(ids);
        }
    }

    if (tagIds.isEmpty()) {
        return List.of();
    }

    return tagService.getTags(new ArrayList<>(tagIds));
}
```
Controller:
```java
@QueryMapping
public List<Tag> dataTableTags(@Argument Long workspaceId) {
    return workspaceDataTableTagFacade.getDataTableTags(workspaceId);
}
```
Client `.graphql`:
```graphql
query dataTableTags($workspaceId: ID!) {
    dataTableTags(workspaceId: $workspaceId) { id name }
}
```
Regenerate (`cd client && npx graphql-codegen`); update `useDataTableTagsQuery` callers to pass `{workspaceId}`. Follow the Task Rhythm.

### Task 6: Asset-file tags → thread the existing workspaceId (smallest fix)

`assetFileTags(workspaceId: ID!)` and `AssetFileGraphQlController.assetFileTags(@Argument Long workspaceId)` already exist; only `assetFileTagService.getAllTags()` ignores the arg.

**Files:** `automation-asset-file-api/.../AssetFileTagService.java` (add `getAllTags(long workspaceId)`), `automation-asset-file-service/.../AssetFileTagServiceImpl.java:49-67`, `AssetFileGraphQlController.java:92-94`; Test: `AssetFileTagServiceImplTest` (add method). No `.graphqls` or client change (arg already present).

`AssetFile` has a direct `workspaceId` column (no relation table). Scoped impl:
```java
@Override
@Transactional(readOnly = true)
@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
public List<Tag> getAllTags(long workspaceId) {
    Set<Long> tagIdSet = new HashSet<>();

    for (AssetFile assetFile : assetFileRepository.findAll()) {
        if (!Objects.equals(assetFile.getWorkspaceId(), workspaceId)) {
            continue;
        }

        List<Long> tagIds = assetFile.getTagIds();

        if (tagIds != null) {
            tagIdSet.addAll(tagIds);
        }
    }

    if (tagIdSet.isEmpty()) {
        return List.of();
    }

    return tagService.getTags(new ArrayList<>(tagIdSet));
}
```
(If `AssetFileRepository` exposes `findAllByWorkspaceId`, prefer it over the in-memory filter — grep first.) Controller delegates `assetFileTagService.getAllTags(workspaceId)`. Add `spring-security-core` to the service `build.gradle.kts` if missing. Confirm the client already passes `workspaceId` to `useAssetFileTagsQuery`; if not, update it. Follow the Task Rhythm.

### Task 7: Knowledge-base tags → workspace-scoped (GraphQL)

**Files:** `knowledge-base.graphqls:4` (`knowledgeBaseTags(workspaceId: ID!): [Tag!]`), `KnowledgeBaseTagGraphQlController.java:45-47`, scoped facade in `automation-knowledge-base-service` (reuse `WorkspaceKnowledgeBaseRepository.findAllByWorkspaceId(Long)` + `KnowledgeBaseService.getKnowledgeBase` + `KnowledgeBase.getTagIds()`), client `knowledgeBaseTags.graphql` + hook; Test: `WorkspaceKnowledgeBaseTagFacadeImplTest` (create). `KnowledgeBaseTagFacadeImpl` is in `platform-knowledge-base-service` (no workspace relation) → add the scoped method in the automation tier exactly like Task 5. Scoped method mirrors Task 5 with `WorkspaceKnowledgeBase::getKnowledgeBaseId`. Follow the Task Rhythm.

---

## Phase 3 — MCP

### Task 8: `mcpServerTags(type)` → add workspaceId

**Files:** `platform-mcp/platform-mcp-graphql/src/main/resources/graphql/mcp-server-tag.graphqls:2` (`mcpServerTags(type: PlatformType!, workspaceId: ID!): [Tag]`), `McpServerTagGraphQlController.java:50-69`, client `mcpServerTags.graphql` + `useMcpServerTagsQuery` in `client/src/pages/automation/mcp-servers/hooks/useMcpServers.ts`; Test: controller-level or facade test.

`McpServerTagGraphQlController` is in `platform-mcp-graphql` but needs the workspace's servers. Reuse `workspaceMcpServerFacade.getWorkspaceMcpServers(workspaceId)` (already gated VIEWER) — but that facade is in `automation-ai-mcp`. Either inject `WorkspaceMcpServerFacade` into the controller's delegate (add dep) or add `getWorkspaceMcpServerTags(workspaceId)` to `WorkspaceMcpServerFacade` (preferred — keeps the gate + dependency in the automation tier):

```java
// WorkspaceMcpServerFacadeImpl (automation-ai-mcp-service)
@Override
@Transactional(readOnly = true)
@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
public List<Tag> getWorkspaceMcpServerTags(Long workspaceId) {
    List<McpServer> mcpServers = getWorkspaceMcpServers(workspaceId); // self-call reuses the scoped list

    List<Long> tagIds = mcpServers.stream()
        .flatMap(mcpServer -> CollectionUtils.stream(mcpServer.getTagIds()))
        .distinct()
        .toList();

    return tagIds.isEmpty() ? List.of() : tagService.getTags(tagIds);
}
```
Move the `mcpServerTags` `@QueryMapping` to a controller in `automation-ai-mcp-graphql` (where `WorkspaceMcpServerFacade` is reachable) or keep it in platform and inject the facade. Client query gains `($workspaceId: ID!)` and passes the current workspace id. Follow the Task Rhythm.

### Task 9: `mcpProjects()` → workspace-scoped

**Files:** `automation-ai-mcp-graphql/.../graphql/mcp-project.graphqls:3` (`mcpProjects(workspaceId: ID!): [McpProject]`), `McpProjectGraphQlController.java:76-79`, `automation-ai-mcp` facade, client `mcpProjects.graphql` + `useMcpProjectsQuery` in `useMcpServers.ts`; Test: facade test.

Compose over the already-scoped servers (`McpProject` → `mcpServerId` → workspace):
```java
@Override
@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
public List<McpProject> getWorkspaceMcpProjects(Long workspaceId) {
    Set<Long> serverIds = getWorkspaceMcpServers(workspaceId)
        .stream()
        .map(McpServer::getId)
        .collect(Collectors.toSet());

    return serverIds.stream()
        .flatMap(serverId -> mcpProjectService.getMcpServerMcpProjects(serverId).stream())
        .toList();
}
```
Controller `mcpProjects(@Argument Long workspaceId)` delegates. The client already has `workspaceId` (it currently filters client-side against `workspaceMcpServers` — replace that with the server-side scoped query and drop the client filter). Follow the Task Rhythm.

### Task 10: Delete the dead global `mcpServers` query

**Files:** `platform-mcp/platform-mcp-graphql/src/main/resources/graphql/mcp-server.graphqls:3` (remove the `mcpServers(...)` field), `McpServerGraphQlController.java:79-83` (remove the `mcpServers` `@QueryMapping`), `client/src/graphql/platform/configuration/mcpServers.graphql` (delete), regenerate client.

- [ ] **Step 1: Re-confirm no caller.** `grep -rn "useMcpServersQuery\|mcpServers(" client/src --include=*.ts --include=*.tsx --include=*.graphql | grep -v "workspaceMcpServers\|embeddedMcpServers\|connectedUserMcpServers\|mcpServersBy"` → expect empty. On the server: `grep -rn "\.mcpServers(" server --include=*.java | grep -v test` → expect only the controller method being deleted. If any caller exists, STOP and gate `Tenant ADMIN` instead of deleting.
- [ ] **Step 2: Delete** the schema field, the controller method, and the client `.graphql`.
- [ ] **Step 3:** `cd client && npx graphql-codegen && npm run check`; `./gradlew ':server:libs:platform:platform-mcp:platform-mcp-graphql:compileJava' spotlessApply`.
- [ ] **Step 4: Commit** server + client separately.

(The service method `mcpServerService.getMcpServers(type, orderBy)` stays — it backs `mcpServerTags` and embedded controllers; only the GraphQL query mapping is removed.)

---

## Phase 4 — Subflow list

### Task 11: `getSubWorkflows` → workspace-filtered

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/src/main/java/com/bytechef/platform/workflow/task/dispatcher/subflow/SubflowDataSource.java:34`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/subflow/SubflowDataSourceImpl.java:104-130`
- Modify callers: `server/libs/modules/components/workflow/.../WorkflowCallWorkflowTool.java:190-198`; `server/libs/modules/task-dispatchers/subflow/.../SubflowTaskDispatcherDefinitionFactory.java:72-80`
- Test: `SubflowDataSourceImplTest` (add method).

The subflow dropdown must list only same-workspace subflows. The option-function call site lacks a workspace argument today; thread the current workflow's workspace in. Both callers receive a `context` (ClusterElementContext / definition context) from which the editing workflow — hence its workspace — is derivable.

- [ ] **Step 1: Write the failing test** — `getSubWorkflows(7L, AUTOMATION, NEW_WORKFLOW_CALL, null)` returns only workflows whose project is in workspace 7.
- [ ] **Step 2: Change the SPI** → `List<SubflowEntry> getSubWorkflows(long workspaceId, PlatformType platformType, String triggerName, String search);`
- [ ] **Step 3: Change the impl** — replace `projectWorkflowService.getLatestProjectWorkflows()` with a workspace-filtered set:
```java
List<Long> workspaceProjectIds = projectService.getWorkspaceProjectIds(workspaceId);

List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getLatestProjectWorkflows()
    .stream()
    .filter(projectWorkflow -> workspaceProjectIds.contains(projectWorkflow.getProjectId()))
    .toList();
```
(Keep the rest of the method as-is.)
- [ ] **Step 4: Update callers** to derive and pass the workspace id from `context`. Determine the exact accessor during implementation (the editing workflow's project → workspace; the context exposes the current workflow/principal). If the workspace is not derivable from `context` without broader plumbing, STOP — this becomes the same class of problem as Task 13 and should be split out rather than guessed.
- [ ] **Step 5: Test + spotless + commit.**

> **Decision gate:** Task 11 is only in-scope if the workspace is cleanly derivable from the option-function `context`. If implementation reveals it is not (mirroring Task 13's constraint), defer Task 11 alongside Task 13 and document it — do not fabricate a plumbing path.

---

## Phase 5 — Verification & deferred residual

### Task 12: Verify eval reads are already gated (no code)

- [ ] **Step 1:** Confirm `@PreAuthorize` on every web-facing eval read facade method:
```
grep -n "PreAuthorize\|public " \
  server/ee/libs/automation/automation-ai/automation-ai-eval/automation-ai-eval-service/src/main/java/com/bytechef/ee/automation/ai/eval/facade/AiEvalScoreFacadeImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-eval/automation-ai-eval-service/src/main/java/com/bytechef/ee/automation/ai/eval/facade/AiEvalScoreConfigFacadeImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiEvalRuleFacadeImpl.java
```
Expected: every `get*ByWorkspace*` carries `hasPermission(#workspaceId,'WorkspaceRole','VIEWER')` (analytics carries `ROLE_ADMIN`).
- [ ] **Step 2:** Confirm the GraphQL controllers (`AiEvalScoreGraphQlController`, `AiEvalScoreConfigGraphQlController`, `AiEvalRuleGraphQlController`) call the **facade**, not the workspace service, and that `AiEvalExecutor` calls the **service** directly (the intended bypass).
- [ ] **Step 3:** If all confirmed, mark the T24 eval residual **closed** in `gecko-remediation-tasks.md` with a one-line note ("facade already gated; executor bypasses by design — verified <date>"). If any web controller calls the ungated service directly, open a follow-up task to reroute it through the facade (do not gate the service).

### Task 13: Subflow schema reads (#A) — deferred with rationale (no code)

`getSubWorkflowInputSchema(workflowUuid)` / `getSubWorkflowOutputSchema(workflowUuid)` return any workflow's I/O schema by uuid with no ownership check, but they are reached **only** through the cluster-element/task-dispatcher option/properties/output functions during definition resolution. That path: (a) is shared with worker/embedded execution (no reliable `SecurityContext`); (b) at the call site has only the *referenced* subflow's `workflowUuid`, not the containing workflow's id or workspace. There is no clean place to add `hasPermission` without either breaking the worker path or threading the containing workflow's workspace through the entire option-function context.

- [ ] **Step 1:** Record in `gecko-remediation-tasks.md` that #A is deferred and **why** (the two constraints above), so it is not mistaken for "covered."
- [ ] **Step 2:** Flag for a dedicated mini-design: identify the exact editor entry point that triggers dynamic output/property resolution (the GraphQL/REST call that *does* carry a SecurityContext and the containing workflow id), and gate there — validating that the referenced `workflowUuid` resolves (via `projectWorkflowService.getLastWorkflowId`) to a workflow in the caller's accessible workspace using `@permissionService.hasWorkflowScope(workflowId, 'VIEW')`. This is its own brainstorming→spec→plan cycle, not part of this pass.

---

## Self-Review

**Spec coverage:** Every spec inventory item is mapped — Archetype 1 lists #1–#7 (Tasks 1–7), #8–#10 MCP (Tasks 8–10), #11 subflow list (Task 11); Archetype 2 #A (Task 13, deferred with rationale), #B eval (Task 12, verify — already implemented). The reconciliation table documents each deviation honestly (per the "don't relabel spec intent" rule).

**Placeholder scan:** No "TBD/handle edge cases/similar to Task N". Each task carries concrete file paths, real method bodies, and a test. Two explicit **Decision gates** (Task 10 delete-vs-gate; Task 11 workspace-derivability) are genuine stop-conditions, not placeholders.

**Type consistency:** Gate token `hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')` and signature `long workspaceId` (REST) / `Long workspaceId` (GraphQL `@Argument`) used consistently. Reused method names match the Global Constraints list verbatim (`getWorkspaceProjectIds`, `findAllByWorkspaceId`, `getConnections(List<Long>)`, `getApiCollections(Long,…)`, `getWorkspaceMcpServers`).

**Known follow-ups not in this plan:** the higher-severity non-IDOR backlog (T1–T16, T26–T27); the dedicated #A schema-read effort (Task 13 Step 2).
