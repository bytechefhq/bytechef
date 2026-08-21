# Embedded Field Mapping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Paragon-style field mapping to the embedded Connect Portal: an end user picks a remote object type and maps application fields (supplied by the embedding app at runtime) to live integration fields, persisted as a workflow-input value and referenceable as data pills in the embedded workflow builder.

**Architecture:** Fetch is Paragon-faithful — the embedding developer supplies `objectTypes.get`/`integrationFields.get` callbacks; the SDK gives them a context-bound `executeAction` helper that proxies (server-side, against live credentials) to a component action via the existing `POST /api/embedded/v1/{externalUserId}/components/{name}/versions/{v}/actions/{action}` endpoint. A new `FIELD_MAPPING` workflow-input type carries a derived `objectName`; the InputEditor's test-value field holds a static `mapObjectFields`-shaped JSON whose `applicationFields` render as data pills. The mapping is stored as the input value (no transform engine).

**Tech Stack:** Java 25 / Spring Boot (server, EE), React 19 + TypeScript (embedded SDK at `sdks/frontend/embedded/library/react`, main client at `client/`), Vitest, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-06-04-embedded-field-mapping-design.md`

**Phases (each independently testable):**
- **A — Server:** `Workflow.Input.objectName`, `FIELD_MAPPING` type + `objectName` in the embedded REST surface, IDOR fix.
- **B — Embedded SDK:** JWT decode, `executeAction` helper, config types, `FieldMappingField`, ConnectDialog wiring.
- **C — InputEditor:** `field_mapping` type, JSON test-value editor, `objectName` derivation on save.
- **D — Data pills:** expand `applicationFields` into child pills.

> **Conventions:** EE files (`server/ee/**`) use the ByteChef Enterprise license header and a `@version ee` Javadoc tag. Run `./gradlew spotlessApply` before each server commit and `cd client && npm run check` (or the SDK's lint) before each client commit. Client object keys must be alphabetical (ESLint `sort-keys`). Insert one blank line before control statements and after variable modification (Java).

---

## Phase A — Server

### Task A1: Add `objectName` to `Workflow.Input`

**Files:**
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/domain/Workflow.java:411-418` (Input record) and `:164-181` (parsing)
- Modify: `server/libs/platform/.../WorkflowConstants.java` (add `OBJECT_NAME`)
- Test: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowTest.java` (create if absent)

- [ ] **Step 1: Write the failing test**

Create or extend `WorkflowTest.java`:

```java
package com.bytechef.atlas.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowTest {

    @Test
    void testFieldMappingInputParsesObjectName() {
        Map<String, Object> definition = Map.of(
            WorkflowConstants.INPUTS,
            List.of(
                Map.of(
                    "name", "contactMapping", "label", "Contact Mapping", "type", "field_mapping",
                    "objectName", "Contacts")));

        Workflow workflow = new Workflow("1", definition, Workflow.Format.JSON);

        Workflow.Input input = workflow.getInputs()
            .getFirst();

        assertEquals("Contacts", input.objectName());
        assertEquals("field_mapping", input.type());
    }

    @Test
    void testPlainInputHasNullObjectName() {
        Map<String, Object> definition = Map.of(
            WorkflowConstants.INPUTS, List.of(Map.of("name", "x", "type", "string")));

        Workflow workflow = new Workflow("1", definition, Workflow.Format.JSON);

        assertNull(workflow.getInputs()
            .getFirst()
            .objectName());
    }
}
```

> If the `Workflow(String, Map, Format)` constructor signature differs, match the existing one used in sibling tests (search `new Workflow(` under `atlas-configuration`); the assertions on `input.objectName()` are the point.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests "com.bytechef.atlas.configuration.domain.WorkflowTest"`
Expected: FAIL — `objectName()` method does not exist / compilation error.

- [ ] **Step 3: Add the constant**

In `WorkflowConstants.java`, add alongside the existing constants (keep alphabetical grouping near `NAME`/`LABEL`):

```java
    public static final String OBJECT_NAME = "objectName";
```

- [ ] **Step 4: Extend the `Input` record and its parsing**

In `Workflow.java`, replace the `Input` record (lines 411-418) with:

```java
    public record Input(
        String name, String label, String type, boolean required,
        ComponentInputReference componentReference, String objectName) implements Serializable {

        public Input(String name, String label, String type, boolean required) {
            this(name, label, type, required, null, null);
        }

        public Input(
            String name, String label, String type, boolean required, ComponentInputReference componentReference) {

            this(name, label, type, required, componentReference, null);
        }
    }
```

In the inputs parsing block (lines 170-180), pass `objectName` as the new last argument:

```java
                        return new Input(
                            MapUtils.getRequiredString(map, WorkflowConstants.NAME),
                            MapUtils.getString(map, WorkflowConstants.LABEL),
                            MapUtils.getString(map, WorkflowConstants.TYPE, "string"),
                            MapUtils.getBoolean(map, WorkflowConstants.REQUIRED, false),
                            componentName == null
                                ? null
                                : new ComponentInputReference(
                                    componentName,
                                    MapUtils.getInteger(map, WorkflowConstants.COMPONENT_VERSION),
                                    MapUtils.getString(map, WorkflowConstants.GROUP_NAME)),
                            MapUtils.getString(map, WorkflowConstants.OBJECT_NAME));
```

> The two extra convenience constructors preserve every existing `new Input(...)` call site, so nothing else needs to change.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests "com.bytechef.atlas.configuration.domain.WorkflowTest"`
Expected: PASS.

- [ ] **Step 6: Compile the module to catch call-site breakage**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/atlas/atlas-configuration/atlas-configuration-api server/libs/platform
git commit -m "732 Add objectName to workflow Input for field mapping"
```

---

### Task A2: Surface `FIELD_MAPPING` type + `objectName` through the embedded REST input model

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml` (the `Input` schema + `InputType` enum)
- Regenerate: `.../embedded-configuration-public-rest/generated/.../model/InputModel.java`, `InputTypeModel.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapper.java:125-131`

- [ ] **Step 1: Edit the OpenAPI schema**

In `openapi.yaml`, find the `Input` schema and the `InputType` enum. Add `FIELD_MAPPING` to the enum and an `objectName` property to `Input`:

```yaml
    InputType:
      type: string
      enum:
        - STRING
        - NUMBER
        - BOOLEAN
        - OBJECT
        - ARRAY
        - FIELD_MAPPING
```

```yaml
    Input:
      type: object
      properties:
        # ...existing properties (label, name, required, type, componentReference)...
        objectName:
          type: string
          description: For FIELD_MAPPING inputs, the object name used to match the SDK mapObjectFields config.
```

> Match the existing enum's exact member set and indentation; only **add** `FIELD_MAPPING` and `objectName`. Do not reorder existing members (ordinal/string stability).

- [ ] **Step 2: Regenerate the models**

Find the OpenAPI generation task for this module (check its `build.gradle.kts` for an `openApiGenerate`/`generate*` task), then run it. Example:

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:openApiGenerate`
Expected: BUILD SUCCESSFUL, and `generated/.../model/InputTypeModel.java` now contains `FIELD_MAPPING`, and `InputModel.java` now has a `objectName` field with getter/setter.

- [ ] **Step 3: Write the failing mapper test**

Create `.../embedded-configuration-public-rest/src/test/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapperTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputTypeModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * @version ee
 */
