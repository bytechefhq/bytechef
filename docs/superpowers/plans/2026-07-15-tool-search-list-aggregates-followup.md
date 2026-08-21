# Tool-Search List Aggregates Follow-up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Serve the editor cluster-element list and the connections list from the build-time index (via the existing `getStaticComponentDefinitions()`), so neither loads the full component catalog.

**Architecture:** Reuse the machinery from the tool-search-from-index feature — `getClusterElementDefinitionStubs(type)` (index-served, Task 1 of that feature) and `ComponentDefinitionRegistry.getStaticComponentDefinitions()` (index stubs when the build index is present, full-load fallback otherwise). Two independent source swaps + no-load regression tests, gated by an audit showing every live consumer reads only index-available fields.

**Tech Stack:** Java 25, Spring Boot 4, JUnit 5, Mockito, AssertJ, Gradle.

## Global Constraints

- Platform module `platform-component-service` — Apache 2.0 header; test class names end with `Test`; test methods camelCase without underscores.
- Do NOT change the existing full-load `getClusterElementDefinitions(type)` or per-component connection detail paths — only the two named list aggregates.
- Run `spotlessApply` before commit; must pass `checkstyleMain`, `checkstyleTest`, `pmdMain`, `spotbugsMain`, and the module `test`.
- Spec: `docs/superpowers/specs/2026-07-15-tool-search-catalog-from-index-design.md` (Follow-ups section).

---

### Task 1: Cluster-element list from index

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceImpl.java` (`getRootClusterElementDefinitions`, ~line 565-571)
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceTest.java` (add a case to the existing file)

**Interfaces:**
- Consumes `ClusterElementDefinitionService.getClusterElementDefinitionStubs(ClusterElementType)` (already exists).

- [ ] **Step 1: Write the failing test**

Add to `ClusterElementDefinitionServiceTest.java` (the file already mocks `ComponentDefinitionRegistry` and builds the service — reuse that setup and the `TOOLS` constant, or add a root component with a tool cluster element). The test drives `getRootClusterElementDefinitions` and asserts the stub path is used and the full-load method is not:

```java
    @Test
    void testGetRootClusterElementDefinitionsUsesStubEnumeration() {
        ComponentDefinition rootComponentDefinition = component("aiAgent")
            .version(1)
            .clusterElements(
                clusterElement("sendMessage")
                    .type(TOOLS)
                    .title("Send Message"));

        // getClusterElementType resolves the type from the root component (one component), then the list is enumerated.
        when(componentDefinitionRegistry.getComponentDefinition("aiAgent", 1)).thenReturn(rootComponentDefinition);
        when(componentDefinitionRegistry.getStaticComponentDefinitions()).thenReturn(List.of(rootComponentDefinition));

        clusterElementDefinitionService.getRootClusterElementDefinitions("aiAgent", 1, "TOOLS");

        // The cluster-element LIST must be enumerated from the index stubs, never the full catalog.
        verify(componentDefinitionRegistry).getStaticComponentDefinitions();
        verify(componentDefinitionRegistry, org.mockito.Mockito.never()).getComponentDefinitions();
    }
```

Note: confirm `getClusterElementType(rootComponentName, rootComponentVersion, typeName)` resolves the
type from the single root component in the common path (it reads `getComponentDefinition(name, version)`);
if the fixture makes it fall into the nested-root fallback (which calls `getComponentDefinitions()`),
adjust the fixture so the root component directly exposes the `TOOLS` type, keeping the test focused on
the list-enumeration swap. Import `com.bytechef.component.definition.ComponentDefinition` and the
`ComponentDsl.component`/`clusterElement` statics as the existing test does.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "com.bytechef.platform.component.service.ClusterElementDefinitionServiceTest"`
Expected: FAIL — `getRootClusterElementDefinitions` still calls `getComponentDefinitions()` (the `never()` verify trips, or `getStaticComponentDefinitions` is never invoked).

- [ ] **Step 3: Switch the delegation to stubs**

In `ClusterElementDefinitionServiceImpl.getRootClusterElementDefinitions`, change the final line from:

```java
        return getClusterElementDefinitions(clusterElementType);
```

to:

```java
        return getClusterElementDefinitionStubs(clusterElementType);
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "com.bytechef.platform.component.service.ClusterElementDefinitionServiceTest"`
Expected: PASS.

- [ ] **Step 5: Format, check, commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-service:spotlessApply
./gradlew :server:libs:platform:platform-component:platform-component-service:checkstyleMain :server:libs:platform:platform-component:platform-component-service:checkstyleTest :server:libs:platform:platform-component:platform-component-service:pmdMain :server:libs:platform:platform-component:platform-component-service:spotbugsMain
git add server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceImpl.java server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceTest.java
git commit -m "732 Serve editor cluster-element list from index stubs"
```

---

### Task 2: Connections list from index

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ConnectionDefinitionServiceImpl.java` (`getConnectionDefinitions()` ~line 318-324; `getConnectableComponentDefinitions` `ScriptComponentDefinition` branch ~line 398-402)
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ConnectionDefinitionServiceTest.java` (create, or add to existing if present)

