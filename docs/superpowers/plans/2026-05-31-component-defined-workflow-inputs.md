# Component-defined Workflow Inputs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let workflow authors define inputs that reference a component's input property or property group, and render them at deploy time as the component's real (optionally connection-backed) fields, with groups rendered as one compound input whose value is a nested object.

**Architecture:** Add a top-level `properties` list and a new `propertyGroups` list to the component definition (DSL → domain → REST → client). Persist component-defined workflow inputs as a *reference* (`componentName`, `componentVersion`, `propertyName` | `groupName`) inside the workflow definition JSON. At deploy time, resolve the reference against the live component definition, convert it to the existing `PropertyAllType`, and render it with the existing `Property`/`Properties`/`SubflowInputGroup` machinery. Dynamic options reuse the node-scoped `getWorkflowNodeOptions` endpoint via a workflow node that already uses the component; with no connection set the field degrades to free-text.

**Tech Stack:** Java 25 / component DSL (`ComponentDsl`), Spring Boot REST + OpenAPI generator + MapStruct, React 19 / TypeScript, react-hook-form, TanStack Query.

**Related spec:** `docs/superpowers/specs/2026-05-31-component-defined-workflow-inputs-design.md`

---

## Ground Truth (verified during planning)

- **Component-level `properties` does NOT exist yet.** `ModifiableComponentDefinition`
  (`sdks/backend/java/component-api/.../ComponentDsl.java:1294-1522`) has `actions`,
  `triggers`, `connection`, etc., but **no** `properties` field, and its parent interface
  `WorkflowComponentDefinition.java` exposes only `getActions/getCustomAction/getCustomActionHelp/getTriggers`.
  `.properties(...)` exists only on `ModifiableActionDefinition` (`:471-488`),
  `ModifiableTriggerDefinition` (`:3599`), `ModifiableConnectionDefinition` (`:1660`),
  `ModifiableClusterElementDefinition` (`:1858`), and `ModifiableObjectProperty` (`:2826`).
  → We ADD component-level `properties` + new `propertyGroups`.
- **No `PropertyGroup` exists anywhere** (`grep PropertyGroup` empty across sdks/server/client).
- **Component factory:** `component(String name)` at `ComponentDsl.java:80`.
- **Action property pattern to mirror:** field `:308`; varargs builder `:472`; list builder `:480`;
  getter `:600` (`Optional<List<? extends Property>> getProperties()`).
- **ObjectProperty grouping pattern to mirror:** `.properties(...)` `:2826/2833`,
  `.optionsLookupDependsOn(...)` `:2791`, getter `:2902`.
- **Static factories:** `string(name)` `:232`, `object(name)` `:148`, `integer(name)` `:124`,
  `bool(name)` `:76`.
- **Platform domain** `com.bytechef.platform.component.domain.ComponentDefinition`
  (`server/libs/platform/platform-component/platform-component-api/.../domain/ComponentDefinition.java`):
  fields `:41-59`, copy-constructor `:71-120`, `getActions(...)` static mapper `:276-283`.
  Domain `Property.toProperty(...)` is the polymorphic converter used by
  `ActionDefinition` (`.../domain/ActionDefinition.java`) via
  `CollectionUtils.map(actionDefinition.getProperties().orElse(List.of()), Property::toProperty)`.
- **REST is authoritative for the single-component fetch.** Client authoring uses
  `useGetComponentDefinitionQuery` →
  `ComponentDefinitionApi.getComponentDefinition`
  (`client/src/shared/queries/platform/componentDefinitions.queries.ts`), i.e. the REST model
  generated from the OpenAPI yaml above
  (`ComponentDefinition:` schema at `:1974`, `WorkflowInput:` at `:3017`).
  The GraphQL `component-definition.graphqls` is used by the editor's component *list*; update it
  too only if a consumer needs `properties` there (out of scope unless a test fails).
- **Workflow input domain record:** `Workflow.Input(String name, String label, String type, boolean required)`
  at `atlas-configuration/.../domain/Workflow.java:401-404`; JSON parse at `:165-171` using
  `WorkflowConstants.{INPUTS,NAME,LABEL,TYPE,REQUIRED}`.
- **workflow-api `Input`** is an empty marker interface
  (`sdks/backend/java/workflow-api/.../definition/Input.java`) → no change needed.
- **Client `WorkflowInput`** model
  (`client/src/shared/middleware/platform/configuration/models/WorkflowInput.ts`):
  `{label?, name, required?, type?}`; `WorkflowInputType` adds `testValue?` (`types.ts:422-424`).
- **Deploy render seam:** `convertInputToProperty`
  (`client/src/shared/components/InputConfigurationList.tsx:33-50`); compound container
  `SubflowInputGroup` (`:90-151`); `PropertyComboBox` props in `Property.tsx:609-656`;
  options request `GetWorkflowNodeOptionsRequest = {id, workflowNodeName, propertyName, environmentId, lookupDependsOnPaths?, searchText?}`.
- **Components used by a workflow:** `workflow.workflowTaskComponentNames` +
  `workflow.workflowTriggerComponentNames` (client `Workflow` model).
- **Save path:** `saveWorkflowInput` in
  `client/src/pages/platform/workflow-editor/components/workflow-inputs/hooks/useWorkflowInputs.ts:136-221`
  (writes `workflowDefinition.inputs` into `workflow.definition` JSON).