class ConnectedUserIntegrationMapperTest {

    @Test
    void testFieldMappingInputMapsTypeAndObjectName() {
        Workflow.Input input = new Workflow.Input(
            "contactMapping", "Contact Mapping", "field_mapping", false, null, "Contacts");

        InputModel model = TestMapper.INSTANCE.map(input);

        assertEquals(InputTypeModel.FIELD_MAPPING, model.getType());
        assertEquals("Contacts", model.getObjectName());
    }

    interface TestMapper {
        ConnectedUserIntegrationMapper.IntegrationInputMapper INSTANCE =
            Mappers.getMapper(ConnectedUserIntegrationMapper.IntegrationInputMapper.class);
    }
}
```

> Adjust the `Mappers.getMapper(...)` target to the actual interface that declares `map(Workflow.Input)` (the explorer found it on `ConnectedUserIntegrationMapper`; if `map` is a `default` method on the top-level mapper, instantiate that mapper instead). The assertions are the contract.

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test --tests "*ConnectedUserIntegrationMapperTest"`
Expected: FAIL — `getObjectName()` returns null / `FIELD_MAPPING` mapping absent.

- [ ] **Step 5: Update the mapper**

In `ConnectedUserIntegrationMapper.java`, update the `map(Workflow.Input)` default method (lines 125-131):

```java
        default InputModel map(Workflow.Input input) {
            return new InputModel()
                .label(input.label())
                .name(input.name())
                .objectName(input.objectName())
                .required(input.required())
                .type(InputTypeModel.valueOf(StringUtils.upperCase(input.type())));
        }
```

> `input.type()` is `"field_mapping"`; `upperCase` → `"FIELD_MAPPING"`, which `InputTypeModel.valueOf` now resolves after Step 2.

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test --tests "*ConnectedUserIntegrationMapperTest"`
Expected: PASS.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest
git commit -m "732 Surface FIELD_MAPPING input type and objectName in embedded REST"
```

---

### Task A3: Fix the `X-Instance-Id` IDOR in `ConnectionIdHelper`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-execution/embedded-execution-service/src/main/java/com/bytechef/ee/embedded/execution/util/ConnectionIdHelper.java:39-56`
- Test: `.../embedded-execution-service/src/test/java/com/bytechef/ee/embedded/execution/util/ConnectionIdHelperTest.java` (create)

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 */
class ConnectionIdHelperTest {

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final ConnectionIdHelper connectionIdHelper =
        new ConnectionIdHelper(connectedUserService, integrationInstanceService);

    @Test
    void testGetConnectionIdRejectsForeignInstance() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(1L);
        when(connectedUserService.getConnectedUser("user-1", Environment.PRODUCTION)).thenReturn(connectedUser);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectedUserId()).thenReturn(99L);
        when(integrationInstanceService.getIntegrationInstance(5L)).thenReturn(integrationInstance);

        assertThrows(
            AccessDeniedException.class,
            () -> connectionIdHelper.getConnectionId("user-1", "slack", 5L, Environment.PRODUCTION));
    }

    @Test
    void testGetConnectionIdReturnsConnectionForOwnedInstance() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(1L);
        when(connectedUserService.getConnectedUser("user-1", Environment.PRODUCTION)).thenReturn(connectedUser);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectedUserId()).thenReturn(1L);
        when(integrationInstance.getConnectionId()).thenReturn(42L);
        when(integrationInstanceService.getIntegrationInstance(5L)).thenReturn(integrationInstance);

        Long connectionId = connectionIdHelper.getConnectionId("user-1", "slack", 5L, Environment.PRODUCTION);

        assertEquals(42L, connectionId);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-execution:embedded-execution-service:test --tests "*ConnectionIdHelperTest"`
Expected: FAIL — first test does not throw (no ownership check); `getConnectedUser` not stubbed-as-called in the instanceId branch.

- [ ] **Step 3: Add the ownership check**

Replace the `getConnectionId` method body (lines 39-56) with:

```java
    public Long getConnectionId(String externalUserid, String componentName, Long instanceId, Environment environment) {
        Long connectionId;

        if (instanceId == null) {
            ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserid, environment);

            connectionId = integrationInstanceService
                .fetchIntegrationInstance(connectedUser.getId(), componentName, environment)
                .map(IntegrationInstance::getConnectionId)
                .orElse(null);
        } else {
            ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserid, environment);

            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(instanceId);

            if (!connectedUser.getId()
                .equals(integrationInstance.getConnectedUserId())) {

                throw new AccessDeniedException(
                    "Integration instance " + instanceId + " is not owned by the connected user");
            }

            connectionId = integrationInstance.getConnectionId();
        }

        return connectionId;
    }
