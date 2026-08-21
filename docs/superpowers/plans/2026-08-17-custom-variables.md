# Custom Variables Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Admin-defined string variables per workspace (automation) and per organization (embedded), per environment, referenced as `${vars.NAME}` from every workflow, with two settings pages and a Variables section in the editor's data-pill panel.

**Architecture:** One `Property` row per variable (key `variable.<NAME>`, `Scope.WORKSPACE`/workspaceId or `Scope.EMBEDDED`/null, `environment` set) behind a new EE `platform-variable` module. At job creation the resolved `name → value` map is seeded into `Job.inputs` under the reserved key `vars` (the `__triggerName` mechanism), so the flat SpEL context resolves `${vars.NAME}` with no evaluator or atlas change. CE consumers reach the EE resolver through an optional `ObjectProvider` SPI; the client reaches the EE hooks through the `shared/edition` registry.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Spring GraphQL / JUnit 5 + Mockito; React 19 + TypeScript + zustand + react-query + graphql-codegen + vitest.

**Spec:** `docs/superpowers/specs/2026-08-17-custom-variables-design.md`

## Global Constraints

- Every file under `server/ee/` carries the ByteChef Enterprise license header (copy verbatim from `IntegrationJobPrincipalAccessor.java` lines 1–6) and a `@version ee` Javadoc tag; every file under `server/libs/` carries the Apache header of its neighbours.
- Reserved job-input key: `vars` (`JobInputConstants.VARIABLES_INPUT`). Reserved as a workflow input name and a node name.
- Variable name regex `^[A-Za-z_][A-Za-z0-9_]{0,49}$`; value ≤ 4096 chars; unique per (scope, environment).
- `Property.Scope` ordinals are persisted; never reorder. `EMBEDDED` (ordinal 2) is used for the first time.
- CE code must never import an EE class; edition-specific behaviour enters CE through `ObjectProvider<…>` beans (server) and `shared/edition/*Api.ts` registries (client).
- Java style: one blank line before control statements and after a variable modification that the next statement uses; no `_`-prefixed methods; no `TODO:` comments; test method names camelCase without underscores; test class names end in `Test` (unit) / `IntTest` (integration), no `Impl`.
- Client style: `Icon`-suffixed lucide imports, `Ref`-suffixed refs, sort-keys, interfaces end in `I`/`Props`, `twMerge` not `cn`, hook order (`useState` → `useRef` → stores → custom hooks → memo/callback → `useEffect` → return).
- Commit messages: server `732 <description>`, client `732 client - <description>`, trailer `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`. Never amend; never judge a Gradle run through a pipe (redirect to a file, check `$?`, grep `^> Task .* FAILED`).
- Run `./gradlew spotlessApply` before every server commit; `cd client && npm run format && npm run check` before every client commit.
- Every EE `@Service`/`@Component`/`@Controller` created here is `@ConditionalOnEEVersion`.

---

## File map

**CE server**
- `server/libs/platform/platform-api/.../platform/workflow/JobInputConstants.java` — add `VARIABLES_INPUT`.
- `server/libs/platform/platform-api/.../platform/variable/WorkflowVariablesResolver.java` — new CE SPI.
- `server/libs/platform/platform-configuration/platform-configuration-api/.../service/PropertyService.java` — add `getPropertiesByKeyPrefix`.
- `server/libs/platform/platform-configuration/platform-configuration-service/.../repository/PropertyRepository.java` — two prefix finders.
- `.../platform-configuration-service/.../service/PropertyServiceImpl.java` — implement.
- `server/ee/libs/platform/platform-configuration/platform-configuration-remote-client/.../RemotePropertyServiceClient.java` — throwing stub.
- `.../platform-configuration-api/.../facade/WorkflowEvaluationInputsFacade.java` — new; `.../platform-configuration-service/.../facade/WorkflowEvaluationInputsFacadeImpl.java` — new; 9 facade classes rewired.
- `server/libs/automation/automation-configuration/automation-configuration-api/.../service/ProjectWorkflowService.java` — add `fetchWorkflowProjectWorkflow`; impl + EE remote stub.
- `server/libs/platform/platform-workflow/platform-workflow-validator/platform-workflow-validator-api/.../WorkflowValidatorFacade.java` — reserve `vars`.
- `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-service/.../facade/PrincipalJobFacadeImpl.java` — seed `vars`.
- `server/libs/platform/platform-workflow/platform-workflow-test/platform-workflow-test-service/.../facade/TestWorkflowExecutorImpl.java` + `config/WorkflowTestConfiguration.java` — seed `vars`.

**EE server**
- `server/ee/libs/platform/platform-variable/platform-variable-api` — `Variable`, `VariableScope`, `VariableScopeType`, `VariableService`, `VariableScopeProvider`, `VariableErrorType`, `VariableNameValidator`.
- `server/ee/libs/platform/platform-variable/platform-variable-service` — `VariableServiceImpl`, `WorkflowVariablesResolverImpl`.
- `server/ee/libs/platform/platform-variable/platform-variable-graphql` — `WorkspaceVariableGraphQlController`, `workspace-variable.graphqls`.
- `server/ee/libs/automation/automation-configuration/automation-configuration-service` — `VariablePermissionScope`, `VariablePermissionScopeProvider`, `ProjectVariableScopeProvider`.
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api` — `IntegrationWorkflowService.fetchWorkflowIntegrationWorkflow`; `-service` impl; `-remote-client` stub.
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-instance-impl` — `IntegrationVariableScopeProvider`.
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql` — `EmbeddedVariableGraphQlController`, `embedded-variable.graphqls`.

**Client**
- `client/src/graphql/platform/variable/*.graphql`, `client/codegen.ts`.
- `client/src/shared/edition/variables/variablesApi.ts`; `client/src/ee/shared/edition/registerEditionModules.ts`.
- `client/src/ee/shared/components/variables/` — `VariablesContent.tsx`, `providers/variablesProvider.tsx`, `hooks/useVariables.ts`, `stores/useVariablesStore.ts`, `components/{VariableTable,VariableDialog,VariableDeleteDialog}.tsx`.
- `client/src/ee/pages/settings/automation/variables/Variables.tsx`, `client/src/ee/pages/settings/embedded/variables/Variables.tsx`, `client/src/routes.tsx`.
- `client/src/pages/platform/workflow-editor/hooks/useWorkflowVariables.ts`, `.../components/datapills/DataPillPanelBodyVariablesItem.tsx`, `.../components/datapills/DataPillPanelBody.tsx`, `.../utils/getWorkflowInputAndVariableDataPills.ts`, three pill producers, `getDataPillIconSource.ts`, `graphNodeMutations.ts`, `useWorkflowInputs.ts`.

---

### Task 1: `PropertyService.getPropertiesByKeyPrefix`

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/repository/PropertyRepository.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/service/PropertyService.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/service/PropertyServiceImpl.java`
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-remote-client/src/main/java/com/bytechef/ee/platform/configuration/remote/client/service/RemotePropertyServiceClient.java`
- Test: `server/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/platform/configuration/service/PropertyServiceImplTest.java`

**Interfaces:**
- Produces: `List<Property> PropertyService.getPropertiesByKeyPrefix(String keyPrefix, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId)` — values populated from the row's credential store, like `getProperties`.

- [ ] **Step 1: Write the failing test** — append to `PropertyServiceImplTest` (it already has `propertyRepository`, `databaseStore`, `externalStore` mocks and a `stubStore` helper; the SUT field is `propertyService`):

```java
    @Test
    void testGetPropertiesByKeyPrefixDispatchesToScopeIdAndEnvironmentFinderAndPopulatesValues() {
        Property property = new Property();

        property.setKey("variable.API_URL");
        property.setScope(Property.Scope.WORKSPACE);
        property.setScopeId(7L);
        property.setEnabled(true);
        property.setCredentialStoreType(CredentialStoreType.DATABASE);

        when(propertyRepository.findAllByKeyStartingWithAndScopeAndScopeIdAndEnvironment(
            "variable.", Property.Scope.WORKSPACE.ordinal(), 7L, 0))
            .thenReturn(List.of(property));
        doReturn(Map.of("value", "https://api")).when(databaseStore)
            .getSecret(any());

        List<Property> properties = propertyService.getPropertiesByKeyPrefix(
            "variable.", Property.Scope.WORKSPACE, 7L, 0L);

        assertEquals(1, properties.size());
        assertEquals("https://api", properties.getFirst()
            .get("value"));
    }

    @Test
    void testGetPropertiesByKeyPrefixWithNullScopeIdUsesScopeIdIsNullFinder() {
        when(propertyRepository.findAllByKeyStartingWithAndScopeAndScopeIdIsNullAndEnvironment(
            "variable.", Property.Scope.EMBEDDED.ordinal(), 2))
            .thenReturn(List.of());

        List<Property> properties = propertyService.getPropertiesByKeyPrefix(
            "variable.", Property.Scope.EMBEDDED, null, 2L);

        assertTrue(properties.isEmpty());
        verify(propertyRepository).findAllByKeyStartingWithAndScopeAndScopeIdIsNullAndEnvironment(
            "variable.", Property.Scope.EMBEDDED.ordinal(), 2);
    }
```

Add `import static org.junit.jupiter.api.Assertions.assertTrue;` and `import java.util.List;` if missing.

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests '*PropertyServiceImplTest*' > /tmp/t1.log 2>&1; echo $?; grep -n "FAILED\|error:" /tmp/t1.log | head
```
Expected: compilation error — `getPropertiesByKeyPrefix` / finders do not exist.

- [ ] **Step 3: Add the repository finders** (`PropertyRepository`, keep alphabetical order among methods):

```java
    List<Property> findAllByKeyStartingWithAndScopeAndScopeIdAndEnvironment(
        String keyPrefix, int scope, long scopeId, int environment);

    List<Property> findAllByKeyStartingWithAndScopeAndScopeIdIsNullAndEnvironment(
        String keyPrefix, int scope, int environment);
```

- [ ] **Step 4: Add the interface method** (`PropertyService`, after the two `getProperties` overloads):

```java
    /**
     * Returns every property whose key starts with {@code keyPrefix} in the given scope and environment, values
     * populated from each row's credential store. {@code scopeId == null} matches rows whose scope id is null.
     * {@code environmentId} is required — the prefix listing exists for environment-scoped entity families (variables).
     */
    List<Property> getPropertiesByKeyPrefix(
        String keyPrefix, Scope scope, @Nullable Long scopeId, Long environmentId);
```

- [ ] **Step 5: Implement** in `PropertyServiceImpl` after `getProperties(List, Scope, Long, Long)`:

```java
    @Override
    public List<Property> getPropertiesByKeyPrefix(
        String keyPrefix, Property.Scope scope, @Nullable Long scopeId, Long environmentId) {

        if (scopeId == null) {
            return populateAll(
                propertyRepository.findAllByKeyStartingWithAndScopeAndScopeIdIsNullAndEnvironment(
                    keyPrefix, scope.ordinal(), environmentId.intValue()));
        }

        return populateAll(
            propertyRepository.findAllByKeyStartingWithAndScopeAndScopeIdAndEnvironment(
                keyPrefix, scope.ordinal(), scopeId, environmentId.intValue()));
    }
```

and in `RemotePropertyServiceClient`:

```java
    @Override
    public List<Property> getPropertiesByKeyPrefix(
        String keyPrefix, Property.Scope scope, @Nullable Long scopeId, Long environmentId) {

        throw new UnsupportedOperationException();
    }
```

- [ ] **Step 6: Run tests, then the remote-client compile**

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests '*PropertyServiceImplTest*' :server:ee:libs:platform:platform-configuration:platform-configuration-remote-client:compileJava > /tmp/t1.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t1.log
```
Expected: exit 0, no FAILED lines.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/platform/platform-configuration server/ee/libs/platform/platform-configuration/platform-configuration-remote-client
git commit -m "732 Add PropertyService.getPropertiesByKeyPrefix for environment-scoped property families

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: CE seams — `VARIABLES_INPUT`, `WorkflowVariablesResolver` SPI, reserved `vars` name

**Files:**
- Modify: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/workflow/JobInputConstants.java`
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/variable/WorkflowVariablesResolver.java`
- Modify: `server/libs/platform/platform-workflow/platform-workflow-validator/platform-workflow-validator-api/src/main/java/com/bytechef/platform/workflow/validator/WorkflowValidatorFacade.java:97-204`
- Test: `server/libs/platform/platform-workflow/platform-workflow-validator/platform-workflow-validator-api/src/test/java/com/bytechef/platform/workflow/validator/WorkflowValidatorFacadeReservedNamesTest.java` (create; if a test for `validateNoReservedInputNames` already exists in that module, add the methods there instead)

**Interfaces:**
- Produces: `JobInputConstants.VARIABLES_INPUT = "vars"`; interface `com.bytechef.platform.variable.WorkflowVariablesResolver { Map<String, String> resolveForJobPrincipal(long jobPrincipalId, PlatformType type); Map<String, String> resolveForWorkflow(String workflowId, long environmentId); }`.

- [ ] **Step 1: Write the failing validator tests**

```java
package com.bytechef.platform.workflow.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.configuration.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowValidatorFacadeReservedNamesTest {

    private final WorkflowValidatorFacade workflowValidatorFacade = new WorkflowValidatorFacade() {};

    @Test
    void testValidateNoReservedInputNamesRejectsVars() {
        String workflow = "{\"inputs\":[{\"name\":\"vars\",\"type\":\"string\"}],\"tasks\":[]}";

        assertThrows(ConfigurationException.class, () -> workflowValidatorFacade.validateNoReservedInputNames(workflow));
    }

    @Test
    void testValidateNoReservedInputNamesAllowsVarsPrefixedNames() {
        String workflow = "{\"inputs\":[{\"name\":\"varsCount\",\"type\":\"string\"}],\"tasks\":[]}";

        assertDoesNotThrow(() -> workflowValidatorFacade.validateNoReservedInputNames(workflow));
    }

    @Test
    void testValidateNoReservedNodeNamesRejectsVars() {
        String workflow = "{\"tasks\":[{\"name\":\"vars\",\"type\":\"var/v1/set\"}]}";

        assertThrows(ConfigurationException.class, () -> workflowValidatorFacade.validateNoReservedNodeNames(workflow));
    }
}
```

