# CommandBar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `GlobalSearchDialog` with a registry-driven CommandBar where every entry — navigation, create actions, and individual resources — is a command contributed through a client-side SPI.

**Architecture:** A module-level Zustand registry collects `CommandSourceI`s from two doors: `registerCommandSource` at bootstrap and `useRegisterCommands` from React. A command holds either an ordered `CommandActionType[]` (executed in sequence) or a `CommandChildrenI` resolver (pushes a nested sub-mode). Navigation to a page-owned dialog is expressed as `navigate` followed by a one-shot `intent` that the dialog claims on mount. The server's existing `SearchAssetProvider` fan-out gains a type filter so a type-scoped sub-mode queries one provider instead of ten.

**Tech Stack:** React 19.2, TypeScript 6.0, Zustand (with `persist`), cmdk, TanStack Query, Vitest 4 + React Testing Library, TailwindCSS 4.3. Server: Java 25, Spring Boot 4.0.8, Spring GraphQL, JUnit 5 + AssertJ + Mockito.

**Spec:** `docs/superpowers/specs/2026-08-24-command-bar-design.md`

## Global Constraints

- Client interface names end in `I` or `Props` (`CommandI`, `CommandBarDialogProps`) — enforced by `@typescript-eslint/naming-convention`.
- Object keys in natural ascending order (ESLint `sort-keys`). `--fix` does NOT fix these; fix by hand.
- Named imports sorted alphabetically inside `{}`; `type` imports sort by name, not grouped.
- Lucide icons imported with the `Icon` suffix: `ArrowRightIcon`, not `ArrowRight`.
- `useRef` variables end in `Ref`.
- Use `twMerge` from `tailwind-merge` for conditional classes. Never `cn()`.
- Hook order in components: `useState` → `useRef` → store hooks → other custom hooks → derived/`useMemo`/`useCallback` → `useEffect` → `return`. All `useEffect` calls last.
- No short or cryptic names anywhere, including arrow-function parameters and loop variables.
- No `_` prefix on private methods.
- Java: exactly one blank line before control statements and after a variable modification that a following statement uses; no blank line before a class's closing brace.
- Java test method names are camelCase with no underscores; this applies to private helpers too.
- Vitest: reset stores in `beforeEach`; never flush async store updates with a fixed `setTimeout` — use `waitFor` on the resulting state.
- `vi.mock` factories hoist. Declare any ref they close over with `vi.hoisted`.
- Commit messages: client-side `2396 client - <description>`; server-side `2396 <description>`.
- Before any commit touching the client: `cd client && npm run check`. Before any commit touching the server: `./gradlew spotlessApply` then `./gradlew check`.

---

## File Structure

**Server (modified):**
- `server/libs/automation/automation-search/automation-search-graphql/src/main/resources/graphql/automation-search.graphqls` — add `ASSET_FILE`, `AssetFileSearchResult`, `projectWorkflowId`, and the `types` argument
- `server/libs/automation/automation-search/automation-search-api/.../facade/AutomationSearchFacade.java` — `types` parameter
- `server/libs/automation/automation-search/automation-search-service/.../facade/AutomationSearchFacadeImpl.java` — filter providers before fan-out
- `server/libs/automation/automation-search/automation-search-graphql/.../AutomationSearchGraphQlController.java` — pass `types` through
- `server/libs/automation/automation-configuration/automation-configuration-service/.../search/ProjectSearchResult.java` and `ProjectSearchAssetProvider.java` — `projectWorkflowId`

**Server (created):**
- `automation-search-graphql/src/test/java/.../SearchAssetTypeSchemaParityTest.java` — guards enum/schema drift

**Client (created), `client/src/shared/command-bar/`:**
- `types.ts` — `CommandI`, `CommandActionType`, `CommandChildrenI`, `CommandContextI`, `CommandRunContextI`, `CommandSourceI`, `RecentCommandI`
- `useCommandSourceRegistry.ts` — the registry store, `registerCommandSource`, dev-mode invariant assertion
- `useRegisterCommands.ts` — the React door
- `useCommandBarStore.ts` — `open`, `query`, `stack`
- `useCommandIntentStore.ts` — publish/claim, unclaimed warning
- `useCommandIntent.ts` — the claim hook
- `executeCommand.ts` — the action pipeline
- `useCommandRecentsStore.ts` — persisted recents
- `resourceCommandRoutes.ts` — `SearchAssetType` → group, icon, and action builder
- `sources/resourceCommandSource.ts`, `sources/createCommandSource.ts`
- `useRegisterNavigationCommands.ts`
- `tests/` — one test file per module above

**Client (created), other:**
- `client/src/components/CommandBar/CommandBarDialog.tsx`
- `client/src/shared/navigation/navigationItems.ts`

**Client (deleted):**
- `client/src/components/GlobalSearch/GlobalSearchDialog.tsx`

---

## Phase 0 — Make search correct before building on it

### Task 1: Close the ASSET_FILE schema gap

`AssetFileSearchAssetProvider` is an active `@Component` returning `AssetFileSearchResult`, and `SearchAssetType.ASSET_FILE` exists in Java, but the GraphQL schema has neither the enum value nor a matching type. Any query matching an asset file fails interface resolution.

**Files:**
- Create: `server/libs/automation/automation-search/automation-search-graphql/src/test/java/com/bytechef/automation/search/web/graphql/SearchAssetTypeSchemaParityTest.java`
- Modify: `server/libs/automation/automation-search/automation-search-graphql/src/main/resources/graphql/automation-search.graphqls`

**Interfaces:**
- Consumes: `com.bytechef.automation.search.SearchAssetType` (already an `api` dependency of the graphql module)
- Produces: nothing later tasks import; the schema gains `ASSET_FILE` and `AssetFileSearchResult`

- [ ] **Step 1: Write the failing test**

`SearchAssetTypeSchemaParityTest.java`:

```java
package com.bytechef.automation.search.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.search.SearchAssetType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every SearchAssetType a provider can return must exist in the GraphQL enum, otherwise a matching result fails
 * interface resolution at query time rather than at build time.
 *
 * @author Ivica Cardic
 */
class SearchAssetTypeSchemaParityTest {

    @Test
    void testEveryAssetTypeIsDeclaredInTheSchema() throws IOException {
        String schema = readSchema();

        List<String> missing = Arrays.stream(SearchAssetType.values())
            .map(Enum::name)
            .filter(name -> !schema.contains(name))
            .toList();

        assertThat(missing).isEmpty();
    }

    private String readSchema() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/graphql/automation-search.graphqls")) {
            assertThat(inputStream).isNotNull();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:libs:automation:automation-search:automation-search-graphql:test --tests '*SearchAssetTypeSchemaParityTest*'
```

Expected: FAIL — `Expecting empty but was: ["ASSET_FILE"]`.

- [ ] **Step 3: Add the enum value and the type**

In `automation-search.graphqls`, append `ASSET_FILE` to the `SearchAssetType` enum (append only, never reorder), and add:

```graphql
type AssetFileSearchResult implements SearchResult {
    id: ID!
    name: String!
    description: String
    type: SearchAssetType!
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :server:libs:automation:automation-search:automation-search-graphql:test --tests '*SearchAssetTypeSchemaParityTest*'
```

Expected: PASS.

- [ ] **Step 5: Format, check, commit**

```bash
./gradlew spotlessApply
./gradlew :server:libs:automation:automation-search:automation-search-graphql:check
git add server/libs/automation/automation-search/automation-search-graphql
git commit -m "2396 Declare ASSET_FILE in the automation search GraphQL schema"
```

---

### Task 2: Carry projectWorkflowId on ProjectSearchResult

Opening a project needs both a project id and a project-workflow id — `ProjectListItem` links to `projects/{id}/project-workflows/{projectWorkflowIds[0]}`. The search result carries only the project id, so there is no way to build that route from a search hit.

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/search/ProjectSearchResult.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/search/ProjectSearchAssetProvider.java`
- Modify: `server/libs/automation/automation-search/automation-search-graphql/src/main/resources/graphql/automation-search.graphqls`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/search/ProjectSearchAssetProviderTest.java` (exists)

**Interfaces:**
- Produces: `ProjectSearchResult(Long id, String name, String description, Long projectWorkflowId, Long workspaceId)`; GraphQL `ProjectSearchResult.projectWorkflowId: ID` (nullable). Task 13 reads this field.

- [ ] **Step 1: Read the existing test to match its setup**

```bash
sed -n '17,120p' server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/search/ProjectSearchAssetProviderTest.java
```

The provider currently takes `(ProjectService, ProjectVisibilityFilter)`. It will take `(ProjectService, ProjectVisibilityFilter, ProjectWorkflowService)`, so the existing test's construction call must be updated in the same commit.

- [ ] **Step 2: Write the failing test**

Add to `ProjectSearchAssetProviderTest`:

```java
    @Test
    void testResultCarriesTheFirstProjectWorkflowId() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectVisibilityFilter projectVisibilityFilter = mock(ProjectVisibilityFilter.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

        Project project = mock(Project.class);

        when(project.getId()).thenReturn(5L);
        when(project.getName()).thenReturn("Billing");
        when(project.getWorkspaceId()).thenReturn(10L);
        when(projectService.getProjects(false, null, null, null, null, null)).thenReturn(List.of(project));
        when(projectVisibilityFilter.filterVisible(List.of(project))).thenReturn(List.of(project));

        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getId()).thenReturn(77L);
        when(projectWorkflow.getProjectId()).thenReturn(5L);
        when(projectWorkflowService.getLatestProjectWorkflows()).thenReturn(List.of(projectWorkflow));

        ProjectSearchAssetProvider provider = new ProjectSearchAssetProvider(
            projectService, projectVisibilityFilter, projectWorkflowService);

        List<ProjectSearchResult> results = provider.search("bill", 10);

        assertThat(results).singleElement()
            .extracting(ProjectSearchResult::projectWorkflowId)
            .isEqualTo(77L);
    }

    @Test
    void testProjectWorkflowIdIsNullWhenTheProjectHasNoWorkflows() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectVisibilityFilter projectVisibilityFilter = mock(ProjectVisibilityFilter.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

        Project project = mock(Project.class);

        when(project.getId()).thenReturn(5L);
        when(project.getName()).thenReturn("Billing");
        when(projectService.getProjects(false, null, null, null, null, null)).thenReturn(List.of(project));
        when(projectVisibilityFilter.filterVisible(List.of(project))).thenReturn(List.of(project));
        when(projectWorkflowService.getLatestProjectWorkflows()).thenReturn(List.of());

        ProjectSearchAssetProvider provider = new ProjectSearchAssetProvider(
            projectService, projectVisibilityFilter, projectWorkflowService);

        List<ProjectSearchResult> results = provider.search("bill", 10);

        assertThat(results).singleElement()
            .extracting(ProjectSearchResult::projectWorkflowId)
            .isNull();
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectSearchAssetProviderTest*'
```

Expected: FAIL to compile — `ProjectSearchAssetProvider` has no three-argument constructor and `ProjectSearchResult` has no `projectWorkflowId`.

- [ ] **Step 4: Widen the record**

`ProjectSearchResult.java`:

```java
public record ProjectSearchResult(
    Long id, String name, String description, Long projectWorkflowId, Long workspaceId)
    implements SearchResult<Long> {
```

- [ ] **Step 5: Resolve the first project-workflow per project**

In `ProjectSearchAssetProvider`, add the `ProjectWorkflowService` collaborator and build the lookup once, before the stream:

```java
        Map<Long, Long> projectIdToProjectWorkflowId = projectWorkflowService.getLatestProjectWorkflows()
            .stream()
            .collect(
                Collectors.toMap(
                    ProjectWorkflow::getProjectId, ProjectWorkflow::getId, (first, second) -> first));
```

then pass `projectIdToProjectWorkflowId.get(project.getId())` into the record, in the new fourth position.

- [ ] **Step 6: Add the GraphQL field**

In `automation-search.graphqls`, add `projectWorkflowId: ID` to `type ProjectSearchResult`. It is nullable because a project with no workflows has none.

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectSearchAssetProviderTest*'
./gradlew :server:libs:automation:automation-search:automation-search-graphql:test
```

Expected: PASS.

- [ ] **Step 8: Format, check, commit**

```bash
./gradlew spotlessApply
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:check
git add server/libs/automation
git commit -m "2396 Carry the first project workflow id on project search results"
```

---

### Task 3: Correct the search result routes

Eight of nine `navigate()` targets in `GlobalSearchDialog` are routes that do not exist. This task fixes them in place, against the current dialog, so the fix is reviewable on its own and lands even if the CommandBar work is deferred.

**Files:**
- Modify: `client/src/graphql/automation/search/automationSearch.graphql`
- Create: `client/src/components/GlobalSearch/globalSearchRoutes.ts`
- Modify: `client/src/components/GlobalSearch/GlobalSearchDialog.tsx`
- Test: `client/src/components/GlobalSearch/tests/globalSearchRoutes.test.ts` (create)

**Interfaces:**
- Produces: `SearchResultRouteInputI` and `buildSearchResultRoute(result): string | undefined`, exported from `globalSearchRoutes.ts`. Task 12 moves both to `resourceCommandRoutes.ts` unchanged. They live in their own module rather than in the component file so the `react-refresh/only-export-components` rule stays satisfied.

- [ ] **Step 1: Add projectWorkflowId to the GraphQL operation**

In `automationSearch.graphql`, add to the `ProjectSearchResult` inline fragment:

```graphql
        ... on ProjectSearchResult {
            projectWorkflowId
        }
