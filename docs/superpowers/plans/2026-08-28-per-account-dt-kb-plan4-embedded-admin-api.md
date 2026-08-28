# Embedded Admin API for Data Tables and Knowledge Bases Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a vendor admin see and assign per-account ownership of data tables and knowledge bases from the embedded console, and make table-level ownership actually govern what a connected user can reach.

**Architecture:** Two thin `-graphql` modules under `server/ee/libs/embedded/`, each an API facade gated `isTenantAdmin()` plus a controller over the existing platform service. No `-api`, no `-service`, no remote client: embedded ownership is a column on the platform entity, not a relation table, so there is nothing to maintain. Before that surface is worth having, table-level ownership has to mean something at runtime — Plan 2 scoped rows and knowledge bases but left `DataTable.ownerId` unread, so this plan closes that first.

**Tech Stack:** Java 25, Spring GraphQL (`@QueryMapping`/`@MutationMapping`), Spring Data JDBC, Spring Security (`@PreAuthorize`), JUnit 5, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-08-27-per-account-data-tables-knowledge-bases-design.md`

**Predecessors:** Plans 1–3, all landed on this branch. Plan 1 added the `owner_id`/`owner_type` columns, Plan 2 scoped rows and knowledge bases at runtime, Plan 3 added the query grammar.

## Global Constraints

- **The API facade owns authorization.** `isTenantAdmin()` goes on the facade, never on the controller and never on a shared facade. Per CLAUDE.md, wiring a controller to an unguarded shared facade compiles fine and silently removes the check.
- **EE files** carry the ByteChef Enterprise licence header and an `@version ee` Javadoc tag. Spotless picks the header by the `@version ee` *content*, not the path.
- **A refusal reads as "not found".** Naming another account's table must be indistinguishable from naming a table that does not exist, matching what `KnowledgeBaseServiceImpl.isReadableBy` already does. Otherwise ids are probeable.
- **Admin filtering is exact; runtime visibility is not.** The runtime rule is `owner = me OR owner IS NULL`. The admin's owner filter is exact equality — the admin is browsing *by* owner, not being restricted to one. These are two different questions and must not share a method.
- **Enum ordinals are persisted as INT**; `ownerType` stays a nullable `Integer` field with converting accessors. Never a mapped enum field.
- **Java style:** blank line before control statements, blank line after a variable modification that a later statement uses, descriptive names, no `TODO:` comments, no trailing blank line before a class's closing brace, test methods camelCase without underscores.
- **Commit prefix convention:** `---` opens a group, `-` continues it.

## File Structure

| File | Responsibility |
|---|---|
| `platform-data-table-api/.../service/DataTableService.java` (modify) | Owner-aware `listTables` and `getDataTableInfo`; `assignOwner`. |
| `platform-data-table-service/.../service/DataTableServiceImpl.java` (modify) | Implement them; one `isReadableBy` helper mirroring the knowledge base one. |
| `data-table/.../util/DataTableUtils.java` (modify) | The options lookup stops listing the whole tenant. |
| `platform-knowledge-base-api/.../service/KnowledgeBaseService.java` (modify) | `assignOwner`. |
| `embedded-data-table-graphql/` (create) | Facade + controller + schema for data tables. |
| `embedded-knowledge-base-graphql/` (create) | Same for knowledge bases. |
| `ConnectedUserResourceMembershipResolver.java` (modify) | `DataTable` / `KnowledgeBase` cases. |
| `settings.gradle.kts`, `server-app/build.gradle.kts` (modify) | Register the two modules. |

---

### Task 1: Make table-level ownership govern at runtime

Plan 1 added `DataTable.ownerId` and nothing has read it since. Until it does, the admin control this plan adds would be decoration.

**Files:**
- Modify: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/configuration/service/DataTableService.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/configuration/service/DataTableServiceImpl.java`
- Modify: `server/libs/modules/components/data-table/src/main/java/com/bytechef/component/datatable/util/DataTableUtils.java`
- Modify: `server/libs/modules/components/data-table/src/main/java/com/bytechef/component/datatable/action/*.java` (the callers of `getActionTableOptions`)
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/configuration/service/DataTableOwnershipTest.java`

**Interfaces:**
- Consumes: `Owner`, `OwnerResolution.resolve(...)`, `RowOwnerFilter` from Plans 1–2.
- Produces:
  - `List<DataTableInfo> DataTableService.listTables(long environmentId, Optional<Owner> owner)`
  - `boolean DataTableService.isReadableBy(long dataTableId, Optional<Owner> owner)` — used by the check below
  - `DataTableUtils.getActionTableOptions(DataTableService, ObjectProvider<OwnerResolver>)`

- [ ] **Step 1: Write the failing test**

The visibility rule is the same one the rows use, so the test names it that way.

```java
    @Test
    void testAnUnownedTableIsVisibleToEveryone() {
        assertTrue(DataTableServiceImpl.isReadableBy(unowned(), Optional.of(Owner.connectedUser(1L))));
    }

    @Test
    void testAnAdminWithNoOwnerSeesEveryTable() {
        assertTrue(DataTableServiceImpl.isReadableBy(ownedBy(2L), Optional.empty()));
    }

    @Test
    void testAnAccountSeesItsOwnTable() {
        assertTrue(DataTableServiceImpl.isReadableBy(ownedBy(1L), Optional.of(Owner.connectedUser(1L))));
    }

    @Test
    void testAnAccountDoesNotSeeAnotherAccountsTable() {
        assertFalse(DataTableServiceImpl.isReadableBy(ownedBy(2L), Optional.of(Owner.connectedUser(1L))));
    }