```

Add the import:

```java
import org.springframework.security.access.AccessDeniedException;
```

> This binds the resolved connection to the user identified by `externalUserId`. It assumes the embedded security filter binds the JWT `sub` to the path `externalUserId` (standard for `/api/embedded/v1/{externalUserId}/...`). If that binding is ever found absent, file a follow-up — it is a broader concern than this helper.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-execution:embedded-execution-service:test --tests "*ConnectionIdHelperTest"`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-execution/embedded-execution-service
git commit -m "732 Verify connected-user ownership of X-Instance-Id before action execution"
```

---

## Phase B — Embedded SDK

> Working directory for SDK lint/test/build: `sdks/frontend/embedded/library/react`. Confirm its scripts first: `cat sdks/frontend/embedded/library/react/package.json` (look for `test`, `lint`). Tests use Vitest (`*.test.ts`/`*.test.tsx`).

### Task B1: JWT `sub` decode utility

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.ts`
- Test: `sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.test.ts` (create)

- [ ] **Step 1: Write the failing test**

```ts
import {describe, expect, it} from 'vitest';

import {decodeJwtSubject} from './utils';

describe('decodeJwtSubject', () => {
    it('extracts the sub claim from a JWT', () => {
        // header.{"sub":"user-123"}.signature  (payload is base64url of {"sub":"user-123"})
        const payload = btoa(JSON.stringify({sub: 'user-123'}))
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');
        const token = `header.${payload}.signature`;

        expect(decodeJwtSubject(token)).toBe('user-123');
    });

    it('returns undefined for a malformed token', () => {
        expect(decodeJwtSubject('not-a-jwt')).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run (from `sdks/frontend/embedded/library/react`): `npx vitest run src/components/connect-dialog/utils.test.ts`
Expected: FAIL — `decodeJwtSubject` is not exported.

- [ ] **Step 3: Implement**

Append to `utils.ts`:

```ts
/**
 * Extracts the `sub` (external user id) claim from a JWT without verifying its signature. The SDK needs the external
 * user id for action-execution URLs; the server independently verifies the token, so client-side decoding is safe to
 * read claims from. Returns `undefined` when the token is malformed.
 */