```

- [ ] **Step 2: Regenerate the GraphQL types**

```bash
cd client && npx graphql-codegen
```

Expected: `src/shared/middleware/graphql.ts` and `graphql-types.ts` regenerate with `projectWorkflowId` on `ProjectSearchResult` and `ASSET_FILE` on `SearchAssetType`.

- [ ] **Step 3: Write the failing test**

`client/src/components/GlobalSearch/tests/globalSearchRoutes.test.ts`:

```ts
import {buildSearchResultRoute} from '@/components/GlobalSearch/globalSearchRoutes';
import {SearchAssetType} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

describe('buildSearchResultRoute', () => {
    it('routes a workflow to its project-workflow editor', () => {
        expect(
            buildSearchResultRoute({id: '77', projectId: '5', type: SearchAssetType.Workflow})
        ).toBe('/automation/projects/5/project-workflows/77');
    });

    it('routes a project through its first project workflow', () => {
        expect(
            buildSearchResultRoute({id: '5', projectWorkflowId: '77', type: SearchAssetType.Project})
        ).toBe('/automation/projects/5/project-workflows/77');
    });

    it('routes a project with no workflows to the projects list', () => {
        expect(buildSearchResultRoute({id: '5', type: SearchAssetType.Project})).toBe('/automation/projects');
    });

    it('routes a data table to the unhyphenated path', () => {
        expect(buildSearchResultRoute({id: '3', type: SearchAssetType.DataTable})).toBe('/automation/datatables/3');
    });

    it('routes an asset file to its detail page', () => {
        expect(buildSearchResultRoute({id: '9', type: SearchAssetType.AssetFile})).toBe('/automation/asset-files/9');
    });

    it('routes a type with no detail route to its list page', () => {
        expect(buildSearchResultRoute({id: '4', type: SearchAssetType.Connection})).toBe('/automation/connections');
        expect(buildSearchResultRoute({id: '4', type: SearchAssetType.Deployment})).toBe('/automation/deployments');
        expect(buildSearchResultRoute({id: '4', type: SearchAssetType.ApiCollection})).toBe(
            '/automation/api-platform/api-collections'
        );
    });
});
```

- [ ] **Step 4: Run the test to verify it fails**

```bash
cd client && npx vitest run src/components/GlobalSearch/tests/globalSearchRoutes.test.ts
```

Expected: FAIL — `buildSearchResultRoute` is not exported.

- [ ] **Step 5: Implement the route builder**

Create `client/src/components/GlobalSearch/globalSearchRoutes.ts`. The verified routes come from `client/src/routes.tsx`:

```ts
interface SearchResultRouteInputI {
    collectionId?: string;
    id: string;
    knowledgeBaseId?: string;
    projectId?: string;
    projectWorkflowId?: string;
    type: SearchAssetType;
}

/**
 * Maps a search result to a route that actually exists in routes.tsx. Five asset types (deployments, connections,
 * API collections, API endpoints, knowledge base documents) have no detail route at all -- they are rendered by list
 * pages with in-page selection -- so they resolve to their list page.
 */
export const buildSearchResultRoute = (result: SearchResultRouteInputI): string | undefined => {
    switch (result.type) {
        case SearchAssetType.ApiCollection:
        case SearchAssetType.ApiEndpoint:
            return '/automation/api-platform/api-collections';
        case SearchAssetType.AssetFile:
            return `/automation/asset-files/${result.id}`;
        case SearchAssetType.Connection:
            return '/automation/connections';
        case SearchAssetType.DataTable:
            return `/automation/datatables/${result.id}`;
        case SearchAssetType.Deployment:
            return '/automation/deployments';
        case SearchAssetType.KnowledgeBase:
            return `/automation/knowledge-bases/${result.id}`;
        case SearchAssetType.KnowledgeBaseDocument:
            return result.knowledgeBaseId ? `/automation/knowledge-bases/${result.knowledgeBaseId}` : undefined;
        case SearchAssetType.Project:
            return result.projectWorkflowId
                ? `/automation/projects/${result.id}/project-workflows/${result.projectWorkflowId}`
                : '/automation/projects';
        case SearchAssetType.Workflow:
            return result.projectId
                ? `/automation/projects/${result.projectId}/project-workflows/${result.id}`
                : undefined;
        default:
            return undefined;
    }
};
```

- [ ] **Step 6: Route every CommandItem through it**

Replace each hardcoded template literal in the nine `CommandGroup` blocks with `handleSelect(buildSearchResultRoute(result))`, and make `handleSelect` ignore an `undefined` path:

```ts
    const handleSelect = useCallback(
        (path: string | undefined) => {
            if (!path) {
                return;
            }

            navigate(path);
            onOpenChange(false);
            setQuery('');
        },
        [navigate, onOpenChange]
    );
```

- [ ] **Step 7: Run the test to verify it passes**

```bash
cd client && npx vitest run src/components/GlobalSearch/tests/globalSearchRoutes.test.ts
```

Expected: PASS.

- [ ] **Step 8: Check and commit**

```bash
cd client && npm run check
git add client/src/components/GlobalSearch client/src/graphql client/src/shared/middleware
git commit -m "2396 client - Point global search results at routes that exist"
```

---

### Task 4: Type filter on automationSearch

A type-scoped sub-mode ("Open workflow") must not fan out to ten providers to use one.

**Files:**
- Modify: `server/libs/automation/automation-search/automation-search-api/src/main/java/com/bytechef/automation/search/facade/AutomationSearchFacade.java`
- Modify: `server/libs/automation/automation-search/automation-search-service/src/main/java/com/bytechef/automation/search/facade/AutomationSearchFacadeImpl.java`
- Modify: `server/libs/automation/automation-search/automation-search-graphql/.../AutomationSearchGraphQlController.java`
- Modify: `server/libs/automation/automation-search/automation-search-graphql/src/main/resources/graphql/automation-search.graphqls`
- Test: `server/libs/automation/automation-search/automation-search-service/src/test/java/com/bytechef/automation/search/facade/AutomationSearchFacadeImplTest.java` (exists)

**Interfaces:**
- Produces: `List<SearchResult<?>> search(String query, int limit, Set<SearchAssetType> types)` — null or empty means all types. GraphQL: `automationSearch(query: String!, limit: Int, types: [SearchAssetType!])`. Task 13 sends `types`.

- [ ] **Step 1: Write the failing tests**

Add to `AutomationSearchFacadeImplTest`. Reuse the existing `TestResult` record and the existing mock setup shape:

```java
    @Test
    void testOnlyRequestedTypeProvidersAreCalled() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace accessible = mock(Workspace.class);

        when(accessible.getId()).thenReturn(10L);
        when(workspaceFacade.getUserWorkspaces(1L)).thenReturn(List.of(accessible));

        AtomicBoolean connectionProviderCalled = new AtomicBoolean();

        SearchAssetProvider workflowProvider = new RecordingProvider(SearchAssetType.WORKFLOW, new AtomicBoolean());
        SearchAssetProvider connectionProvider = new RecordingProvider(
            SearchAssetType.CONNECTION, connectionProviderCalled);

        AutomationSearchFacadeImpl facade = new AutomationSearchFacadeImpl(
            List.of(workflowProvider, connectionProvider), userService, workspaceFacade);

        facade.search("q", 10, Set.of(SearchAssetType.WORKFLOW));

        assertThat(connectionProviderCalled).isFalse();
    }

    @Test
    void testNullTypesMeansEveryProvider() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace accessible = mock(Workspace.class);

        when(accessible.getId()).thenReturn(10L);
        when(workspaceFacade.getUserWorkspaces(1L)).thenReturn(List.of(accessible));

        AtomicBoolean connectionProviderCalled = new AtomicBoolean();

        AutomationSearchFacadeImpl facade = new AutomationSearchFacadeImpl(
            List.of(new RecordingProvider(SearchAssetType.CONNECTION, connectionProviderCalled)), userService,
            workspaceFacade);

        facade.search("q", 10, null);

        assertThat(connectionProviderCalled).isTrue();
    }

    private record RecordingProvider(SearchAssetType assetType, AtomicBoolean called) implements SearchAssetProvider {

        @Override
        public List<? extends SearchResult> search(String query, int limit) {
            called.set(true);

            return List.of();
        }

        @Override
        public SearchAssetType getAssetType() {
            return assetType;
        }
    }
```

Add the imports `java.util.Set` and `java.util.concurrent.atomic.AtomicBoolean`.

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew :server:libs:automation:automation-search:automation-search-service:test --tests '*AutomationSearchFacadeImplTest*'
```

Expected: FAIL to compile — no three-argument `search`.

- [ ] **Step 3: Widen the facade interface**

In `AutomationSearchFacade`, replace the two-argument method:

```java
    /**
     * Search across automation entities matching the given query.
     *
     * @param query the search query string
     * @param limit maximum number of results per category
     * @param types the asset types to search; null or empty searches every type
     * @return list of search results
     */
    List<SearchResult<?>> search(String query, int limit, @Nullable Set<SearchAssetType> types);
```

- [ ] **Step 4: Filter providers before the fan-out**

In `AutomationSearchFacadeImpl.search`, before the loop that builds futures:

```java
        List<SearchAssetProvider> requestedProviders = types == null || types.isEmpty()
            ? providers
            : providers.stream()
                .filter(provider -> types.contains(provider.getAssetType()))
                .toList();
```

then iterate `requestedProviders` instead of `providers`. Everything else in the method — the tenant id capture, the `SecurityContext` re-establishment, the workspace accessibility filter — is unchanged.

- [ ] **Step 5: Thread it through the controller and schema**

`AutomationSearchGraphQlController`:

```java
    @QueryMapping(name = "automationSearch")
    public List<SearchResult<?>> automationSearch(
        @Argument String query, @Argument Integer limit, @Argument List<SearchAssetType> types) {

        int effectiveLimit = limit != null ? limit : DEFAULT_LIMIT;

        return automationSearchFacade.search(
            query, effectiveLimit, types == null ? null : Set.copyOf(types));
    }
```

`automation-search.graphqls`:

```graphql
    automationSearch(query: String!, limit: Int, types: [SearchAssetType!]): [SearchResult!]!
```

- [ ] **Step 6: Update the other caller**

```bash
grep -rn "automationSearchFacade.search\|AutomationSearchFacade" server --include=*.java | grep -v "/build/"
```

Update `AutomationSearchFacadeSecurityContextTest` to pass `null` for `types`.

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./gradlew :server:libs:automation:automation-search:automation-search-service:test
```

Expected: PASS, including the two pre-existing tests.

- [ ] **Step 8: Format, check, commit**

```bash
./gradlew spotlessApply
./gradlew :server:libs:automation:automation-search:automation-search-service:check
git add server/libs/automation/automation-search
git commit -m "2396 Add a type filter to automation search"
```

---

## Phase 1 — The command SPI core

Every module in this phase is framework-light and unit-testable without rendering anything. Build them before any UI so the UI has a settled contract to consume.

### Task 5: Command types and the source registry

**Files:**
- Create: `client/src/shared/command-bar/types.ts`
- Create: `client/src/shared/command-bar/useCommandSourceRegistry.ts`
- Create: `client/src/shared/command-bar/useRegisterCommands.ts`
- Test: `client/src/shared/command-bar/tests/useCommandSourceRegistry.test.ts`
- Test: `client/src/shared/command-bar/tests/useRegisterCommands.test.tsx`

**Interfaces:**
- Produces: `CommandI`, `CommandActionType`, `CommandChildrenI`, `CommandContextI`, `CommandRunContextI`, `CommandSourceI`; `useCommandSourceRegistry` (Zustand store with `register`, `reset`, `sources`), `registerCommandSource(source): () => void`, `collectCommands(sources, context): CommandI[]`, `useRegisterCommands(commands, dependencies)`. Every later client task consumes these.

- [ ] **Step 1: Write the types**

`client/src/shared/command-bar/types.ts`:

```ts
import {type LucideIcon} from 'lucide-react';
import {type NavigateFunction} from 'react-router-dom';

export type CommandActionType =
    | {run: (runContext: CommandRunContextI) => Promise<void> | void; type: 'callback'}
    | {key: string; payload?: unknown; type: 'intent'}
    | {to: string; type: 'navigate'};

export interface CommandContextI {
    edition: string | undefined;
    featureFlags: (featureFlag: string) => boolean;
    pathname: string;
}

export interface CommandRunContextI {
    command: CommandI;
    context: CommandContextI;
    navigate: NavigateFunction;
}

export interface CommandChildrenI {
    minQueryLength?: number;
    placeholder: string;
    resolve: (query: string, signal: AbortSignal) => Promise<CommandI[]>;
}

export interface CommandI {
    actions?: CommandActionType[];
    children?: CommandChildrenI;
    group?: string;
    icon?: LucideIcon;
    id: string;
    keywords?: string[];
    subtitle?: string;
    title: string;
    when?: (context: CommandContextI) => boolean;
}

