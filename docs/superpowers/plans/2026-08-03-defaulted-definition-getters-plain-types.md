# Defaulted Definition Getters → Plain Types Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Change the 43 definition getters that carry a non-empty default from `Optional<T>` / `OptionalInt` to plain `T` / `int` with a `default` body, so that "has a default" and "may be absent" stop being conflated.

**Architecture:** `Optional` in a return type means *may be absent*. A `default` that returns a value removes absence, so wrapping it in `Optional` costs an unwrap at every call site and — worse — lets an `Optional` leak into string concatenation and serialization, where it silently corrupts. Getters keep `Optional` only when absence is genuinely meaningful (`getDescription`, `getHelp`, `getTitle`, `getIcon`, the function getters); getters with a natural default return the plain type. The compiler drives the migration: changing a return type makes every affected call site a compile error, which is the safety net that was absent when these defaults were first introduced.

**Tech Stack:** Java 25, Gradle 9.4.1 (Kotlin DSL), JUnit 5, Mockito, Jackson (2.x `com.fasterxml` and 3.x `tools.jackson` both present), Spotless.

## Global Constraints

- Commit message convention, server-side: `1203 platform-scheduler - <description>`.
- Run `./gradlew spotlessApply` before every commit.
- **Never judge a Gradle run piped into `tail`/`grep`** — the pipeline exit code belongs to the filter. Redirect to a file, check `$?` on its own line, then grep the file. Use `--continue`, and grep for `^> Task .* FAILED`, never `error:` (which matches module paths like `:server:libs:core:error:`).
- **Invariant — no UNEXPECTED change to the definition snapshots under `*/src/test/resources/definition/*.json`.** `Optional.of(false)` and `false` serialize identically, as do `OptionalInt.of(1)` and `1`, so a getter that merely sheds its wrapper changes nothing. A snapshot legitimately changes only when a field that previously serialized as `null` gains a default — which happens wherever a DSL backing field was NOT already seeded.
  **Verify with the test suite, not with `git status`.** `JsonFileAssert` *generates* a snapshot only when the file is missing and *compares* when it exists, so `git status --short -- '*/definition/*.json'` stays at `0` even when serialization has drifted. It detects regeneration, not drift. The failing `*DefinitionFactoryTest` / `*ComponentHandlerTest` tasks are the real signal. (Corrected mid-execution: Task 2 was misled by the original wording of this constraint.)
- Before seeding any DSL backing field, check whether it is already seeded. `ComponentDsl`'s property flags were seeded in earlier work; `TaskDispatcherDsl`'s were not. Seeding an unseeded field changes that definition's serialized output from `null` to the default, and its snapshots must be regenerated — with the semantic diff reviewed, never blind-regenerated.
- Do **not** touch getters whose default is `Optional.empty()`. They stay `Optional`. Nullness is not compiler-enforced in this build (jspecify annotations only; no NullAway/ErrorProne), and `com.bytechef.component.definition` is not `@NullMarked`, so on the SDK surface `Optional` is the only enforced signal.
- Mockito's default answer for an unstubbed getter returning `boolean` is `false`, for `List`/`Map` it is an empty collection, and for `int` it is `0`. These coincide with the intended defaults everywhere except `getExpressionEnabled`, `getMultipleValues`, `getAuthorizationRequired` (default `true`) and `getVersion` (default `1`) — tests mocking those four must stub them explicitly.

---

## File Structure

**Interfaces changed (13):**

