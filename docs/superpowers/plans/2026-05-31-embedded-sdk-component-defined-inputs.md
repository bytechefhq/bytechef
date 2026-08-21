# Component-defined Inputs in the Embedded SDK ConnectDialog — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the embedded SDK `ConnectDialog` render component-defined workflow inputs — single component properties and compound property groups, with connection-backed dynamic option dropdowns and `optionsLookupDependsOn` dependencies — by extending the embedded public REST surface and reworking the hand-written SDK dialog.

**Architecture:** Server side: extend the public `Input` schema with the 4 component-reference fields + an embedded resolved `property`/`group` definition (resolved via `ComponentDefinitionService`), and add a new public options endpoint that resolves dynamic options against the integration instance's connection by delegating to `ActionDefinitionFacade`/`TriggerDefinitionFacade.executeOptions(...)`. SDK side (hand-written, no codegen): extend `types.ts`, add an options-fetch data flow to the `useConnectDialog` hook, and rework `ConnectDialog.tsx`'s input rendering to support dynamic selects, groups, and dependencies.

**Tech Stack:** Java 25 / Spring Boot REST + OpenAPI generator (server `spring` generator only — the SDK is NOT generated); React 19 + TypeScript + Vitest + jsdom (hand-written SDK lib).

**Related spec:** `docs/superpowers/specs/2026-05-31-embedded-sdk-component-defined-inputs-design.md`

---

## Ground Truth (verified during planning)

### Server
- **Public OpenAPI spec:** `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`.
  - `Input:` schema at ~`:1658` (fields: `label`, `name`, `required`, `type`→`InputType`); `InputType` enum right after.
  - `IntegrationWorkflow:` schema at ~`:1638` (its `inputs` is `array` of `#/components/schemas/Input`).
  - `GET /integrations/{id}` at ~`:627`; `PUT /integration-instances/{id}/workflows/{workflowUuid}` at ~`:506`; `{externalUserId}` variant at ~`:1201`.
  - Build: `build.gradle.kts` `generateOpenAPISpring` GenerateTask — `generatorName="spring"`, `interfaceOnly=true`, `modelNameSuffix="Model"`, `modelPackage="com.bytechef.ee.embedded.configuration.public_.web.rest.model"`, `outputDir="$projectDir/generated"`, `inputSpec="$projectDir/openapi.yaml"`. **Generates server models only — NOT the SDK.**
- **`IntegrationApiController`** (`.../public_/web/rest/IntegrationApiController.java`): constructor injects (among others) `WorkflowService workflowService`, `ConversionService conversionService`. The input mapping is in `populateMcpData(...)` at ~`:285`:
  ```java
  List<InputModel> inputModels = workflow.getInputs()
      .stream()
      .map(input -> new InputModel()
          .label(input.label())
          .name(input.name())
          .required(input.required())
          .type(InputTypeModel.fromValue(input.type())))
      .toList();
  return new IntegrationWorkflowModel()
      .description(workflow.getDescription())
      .inputs(inputModels)
      .label(workflow.getLabel())
      .workflowUuid(integrationWorkflow.getUuidAsString());
  ```
  `workflow` is `com.bytechef.atlas.configuration.domain.Workflow` via `workflowService.getWorkflow(workflowId)`. Its `getInputs()` returns the 8-field `Workflow.Input` record (carries `componentName/componentVersion/propertyName/groupName`). `workflow.getTask(name)` and `workflow.getTasks(boolean)` exist; `WorkflowTask.getType()` returns `"componentName/vN/operation"`.
- **`IntegrationInstanceWorkflowApiController`** (`.../public_/web/rest/`): currently injects only `ConnectedUserIntegrationInstanceFacade`; has `updateFrontendIntegrationInstanceWorkflow` + `{externalUserId}` `updateIntegrationInstanceWorkflow`. New options endpoint methods land here (both `@CrossOrigin` frontend + externalUserId variants, matching the existing pair pattern).
- **`ComponentDefinitionService`** (platform): `ComponentDefinition getComponentDefinition(String name, @Nullable Integer version)`; domain `ComponentDefinition.getProperties(): List<? extends Property>` and `getPropertyGroups(): List<PropertyGroup>`. Already injected by the embedded internal `ComponentDefinitionApiController`, proving reachability.
- **Option execution facades** (`com.bytechef.platform.component.facade`):
  - `ActionDefinitionFacade.executeOptions(String componentName, int componentVersion, String actionName, String propertyName, Map<String,?> inputParameters, List<String> lookupDependsOnPaths, String searchText, @Nullable Long connectionId): List<Option>`
  - `TriggerDefinitionFacade.executeOptions(String componentName, int componentVersion, String triggerName, String propertyName, Map<String,?> inputParameters, List<String> lookupDependsOnPaths, String searchText, @Nullable Long connectionId): List<Option>`
  - `Option` = `com.bytechef.platform.component.domain.Option` (has label/value).