export interface CommandSourceI {
    getCommands: (context: CommandContextI) => CommandI[];
    id: string;
}

export interface RecentCommandI {
    actions: CommandActionType[];
    id: string;
    title: string;
}
```

- [ ] **Step 2: Write the failing registry test**

`client/src/shared/command-bar/tests/useCommandSourceRegistry.test.ts`:

```ts
import {collectCommands, registerCommandSource, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {type CommandContextI, type CommandI} from '@/shared/command-bar/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const context: CommandContextI = {
    edition: 'EE',
    featureFlags: () => true,
    pathname: '/automation/projects',
};

const command = (id: string, overrides: Partial<CommandI> = {}): CommandI => ({
    actions: [{to: `/${id}`, type: 'navigate'}],
    id,
    title: id,
    ...overrides,
});

describe('useCommandSourceRegistry', () => {
    beforeEach(() => {
        useCommandSourceRegistry.getState().reset();
    });

    it('collects commands from every registered source', () => {
        registerCommandSource({getCommands: () => [command('a')], id: 'first'});
        registerCommandSource({getCommands: () => [command('b')], id: 'second'});

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands.map((collected) => collected.id)).toEqual(['a', 'b']);
    });

    it('unregisters through the returned callback', () => {
        const unregister = registerCommandSource({getCommands: () => [command('a')], id: 'first'});

        unregister();

        expect(useCommandSourceRegistry.getState().sources).toHaveLength(0);
    });

    it('passes the context to each source', () => {
        const getCommands = vi.fn().mockReturnValue([]);

        registerCommandSource({getCommands, id: 'first'});

        collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(getCommands).toHaveBeenCalledWith(context);
    });

    it('drops commands whose when predicate is false', () => {
        registerCommandSource({
            getCommands: () => [command('a', {when: () => false}), command('b')],
            id: 'first',
        });

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands.map((collected) => collected.id)).toEqual(['b']);
    });

    it('keeps the last registration when two sources share an id', () => {
        registerCommandSource({getCommands: () => [command('a', {title: 'first'})], id: 'first'});
        registerCommandSource({getCommands: () => [command('a', {title: 'second'})], id: 'second'});

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands).toHaveLength(1);
        expect(commands[0].title).toBe('second');
    });

    it('rejects a command declaring both actions and children', () => {
        registerCommandSource({
            getCommands: () => [
                command('a', {children: {placeholder: 'Search...', resolve: async () => []}}),
            ],
            id: 'first',
        });

        expect(() => collectCommands(useCommandSourceRegistry.getState().sources, context)).toThrow(
            /exactly one of/
        );
    });

    it('rejects a command declaring neither actions nor children', () => {
        registerCommandSource({getCommands: () => [{id: 'a', title: 'a'}], id: 'first'});

        expect(() => collectCommands(useCommandSourceRegistry.getState().sources, context)).toThrow(
            /exactly one of/
        );
    });
});
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandSourceRegistry.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 4: Implement the registry**

`client/src/shared/command-bar/useCommandSourceRegistry.ts`:

```ts
import {type CommandContextI, type CommandI, type CommandSourceI} from '@/shared/command-bar/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CommandSourceRegistryStateI {
    register: (source: CommandSourceI) => () => void;
    reset: () => void;
    sources: CommandSourceI[];
}

export const useCommandSourceRegistry = create<CommandSourceRegistryStateI>()(
    devtools((set) => ({
        register: (source: CommandSourceI) => {
            set((state) => ({sources: [...state.sources, source]}));

            return () => {
                set((state) => ({
                    sources: state.sources.filter((registeredSource) => registeredSource !== source),
                }));
            };
        },
        reset: () => set(() => ({sources: []})),
        sources: [],
    }))
);

export function registerCommandSource(source: CommandSourceI): () => void {
    return useCommandSourceRegistry.getState().register(source);
}

/**
 * A command declares either an ordered action list or a nested children resolver, never both and never neither.
 * TypeScript cannot express that without an artificial discriminant on every command literal, so it is checked here,
 * where commands materialise, and it throws in development so the failure surfaces at its cause.
 */
function assertCommandShape(command: CommandI): void {
    const hasActions = (command.actions ?? []).length > 0;
    const hasChildren = command.children !== undefined;

    if (hasActions === hasChildren) {
        const message = `Command "${command.id}" must declare exactly one of "actions" or "children".`;

        if (import.meta.env.DEV) {
            throw new Error(message);
        }

        console.error(message);
    }
}

export function collectCommands(sources: CommandSourceI[], context: CommandContextI): CommandI[] {
    const commandsById = new Map<string, CommandI>();

    for (const source of sources) {
        for (const command of source.getCommands(context)) {
            assertCommandShape(command);

            if (command.when && !command.when(context)) {
                continue;
            }

            if (commandsById.has(command.id) && import.meta.env.DEV) {
                console.warn(`Duplicate command id "${command.id}"; the last registration wins.`);
            }

            commandsById.set(command.id, command);
        }
    }

    return [...commandsById.values()];
}
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandSourceRegistry.test.ts
```

Expected: PASS, 7 tests.

- [ ] **Step 6: Write the failing hook test**

`client/src/shared/command-bar/tests/useRegisterCommands.test.tsx`:

```tsx
import {type CommandI} from '@/shared/command-bar/types';
import {useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {useRegisterCommands} from '@/shared/command-bar/useRegisterCommands';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const commands: CommandI[] = [{actions: [{to: '/a', type: 'navigate'}], id: 'a', title: 'A'}];

describe('useRegisterCommands', () => {
    beforeEach(() => {
        useCommandSourceRegistry.getState().reset();
    });

    it('registers on mount', () => {
        renderHook(() => useRegisterCommands(commands, []));

        expect(useCommandSourceRegistry.getState().sources).toHaveLength(1);
    });

    it('unregisters on unmount', () => {
        const {unmount} = renderHook(() => useRegisterCommands(commands, []));

        unmount();

        expect(useCommandSourceRegistry.getState().sources).toHaveLength(0);
    });

    it('does not accumulate sources across re-renders', () => {
        const {rerender} = renderHook(() => useRegisterCommands(commands, []));

        rerender();
        rerender();

        expect(useCommandSourceRegistry.getState().sources).toHaveLength(1);
    });
});
```

- [ ] **Step 7: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useRegisterCommands.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 8: Implement the hook**

`client/src/shared/command-bar/useRegisterCommands.ts`:

```ts
import {type CommandI} from '@/shared/command-bar/types';
import {registerCommandSource} from '@/shared/command-bar/useCommandSourceRegistry';
import {useEffect, useRef} from 'react';

let nextHookSourceId = 0;

/**
 * The React door into the command registry: commands live exactly as long as the component that registered them.
 * `dependencies` is the caller's own dependency array -- pass the values the commands close over.
 */
export function useRegisterCommands(commands: CommandI[], dependencies: unknown[]): void {
    const sourceIdRef = useRef<string | undefined>(undefined);

    if (!sourceIdRef.current) {
        sourceIdRef.current = `hook-${nextHookSourceId++}`;
    }

    useEffect(() => {
        return registerCommandSource({getCommands: () => commands, id: sourceIdRef.current!});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, dependencies);
}
```

- [ ] **Step 9: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useRegisterCommands.test.tsx
```

Expected: PASS, 3 tests.

- [ ] **Step 10: Check and commit**

```bash
cd client && npm run check
git add client/src/shared/command-bar
git commit -m "2396 client - Add the command source registry and its React door"
```

---

### Task 6: The intent store

Creation dialogs own their `open` state internally and expose no controlled prop, so a command reaches them by publishing a one-shot intent the dialog claims on mount.

**Files:**
- Create: `client/src/shared/command-bar/useCommandIntentStore.ts`
- Create: `client/src/shared/command-bar/useCommandIntent.ts`
- Test: `client/src/shared/command-bar/tests/useCommandIntentStore.test.ts`
- Test: `client/src/shared/command-bar/tests/useCommandIntent.test.tsx`

**Interfaces:**
- Produces: `useCommandIntentStore` (`claim(key)`, `intent`, `publish(key, payload?)`, `reset()`), `useCommandIntent(key, handler)`, `UNCLAIMED_INTENT_WARNING_DELAY`. Task 7 calls `publish`; Tasks 14 and 15 call `useCommandIntent`.

- [ ] **Step 1: Write the failing store test**

`client/src/shared/command-bar/tests/useCommandIntentStore.test.ts`:

```ts
import {UNCLAIMED_INTENT_WARNING_DELAY, useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('useCommandIntentStore', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        useCommandIntentStore.getState().reset();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('returns the intent to the matching claimant', () => {
        useCommandIntentStore.getState().publish('project.create', {id: 7});

        expect(useCommandIntentStore.getState().claim('project.create')).toEqual({
            key: 'project.create',
            payload: {id: 7},
        });
    });

    it('returns undefined to a non-matching claimant and leaves the intent pending', () => {
        useCommandIntentStore.getState().publish('project.create');

        expect(useCommandIntentStore.getState().claim('dataTable.create')).toBeUndefined();
        expect(useCommandIntentStore.getState().intent).toBeDefined();
    });

    it('clears the intent synchronously so a second claimant finds nothing', () => {
        useCommandIntentStore.getState().publish('project.create');

        useCommandIntentStore.getState().claim('project.create');

        expect(useCommandIntentStore.getState().claim('project.create')).toBeUndefined();
    });

    it('warns when an intent goes unclaimed', () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});

        useCommandIntentStore.getState().publish('project.create');

        vi.advanceTimersByTime(UNCLAIMED_INTENT_WARNING_DELAY);

        expect(warn).toHaveBeenCalledWith(expect.stringContaining('project.create'));
    });

    it('does not warn when the intent was claimed in time', () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});

        useCommandIntentStore.getState().publish('project.create');
        useCommandIntentStore.getState().claim('project.create');

        vi.advanceTimersByTime(UNCLAIMED_INTENT_WARNING_DELAY);

        expect(warn).not.toHaveBeenCalled();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandIntentStore.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the store**

`client/src/shared/command-bar/useCommandIntentStore.ts`:

```ts
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export const UNCLAIMED_INTENT_WARNING_DELAY = 5000;

export interface CommandIntentI {
    key: string;
    payload?: unknown;
}

interface CommandIntentStateI {
    claim: (key: string) => CommandIntentI | undefined;
    intent: CommandIntentI | undefined;
    publish: (key: string, payload?: unknown) => void;
    reset: () => void;
}

// Held outside the store because a timeout handle is not state anyone should read or persist.
let unclaimedWarningTimeoutId: ReturnType<typeof setTimeout> | undefined;

function cancelUnclaimedWarning() {
    if (unclaimedWarningTimeoutId !== undefined) {
        clearTimeout(unclaimedWarningTimeoutId);

        unclaimedWarningTimeoutId = undefined;
    }
}

export const useCommandIntentStore = create<CommandIntentStateI>()(
    devtools((set, get) => ({
        claim: (key: string) => {
            const {intent} = get();

            if (intent?.key !== key) {
                return undefined;
            }

            cancelUnclaimedWarning();

            // Cleared synchronously so a page rendering the same dialog twice opens only one of them.
            set(() => ({intent: undefined}));

            return intent;
        },
        intent: undefined,
        publish: (key: string, payload?: unknown) => {
            cancelUnclaimedWarning();

            set(() => ({intent: {key, payload}}));

            // A renamed route or a dialog that never opted in turns a command into one that silently does nothing.
            // This is the only signal that would surface it.
            unclaimedWarningTimeoutId = setTimeout(() => {
                if (import.meta.env.DEV && get().intent?.key === key) {
                    console.warn(`Command intent "${key}" was published but never claimed.`);
                }
            }, UNCLAIMED_INTENT_WARNING_DELAY);
        },
        reset: () => {
            cancelUnclaimedWarning();

            set(() => ({intent: undefined}));
        },
    }))
);
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandIntentStore.test.ts
```

Expected: PASS, 5 tests.

- [ ] **Step 5: Write the failing hook test**

`client/src/shared/command-bar/tests/useCommandIntent.test.tsx`:

```tsx
import {useCommandIntent} from '@/shared/command-bar/useCommandIntent';
import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

describe('useCommandIntent', () => {
    beforeEach(() => {
        useCommandIntentStore.getState().reset();
    });

    it('runs the handler with the payload when the intent matches', () => {
        const handler = vi.fn();

        useCommandIntentStore.getState().publish('project.create', {id: 7});

        renderHook(() => useCommandIntent('project.create', handler));

        expect(handler).toHaveBeenCalledWith({id: 7});
    });

    it('does not run the handler when no intent is pending', () => {
        const handler = vi.fn();

        renderHook(() => useCommandIntent('project.create', handler));

        expect(handler).not.toHaveBeenCalled();
    });

    it('runs the handler only once when two components claim the same key', () => {
        const first = vi.fn();
        const second = vi.fn();

        useCommandIntentStore.getState().publish('dataTable.create');

        renderHook(() => useCommandIntent('dataTable.create', first));
        renderHook(() => useCommandIntent('dataTable.create', second));

        expect(first).toHaveBeenCalledTimes(1);
        expect(second).not.toHaveBeenCalled();
    });
});
```

- [ ] **Step 6: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandIntent.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 7: Implement the hook**