If `WorkflowValidatorFacade` has abstract methods that make the anonymous class fail to compile, look at how the existing validator tests instantiate it (grep `WorkflowValidatorFacade()` in that module's `src/test`) and use the same construction.

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-api:test --tests '*ReservedNamesTest*' > /tmp/t2.log 2>&1; echo $?; grep -n "FAILED\|tests completed" /tmp/t2.log | head
```
Expected: the two `RejectsVars` tests FAIL (no exception thrown).

- [ ] **Step 3: Add the constant and the SPI**

`JobInputConstants` — add after `TRIGGER_NAME_INPUT`:

```java
    /**
     * Key under which the workspace / organization variables snapshot is seeded into a job's inputs at creation time,
     * so {@code ${vars.NAME}} resolves against the flat job context. Deliberately NOT {@code __}-prefixed for
     * ergonomics, which is why {@code vars} is additionally reserved as an input name and node name by
     * {@code WorkflowValidatorFacade}. Populated only when a {@code WorkflowVariablesResolver} bean is present (EE).
     */
    public static final String VARIABLES_INPUT = "vars";
```

Update the class Javadoc's second sentence to: "Reserved keys use a double-underscore prefix, except {@link #VARIABLES_INPUT}, which is reserved by name; workflow input names matching either rule are rejected at validation time so user inputs can never collide."

`WorkflowVariablesResolver.java` (new, Apache header):

```java
package com.bytechef.platform.variable;

import com.bytechef.platform.constant.PlatformType;
import java.util.Map;

/**
 * Resolves the variables (name to string value) visible to a workflow run. Optional CE seam: implemented by the EE
 * {@code platform-variable} module; when no bean is present, CE consumers do not seed a {@code vars} job input at all.
 * Implementations MUST fail open (return an empty map, never throw) — a variable-store outage must not stop jobs.
 *
 * @author Ivica Cardic
 */
public interface WorkflowVariablesResolver {

    /**
     * Variables for a job created for the given principal (automation project deployment / embedded integration
     * instance), in that principal's environment.
     */
    Map<String, String> resolveForJobPrincipal(long jobPrincipalId, PlatformType type);

    /**
     * Variables for editor previews and test runs of the given workflow in the given environment.
     */
    Map<String, String> resolveForWorkflow(String workflowId, long environmentId);
}
```

Verify `platform-api` already depends on nothing extra for `PlatformType` (it lives in `platform-api` itself: `com.bytechef.platform.constant.PlatformType`).

- [ ] **Step 4: Reserve `vars` in the validator.** In `WorkflowValidatorFacade`:

Replace the two `startsWith("__")` conditions (lines 153–154 and 198–199) with a call to a new private helper, and update messages:

```java
    private static boolean isReservedName(String name) {
        return name.startsWith("__") || name.equals(JobInputConstants.VARIABLES_INPUT);
    }
```

Line 153: `if (nameJsonNode != null && nameJsonNode.isString() && isReservedName(nameJsonNode.asString())) {`
Line 198: same shape.

Messages:
- `validateNoReservedInputNames`: `"Workflow input names must not start with the reserved '__' prefix or equal the reserved name '" + JobInputConstants.VARIABLES_INPUT + "'. Reserved input names: " + ...`
- `validateNoReservedNodeNames`: `"Workflow node names must not start with the reserved '__' prefix or equal the reserved name '" + JobInputConstants.VARIABLES_INPUT + "'. Reserved node names: " + ...`

Add `import com.bytechef.platform.workflow.JobInputConstants;`. Confirm `platform-workflow-validator-api/build.gradle.kts` depends on `:server:libs:platform:platform-api` (grep; add `implementation(project(":server:libs:platform:platform-api"))` if absent). Update the two Javadoc blocks (lines 85–88, 109–112) to mention the by-name reservation of `vars`.

- [ ] **Step 5: Run the validator module tests**

```bash
./gradlew :server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-api:test :server:libs:platform:platform-api:compileJava > /tmp/t2.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t2.log
```
Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/platform/platform-api server/libs/platform/platform-workflow/platform-workflow-validator
git commit -m "732 Reserve the vars job input and add the WorkflowVariablesResolver seam

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: EE `platform-variable-api` module

**Files:**
- Create: `server/ee/libs/platform/platform-variable/platform-variable-api/build.gradle.kts`
- Create under `server/ee/libs/platform/platform-variable/platform-variable-api/src/main/java/com/bytechef/ee/platform/variable/`: `domain/Variable.java`, `domain/VariableScope.java`, `domain/VariableScopeType.java`, `service/VariableService.java`, `provider/VariableScopeProvider.java`, `exception/VariableErrorType.java`, `validator/VariableNameValidator.java`
- Test: `.../platform-variable-api/src/test/java/com/bytechef/ee/platform/variable/validator/VariableNameValidatorTest.java`
- Modify: `settings.gradle.kts` (add include lines next to line 781–783, alphabetical), `server/apps/server-app/build.gradle.kts` (near line 307)

**Interfaces (Produces):**
```java
record Variable(long id, String name, String value, int environmentId, @Nullable String createdBy,
    @Nullable Instant createdDate, @Nullable String lastModifiedBy, @Nullable Instant lastModifiedDate) {
    static final String KEY_PREFIX = "variable.";  }
enum VariableScopeType { WORKSPACE, EMBEDDED }
record VariableScope(VariableScopeType type, @Nullable Long workspaceId) { static workspace(long); static embedded(); }
interface VariableService {
    List<Variable> getVariables(VariableScope scope, long environmentId);
    Map<String, String> getVariableMap(VariableScope scope, long environmentId);
    Variable create(VariableScope scope, long environmentId, String name, String value);
    Variable update(VariableScope scope, long environmentId, long id, String name, String value);
    void delete(VariableScope scope, long environmentId, long id); }
interface VariableScopeProvider { PlatformType getType(); Optional<VariableScope> getVariableScope(long jobPrincipalId);
    Optional<VariableScope> getVariableScopeByWorkflowId(String workflowId); }
class VariableErrorType extends AbstractErrorType { VARIABLE_NAME_INVALID(100), VARIABLE_VALUE_TOO_LONG(101),
    VARIABLE_NAME_ALREADY_EXISTS(102), VARIABLE_NOT_FOUND(103) }
final class VariableNameValidator { static void validate(String name, String value) throws ConfigurationException; }
```

- [ ] **Step 1: Register the three modules and write build files**

`settings.gradle.kts` — insert (keep the file's alphabetical grouping; `platform-variable` sorts after `platform-user`):
```kotlin
include("server:ee:libs:platform:platform-variable:platform-variable-api")
include("server:ee:libs:platform:platform-variable:platform-variable-graphql")
include("server:ee:libs:platform:platform-variable:platform-variable-service")
```

`platform-variable-api/build.gradle.kts`:
```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
```
Verify the exception module path with `grep -n "exception" settings.gradle.kts | head` — `ConfigurationException` lives in `com.bytechef.platform.configuration.exception` (platform-configuration-api) and `AbstractErrorType` in `com.bytechef.exception` (`server/libs/core/exception/exception-api`); adjust the coordinate to whatever `settings.gradle.kts` lists.

Also create empty `platform-variable-service/build.gradle.kts` and `platform-variable-graphql/build.gradle.kts` now with just `dependencies { }` so Gradle configuration succeeds; Tasks 4 and 8 fill them.

`server/apps/server-app/build.gradle.kts` — add after line 307:
```kotlin
    implementation(project(":server:ee:libs:platform:platform-variable:platform-variable-graphql"))
    implementation(project(":server:ee:libs:platform:platform-variable:platform-variable-service"))
```

- [ ] **Step 2: Write the failing validator test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class VariableNameValidatorTest {

    @Test
    void testValidNamesPass() {
        assertThatCode(() -> VariableNameValidator.validate("API_URL", "x")).doesNotThrowAnyException();
        assertThatCode(() -> VariableNameValidator.validate("_private", "x")).doesNotThrowAnyException();
        assertThatCode(() -> VariableNameValidator.validate("a".repeat(50), "x")).doesNotThrowAnyException();
        assertThatCode(() -> VariableNameValidator.validate("A", "")).doesNotThrowAnyException();
    }

    @Test
    void testInvalidNamesAreRejected() {
        for (String name : new String[] {
            "", " ", "1abc", "api-url", "api url", "naïve", "a".repeat(51)
        }) {
            assertThatThrownBy(() -> VariableNameValidator.validate(name, "x"))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorType")
                .isEqualTo(VariableErrorType.VARIABLE_NAME_INVALID);
        }
    }

    @Test
    void testTooLongValueIsRejected() {
        assertThatThrownBy(() -> VariableNameValidator.validate("OK", "v".repeat(4097)))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(VariableErrorType.VARIABLE_VALUE_TOO_LONG);
        assertThatCode(() -> VariableNameValidator.validate("OK", "v".repeat(4096))).doesNotThrowAnyException();
    }
}
```
Check `ConfigurationException`'s accessor name for the error type (`grep -n "getErrorType\|errorType" server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/exception/ConfigurationException.java`) and adjust `.extracting("errorType")` to match.

- [ ] **Step 3: Run to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-variable:platform-variable-api:test > /tmp/t3.log 2>&1; echo $?; grep -n "FAILED\|error:" /tmp/t3.log | head
```
Expected: compilation error.

- [ ] **Step 4: Write the API types** (all with the EE header + `@version ee` Javadoc)

`domain/VariableScopeType.java`:
```java
package com.bytechef.ee.platform.variable.domain;

/**
 * Where a set of variables lives: an automation workspace or the embedded organization (the tenant).
 *
 * @version ee
 */
public enum VariableScopeType {

    WORKSPACE, EMBEDDED
}
```

`domain/VariableScope.java`:
```java
package com.bytechef.ee.platform.variable.domain;

import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Identifies one variable set. {@code workspaceId} is non-null iff {@code type == WORKSPACE}.
 *
 * @version ee
 */
public record VariableScope(VariableScopeType type, @Nullable Long workspaceId) {

    public VariableScope {
        Validate.notNull(type, "type");

        if (type == VariableScopeType.WORKSPACE) {
            Validate.notNull(workspaceId, "workspaceId");
        } else {
            Validate.isTrue(workspaceId == null, "workspaceId must be null for scope type %s", type);
        }
    }

    public static VariableScope workspace(long workspaceId) {
        return new VariableScope(VariableScopeType.WORKSPACE, workspaceId);
    }

    public static VariableScope embedded() {
        return new VariableScope(VariableScopeType.EMBEDDED, null);
    }
}
```
(add `implementation("org.apache.commons:commons-lang3")` to the api build file.)

`domain/Variable.java`:
```java
package com.bytechef.ee.platform.variable.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A named string value defined once per {@link VariableScope} and environment, referenced from workflows as
 * {@code ${vars.<name>}}. Backed by one {@code property} row whose key is {@link #KEY_PREFIX} + name.
 *
 * @version ee
 */
public record Variable(
    long id, String name, String value, int environmentId, @Nullable String createdBy, @Nullable Instant createdDate,
    @Nullable String lastModifiedBy, @Nullable Instant lastModifiedDate) {

    public static final String KEY_PREFIX = "variable.";
    public static final String VALUE_KEY = "value";
}
```

`exception/VariableErrorType.java`:
```java
package com.bytechef.ee.platform.variable.exception;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 */
public class VariableErrorType extends AbstractErrorType {

    public static final VariableErrorType VARIABLE_NAME_INVALID = new VariableErrorType(100);
    public static final VariableErrorType VARIABLE_VALUE_TOO_LONG = new VariableErrorType(101);
    public static final VariableErrorType VARIABLE_NAME_ALREADY_EXISTS = new VariableErrorType(102);
    public static final VariableErrorType VARIABLE_NOT_FOUND = new VariableErrorType(103);

    public VariableErrorType(int errorKey) {
        super(Variable.class, errorKey);
    }
}
```

`validator/VariableNameValidator.java`:
```java
package com.bytechef.ee.platform.variable.validator;

import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import java.util.regex.Pattern;

/**
 * Static, unconditional validation of a variable's name and value — deliberately not a Spring bean so it cannot be
 * silently disabled by a conditional; invoked by {@code VariableServiceImpl} on every create/update.
 *
 * <p>
 * Names are identifiers ({@code ^[A-Za-z_][A-Za-z0-9_]{0,49}$}): a leading digit is rejected because
 * {@code ${vars.1abc}} is not a valid SpEL property path. Values are capped at {@link #MAX_VALUE_LENGTH} characters.
 *
 * @version ee
 */
public final class VariableNameValidator {

    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_VALUE_LENGTH = 4096;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0," + (MAX_NAME_LENGTH - 1) + "}$");

    private VariableNameValidator() {
    }

    public static void validate(String name, String value) {
        if (name == null || !NAME_PATTERN.matcher(name)
            .matches()) {

            throw new ConfigurationException(
                "Variable name must match [A-Za-z_][A-Za-z0-9_]* and be at most " + MAX_NAME_LENGTH +
                    " characters: '" + name + "'",
                VariableErrorType.VARIABLE_NAME_INVALID);
        }

        if (value != null && value.length() > MAX_VALUE_LENGTH) {
            throw new ConfigurationException(
                "Variable value must be at most " + MAX_VALUE_LENGTH + " characters",
                VariableErrorType.VARIABLE_VALUE_TOO_LONG);
        }
    }
}
```

`service/VariableService.java`:
```java
package com.bytechef.ee.platform.variable.service;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import java.util.List;
import java.util.Map;

/**
 * CRUD over the variables of one scope + environment. Not authorization-aware: the GraphQL controllers guard the
 * admin surface, and the runtime resolver calls it with no security context.
 *
 * @version ee
 */
public interface VariableService {

    Variable create(VariableScope scope, long environmentId, String name, String value);

    void delete(VariableScope scope, long environmentId, long id);

    /** Name to value, for seeding the {@code vars} job input. Never null. */
    Map<String, String> getVariableMap(VariableScope scope, long environmentId);

    List<Variable> getVariables(VariableScope scope, long environmentId);

    Variable update(VariableScope scope, long environmentId, long id, String name, String value);
}
```

`provider/VariableScopeProvider.java`:
```java
package com.bytechef.ee.platform.variable.provider;

import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;

/**
 * Per-{@link PlatformType} SPI that maps a job principal or a workflow id onto the {@link VariableScope} whose variables
 * it should see. Contributed by the automation and embedded configuration modules so that {@code platform-variable}
 * depends on neither.
 *
 * @version ee
 */
public interface VariableScopeProvider {

    PlatformType getType();

    /** Scope for a job created for {@code jobPrincipalId} (project deployment id / integration instance id). */
    Optional<VariableScope> getVariableScope(long jobPrincipalId);

    /** Scope for editor previews / test runs of {@code workflowId}; empty when this provider does not own it. */
    Optional<VariableScope> getVariableScopeByWorkflowId(String workflowId);
}
```

- [ ] **Step 5: Run the test**

```bash
./gradlew :server:ee:libs:platform:platform-variable:platform-variable-api:test > /tmp/t3.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t3.log
```
Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add settings.gradle.kts server/apps/server-app/build.gradle.kts server/ee/libs/platform/platform-variable
git commit -m "732 Add platform-variable-api domain, service contract and scope provider SPI

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: `VariableServiceImpl` over `PropertyService`

**Files:**
- Modify: `server/ee/libs/platform/platform-variable/platform-variable-service/build.gradle.kts`
- Create: `server/ee/libs/platform/platform-variable/platform-variable-service/src/main/java/com/bytechef/ee/platform/variable/service/VariableServiceImpl.java`
- Test: `.../platform-variable-service/src/test/java/com/bytechef/ee/platform/variable/service/VariableServiceTest.java`

**Interfaces:**
- Consumes: `PropertyService` (Task 1), `Variable`/`VariableScope`/`VariableNameValidator`/`VariableErrorType` (Task 3).
- Produces: `@Service VariableServiceImpl implements VariableService`.

- [ ] **Step 1: build file**

```kotlin
dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    api(project(":server:ee:libs:platform:platform-variable:platform-variable-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
```

- [ ] **Step 2: Write the failing tests**

```java
package com.bytechef.ee.platform.variable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class VariableServiceTest {

    private PropertyService propertyService;
    private VariableServiceImpl variableService;

    @BeforeEach
    void beforeEach() {
        propertyService = mock(PropertyService.class);
        variableService = new VariableServiceImpl(propertyService);
    }

    @Test
    void testGetVariablesMapsWorkspaceScopeRows() {
        Property property = property(11L, "variable.API_URL", "https://api", Property.Scope.WORKSPACE, 7L, 1);

        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(property));

        List<Variable> variables = variableService.getVariables(VariableScope.workspace(7L), 1L);

        assertThat(variables).hasSize(1);
        assertThat(variables.getFirst()
            .name()).isEqualTo("API_URL");
        assertThat(variables.getFirst()
            .value()).isEqualTo("https://api");
        assertThat(variables.getFirst()
            .id()).isEqualTo(11L);
    }

    @Test
    void testGetVariableMapUsesEmbeddedScopeWithNullScopeId() {
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.EMBEDDED, null, 2L))
            .thenReturn(List.of(property(1L, "variable.REGION", "eu", Property.Scope.EMBEDDED, null, 2)));

        Map<String, String> map = variableService.getVariableMap(VariableScope.embedded(), 2L);

        assertThat(map).containsExactly(Map.entry("REGION", "eu"));
    }

    @Test
    void testCreateSavesPrefixedKeyAndRejectsDuplicates() {
        when(propertyService.fetchProperty("variable.API_URL", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(Optional.empty(), Optional.of(
                property(5L, "variable.API_URL", "https://api", Property.Scope.WORKSPACE, 7L, 1)));

        Variable created = variableService.create(VariableScope.workspace(7L), 1L, "API_URL", "https://api");

        verify(propertyService).save("variable.API_URL", Map.of("value", "https://api"), Property.Scope.WORKSPACE, 7L, 1L);
        assertThat(created.id()).isEqualTo(5L);

        assertThatThrownBy(() -> variableService.create(VariableScope.workspace(7L), 1L, "API_URL", "x"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS);
    }

    @Test
    void testCreateRejectsInvalidName() {
        assertThatThrownBy(() -> variableService.create(VariableScope.workspace(7L), 1L, "1bad", "x"))
            .isInstanceOf(ConfigurationException.class);

        verify(propertyService, never()).save(eq("variable.1bad"), eq(Map.of("value", "x")), eq(Property.Scope.WORKSPACE),
            eq(7L), eq(1L));
    }

    @Test
    void testUpdateRenamesByDeletingOldKeyAndSavingNew() {
        Property existing = property(5L, "variable.OLD", "a", Property.Scope.WORKSPACE, 7L, 1);

        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(existing));
        when(propertyService.fetchProperty("variable.NEW", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(Optional.empty(), Optional.of(property(9L, "variable.NEW", "b", Property.Scope.WORKSPACE, 7L, 1)));

        Variable updated = variableService.update(VariableScope.workspace(7L), 1L, 5L, "NEW", "b");

        verify(propertyService).delete("variable.OLD", Property.Scope.WORKSPACE, 7L, 1L);
        verify(propertyService).save("variable.NEW", Map.of("value", "b"), Property.Scope.WORKSPACE, 7L, 1L);
        assertThat(updated.name()).isEqualTo("NEW");
    }

    @Test
    void testUpdateWithSameNameOnlySaves() {
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(property(5L, "variable.SAME", "a", Property.Scope.WORKSPACE, 7L, 1)));
        when(propertyService.fetchProperty("variable.SAME", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(Optional.of(property(5L, "variable.SAME", "b", Property.Scope.WORKSPACE, 7L, 1)));

        variableService.update(VariableScope.workspace(7L), 1L, 5L, "SAME", "b");

        verify(propertyService, never()).delete(eq("variable.SAME"), eq(Property.Scope.WORKSPACE), eq(7L), eq(1L));
        verify(propertyService).save("variable.SAME", Map.of("value", "b"), Property.Scope.WORKSPACE, 7L, 1L);
    }

    @Test
    void testUpdateAndDeleteRejectIdOutsideScope() {
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of());

        assertThatThrownBy(() -> variableService.update(VariableScope.workspace(7L), 1L, 99L, "X", "y"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(VariableErrorType.VARIABLE_NOT_FOUND);
        assertThatThrownBy(() -> variableService.delete(VariableScope.workspace(7L), 1L, 99L))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(VariableErrorType.VARIABLE_NOT_FOUND);
    }

    @Test
    void testDeleteRemovesRow() {
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(property(5L, "variable.GONE", "a", Property.Scope.WORKSPACE, 7L, 1)));

        variableService.delete(VariableScope.workspace(7L), 1L, 5L);

        verify(propertyService).delete("variable.GONE", Property.Scope.WORKSPACE, 7L, 1L);
    }

    private static Property property(
        long id, String key, String value, Property.Scope scope, Long scopeId, int environment) {

        Property property = new Property();

        property.setId(id);
        property.setKey(key);
        property.setScope(scope);
        property.setScopeId(scopeId);
        property.setEnvironment(environment);
        property.setEnabled(true);
        property.setValue(Map.of("value", value));

        return property;
    }
}
```
Check `Property` setter names (`setId`, `setScopeId`, `setEnvironment(Integer)`) against `Property.java` and adjust.

- [ ] **Step 3: Run to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-variable:platform-variable-service:test > /tmp/t4.log 2>&1; echo $?; grep -n "error:" /tmp/t4.log | head -3
```
Expected: compilation error (no `VariableServiceImpl`).

- [ ] **Step 4: Implement**

```java
package com.bytechef.ee.platform.variable.service;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.domain.VariableScopeType;
import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.ee.platform.variable.validator.VariableNameValidator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One {@link Property} row per variable: key {@code variable.<name>}, value map {@code {"value": <string>}},
 * {@code Scope.WORKSPACE}/workspaceId or {@code Scope.EMBEDDED}/null, environment always set. Ids are property row ids;
 * every by-id operation re-lists the scope so an id from another scope is indistinguishable from a missing one.
 *
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class VariableServiceImpl implements VariableService {

    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public VariableServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public Variable create(VariableScope scope, long environmentId, String name, String value) {
        VariableNameValidator.validate(name, value);

        String key = Variable.KEY_PREFIX + name;

        if (propertyService.fetchProperty(key, toPropertyScope(scope), scope.workspaceId(), environmentId)
            .isPresent()) {

            throw new ConfigurationException(
                "Variable '" + name + "' already exists", VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS);
        }

        propertyService.save(
            key, Map.of(Variable.VALUE_KEY, value), toPropertyScope(scope), scope.workspaceId(), environmentId);

        return toVariable(propertyService.getProperty(key, toPropertyScope(scope), scope.workspaceId(), environmentId));
    }

    @Override
    public void delete(VariableScope scope, long environmentId, long id) {
        Property property = getProperty(scope, environmentId, id);

        propertyService.delete(property.getKey(), toPropertyScope(scope), scope.workspaceId(), environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getVariableMap(VariableScope scope, long environmentId) {
        Map<String, String> variableMap = new LinkedHashMap<>();

        for (Variable variable : getVariables(scope, environmentId)) {
            variableMap.put(variable.name(), variable.value());
        }

        return variableMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Variable> getVariables(VariableScope scope, long environmentId) {
        return propertyService.getPropertiesByKeyPrefix(
            Variable.KEY_PREFIX, toPropertyScope(scope), scope.workspaceId(), environmentId)
            .stream()
            .map(VariableServiceImpl::toVariable)
            .sorted((v1, v2) -> v1.name()
                .compareToIgnoreCase(v2.name()))
            .toList();
    }

    @Override
    public Variable update(VariableScope scope, long environmentId, long id, String name, String value) {
        VariableNameValidator.validate(name, value);

        Property existing = getProperty(scope, environmentId, id);

        String newKey = Variable.KEY_PREFIX + name;

        if (!Objects.equals(existing.getKey(), newKey)) {
            if (propertyService.fetchProperty(newKey, toPropertyScope(scope), scope.workspaceId(), environmentId)
                .isPresent()) {

                throw new ConfigurationException(
                    "Variable '" + name + "' already exists", VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS);
            }

            propertyService.delete(existing.getKey(), toPropertyScope(scope), scope.workspaceId(), environmentId);
        }

        propertyService.save(
            newKey, Map.of(Variable.VALUE_KEY, value), toPropertyScope(scope), scope.workspaceId(), environmentId);

        return toVariable(
            propertyService.getProperty(newKey, toPropertyScope(scope), scope.workspaceId(), environmentId));
    }

    private Property getProperty(VariableScope scope, long environmentId, long id) {
        return propertyService.getPropertiesByKeyPrefix(
            Variable.KEY_PREFIX, toPropertyScope(scope), scope.workspaceId(), environmentId)
            .stream()
            .filter(property -> Objects.equals(property.getId(), id))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "Variable not found: " + id, VariableErrorType.VARIABLE_NOT_FOUND));
    }

    private static Property.Scope toPropertyScope(VariableScope scope) {
        return scope.type() == VariableScopeType.WORKSPACE ? Property.Scope.WORKSPACE : Property.Scope.EMBEDDED;
    }

    private static Variable toVariable(Property property) {
        String key = property.getKey();
        @Nullable
        Object value = property.get(Variable.VALUE_KEY);

        return new Variable(
            Objects.requireNonNull(property.getId(), "id"), key.substring(Variable.KEY_PREFIX.length()),
            value == null ? "" : String.valueOf(value), Objects.requireNonNull(property.getEnvironment(), "environment"),
            property.getCreatedBy(), property.getCreatedDate(), property.getLastModifiedBy(),
            property.getLastModifiedDate());
    }
}
```
Confirm `Property.getEnvironment()` returns `Integer` and `getId()` returns `Long`; adjust unboxing accordingly.

- [ ] **Step 5: Run tests**

```bash
./gradlew :server:ee:libs:platform:platform-variable:platform-variable-service:test > /tmp/t4.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t4.log
```
Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/ee/libs/platform/platform-variable
git commit -m "732 Add VariableServiceImpl storing one property row per variable

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: Scope providers (automation + embedded) and permission scopes

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectWorkflowService.java` (+ `ProjectWorkflowServiceImpl`, `RemoteProjectWorkflowServiceClient`)
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationWorkflowService.java` (+ `IntegrationWorkflowServiceImpl`, `RemoteIntegrationWorkflowServiceClient`)
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/security/scope/VariablePermissionScope.java`, `VariablePermissionScopeProvider.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/variable/ProjectVariableScopeProvider.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-instance-impl/src/main/java/com/bytechef/ee/embedded/configuration/instance/variable/IntegrationVariableScopeProvider.java`
- Modify: both modules' `build.gradle.kts` (add `implementation(project(":server:ee:libs:platform:platform-variable:platform-variable-api"))`; the embedded module also needs nothing else — it already has `embedded-configuration-api`)
- Tests: `.../ee/automation/configuration/variable/ProjectVariableScopeProviderTest.java`, `.../ee/embedded/configuration/instance/variable/IntegrationVariableScopeProviderTest.java`

**Interfaces:**
- Consumes: `VariableScopeProvider`, `VariableScope` (Task 3).
- Produces: `Optional<ProjectWorkflow> ProjectWorkflowService.fetchWorkflowProjectWorkflow(String workflowId)`; `Optional<IntegrationWorkflow> IntegrationWorkflowService.fetchWorkflowIntegrationWorkflow(String workflowId)`; two `@Component @ConditionalOnEEVersion` providers; scopes `VARIABLE_VIEW` (VIEWER) and `VARIABLE_MANAGE` (ADMIN).

- [ ] **Step 1: Non-throwing workflow lookups**

`ProjectWorkflowService` — add next to `fetchProjectWorkflow(long, int, String)`:
```java
    /**
     * Non-throwing sibling of {@link #getWorkflowProjectWorkflow(String)}: an empty result means the id is not a
     * project workflow, letting a caller fall through to another resolution path without a rollback-poisoning
     * exception. The throwing method is unchanged for its existing callers.
     */
    Optional<ProjectWorkflow> fetchWorkflowProjectWorkflow(String workflowId);
```
`ProjectWorkflowServiceImpl`:
```java
    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectWorkflow> fetchWorkflowProjectWorkflow(String workflowId) {
        return projectWorkflowRepository.findByWorkflowId(workflowId);
    }
```
`RemoteProjectWorkflowServiceClient`: `throw new UnsupportedOperationException();` override.

`IntegrationWorkflowService` — add the same-shaped `Optional<IntegrationWorkflow> fetchWorkflowIntegrationWorkflow(String workflowId);` with the same Javadoc, impl `return integrationWorkflowRepository.findByWorkflowId(workflowId);` in `IntegrationWorkflowServiceImpl`, throwing stub in `RemoteIntegrationWorkflowServiceClient`.

- [ ] **Step 2: Write the failing provider tests**

`ProjectVariableScopeProviderTest`:
```java
package com.bytechef.ee.automation.configuration.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ProjectVariableScopeProviderTest {

    private ProjectDeploymentService projectDeploymentService;
    private ProjectService projectService;
    private ProjectWorkflowService projectWorkflowService;
    private ProjectVariableScopeProvider provider;

    @BeforeEach
    void beforeEach() {
        projectDeploymentService = mock(ProjectDeploymentService.class);
        projectService = mock(ProjectService.class);
        projectWorkflowService = mock(ProjectWorkflowService.class);
        provider = new ProjectVariableScopeProvider(projectDeploymentService, projectService, projectWorkflowService);
    }

    @Test
    void testTypeIsAutomation() {
        assertThat(provider.getType()).isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testScopeForJobPrincipalWalksDeploymentToProjectWorkspace() {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);
        Project project = mock(Project.class);

        when(projectDeploymentService.getProjectDeployment(42L)).thenReturn(projectDeployment);
        when(projectDeployment.getProjectId()).thenReturn(3L);
        when(projectService.getProject(3L)).thenReturn(project);
        when(project.getWorkspaceId()).thenReturn(7L);

        assertThat(provider.getVariableScope(42L)).contains(VariableScope.workspace(7L));
    }

    @Test
    void testScopeForJobPrincipalIsEmptyWhenProjectHasNoWorkspace() {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);
        Project project = mock(Project.class);

        when(projectDeploymentService.getProjectDeployment(42L)).thenReturn(projectDeployment);
        when(projectDeployment.getProjectId()).thenReturn(3L);
        when(projectService.getProject(3L)).thenReturn(project);
        when(project.getWorkspaceId()).thenReturn(null);

        assertThat(provider.getVariableScope(42L)).isEmpty();
    }

    @Test
    void testScopeByWorkflowIdIsEmptyForNonProjectWorkflow() {
        when(projectWorkflowService.fetchWorkflowProjectWorkflow("wf-1")).thenReturn(Optional.empty());

        assertThat(provider.getVariableScopeByWorkflowId("wf-1")).isEmpty();
    }

    @Test
    void testScopeByWorkflowIdResolvesProjectWorkspace() {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);
        Project project = mock(Project.class);

        when(projectWorkflowService.fetchWorkflowProjectWorkflow("wf-1")).thenReturn(Optional.of(projectWorkflow));
        when(projectWorkflow.getProjectId()).thenReturn(3L);
        when(projectService.getProject(3L)).thenReturn(project);
        when(project.getWorkspaceId()).thenReturn(7L);

        assertThat(provider.getVariableScopeByWorkflowId("wf-1")).contains(VariableScope.workspace(7L));
    }
}
```
`Project` is `final` (per `Project.java:50`) — Mockito inline mock maker (Mockito 5 default) handles it; if the module's Mockito setup does not, build a real `Project` via its builder/setters instead (`grep -n "setWorkspaceId\|builder()" Project.java`). Note `Mockito` returns `0L` for an unstubbed `Long`, so `thenReturn(null)` is explicit above (CLAUDE.md gotcha).

`IntegrationVariableScopeProviderTest`:
```java
package com.bytechef.ee.embedded.configuration.instance.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class IntegrationVariableScopeProviderTest {

    private final IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
    private final IntegrationVariableScopeProvider provider = new IntegrationVariableScopeProvider(
        integrationWorkflowService);

    @Test
    void testTypeIsEmbedded() {
        assertThat(provider.getType()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testJobPrincipalAlwaysMapsToEmbeddedScope() {
        assertThat(provider.getVariableScope(123L)).contains(VariableScope.embedded());
    }

    @Test
    void testWorkflowIdMapsToEmbeddedScopeOnlyWhenIntegrationWorkflowExists() {
        when(integrationWorkflowService.fetchWorkflowIntegrationWorkflow("wf-e")).thenReturn(
            Optional.of(mock(IntegrationWorkflow.class)));
        when(integrationWorkflowService.fetchWorkflowIntegrationWorkflow("wf-a")).thenReturn(Optional.empty());

        assertThat(provider.getVariableScopeByWorkflowId("wf-e")).contains(VariableScope.embedded());
        assertThat(provider.getVariableScopeByWorkflowId("wf-a")).isEmpty();
    }
}
```
Add `testImplementation("org.assertj:assertj-core")`, `testImplementation("org.mockito:mockito-core")`, `testImplementation("org.junit.jupiter:junit-jupiter")` to `embedded-configuration-instance-impl/build.gradle.kts` if absent.

- [ ] **Step 3: Run to verify failure**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:compileTestJava :server:ee:libs:embedded:embedded-configuration:embedded-configuration-instance-impl:compileTestJava > /tmp/t5.log 2>&1; echo $?
```
Expected: non-zero (classes missing).

- [ ] **Step 4: Implement the providers and scopes**

`ProjectVariableScopeProvider`:
```java
package com.bytechef.ee.automation.configuration.variable;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.provider.VariableScopeProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps automation job principals (project deployments) and project workflows to their workspace's variable scope. A
 * project without a workspace yields no scope, hence no variables. Embedded's automation-bridge catalog projects carry
 * {@code Workspace.DEFAULT_WORKSPACE_ID} and therefore read the default workspace's variables.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class ProjectVariableScopeProvider implements VariableScopeProvider {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectVariableScopeProvider(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.AUTOMATION;
    }

    @Override
    public Optional<VariableScope> getVariableScope(long jobPrincipalId) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);

        return workspaceScope(projectDeployment.getProjectId());
    }

    @Override
    public Optional<VariableScope> getVariableScopeByWorkflowId(String workflowId) {
        return projectWorkflowService.fetchWorkflowProjectWorkflow(workflowId)
            .flatMap(projectWorkflow -> workspaceScope(projectWorkflow.getProjectId()));
    }

    private Optional<VariableScope> workspaceScope(long projectId) {
        Project project = projectService.getProject(projectId);

        Long workspaceId = project.getWorkspaceId();

        if (workspaceId == null) {
            return Optional.empty();
        }

        return Optional.of(VariableScope.workspace(workspaceId));
    }
}
```
Verify `ProjectDeployment.getProjectId()` returns `long`/`Long` and adapt.

`IntegrationVariableScopeProvider`:
```java
package com.bytechef.ee.embedded.configuration.instance.variable;

import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.provider.VariableScopeProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Embedded is tenant-scoped: every integration-instance job and every integration workflow reads the single embedded
 * (organization) variable set. The by-workflow lookup still checks ownership so an automation workflow id never falls
 * into the embedded scope.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class IntegrationVariableScopeProvider implements VariableScopeProvider {

    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationVariableScopeProvider(IntegrationWorkflowService integrationWorkflowService) {
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.EMBEDDED;
    }

    @Override
    public Optional<VariableScope> getVariableScope(long jobPrincipalId) {
        return Optional.of(VariableScope.embedded());
    }

    @Override
    public Optional<VariableScope> getVariableScopeByWorkflowId(String workflowId) {
        return integrationWorkflowService.fetchWorkflowIntegrationWorkflow(workflowId)
            .map(integrationWorkflow -> VariableScope.embedded());
    }
}
```

`VariablePermissionScope` (same shape as `AiGatewayPermissionScope`):
```java
public enum VariablePermissionScope implements PermissionScopeType {

    VARIABLE_VIEW,
    VARIABLE_MANAGE
}
```
`VariablePermissionScopeProvider` (`@Component`, EE header, `@version ee`):
```java
    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(VariablePermissionScope.VARIABLE_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(VariablePermissionScope.VARIABLE_MANAGE, WorkspaceRole.ADMIN));
    }
```

- [ ] **Step 5: Run the module tests (including `PermissionScopeRegistryTest`, which may pin the scope catalogue — update its expectations if it enumerates scopes)**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava :server:ee:libs:automation:automation-configuration:automation-configuration-remote-client:compileJava :server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client:compileJava :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectVariableScopeProviderTest*' --tests '*PermissionScopeRegistryTest*' :server:ee:libs:embedded:embedded-configuration:embedded-configuration-instance-impl:test > /tmp/t5.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t5.log
```
Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/automation/automation-configuration server/ee/libs/automation/automation-configuration server/ee/libs/embedded/embedded-configuration
git commit -m "732 Add variable scope providers for automation and embedded plus VARIABLE_* permission scopes

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: `WorkflowVariablesResolverImpl` (EE, fail-open)

**Files:**
- Create: `server/ee/libs/platform/platform-variable/platform-variable-service/src/main/java/com/bytechef/ee/platform/variable/resolver/WorkflowVariablesResolverImpl.java`
- Test: `.../platform-variable-service/src/test/java/com/bytechef/ee/platform/variable/resolver/WorkflowVariablesResolverTest.java`

**Interfaces:**
- Consumes: `WorkflowVariablesResolver` (Task 2), `VariableService` (Task 4), `VariableScopeProvider` (Task 3/5), `JobPrincipalAccessorRegistry.getJobPrincipalAccessor(type).getEnvironmentId(jobPrincipalId)`.
- Produces: `@Component @ConditionalOnEEVersion WorkflowVariablesResolverImpl implements WorkflowVariablesResolver`.

- [ ] **Step 1: Failing tests**

```java
package com.bytechef.ee.platform.variable.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.provider.VariableScopeProvider;
import com.bytechef.ee.platform.variable.service.VariableService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class WorkflowVariablesResolverTest {

    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private VariableScopeProvider automationProvider;
    private VariableService variableService;
    private WorkflowVariablesResolverImpl resolver;

    @BeforeEach
    void beforeEach() {
        jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
        automationProvider = mock(VariableScopeProvider.class);
        variableService = mock(VariableService.class);

        when(automationProvider.getType()).thenReturn(PlatformType.AUTOMATION);

        resolver = new WorkflowVariablesResolverImpl(
            jobPrincipalAccessorRegistry, variableService, List.of(automationProvider));
    }

    @Test
    void testResolveForJobPrincipalUsesProviderScopeAndAccessorEnvironment() {
        JobPrincipalAccessor accessor = mock(JobPrincipalAccessor.class);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION)).thenReturn(accessor);
        when(accessor.getEnvironmentId(42L)).thenReturn(2L);
        when(automationProvider.getVariableScope(42L)).thenReturn(Optional.of(VariableScope.workspace(7L)));
        when(variableService.getVariableMap(VariableScope.workspace(7L), 2L)).thenReturn(Map.of("A", "1"));

        assertThat(resolver.resolveForJobPrincipal(42L, PlatformType.AUTOMATION)).containsExactly(Map.entry("A", "1"));
    }

    @Test
    void testResolveForJobPrincipalIsEmptyWithoutProviderForType() {
        assertThat(resolver.resolveForJobPrincipal(42L, PlatformType.EMBEDDED)).isEmpty();
    }

    @Test
    void testResolveForWorkflowAsksEveryProvider() {
        when(automationProvider.getVariableScopeByWorkflowId("wf")).thenReturn(Optional.of(VariableScope.workspace(7L)));
        when(variableService.getVariableMap(VariableScope.workspace(7L), 0L)).thenReturn(Map.of("B", "2"));

        assertThat(resolver.resolveForWorkflow("wf", 0L)).containsExactly(Map.entry("B", "2"));
    }

    @Test
    void testResolverFailsOpenOnException() {
        when(automationProvider.getVariableScopeByWorkflowId("wf")).thenThrow(new UnsupportedOperationException());

        assertThat(resolver.resolveForWorkflow("wf", 0L)).isEmpty();
    }
}
```

- [ ] **Step 2: Run, expect compilation failure**

```bash
./gradlew :server:ee:libs:platform:platform-variable:platform-variable-service:test --tests '*WorkflowVariablesResolverTest*' > /tmp/t6.log 2>&1; echo $?
```

- [ ] **Step 3: Implement**

```java
package com.bytechef.ee.platform.variable.resolver;

import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.provider.VariableScopeProvider;
import com.bytechef.ee.platform.variable.service.VariableService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.variable.WorkflowVariablesResolver;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * EE implementation of the CE {@link WorkflowVariablesResolver} seam. Fail-open by contract: any failure (including the
 * distributed apps' throwing remote {@code PropertyService} client) yields an empty map and a single WARN per JVM.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class WorkflowVariablesResolverImpl implements WorkflowVariablesResolver {

    private static final Logger log = LoggerFactory.getLogger(WorkflowVariablesResolverImpl.class);

    private final AtomicBoolean failureLogged = new AtomicBoolean();
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final VariableService variableService;
    private final Map<PlatformType, VariableScopeProvider> variableScopeProviderMap;

    @SuppressFBWarnings("EI")
    public WorkflowVariablesResolverImpl(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, VariableService variableService,
        List<VariableScopeProvider> variableScopeProviders) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.variableService = variableService;
        this.variableScopeProviderMap = variableScopeProviders.stream()
            .collect(Collectors.toMap(VariableScopeProvider::getType, Function.identity()));
    }

    @Override
    public Map<String, String> resolveForJobPrincipal(long jobPrincipalId, PlatformType type) {
        try {
            VariableScopeProvider variableScopeProvider = variableScopeProviderMap.get(type);

            if (variableScopeProvider == null) {
                return Map.of();
            }

            Optional<VariableScope> variableScope = variableScopeProvider.getVariableScope(jobPrincipalId);

            if (variableScope.isEmpty()) {
                return Map.of();
            }

            JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

            long environmentId = jobPrincipalAccessor.getEnvironmentId(jobPrincipalId);

            return variableService.getVariableMap(variableScope.get(), environmentId);
        } catch (RuntimeException exception) {
            logFailure(exception);

            return Map.of();
        }
    }

    @Override
    public Map<String, String> resolveForWorkflow(String workflowId, long environmentId) {
        try {
            for (VariableScopeProvider variableScopeProvider : variableScopeProviderMap.values()) {
                Optional<VariableScope> variableScope = variableScopeProvider.getVariableScopeByWorkflowId(workflowId);

                if (variableScope.isPresent()) {
                    return variableService.getVariableMap(variableScope.get(), environmentId);
                }
            }

            return Map.of();
        } catch (RuntimeException exception) {
            logFailure(exception);

            return Map.of();
        }
    }

    private void logFailure(RuntimeException exception) {
        if (failureLogged.compareAndSet(false, true)) {
            log.warn(
                "Workflow variables could not be resolved; jobs continue without a 'vars' snapshot. Further failures " +
                    "are logged at debug level.",
                exception);
        } else if (log.isDebugEnabled()) {
            log.debug("Workflow variables could not be resolved", exception);
        }
    }
}
```
Add `implementation("org.slf4j:slf4j-api")` and `implementation("com.github.spotbugs:spotbugs-annotations")` to the service build file if the compile complains (check how a sibling module declares them).

- [ ] **Step 4: Run tests**

```bash
./gradlew :server:ee:libs:platform:platform-variable:platform-variable-service:test > /tmp/t6.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t6.log
```

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/ee/libs/platform/platform-variable
git commit -m "732 Add fail-open WorkflowVariablesResolverImpl over the scope providers

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: Seed `vars` at job creation (`PrincipalJobFacadeImpl`, `TestWorkflowExecutorImpl`)

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/facade/PrincipalJobFacadeImpl.java:53-300`
- Test: `.../platform-workflow-execution-service/src/test/java/com/bytechef/platform/workflow/execution/facade/PrincipalJobFacadeImplTest.java`
- Modify: `server/libs/platform/platform-workflow/platform-workflow-test/platform-workflow-test-service/src/main/java/com/bytechef/platform/workflow/test/facade/TestWorkflowExecutorImpl.java:90-126, 437-443`
- Modify: `.../platform-workflow-test-service/src/main/java/com/bytechef/platform/workflow/test/config/WorkflowTestConfiguration.java:119-161`
- Test: `.../platform-workflow-test-service/src/test/java/com/bytechef/platform/workflow/test/facade/TestWorkflowExecutorTest.java`

**Interfaces:**
- Consumes: `WorkflowVariablesResolver`, `JobInputConstants.VARIABLES_INPUT` (Task 2). Both classes take `ObjectProvider<WorkflowVariablesResolver>` as a NEW LAST constructor parameter.

- [ ] **Step 1: Failing tests in `PrincipalJobFacadeImplTest`** (add a `@Mock private WorkflowVariablesResolver workflowVariablesResolver;` field; update every existing `new PrincipalJobFacadeImpl(...)` call to pass one more `emptyObjectProvider()` at the end):

```java
    @Test
    void testCreateJobSeedsVarsInputWhenResolverPresent() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-1", Map.of("name", "x"), Map.of());

        when(workflowVariablesResolver.resolveForJobPrincipal(7L, PlatformType.AUTOMATION))
            .thenReturn(Map.of("API_URL", "https://api"));
        when(jobFacade.createJob(any(JobParametersDTO.class))).thenReturn(200L);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(),
            emptyObjectProvider(), emptyObjectProvider(), objectProviderOf(workflowVariablesResolver));

        facade.createJob(jobParametersDTO, 7L, PlatformType.AUTOMATION);

        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobFacade).createJob(captor.capture());

        Map<String, Object> inputs = captor.getValue()
            .getInputs();

        assertEquals("x", inputs.get("name"));
        assertEquals(Map.of("API_URL", "https://api"), inputs.get(JobInputConstants.VARIABLES_INPUT));
    }

    @Test
    void testCreateJobDoesNotAddVarsWithoutResolver() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-1", Map.of("name", "x"), Map.of());

        when(jobFacade.createJob(any(JobParametersDTO.class))).thenReturn(200L);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(),
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider());

        facade.createJob(jobParametersDTO, 7L, PlatformType.AUTOMATION);

        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobFacade).createJob(captor.capture());
        assertFalse(captor.getValue()
            .getInputs()
            .containsKey(JobInputConstants.VARIABLES_INPUT));
    }

    @Test
    void testCallerSuppliedVarsInputIsOverwritten() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            "wf-1", Map.of(JobInputConstants.VARIABLES_INPUT, Map.of("EVIL", "1")), Map.of());

        when(workflowVariablesResolver.resolveForJobPrincipal(7L, PlatformType.AUTOMATION)).thenReturn(Map.of());
        when(jobFacade.createJob(any(JobParametersDTO.class))).thenReturn(200L);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(),
            emptyObjectProvider(), emptyObjectProvider(), objectProviderOf(workflowVariablesResolver));

        facade.createJob(jobParametersDTO, 7L, PlatformType.AUTOMATION);

        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobFacade).createJob(captor.capture());
        assertEquals(Map.of(), captor.getValue()
            .getInputs()
            .get(JobInputConstants.VARIABLES_INPUT));
    }
```
Imports: `org.mockito.ArgumentCaptor`, `com.bytechef.platform.variable.WorkflowVariablesResolver`, `com.bytechef.platform.workflow.JobInputConstants`, `assertFalse`. The existing `testCreatePrincipalLinkedJob…` test uses `when(jobFacade.createJob(jobParametersDTO))` with the exact instance — after this change the facade passes a **copy**, so change that stub to `any(JobParametersDTO.class)` and add a `verify` on the captured workflow id if it asserted identity.

- [ ] **Step 2: Run to verify failure**

```bash
./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:test --tests '*PrincipalJobFacadeImplTest*' > /tmp/t7.log 2>&1; echo $?
```
Expected: compilation error (constructor arity).

- [ ] **Step 3: Implement in `PrincipalJobFacadeImpl`**

Add field `private final ObjectProvider<WorkflowVariablesResolver> workflowVariablesResolverObjectProvider;`, the last constructor parameter, and:

```java
    /**
     * Copies {@code jobParametersDTO} with the principal's variables snapshot under
     * {@link JobInputConstants#VARIABLES_INPUT}. No resolver bean (CE) leaves the inputs untouched; with a resolver
     * (EE) the key is always written — an empty map included — and overwrites any caller-supplied {@code vars}, since
     * the name is reserved.
     */
    private JobParametersDTO withVariables(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        WorkflowVariablesResolver workflowVariablesResolver = workflowVariablesResolverObjectProvider.getIfAvailable();

        if (workflowVariablesResolver == null) {
            return jobParametersDTO;
        }

        Map<String, Object> inputs = new HashMap<>(jobParametersDTO.getInputs());

        inputs.put(
            JobInputConstants.VARIABLES_INPUT, workflowVariablesResolver.resolveForJobPrincipal(jobPrincipalId, type));

        return new JobParametersDTO(
            jobParametersDTO.getWorkflowId(), jobParametersDTO.getLabel(), jobParametersDTO.getParentTaskExecutionId(),
            jobParametersDTO.getPriority(), inputs, jobParametersDTO.getMetadata(), jobParametersDTO.getWebhooks());
    }
```
Check `JobParametersDTO`'s 7-arg constructor order at `JobParametersDTO.java:61` and match it exactly (the arg order above is a guess — read the file).

Call sites:
- `createJob`: `long jobId = jobFacade.createJob(withVariables(jobParametersDTO, jobPrincipalId, type));`
- `createJobWithoutDispatch`: `Job job = jobService.create(withVariables(jobParametersDTO, jobPrincipalId, type), workflowService.getWorkflow(jobParametersDTO.getWorkflowId()));`
- `createChildJob`: resolve `principalId` FIRST (move the `fetchJobPrincipalId` call above the create), then `jobFacade.createJob(principalId.map(id -> withVariables(jobParametersDTO, id, platformType)).orElse(jobParametersDTO))`.
- `createPrincipalLinkedJob`: `jobFacade.createJob(withVariables(jobParametersDTO, principalId, platformType))`.

Add `implementation(project(":server:libs:platform:platform-api"))` to the module's build file if not present (it is — `PlatformType` is already imported).

- [ ] **Step 4: `TestWorkflowExecutorImpl`** — add `ObjectProvider<WorkflowVariablesResolver> workflowVariablesResolverObjectProvider` as the last constructor param + field; at the end of `getWorkflowTestParameters` (before `return new WorkflowTestParameters(...)`, line ~437):

```java
        WorkflowVariablesResolver workflowVariablesResolver = workflowVariablesResolverObjectProvider.getIfAvailable();

        if (workflowVariablesResolver != null) {
            inputs = MapUtils.concat(
                inputs,
                Map.of(
                    JobInputConstants.VARIABLES_INPUT,
                    workflowVariablesResolver.resolveForWorkflow(workflowId, environmentId)));
        }
```
`inputs` is `Map<String, Object>` in that method — cast the `Map.of(...)` as the existing lines 415–419 do. `WorkflowTestConfiguration.testWorkflowExecutor(...)` bean method: add `ObjectProvider<WorkflowVariablesResolver> workflowVariablesResolverObjectProvider` as a parameter and pass it last. `TestWorkflowExecutorTest.beforeEach`: pass a mocked `ObjectProvider` (`emptyObjectProvider()` style — copy the two helpers from `PrincipalJobFacadeImplTest`), and add:

```java
    @Test
    void testExecuteSeedsVarsInputWhenResolverPresent() {
        // arrange as executeSyncNoTriggersMergesInputsAndExecutesJob does, with a resolver-bearing ObjectProvider
        // returning Map.of("A", "1") for resolveForWorkflow(any(), anyLong()); capture jobSyncExecutor.startJob(...)
        // and assert getInputs().get("vars") equals Map.of("A", "1").
    }
```
Write it fully by copying the arrangement of `executeSyncNoTriggersMergesInputsAndExecutesJob` (lines ~104–140 of that test) and replacing its assertion with the `vars` capture; the SUT must be rebuilt in that test with `objectProviderOf(resolver)`.

- [ ] **Step 5: Run both modules' tests**

```bash
./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:test :server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-service:test > /tmp/t7.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t7.log
```

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/platform/platform-workflow
git commit -m "732 Seed the vars job input at job creation and in editor test runs

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 8: Editor-preview parity — `WorkflowEvaluationInputsFacade`

**Files:**
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/facade/WorkflowEvaluationInputsFacade.java`
- Create: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/facade/WorkflowEvaluationInputsFacadeImpl.java`
- Test: `.../platform-configuration-service/src/test/java/com/bytechef/platform/configuration/facade/WorkflowEvaluationInputsFacadeTest.java`
- Modify (replace `workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId|workflow.getId(), environmentId)` with `workflowEvaluationInputsFacade.getEvaluationInputs(...)`, injecting the new facade via constructor; remove the `WorkflowTestConfigurationService` field only if it becomes unused): `WorkflowNodeDynamicPropertiesFacadeImpl.java:111,177`, `WorkflowNodeOptionFacadeImpl.java:104,194`, `WorkflowNodeDescriptionFacadeImpl.java:76,94`, `WorkflowNodeOutputFacadeImpl.java:572,617,675`, `WorkflowNodeParameterFacadeImpl.java:761`, `WorkflowNodeScriptFacadeImpl.java:165`, `WebhookTriggerTestFacadeImpl.java:138,162`, `WorkflowNodeTestOutputFacadeImpl.java:268,303,443,502,523`, and `server/libs/platform/platform-workflow/platform-workflow-test/platform-workflow-test-service/.../facade/AiAgentTestFacadeImpl.java:102` (+ its bean method in `WorkflowTestConfiguration.aiAgentTestFacade`).

**Interfaces:**
- Produces: `interface WorkflowEvaluationInputsFacade { Map<String, ?> getEvaluationInputs(String workflowId, long environmentId); }` = test-configuration inputs plus `vars` when a resolver bean exists.

- [ ] **Step 1: Failing test**

```java
package com.bytechef.platform.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.variable.WorkflowVariablesResolver;
import com.bytechef.platform.workflow.JobInputConstants;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class WorkflowEvaluationInputsFacadeTest {

    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);

    @Test
    @SuppressWarnings("unchecked")
    void testMergesTestConfigurationInputsWithVars() {
        WorkflowVariablesResolver resolver = mock(WorkflowVariablesResolver.class);
        ObjectProvider<WorkflowVariablesResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resolver);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs("wf", 0L))
            .thenReturn(Map.of("name", "x"));
        when(resolver.resolveForWorkflow("wf", 0L)).thenReturn(Map.of("A", "1"));

        WorkflowEvaluationInputsFacadeImpl facade = new WorkflowEvaluationInputsFacadeImpl(
            objectProvider, workflowTestConfigurationService);

        Map<String, ?> inputs = facade.getEvaluationInputs("wf", 0L);

        assertEquals("x", inputs.get("name"));
        assertEquals(Map.of("A", "1"), inputs.get(JobInputConstants.VARIABLES_INPUT));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReturnsInputsOnlyWithoutResolver() {
        ObjectProvider<WorkflowVariablesResolver> objectProvider = mock(ObjectProvider.class);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs("wf", 0L))
            .thenReturn(Map.of("name", "x"));

        WorkflowEvaluationInputsFacadeImpl facade = new WorkflowEvaluationInputsFacadeImpl(
            objectProvider, workflowTestConfigurationService);

        assertFalse(facade.getEvaluationInputs("wf", 0L)
            .containsKey(JobInputConstants.VARIABLES_INPUT));
    }
}
```

- [ ] **Step 2: Run, expect compile failure.** `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests '*WorkflowEvaluationInputsFacadeTest*' > /tmp/t8.log 2>&1; echo $?`

- [ ] **Step 3: Implement**

Interface (api module, Apache header):
```java
package com.bytechef.platform.configuration.facade;

import java.util.Map;

/**
 * The inputs half of the editor's evaluation context: the workflow's test-configuration inputs plus, when the EE
 * variables resolver is present, the {@code vars} snapshot — the same shape a real job's inputs have. Every
 * editor-side preview site (dynamic properties, options, descriptions, sample outputs, script previews, trigger tests)
 * concats this with previous-node sample outputs, so this is the single place that knows how {@code vars} reaches
 * previews.
 *
 * @author Ivica Cardic
 */
public interface WorkflowEvaluationInputsFacade {

    Map<String, ?> getEvaluationInputs(String workflowId, long environmentId);
}
```
Impl (service module):
```java
@Service
public class WorkflowEvaluationInputsFacadeImpl implements WorkflowEvaluationInputsFacade {

    private final ObjectProvider<WorkflowVariablesResolver> workflowVariablesResolverObjectProvider;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowEvaluationInputsFacadeImpl(
        ObjectProvider<WorkflowVariablesResolver> workflowVariablesResolverObjectProvider,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.workflowVariablesResolverObjectProvider = workflowVariablesResolverObjectProvider;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> getEvaluationInputs(String workflowId, long environmentId) {
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        WorkflowVariablesResolver workflowVariablesResolver = workflowVariablesResolverObjectProvider.getIfAvailable();

        if (workflowVariablesResolver == null) {
            return inputs;
        }

        return MapUtils.concat(
            (Map<String, Object>) inputs,
            Map.of(JobInputConstants.VARIABLES_INPUT, workflowVariablesResolver.resolveForWorkflow(workflowId, environmentId)));
    }
}
```
Guard: `RemoteWorkflowTestConfigurationServiceClient.getWorkflowTestConfigurationInputs` returns `null` — treat `inputs == null` as `Map.of()` before concat.

- [ ] **Step 4: Rewire the 20 call sites.** For each facade class listed above: add a `WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade` constructor parameter + field, replace each `workflowTestConfigurationService.getWorkflowTestConfigurationInputs(X, environmentId)` with `workflowEvaluationInputsFacade.getEvaluationInputs(X, environmentId)`. Keep the `WorkflowTestConfigurationService` field where the class still uses it for other calls (`fetchWorkflowTestConfigurationConnectionId`, etc.). For `AiAgentTestFacadeImpl` add the facade to its `@Bean` factory in `WorkflowTestConfiguration.aiAgentTestFacade(...)`. Every constructor change breaks that class's unit test and any `*IntTestConfiguration` that hand-assembles it — run this to find them:

```bash
grep -rln "new WorkflowNodeDynamicPropertiesFacadeImpl(\|new WorkflowNodeOptionFacadeImpl(\|new WorkflowNodeDescriptionFacadeImpl(\|new WorkflowNodeOutputFacadeImpl(\|new WorkflowNodeParameterFacadeImpl(\|new WorkflowNodeScriptFacadeImpl(\|new WebhookTriggerTestFacadeImpl(\|new WorkflowNodeTestOutputFacadeImpl(\|new AiAgentTestFacadeImpl(" server --include='*.java' | grep -v /bin/
```
and add a `mock(WorkflowEvaluationInputsFacade.class)` (unit) or `@Bean` mock (IntTest configurations) at each.

- [ ] **Step 5: Compile everything and run the touched modules' tests**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t8c.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t8c.log
./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test :server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-service:test > /tmp/t8.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t8.log
```

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add -A server/libs server/ee/libs
git status --short | grep -v "^[AM]" # must be empty — only intended files
git commit -m "732 Route editor preview evaluation inputs through WorkflowEvaluationInputsFacade so vars resolve in previews

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 9: GraphQL — workspace controller (`platform-variable-graphql`) and embedded controller

**Files:**
- Modify: `server/ee/libs/platform/platform-variable/platform-variable-graphql/build.gradle.kts`
- Create: `.../platform-variable-graphql/src/main/java/com/bytechef/ee/platform/variable/web/graphql/WorkspaceVariableGraphQlController.java`, `.../src/main/resources/graphql/workspace-variable.graphqls`
- Test: `.../platform-variable-graphql/src/test/java/com/bytechef/ee/platform/variable/web/graphql/WorkspaceVariableGraphQlControllerTest.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/EmbeddedVariableGraphQlController.java`, `.../src/main/resources/graphql/embedded-variable.graphqls`; add `implementation(project(":server:ee:libs:platform:platform-variable:platform-variable-api"))` to that module's build file
- Test: `.../embedded-configuration-graphql/src/test/java/com/bytechef/ee/embedded/configuration/web/graphql/EmbeddedVariableGraphQlControllerTest.java`
- Modify: `client/codegen.ts:66-114` — add `'../server/ee/libs/platform/platform-variable/platform-variable-graphql/src/main/resources/graphql/*.graphqls',` (the embedded module's glob is already listed at line 93).

**Interfaces (Produces):** the GraphQL operations from the spec — `workspaceVariables`, `createWorkspaceVariable`, `updateWorkspaceVariable`, `deleteWorkspaceVariable`, `embeddedVariables`, `createEmbeddedVariable`, `updateEmbeddedVariable`, `deleteEmbeddedVariable`; type `Variable`; input `VariableInput`.

- [ ] **Step 1: build file**

```kotlin
dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:platform:platform-variable:platform-variable-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
```

- [ ] **Step 2: Failing controller tests** (reflection-pinned `@PreAuthorize`, the guardrails-test pattern):

`WorkspaceVariableGraphQlControllerTest`:
```java
class WorkspaceVariableGraphQlControllerTest {

    private VariableService variableService;
    private WorkspaceVariableGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        variableService = mock(VariableService.class);
        controller = new WorkspaceVariableGraphQlController(variableService);
    }

    @Test
    void testWorkspaceVariablesRequiresVariableView() throws NoSuchMethodException {
        Method method = WorkspaceVariableGraphQlController.class.getDeclaredMethod(
            "workspaceVariables", long.class, long.class);

        assertThat(method.getAnnotation(QueryMapping.class)).isNotNull();
        assertThat(method.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("hasPermission(#workspaceId, 'Workspace', 'VARIABLE_VIEW')");
    }

    @Test
    void testMutationsRequireVariableManage() throws NoSuchMethodException {
        for (Method method : new Method[] {
            WorkspaceVariableGraphQlController.class.getDeclaredMethod(
                "createWorkspaceVariable", long.class, long.class,
                WorkspaceVariableGraphQlController.VariableInput.class),
            WorkspaceVariableGraphQlController.class.getDeclaredMethod(
                "updateWorkspaceVariable", long.class, long.class, long.class,
                WorkspaceVariableGraphQlController.VariableInput.class),
            WorkspaceVariableGraphQlController.class.getDeclaredMethod(
                "deleteWorkspaceVariable", long.class, long.class, long.class)
        }) {
            assertThat(method.getAnnotation(MutationMapping.class)).isNotNull();
            assertThat(method.getAnnotation(PreAuthorize.class)
                .value()).isEqualTo("hasPermission(#workspaceId, 'Workspace', 'VARIABLE_MANAGE')");
        }
    }

    @Test
    void testWorkspaceVariablesDelegatesWithWorkspaceScope() {
        Variable variable = new Variable(1L, "A", "1", 0, null, null, null, null);

        when(variableService.getVariables(VariableScope.workspace(7L), 0L)).thenReturn(List.of(variable));

        assertThat(controller.workspaceVariables(7L, 0L)).containsExactly(variable);
    }

    @Test
    void testCreateWorkspaceVariableDelegates() {
        Variable variable = new Variable(1L, "A", "1", 0, null, null, null, null);

        when(variableService.create(VariableScope.workspace(7L), 0L, "A", "1")).thenReturn(variable);

        assertThat(controller.createWorkspaceVariable(
            7L, 0L, new WorkspaceVariableGraphQlController.VariableInput("A", "1"))).isEqualTo(variable);
    }

    @Test
    void testDeleteWorkspaceVariableDelegatesAndReturnsTrue() {
        assertThat(controller.deleteWorkspaceVariable(7L, 0L, 5L)).isTrue();

        verify(variableService).delete(VariableScope.workspace(7L), 0L, 5L);
    }
}
```
`EmbeddedVariableGraphQlControllerTest`: same shape with `VariableScope.embedded()`, method names `embeddedVariables(long)`, `createEmbeddedVariable(long, VariableInput)`, `updateEmbeddedVariable(long, long, VariableInput)`, `deleteEmbeddedVariable(long, long)`; expected `@PreAuthorize` values `isAuthenticated()` (query) and `hasAuthority('ROLE_ADMIN')` (mutations).

- [ ] **Step 3: Run, expect compile failure.**

- [ ] **Step 4: Implement**

`workspace-variable.graphqls`:
```graphql
extend type Query {
    workspaceVariables(workspaceId: ID!, environmentId: ID!): [Variable!]!
}

extend type Mutation {
    createWorkspaceVariable(workspaceId: ID!, environmentId: ID!, input: VariableInput!): Variable!
    updateWorkspaceVariable(workspaceId: ID!, environmentId: ID!, id: ID!, input: VariableInput!): Variable!
    deleteWorkspaceVariable(workspaceId: ID!, environmentId: ID!, id: ID!): Boolean!
}

type Variable {
    id: ID!
    name: String!
    value: String!
    environmentId: ID!
    createdBy: String
    createdDate: String
    lastModifiedBy: String
    lastModifiedDate: String
}

input VariableInput {
    name: String!
    value: String!
}
```
`embedded-variable.graphqls` (defines only `extend type Query/Mutation` — `Variable`/`VariableInput` are declared once, in the workspace file; both modules are always on the classpath together in EE):
```graphql
extend type Query {
    embeddedVariables(environmentId: ID!): [Variable!]!
}

extend type Mutation {
    createEmbeddedVariable(environmentId: ID!, input: VariableInput!): Variable!
    updateEmbeddedVariable(environmentId: ID!, id: ID!, input: VariableInput!): Variable!
    deleteEmbeddedVariable(environmentId: ID!, id: ID!): Boolean!
}
```

`WorkspaceVariableGraphQlController`:
```java
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class WorkspaceVariableGraphQlController {

    private final VariableService variableService;

    @SuppressFBWarnings("EI")
    WorkspaceVariableGraphQlController(VariableService variableService) {
        this.variableService = variableService;
    }

    @QueryMapping
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'VARIABLE_VIEW')")
    public List<Variable> workspaceVariables(@Argument long workspaceId, @Argument long environmentId) {
        return variableService.getVariables(VariableScope.workspace(workspaceId), environmentId);
    }

    @MutationMapping
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'VARIABLE_MANAGE')")
    public Variable createWorkspaceVariable(
        @Argument long workspaceId, @Argument long environmentId, @Argument VariableInput input) {

        return variableService.create(VariableScope.workspace(workspaceId), environmentId, input.name(), input.value());
    }

    @MutationMapping
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'VARIABLE_MANAGE')")
    public Variable updateWorkspaceVariable(
        @Argument long workspaceId, @Argument long environmentId, @Argument long id, @Argument VariableInput input) {

        return variableService.update(
            VariableScope.workspace(workspaceId), environmentId, id, input.name(), input.value());
    }

    @MutationMapping
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'VARIABLE_MANAGE')")
    public boolean deleteWorkspaceVariable(@Argument long workspaceId, @Argument long environmentId, @Argument long id) {
        variableService.delete(VariableScope.workspace(workspaceId), environmentId, id);

        return true;
    }

    public record VariableInput(String name, String value) {
    }
}
```
GraphQL `Instant` fields: existing controllers return `String` dates (`OrganizationConnectionGraphQlController:93`) — if Spring GraphQL fails to coerce `Instant` to `String` at runtime, map `Variable` to a controller-local `VariableResponse` record with `String` dates (`instant == null ? null : instant.toString()`), the way `WorkspaceUserGraphQlController` does. Do that in both controllers.

`EmbeddedVariableGraphQlController` — same, with `VariableScope.embedded()`, `@PreAuthorize("isAuthenticated()")` on the query and `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` on the mutations, its own nested `VariableInput` record (Spring GraphQL binds by field name, so two records named `VariableInput` in different controllers are fine).

- [ ] **Step 5: Tests + boot smoke** (schema must merge cleanly):

```bash
./gradlew :server:ee:libs:platform:platform-variable:platform-variable-graphql:test :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:test > /tmp/t9.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/t9.log
```
Then start the server (`./gradlew -p server/apps/server-app bootRun`, infra up) and `curl -s -X POST localhost:9555/graphql -H 'Content-Type: application/json' -d '{"query":"{ __type(name:\"Variable\"){ name } }"}'` — expected `{"data":{"__type":{"name":"Variable"}}}`. Stop the server.

- [ ] **Step 6: Codegen entry + commit**

Add the `platform-variable-graphql` glob to `client/codegen.ts` (after the `platform-ai-workspace-prompt-graphql` line), then:

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/ee/libs/platform/platform-variable server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql client/codegen.ts
git commit -m "732 Add workspace and embedded variable GraphQL controllers

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 10: Client GraphQL documents, codegen, edition seam

**Files:**
- Create: `client/src/graphql/platform/variable/workspaceVariables.graphql`, `createWorkspaceVariable.graphql`, `updateWorkspaceVariable.graphql`, `deleteWorkspaceVariable.graphql`, `embeddedVariables.graphql`, `createEmbeddedVariable.graphql`, `updateEmbeddedVariable.graphql`, `deleteEmbeddedVariable.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`, `graphql-types.ts`
- Create: `client/src/shared/edition/variables/variablesApi.ts`
- Modify: `client/src/ee/shared/edition/registerEditionModules.ts`
- Test: `client/src/shared/edition/variables/variablesApi.test.ts`

**Interfaces (Produces):**
```ts
export interface VariableI { id: string; name: string; value: string; }
export type VariableScopeType = {type: 'WORKSPACE'; workspaceId: number} | {type: 'EMBEDDED'};
export interface VariablesApiI {
    useWorkflowVariablesQuery: (scope: VariableScopeType | undefined, environmentId: number) => {data: VariableI[] | undefined};
}
export function registerVariablesApi(api: VariablesApiI): void;
export function getVariablesApi(): VariablesApiI;
```

- [ ] **Step 1: GraphQL documents** (one operation per file, the `workspaceApiKeys.graphql` convention):

`workspaceVariables.graphql`:
```graphql
query workspaceVariables($workspaceId: ID!, $environmentId: ID!) {
    workspaceVariables(workspaceId: $workspaceId, environmentId: $environmentId) {
        id
        name
        value
        environmentId
        createdBy
        createdDate
        lastModifiedBy
        lastModifiedDate
    }
}
```
`createWorkspaceVariable.graphql`:
```graphql
mutation createWorkspaceVariable($workspaceId: ID!, $environmentId: ID!, $input: VariableInput!) {
    createWorkspaceVariable(workspaceId: $workspaceId, environmentId: $environmentId, input: $input) {
        id
        name
        value
    }
}
```
`updateWorkspaceVariable.graphql`: `mutation updateWorkspaceVariable($workspaceId: ID!, $environmentId: ID!, $id: ID!, $input: VariableInput!) { updateWorkspaceVariable(workspaceId: $workspaceId, environmentId: $environmentId, id: $id, input: $input) { id name value } }`
`deleteWorkspaceVariable.graphql`: `mutation deleteWorkspaceVariable($workspaceId: ID!, $environmentId: ID!, $id: ID!) { deleteWorkspaceVariable(workspaceId: $workspaceId, environmentId: $environmentId, id: $id) }`
Embedded four: identical minus `$workspaceId`, operation names `embeddedVariables` / `createEmbeddedVariable` / `updateEmbeddedVariable` / `deleteEmbeddedVariable`.

- [ ] **Step 2: Codegen**

```bash
cd client && npx graphql-codegen > /tmp/cg.log 2>&1; echo $?; grep -c "useWorkspaceVariablesQuery\|useEmbeddedVariablesQuery" src/shared/middleware/graphql.ts
```
Expected: exit 0, count ≥ 2. Commit operations and the generated file separately (CLAUDE.md convention):

```bash
git add src/graphql/platform/variable && git commit -m "732 client - Add variable GraphQL operations

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
git add src/shared/middleware/graphql.ts src/shared/middleware/graphql-types.ts && git commit -m "732 client - Regenerate GraphQL client for variables

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