export const decodeJwtSubject = (jwtToken: string): string | undefined => {
    try {
        const payloadSegment = jwtToken.split('.')[1];

        if (!payloadSegment) {
            return undefined;
        }

        const normalized = payloadSegment.replace(/-/g, '+').replace(/_/g, '/');
        const payload = JSON.parse(atob(normalized)) as {sub?: string};

        return payload.sub;
    } catch {
        return undefined;
    }
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/utils.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.ts sdks/frontend/embedded/library/react/src/components/connect-dialog/utils.test.ts
git commit -m "520 client - Add JWT subject decode util to embedded SDK"
```

---

### Task B2: `useExecuteAction` helper hook

**Files:**
- Create: `sdks/frontend/embedded/library/react/src/components/connect-dialog/useExecuteAction.ts`
- Test: `sdks/frontend/embedded/library/react/src/components/connect-dialog/useExecuteAction.test.ts` (create)

- [ ] **Step 1: Write the failing test**

```ts
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useExecuteAction from './useExecuteAction';

describe('useExecuteAction', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    it('posts to the action endpoint with the instance header and returns result', async () => {
        const apiFetch = vi.fn().mockResolvedValue({result: [{name: 'Contacts'}]});

        const {result} = renderHook(() => useExecuteAction(apiFetch, 'user-1', 7));

        let actionResult: unknown;

        await act(async () => {
            actionResult = await result.current('hubspot', 1, 'listObjects', {q: 'x'});
        });

        expect(apiFetch).toHaveBeenCalledWith(
            '/api/embedded/v1/user-1/components/hubspot/versions/1/actions/listObjects',
            {body: {input: {q: 'x'}}, headers: {'X-Instance-Id': '7'}, method: 'POST'}
        );
        expect(actionResult).toEqual([{name: 'Contacts'}]);
    });

    it('returns an empty array when externalUserId is missing', async () => {
        const apiFetch = vi.fn();

        const {result} = renderHook(() => useExecuteAction(apiFetch, undefined, 7));

        let actionResult: unknown;

        await act(async () => {
            actionResult = await result.current('hubspot', 1, 'listObjects', {});
        });

        expect(apiFetch).not.toHaveBeenCalled();
        expect(actionResult).toEqual([]);
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/components/connect-dialog/useExecuteAction.test.ts`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Implement**

```ts
import {useCallback} from 'react';

import {ApiFetch} from './types';

export type ExecuteActionFunction = (
    componentName: string,
    componentVersion: number,
    actionName: string,
    input: Record<string, unknown>
) => Promise<unknown[]>;

/**
 * Returns a function the embedding app's field-mapping callbacks use to fetch object types / integration fields. It
 * proxies to the embedded generic action endpoint, which runs the named component action against the connected
 * account's live credentials. The integration instance id is bound here (sent as `X-Instance-Id`) so callbacks never
 * thread it and cannot target another user's instance. Returns the action's `result` array (or `[]`).
 */
export default function useExecuteAction(
    apiFetch: ApiFetch | undefined,
    externalUserId: string | undefined,
    integrationInstanceId: number | undefined
): ExecuteActionFunction {
    return useCallback(
        async (componentName, componentVersion, actionName, input) => {
            if (!apiFetch || !externalUserId || !integrationInstanceId) {
                return [];
            }

            try {
                const response = await apiFetch<{result?: unknown[]}>(
                    `/api/embedded/v1/${externalUserId}/components/${componentName}/versions/${componentVersion}/actions/${actionName}`,
                    {
                        body: {input},
                        headers: {'X-Instance-Id': String(integrationInstanceId)},
                        method: 'POST',
                    }
                );

                return response?.result ?? [];
            } catch (error: unknown) {
                console.error('Failed to execute action:', (error as Error).message);

                return [];
            }
        },
        [apiFetch, externalUserId, integrationInstanceId]
    );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/useExecuteAction.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/useExecuteAction.ts sdks/frontend/embedded/library/react/src/components/connect-dialog/useExecuteAction.test.ts
git commit -m "520 client - Add useExecuteAction helper to embedded SDK"
```

---

### Task B3: Field-mapping config + value types

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/types.ts`

- [ ] **Step 1: Add the types**

Append to `types.ts` (keep object keys alphabetical per ESLint):

```ts
export interface FieldMappingObjectListArgsType {
    executeAction: import('./useExecuteAction').ExecuteActionFunction;
    search?: string;
}

export interface FieldMappingIntegrationFieldArgsType {
    executeAction: import('./useExecuteAction').ExecuteActionFunction;
    objectType: string;
    search?: string;
}

export interface FieldMappingConfigType {
    applicationFields?: OptionType[];
    defaultFields?: string[];
    fields?: OptionType[];
    integrationFields: {get: (args: FieldMappingIntegrationFieldArgsType) => Promise<OptionType[]>};
    objectTypes: {get: (args: FieldMappingObjectListArgsType) => Promise<OptionType[]>};
    userCanCreateFields?: boolean;
    userCanRemoveMappings?: boolean;
}

export type MapObjectFieldsType = Record<string, FieldMappingConfigType>;

// Component-facing config: identical to FieldMappingConfigType but with `executeAction` already bound into the
// callbacks by ConnectDialog (Task B5), so FieldMappingField never sees or threads executeAction.
export interface BoundFieldMappingObjectListArgsType {
    search?: string;
}

export interface BoundFieldMappingIntegrationFieldArgsType {
    objectType: string;
    search?: string;
}

export interface BoundFieldMappingConfigType {
    applicationFields?: OptionType[];
    defaultFields?: string[];
    fields?: OptionType[];
    integrationFields: {get: (args: BoundFieldMappingIntegrationFieldArgsType) => Promise<OptionType[]>};
    objectTypes: {get: (args: BoundFieldMappingObjectListArgsType) => Promise<OptionType[]>};
    userCanCreateFields?: boolean;
    userCanRemoveMappings?: boolean;
}

export interface FieldMappingRowValueType {
    applicationField: {custom: boolean; label: string; value: string};
    integrationField: string;
}

export interface FieldMappingValueType {
    mappings: FieldMappingRowValueType[];
    objectType: string;
}
```

Then extend `WorkflowInputType` to carry `objectName` (returned by the server for `FIELD_MAPPING`):

```ts
export interface WorkflowInputType {
    name: string;
    label: string;
    objectName?: string;
    type: 'string' | 'number' | 'boolean' | 'object' | 'array' | 'field_mapping';
    componentReference?: ComponentInputReferenceType;
    defaultValue?: unknown;
    required?: boolean;
    value?: string | number | readonly string[] | Record<string, unknown> | undefined;
}
```

> `import('./useExecuteAction')` inline-type-imports avoid a circular static import. If the SDK's lint forbids inline imports, add `import type {ExecuteActionFunction} from './useExecuteAction';` at the top and reference it directly.

- [ ] **Step 2: Typecheck**

Run: `npx tsc --noEmit -p sdks/frontend/embedded/library/react/tsconfig.json` (or the SDK's `npm run typecheck` if defined)
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/types.ts
git commit -m "520 client - Add field-mapping config and value types to embedded SDK"
```

---

### Task B4: `FieldMappingField` component

**Files:**
- Create: `sdks/frontend/embedded/library/react/src/components/connect-dialog/FieldMappingField.tsx`
- Test: `sdks/frontend/embedded/library/react/src/components/connect-dialog/FieldMappingField.test.tsx` (create)

- [ ] **Step 1: Write the failing test**

```tsx
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import FieldMappingField from './FieldMappingField';
import {BoundFieldMappingConfigType} from './types';

const config: BoundFieldMappingConfigType = {
    fields: [
        {label: 'Title', value: 'title'},
        {label: 'Email', value: 'email'},
    ],
    integrationFields: {
        get: vi.fn().mockResolvedValue([
            {label: 'First Name', value: 'first_name'},
            {label: 'Subject', value: 'subject'},
        ]),
    },
    objectTypes: {
        get: vi.fn().mockResolvedValue([
            {label: 'Contacts', value: 'contacts'},
            {label: 'Leads', value: 'leads'},
        ]),
    },
};

describe('FieldMappingField', () => {
    it('loads object types and renders an application-field row per configured field', async () => {
        render(<FieldMappingField config={config} label="Contact Mapping" onChange={vi.fn()} value={undefined} />);

        await waitFor(() => expect(screen.getByRole('option', {name: 'Contacts'})).toBeInTheDocument());

        expect(screen.getByText('Title')).toBeInTheDocument();
        expect(screen.getByText('Email')).toBeInTheDocument();
    });

    it('loads integration fields after an object type is chosen and emits the mapping', async () => {
        const onChange = vi.fn();
        const user = userEvent.setup();

        render(<FieldMappingField config={config} label="Contact Mapping" onChange={onChange} value={undefined} />);

        await waitFor(() => expect(screen.getByRole('option', {name: 'Contacts'})).toBeInTheDocument());

        const objectSelect = screen.getByLabelText('Object type', {exact: false});

        await act(async () => {
            await user.selectOptions(objectSelect, 'contacts');
        });

        await waitFor(() => expect(config.integrationFields.get).toHaveBeenCalled());

        const titleRowSelect = screen.getByLabelText('Title', {exact: false});

        await act(async () => {
            await user.selectOptions(titleRowSelect, 'subject');
        });

        expect(onChange).toHaveBeenLastCalledWith({
            mappings: [{applicationField: {custom: false, label: 'Title', value: 'title'}, integrationField: 'subject'}],
            objectType: 'contacts',
        });
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/components/connect-dialog/FieldMappingField.test.tsx`
Expected: FAIL — component does not exist.

- [ ] **Step 3: Implement**

```tsx
import {useEffect, useMemo, useState} from 'react';

import styles from './styles.module.css';
import {BoundFieldMappingConfigType, FieldMappingValueType, OptionType} from './types';

interface FieldMappingFieldProps {
    config: BoundFieldMappingConfigType;
    label: string;
    onChange: (value: FieldMappingValueType) => void;
    required?: boolean;
    value?: FieldMappingValueType;
}

interface RowType {
    custom: boolean;
    label: string;
    value: string;
}

const FieldMappingField = ({config, label, onChange, required, value}: FieldMappingFieldProps) => {
    const [objectType, setObjectType] = useState<string>(value?.objectType ?? '');
    const [objectTypeOptions, setObjectTypeOptions] = useState<OptionType[]>([]);
    const [integrationFieldOptions, setIntegrationFieldOptions] = useState<OptionType[]>([]);
    const [mappings, setMappings] = useState<Record<string, string>>(
        () => Object.fromEntries((value?.mappings ?? []).map((row) => [row.applicationField.value, row.integrationField]))
    );

    const configuredFields = useMemo<OptionType[]>(
        () => config.fields ?? config.applicationFields ?? [],
        [config.fields, config.applicationFields]
    );

    const [rows, setRows] = useState<RowType[]>(() => {
        const visible = config.defaultFields
            ? configuredFields.filter((field) => config.defaultFields!.includes(field.value))
            : configuredFields;

        return visible.map((field) => ({custom: false, label: field.label, value: field.value}));
    });

    useEffect(() => {
        let cancelled = false;

        void config.objectTypes
            .get({search: undefined})
            .catch(() => [] as OptionType[])
            .then((options) => {
                if (!cancelled) {
                    setObjectTypeOptions(options);
                }
            });

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (!objectType) {
            setIntegrationFieldOptions([]);

            return;
        }

        let cancelled = false;

        void config.integrationFields
            .get({objectType, search: undefined})
            .catch(() => [] as OptionType[])
            .then((options) => {
                if (!cancelled) {
                    setIntegrationFieldOptions(options);
                }
            });

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [objectType]);

    const emit = (nextObjectType: string, nextMappings: Record<string, string>, nextRows: RowType[]) => {
        onChange({
            mappings: nextRows
                .filter((row) => nextMappings[row.value])
                .map((row) => ({
                    applicationField: {custom: row.custom, label: row.label, value: row.value},
                    integrationField: nextMappings[row.value],
                })),
            objectType: nextObjectType,
        });
    };

    const handleObjectTypeChange = (next: string) => {
        setObjectType(next);

        emit(next, mappings, rows);
    };

    const handleRowChange = (fieldValue: string, integrationField: string) => {
        const nextMappings = {...mappings, [fieldValue]: integrationField};

        setMappings(nextMappings);

        emit(objectType, nextMappings, rows);
    };

    const handleRemoveRow = (fieldValue: string) => {
        const nextRows = rows.filter((row) => row.value !== fieldValue);
        const nextMappings = {...mappings};

        delete nextMappings[fieldValue];

        setRows(nextRows);
        setMappings(nextMappings);

        emit(objectType, nextMappings, nextRows);
    };

    const handleCreateField = () => {
        const fieldLabel = window.prompt('New field name');

        if (!fieldLabel) {
            return;
        }

        const nextRows = [...rows, {custom: true, label: fieldLabel, value: fieldLabel}];

        setRows(nextRows);

        emit(objectType, mappings, nextRows);
    };

    return (
        <fieldset className={styles.workflowInputsContainer}>
            <label>
                {label}

                {required && <span className={styles.requiredIndicator}>*</span>}
            </label>

            <fieldset className={styles.dialogInputField}>
                <label htmlFor={`${label}-objectType`}>Object type</label>

                <select
                    id={`${label}-objectType`}
                    onChange={(event) => handleObjectTypeChange(event.target.value)}
                    value={objectType}
                >
                    <option value="">Select object type</option>

                    {objectTypeOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>
            </fieldset>

            {rows.map((row) => (
                <fieldset className={styles.dialogInputField} key={row.value}>
                    <label htmlFor={`${label}-${row.value}`}>{row.label}</label>

                    <select
                        disabled={!objectType}
                        id={`${label}-${row.value}`}
                        onChange={(event) => handleRowChange(row.value, event.target.value)}
                        value={mappings[row.value] ?? ''}
                    >
                        <option value="">{objectType ? 'Select field' : 'Select object type first'}</option>

                        {integrationFieldOptions.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>

                    {config.userCanRemoveMappings && (
                        <button onClick={() => handleRemoveRow(row.value)} type="button">
                            Remove
                        </button>
                    )}
                </fieldset>
            ))}

            {config.userCanCreateFields && (
                <button onClick={handleCreateField} type="button">
                    Add custom field
                </button>
            )}
        </fieldset>
    );
};

export default FieldMappingField;
```

> Note: this component receives a `BoundFieldMappingConfigType` whose callbacks already have `executeAction` bound (ConnectDialog does the binding in Task B5), so it calls `get({search})` / `get({objectType, search})` and never touches `executeAction`. In the isolated test the callbacks are mocks.

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/FieldMappingField.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/FieldMappingField.tsx sdks/frontend/embedded/library/react/src/components/connect-dialog/FieldMappingField.test.tsx
git commit -m "520 client - Add FieldMappingField component to embedded SDK"
```

---

### Task B5: Wire `FieldMappingField` into ConnectDialog

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.tsx` (props type + `renderWorkflowInput`)
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/index.tsx` (hook props `mapObjectFields`, build `executeAction`, pass down)

- [ ] **Step 1: Extend ConnectDialog props and dispatch**

In `ConnectDialog.tsx`, add to `DialogProps`:

```ts
    executeAction?: import('./useExecuteAction').ExecuteActionFunction;
    mapObjectFields?: import('./types').MapObjectFieldsType;
```

In `renderWorkflowInput`, add a `FIELD_MAPPING` branch **before** the group/plain branches (it needs `mapObjectFields` + `executeAction`, threaded via the render args — extend `RenderWorkflowInputArgs` to include `executeAction` and `mapObjectFields`, populated where `renderWorkflowInput` is invoked):

```tsx
    if (input.type === 'field_mapping') {
        const objectName = input.objectName ?? input.name;
        const rawConfig = mapObjectFields?.[objectName];

        if (!rawConfig || !executeAction) {
            return null;
        }

        const config = {
            ...rawConfig,
            integrationFields: {
                get: (args: import('./types').BoundFieldMappingIntegrationFieldArgsType) =>
                    rawConfig.integrationFields.get({...args, executeAction}),
            },
            objectTypes: {
                get: (args: import('./types').BoundFieldMappingObjectListArgsType) =>
                    rawConfig.objectTypes.get({...args, executeAction}),
            },
        };

        return (
            <FieldMappingField
                config={config}
                label={input.label}
                onChange={(value) => handleInputChange(workflowUuid, input.name, value)}
                required={input.required}
                value={input.value as import('./types').FieldMappingValueType | undefined}
            />
        );
    }
```

Add the import at the top of `ConnectDialog.tsx`:

```ts
import FieldMappingField from './FieldMappingField';
```

> `handleInputChange` (a.k.a. `handleWorkflowInputChange`) currently expects a string; the field-mapping value is an object. For the `onChange` line above to typecheck, **first** widen its signature and the `inputOverrides` state value type to `unknown` (Step 3 below) — `inputOverrides` is typed `Record<string, Record<string, string | Record<string, string>>>`; change the innermost value to `unknown`. The debounced PUT serializes `mergedInputs` verbatim, so this is a type-only change with no runtime effect.

- [ ] **Step 2: Add `mapObjectFields` to the hook props and build `executeAction`**

In `index.tsx`:

- Add `mapObjectFields?: MapObjectFieldsType;` to `UseConnectDialogProps` and destructure it.
- Compute `const externalUserId = useMemo(() => decodeJwtSubject(jwtToken), [jwtToken]);`
- Build the bound helper: `const executeAction = useExecuteAction(fetch, externalUserId, currentIntegrationInstanceId);`
- Pass both into the rendered `<ConnectDialog ... executeAction={executeAction} mapObjectFields={mapObjectFields} />` (props block at lines 926-951).

Add imports:

```ts
import useExecuteAction from './useExecuteAction';
import {decodeJwtSubject} from './utils';
import {MapObjectFieldsType} from './types';
```

- [ ] **Step 3: Widen `handleWorkflowInputChange` value type if needed**

If TS errors on storing an object value, change the `inputOverrides` state type and the `handleWorkflowInputChange` signature to accept `unknown` for the value (the debounced PUT already serializes `mergedInputs` verbatim, so no runtime change is required).

- [ ] **Step 4: Typecheck + run the SDK test suite**

Run: `npx vitest run src/components/connect-dialog` and `npx tsc --noEmit -p sdks/frontend/embedded/library/react/tsconfig.json`
Expected: all pass; existing ConnectDialog tests still green.

- [ ] **Step 5: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.tsx sdks/frontend/embedded/library/react/src/components/connect-dialog/index.tsx
git commit -m "520 client - Render field-mapping inputs in ConnectDialog with bound executeAction"
```

---

## Phase C — InputEditor (main client)

> Working directory: `client/`. Run `npm run check` before commits.

### Task C1: Add `field_mapping` to the input type dropdown

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx:193-209`
- Test: `client/src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.test.tsx`

- [ ] **Step 1: Write the failing test**

Add to `WorkflowInputsEditDialog.test.tsx`:

```tsx
    it('offers a Field Mapping input type', async () => {
        const user = setupUser();

        render(<Harness />);

        await user.click(screen.getByRole('combobox'));

        expect(screen.getByRole('option', {name: 'Field Mapping'})).toBeInTheDocument();
    });
```

> If the type select isn't the first/only `combobox`, scope the query (e.g. `within(screen.getByLabelText('Type'))`) to match the existing test style.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.test.tsx -t "Field Mapping"`
Expected: FAIL — option not found.

- [ ] **Step 3: Add the SelectItem**

In the `<SelectContent>` (lines 193-209), add (keep `component` first, then the new entry grouped sensibly — place it after `date_time`, before `integer`, to stay readable):

```tsx
                                                <SelectItem value="field_mapping">Field Mapping</SelectItem>
```

- [ ] **Step 4: Run test to verify it passes**

Run: same command as Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
cd client && npm run check
git add src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.test.tsx
git commit -m "732 client - Add Field Mapping option to workflow input type dropdown"
```

---

### Task C2: Render a JSON test-value editor for `field_mapping`

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx` (test-value block at lines 366-418)
- Test: `WorkflowInputsEditDialog.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
    it('renders a JSON editor for the field_mapping test value', async () => {
        const user = setupUser();

        render(<Harness />);

        await user.click(screen.getByRole('combobox'));
        await user.click(screen.getByRole('option', {name: 'Field Mapping'}));

        expect(screen.getByText('Test Value')).toBeInTheDocument();
        expect(screen.getByTestId('field-mapping-json-editor')).toBeInTheDocument();
    });
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.test.tsx -t "JSON editor"`
Expected: FAIL — testid not present.

- [ ] **Step 3: Add the branch**

In the test-value rendering, add a `field_mapping` branch. The component test value is rendered by the `selectedType === 'component'` ternary at line 366; restructure to handle three cases. Add this branch as the first condition:

```tsx
                        {selectedType === 'field_mapping' ? (
                            <fieldset className="space-y-2 border-0 p-0">
                                <FormLabel>Test Value</FormLabel>

                                <FormField
                                    control={form.control}
                                    name="testValue"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormControl>
                                                <div data-testid="field-mapping-json-editor">
                                                    <PropertyCodeEditor
                                                        language="json"
                                                        name="testValue"
                                                        onChange={(value) => field.onChange(value ?? '')}
                                                        value={field.value ?? ''}
                                                        workflow={workflow}
                                                        workflowNodeName="inputs"
                                                    />
                                                </div>
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <p className="text-sm text-content-neutral-secondary">
                                    Static mapObjectFields-shaped sample; the top-level key is the object name.
                                </p>
                            </fieldset>
                        ) : selectedType === 'component' ? (
```

Add the import:

```tsx
import PropertyCodeEditor from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditor';
```

> If `PropertyCodeEditor` requires props beyond those shown (the explorer listed `label`, `description`, `error`, `required` as optional), pass only the required ones; check the component's prop types and supply `language="json"`, `name`, `onChange`, `value`, `workflow`, `workflowNodeName`. If it hard-requires a non-empty `workflowNodeName` that must match a real node, substitute a plain `<textarea {...field} data-testid="field-mapping-json-editor" />` — the sample is just JSON text.

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
cd client && npm run check
git add src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.test.tsx
git commit -m "732 client - Render JSON test-value editor for field_mapping inputs"
```

---

### Task C3: Derive `objectName` from the test JSON on save

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/workflow-inputs/hooks/useWorkflowInputs.ts:140-172` (`saveWorkflowInput`)
- Test: `client/src/pages/platform/workflow-editor/components/workflow-inputs/hooks/useWorkflowInputs.test.ts` (create if absent) OR extend an existing hook test

- [ ] **Step 1: Write the failing test**

Add a focused unit test for the derivation helper (extract it so it's testable). Create `client/src/pages/platform/workflow-editor/components/workflow-inputs/utils/deriveObjectName.ts` test:

`deriveObjectName.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import deriveObjectName from './deriveObjectName';

describe('deriveObjectName', () => {
    it('returns the single top-level key of the mapObjectFields JSON', () => {
        expect(deriveObjectName('{"Contacts": {"applicationFields": []}}')).toBe('Contacts');
    });

    it('unwraps a mapObjectFields envelope', () => {
        expect(deriveObjectName('{"mapObjectFields": {"Leads": {}}}')).toBe('Leads');
    });

    it('returns undefined for invalid JSON', () => {
        expect(deriveObjectName('not json')).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/workflow-inputs/utils/deriveObjectName.test.ts`
Expected: FAIL — module missing.

- [ ] **Step 3: Implement the helper**

`deriveObjectName.ts`:

```ts
/**
 * Derives the field-mapping object name from a static mapObjectFields-shaped test value. The object name is the single
 * top-level key (optionally inside a `mapObjectFields` envelope). Returns `undefined` when the JSON is invalid or empty.
 */
export default function deriveObjectName(testValue: string | undefined): string | undefined {
    if (!testValue) {
        return undefined;
    }

    try {
        const parsed = JSON.parse(testValue) as Record<string, unknown>;
        const root = (parsed.mapObjectFields as Record<string, unknown>) ?? parsed;
        const keys = Object.keys(root);

        return keys.length > 0 ? keys[0] : undefined;
    } catch {
        return undefined;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2.
Expected: PASS.

- [ ] **Step 5: Use it in `saveWorkflowInput`**

In `useWorkflowInputs.ts`, at the top of `saveWorkflowInput` (after `delete input['testValue']` is currently called — but we need the test value first), adjust so the test value is read *before* deletion and `objectName` is set for field-mapping inputs:

```ts
    function saveWorkflowInput(input: WorkflowInputType) {
        const {getValues} = form;

        const testValue = getValues().testValue;

        if (input.type === 'field_mapping') {
            input.objectName = deriveObjectName(testValue);
        }

        delete input['testValue'];

        // ...unchanged from here...
```

Ensure `toWorkflowDefinitionInput` writes `objectName` into the definition map (add `objectName: input.objectName` to the object it builds for field-mapping inputs). Add the import:

```ts
import deriveObjectName from '../utils/deriveObjectName';
```

> `WorkflowInputType` needs an optional `objectName`. In `client/src/shared/types.ts` the type is `WorkflowInput & {testValue?: string}`; the generated `WorkflowInput` gets `objectName` automatically once `client/src/shared/middleware/platform/configuration` is regenerated from the platform OpenAPI. If the platform-side `WorkflowInput` schema does not yet include `objectName`, add it to `client/src/shared/types.ts`: `WorkflowInput & {objectName?: string; testValue?: string}`.

- [ ] **Step 6: Run the hook/dialog tests**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/workflow-inputs`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
cd client && npm run check
git add src/pages/platform/workflow-editor/components/workflow-inputs src/shared/types.ts
git commit -m "732 client - Derive field-mapping objectName from test value on save"
```

---

## Phase D — Data pills

### Task D1: Expand `applicationFields` into child pills for `FIELD_MAPPING` inputs

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/getFieldMappingPillProperties.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/getFieldMappingPillProperties.test.ts` (create)
- Modify: `client/src/pages/platform/workflow-editor/components/datapills/DataPillPanelBodyInputsItem.tsx:40-56`

- [ ] **Step 1: Write the failing test for the helper**

```ts
import {describe, expect, it} from 'vitest';

import getFieldMappingPillProperties from './getFieldMappingPillProperties';

describe('getFieldMappingPillProperties', () => {
    it('builds one child property per applicationFields entry', () => {
        const sample = JSON.stringify({
            Contacts: {applicationFields: [{label: 'Title', value: 'title'}, {label: 'Email', value: 'email'}]},
        });

        expect(getFieldMappingPillProperties(sample)).toEqual([
            {label: 'Title', name: 'title', type: 'STRING'},
            {label: 'Email', name: 'email', type: 'STRING'},
        ]);
    });

    it('returns an empty array for invalid or empty samples', () => {
        expect(getFieldMappingPillProperties(undefined)).toEqual([]);
        expect(getFieldMappingPillProperties('nope')).toEqual([]);
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/getFieldMappingPillProperties.test.ts`
Expected: FAIL — module missing.

- [ ] **Step 3: Implement the helper**

```ts
import {PropertyType} from '@/shared/middleware/platform/configuration';

interface FieldMappingPillPropertyType {
    label: string;
    name: string;
    type: PropertyType;
}

/**
 * Builds synthetic child properties (one per applicationFields entry) from a field-mapping input's static test value,
 * so the data-pill panel renders a pill per application field. Accepts the mapObjectFields-shaped JSON (optionally
 * inside a `mapObjectFields` envelope); returns `[]` on invalid/empty input.
 */
export default function getFieldMappingPillProperties(
    testValue: string | undefined
): Array<FieldMappingPillPropertyType> {
    if (!testValue) {
        return [];
    }

    try {
        const parsed = JSON.parse(testValue) as Record<string, {applicationFields?: Array<{label: string; value: string}>}>;
        const root = (parsed.mapObjectFields as typeof parsed) ?? parsed;
        const firstKey = Object.keys(root)[0];

        if (!firstKey) {
            return [];
        }

        const applicationFields = root[firstKey]?.applicationFields ?? [];

        return applicationFields.map((field) => ({label: field.label, name: field.value, type: PropertyType.String}));
    } catch {
        return [];
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2.
Expected: PASS.

- [ ] **Step 5: Write the failing component test**

Add to a new `DataPillPanelBodyInputsItem.test.tsx` (mirror the store/query mock pattern from existing data-pill or workflow-editor tests):

```tsx
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import DataPillPanelBodyInputsItem from './DataPillPanelBodyInputsItem';

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationQuery: () => ({
        data: {inputs: {contactMapping: JSON.stringify({Contacts: {applicationFields: [{label: 'Title', value: 'title'}]}})}},
    }),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: {workflow: unknown}) => unknown) =>
        selector({workflow: {id: 'w1', inputs: [{name: 'contactMapping', type: 'field_mapping'}]}}),
}));

describe('DataPillPanelBodyInputsItem', () => {
    it('renders a child pill per applicationFields entry for a field_mapping input', () => {
        render(<DataPillPanelBodyInputsItem />);

        expect(screen.getByText('Title')).toBeInTheDocument();
    });
});
```

> Match the actual mock surface of the existing tests (the explorer showed these exact stores/queries are imported). Adjust mock shapes if the real `DataPill` requires more context; the assertion (a `Title` pill appears) is the contract.

- [ ] **Step 6: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/datapills/DataPillPanelBodyInputsItem.test.tsx`
Expected: FAIL — no `Title` pill (child expansion not wired).

- [ ] **Step 7: Wire the helper into the pill item**

In `DataPillPanelBodyInputsItem.tsx`, inside the `.map`, build `properties` for field-mapping inputs and pass them on the `property`:

```tsx
                    {workflow.inputs.map((input, index) => {
                        const sampleOutput = workflowTestConfiguration?.inputs?.[input.name];

                        const properties =
                            input.type === 'field_mapping'
                                ? getFieldMappingPillProperties(sampleOutput as string | undefined)
                                : undefined;

                        return (
                            <li className="flex w-full items-center space-x-3" key={`${input.name}-${index}`}>
                                <DataPill
                                    property={{
                                        name: input.name,
                                        properties,
                                        type: input.type?.toUpperCase() as PropertyType,
                                    }}
                                    root
                                    sampleOutput={sampleOutput}
                                    workflowNodeName={input.name}
                                />
                            </li>
                        );
                    })}
```

Add the import:

```tsx
import getFieldMappingPillProperties from '../../utils/getFieldMappingPillProperties';
```

> `DataPill` reads `property?.properties || property?.items` to compute `subProperties` (DataPill.tsx:132), so providing `properties` makes each application field render as a child pill. `properties` is `undefined` for every non-field-mapping input, so existing pills are unchanged. The `property` object keys must be alphabetical (`name`, `properties`, `type`) per ESLint `sort-keys`.

- [ ] **Step 8: Run test to verify it passes**

Run: same as Step 6.
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
cd client && npm run check
git add src/pages/platform/workflow-editor/utils/getFieldMappingPillProperties.ts src/pages/platform/workflow-editor/utils/getFieldMappingPillProperties.test.ts src/pages/platform/workflow-editor/components/datapills/DataPillPanelBodyInputsItem.tsx src/pages/platform/workflow-editor/components/datapills/DataPillPanelBodyInputsItem.test.tsx
git commit -m "732 client - Render field-mapping application fields as data pills"
```

---

## Final verification

- [ ] **Server:** `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test :server:ee:libs:embedded:embedded-execution:embedded-execution-service:test` — all green.
- [ ] **SDK:** from `sdks/frontend/embedded/library/react`, `npx vitest run src/components/connect-dialog` — all green.
- [ ] **Client:** `cd client && npm run check` — lint, typecheck, tests green.
- [ ] **Manual smoke (optional, requires running stack):** create a `field_mapping` input in the embedded workflow builder with test value `{"Contacts":{"objectTypes":[...],"integrationFields":[...],"applicationFields":[{"label":"Title","value":"title"}]}}`; confirm `contactMapping.title` appears as a data pill. In a test embedding app, supply `mapObjectFields.Contacts` with `objectTypes.get`/`integrationFields.get` calling `executeAction(...)`; open ConnectDialog; confirm the object-type select loads, integration-field selects populate after selection, and the mapping persists.

---

## Notes for the executor

- **Generated code (Task A2):** the embedded `InputModel`/`InputTypeModel` are OpenAPI-generated. Edit `openapi.yaml`, regenerate, and commit the regenerated files together. Do not hand-edit files under `generated/` — they will be overwritten.
- **Liquibase:** none required; the mapping is stored in the existing workflow-instance `inputs` map and the sample in `WorkflowTestConfiguration.inputs` — both already JSON columns.
- **EE headers:** every new file under `server/ee/**` needs the Enterprise license header and `@version ee` (Spotless selects the header by content — see the existing files in each module).
- **Commit prefixes:** server commits `732 <desc>`; client/SDK commits `<ticket> client - <desc>`.