> Verified `ComponentDsl.java:1294-1535` directly: `ModifiableComponentDefinition` fields
> (`:1296-1311`), `actions(...)` varargs/list builders (`:1320-1329`), `getActions()` (`:1457-1460`).
> No component-level `properties` field exists. Insert new builders after `actions(List<A>)`
> (`:1329`) and new getters after `getActions()` (`:1460`).

## File Structure

**Server — component DSL (component-api)**
- `sdks/backend/java/component-api/.../definition/PropertyGroup.java` — NEW interface.
- `sdks/backend/java/component-api/.../definition/ComponentDefinition.java` — add `getProperties()` + `getPropertyGroups()`.
- `sdks/backend/java/component-api/.../definition/WorkflowComponentDefinition.java` — add `getProperties()`.
- `sdks/backend/java/component-api/.../definition/ComponentDsl.java` — `propertyGroup(...)` factory, `ModifiablePropertyGroup`, component-level `properties(...)` + `propertyGroups(...)` + getters.

**Server — platform domain (platform-component-api)**
- `.../platform/component/domain/PropertyGroup.java` — NEW domain class.
- `.../platform/component/domain/ComponentDefinition.java` — map `properties` + `propertyGroups`.

**Server — REST (platform-configuration-rest)**
- `.../platform-configuration-rest-impl/openapi.yaml` — extend `ComponentDefinition` + `WorkflowInput` schemas; add `PropertyGroup` schema.
- generated models + MapStruct mapper (regenerated/auto-mapped).

**Server — workflow input domain (atlas-configuration)**
- `.../atlas/configuration/domain/Workflow.java` — extend `Input` record + parser.
- `.../atlas/configuration/constant/WorkflowConstants.java` — add constants.

**Client**
- `client/src/shared/middleware/.../models/WorkflowInput.ts`, `ComponentDefinition.ts`, new `PropertyGroup.ts` (regenerated).
- `client/src/shared/types.ts` — extend `WorkflowInputType` if needed.
- `client/src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx` — authoring flow.
- `client/src/pages/platform/workflow-editor/components/workflow-inputs/hooks/useWorkflowInputs.ts` — persist reference.
- new `.../workflow-inputs/utils/getWorkflowComponentNames.ts` — components-used helper.
- `client/src/shared/components/InputConfigurationList.tsx` — reference resolution + group render + options wiring.

---

## Task 1: Component DSL — `PropertyGroup` interface

**Files:**
- Create: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/PropertyGroup.java`

- [ ] **Step 1: Create the interface**

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

package com.bytechef.component.definition;

import java.util.List;
import java.util.Optional;

/**
 * A named, explicitly-declared group of component input properties whose members may depend on
 * one another via {@code optionsLookupDependsOn}. Picked and rendered together as one compound
 * workflow input.
 *
 * @author Ivica Cardic
 */
public interface PropertyGroup {

    String getName();

    Optional<String> getLabel();

    List<? extends Property.ValueProperty<?>> getProperties();
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :sdks:backend:java:component-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/PropertyGroup.java
git commit -m "732 Add PropertyGroup definition interface to component-api"
```

---

## Task 2: Component DSL — expose `getProperties()` + `getPropertyGroups()` on the interface

**Files:**
- Modify: `sdks/backend/java/component-api/.../definition/WorkflowComponentDefinition.java`
- Modify: `sdks/backend/java/component-api/.../definition/ComponentDefinition.java`

- [ ] **Step 1: Add `getProperties()` to `WorkflowComponentDefinition`**

In `WorkflowComponentDefinition.java`, after `getCustomActionHelp()` and before `getTriggers()`:

```java
    /**
     * The component's reusable input properties, selectable as workflow inputs.
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();
```

- [ ] **Step 2: Add `getPropertyGroups()` to `ComponentDefinition`**

In `ComponentDefinition.java`, after `getName()`:

```java
    /**
     * The component's explicitly-declared property groups (compound inputs).
     *
     * @return
     */
    Optional<List<? extends PropertyGroup>> getPropertyGroups();
```

- [ ] **Step 3: Compile (expected to FAIL until Task 3)**

Run: `./gradlew :sdks:backend:java:component-api:compileJava`
Expected: FAIL — `ModifiableComponentDefinition` does not yet implement the new abstract
methods. This confirms the interface methods are wired. Proceed to Task 3 (do not commit yet).

---

## Task 3: Component DSL — `ModifiablePropertyGroup`, `propertyGroup(...)`, component-level builders/getters

**Files:**
- Modify: `sdks/backend/java/component-api/.../definition/ComponentDsl.java`

- [ ] **Step 1: Add the `propertyGroup(String name)` static factory**

Immediately after the `component(String name)` factory at `:80`:

```java
    public static ModifiablePropertyGroup propertyGroup(String name) {
        return new ModifiablePropertyGroup(name);
    }
```

- [ ] **Step 2: Add component-level `properties`/`propertyGroups` fields**

In `ModifiableComponentDefinition` (with the field block `:1296-1311`), add:

```java
        private List<? extends Property> properties;
        private List<ModifiablePropertyGroup> propertyGroups;
```

- [ ] **Step 3: Add the builders**

After the `actions(List<A> ...)` builder (ends `:1329`):

