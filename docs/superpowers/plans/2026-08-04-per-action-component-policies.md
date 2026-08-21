# Per-Action Component Policies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let admins disable individual actions and triggers per component (deny-list under the existing component toggle), hidden from editor pickers and blocked at execution.

**Architecture:** New `component_operation_policy` deny-list child table in the EE `platform-component-policy` module (presence of a row = operation disabled). The CE `ComponentVisibilityProvider` SPI gains `isActionVisible`/`isTriggerVisible` default methods that fall back to `isVisible(componentName)`; the EE provider answers both from one cached per-tenant query. Guards mirror the component-level pattern in `ActionDefinitionServiceImpl` / `TriggerDefinitionServiceImpl`; the Component Policies page rows become expandable with per-operation switches.

**Tech Stack:** Spring Data JDBC + Liquibase, Spring GraphQL, Caffeine, React 19 + TanStack Query + generated GraphQL hooks, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-04-per-action-component-policies-design.md` — read it first.

## Global Constraints

- Files under `server/ee/` use the ByteChef Enterprise license header and `@version ee` Javadoc tag; files under `server/libs/` use the Apache 2.0 header (copy from a sibling file in the same directory).
- Java style: one blank line before control statements; blank line between a variable modification and its first use; no `_` prefixes; descriptive variable names; no `TODO:` comments; test method names camelCase without underscores.
- INT-ordinal enums are append-only. `OperationType` ordinals get a stability test.
- Error-type keys are append-only integers: `ActionDefinitionErrorType.ACTION_DISABLED = 107`, `TriggerDefinitionErrorType.TRIGGER_DISABLED = 111`.
- Before every server commit: `./gradlew spotlessApply` then the module's `check`; redirect Gradle output to a file, check `$?` on its own line, grep the file for `^> Task .* FAILED` (never judge a piped Gradle run).
- Client: object keys sorted (sort-keys is NOT auto-fixed), lucide icons imported with `Icon` suffix, `twMerge` (never `cn()`), interfaces end in `I`/`Props`, hook ordering per CLAUDE.md. Run `npm run check` in `client/` before client commits.
- Commit messages: server `- component-policies <description>` (matches the module's history); client `- component-policies client - <description>`. Never `git commit --amend` on this branch.
- The user commits in parallel: `git add` ONLY the files your task touched, never `git add -A`.

---

### Task 1: `ComponentOperationPolicy` domain, repository, Liquibase table

**Files:**
- Create: `server/ee/libs/platform/platform-component-policy/platform-component-policy-api/src/main/java/com/bytechef/ee/platform/component/policy/ComponentOperationPolicy.java`
- Create: `server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/main/java/com/bytechef/ee/platform/component/policy/repository/ComponentOperationPolicyRepository.java`
- Create: `server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/main/resources/config/liquibase/changelog/platform/component_policy/20260804000001_component_operation_policy.xml`
- Test: `server/ee/libs/platform/platform-component-policy/platform-component-policy-api/src/test/java/com/bytechef/ee/platform/component/policy/ComponentOperationPolicyOperationTypeTest.java`
- Test (extend): `server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/test/java/com/bytechef/ee/platform/component/policy/repository/ComponentPolicyRepositoryIntTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: `ComponentOperationPolicy` entity with nested `enum OperationType { ACTION, TRIGGER }`, `getComponentName()`, `getOperationType()` (enum), `getOperationName()`, and static `String key(String componentName, OperationType operationType, String operationName)` returning `componentName + "#" + operationType.name() + "#" + operationName`. `ComponentOperationPolicyRepository extends CrudRepository<ComponentOperationPolicy, Long>` with `List<ComponentOperationPolicy> findAllByComponentName(String componentName)` and `Optional<ComponentOperationPolicy> findByComponentNameAndOperationTypeAndOperationName(String componentName, int operationType, String operationName)`.

- [ ] **Step 1: Write the failing ordinal-stability test**

`ComponentOperationPolicyOperationTypeTest.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.component.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ComponentOperationPolicyOperationTypeTest {

    @Test
    void testOperationTypeOrdinalsAreStable() {
        assertThat(ComponentOperationPolicy.OperationType.ACTION.ordinal()).isEqualTo(0);
        assertThat(ComponentOperationPolicy.OperationType.TRIGGER.ordinal()).isEqualTo(1);
    }

    @Test
    void testKeyFormat() {
        assertThat(
            ComponentOperationPolicy.key("slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage"))
                .isEqualTo("slack#ACTION#sendMessage");
    }
}
```

If the api module's `build.gradle.kts` has no `testImplementation("org.assertj:assertj-core")`, add it.

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-api:test --tests ComponentOperationPolicyOperationTypeTest > /tmp/t1.log 2>&1
echo $?
grep "FAILED\|cannot find symbol" /tmp/t1.log
```

Expected: compilation failure — `ComponentOperationPolicy` does not exist.

- [ ] **Step 3: Create the entity**

`ComponentOperationPolicy.java` — model on the existing `ComponentPolicy.java` in the same package (Enterprise header, `@version ee` Javadoc). INT-ordinal enum storage follows the repo pattern (int field + enum accessor, as in `TriggerExecution.getStatus()`):

```java
package com.bytechef.ee.platform.component.policy;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A deny-list row disabling a single action or trigger of a component tenant-wide. Presence of a row means the
 * operation is disabled; absence means enabled. See the per-action component policies design spec.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("component_operation_policy")
