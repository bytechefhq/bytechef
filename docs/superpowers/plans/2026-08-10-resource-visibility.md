# Resource Visibility & Grants — Implementation Plan (Phase 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the connection-specific visibility model with a resource-agnostic one — a shared
`ResourceVisibility` enum plus `resource_grant` rows for named users — wiring `Connection` as the only
resource in phase 1.

**Architecture:** A `visibility` column carries a resource's reach (`WORKSPACE` by default, forced in CE,
`PRIVATE` forced for embedded). Grant rows name individual users who may see a resource its owner has
withheld. A `ResourceVisibilityResolver` SPI filters candidate lists; `PermissionService.hasResourceScope`
gains visibility as a precondition so list filtering and by-id authorization agree.

**Tech Stack:** Java 21, Spring Boot, Spring Data JDBC (not JPA), Liquibase, GraphQL (Spring for GraphQL),
JUnit 5 + AssertJ + Mockito, React + TypeScript + Vitest + TanStack Query.

**Spec:** `docs/superpowers/specs/2026-08-10-resource-visibility-design.md`

## Global Constraints

- **Branch:** `worktree-resource-visibility`, based on `0_732` at `38c1039591f`.
- **Commit convention** (`CLAUDE.md:1159`): server `732 <description>`; client `732 client - <description>`.
- **Stage only files touched by the current task.** Never sweep in unrelated working-tree changes.
- **Never judge a Gradle run piped into `tail`/`grep`.** Redirect to a file, capture the status
  immediately, then grep the file for `^> Task .* FAILED`. Use `--continue`.
- **Capture the exit status into a variable on the same line as the command**, never in a later shell:

  ```bash
  ./gradlew check --continue > /tmp/x.log 2>&1; STATUS=$?
  echo "gradle exit=$STATUS"
  grep '^> Task .* FAILED' /tmp/x.log || echo "no failed tasks"
  ```

  A bare `echo "exit=$?"` issued as a *separate* command reports the previous echo's status, not
  Gradle's — two failed suites have looked green that way before.
- **`--tests '*SomeTest'` is for the red/green loop only. Every commit gate runs `./gradlew check`.**
  A module `test` task skips static analysis and integration tests; using it as the gate is how ~30
  commits once shipped with both silently failing. Where a task below shows `spotlessApply` before
  `git commit`, run the full sequence:

  ```bash
  ./gradlew spotlessApply
  ./gradlew check --continue > /tmp/gate.log 2>&1; STATUS=$?
  echo "gradle exit=$STATUS"
  grep '^> Task .* FAILED' /tmp/gate.log || echo "no failed tasks"
  ```

  Do not commit unless `STATUS` is 0 **and** no failed tasks are listed. Both conditions — the status
  alone has been wrong before.
- **Before committing client code:** `cd client && npm run format && npm run check`.
- **EE classes carry the `@version ee` Javadoc tag** — Spotless uses it to select the Enterprise licence
  header. Omitting it fails the build.
- **Integration tests end in `IntTest`**; unit tests end in `Test`.
- **Spring Data JDBC**, so no `@Entity`/`@ManyToOne`. Use `@Table`, `@Id`, `AggregateReference`.
- `hasResourceScope` signature is `(Serializable id, String resourceType, String scope)` — **id first**.

## Hard ordering constraint

`DefaultConnectionVisibilityResolver` currently filters `default -> false`: anything that is not `PRIVATE`
is dropped from the CE list. The moment CE writes `WORKSPACE`, every CE connection disappears from the
connections page.

**Task 5 therefore flips the CE default and replaces the CE resolver in a single commit.** Splitting them
leaves CE with an empty connections list at the intermediate commit. Do not reorder.

## File structure

| File | Responsibility |
| --- | --- |
| `platform-api/.../security/domain/ResourceVisibility.java` | the three-rung enum |
| `platform-api/.../security/domain/ResourceVisibilityPolicy.java` | per-module SPI: supported rungs |
| `platform-api/.../security/domain/ResourceVisibilityPolicyRegistry.java` | aggregates providers, validates uniqueness |
| `automation-configuration-api/.../service/ResourceVisibilityResolver.java` | filter SPI + `VisibilityRecord` |
| `automation-configuration-service/.../service/DefaultResourceVisibilityResolver.java` | CE: admin / reach / owner |
| `ee/.../automation-configuration-service/.../service/ResourceVisibilityResolverImpl.java` | EE: adds grants |
| `ee/libs/platform/platform-resource-grant/*` | grant entity, repository, service, changelog |
| `client/.../connection/ConnectionVisibilityPicker.tsx` | the three-state picker |

---

### Task 1: `ResourceVisibility` enum and policy registry

Creates the shared vocabulary. No behaviour changes yet — nothing consumes it until Task 2.

**Files:**
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/domain/ResourceVisibility.java`
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/domain/ResourceVisibilityPolicy.java`
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/domain/ResourceVisibilityPolicyRegistry.java`
- Test: `server/libs/platform/platform-api/src/test/java/com/bytechef/platform/security/domain/ResourceVisibilityPolicyRegistryTest.java`

**Interfaces:**
- Produces: `ResourceVisibility.{PRIVATE,WORKSPACE,ORGANIZATION}`; `ResourceVisibility.isAtLeast(ResourceVisibility)`;
  `ResourceVisibilityPolicy.resourceType()`, `.supportedVisibilities()`, `.defaultVisibility()`;
  `ResourceVisibilityPolicyRegistry.supports(String, ResourceVisibility)`, `.defaultVisibility(String)`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.security.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceVisibilityPolicyRegistryTest {

    private static ResourceVisibilityPolicy policy(
        String resourceType, ResourceVisibility defaultVisibility, Set<ResourceVisibility> supported) {

        return new ResourceVisibilityPolicy() {

            @Override
            public String resourceType() {
                return resourceType;
            }

            @Override
            public ResourceVisibility defaultVisibility() {
                return defaultVisibility;
            }

            @Override
            public Set<ResourceVisibility> supportedVisibilities() {
                return supported;
            }
        };
    }

    @Test
    void testSupportsReturnsTrueForDeclaredRung() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(
            List.of(policy("Connection", ResourceVisibility.WORKSPACE, Set.of(
                ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE, ResourceVisibility.ORGANIZATION))));

        assertThat(registry.supports("Connection", ResourceVisibility.ORGANIZATION)).isTrue();
    }

    @Test
    void testSupportsReturnsFalseForUndeclaredRung() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(
            List.of(policy("Project", ResourceVisibility.WORKSPACE, Set.of(
                ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE))));

        assertThat(registry.supports("Project", ResourceVisibility.ORGANIZATION)).isFalse();
    }

    @Test
    void testSupportsFailsClosedForUnregisteredResourceType() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(List.of());

        assertThat(registry.supports("Unknown", ResourceVisibility.WORKSPACE)).isFalse();
    }

    @Test
    void testDefaultVisibilityIsReturnedPerResourceType() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(
            List.of(policy("Connection", ResourceVisibility.WORKSPACE, Set.of(ResourceVisibility.WORKSPACE))));

        assertThat(registry.defaultVisibility("Connection")).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testDuplicateResourceTypeIsRegistrationError() {
        List<ResourceVisibilityPolicy> policies = List.of(
            policy("Connection", ResourceVisibility.WORKSPACE, Set.of(ResourceVisibility.WORKSPACE)),
            policy("Connection", ResourceVisibility.PRIVATE, Set.of(ResourceVisibility.PRIVATE)));

        assertThatThrownBy(() -> new ResourceVisibilityPolicyRegistry(policies))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Connection");
    }

    @Test
    void testDefaultMustBeInSupportedSet() {
        List<ResourceVisibilityPolicy> policies = List.of(
            policy("Connection", ResourceVisibility.ORGANIZATION, Set.of(ResourceVisibility.PRIVATE)));

        assertThatThrownBy(() -> new ResourceVisibilityPolicyRegistry(policies))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("default");
    }

    @Test
    void testIsAtLeastOrdersTheRungs() {
        assertThat(ResourceVisibility.ORGANIZATION.isAtLeast(ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(ResourceVisibility.WORKSPACE.isAtLeast(ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(ResourceVisibility.PRIVATE.isAtLeast(ResourceVisibility.WORKSPACE)).isFalse();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-api:test --tests '*ResourceVisibilityPolicyRegistryTest' > /tmp/t1.log 2>&1; echo "exit=$?"`
Expected: FAIL — compilation error, `ResourceVisibility` does not exist.

- [ ] **Step 3: Create `ResourceVisibility`**

Copy the Apache licence header from `ConnectionVisibility.java` (lines 1–16) verbatim.

```java
package com.bytechef.platform.security.domain;

import java.util.Objects;

/**
 * How far a resource reaches. Ordered: PRIVATE &lt; WORKSPACE &lt; ORGANIZATION. Which rungs a given resource type
 * actually supports is declared per module through {@link ResourceVisibilityPolicy}; this enum is only the vocabulary.
 *
 * <p>
 * Persisted as an INT ordinal by Spring Data JDBC. Appending a value is safe; reordering is not.
 *
 * @author Ivica Cardic
 */
public enum ResourceVisibility {

    PRIVATE,        // the owner, plus any users named in a resource grant
    WORKSPACE,      // every member of the owning workspace — the default for every resource type
    ORGANIZATION;   // every member of every workspace in the organization

    /**
     * Returns true when this visibility reaches at least as far as {@code other}.
     */
    public boolean isAtLeast(ResourceVisibility other) {
        Objects.requireNonNull(other, "other");

        return ordinal() >= other.ordinal();
    }
}
```