**Interfaces:**
- Consumes `ComponentDefinitionRegistry.getStaticComponentDefinitions()` (already exists).

- [ ] **Step 1: Write the failing test**

Create/extend `ConnectionDefinitionServiceTest.java`. Build a `ScriptComponentDefinition`-typed component for the target lookup and a connectable component (with a connection) for the catalog, and assert the connectable-list path enumerates via stubs — and that `toConnectionDefinition` builds cleanly from a summary-only stub connection:

```java
    @Test
    void testGetConnectionDefinitionsForScriptComponentUsesStubEnumeration() {
        ScriptComponentDefinition scriptComponentDefinition = mock(ScriptComponentDefinition.class);

        ComponentDefinition connectableComponentDefinition = component("slack")
            .version(1)
            .title("Slack")
            .connection(connection().version(3))
            .actions(action("sendMessage"));

        when(componentDefinitionRegistry.getComponentDefinition("script", 1)).thenReturn(scriptComponentDefinition);
        when(componentDefinitionRegistry.getStaticComponentDefinitions())
            .thenReturn(List.of(connectableComponentDefinition));

        List<ConnectionDefinition> connectionDefinitions =
            connectionDefinitionService.getConnectionDefinitions("script", 1);

        // The connectable-components list must come from index stubs, never the full catalog...
        verify(componentDefinitionRegistry).getStaticComponentDefinitions();
        verify(componentDefinitionRegistry, org.mockito.Mockito.never()).getComponentDefinitions();

        // ...and toConnectionDefinition must build cleanly from a summary-only stub connection (identity + version).
        assertThat(connectionDefinitions).singleElement()
            .satisfies(connectionDefinition -> {
                assertThat(connectionDefinition.getComponentName()).isEqualTo("slack");
                assertThat(connectionDefinition.getVersion()).isEqualTo(3);
            });
    }
```

Use the same construction style the existing platform-component-service tests use for the service
(`new ConnectionDefinitionServiceImpl(componentDefinitionRegistry, ...)` — supply whatever
collaborators its constructor requires; mock them). Import `ComponentDsl.component`/`connection`/`action`
statics, `ScriptComponentDefinition`, and the domain `ComponentDefinition`/`ConnectionDefinition`.
Verify the real `ConnectionDefinition`'s accessor names (`getComponentName()`, `getVersion()`) against
the domain class and adjust the assertions to the actual getters if they differ.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "com.bytechef.platform.component.service.ConnectionDefinitionServiceTest"`
Expected: FAIL — the script branch still calls `getComponentDefinitions()`.

- [ ] **Step 3: Switch both aggregators to stubs**

In `ConnectionDefinitionServiceImpl.getConnectionDefinitions()` (no-arg), change:

```java
        return componentDefinitionRegistry.getComponentDefinitions()
```

to:

```java
        return componentDefinitionRegistry.getStaticComponentDefinitions()
```

And in `getConnectableComponentDefinitions`, the `ScriptComponentDefinition` branch, change:

```java
            return componentDefinitionRegistry.getComponentDefinitions()
                .stream()
                .filter(curComponentDefinition -> curComponentDefinition.getConnection()
                    .isPresent())
                .toList();
```

to:

```java
            return componentDefinitionRegistry.getStaticComponentDefinitions()
                .stream()
                .filter(curComponentDefinition -> curComponentDefinition.getConnection()
                    .isPresent())
                .toList();
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "com.bytechef.platform.component.service.ConnectionDefinitionServiceTest"`
Expected: PASS. If `toConnectionDefinition` throws on the stub connection (e.g. it dereferences auth details), STOP and report — that means the connections list is NOT stub-safe as assumed and needs the controller to map from a lighter shape; do not force it.

- [ ] **Step 5: Format, check, commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-service:spotlessApply
./gradlew :server:libs:platform:platform-component:platform-component-service:checkstyleMain :server:libs:platform:platform-component:platform-component-service:checkstyleTest :server:libs:platform:platform-component:platform-component-service:pmdMain :server:libs:platform:platform-component:platform-component-service:spotbugsMain :server:libs:platform:platform-component:platform-component-service:test
git add server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ConnectionDefinitionServiceImpl.java server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ConnectionDefinitionServiceTest.java
git commit -m "732 Serve connections list from index stubs"
```

---

## Self-Review

- **Spec coverage:** cluster-element list → Task 1; connections list (both aggregators) → Task 2. The `toConnectionDefinition`-on-stub risk is covered by Task 2 Step 1's assertions + the Step 4 stop-condition. Unified API remains deferred per spec.
- **Placeholder scan:** none — both tasks show the exact before/after source and complete test code.
- **Type consistency:** both tasks consume already-existing methods (`getClusterElementDefinitionStubs`, `getStaticComponentDefinitions`); no new signatures introduced.
- **Open risk flagged for the implementer:** Task 2's assertion getters (`getComponentName`/`getVersion`) must be verified against the real domain `ConnectionDefinition`; and the Step 4 stop-condition guards the stub-safety assumption for `toConnectionDefinition`.
