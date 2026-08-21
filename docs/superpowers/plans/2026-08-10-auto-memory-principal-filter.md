# Auto-Memory Principal Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the auto-memory GraphQL operations address a `PROJECT_DEPLOYMENT` principal as well as the signed-in user, so a workspace can inspect and correct the long-term memory its deployed agents accumulate.

**Architecture:** All four operations gain an optional `(principalType, principalId)` pair defaulting to `(USER, current user)`. A single package-private `resolvePrincipal` helper in the existing controller decides what the caller may address; it returns `null` for denial and throws only for malformed input. No service, repository, or entity changes — `AiAutoMemoryService` already takes `(workspaceId, principalType, principalId, …)` on every method.

**Tech Stack:** Java 25, Spring Boot 4, Spring GraphQL, JUnit 5, Mockito, AssertJ; GraphQL Code Generator on the client.

**Spec:** `docs/superpowers/specs/2026-08-10-auto-memory-principal-filter-design.md`

## Global Constraints

- These files are under `server/libs/` — **Apache 2.0 license header, no `@version ee` tag**.
- Unit test classes end in `Test`. Test method names are camelCase with **no underscores** — Checkstyle enforces this on every method in test sources, including private helpers.
- No `TODO:` comments (Checkstyle `TodoComment`).
- One blank line before control statements, except immediately after an opening `{`; one blank line between a variable modification and the next statement using it; no trailing blank line before a class's closing `}`.
- Descriptive names everywhere, including lambda parameters. No `_` prefix on private methods.
- Run `./gradlew spotlessApply` before every server commit and `npm run format` before every client commit.
- Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep `^> Task .* FAILED`.
- Gradle's `test` task excludes `**/*IntTest*` **and** sets `failOnNoDiscoveredTests=false`, so a misnamed test exits 0 having run nothing. Always confirm the `Results:` line shows the expected count.
- Commit messages: `732 <description>` for server, `732 client - <description>` for client.

---

### Task 1: The principal resolution helper

Pure decision table, no schema or resolver changes yet. Package-private so it is directly testable — the test class already lives in the same package.

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/main/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlController.java`
- Test: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/test/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlControllerTest.java`

**Interfaces:**
- Produces: `ResolvedPrincipal(AiAutoMemoryPrincipalType principalType, long principalId)` and
  `@Nullable ResolvedPrincipal resolvePrincipal(@Nullable AiAutoMemoryPrincipalType principalType, @Nullable Long principalId, long currentUserId, boolean mutating)`. Tasks 2 and 3 call it from the resolvers.

- [ ] **Step 1: Write the failing tests**

Append to `AiAutoMemoryGraphQlControllerTest`. Add these imports: `org.junit.jupiter.api.AfterEach`, `java.util.List`, `org.springframework.security.authentication.UsernamePasswordAuthenticationToken`, `org.springframework.security.core.authority.SimpleGrantedAuthority`, `org.springframework.security.core.context.SecurityContextHolder`, and `static org.assertj.core.api.Assertions.assertThatThrownBy`.

```java
    private static final long CURRENT_USER_ID = 7;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testOmittingBothArgumentsDefaultsToTheCurrentUser() {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal =
            aiAutoMemoryGraphQlController.resolvePrincipal(null, null, CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalType()).isEqualTo(AiAutoMemoryPrincipalType.USER);
        assertThat(principal.principalId()).isEqualTo(CURRENT_USER_ID);
    }

    @Test
    void testOwnUserIdIsAddressable() {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal =
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalId()).isEqualTo(CURRENT_USER_ID);
    }

    /**
     * Two members of one workspace both have rows under that workspaceId, so the lookup key does not isolate them.
     * Denial is a null return rather than an exception so a caller cannot tell "not yours" from "no such memory".
     */
    @Test
    void testAnotherUsersIdIsDenied() {
        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.USER, 99L, CURRENT_USER_ID, false)).isNull();
    }

    @Test
    void testDeploymentIsReadableByAnyMember() {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal =
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalType()).isEqualTo(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT);
        assertThat(principal.principalId()).isEqualTo(5);
    }

    @Test
    void testDeploymentMutationIsDeniedWithoutAdmin() {
        authenticateWith("ROLE_USER");

        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, CURRENT_USER_ID, true)).isNull();
    }

    @Test
    void testDeploymentMutationIsAllowedForAdmin() {
        authenticateWith("ROLE_ADMIN");

        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, CURRENT_USER_ID, true)).isNotNull();
    }

    @Test
    void testIntegrationInstanceIsRejected() {
        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 5L, CURRENT_USER_ID, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("INTEGRATION_INSTANCE");
    }

    @Test
    void testHalfSpecifiedPrincipalIsRejected() {
        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, null, CURRENT_USER_ID, false))
                    .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.resolvePrincipal(null, 5L, CURRENT_USER_ID, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void authenticateWith(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "tester", "password", List.of(new SimpleGrantedAuthority(authority))));
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:
```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:test \
  --tests '*AiAutoMemoryGraphQlControllerTest*' > /tmp/red1.log 2>&1