- **Integration instance connection:** `IntegrationInstanceService.getIntegrationInstance(long id)` → `IntegrationInstance.getConnectionId(): long`.
- **No existing embedded facade calls executeOptions** — this is new wiring. Pattern to mirror:
  `ConnectedUserIntegrationInstanceFacadeImpl` (embedded-configuration-service) resolves instance → connection → delegates.
- **Domain `Property`** (`com.bytechef.platform.component.domain`) is a polymorphic hierarchy; `ValueProperty` carries `controlType`, `label`, `options`, `optionsDataSource`. `OptionsDataSource` carries `optionsLookupDependsOn`. The mapping in Task 2 reads these via instanceof/getters — confirm exact getters when implementing (read `domain/ValueProperty.java`, `domain/OptionsDataSource.java`).

### SDK (`sdks/frontend/embedded/library/react`)
- **Hand-written**, no codegen. Vitest 4 + jsdom configured (`vite.config.ts` `test` block; `src/test/setup.ts`). Existing tests:
  `src/components/connect-dialog/index.test.tsx`, `ConnectDialog.test.tsx`. Build: `tsc && vite build`. Check scripts:
  `npm run lint`, `npm run format`, `npm test`.
- **`types.ts`:** `WorkflowInputType {name, label, type:'string'|'number'|'boolean'|'object'|'array', required?, defaultValue?, value?}`;
  `PropertyType {name, label, type, required?, options?: string[], placeholder?}`;
  `MergedWorkflowType {description?, inputs?: WorkflowInputType[], enabled?, label?, workflowUuid}`;
  `IntegrationWorkflowType {enabled?, inputs?: WorkflowInputType[], label?, workflowUuid}`;
  `IntegrationInstanceWorkflowType {enabled?, inputs?: Record<string,unknown>, workflowUuid}`.
- **`index.tsx`:** `createApiClient(baseUrl, environment, jwtToken)` returns `{fetch<T>(endpoint, {method,body,headers})}`. `mergedWorkflows` useMemo merges integration workflows + instance values. `handleWorkflowInputChange(workflowUuid, inputName, value)` updates `inputOverrides`, 600ms-debounced PUT to `/api/embedded/v1/integration-instances/${instanceId}/workflows/${workflowUuid}` with `{inputs: mergedInputs}`. `handleMcpWorkflowInputChange` is the MCP twin (`mcp-workflows`). Hook holds `integration`, `currentIntegrationInstanceId` (+ refs), `inputOverrides`, etc. Returns `{openDialog, closeDialog}` — BUT `ConnectDialog` receives handlers via props from wherever it's rendered; trace how `mergedWorkflows`/handlers reach `<ConnectDialog>` (the hook renders it via a portal `createRoot` — read how props are passed and extend that pass-through).
- **`ConnectDialog.tsx`:** `DialogInputField({label,name,options?,placeholder?,onChange?,required?,field?,error?})` renders a `<select>` when `options` present else `<input>`. `DialogWorkflowsContainer` + `DialogToolsContainer` map `inputs` → `DialogInputField`, threading `handleWorkflowInputChange`/`handleMcpWorkflowInputChange`.
- **Styles** (`styles.module.css`): `.dialogInputField`, `.requiredIndicator`, `.inputError`, `.workflowInputsContainer`, `.workflowsList`, `.noInputsMessage`. New group/dynamic markup reuses these.
- **No dynamic option fetching exists anywhere** in the SDK today.

## File Structure

**Server (slices 1–2)**
- `embedded-configuration-public-rest/openapi.yaml` — extend `Input`; add `ComponentProperty`, `ComponentPropertyGroup`, `Option`, `WorkflowInputOptionsRequest` schemas; add options endpoint paths (+`{externalUserId}`). Regenerated models follow.
- `IntegrationApiController.java` — inject `ComponentDefinitionService`; resolve + attach `property`/`group` per reference input.
- new `EmbeddedWorkflowInputOptionFacade` (interface in `embedded-configuration-api`, impl in `embedded-configuration-service`) — resolve instance connection + delegate to action/trigger options execution.
- `IntegrationInstanceWorkflowApiController.java` — inject the new facade; add the options endpoint method(s).

**SDK (slices 3–4)**
- `types.ts` — extend `WorkflowInputType`; add `ComponentPropertyType`, `ComponentPropertyGroupType`, `OptionType`.
- `index.tsx` — options-fetch apiClient call + cache + dependency invalidation; nested group values in the PUT payload.
- `ConnectDialog.tsx` — dynamic select, group renderer, dependency wiring, dangling fallback.

**Tests (slice 5)** — server facade + controller/mapping tests; SDK Vitest tests.

## Verify-First Checklist (before the tasks)