public class ComponentOperationPolicy {

    /**
     * Operation kind. INT ordinal persisted — append new values at the end, never reorder.
     */
    public enum OperationType {
        ACTION, TRIGGER
    }

    @Id
    private Long id;

    @Column("component_name")
    private String componentName;

    @Column("operation_type")
    private int operationType;

    @Column("operation_name")
    private String operationName;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public ComponentOperationPolicy() {
    }

    public ComponentOperationPolicy(String componentName, OperationType operationType, String operationName) {
        this.componentName = componentName;
        this.operationType = operationType.ordinal();
        this.operationName = operationName;
    }

    public static String key(String componentName, OperationType operationType, String operationName) {
        return componentName + "#" + operationType.name() + "#" + operationName;
    }

    public Long getId() {
        return id;
    }

    public String getComponentName() {
        return componentName;
    }

    public OperationType getOperationType() {
        return OperationType.values()[operationType];
    }

    public String getOperationName() {
        return operationName;
    }

    public String toKey() {
        return key(componentName, getOperationType(), operationName);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Same command as Step 2. Expected: PASS.

- [ ] **Step 5: Add the Liquibase changeset**

New file `20260804000001_component_operation_policy.xml` in the SAME directory as `20260620000001_component_policy_init.xml` (the `liquibase-config` master.xml already `includeAll`s that directory — no registration needed):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260804000001" author="Ivica Cardic">
        <createTable tableName="component_operation_policy">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="component_name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="operation_type" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="operation_name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </createTable>
        <addUniqueConstraint
            tableName="component_operation_policy"
            columnNames="component_name, operation_type, operation_name"
            constraintName="uk_component_operation_policy"/>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 6: Create the repository**

`ComponentOperationPolicyRepository.java` (Enterprise header, `@version ee`), modeled on `ComponentPolicyRepository`:

```java
package com.bytechef.ee.platform.component.policy.repository;

import com.bytechef.ee.platform.component.policy.ComponentOperationPolicy;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnEEVersion
public interface ComponentOperationPolicyRepository extends CrudRepository<ComponentOperationPolicy, Long> {

    List<ComponentOperationPolicy> findAllByComponentName(String componentName);

    Optional<ComponentOperationPolicy> findByComponentNameAndOperationTypeAndOperationName(
        String componentName, int operationType, String operationName);
}
```

- [ ] **Step 7: Extend the repository integration test**

Add to the existing `ComponentPolicyRepositoryIntTest` (reuse its Testcontainers setup — read the class first; it already loads the module changelogs):

```java
@Test
void testComponentOperationPolicyRoundTrip() {
    componentOperationPolicyRepository.save(
        new ComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage"));

    List<ComponentOperationPolicy> componentOperationPolicies =
        componentOperationPolicyRepository.findAllByComponentName("slack");

    assertThat(componentOperationPolicies).hasSize(1);
    assertThat(componentOperationPolicies.getFirst()
        .getOperationType()).isEqualTo(ComponentOperationPolicy.OperationType.ACTION);
    assertThat(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
        "slack", ComponentOperationPolicy.OperationType.ACTION.ordinal(), "sendMessage")).isPresent();
}
```

Autowire `ComponentOperationPolicyRepository` alongside the existing repository field.

- [ ] **Step 8: Run module tests (Docker required for the IntTest), spotless, check**

```bash
./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-api:test :server:ee:libs:platform:platform-component-policy:platform-component-policy-service:test > /tmp/t1b.log 2>&1
echo $?
grep "^> Task .* FAILED" /tmp/t1b.log
./gradlew spotlessApply > /tmp/t1c.log 2>&1
echo $?
```

Expected: all pass. (`testIntegration` runs the IntTest — run it if Docker is available: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-service:testIntegration`.)

- [ ] **Step 9: Commit**

```bash
git add server/ee/libs/platform/platform-component-policy
git commit -m "- component-policies Add component_operation_policy entity, repository and migration"
```

---

### Task 2: Service methods (deny-list semantics)

**Files:**
- Modify: `server/ee/libs/platform/platform-component-policy/platform-component-policy-api/src/main/java/com/bytechef/ee/platform/component/policy/ComponentPolicyService.java`
- Modify: `server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/main/java/com/bytechef/ee/platform/component/policy/service/ComponentPolicyServiceImpl.java`
- Test (extend): `server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/test/java/com/bytechef/ee/platform/component/policy/service/ComponentPolicyServiceImplTest.java`

**Interfaces:**
- Consumes: Task 1's entity + repository (exact signatures in Task 1's Produces block).
- Produces: on `ComponentPolicyService`:
  - `Set<String> getDisabledOperationKeys()` — keys in `ComponentOperationPolicy.key(...)` format.
  - `List<ComponentOperationPolicy> getComponentOperationPolicies(String componentName)`
  - `void updateComponentOperationPolicy(String componentName, ComponentOperationPolicy.OperationType operationType, String operationName, boolean enabled)` — `enabled=true` deletes the row if present, `enabled=false` inserts if absent; both idempotent.

- [ ] **Step 1: Write the failing tests**

Add to `ComponentPolicyServiceImplTest` (read the class first; it mocks `ComponentPolicyRepository` — add a `ComponentOperationPolicyRepository` mock and pass it to the impl constructor):

```java
@Test
void testUpdateComponentOperationPolicyDisableInsertsRow() {
    when(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
        "slack", 0, "sendMessage")).thenReturn(Optional.empty());

    componentPolicyService.updateComponentOperationPolicy(
        "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);

    verify(componentOperationPolicyRepository).save(any(ComponentOperationPolicy.class));
}

@Test
void testUpdateComponentOperationPolicyDisableIsIdempotent() {
    when(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
        "slack", 0, "sendMessage")).thenReturn(Optional.of(
            new ComponentOperationPolicy("slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage")));

    componentPolicyService.updateComponentOperationPolicy(
        "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);

    verify(componentOperationPolicyRepository, never()).save(any());
}

@Test
void testUpdateComponentOperationPolicyEnableDeletesRow() {
    ComponentOperationPolicy row = new ComponentOperationPolicy(
        "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage");

    when(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
        "slack", 0, "sendMessage")).thenReturn(Optional.of(row));

    componentPolicyService.updateComponentOperationPolicy(
        "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", true);

    verify(componentOperationPolicyRepository).delete(row);
}

@Test
void testGetDisabledOperationKeys() {
    when(componentOperationPolicyRepository.findAll()).thenReturn(List.of(
        new ComponentOperationPolicy("slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage")));

    assertThat(componentPolicyService.getDisabledOperationKeys()).containsExactly("slack#ACTION#sendMessage");
}
```

- [ ] **Step 2: Run to verify failure** — same Gradle test command pattern as Task 1; expected: compilation failure (methods missing).

- [ ] **Step 3: Implement**

Interface additions on `ComponentPolicyService`:

```java
Set<String> getDisabledOperationKeys();

List<ComponentOperationPolicy> getComponentOperationPolicies(String componentName);

void updateComponentOperationPolicy(
    String componentName, ComponentOperationPolicy.OperationType operationType, String operationName,
    boolean enabled);
```

Impl (constructor gains `ComponentOperationPolicyRepository componentOperationPolicyRepository`; the write method carries the same `@PreAuthorize(ADMIN)` as `updateComponentPolicy`, reads are `@Transactional(readOnly = true)`):

```java
@Override
@Transactional(readOnly = true)
public Set<String> getDisabledOperationKeys() {
    return StreamSupport.stream(componentOperationPolicyRepository.findAll().spliterator(), false)
        .map(ComponentOperationPolicy::toKey)
        .collect(Collectors.toSet());
}

@Override
@Transactional(readOnly = true)
public List<ComponentOperationPolicy> getComponentOperationPolicies(String componentName) {
    return componentOperationPolicyRepository.findAllByComponentName(componentName);
}

@Override
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public void updateComponentOperationPolicy(
    String componentName, ComponentOperationPolicy.OperationType operationType, String operationName,
    boolean enabled) {

    Optional<ComponentOperationPolicy> componentOperationPolicyOptional =
        componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
            componentName, operationType.ordinal(), operationName);

    if (enabled) {
        componentOperationPolicyOptional.ifPresent(componentOperationPolicyRepository::delete);
    } else if (componentOperationPolicyOptional.isEmpty()) {
        componentOperationPolicyRepository.save(
            new ComponentOperationPolicy(componentName, operationType, operationName));
    }
}
```

- [ ] **Step 4: Run tests to verify pass**, then `./gradlew spotlessApply` + module `check` (file-redirect discipline).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-component-policy
git commit -m "- component-policies Add per-operation deny-list service methods"
```

---

### Task 3: SPI default methods + EE provider

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/visibility/ComponentVisibilityProvider.java`
- Modify: `server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/main/java/com/bytechef/ee/platform/component/policy/ComponentPolicyVisibilityProvider.java`
- Test (extend): `server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/test/java/com/bytechef/ee/platform/component/policy/ComponentPolicyVisibilityProviderTest.java`

**Interfaces:**
- Consumes: `ComponentPolicyService.getDisabledComponentNames()` and `getDisabledOperationKeys()` (Task 2).
- Produces: on the CE SPI:

```java
default boolean isActionVisible(String componentName, String actionName) {
    return isVisible(componentName);
}

default boolean isTriggerVisible(String componentName, String triggerName) {
    return isVisible(componentName);
}
```

- [ ] **Step 1: Write the failing provider tests**

Add to `ComponentPolicyVisibilityProviderTest` (read it first; it mocks `ComponentPolicyService`):

```java
@Test
void testActionInvisibleWhenOperationDisabled() {
    when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of());
    when(componentPolicyService.getDisabledOperationKeys()).thenReturn(Set.of("slack#ACTION#sendMessage"));