- [ ] **Step 4: Create `ResourceVisibilityPolicy`**

```java
package com.bytechef.platform.security.domain;

import java.util.Set;

/**
 * SPI contributed once per resource family to declare which {@link ResourceVisibility} rungs the resource supports and
 * which one it is created with. Mirrors the {@code ResourceOwnershipResolver} / {@code PermissionScopeProvider}
 * per-module SPI shape; aggregated by {@link ResourceVisibilityPolicyRegistry}.
 *
 * @author Ivica Cardic
 */
public interface ResourceVisibilityPolicy {

    /**
     * Resource-type discriminator, matching {@code ResourceOwnershipResolver.resourceType()} — e.g. {@code
     * "Connection"}. Must be unique across all registered policies.
     */
    String resourceType();

    /**
     * The rung a resource of this type is created with. Must be a member of {@link #supportedVisibilities()}.
     */
    ResourceVisibility defaultVisibility();

    /**
     * Every rung this resource type may legally hold. A value outside this set is rejected server-side.
     */
    Set<ResourceVisibility> supportedVisibilities();
}
```

- [ ] **Step 5: Create `ResourceVisibilityPolicyRegistry`**

```java
package com.bytechef.platform.security.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates every {@link ResourceVisibilityPolicy} on the classpath into one lookup, validating at construction that
 * resource types are unique and that each declared default is itself supported. An unregistered resource type fails
 * closed: {@link #supports} returns false rather than defaulting to permissive.
 *
 * @author Ivica Cardic
 */
public class ResourceVisibilityPolicyRegistry {

    private final Map<String, ResourceVisibilityPolicy> policies = new HashMap<>();

    public ResourceVisibilityPolicyRegistry(List<ResourceVisibilityPolicy> resourceVisibilityPolicies) {
        for (ResourceVisibilityPolicy policy : resourceVisibilityPolicies) {
            String resourceType = policy.resourceType();

            ResourceVisibilityPolicy previous = policies.put(resourceType, policy);

            if (previous != null) {
                throw new IllegalStateException(
                    "Duplicate ResourceVisibilityPolicy for resource type '" + resourceType + "'");
            }

            if (!policy.supportedVisibilities()
                .contains(policy.defaultVisibility())) {

                throw new IllegalStateException(
                    "ResourceVisibilityPolicy for '" + resourceType + "' declares default " +
                        policy.defaultVisibility() + " which is not in its supported set");
            }
        }
    }

    /**
     * Returns whether {@code visibility} is legal for {@code resourceType}. Unregistered types return false.
     */
    public boolean supports(String resourceType, ResourceVisibility visibility) {
        ResourceVisibilityPolicy policy = policies.get(resourceType);

        return policy != null && policy.supportedVisibilities()
            .contains(visibility);
    }

    /**
     * Returns the creation default for {@code resourceType}.
     *
     * @throws IllegalArgumentException when the resource type has no registered policy
     */
    public ResourceVisibility defaultVisibility(String resourceType) {
        ResourceVisibilityPolicy policy = policies.get(resourceType);

        if (policy == null) {
            throw new IllegalArgumentException("No ResourceVisibilityPolicy registered for '" + resourceType + "'");
        }

        return policy.defaultVisibility();
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-api:test --tests '*ResourceVisibilityPolicyRegistryTest' > /tmp/t1.log 2>&1; echo "exit=$?"`
Expected: exit=0, 7 tests pass.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/domain/ \
        server/libs/platform/platform-api/src/test/java/com/bytechef/platform/security/domain/
git commit -m "732 Add ResourceVisibility enum and per-resource visibility policy registry"
```

---

### Task 2: Rename `ConnectionVisibility` to `ResourceVisibility`

Purely mechanical. No behaviour change — every call site keeps its current semantics. This isolates a
large, boring diff from the behavioural commits that follow, so their reviews stay readable.

**Files:**
- Delete: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/ConnectionVisibility.java`
- Delete: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/domain/ConnectionVisibilityTest.java`
- Modify: ~40 files referencing the symbol (enumerated by the command in Step 2)

**Interfaces:**
- Consumes: `ResourceVisibility` from Task 1.
- Produces: every connection call site now typed `ResourceVisibility`.

- [ ] **Step 1: Delete the old enum and its stability test**

```bash
git rm server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/ConnectionVisibility.java \
       server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/domain/ConnectionVisibilityTest.java
```

The stability test is deleted rather than ported: it pinned `getCode()`/`ordinal()` against persisted rows,
and `ResourceVisibility` has no `getCode()` because nothing has shipped. Task 1's `testIsAtLeastOrdersTheRungs`
covers the ordering that remains meaningful.

- [ ] **Step 2: Rewrite every reference**

```bash
files=$(grep -rl "ConnectionVisibility" server --include='*.java' --include='*.proto' \
  --exclude-dir=build --exclude-dir=bin)
echo "$files" | wc -l    # expect ~40
for f in $files; do
  perl -pi -e 's/\bConnectionVisibility\b/ResourceVisibility/g' "$f"
  perl -pi -e 's{^import com\.bytechef\.platform\.connection\.domain\.ResourceVisibility;}{import com.bytechef.platform.security.domain.ResourceVisibility;}' "$f"
done
```

The second `perl` fixes imports the first one rewrote to a package that no longer holds the type. Files in
`com.bytechef.platform.connection.domain` itself (`Connection.java`, `ConnectionStatus.java`) had no import
to rewrite and now need one added — Step 4's compile will name them.

- [ ] **Step 3: Add the missing import where the compiler asks**

Run the compile, then add `import com.bytechef.platform.security.domain.ResourceVisibility;` to each file
reported as "cannot find symbol: class ResourceVisibility".

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t2.log 2>&1
echo "exit=$?"
grep "cannot find symbol" -B2 /tmp/t2.log | grep '\.java'
```

- [ ] **Step 4: Verify the whole tree compiles**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t2.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/t2.log || echo "no failed tasks"
```

Expected: exit=0, no failed tasks.

- [ ] **Step 5: Verify existing connection tests still pass**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-service:test \
          :server:libs:automation:automation-configuration:automation-configuration-service:test \
          --continue > /tmp/t2b.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/t2b.log || echo "no failed tasks"
```

Expected: exit=0. A rename must not change a single assertion.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add -A server
git commit -m "732 Rename ConnectionVisibility to the shared ResourceVisibility type"
```

---

### Task 3: Delete the promote/demote surface (server)

Removes the affordances built for one-way promotion. The picker in Task 10 replaces them.

**Files:**
- Delete: `server/ee/.../automation-configuration-graphql/.../web/graphql/ConnectionVisibilityGraphQlController.java`
- Delete: `server/ee/.../automation-configuration-graphql/src/main/resources/graphql/connection-visibility.graphqls`
- Delete: `server/ee/.../automation-configuration-api/.../dto/BulkPromoteResultDTO.java`
- Modify: `server/ee/.../automation-configuration-api/.../facade/WorkspaceConnectionFacade.java` — drop the three promote/demote methods
- Modify: `server/ee/.../automation-configuration-service/.../facade/WorkspaceConnectionFacadeImpl.java` — drop their implementations
- Modify: `server/ee/.../automation-configuration-remote-client/.../facade/RemoteWorkspaceConnectionFacadeClient.java` — drop the mirrored methods
- Modify: `server/ee/.../automation-configuration-service/src/test/.../facade/WorkspaceConnectionFacadeImplTest.java` — drop the promote/demote tests

- [ ] **Step 1: Enumerate exactly what references the promote/demote API**

```bash
grep -rn "promoteConnectionToWorkspace\|demoteConnectionToPrivate\|promoteAllPrivateConnectionsToWorkspace\|BulkPromoteResultDTO" \
  server --include='*.java' --include='*.graphqls' --exclude-dir=build --exclude-dir=bin
```

Record the list. Every hit is either deleted or has the calling method deleted; none survive.

- [ ] **Step 2: Delete the files**

```bash
git rm server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/ConnectionVisibilityGraphQlController.java \
       server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/connection-visibility.graphqls \
       server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/dto/BulkPromoteResultDTO.java
```

- [ ] **Step 3: Remove the methods from the facade interface, impl, remote client and tests**

Delete each method whose name appears in Step 1's output, together with its `@PreAuthorize`, its Javadoc,
and any now-unused imports. Do not leave a deprecated stub — the branch has no external consumers.

- [ ] **Step 4: Verify compilation and tests**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t3.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/t3.log || echo "no failed tasks"
grep -rn "promoteConnectionToWorkspace\|demoteConnectionToPrivate\|BulkPromoteResultDTO" server --include='*.java' --exclude-dir=build || echo "no references remain"
```