| File | Getters migrated |
|---|---|
| `sdks/backend/java/definition-api/.../BaseProperty.java` | `getAdvancedOption`, `getExpressionEnabled`, `getHidden`, `getRequired`, `getMultipleValues` ×2, `getOptionsLoadedDynamically`, `getMetadata`, `getItems`, `getProperties`, `getAdditionalProperties` |
| `sdks/backend/java/definition-api/.../BaseOptionsProperty.java` | `getOptions` |
| `sdks/backend/java/definition-api/.../BaseResources.java` | `getAdditionalUrls` |
| `sdks/backend/java/component-api/.../ComponentDefinition.java` | `getVersion`, `getComponentCategories`, `getMetadata`, `getTags` |
| `sdks/backend/java/component-api/.../ConnectionDefinition.java` | `getVersion`, `getAuthorizationRequired`, `getAuthorizations`, `getProperties` |
| `sdks/backend/java/component-api/.../WorkflowComponentDefinition.java` | `getActions`, `getTriggers`, `getInputs`, `getCustomAction` |
| `sdks/backend/java/component-api/.../ClusterElementComponentDefinition.java` | `getClusterElements` |
| `sdks/backend/java/component-api/.../ActionDefinition.java` | `getBatch`, `getDeprecated`, `getMetadata`, `getProperties` |
| `sdks/backend/java/component-api/.../TriggerDefinition.java` | `getBatch`, `getDeprecated`, `getProperties`, `getWebhookRawBody`, `getWorkflowSyncExecution` |
| `sdks/backend/java/component-api/.../ClusterElementDefinition.java` | `getProperties` |
| `sdks/backend/java/component-api/.../Authorization.java` | `getDetectOn`, `getProperties`, `getRefreshOn` |
| `sdks/backend/java/component-api/.../Property.java` | `getItems`, `getAdditionalProperties`, `getProperties` |
| `server/libs/platform/.../task/dispatcher/definition/TaskDispatcherDefinition.java` | `getVersion` |

**Implementations that override them (2):** `ComponentDsl.java`, `TaskDispatcherDsl.java`.

**Consumers that unwrap them:** `server/libs/platform/platform-api/.../domain/BaseProperty.java`, `BaseResources.java`; the ~30 wrappers under `server/libs/platform/platform-component/platform-component-api/.../domain/` and `.../definition/`; plus scattered call sites the compiler will name.

Task order is chosen so each task's blast radius is bounded and independently reviewable. Task 1 first because it is the one that caused three production defects.

---

### Task 1: `getVersion()` — `OptionalInt` → `int`

This is the highest-value task. `OptionalInt` returned from `getVersion()` caused three shipped defects: a Jackson `InvalidDefinitionException` in `GuestComponentBridge`, filenames built as `example_OptionalInt[1].js` in `CustomComponentFacadeImpl` (silent — no exception), and it is the archetype for the whole migration.

**Files:**
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDefinition.java`
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ConnectionDefinition.java`
- Modify: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/src/main/java/com/bytechef/platform/workflow/task/dispatcher/definition/TaskDispatcherDefinition.java`
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java`
- Modify: `server/libs/platform/platform-workflow/.../definition/TaskDispatcherDsl.java`
- Modify (unwrap removal): `server/ee/libs/platform/platform-custom-component/platform-custom-component-guest-bridge/src/main/java/com/bytechef/ee/platform/customcomponent/guest/GuestComponentBridge.java`
- Modify (unwrap removal): `server/ee/libs/platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-service/src/main/java/com/bytechef/ee/platform/customcomponent/configuration/facade/CustomComponentFacadeImpl.java`

**Interfaces:**
- Produces: `int getVersion()` on all three definition interfaces. Every later task and every consumer relies on this signature.

- [ ] **Step 1: Add a regression test pinning the filename bug**

The filename defect was silent — no exception, just a wrong name. Pin it so it cannot return. Add to `server/ee/libs/platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-service/src/test/java/com/bytechef/ee/platform/customcomponent/configuration/facade/CustomComponentFacadeUpdateSourceTest.java`:

```java
    @Test
    void testStoredFileNameContainsPlainVersionNumber() {
        // Guards against a version wrapper (OptionalInt) reaching string concatenation, which produced
        // "example_OptionalInt[1].js" without throwing.
        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);

        customComponentFacade.updateCustomComponentSource(CUSTOM_COMPONENT_ID, VALID_SOURCE);

        verify(customComponentFileStorage).storeCustomComponentFile(fileNameCaptor.capture(), any());

        assertEquals("example_1.js", fileNameCaptor.getValue());
    }
```

Add the imports it needs if absent: `org.mockito.ArgumentCaptor`, `static org.mockito.ArgumentMatchers.any`, `static org.junit.jupiter.api.Assertions.assertEquals`. Reuse the existing constants in that test class for the component id and source; if their names differ, use the ones already present rather than inventing new ones.

- [ ] **Step 2: Run it and confirm it passes today**

```bash
./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-service:test \
  --tests '*CustomComponentFacadeUpdateSourceTest*' > /tmp/t.log 2>&1
echo $?
grep -E "^BUILD |^> Task .* FAILED" /tmp/t.log
```
Expected: PASS. The bug is already fixed by an `.orElseThrow()`; this test is what lets Step 4 remove that unwrap safely.