```java
        @SafeVarargs
        public final <P extends Property> ModifiableComponentDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableComponentDefinition propertyGroups(ModifiablePropertyGroup... propertyGroups) {
            if (propertyGroups != null) {
                this.propertyGroups = List.of(propertyGroups);
            }

            return this;
        }
```

- [ ] **Step 4: Add the getters**

After `getActions()` (`:1457-1460`):

```java
        @Override
        public Optional<List<? extends Property>> getProperties() {
            return Optional.ofNullable(properties);
        }

        @Override
        public Optional<List<? extends PropertyGroup>> getPropertyGroups() {
            return Optional.ofNullable(propertyGroups);
        }
```

- [ ] **Step 5: Add the `ModifiablePropertyGroup` class**

Add as a `public static final class` near the other modifiable classes (e.g. just before
`ModifiableConnectionDefinition`):

```java
    public static final class ModifiablePropertyGroup implements PropertyGroup {

        private String label;
        private final String name;
        private List<? extends Property.ValueProperty<?>> properties = List.of();

        private ModifiablePropertyGroup(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiablePropertyGroup label(String label) {
            this.label = label;

            return this;
        }

        @SafeVarargs
        public final <P extends Property.ValueProperty<?>> ModifiablePropertyGroup properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        @Override
        public List<? extends Property.ValueProperty<?>> getProperties() {
            return properties;
        }
    }
```

- [ ] **Step 6: Compile**

Run: `./gradlew :sdks:backend:java:component-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Spotless**

Run: `./gradlew :sdks:backend:java:component-api:spotlessApply`
Expected: formatting applied, no errors.

---

## Task 4: Component DSL — unit test for properties + groups

**Files:**
- Create: `sdks/backend/java/component-api/src/test/java/com/bytechef/component/definition/ComponentDslPropertyGroupTest.java`

- [ ] **Step 1: Write the failing test**

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

package com.bytechef.component.definition;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.propertyGroup;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ComponentDslPropertyGroupTest {

    @Test
    void testComponentLevelProperties() {
        ComponentDefinition componentDefinition = component("sample")
            .properties(string("spreadsheetId").label("Spreadsheet"));

        List<? extends Property> properties = componentDefinition.getProperties()
            .orElseThrow();

        assertEquals(1, properties.size());
        assertEquals("spreadsheetId", properties.getFirst()
            .getName());
    }

    @Test
    void testPropertyGroups() {
        ComponentDefinition componentDefinition = component("sample")
            .propertyGroups(
                propertyGroup("sheetSelection")
                    .label("Sheet")
                    .properties(
                        string("spreadsheetId").label("Spreadsheet"),
                        string("sheetName").label("Sheet")
                            .optionsLookupDependsOn("spreadsheetId")));

        List<? extends PropertyGroup> groups = componentDefinition.getPropertyGroups()
            .orElseThrow();

        assertEquals(1, groups.size());

        PropertyGroup group = groups.getFirst();

        assertEquals("sheetSelection", group.getName());
        assertEquals("Sheet", group.getLabel()
            .orElseThrow());
        assertEquals(2, group.getProperties()
            .size());
        assertTrue(componentDefinition.getProperties()
            .isEmpty());
    }
}
```

- [ ] **Step 2: Run — verify it passes**

Run: `./gradlew :sdks:backend:java:component-api:test --tests '*ComponentDslPropertyGroupTest'`
Expected: PASS (implementation already exists from Task 3; this locks the contract).

- [ ] **Step 3: Commit Tasks 2–4 together**

```bash
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/WorkflowComponentDefinition.java \
        sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDefinition.java \
        sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java \
        sdks/backend/java/component-api/src/test/java/com/bytechef/component/definition/ComponentDslPropertyGroupTest.java
git commit -m "732 Add component-level properties and property groups to component DSL"
```

---

## Task 5: Platform domain — `PropertyGroup` domain class

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/PropertyGroup.java`

- [ ] **Step 1: Create the domain class**

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

package com.bytechef.platform.component.domain;

import com.bytechef.commons.util.CollectionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class PropertyGroup {

    private String label;
    private String name;
    private List<? extends Property> properties;

    private PropertyGroup() {
    }

    public PropertyGroup(com.bytechef.component.definition.PropertyGroup propertyGroup) {
        this.label = propertyGroup.getLabel()
            .orElse(null);
        this.name = propertyGroup.getName();
        this.properties = CollectionUtils.map(propertyGroup.getProperties(), Property::toProperty);
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyGroup that)) {
            return false;
        }

        return Objects.equals(label, that.label) && Objects.equals(name, that.name) &&
            Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, name, properties);
    }

    @Override
    public String toString() {
        return "PropertyGroup{" +
            "name='" + name + '\'' +
            ", label='" + label + '\'' +
            ", properties=" + properties +
            '}';
    }
}
```

> Note: confirm the static converter is named `Property.toProperty` in
> `.../domain/Property.java` (verified during planning). If the project exposes it under a
> different name, use that name here and in Task 6.

- [ ] **Step 2: Compile**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/PropertyGroup.java
git commit -m "732 Add PropertyGroup domain class"
```

---

## Task 6: Platform domain — map `properties` + `propertyGroups` in `ComponentDefinition`

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/.../domain/ComponentDefinition.java`

- [ ] **Step 1: Add fields**

After `private List<ActionDefinition> actions;` (`:42`):

```java
    private List<Property> properties;
    private List<PropertyGroup> propertyGroups;
```