Expected: exit=0; "no references remain".

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add -A server
git commit -m "732 Remove the connection promote and demote visibility surface"
```

---

### Task 4: Replace the visibility changeset

**Files:**
- Delete: `server/libs/platform/platform-connection/platform-connection-service/src/main/resources/config/liquibase/changelog/platform/connection/20260407000001_platform_connection_added_column_visibility.xml`
- Create: `server/libs/platform/platform-connection/platform-connection-service/src/main/resources/config/liquibase/changelog/platform/connection/20260810000001_platform_connection_visibility.xml`

The changelog directory loads via `includeAll`, so no parent file references the deleted changeset and
nothing needs unpicking. `includeAll` orders by filename, so `20260407000002_..._add_column_status.xml`
continues to apply normally.

- [ ] **Step 1: Read the existing changeset to copy its author and formatting**

```bash
cat server/libs/platform/platform-connection/platform-connection-service/src/main/resources/config/liquibase/changelog/platform/connection/20260407000001_platform_connection_added_column_visibility.xml
```

- [ ] **Step 2: Delete it**

```bash
git rm server/libs/platform/platform-connection/platform-connection-service/src/main/resources/config/liquibase/changelog/platform/connection/20260407000001_platform_connection_added_column_visibility.xml
```

- [ ] **Step 3: Create the replacement with the new default**

Match the header and namespace of the file read in Step 1 exactly; only the changeset differs.

```xml
<changeSet id="20260810000001" author="Ivica Cardic">
    <addColumn tableName="connection">
        <column name="visibility" type="INT" defaultValueNumeric="1">
            <constraints nullable="false"/>
        </column>
    </addColumn>
</changeSet>
```

`1` is `ResourceVisibility.WORKSPACE.ordinal()`.

- [ ] **Step 4: Verify a fresh database migrates**

Drop the local dev schema (or the `connection.visibility` column) first — Liquibase will not re-add an
existing column, and every `0_732` developer database already has it.

```bash
./gradlew :server:apps:server-app:bootRun > /tmp/t4.log 2>&1 &
sleep 90
grep -i "ChangeSet.*20260810000001.*ran successfully\|Liquibase.*Update.*successful" /tmp/t4.log
kill %1
```

Expected: the changeset applies. Then confirm the default:

```sql
select column_default, is_nullable from information_schema.columns
 where table_name = 'connection' and column_name = 'visibility';
-- expect: 1, NO
```

- [ ] **Step 5: Commit**

```bash
git add -A server/libs/platform/platform-connection/platform-connection-service/src/main/resources
git commit -m "732 Recreate the connection visibility column defaulting to WORKSPACE"
```

---

### Task 5: CE default flip and the CE resolver — one commit

**This task must not be split.** `DefaultConnectionVisibilityResolver` drops everything that is not
`PRIVATE`; flipping the CE default without replacing it empties the CE connections list.

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ResourceVisibilityResolver.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/DefaultResourceVisibilityResolver.java`
- Create: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/ConnectionVisibilityPolicy.java`
- Delete: `.../automation-configuration-api/.../service/ConnectionVisibilityResolver.java`
- Delete: `.../automation-configuration-service/.../service/DefaultConnectionVisibilityResolver.java` and its test
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacadeImpl.java:118-131`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java:247-266`
- Test: `.../automation-configuration-service/src/test/.../service/DefaultResourceVisibilityResolverTest.java`
- Test: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/facade/ConnectionFacadeTest.java`

**Interfaces:**
- Consumes: `ResourceVisibility`, `ResourceVisibilityPolicyRegistry` (Task 1).
- Produces: `ResourceVisibilityResolver.filterVisibleIds(String resourceType, long workspaceId, Collection<VisibilityRecord>) -> Set<Long>`;
  `ResourceVisibilityResolver.VisibilityRecord(long id, ResourceVisibility visibility, String createdBy)`.

- [ ] **Step 1: Write the failing resolver test**

```java
package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class DefaultResourceVisibilityResolverTest {

    private final DefaultResourceVisibilityResolver resolver = new DefaultResourceVisibilityResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                login, "password", List.of(authorities)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList()));
    }

    @Test
    void testWorkspaceVisibleToEveryMember() {
        authenticate("ana");

        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.WORKSPACE, "ivica")));

        assertThat(visible).containsExactly(10L);
    }

    @Test
    void testOrganizationVisibleToEveryMember() {
        authenticate("ana");

        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.ORGANIZATION, "ivica")));

        assertThat(visible).containsExactly(10L);
    }

    @Test
    void testPrivateHiddenFromNonOwner() {
        authenticate("ana");

        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visible).isEmpty();
    }

    @Test
    void testPrivateVisibleToOwner() {
        authenticate("ivica");

        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visible).containsExactly(10L);
    }

    @Test
    void testPrivateVisibleToAdmin() {
        authenticate("marko", AuthorityConstants.ADMIN);

        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visible).containsExactly(10L);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*DefaultResourceVisibilityResolverTest' > /tmp/t5.log 2>&1; echo "exit=$?"`
Expected: FAIL — `ResourceVisibilityResolver` does not exist.

- [ ] **Step 3: Create the SPI**

```java
package com.bytechef.automation.configuration.service;

import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Collection;
import java.util.Set;

/**
 * Narrows an already workspace-scoped candidate set to the resources the current principal may see. CE resolves admin
 * bypass, reach and ownership; EE additionally consults resource grants.
 *
 * <p>
 * Returns ids rather than filtered objects so one implementation serves every resource type without generics, and so a
 * future SQL-predicate implementation can replace the body without moving callers.
 *
 * @author Ivica Cardic
 */
public interface ResourceVisibilityResolver {

    /**
     * @param resourceType the {@code ResourceVisibilityPolicy} / {@code ResourceOwnershipResolver} discriminator
     * @param workspaceId  the workspace whose resources are being listed
     * @param candidates   the workspace-scoped candidate set; never {@code null}
     * @return the ids of the candidates visible to the current principal. Never {@code null}; may be empty.
     */
    Set<Long> filterVisibleIds(String resourceType, long workspaceId, Collection<VisibilityRecord> candidates);

    /**
     * The three facts visibility resolution needs from any resource, decoupling the resolver from every DTO type.
     */
    record VisibilityRecord(long id, ResourceVisibility visibility, String createdBy) {
    }
}
```

- [ ] **Step 4: Create the CE implementation**

```java
package com.bytechef.automation.configuration.service;

import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * CE resolution: admin bypass, then reach, then ownership. CE writes WORKSPACE for every connection it creates, so in
 * practice every candidate passes on the reach branch; the PRIVATE branch exists for rows written before the flip and
 * for embedded rows that should never surface here anyway.
 *
 * <p>
 * CE has no grants — {@code resource_grant} is an EE artefact — so step 4 of the spec's resolution order is absent.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnCEVersion
public class DefaultResourceVisibilityResolver implements ResourceVisibilityResolver {

    @Override
    public Set<Long> filterVisibleIds(
        String resourceType, long workspaceId, Collection<VisibilityRecord> candidates) {

        boolean admin = SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
        String currentUserLogin = SecurityUtils.getCurrentUserLogin();

        Set<Long> visibleIds = new LinkedHashSet<>();

        for (VisibilityRecord candidate : candidates) {
            if (admin || candidate.visibility()
                .isAtLeast(ResourceVisibility.WORKSPACE) ||
                Objects.equals(currentUserLogin, candidate.createdBy())) {

                visibleIds.add(candidate.id());
            }
        }

        return visibleIds;
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*DefaultResourceVisibilityResolverTest' > /tmp/t5.log 2>&1; echo "exit=$?"`
Expected: exit=0, 5 tests pass.

- [ ] **Step 6: Create the connection visibility policy**

```java
package com.bytechef.platform.connection;

import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Connections support every rung: a credential legitimately serves one person, one workspace, or the whole
 * organization. Created WORKSPACE — see the design spec's risk 1 for why the default is shared rather than private.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionVisibilityPolicy implements ResourceVisibilityPolicy {

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public ResourceVisibility defaultVisibility() {
        return ResourceVisibility.WORKSPACE;
    }

    @Override
    public Set<ResourceVisibility> supportedVisibilities() {
        return Set.of(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE, ResourceVisibility.ORGANIZATION);
    }
}
```

- [ ] **Step 7: Write the failing facade test for the CE flip**

Add to `ConnectionFacadeTest`:

```java
@Test
void testCeForcesWorkspaceVisibility() {
    ConnectionFacadeImpl facade = connectionFacade("CE");

    ConnectionDTO connectionDTO = connectionDTO(ResourceVisibility.PRIVATE);

    facade.create(connectionDTO, PlatformType.AUTOMATION);

    assertThat(capturedConnection().getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
}

@Test
void testEmbeddedForcesPrivateVisibilityInEe() {
    ConnectionFacadeImpl facade = connectionFacade("EE");

    ConnectionDTO connectionDTO = connectionDTO(ResourceVisibility.WORKSPACE);

    facade.create(connectionDTO, PlatformType.EMBEDDED);

    assertThat(capturedConnection().getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
}

@Test
void testEeAutomationHonoursRequestedVisibility() {
    ConnectionFacadeImpl facade = connectionFacade("EE");

    ConnectionDTO connectionDTO = connectionDTO(ResourceVisibility.PRIVATE);

    facade.create(connectionDTO, PlatformType.AUTOMATION);

    assertThat(capturedConnection().getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
}
```

Reuse the existing fixtures in `ConnectionFacadeTest` for `connectionFacade(String edition)`,
`connectionDTO(...)` and `capturedConnection()`; add them following the file's existing style if absent.