- [ ] **Step 3: Change the three interfaces**

In `ComponentDefinition.java` and `ConnectionDefinition.java`, replace:

```java
    default OptionalInt getVersion() {
        return OptionalInt.of(VERSION_1);
    }
```
with:
```java
    default int getVersion() {
        return VERSION_1;
    }
```

In `TaskDispatcherDefinition.java`, the same change. Remove the now-unused `import java.util.OptionalInt;` from all three.

- [ ] **Step 4: Update the two DSL overrides and remove the three unwrap sites**

In `ComponentDsl.java`, both `ModifiableComponentDefinition` and `ModifiableConnectionDefinition`:
```java
        @Override
        public int getVersion() {
            return version;
        }
```
In `TaskDispatcherDsl.java`, `ModifiableTaskDispatcherDefinition`: the same.

In `GuestComponentBridge.java`:
```java
            component.put("version", componentDefinition.getVersion());
```

In `CustomComponentFacadeImpl.java`, all three `storeCustomComponentFile` sites:
```java
                componentDefinition.getName() + "_" + componentDefinition.getVersion() + "."
                    + language.getExtension(),
```
and the standalone `int version = componentDefinition.getVersion();` (drop its `.orElseThrow()`).

- [ ] **Step 5: Compile and let the compiler find the rest**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/c.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/c.log
sed -n '/^\* What went wrong/,$p' /tmp/c.log | grep -E "\.java:[0-9]+: error"
```
Every remaining error is a call site doing `.getVersion().orElseThrow()` or `.getVersion().getAsInt()`. Delete the unwrap. Repeat until clean. Known sites include `AbstractComponentDefinitionWrapper`, `ComponentDefinitionRegistry` (two places), `platform/component/domain/ComponentDefinition`, and the `ComponentDefinitionRegistryIndexTest` / `ComponentHandlerEspressoEngineDslAssemblyTest` assertions.

- [ ] **Step 6: Check for silent survivors the compiler cannot see**

An `OptionalInt` in string concatenation compiles fine. Confirm none remain:
```bash
grep -rn 'getVersion()' --include='*.java' server sdks | grep -v '/build/' | grep -E '\+ *"|" *\+'
```
Expected: only `TaskDispatcherTools.java` (domain type, always `int`), `AiAgentEvalRunExecutor.java`, `Integration.java`, `Project.java` — all unrelated domain objects.

- [ ] **Step 7: Verify the snapshot invariant**

```bash
./gradlew test --continue > /tmp/t.log 2>&1
echo $?
git status --short -- '*/src/test/resources/definition/*.json' | wc -l
```
Expected: `0` changed snapshots. If non-zero, a default value changed — stop and investigate rather than regenerating.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add -A
git commit -m "1203 platform-scheduler - Return int from getVersion instead of OptionalInt

A version always has a value, so wrapping it in OptionalInt bought nothing
and cost three defects: Jackson could not serialize it in the guest bridge,
and string concatenation produced example_OptionalInt[1].js silently. Plain
int makes both unrepresentable."
```

---

### Task 2: `BaseProperty` booleans → `boolean`

**Files:**
- Modify: `sdks/backend/java/definition-api/src/main/java/com/bytechef/definition/BaseProperty.java`
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java`
- Modify: `server/libs/platform/platform-workflow/.../definition/TaskDispatcherDsl.java`
- Modify: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/domain/BaseProperty.java`
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/loop/LoopActionsTest.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `boolean getRequired()`, `boolean getHidden()`, `boolean getAdvancedOption()`, `boolean getExpressionEnabled()`, `boolean getMultipleValues()`, `boolean getOptionsLoadedDynamically()`.

- [ ] **Step 1: Change the six getters**

In `definition-api/BaseProperty.java`, top level:
```java
    default boolean getAdvancedOption() {
        return false;
    }

    default boolean getExpressionEnabled() {
        return true;
    }

    default boolean getHidden() {
        return false;
    }

    default boolean getRequired() {
        return false;
    }
```
In the nested `BaseArrayProperty` and `BaseObjectProperty`:
```java
        default boolean getMultipleValues() {
            return true;
        }
```
In the nested `BaseStringProperty`:
```java
        default boolean getOptionsLoadedDynamically() {
            return false;
        }
```