- [ ] **Step 3: Failing seam test** — `variablesApi.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import {getVariablesApi, registerVariablesApi} from './variablesApi';

describe('variablesApi', () => {
    it('defaults to a no-op query that reports no data', () => {
        const {data} = getVariablesApi().useWorkflowVariablesQuery({type: 'EMBEDDED'}, 0);

        expect(data).toBeUndefined();
    });

    it('returns the registered implementation', () => {
        const registered = {
            useWorkflowVariablesQuery: () => ({data: [{id: '1', name: 'A', value: '1'}]}),
        };

        registerVariablesApi(registered);

        expect(getVariablesApi()).toBe(registered);
    });
});
```

- [ ] **Step 4: Implement `variablesApi.ts`**

```ts
/**
 * Edition seam for workflow variables (EE-only). The CE workflow editor reads variables through this registry rather
 * than importing the EE generated hooks; the default reports nothing, so the data-pill panel renders no Variables
 * section on CE. The EE bundle registers the real hooks from registerEditionModules before the first render, so hook
 * identity never changes between renders.
 */

export interface VariableI {
    id: string;
    name: string;
    value: string;
}

export type VariableScopeType = {type: 'WORKSPACE'; workspaceId: number} | {type: 'EMBEDDED'};

export interface VariablesApiI {
    useWorkflowVariablesQuery: (
        scope: VariableScopeType | undefined,
        environmentId: number
    ) => {data: VariableI[] | undefined};
}

const noopVariablesApi: VariablesApiI = {
    useWorkflowVariablesQuery: () => ({data: undefined}),
};

let variablesApi: VariablesApiI = noopVariablesApi;

export function registerVariablesApi(api: VariablesApiI) {
    variablesApi = api;
}

export function getVariablesApi(): VariablesApiI {
    return variablesApi;
}
```