- [ ] **Step 8: Run it to verify `testCeForcesWorkspaceVisibility` fails**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests '*ConnectionFacadeTest' > /tmp/t5b.log 2>&1; echo "exit=$?"`
Expected: FAIL — CE still writes `PRIVATE`.

- [ ] **Step 9: Flip the CE default in `ConnectionFacadeImpl`**

Replace lines 118–131 with:

```java
// Embedded connections belong to a connected user, not a workspace member, so workspace reach is meaningless
// there and PRIVATE is the only value that cannot widen anything if some query reaches the row by another
// path. CE has no authorization boundary between workspace members and no picker, so it writes WORKSPACE
// unconditionally. Both overrides ignore the request body — the UI gates these surfaces too, but a facade-level
// override is what actually stops a hand-crafted request.
if (type == PlatformType.EMBEDDED) {
    logVisibilityOverride(connection.getVisibility(), ResourceVisibility.PRIVATE, type);

    connection.setVisibility(ResourceVisibility.PRIVATE);
} else if (!eeEdition) {
    logVisibilityOverride(connection.getVisibility(), ResourceVisibility.WORKSPACE, type);

    connection.setVisibility(ResourceVisibility.WORKSPACE);
}
```

and add the helper beside it:

```java
private void logVisibilityOverride(ResourceVisibility requested, ResourceVisibility forced, PlatformType type) {
    if (requested != forced && log.isInfoEnabled()) {
        log.info(
            "Forcing {} visibility for connection (requested={}, platformType={}, eeEdition={})",
            forced, requested, type, eeEdition);
    }
}
```

- [ ] **Step 10: Swap the resolver at the list call site**

In `WorkspaceConnectionFacadeImpl.getConnections` (lines 247–266), replace the `filterVisible` call:

```java
List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(
    componentName, connectionVersion, connectionIds, tagId, environmentId, PlatformType.AUTOMATION);

Set<Long> visibleIds = resourceVisibilityResolver.filterVisibleIds(
    "Connection", workspaceId,
    CollectionUtils.map(
        connectionDTOs,
        connectionDTO -> new VisibilityRecord(
            connectionDTO.id(), connectionDTO.visibility(), connectionDTO.createdBy())));

List<ConnectionDTO> workspaceConnections = CollectionUtils.filter(
    connectionDTOs, connectionDTO -> visibleIds.contains(connectionDTO.id()));
```

Update the constructor parameter and field from `ConnectionVisibilityResolver` to
`ResourceVisibilityResolver`. Leave the AI-provider concatenation below it untouched — those connections
deliberately bypass visibility.

- [ ] **Step 11: Delete the old resolver interface, CE impl and its test**

```bash
git rm server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ConnectionVisibilityResolver.java \
       server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/DefaultConnectionVisibilityResolver.java \
       server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/DefaultConnectionVisibilityResolverTest.java
```

The EE `ConnectionVisibilityResolverImpl` still implements the deleted interface and will not compile.
Point it at `ResourceVisibilityResolver` with a temporary body that mirrors the CE implementation; Task 7
replaces it properly with grant support.

- [ ] **Step 12: Verify everything compiles and passes**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t5c.log 2>&1
echo "exit=$?"
./gradlew :server:libs:platform:platform-connection:platform-connection-service:test \
          :server:libs:automation:automation-configuration:automation-configuration-service:test \
          :server:ee:libs:automation:automation-configuration:automation-configuration-service:test \
          --continue > /tmp/t5d.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/t5d.log || echo "no failed tasks"
```

Expected: exit=0 on both.

- [ ] **Step 13: Format and commit**

```bash
./gradlew spotlessApply
git add -A server
git commit -m "732 Default connections to WORKSPACE and replace the visibility resolver"
```

---

### Task 6: `resource_grant` module

**Files:**
- Create: `server/ee/libs/platform/platform-resource-grant/platform-resource-grant-api/build.gradle.kts`
- Create: `.../platform-resource-grant-api/src/main/java/com/bytechef/ee/platform/resource/grant/domain/ResourceGrant.java`
- Create: `.../platform-resource-grant-api/src/main/java/com/bytechef/ee/platform/resource/grant/service/ResourceGrantService.java`
- Create: `server/ee/libs/platform/platform-resource-grant/platform-resource-grant-service/build.gradle.kts`
- Create: `.../platform-resource-grant-service/src/main/java/com/bytechef/ee/platform/resource/grant/repository/ResourceGrantRepository.java`
- Create: `.../platform-resource-grant-service/src/main/java/com/bytechef/ee/platform/resource/grant/service/ResourceGrantServiceImpl.java`
- Create: `.../platform-resource-grant-service/src/main/resources/config/liquibase/changelog/platform/resource_grant/20260810000002_resource_grant_init.xml`
- Modify: `settings.gradle.kts` — register both modules
- Test: `.../platform-resource-grant-service/src/test/java/com/bytechef/ee/platform/resource/grant/service/ResourceGrantServiceIntTest.java`

**Interfaces:**
- Produces: `ResourceGrantService.grant(String resourceType, long resourceId, long userId)`,
  `.revoke(String resourceType, long resourceId, long userId)`,
  `.getGrantedUserIds(String resourceType, long resourceId) -> List<Long>`,
  `.filterGrantedResourceIds(String resourceType, long userId, Collection<Long> resourceIds) -> Set<Long>`.

- [ ] **Step 1: Copy the module skeleton from an existing EE platform module**

```bash
ls server/ee/libs/platform/platform-component-policy/
cat server/ee/libs/platform/platform-component-policy/platform-component-policy-api/build.gradle.kts
cat server/ee/libs/platform/platform-component-policy/platform-component-policy-service/build.gradle.kts
grep -n "platform-component-policy" settings.gradle.kts
```

Mirror that shape exactly — dependencies, plugin block, and the `settings.gradle.kts` include lines.

- [ ] **Step 2: Write the failing integration test**

```java
package com.bytechef.ee.platform.resource.grant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ResourceGrantServiceIntTest {

    @Autowired
    private ResourceGrantService resourceGrantService;

    @Test
    void testGrantIsIdempotent() {
        resourceGrantService.grant("Connection", 10L, 7L);
        resourceGrantService.grant("Connection", 10L, 7L);

        assertThat(resourceGrantService.getGrantedUserIds("Connection", 10L)).containsExactly(7L);
    }

    @Test
    void testRevokeRemovesTheGrant() {
        resourceGrantService.grant("Connection", 11L, 7L);
        resourceGrantService.revoke("Connection", 11L, 7L);

        assertThat(resourceGrantService.getGrantedUserIds("Connection", 11L)).isEmpty();
    }

    @Test
    void testRevokeOfAbsentGrantIsSilent() {
        resourceGrantService.revoke("Connection", 12L, 7L);

        assertThat(resourceGrantService.getGrantedUserIds("Connection", 12L)).isEmpty();
    }

    @Test
    void testFilterGrantedResourceIdsReturnsOnlyGrantedSubset() {
        resourceGrantService.grant("Connection", 20L, 7L);
        resourceGrantService.grant("Connection", 22L, 7L);
        resourceGrantService.grant("Connection", 21L, 8L);

        Set<Long> granted = resourceGrantService.filterGrantedResourceIds(
            "Connection", 7L, List.of(20L, 21L, 22L, 23L));

        assertThat(granted).containsExactlyInAnyOrder(20L, 22L);
    }

    @Test
    void testGrantsAreScopedByResourceType() {
        resourceGrantService.grant("Connection", 30L, 7L);

        assertThat(resourceGrantService.getGrantedUserIds("Project", 30L)).isEmpty();
    }
}
```

Add the module's standard integration-test configuration by copying it from
`platform-component-policy-service`'s own `IntTest` setup.

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-resource-grant:platform-resource-grant-service:test > /tmp/t6.log 2>&1; echo "exit=$?"`
Expected: FAIL — module and types do not exist.

- [ ] **Step 4: Create the changeset**

```xml
<changeSet id="20260810000002" author="Ivica Cardic">
    <createTable tableName="resource_grant">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="resource_type" type="VARCHAR(64)">
            <constraints nullable="false"/>
        </column>
        <column name="resource_id" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="user_id" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="created_by" type="VARCHAR(255)">
            <constraints nullable="false"/>
        </column>
        <column name="created_date" type="TIMESTAMP">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <addUniqueConstraint
        tableName="resource_grant"
        columnNames="resource_type, resource_id, user_id"
        constraintName="resource_grant_uq"/>

    <createIndex tableName="resource_grant" indexName="resource_grant_user_idx">
        <column name="user_id"/>
        <column name="resource_type"/>
    </createIndex>
</changeSet>
```

- [ ] **Step 5: Create the entity**

```java
package com.bytechef.ee.platform.resource.grant.domain;

import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One user's access to one resource its owner has withheld. Carries visibility only — what the recipient may then do is
 * decided by the workspace-role scope machinery, unchanged.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("resource_grant")
public class ResourceGrant {

    @Id
    private Long id;

    private String resourceType;

    private long resourceId;

    private long userId;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private Instant createdDate;

    public ResourceGrant() {
    }