Keep the Javadoc, but drop the `{@link Optional}` wording — e.g. "Returns whether a value for this property is required." with `@return {@code true} if the property is required, {@code false} otherwise (the default)".

- [ ] **Step 2: Update the DSL overrides**

In `ComponentDsl.ModifiableProperty` the fields stay `Boolean` (the builder must distinguish "not set" internally is **not** required — they are already seeded), but the getters return the primitive:
```java
        @Override
        public boolean getRequired() {
            return required;
        }
```
Do the same for `getAdvancedOption`, `getExpressionEnabled`, `getHidden`; and for `getMultipleValues` on `ModifiableArrayProperty`/`ModifiableObjectProperty` and `getOptionsLoadedDynamically` on `ModifiableStringProperty`. Apply the identical change in `TaskDispatcherDsl`.

Because every one of those fields is already seeded (`= Boolean.FALSE` / `= Boolean.TRUE`), the auto-unboxing here cannot NPE. Verify the seed is present on each field before relying on that.

- [ ] **Step 3: Simplify the platform domain wrapper**

In `server/libs/platform/platform-api/.../domain/BaseProperty.java`:
```java
        this.advancedOption = property.getAdvancedOption();
        this.description = OptionalUtils.orElse(property.getDescription(), null);
        this.displayCondition = OptionalUtils.orElse(property.getDisplayCondition(), null);
        this.expressionEnabled = property.getExpressionEnabled();
        this.hidden = property.getHidden();
        this.metadata = property.getMetadata();
        this.required = property.getRequired();
```
(`getMetadata` lands in Task 3; leave it as `.orElseThrow()` until then.)

- [ ] **Step 4: Compile and fix call sites**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/c.log 2>&1
echo $?
sed -n '/^\* What went wrong/,$p' /tmp/c.log | grep -E "\.java:[0-9]+: error"
```
Remove `.orElseThrow()` / `OptionalUtils.orElse(..., false)` at each named site. In `LoopSchemaLifter` the `required.ifPresent(wrapper::required)` becomes `wrapper.required(property.getRequired())`. In `GuestComponentBridge` the `property.getRequired().ifPresent(...)` becomes `propertyMap.put("required", property.getRequired())`.

- [ ] **Step 5: Fix the two assertion sites**

`LoopActionsTest` line ~74 and `QuickbooksUtilsTest` lines ~83-89 currently call `.getRequired().orElseThrow()`. Drop the `.orElseThrow()`.

- [ ] **Step 6: Verify — including that no derivation keys on absence**

```bash
./gradlew test --continue > /tmp/t.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/t.log
git status --short -- '*/src/test/resources/definition/*.json' | wc -l
```
Expected: no failed tasks, `0` changed snapshots.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add -A
git commit -m "1203 platform-scheduler - Return boolean from the defaulted property flags

required, hidden, advancedOption, expressionEnabled, multipleValues and
optionsLoadedDynamically all have a natural default, so Optional never
conveyed absence for them -- it only forced an unwrap at every call site
and made an empty Optional from a hand-rolled implementor a runtime fault."
```

---

### Task 3: `BaseProperty` / `BaseOptionsProperty` / `BaseResources` collections

**Files:**
- Modify: `sdks/backend/java/definition-api/src/main/java/com/bytechef/definition/BaseProperty.java`
- Modify: `sdks/backend/java/definition-api/src/main/java/com/bytechef/definition/BaseOptionsProperty.java`
- Modify: `sdks/backend/java/definition-api/src/main/java/com/bytechef/definition/BaseResources.java`
- Modify: `sdks/backend/java/component-api/.../ComponentDsl.java`, `server/.../TaskDispatcherDsl.java`
- Modify: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/domain/BaseResources.java`

**Interfaces:**
- Produces: `Map<String, Object> getMetadata()`, `List<? extends I> getItems()`, `List<? extends P> getProperties()`, `List<? extends P> getAdditionalProperties()`, `List<? extends I> getOptions()`, `Map<String, String> getAdditionalUrls()`.

- [ ] **Step 1: Change the getters**

```java
    default Map<String, Object> getMetadata() {
        return Map.of();
    }
```
in `BaseProperty`; `getItems`, `getProperties`, `getAdditionalProperties` in the nested interfaces return `List.of()`; `BaseOptionsProperty.getOptions()` returns `List.of()`; `BaseResources.getAdditionalUrls()` returns `Map.of()`.