`registerEditionModules.ts` — add:
```ts
import {registerVariablesApi} from '@/shared/edition/variables/variablesApi';
import {useEmbeddedVariablesQuery, useWorkspaceVariablesQuery} from '@/shared/middleware/graphql';

registerVariablesApi({
    useWorkflowVariablesQuery: (scope, environmentId) => {
        // Both hooks are always called (rules of hooks); `enabled` picks the live one.
        const workspaceQuery = useWorkspaceVariablesQuery(
            {
                environmentId: `${environmentId}`,
                workspaceId: scope?.type === 'WORKSPACE' ? `${scope.workspaceId}` : '',
            },
            {enabled: scope?.type === 'WORKSPACE'}
        );
        const embeddedQuery = useEmbeddedVariablesQuery(
            {environmentId: `${environmentId}`},
            {enabled: scope?.type === 'EMBEDDED'}
        );

        if (scope?.type === 'WORKSPACE') {
            return {data: workspaceQuery.data?.workspaceVariables};
        }

        if (scope?.type === 'EMBEDDED') {
            return {data: embeddedQuery.data?.embeddedVariables};
        }

        return {data: undefined};
    },
});
```
(Keep imports sorted; the generated `Variable` type has `id: string`.)

- [ ] **Step 5: Run**

```bash
cd client && npx vitest run src/shared/edition/variables > /tmp/v10.log 2>&1; echo $?; tail -5 /tmp/v10.log
npm run typecheck > /tmp/tc.log 2>&1; echo $?
```