echo $?
```

Expected: non-zero exit, compilation failure — `resolvePrincipal` and `ResolvedPrincipal` do not exist.

- [ ] **Step 3: Implement the helper**

Add to `AiAutoMemoryGraphQlController`, above the private `verifyUserCanAccessWorkspace`. Add imports `com.bytechef.platform.security.constant.AuthorityConstants` and `com.bytechef.platform.security.util.SecurityUtils`.

```java
    /**
     * Decides which principal the caller may address. Returns {@code null} when the caller may not address the
     * requested principal — reads map that to an empty result and mutations to not-found, so a caller cannot tell
     * "not yours" from "no such memory" and ids stay unenumerable. Throws only for malformed input, which is a client
     * bug rather than an ownership signal.
     *
     * <p>
     * {@code PROJECT_DEPLOYMENT} needs no ownership lookup: every service call filters on {@code workspaceId} and the caller's
     * membership of that workspace is verified before this runs, so a deployment in another workspace matches nothing.
     * {@code USER} cannot rely on that — two members of one workspace both have rows under it — which is the only
     * reason the own-id guard exists.
     * </p>
     */
    @Nullable
    ResolvedPrincipal resolvePrincipal(
        @Nullable AiAutoMemoryPrincipalType principalType, @Nullable Long principalId, long currentUserId,
        boolean mutating) {

        if ((principalType == null) != (principalId == null)) {
            throw new IllegalArgumentException(
                "principalType and principalId must be supplied together or both omitted");
        }

        if (principalType == null) {
            return new ResolvedPrincipal(AiAutoMemoryPrincipalType.USER, currentUserId);
        }

        if (principalType == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE) {
            throw new IllegalArgumentException(
                "INTEGRATION_INSTANCE memories are not addressable through this API: embedded integration-instance " +
                    "rows all live in the default workspace, so workspace membership does not isolate them");
        }

        if (principalType == AiAutoMemoryPrincipalType.USER) {
            return principalId == currentUserId ? new ResolvedPrincipal(principalType, principalId) : null;
        }

        // Deployment memory is written by a running workflow, and editing it changes how a live agent behaves on its
        // next run with no notification to anyone — so mutating requires admin while reading does not.
        if (mutating && !SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {
            return null;
        }

        return new ResolvedPrincipal(principalType, principalId);
    }
```

And the carrier, next to `UpdateAiAutoMemoryInput`:

```java
    /**
     * The principal a request resolved to, after defaulting and authorization.
     */
    record ResolvedPrincipal(AiAutoMemoryPrincipalType principalType, long principalId) {
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:
```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:test \
  --tests '*AiAutoMemoryGraphQlControllerTest*' > /tmp/green1.log 2>&1
echo $?
grep -E "Results:" /tmp/green1.log
```

Expected: exit 0, and the `Results:` line shows **11 tests** (3 pre-existing plus 8 new).

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src
git commit -m "732 Add auto-memory principal resolution with per-principal authorization"
```

---

### Task 2: Read operations take the principal

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/main/resources/graphql/ai-auto-memory.graphqls`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/main/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlController.java`
- Test: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/test/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `resolvePrincipal(...)` and `ResolvedPrincipal` (Task 1).

- [ ] **Step 1: Write the failing tests**

The existing test class constructs the controller with `mock(AiAutoMemoryService.class)` and `mock(UserService.class)`. These two tests need those mocks stubbed, so declare them as fields and build the controller from them — replace the existing single-line field initialisation with:

```java
    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);
    private final UserService userService = mock(UserService.class);
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    private final AiAutoMemoryGraphQlController aiAutoMemoryGraphQlController = new AiAutoMemoryGraphQlController(
        aiAutoMemoryService, userService, workspaceFacade);
```

Then add these tests. Imports needed: `com.bytechef.automation.configuration.domain.Workspace`, `com.bytechef.platform.user.domain.User`, and `static org.mockito.Mockito.verify`, `static org.mockito.Mockito.when`, `static org.mockito.ArgumentMatchers.eq`, `static org.mockito.ArgumentMatchers.isNull`.

```java
    @Test
    void testListWithADeploymentPrincipalQueriesThatPrincipal() {
        givenCurrentUserInWorkspace();

        aiAutoMemoryGraphQlController.aiAutoMemories(
            1L, 0, null, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L);

        verify(aiAutoMemoryService).list(
            eq(1L), eq(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT), eq(5L), eq(0), isNull());
    }

    /**
     * A denied principal must not reach the service at all — returning empty is what keeps ids unenumerable.
     */
    @Test
    void testListWithAnotherUsersIdReturnsEmptyWithoutQuerying() {
        givenCurrentUserInWorkspace();

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, AiAutoMemoryPrincipalType.USER, 99L))
            .isEmpty();

        verify(aiAutoMemoryService, never()).list(anyLong(), any(), anyLong(), anyInt(), any());
    }

    @Test
    void testSingleFetchWithAnotherUsersIdReturnsNull() {
        givenCurrentUserInWorkspace();

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemory(1L, 3L, AiAutoMemoryPrincipalType.USER, 99L)).isNull();
    }

    private void givenCurrentUserInWorkspace() {
        User user = new User();

        user.setId(CURRENT_USER_ID);

        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace = new Workspace();

        workspace.setId(1L);

        when(workspaceFacade.getUserWorkspaces(CURRENT_USER_ID)).thenReturn(List.of(workspace));
    }