- [ ] **Step 2: Update the DSL overrides**

`ModifiableProperty.getMetadata()` becomes:
```java
        @Override
        public Map<String, Object> getMetadata() {
            return new HashMap<>(metadata);
        }
```
The defensive copy is retained — it is why the field is mutable, and callers must not be able to mutate the builder through the getter. Apply the same shape to the list getters (`List.copyOf(...)` where the existing code copied, plain return where it did not).

- [ ] **Step 3: Simplify the two platform domain wrappers**

`platform/domain/BaseProperty.java`: `this.metadata = property.getMetadata();`
`platform/domain/BaseResources.java`: `this.additionalUrls = resources.getAdditionalUrls();` — and delete the `OptionalUtils` import if it becomes unused (check with `grep -c 'OptionalUtils\.'`; the import is unused only at count `0`).

- [ ] **Step 4: Compile and fix call sites**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/c.log 2>&1
echo $?
sed -n '/^\* What went wrong/,$p' /tmp/c.log | grep -E "\.java:[0-9]+: error"
```
`OpenApiClientUtils` and `GaurusComponentHandler` each have three `getMetadata().orElse(Map.of())` sites that collapse to `getMetadata()`. `ObjectProperty`, `ArrayProperty`, and the six option-bearing property wrappers lose their `.orElseThrow()`.

- [ ] **Step 5: Re-check the control-type derivations**

`ComponentDsl` has seven `getControlType()` overrides guarding on `(options == null || options.isEmpty()) && optionsFunction == null`. The `options == null` half is now dead for DSL-built properties but must stay for safety. Confirm all seven still read `options.isEmpty()` and that none reverted to a bare null check:
```bash
grep -c "options == null || options.isEmpty()" sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java
```
Expected: `7`.

- [ ] **Step 6: Verify**

```bash
./gradlew test --continue > /tmp/t.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/t.log
git status --short -- '*/src/test/resources/definition/*.json' | wc -l
```
Expected: no failed tasks, `0` changed snapshots.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add -A
git commit -m "1203 platform-scheduler - Return plain collections from the defaulted property getters"
```

---

### Task 4: `ActionDefinition`, `TriggerDefinition`, `ClusterElementDefinition`

**Files:**
- Modify: `sdks/backend/java/component-api/.../ActionDefinition.java`, `TriggerDefinition.java`, `ClusterElementDefinition.java`
- Modify: `sdks/backend/java/component-api/.../ComponentDsl.java`
- Modify: `server/libs/platform/platform-component/platform-component-api/.../definition/AbstractActionDefinitionWrapper.java`, `AbstractClusterElementDefinitionWrapper.java`
- Modify: `server/libs/platform/platform-component/platform-component-api/.../domain/ActionDefinition.java`, `TriggerDefinition.java`, `ClusterElementDefinition.java`
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/definition/AbstractClusterElementDefinitionWrapperTest.java`
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceTest.java`

**Interfaces:**
- Produces: `boolean getBatch()`, `boolean getDeprecated()`, `boolean getWebhookRawBody()`, `boolean getWorkflowSyncExecution()`, `Map<String, Object> getMetadata()`, `List<? extends Property> getProperties()`.

- [ ] **Step 1: Change the getters** — booleans to `boolean`, `getMetadata` to `Map<String, Object>` returning `Map.of()`, `getProperties` to `List<? extends Property>` returning `List.of()`, across all three interfaces.

- [ ] **Step 2: Update `ComponentDsl`'s `ModifiableActionDefinition`, `ModifiableTriggerDefinition`, `ModifiableClusterElementDefinition` overrides** to match.

- [ ] **Step 3: Simplify the wrappers** — in `AbstractActionDefinitionWrapper` the four `.orElseThrow()` calls on `getBatch`, `getDeprecated`, `getMetadata`, `getProperties` become direct reads; likewise `AbstractClusterElementDefinitionWrapper.getProperties()`; and in the three domain classes the `.orElseThrow()` on the same getters.

- [ ] **Step 4: Fix the two test doubles**