1. **Domain Property getters.** Read `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ValueProperty.java` and `OptionsDataSource.java` to confirm the exact getters for `controlType`, `label`, `options`, and `optionsDataSource`/`optionsLookupDependsOn`. Task 2's mapping uses these.
2. **Action vs trigger + operation lookup.** Confirm how to decide whether a referenced input's component is used by a task (action) or a trigger, and the operation name: parse `WorkflowTask.getType()` / trigger type `"name/vN/operation"`. Task 4 (facade) needs the operation name to call `executeOptions`. Decide: derive from the workflow node that uses `componentName` (first match), or — simpler — store/pass the action/trigger name. Pin during Task 4.
3. **How `ConnectDialog` receives handlers/data.** `index.tsx` returns only `{openDialog, closeDialog}` and renders `ConnectDialog` via a portal. Read the render call to confirm exactly how `mergedWorkflows`, `handleWorkflowInputChange`, etc. are passed, so SDK tasks extend the real pass-through (not an assumed prop API).
4. **SDK build/test commands** run from `sdks/frontend/embedded/library/react` (its own `node_modules`). Confirm `npm install` there first if `node_modules` absent.

---

## Task 1: Public OpenAPI — extend `Input` + add component-property schemas

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`

- [ ] **Step 1: Read the current `Input` + `IntegrationWorkflow` schemas**

Run: `sed -n '1638,1690p' server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`
Expected: the `IntegrationWorkflow` and `Input`/`InputType` schemas.

- [ ] **Step 2: Extend the `Input` schema with reference fields + resolved defs**

Add to `Input.properties` (keep existing label/name/required/type; do not change `required` list):
```yaml
        componentName:
          description: "The component a referenced input property belongs to."
          type: "string"
        componentVersion:
          description: "The component version a referenced input property belongs to."
          type: "integer"
          format: "int32"
        propertyName:
          description: "The referenced component input property name."
          type: "string"
        groupName:
          description: "The referenced component property group name."
          type: "string"
        property:
          $ref: "#/components/schemas/ComponentProperty"
        group:
          $ref: "#/components/schemas/ComponentPropertyGroup"
```

- [ ] **Step 3: Add the minimal `ComponentProperty` / `ComponentPropertyGroup` / `Option` schemas**

Add as top-level schemas under `components.schemas` (after `InputType`):
```yaml
    Option:
      type: "object"
      required:
        - "label"
        - "value"
      properties:
        label:
          type: "string"
        value:
          type: "string"
    ComponentProperty:
      description: "A resolved component input property the SDK renders."
      type: "object"
      required:
        - "name"
        - "type"
      properties:
        name:
          type: "string"
        label:
          type: "string"
        type:
          $ref: "#/components/schemas/InputType"
        controlType:
          type: "string"
        required:
          type: "boolean"
        options:
          type: "array"
          items:
            $ref: "#/components/schemas/Option"
        dynamicOptions:
          description: "True when options must be fetched from the options endpoint."
          type: "boolean"
        optionsLookupDependsOn:
          type: "array"
          items:
            type: "string"
    ComponentPropertyGroup:
      description: "A resolved component property group rendered as one compound input."
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
            $ref: "#/components/schemas/ComponentProperty"
```

- [ ] **Step 4: Commit (regeneration in Task 3)**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/0_732-embedded-inputs
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml
git commit -m "732 Add component-reference fields and property schemas to embedded public Input

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Public OpenAPI — add the options endpoint + request schema

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`

- [ ] **Step 1: Add the request schema**

Under `components.schemas`:
```yaml
    WorkflowInputOptionsRequest:
      type: "object"
      required:
        - "propertyName"
      properties:
        propertyName:
          description: "The component property whose options to resolve."
          type: "string"
        lookupDependsOnValues:
          description: "Current values of the properties this lookup depends on."
          type: "object"
          additionalProperties: true
        searchText:
          type: "string"
```

- [ ] **Step 2: Add the two endpoint paths**

Add (mirroring the existing frontend + `{externalUserId}` pair for workflow update). Frontend variant:
```yaml
  /integration-instances/{id}/workflows/{workflowUuid}/options:
    post:
      description: "Resolve dynamic options for a component-defined workflow input."
      summary: "Get integration instance workflow input options"
      tags:
        - "integration-instance-workflow"
      operationId: "getFrontendIntegrationInstanceWorkflowInputOptions"
      parameters:
        - name: "id"
          in: "path"
          required: true
          schema:
            type: "integer"
            format: "int64"
        - name: "workflowUuid"
          in: "path"
          required: true
          schema:
            type: "string"
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/WorkflowInputOptionsRequest"
        required: true
      responses:
        "200":
          description: "The resolved options."
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
And the externalUserId variant:
```yaml
  /{externalUserId}/integration-instances/{id}/workflows/{workflowUuid}/options:
    post:
      description: "Resolve dynamic options for a component-defined workflow input."
      summary: "Get integration instance workflow input options"
      tags:
        - "integration-instance-workflow"
      operationId: "getIntegrationInstanceWorkflowInputOptions"
      parameters:
        - name: "externalUserId"
          in: "path"
          required: true
          schema:
            type: "string"
        - name: "id"
          in: "path"
          required: true
          schema:
            type: "integer"
            format: "int64"
        - name: "workflowUuid"
          in: "path"
          required: true
          schema:
            type: "string"
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/WorkflowInputOptionsRequest"
        required: true
      responses:
        "200":
          description: "The resolved options."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/Option"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
      security:
        - bearerAuth: [ ]