    public ResourceGrant(String resourceType, long resourceId, long userId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public long getResourceId() {
        return resourceId;
    }

    public long getUserId() {
        return userId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }
}
```

- [ ] **Step 6: Create the repository**

```java
package com.bytechef.ee.platform.resource.grant.repository;

import com.bytechef.ee.platform.resource.grant.domain.ResourceGrant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ResourceGrantRepository extends ListCrudRepository<ResourceGrant, Long> {

    List<ResourceGrant> findAllByResourceTypeAndResourceId(String resourceType, long resourceId);

    Optional<ResourceGrant> findByResourceTypeAndResourceIdAndUserId(
        String resourceType, long resourceId, long userId);

    @Query("""
        SELECT resource_id FROM resource_grant
         WHERE resource_type = :resourceType
           AND user_id = :userId
           AND resource_id IN (:resourceIds)
        """)
    List<Long> findGrantedResourceIds(String resourceType, long userId, Collection<Long> resourceIds);
}
```

- [ ] **Step 7: Create the service interface and implementation**

```java
package com.bytechef.ee.platform.resource.grant.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ResourceGrantService {

    /**
     * Grants {@code userId} visibility of the resource. Idempotent: re-granting an existing pair is a no-op.
     */
    void grant(String resourceType, long resourceId, long userId);

    /**
     * Revokes the grant. Silent when no grant exists.
     */
    void revoke(String resourceType, long resourceId, long userId);

    /**
     * The users currently granted the resource.
     */
    List<Long> getGrantedUserIds(String resourceType, long resourceId);

    /**
     * The subset of {@code resourceIds} that {@code userId} has been granted. One query, no N+1; returns empty for an
     * empty candidate set without touching the database.
     */
    Set<Long> filterGrantedResourceIds(String resourceType, long userId, Collection<Long> resourceIds);
}
```

```java
package com.bytechef.ee.platform.resource.grant.service;

import com.bytechef.ee.platform.resource.grant.domain.ResourceGrant;
import com.bytechef.ee.platform.resource.grant.repository.ResourceGrantRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional
public class ResourceGrantServiceImpl implements ResourceGrantService {

    private final ResourceGrantRepository resourceGrantRepository;

    @SuppressFBWarnings("EI")
    public ResourceGrantServiceImpl(ResourceGrantRepository resourceGrantRepository) {
        this.resourceGrantRepository = resourceGrantRepository;
    }

    @Override
    public void grant(String resourceType, long resourceId, long userId) {
        try {
            resourceGrantRepository.save(new ResourceGrant(resourceType, resourceId, userId));
        } catch (DuplicateKeyException duplicateKeyException) {
            // The unique constraint absorbed a concurrent or repeated grant of the same triple. The caller asked for
            // the row to exist and it does, so this is success, not a conflict.
        }
    }