This is the payoff for the `NoSuchElementException` class of failure. In `ClusterElementDefinitionServiceTest`, **delete** the line:
```java
        when(elementDefinition.getProperties()).thenReturn(Optional.of(List.of()));
```
Mockito's default for a `List`-returning method is already an empty list. Do the same in `AbstractClusterElementDefinitionWrapperTest` at both stub sites, and restore the assertion in `testWrapperWithNullOptionalFields` to:
```java
        assertEquals(List.of(), wrapper.getProperties());
```

- [ ] **Step 5: Compile, then verify**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/c.log 2>&1
echo $?
sed -n '/^\* What went wrong/,$p' /tmp/c.log | grep -E "\.java:[0-9]+: error"
./gradlew test --continue > /tmp/t.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/t.log
git status --short -- '*/src/test/resources/definition/*.json' | wc -l
```
Expected: no failed tasks, `0` changed snapshots.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add -A
git commit -m "1203 platform-scheduler - Return plain types from the action, trigger and cluster element defaults"
```

---

### Task 5: `ComponentDefinition`, `WorkflowComponentDefinition`, `ClusterElementComponentDefinition`, `ConnectionDefinition`

**Files:**
- Modify: `sdks/backend/java/component-api/.../ComponentDefinition.java`, `WorkflowComponentDefinition.java`, `ClusterElementComponentDefinition.java`, `ConnectionDefinition.java`
- Modify: `sdks/backend/java/component-api/.../ComponentDsl.java`
- Modify: `server/libs/platform/platform-component/platform-component-api/.../definition/AbstractComponentDefinitionWrapper.java`, `ComponentDefinitionWrapper.java`
- Modify: `server/libs/platform/platform-component/platform-component-api/.../domain/ComponentDefinition.java`, `ConnectionDefinition.java`
- Modify: `server/libs/platform/platform-component/platform-component-service/.../index/ComponentIndexGenerator.java`

**Interfaces:**
- Produces: `List<ComponentCategory> getComponentCategories()`, `Map<String, Object> getMetadata()`, `List<String> getTags()`, `List<ActionDefinition> getActions()`, `List<TriggerDefinition> getTriggers()`, `List<? extends PropertyGroup> getInputs()`, `boolean getCustomAction()`, `List<ClusterElementDefinition<?>> getClusterElements()`, `boolean getAuthorizationRequired()`, `List<? extends Authorization> getAuthorizations()`, `List<? extends Property> getProperties()`.

- [ ] **Step 1: Change the eleven getters** across the four interfaces, per the signatures above. `getAuthorizationRequired()` defaults to `true`, everything else to `false` / `List.of()` / `Map.of()`.

- [ ] **Step 2: Update `ModifiableComponentDefinition` and `ModifiableConnectionDefinition`.** `ModifiableConnectionDefinition.properties` is seeded `new ArrayList<>()` because `append()` mutates it in place — keep the mutable seed and return `List.copyOf(properties)` from the getter so callers cannot mutate the builder.

- [ ] **Step 3: Simplify `AbstractComponentDefinitionWrapper`** — its eight `.orElseThrow()` reads become direct, and the `OptionalUtils.orElse(..., null)` reads for `getConnection`, `getCustomActionHelp`, `getDescription`, `getIcon`, `getResources`, `getTitle`, `getUnifiedApi` stay untouched (those remain `Optional`).

- [ ] **Step 4: Simplify `ComponentDefinitionWrapper`** — `getClusterElements()` and `getCustomAction()` lose their unwraps.

- [ ] **Step 5: Simplify the domain classes and the index generator**

In `platform/component/domain/ComponentDefinition.java` the nine unwraps collapse. In `ComponentIndexGenerator.toConnectionSummary`, the required-property check becomes:
```java
        boolean anyPropertyRequired = connectionDefinition.getProperties()
            .stream()
            .anyMatch(Property::getRequired);
```
Note this site previously held a `Boolean.TRUE.equals(...)` bug that compiled against an `Optional` and always evaluated false — the plain `boolean` makes that shape impossible.

- [ ] **Step 6: Compile, then verify**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/c.log 2>&1
echo $?
sed -n '/^\* What went wrong/,$p' /tmp/c.log | grep -E "\.java:[0-9]+: error"
./gradlew test --continue > /tmp/t.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/t.log
git status --short -- '*/src/test/resources/definition/*.json' | wc -l
```
`ComponentDefinitionRegistryIndexTest` and `ComponentToolsTest` mock these interfaces — the four `true`-defaulting getters need explicit stubs there if a test depends on them.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add -A
git commit -m "1203 platform-scheduler - Return plain types from the component and connection defaults"
```