- [ ] **Step 2: Map in the copy-constructor**

In the `ComponentDefinition(com.bytechef.component.definition.ComponentDefinition componentDefinition)`
constructor (after `this.actions = getActions(componentDefinition);`, `:72`):

```java
        this.properties = getProperties(componentDefinition);
        this.propertyGroups = getPropertyGroups(componentDefinition);
```

- [ ] **Step 3: Add the static mappers**

Next to `getActions(...)` (`:276`):

```java
    private static List<Property> getProperties(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return componentDefinition.getProperties()
            .map(properties -> CollectionUtils.map(properties, Property::toProperty))
            .orElse(Collections.emptyList());
    }

    private static List<PropertyGroup> getPropertyGroups(
        com.bytechef.component.definition.ComponentDefinition componentDefinition) {

        return componentDefinition.getPropertyGroups()
            .map(propertyGroups -> CollectionUtils.map(propertyGroups, PropertyGroup::new))
            .orElse(Collections.emptyList());
    }
```

- [ ] **Step 4: Add getters**

After `getActions()` (`:138-140`):

```java
    public List<Property> getProperties() {
        return properties;
    }

    public List<PropertyGroup> getPropertyGroups() {
        return propertyGroups;
    }
```

- [ ] **Step 5: Initialize in the `(String name)` constructor and update `equals`/`hashCode`/`toString`**

In `public ComponentDefinition(String name)` (`:64-69`) add:

```java
        this.properties = List.of();
        this.propertyGroups = List.of();
```

Add `properties` and `propertyGroups` to the `Objects.equals(...)` chain, the
`Objects.hash(...)` argument list, and the `toString()` body, following the existing field
pattern.

- [ ] **Step 6: Compile**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ComponentDefinition.java
git commit -m "732 Map component properties and property groups in domain ComponentDefinition"
```

---

## Task 7: REST — extend OpenAPI schema for `ComponentDefinition` + `PropertyGroup`

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml`

- [ ] **Step 1: Read the existing `ComponentDefinition` schema**

Run: `sed -n '1974,2080p' server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml`
Expected: the schema with `actions`, `triggers`, `connection`, etc. Note the existing
`$ref: "#/components/schemas/Property"` usages (Property schema already exists because actions
expose properties).

- [ ] **Step 2: Add `properties` and `propertyGroups` to the `ComponentDefinition` schema**

Under `ComponentDefinition.properties:`, add (matching the existing 6-space indentation and the
existing `Property` ref used by actions):

```yaml
        properties:
          type: "array"
          description: "The component's reusable input properties selectable as workflow inputs."
          items:
            $ref: "#/components/schemas/Property"
        propertyGroups:
          type: "array"
          description: "The component's explicitly-declared property groups (compound inputs)."
          items:
            $ref: "#/components/schemas/PropertyGroup"
```

- [ ] **Step 3: Add the `PropertyGroup` schema**

In the `components.schemas` block (e.g. directly after the `ComponentDefinition` schema ends):

```yaml
    PropertyGroup:
      title: "Property group"
      description: "A named group of component input properties rendered as one compound input."
      type: "object"
      required:
        - "name"
      properties:
        name:
          type: "string"
        label:
          type: "string"
        properties:
          type: "array"
          items:
            $ref: "#/components/schemas/Property"
```

- [ ] **Step 4: Commit (regeneration happens in Task 9)**

```bash
git add server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml
git commit -m "732 Add component properties and propertyGroups to component-definition OpenAPI schema"
```

---

## Task 8: REST — extend OpenAPI `WorkflowInput` schema with reference fields

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml`

- [ ] **Step 1: Read the existing `WorkflowInput` schema**

Run: `sed -n '3017,3050p' server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml`
Expected: `label`, `name`, `required`, `type` properties.

- [ ] **Step 2: Add reference fields**

Under `WorkflowInput.properties:`, add (matching indentation):

```yaml
        componentName:
          description: "The name of the component a referenced input property belongs to."
          type: "string"
        componentVersion:
          description: "The version of the component a referenced input property belongs to."
          type: "integer"
          format: "int32"
        propertyName:
          description: "The name of the referenced component input property."
          type: "string"
        groupName:
          description: "The name of the referenced component property group (compound input)."
          type: "string"
```

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml
git commit -m "732 Add component-reference fields to WorkflowInput OpenAPI schema"
```

---

## Task 9: Regenerate REST + TypeScript models, fix mapper, compile

**Files:**
- Auto-generated: `ComponentDefinitionModel.java`, `WorkflowInputModel.java`, `PropertyGroupModel.java` (server) + TS models under `client/src/shared/middleware/...`.
- Possibly modify: `.../web/rest/mapper/ComponentDefinitionMapper.java` (MapStruct).

- [ ] **Step 1: Regenerate models**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-api:openApiGenerate` (or the project's documented codegen task; if unsure run `./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-api:build -x test`).
Expected: `ComponentDefinitionModel` gains `properties`/`propertyGroups`; new `PropertyGroupModel`; `WorkflowInputModel` gains the four reference fields.

- [ ] **Step 2: Compile server; resolve MapStruct gaps**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:compileJava`
Expected: BUILD SUCCESSFUL. MapStruct auto-maps same-named fields
(domain `ComponentDefinition.getProperties()/getPropertyGroups()` →
`ComponentDefinitionModel`). If MapStruct reports an unmapped target/type mismatch for
`PropertyGroup → PropertyGroupModel`, add an explicit mapping method to
`ComponentDefinitionMapper` mirroring how `Property → PropertyModel` is mapped, then recompile.