```

with helpers building a `DataTable` whose `ownerId`/`ownerType` are set or left null. Model the shape on `KnowledgeBaseServiceImpl`'s existing ownership test — read that file first and mirror its naming.

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests '*DataTableOwnershipTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E 'error:|^> Task .* FAILED' /tmp/t.log | head
```

Expected: compile failure — `isReadableBy` does not exist.

- [ ] **Step 3: Add the ownership predicate and the owner-aware listing**

In `DataTableServiceImpl`, mirroring `KnowledgeBaseServiceImpl.isReadableBy` exactly — including that an empty owner reads everything, which is what makes admin and automation callers work:

```java
    static boolean isReadableBy(DataTable dataTable, Optional<Owner> owner) {
        Long ownerId = dataTable.getOwnerId();

        if (ownerId == null || owner.isEmpty()) {
            return true;
        }

        Owner currentOwner = owner.get();

        return currentOwner.id() == ownerId && currentOwner.type() == dataTable.getOwnerType();
    }
```

Then the listing overload filters the registry rows through it before the physical-table lookup, so an invisible table costs no `information_schema` work.

- [ ] **Step 4: Narrow the component's table dropdown**

`DataTableUtils.getTableOptions` currently calls `dataTableService.listTables(DEVELOPMENT.ordinal())` and so offers every table in the tenant to every caller. Thread the resolved owner through, exactly as the row operations already do:

```java
    public static ActionDefinition.OptionsFunction<String> getActionTableOptions(
        DataTableService dataTableService, ObjectProvider<OwnerResolver> ownerResolverProvider) {

        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> getTableOptions(
            searchText, dataTableService, OwnerResolution.resolve((ActionContextAware) context, ownerResolverProvider));
    }
```

Update every caller of `getActionTableOptions` and `getTriggerTableOptions`. Check the trigger form separately: `TriggerContextAware` has its own `OwnerResolution` overload.

- [ ] **Step 5: Run the test to verify it passes, then the whole module**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test :server:libs:modules:components:data-table:test --continue > /tmp/t.log 2>&1; echo "exit=$?"; grep -cE '^> Task .* FAILED' /tmp/t.log
```

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply -q
git add server/libs/platform/platform-data-table server/libs/modules/components/data-table
git commit -m "--- Make a data table's owner govern who can see it"
```

---

### Task 2: Owner assignment on both services

