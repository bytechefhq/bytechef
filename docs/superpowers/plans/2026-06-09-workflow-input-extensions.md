# Workflow Input Extensions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the component-defined workflow-input concepts (`componentReference`, `objectName`, `internalOnly`) out of the atlas `Workflow.Input` record and into a platform-level `WorkflowInput` that reads them from a generic per-input `extensions` map — mirroring how `WorkflowTrigger` gives meaning to the generic `extensions` map on atlas `Workflow`.

**Architecture:** atlas stays domain-free. `Workflow.Input` keeps only the four core fields plus a generic `Map<String,Object> extensions` (populated by an `else` branch exactly like `Workflow`'s own top-level extensions), exposing `getExtension`/`getExtensions`. A new `WorkflowInput` in `platform-configuration` wraps `Workflow.Input` and gives the extension keys domain meaning via typed accessors (`getComponentInputReference()`, `getObjectName()`, `isInternalOnly()`), with `ComponentInputReference` nested inside it. The EE-embedded consumers switch from `Workflow.Input` typed fields to `WorkflowInput`.

**Tech Stack:** Java 25, Spring Boot, JUnit 5, MapStruct (embedded REST mapper), `ObjectMapperSetupExtension` for `MapUtils`-backed tests.

---

## File Structure

**Atlas (made domain-free):**
- `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/constant/WorkflowConstants.java` — remove domain keys.
- `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/domain/Workflow.java` — `Input` record gains generic `extensions`; `ComponentInputReference` removed; parsing loop dumps unknown keys into `extensions`.
- `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowInputParsingTest.java` — rewrite around extensions.
- `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowTest.java` — rewrite around extensions.

**Platform (new domain meaning):**
- `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/constant/WorkflowExtConstants.java` — add `GROUP_NAME`, `INTERNAL_ONLY`, `OBJECT_NAME`.
- `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/domain/WorkflowInput.java` — **new**, wraps `Workflow.Input`, nested `ComponentInputReference`.
- `server/libs/platform/platform-configuration/platform-configuration-api/src/test/java/com/bytechef/platform/configuration/domain/WorkflowInputTest.java` — **new**.

**EE consumers (switch to `WorkflowInput`):**
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImpl.java`
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeImpl.java`
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapper.java`
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/IntegrationInstanceConfigurationWorkflowDTO.java` — javadoc only.
- Tests: `ConnectedUserIntegrationMapperTest.java`, `EmbeddedWorkflowInputOptionFacadeImplTest.java` (re-enable).

---

## Task 1: Atlas — make `Input` carry generic extensions

**Files:**
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/constant/WorkflowConstants.java`
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/domain/Workflow.java`

- [ ] **Step 1: Remove domain keys from `WorkflowConstants`**

Delete the five fields `COMPONENT_NAME`, `COMPONENT_VERSION`, `GROUP_NAME`, `INTERNAL_ONLY`, `OBJECT_NAME`, and restore `WORKFLOW_DEFINITION_CONSTANTS` to its pre-commit value:

```java
    public static final List<String> WORKFLOW_DEFINITION_CONSTANTS = List.of(
        DEFAULT, DESCRIPTION, FINALIZE, INPUTS, LABEL, METADATA, NAME, NODE, OUTPUTS, PARAMETERS, POST, PRE,
        MAX_RETRIES, REQUIRED, TASKS, TIMEOUT, TYPE, VALUE);
```

- [ ] **Step 2: Replace the `Input` record and delete `ComponentInputReference`**

Replace the whole `Input` record and the `ComponentInputReference` record (Workflow.java ~413–451) with:

```java
    public record Input(
        String name, String label, String type, boolean required, Map<String, Object> extensions)
        implements Serializable {

        public Input(String name, String label, String type, boolean required) {
            this(name, label, type, required, Map.of());
        }

        public <T> T getExtension(String name, Class<T> elementType, T defaultValue) {
            return MapUtils.get(extensions, name, elementType, defaultValue);
        }

        public <T> List<T> getExtensions(String name, Class<T> elementType, List<T> defaultValue) {
            return MapUtils.getList(extensions, name, elementType, defaultValue);
        }
    }
```

(`MapUtils`, `Map`, `List` are already imported in `Workflow.java`.)

- [ ] **Step 3: Rewrite the input-parsing branch to collect unknown keys**

Replace the `INPUTS` branch body (Workflow.java ~165–183) with:

```java
            } else if (WorkflowConstants.INPUTS.equals(entry.getKey())) {
                this.inputs = CollectionUtils.map(
                    MapUtils.getList(sourceMap, WorkflowConstants.INPUTS, Map.class, Collections.emptyList()),
                    map -> {
                        Map<String, Object> extensions = new HashMap<>();

                        for (Object entryObject : map.entrySet()) {
                            Map.Entry<?, ?> inputEntry = (Map.Entry<?, ?>) entryObject;

                            String key = String.valueOf(inputEntry.getKey());

                            if (!WorkflowConstants.NAME.equals(key) && !WorkflowConstants.LABEL.equals(key)
                                && !WorkflowConstants.TYPE.equals(key) && !WorkflowConstants.REQUIRED.equals(key)) {

                                extensions.put(key, inputEntry.getValue());
                            }
                        }

                        return new Input(
                            MapUtils.getRequiredString(map, WorkflowConstants.NAME),
                            MapUtils.getString(map, WorkflowConstants.LABEL),
                            MapUtils.getString(map, WorkflowConstants.TYPE, "string"),
                            MapUtils.getBoolean(map, WorkflowConstants.REQUIRED, false),
                            extensions);
                    });
```

(`HashMap` is already imported.)

- [ ] **Step 4: Compile atlas-configuration-api**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/constant/WorkflowConstants.java \
        server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/domain/Workflow.java
git commit -m "0_732 Make atlas Workflow.Input carry generic extensions"
```

---

## Task 2: Atlas — rewrite input parsing tests around extensions

**Files:**
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowInputParsingTest.java`
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowTest.java`

- [ ] **Step 1: Replace `WorkflowInputParsingTest` body**

```java
package com.bytechef.atlas.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.Input;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowInputParsingTest {

    @Test
    void testParseInputWithoutExtensions() {
        String definition =
            """
                {
                    "inputs": [
                        {
                            "name": "email",
                            "label": "Email",
                            "type": "string",
                            "required": true
                        }
                    ]
                }
                """;

        Workflow workflow = new Workflow("workflow1", definition, Format.JSON);

        Input input = workflow.getInputs()
            .getFirst();

        assertEquals("email", input.name());
        assertTrue(input.extensions()
            .isEmpty());
    }

    @Test
    void testParseInputCapturesUnknownKeysAsExtensions() {
        String definition =
            """
                {
                    "inputs": [
                        {
                            "name": "dateRange",
                            "label": "Date Range",
                            "componentName": "googleSheets",
                            "componentVersion": 2,
                            "groupName": "sheetSelection"
                        }
                    ]
                }
                """;

        Workflow workflow = new Workflow("workflow1", definition, Format.JSON);

        List<Input> inputs = workflow.getInputs();

        assertEquals(1, inputs.size());

        Input input = inputs.getFirst();

        assertEquals("dateRange", input.name());
        assertEquals("Date Range", input.label());
        assertEquals("googleSheets", input.getExtension("componentName", String.class, null));
        assertEquals(2, input.getExtension("componentVersion", Integer.class, null));
        assertEquals("sheetSelection", input.getExtension("groupName", String.class, null));
    }
}
```

- [ ] **Step 2: Replace `WorkflowTest` body**

```java
package com.bytechef.atlas.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTest {

    @Test
    void testInputCapturesObjectNameExtension() {
        String definition =
            """
                {
                    "inputs": [
                        {
                            "name": "contactMapping",
                            "label": "Contact Mapping",
                            "type": "field_mapping",
                            "objectName": "Contacts"
                        }
                    ]
                }
                """;

        Workflow workflow = new Workflow("1", definition, Format.JSON);

        Workflow.Input input = workflow.getInputs()
            .getFirst();

        assertEquals("Contacts", input.getExtension("objectName", String.class, null));
        assertEquals("field_mapping", input.type());
    }

    @Test
    void testPlainInputHasNoExtensions() {
        String definition =
            """
                {
                    "inputs": [
                        {
                            "name": "x",
                            "type": "string"
                        }
                    ]
                }
                """;

        Workflow workflow = new Workflow("1", definition, Format.JSON);

        assertTrue(workflow.getInputs()
            .getFirst()
            .extensions()
            .isEmpty());
        assertNull(workflow.getInputs()
            .getFirst()
            .getExtension("objectName", String.class, null));
    }

    @Test
    void testInputCapturesInternalOnlyExtension() {
        Workflow workflow = new Workflow(
            "1",
            """
                {"inputs": [{"name": "apiKey", "type": "string", "internalOnly": true}]}
                """,
            Format.JSON);

        Workflow.Input input = workflow.getInputs()
            .getFirst();

        assertTrue(input.getExtension("internalOnly", Boolean.class, false));
    }

    @Test
    void testInputInternalOnlyExtensionAbsentDefaultsFalse() {
        Workflow workflow = new Workflow(
            "1", """
                {"inputs": [{"name": "channel", "type": "string"}]}
                """, Format.JSON);

        Workflow.Input input = workflow.getInputs()
            .getFirst();

        assertFalse(input.getExtension("internalOnly", Boolean.class, false));
    }
}
```

- [ ] **Step 3: Run atlas tests**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests "com.bytechef.atlas.configuration.domain.WorkflowInputParsingTest" --tests "com.bytechef.atlas.configuration.domain.WorkflowTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowInputParsingTest.java \
        server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowTest.java
git commit -m "0_732 Rewrite atlas input parsing tests around generic extensions"
```

---

## Task 3: Platform — `WorkflowInput` + constants

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/constant/WorkflowExtConstants.java`
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/domain/WorkflowInput.java`

- [ ] **Step 1: Add the three keys to `WorkflowExtConstants`**

Add (alphabetical order; `COMPONENT_NAME`/`COMPONENT_VERSION` already present):

```java
    public static final String GROUP_NAME = "groupName";
    public static final String INTERNAL_ONLY = "internalOnly";
    public static final String OBJECT_NAME = "objectName";
```

- [ ] **Step 2: Create `WorkflowInput`**

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

package com.bytechef.platform.configuration.domain;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * Gives domain meaning to the generic per-input {@link Workflow.Input#extensions()} map, the same way
 * {@link WorkflowTrigger} gives meaning to the generic extensions of {@link Workflow}.
 *
 * @author Ivica Cardic
 */
public class WorkflowInput {

    private final Workflow.Input input;

    public WorkflowInput(Workflow.Input input) {
        Assert.notNull(input, "'input' must not be null");

        this.input = input;
    }

    public static List<WorkflowInput> of(Workflow workflow) {
        return CollectionUtils.map(workflow.getInputs(), WorkflowInput::new);
    }

    public String getName() {
        return input.name();
    }

    public String getLabel() {
        return input.label();
    }

    public String getType() {
        return input.type();
    }

    public boolean isRequired() {
        return input.required();
    }

    public <T> T getExtension(String name, Class<T> elementType, T defaultValue) {
        return input.getExtension(name, elementType, defaultValue);
    }

    public Map<String, ?> getExtensions() {
        return input.extensions();
    }

    /**
     * Resolves the component-input-group reference carried in the input extensions, or {@code null} when the input does
     * not reference a component.
     */
    public ComponentInputReference getComponentInputReference() {
        String componentName = getExtension(WorkflowExtConstants.COMPONENT_NAME, String.class, null);

        if (componentName == null) {
            return null;
        }

        return new ComponentInputReference(
            componentName,
            getExtension(WorkflowExtConstants.COMPONENT_VERSION, Integer.class, null),
            getExtension(WorkflowExtConstants.GROUP_NAME, String.class, null));
    }

    public String getObjectName() {
        return getExtension(WorkflowExtConstants.OBJECT_NAME, String.class, null);
    }

    public boolean isInternalOnly() {
        return getExtension(WorkflowExtConstants.INTERNAL_ONLY, Boolean.class, false);
    }

    /**
     * An all-or-nothing reference from a workflow input to a component-defined input group. Every component-defined
     * input is a property group (a lone property is a group with one property), so a reference always targets a group.
     */
    public record ComponentInputReference(String componentName, Integer componentVersion, String groupName)
        implements Serializable {

        public ComponentInputReference {
            Assert.notNull(componentName, "componentName is required");
            Assert.notNull(componentVersion, "componentVersion is required");
            Assert.notNull(groupName, "groupName is required");
        }
    }
}
```

- [ ] **Step 3: Compile platform-configuration-api**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/constant/WorkflowExtConstants.java \
        server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/domain/WorkflowInput.java
git commit -m "0_732 Introduce WorkflowInput input-extension concept"
```

---

## Task 4: Platform — `WorkflowInputTest`

**Files:**
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/test/java/com/bytechef/platform/configuration/domain/WorkflowInputTest.java`

- [ ] **Step 1: Write the test**

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

package com.bytechef.platform.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.domain.WorkflowInput.ComponentInputReference;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowInputTest {

    @Test
    void testPlainInputHasNoComponentReference() {
        WorkflowInput workflowInput = new WorkflowInput(new Workflow.Input("email", "Email", "string", true));

        assertEquals("email", workflowInput.getName());
        assertNull(workflowInput.getComponentInputReference());
        assertNull(workflowInput.getObjectName());
        assertFalse(workflowInput.isInternalOnly());
    }

    @Test
    void testComponentInputReferenceReadFromExtensions() {
        WorkflowInput workflowInput = new WorkflowInput(
            new Workflow.Input(
                "dateRange", "Date Range", "string", false,
                Map.of("componentName", "googleSheets", "componentVersion", 2, "groupName", "sheetSelection")));

        ComponentInputReference componentInputReference = workflowInput.getComponentInputReference();

        assertEquals("googleSheets", componentInputReference.componentName());
        assertEquals(2, componentInputReference.componentVersion());
        assertEquals("sheetSelection", componentInputReference.groupName());
    }

    @Test
    void testObjectNameAndInternalOnlyReadFromExtensions() {
        WorkflowInput workflowInput = new WorkflowInput(
            new Workflow.Input(
                "contactMapping", "Contact Mapping", "field_mapping", false,
                Map.of("objectName", "Contacts", "internalOnly", true)));

        assertEquals("Contacts", workflowInput.getObjectName());
        assertTrue(workflowInput.isInternalOnly());
    }

    @Test
    void testComponentInputReferenceRejectsMissingGroupName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ComponentInputReference("googleSheets", 2, null));
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:test --tests "com.bytechef.platform.configuration.domain.WorkflowInputTest"`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-configuration/platform-configuration-api/src/test/java/com/bytechef/platform/configuration/domain/WorkflowInputTest.java
git commit -m "0_732 Add WorkflowInput unit tests"
```

---

## Task 5: EE — switch consumers to `WorkflowInput`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapper.java`
- Modify (javadoc): `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/IntegrationInstanceConfigurationWorkflowDTO.java`

- [ ] **Step 1: Verify module deps see `platform-configuration-api`**

Run: `grep -rn "platform-configuration-api\|platform-configuration:platform-configuration-api" server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/build.gradle.kts`
Expected: each module already depends (directly or transitively) on `platform-configuration-api`. If a module does not, add `implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))` to its `build.gradle.kts` before proceeding.

- [ ] **Step 2: `EmbeddedWorkflowInputOptionFacadeImpl` — use `WorkflowInput`**

Replace the input lookup block (lines ~70–84) with:

```java
        WorkflowInput workflowInput = WorkflowInput.of(workflow)
            .stream()
            .filter(currentInput -> inputName.equals(currentInput.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown workflow input: " + inputName));

        WorkflowInput.ComponentInputReference componentReference = workflowInput.getComponentInputReference();

        if (componentReference == null) {
            throw new IllegalArgumentException("Workflow input does not reference a component: " + inputName);
        }

        String componentName = componentReference.componentName();
        int componentVersion =
            componentReference.componentVersion() == null ? 1 : componentReference.componentVersion();
```

Add import `import com.bytechef.platform.configuration.domain.WorkflowInput;` and remove the now-unused `Workflow.Input` usage (keep the `Workflow` import — still used for `workflowService.getWorkflow`).

- [ ] **Step 3: `ConnectedUserIntegrationFacadeImpl` — use `WorkflowInput`**

Replace the loop in `resolveComponentInputGroups` (lines ~350–364) with:

```java
        for (WorkflowInput workflowInput : WorkflowInput.of(workflow)) {
            WorkflowInput.ComponentInputReference componentReference = workflowInput.getComponentInputReference();

            if (componentReference == null) {
                continue;
            }

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                componentReference.componentName(), componentReference.componentVersion());

            componentDefinition.getInputs()
                .stream()
                .filter(propertyGroup -> Objects.equals(propertyGroup.getName(), componentReference.groupName()))
                .findFirst()
                .ifPresent(propertyGroup -> componentInputGroups.put(workflowInput.getName(), propertyGroup));
        }
```

Update the javadoc reference on the method from `{@link Workflow.ComponentInputReference}` to `{@link WorkflowInput.ComponentInputReference}`. Add import `import com.bytechef.platform.configuration.domain.WorkflowInput;` (keep `Workflow` import — still used for `workflowDTO.workflow()`).

- [ ] **Step 4: `ConnectedUserIntegrationMapper` — map from `WorkflowInput`**

Replace the list mapping (lines ~164–178) to iterate `WorkflowInput.of(workflow)` and key groups by `getName()`:

```java
            return WorkflowInput.of(workflow)
                .stream()
                .map(input -> {
                    InputModel inputModel = map(input);

                    PropertyGroup propertyGroup = resolvedGroups.get(input.getName());

                    if (propertyGroup != null && inputModel.getComponentReference() != null) {
                        inputModel.getComponentReference()
                            .group(map(propertyGroup));
                    }

                    return inputModel;
                })
                .toList();
```

Replace the `map(Workflow.Input input)` default method (lines ~181–200) with:

```java
        default InputModel map(WorkflowInput input) {
            InputModel inputModel = new InputModel()
                .internalOnly(input.isInternalOnly())
                .label(input.getLabel())
                .name(input.getName())
                .objectName(input.getObjectName())
                .required(input.isRequired())
                .type(InputTypeModel.valueOf(StringUtils.upperCase(input.getType())));

            WorkflowInput.ComponentInputReference componentReference = input.getComponentInputReference();

            if (componentReference != null) {
                inputModel.componentReference(
                    new ComponentInputReferenceModel(
                        componentReference.componentName(), componentReference.componentVersion(),
                        componentReference.groupName()));
            }

            return inputModel;
        }
```

Update imports: add `import com.bytechef.platform.configuration.domain.WorkflowInput;`. The `import com.bytechef.atlas.configuration.domain.Workflow;` stays (used by `mapInputs(Workflow workflow, ...)` signature). Remove any now-unused `Workflow.ComponentInputReference` references — there is no separate import line for it (it was used as a qualified inner type).

- [ ] **Step 5: `IntegrationInstanceConfigurationWorkflowDTO` — fix javadoc**

Change the javadoc on `withComponentInputGroups` from `{@link Workflow.Input#componentReference()}` to:
`{@link com.bytechef.platform.configuration.domain.WorkflowInput#getComponentInputReference()}`.

- [ ] **Step 6: Compile the embedded-configuration modules**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration
git commit -m "0_732 Switch embedded-configuration consumers to WorkflowInput"
```

---

## Task 6: EE — update/re-enable consumer tests

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/test/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapperTest.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImplTest.java`

- [ ] **Step 1: `ConnectedUserIntegrationMapperTest` — build `WorkflowInput`**

For the plain-input test (`mapper.map(input)` expecting null component reference), build:

```java
        WorkflowInput input = new WorkflowInput(new Workflow.Input("x", "X", "string", false));
```

For `testComponentReferenceInputMapsFlatReference`, replace the input construction with:

```java
        WorkflowInput input = new WorkflowInput(
            new Workflow.Input(
                "channel", "Channel", "string", false,
                Map.of("componentName", "slack", "componentVersion", 1, "groupName", "channel")));
```

Update imports: add `import com.bytechef.platform.configuration.domain.WorkflowInput;` and `import java.util.Map;`. Remove the `ComponentInputReferenceModel` import only if it becomes unused (it is still used to read `model.getComponentReference()` — keep it). Keep `import com.bytechef.atlas.configuration.domain.Workflow;`.

- [ ] **Step 2: `EmbeddedWorkflowInputOptionFacadeImplTest` — re-enable and fix inputs**

Delete the two-line `// Disabled pending ...` comment and the `@Disabled(...)` annotation (and the now-unused `import org.junit.jupiter.api.Disabled;`).

Replace every occurrence of:

```java
            "channel", "Channel", "STRING", true, new Workflow.ComponentInputReference("slack", 1, "channel"));
```

with:

```java
            "channel", "Channel", "STRING", true,
            Map.of("componentName", "slack", "componentVersion", 1, "groupName", "channel"));
```

and the `googleSheets`/`sheetSelection` occurrence similarly:

```java
            Map.of("componentName", "googleSheets", "componentVersion", 1, "groupName", "sheetSelection"));
```

Ensure `import java.util.Map;` is present (it already is).

- [ ] **Step 3: Run the EE tests**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test --tests "com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.ConnectedUserIntegrationMapperTest" :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.facade.EmbeddedWorkflowInputOptionFacadeImplTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration
git commit -m "0_732 Update embedded-configuration tests for WorkflowInput"
```

---

## Task 7: Full verification

- [ ] **Step 1: Spotless + compile + check the touched modules**

Run:
```bash
./gradlew spotlessApply
./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:check \
          :server:libs:platform:platform-configuration:platform-configuration-api:check \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Grep for stragglers**

Run: `grep -rn "Workflow.ComponentInputReference\|ComponentInputReference\b\|\.componentReference()\|\.objectName()\|\.internalOnly()" server --include=*.java | grep -v "/generated/" | grep -v "Api.java:" | grep -v "platform/configuration/domain/WorkflowInput\|WorkflowInputModel\|ComponentInputReferenceModel"`
Expected: no remaining references to the removed atlas `Workflow.ComponentInputReference` / `Input.componentReference()/objectName()/internalOnly()` outside the new `WorkflowInput`, the generated REST models, and intentional usages.

- [ ] **Step 3: Final commit (if spotless reformatted anything)**

```bash
git add -A
git commit -m "0_732 spotlessApply for WorkflowInput extension refactor"
```

---

## Self-Review Notes

- **Spec coverage:** atlas revert (Task 1), atlas tests (Task 2), `WorkflowInput` + constants (Task 3), `WorkflowInput` tests (Task 4), EE consumers (Task 5), EE tests (Task 6), verification (Task 7). All consumers found via grep are covered.
- **Type consistency:** `ComponentInputReference` is `WorkflowInput.ComponentInputReference` everywhere post-refactor; accessor names are `getComponentInputReference()`, `getObjectName()`, `isInternalOnly()`, `getName()/getLabel()/getType()/isRequired()`; atlas `Input` exposes `extensions()`, `getExtension(...)`, `getExtensions(...)`.
- **REST contract unchanged:** generated `InputModel` / `ComponentInputReferenceModel` are untouched; only the mapper's source type changes.
