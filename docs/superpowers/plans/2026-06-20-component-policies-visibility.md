# Component Policies — Component Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an EE "Component Policies" settings page with a "Component Visibility" tab where a tenant admin enables/disables each component; disabled components are hidden from component listings and rejected at execution time.

**Architecture:** A new CE SPI `ComponentVisibilityProvider` (an extension point) is consumed by three CE seams — the listing methods of `ComponentDefinitionServiceImpl`, and the execution funnels `ActionDefinitionServiceImpl.executePerform` / `TriggerDefinitionServiceImpl.executeTrigger`. CE ships zero implementations (behavior unchanged). EE ships one implementation backed by a `component_policy` table, plus a GraphQL surface and a React settings page. No atlas modules are touched.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Spring for GraphQL, Liquibase, React 19 + TypeScript, graphql-codegen, React Query, Radix UI (`Switch`, `Tabs`).

## Global Constraints

- EE files (everything under `server/ee/` and `client/src/ee/`) MUST carry the ByteChef Enterprise license header and Java classes MUST have a `@version ee` Javadoc tag. Spotless selects the header by file CONTENT (`@version ee`), so add the tag to every new EE Java file incl. tests.
- CE files (`server/libs/...`) keep the Apache 2.0 header.
- Admin gating uses exactly: `@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")` where `AuthorityConstants` = `com.bytechef.platform.security.constant.AuthorityConstants` (`AuthorityConstants.ADMIN` = `"ROLE_ADMIN"`). This is the admin-gating form used in this worktree (there is no `hasPermission('Tenant', 'ADMIN')` here).
- `@ConditionalOnEEVersion` = `com.bytechef.platform.annotation.ConditionalOnEEVersion`; requires `bytechef.edition=ee`.
- Persist enum-like columns and keys per existing conventions; `component_policy` uses the component name (VARCHAR) as natural primary key.
- Client conventions: interface names end in `I`/`Props`; named imports sorted alphabetically; lucide icons imported with `Icon` suffix; use `twMerge` not `cn()` in new app code (note: `components/ui/*` primitives still use `cn` — do not touch them); hook ordering (useState → useRef → store hooks → other hooks → derived/useMemo/useCallback → useEffect → return).
- Run `./gradlew spotlessApply` before each server commit; `cd client && npm run check` before each client commit.
- Commit message convention: server `<ticket> <description>`, client `<ticket> client - <description>`. No ticket number is assigned; use `component-policies <description>` / `component-policies client - <description>`.

---

## File Structure

**CE (extension point + enforcement) — `server/libs/platform/platform-component/`**
- Create `platform-component-api/.../component/visibility/ComponentVisibilityProvider.java` — the SPI.
- Modify `platform-component-service/.../service/ComponentDefinitionServiceImpl.java` — listing filter.
- Modify `platform-component-service/.../service/ActionDefinitionServiceImpl.java` — execution guard.
- Modify `platform-component-service/.../service/TriggerDefinitionServiceImpl.java` — execution guard.
- Modify `platform-component-service/.../exception/ActionDefinitionErrorType.java` — add `COMPONENT_DISABLED`.
- Modify `platform-component-service/.../exception/TriggerDefinitionErrorType.java` — add `COMPONENT_DISABLED`.

**EE backend — `server/ee/libs/platform/platform-component-policy/`**
- `platform-component-policy-api/` — `ComponentPolicy` entity, `ComponentPolicyService` interface.
- `platform-component-policy-service/` — repository, `ComponentPolicyServiceImpl`, `ComponentPolicyVisibilityProvider`, JDBC autoconfig, Liquibase changelog.
- `platform-component-policy-graphql/` — `ComponentPolicyGraphQlController`, `.graphqls` schema.

**EE client — `client/`**
- `src/graphql/platform/component-policy/componentPolicies.graphql`, `updateComponentPolicy.graphql`.
- `src/ee/pages/settings/platform/component-policies/ComponentPolicies.tsx`
- `src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.tsx`
- Modify `src/routes.tsx`, `codegen.ts`.

---

## Task 1: CE SPI + listing filter

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/visibility/ComponentVisibilityProvider.java`
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImpl.java`
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImplVisibilityTest.java`

**Interfaces:**
- Produces: `interface ComponentVisibilityProvider { boolean isVisible(String componentName); }` in package `com.bytechef.platform.component.visibility`. Consumed by Tasks 2 and 5.

- [ ] **Step 1: Create the SPI interface**

`ComponentVisibilityProvider.java` (Apache header):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.visibility;

/**
 * Extension point for administrative component visibility. Implementations decide whether a component is visible
 * (enabled) for the current tenant. CE ships no implementation, so all components are visible; EE ships a
 * persistence-backed implementation. Consumed on component listing and before action/trigger execution.
 *
 * @author Ivica Cardic
 */
public interface ComponentVisibilityProvider {

    /**
     * @return {@code true} if the component is visible/enabled for the current tenant.
     */
    boolean isVisible(String componentName);
}
```

- [ ] **Step 2: Write the failing test**

`ComponentDefinitionServiceImplVisibilityTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.service;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComponentDefinitionServiceImplVisibilityTest {

    private final ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
    private final ContextFactory contextFactory = mock(ContextFactory.class);

    private final ComponentDefinitionFilter allowAllAutomationFilter = new ComponentDefinitionFilter() {
        @Override
        public boolean filter(com.bytechef.platform.component.domain.ComponentDefinition componentDefinition) {
            return true;
        }

        @Override
        public boolean supports(PlatformType type) {
            return type == PlatformType.AUTOMATION;
        }
    };

    @Test
    void testDisabledComponentHiddenFromListingButReturnedByNoArgGetter() {
        when(componentDefinitionRegistry.getStaticComponentDefinitions())
            .thenReturn(
                List.of(
                    (com.bytechef.component.definition.ComponentDefinition) component("slack")
                        .title("Slack")
                        .actions(action("sendMessage").title("Send Message")),
                    (com.bytechef.component.definition.ComponentDefinition) component("mailchimp")
                        .title("Mailchimp")
                        .actions(action("addMember").title("Add Member"))));
        when(componentDefinitionRegistry.getDynamicComponentDefinitions())
            .thenReturn(List.of());

        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        ComponentDefinitionServiceImpl service = new ComponentDefinitionServiceImpl(
            List.of(allowAllAutomationFilter), componentDefinitionRegistry, contextFactory, List.of(disableSlack));

        List<ComponentDefinition> listed =
            service.getComponentDefinitions(true, null, null, null, null, PlatformType.AUTOMATION);

        assertThat(listed)
            .extracting(ComponentDefinition::getName)
            .containsExactly("mailchimp");

        assertThat(service.getComponentDefinitions())
            .extracting(ComponentDefinition::getName)
            .contains("slack", "mailchimp");
    }
}
```