    assertThat(componentPolicyVisibilityProvider.isActionVisible("slack", "sendMessage")).isFalse();
    assertThat(componentPolicyVisibilityProvider.isActionVisible("slack", "sendDirectMessage")).isTrue();
    assertThat(componentPolicyVisibilityProvider.isTriggerVisible("slack", "sendMessage")).isTrue();
}

@Test
void testOperationInvisibleWhenWholeComponentDisabled() {
    when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of("slack"));
    when(componentPolicyService.getDisabledOperationKeys()).thenReturn(Set.of());

    assertThat(componentPolicyVisibilityProvider.isActionVisible("slack", "sendMessage")).isFalse();
    assertThat(componentPolicyVisibilityProvider.isTriggerVisible("slack", "newMessage")).isFalse();
}
```

Also add a CE-default test in the CE module — create `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/visibility/ComponentVisibilityProviderTest.java` (Apache header):

```java
class ComponentVisibilityProviderTest {

    @Test
    void testDefaultOperationVisibilityFallsBackToComponentVisibility() {
        ComponentVisibilityProvider visibilityProvider = componentName -> !"disabled".equals(componentName);

        assertThat(visibilityProvider.isActionVisible("disabled", "anyAction")).isFalse();
        assertThat(visibilityProvider.isActionVisible("enabled", "anyAction")).isTrue();
        assertThat(visibilityProvider.isTriggerVisible("disabled", "anyTrigger")).isFalse();
    }
}
```

- [ ] **Step 2: Run both test classes to verify failure** (methods missing → compilation failure).

- [ ] **Step 3: Implement**

SPI: add the two default methods exactly as in Produces (keep the existing Javadoc style; document that a disabled component implies every operation invisible).

EE provider: replace the `Cache<String, Set<String>>` with a `Cache<String, DisabledPolicies>` where:

```java
private record DisabledPolicies(Set<String> componentNames, Set<String> operationKeys) {
}

