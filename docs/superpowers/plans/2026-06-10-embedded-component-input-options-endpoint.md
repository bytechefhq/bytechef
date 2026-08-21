# Dedicated Component-Input Options Endpoint for Embedded ConnectDialog — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve component-defined workflow input options (e.g. the Slack `channel` picker) in the embedded ConnectDialog by executing the component's own input-level `OptionsFunction` directly — keyed by component reference + the integration instance's connection — and remove the old node-coupled path.

**Architecture:** Property resolution lands on `ComponentDefinitionRegistry.getComponentInputProperty` (mirror of `getActionProperty`). Execution lands on a new `ComponentDefinitionFacade` (facade tier — the codebase's home for execute-with-connection), which resolves `connectionId → ComponentConnection` and delegates to a new `ComponentDefinitionService.executeWorkflowInputOptions` engine method. EE distributed mode is served by a new `RemoteComponentDefinitionFacadeClient` (POSTs `connectionId` to a worker) + `RemoteComponentDefinitionFacadeController`, exactly mirroring the `ActionDefinitionFacade` remoting. A new public endpoint `POST /api/embedded/v1/integration-instances/{id}/component-input-options` (component-ref-keyed) replaces the old `/workflows/{uuid}/options`. The SDK retargets its options hook to the new endpoint.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, OpenAPI generator, React 19 + TypeScript (embedded SDK), Vitest, JUnit 5 + Mockito.

**Spec:** `docs/superpowers/specs/2026-06-10-embedded-component-input-options-endpoint-design.md`

---

## File Structure

**Platform (created):**
- `server/libs/platform/platform-component/platform-component-api/.../facade/ComponentDefinitionFacade.java` — new facade interface; one method `executeWorkflowInputOptions(..., @Nullable Long connectionId)`.
- `server/libs/platform/platform-component/platform-component-service/.../facade/ComponentDefinitionFacadeImpl.java` — resolves connection, delegates to service.

**Platform (modified):**
- `.../platform-component-api/.../service/ComponentDefinitionService.java` — add `executeWorkflowInputOptions(..., @Nullable ComponentConnection)`.
- `.../platform-component-service/.../service/ComponentDefinitionServiceImpl.java` — implement engine (inject `ContextFactory`).
- `.../platform-component-service/.../ComponentDefinitionRegistry.java` — add `getComponentInputProperty(...)`.

**EE remote (created/modified):**
- `server/ee/libs/platform/platform-component/platform-component-remote-client/.../facade/RemoteComponentDefinitionFacadeClient.java` — new.
- `.../platform-component-remote-rest/.../facade/RemoteComponentDefinitionFacadeController.java` — new.
- `.../platform-component-remote-client/.../service/RemoteComponentDefinitionServiceClient.java` — add unsupported method.

**EE embedded (modified/deleted):**
- `.../embedded-configuration-api/.../facade/ConnectedUserIntegrationInstanceFacade.java` — swap options method.
- `.../embedded-configuration-service/.../facade/ConnectedUserIntegrationInstanceFacadeImpl.java` — swap options method + dependency.
- `.../embedded-configuration-api/.../facade/EmbeddedWorkflowInputOptionFacade.java` — **delete**.
- `.../embedded-configuration-service/.../facade/EmbeddedWorkflowInputOptionFacadeImpl.java` — **delete**.
- `.../embedded-configuration-public-rest/openapi.yaml` — remove old ops + `WorkflowInputOptionsRequest`; add `component-input-options` op + `ComponentInputOptionsRequest`.
- `.../embedded-configuration-public-rest/.../web/rest/IntegrationInstanceWorkflowApiController.java` — remove old handlers; add new handlers (see Task 8).

**SDK (modified):**
- `sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.ts` — `optionsCacheKey` signature.
- `.../connect-dialog/useWorkflowInputOptions.ts` — endpoint + body + cache key.
- `.../connect-dialog/ConnectDialog.tsx` — thread `componentReference` into the options call.
- `.../connect-dialog/*.test.ts(x)` — update contracts.

---

## Task 1: `ComponentDefinitionRegistry.getComponentInputProperty`

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/ComponentDefinitionRegistry.java`
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/ComponentDefinitionRegistryTest.java`

- [ ] **Step 1: Write the failing test**

Append this test method to `ComponentDefinitionRegistryTest` (it already constructs a registry over the on-classpath components — follow the existing test's setup for obtaining the `componentDefinitionRegistry` instance and a no-op `ActionContext` mock):

```java
@Test
public void testGetComponentInputProperty() throws Exception {
    Property property = componentDefinitionRegistry.getComponentInputProperty(
        "slack", 1, "channel", "channel", ParametersFactory.create(Map.of()),
        ParametersFactory.create((ComponentConnection) null), Map.of(), Mockito.mock(ActionContext.class));

    Assertions.assertThat(property.getName())
        .isEqualTo("channel");
    Assertions.assertThat(property)
        .isInstanceOf(DynamicOptionsProperty.class);
}
```

Add imports as needed: `com.bytechef.component.definition.ActionContext`, `com.bytechef.component.definition.DynamicOptionsProperty`, `com.bytechef.component.definition.Property`, `com.bytechef.platform.component.ComponentConnection`, `com.bytechef.platform.component.definition.ParametersFactory`, `java.util.Map`, `org.assertj.core.api.Assertions`, `org.mockito.Mockito`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "com.bytechef.platform.component.ComponentDefinitionRegistryTest.testGetComponentInputProperty"`
Expected: FAIL — `getComponentInputProperty` does not exist (compile error).

- [ ] **Step 3: Add the method**

In `ComponentDefinitionRegistry.java`, add after `getActionProperty(...)` (the method ends around line 231). Use the existing private static `getProperty(...)` helper and the existing `getComponentDefinition(...)`:

```java
public Property getComponentInputProperty(
    String componentName, int componentVersion, String groupName, String propertyName,
    Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
    Context context) throws Exception {

    ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

    List<? extends PropertyGroup> inputs = componentDefinition.getInputs()
        .orElse(List.of());

    PropertyGroup propertyGroup = CollectionUtils.getFirst(
        inputs, group -> Objects.equals(group.getName(), groupName),
        "Input group '%s' not found in component '%s'".formatted(groupName, componentName));

    return getProperty(
        propertyName, propertyGroup.getProperties(), inputParameters, connectionParameters, lookupDependsOnPaths,
        context);
}
```

Add the import `import com.bytechef.component.definition.PropertyGroup;` (the raw component-api type; `ComponentDefinition`, `Context`, `Parameters`, `Property`, `CollectionUtils`, `Objects`, `List`, `Map` are already imported). `componentDefinition.getInputs()` returns `Optional<List<? extends PropertyGroup>>` and `PropertyGroup.getProperties()` returns `List<? extends ValueProperty<?>>`, which satisfies `getProperty`'s `List<? extends Property>` parameter.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "com.bytechef.platform.component.ComponentDefinitionRegistryTest.testGetComponentInputProperty"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-service:spotlessApply
git add server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/ComponentDefinitionRegistry.java \
        server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/ComponentDefinitionRegistryTest.java
git commit -m "1026 Add ComponentDefinitionRegistry.getComponentInputProperty"
```

---

## Task 2: `ComponentDefinitionService.executeWorkflowInputOptions` (engine)

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionService.java`
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImpl.java`
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ComponentDefinitionServiceExecuteWorkflowInputOptionsTest.java` (new)

- [ ] **Step 1: Add the interface method**

In `ComponentDefinitionService.java`, add (keep methods alphabetical-ish per file style; place after `getConnectionComponentDefinition`):

```java
List<Option> executeWorkflowInputOptions(
    String componentName, int componentVersion, String groupName, String propertyName,
    Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
    @Nullable ComponentConnection componentConnection);
```

Add imports: `com.bytechef.platform.component.ComponentConnection`, `com.bytechef.platform.component.domain.Option`, `java.util.List`, `java.util.Map` (some may already be present — `org.jspecify.annotations.Nullable` is already used).

- [ ] **Step 2: Write the failing test**

Create `ComponentDefinitionServiceExecuteWorkflowInputOptionsTest.java`. It mocks the registry + context factory and a stub `OptionsFunction`, proving the engine resolves the property, reads its options data source, applies the function, and maps to `Option`:

```java
package com.bytechef.platform.component.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Option;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ComponentDefinitionServiceExecuteWorkflowInputOptionsTest {

    @Test
    void testExecuteWorkflowInputOptions() throws Exception {
        ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
        ContextFactory contextFactory = mock(ContextFactory.class);
        ActionContext actionContext = mock(ActionContext.class);

        when(contextFactory.createActionContext(
            anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(true)))
            .thenReturn(actionContext);

        DynamicOptionsProperty<?> property = mock(DynamicOptionsProperty.class);
        OptionsDataSource<OptionsFunction> optionsDataSource = mock(OptionsDataSource.class);
        OptionsFunction optionsFunction = (in, conn, deps, search, ctx) ->
            List.of(new Option<>("General", "C1"));

        when(componentDefinitionRegistry.getComponentInputProperty(
            eq("slack"), eq(1), eq("channel"), eq("channel"), any(), any(), any(), any()))
            .thenAnswer(invocation -> property);
        when(property.getOptionsDataSource()).thenReturn((Optional) Optional.of(optionsDataSource));
        when(optionsDataSource.getOptions()).thenReturn(optionsFunction);

        ComponentDefinitionServiceImpl service = new ComponentDefinitionServiceImpl(
            List.of(), componentDefinitionRegistry, contextFactory);

        List<com.bytechef.platform.component.domain.Option> options = service.executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of(), List.of(), null, null);

        assertThat(options)
            .extracting(com.bytechef.platform.component.domain.Option::getValue)
            .containsExactly("C1");
    }
}
```

> Note: this test pins the exact `ComponentDefinitionServiceImpl` constructor arity used in Step 4 (filters, registry, contextFactory). If the existing constructor differs, adjust both to match.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*ComponentDefinitionServiceExecuteWorkflowInputOptionsTest"`
Expected: FAIL — method/constructor not present.

- [ ] **Step 4: Implement the engine method**

In `ComponentDefinitionServiceImpl.java`:

a. Inject `ContextFactory`. Update the constructor (current signature is `ComponentDefinitionServiceImpl(List<ComponentDefinitionFilter> componentDefinitionFilters, @Lazy ComponentDefinitionRegistry componentDefinitionRegistry)`):

```java
private final ContextFactory contextFactory;

@SuppressFBWarnings("EI")
public ComponentDefinitionServiceImpl(
    List<ComponentDefinitionFilter> componentDefinitionFilters,
    @Lazy ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory) {

    this.componentDefinitionFilters = componentDefinitionFilters;
    this.componentDefinitionRegistry = componentDefinitionRegistry;
    this.contextFactory = contextFactory;
}
```

b. Add the method + a private constant + a small deps helper:

```java
private static final String COMPONENT_INPUT_ACTION_NAME = "__componentInput__";

@Override
public List<Option> executeWorkflowInputOptions(
    String componentName, int componentVersion, String groupName, String propertyName,
    Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
    @Nullable ComponentConnection componentConnection) {

    ActionContext actionContext = contextFactory.createActionContext(
        componentName, componentVersion, COMPONENT_INPUT_ACTION_NAME, null, null, null, null, null,
        componentConnection, null, null, true);

    Parameters inputParametersInstance = ParametersFactory.create(inputParameters);
    Parameters connectionParameters = ParametersFactory.create(componentConnection);
    Map<String, String> lookupDependsOnPathsMap = MapUtils.toMap(
        lookupDependsOnPaths, path -> path.substring(path.lastIndexOf(".") + 1), path -> path);

    try {
        DynamicOptionsProperty<?> dynamicOptionsProperty =
            (DynamicOptionsProperty<?>) componentDefinitionRegistry.getComponentInputProperty(
                componentName, componentVersion, groupName, propertyName, inputParametersInstance,
                connectionParameters, lookupDependsOnPathsMap, actionContext);

        OptionsDataSource<?> optionsDataSource = dynamicOptionsProperty.getOptionsDataSource()
            .orElseThrow(() -> new IllegalArgumentException("Options data source is not defined."));

        OptionsFunction<?> optionsFunction = (OptionsFunction<?>) optionsDataSource.getOptions();

        return optionsFunction
            .apply(
                inputParametersInstance, connectionParameters, lookupDependsOnPathsMap, searchText, actionContext)
            .stream()
            .map(Option::new)
            .toList();
    } catch (Exception e) {
        if (e instanceof ProviderException providerException) {
            throw providerException;
        }

        throw new ConfigurationException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OPTIONS);
    }
}
```

c. Add imports:
```java
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import java.util.Map;
import org.jspecify.annotations.Nullable;
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*ComponentDefinitionServiceExecuteWorkflowInputOptionsTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-service:spotlessApply
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionService.java \
        server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImpl.java \
        server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ComponentDefinitionServiceExecuteWorkflowInputOptionsTest.java
git commit -m "1026 Add ComponentDefinitionService.executeWorkflowInputOptions engine"
```

---

## Task 3: `ComponentDefinitionFacade` (facade tier, connection resolution)

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/facade/ComponentDefinitionFacade.java`
- Create: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/facade/ComponentDefinitionFacadeImpl.java`
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/facade/ComponentDefinitionFacadeImplTest.java` (new)

- [ ] **Step 1: Create the interface**

`ComponentDefinitionFacade.java`:

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

package com.bytechef.platform.component.facade;

import com.bytechef.platform.component.domain.Option;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ComponentDefinitionFacade {

    List<Option> executeWorkflowInputOptions(
        String componentName, int componentVersion, String groupName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable Long connectionId);
}
```

- [ ] **Step 2: Write the failing test**

`ComponentDefinitionFacadeImplTest.java` — verifies connection resolution + delegation, and that an inactive connection fails closed:

```java
package com.bytechef.platform.component.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComponentDefinitionFacadeImplTest {

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final ComponentDefinitionFacadeImpl facade =
        new ComponentDefinitionFacadeImpl(componentDefinitionService, connectionService);

    @Test
    void testExecuteWorkflowInputOptionsResolvesActiveConnectionAndDelegates() {
        Connection connection = mock(Connection.class);

        when(connection.getStatus()).thenReturn(ConnectionStatus.ACTIVE);
        when(connection.getComponentName()).thenReturn("slack");
        when(connection.getConnectionVersion()).thenReturn(1);
        when(connection.getParameters()).thenReturn(Map.of());
        when(connection.getAuthorizationType()).thenReturn(null);
        when(connectionService.getConnection(5L)).thenReturn(connection);
        when(componentDefinitionService.executeWorkflowInputOptions(
            eq("slack"), eq(1), eq("channel"), eq("channel"), any(), any(), any(), any(ComponentConnection.class)))
            .thenReturn(List.of(new Option("General", "C1")));

        List<Option> options = facade.executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of(), List.of(), null, 5L);

        assertThat(options)
            .extracting(Option::getValue)
            .containsExactly("C1");
    }

    @Test
    void testExecuteWorkflowInputOptionsNullConnectionPassesNull() {
        when(componentDefinitionService.executeWorkflowInputOptions(
            eq("slack"), eq(1), eq("channel"), eq("channel"), any(), any(), any(), eq(null)))
            .thenReturn(List.of());

        List<Option> options = facade.executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of(), List.of(), null, null);

        assertThat(options)
            .isEmpty();
    }

    @Test
    void testExecuteWorkflowInputOptionsInactiveConnectionThrows() {
        Connection connection = mock(Connection.class);

        when(connection.getStatus()).thenReturn(ConnectionStatus.INACTIVE);
        when(connection.getName()).thenReturn("Slack");
        when(connectionService.getConnection(5L)).thenReturn(connection);

        assertThatThrownBy(() -> facade.executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of(), List.of(), null, 5L))
            .isInstanceOf(ConfigurationException.class);
    }
}
```

> Verify `ConnectionStatus` has an `INACTIVE` (or equivalent non-`ACTIVE`) value; if the enum differs, use any non-`ACTIVE` constant.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*ComponentDefinitionFacadeImplTest"`
Expected: FAIL — `ComponentDefinitionFacadeImpl` does not exist.

- [ ] **Step 4: Create the impl**

`ComponentDefinitionFacadeImpl.java`:

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

package com.bytechef.platform.component.facade;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ComponentDefinitionFacadeImpl implements ComponentDefinitionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
    }

    @Override
    public List<Option> executeWorkflowInputOptions(
        String componentName, int componentVersion, String groupName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return componentDefinitionService.executeWorkflowInputOptions(
            componentName, componentVersion, groupName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText, componentConnection);
    }

    private @Nullable ComponentConnection getComponentConnection(@Nullable Long connectionId) {
        if (connectionId == null) {
            return null;
        }

        Connection connection = connectionService.getConnection(connectionId);

        validateConnectionActive(connection);

        return new ComponentConnection(
            connection.getComponentName(), connection.getConnectionVersion(), connectionId,
            connection.getParameters(), connection.getAuthorizationType());
    }

    private void validateConnectionActive(Connection connection) {
        ConnectionStatus status = connection.getStatus();

        if (status != ConnectionStatus.ACTIVE) {
            throw new ConfigurationException(
                "Connection '%s' has status %s and cannot be used for execution.".formatted(
                    connection.getName(), status),
                ConnectionErrorType.CONNECTION_NOT_ACTIVE);
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*ComponentDefinitionFacadeImplTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-service:spotlessApply
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/facade/ComponentDefinitionFacade.java \
        server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/facade/ComponentDefinitionFacadeImpl.java \
        server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/facade/ComponentDefinitionFacadeImplTest.java
git commit -m "1026 Add ComponentDefinitionFacade with connection resolution"
```

---

## Task 4: EE remote wiring (client + controller + service-client stub)

**Files:**
- Create: `server/ee/libs/platform/platform-component/platform-component-remote-client/src/main/java/com/bytechef/ee/platform/component/remote/client/facade/RemoteComponentDefinitionFacadeClient.java`
- Create: `server/ee/libs/platform/platform-component/platform-component-remote-rest/src/main/java/com/bytechef/ee/platform/component/remote/web/rest/facade/RemoteComponentDefinitionFacadeController.java`
- Modify: `server/ee/libs/platform/platform-component/platform-component-remote-client/src/main/java/com/bytechef/ee/platform/component/remote/client/service/RemoteComponentDefinitionServiceClient.java`

> These are EE files: use the **ByteChef Enterprise** license header and add `@version ee` to the class Javadoc (matches the sibling `RemoteActionDefinition*` files).

- [ ] **Step 1: Add the unsupported service-client method**

In `RemoteComponentDefinitionServiceClient.java`, add (it `implements ComponentDefinitionService`, so the new interface method from Task 2 must be present or the module won't compile):

```java
@Override
public List<Option> executeWorkflowInputOptions(
    String componentName, int componentVersion, String groupName, String propertyName,
    Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
    @Nullable ComponentConnection componentConnection) {

    throw new UnsupportedOperationException();
}
```

Add imports: `com.bytechef.platform.component.ComponentConnection`, `com.bytechef.platform.component.domain.Option`, `java.util.Map`, `org.jspecify.annotations.Nullable` (some already present).

- [ ] **Step 2: Create the facade remote client**

`RemoteComponentDefinitionFacadeClient.java` (mirror `RemoteActionDefinitionFacadeClient`):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.ee.platform.component.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteComponentDefinitionFacadeClient extends AbstractWorkerClient
    implements ComponentDefinitionFacade {

    private static final String COMPONENT_DEFINITION_FACADE = "/component-definition-facade";

    public RemoteComponentDefinitionFacadeClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public List<Option> executeWorkflowInputOptions(
        String componentName, int componentVersion, String groupName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, COMPONENT_DEFINITION_FACADE + "/execute-workflow-input-options"),
            new WorkflowInputOptionsRequest(
                componentName, componentVersion, groupName, propertyName, inputParameters, connectionId,
                lookupDependsOnPaths, searchText),
            new ParameterizedTypeReference<>() {});
    }

    private record WorkflowInputOptionsRequest(
        String componentName, int componentVersion, String groupName, String propertyName,
        Map<String, ?> inputParameters, Long connectionId, List<String> lookupDependsOnPaths, String searchText) {
    }
}
```

- [ ] **Step 3: Create the worker-side controller**

`RemoteComponentDefinitionFacadeController.java` (mirror `RemoteActionDefinitionFacadeController`):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.web.rest.facade;

import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/remote/component-definition-facade")
public class RemoteComponentDefinitionFacadeController {

    private final ComponentDefinitionFacade componentDefinitionFacade;

    public RemoteComponentDefinitionFacadeController(ComponentDefinitionFacade componentDefinitionFacade) {
        this.componentDefinitionFacade = componentDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-workflow-input-options",
        consumes = {
            "application/json"
        })
    public ResponseEntity<List<Option>> executeWorkflowInputOptions(
        @Valid @RequestBody WorkflowInputOptionsRequest workflowInputOptionsRequest) {

        return ResponseEntity.ok(
            componentDefinitionFacade.executeWorkflowInputOptions(
                workflowInputOptionsRequest.componentName, workflowInputOptionsRequest.componentVersion,
                workflowInputOptionsRequest.groupName, workflowInputOptionsRequest.propertyName,
                workflowInputOptionsRequest.inputParameters, workflowInputOptionsRequest.lookupDependsOnPaths,
                workflowInputOptionsRequest.searchText, workflowInputOptionsRequest.connectionId));
    }

    public record WorkflowInputOptionsRequest(
        String componentName, int componentVersion, String groupName, String propertyName,
        Map<String, Object> inputParameters, @Nullable Long connectionId, List<String> lookupDependsOnPaths,
        String searchText) {
    }
}
```

- [ ] **Step 4: Compile both EE modules**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-remote-client:compileJava :server:ee:libs:platform:platform-component:platform-component-remote-rest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
./gradlew :server:ee:libs:platform:platform-component:platform-component-remote-client:spotlessApply :server:ee:libs:platform:platform-component:platform-component-remote-rest:spotlessApply
git add server/ee/libs/platform/platform-component/platform-component-remote-client/src/main/java/com/bytechef/ee/platform/component/remote/client/facade/RemoteComponentDefinitionFacadeClient.java \
        server/ee/libs/platform/platform-component/platform-component-remote-client/src/main/java/com/bytechef/ee/platform/component/remote/client/service/RemoteComponentDefinitionServiceClient.java \
        server/ee/libs/platform/platform-component/platform-component-remote-rest/src/main/java/com/bytechef/ee/platform/component/remote/web/rest/facade/RemoteComponentDefinitionFacadeController.java
git commit -m "1026 Add EE remote client/controller for ComponentDefinitionFacade"
```

---

## Task 5: EE embedded facade — swap to `getComponentInputOptions`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationInstanceFacade.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationInstanceFacadeImpl.java`

- [ ] **Step 1: Update the interface**

In `ConnectedUserIntegrationInstanceFacade.java`, replace the `getIntegrationInstanceWorkflowInputOptions(...)` method with:

```java
List<Option> getComponentInputOptions(
    String externalUserId, long id, String componentName, int componentVersion, String groupName,
    String propertyName, Map<String, Object> lookupDependsOnValues, String searchText);
```

- [ ] **Step 2: Update the impl**

In `ConnectedUserIntegrationInstanceFacadeImpl.java`:

a. Replace the field + constructor parameter `EmbeddedWorkflowInputOptionFacade embeddedWorkflowInputOptionFacade` with `ComponentDefinitionFacade componentDefinitionFacade`. The constructor becomes:

```java
private final ComponentDefinitionFacade componentDefinitionFacade;
private final ConnectedUserService connectedUserService;
private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
private final IntegrationInstanceService integrationInstanceService;
private final IntegrationInstanceFacade integrationInstanceFacade;
private final IntegrationWorkflowService integrationWorkflowService;

@SuppressFBWarnings("EI")
public ConnectedUserIntegrationInstanceFacadeImpl(
    ComponentDefinitionFacade componentDefinitionFacade, ConnectedUserService connectedUserService,
    IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
    IntegrationInstanceService integrationInstanceService, IntegrationInstanceFacade integrationInstanceFacade,
    IntegrationWorkflowService integrationWorkflowService) {

    this.componentDefinitionFacade = componentDefinitionFacade;
    this.connectedUserService = connectedUserService;
    this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
    this.integrationInstanceService = integrationInstanceService;
    this.integrationInstanceFacade = integrationInstanceFacade;
    this.integrationWorkflowService = integrationWorkflowService;
}
```

b. Replace the `getIntegrationInstanceWorkflowInputOptions(...)` method with:

```java
@Override
public List<Option> getComponentInputOptions(
    String externalUserId, long id, String componentName, int componentVersion, String groupName,
    String propertyName, Map<String, Object> lookupDependsOnValues, String searchText) {

    IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

    // Return no options when the integration instance does not belong to the connected user, instead of
    // leaking instance existence (anti-enumeration).
    if (!isOwnedByConnectedUser(externalUserId, id, integrationInstance)) {
        return List.of();
    }

    return componentDefinitionFacade.executeWorkflowInputOptions(
        componentName, componentVersion, groupName, propertyName, lookupDependsOnValues,
        List.copyOf(lookupDependsOnValues.keySet()), searchText, integrationInstance.getConnectionId());
}
```

c. Update imports: remove `EmbeddedWorkflowInputOptionFacade` usage; add `import com.bytechef.platform.component.facade.ComponentDefinitionFacade;`. (`IntegrationWorkflowService` may now be unused by this method but is still used by `updateIntegrationInstanceWorkflow`/`enableIntegrationInstanceWorkflow`, so keep it.)

- [ ] **Step 3: Compile**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava`
Expected: FAIL — `EmbeddedWorkflowInputOptionFacade` is still referenced elsewhere (the deleted-in-Task-6 classes) **and** the public-rest controller (Task 8) still calls the old method. This is expected; Tasks 6 and 8 complete the removal. Do not commit yet.

- [ ] **Step 4: Commit after Tasks 6 + 8 compile**

(The whole server change — Tasks 5, 6, 7, 8 — must land in one compiling commit; see Task 8 Step 4.)

---

## Task 6: Delete `EmbeddedWorkflowInputOptionFacade(Impl)`

**Files:**
- Delete: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacade.java`
- Delete: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImpl.java`

- [ ] **Step 1: Confirm no other references**

Run: `grep -rn "EmbeddedWorkflowInputOptionFacade" server --include="*.java"`
Expected: only the two files to delete (and, before Task 5, the embedded facade impl — already swapped). If anything else references it, stop and reconcile.

- [ ] **Step 2: Delete the files**

```bash
git rm server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacade.java \
       server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImpl.java
```

- [ ] **Step 3: Build the service module (after Task 8 controller change)**

The `embedded-configuration-service` module compiles once Tasks 5 + 6 are done. The public-rest module needs Task 8. After completing Task 8, run:

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL.

Then commit Tasks 5 + 6 together:

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:spotlessApply
git add -A server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/ \
          server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/
git commit -m "1026 Embedded facade resolves component-input options via ComponentDefinitionFacade; remove node-coupled path"
```

---

## Task 7: OpenAPI — replace options operations

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`

- [ ] **Step 1: Remove old `WorkflowInputOptionsRequest` schema**

Delete the `WorkflowInputOptionsRequest:` schema block (currently ~lines 1946–1962, ending right before `OAuth2:`).

- [ ] **Step 2: Add `ComponentInputOptionsRequest` schema**

In its place (under `components.schemas`), add:

```yaml
    ComponentInputOptionsRequest:
      type: "object"
      required:
        - "componentName"
        - "componentVersion"
        - "groupName"
        - "propertyName"
      properties:
        componentName:
          description: "The component the input group belongs to."
          type: "string"
        componentVersion:
          description: "The component version."
          type: "integer"
          format: "int32"
        groupName:
          description: "The component input group the property belongs to."
          type: "string"
        propertyName:
          description: "The group member property whose options to resolve."
          type: "string"
        lookupDependsOnValues:
          description: "Current values of the properties this lookup depends on."
          type: "object"
          additionalProperties: true
        searchText:
          type: "string"
```

- [ ] **Step 3: Replace the frontend options path**

Replace the whole `/integration-instances/{id}/workflows/{workflowUuid}/options:` block (currently ~lines 633–672) with:

```yaml
  /integration-instances/{id}/component-input-options:
    post:
      description: "Resolve dynamic option values for a component-defined workflow input property."
      summary: "Get component input options"
      tags:
        - "integration-instance-workflow"
      operationId: "getFrontendComponentInputOptions"
      parameters:
        - name: "id"
          description: "The id of an integration instance."
          in: "path"
          required: true
          schema:
            type: "integer"
            format: "int64"
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ComponentInputOptionsRequest"
        required: true
      responses:
        "200":
          description: "The list of resolved options."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/Option"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
      security:
        - jwtBearerAuth: [ ]
```

- [ ] **Step 4: Replace the backend (externalUserId) options path**

Replace the whole `/{externalUserId}/integration-instances/{id}/workflows/{workflowUuid}/options:` block (currently starting ~line 1374) with the backend variant. Match the surrounding backend operations' parameter style for `externalUserId` (copy the `externalUserId` path-parameter block verbatim from a sibling backend operation in the same file):

```yaml
  /{externalUserId}/integration-instances/{id}/component-input-options:
    post:
      description: "Resolve dynamic option values for a component-defined workflow input property."
      summary: "Get component input options"
      tags:
        - "integration-instance-workflow"
      operationId: "getComponentInputOptions"
      parameters:
        - name: "externalUserId"
          description: "The external id of a connected user."
          in: "path"
          required: true
          schema:
            type: "string"
        - name: "id"
          description: "The id of an integration instance."
          in: "path"
          required: true
          schema:
            type: "integer"
            format: "int64"
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ComponentInputOptionsRequest"
        required: true
      responses:
        "200":
          description: "The list of resolved options."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/Option"
      security:
        - apiKeyAuth: [ ]
```

> Verify the backend security scheme name (`apiKeyAuth` vs whatever the file uses) against a sibling `/{externalUserId}/...` operation and match it exactly.

- [ ] **Step 5: Regenerate + compile the generated API**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:openApiGenerate`
Then: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava`
Expected: the generated `IntegrationInstanceWorkflowApi` interface now declares `getFrontendComponentInputOptions` / `getComponentInputOptions` and a `ComponentInputOptionsRequestModel`; `compileJava` FAILS because `IntegrationInstanceWorkflowApiController` no longer matches the regenerated interface (old `@Override`s dangle). Fixed in Task 8.

- [ ] **Step 6: Commit after Task 8**

(Generated sources + controller commit together — see Task 8 Step 4.)

---

## Task 8: Controller — new handlers, remove old

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/IntegrationInstanceWorkflowApiController.java`

> This controller work is the unit referenced from Tasks 5–7; the server change (Tasks 5–8) lands as one compiling commit here.

- [ ] **Step 1: Remove the two old options handler methods**

Delete `getFrontendIntegrationInstanceWorkflowInputOptions(...)` and `getIntegrationInstanceWorkflowInputOptions(...)` and the private `getWorkflowInputOptions(...)` helper.

- [ ] **Step 2: Add the two new handlers + helper**

```java
@Override
@CrossOrigin
public ResponseEntity<List<OptionModel>> getFrontendComponentInputOptions(
    Long id, ComponentInputOptionsRequestModel componentInputOptionsRequestModel) {

    String externalUserId = SecurityUtils.fetchCurrentUserLogin()
        .orElseThrow(() -> new RuntimeException("User not authenticated"));

    return ResponseEntity.ok(getComponentInputOptions(externalUserId, id, componentInputOptionsRequestModel));
}

@Override
public ResponseEntity<List<OptionModel>> getComponentInputOptions(
    String externalUserId, Long id, ComponentInputOptionsRequestModel componentInputOptionsRequestModel) {

    return ResponseEntity.ok(getComponentInputOptions(externalUserId, id, componentInputOptionsRequestModel));
}

private List<OptionModel> getComponentInputOptions(
    String externalUserId, Long id, ComponentInputOptionsRequestModel componentInputOptionsRequestModel) {

    Map<String, Object> lookupDependsOnValues = componentInputOptionsRequestModel.getLookupDependsOnValues();

    List<Option> options = connectedUserIntegrationInstanceFacade.getComponentInputOptions(
        externalUserId, id, componentInputOptionsRequestModel.getComponentName(),
        componentInputOptionsRequestModel.getComponentVersion(),
        componentInputOptionsRequestModel.getGroupName(), componentInputOptionsRequestModel.getPropertyName(),
        lookupDependsOnValues == null ? Map.of() : lookupDependsOnValues,
        componentInputOptionsRequestModel.getSearchText());

    return options.stream()
        .map(option -> new OptionModel()
            .label(option.getLabel())
            .value(String.valueOf(option.getValue())))
        .toList();
}
```

- [ ] **Step 3: Fix imports**

Remove the `WorkflowInputOptionsRequestModel` import; add `import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentInputOptionsRequestModel;` (confirm the generated package via the regenerated sources from Task 7). `OptionModel`, `Option`, `Map`, `List`, `SecurityUtils` imports already exist.

- [ ] **Step 4: Compile the public-rest module + commit Tasks 5–8**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava`
Expected: BUILD SUCCESSFUL.

Then run the deferred service-module build (Task 6 Step 3), `spotlessApply`, and commit:

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/generated 2>/dev/null
git add -A server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest
git commit -m "1026 Replace workflow options endpoint with component-input-options endpoint"
```

(If Tasks 5+6 were not yet committed, include those paths in this commit instead — the whole server change must land in a compiling state.)

---

## Task 9: Server-wide build + check

- [ ] **Step 1: Full compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL. If a remote-client/app module fails to compile because the `ComponentDefinitionService` interface grew, ensure Task 4 Step 1 (the unsupported stub) is in place.

- [ ] **Step 2: Format + targeted checks**

Run:
```bash
./gradlew spotlessApply
./gradlew :server:libs:platform:platform-component:platform-component-service:test \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test
```
Expected: PASS.

- [ ] **Step 3: Commit any formatting**

```bash
git add -A
git commit -m "1026 spotlessApply" --allow-empty
```

---

## Task 10: SDK — `optionsCacheKey` becomes component-ref keyed

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.ts`
- Test: `sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.test.ts` (create if absent)

Working dir for all SDK steps: `sdks/frontend/embedded/library/react`.

- [ ] **Step 1: Write the failing test**

Add to `utils.test.ts`:

```ts
import {describe, expect, it} from 'vitest';
import {optionsCacheKey} from './utils';

describe('optionsCacheKey', () => {
    it('keys by component reference, property, and dependency values', () => {
        expect(optionsCacheKey('slack', 1, 'channel', 'channel', {workspace: 'W1'})).toBe(
            'slack:1:channel:channel:{"workspace":"W1"}'
        );
    });

    it('treats undefined dependency values as empty', () => {
        expect(optionsCacheKey('slack', 1, 'channel', 'channel', undefined as never)).toBe(
            'slack:1:channel:channel:{}'
        );
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/components/connect-dialog/utils.test.ts`
Expected: FAIL — `optionsCacheKey` still has the old signature.

- [ ] **Step 3: Update `optionsCacheKey`**

Replace the existing `optionsCacheKey` export (and update its doc comment to mention the component reference):

```ts
export const optionsCacheKey = (
    componentName: string,
    componentVersion: number,
    groupName: string,
    propertyName: string,
    dependencyValues: Record<string, unknown>
): string =>
    `${componentName}:${componentVersion}:${groupName}:${propertyName}:${JSON.stringify(dependencyValues ?? {})}`;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/utils.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
npm run format:fix
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.ts \
        sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.test.ts
git commit -m "1026 client - Key embedded input options cache by component reference"
```

---

## Task 11: SDK — retarget `useWorkflowInputOptions` to the new endpoint

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/useWorkflowInputOptions.ts`
- Test: `sdks/frontend/embedded/library/react/src/components/connect-dialog/useWorkflowInputOptions.test.ts`

- [ ] **Step 1: Update the test contract**

In `useWorkflowInputOptions.test.ts`, change the `loadOptions` call sites and the asserted request to the component-ref contract. The decisive assertions:

```ts
// calling loadOptions(componentName, componentVersion, groupName, propertyName, deps)
act(() => {
    result.current.loadOptions('slack', 1, 'channel', 'channel', {});
});

await waitFor(() =>
    expect(apiFetch).toHaveBeenCalledWith(
        '/api/embedded/v1/integration-instances/55/component-input-options',
        {
            body: {
                componentName: 'slack',
                componentVersion: 1,
                groupName: 'channel',
                lookupDependsOnValues: {},
                propertyName: 'channel',
            },
            method: 'POST',
        }
    )
);
```

Keep the existing missing-`integrationInstanceId` no-op, in-flight dedup, and `resetOptions` cases, updating their `loadOptions(...)` calls to the new five-arg signature and their cache expectations to the new key.

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/components/connect-dialog/useWorkflowInputOptions.test.ts`
Expected: FAIL — old signature/endpoint.

- [ ] **Step 3: Rewrite the hook**

Replace the contents of `useWorkflowInputOptions.ts` with the component-ref version:

```ts
import {useCallback, useRef, useState} from 'react';
import {ApiFetch, OptionType} from './types';
import {optionsCacheKey} from './utils';

interface UseWorkflowInputOptionsReturnType {
    loadOptions: (
        componentName: string,
        componentVersion: number,
        groupName: string,
        propertyName: string,
        lookupDependsOnValues: Record<string, unknown>
    ) => void;
    optionsByKey: Record<string, OptionType[]>;
    resetOptions: () => void;
}

export default function useWorkflowInputOptions(
    apiFetch: ApiFetch | undefined,
    integrationInstanceId: number | undefined
): UseWorkflowInputOptionsReturnType {
    const [optionsByKey, setOptionsByKey] = useState<Record<string, OptionType[]>>({});

    const optionsByKeyRef = useRef<Record<string, OptionType[]>>({});
    const inFlightKeysRef = useRef<Set<string>>(new Set());
    const generationRef = useRef(0);

    const loadOptions = useCallback(
        (
            componentName: string,
            componentVersion: number,
            groupName: string,
            propertyName: string,
            lookupDependsOnValues: Record<string, unknown>
        ) => {
            if (!apiFetch || !integrationInstanceId) {
                return;
            }

            const cacheKey = optionsCacheKey(
                componentName,
                componentVersion,
                groupName,
                propertyName,
                lookupDependsOnValues
            );

            if (optionsByKeyRef.current[cacheKey] !== undefined || inFlightKeysRef.current.has(cacheKey)) {
                return;
            }

            inFlightKeysRef.current.add(cacheKey);

            const requestGeneration = generationRef.current;

            void apiFetch<OptionType[]>(
                `/api/embedded/v1/integration-instances/${integrationInstanceId}/component-input-options`,
                {
                    body: {componentName, componentVersion, groupName, lookupDependsOnValues, propertyName},
                    method: 'POST',
                }
            )
                .then((options) => {
                    if (generationRef.current !== requestGeneration) {
                        return;
                    }

                    optionsByKeyRef.current = {...optionsByKeyRef.current, [cacheKey]: options ?? []};

                    setOptionsByKey(optionsByKeyRef.current);
                })
                .catch((error: unknown) => {
                    console.error('Failed to load workflow input options:', (error as Error).message);
                })
                .finally(() => {
                    inFlightKeysRef.current.delete(cacheKey);
                });
        },
        [apiFetch, integrationInstanceId]
    );

    const resetOptions = useCallback(() => {
        generationRef.current += 1;

        optionsByKeyRef.current = {};

        inFlightKeysRef.current.clear();

        setOptionsByKey({});
    }, []);

    return {
        loadOptions,
        optionsByKey,
        resetOptions,
    };
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/useWorkflowInputOptions.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
npm run format:fix
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/useWorkflowInputOptions.ts \
        sdks/frontend/embedded/library/react/src/components/connect-dialog/useWorkflowInputOptions.test.ts
git commit -m "1026 client - Retarget embedded input options to component-input-options endpoint"
```

---

## Task 12: SDK — thread component reference through `ConnectDialog`

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.tsx`

The options call now needs `componentName`/`componentVersion`/`groupName` (from `input.componentReference`) instead of `workflowUuid`/`inputName`. Persistence stays keyed by `inputName`.

- [ ] **Step 1: Update the `LoadWorkflowInputOptionsFunction` type**

Replace (around line 27):

```ts
type LoadWorkflowInputOptionsFunction = (
    componentName: string,
    componentVersion: number,
    groupName: string,
    propertyName: string,
    dependencyValues: Record<string, unknown>
) => void;
```

- [ ] **Step 2: Pass component-reference fields into `DialogGroupField`**

In `DialogGroupFieldProps` (around line 664) add the three fields:

```ts
interface DialogGroupFieldProps {
    componentName: string;
    componentVersion: number;
    group: ComponentPropertyGroupType;
    groupName: string;
    handleWorkflowGroupInputChange: HandleWorkflowGroupInputChangeFunction;
    inputName: string;
    loadWorkflowInputOptions: LoadWorkflowInputOptionsFunction;
    memberValues: Record<string, unknown>;
    workflowInputOptions: Record<string, OptionType[]>;
    workflowUuid: string;
}
```

In the `DialogGroupField` destructure (around line 674) add `componentName`, `componentVersion`, `groupName`. Then change the `loadOptions` call (currently `loadWorkflowInputOptions(workflowUuid, inputName, member.name, dependencies)`) to:

```ts
loadOptions={(dependencies) =>
    loadWorkflowInputOptions(componentName, componentVersion, groupName, member.name, dependencies)
}
```

And change the options lookup (currently `optionsCacheKey(workflowUuid, inputName, member.name, ...)`) to:

```ts
options={
    workflowInputOptions[
        optionsCacheKey(
            componentName,
            componentVersion,
            groupName,
            member.name,
            collectDependencyValues(member.optionsLookupDependsOn, memberValues)
        )
    ]
}
```

> Leave the `handleWorkflowGroupInputChange(workflowUuid, inputName, member.name, value)` calls unchanged — persistence is still input-keyed.

- [ ] **Step 3: Supply the fields at the `renderWorkflowInput` call site**

In `renderWorkflowInput` (around line 800), the group branch already reads `const group = input.componentReference?.group;`. Pull the reference and pass its fields:

```ts
const componentReference = input.componentReference;
const group = componentReference?.group;

if (componentReference && group) {
    const memberValues = (input.value as Record<string, unknown> | undefined) ?? {};

    return (
        <DialogGroupField
            componentName={componentReference.componentName}
            componentVersion={componentReference.componentVersion}
            group={group}
            groupName={componentReference.groupName}
            handleWorkflowGroupInputChange={handleWorkflowGroupInputChange}
            inputName={input.name}
            loadWorkflowInputOptions={loadWorkflowInputOptions}
            memberValues={memberValues}
            workflowInputOptions={workflowInputOptions}
            workflowUuid={workflowUuid}
        />
    );
}
```

> Keep props alphabetically ordered (ESLint `sort-keys`/JSX a11y conventions in this repo).

- [ ] **Step 4: Typecheck**

Run: `npx tsc --noEmit`
Expected: PASS (no references to the old 4-arg `loadWorkflowInputOptions`/`optionsCacheKey` remain). If the tools container (`DialogToolsContainer`) also renders `DialogGroupField`/`renderWorkflowInput`, it inherits the same change automatically since it passes `loadWorkflowInputOptions`/`workflowInputOptions` straight through — verify no other call site constructs the old 4-arg key.

- [ ] **Step 5: Commit**

```bash
npm run format:fix
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.tsx
git commit -m "1026 client - Thread component reference into embedded input options call"
```

---

## Task 13: SDK — update `ConnectDialog.dynamic` test + full check

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.dynamic.test.tsx`

- [ ] **Step 1: Update the dynamic-options assertions**

Update the mocked `apiFetch` expectations so the request hits the new endpoint with the component-ref body. The fixture input must include `componentReference` with `componentName`/`componentVersion`/`groupName` and a `group` whose member has `dynamicOptions: true`. Key assertion:

```ts
await waitFor(() =>
    expect(apiFetch).toHaveBeenCalledWith(
        '/api/embedded/v1/integration-instances/55/component-input-options',
        {
            body: {
                componentName: 'slack',
                componentVersion: 1,
                groupName: 'channel',
                lookupDependsOnValues: {},
                propertyName: 'channel',
            },
            method: 'POST',
        }
    )
);
```

Keep the existing behavioral cases (dependent member stays "Select dependencies first" until its dependency has a value; selecting a value still calls the group-change handler with `(workflowUuid, input.name, member.name, value)`; MCP-workflow member uses the same endpoint).

- [ ] **Step 2: Run the dynamic test**

Run: `npx vitest run src/components/connect-dialog/ConnectDialog.dynamic.test.tsx`
Expected: PASS.

- [ ] **Step 3: Full SDK check**

Run:
```bash
npm run lint
npx tsc --noEmit
npx vitest run
```
Expected: all PASS.

- [ ] **Step 4: Commit**

```bash
npm run format:fix
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.dynamic.test.tsx
git commit -m "1026 client - Update embedded dynamic options test for component-input-options endpoint"
```

---

## Task 14: Final verification

- [ ] **Step 1: Server check (targeted modules)**

Run:
```bash
./gradlew :server:libs:platform:platform-component:platform-component-service:check \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Confirm the old path is gone**

Run:
```bash
grep -rn "EmbeddedWorkflowInputOption\|WorkflowInputOptionsRequest\|getIntegrationInstanceWorkflowInputOptions\|/workflows/{workflowUuid}/options" server sdks --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yaml"
```
Expected: no matches (only possibly historical references in `build/` generated output, which are regenerated).

- [ ] **Step 3: Manual smoke (optional, requires running stack)**

Open the embedded ConnectDialog for the Slack integration, enable `workflow1`, and confirm the `channel` select populates with channels (network tab shows `POST .../component-input-options` returning a non-empty array).
```