---

### Task 6: `Authorization` and `Property`

`Authorization.getRefreshOn()` is the subtle one: its default is `DEFAULT_REFRESH_ON` (`List.of(401)`), not an empty list, and an earlier seeding attempt broke OAuth2 token refresh by making it empty. The plain-type form makes the real default explicit at the declaration.

**Files:**
- Modify: `sdks/backend/java/component-api/.../Authorization.java`, `Property.java`
- Modify: `sdks/backend/java/component-api/.../ComponentDsl.java`
- Modify: `server/libs/platform/platform-component/platform-component-api/.../domain/Authorization.java`
- Modify: `server/libs/platform/platform-component/platform-component-service/.../service/ConnectionDefinitionServiceImpl.java`

**Interfaces:**
- Produces: `List<String> getDetectOn()`, `List<? extends Property> getProperties()`, `List<Object> getRefreshOn()`, and on `Property`'s nested interfaces `List<? extends Property.ValueProperty<?>> getItems()`, `getAdditionalProperties()`, `getProperties()`.

- [ ] **Step 1: Change the getters**

```java
    default List<Object> getRefreshOn() {
        return DEFAULT_REFRESH_ON;
    }
```
and `getDetectOn()` / `getProperties()` returning `List.of()`; the three `Property` nested getters returning `List.of()`.

- [ ] **Step 2: Update `ModifiableAuthorization`.** Its `refreshOn` field is seeded `DEFAULT_REFRESH_ON`; the getter returns it directly.

- [ ] **Step 3: Simplify the two consumers**

`platform/component/domain/Authorization.java`: `this.detectOn = authorization.getDetectOn();`, `this.refreshOn = authorization.getRefreshOn();`, and the `getProperties()` unwrap inside the `CollectionUtils.map(...)` call.
`ConnectionDefinitionServiceImpl` line ~275: `return authorizationOptional.get().getRefreshOn();`

- [ ] **Step 4: Pin the 401 default**

Add to `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/domain/AuthorizationTest.java` (create the file if absent, with the standard Apache licence header used by its siblings):

```java
    @Test
    void testRefreshOnDefaultsToUnauthorized() {
        // An authorization that declares no refresh signals must still refresh on HTTP 401; an earlier
        // change made this an empty list and silently disabled OAuth2 token refresh.
        ModifiableAuthorization authorization = ComponentDsl.authorization(
            Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        assertEquals(List.of(401), authorization.getRefreshOn());
    }
```
Match the actual `ComponentDsl.authorization(...)` overload present in the codebase; if it takes a name plus a type, pass both.

- [ ] **Step 5: Compile, then verify**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/c.log 2>&1
echo $?
sed -n '/^\* What went wrong/,$p' /tmp/c.log | grep -E "\.java:[0-9]+: error"
./gradlew test --continue > /tmp/t.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/t.log
git status --short -- '*/src/test/resources/definition/*.json' | wc -l
```

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add -A
git commit -m "1203 platform-scheduler - Return plain lists from the authorization and property defaults

getRefreshOn now declares DEFAULT_REFRESH_ON as its default at the
declaration rather than relying on every caller to re-apply the HTTP 401
fallback."
```

---

### Task 7: Sweep for dead unwrapping and stale documentation

**Files:** whichever the greps below name.

- [ ] **Step 1: Find unwraps that no longer unwrap anything**

```bash
grep -rn 'OptionalUtils\.' --include='*.java' server sdks | grep -v '/build/' | wc -l
for f in $(grep -rl 'import com.bytechef.commons.util.OptionalUtils;' --include='*.java' server sdks | grep -v '/build/'); do
  [ "$(grep -c 'OptionalUtils\.' $f)" -eq 0 ] && echo "UNUSED IMPORT: $f"
done
```
Remove every unused import reported. Note the count must be `0`, not `1` — the import line itself does not match `OptionalUtils\.`.

- [ ] **Step 2: Find Javadoc that still promises an Optional**

```bash
grep -rn "an empty {@link Optional}\|an empty {@code Optional}" --include='*.java' sdks/backend/java | grep -v '/build/'
```
For every getter migrated in Tasks 1-6, rewrite the `@return` to describe the default (e.g. "the metadata map, empty when none is set"). Leave the still-`Optional` getters' Javadoc alone.