private DisabledPolicies loadDisabledPolicies() {
    return new DisabledPolicies(
        componentPolicyService.getDisabledComponentNames(), componentPolicyService.getDisabledOperationKeys());
}
```

`isVisible` reads `disabledPolicies.componentNames()`; the overrides:

```java
@Override
public boolean isActionVisible(String componentName, String actionName) {
    DisabledPolicies disabledPolicies = getDisabledPolicies();

    return !disabledPolicies.componentNames()
        .contains(componentName) &&
        !disabledPolicies.operationKeys()
            .contains(ComponentOperationPolicy.key(
                componentName, ComponentOperationPolicy.OperationType.ACTION, actionName));
}
```

(`isTriggerVisible` mirrors with `OperationType.TRIGGER`; `getDisabledPolicies()` is the cache lookup keyed by `TenantContext.getCurrentTenantId()`, same 10s TTL.)

- [ ] **Step 4: Run tests to verify pass**, spotless + both modules' `check` (file-redirect discipline).

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api server/ee/libs/platform/platform-component-policy
git commit -m "- component-policies Extend ComponentVisibilityProvider SPI to actions and triggers"
```

---

### Task 4: Action execution guard + listing filter

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ActionDefinitionServiceImpl.java` (guard helper at ~line 905, call sites at lines 258 and 334, listing at line 390)
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/exception/ActionDefinitionErrorType.java`
- Test (extend): `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ActionDefinitionServiceImplVisibilityTest.java`

**Interfaces:**
- Consumes: `ComponentVisibilityProvider.isActionVisible(componentName, actionName)` (Task 3).
- Produces: `ActionDefinitionErrorType.ACTION_DISABLED` (key 107). Guard behavior relied on by nothing downstream — terminal.

- [ ] **Step 1: Write the failing tests**

Read `ActionDefinitionServiceImplVisibilityTest` first and follow its fixture style (it already stubs a `ComponentVisibilityProvider` and asserts the `COMPONENT_DISABLED` path). Add:

```java
@Test
void testExecutePerformThrowsWhenActionDisabled() {
    ComponentVisibilityProvider componentVisibilityProvider = new ComponentVisibilityProvider() {

        @Override
        public boolean isVisible(String componentName) {
            return true;
        }

        @Override
        public boolean isActionVisible(String componentName, String actionName) {
            return !"sendMessage".equals(actionName);
        }
    };

    // build the service exactly as the existing COMPONENT_DISABLED test does, with this provider

    ConfigurationException configurationException = assertThrows(
        ConfigurationException.class, () -> /* the same executePerform call the existing test makes,
            with actionName "sendMessage" */);

    assertThat(configurationException.getErrorType()).isEqualTo(ActionDefinitionErrorType.ACTION_DISABLED);
}

@Test
void testGetActionDefinitionsFiltersDisabledActions() {
    // same provider as above; call getActionDefinitions(componentName, version) against the fixture
    // component (see how the existing test seeds the registry) and assert the returned list contains
    // no ActionDefinition named "sendMessage" while other actions remain.
}
```

(The comment placeholders above refer to the fixture wiring that already exists in the test class — copy the arrange/act lines from the neighboring `COMPONENT_DISABLED` test verbatim and change only the provider and assertions.)

- [ ] **Step 2: Run to verify failure** — `ACTION_DISABLED` symbol missing → compilation failure.

- [ ] **Step 3: Implement**

`ActionDefinitionErrorType`: append