- [ ] **Step 6: Commit**

```bash
cd client && npm run format > /dev/null 2>&1; git add src/shared/edition/variables src/ee/shared/edition/registerEditionModules.ts
git commit -m "732 client - Add the variables edition seam and register the EE hooks

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 11: Shared `VariablesContent` (table + dialogs) in `client/src/ee/shared/components/variables/`

**Files:**
- Create: `providers/variablesProvider.tsx`, `stores/useVariablesStore.ts`, `hooks/useVariables.ts`, `components/VariableTable.tsx`, `components/VariableDialog.tsx`, `components/VariableDeleteDialog.tsx`, `VariablesContent.tsx`
- Test: `client/src/ee/shared/components/variables/tests/VariablesContent.test.tsx`

**Interfaces (Produces):**
```ts
export interface VariableFormValuesI { name: string; value: string; }
export interface VariablesProviderStateI {
    canManage: boolean;
    useCreateVariableMutation: (props?: SimpleMutationProps) => {mutate: (input: VariableFormValuesI) => void};
    useDeleteVariableMutation: (props?: SimpleMutationProps) => {mutate: (input: {id: string}) => void};
    useUpdateVariableMutation: (props?: SimpleMutationProps) => {mutate: (input: {id: string} & VariableFormValuesI) => void};
    useVariablesQuery: () => {data: Variable[] | undefined; error: Error | null; isLoading: boolean};
}
<VariablesProvider value={...}><VariablesContent description title /></VariablesProvider>
```
Copy the file shapes of `client/src/ee/shared/components/api-keys/*` (provider context, zustand dialog store, `useX` hook that wires the provider hooks to the store).

- [ ] **Step 1: Failing test** (mock the provider value; render `VariablesContent`):

```tsx
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import VariablesContent from '../VariablesContent';
import {VariablesProvider} from '../providers/variablesProvider';

vi.mock('@/shared/components/EnvironmentSelect', () => ({default: () => <div>env-select</div>}));

const variables = [{environmentId: '0', id: '1', name: 'API_URL', value: 'https://api'}];

const renderContent = (canManage = true, data = variables) =>
    render(
        <VariablesProvider
            value={{
                canManage,
                useCreateVariableMutation: () => ({mutate: vi.fn()}),
                useDeleteVariableMutation: () => ({mutate: vi.fn()}),
                useUpdateVariableMutation: () => ({mutate: vi.fn()}),
                useVariablesQuery: () => ({data, error: null, isLoading: false}),
            }}
        >
            <VariablesContent description="d" title="Variables" />
        </VariablesProvider>
    );

describe('VariablesContent', () => {
    it('lists variables with their reference expression', () => {
        renderContent();

        expect(screen.getByText('API_URL')).toBeInTheDocument();
        expect(screen.getByText('${vars.API_URL}')).toBeInTheDocument();
    });

    it('shows the empty state with a create button', () => {
        renderContent(true, []);

        expect(screen.getByText('No Variables')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Variable'})).toBeInTheDocument();
    });

    it('hides create and row actions without manage permission', () => {
        renderContent(false);

        expect(screen.queryByRole('button', {name: 'New Variable'})).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Variable actions')).not.toBeInTheDocument();
    });

    it('validates the name in the dialog', async () => {
        renderContent();

        await userEvent.click(screen.getByRole('button', {name: 'New Variable'}));
        await userEvent.type(screen.getByLabelText('Name'), '1bad');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText(/must start with a letter or underscore/i)).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run, expect failure** — `cd client && npx vitest run src/ee/shared/components/variables > /tmp/v11.log 2>&1; echo $?`

- [ ] **Step 3: Implement**

`providers/variablesProvider.tsx` — as `apiKeysProvider.tsx`, exporting `VariablesProvider`, `useVariablesProvider`, `VariablesProviderStateI`, `SimpleMutationProps`, `VariableFormValuesI` (types above; `Variable` from `@/shared/middleware/graphql`).

`stores/useVariablesStore.ts` — as `useApiKeysStore.ts` with `currentVariable: Variable | undefined`, `showDeleteDialog`, `showEditDialog` (no secretKey); store name `'bytechef.variables'`.

`hooks/useVariables.ts`:
```ts
const useVariables = () => {
    const {setCurrentVariable, setShowDeleteDialog, setShowEditDialog} = useVariablesStore(useShallow(...));
    const {canManage, useCreateVariableMutation, useDeleteVariableMutation, useUpdateVariableMutation, useVariablesQuery} =
        useVariablesProvider();
    const {data: variables, error: variablesError, isLoading: variablesLoading} = useVariablesQuery();
    const createVariableMutation = useCreateVariableMutation({onSuccess: () => { setShowEditDialog(false); setCurrentVariable(undefined); }});
    const deleteVariableMutation = useDeleteVariableMutation({onSuccess: () => { setShowDeleteDialog(false); setCurrentVariable(undefined); }});
    const updateVariableMutation = useUpdateVariableMutation({onSuccess: () => { setShowEditDialog(false); setCurrentVariable(undefined); }});
    const handleDelete = (id: string) => deleteVariableMutation.mutate({id});
    const handleSave = (values: VariableFormValuesI, id?: string) => id ? updateVariableMutation.mutate({id, ...values}) : createVariableMutation.mutate(values);
    return {canManage, handleDelete, handleSave, variables, variablesError, variablesLoading};
};
```

`components/VariableDialog.tsx` — Radix `Dialog` (import from `@/components/ui/dialog` as `ApiKeyDialog.tsx` does), react-hook-form + zod:
```ts
const formSchema = z.object({
    name: z
        .string()
        .min(1, 'Name is required')
        .max(50, 'Name cannot be longer than 50 characters')
        .regex(/^[A-Za-z_][A-Za-z0-9_]*$/, 'Name must start with a letter or underscore and contain only letters, digits and underscores'),
    value: z.string().max(4096, 'Value cannot be longer than 4096 characters'),
});
```
Fields: `Input` labelled "Name" (`<Label htmlFor="name">Name</Label>`), `Textarea` labelled "Value", a muted helper line `Reference it as ${vars.<name>}` that live-updates from the name field, footer buttons Cancel / Save. Props: `triggerNode?: ReactNode`, `onClose?: () => void`; reads `currentVariable` from the store for edit mode (defaultValues from it) — same as `ApiKeyDialog`.

`components/VariableDeleteDialog.tsx` — copy `ApiKeyDeleteDialog.tsx`, text "Delete variable" / `Are you sure you want to delete "{name}"? Workflows referencing ${vars.NAME} will see the literal expression instead of a value.`

`components/VariableTable.tsx` — `@tanstack/react-table` like `ApiKeyTable.tsx`, columns: Name (`font-mono`), Value (truncate to 60 chars with a `Tooltip` for the full value), Reference (`<code>${vars.NAME}</code>` + a copy-to-clipboard `Button` with `CopyIcon`, `aria-label="Copy reference"`), Last modified (`lastModifiedDate` formatted with the app's existing date util — grep `formatDate\|toLocaleDateString` in `ApiKeyTable.tsx` and reuse), Actions (`DropdownMenu` with Edit / Delete, trigger `aria-label="Variable actions"`, rendered only when `canManage`).

`VariablesContent.tsx` — copy `ApiKeysContent.tsx`: `PageLoader` → `LayoutContainer` with `Header` (`centerTitle`, `description`, `position="main"`, `right` = `EnvironmentSelect` + `VariableDialog triggerNode={<Button>New Variable</Button>}` when `canManage`), body `VariableTable` or `EmptyList` (icon `VariableIcon` from lucide, title "No Variables", message "Get started by defining a variable, then reference it in any workflow as ${vars.NAME}."; the `button` only when `canManage`), and the two dialogs behind their store flags.

- [ ] **Step 4: Run tests + check**

```bash
cd client && npx vitest run src/ee/shared/components/variables > /tmp/v11.log 2>&1; echo $?; tail -8 /tmp/v11.log
npm run check > /tmp/ck.log 2>&1; echo $?; grep -n "error" /tmp/ck.log | head
```

- [ ] **Step 5: Commit**

```bash
cd client && npm run format > /dev/null 2>&1; git add src/ee/shared/components/variables
git commit -m "732 client - Add shared VariablesContent table and dialogs

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 12: Settings pages + routes (automation workspace, embedded)

**Files:**
- Create: `client/src/ee/pages/settings/automation/variables/Variables.tsx`, `client/src/ee/pages/settings/embedded/variables/Variables.tsx`
- Modify: `client/src/routes.tsx` (lazy imports near lines 102/133; `currentWorkspaceSettingsRoutes.children` + `navItems` lines 198–296; embedded settings block lines 1438–1488)
- Test: `client/src/ee/pages/settings/automation/variables/tests/Variables.test.tsx`

**Interfaces:**
- Consumes: `VariablesProvider`/`VariablesContent` (Task 11), generated hooks (Task 10), `useMyWorkspaceScopesQuery`.

- [ ] **Step 1: Failing page test** (mock generated hooks the `WorkspaceUsers.test.tsx` way):

```tsx
const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    scopes: ['VARIABLE_VIEW', 'VARIABLE_MANAGE'] as string[],
    variables: [{environmentId: '0', id: '1', name: 'API_URL', value: 'x'}] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateWorkspaceVariableMutation: vi.fn(() => ({mutate: hoisted.createMutate})),
    useDeleteWorkspaceVariableMutation: vi.fn(() => ({mutate: vi.fn()})),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: hoisted.scopes}, isLoading: false})),
    useUpdateWorkspaceVariableMutation: vi.fn(() => ({mutate: vi.fn()})),
    useWorkspaceVariablesQuery: vi.fn(() => ({data: {workspaceVariables: hoisted.variables}, error: null, isLoading: false})),
}));
vi.mock('@/shared/components/EnvironmentSelect', () => ({default: () => <div>env-select</div>}));
vi.mock(import('@tanstack/react-query'), async (importOriginal) => ({
    ...(await importOriginal()),
    useQueryClient: vi.fn(() => ({invalidateQueries: vi.fn()}) as never),
}));

describe('Variables (workspace)', () => {
    it('renders the workspace variables', () => {
        render(<Variables />);

        expect(screen.getByText('API_URL')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Variable'})).toBeInTheDocument();
    });

    it('hides management controls without VARIABLE_MANAGE', () => {
        hoisted.scopes = ['VARIABLE_VIEW'];

        render(<Variables />);

        expect(screen.queryByRole('button', {name: 'New Variable'})).not.toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run, expect failure.**

- [ ] **Step 3: Implement pages**

`automation/variables/Variables.tsx` — copy `WorkspaceApiKeys.tsx`'s structure: read `environmentId` (`useEnvironmentStore`) and `workspaceId` (`useWorkspaceStore`), `useMyWorkspaceScopesQuery({workspaceId: String(workspaceId)})` → `canManage = scopes.includes('VARIABLE_MANAGE')`; provider value maps `useWorkspaceVariablesQuery({environmentId: \`${environmentId}\`, workspaceId: \`${workspaceId}\`})` → `data.workspaceVariables`; the three mutations invalidate `['workspaceVariables']` on success and pass `workspaceId`/`environmentId` plus `input: {name, value}` (and `id` for update/delete). Render `<VariablesContent description="Reusable values every workflow in this workspace can reference as ${vars.NAME}." title="Variables" />`.

`embedded/variables/Variables.tsx` — same with the `Embedded*` hooks, `canManage = account.authorities.includes('ROLE_ADMIN')` (`useAuthenticationStore`), description "Reusable values every integration workflow can reference as ${vars.NAME}."

`routes.tsx`:
- lazy imports: `const EmbeddedVariables = lazy(() => import('@/ee/pages/settings/embedded/variables/Variables'));` and `const WorkspaceVariables = lazy(() => import('@/ee/pages/settings/automation/variables/Variables'));` (alphabetical among the existing ones).
- `currentWorkspaceSettingsRoutes.children`: add after the `workspace-api-keys` entry
  ```tsx
  {
      element: (
          <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
              <EEVersion>
                  <LazyLoadWrapper>
                      <WorkspaceVariables />
                  </LazyLoadWrapper>
              </EEVersion>
          </PrivateRoute>
      ),
      path: 'variables',
  },
  ```
  and `navItems`: `{href: 'variables', title: 'Variables'}` after API Keys.
- embedded block: after the `api-keys` child add the same route element with `hasAnyAuthorities={[AUTHORITIES.ADMIN]}` and `<EmbeddedVariables />`, `path: 'variables'`; nav item `{href: '/embedded/settings/variables', title: 'Variables'}` after API Keys.

- [ ] **Step 4: Run + check + manual smoke** — `npx vitest run src/ee/pages/settings` then `npm run check`; start client + server, log in as admin, visit `/automation/settings/variables` and `/embedded/settings/variables`, create/edit/delete a variable in DEVELOPMENT, confirm the row survives reload and the toast on a duplicate name.

- [ ] **Step 5: Commit**

```bash
cd client && npm run format > /dev/null 2>&1; git add src/ee/pages/settings/automation/variables src/ee/pages/settings/embedded/variables src/routes.tsx
git commit -m "732 client - Add workspace and embedded Variables settings pages

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 13: Editor data-pill panel — Variables section, pill sets, icon, reserved name

**Files:**
- Create: `client/src/pages/platform/workflow-editor/hooks/useWorkflowVariables.ts`
- Create: `client/src/pages/platform/workflow-editor/utils/getWorkflowInputAndVariableDataPills.ts`
- Create: `client/src/pages/platform/workflow-editor/components/datapills/DataPillPanelBodyVariablesItem.tsx`
- Modify: `.../datapills/DataPillPanelBody.tsx:41-90`, `.../datapills/DataPillPanel.tsx` (pass `variables`), `.../components/hooks/useWorkflowNodeDetailsPanel.ts:910-929`, `client/src/pages/platform/cluster-element-editor/data-stream-editor/hooks/useDataStreamDataPills.ts:79-89`, `.../ai-agent-testing-panel/hooks/useAiAgentTestDataPills.ts:80-90`, `.../property-mentions-input/getDataPillIconSource.ts:26`, `.../utils/graphNodeMutations.ts:32-50`, `.../workflow-inputs/hooks/useWorkflowInputs.ts:176-186`
- Tests: `.../utils/tests/getWorkflowInputAndVariableDataPills.test.ts`, `.../datapills/tests/DataPillPanelBodyVariablesItem.test.tsx`, extend `graphNodeMutations` test if one exists (grep `validateGraphNodeName` under `src/pages/platform/workflow-editor` tests)

**Interfaces (Produces):**
```ts
useWorkflowVariables(): VariableI[]                                   // [] on CE / while loading
getWorkflowInputAndVariableDataPills(inputs: WorkflowInput[] | undefined, variables: VariableI[]): DataPillType[]
```

- [ ] **Step 1: Failing tests**

`getWorkflowInputAndVariableDataPills.test.ts`:
```ts
import {describe, expect, it} from 'vitest';

import getWorkflowInputAndVariableDataPills from '../getWorkflowInputAndVariableDataPills';

describe('getWorkflowInputAndVariableDataPills', () => {
    it('builds flat input pills and vars.NAME pills', () => {
        const pills = getWorkflowInputAndVariableDataPills([{name: 'customer', type: 'string'}], [
            {id: '1', name: 'API_URL', value: 'x'},
        ]);

        expect(pills).toEqual([
            {id: 'customer', nodeName: 'customer', value: 'customer'},
            {id: 'vars.API_URL', nodeName: 'vars', value: 'vars.API_URL'},
        ]);
    });

    it('tolerates undefined inputs', () => {
        expect(getWorkflowInputAndVariableDataPills(undefined, [])).toEqual([]);
    });
});
```
`DataPillPanelBodyVariablesItem.test.tsx`: mock `../../../hooks/useWorkflowVariables` to return two variables; render inside `<Accordion type="single" collapsible><AccordionItem value="variables">…</AccordionItem></Accordion>` with `dataPillFilterQuery=""` → both names visible; with `dataPillFilterQuery="API"` → only `API_URL`; with the hook returning `[]` → "No variables defined." text.

`graphNodeMutations` test: `validateGraphNodeName([], 0, 'vars')` → `{valid: false, error: '"vars" is a reserved name.'}`.

- [ ] **Step 2: Run, expect failure.**

- [ ] **Step 3: Implement**

`useWorkflowVariables.ts`:
```ts
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {VariableI, VariableScopeType, getVariablesApi} from '@/shared/edition/variables/variablesApi';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

const EMPTY_VARIABLES: VariableI[] = [];

/**
 * Variables visible to the workflow open in the editor: the current workspace's set in automation, the organization
 * set in embedded, always for the editor's current environment. Empty on CE (the edition seam's default) and while
 * loading.
 */