- [ ] **Step 3: Confirm no defaulted getter still returns Optional**

```bash
python3 - <<'PY'
import re, glob
files = glob.glob("sdks/backend/java/definition-api/src/main/java/com/bytechef/definition/*.java") + \
        glob.glob("sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/*.java")
pat = re.compile(r'default\s+(\S.*?)\s+(\w+)\(\)\s*\{\s*return\s+([^;]+);', re.S)
bad = [(f.split("/")[-1], m.group(2)) for f in files for m in pat.finditer(open(f).read())
       if m.group(1).strip().startswith("Optional") and "Optional.empty()" not in m.group(3)]
print("Defaulted getters still returning Optional:", bad or "none")
PY
```
Expected: `none`.

- [ ] **Step 4: Full verification**

```bash
./gradlew spotlessApply check --continue > /tmp/v.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/v.log
git status --short -- '*/src/test/resources/definition/*.json' | wc -l
```
`check` runs Checkstyle, PMD and SpotBugs in addition to the tests, which is what catches the unused imports if Step 1 missed any. Expected: no failed tasks, `0` changed snapshots.

Note: `IntegrationCodeWorkflowFacadeCreateEmptyTest > testPythonStarterLoadsThroughLoader` is a known flake — a 30-second timeout on a GraalVM Python context load that passes standalone in ~21s and only trips under full-suite contention. It is not a regression from this work.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "1203 platform-scheduler - Remove unwrapping left dead by the plain-type definition getters"
```

---

### Task 8: Integration verification

Every defect this migration removes was invisible to `compileJava` and surfaced only at runtime. The unit suite exercises these paths through fixtures; the integration suite exercises them through real wiring.

**Files:** none — verification only.

- [ ] **Step 1: Start the infrastructure**

```bash
cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd ..
```

- [ ] **Step 2: Run the integration suite**

```bash
./gradlew testIntegration --continue > /tmp/i.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/i.log
```

- [ ] **Step 3: Triage**

Any failure here is either a genuine regression or a pre-existing failure. Establish which by stashing the branch's changes and re-running the failing module against the merge base — do not assume. Fix genuine regressions; record pre-existing ones in the final commit message rather than silently leaving them.

- [ ] **Step 4: Exercise the two paths that broke before**

The custom-component filename and guest-bridge serialization defects were both outside the unit suite's reach until a mock happened to assert an exact string. Confirm by hand:
- Upload a JavaScript custom component through the UI and confirm the stored file is named `<name>_<version>.js`.
- Open a workflow that uses a custom component and confirm the component describes without error.

- [ ] **Step 5: Commit any fixes**

```bash
./gradlew spotlessApply
git add -A
git commit -m "1203 platform-scheduler - Fix integration failures from the plain-type definition getters"
```

---

## Notes for the implementer

**Why the compiler is the test here.** This plan does not open each task with a failing unit test, because for a return-type migration the type system is the failing test: changing `Optional<Boolean>` to `boolean` turns every affected call site into a compile error that names its own file and line. The two places where a genuine test is added (Task 1 Step 1, Task 6 Step 4) are exactly the two places where the compiler cannot help — a wrapper reaching string concatenation, and a default value silently becoming empty. Those are the failure modes that shipped.

**The snapshot suite is the strongest check in this plan — but run it, don't stat it.** The committed JSON files record the full serialized shape of every component and task-dispatcher definition, so they catch any drift in a default value. The catch, learned the hard way in Task 2: `JsonFileAssert` compares against an existing file and only writes a missing one, so `git status` on the snapshot glob reports `0` whether or not serialization changed. Always judge from the `*DefinitionFactoryTest` / `*ComponentHandlerTest` results. Where a snapshot legitimately must change (an unseeded DSL field gaining a default), regenerate only the affected files and review the semantic diff — normalise both sides through a JSON parser so formatting churn doesn't hide the real change.

**What is deliberately not changing.** Getters whose default is `Optional.empty()` keep `Optional` — `getDescription`, `getTitle`, `getHelp`, `getIcon`, `getConnection`, `getResources`, `getOutputDefinition`, `getUnifiedApi`, and the function getters. Absence is meaningful for those, and on a public SDK surface with no NullAway in the build, `Optional` is the only signal the compiler enforces.