```

Add `static org.mockito.Mockito.never`, `static org.mockito.ArgumentMatchers.any`, `static org.mockito.ArgumentMatchers.anyLong`, `static org.mockito.ArgumentMatchers.anyInt` to the imports.

- [ ] **Step 2: Run the tests to verify they fail**

Run:
```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:test \
  --tests '*AiAutoMemoryGraphQlControllerTest*' > /tmp/red2.log 2>&1
echo $?
```

Expected: non-zero — the two-argument `aiAutoMemories`/`aiAutoMemory` do not accept a principal.

- [ ] **Step 3: Add the schema arguments**

In `ai-auto-memory.graphqls`, replace the two query definitions inside `extend type Query`:

```graphql
    """
    Lists memories in the workspace, scoped to the supplied environment so DEVELOPMENT preferences do not bleed
    into PRODUCTION sessions and vice versa. The optional memoryType filter narrows results to a single category.
    Ordered by updatedAt DESC.

    principalType and principalId are supplied together or both omitted; omitting them means the signed-in user.
    A USER principal other than the caller returns an empty list rather than an error, so ids stay unenumerable.
    INTEGRATION_INSTANCE is not addressable — those rows are not workspace-isolated.
    """
    aiAutoMemories(
        workspaceId: ID!,
        environment: Int!,
        memoryType: AiAutoMemoryType,
        principalType: AiAutoMemoryPrincipalType,
        principalId: Long
    ): [AiAutoMemory!]!

    """
    Returns a single memory by id, verifying ownership against the resolved principal. Returns null when missing
    or not addressable by the caller — the same shape on both errors so a probe cannot enumerate ids across
    workspaces.
    """
    aiAutoMemory(
        workspaceId: ID!,
        id: ID!,
        principalType: AiAutoMemoryPrincipalType,
        principalId: Long
    ): AiAutoMemory
```

- [ ] **Step 4: Wire the read resolvers**

Replace the two query methods in `AiAutoMemoryGraphQlController`:

```java
    @QueryMapping
    public List<AiAutoMemory> aiAutoMemories(
        @Argument long workspaceId, @Argument int environment, @Argument @Nullable AiAutoMemoryType memoryType,
        @Argument @Nullable AiAutoMemoryPrincipalType principalType, @Argument @Nullable Long principalId) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal principal = resolvePrincipal(principalType, principalId, userId, false);

        if (principal == null) {
            return List.of();
        }

        return aiAutoMemoryService.list(
            workspaceId, principal.principalType(), principal.principalId(), environment, memoryType);
    }

    @QueryMapping
    @Nullable
    public AiAutoMemory aiAutoMemory(
        @Argument long workspaceId, @Argument long id,
        @Argument @Nullable AiAutoMemoryPrincipalType principalType, @Argument @Nullable Long principalId) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal principal = resolvePrincipal(principalType, principalId, userId, false);

        if (principal == null) {
            return null;
        }

        Optional<AiAutoMemory> memory =
            aiAutoMemoryService.findById(workspaceId, principal.principalType(), principal.principalId(), id);

        return memory.orElse(null);
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run:
```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:test \
  --tests '*AiAutoMemoryGraphQlControllerTest*' > /tmp/green2.log 2>&1
echo $?
grep -E "Results:" /tmp/green2.log
```