```java
public static final ActionDefinitionErrorType ACTION_DISABLED = new ActionDefinitionErrorType(107);
```

`ActionDefinitionServiceImpl`: add next to `checkComponentVisible`:

```java
private void checkActionVisible(String componentName, String actionName) {
    boolean visible = componentVisibilityProviders.stream()
        .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isActionVisible(
            componentName, actionName));

    if (!visible) {
        throw new ConfigurationException(
            "Action '%s' of component '%s' is disabled by an administrator and cannot be executed."
                .formatted(actionName, componentName),
            ActionDefinitionErrorType.ACTION_DISABLED);
    }
}
```

Call it immediately after both existing `checkComponentVisible(componentName);` call sites (lines 258 and 334 — `executePerform` and `executePerformForPolyglot`; both have `actionName` in scope).

Listing filter in `getActionDefinitions(String componentName, int componentVersion)`:

```java
return componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
    .stream()
    .filter(actionDefinition -> componentVisibilityProviders.stream()
        .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isActionVisible(
            componentName, actionDefinition.getName())))
    .map(actionDefinition -> new ActionDefinition(actionDefinition, componentName, componentVersion))
    .toList();
```

- [ ] **Step 4: Run the visibility test class + module check** (file-redirect discipline). Expected: PASS, no other visibility tests broken (the CE default keeps old behavior).

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-service
git commit -m "- component-policies Guard and filter disabled actions"
```

---

### Task 5: Trigger execution guard + listing filter

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/TriggerDefinitionServiceImpl.java` (guard helper at ~line 901, call site at line 269, listing method `getTriggerDefinitions(componentName, componentVersion)` — locate with grep)
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/exception/TriggerDefinitionErrorType.java`
- Test (extend): `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/TriggerDefinitionServiceImplVisibilityTest.java`

**Interfaces:**
- Consumes: `ComponentVisibilityProvider.isTriggerVisible(componentName, triggerName)` (Task 3).
- Produces: `TriggerDefinitionErrorType.TRIGGER_DISABLED` (key 111).

- [ ] **Step 1: Write the failing tests**

Read `TriggerDefinitionServiceImplVisibilityTest` first and follow its fixture style. Add an anonymous provider overriding `isTriggerVisible` for one trigger name (component-level `isVisible` returns true); assert the trigger-execution entry the existing `COMPONENT_DISABLED` test exercises now throws with the new error type, and that the listing filters:

```java
@Test
void testExecuteTriggerThrowsWhenTriggerDisabled() {
    ComponentVisibilityProvider componentVisibilityProvider = new ComponentVisibilityProvider() {

        @Override
        public boolean isVisible(String componentName) {
            return true;
        }

        @Override
        public boolean isTriggerVisible(String componentName, String triggerName) {
            return !"newMessage".equals(triggerName);
        }
    };

    // build the service exactly as the existing COMPONENT_DISABLED test does, with this provider

    ConfigurationException configurationException = assertThrows(
        ConfigurationException.class, () -> /* the same trigger-execution call the existing test makes,
            with triggerName "newMessage" */);

    assertThat(configurationException.getErrorType()).isEqualTo(TriggerDefinitionErrorType.TRIGGER_DISABLED);
}

@Test
void testGetTriggerDefinitionsFiltersDisabledTriggers() {
    // same provider; call getTriggerDefinitions(componentName, version) against the fixture component
    // and assert the returned list contains no TriggerDefinition named "newMessage".
}
```

(The comment placeholders refer to arrange/act lines that already exist in the test class next to the `COMPONENT_DISABLED` case — copy them verbatim and change only the provider and assertions.)

- [ ] **Step 2: Verify compilation failure** (`TRIGGER_DISABLED` symbol missing).
- [ ] **Step 3: Implement**

`TriggerDefinitionErrorType`: append

```java
public static final TriggerDefinitionErrorType TRIGGER_DISABLED = new TriggerDefinitionErrorType(111);
```

`TriggerDefinitionServiceImpl`: add next to `checkComponentVisible`:

```java
private void checkTriggerVisible(String componentName, String triggerName) {
    boolean visible = componentVisibilityProviders.stream()
        .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isTriggerVisible(
            componentName, triggerName));

    if (!visible) {
        throw new ConfigurationException(
            "Trigger '%s' of component '%s' is disabled by an administrator and cannot be executed."
                .formatted(triggerName, componentName),
            TriggerDefinitionErrorType.TRIGGER_DISABLED);
    }
}
```

Call it immediately after the existing `checkComponentVisible(componentName);` call at line 269 (`triggerName` is in scope there). Then add the listing filter to `getTriggerDefinitions(String componentName, int componentVersion)`:

```java
return componentDefinitionRegistry.getTriggerDefinitions(componentName, componentVersion)
    .stream()
    .filter(triggerDefinition -> componentVisibilityProviders.stream()
        .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isTriggerVisible(
            componentName, triggerDefinition.getName())))
    .map(triggerDefinition -> new TriggerDefinition(triggerDefinition, componentName, componentVersion))
    .toList();
```

(Match the existing method body's mapping line exactly — only the `.filter(...)` step is new.)

- [ ] **Step 4: Run tests + module check** (file-redirect discipline).
- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-service
git commit -m "- component-policies Guard and filter disabled triggers"
```