- [ ] **Step 3: Run the test — verify it fails to compile**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*ComponentDefinitionServiceImplVisibilityTest"`
Expected: COMPILE FAILURE — `ComponentDefinitionServiceImpl` constructor does not accept a 4th argument.

- [ ] **Step 4: Add the provider list + filter to `ComponentDefinitionServiceImpl`**

Add import after line 39 (`import com.bytechef.platform.constant.PlatformType;`):

```java
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
```

Add a field after line 64 (`private final ComponentDefinitionRegistry componentDefinitionRegistry;`):

```java
    private final List<ComponentVisibilityProvider> componentVisibilityProviders;
```

Replace the constructor (lines 67-75) with:

```java
    @SuppressFBWarnings("EI2")
    public ComponentDefinitionServiceImpl(
        List<ComponentDefinitionFilter> componentDefinitionFilters,
        @Lazy ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        List<ComponentVisibilityProvider> componentVisibilityProviders) {

        this.componentDefinitionFilters = componentDefinitionFilters;
        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextFactory = contextFactory;
        this.componentVisibilityProviders = componentVisibilityProviders;
    }
```

In `getComponentDefinitions(Boolean, ..., PlatformType)` (lines 173-180), add the visibility filter after the platform filter:

```java
        List<ComponentDefinition> components = getComponentDefinitions()
            .stream()
            .filter(componentDefinitionFilter::filter)
            .filter(componentDefinition -> isComponentVisible(componentDefinition.getName()))
            .filter(
                filter(
                    actionDefinitions, clusterElementDefinitions, connectionDefinitions, triggerDefinitions, include))
            .distinct()
            .toList();
```

In `getComponentDefinitions(String query, PlatformType)` (lines 200-206), add the same filter:

```java
        return getComponentDefinitions()
            .stream()
            .filter(componentDefinitionFilter::filter)
            .filter(componentDefinition -> isComponentVisible(componentDefinition.getName()))
            .filter(componentDefinition -> hasMatchingComponent(componentDefinition, lowerCaseQuery) ||
                hasMatchingAction(componentDefinition.getActions(), lowerCaseQuery) ||
                hasMatchingTrigger(componentDefinition.getTriggers(), lowerCaseQuery))
            .toList();
```

Add this private helper just above `private static Predicate<ComponentDefinition> filter(` (line 248):

```java
    private boolean isComponentVisible(String componentName) {
        return componentVisibilityProviders.stream()
            .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isVisible(componentName));
    }

```

- [ ] **Step 5: Update any other direct instantiations of `ComponentDefinitionServiceImpl`**

Run: `grep -rn "new ComponentDefinitionServiceImpl(" server/`
For each hit (excluding the test from Step 2), add `, List.of()` as the final constructor argument. Expected: likely none outside tests; fix any found.

- [ ] **Step 6: Run the test — verify it passes**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*ComponentDefinitionServiceImplVisibilityTest"`
Expected: PASS.

- [ ] **Step 7: Format + commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/visibility/ComponentVisibilityProvider.java \
        server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImpl.java \
        server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImplVisibilityTest.java
git commit -m "component-policies Add ComponentVisibilityProvider SPI and listing filter"
```

---

## Task 2: CE execution guards

**Files:**
- Modify: `.../service/ActionDefinitionServiceImpl.java`
- Modify: `.../service/TriggerDefinitionServiceImpl.java`
- Modify: `.../exception/ActionDefinitionErrorType.java`
- Modify: `.../exception/TriggerDefinitionErrorType.java`
- Test: `.../service/ActionDefinitionServiceImplVisibilityTest.java`
- Test: `.../service/TriggerDefinitionServiceImplVisibilityTest.java`

**Interfaces:**
- Consumes: `ComponentVisibilityProvider` (Task 1).
- Produces: `ActionDefinitionErrorType.COMPONENT_DISABLED`, `TriggerDefinitionErrorType.COMPONENT_DISABLED`.

- [ ] **Step 1: Add error-type constants**

In `ActionDefinitionErrorType.java`, add after line 33 (`EXECUTE_PROCESS_ERROR_RESPONSE`):

```java
    public static final ActionDefinitionErrorType COMPONENT_DISABLED = new ActionDefinitionErrorType(106);
```

In `TriggerDefinitionErrorType.java`, add after line 39 (`TRIGGER_TEST_FAILED`):

```java
    public static final TriggerDefinitionErrorType COMPONENT_DISABLED = new TriggerDefinitionErrorType(110);
```

- [ ] **Step 2: Write the failing action-guard test**

`ActionDefinitionServiceImplVisibilityTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionDefinitionServiceImplVisibilityTest {

    @Test
    void testExecutePerformRejectsDisabledComponent() {
        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        ActionDefinitionServiceImpl service = new ActionDefinitionServiceImpl(
            mock(ComponentDefinitionRegistry.class), mock(ContextFactory.class), List.of(disableSlack));

        assertThatThrownBy(
            () -> service.executePerform(
                "slack", 1, "sendMessage", 1L, 1L, 1L, 1L, "workflow1", Map.of(), Map.of(), Map.of(), 1L, false,
                PlatformType.AUTOMATION, null, null, null))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("disabled");
    }
}
```

- [ ] **Step 3: Run it — verify compile failure**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*ActionDefinitionServiceImplVisibilityTest"`
Expected: COMPILE FAILURE — constructor has no 3rd argument.

- [ ] **Step 4: Add the guard to `ActionDefinitionServiceImpl`**

Add imports (after the existing `com.bytechef.platform.component.*` imports, keeping alphabetical order):

```java
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
```

Add a field after `private final ContextFactory contextFactory;` (line 89):

```java
    private final List<ComponentVisibilityProvider> componentVisibilityProviders;
```

Replace the constructor (lines 91-96) with:

```java
    public ActionDefinitionServiceImpl(
        @Lazy ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        List<ComponentVisibilityProvider> componentVisibilityProviders) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextFactory = contextFactory;
        this.componentVisibilityProviders = componentVisibilityProviders;
    }
```

In `executePerform` (line 195), make the first line of the body (before line 203's `actionDefinition` resolution):

```java
        checkComponentVisible(componentName);

        com.bytechef.component.definition.ActionDefinition actionDefinition = componentDefinitionRegistry
            .getActionDefinition(componentName, componentVersion, actionName);
```

Add this private helper at the end of the class (just before the final closing brace):

```java
    private void checkComponentVisible(String componentName) {
        boolean visible = componentVisibilityProviders.stream()
            .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isVisible(componentName));

        if (!visible) {
            throw new ConfigurationException(
                "Component '%s' is disabled by an administrator and cannot be executed.".formatted(componentName),
                ActionDefinitionErrorType.COMPONENT_DISABLED);
        }
    }
```

- [ ] **Step 5: Update other direct instantiations of `ActionDefinitionServiceImpl`**

Run: `grep -rn "new ActionDefinitionServiceImpl(" server/`
For each hit besides the new test, append `, List.of()` as the final argument.

- [ ] **Step 6: Write + wire the trigger-guard test**

`TriggerDefinitionServiceImplVisibilityTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class TriggerDefinitionServiceImplVisibilityTest {

    @Test
    void testExecuteTriggerRejectsDisabledComponent() {
        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        TriggerDefinitionServiceImpl service = new TriggerDefinitionServiceImpl(
            mock(ComponentDefinitionRegistry.class), mock(ContextFactory.class),
            mock(ApplicationEventPublisher.class), List.of(disableSlack));

        assertThatThrownBy(
            () -> service.executeTrigger(
                "slack", 1, "newMessage", 1L, "uuid", Map.of(), null, null, null, 1L, PlatformType.AUTOMATION,
                false))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("disabled");
    }
}
```

- [ ] **Step 7: Add the guard to `TriggerDefinitionServiceImpl`**

Add import (alphabetically near the other `com.bytechef.platform.component.*` imports):

```java
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
```

(`ConfigurationException` is already imported in this file.)

Add a field after `private final ApplicationEventPublisher eventPublisher;` (line 100):

```java
    private final List<ComponentVisibilityProvider> componentVisibilityProviders;
```

Replace the constructor (lines 102-109) with:

```java
    public TriggerDefinitionServiceImpl(
        @Lazy ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        ApplicationEventPublisher eventPublisher,
        List<ComponentVisibilityProvider> componentVisibilityProviders) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextFactory = contextFactory;
        this.eventPublisher = eventPublisher;
        this.componentVisibilityProviders = componentVisibilityProviders;
    }
```

In `executeTrigger` (line 254), make the first statement of the body (before line 261's `triggerContext`):

```java
        checkComponentVisible(componentName);

        TriggerContext triggerContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, jobPrincipalId, workflowUuid, componentConnection,
            environmentId, type, editorEnvironment);
```

Add this private helper at the end of the class (just before the final closing brace):

```java
    private void checkComponentVisible(String componentName) {
        boolean visible = componentVisibilityProviders.stream()
            .allMatch(componentVisibilityProvider -> componentVisibilityProvider.isVisible(componentName));

        if (!visible) {
            throw new ConfigurationException(
                "Component '%s' is disabled by an administrator and cannot be executed.".formatted(componentName),
                TriggerDefinitionErrorType.COMPONENT_DISABLED);
        }
    }
```

Then run: `grep -rn "new TriggerDefinitionServiceImpl(" server/` and append `, List.of()` to any non-test instantiations.

- [ ] **Step 8: Run both guard tests**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*VisibilityTest"`
Expected: PASS (3 tests across Tasks 1+2).

- [ ] **Step 9: Format + commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ActionDefinitionServiceImpl.java \
        server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/TriggerDefinitionServiceImpl.java \
        server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/exception/ActionDefinitionErrorType.java \
        server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/exception/TriggerDefinitionErrorType.java \
        server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ActionDefinitionServiceImplVisibilityTest.java \
        server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/TriggerDefinitionServiceImplVisibilityTest.java
git commit -m "component-policies Add execution guards for disabled components"
```

---

## Task 3: Scaffold the EE module group

**Files:**
- Create: `server/ee/libs/platform/platform-component-policy/platform-component-policy-api/build.gradle.kts`
- Create: `server/ee/libs/platform/platform-component-policy/platform-component-policy-service/build.gradle.kts`
- Create: `server/ee/libs/platform/platform-component-policy/platform-component-policy-graphql/build.gradle.kts`
- Modify: `settings.gradle.kts`

**Interfaces:**
- Produces: three Gradle modules on the build graph. No runtime code yet.

- [ ] **Step 1: Create the three `build.gradle.kts` files**

`platform-component-policy-api/build.gradle.kts` (mirrors `platform-custom-component-configuration-api` — the API module carries the entity + service interface and uses Spring Data Relational/Commons annotations, NOT spring-data-jdbc):

```kotlin
dependencies {
    api("org.springframework.data:spring-data-commons")

    implementation("org.springframework.data:spring-data-relational")
}
```

`platform-component-policy-service/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:ee:libs:platform:platform-component-policy:platform-component-policy-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
```

`platform-component-policy-graphql/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:ee:libs:platform:platform-component-policy:platform-component-policy-api"))
    implementation(project(":server:ee:libs:platform:platform-component-policy:platform-component-policy-service"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
```

- [ ] **Step 2: Register the modules in `settings.gradle.kts`**

Run: `grep -n "server:ee:libs:platform:platform-audit:platform-audit-api" settings.gradle.kts`
Add these three lines next to the other `server:ee:libs:platform:` includes (keep the file's existing ordering):

```kotlin
include("server:ee:libs:platform:platform-component-policy:platform-component-policy-api")
include("server:ee:libs:platform:platform-component-policy:platform-component-policy-graphql")
include("server:ee:libs:platform:platform-component-policy:platform-component-policy-service")
```

- [ ] **Step 3: Verify the modules resolve**

Run: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-api:dependencies --configuration compileClasspath`
Expected: BUILD SUCCESSFUL (empty source modules resolve their dependency graph).

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts server/ee/libs/platform/platform-component-policy
git commit -m "component-policies Scaffold platform-component-policy EE module group"
```

---

## Task 4: EE entity, repository, service, Liquibase

**Files:**
- Create: `platform-component-policy-api/.../component/policy/ComponentPolicy.java`
- Create: `platform-component-policy-api/.../component/policy/ComponentPolicyService.java`
- Create: `platform-component-policy-service/.../component/policy/repository/ComponentPolicyRepository.java`
- Create: `platform-component-policy-service/.../component/policy/service/ComponentPolicyServiceImpl.java`
- Create: `platform-component-policy-service/.../component/policy/config/ComponentPolicyJdbcRepositoryConfiguration.java`
- Create: `platform-component-policy-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `platform-component-policy-service/src/main/resources/config/liquibase/changelog/platform/component_policy/20260620000001_component_policy_init.xml`
- Modify: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml`
- Test: `platform-component-policy-service/.../component/policy/service/ComponentPolicyServiceImplTest.java`
- Test (IntTest): `platform-component-policy-service/.../component/policy/repository/ComponentPolicyRepositoryIntTest.java`

Base package for all EE files: `com.bytechef.ee.platform.component.policy`.

**Interfaces:**
- Produces:
  - `class ComponentPolicy` — natural-key entity (`String componentName`, `boolean enabled`, audit cols, `int version`).
  - `interface ComponentPolicyService { boolean isEnabled(String componentName); Set<String> getDisabledComponentNames(); ComponentPolicy updateComponentPolicy(String componentName, boolean enabled); }`
- Consumed by Tasks 5 and 6.

- [ ] **Step 1: Create the entity** (EE header + `@version ee`)

`ComponentPolicy.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

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
 * Tenant-wide administrative visibility override for a single component, keyed by component name. A missing row means
 * the component is enabled (default-on); {@code enabled = false} hides it from listings and blocks its execution.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("component_policy")
public class ComponentPolicy {

    @Id
    @Column("component_name")
    private String componentName;

    @Column("enabled")
    private boolean enabled = true;

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

    public ComponentPolicy() {
    }

    public ComponentPolicy(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
```

> Note: `@Version int version` lets Spring Data JDBC distinguish INSERT (version 0 on a freshly constructed row) from UPDATE (version > 0 after load), which is exactly the upsert behavior `updateComponentPolicy` needs despite the non-generated String `@Id`.

- [ ] **Step 2: Create the service interface**

`ComponentPolicyService.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import java.util.Set;

/**
 * Tenant-wide component visibility policy operations.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ComponentPolicyService {

    boolean isEnabled(String componentName);

    Set<String> getDisabledComponentNames();

    ComponentPolicy updateComponentPolicy(String componentName, boolean enabled);
}
```

- [ ] **Step 3: Create the repository**

`ComponentPolicyRepository.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.repository;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface ComponentPolicyRepository extends CrudRepository<ComponentPolicy, String> {

    List<ComponentPolicy> findByEnabled(boolean enabled);
}
```

> Mirrors `CustomComponentRepository` in this worktree (`@Repository @ConditionalOnEEVersion` on the interface). `platform-component-policy-service/build.gradle.kts` must therefore also depend on `platform-api` (for `@ConditionalOnEEVersion`) — it already does.

- [ ] **Step 4: Write the failing service test**

`ComponentPolicyServiceImplTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.repository.ComponentPolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentPolicyServiceImplTest {

    private final ComponentPolicyRepository componentPolicyRepository = mock(ComponentPolicyRepository.class);
    private final ComponentPolicyServiceImpl componentPolicyService =
        new ComponentPolicyServiceImpl(componentPolicyRepository);

    @Test
    void testIsEnabledDefaultsToTrueWhenNoRow() {
        when(componentPolicyRepository.findById("slack")).thenReturn(Optional.empty());

        assertThat(componentPolicyService.isEnabled("slack")).isTrue();
    }

    @Test
    void testIsEnabledReflectsStoredRow() {
        ComponentPolicy componentPolicy = new ComponentPolicy("slack");

        componentPolicy.setEnabled(false);

        when(componentPolicyRepository.findById("slack")).thenReturn(Optional.of(componentPolicy));

        assertThat(componentPolicyService.isEnabled("slack")).isFalse();
    }

    @Test
    void testGetDisabledComponentNames() {
        ComponentPolicy componentPolicy = new ComponentPolicy("slack");

        componentPolicy.setEnabled(false);

        when(componentPolicyRepository.findByEnabled(false)).thenReturn(List.of(componentPolicy));

        assertThat(componentPolicyService.getDisabledComponentNames()).containsExactly("slack");
    }

    @Test
    void testUpdateComponentPolicyUpserts() {
        when(componentPolicyRepository.findById("slack")).thenReturn(Optional.empty());
        when(componentPolicyRepository.save(any(ComponentPolicy.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ComponentPolicy result = componentPolicyService.updateComponentPolicy("slack", false);

        assertThat(result.getComponentName()).isEqualTo("slack");
        assertThat(result.isEnabled()).isFalse();
    }
}
```

- [ ] **Step 5: Run it — verify compile failure**

Run: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-service:test --tests "*ComponentPolicyServiceImplTest"`
Expected: COMPILE FAILURE — `ComponentPolicyServiceImpl` does not exist.

- [ ] **Step 6: Implement the service**

`ComponentPolicyServiceImpl.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.service;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicyService;
import com.bytechef.ee.platform.component.policy.repository.ComponentPolicyRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ComponentPolicyServiceImpl implements ComponentPolicyService {

    private final ComponentPolicyRepository componentPolicyRepository;

    public ComponentPolicyServiceImpl(ComponentPolicyRepository componentPolicyRepository) {
        this.componentPolicyRepository = componentPolicyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(String componentName) {
        return componentPolicyRepository.findById(componentName)
            .map(ComponentPolicy::isEnabled)
            .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getDisabledComponentNames() {
        return componentPolicyRepository.findByEnabled(false)
            .stream()
            .map(ComponentPolicy::getComponentName)
            .collect(Collectors.toSet());
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentPolicy updateComponentPolicy(String componentName, boolean enabled) {
        ComponentPolicy componentPolicy = componentPolicyRepository.findById(componentName)
            .orElseGet(() -> new ComponentPolicy(componentName));

        componentPolicy.setEnabled(enabled);

        return componentPolicyRepository.save(componentPolicy);
    }
}
```

- [ ] **Step 7: Create the JDBC autoconfiguration**

`ComponentPolicyJdbcRepositoryConfiguration.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.component.policy.repository")
public class ComponentPolicyJdbcRepositoryConfiguration {
}
```

Create `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` with:

```
com.bytechef.ee.platform.component.policy.config.ComponentPolicyJdbcRepositoryConfiguration
```

- [ ] **Step 8: Create the Liquibase changelog**

`20260620000001_component_policy_init.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260620000001" author="Ivica Cardic">
        <createTable tableName="component_policy">
            <column name="component_name" type="VARCHAR(256)">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
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
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 9: Register the changelog in `master.xml`**

Run: `grep -n "custom_component" server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml`
That existing EE platform table is the template. Add an analogous line near the other `changelog/platform/` includes:

```xml
    <includeAll path="classpath:config/liquibase/changelog/platform/component_policy/" relativeToChangelogFile="false" errorIfMissingOrEmpty="false" contextFilter="mono or configuration or multitenant" />
```

> The confirmed `contextFilter` for EE platform configuration tables in this worktree is `mono or configuration or multitenant` (same as `platform/custom_component/`). Match the trailing-slash path style of the existing entries.

- [ ] **Step 10: Run the service test — verify pass**

Run: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-service:test --tests "*ComponentPolicyServiceImplTest"`
Expected: PASS (4 tests).

- [ ] **Step 11: Write the repository integration test**

`ComponentPolicyRepositoryIntTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentPolicyRepositoryIntTest extends AbstractComponentPolicyIntTest {

    @Autowired
    private ComponentPolicyRepository componentPolicyRepository;

    @Test
    void testInsertThenUpdateRoundTrip() {
        ComponentPolicy componentPolicy = new ComponentPolicy("slack");

        componentPolicy.setEnabled(false);

        ComponentPolicy saved = componentPolicyRepository.save(componentPolicy);

        assertThat(componentPolicyRepository.findByEnabled(false))
            .extracting(ComponentPolicy::getComponentName)
            .containsExactly("slack");

        saved.setEnabled(true);

        componentPolicyRepository.save(saved);

        assertThat(componentPolicyRepository.findByEnabled(false)).isEmpty();
    }
}
```

> Create a minimal `AbstractComponentPolicyIntTest` test-config in `src/test/java/.../repository/` annotated with `@SpringBootTest`, `@ActiveProfiles("testint")`, importing the project's standard JDBC integration test configuration (mirror an existing `*IntTest` base class in a sibling EE service module — find one via `grep -rln "ActiveProfiles(\"testint\")" server/ee/libs/platform`). It must enable the `ComponentPolicyJdbcRepositoryConfiguration` and provide auditing beans so `@CreatedDate`/`@CreatedBy` populate. If wiring auditing in the test proves heavy, drop the audit-column `nullable=false` requirement is NOT acceptable; instead supply an `AuditorAware<String>` test bean returning `"system"` and a JDBC auditing config import.

- [ ] **Step 12: Run the integration test**

Run: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-service:testIntegration --tests "*ComponentPolicyRepositoryIntTest"`
Expected: PASS (requires Docker for Testcontainers).

- [ ] **Step 13: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-component-policy/platform-component-policy-api \
        server/ee/libs/platform/platform-component-policy/platform-component-policy-service \
        server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml
git commit -m "component-policies Add component_policy entity, repository, service and migration"
```

---

## Task 5: EE visibility provider

**Files:**
- Create: `platform-component-policy-service/.../component/policy/ComponentPolicyVisibilityProvider.java`
- Test: `platform-component-policy-service/.../component/policy/ComponentPolicyVisibilityProviderTest.java`

**Interfaces:**
- Consumes: `ComponentVisibilityProvider` (Task 1), `ComponentPolicyService` (Task 4).
- Produces: the EE `@Component` bean that CE's `ComponentDefinitionServiceImpl`/`ActionDefinitionServiceImpl`/`TriggerDefinitionServiceImpl` pick up via their injected `List<ComponentVisibilityProvider>`.

- [ ] **Step 1: Write the failing test**

`ComponentPolicyVisibilityProviderTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentPolicyVisibilityProviderTest {

    private final ComponentPolicyService componentPolicyService = mock(ComponentPolicyService.class);
    private final ComponentPolicyVisibilityProvider componentPolicyVisibilityProvider =
        new ComponentPolicyVisibilityProvider(componentPolicyService);

    @Test
    void testIsVisibleReflectsDisabledSet() {
        when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of("slack"));

        assertThat(componentPolicyVisibilityProvider.isVisible("slack")).isFalse();
        assertThat(componentPolicyVisibilityProvider.isVisible("mailchimp")).isTrue();
    }
}
```

- [ ] **Step 2: Run it — verify compile failure**

Run: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-service:test --tests "*ComponentPolicyVisibilityProviderTest"`
Expected: COMPILE FAILURE.

- [ ] **Step 3: Implement the provider (with a short per-tenant cache)**

`ComponentPolicyVisibilityProvider.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * EE implementation of {@link ComponentVisibilityProvider} backed by the {@code component_policy} table. The disabled
 * set is cached per tenant for a short window to keep component listings (which probe every component) off the
 * database; administrative toggles take effect within the cache TTL.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ComponentPolicyVisibilityProvider implements ComponentVisibilityProvider {

    private final Cache<String, Set<String>> disabledComponentNamesCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(10))
        .build();
    private final ComponentPolicyService componentPolicyService;

    public ComponentPolicyVisibilityProvider(ComponentPolicyService componentPolicyService) {
        this.componentPolicyService = componentPolicyService;
    }

    @Override
    public boolean isVisible(String componentName) {
        Set<String> disabledComponentNames = disabledComponentNamesCache.get(
            TenantContext.getCurrentTenantId(), tenantId -> componentPolicyService.getDisabledComponentNames());

        return !disabledComponentNames.contains(componentName);
    }
}
```

> The unit test exercises a fresh provider, so the 10s cache loads from the mock on first call. Eventual consistency (≤10s) is the documented MVP tradeoff from the spec.

- [ ] **Step 4: Run the test — verify pass**

Run: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-service:test --tests "*ComponentPolicyVisibilityProviderTest"`
Expected: PASS.

- [ ] **Step 5: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/main/java/com/bytechef/ee/platform/component/policy/ComponentPolicyVisibilityProvider.java \
        server/ee/libs/platform/platform-component-policy/platform-component-policy-service/src/test/java/com/bytechef/ee/platform/component/policy/ComponentPolicyVisibilityProviderTest.java
git commit -m "component-policies Add EE ComponentPolicyVisibilityProvider"
```

---

## Task 6: EE GraphQL surface

**Files:**
- Create: `platform-component-policy-graphql/.../component/policy/web/graphql/ComponentPolicyGraphQlController.java`
- Create: `platform-component-policy-graphql/src/main/resources/graphql/component-policy.graphqls`
- Test: `platform-component-policy-graphql/.../component/policy/web/graphql/ComponentPolicyGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `ComponentPolicyService` (Task 4), `ComponentDefinitionService` (`com.bytechef.platform.component.service.ComponentDefinitionService`, returning domain `ComponentDefinition` with `getName/getTitle/getIcon/getVersion`).
- Produces: GraphQL `componentPolicies: [ComponentPolicy!]!` and `updateComponentPolicy(name, enabled): ComponentPolicy!`.

- [ ] **Step 1: Create the GraphQL schema**

`component-policy.graphqls`:

```graphql
type ComponentPolicy {
    name: String!
    title: String
    icon: String
    version: Int!
    enabled: Boolean!
}

extend type Query {
    """
    Lists every registry component with its tenant-wide visibility flag. Components with no policy row are reported
    enabled. Admin-only.
    """
    componentPolicies: [ComponentPolicy!]!
}

extend type Mutation {
    """
    Enables or disables a component tenant-wide. Admin-only.
    """
    updateComponentPolicy(name: String!, enabled: Boolean!): ComponentPolicy!
}
```

- [ ] **Step 2: Write the failing resolver test**

`ComponentPolicyGraphQlControllerTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicyService;
import com.bytechef.ee.platform.component.policy.web.graphql.ComponentPolicyGraphQlController.ComponentPolicyItem;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentPolicyGraphQlControllerTest {

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ComponentPolicyService componentPolicyService = mock(ComponentPolicyService.class);
    private final ComponentPolicyGraphQlController controller =
        new ComponentPolicyGraphQlController(componentDefinitionService, componentPolicyService);

    @Test
    void testComponentPoliciesMergesDisabledFlag() {
        ComponentDefinition slack = mock(ComponentDefinition.class);
        ComponentDefinition mailchimp = mock(ComponentDefinition.class);

        when(slack.getName()).thenReturn("slack");
        when(slack.getTitle()).thenReturn("Slack");
        when(slack.getVersion()).thenReturn(1);
        when(mailchimp.getName()).thenReturn("mailchimp");
        when(mailchimp.getTitle()).thenReturn("Mailchimp");
        when(mailchimp.getVersion()).thenReturn(1);

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(slack, mailchimp));
        when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of("slack"));

        List<ComponentPolicyItem> result = controller.componentPolicies();

        assertThat(result)
            .extracting(ComponentPolicyItem::name, ComponentPolicyItem::enabled)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("mailchimp", true),
                org.assertj.core.groups.Tuple.tuple("slack", false));
    }

    @Test
    void testUpdateComponentPolicyReturnsUpdatedItem() {
        ComponentPolicy componentPolicy = new ComponentPolicy("slack");

        componentPolicy.setEnabled(false);

        ComponentDefinition slack = mock(ComponentDefinition.class);

        when(slack.getName()).thenReturn("slack");
        when(slack.getTitle()).thenReturn("Slack");
        when(slack.getVersion()).thenReturn(1);

        when(componentPolicyService.updateComponentPolicy("slack", false)).thenReturn(componentPolicy);
        when(componentDefinitionService.getComponentDefinition(any(), any())).thenReturn(slack);

        ComponentPolicyItem result = controller.updateComponentPolicy("slack", false);

        assertThat(result.name()).isEqualTo("slack");
        assertThat(result.enabled()).isFalse();
    }
}
```

- [ ] **Step 3: Run it — verify compile failure**

Run: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-graphql:test --tests "*ComponentPolicyGraphQlControllerTest"`
Expected: COMPILE FAILURE.

- [ ] **Step 4: Implement the resolver**

`ComponentPolicyGraphQlController.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.web.graphql;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for tenant-wide component visibility policies. Admin-only.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
public class ComponentPolicyGraphQlController {

    private final ComponentDefinitionService componentDefinitionService;
    private final ComponentPolicyService componentPolicyService;

    @SuppressFBWarnings("EI2")
    public ComponentPolicyGraphQlController(
        ComponentDefinitionService componentDefinitionService, ComponentPolicyService componentPolicyService) {

        this.componentDefinitionService = componentDefinitionService;
        this.componentPolicyService = componentPolicyService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<ComponentPolicyItem> componentPolicies() {
        Set<String> disabledComponentNames = componentPolicyService.getDisabledComponentNames();

        return componentDefinitionService.getComponentDefinitions()
            .stream()
            .map(componentDefinition -> toItem(componentDefinition, disabledComponentNames))
            .sorted(Comparator.comparing(ComponentPolicyItem::sortKey, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentPolicyItem updateComponentPolicy(@Argument String name, @Argument boolean enabled) {
        ComponentPolicy componentPolicy = componentPolicyService.updateComponentPolicy(name, enabled);

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(name, null);

        return new ComponentPolicyItem(
            componentDefinition.getName(), componentDefinition.getTitle(), componentDefinition.getIcon(),
            componentDefinition.getVersion(), componentPolicy.isEnabled());
    }

    private static ComponentPolicyItem toItem(
        ComponentDefinition componentDefinition, Set<String> disabledComponentNames) {

        return new ComponentPolicyItem(
            componentDefinition.getName(), componentDefinition.getTitle(), componentDefinition.getIcon(),
            componentDefinition.getVersion(), !disabledComponentNames.contains(componentDefinition.getName()));
    }

    public record ComponentPolicyItem(
        String name, @Nullable String title, @Nullable String icon, int version, boolean enabled) {

        String sortKey() {
            return title == null ? name : title;
        }
    }
}
```

- [ ] **Step 5: Run the resolver test — verify pass**

Run: `./gradlew :server:ee:libs:platform:platform-component-policy:platform-component-policy-graphql:test --tests "*ComponentPolicyGraphQlControllerTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Wire the EE modules into the running app(s)**

The GraphQL controller and service beans must be on the application classpath so they load (and so CE picks up the provider). In this worktree the single place that aggregates EE platform graphql/service modules is `server/apps/server-app/build.gradle.kts`. Confirm with:

```bash
grep -n "platform-custom-component-configuration-graphql\|platform-custom-component-configuration-service" server/apps/server-app/build.gradle.kts
```

Add, next to those existing entries (keep the file's alphabetical-ish grouping):

```kotlin
    implementation(project(":server:ee:libs:platform:platform-component-policy:platform-component-policy-graphql"))
    implementation(project(":server:ee:libs:platform:platform-component-policy:platform-component-policy-service"))
```

> The `-service` module carries the `@Service`/`@Component` beans (incl. the `ComponentPolicyVisibilityProvider` that CE's services pick up) and the JDBC autoconfig + Liquibase changelog; the `-graphql` module carries the resolver + schema (no autoconfig — the `@Controller` is found by component scan).

- [ ] **Step 7: Compile the whole server to confirm wiring**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-component-policy/platform-component-policy-graphql <app build files modified in Step 6>
git commit -m "component-policies Add GraphQL surface and wire EE modules into app"
```

---

## Task 7: Client GraphQL operations + codegen

**Files:**
- Create: `client/src/graphql/platform/component-policy/componentPolicies.graphql`
- Create: `client/src/graphql/platform/component-policy/updateComponentPolicy.graphql`
- Modify: `client/codegen.ts`
- Regenerates: `client/src/shared/middleware/graphql.ts`, `client/src/shared/middleware/graphql-types.ts`

**Interfaces:**
- Produces: generated hooks `useComponentPoliciesQuery`, `useUpdateComponentPolicyMutation`, types `ComponentPolicy`. Consumed by Task 8.

- [ ] **Step 1: Add the schema path to `codegen.ts`**

In the `schema` array, add (alphabetically near other `server/ee/libs/platform` entries):

```typescript
    '../server/ee/libs/platform/platform-component-policy/platform-component-policy-graphql/src/main/resources/graphql/*.graphqls',
```

- [ ] **Step 2: Create the query operation**

`componentPolicies.graphql`:

```graphql
query ComponentPolicies {
    componentPolicies {
        name
        title
        icon
        version
        enabled
    }
}
```

- [ ] **Step 3: Create the mutation operation**

`updateComponentPolicy.graphql`:

```graphql
mutation UpdateComponentPolicy($name: String!, $enabled: Boolean!) {
    updateComponentPolicy(name: $name, enabled: $enabled) {
        name
        title
        icon
        version
        enabled
    }
}
```

- [ ] **Step 4: Run codegen**

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` now exports `useComponentPoliciesQuery` and `useUpdateComponentPolicyMutation`. Verify:

Run: `cd client && grep -n "useComponentPoliciesQuery\|useUpdateComponentPolicyMutation" src/shared/middleware/graphql.ts`
Expected: both symbols present.

- [ ] **Step 5: Typecheck + commit**

```bash
cd client && npm run typecheck
cd ..
git add client/codegen.ts client/src/graphql/platform/component-policy client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "component-policies client - Add component policy GraphQL operations and codegen"
```

---

## Task 8: Client settings page + Component Visibility tab

**Files:**
- Create: `client/src/ee/pages/settings/platform/component-policies/ComponentPolicies.tsx`
- Create: `client/src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.tsx`
- Modify: `client/src/routes.tsx`
- Test: `client/src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.test.tsx`

**Interfaces:**
- Consumes: `useComponentPoliciesQuery`, `useUpdateComponentPolicyMutation` (Task 7); `Tabs`/`TabsList`/`TabsTrigger`/`TabsContent` from `@/components/ui/tabs`; `Switch` from `@/components/ui/switch`; `LayoutContainer`, `Header`, `PageLoader`.

- [ ] **Step 1: Create the page shell with the single tab**

`ComponentPolicies.tsx`:

```tsx
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

import ComponentVisibilityTab from './components/ComponentVisibilityTab';

const ComponentPolicies = () => {
    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="Component Policies" />}
            leftSidebarOpen={false}
        >
            <Tabs className="size-full p-4" defaultValue="component-visibility">
                <TabsList>
                    <TabsTrigger value="component-visibility">Component Visibility</TabsTrigger>
                </TabsList>

                <TabsContent value="component-visibility">
                    <ComponentVisibilityTab />
                </TabsContent>
            </Tabs>
        </LayoutContainer>
    );
};

export default ComponentPolicies;
```

- [ ] **Step 2: Write the failing tab test**

`ComponentVisibilityTab.test.tsx`:

```tsx
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ComponentVisibilityTab from './ComponentVisibilityTab';

const mutateMock = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useComponentPoliciesQuery: () => ({
        data: {
            componentPolicies: [
                {enabled: true, icon: null, name: 'mailchimp', title: 'Mailchimp', version: 1},
                {enabled: false, icon: null, name: 'slack', title: 'Slack', version: 1},
            ],
        },
        error: null,
        isLoading: false,
    }),
    useUpdateComponentPolicyMutation: () => ({mutate: mutateMock}),
}));

describe('ComponentVisibilityTab', () => {
    beforeEach(() => {
        mutateMock.mockClear();
    });

    it('renders a switch per component reflecting enabled state', () => {
        render(<ComponentVisibilityTab />);

        const switches = screen.getAllByRole('switch');

        expect(switches).toHaveLength(2);
    });

    it('calls the mutation when a switch is toggled', async () => {
        render(<ComponentVisibilityTab />);

        const slackSwitch = screen.getByRole('switch', {name: /slack/i});

        await userEvent.click(slackSwitch);

        expect(mutateMock).toHaveBeenCalledWith(
            expect.objectContaining({enabled: true, name: 'slack'})
        );
    });
});
```

- [ ] **Step 3: Run it — verify failure**

Run: `cd client && npx vitest run src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.test.tsx`
Expected: FAIL — module `ComponentVisibilityTab` not found.

- [ ] **Step 4: Implement the tab with optimistic update**

`ComponentVisibilityTab.tsx`:

```tsx
import {Input} from '@/components/ui/input';
import {Switch} from '@/components/ui/switch';
import PageLoader from '@/components/PageLoader';
import {
    type ComponentPoliciesQuery,
    useComponentPoliciesQuery,
    useUpdateComponentPolicyMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {SearchIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

type ComponentPolicyItemType = ComponentPoliciesQuery['componentPolicies'][number];

const ComponentVisibilityTab = () => {
    const [search, setSearch] = useState('');

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useComponentPoliciesQuery();

    const updateComponentPolicyMutation = useUpdateComponentPolicyMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ['ComponentPolicies']});
        },
        onMutate: async ({enabled, name}: {enabled: boolean; name: string}) => {
            await queryClient.cancelQueries({queryKey: ['ComponentPolicies']});

            const previous = queryClient.getQueryData<ComponentPoliciesQuery>(['ComponentPolicies']);

            queryClient.setQueryData<ComponentPoliciesQuery>(['ComponentPolicies'], (current) =>
                current
                    ? {
                          componentPolicies: current.componentPolicies.map((componentPolicy) =>
                              componentPolicy.name === name ? {...componentPolicy, enabled} : componentPolicy
                          ),
                      }
                    : current
            );

            return {previous};
        },
    });

    const componentPolicies = useMemo(
        () =>
            (data?.componentPolicies ?? []).filter((componentPolicy) => {
                const haystack = `${componentPolicy.title ?? ''} ${componentPolicy.name}`.toLowerCase();

                return haystack.includes(search.toLowerCase());
            }),
        [data?.componentPolicies, search]
    );

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <div className="mt-4 flex flex-col gap-4">
                <div className="relative max-w-sm">
                    <SearchIcon className="absolute left-2 top-2.5 size-4 text-muted-foreground" />

                    <Input
                        className="pl-8"
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder="Search components"
                        value={search}
                    />
                </div>

                <ul className="divide-y rounded-md border">
                    {componentPolicies.map((componentPolicy: ComponentPolicyItemType) => (
                        <li className="flex items-center justify-between gap-3 px-4 py-3" key={componentPolicy.name}>
                            <div className="flex items-center gap-3">
                                {componentPolicy.icon ? (
                                    <InlineSVG className="size-6 flex-none" src={componentPolicy.icon} />
                                ) : (
                                    <span className="size-6 flex-none rounded bg-muted" />
                                )}

                                <div className="flex flex-col">
                                    <span className="text-sm font-semibold">
                                        {componentPolicy.title ?? componentPolicy.name}
                                    </span>

                                    <span className={twMerge('text-xs text-muted-foreground')}>
                                        {componentPolicy.name}
                                    </span>
                                </div>
                            </div>

                            <Switch
                                aria-label={componentPolicy.title ?? componentPolicy.name}
                                checked={componentPolicy.enabled}
                                onCheckedChange={(checked) =>
                                    updateComponentPolicyMutation.mutate({
                                        enabled: checked,
                                        name: componentPolicy.name,
                                    })
                                }
                            />
                        </li>
                    ))}
                </ul>
            </div>
        </PageLoader>
    );
};

export default ComponentVisibilityTab;
```

> If the generated `use*Mutation` hook signature differs (codegen sometimes returns `mutate(variables)` only), adapt the `onMutate`/`onError` wiring to the generated `UseMutationOptions` shape — the generated hook accepts a React Query options object as its first argument. Confirm by reading the generated `useUpdateComponentPolicyMutation` signature in `graphql.ts`.

- [ ] **Step 5: Run the test — verify pass**

Run: `cd client && npx vitest run src/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 6: Wire the route + nav item in `routes.tsx`**

Add the lazy import near the other EE settings lazy imports (around line 94):

```typescript
const ComponentPolicies = lazy(() => import('@/ee/pages/settings/platform/component-policies/ComponentPolicies'));
```

Add a child route to `platformSettingsRoutes.children` (mirror the `custom-components` entry):

```typescript
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <ComponentPolicies />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'component-policies',
                },
```

Add a nav item to `platformSettingsRoutes.navItems` (place near `custom-components`):

```typescript
                {
                    href: 'component-policies',
                    title: 'Component Policies',
                },
```

> Match the exact symbol names already imported in `routes.tsx` (`PrivateRoute`, `EEVersion`, `LazyLoadWrapper`, `AUTHORITIES`). If `custom-components` uses different gating wrappers, copy that entry's exact wrappers instead.

- [ ] **Step 7: Full client check**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests PASS. Fix any sort-keys / import-order / interface-naming violations the linter reports (these are not auto-fixed).

- [ ] **Step 8: Commit**

```bash
cd client && npm run format
cd ..
git add client/src/ee/pages/settings/platform/component-policies client/src/routes.tsx
git commit -m "component-policies client - Add Component Policies page with Component Visibility tab"
```

---

## Final verification

- [ ] **Server:** `./gradlew spotlessApply check` (or at minimum `./gradlew :server:libs:platform:platform-component:platform-component-service:test` + the new EE module `test`/`testIntegration`).
- [ ] **Client:** `cd client && npm run check`.
- [ ] **Manual smoke (optional, requires running app with `bytechef.edition=ee`):** Settings → Component Policies → Component Visibility → toggle a component off → confirm it disappears from the workflow editor component picker, and a workflow whose definition references it fails on execution with the "disabled by an administrator" message.

---

## Self-Review notes (for the implementer)

- **Spec coverage:** Listing filter (Task 1) + execution guard (Task 2) = "hide from listing AND block execution". Tenant-wide table (Task 4). EE placement throughout. Immediate per-toggle optimistic save (Task 8). All registry components via unfiltered `getComponentDefinitions()` in the resolver (Task 6). Admin gating via `hasAuthority(AuthorityConstants.ADMIN)` (Tasks 4, 6). Single tab structured for future Rules/Restrictions/Claims (Task 8).
- **Open items from the spec, resolved here:** admin authority = `hasAuthority(AuthorityConstants.ADMIN)` (= `ROLE_ADMIN`, the form used in this worktree); execution exception = `ConfigurationException` + new `COMPONENT_DISABLED` error types; cache = 10s per-tenant Caffeine in the EE provider; Liquibase changelog under the EE service module + `master.xml` include (`platform/custom_component/` is the template).
- **Risk flags to watch during execution:** (1) the exact `master.xml` `contextFilter` and the app build file(s) that aggregate EE graphql/service modules must be confirmed by grep, not assumed; (2) the generated React Query mutation hook's options shape (Task 8 Step 4 note); (3) the integration-test base config for auditing beans (Task 4 Step 11).
```