Expected: exit 0, `Results:` shows **14 tests**.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src
git commit -m "732 Let auto-memory reads address a deployment principal"
```

---

### Task 3: Mutations take the principal

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/main/resources/graphql/ai-auto-memory.graphqls`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/main/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlController.java`
- Test: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/test/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `resolvePrincipal(...)` (Task 1), `givenCurrentUserInWorkspace()` and `authenticateWith(...)` (Tasks 1-2).
- Produces: `UpdateAiAutoMemoryInput` gains two trailing components — `@Nullable AiAutoMemoryPrincipalType principalType, @Nullable Long principalId`.

- [ ] **Step 1: Write the failing tests**

```java
    @Test
    void testDeletingADeploymentMemoryAsAdminReachesTheService() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_ADMIN");

        aiAutoMemoryGraphQlController.deleteAiAutoMemory(1L, 3L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L);

        verify(aiAutoMemoryService).deleteById(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 3L);
    }

    /**
     * A non-admin gets the not-found shape rather than a distinct authorization error, matching the read path — the
     * caller learns nothing about whether the row exists.
     */
    @Test
    void testDeletingADeploymentMemoryWithoutAdminThrowsNotFound() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_USER");

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.deleteAiAutoMemory(
                1L, 3L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L))
                    .isInstanceOf(AiAutoMemoryNotFoundException.class);

        verify(aiAutoMemoryService, never()).deleteById(anyLong(), any(), anyLong(), anyLong());
    }

    @Test
    void testUpdatingADeploymentMemoryAsAdminReachesTheService() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_ADMIN");

        aiAutoMemoryGraphQlController.updateAiAutoMemory(
            new AiAutoMemoryGraphQlController.UpdateAiAutoMemoryInput(
                3L, 1L, "New title", null, null, null, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L));

        verify(aiAutoMemoryService).updateById(
            eq(1L), eq(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT), eq(5L), eq(3L), eq("New title"), isNull(), isNull(),
            isNull());
    }
```

Add `com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException` to the imports.

- [ ] **Step 2: Run the tests to verify they fail**

Run:
```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:test \
  --tests '*AiAutoMemoryGraphQlControllerTest*' > /tmp/red3.log 2>&1
echo $?
```

Expected: non-zero — the record has six components and `deleteAiAutoMemory` takes two arguments.

- [ ] **Step 3: Add the schema arguments**

In `ai-auto-memory.graphqls`, replace the mutation block and the input:

```graphql
extend type Mutation {
    """
    Partial update of a memory by primary key, scoped to the resolved principal. The memory's environment is
    immutable post-create so it does not appear in the patch input — environments do not move. Updating a
    PROJECT_DEPLOYMENT-owned memory requires ROLE_ADMIN: it changes how a live agent behaves on its next run.
    """
    updateAiAutoMemory(input: UpdateAiAutoMemoryInput!): AiAutoMemory!

    """
    Deletes a memory by primary key. Returns true on success; throws NotFound when the row does not exist or is
    not addressable by the caller. Deleting a PROJECT_DEPLOYMENT-owned memory requires ROLE_ADMIN.
    """
    deleteAiAutoMemory(
        workspaceId: ID!,
        id: ID!,
        principalType: AiAutoMemoryPrincipalType,
        principalId: Long
    ): Boolean!
}

input UpdateAiAutoMemoryInput {
    id: ID!
    workspaceId: ID!
    title: String
    description: String
    memoryType: AiAutoMemoryType
    content: String
    principalType: AiAutoMemoryPrincipalType
    principalId: Long
}
```

- [ ] **Step 4: Wire the mutation resolvers**

Replace both mutation methods and the input record:

```java
    @MutationMapping
    public AiAutoMemory updateAiAutoMemory(@Argument UpdateAiAutoMemoryInput input) {
        if (input.title() == null && input.description() == null && input.memoryType() == null
            && input.content() == null) {
            throw new IllegalArgumentException(
                "UpdateAiAutoMemoryInput requires at least one of title, description, memoryType, content");
        }

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, input.workspaceId());

        ResolvedPrincipal principal = resolvePrincipal(input.principalType(), input.principalId(), userId, true);

        if (principal == null) {
            throw new AiAutoMemoryNotFoundException("AiAutoMemory not found");
        }

        return aiAutoMemoryService.updateById(
            input.workspaceId(), principal.principalType(), principal.principalId(), input.id(),
            input.title(), input.description(), input.memoryType(), input.content());
    }

    @MutationMapping
    public boolean deleteAiAutoMemory(
        @Argument long workspaceId, @Argument long id,
        @Argument @Nullable AiAutoMemoryPrincipalType principalType, @Argument @Nullable Long principalId) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal principal = resolvePrincipal(principalType, principalId, userId, true);

        if (principal == null) {
            throw new AiAutoMemoryNotFoundException("AiAutoMemory not found");
        }

        aiAutoMemoryService.deleteById(workspaceId, principal.principalType(), principal.principalId(), id);

        return true;
    }
```

```java
    /**
     * Workspace, id, and the patch fields. Environment is intentionally absent — the row's environment is immutable
     * post-create, and the primary key already pins the partition. {@code principalType}/{@code principalId} are
     * supplied together or both omitted; omitting them targets the signed-in user.
     */
    public record UpdateAiAutoMemoryInput(
        long id, long workspaceId, @Nullable String title, @Nullable String description,
        @Nullable AiAutoMemoryType memoryType, @Nullable String content,
        @Nullable AiAutoMemoryPrincipalType principalType, @Nullable Long principalId) {
    }