```

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml
git commit -m "732 Add embedded public workflow-input options endpoint to OpenAPI

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Regenerate public REST models + compile

**Files:**
- Auto-generated under `embedded-configuration-public-rest/generated/...model/` (`InputModel`, new `ComponentPropertyModel`, `ComponentPropertyGroupModel`, `OptionModel`, `WorkflowInputOptionsRequestModel`, the new `IntegrationInstanceWorkflowApi` options methods).

- [ ] **Step 1: Regenerate**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:generateOpenAPISpring`
Expected: BUILD SUCCESSFUL; new model classes + the `IntegrationInstanceWorkflowApi` interface gains
`getFrontendIntegrationInstanceWorkflowInputOptions` / `getIntegrationInstanceWorkflowInputOptions`, and
`InputModel` gains the new fields/`property`/`group`.

- [ ] **Step 2: Compile the public-rest module (will FAIL — controller must implement new methods)**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava`
Expected: FAIL — `IntegrationInstanceWorkflowApiController` does not implement the 2 new interface methods, and
`IntegrationApiController` does not yet set `property`/`group`. This confirms the interface changed. Proceed to
Tasks 4–6 before committing generated output (commit generated code together with the controller changes in Task 6).

---

## Task 4: Embedded options facade

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacade.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImpl.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImplTest.java`

- [ ] **Step 1: Verify-first — Property/OptionsDataSource getters + node lookup**

Run: `sed -n '1,80p' server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ValueProperty.java`
Then: `sed -n '1,80p' server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/OptionsDataSource.java`
Confirm getters for `controlType`, `options`, `optionsDataSource`, and `optionsLookupDependsOn`. Also read
`Workflow.getTasks(false)` / `getTriggers()` shape to map `componentName` → node + operation.

- [ ] **Step 2: Write the interface**

```java
/*
 * Copyright 2025 ByteChef
 *
 * The ByteChef Enterprise license (the "Enterprise License"); you may not use this file except in compliance
 * with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.component.domain.Option;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedWorkflowInputOptionFacade {

    List<Option> getWorkflowInputOptions(
        long integrationInstanceId, String workflowUuid, String propertyName,
        Map<String, ?> lookupDependsOnValues, String searchText);
}
```
> Use the exact EE license header used by other files under `server/ee/` (copy from a sibling file in the same
> module) and the `@version ee` Javadoc tag (CLAUDE.md EE conventions).

- [ ] **Step 3: Write the failing test**

```java
/* EE license header + @version ee */
package com.bytechef.ee.embedded.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EmbeddedWorkflowInputOptionFacadeImplTest {

    @Test
    void testGetWorkflowInputOptionsDelegatesToActionFacadeWithInstanceConnection() {
        IntegrationInstanceService integrationInstanceService = Mockito.mock(IntegrationInstanceService.class);
        IntegrationWorkflowService integrationWorkflowService = Mockito.mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = Mockito.mock(WorkflowService.class);
        ActionDefinitionFacade actionDefinitionFacade = Mockito.mock(ActionDefinitionFacade.class);
        TriggerDefinitionFacade triggerDefinitionFacade = Mockito.mock(TriggerDefinitionFacade.class);

        IntegrationInstance integrationInstance = Mockito.mock(IntegrationInstance.class);
        when(integrationInstance.getConnectionId()).thenReturn(55L);
        when(integrationInstanceService.getIntegrationInstance(7L)).thenReturn(integrationInstance);

        when(integrationWorkflowService.getWorkflowId(7L, "wf-uuid")).thenReturn("wf-id");

        WorkflowTask workflowTask = Mockito.mock(WorkflowTask.class);
        when(workflowTask.getType()).thenReturn("slack/v1/sendMessage");
        Workflow workflow = Mockito.mock(Workflow.class);
        when(workflow.getTasks(false)).thenReturn(List.of(workflowTask));
        when(workflow.getTriggers()).thenReturn(List.of());
        when(workflowService.getWorkflow("wf-id")).thenReturn(workflow);

        Option option = new Option("General", "C123");
        when(actionDefinitionFacade.executeOptions(
            eq("slack"), anyInt(), eq("sendMessage"), eq("channelId"), any(), any(), any(), eq(55L)))
            .thenReturn(List.of(option));

        EmbeddedWorkflowInputOptionFacadeImpl facade = new EmbeddedWorkflowInputOptionFacadeImpl(
            actionDefinitionFacade, integrationInstanceService, integrationWorkflowService,
            triggerDefinitionFacade, workflowService);

        // The referenced property carries its component via the workflow input; here the test asserts the
        // action path. The facade derives component+operation from the node using the property's component.
        List<Option> options = facade.getWorkflowInputOptions(7L, "wf-uuid", "channelId", Map.of(), null);

        assertEquals(1, options.size());
        assertEquals("C123", options.getFirst().getValue());
    }
}
```
> This test pins the delegation contract. Adjust the exact mocks to the real lookup the impl uses (the impl must
> map `propertyName` → the component + action/trigger + operation that owns it). The KEY assertion: the instance's
> `connectionId` (55) is the `connectionId` passed to `executeOptions`. Refine mock arg matchers to match the impl
> once Step 4 fixes the lookup approach.

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*EmbeddedWorkflowInputOptionFacadeImplTest'`
Expected: FAIL (class not yet created).