`client/src/shared/command-bar/useCommandIntent.ts`:

```ts
import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {useEffect, useRef} from 'react';

/**
 * Claims a one-shot command intent on mount. Call it from the component that owns the state the intent should change
 * -- for the creation dialogs that is the dialog itself, since none of them expose a controlled `open` prop.
 */
export function useCommandIntent(key: string, handler: (payload?: unknown) => void): void {
    const handlerRef = useRef(handler);

    handlerRef.current = handler;

    const claim = useCommandIntentStore((state) => state.claim);

    useEffect(() => {
        const intent = claim(key);

        if (intent) {
            handlerRef.current(intent.payload);
        }
    }, [claim, key]);
}
```

- [ ] **Step 8: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandIntent.test.tsx
```

Expected: PASS, 3 tests.

- [ ] **Step 9: Check and commit**

```bash
cd client && npm run check
git add client/src/shared/command-bar
git commit -m "2396 client - Add one-shot command intents"
```

---

### Task 7: The action pipeline

**Files:**
- Create: `client/src/shared/command-bar/executeCommand.ts`
- Test: `client/src/shared/command-bar/tests/executeCommand.test.ts`

**Interfaces:**
- Produces: `executeCommand(command, options): Promise<void>` where `options` is `{closePalette, context, navigate, onError, recordRecent}`. Task 9 calls it from the dialog.

- [ ] **Step 1: Write the failing test**

`client/src/shared/command-bar/tests/executeCommand.test.ts`:

```ts
import {executeCommand} from '@/shared/command-bar/executeCommand';
import {type CommandContextI, type CommandI} from '@/shared/command-bar/types';
import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/'};

const options = () => ({
    closePalette: vi.fn(),
    context,
    navigate: vi.fn(),
    onError: vi.fn(),
    recordRecent: vi.fn(),
});