- [ ] **Step 3: Regenerate the TypeScript client models**

Run: the client model codegen task (e.g. `./gradlew :client:generateOpenAPITypeScriptFetch` or the documented equivalent), then `cd client && npm run typecheck`.
Expected: `ComponentDefinition.ts` gains `properties?`/`propertyGroups?`; new `PropertyGroup.ts`; `WorkflowInput.ts` gains `componentName?`, `componentVersion?`, `propertyName?`, `groupName?`. Typecheck passes.

- [ ] **Step 4: Commit generated changes**

```bash
git add server/libs/platform/platform-configuration/platform-configuration-rest client/src/shared/middleware
git commit -m "732 Regenerate REST and TS models for component-defined workflow inputs"
```

---

## Task 10: Workflow input domain — extend `Input` record + parser + constants

**Files:**
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/.../domain/Workflow.java`
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/.../constant/WorkflowConstants.java`
- Test: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowInputParsingTest.java`

- [ ] **Step 1: Add constants**

In `WorkflowConstants.java`, add (alphabetically/with the existing block):

```java
    public static final String COMPONENT_NAME = "componentName";
    public static final String COMPONENT_VERSION = "componentVersion";
    public static final String GROUP_NAME = "groupName";
    public static final String PROPERTY_NAME = "propertyName";
```

- [ ] **Step 2: Write the failing parser test**

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

package com.bytechef.atlas.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowInputParsingTest {

    @Test
    void testParsesComponentReferenceInput() {
        Workflow workflow = new Workflow(
            "id1",
            Map.of(
                "inputs", List.of(
                    Map.of(
                        "name", "channel",
                        "label", "Channel",
                        "componentName", "slack",
                        "componentVersion", 1,
                        "propertyName", "channelId"))),
            Workflow.Format.JSON,
            "{}",
            Map.of());

        Workflow.Input input = workflow.getInputs()
            .getFirst();

        assertEquals("channel", input.name());
        assertEquals("slack", input.componentName());
        assertEquals(1, input.componentVersion());
        assertEquals("channelId", input.propertyName());
        assertNull(input.groupName());
    }
}
```

> Adjust the `Workflow` constructor invocation to the real signature in `Workflow.java` (the
> test goal is: a parsed reference input exposes the new accessors). Confirm the constructor
> shape via `grep -n "public Workflow(" Workflow.java` before running.

- [ ] **Step 3: Run — verify it fails**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests '*WorkflowInputParsingTest'`
Expected: FAIL (compile error — `componentName()` etc. do not exist yet).

- [ ] **Step 4: Extend the `Input` record**

Replace the record (`:401-404`) with:

```java
    public record Input(
        String name, String label, String type, boolean required, String componentName,
        Integer componentVersion, String propertyName, String groupName) implements Serializable {

        public Input(String name, String label, String type, boolean required) {
            this(name, label, type, required, null, null, null, null);
        }
    }
```

> The extra compact-friendly constructor keeps existing 4-arg call sites compiling.

- [ ] **Step 5: Extend the parser**

Replace the `new Input(...)` mapping (`:167-171`) with:

```java
                    map -> new Input(
                        MapUtils.getRequiredString(map, WorkflowConstants.NAME),
                        MapUtils.getString(map, WorkflowConstants.LABEL),
                        MapUtils.getString(map, WorkflowConstants.TYPE, "string"),
                        MapUtils.getBoolean(map, WorkflowConstants.REQUIRED, false),
                        MapUtils.getString(map, WorkflowConstants.COMPONENT_NAME),
                        MapUtils.getInteger(map, WorkflowConstants.COMPONENT_VERSION),
                        MapUtils.getString(map, WorkflowConstants.PROPERTY_NAME),
                        MapUtils.getString(map, WorkflowConstants.GROUP_NAME)));
```

> Verify `MapUtils.getInteger(Map, String)` exists; if the nullable variant differs, use the
> matching MapUtils accessor.

- [ ] **Step 6: Run — verify it passes**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests '*WorkflowInputParsingTest'`
Expected: PASS.

- [ ] **Step 7: Check existing call sites + WorkflowDTO mapping**

Run: `grep -rn "new Workflow.Input(\|Input(" server/libs --include=*.java | grep -i "workflow.input\|\.Input("`
Expected: existing 4-arg usages still compile via the added constructor. If `WorkflowDTO` or a
REST mapper copies input fields, extend those to carry the new fields (mirror `name/label`).

- [ ] **Step 8: Compile the configuration modules + commit**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:compileJava :server:libs:platform:platform-configuration:platform-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add server/libs/atlas/atlas-configuration server/libs/platform/platform-configuration
git commit -m "732 Add component-reference fields to workflow Input domain and parser"
```

---

## Task 11: Client — "components used by this workflow" helper

**Files:**
- Create: `client/src/pages/platform/workflow-editor/components/workflow-inputs/utils/getWorkflowComponentNames.ts`
- Test: `client/src/pages/platform/workflow-editor/components/workflow-inputs/utils/getWorkflowComponentNames.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import {Workflow} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getWorkflowComponentNames from './getWorkflowComponentNames';