- [ ] **Step 4: Write the implementation**

```java
/* EE license header + @version ee */
package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnEEVersion
public class EmbeddedWorkflowInputOptionFacadeImpl implements EmbeddedWorkflowInputOptionFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedWorkflowInputOptionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, IntegrationInstanceService integrationInstanceService,
        IntegrationWorkflowService integrationWorkflowService, TriggerDefinitionFacade triggerDefinitionFacade,
        WorkflowService workflowService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public List<Option> getWorkflowInputOptions(
        long integrationInstanceId, String workflowUuid, String propertyName, Map<String, ?> lookupDependsOnValues,
        String searchText) {

        IntegrationInstance integrationInstance =
            integrationInstanceService.getIntegrationInstance(integrationInstanceId);

        long connectionId = integrationInstance.getConnectionId();

        String workflowId = integrationWorkflowService.getWorkflowId(integrationInstanceId, workflowUuid);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Workflow.Input input = workflow.getInputs()
            .stream()
            .filter(currentInput -> propertyName.equals(currentInput.propertyName()) ||
                propertyName.equals(currentInput.name()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown input property: " + propertyName));

        String componentName = input.componentName();
        int componentVersion = input.componentVersion() == null ? 1 : input.componentVersion();
        String leafPropertyName = input.propertyName() == null ? propertyName : input.propertyName();

        List<String> lookupDependsOnPaths = List.copyOf(lookupDependsOnValues.keySet());
        Map<String, ?> inputParameters = lookupDependsOnValues;

        NodeRef nodeRef = findNode(workflow, componentName);

        if (nodeRef.trigger()) {
            return triggerDefinitionFacade.executeOptions(
                componentName, componentVersion, nodeRef.operation(), leafPropertyName, inputParameters,
                lookupDependsOnPaths, searchText, connectionId);
        }

        return actionDefinitionFacade.executeOptions(
            componentName, componentVersion, nodeRef.operation(), leafPropertyName, inputParameters,
            lookupDependsOnPaths, searchText, connectionId);
    }

    private static NodeRef findNode(Workflow workflow, String componentName) {
        for (WorkflowTask workflowTask : workflow.getTasks(false)) {
            NodeRef nodeRef = toNodeRef(workflowTask.getType(), componentName, false);

            if (nodeRef != null) {
                return nodeRef;
            }
        }

        for (var workflowTrigger : workflow.getTriggers()) {
            NodeRef nodeRef = toNodeRef(workflowTrigger.getType(), componentName, true);

            if (nodeRef != null) {
                return nodeRef;
            }
        }

        throw new IllegalArgumentException("No node uses component: " + componentName);
    }

    private static NodeRef toNodeRef(String type, String componentName, boolean trigger) {
        String[] parts = type.split("/");

        if (parts.length >= 3 && componentName.equals(parts[0])) {
            return new NodeRef(parts[2], trigger);
        }

        return null;
    }

    private record NodeRef(String operation, boolean trigger) {
    }
}
```
> Verify `WorkflowTrigger.getType()` exists and returns the same `"name/vN/operation"` shape (read
> `WorkflowTrigger.java`); adjust the trigger loop accordingly. Confirm `IntegrationWorkflowService.getWorkflowId(long, String)`
> exists (it's used by `ConnectedUserIntegrationInstanceFacadeImpl`). `MapUtils` import may be unused — drop it if so
> (PMD). The "use the connection's parameters as input parameters" nuance: `executeOptions` itself loads the
> connection by id; `inputParameters` here carries the dependency values the user has entered so far.

Run the test:
`./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*EmbeddedWorkflowInputOptionFacadeImplTest'`
Adjust mock arg matchers until GREEN. Then `./gradlew ...:embedded-configuration-service:spotlessApply`.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacade.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImpl.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacadeImplTest.java
git commit -m "732 Add embedded workflow-input options facade resolving instance connection

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Embed resolved property/group defs in the integration payload

**Files:**
- Modify: `server/ee/.../public_/web/rest/IntegrationApiController.java`

- [ ] **Step 1: Inject `ComponentDefinitionService`**

Add `com.bytechef.platform.component.service.ComponentDefinitionService componentDefinitionService` to the
constructor + field (mirror the existing injected services; keep alphabetical order consistent with the file).

- [ ] **Step 2: Map resolved defs in the input mapping block**

Replace the `InputModel` mapping (~`:285`) so a reference input also carries `property` or `group`:
```java
List<InputModel> inputModels = workflow.getInputs()
    .stream()
    .map(input -> {
        InputModel inputModel = new InputModel()
            .componentName(input.componentName())
            .componentVersion(input.componentVersion())
            .groupName(input.groupName())
            .label(input.label())
            .name(input.name())
            .propertyName(input.propertyName())
            .required(input.required())
            .type(input.type() == null ? null : InputTypeModel.fromValue(input.type()));

        if (input.componentName() != null && (input.propertyName() != null || input.groupName() != null)) {
            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                input.componentName(), input.componentVersion());

            if (input.propertyName() != null) {
                inputModel.property(toComponentPropertyModel(componentDefinition, input.propertyName()));
            } else {
                inputModel.group(toComponentPropertyGroupModel(componentDefinition, input.groupName()));
            }
        }

        return inputModel;
    })
    .toList();
```
Add private static mappers `toComponentPropertyModel(ComponentDefinition, String propertyName)` and
`toComponentPropertyGroupModel(ComponentDefinition, String groupName)` that read the domain
`getProperties()`/`getPropertyGroups()`, find the matching entry, and build the generated
`ComponentPropertyModel`/`ComponentPropertyGroupModel` (name, label, type, controlType, required,
static options as `OptionModel`, `dynamicOptions` = (optionsDataSource present), `optionsLookupDependsOn`).
Use the exact domain getters confirmed in Task 4 Step 1. For `type`, map the domain `Property.getType()` enum to
`InputTypeModel` by name where it matches; if a property type has no `InputType` equivalent, set `type` to null
(the SDK keys off `controlType`).
Add imports: `com.bytechef.platform.component.domain.ComponentDefinition`, the generated
`ComponentPropertyModel`/`ComponentPropertyGroupModel`/`OptionModel`.

- [ ] **Step 3: Compile**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava`
Expected: progresses past the `IntegrationApiController` errors; may still FAIL on the unimplemented controller
options methods (Task 6). That's expected.

- [ ] **Step 4: (commit deferred to Task 6 with the rest of the server change)**

---

## Task 6: Options endpoint controller methods + compile + commit server

**Files:**
- Modify: `server/ee/.../public_/web/rest/IntegrationInstanceWorkflowApiController.java`

- [ ] **Step 1: Inject the facade + add the methods**

Add `EmbeddedWorkflowInputOptionFacade` to the constructor/field. Implement both generated interface methods
(map `Option` domain → `OptionModel`). Mirror the existing frontend/externalUserId pair (frontend resolves the
external user from `SecurityUtils`; the externalUserId variant takes it as a path param — note the facade is
keyed by integration instance id + workflow uuid, so the externalUserId is not strictly needed for resolution
but keep the method signature the generated interface requires):
```java
@Override
@CrossOrigin
public ResponseEntity<List<OptionModel>> getFrontendIntegrationInstanceWorkflowInputOptions(
    Long id, String workflowUuid, WorkflowInputOptionsRequestModel workflowInputOptionsRequestModel) {

    List<Option> options = embeddedWorkflowInputOptionFacade.getWorkflowInputOptions(
        id, workflowUuid, workflowInputOptionsRequestModel.getPropertyName(),
        workflowInputOptionsRequestModel.getLookupDependsOnValues() == null
            ? Map.of() : workflowInputOptionsRequestModel.getLookupDependsOnValues(),
        workflowInputOptionsRequestModel.getSearchText());

    return ResponseEntity.ok(options.stream()
        .map(option -> new OptionModel().label(option.getLabel())
            .value(String.valueOf(option.getValue())))
        .toList());
}

@Override
public ResponseEntity<List<OptionModel>> getIntegrationInstanceWorkflowInputOptions(
    String externalUserId, Long id, String workflowUuid,
    WorkflowInputOptionsRequestModel workflowInputOptionsRequestModel) {

    List<Option> options = embeddedWorkflowInputOptionFacade.getWorkflowInputOptions(
        id, workflowUuid, workflowInputOptionsRequestModel.getPropertyName(),
        workflowInputOptionsRequestModel.getLookupDependsOnValues() == null
            ? Map.of() : workflowInputOptionsRequestModel.getLookupDependsOnValues(),
        workflowInputOptionsRequestModel.getSearchText());

    return ResponseEntity.ok(options.stream()
        .map(option -> new OptionModel().label(option.getLabel())
            .value(String.valueOf(option.getValue())))
        .toList());
}
```
Add imports (`Option`, `OptionModel`, `WorkflowInputOptionsRequestModel`, `Map`, the facade). Confirm the exact
generated method names/param types from Task 3's regen and match them.

- [ ] **Step 2: Compile the module + spotless**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava`
Expected: BUILD SUCCESSFUL.
Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:spotlessApply`

- [ ] **Step 3: Commit the whole server slice (generated + controllers)**

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest
git commit -m "732 Embed resolved component property defs and add options endpoint to embedded public API

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: SDK types

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/types.ts`

- [ ] **Step 1: Add the new types + extend `WorkflowInputType`**

Append new interfaces and extend `WorkflowInputType` (keep keys alphabetical per the lib's lint):
```ts
export interface OptionType {
    label: string;
    value: string;
}

export interface ComponentPropertyType {
    controlType?: string;
    dynamicOptions?: boolean;
    label?: string;
    name: string;
    options?: OptionType[];
    optionsLookupDependsOn?: string[];
    required?: boolean;
    type?: string;
}

export interface ComponentPropertyGroupType {
    label?: string;
    name: string;
    properties?: ComponentPropertyType[];
}
```
Extend `WorkflowInputType` with (alphabetical insertion):
```ts
    componentName?: string;
    componentVersion?: number;
    group?: ComponentPropertyGroupType;
    groupName?: string;
    property?: ComponentPropertyType;
    propertyName?: string;
```

- [ ] **Step 2: Typecheck (build)**

Run: `cd sdks/frontend/embedded/library/react && npm run build`
(or `npx tsc --noEmit`) Expected: passes (additive optional fields). If `node_modules` missing, run `npm install` first.

- [ ] **Step 3: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/types.ts
git commit -m "732 sdk - Add component-property types to embedded connect dialog

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: SDK data flow — options fetch + cache + nested group values

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/index.tsx`

- [ ] **Step 1: Read how `ConnectDialog` is rendered + props passed**

Run: `grep -n "ConnectDialog\|mergedWorkflows\|handleWorkflowInputChange\|createRoot\|render(" sdks/frontend/embedded/library/react/src/components/connect-dialog/index.tsx`
Confirm exactly how props reach `<ConnectDialog>` so the new options handler is threaded the same way.

- [ ] **Step 2: Add an options-fetch handler + cache**

Add hook state for an options cache and an async fetch using the existing `fetch` apiClient:
```ts
const [workflowInputOptions, setWorkflowInputOptions] = useState<Record<string, OptionType[]>>({});

const optionsCacheKey = (workflowUuid: string, propertyName: string, dependencyValues: Record<string, unknown>) =>
    `${workflowUuid}:${propertyName}:${JSON.stringify(dependencyValues ?? {})}`;

const loadWorkflowInputOptions = useCallback(
    async (workflowUuid: string, propertyName: string, dependencyValues: Record<string, unknown>) => {
        const instanceId = currentIntegrationInstanceIdRef.current;

        if (!instanceId) {
            return;
        }

        const cacheKey = optionsCacheKey(workflowUuid, propertyName, dependencyValues);

        const options = await fetch<OptionType[]>(
            `/api/embedded/v1/integration-instances/${instanceId}/workflows/${workflowUuid}/options`,
            {body: {lookupDependsOnValues: dependencyValues, propertyName}, method: 'POST'}
        ).catch(() => [] as OptionType[]);

        setWorkflowInputOptions((previous) => ({...previous, [cacheKey]: options}));
    },
    [fetch]
);
```
Pass `workflowInputOptions`, `loadWorkflowInputOptions`, and `optionsCacheKey` (or a derived getter) to
`ConnectDialog` via the same prop pass-through used for `mergedWorkflows`/`handleWorkflowInputChange`.

- [ ] **Step 3: Support nested group values in the PUT payload**

`handleWorkflowInputChange(workflowUuid, inputName, value)` already merges into `inputOverrides[workflowUuid]`.
Add a sibling for group members that writes a nested object:
```ts
const handleWorkflowGroupInputChange = useCallback(
    (workflowUuid: string, groupName: string, memberName: string, value: string) => {
        setInputOverrides((previous) => {
            const groupValue = {
                ...((previous[workflowUuid]?.[groupName] as Record<string, string> | undefined) ?? {}),
                [memberName]: value,
            };

            const updated = {
                ...previous,
                [workflowUuid]: {...previous[workflowUuid], [groupName]: groupValue},
            };

            inputOverridesRef.current = updated;

            return updated;
        });

        // reuse the same debounced PUT as handleWorkflowInputChange
        triggerDebouncedSave(workflowUuid);
    },
    []
);
```
Factor the debounced-PUT body of `handleWorkflowInputChange` into a `triggerDebouncedSave(workflowUuid)` helper
so both single and group changes reuse it (DRY). `inputOverrides` value type widens to
`Record<string, Record<string, unknown>>` (string | nested object). Update the type accordingly.

- [ ] **Step 4: Build**

Run: `cd sdks/frontend/embedded/library/react && npm run build`
Expected: passes.

- [ ] **Step 5: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/index.tsx
git commit -m "732 sdk - Add workflow input options fetch and nested group values to connect dialog hook

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: SDK rendering — dynamic select, groups, dependencies, fallback

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.tsx`

- [ ] **Step 1: Add a `DialogDynamicSelectField` component**

A select whose options come from `workflowInputOptions` and that triggers `loadWorkflowInputOptions` on mount
and when its dependency values change. Reuse `styles.dialogInputField`/`requiredIndicator`. Signature:
```tsx
interface DialogDynamicSelectFieldProps {
    dependencyValues: Record<string, unknown>;
    label: string;
    loadOptions: (dependencyValues: Record<string, unknown>) => void;
    name: string;
    onChange: (value: string) => void;
    options?: OptionType[];
    required?: boolean;
    value?: string;
}
```
Implement with a `useEffect` keyed on `JSON.stringify(dependencyValues)` calling `loadOptions(dependencyValues)`;
render a `<select>` with the resolved `options` (empty → a disabled "No options" placeholder); a dependency not
yet satisfied (any dependency value empty) → disabled with "Select dependencies first".

- [ ] **Step 2: Add a `DialogGroupField` component**

Renders a labeled fieldset of member fields for a `ComponentPropertyGroupType`; each member is a
`DialogInputField` (static/scalar) or `DialogDynamicSelectField` (when `member.dynamicOptions`), calling
`handleWorkflowGroupInputChange(workflowUuid, group.name, member.name, value)`. Reuse
`styles.workflowInputsContainer`.

- [ ] **Step 3: Branch input rendering in the workflows container**

In `DialogWorkflowsContainer`, replace the single `<DialogInputField>` per input with a branch:
- `input.group` → `<DialogGroupField .../>`
- `input.property?.dynamicOptions` → `<DialogDynamicSelectField .../>` (dependencyValues = current sibling
  values named in `optionsLookupDependsOn`)
- `input.property` (static options) → `<DialogInputField options={input.property.options?.map((o) => o.value)} .../>`
  (and render label via the option label — keep simple: pass `options` as `OptionType[]` by extending
  `DialogInputField` to accept `OptionType[]`)
- else (no resolved def, or plain primitive) → existing `<DialogInputField>` text/select (dangling fallback).
Thread the new props (`workflowInputOptions`, `loadWorkflowInputOptions`, `handleWorkflowGroupInputChange`) down
from `ConnectDialog`'s props (added in Task 8). Apply the same branch in `DialogToolsContainer` for MCP workflows
(reuse the same sub-components + the MCP handlers).

- [ ] **Step 4: Build + lint**

Run: `cd sdks/frontend/embedded/library/react && npm run build && npm run lint`
Expected: pass. Fix lint (the lib uses its own eslint config; match it).

- [ ] **Step 5: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.tsx
git commit -m "732 sdk - Render dynamic selects, property groups, and dependencies in connect dialog

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: SDK tests

**Files:**
- Modify/Create: `sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.test.tsx`

- [ ] **Step 1: Read the existing test harness**

Run: `sed -n '1,80p' sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.test.tsx`
Mirror its render setup, mocks, and assertions style.

- [ ] **Step 2: Write tests (RED → GREEN)**

Add tests:
1. A workflow input with `property.dynamicOptions` renders a select that, after `loadOptions` resolves
   (mock the prop to populate options), shows the option labels.
2. A workflow input with a `group` renders the group's member fields (assert member labels present).
3. A dependent dynamic select is disabled until its dependency value is set, then enabled.
4. An input with `componentName` but no resolved `property`/`group` falls back to a plain text input.
Provide the new props (`workflowInputOptions`, `loadWorkflowInputOptions`, `handleWorkflowGroupInputChange`) as
mocks/fixtures. Use `@testing-library/react` + `vitest` (globals enabled). For Radix-free plain DOM, no special
pointer stubs needed.

Run: `cd sdks/frontend/embedded/library/react && npm test -- ConnectDialog`
Iterate to GREEN.

- [ ] **Step 3: Full SDK check**

Run: `cd sdks/frontend/embedded/library/react && npm run lint && npm run build && npm test`
Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.test.tsx
git commit -m "732 sdk - Test dynamic options, groups, dependencies, and fallback in connect dialog

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: Full verification

**Files:** none.

- [ ] **Step 1: Server checks (touched modules)**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*EmbeddedWorkflowInputOptionFacadeImplTest'`
Then: `./gradlew spotlessApply` (scoped to touched modules) — confirm no diffs remain uncommitted.

- [ ] **Step 2: SDK checks**

Run: `cd sdks/frontend/embedded/library/react && npm run lint && npm run build && npm test`
Expected: all pass.

- [ ] **Step 3: Manual E2E (record outcome)**

In a running embedded environment: author a workflow input referencing a component property with
connection-backed dynamic options (and a group), expose it on an embedded integration, open the SDK
ConnectDialog after connecting, and verify: dynamic dropdown populates from the options endpoint; a dependent
option refetches when its parent changes; a group submits a nested-object value; a dangling reference shows a
text input. Record reachability; if the running env is unavailable, note that the automated tests + compile are
the gate and E2E is pending.

---

## Testing Strategy

- **Server unit:** `EmbeddedWorkflowInputOptionFacadeImplTest` (delegation + instance connection). Add a mapping
  assertion for `IntegrationApiController` if a controller test harness exists in the module; otherwise compile +
  the SDK integration is the gate (note it).
- **SDK unit (Vitest):** dynamic select render + options, group render, dependency gating, dangling fallback.
- **Manual/E2E:** Task 11 Step 3.

## Rollback Plan

All changes are additive. Server: new optional `Input` fields + new schemas + a new endpoint + a new facade —
existing endpoints and primitive inputs are unaffected; revert the commits and regenerate. SDK: additive types +
gated rendering (inputs without resolved defs render exactly as before); revert the SDK commits. No DB
migrations. The new endpoint simply becomes unused if the SDK is reverted.