export default function useWorkflowVariables(): VariableI[] {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentType = usePlatformTypeStore((state) => state.currentType);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const scope = useMemo<VariableScopeType | undefined>(() => {
        if (currentType === PlatformType.EMBEDDED) {
            return {type: 'EMBEDDED'};
        }

        return currentWorkspaceId != null ? {type: 'WORKSPACE', workspaceId: currentWorkspaceId} : undefined;
    }, [currentType, currentWorkspaceId]);

    const {data} = getVariablesApi().useWorkflowVariablesQuery(scope, currentEnvironmentId);

    return data ?? EMPTY_VARIABLES;
}
```

`getWorkflowInputAndVariableDataPills.ts`:
```ts
import {VariableI} from '@/shared/edition/variables/variablesApi';
import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';

export const VARIABLES_NODE_NAME = 'vars';

export default function getWorkflowInputAndVariableDataPills(
    inputs: WorkflowInput[] | undefined,
    variables: VariableI[]
): DataPillType[] {
    const inputDataPills: DataPillType[] = (inputs ?? []).map((input) => ({
        id: input.name,
        nodeName: input.name,
        value: input.name,
    }));

    const variableDataPills: DataPillType[] = variables.map((variable) => ({
        id: `${VARIABLES_NODE_NAME}.${variable.name}`,
        nodeName: VARIABLES_NODE_NAME,
        value: `${VARIABLES_NODE_NAME}.${variable.name}`,
    }));

    return [...inputDataPills, ...variableDataPills];
}
```
Replace the triplicated block in the three producer hooks with `...getWorkflowInputAndVariableDataPills(workflow.inputs, variables)` where `const variables = useWorkflowVariables();` is called at hook top (respect hook ordering) and added to the `useMemo` deps.

`DataPillPanelBodyVariablesItem.tsx` — mirror `DataPillPanelBodyInputsItem.tsx` (trigger row with `VariableIcon` + "Variables"), props `{dataPillFilterQuery: string}`, `const variables = useWorkflowVariables();`, filter by `variable.name.toLowerCase().includes(dataPillFilterQuery.toLowerCase())`, render:
```tsx
<li className="flex w-full items-center space-x-3" key={variable.id}>
    <DataPill
        parentProperty={{name: VARIABLES_NODE_NAME, type: 'OBJECT' as PropertyType}}
        path={variable.name}
        property={{name: variable.name, type: 'STRING' as PropertyType}}
        sampleOutput={variable.value}
        workflowNodeName={VARIABLES_NODE_NAME}
    />