```

Add `com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException` to the controller's imports.

- [ ] **Step 5: Run the module suite**

Run:
```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:test \
  > /tmp/green3.log 2>&1
echo $?
grep -E "^> Task .* FAILED|Results:" /tmp/green3.log
```

Expected: exit 0, no FAILED tasks, `Results:` shows **17 tests**.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src
git commit -m "732 Gate deployment auto-memory mutations behind ROLE_ADMIN"
```

---

### Task 4: Regenerate client types and verify

At this point in the sequence the client operations do not change — the new arguments are optional and the Memories page still shows the signed-in user's memories. Only the schema-derived types in the generated file move, and they must not be left stale. Task 7 changes the operations themselves and regenerates again.

**Files:**
- Modify: `client/src/shared/middleware/graphql-types.ts` (generated)
- Modify: `client/src/shared/middleware/graphql.ts` (generated)

**Interfaces:**
- Consumes: the schema from Tasks 2 and 3.

- [ ] **Step 1: Regenerate**

```bash
cd client
npx graphql-codegen
```

Expected: exit 0, ending with "Generate outputs". A `Cannot query field` error here means a `.graphql` operation selects something the schema no longer has — fix the operation, do not hand-edit the generated file.

- [ ] **Step 2: Confirm the arguments reached the generated types**

Run:
```bash
grep -n "principalType" client/src/shared/middleware/graphql-types.ts | head
```

Expected: matches on the `QueryAiAutoMemoriesArgs`, `QueryAiAutoMemoryArgs`, `MutationDeleteAiAutoMemoryArgs`, and `UpdateAiAutoMemoryInput` types.

- [ ] **Step 3: Full client check**

```bash
cd client
npm run format
npm run check
```

Expected: exit 0. `Test Files 564 passed`, `Tests 5184 passed`.

- [ ] **Step 4: Commit**

```bash
git add client/src/shared/middleware/graphql-types.ts client/src/shared/middleware/graphql.ts
git commit -m "732 client - Regenerate GraphQL types for the auto-memory principal filter"
```

---

### Task 5: The principals-with-memories finder

The picker needs the distinct owners that actually hold memory. This task is the repository + service half; Task 6 exposes it.

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-api/src/main/java/com/bytechef/platform/ai/auto/memory/repository/AiAutoMemoryRepository.java`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-jdbc/src/main/java/com/bytechef/platform/ai/auto/memory/repository/jdbc/JdbcAiAutoMemoryRepository.java`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-file-storage/src/main/java/com/bytechef/platform/ai/auto/memory/repository/filestorage/FileStorageAiAutoMemoryRepository.java`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryService.java`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-service/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryServiceImpl.java`
- Test: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-test-support/src/main/java/com/bytechef/platform/ai/auto/memory/repository/AiAutoMemoryRepositoryContractTests.java`

**Interfaces:**
- Produces: `AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType principalType, long principalId, int memoryCount)` in `platform-ai-auto-memory-api`, and
  `List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, int environment)` on `AiAutoMemoryService`. Task 6 consumes both.

**Read first:** open `AiAutoMemoryRepository` and both implementations before writing anything. The contract test in `-test-support` is shared by the jdbc and file-storage backends; a finder added to only one will fail the other's contract run, which is the point of that module.

- [ ] **Step 1: Add the contract test**

Append to `AiAutoMemoryRepositoryContractTests`, following the arrangement of the finder tests already in that class:

```java
    @Test
    void testListPrincipalsReturnsDistinctOwnersWithCounts() {
        save(newMemory(WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT, "a"));
        save(newMemory(WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT, "b"));
        save(newMemory(WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5, ENVIRONMENT, "c"));

        List<AiAutoMemoryPrincipalCount> principals = getRepository().listPrincipals(WORKSPACE_ID, ENVIRONMENT);

        assertThat(principals)
            .extracting(
                AiAutoMemoryPrincipalCount::principalType, AiAutoMemoryPrincipalCount::principalId,
                AiAutoMemoryPrincipalCount::memoryCount)
            .containsExactlyInAnyOrder(
                tuple(AiAutoMemoryPrincipalType.USER, 1L, 2),
                tuple(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 1));
    }

    @Test
    void testListPrincipalsExcludesOtherWorkspacesAndEnvironments() {
        save(newMemory(WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT, "a"));
        save(newMemory(WORKSPACE_ID + 1, AiAutoMemoryPrincipalType.USER, 2, ENVIRONMENT, "b"));
        save(newMemory(WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, 3, ENVIRONMENT + 1, "c"));

        List<AiAutoMemoryPrincipalCount> principals = getRepository().listPrincipals(WORKSPACE_ID, ENVIRONMENT);

        assertThat(principals)
            .extracting(AiAutoMemoryPrincipalCount::principalId)
            .containsExactly(1L);
    }
```