**Files:**
- Modify: `platform-data-table-api/.../service/DataTableService.java`, `platform-data-table-service/.../service/DataTableServiceImpl.java`
- Modify: `platform-knowledge-base-api/.../service/KnowledgeBaseService.java`, and its `-service` impl
- Modify: `server/ee/libs/platform/platform-data-table/platform-data-table-remote-client/.../RemoteDataTableRowServiceClient.java` only if the row service changes — it should not; check whether a `DataTableService` remote client also exists and stub it if so
- Test: extend `DataTableOwnershipTest`, and the knowledge base service's existing ownership test

**Interfaces:**
- Produces:
  - `void DataTableService.assignOwner(long dataTableId, @Nullable Owner owner)`
  - `void KnowledgeBaseService.assignOwner(long knowledgeBaseId, @Nullable Owner owner)`

- [ ] **Step 1: Write the failing tests**

```java
    @Test
    void testAssigningAnOwnerStampsBothColumns() {
        // owner id and owner type are set together or not at all
    }

    @Test
    void testAssigningANullOwnerReturnsTheTableToTheVendor() {
        // both columns cleared, so the table becomes visible to every account again
    }
```

Assert on both columns in both directions. A half-written owner — id set, type null — would read as unowned through `isReadableBy` and silently share the table.

- [ ] **Step 2: Run to verify it fails**

- [ ] **Step 3: Implement on both services**

```java
    @Override
    public void assignOwner(long dataTableId, @Nullable Owner owner) {
        DataTable dataTable = dataTableRepository.findById(dataTableId)
            .orElseThrow(() -> new IllegalArgumentException("Data table not found: " + dataTableId));

        dataTable.setOwnerId(owner == null ? null : owner.id());
        dataTable.setOwnerType(owner == null ? null : owner.type());

        dataTableRepository.save(dataTable);
    }
```

Note the blank line before `dataTableRepository.save` — the repo's "blank line after variable modification" rule.

- [ ] **Step 4: Run the tests, format, commit**

```bash
git commit -m "- Assign a data table or knowledge base to an account"
```

---

### Task 3: The embedded data table admin API

**Files:**
- Create: `server/ee/libs/embedded/embedded-data-table-graphql/build.gradle.kts`
- Create: `.../src/main/java/com/bytechef/ee/embedded/data/table/facade/EmbeddedDataTableApiFacade.java` (+ `Impl`)
- Create: `.../src/main/java/com/bytechef/ee/embedded/data/table/web/graphql/EmbeddedDataTableGraphQlController.java`
- Create: `.../src/main/resources/graphql/embedded-data-table.graphqls`
- Modify: `settings.gradle.kts`, `server/apps/server-app/build.gradle.kts`
- Test: `.../src/test/java/.../EmbeddedDataTableApiFacadeTest.java`

**Interfaces:**
- Consumes: `DataTableService.listTables(environmentId, Optional<Owner>)` and `assignOwner` from Tasks 1–2; `ConnectedUserService` for resolving an owner id to a connected user.
- Produces: GraphQL `dataTables(environmentId, ownerId)` and `assignDataTableOwner(input)`.

- [ ] **Step 1: Create the module and register it**

`build.gradle.kts` modelled on `embedded-connected-user-graphql` — read that file and mirror its dependency shape rather than inventing one. Add the module to `settings.gradle.kts` and as an `implementation(project(...))` in `server-app`.

- [ ] **Step 2: Write the failing facade test**

The one behaviour worth pinning is that the gate is on the facade:

```java
    @Test
    void testTheFacadeIsAnnotatedForTenantAdmin() throws Exception {
        Method method = EmbeddedDataTableApiFacadeImpl.class.getMethod("getDataTables", Long.class, Long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("isTenantAdmin()", preAuthorize.value());
    }
```

That reads as a tautology test but is not: the failure it catches is a later refactor moving the annotation to the controller, where CLAUDE.md records that it silently stops applying. Add the same assertion for the mutation.

- [ ] **Step 3: Write the facade**

```java
    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<DataTableInfo> getDataTables(Long environmentId, @Nullable Long ownerId) {
        return dataTableService.listTables(
            environmentId, ownerId == null ? Optional.empty() : Optional.of(Owner.connectedUser(ownerId)));
    }
```