describe('getWorkflowComponentNames', () => {
    it('returns the union of task and trigger component names, de-duplicated and sorted', () => {
        const workflow = {
            workflowTaskComponentNames: ['slack', 'googleSheets', 'slack'],
            workflowTriggerComponentNames: ['googleSheets', 'webhook'],
        } as Workflow;

        expect(getWorkflowComponentNames(workflow)).toEqual(['googleSheets', 'slack', 'webhook']);
    });

    it('returns an empty array when no components are present', () => {
        expect(getWorkflowComponentNames({} as Workflow)).toEqual([]);
    });
});
```

- [ ] **Step 2: Run — verify it fails**

Run: `cd client && npm run test -- getWorkflowComponentNames`
Expected: FAIL (module not found).

- [ ] **Step 3: Implement**

```ts
import {Workflow} from '@/shared/middleware/platform/configuration';

const getWorkflowComponentNames = (workflow: Workflow): string[] => {
    const componentNames = new Set<string>([
        ...(workflow.workflowTaskComponentNames ?? []),
        ...(workflow.workflowTriggerComponentNames ?? []),
    ]);

    return Array.from(componentNames).sort();
};

export default getWorkflowComponentNames;
```

- [ ] **Step 4: Run — verify it passes**

Run: `cd client && npm run test -- getWorkflowComponentNames`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/workflow-inputs/utils/getWorkflowComponentNames.ts \
        client/src/pages/platform/workflow-editor/components/workflow-inputs/utils/getWorkflowComponentNames.test.ts
git commit -m "732 client - Add getWorkflowComponentNames helper for workflow inputs"
```

---

## Task 12: Client — authoring flow in `WorkflowInputsEditDialog`

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx`
- Modify (if needed): `client/src/shared/types.ts` (`WorkflowInputType`)

- [ ] **Step 1: Add the "Component property" type option**

In the `Type` `<SelectContent>` (`:137-151`), add as the first item:

```tsx
                                                <SelectItem value="component">Component property</SelectItem>
```

- [ ] **Step 2: Render cascading pickers when `component` is selected**

When `selectedType === 'component'`, render two selects below the Type field:
- **Component** — options from `getWorkflowComponentNames(workflow)` (pass `workflow` in as a
  prop from the parent sheet; confirm the dialog already receives it, else thread it through).
- **Property or Group** — fetched via `useGetComponentDefinitionQuery({componentName, componentVersion})`;
  options are `componentDefinition.properties` (label: `property.label ?? property.name`,
  value: `prop:<name>`) and `componentDefinition.propertyGroups` (value: `group:<name>`).

On picking a property/group, set form values:

```tsx
form.setValue('componentName', componentName);
form.setValue('componentVersion', componentVersion);
// for a single property:
form.setValue('propertyName', propertyName);
form.setValue('groupName', undefined);
// for a group:
form.setValue('groupName', groupName);
form.setValue('propertyName', undefined);
// auto-fill (editable):
form.setValue('name', form.getValues('name') || pickedName);
form.setValue('label', form.getValues('label') || pickedLabel);
```

Hide the primitive-only `type` semantics for component inputs (the resolved type comes from the
component at render time); keep `name`, `label`, `required` visible.

- [ ] **Step 3: Test value handling**

For `selectedType === 'component'`, render the Test Value as plain text (free-text). A
connection-backed live picker for the test value is out of scope for this phase; document the
limitation with a one-line helper text.

- [ ] **Step 4: Component tests**

Add cases to (or create) `WorkflowInputsEditDialog.test.tsx`:
- selecting "Component property" reveals the Component select;
- picking a component then a property auto-fills name/label and sets `propertyName`;
- picking a group sets `groupName` and clears `propertyName`.

```tsx
// sketch — adapt to existing render harness / mocks in the sibling tests
it('sets propertyName when a component property is picked', async () => {
    // render dialog with currentInputIndex=-1 and a workflow using ['slack']
    // mock useGetComponentDefinitionQuery -> {properties: [{name: 'channelId', label: 'Channel'}], propertyGroups: []}
    // select type "Component property", select component "slack", select property "Channel"
    // expect form.getValues('propertyName') === 'channelId' and name === 'channelId'
});
```

- [ ] **Step 5: Run checks**

Run: `cd client && npm run test -- WorkflowInputsEditDialog && npm run check`
Expected: PASS; lint/typecheck clean (watch sort-keys, interface `I`/`Props` suffix,
import-destructure order, Lucide `Icon` suffix, `twMerge`).

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/workflow-inputs client/src/shared/types.ts
git commit -m "732 client - Add component-property authoring flow to Workflow Inputs dialog"
```

---