Match the existing class's helper names — if it does not already have `save`, `newMemory`, `getRepository`, `WORKSPACE_ID`, or `ENVIRONMENT`, use whatever it does have rather than introducing new ones. Add `static org.assertj.core.api.Assertions.tuple` to the imports.

- [ ] **Step 2: Run the contract suites to verify they fail**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:test \
  > /tmp/red5.log 2>&1
echo $?
```

Expected: non-zero, compilation failure — `listPrincipals` and `AiAutoMemoryPrincipalCount` do not exist.

- [ ] **Step 3: Add the record and the repository method**

Create `platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryPrincipalCount.java` with the Apache header, then:

```java
/**
 * One owner that holds memory in a workspace and environment, with how many rows it holds. Drives the Memories
 * page's owner picker so it never offers an owner with nothing to show.
 *
 * @author Ivica Cardic
 */
public record AiAutoMemoryPrincipalCount(
    AiAutoMemoryPrincipalType principalType, long principalId, int memoryCount) {
}
```

Add to `AiAutoMemoryRepository`:

```java
    /**
     * The distinct {@code (principalType, principalId)} pairs holding memory in this workspace and environment, with
     * per-principal counts.
     */
    List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, int environment);
```

Implement in `JdbcAiAutoMemoryRepository` with a grouped query over `(principal_type, principal_id)` filtered by `workspace_id` and `environment`, mapping `principal_type` back through `AiAutoMemoryPrincipalType.values()[ordinal]`. Implement in `FileStorageAiAutoMemoryRepository` by grouping the workspace + environment directory listing it already walks.

- [ ] **Step 4: Add the service passthrough**

On `AiAutoMemoryService`:

```java
    /**
     * The owners holding memory in this workspace and environment. Unfiltered — the GraphQL layer applies the same
     * per-principal authorization it applies to reads.
     */
    List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, int environment);
```

and the matching one-line delegation in `AiAutoMemoryServiceImpl`.

- [ ] **Step 5: Run both backends' contract suites**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:test \
          :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-service:test \
          > /tmp/green5.log 2>&1
echo $?
grep -E "^> Task .* FAILED|Results:" /tmp/green5.log
```

Expected: exit 0, no FAILED tasks. The jdbc backend's contract runs under `testIntegration` (Testcontainers) — run it too:

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-jdbc:testIntegration \
  > /tmp/green5b.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/green5b.log
```

Expected: exit 0, no FAILED tasks. Requires Docker.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory
git commit -m "732 Add a finder for the principals holding auto-memory"
```

---

### Task 6: Expose the principals query

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/main/resources/graphql/ai-auto-memory.graphqls`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/main/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlController.java`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/build.gradle.kts` — no change needed; `automation-configuration-api` is already a dependency. Verify before assuming.
- Test: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/test/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `AiAutoMemoryService.listPrincipals(...)` and `AiAutoMemoryPrincipalCount` (Task 5), `resolvePrincipal(...)` (Task 1).
- Produces: query `aiAutoMemoryPrincipals(workspaceId, environment)` returning `AiAutoMemoryPrincipal` rows.

- [ ] **Step 1: Write the failing tests**

```java
    /**
     * The picker must apply the same rules as the read path: offering an owner the reads then refuse would be a bug
     * surface, and revealing that other users hold memory would leak exactly what the foreign-id guard protects.
     */
    @Test
    void testPrincipalsExcludesOtherUsersAndIntegrationInstances() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, 0)).thenReturn(
            List.of(
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, 99, 5),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5, 1),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 7, 3)));

        List<AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal> principals =
            aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0);

        assertThat(principals)
            .extracting(
                AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::principalType,
                AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::principalId)
            .containsExactly(
                tuple(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID),
                tuple(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L));
    }

    @Test
    void testOwnUserPrincipalIsLabelledWithoutTheWordUser() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, 0)).thenReturn(
            List.of(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2)));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0))
            .singleElement()
            .extracting(AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::label)
            .isEqualTo("My memories");
    }
```

Add `static org.assertj.core.api.Assertions.tuple` and `com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount` to the imports. The controller now needs a `ProjectDeploymentService` mock — add it as a fourth constructor argument in the test's field initialisation.

- [ ] **Step 2: Run to verify they fail**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:test \
  --tests '*AiAutoMemoryGraphQlControllerTest*' > /tmp/red6.log 2>&1
echo $?
```

Expected: non-zero — `aiAutoMemoryPrincipals` does not exist.