describe('executeCommand', () => {
    beforeEach(() => {
        useCommandIntentStore.getState().reset();
    });

    it('closes the palette before running the first action', async () => {
        const order: string[] = [];
        const commandOptions = options();

        commandOptions.closePalette.mockImplementation(() => order.push('close'));
        commandOptions.navigate.mockImplementation(() => order.push('navigate'));

        const command: CommandI = {actions: [{to: '/a', type: 'navigate'}], id: 'a', title: 'A'};

        await executeCommand(command, commandOptions);

        expect(order).toEqual(['close', 'navigate']);
    });

    it('runs actions in order, awaiting each', async () => {
        const order: string[] = [];
        const commandOptions = options();

        const command: CommandI = {
            actions: [
                {
                    run: async () => {
                        await Promise.resolve();

                        order.push('first');
                    },
                    type: 'callback',
                },
                {run: () => void order.push('second'), type: 'callback'},
            ],
            id: 'a',
            title: 'A',
        };

        await executeCommand(command, commandOptions);

        expect(order).toEqual(['first', 'second']);
    });

    it('publishes an intent action', async () => {
        const command: CommandI = {
            actions: [{key: 'project.create', payload: {id: 7}, type: 'intent'}],
            id: 'a',
            title: 'A',
        };

        await executeCommand(command, options());

        expect(useCommandIntentStore.getState().intent).toEqual({key: 'project.create', payload: {id: 7}});
    });

    it('stops at the first failing action and reports it', async () => {
        const commandOptions = options();
        const never = vi.fn();

        const command: CommandI = {
            actions: [
                {
                    run: () => {
                        throw new Error('boom');
                    },
                    type: 'callback',
                },
                {run: never, type: 'callback'},
            ],
            id: 'a',
            title: 'A',
        };

        await executeCommand(command, commandOptions);

        expect(never).not.toHaveBeenCalled();
        expect(commandOptions.onError).toHaveBeenCalled();
    });

    it('does not record a failed command in recents', async () => {
        const commandOptions = options();

        const command: CommandI = {
            actions: [
                {
                    run: () => {
                        throw new Error('boom');
                    },
                    type: 'callback',
                },
            ],
            id: 'a',
            title: 'A',
        };

        await executeCommand(command, commandOptions);

        expect(commandOptions.recordRecent).not.toHaveBeenCalled();
    });

    it('records a successful command in recents', async () => {
        const commandOptions = options();

        const command: CommandI = {actions: [{to: '/a', type: 'navigate'}], id: 'a', title: 'A'};

        await executeCommand(command, commandOptions);

        expect(commandOptions.recordRecent).toHaveBeenCalledWith(command);
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/executeCommand.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the pipeline**

`client/src/shared/command-bar/executeCommand.ts`:

```ts
import {
    type CommandActionType,
    type CommandContextI,
    type CommandI,
    type CommandRunContextI,
} from '@/shared/command-bar/types';
import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {type NavigateFunction} from 'react-router-dom';

export interface ExecuteCommandOptionsI {
    closePalette: () => void;
    context: CommandContextI;
    navigate: NavigateFunction;
    onError: (error: unknown) => void;
    recordRecent: (command: CommandI) => void;
}

async function runAction(action: CommandActionType, runContext: CommandRunContextI): Promise<void> {
    switch (action.type) {
        case 'callback':
            await action.run(runContext);

            return;
        case 'intent':
            useCommandIntentStore.getState()
                .publish(action.key, action.payload);

            return;
        case 'navigate':
            await runContext.navigate(action.to);

            return;
    }
}

/**
 * Runs a command's actions in order, awaiting each. The palette closes first so a dialog opened by a later action does
 * not mount while the command dialog's own overlay is still unmounting.
 */
export async function executeCommand(command: CommandI, options: ExecuteCommandOptionsI): Promise<void> {
    const {closePalette, context, navigate, onError, recordRecent} = options;

    closePalette();

    const runContext: CommandRunContextI = {command, context, navigate};

    try {
        for (const action of command.actions ?? []) {
            await runAction(action, runContext);
        }
    } catch (error) {
        onError(error);

        return;
    }

    recordRecent(command);
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/executeCommand.test.ts
```

Expected: PASS, 6 tests.

- [ ] **Step 5: Check and commit**

```bash
cd client && npm run check
git add client/src/shared/command-bar
git commit -m "2396 client - Add the command action pipeline"
```

---

### Task 8: Persisted recents

**Files:**
- Create: `client/src/shared/command-bar/useCommandRecentsStore.ts`
- Test: `client/src/shared/command-bar/tests/useCommandRecentsStore.test.ts`

**Interfaces:**
- Produces: `useCommandRecentsStore` (`addRecent(userId, command)`, `recentsByUserId`, `reset()`), `RECENT_COMMANDS_LIMIT`. Task 11 renders from it; Task 9 wires `addRecent` into `executeCommand`.

- [ ] **Step 1: Write the failing test**

`client/src/shared/command-bar/tests/useCommandRecentsStore.test.ts`:

```ts
import {type CommandI} from '@/shared/command-bar/types';
import {RECENT_COMMANDS_LIMIT, useCommandRecentsStore} from '@/shared/command-bar/useCommandRecentsStore';
import {beforeEach, describe, expect, it} from 'vitest';

const navigateCommand = (id: string): CommandI => ({
    actions: [{to: `/${id}`, type: 'navigate'}],
    id,
    title: id.toUpperCase(),
});

describe('useCommandRecentsStore', () => {
    beforeEach(() => {
        useCommandRecentsStore.getState().reset();
    });

    it('records a navigate command, most recent first', () => {
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('b'));

        expect(useCommandRecentsStore.getState().recentsByUserId['1'].map((recent) => recent.id)).toEqual(['b', 'a']);
    });

    it('stores only the serialisable fields', () => {
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));

        expect(useCommandRecentsStore.getState().recentsByUserId['1'][0]).toEqual({
            actions: [{to: '/a', type: 'navigate'}],
            id: 'a',
            title: 'A',
        });
    });

    it('moves a repeated command back to the front without duplicating it', () => {
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('b'));
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));

        expect(useCommandRecentsStore.getState().recentsByUserId['1'].map((recent) => recent.id)).toEqual(['a', 'b']);
    });

    it('caps the list', () => {
        for (let index = 0; index < RECENT_COMMANDS_LIMIT + 3; index++) {
            useCommandRecentsStore.getState().addRecent('1', navigateCommand(`command-${index}`));
        }

        expect(useCommandRecentsStore.getState().recentsByUserId['1']).toHaveLength(RECENT_COMMANDS_LIMIT);
    });

    it('keeps users separate', () => {
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));
        useCommandRecentsStore.getState().addRecent('2', navigateCommand('b'));

        expect(useCommandRecentsStore.getState().recentsByUserId['1'].map((recent) => recent.id)).toEqual(['a']);
        expect(useCommandRecentsStore.getState().recentsByUserId['2'].map((recent) => recent.id)).toEqual(['b']);
    });

    it('does not record a command with a callback action', () => {
        useCommandRecentsStore.getState()
            .addRecent('1', {actions: [{run: () => {}, type: 'callback'}], id: 'a', title: 'A'});

        expect(useCommandRecentsStore.getState().recentsByUserId['1']).toBeUndefined();
    });

    it('does not record a children command', () => {
        useCommandRecentsStore.getState()
            .addRecent('1', {children: {placeholder: 'Search...', resolve: async () => []}, id: 'a', title: 'A'});

        expect(useCommandRecentsStore.getState().recentsByUserId['1']).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandRecentsStore.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the store**

`client/src/shared/command-bar/useCommandRecentsStore.ts`:

```ts
import {type CommandI, type RecentCommandI} from '@/shared/command-bar/types';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export const RECENT_COMMANDS_LIMIT = 5;

interface CommandRecentsStateI {
    addRecent: (userId: string, command: CommandI) => void;
    recentsByUserId: Record<string, RecentCommandI[]>;
    reset: () => void;
}

/**
 * A recent must survive a page reload, so only commands whose every action is serialisable are recorded. A `callback`
 * action is a closure; a replayed one would silently do nothing. Page-scoped commands are the usual case, and they are
 * meaningless outside the page that registered them anyway.
 */
function isReplayable(command: CommandI): boolean {
    const actions = command.actions ?? [];

    return (
        actions.length > 0 && actions.every((action) => action.type === 'intent' || action.type === 'navigate')
    );
}

export const useCommandRecentsStore = create<CommandRecentsStateI>()(
    devtools(
        persist(
            (set) => ({
                addRecent: (userId: string, command: CommandI) => {
                    if (!isReplayable(command)) {
                        return;
                    }

                    const recent: RecentCommandI = {
                        actions: command.actions!,
                        id: command.id,
                        title: command.title,
                    };

                    set((state) => {
                        const existing = state.recentsByUserId[userId] ?? [];

                        return {
                            recentsByUserId: {
                                ...state.recentsByUserId,
                                [userId]: [
                                    recent,
                                    ...existing.filter((stored) => stored.id !== recent.id),
                                ].slice(0, RECENT_COMMANDS_LIMIT),
                            },
                        };
                    });
                },
                recentsByUserId: {},
                reset: () => set(() => ({recentsByUserId: {}})),
            }),
            {
                // localStorage is partitioned by origin and tenants are separated by host, so the user id is the only
                // discriminator this key needs.
                name: 'bytechef.commandBar.recents',
                partialize: (state) => ({recentsByUserId: state.recentsByUserId}),
            }
        )
    )
);
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandRecentsStore.test.ts
```

Expected: PASS, 7 tests.

- [ ] **Step 5: Check and commit**

```bash
cd client && npm run check
git add client/src/shared/command-bar
git commit -m "2396 client - Persist recently executed commands"
```

---

## Phase 2 — The palette

Order matters here: the dialog and its command sources are built first, and only then does `App.tsx` swap `GlobalSearchDialog` for `CommandBarDialog` (Task 13). That way the replacement is already at feature parity when it lands, and no intermediate commit ships a search box that finds less than the one it replaced.

### Task 9: The palette store and context

**Files:**
- Create: `client/src/shared/command-bar/useCommandBarStore.ts`
- Create: `client/src/shared/command-bar/useCommandContext.ts`
- Test: `client/src/shared/command-bar/tests/useCommandBarStore.test.ts`

**Interfaces:**
- Produces: `useCommandBarStore` (`close`, `open`, `popCommand`, `pushCommand`, `query`, `setOpen`, `setQuery`, `stack`), `useCommandContext(): CommandContextI`. Tasks 10, 11, 13 and 14 consume both.

- [ ] **Step 1: Write the failing test**

`client/src/shared/command-bar/tests/useCommandBarStore.test.ts`:

```ts
import {type CommandI} from '@/shared/command-bar/types';
import {useCommandBarStore} from '@/shared/command-bar/useCommandBarStore';
import {beforeEach, describe, expect, it} from 'vitest';

const openWorkflow: CommandI = {
    children: {placeholder: 'Search by workflow name...', resolve: async () => []},
    id: 'workflow.open',
    title: 'Open workflow',
};

describe('useCommandBarStore', () => {
    beforeEach(() => {
        useCommandBarStore.getState().close();
    });

    it('pushes a command onto the stack and clears the query', () => {
        useCommandBarStore.getState().setQuery('wor');
        useCommandBarStore.getState().pushCommand(openWorkflow);

        expect(useCommandBarStore.getState().stack).toEqual([openWorkflow]);
        expect(useCommandBarStore.getState().query).toBe('');
    });

    it('pops the stack and clears the query', () => {
        useCommandBarStore.getState().pushCommand(openWorkflow);
        useCommandBarStore.getState().setQuery('my');
        useCommandBarStore.getState().popCommand();

        expect(useCommandBarStore.getState().stack).toEqual([]);
        expect(useCommandBarStore.getState().query).toBe('');
    });

    it('resets the stack and the query when closed', () => {
        useCommandBarStore.getState().setOpen(true);
        useCommandBarStore.getState().pushCommand(openWorkflow);
        useCommandBarStore.getState().setQuery('my');
        useCommandBarStore.getState().close();

        expect(useCommandBarStore.getState().open).toBe(false);
        expect(useCommandBarStore.getState().stack).toEqual([]);
        expect(useCommandBarStore.getState().query).toBe('');
    });

    it('treats setOpen(false) as a close', () => {
        useCommandBarStore.getState().setOpen(true);
        useCommandBarStore.getState().pushCommand(openWorkflow);
        useCommandBarStore.getState().setOpen(false);

        expect(useCommandBarStore.getState().stack).toEqual([]);
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandBarStore.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the store**

`client/src/shared/command-bar/useCommandBarStore.ts`:

```ts
import {type CommandI} from '@/shared/command-bar/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CommandBarStateI {
    close: () => void;
    open: boolean;
    popCommand: () => void;
    pushCommand: (command: CommandI) => void;
    query: string;
    setOpen: (open: boolean) => void;
    setQuery: (query: string) => void;
    stack: CommandI[];
}

export const useCommandBarStore = create<CommandBarStateI>()(
    devtools((set) => ({
        close: () => set(() => ({open: false, query: '', stack: []})),
        open: false,
        // Entering or leaving a sub-mode always clears the query: the text that matched "Open workflow" is not text
        // anyone wants applied to the list of workflows.
        popCommand: () =>
            set((state) => ({
                query: '',
                stack: state.stack.slice(0, -1),
            })),
        pushCommand: (command: CommandI) =>
            set((state) => ({
                query: '',
                stack: [...state.stack, command],
            })),
        query: '',
        setOpen: (open: boolean) => set(() => (open ? {open: true} : {open: false, query: '', stack: []})),
        setQuery: (query: string) => set(() => ({query})),
        stack: [],
    }))
);
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useCommandBarStore.test.ts
```

Expected: PASS, 4 tests.

- [ ] **Step 5: Implement the context hook**

`client/src/shared/command-bar/useCommandContext.ts`:

```ts
import {type CommandContextI} from '@/shared/command-bar/types';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useMemo} from 'react';
import {useLocation} from 'react-router-dom';

export function useCommandContext(): CommandContextI {
    const edition = useApplicationInfoStore((state) => state.application?.edition);
    const featureFlags = useFeatureFlagsStore();

    const {pathname} = useLocation();

    return useMemo(() => ({edition, featureFlags, pathname}), [edition, featureFlags, pathname]);
}
```

- [ ] **Step 6: Check and commit**

```bash
cd client && npm run check
git add client/src/shared/command-bar
git commit -m "2396 client - Add the command bar store and context"
```

---

### Task 10: CommandBarDialog, root level

Renders registered commands grouped by `group`. Nesting comes in Task 11.

**Files:**
- Create: `client/src/components/CommandBar/CommandBarDialog.tsx`
- Test: `client/src/components/CommandBar/tests/CommandBarDialog.test.tsx`

**Interfaces:**
- Produces: `CommandBarDialog` (default export, no props — it reads `useCommandBarStore`). Task 13 mounts it.

- [ ] **Step 1: Write the failing test**

`client/src/components/CommandBar/tests/CommandBarDialog.test.tsx`:

```tsx
import CommandBarDialog from '@/components/CommandBar/CommandBarDialog';
import {useCommandBarStore} from '@/shared/command-bar/useCommandBarStore';
import {registerCommandSource, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {navigateMock} = vi.hoisted(() => ({navigateMock: vi.fn()}));

vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual<typeof import('react-router-dom')>('react-router-dom')),
    useNavigate: () => navigateMock,
}));

const renderDialog = () => render(<CommandBarDialog />, {wrapper: MemoryRouter});

describe('CommandBarDialog', () => {
    beforeEach(() => {
        navigateMock.mockClear();
        useCommandSourceRegistry.getState().reset();
        useCommandBarStore.getState().close();
        useCommandBarStore.getState().setOpen(true);
    });

    it('renders registered commands under their group heading', () => {
        registerCommandSource({
            getCommands: () => [
                {actions: [{to: '/automation/projects', type: 'navigate'}], group: 'Navigation', id: 'nav.projects', title: 'Go to Projects'},
            ],
            id: 'test',
        });

        renderDialog();

        expect(screen.getByText('Navigation')).toBeInTheDocument();
        expect(screen.getByText('Go to Projects')).toBeInTheDocument();
    });

    it('runs the actions of a selected command', async () => {
        registerCommandSource({
            getCommands: () => [
                {actions: [{to: '/automation/projects', type: 'navigate'}], id: 'nav.projects', title: 'Go to Projects'},
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Go to Projects'));

        expect(navigateMock).toHaveBeenCalledWith('/automation/projects');
    });

    it('orders groups so Navigation comes last', () => {
        registerCommandSource({
            getCommands: () => [
                {actions: [{to: '/automation/projects', type: 'navigate'}], group: 'Navigation', id: 'nav.projects', title: 'Go to Projects'},
                {actions: [{to: '/automation/datatables', type: 'navigate'}], group: 'Data Tables', id: 'open.table', title: 'Open data table'},
            ],
            id: 'test',
        });

        renderDialog();

        const dataTables = screen.getByText('Data Tables');
        const navigation = screen.getByText('Navigation');

        expect(dataTables.compareDocumentPosition(navigation) & 4).toBe(4);
    });

    it('closes when a command is selected', async () => {
        registerCommandSource({
            getCommands: () => [
                {actions: [{to: '/automation/projects', type: 'navigate'}], id: 'nav.projects', title: 'Go to Projects'},
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Go to Projects'));

        expect(useCommandBarStore.getState().open).toBe(false);
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd client && npx vitest run src/components/CommandBar/tests/CommandBarDialog.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the dialog**

`client/src/components/CommandBar/CommandBarDialog.tsx`:

```tsx
import {CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {executeCommand} from '@/shared/command-bar/executeCommand';
import {type CommandI} from '@/shared/command-bar/types';
import {useCommandBarStore} from '@/shared/command-bar/useCommandBarStore';
import {useCommandContext} from '@/shared/command-bar/useCommandContext';
import {useCommandRecentsStore} from '@/shared/command-bar/useCommandRecentsStore';
import {collectCommands, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {toast} from 'sonner';
import {ArrowRightIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

const UNGROUPED = 'Commands';

// Group order must not depend on the order sources happened to register in: navigation comes from a React hook and
// resources from the bootstrap module, so insertion order is not stable. Anything unlisted sorts before Navigation.
const GROUP_ORDER = [
    'Workflows',
    'Projects',
    'Connections',
    'Data Tables',
    'Deployments',
    'API Platform',
    'Knowledge Base',
    'Files',
    UNGROUPED,
    'Navigation',
];

function compareGroups(first: string, second: string): number {
    const firstIndex = GROUP_ORDER.indexOf(first);
    const secondIndex = GROUP_ORDER.indexOf(second);

    return (firstIndex === -1 ? GROUP_ORDER.indexOf(UNGROUPED) : firstIndex) -
        (secondIndex === -1 ? GROUP_ORDER.indexOf(UNGROUPED) : secondIndex);
}

const CommandBarDialog = () => {
    const navigate = useNavigate();

    const open = useCommandBarStore((state) => state.open);
    const query = useCommandBarStore((state) => state.query);
    const setOpen = useCommandBarStore((state) => state.setOpen);
    const setQuery = useCommandBarStore((state) => state.setQuery);

    const sources = useCommandSourceRegistry((state) => state.sources);
    const addRecent = useCommandRecentsStore((state) => state.addRecent);
    const userId = useAuthenticationStore((state) => state.account?.id);

    const context = useCommandContext();

    const commands = useMemo(() => collectCommands(sources, context), [context, sources]);

    const groupedCommands = useMemo(() => {
        const groups = new Map<string, CommandI[]>();

        for (const command of commands) {
            const group = command.group ?? UNGROUPED;

            groups.set(group, [...(groups.get(group) ?? []), command]);
        }

        return [...groups.entries()].sort(([first], [second]) => compareGroups(first, second));
    }, [commands]);

    const handleSelect = useCallback(
        (command: CommandI) => {
            void executeCommand(command, {
                closePalette: () => setOpen(false),
                context,
                navigate,
                onError: (error) =>
                    toast.error(`"${command.title}" failed`, {
                        description: error instanceof Error ? error.message : String(error),
                    }),
                recordRecent: (executed) => userId && addRecent(String(userId), executed),
            });
        },
        [addRecent, context, navigate, setOpen, userId]
    );

    return (
        <CommandDialog onOpenChange={setOpen} open={open}>
            <CommandInput
                className="my-2"
                onValueChange={setQuery}
                placeholder="Type a command or search..."
                value={query}
            />

            <CommandList>
                <CommandEmpty>No results found.</CommandEmpty>

                {groupedCommands.map(([group, groupCommands]) => (
                    <CommandGroup heading={group} key={group}>
                        {groupCommands.map((command) => {
                            const Icon = command.icon ?? ArrowRightIcon;

                            return (
                                <CommandItem
                                    key={command.id}
                                    keywords={command.keywords}
                                    onSelect={() => handleSelect(command)}
                                    value={command.title}
                                >
                                    <Icon className="mr-2 size-4" />

                                    <span>{command.title}</span>

                                    {command.subtitle && (
                                        <span className="ml-2 text-xs text-muted-foreground">{command.subtitle}</span>
                                    )}
                                </CommandItem>
                            );
                        })}
                    </CommandGroup>
                ))}
            </CommandList>
        </CommandDialog>
    );
};

export default CommandBarDialog;
```

- [ ] **Step 4: Confirm the toast convention**

```bash
grep -rn "toast.error" client/src | head -3
```

This codebase uses `sonner` — `import {toast} from 'sonner'` and `toast.error(title, {description})`, as in
`client/src/config/useFetchInterceptor.ts:97`. There is no `@/hooks/use-toast` module. Confirm the shape
matches before relying on it.

- [ ] **Step 5: Run the test to verify it passes**

```bash
cd client && npx vitest run src/components/CommandBar/tests/CommandBarDialog.test.tsx
```

Expected: PASS, 4 tests.

- [ ] **Step 6: Check and commit**

```bash
cd client && npm run check
git add client/src/components/CommandBar
git commit -m "2396 client - Add the registry-driven CommandBar dialog"
```

---

### Task 11: Nested sub-modes

Selecting a children-command pushes a level: the parent's title becomes a static heading, the placeholder changes, and the list shows asynchronously resolved children.

**Files:**
- Modify: `client/src/components/CommandBar/CommandBarDialog.tsx`
- Create: `client/src/shared/command-bar/useResolvedChildren.ts`
- Test: `client/src/shared/command-bar/tests/useResolvedChildren.test.tsx`
- Test: `client/src/components/CommandBar/tests/CommandBarDialog.test.tsx` (extend)

**Interfaces:**
- Produces: `useResolvedChildren(children, query): {children: CommandI[]; isBelowMinimum: boolean}`. Only `CommandBarDialog` consumes it.

- [ ] **Step 1: Write the failing resolver test**

`client/src/shared/command-bar/tests/useResolvedChildren.test.tsx`:

```tsx
import {type CommandChildrenI, type CommandI} from '@/shared/command-bar/types';
import {useResolvedChildren} from '@/shared/command-bar/useResolvedChildren';
import {renderHook, waitFor} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

const command = (id: string): CommandI => ({actions: [{to: `/${id}`, type: 'navigate'}], id, title: id});

describe('useResolvedChildren', () => {
    it('reports when the query is below the minimum length', () => {
        const children: CommandChildrenI = {minQueryLength: 2, placeholder: 'Search...', resolve: vi.fn()};

        const {result} = renderHook(() => useResolvedChildren(children, 'a'));

        expect(result.current.isBelowMinimum).toBe(true);
        expect(children.resolve).not.toHaveBeenCalled();
    });

    it('resolves children once the query is long enough', async () => {
        const children: CommandChildrenI = {
            placeholder: 'Search...',
            resolve: async () => [command('a')],
        };

        const {result} = renderHook(() => useResolvedChildren(children, 'abc'));

        await waitFor(() => {
            expect(result.current.children.map((child) => child.id)).toEqual(['a']);
        });
    });

    it('resolves immediately when minQueryLength is zero', async () => {
        const children: CommandChildrenI = {
            minQueryLength: 0,
            placeholder: 'Search...',
            resolve: async () => [command('a')],
        };

        const {result} = renderHook(() => useResolvedChildren(children, ''));

        await waitFor(() => {
            expect(result.current.children).toHaveLength(1);
        });
    });

    it('aborts the previous resolve when the query changes', async () => {
        const abortedSignals: boolean[] = [];

        const children: CommandChildrenI = {
            placeholder: 'Search...',
            resolve: async (query, signal) => {
                signal.addEventListener('abort', () => abortedSignals.push(true));

                return [command(query)];
            },
        };

        const {rerender} = renderHook(({query}) => useResolvedChildren(children, query), {
            initialProps: {query: 'abc'},
        });

        rerender({query: 'abcd'});

        await waitFor(() => {
            expect(abortedSignals).toHaveLength(1);
        });
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useResolvedChildren.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the resolver hook**

`client/src/shared/command-bar/useResolvedChildren.ts`:

```ts
import {type CommandChildrenI, type CommandI} from '@/shared/command-bar/types';
import {useEffect, useRef, useState} from 'react';
import {useDebounce} from 'use-debounce';

const DEFAULT_MIN_QUERY_LENGTH = 2;
const RESOLVE_DEBOUNCE_MS = 300;

interface ResolvedChildrenI {
    children: CommandI[];
    isBelowMinimum: boolean;
}

/**
 * Resolves a nested command's children for the current query. Each new query aborts the previous request, so a slow
 * early response cannot land after -- and overwrite -- a fast later one.
 */
export function useResolvedChildren(children: CommandChildrenI, query: string): ResolvedChildrenI {
    const [resolvedChildren, setResolvedChildren] = useState<CommandI[]>([]);

    const abortControllerRef = useRef<AbortController | undefined>(undefined);

    const [debouncedQuery] = useDebounce(query, RESOLVE_DEBOUNCE_MS);

    const minQueryLength = children.minQueryLength ?? DEFAULT_MIN_QUERY_LENGTH;
    const isBelowMinimum = debouncedQuery.length < minQueryLength;

    useEffect(() => {
        abortControllerRef.current?.abort();

        if (isBelowMinimum) {
            setResolvedChildren([]);

            return;
        }

        const abortController = new AbortController();

        abortControllerRef.current = abortController;

        children
            .resolve(debouncedQuery, abortController.signal)
            .then((resolved) => {
                if (!abortController.signal.aborted) {
                    setResolvedChildren(resolved);
                }
            })
            .catch(() => setResolvedChildren([]));

        return () => abortController.abort();
    }, [children, debouncedQuery, isBelowMinimum]);

    return {children: resolvedChildren, isBelowMinimum};
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useResolvedChildren.test.tsx
```

Expected: PASS, 4 tests.

- [ ] **Step 5: Write the failing dialog tests**

Append to `client/src/components/CommandBar/tests/CommandBarDialog.test.tsx`:

```tsx
    it('pushes a sub-mode and swaps the placeholder', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    children: {
                        minQueryLength: 0,
                        placeholder: 'Search by workflow name...',
                        resolve: async () => [
                            {actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}], id: 'wf.2', title: 'My workflow'},
                        ],
                    },
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Open workflow'));

        expect(await screen.findByPlaceholderText('Search by workflow name...')).toBeInTheDocument();
        expect(await screen.findByText('My workflow')).toBeInTheDocument();
    });

    it('pops the sub-mode when Backspace is pressed on an empty input', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    children: {minQueryLength: 0, placeholder: 'Search by workflow name...', resolve: async () => []},
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Open workflow'));
        await userEvent.type(screen.getByPlaceholderText('Search by workflow name...'), '{backspace}');

        expect(useCommandBarStore.getState().stack).toHaveLength(0);
    });
```

- [ ] **Step 6: Run the tests to verify they fail**

```bash
cd client && npx vitest run src/components/CommandBar/tests/CommandBarDialog.test.tsx
```

Expected: FAIL — selecting a children-command currently does nothing.

- [ ] **Step 7: Add nesting to the dialog**

In `CommandBarDialog.tsx`:

Read the stack and derive the active level:

```ts
    const popCommand = useCommandBarStore((state) => state.popCommand);
    const pushCommand = useCommandBarStore((state) => state.pushCommand);
    const stack = useCommandBarStore((state) => state.stack);

    const activeCommand = stack.at(-1);
```

Route selection by shape — a children-command pushes, an action-command executes:

```ts
    const handleSelect = useCallback(
        (command: CommandI) => {
            if (command.children) {
                pushCommand(command);

                return;
            }

            void executeCommand(command, {/* unchanged */});
        },
        [addRecent, context, navigate, pushCommand, setOpen, userId]
    );
```

Resolve the active level's children (the hook is called unconditionally with a stable no-op when at root, so hook order never changes):

```ts
    const noChildren = useMemo<CommandChildrenI>(
        () => ({minQueryLength: Number.MAX_SAFE_INTEGER, placeholder: '', resolve: async () => []}),
        []
    );

    const {children: resolvedChildren, isBelowMinimum} = useResolvedChildren(
        activeCommand?.children ?? noChildren,
        query
    );
```

Pop on Backspace at an empty query:

```ts
    const handleKeyDown = useCallback(
        (event: KeyboardEvent<HTMLInputElement>) => {
            if (event.key === 'Backspace' && query === '' && stack.length > 0) {
                event.preventDefault();

                popCommand();
            }
        },
        [popCommand, query, stack.length]
    );
```

Escape already closes the dialog through Radix; when nested it should pop instead:

```ts
    const handleOpenChange = useCallback(
        (nextOpen: boolean) => {
            if (!nextOpen && stack.length > 0) {
                popCommand();

                return;
            }

            setOpen(nextOpen);
        },
        [popCommand, setOpen, stack.length]
    );
```

Render the nested level when `activeCommand` is set: `onKeyDown={handleKeyDown}` and `placeholder={activeCommand.children!.placeholder}` on the input, `onOpenChange={handleOpenChange}` on the dialog, and a single `CommandGroup heading={activeCommand.title}` listing `resolvedChildren` with the same `CommandItem` markup used at root. When `isBelowMinimum`, render `<CommandEmpty>Type at least 2 characters to search...</CommandEmpty>` instead of the group.

- [ ] **Step 8: Run the tests to verify they pass**

```bash
cd client && npx vitest run src/components/CommandBar/tests/CommandBarDialog.test.tsx
```

Expected: PASS, 6 tests.

- [ ] **Step 9: Check and commit**

```bash
cd client && npm run check
git add client/src/components/CommandBar client/src/shared/command-bar
git commit -m "2396 client - Add nested sub-modes to the CommandBar"
```

---

## Phase 3 — Command sources

### Task 12: Resource commands

Nine "Open X" commands, each a nested sub-mode backed by a type-scoped search. The route map moves out of `GlobalSearchDialog` unchanged, so this task adds no new routing behaviour — Task 3 already fixed it.

**Files:**
- Create: `client/src/shared/command-bar/resourceCommandRoutes.ts` (moved from `GlobalSearchDialog.tsx`)
- Create: `client/src/shared/command-bar/sources/resourceCommandSource.ts`
- Modify: `client/src/components/GlobalSearch/GlobalSearchDialog.tsx` — import `buildSearchResultRoute` from its new home
- Modify: `client/src/graphql/automation/search/automationSearch.graphql` — accept `$types`
- Move: `client/src/components/GlobalSearch/tests/globalSearchRoutes.test.ts` → `client/src/shared/command-bar/tests/resourceCommandRoutes.test.ts`
- Test: `client/src/shared/command-bar/tests/resourceCommandSource.test.ts`

**Interfaces:**
- Produces: `buildSearchResultActions(result): CommandActionType[]`, `RESOURCE_COMMAND_DESCRIPTORS`, `resourceCommandSource: CommandSourceI`. Task 13 registers the source.

- [ ] **Step 1: Send types with the query**

`automationSearch.graphql`:

```graphql
query automationSearch($query: String!, $limit: Int, $types: [SearchAssetType!]) {
    automationSearch(query: $query, limit: $limit, types: $types) {
```

Then regenerate:

```bash
cd client && npx graphql-codegen
```

- [ ] **Step 2: Move the route builder and widen it to actions**

Move `globalSearchRoutes.ts` to `client/src/shared/command-bar/resourceCommandRoutes.ts` unchanged -- both `SearchResultRouteInputI` and `buildSearchResultRoute` -- and add to it:

```ts
/**
 * Five asset types have no detail route -- they are rendered by list pages with in-page selection -- so their command
 * navigates to the list and publishes a select intent carrying the id. A page that has not claimed its intent yet
 * still lands the user on the right list, so pages can opt in one at a time.
 */
const SELECT_INTENT_KEYS: Partial<Record<SearchAssetType, string>> = {
    [SearchAssetType.ApiCollection]: 'apiCollection.select',
    [SearchAssetType.ApiEndpoint]: 'apiEndpoint.select',
    [SearchAssetType.Connection]: 'connection.select',
    [SearchAssetType.Deployment]: 'deployment.select',
    [SearchAssetType.KnowledgeBaseDocument]: 'knowledgeBaseDocument.select',
};

export function buildSearchResultActions(result: SearchResultRouteInputI): CommandActionType[] {
    const to = buildSearchResultRoute(result);

    if (!to) {
        return [];
    }

    const intentKey = SELECT_INTENT_KEYS[result.type];

    return intentKey
        ? [{to, type: 'navigate'}, {key: intentKey, payload: {id: result.id}, type: 'intent'}]
        : [{to, type: 'navigate'}];
}
```

Move the test too: `client/src/components/GlobalSearch/tests/globalSearchRoutes.test.ts` becomes `client/src/shared/command-bar/tests/resourceCommandRoutes.test.ts`, importing from the new path. Task 13 deletes the whole `components/GlobalSearch` directory, so a test left behind there would be deleted with it. Then update `GlobalSearchDialog.tsx`'s import and delete `client/src/components/GlobalSearch/globalSearchRoutes.ts`.

- [ ] **Step 3: Write the failing source test**

`client/src/shared/command-bar/tests/resourceCommandSource.test.ts`:

```ts
import {resourceCommandSource} from '@/shared/command-bar/sources/resourceCommandSource';
import {type CommandContextI} from '@/shared/command-bar/types';
import {SearchAssetType} from '@/shared/middleware/graphql';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {fetcherMock} = vi.hoisted(() => ({fetcherMock: vi.fn()}));

vi.mock('@/shared/middleware/graphqlFetcher', () => ({
    fetcher: (...args: unknown[]) => fetcherMock(...args),
}));

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/automation/projects'};

describe('resourceCommandSource', () => {
    beforeEach(() => {
        fetcherMock.mockReset();
    });

    it('exposes one nested command per resource type', () => {
        const commands = resourceCommandSource.getCommands(context);

        expect(commands.map((command) => command.id)).toContain('resource.open.WORKFLOW');
        expect(commands.every((command) => command.children !== undefined)).toBe(true);
    });

    it('asks the server only for the type being browsed', async () => {
        fetcherMock.mockReturnValue(async () => ({automationSearch: []}));

        const openWorkflow = resourceCommandSource
            .getCommands(context)
            .find((command) => command.id === 'resource.open.WORKFLOW')!;

        await openWorkflow.children!.resolve('my', new AbortController().signal);

        expect(fetcherMock).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({types: [SearchAssetType.Workflow]})
        );
    });

    it('turns a workflow result into a navigate command', async () => {
        fetcherMock.mockReturnValue(async () => ({
            automationSearch: [
                {id: '77', label: 'My workflow', name: 'My workflow', projectId: '5', type: SearchAssetType.Workflow},
            ],
        }));

        const openWorkflow = resourceCommandSource
            .getCommands(context)
            .find((command) => command.id === 'resource.open.WORKFLOW')!;

        const children = await openWorkflow.children!.resolve('my', new AbortController().signal);

        expect(children[0].actions).toEqual([
            {to: '/automation/projects/5/project-workflows/77', type: 'navigate'},
        ]);
    });

    it('turns a connection result into navigate plus a select intent', async () => {
        fetcherMock.mockReturnValue(async () => ({
            automationSearch: [{id: '4', name: 'Slack account', type: SearchAssetType.Connection}],
        }));

        const openConnection = resourceCommandSource
            .getCommands(context)
            .find((command) => command.id === 'resource.open.CONNECTION')!;

        const children = await openConnection.children!.resolve('slack', new AbortController().signal);

        expect(children[0].actions).toEqual([
            {to: '/automation/connections', type: 'navigate'},
            {key: 'connection.select', payload: {id: '4'}, type: 'intent'},
        ]);
    });
});
```

- [ ] **Step 4: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/resourceCommandSource.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 5: Implement the source**

`client/src/shared/command-bar/sources/resourceCommandSource.ts`:

```ts
import {buildSearchResultActions} from '@/shared/command-bar/resourceCommandRoutes';
import {type CommandI, type CommandSourceI} from '@/shared/command-bar/types';
import {
    AutomationSearchDocument,
    type AutomationSearchQuery,
    type AutomationSearchQueryVariables,
    SearchAssetType,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {
    FileTextIcon,
    FolderIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    type LucideIcon,
    RouteIcon,
    Table2Icon,
    VectorSquareIcon,
    ZapIcon,
} from 'lucide-react';

const RESULT_LIMIT = 20;

interface ResourceCommandDescriptorI {
    group: string;
    icon: LucideIcon;
    placeholder: string;
    title: string;
    type: SearchAssetType;
}

export const RESOURCE_COMMAND_DESCRIPTORS: ResourceCommandDescriptorI[] = [
    {group: 'Workflows', icon: ZapIcon, placeholder: 'Search by workflow name...', title: 'Open workflow', type: SearchAssetType.Workflow},
    {group: 'Projects', icon: FolderIcon, placeholder: 'Search by project name...', title: 'Open project', type: SearchAssetType.Project},
    {group: 'Connections', icon: Link2Icon, placeholder: 'Search by connection name...', title: 'Open connection', type: SearchAssetType.Connection},
    {group: 'Data Tables', icon: Table2Icon, placeholder: 'Search by table name...', title: 'Open data table', type: SearchAssetType.DataTable},
    {group: 'Deployments', icon: Layers3Icon, placeholder: 'Search by deployment name...', title: 'Open deployment', type: SearchAssetType.Deployment},
    {group: 'API Platform', icon: LayoutTemplateIcon, placeholder: 'Search by collection name...', title: 'Open API collection', type: SearchAssetType.ApiCollection},
    {group: 'API Platform', icon: RouteIcon, placeholder: 'Search by endpoint name...', title: 'Open API endpoint', type: SearchAssetType.ApiEndpoint},
    {group: 'Knowledge Base', icon: VectorSquareIcon, placeholder: 'Search by knowledge base name...', title: 'Open knowledge base', type: SearchAssetType.KnowledgeBase},
    {group: 'Files', icon: FileTextIcon, placeholder: 'Search by file name...', title: 'Open file', type: SearchAssetType.AssetFile},
];

async function searchByType(query: string, type: SearchAssetType): Promise<CommandI[]> {
    const data = await fetcher<AutomationSearchQuery, AutomationSearchQueryVariables>(AutomationSearchDocument, {
        limit: RESULT_LIMIT,
        query,
        types: [type],
    })();

    return (data.automationSearch ?? [])
        .map((result) => {
            const actions = buildSearchResultActions(result);

            if (actions.length === 0) {
                return undefined;
            }

            return {
                actions,
                id: `resource.${result.type}.${result.id}`,
                subtitle: result.description ?? undefined,
                title: 'label' in result && result.label ? result.label : result.name,
            } satisfies CommandI;
        })
        .filter((command) => command !== undefined);
}

export const resourceCommandSource: CommandSourceI = {
    getCommands: () =>
        RESOURCE_COMMAND_DESCRIPTORS.map((descriptor) => ({
            children: {
                // Zero so an opened sub-mode lists the newest results before anything is typed.
                minQueryLength: 0,
                placeholder: descriptor.placeholder,
                resolve: (query: string) => searchByType(query, descriptor.type),
            },
            group: descriptor.group,
            icon: descriptor.icon,
            id: `resource.open.${descriptor.type}`,
            title: descriptor.title,
        })),
    id: 'resource',
};
```

Note the `AbortSignal` is accepted by the resolver contract but not forwarded: `fetcher` takes no signal, so aborting discards the result rather than cancelling the request. That is enough to fix the out-of-order-response bug, which is what the signal is for here.

- [ ] **Step 6: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/resourceCommandSource.test.ts src/shared/command-bar/tests/resourceCommandRoutes.test.ts
```

Expected: PASS.

- [ ] **Step 7: Check and commit**

```bash
cd client && npm run check
git add client/src/shared/command-bar client/src/components/GlobalSearch client/src/graphql client/src/shared/middleware
git commit -m "2396 client - Add resource open commands backed by type-scoped search"
```

---

### Task 13: Navigation commands, and the swap

This is the commit where the CommandBar replaces global search. It lands only now, with navigation and resource commands already registered, so the palette is at parity the moment it appears.

**Files:**
- Create: `client/src/shared/navigation/navigationItems.ts`
- Create: `client/src/shared/command-bar/useRegisterNavigationCommands.ts`
- Create: `client/src/shared/command-bar/commandBarBootstrap.ts`
- Modify: `client/src/App.tsx`
- Delete: `client/src/components/GlobalSearch/` (Task 12 already moved its route module and route test into `shared/command-bar/`)
- Test: `client/src/shared/command-bar/tests/useRegisterNavigationCommands.test.tsx`

**Interfaces:**
- Produces: `NavigationItemI`, `automationNavigation`, `embeddedNavigation`, `platformNavigation`, `useRegisterNavigationCommands(navigationItems)`, `bootstrapCommandBar()`.

- [ ] **Step 1: Move the navigation arrays**

Cut `NavigationType` (rename it `NavigationItemI` for the naming rule), `automationNavigation`, `embeddedNavigation` and `platformNavigation` from `App.tsx` into `client/src/shared/navigation/navigationItems.ts`, exporting all four. `NavigationType` is referenced inside `App.tsx` as `let navigation: NavigationType[] = [];` — update that annotation and any other use to the new name:

```bash
grep -rn "NavigationType" client/src
``` Import them back into `App.tsx`. Keep the arrays' order exactly as it is — `AppSidebar` folds only *consecutive* items sharing a `group`, so reordering silently splits a group into two sections.

- [ ] **Step 2: Verify the move changed nothing**

```bash
cd client && npx vitest run src/shared/layout/app-sidebar/AppSidebar.test.tsx && npm run typecheck
```

Expected: PASS. Commit this move on its own so the mechanical change is separable from the behavioural one:

```bash
git add client/src/App.tsx client/src/shared/navigation
git commit -m "2396 client - Move the navigation arrays out of App.tsx"
```

- [ ] **Step 3: Write the failing navigation-commands test**

`client/src/shared/command-bar/tests/useRegisterNavigationCommands.test.tsx`:

```tsx
import {useRegisterNavigationCommands} from '@/shared/command-bar/useRegisterNavigationCommands';
import {collectCommands, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {type CommandContextI} from '@/shared/command-bar/types';
import {renderHook} from '@testing-library/react';
import {FolderIcon} from 'lucide-react';
import {beforeEach, describe, expect, it} from 'vitest';

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/automation/projects'};

describe('useRegisterNavigationCommands', () => {
    beforeEach(() => {
        useCommandSourceRegistry.getState().reset();
    });

    it('registers one navigate command per item, in the Navigation group', () => {
        renderHook(() =>
            useRegisterNavigationCommands([{href: '/automation/projects', icon: FolderIcon, name: 'Projects'}])
        );

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands).toEqual([
            {
                actions: [{to: '/automation/projects', type: 'navigate'}],
                group: 'Navigation',
                icon: FolderIcon,
                id: 'navigation./automation/projects',
                title: 'Go to Projects',
            },
        ]);
    });

    it('registers nothing when the filtered navigation is empty', () => {
        renderHook(() => useRegisterNavigationCommands([]));

        expect(collectCommands(useCommandSourceRegistry.getState().sources, context)).toEqual([]);
    });
});
```

- [ ] **Step 4: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useRegisterNavigationCommands.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 5: Implement it**

`client/src/shared/command-bar/useRegisterNavigationCommands.ts`:

```ts
import {type CommandI} from '@/shared/command-bar/types';
import {useRegisterCommands} from '@/shared/command-bar/useRegisterCommands';
import {type NavigationItemI} from '@/shared/navigation/navigationItems';
import {useMemo} from 'react';

/**
 * Uses the hook door rather than a bootstrap source because which entries are visible depends on the edition, three
 * feature flags, the AI configuration and the current environment -- roughly forty lines of filtering that already
 * live in App.tsx. Passing the filtered list in keeps that logic with exactly one implementation.
 */
export function useRegisterNavigationCommands(navigationItems: NavigationItemI[]): void {
    const commands = useMemo<CommandI[]>(
        () =>
            navigationItems.map((navigationItem) => ({
                actions: [{to: navigationItem.href, type: 'navigate'}],
                group: 'Navigation',
                icon: navigationItem.icon,
                id: `navigation.${navigationItem.href}`,
                title: `Go to ${navigationItem.name}`,
            })),
        [navigationItems]
    );

    useRegisterCommands(commands, [commands]);
}
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/useRegisterNavigationCommands.test.tsx
```

Expected: PASS, 2 tests.

- [ ] **Step 7: Add the bootstrap module**

`client/src/shared/command-bar/commandBarBootstrap.ts`:

```ts
import {resourceCommandSource} from '@/shared/command-bar/sources/resourceCommandSource';
import {registerCommandSource} from '@/shared/command-bar/useCommandSourceRegistry';

let bootstrapped = false;

/**
 * Registers the always-available command sources. Idempotent because React strict mode mounts App twice in
 * development, and a second registration would duplicate every resource command.
 */
export function bootstrapCommandBar(): void {
    if (bootstrapped) {
        return;
    }

    bootstrapped = true;

    registerCommandSource(resourceCommandSource);
}
```

- [ ] **Step 8: Swap the mount in App.tsx**

Replace the lazy import of `GlobalSearchDialog` with `CommandBarDialog`:

```ts
const CommandBarDialog = lazy(() => import('@/components/CommandBar/CommandBarDialog'));
```

Delete the `const [searchOpen, setSearchOpen] = useState(false);` line and read the store instead:

```ts
    const setCommandBarOpen = useCommandBarStore((state) => state.setOpen);
```

Register navigation commands and bootstrap the global sources:

```ts
    useRegisterNavigationCommands(navigation);

    useEffect(() => {
        if (ff_2396) {
            bootstrapCommandBar();
        }
    }, [ff_2396]);
```

In the existing Cmd+K effect, replace `setSearchOpen(true)` with `setCommandBarOpen(true)` and add `setCommandBarOpen` to its dependency array. Replace the render:

```tsx
            {ff_2396 && (
                <Suspense fallback={null}>
                    <CommandBarDialog />
                </Suspense>
            )}
```

- [ ] **Step 9: Delete the old dialog**

```bash
git rm -r client/src/components/GlobalSearch
```

- [ ] **Step 10: Verify nothing still references it**

```bash
grep -rn "GlobalSearch" client/src || echo "clean"
```

Expected: `clean`.

- [ ] **Step 11: Check and commit**

```bash
cd client && npm run check
git add client/src
git commit -m "2396 client - Replace global search with the CommandBar"
```

---

### Task 14: The Recent group

**Files:**
- Modify: `client/src/components/CommandBar/CommandBarDialog.tsx`
- Test: `client/src/components/CommandBar/tests/CommandBarDialog.test.tsx` (extend)

**Interfaces:**
- Consumes: `useCommandRecentsStore` from Task 8, `useAuthenticationStore` for the user id.

- [ ] **Step 1: Write the failing tests**

Append to `CommandBarDialog.test.tsx`:

```tsx
    it('renders recents first, above the registered commands', async () => {
        useCommandRecentsStore.getState().reset();
        useCommandRecentsStore.getState().addRecent('1', {
            actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
            id: 'resource.WORKFLOW.2',
            title: 'My workflow',
        });

        registerCommandSource({
            getCommands: () => [
                {actions: [{to: '/automation/projects', type: 'navigate'}], group: 'Navigation', id: 'nav.projects', title: 'Go to Projects'},
            ],
            id: 'test',
        });

        renderDialog();

        const recentHeading = screen.getByText('Recent');
        const navigationHeading = screen.getByText('Navigation');

        // Node.DOCUMENT_POSITION_FOLLOWING === 4: Navigation comes after Recent in document order.
        expect(recentHeading.compareDocumentPosition(navigationHeading) & 4).toBe(4);
    });

    it('replays a recent command', async () => {
        useCommandRecentsStore.getState().reset();
        useCommandRecentsStore.getState().addRecent('1', {
            actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
            id: 'resource.WORKFLOW.2',
            title: 'My workflow',
        });

        renderDialog();

        await userEvent.click(screen.getByText('My workflow'));

        expect(navigateMock).toHaveBeenCalledWith('/automation/projects/1/project-workflows/2');
    });
```

Add the imports these two tests need — `useCommandRecentsStore` from `@/shared/command-bar/useCommandRecentsStore` and `useAuthenticationStore` from `@/shared/stores/useAuthenticationStore` — and set an authenticated account in `beforeEach` so `userId` resolves:

```tsx
        useAuthenticationStore.setState({account: {id: 1}});
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd client && npx vitest run src/components/CommandBar/tests/CommandBarDialog.test.tsx
```

Expected: FAIL — no Recent heading.

- [ ] **Step 3: Render the group**

In `CommandBarDialog.tsx`, build recent commands from the store and prepend the group at root level only (a sub-mode shows its own children, not recents):

```ts
    const recentsByUserId = useCommandRecentsStore((state) => state.recentsByUserId);

    const recentCommands = useMemo<CommandI[]>(() => {
        if (!userId) {
            return [];
        }

        const commandsById = new Map(commands.map((command) => [command.id, command]));

        // The icon is not persisted -- a LucideIcon is a component, not JSON -- so it is re-resolved from the live
        // registry and falls back to the generic arrow when the command is no longer registered.
        return (recentsByUserId[String(userId)] ?? []).map((recent) => ({
            actions: recent.actions,
            group: 'Recent',
            icon: commandsById.get(recent.id)?.icon,
            id: recent.id,
            title: recent.title,
        }));
    }, [commands, recentsByUserId, userId]);
```

Render `recentCommands` as a `CommandGroup heading="Recent"` before `groupedCommands`, using the same `CommandItem` markup. Give the items a `value` of `` `recent-${command.id}` `` so cmdk does not treat a recent and its registered twin as one entry.

- [ ] **Step 4: Run the tests to verify they pass**

```bash
cd client && npx vitest run src/components/CommandBar/tests/CommandBarDialog.test.tsx
```

Expected: PASS, 8 tests.

- [ ] **Step 5: Check and commit**

```bash
cd client && npm run check
git add client/src/components/CommandBar
git commit -m "2396 client - Show recently executed commands"
```

---

### Task 15: Create commands

Four commands that navigate to a list page and publish an intent the creation dialog claims.

**Files:**
- Create: `client/src/shared/command-bar/sources/createCommandSource.ts`
- Modify: `client/src/shared/command-bar/commandBarBootstrap.ts`
- Modify: `client/src/pages/automation/projects/components/ProjectDialog.tsx`
- Modify: `client/src/pages/automation/datatables/components/CreateDataTableDialog.tsx`
- Modify: `client/src/pages/automation/knowledge-bases/components/CreateKnowledgeBaseDialog.tsx`
- Modify: `client/src/shared/components/connection/ConnectionDialog.tsx`
- Test: `client/src/shared/command-bar/tests/createCommandSource.test.ts`

**Interfaces:**
- Produces: `createCommandSource: CommandSourceI`, and the intent keys `project.create`, `dataTable.create`, `knowledgeBase.create`, `connection.create`.

- [ ] **Step 1: Write the failing test**

`client/src/shared/command-bar/tests/createCommandSource.test.ts`:

```ts
import {createCommandSource} from '@/shared/command-bar/sources/createCommandSource';
import {type CommandContextI} from '@/shared/command-bar/types';
import {describe, expect, it} from 'vitest';

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/automation/datatables'};

describe('createCommandSource', () => {
    it('navigates to the list page then publishes the create intent', () => {
        const createProject = createCommandSource
            .getCommands(context)
            .find((command) => command.id === 'create.project')!;

        expect(createProject.actions).toEqual([
            {to: '/automation/projects', type: 'navigate'},
            {key: 'project.create', type: 'intent'},
        ]);
    });

    it('exposes a create command for each supported resource', () => {
        expect(createCommandSource.getCommands(context).map((command) => command.id)).toEqual([
            'create.project',
            'create.connection',
            'create.dataTable',
            'create.knowledgeBase',
        ]);
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd client && npx vitest run src/shared/command-bar/tests/createCommandSource.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the source**

`client/src/shared/command-bar/sources/createCommandSource.ts`:

```ts
import {type CommandSourceI} from '@/shared/command-bar/types';
import {FolderIcon, Link2Icon, PlusIcon, Table2Icon, VectorSquareIcon} from 'lucide-react';

interface CreateCommandDescriptorI {
    group: string;
    icon: typeof PlusIcon;
    id: string;
    intentKey: string;
    title: string;
    to: string;
}

const CREATE_COMMAND_DESCRIPTORS: CreateCommandDescriptorI[] = [
    {group: 'Projects', icon: FolderIcon, id: 'create.project', intentKey: 'project.create', title: 'Create project', to: '/automation/projects'},
    {group: 'Connections', icon: Link2Icon, id: 'create.connection', intentKey: 'connection.create', title: 'Create connection', to: '/automation/connections'},
    {group: 'Data Tables', icon: Table2Icon, id: 'create.dataTable', intentKey: 'dataTable.create', title: 'Create data table', to: '/automation/datatables'},
    {group: 'Knowledge Base', icon: VectorSquareIcon, id: 'create.knowledgeBase', intentKey: 'knowledgeBase.create', title: 'Create knowledge base', to: '/automation/knowledge-bases'},
];

export const createCommandSource: CommandSourceI = {
    getCommands: () =>
        CREATE_COMMAND_DESCRIPTORS.map((descriptor) => ({
            actions: [
                {to: descriptor.to, type: 'navigate'},
                {key: descriptor.intentKey, type: 'intent'},
            ],
            group: descriptor.group,
            icon: descriptor.icon,
            id: descriptor.id,
            title: descriptor.title,
        })),
    id: 'create',
};
```

Register it in `commandBarBootstrap.ts` alongside `resourceCommandSource`.

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd client && npx vitest run src/shared/command-bar/tests/createCommandSource.test.ts
```

Expected: PASS, 2 tests.

- [ ] **Step 5: Claim the intents in the dialogs**

Each creation dialog owns its own `open` state, so the claim goes inside the dialog, not the page.

`CreateDataTableDialog.tsx` — after the `useCreateDataTableDialog()` destructure, before the `handleDialogOpenChange` definition:

```ts
    useCommandIntent('dataTable.create', handleOpen);
```

`ProjectDialog.tsx` — it holds `const [isOpen, setIsOpen] = useState(!triggerNode);`, so:

```ts
    useCommandIntent('project.create', () => setIsOpen(true));
```

`CreateKnowledgeBaseDialog.tsx` and `ConnectionDialog.tsx` — read each file's open-state handling first and add the equivalent one-line claim:

```bash
grep -n "useState\|open\|setOpen\|handleOpen" client/src/pages/automation/knowledge-bases/components/CreateKnowledgeBaseDialog.tsx client/src/shared/components/connection/ConnectionDialog.tsx | head -20
```

- [ ] **Step 6: Verify each dialog is actually mounted on its list page**

A claim only fires if the component mounts. `ConnectionDialog` in particular is rendered from two places in `Connections.tsx`; if either is inside a dropdown or popover that mounts lazily, the claim never runs there.

```bash
grep -n "ConnectionDialog" -B 5 client/src/pages/automation/connections/Connections.tsx
```

If a dialog turns out to be mounted only inside a lazily-mounted menu, hoist one always-mounted instance onto the page for the intent to claim, and say so in the commit message.

- [ ] **Step 7: Verify by hand**

```bash
cd client && npm run dev
```

Open the app, press Cmd+K from `/automation/datatables`, run "Create project": the app should land on the projects list with the create dialog open. Repeat for each of the four. Any that opens no dialog will have logged the unclaimed-intent warning from Task 6 in the console — that message names the key that went unclaimed.

- [ ] **Step 8: Check and commit**

```bash
cd client && npm run check
git add client/src
git commit -m "2396 client - Add create commands and claim them in the creation dialogs"
```

---

### Task 16: (dropped at pre-flight — see ledger Ruling B)

The plan assumed a list page could mark a row selected in about two lines. It cannot: `ConnectionList`
takes only `componentDefinitions`, `connections` and `tags` and has no selection concept, and
`ApiCollections` has none either. Introducing one is a UI feature per page, not an intent claim.

The select intents themselves still ship — Task 12 publishes them, and the five list-only types land
on the correct list page, which is the behaviour the spec calls incremental opt-in. Claiming them is
separate work, sized per page.

---

### Task 17: Workflow editor commands

The first page-scoped registration, and the first commands whose actions are callbacks — which is also why none of them appear in recents.

**Files:**
- Create: `client/src/pages/platform/workflow-editor/hooks/useRegisterWorkflowEditorCommands.ts`
- Modify: `client/src/pages/automation/project/Project.tsx`
- Test: `client/src/pages/platform/workflow-editor/hooks/tests/useRegisterWorkflowEditorCommands.test.tsx`

**Interfaces:**
- Consumes: `useRegisterCommands` from Task 5, `useWorkflowEditorStore`.

- [ ] **Step 1: Write the failing test**

```tsx
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useRegisterWorkflowEditorCommands} from '@/pages/platform/workflow-editor/hooks/useRegisterWorkflowEditorCommands';
import {type CommandContextI} from '@/shared/command-bar/types';
import {collectCommands, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/automation/projects/1/project-workflows/2'};

describe('useRegisterWorkflowEditorCommands', () => {
    beforeEach(() => {
        useCommandSourceRegistry.getState().reset();
    });

    it('registers the editor commands while mounted', () => {
        const {unmount} = renderHook(() => useRegisterWorkflowEditorCommands());

        const ids = collectCommands(useCommandSourceRegistry.getState().sources, context).map((command) => command.id);

        expect(ids).toContain('workflowEditor.inputs');

        unmount();

        expect(collectCommands(useCommandSourceRegistry.getState().sources, context)).toEqual([]);
    });

    it('opens the inputs sheet when its command runs', async () => {
        renderHook(() => useRegisterWorkflowEditorCommands());

        const command = collectCommands(useCommandSourceRegistry.getState().sources, context).find(
            (registered) => registered.id === 'workflowEditor.inputs'
        )!;

        await (command.actions![0] as {run: () => Promise<void> | void}).run();

        expect(useWorkflowEditorStore.getState().showWorkflowInputsSheet).toBe(true);
    });
});
```

- [ ] **Step 2: Run it to verify it fails**

```bash
cd client && npx vitest run src/pages/platform/workflow-editor/hooks/tests/useRegisterWorkflowEditorCommands.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the hook**

```ts
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {type CommandI} from '@/shared/command-bar/types';
import {useRegisterCommands} from '@/shared/command-bar/useRegisterCommands';
import {CodeIcon, PanelBottomIcon, SettingsIcon, SquareArrowDownIcon, SquareArrowUpIcon} from 'lucide-react';
import {useMemo} from 'react';

const GROUP = 'Workflow Editor';

/**
 * Page-scoped commands: they exist only while the editor is mounted. Every action is a callback, so none of them are
 * recorded in recents -- replaying "toggle the bottom panel" from another page would be meaningless.
 */
export function useRegisterWorkflowEditorCommands(): void {
    const commands = useMemo<CommandI[]>(() => {
        const {
            setShowBottomPanelOpen,
            setShowEditWorkflowDialog,
            setShowWorkflowCodeEditorSheet,
            setShowWorkflowInputsSheet,
            setShowWorkflowOutputsSheet,
        } = useWorkflowEditorStore.getState();

        return [
            {actions: [{run: () => setShowWorkflowInputsSheet(true), type: 'callback'}], group: GROUP, icon: SquareArrowDownIcon, id: 'workflowEditor.inputs', title: 'Edit workflow inputs'},
            {actions: [{run: () => setShowWorkflowOutputsSheet(true), type: 'callback'}], group: GROUP, icon: SquareArrowUpIcon, id: 'workflowEditor.outputs', title: 'Edit workflow outputs'},
            {actions: [{run: () => setShowWorkflowCodeEditorSheet(true), type: 'callback'}], group: GROUP, icon: CodeIcon, id: 'workflowEditor.source', title: 'Open workflow source'},
            {actions: [{run: () => setShowEditWorkflowDialog(true), type: 'callback'}], group: GROUP, icon: SettingsIcon, id: 'workflowEditor.settings', title: 'Edit workflow settings'},
            {
                actions: [
                    {
                        run: () => setShowBottomPanelOpen(!useWorkflowEditorStore.getState().showBottomPanel),
                        type: 'callback',
                    },
                ],
                group: GROUP,
                icon: PanelBottomIcon,
                id: 'workflowEditor.bottomPanel',
                title: 'Toggle bottom panel',
            },
        ];
    }, []);

    useRegisterCommands(commands, [commands]);
}
```

Setters are read through `getState()` rather than the hook so the memo does not re-run — Zustand setter identities are stable, but subscribing to them would re-render the editor on every unrelated store change.

- [ ] **Step 4: Verify the setter names against the store**

```bash
grep -n "setShowBottomPanelOpen\|setShowEditWorkflowDialog\|setShowWorkflowCodeEditorSheet\|setShowWorkflowInputsSheet\|setShowWorkflowOutputsSheet\|showBottomPanel" client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.ts
```

Correct any name that does not match exactly.

- [ ] **Step 5: Call it from the editor**

Add `useRegisterWorkflowEditorCommands();` to `Project.tsx`, placed with the other custom hooks (before any derived values or `useEffect`, per the hook-ordering rule).

- [ ] **Step 6: Run it to verify it passes**

```bash
cd client && npx vitest run src/pages/platform/workflow-editor/hooks/tests/useRegisterWorkflowEditorCommands.test.tsx
```

Expected: PASS, 2 tests.

- [ ] **Step 7: Verify by hand**

```bash
cd client && npm run dev
```

Open a workflow, press Cmd+K — the Workflow Editor group appears. Navigate away and press Cmd+K again — it is gone.

- [ ] **Step 8: Check and commit**

```bash
cd client && npm run check
git add client/src
git commit -m "2396 client - Register workflow editor commands while the editor is mounted"
```

---

## Final verification

- [ ] **Full client check**

```bash
cd client && npm run check
```

- [ ] **Full server check**

```bash
./gradlew spotlessApply
./gradlew check --continue > /tmp/gradle-check.log 2>&1; echo "exit: $?"
grep '^> Task .* FAILED' /tmp/gradle-check.log || echo "no failed tasks"
```

Never read the exit status of a Gradle run through a pipe — the status belongs to the filter. Redirect, check `$?` on its own line, then grep the file.

- [ ] **Manual smoke test of the whole surface**

With `ff-2396` enabled: Cmd+K opens; typing filters; Recent appears after the first command; "Open workflow" pushes a sub-mode whose placeholder changes; Backspace on an empty input pops it; Escape pops when nested and closes at root; selecting a workflow lands in the editor rather than on a 404.