## Task 13: Client — persist reference fields in `useWorkflowInputs`

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/workflow-inputs/hooks/useWorkflowInputs.ts`

- [ ] **Step 1: Persist the reference fields**

In `saveWorkflowInput` (`:136-221`), the input object is spread into `inputs` already; ensure
the new fields (`componentName`, `componentVersion`, `propertyName`, `groupName`) survive into
the persisted `input`. Only `testValue` is deleted before save (`delete input['testValue'];`);
do not strip the reference fields. If the form leaves stale `type` for component inputs, clear
it:

```ts
if (input.componentName) {
    delete input['type'];
}
```

- [ ] **Step 2: Manual verification**

Author a single-property input and a group input; reload the editor; confirm the workflow
definition JSON `inputs[]` contains the reference fields (inspect via the Code editor or
network response).

- [ ] **Step 3: Run checks + commit**

Run: `cd client && npm run check`
Expected: PASS.

```bash
git add client/src/pages/platform/workflow-editor/components/workflow-inputs/hooks/useWorkflowInputs.ts
git commit -m "732 client - Persist component-reference fields for workflow inputs"
```

---

## Task 14: Client — resolve references + render single property at deploy time

**Files:**
- Modify: `client/src/shared/components/InputConfigurationList.tsx`
- Test: `client/src/shared/components/InputConfigurationList.test.tsx`

- [ ] **Step 1: Write the failing resolver test**

Extract reference→`PropertyAllType` resolution into a pure helper
`resolveComponentInputProperty(input, componentDefinition)` so it is unit-testable:

```tsx
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {resolveComponentInputProperty} from './InputConfigurationList';

describe('resolveComponentInputProperty', () => {
    const componentDefinition = {
        name: 'slack',
        properties: [{controlType: 'SELECT', label: 'Channel', name: 'channelId', type: 'STRING'}],
        propertyGroups: [],
        version: 1,
    } as unknown as ComponentDefinition;

    it('resolves a single property reference to the component property', () => {
        const property = resolveComponentInputProperty(
            {componentName: 'slack', componentVersion: 1, name: 'channel', propertyName: 'channelId'},
            componentDefinition
        );

        expect(property?.name).toBe('channel');
        expect(property?.controlType).toBe('SELECT');
    });

    it('returns a disabled placeholder for a dangling reference', () => {
        const property = resolveComponentInputProperty(
            {componentName: 'slack', componentVersion: 1, name: 'gone', propertyName: 'missing'},
            componentDefinition
        );

        expect(property?.name).toBe('gone');
        expect(property?.controlType).toBe('TEXT');
    });
});
```

- [ ] **Step 2: Run — verify it fails**

Run: `cd client && npm run test -- InputConfigurationList`
Expected: FAIL (`resolveComponentInputProperty` not exported).

- [ ] **Step 3: Implement the resolver**

Export from `InputConfigurationList.tsx`. The resolved property keeps the *input* `name` (so the
form binds to `inputs.<name>`) but takes `controlType`/`type`/`options`/`optionsDataSource` from
the component property. Dangling reference → a disabled `TEXT` placeholder with a description
"This input is no longer available.".

```tsx
export const resolveComponentInputProperty = (
    input: WorkflowInput,
    componentDefinition?: ComponentDefinition
): PropertyAllType | undefined => {
    const componentProperty = componentDefinition?.properties?.find(
        (property) => property.name === input.propertyName
    );

    if (!componentProperty) {
        return {
            controlType: ControlType.Text,
            description: 'This input is no longer available.',
            disabled: true,
            label: input.label || input.name,
            name: input.name,
            type: PropertyType.String,
        } as PropertyAllType;
    }

    return {
        ...componentProperty,
        label: input.label || componentProperty.label,
        name: input.name,
        required: input.required ?? componentProperty.required,
    } as PropertyAllType;
};
```

- [ ] **Step 4: Wire resolver into `convertInputToProperty`**

In `convertInputToProperty` (`:33-50`), branch first on `input.componentName`: for a single
reference (`propertyName` set), call `resolveComponentInputProperty` using a component
definition the component fetches/caches (see Step 5). For non-reference inputs keep the existing
primitive switch unchanged.

- [ ] **Step 5: Fetch component definitions for referenced inputs**

In `InputConfigurationList`, collect the distinct `{componentName, componentVersion}` from
`inputs` that carry references, and fetch their definitions (use
`useGetComponentDefinitionQuery` per distinct component, or a small wrapper that batches). Pass
the resolved map into the conversion. Guard for loading state (render a lightweight skeleton or
the free-text placeholder until loaded).

- [ ] **Step 6: Run — verify it passes**

Run: `cd client && npm run test -- InputConfigurationList`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add client/src/shared/components/InputConfigurationList.tsx client/src/shared/components/InputConfigurationList.test.tsx
git commit -m "732 client - Resolve component-defined inputs to real properties at deploy time"
```

---

## Task 15: Client — dynamic options via the backing node connection

**Files:**
- Modify: `client/src/shared/components/InputConfigurationList.tsx`
- Modify: `client/src/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem.tsx`

- [ ] **Step 1: Determine the backing node for a referenced input**

For each referenced input, pick the workflow node whose component matches `componentName` and
whose deployment connection is configured; if several match, the first deterministically. Expose
its `workflowNodeName` (and the leaf component `propertyName`) so the rendered `PropertyComboBox`
issues `getWorkflowNodeOptions` against that node. (The workflow's tasks/triggers are available
on the `Workflow` object passed into the step item; thread the chosen `workflowNodeName` to the
resolver/property.)

- [ ] **Step 2: Free-text fallback when no connection**

If no matching node has a configured connection, render the input as free-text (skip the options
query). Reuse the placeholder/text path from Task 14. Add a `log`/helper text noting the
connection is needed for live options.

- [ ] **Step 3: Manual verification**

With a workflow containing a Slack node (connection configured) and a Slack channel input:
- the input shows a live channel dropdown;
- a group member with `optionsLookupDependsOn` refreshes when its parent value changes;
- clearing the node's connection degrades the field to free-text.