- [ ] **Step 3: Add the schema**

```graphql
type AiAutoMemoryPrincipal {
    principalType: AiAutoMemoryPrincipalType!
    principalId: Long!
    label: String!
    memoryCount: Int!
}
```

and inside `extend type Query`:

```graphql
    """
    The owners that actually hold memory in this workspace and environment, for the Memories page's owner picker.
    Applies the same per-principal rules as the read operations: only the caller's own USER entry, every
    PROJECT_DEPLOYMENT entry, and never INTEGRATION_INSTANCE.
    """
    aiAutoMemoryPrincipals(workspaceId: ID!, environment: Int!): [AiAutoMemoryPrincipal!]!
```

- [ ] **Step 4: Implement the resolver**

Add `ProjectDeploymentService` as a constructor dependency (import `com.bytechef.automation.configuration.service.ProjectDeploymentService`), then:

```java
    @QueryMapping
    public List<AiAutoMemoryPrincipal> aiAutoMemoryPrincipals(@Argument long workspaceId, @Argument int environment) {
        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        List<AiAutoMemoryPrincipalCount> principalCounts =
            aiAutoMemoryService.listPrincipals(workspaceId, environment);

        List<ResolvedPrincipalCount> addressable = new ArrayList<>();

        for (AiAutoMemoryPrincipalCount principalCount : principalCounts) {
            // Reuses the read path's decision table so the picker can never offer an owner the reads refuse.
            ResolvedPrincipal resolved = resolvePrincipalForListing(principalCount, userId);

            if (resolved != null) {
                addressable.add(new ResolvedPrincipalCount(resolved, principalCount.memoryCount()));
            }
        }

        Map<Long, String> deploymentNames = resolveDeploymentNames(addressable);

        List<AiAutoMemoryPrincipal> principals = new ArrayList<>();

        for (ResolvedPrincipalCount resolvedCount : addressable) {
            ResolvedPrincipal resolved = resolvedCount.principal();

            principals.add(
                new AiAutoMemoryPrincipal(
                    resolved.principalType(), resolved.principalId(),
                    resolveLabel(resolved, userId, deploymentNames), resolvedCount.memoryCount()));
        }

        return principals;
    }

    /**
     * Resolves every deployment name in one call. A per-principal lookup inside the loop above would issue one query
     * per owner, which is the N+1 this exists to avoid.
     */
    private Map<Long, String> resolveDeploymentNames(List<ResolvedPrincipalCount> addressable) {
        List<Long> deploymentIds = addressable.stream()
            .map(ResolvedPrincipalCount::principal)
            .filter(principal -> principal.principalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT)
            .map(ResolvedPrincipal::principalId)
            .toList();

        if (deploymentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> namesById = new HashMap<>();

        for (ProjectDeployment projectDeployment : projectDeploymentService.getProjectDeployments(deploymentIds)) {
            namesById.put(projectDeployment.getId(), projectDeployment.getName());
        }

        return namesById;
    }

    /**
     * Pairs a principal the caller may address with how much memory it holds.
     */
    private record ResolvedPrincipalCount(ResolvedPrincipal principal, int memoryCount) {
    }

    /**
     * The listing variant of {@link #resolvePrincipal}: same rules, but a principal this caller may not address is
     * skipped rather than raised, because a catalogue legitimately contains entries the caller cannot open.
     */
    private @Nullable ResolvedPrincipal resolvePrincipalForListing(
        AiAutoMemoryPrincipalCount principalCount, long currentUserId) {

        AiAutoMemoryPrincipalType principalType = principalCount.principalType();

        if (principalType == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE) {
            return null;
        }

        return resolvePrincipal(principalType, principalCount.principalId(), currentUserId, false);
    }

    private static String resolveLabel(
        ResolvedPrincipal principal, long currentUserId, Map<Long, String> deploymentNames) {

        if (principal.principalType() == AiAutoMemoryPrincipalType.USER) {
            return principal.principalId() == currentUserId ? "My memories" : "Memories";
        }

        // A deployment deleted while its memory rows remain is absent from the batch lookup: label it by id rather
        // than dropping the entry, so the orphaned memory stays reachable for cleanup.
        return deploymentNames.getOrDefault(principal.principalId(), "Deployment " + principal.principalId());
    }
```

and the carrier next to `ResolvedPrincipal`:

```java
    /**
     * One selectable owner in the Memories picker.
     */
    public record AiAutoMemoryPrincipal(
        AiAutoMemoryPrincipalType principalType, long principalId, String label, int memoryCount) {
    }
```

Add imports `java.util.ArrayList`, `java.util.HashMap`, `java.util.Map`, and `com.bytechef.automation.configuration.domain.ProjectDeployment`.