</li>
```
(so `buildMentionId` yields `vars.NAME`), and empty text "No variables defined." Verify in the browser that the inserted chip serializes to `${vars.NAME}` and renders as available.

`DataPillPanelBody.tsx` — add `variables: VariableI[]` prop, `hasVariables = variables.length > 0`, include it in the empty-state condition, and render after the inputs item:
```tsx
{hasVariables && (
    <AccordionItem className="group" value="variables">
        <DataPillPanelBodyVariablesItem dataPillFilterQuery={dataPillFilterQuery} />
    </AccordionItem>
)}
```
`DataPillPanel.tsx` — `const variables = useWorkflowVariables();` and pass `variables={variables}`.

`getDataPillIconSource.ts` — before the `trigger` branch:
```ts
if (mentionDisplay?.startsWith('${vars.') || componentName === 'vars') {
    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(renderToStaticMarkup(<VariableIcon className="size-4" />))}`;
}
```
(the file is `.ts`; if JSX is not allowed there, render `TYPE_ICONS.STRING` for now and keep the branch for the `vars` prefix — or rename to `.tsx` if the import graph tolerates it; check how `TYPE_ICONS` is defined in `@/shared/typeIcons` and reuse a `VARIABLE` icon entry added there.)

`graphNodeMutations.ts` — after the empty check:
```ts
if (trimmedName === 'vars') {
    return {error: '"vars" is a reserved name.', valid: false};
}
```
`useWorkflowInputs.ts` — in the save handler before the duplicate check: if `input.name === 'vars'`, `setError('name', {message: '"vars" is a reserved name.'})` (react-hook-form's `setError` from the form already in scope) and `return;`.

- [ ] **Step 4: Run + check + manual smoke** — `npx vitest run src/pages/platform/workflow-editor src/pages/platform/cluster-element-editor`; `npm run check`; in the browser open a workflow in an EE workspace with variables: Variables section shows, drag a pill into a text property, save, run the workflow test → the node received the value; the execution's inputs show `vars`.

- [ ] **Step 5: Commit**

```bash
cd client && npm run format > /dev/null 2>&1; git add src/pages/platform src/shared
git commit -m "732 client - Show workspace variables in the data-pill panel and reserve the vars name

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 14: Integration test + docs + CLAUDE.md

**Files:**
- Create: `server/ee/libs/platform/platform-variable/platform-variable-service/src/test/java/com/bytechef/ee/platform/variable/service/VariableServiceIntTest.java` (+ `src/test/resources/config/application-testint.yml` and a `VariableIntTestConfiguration` assembling `PropertyServiceImpl` over the real repository, modelled on `PlatformConfigurationIntTestConfiguration` in `platform-configuration-service/src/test` — including the `EncryptedMapWrapper` converters registration noted there)
- Create: `docs/content/docs/automation/variables.mdx`
- Modify: `CLAUDE.md` (new subsection under "AI Hub Chats"' neighbours, e.g. after "Workspace scoping for platform entities")

- [ ] **Step 1: IntTest** — round-trip through the real `property` table:

```java
@SpringBootTest(classes = VariableIntTestConfiguration.class)
@ActiveProfiles("testint")
class VariableServiceIntTest {

    @Autowired
    private VariableService variableService;

    @Test
    void testCreateListUpdateDeleteRoundTrip() {
        Variable created = variableService.create(VariableScope.workspace(1049L), 0L, "API_URL", "https://a");

        assertThat(variableService.getVariables(VariableScope.workspace(1049L), 0L)).extracting(Variable::name)
            .containsExactly("API_URL");
        assertThat(variableService.getVariables(VariableScope.workspace(1049L), 1L)).isEmpty();
        assertThat(variableService.getVariables(VariableScope.embedded(), 0L)).isEmpty();

        variableService.update(VariableScope.workspace(1049L), 0L, created.id(), "BASE_URL", "https://b");

        assertThat(variableService.getVariableMap(VariableScope.workspace(1049L), 0L))
            .containsExactly(Map.entry("BASE_URL", "https://b"));

        variableService.delete(VariableScope.workspace(1049L), 0L, created.id() /* re-fetch id after rename */);
    }

    @Test
    void testEmbeddedScopeStoresNullScopeId() {
        variableService.create(VariableScope.embedded(), 2L, "REGION", "eu");

        assertThat(variableService.getVariableMap(VariableScope.embedded(), 2L)).containsEntry("REGION", "eu");
        assertThat(variableService.getVariableMap(VariableScope.workspace(1049L), 2L)).doesNotContainKey("REGION");
    }

    @Test
    void testDuplicateNameIsRejected() {
        variableService.create(VariableScope.embedded(), 0L, "DUP", "1");

        assertThatThrownBy(() -> variableService.create(VariableScope.embedded(), 0L, "DUP", "2"))
            .isInstanceOf(ConfigurationException.class);
    }
}
```
(Fix the first test's delete to look the renamed row's id up via `getVariables` before deleting.) Run: `./gradlew :server:ee:libs:platform:platform-variable:platform-variable-service:testIntegration > /tmp/it.log 2>&1; echo $?` (Docker required).

- [ ] **Step 2: Docs page** `docs/content/docs/automation/variables.mdx` — sections: What variables are; Where to define them (Settings → Current Workspace → Variables; embedded: Settings → Variables), per environment; Referencing (`${vars.NAME}`, casts, data-pill panel Variables section, code workflows `context.input().get("vars")`); Rules (name regex, 4096 chars, `vars` reserved, values snapshotted at run start, missing variable leaves the literal, not for secrets). Follow the docs' banner convention for unreleased features (see the memory note "docs track released version").

- [ ] **Step 3: CLAUDE.md** — add:

```markdown
### Variables (workspace / embedded organization, EE)

`server/ee/libs/platform/platform-variable/` (`-api`/`-service`/`-graphql`). One `Property` row per variable:
key `variable.<NAME>`, value `{"value": …}`, `Scope.WORKSPACE`/workspaceId (automation) or `Scope.EMBEDDED`/null
(embedded — its first use), `environment` always set; listed via `PropertyService.getPropertiesByKeyPrefix`. No
table, no changelog. Runtime: `PrincipalJobFacadeImpl` (all four create methods) and `TestWorkflowExecutorImpl` seed
the resolved map into `Job.inputs` under `JobInputConstants.VARIABLES_INPUT` (`"vars"`) through the CE
`WorkflowVariablesResolver` seam (`platform-api`), implemented by `WorkflowVariablesResolverImpl` — fail-open, WARN
once, so distributed EE apps whose `RemotePropertyServiceClient` throws simply run without `vars`. Scope comes from
per-`PlatformType` `VariableScopeProvider`s (`ProjectVariableScopeProvider` in EE automation-configuration-service,
`IntegrationVariableScopeProvider` in embedded-configuration-instance-impl). Editor previews get `vars` through
`WorkflowEvaluationInputsFacade` — the ONE place that merges test-configuration inputs with `vars`; never call
`getWorkflowTestConfigurationInputs` directly from a preview facade. `vars` is reserved as an input and node name
(`WorkflowValidatorFacade` + client). Client: CE editor reads variables through `shared/edition/variables/variablesApi.ts`;
pages `ee/pages/settings/{automation,embedded}/variables`, shared `ee/shared/components/variables`.
Spec: `docs/superpowers/specs/2026-08-17-custom-variables-design.md`.
```

- [ ] **Step 4: Full verification**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
./gradlew check --continue > /tmp/check.log 2>&1; echo $?; grep -n "^> Task .* FAILED" /tmp/check.log
cd client && npm run check > /tmp/ck.log 2>&1; echo $?
```
Fix anything red before committing.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-variable docs/content/docs/automation/variables.mdx CLAUDE.md
git commit -m "732 Add variables integration test, user docs and CLAUDE.md notes

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-review notes (already applied)

- Spec coverage: storage (T1, T3, T4), scope providers + permission scopes (T5), CE seams + reserved name (T2), runtime seeding incl. subflow/linked/sync paths (T7), preview parity (T8), GraphQL (T9), client seam (T10), settings pages (T11–T12), editor panel + pills + icon + client reserved-name checks (T13), IntTest/docs/CLAUDE.md (T14). Non-goals stay untouched.
- Naming is consistent across tasks: `WorkflowVariablesResolver{,Impl}`, `VariableScopeProvider`, `VariableScope.workspace/embedded`, `WorkflowEvaluationInputsFacade{,Impl}`, `getWorkflowInputAndVariableDataPills`, `useWorkflowVariables`, `variablesApi.ts`.
- Known judgment calls the executor may hit: `JobParametersDTO` 7-arg constructor order (read the file), `Instant`→GraphQL `String` mapping (map in the controller if coercion fails), `Project` mocking (final class), and whether `getDataPillIconSource.ts` can render JSX (add a `VARIABLE` entry to `@/shared/typeIcons` if not).