---

### Task 6: Component-detail filtering

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ComponentDefinition.java` (new copy constructor)
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImpl.java` (`getComponentDefinition(String, Integer)` at line 129)
- Test (extend): `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImplVisibilityTest.java`

**Interfaces:**
- Consumes: SPI default methods (Task 3).
- Produces: `ComponentDefinition(ComponentDefinition componentDefinition, List<ActionDefinition> actions, List<TriggerDefinition> triggers)` copy constructor on the platform domain class (public — the service package is different).

- [ ] **Step 1: Failing test** — in `ComponentDefinitionServiceImplVisibilityTest`, with a provider whose `isActionVisible` returns false for one action name and `isTriggerVisible` false for one trigger name, assert `getComponentDefinition(componentName, version)` returns a DTO whose `getActions()` / `getTriggers()` omit exactly those operations (copy the fixture wiring from the class's existing component-visibility test).
- [ ] **Step 2: Verify failure** (assertion failure — DTO currently carries all operations).
- [ ] **Step 3: Implement** — domain copy constructor assigns every field from the source DTO except `actions`/`triggers`, which come from the parameters (read the class; all fields are assigned in the existing SDK-mapping constructor — mirror that assignment list). In `ComponentDefinitionServiceImpl.getComponentDefinition`, after building `new ComponentDefinition(componentDefinition)`, filter:

```java
ComponentDefinition componentDefinitionDTO = new ComponentDefinition(componentDefinition);

List<ActionDefinition> visibleActions = componentDefinitionDTO.getActions()
    .stream()
    .filter(actionDefinition -> componentVisibilityProviders.stream()
        .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isActionVisible(
            name, actionDefinition.getName())))
    .toList();

List<TriggerDefinition> visibleTriggers = componentDefinitionDTO.getTriggers()
    .stream()
    .filter(triggerDefinition -> componentVisibilityProviders.stream()
        .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isTriggerVisible(
            name, triggerDefinition.getName())))
    .toList();

if (visibleActions.size() == componentDefinitionDTO.getActionsCount()
    && visibleTriggers.size() == componentDefinitionDTO.getTriggersCount()) {

    return componentDefinitionDTO;
}

return new ComponentDefinition(componentDefinitionDTO, visibleActions, visibleTriggers);
```

(If `getTriggersCount()` doesn't exist, compare against `getTriggers().size()` captured before filtering.) The list-view path (`getComponentDefinitions()`, index stubs) is deliberately untouched per the spec.

- [ ] **Step 4: Run the visibility test class + module check.**
- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component
git commit -m "- component-policies Filter disabled operations from the component detail DTO"
```

---

### Task 7: GraphQL surface

**Files:**
- Modify: `server/ee/libs/platform/platform-component-policy/platform-component-policy-graphql/src/main/resources/graphql/component-policy.graphqls` (locate the exact filename with `find ... -name "*.graphqls"`)
- Modify: `server/ee/libs/platform/platform-component-policy/platform-component-policy-graphql/src/main/java/com/bytechef/ee/platform/component/policy/web/graphql/ComponentPolicyGraphQlController.java`
- Test (extend): `server/ee/libs/platform/platform-component-policy/platform-component-policy-graphql/src/test/java/com/bytechef/ee/platform/component/policy/web/graphql/ComponentPolicyGraphQlControllerTest.java`

**Interfaces:**
- Consumes: Task 2's service methods.
- Produces: the GraphQL contract Task 8's client operations are written against (schema below, verbatim).

- [ ] **Step 1: Failing controller tests** — follow the existing test class's style (read it first):

```java
@Test
void testComponentOperationPoliciesReturnsDisabledRows() {
    when(componentPolicyService.getComponentOperationPolicies("slack")).thenReturn(List.of(
        new ComponentOperationPolicy("slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage")));

    List<ComponentPolicyGraphQlController.ComponentOperationPolicyItem> items =
        componentPolicyGraphQlController.componentOperationPolicies("slack");

    assertThat(items).hasSize(1);
    assertThat(items.getFirst()
        .operationType()).isEqualTo(ComponentOperationPolicy.OperationType.ACTION);
    assertThat(items.getFirst()
        .operationName()).isEqualTo("sendMessage");
}

@Test
void testUpdateComponentOperationPolicyDelegatesToService() {
    boolean result = componentPolicyGraphQlController.updateComponentOperationPolicy(
        "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);

    assertThat(result).isTrue();
    verify(componentPolicyService).updateComponentOperationPolicy(
        "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);
}
```

Also add a `@PreAuthorize` pinning assertion if the existing class pins admin gating via reflection — mirror it for both new methods.

- [ ] **Step 2: Verify compilation failure.**
- [ ] **Step 3: Implement**

Schema additions (SCREAMING_SNAKE_CASE enum per GraphQL conventions):

```graphql
enum ComponentOperationType {
    ACTION
    TRIGGER
}

type ComponentOperationPolicy {
    componentName: String!
    operationType: ComponentOperationType!
    operationName: String!
}

extend type Query {
    """
    Lists the disabled operations of a component (deny-list rows only). Admin-only.
    """
    componentOperationPolicies(componentName: String!): [ComponentOperationPolicy!]!
}

extend type Mutation {
    """
    Disables (enabled: false) or re-enables (enabled: true) a single action or trigger tenant-wide. Admin-only.
    """
    updateComponentOperationPolicy(
        componentName: String!, operationType: ComponentOperationType!, operationName: String!,
        enabled: Boolean!): Boolean!
}
```

Controller additions (Spring GraphQL binds the schema enum to the Java enum by name):

```java
@QueryMapping
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public List<ComponentOperationPolicyItem> componentOperationPolicies(@Argument String componentName) {
    return componentPolicyService.getComponentOperationPolicies(componentName)
        .stream()
        .map(componentOperationPolicy -> new ComponentOperationPolicyItem(
            componentOperationPolicy.getComponentName(), componentOperationPolicy.getOperationType(),
            componentOperationPolicy.getOperationName()))
        .toList();
}

@MutationMapping
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public boolean updateComponentOperationPolicy(
    @Argument String componentName, @Argument ComponentOperationPolicy.OperationType operationType,
    @Argument String operationName, @Argument boolean enabled) {

    componentPolicyService.updateComponentOperationPolicy(componentName, operationType, operationName, enabled);

    return true;
}

public record ComponentOperationPolicyItem(
    String componentName, ComponentOperationPolicy.OperationType operationType, String operationName) {
}
```

(If schema/Java enum name binding complains because the Java enum is nested, register it explicitly the way the module's `RuntimeWiringConfigurer`/scalar config does — check the module's config class; if none exists, Spring GraphQL default enum binding by name handles it.)

- [ ] **Step 4: Run tests + module check.**
- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-component-policy
git commit -m "- component-policies Add per-operation GraphQL query and mutation"
```

---

### Task 8: Client GraphQL operations + codegen

**Files:**
- Create: `client/src/graphql/platform/component-policy/componentOperationPolicies.graphql`
- Create: `client/src/graphql/platform/component-policy/updateComponentOperationPolicy.graphql`
- Modify (generated): `client/src/shared/middleware/graphql.ts`

**Interfaces:**
- Consumes: Task 7's schema (must be committed and, for codegen against a running schema file, present in the graphql module — `client/codegen.ts` reads the `.graphqls` files from the server tree; the component-policy schema path is already registered from the original slice, verify with `grep component-policy client/codegen.ts`).
- Produces: generated hooks `useComponentOperationPoliciesQuery`, `useUpdateComponentOperationPolicyMutation` used by Task 9.

- [ ] **Step 1: Write the operations**

`componentOperationPolicies.graphql`:

```graphql
query ComponentOperationPolicies($componentName: String!) {
    componentOperationPolicies(componentName: $componentName) {
        componentName
        operationType
        operationName
    }
}
```

`updateComponentOperationPolicy.graphql`:

```graphql
mutation UpdateComponentOperationPolicy(
    $componentName: String!
    $operationType: ComponentOperationType!
    $operationName: String!
    $enabled: Boolean!
) {
    updateComponentOperationPolicy(
        componentName: $componentName
        operationType: $operationType
        operationName: $operationName
        enabled: $enabled
    )
}
```

- [ ] **Step 2: Run codegen**

```bash
cd client && npx graphql-codegen
```

Expected: `src/shared/middleware/graphql.ts` regenerates with the two hooks. If codegen fails on the schema, verify the new `.graphqls` content compiled into the server module's build resources or add the source path to `codegen.ts`'s `schema` array the way the other component-policy path is registered.

- [ ] **Step 3: Commit operations and generated file separately** (repo convention):

```bash
git add client/src/graphql/platform/component-policy
git commit -m "- component-policies client - Add per-operation policy GraphQL operations"
git add client/src/shared/middleware/graphql.ts
git commit -m "- component-policies client - Regenerate GraphQL client"
```

---

### Task 9: Expandable rows on the Component Visibility tab

**Files:**
- Modify: `client/src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.tsx`
- Create: `client/src/ee/pages/settings/platform/component-policies/components/ComponentOperationPolicyList.tsx`
- Test (extend): `client/src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.test.tsx`

**Interfaces:**
- Consumes: Task 8's hooks; `useGetComponentDefinitionQuery` from `client/src/shared/queries/platform/componentDefinitions.queries.ts` (signature: `useGetComponentDefinitionQuery(request: {componentName: string; componentVersion?: number}, enabled?: boolean)` — verify the request type at the import site); existing `Switch`, `Collapsible` primitives.
- Produces: terminal UI task.

- [ ] **Step 1: Write the failing test**

Extend `ComponentVisibilityTab.test.tsx` (read it first — it already mocks the policy hooks; add `vi.hoisted` mocks for the two new hooks and `useGetComponentDefinitionQuery`):

```tsx
it('expands a component row and disables an action', async () => {
    // hoisted mocks: useGetComponentDefinitionQuery returns
    //   {data: {actions: [{name: 'sendMessage', title: 'Send Message'}], triggers: []}}
    // useComponentOperationPoliciesQuery returns {data: {componentOperationPolicies: []}}

    render(<ComponentVisibilityTab />);

    await userEvent.click(screen.getByLabelText('Expand slack operations'));

    const actionSwitch = await screen.findByLabelText('Send Message');

    await userEvent.click(actionSwitch);

    expect(updateComponentOperationPolicyMutateMock).toHaveBeenCalledWith(
        expect.objectContaining({
            componentName: 'slack',
            enabled: false,
            operationName: 'sendMessage',
            operationType: 'ACTION',
        }),
        expect.anything()
    );
});
```

(Adapt the exact render/fixture bootstrapping from the file's existing tests; the mock shape mirrors how the file already mocks `useComponentPoliciesQuery`.)

- [ ] **Step 2: Run it to verify failure**

```bash
cd client && npx vitest run src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.test.tsx
```

Expected: FAIL — no expand button exists.

- [ ] **Step 3: Implement**

`ComponentOperationPolicyList.tsx` — rendered inside an expanded row; fetches lazily via the `enabled` flags:

```tsx
import Switch from '@/components/Switch/Switch';
import {
    useComponentOperationPoliciesQuery,
    useUpdateComponentOperationPolicyMutation,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {twMerge} from 'tailwind-merge';

interface ComponentOperationPolicyListProps {
    componentEnabled: boolean;
    componentName: string;
    componentVersion: number;
}

const ComponentOperationPolicyList = ({
    componentEnabled,
    componentName,
    componentVersion,
}: ComponentOperationPolicyListProps) => {
    const queryClient = useQueryClient();

    const {data: componentDefinition} = useGetComponentDefinitionQuery({componentName, componentVersion});

    const {data: operationPoliciesData} = useComponentOperationPoliciesQuery({componentName});

    const updateComponentOperationPolicyMutation = useUpdateComponentOperationPolicyMutation({
        onSuccess: () =>
            queryClient.invalidateQueries({queryKey: ['ComponentOperationPolicies', {componentName}]}),
    });

    const disabledOperationKeys = new Set(
        (operationPoliciesData?.componentOperationPolicies ?? []).map(
            (operationPolicy) => `${operationPolicy.operationType}#${operationPolicy.operationName}`
        )
    );

    const renderOperations = (
        kind: 'ACTION' | 'TRIGGER',
        label: string,
        operations: {name: string; title?: string}[]
    ) =>
        operations.length > 0 && (
            <div className="flex flex-col gap-1">
                <span className="text-xs font-semibold text-muted-foreground uppercase">{label}</span>

                {operations.map((operation) => (
                    <div className="flex items-center justify-between py-1" key={operation.name}>
                        <span className={twMerge('text-sm', !componentEnabled && 'text-muted-foreground')}>
                            {operation.title || operation.name}
                        </span>

                        <Switch
                            aria-label={operation.title || operation.name}
                            checked={!disabledOperationKeys.has(`${kind}#${operation.name}`)}
                            disabled={!componentEnabled}
                            onCheckedChange={(checked) =>
                                updateComponentOperationPolicyMutation.mutate({
                                    componentName,
                                    enabled: checked,
                                    operationName: operation.name,
                                    operationType: kind,
                                })
                            }
                        />
                    </div>
                ))}
            </div>
        );

    return (
        <div className="flex flex-col gap-3 border-t px-4 py-3 pl-13">
            {renderOperations('ACTION', 'Actions', componentDefinition?.actions ?? [])}

            {renderOperations('TRIGGER', 'Triggers', componentDefinition?.triggers ?? [])}
        </div>
    );
};

export default ComponentOperationPolicyList;
```

`ComponentVisibilityTab.tsx` — wrap each `<li>` content in a `Collapsible` with a chevron trigger (`ChevronRightIcon`/`ChevronDownIcon` from lucide, `aria-label={'Expand ' + componentPolicy.name + ' operations'}`), track expansion per component in a `useState<Set<string>>`, and mount `<ComponentOperationPolicyList componentEnabled={componentPolicy.enabled} componentName={componentPolicy.name} componentVersion={componentPolicy.version} />` ONLY when expanded (that is the lazy-fetch: unmounted = no queries). Keep the existing optimistic component-toggle logic untouched.

- [ ] **Step 4: Run the test to verify pass, then the full client gate**

```bash
cd client && npx vitest run src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.test.tsx
cd client && npm run check
```

Expected: PASS / clean.

- [ ] **Step 5: Commit**

```bash
git add client/src/ee/pages/settings/platform/component-policies
git commit -m "- component-policies client - Add per-operation toggles to component rows"
```

---

## Final verification (after all tasks)

- [ ] `./gradlew :server:libs:platform:platform-component:platform-component-service:check :server:ee:libs:platform:platform-component-policy:platform-component-policy-api:check :server:ee:libs:platform:platform-component-policy:platform-component-policy-service:check :server:ee:libs:platform:platform-component-policy:platform-component-policy-graphql:check` (redirect to file, grep `^> Task .* FAILED`)
- [ ] `cd client && npm run check`
- [ ] Manual smoke against the dev stack: disable one action on an enabled component → it disappears from the workflow editor's action picker (within the 10s provider cache TTL) and a workflow already using it fails its next run with `ACTION_DISABLED`; re-enable → row deleted, action reappears.