Careful: `listTables(environmentId, Optional.empty())` means "admin sees everything", which is what a null `ownerId` should do here. Filtering to *exactly* one owner is a different query from the runtime visibility rule — if the admin console needs "owned by 42 and nothing else", add a distinct method rather than overloading the visibility one. Decide this before writing the controller and write down which you chose.

- [ ] **Step 4: Write the controller and schema**

Model the controller on `DataTableGraphQlController` in `automation-data-table-graphql`, including its `@ConditionalOnCoordinator` and its input/output records. The schema file goes under `src/main/resources/graphql/`; check how the automation module's `data-table.graphqls` is picked up and match it.

- [ ] **Step 5: Run the module's tests, format, commit**

```bash
git commit -m "- Add the embedded data table admin API"
```

---

### Task 4: The embedded knowledge base admin API

Identical shape to Task 3, one store over. Repeat the steps rather than referring back to them — the two modules are read separately.

**Files:**
- Create: `server/ee/libs/embedded/embedded-knowledge-base-graphql/` with the same four files
- Modify: `settings.gradle.kts`, `server/apps/server-app/build.gradle.kts`
- Test: `.../EmbeddedKnowledgeBaseApiFacadeTest.java`

**Interfaces:**
- Consumes: `KnowledgeBaseService.getKnowledgeBases(environment, Optional<Owner>)` — which already exists from Plan 2 — and `assignOwner` from Task 2.
- Produces: GraphQL `knowledgeBases(environmentId, ownerId)` and `assignKnowledgeBaseOwner(input)`.

- [ ] **Step 1: Create the module and register it**
- [ ] **Step 2: Write the failing facade test** — the same `@PreAuthorize` assertions as Task 3, Step 2
- [ ] **Step 3: Write the facade**, gated `isTenantAdmin()` on every method
- [ ] **Step 4: Write the controller and schema**, modelled on `automation-knowledge-base-graphql`
- [ ] **Step 5: Run, format, commit**

```bash
git commit -m "- Add the embedded knowledge base admin API"
```

---

### Task 5: Connected-user resource cases

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/ConnectedUserResourceMembershipResolver.java`
- Test: its existing test class

**Interfaces:**
- Consumes: `DataTableService.isReadableBy` and the knowledge base equivalent from Tasks 1–2.

- [ ] **Step 1: Read the resolver's contract first**

Its class Javadoc is long and load-bearing — it enumerates the six `ResourceMembershipDecider` consumers and explains why the resolver fails closed in one method and not the other. Read it before adding a case. In particular, note that the `switch` currently ends `default -> Decision.NOT_APPLICABLE`, which **delegates** rather than denying; check what `ResourceMembershipDecider` does with `NOT_APPLICABLE` before assuming a new resource type is denied by default. The spec asserts "denied by default until added" — verify that claim rather than inheriting it, and if it is wrong, say so in the commit message.

- [ ] **Step 2: Write the failing tests**

One granting case and one denying case per resource type, plus a case for a caller who is not a connected user at all (`NOT_APPLICABLE`).

- [ ] **Step 3: Add the cases**

```java
            case DATA_TABLE -> resolveDataTable(id, owner);
            case KNOWLEDGE_BASE -> resolveKnowledgeBase(id, owner);
```

with constants alongside the existing `PROJECT`/`WORKFLOW` ones.

- [ ] **Step 4: Run, format, commit**

```bash
git commit -m "- Answer data table and knowledge base membership for a connected user"
```

---

## Deliberately not in this plan

- **No admin UI.** That is Plan 5, together with moving the automation presentation components under `shared/components/` and threading the scope union through them.
- **No component palette change.** The one-line `IntegrationComponentDefinitionFilter.COMPONENT_NAMES` addition also belongs to Plan 5, since it is a UI-facing decision and the demo's integration workflows have to migrate to the bridge first.
- **No row-level owner assignment from the console.** The admin assigns a table or a knowledge base to an account; rows take their owner from whoever writes them at runtime.
- **No connected-user-facing API or SDK surface**, per the spec: the audience is the vendor admin.