    @Override
    public void revoke(String resourceType, long resourceId, long userId) {
        resourceGrantRepository.findByResourceTypeAndResourceIdAndUserId(resourceType, resourceId, userId)
            .ifPresent(resourceGrantRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getGrantedUserIds(String resourceType, long resourceId) {
        return resourceGrantRepository.findAllByResourceTypeAndResourceId(resourceType, resourceId)
            .stream()
            .map(ResourceGrant::getUserId)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> filterGrantedResourceIds(String resourceType, long userId, Collection<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(resourceGrantRepository.findGrantedResourceIds(resourceType, userId, resourceIds));
    }
}
```

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-resource-grant:platform-resource-grant-service:test > /tmp/t6.log 2>&1; echo "exit=$?"`
Expected: exit=0, 5 tests pass.

- [ ] **Step 9: Format and commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/platform/platform-resource-grant settings.gradle.kts
git commit -m "732 Add the resource grant module for named-user resource access"
```

---

### Task 7: EE resolver with grants

**Files:**
- Modify: `server/ee/.../automation-configuration-service/.../service/ResourceVisibilityResolverImpl.java` (the temporary body from Task 5, Step 11)
- Test: `server/ee/.../automation-configuration-service/src/test/.../service/ResourceVisibilityResolverImplTest.java`
- Delete: `server/ee/.../automation-configuration-service/src/test/.../service/ConnectionVisibilityResolverImplTest.java`

**Interfaces:**
- Consumes: `ResourceGrantService.filterGrantedResourceIds` (Task 6); `CurrentUserResolver.fetchCurrentUserId()` (existing).

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ResourceVisibilityResolverImplTest {

    private final ResourceGrantService resourceGrantService = Mockito.mock(ResourceGrantService.class);
    private final CurrentUserResolver currentUserResolver = Mockito.mock(CurrentUserResolver.class);

    private ResourceVisibilityResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new ResourceVisibilityResolverImpl(resourceGrantService, currentUserResolver);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("ana", "password", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGrantMakesPrivateResourceVisible() {
        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any()))
            .thenReturn(Set.of(10L));

        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visible).containsExactly(10L);
    }

    @Test
    void testPrivateWithoutGrantStaysHidden() {
        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any()))
            .thenReturn(Set.of());

        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visible).isEmpty();
    }

    @Test
    void testNoGrantQueryWhenNothingIsPrivate() {
        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(
                new VisibilityRecord(10L, ResourceVisibility.WORKSPACE, "ivica"),
                new VisibilityRecord(11L, ResourceVisibility.ORGANIZATION, "ivica")));

        assertThat(visible).containsExactlyInAnyOrder(10L, 11L);

        verify(resourceGrantService, never()).filterGrantedResourceIds(anyString(), anyLong(), any());
    }

    @Test
    void testGrantQueryIsIssuedOnceForManyPrivateCandidates() {
        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any()))
            .thenReturn(Set.of(10L));

        resolver.filterVisibleIds(
            "Connection", 1L, List.of(
                new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica"),
                new VisibilityRecord(11L, ResourceVisibility.PRIVATE, "ivica"),
                new VisibilityRecord(12L, ResourceVisibility.PRIVATE, "ivica")));

        verify(resourceGrantService).filterGrantedResourceIds(anyString(), anyLong(), any());
    }

    @Test
    void testOwnerSeesPrivateResourceWithoutGrant() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("ivica", "password", List.of()));

        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any()))
            .thenReturn(Set.of());

        Set<Long> visible = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visible).containsExactly(10L);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ResourceVisibilityResolverImplTest' > /tmp/t7.log 2>&1; echo "exit=$?"`
Expected: FAIL — the temporary body has no grant support.

- [ ] **Step 3: Implement the EE resolver**

```java
@Override
public Set<Long> filterVisibleIds(String resourceType, long workspaceId, Collection<VisibilityRecord> candidates) {
    boolean admin = SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    String currentUserLogin = SecurityUtils.getCurrentUserLogin();

    Set<Long> visibleIds = new LinkedHashSet<>();
    List<Long> undecidedPrivateIds = new ArrayList<>();

    for (VisibilityRecord candidate : candidates) {
        if (admin || candidate.visibility()
            .isAtLeast(ResourceVisibility.WORKSPACE) ||
            Objects.equals(currentUserLogin, candidate.createdBy())) {

            visibleIds.add(candidate.id());
        } else {
            undecidedPrivateIds.add(candidate.id());
        }
    }

    // Only the private candidates the cheap checks could not settle reach the grant table, and they go in one
    // batched query rather than one per row.
    if (!undecidedPrivateIds.isEmpty()) {
        OptionalLong currentUserId = currentUserResolver.fetchCurrentUserId();

        if (currentUserId.isPresent()) {
            visibleIds.addAll(
                resourceGrantService.filterGrantedResourceIds(
                    resourceType, currentUserId.getAsLong(), undecidedPrivateIds));
        }
    }

    return visibleIds;
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ResourceVisibilityResolverImplTest' > /tmp/t7.log 2>&1; echo "exit=$?"`
Expected: exit=0, 5 tests pass.

- [ ] **Step 5: Delete the superseded test**

```bash
git rm server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/ConnectionVisibilityResolverImplTest.java
```

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add -A server/ee
git commit -m "732 Resolve resource grants when filtering private resources in EE"
```

---

### Task 8: Make visibility a precondition of `hasResourceScope`

The correctness fix. Today a workspace member holding `CONNECTION_EDIT` passes `hasResourceScope` for a
colleague's `PRIVATE` connection that the list correctly hides.

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceVisibilityProvider.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ConnectionVisibilityProvider.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java:92-117`
- Modify: `server/ee/.../automation-configuration-service/.../service/PermissionServiceImpl.java:160-183`
- Test: `.../automation-configuration-service/src/test/.../service/PermissionServiceVisibilityTest.java`
- Test: `server/ee/.../automation-configuration-service/src/test/.../service/PermissionServiceVisibilityTest.java`

**Interfaces:**
- Produces: `ResourceVisibilityProvider.resourceType()`, `.fetchVisibility(long id) -> Optional<VisibilityRecord>`.

- [ ] **Step 1: Write the failing EE test**

```java
@Test
void testPrivateResourceDeniedToNonOwnerWithScope() {
    // ana holds CONNECTION_EDIT in workspace 1; connection 10 is ivica's PRIVATE connection
    authenticate("ana");

    when(workspaceScopeCacheService.getScopes(anyLong(), anyLong())).thenReturn(Set.of("CONNECTION_EDIT"));

    assertThat(permissionService.hasResourceScope(10L, "Connection", "CONNECTION_EDIT")).isFalse();
}

@Test
void testPrivateResourceAllowedToGrantee() {
    authenticate("ana");

    when(workspaceScopeCacheService.getScopes(anyLong(), anyLong())).thenReturn(Set.of("CONNECTION_EDIT"));
    when(resourceGrantService.filterGrantedResourceIds("Connection", 7L, List.of(10L))).thenReturn(Set.of(10L));

    assertThat(permissionService.hasResourceScope(10L, "Connection", "CONNECTION_EDIT")).isTrue();
}

@Test
void testWorkspaceResourceAllowedToMemberWithScope() {
    authenticate("ana");

    when(workspaceScopeCacheService.getScopes(anyLong(), anyLong())).thenReturn(Set.of("CONNECTION_EDIT"));

    assertThat(permissionService.hasResourceScope(11L, "Connection", "CONNECTION_EDIT")).isTrue();
}

@Test
void testPrivateResourceAllowedToOwner() {
    authenticate("ivica");

    when(workspaceScopeCacheService.getScopes(anyLong(), anyLong())).thenReturn(Set.of("CONNECTION_EDIT"));

    assertThat(permissionService.hasResourceScope(10L, "Connection", "CONNECTION_EDIT")).isTrue();
}

@Test
void testTenantAdminBypassesVisibility() {
    authenticateTenantAdmin("marko");

    assertThat(permissionService.hasResourceScope(10L, "Connection", "CONNECTION_EDIT")).isTrue();
}
```

Stub the visibility provider so id `10` is `PRIVATE` created by `ivica` and id `11` is `WORKSPACE`.

Write the CE mirror in the CE test class, asserting that a `WORKSPACE` connection now passes for a
non-owner — the behaviour Task 5 requires and that CE's owner-only branch currently refuses.

- [ ] **Step 2: Run both to verify they fail**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test \
          :server:ee:libs:automation:automation-configuration:automation-configuration-service:test \
          --tests '*PermissionServiceVisibilityTest' --continue > /tmp/t8.log 2>&1
echo "exit=$?"
```

Expected: FAIL.

- [ ] **Step 3: Create the visibility-lookup SPI**

```java
package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import java.util.Optional;

/**
 * SPI contributed once per resource family so {@code PermissionService} can read a single resource's visibility without
 * depending on that resource's module. Complements {@code ResourceOwnershipResolver}, which answers the workspace and
 * owner questions; this one answers reach.
 *
 * <p>
 * Fails closed: an unknown id returns {@link Optional#empty()}, which callers treat as not visible.
 *
 * @author Ivica Cardic
 */
public interface ResourceVisibilityProvider {

    /**
     * Discriminator matching {@code ResourceOwnershipResolver.resourceType()}.
     */
    String resourceType();

    /**
     * The resource's visibility and creator, or empty when it does not exist.
     */
    Optional<VisibilityRecord> fetchVisibility(long id);
}
```

- [ ] **Step 4: Implement it for connections**

```java
package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionVisibilityProvider implements ResourceVisibilityProvider {

    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ConnectionVisibilityProvider(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return connectionService.fetchConnection(id)
            .map(this::toVisibilityRecord);
    }

    private VisibilityRecord toVisibilityRecord(Connection connection) {
        return new VisibilityRecord(connection.getId(), connection.getVisibility(), connection.getCreatedBy());
    }
}
```

If `ConnectionService` has no `fetchConnection(long)` returning `Optional`, add one beside the existing
`getConnection(long)` rather than making the provider catch a not-found exception.

- [ ] **Step 5: Gate both `hasResourceScope` implementations**

Insert the same visibility precondition into each, immediately after the tenant-admin bypass and before
the existing owner/scope logic:

```java
if (!isResourceVisible(id, resourceType)) {
    return false;
}
```

with this private helper in both classes:

```java
/**
 * Visibility is a precondition of every scope check, not a filter running beside it. Without this, a member holding
 * the scope would pass here for a resource the list correctly hides — the by-id half of the same authorization
 * question answering differently from the list half.
 */
private boolean isResourceVisible(Serializable id, String resourceType) {
    ResourceVisibilityProvider provider = resourceVisibilityProviders.get(resourceType);

    if (provider == null) {
        // Resource types that have not opted into visibility are unrestricted by it; ownership and scope checks
        // below still apply.
        return true;
    }

    if (!(id instanceof Number number)) {
        return false;
    }

    return provider.fetchVisibility(number.longValue())
        .map(record -> !resourceVisibilityResolver
            .filterVisibleIds(resourceType, 0L, List.of(record))
            .isEmpty())
        .orElse(false);
}
```

Reusing `filterVisibleIds` for the single-resource case keeps one implementation of the resolution order,
so the list and by-id paths cannot drift. `workspaceId` is unused by both resolver implementations — they
resolve against the current principal, not the workspace argument — so `0L` is safe here; the parameter
exists for the future SQL-predicate implementation.

Index the providers by `resourceType()` in each constructor, exactly as `resourceOwnershipResolvers` is
indexed today.

- [ ] **Step 6: Run both tests to verify they pass**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test \
          :server:ee:libs:automation:automation-configuration:automation-configuration-service:test \
          --tests '*PermissionServiceVisibilityTest' --continue > /tmp/t8.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/t8.log || echo "no failed tasks"
```

Expected: exit=0.

- [ ] **Step 7: Run the full permission-service suites for regressions**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test \
          :server:ee:libs:automation:automation-configuration:automation-configuration-service:test \
          --continue > /tmp/t8b.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/t8b.log || echo "no failed tasks"
```

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply
git add -A server
git commit -m "732 Require resource visibility before granting any resource scope"
```

---

### Task 9: GraphQL surface

**Files:**
- Create: `server/ee/.../automation-configuration-graphql/src/main/resources/graphql/connection-sharing.graphqls`
- Create: `server/ee/.../automation-configuration-graphql/.../web/graphql/ConnectionSharingGraphQlController.java`
- Modify: `server/ee/.../automation-configuration-api/.../facade/WorkspaceConnectionFacade.java`
- Modify: `server/ee/.../automation-configuration-service/.../facade/WorkspaceConnectionFacadeImpl.java`
- Test: `server/ee/.../automation-configuration-service/src/test/.../facade/ConnectionSharingFacadeAuthorizationTest.java`

- [ ] **Step 1: Write the schema**

```graphql
extend type Mutation {
    setConnectionVisibility(workspaceId: ID!, connectionId: ID!, visibility: ResourceVisibility!): Connection!
    grantConnectionAccess(workspaceId: ID!, connectionId: ID!, userId: ID!): Boolean!
    revokeConnectionAccess(workspaceId: ID!, connectionId: ID!, userId: ID!): Boolean!
}

extend type Query {
    connectionGrants(workspaceId: ID!, connectionId: ID!): [User!]!
}

enum ResourceVisibility {
    PRIVATE
    WORKSPACE
    ORGANIZATION
}
```

- [ ] **Step 2: Write the failing authorization test**

```java
@Test
void testSetVisibilityDeniedToNonOwnerNonAdmin() {
    authenticate("ana");

    assertThatThrownBy(
        () -> workspaceConnectionFacade.setConnectionVisibility(1L, 10L, ResourceVisibility.PRIVATE))
            .isInstanceOf(AccessDeniedException.class);
}

@Test
void testSetVisibilityRejectsUnsupportedRung() {
    authenticate("ivica");

    assertThatThrownBy(
        () -> workspaceConnectionFacade.setConnectionVisibility(1L, 10L, ResourceVisibility.ORGANIZATION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ORGANIZATION");
}

@Test
void testGrantRejectsUserOutsideOwningWorkspace() {
    authenticate("ivica");

    when(workspaceUserService.isMember(1L, 99L)).thenReturn(false);

    assertThatThrownBy(() -> workspaceConnectionFacade.grantConnectionAccess(1L, 10L, 99L))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
void testGrantIsIdempotentThroughTheFacade() {
    authenticate("ivica");

    when(workspaceUserService.isMember(1L, 8L)).thenReturn(true);

    workspaceConnectionFacade.grantConnectionAccess(1L, 10L, 8L);
    workspaceConnectionFacade.grantConnectionAccess(1L, 10L, 8L);

    verify(resourceGrantService, times(2)).grant("Connection", 10L, 8L);

    assertThat(workspaceConnectionFacade.getConnectionGrants(1L, 10L)).containsExactly(8L);
}

@Test
void testConnectionGrantsDeniedToPlainViewer() {
    authenticate("ana");

    assertThatThrownBy(() -> workspaceConnectionFacade.getConnectionGrants(1L, 10L))
        .isInstanceOf(AccessDeniedException.class);
}

// Carried over from the deleted WorkspaceConnectionFacadeImplTest — these two covered the
// validators reinstated above, and are the reason those validators exist.

@Test
void testSetVisibilityToPrivateBlockedWhenConnectionIsUsedByDeployment() {
    authenticate("ivica");

    when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(true);

    assertThatThrownBy(
        () -> workspaceConnectionFacade.setConnectionVisibility(1L, 10L, ResourceVisibility.PRIVATE))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(e -> assertThat(((ConfigurationException) e).getErrorType())
                .isEqualTo(ConnectionErrorType.CONNECTION_IS_USED));
}

@Test
void testSetVisibilityBlockedWhenConnectionNotInWorkspace() {
    authenticate("ivica");

    when(workspaceConnectionService.getWorkspaceConnections(1L)).thenReturn(List.of());

    assertThatThrownBy(
        () -> workspaceConnectionFacade.setConnectionVisibility(1L, 10L, ResourceVisibility.WORKSPACE))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(e -> assertThat(((ConfigurationException) e).getErrorType())
                .isEqualTo(ConnectionErrorType.INVALID_CONNECTION));
}

@Test
void testWideningVisibilityDoesNotCheckDeploymentUsage() {
    authenticate("ivica");

    workspaceConnectionFacade.setConnectionVisibility(1L, 10L, ResourceVisibility.WORKSPACE);

    // Widening can never strand a deployment, so the usage query must not run — it is a real
    // database round-trip on a path that has no use for it.
    verify(projectDeploymentWorkflowService, never()).isConnectionUsed(anyLong());
}
```

Note the `ORGANIZATION` case asserts rejection at the *facade*: the picker offers it to admins, and the
admin path runs through `OrganizationConnectionFacade`, so the workspace facade rejects it regardless of
caller.

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ConnectionSharingFacadeAuthorizationTest' > /tmp/t9.log 2>&1; echo "exit=$?"`
Expected: FAIL — methods do not exist.

- [ ] **Step 4: Add the facade methods**

Annotate on the facade, not the controller, per the established convention. Owner-or-admin is expressed as
`isResourceOwner` OR the admin bypass already inside `hasResourceRole`:

**Guards inherited from the deleted `demoteToPrivate`** (Task 3 removed the method; its invariants
survive and must be reproduced here):

1. `validateConnectionBelongsToWorkspace(workspaceId, connectionId)`.
2. `validateConnectionNotUsedByDeployments(connectionId)` — **only when narrowing to `PRIVATE`**.
   Withdrawing a connection that an active deployment depends on breaks that deployment; the old
   demote path blocked it and the new setter must too.
3. Non-disclosing failures: authorization is checked before any existence or usage validation, and
   "not found", "not yours" and "in use" collapse to one `INVALID_CONNECTION` response so a caller
   cannot probe the `(workspaceId, connectionId)` namespace by comparing error messages.

Both validators were deleted by Task 3 (they became uncalled private methods, which SpotBugs rejects).
Reinstate them verbatim — they were correct, and rewriting them from the description invites drift:

```java
private void validateConnectionBelongsToWorkspace(long workspaceId, long connectionId) {
    List<Long> workspaceConnectionIds = CollectionUtils.map(
        workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

    if (!workspaceConnectionIds.contains(connectionId)) {
        throw new ConfigurationException(
            "Connection id=%s does not belong to workspace id=%s".formatted(connectionId, workspaceId),
            ConnectionErrorType.INVALID_CONNECTION);
    }
}

private void validateConnectionNotUsedByDeployments(long connectionId) {
    if (projectDeploymentWorkflowService.isConnectionUsed(connectionId)) {
        throw new ConfigurationException(
            "Connection id=%s is used by active deployments".formatted(connectionId),
            ConnectionErrorType.CONNECTION_IS_USED);
    }
}
```

```java
@Override
@Transactional
@PreAuthorize("@permissionService.isResourceOwner('Connection', #connectionId) || " +
    "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')")
public ConnectionDTO setConnectionVisibility(
    long workspaceId, long connectionId, ResourceVisibility visibility) {

    if (!resourceVisibilityPolicyRegistry.supports("Connection", visibility)) {
        throw new IllegalArgumentException("Connection does not support visibility " + visibility);
    }

    validateConnectionBelongsToWorkspace(workspaceId, connectionId);

    if (visibility == ResourceVisibility.PRIVATE) {
        validateConnectionNotUsedByDeployments(connectionId);
    }

    if (visibility == ResourceVisibility.ORGANIZATION) {
        throw new IllegalArgumentException(
            "ORGANIZATION visibility is set through OrganizationConnectionFacade, not the workspace facade");
    }

    return connectionFacade.updateVisibility(connectionId, visibility);
}
```

`grantConnectionAccess` / `revokeConnectionAccess` / `getConnectionGrants` carry the same `@PreAuthorize`;
the grant method additionally checks `workspaceUserService.isMember(workspaceId, userId)` and throws
`IllegalArgumentException` — not `AccessDenied` — so a non-member id is indistinguishable from a bad id.

- [ ] **Step 5: Add the controller delegating to the facade**

No business logic in the controller; it maps arguments and returns.

- [ ] **Step 5b: Recreate the annotation-pinning test**

Task 3 deleted `WorkspaceConnectionFacadeAuthorizationTest` along with the methods it pinned. Recreate it
for the new mutations, keeping its reflection helper and its rationale: the guards live on the facade so
they protect *every* caller rather than only the GraphQL entry point, and a reflection test catches a
refactor that silently drops one. Runtime enforcement of the expressions is proven generically by
`PreAuthorizeProxyEnforcementIntTest`, so this test only pins their presence and text.

Pin `@PreAuthorize` on all four of `setConnectionVisibility`, `grantConnectionAccess`,
`revokeConnectionAccess` and `getConnectionGrants`. Unlike the old `demoteToPrivate`, none of them
needs the annotation *absent*: the owner-OR-admin rule is expressible in SpEL
(`isResourceOwner(...) || hasResourceRole(...)`), so there is no reason to drop to a programmatic check
and no orphan-recovery hole to keep open — an admin always satisfies the second disjunct.

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ConnectionSharingFacadeAuthorizationTest' > /tmp/t9.log 2>&1; echo "exit=$?"`
Expected: exit=0, 5 tests pass.

- [ ] **Step 7: Format, run full check, commit**

```bash
./gradlew spotlessApply
./gradlew check --continue > /tmp/t9b.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/t9b.log || echo "no failed tasks"
git add -A server
git commit -m "732 Add connection visibility and sharing GraphQL mutations"
```

---

### Task 10: Client picker

**Files:**
- Create: `client/src/shared/components/connection/ConnectionVisibilityPicker.tsx`
- Create: `client/src/shared/components/connection/ConnectionVisibilityPicker.test.tsx`
- Create: `client/src/graphql/automation/configuration/setConnectionVisibility.graphql`
- Create: `client/src/graphql/automation/configuration/grantConnectionAccess.graphql`
- Create: `client/src/graphql/automation/configuration/revokeConnectionAccess.graphql`
- Create: `client/src/graphql/automation/configuration/connectionGrants.graphql`
- Delete: `client/src/pages/automation/connections/components/VisibilityMenuItems.tsx` and its test
- Delete: `client/src/pages/automation/connections/components/connection-list/tests/ConnectionListItemVisibilityGate.test.tsx`
- Delete: `client/src/graphql/automation/configuration/promoteConnectionToWorkspace.graphql`, `demoteConnectionToPrivate.graphql`, `promoteAllPrivateConnectionsToWorkspace.graphql`
- Modify: `client/src/shared/components/connection/ConnectionDialog.tsx` — mount the picker
- Modify: `client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx` — drop the menu items
- Modify: `client/src/pages/automation/connections/components/ConnectionScopeBadge.tsx` — render the new states

- [ ] **Step 1: Write the failing picker test**

```tsx
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import ConnectionVisibilityPicker from './ConnectionVisibilityPicker';

describe('ConnectionVisibilityPicker', () => {
    it('preselects Shared for a new connection', () => {
        render(<ConnectionVisibilityPicker onChange={vi.fn()} value="WORKSPACE" grantedUserIds={[]} />);

        expect(screen.getByRole('radio', {name: /shared with workspace/i})).toBeChecked();
    });

    it('shows the people picker only when Specific people is selected', async () => {
        const user = userEvent.setup();

        render(<ConnectionVisibilityPicker onChange={vi.fn()} value="PRIVATE" grantedUserIds={[]} />);

        expect(screen.queryByLabelText(/add person/i)).not.toBeInTheDocument();

        await user.click(screen.getByRole('radio', {name: /specific people/i}));

        expect(screen.getByLabelText(/add person/i)).toBeInTheDocument();
    });

    it('reports PRIVATE when Specific people is chosen', async () => {
        const onChange = vi.fn();
        const user = userEvent.setup();

        render(<ConnectionVisibilityPicker onChange={onChange} value="WORKSPACE" grantedUserIds={[]} />);

        await user.click(screen.getByRole('radio', {name: /specific people/i}));

        expect(onChange).toHaveBeenCalledWith('PRIVATE');
    });

    it('renders Specific people as selected when grants exist', () => {
        render(<ConnectionVisibilityPicker onChange={vi.fn()} value="PRIVATE" grantedUserIds={[7]} />);

        expect(screen.getByRole('radio', {name: /specific people/i})).toBeChecked();
    });

    it('hides the Organization option for non-admins', () => {
        render(<ConnectionVisibilityPicker onChange={vi.fn()} value="WORKSPACE" grantedUserIds={[]} />);

        expect(screen.queryByRole('radio', {name: /organization/i})).not.toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run it to verify it fails**

Run: `cd client && npx vitest run src/shared/components/connection/ConnectionVisibilityPicker.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement the picker**

Radio group over `Shared with workspace` / `Private` / `Specific people` / `Organization` (admin only).
`Specific people` and `Private` both emit `PRIVATE`; the two are distinguished by whether `grantedUserIds`
is non-empty, matching the spec's one-state-machine rule. Follow the styling of the existing radio groups
in `ConnectionDialog.tsx`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd client && npx vitest run src/shared/components/connection/ConnectionVisibilityPicker.test.tsx`
Expected: 5 tests pass.

- [ ] **Step 5: Regenerate GraphQL types**

```bash
cd client && npm run graphql-codegen
```

- [ ] **Step 6: Delete the superseded client surface and mount the picker**

Delete the files listed above, mount `ConnectionVisibilityPicker` in `ConnectionDialog`, drop the
visibility entries from `ConnectionListItem`'s ellipsis menu, and update `ConnectionScopeBadge` to render
`Private` / `Specific people` / `Organization` (nothing for `WORKSPACE`, which is the unremarkable default).

- [ ] **Step 7: Verify the client is clean**

```bash
cd client && npm run format && npm run check
echo "exit=$?"
```

Expected: exit=0. `npm run check` covers lint, typecheck and the full Vitest suite — a dangling import from
a deleted component fails here.

- [ ] **Step 8: Commit**

```bash
git add -A client
git commit -m "732 client - Replace the connection promote menu with a visibility picker"
```

---

### Task 11: Documentation

**Files:**
- Modify: `CLAUDE.md:974` — carve named-user grants out of the no-relation-table rule
- Modify: `CLAUDE.md` "Connection Visibility (EE-only feature)" section — rewrite
- Modify: `gecko-remediation-tasks.md` — record the deliberate T18 CE reversal

- [ ] **Step 1: Amend the workspace-scoping convention**

After the existing sentence *"Sharing is additive to a column — it does not need a relation table"*, add:

```markdown
  **Exception — named-user grants.** A column expresses *reach* (private / workspace / organization) and
  is the right shape for it. It cannot express "these three specific people", which would need an
  unbounded array in a cell. Grants to individual users therefore live in the polymorphic
  `resource_grant` table (EE) — see
  `docs/superpowers/specs/2026-08-10-resource-visibility-design.md`. This is the one sanctioned
  relation table for sharing; the rule above still governs reach.
```

- [ ] **Step 2: Rewrite the Connection Visibility section**

Retitle to `### Resource Visibility & Sharing` and replace the body: `ResourceVisibility` replaces
`ConnectionVisibility`; `WORKSPACE` is the default in both editions; CE force-writes `WORKSPACE` and
embedded force-writes `PRIVATE`; the `ROLE_ADMIN` gate on `WORKSPACE` is gone; the promote/demote
mutations are replaced by `setConnectionVisibility` plus the grant mutations; visibility is a precondition
of `hasResourceScope`. Keep the metric note, updating the tag values to `PRIVATE|WORKSPACE|ORGANIZATION`.

- [ ] **Step 3: Record the T18 reversal**

Under T18 in `gecko-remediation-tasks.md`:

```markdown
- **CE owner-isolation for connections was deliberately reversed** on 2026-08-10. CE now force-writes
  WORKSPACE visibility, so workspace members share stored credentials by default. The EE half is not only
  intact but tightened: visibility is now a precondition of every `hasResourceScope` check, closing a hole
  where a member holding `CONNECTION_EDIT` could operate by id on a colleague's PRIVATE connection.
  See `docs/superpowers/specs/2026-08-10-resource-visibility-design.md` §3.5 and §7.
```

- [ ] **Step 4: Verify the claims are accurate**

Re-read each amended paragraph against the code as it now stands. Documentation that describes the plan
rather than the result is worse than none.

- [ ] **Step 5: Commit**

```bash
git add CLAUDE.md gecko-remediation-tasks.md
git commit -m "732 Document the resource visibility model and the deliberate T18 CE reversal"
```

---

### Task 12: Audit events for visibility and grant changes

Spec §8.3. Every state change that widens or narrows who can reach a credential must be reconstructable
after the fact.

**Files:**
- Modify: `server/ee/libs/platform/platform-connection/platform-connection-audit/src/main/java/com/bytechef/ee/platform/connection/audit/ConnectionAuditEvent.java`
- Modify: `server/ee/.../automation-configuration-service/.../listener/ConnectionAuditEventListener.java`
- Modify: `server/ee/.../automation-configuration-service/.../facade/WorkspaceConnectionFacadeImpl.java` — publish the events
- Test: `server/ee/.../automation-configuration-service/src/test/.../listener/ConnectionAuditEventListenerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void testVisibilityChangeIsAudited() {
    workspaceConnectionFacade.setConnectionVisibility(1L, 10L, ResourceVisibility.PRIVATE);

    ConnectionAuditEvent event = capturedAuditEvent();

    assertThat(event.getEventType()).isEqualTo(ConnectionAuditEvent.EventType.VISIBILITY_CHANGED);
    assertThat(event.getConnectionId()).isEqualTo(10L);
    assertThat(event.getPreviousVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
    assertThat(event.getNewVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
}

@Test
void testGrantIsAudited() {
    workspaceConnectionFacade.grantConnectionAccess(1L, 10L, 8L);

    ConnectionAuditEvent event = capturedAuditEvent();

    assertThat(event.getEventType()).isEqualTo(ConnectionAuditEvent.EventType.ACCESS_GRANTED);
    assertThat(event.getTargetUserId()).isEqualTo(8L);
}

@Test
void testRevokeIsAudited() {
    workspaceConnectionFacade.revokeConnectionAccess(1L, 10L, 8L);

    ConnectionAuditEvent event = capturedAuditEvent();

    assertThat(event.getEventType()).isEqualTo(ConnectionAuditEvent.EventType.ACCESS_REVOKED);
    assertThat(event.getTargetUserId()).isEqualTo(8L);
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ConnectionAuditEventListenerTest' > /tmp/t12.log 2>&1; echo "exit=$?"`
Expected: FAIL — the event types do not exist.

- [ ] **Step 3: Add the three event types**

Read the existing `EventType` enum and follow its shape exactly, appending `VISIBILITY_CHANGED`,
`ACCESS_GRANTED`, `ACCESS_REVOKED` at the **end** — the enum is persisted, so appending is safe and
reordering is not. Add `previousVisibility`, `newVisibility` and `targetUserId` fields, all nullable, since
existing event types populate none of them.

- [ ] **Step 4: Publish the events from the facade**

Publish inside the same transaction as the mutation, so a rolled-back change leaves no audit record
claiming it happened.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ConnectionAuditEventListenerTest' > /tmp/t12.log 2>&1; echo "exit=$?"`
Expected: exit=0, 3 tests pass.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add -A server/ee
git commit -m "732 Audit connection visibility changes and access grants"
```

---

### Task 13: Full verification

- [ ] **Step 1: Full server check**

```bash
./gradlew check --continue > /tmp/final.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/final.log || echo "no failed tasks"
```

- [ ] **Step 2: Full client check**

```bash
cd client && npm run check
echo "exit=$?"
```

- [ ] **Step 3: Confirm the old vocabulary is gone**

```bash
grep -rn "ConnectionVisibility\|promoteConnectionToWorkspace\|demoteConnectionToPrivate\|BulkPromoteResult" \
  server client docs/superpowers/specs/2026-08-10-resource-visibility-design.md \
  --exclude-dir=build --exclude-dir=bin --exclude-dir=node_modules \
  || echo "clean"
```

Expected: `clean`, except for the spec's own §4.1 removal table, which names them deliberately.

- [ ] **Step 4: Manual smoke test in EE**

Start the app with `bytechef.edition=EE`, then confirm:
1. A new connection is created `Shared`, with no admin role required.
2. Setting it `Private` removes it from a second user's connections list.
3. Granting that user access returns it to their list.
4. That user can open and edit it (the §7 path — the check that used to disagree with the list).
5. Revoking removes it again.

- [ ] **Step 5: Manual smoke test in CE**

Start with `bytechef.edition=CE` and confirm the connections list is populated, the picker is absent, and a
second user sees the first user's connections. **An empty list here means Task 5's two halves were split.**

---

## Self-review

**Spec coverage.** §3.1 → Tasks 1, 2. §3.2 → Tasks 1, 5. §3.3 → Task 10. §3.4 → Tasks 5, 7. §3.5 → Task 5.
§4.1 → Tasks 2, 3, 5, 7, 10. §4.2 → Task 4. §4.3 → Task 6. §5 → Tasks 1, 5, 6, 7. §6 → Tasks 5, 7. §7 →
Task 8. §8.1–8.2 → Task 9. §8.3 → Task 12. §9 → Task 10. §10 → Task 9. §11 → phase 2, out of scope.
§12 → every task's test steps. §13 → Task 11. No spec section is unclaimed.

**Type consistency.** `filterVisibleIds(String, long, Collection<VisibilityRecord>) -> Set<Long>` is
identical in Tasks 5, 7 and 8. `VisibilityRecord(long, ResourceVisibility, String)` is identical in Tasks
5, 7 and 8. `hasResourceScope(Serializable, String, String)` matches the existing interface — id first.
`ResourceGrantService.filterGrantedResourceIds(String, long, Collection<Long>) -> Set<Long>` is identical
in Tasks 6, 7 and 8.