`ProjectDeploymentService.getProjectDeployments(List<Long> ids)` is the batch accessor to use — it is declared at `ProjectDeploymentService.java:51`. Do not use the single-id `getProjectDeployment(long)` inside the loop; that is an N+1 query per owner. `ProjectDeployment.getId()` returns `Long`.

- [ ] **Step 5: Run the module suite**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:test \
  > /tmp/green6.log 2>&1
echo $?
grep -E "^> Task .* FAILED|Results:" /tmp/green6.log
```

Expected: exit 0, no FAILED tasks, `Results:` shows **19 tests**.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src
git commit -m "732 Expose the auto-memory principals a caller may address"
```

---

### Task 7: The Memories owner picker

**Files:**
- Create: `client/src/graphql/ai/auto-memory/aiAutoMemoryPrincipals.graphql`
- Modify: `client/src/pages/automation/ai/memories/Memories.tsx`
- Modify: `client/src/pages/automation/ai/memories/hooks/useAiAutoMemories.ts`
- Modify: `client/src/pages/automation/ai/memories/tests/Memories.test.tsx`

**Interfaces:**
- Consumes: the `aiAutoMemoryPrincipals` query (Task 6) and the principal arguments (Tasks 2-3).

- [ ] **Step 1: Add the operation and regenerate**

`aiAutoMemoryPrincipals.graphql`:

```graphql
query aiAutoMemoryPrincipals($workspaceId: ID!, $environment: Int!) {
    aiAutoMemoryPrincipals(workspaceId: $workspaceId, environment: $environment) {
        principalType
        principalId
        label
        memoryCount
    }
}
```

Then update the two list/detail operations to pass the principal through — add `$principalType: AiAutoMemoryPrincipalType` and `$principalId: Long` variables and forward them as arguments in `aiAutoMemories.graphql` and `aiAutoMemory.graphql`.

```bash
cd client
npx graphql-codegen
```

Expected: exit 0.

- [ ] **Step 2: Remove the vestigial embedding**

In `Memories.tsx`, delete the `renderSidebarNav` and `sidebarTitle` props from `MemoriesProps` and the component signature, and collapse the three conditionals that used them: the header-fallback Type filter block, the `leftSidebarBody` ternary (keep the `LeftSidebarNav` branch), and the `leftSidebarHeader` title ternary (keep `'Memories'`).

Delete the test at `Memories.test.tsx:188` that passes `renderSidebarNav` — it is the only caller and it pins behaviour that no longer exists.

- [ ] **Step 3: Run the client tests to confirm the removal is clean**

```bash
cd client
npx vitest run src/pages/automation/ai/memories
```

Expected: pass. A failure here means something other than that one test depended on the embedding — read it before deleting anything further.

- [ ] **Step 4: Add the Owner nav group**

In `Memories.tsx`, add state for the selected principal, call the new principals query, and render a second `LeftSidebarNav` titled `"Owner"` above the existing `"Type"` one inside `leftSidebarBody`. Items come from the query's `label`; the selected principal's `principalType`/`principalId` are passed into `useAiAutoMemoriesQuery`. Selecting nothing leaves both undefined, which the server resolves to the caller.

Keep object literals in natural ascending key order — `sort-keys` is **not** auto-fixable.

- [ ] **Step 5: Full client check**

```bash
cd client
npm run format
npm run check
```

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
git add client/src/graphql/ai/auto-memory client/src/pages/automation/ai/memories
git commit -m "732 client - Add an owner picker to the Memories sidebar"
git add client/src/shared/middleware/graphql-types.ts client/src/shared/middleware/graphql.ts
git commit -m "732 client - Regenerate GraphQL types for the memories owner picker"
```

---

### Task 8: Full verification

- [ ] **Step 1: Run the whole module's checks**

```bash
./gradlew spotlessApply
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql:check \
          :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-api:check \
          :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-service:check \
          :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-jdbc:check \
          :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:check \
          --continue > /tmp/final.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/final.log
```

Expected: exit 0, no FAILED tasks. If `check` reports `BUILD SUCCESSFUL` in a few seconds with no `Results:` line, Gradle considered the tests up to date — re-run the `test` task with `--rerun` to get real evidence.

- [ ] **Step 2: Whole-project compile**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/compile.log 2>&1
echo $?
grep -cE "^> Task .* FAILED" /tmp/compile.log
```

Expected: exit 0, count 0. This catches any other caller of the changed `UpdateAiAutoMemoryInput` constructor.

- [ ] **Step 3: Commit any formatting fallout**

```bash
git status --short
```

If `spotlessApply` changed files, commit them with `732 Format`.

---

## Out of scope

- Listing or mutating `INTEGRATION_INSTANCE` memories.
- A principal picker in the Memories UI. This exposes the capability through GraphQL only.
- Notifying anyone when an admin edits a deployment's memory.