- [ ] **Step 4: Run checks + commit**

Run: `cd client && npm run check`
Expected: PASS.

```bash
git add client/src/shared/components client/src/pages/automation/project-deployments
git commit -m "732 client - Source component-input options from backing node connection"
```

---

## Task 16: Client — compound group rendering with nested values

**Files:**
- Modify: `client/src/shared/components/InputConfigurationList.tsx`
- Test: `client/src/shared/components/InputConfigurationList.test.tsx`

- [ ] **Step 1: Write the failing group-resolution test**

```tsx
it('resolves a group reference to a compound property with nested members', () => {
    const componentDefinition = {
        name: 'googleSheets',
        properties: [],
        propertyGroups: [
            {
                label: 'Sheet',
                name: 'sheetSelection',
                properties: [
                    {controlType: 'SELECT', label: 'Spreadsheet', name: 'spreadsheetId', type: 'STRING'},
                    {controlType: 'SELECT', label: 'Sheet', name: 'sheetName', type: 'STRING'},
                ],
            },
        ],
        version: 1,
    } as unknown as ComponentDefinition;

    const group = resolveComponentInputGroup(
        {componentName: 'googleSheets', componentVersion: 1, groupName: 'sheetSelection', name: 'sheet'},
        componentDefinition
    );

    expect(group?.members).toHaveLength(2);
    expect(group?.name).toBe('sheet');
});
```

- [ ] **Step 2: Run — verify it fails**

Run: `cd client && npm run test -- InputConfigurationList`
Expected: FAIL (`resolveComponentInputGroup` not exported).

- [ ] **Step 3: Implement group resolution + rendering**

Add `resolveComponentInputGroup(input, componentDefinition)` returning
`{name, label, members: PropertyAllType[]}` (members keep their own names). Render the group via
a `SubflowInputGroup`-style collapsible (reuse the existing component or a thin sibling) whose
`Properties` bind under `controlPath = ${baseControlPath}.${input.name}`, so member values nest
as an object (`${input.<name>.<member>}`). Dangling group → disabled placeholder (as in Task 14).

- [ ] **Step 4: Run — verify it passes**

Run: `cd client && npm run test -- InputConfigurationList`
Expected: PASS.

- [ ] **Step 5: Fold/refresh the legacy inline converter**

`ProjectDeploymentDialogWorkflowsStepItemInputs.tsx` duplicates the primitive `convertInputToProperty`.
Either route it through the shared resolver or update it identically so both code paths handle
references. Prefer routing through the shared path.

- [ ] **Step 6: Run checks + commit**

Run: `cd client && npm run check`
Expected: PASS.

```bash
git add client/src/shared/components client/src/pages/automation/project-deployments
git commit -m "732 client - Render component property groups as compound inputs with nested values"
```

---

## Task 17: End-to-end verification + server acceptance of nested values

**Files:**
- Possibly modify: project-deployment input handling (server) if a primitive-only assumption surfaces.

- [ ] **Step 1: Verify server accepts nested-object input values**

Check the deploy input persistence/validation path
(`WorkflowTestConfigurationFacadeImpl.validateInputs` only checks `required`; project
deployment stores inputs as a JSON map). Confirm a nested-object value for a group input
persists and reloads. If any code coerces input values to `String`, relax it for object values
(add a targeted test there).

- [ ] **Step 2: Full manual E2E**

1. Add `.properties(...)` and a `.propertyGroups(...)` group (with a connection-backed dynamic
   option) to a real component; rebuild.
2. In the editor, add + configure a node using that component.
3. Workflow Inputs → New Input → Component property → pick component → pick the single property,
   then (separately) the group.
4. Deploy; configure the node connection; verify the live dropdown + compound group; change the
   group's parent value and confirm the dependent option refreshes.
5. Save; inspect deployment `inputs`: scalar for single, nested object for group.
6. Remove the component property; reopen deploy dialog → disabled "no longer available"
   placeholder, no crash.

- [ ] **Step 3: Full checks**

Run: `cd client && npm run check` and `./gradlew spotlessApply check` (scoped to touched modules).
Expected: PASS.

- [ ] **Step 4: Commit any server acceptance fix**

```bash
git add server/libs/...   # only if a fix was needed
git commit -m "732 Accept nested-object values for component group workflow inputs"
```

---

## Testing Strategy

- **Server unit:** `ComponentDslPropertyGroupTest` (DSL); `WorkflowInputParsingTest` (reference
  parsing). Add a domain mapping assertion if a `ComponentDefinition` domain test exists.
- **Client unit:** `getWorkflowComponentNames.test.ts`; `InputConfigurationList.test.tsx`
  (single resolve, dangling, group resolve); `WorkflowInputsEditDialog.test.tsx` (authoring).
- **Manual/E2E:** Task 17 Step 2 — the full author→deploy→render→save loop, dependent-option
  refresh, and dangling-reference degradation.

## Rollback Plan

All changes are additive and opt-in. Server: new `PropertyGroup` types, new component
`properties`/`propertyGroups`, new optional `WorkflowInput` reference fields — none affect
primitive inputs. Client: the "Component property" type is additive; `convertInputToProperty`
falls through to the existing primitive branch when no reference is present. No DB migrations
(inputs live in the workflow definition JSON; deployment inputs are a JSON map). Revert the
commits to fully back out.
```
