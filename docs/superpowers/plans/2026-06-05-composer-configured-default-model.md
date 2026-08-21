# Composer Default Model + Hub Provider + AI Hub Visibility — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** (1) Show the deployment-configured default chat model in the AI Hub / Copilot composer picker (display-only, surface-aware), (2) add `bytechef.ai.hub.provider` via a shared `ChatProvider` enum, (3) gate the AI Hub UI on `bytechef.ai.hub.enabled`.

**Architecture:** A surface-aware backend read `AiProviderFacade.getAiDefaultModel(environment, surface)` resolves the default from `copilot.provider`/`hub.provider` + `provider.chat.<provider>.options.model`, enabled-gated like the catalog, exposed via GraphQL `aiDefaultModel(environment, surface)`. Composers pass display-only `defaultProvider`/`defaultModel` into `ModelPicker`. Separately the client reads the already-exposed `info.ai.hub.enabled` and gates the AI Hub nav/route.

**Tech Stack:** Java 25 / Spring Boot, Spring GraphQL, JUnit 5 + Mockito + AssertJ; React 19 + TypeScript, GraphQL Codegen, Vitest, Zustand.

**Spec:** `docs/superpowers/specs/2026-06-05-composer-configured-default-model-design.md`

---

## Task 1: Part 1 — commit the `ChatProvider` + `Hub.provider` config (already coded)

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`

The edit is already applied: a shared `Ai.ChatProvider` enum (`OPENAI`, `ANTHROPIC`), `Hub.provider`
(`ChatProvider`, default `OPENAI`) with getter/setter, and `Copilot.provider` retyped to `ChatProvider`.

- [ ] **Step 1: Verify it compiles**

Run: `./gradlew :server:libs:config:app-config:compileJava -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Commit**

```bash
git add server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java
git commit -m "732 Add shared ChatProvider enum and bytechef.ai.hub.provider config"
```

---

## Task 2: Backend — `AiDefaultModelDTO` + `AiChatSurface`

**Files:**
- Create: `server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/dto/AiDefaultModelDTO.java`
- Create: `server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/dto/AiChatSurface.java`

- [ ] **Step 1: Create the DTO**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.dto;

/**
 * The deployment-configured default chat (provider, model) surfaced to a composer model picker so it can display the
 * model the server will use when no explicit selection is sent. {@code provider} is the {@code Provider.getKey()}
 * (e.g. {@code ai.provider.anthropic}); {@code model} is the model name (e.g. {@code claude-sonnet-4-6}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiDefaultModelDTO(String provider, String model) {
}
```

- [ ] **Step 2: Create the surface enum**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.dto;

/**
 * Which composer surface is asking for its configured default chat model. Selects the default provider source:
 * {@code COPILOT} → {@code bytechef.ai.copilot.provider}; {@code HUB} → {@code bytechef.ai.hub.provider}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiChatSurface {
    COPILOT, HUB
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/dto/AiDefaultModelDTO.java \
        server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/dto/AiChatSurface.java
git commit -m "732 Add AiDefaultModelDTO and AiChatSurface"
```

---

## Task 3: Backend — facade method (surface-aware, TDD)

**Files:**
- Modify: `.../platform-configuration-api/.../facade/AiProviderFacade.java`
- Modify: `.../platform-configuration-service/.../facade/AiProviderFacadeImpl.java`
- Test: `.../platform-configuration-service/src/test/java/.../facade/AiProviderFacadeDefaultModelTest.java`

- [ ] **Step 1: Add the interface method**

In `AiProviderFacade.java`, add imports and the method after `getAiProviderCatalog`:

```java
import com.bytechef.ee.platform.configuration.dto.AiChatSurface;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
```

```java
    /**
     * The deployment-configured default chat model for the given composer surface, or {@code null} when that
     * surface's default provider is not enabled or has no configured chat model.
     */
    AiDefaultModelDTO getAiDefaultModel(int environment, AiChatSurface surface);
```

- [ ] **Step 2: Write the failing test**

Create `AiProviderFacadeDefaultModelTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.ChatProvider;
import com.bytechef.ee.platform.configuration.dto.AiChatSurface;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pins the contract of {@link AiProviderFacadeImpl#getAiDefaultModel} — the display-only configured default chat
 * model surfaced to the composer picker, resolved per surface (Copilot vs AI Hub).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiProviderFacadeDefaultModelTest {

    private static final int ENVIRONMENT = 1;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private PropertyService propertyService;

    private ApplicationProperties applicationProperties;

    private AiProviderFacadeImpl facade;

    @BeforeEach
    void setUp() {
        applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

        facade = new AiProviderFacadeImpl(componentDefinitionService, propertyService, applicationProperties);
    }

    private void stubNoDbProperties() {
        when(propertyService.getProperties(
            ArgumentMatchers.anyList(), ArgumentMatchers.eq(Scope.PLATFORM), ArgumentMatchers.isNull(),
            ArgumentMatchers.eq((long) ENVIRONMENT)))
                .thenReturn(List.of());
    }

    @Test
    void testCopilotSurfaceReturnsConfiguredAnthropicDefault() {
        when(applicationProperties.getAi()
            .getCopilot()
            .getProvider()).thenReturn(ChatProvider.ANTHROPIC);
        when(applicationProperties.getAi()
            .getProvider()
            .getAnthropic()
            .getApiKey()).thenReturn("sk-anthropic");
        when(applicationProperties.getAi()
            .getProvider()
            .getChat()
            .getAnthropic()
            .getOptions()
            .getModel()).thenReturn("claude-sonnet-4-6");
        stubNoDbProperties();

        AiDefaultModelDTO result = facade.getAiDefaultModel(ENVIRONMENT, AiChatSurface.COPILOT);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo("ai.provider.anthropic");
        assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
    }

    @Test
    void testHubSurfaceReturnsConfiguredOpenAiDefault() {
        when(applicationProperties.getAi()
            .getHub()
            .getProvider()).thenReturn(ChatProvider.OPENAI);
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn("sk-openai");
        when(applicationProperties.getAi()
            .getProvider()
            .getChat()
            .getOpenAi()
            .getOptions()
            .getModel()).thenReturn("gpt-5.1");
        stubNoDbProperties();

        AiDefaultModelDTO result = facade.getAiDefaultModel(ENVIRONMENT, AiChatSurface.HUB);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo("ai.provider.openAi");
        assertThat(result.model()).isEqualTo("gpt-5.1");
    }

    @Test
    void testReturnsDefaultWhenEnabledViaDbPropertyWithoutConfigApiKey() {
        Property property = mock(Property.class);
        when(property.getKey()).thenReturn("ai.provider.anthropic");
        when(property.isEnabled()).thenReturn(true);

        when(applicationProperties.getAi()
            .getCopilot()
            .getProvider()).thenReturn(ChatProvider.ANTHROPIC);
        lenient().when(applicationProperties.getAi()
            .getProvider()
            .getAnthropic()
            .getApiKey()).thenReturn(null);
        when(applicationProperties.getAi()
            .getProvider()
            .getChat()
            .getAnthropic()
            .getOptions()
            .getModel()).thenReturn("claude-sonnet-4-6");

        when(propertyService.getProperties(
            ArgumentMatchers.anyList(), ArgumentMatchers.eq(Scope.PLATFORM), ArgumentMatchers.isNull(),
            ArgumentMatchers.eq((long) ENVIRONMENT)))
                .thenReturn(List.of(property));

        AiDefaultModelDTO result = facade.getAiDefaultModel(ENVIRONMENT, AiChatSurface.COPILOT);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo("ai.provider.anthropic");
    }

    @Test
    void testReturnsNullWhenDefaultProviderNotEnabled() {
        when(applicationProperties.getAi()
            .getCopilot()
            .getProvider()).thenReturn(ChatProvider.ANTHROPIC);
        when(applicationProperties.getAi()
            .getProvider()
            .getAnthropic()
            .getApiKey()).thenReturn(null);
        stubNoDbProperties();

        AiDefaultModelDTO result = facade.getAiDefaultModel(ENVIRONMENT, AiChatSurface.COPILOT);

        assertThat(result).isNull();
    }

    @Test
    void testReturnsNullWhenModelNotConfigured() {
        when(applicationProperties.getAi()
            .getCopilot()
            .getProvider()).thenReturn(ChatProvider.ANTHROPIC);
        when(applicationProperties.getAi()
            .getProvider()
            .getAnthropic()
            .getApiKey()).thenReturn("sk-anthropic");
        when(applicationProperties.getAi()
            .getProvider()
            .getChat()
            .getAnthropic()
            .getOptions()
            .getModel()).thenReturn("  ");
        stubNoDbProperties();

        AiDefaultModelDTO result = facade.getAiDefaultModel(ENVIRONMENT, AiChatSurface.COPILOT);

        assertThat(result).isNull();
    }
}
```

- [ ] **Step 3: Run the test (expect compile failure / method missing)**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test --tests "*AiProviderFacadeDefaultModelTest"`
Expected: FAIL — `getAiDefaultModel(int, AiChatSurface)` not implemented.

- [ ] **Step 4: Implement in `AiProviderFacadeImpl`**

Add imports:

```java
import com.bytechef.config.ApplicationProperties.Ai.ChatProvider;
import com.bytechef.ee.platform.configuration.dto.AiChatSurface;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
```

Add the methods (place `getAiDefaultModel` directly after `getAiProviderCatalog`, and the private
helpers near `hasConfigApiKey`):

```java
    @Override
    @Transactional(readOnly = true)
    public AiDefaultModelDTO getAiDefaultModel(int environment, AiChatSurface surface) {
        ChatProvider chatProvider = switch (surface) {
            case COPILOT -> applicationProperties.getAi()
                .getCopilot()
                .getProvider();
            case HUB -> applicationProperties.getAi()
                .getHub()
                .getProvider();
        };

        Provider provider = switch (chatProvider) {
            case OPENAI -> Provider.OPEN_AI;
            case ANTHROPIC -> Provider.ANTHROPIC;
        };

        List<Property> properties = propertyService.getProperties(
            List.of(provider.getKey()), Scope.PLATFORM, null, (long) environment);

        Property property = properties.stream()
            .filter(curProperty -> curProperty.getKey()
                .equals(provider.getKey()))
            .findFirst()
            .orElse(null);

        boolean enabled = (property != null && property.isEnabled()) || hasConfigApiKey(provider);

        if (!enabled) {
            return null;
        }

        String model = resolveDefaultModel(provider);

        if (model == null || model.isBlank()) {
            return null;
        }

        return new AiDefaultModelDTO(provider.getKey(), model);
    }

    private String resolveDefaultModel(Provider provider) {
        ApplicationProperties.Ai.Provider.Chat chat = applicationProperties.getAi()
            .getProvider()
            .getChat();

        return switch (provider) {
            case OPEN_AI -> chat.getOpenAi()
                .getOptions()
                .getModel();
            case ANTHROPIC -> chat.getAnthropic()
                .getOptions()
                .getModel();
            default -> null;
        };
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test --tests "*AiProviderFacadeDefaultModelTest"`
Expected: PASS (5 tests)

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacade.java \
        server/ee/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImpl.java \
        server/ee/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeDefaultModelTest.java
git commit -m "732 Resolve surface-aware configured default chat model in AiProviderFacade"
```

---

## Task 4: Backend — GraphQL query (TDD)

**Files:**
- Modify: `.../automation-ai-gateway-graphql/src/main/resources/graphql/ai-provider-catalog.graphqls`
- Modify: `.../automation-ai-gateway-graphql/.../web/graphql/AiProviderCatalogGraphQlController.java`
- Test: `.../automation-ai-gateway-graphql/src/test/.../AiProviderCatalogGraphQlControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

Add to `AiProviderCatalogGraphQlControllerTest` (add imports
`import com.bytechef.ee.platform.configuration.dto.AiChatSurface;` and
`import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;`):

```java
    @Test
    void testAiDefaultModelDelegatesToFacade() {
        when(aiProviderFacade.getAiDefaultModel(2, AiChatSurface.COPILOT))
            .thenReturn(new AiDefaultModelDTO("ai.provider.anthropic", "claude-sonnet-4-6"));

        AiProviderCatalogGraphQlController controller = new AiProviderCatalogGraphQlController(aiProviderFacade);

        AiDefaultModelDTO result = controller.aiDefaultModel(2L, AiChatSurface.COPILOT);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo("ai.provider.anthropic");
        assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:test --tests "*AiProviderCatalogGraphQlControllerTest"`
Expected: FAIL — `aiDefaultModel` method missing.

- [ ] **Step 3: Add the resolver method**

In `AiProviderCatalogGraphQlController.java` add imports
`import com.bytechef.ee.platform.configuration.dto.AiChatSurface;` and
`import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;`, and the method after
`aiProviderCatalog`:

```java
    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiDefaultModelDTO aiDefaultModel(@Argument Long environment, @Argument AiChatSurface surface) {
        return aiProviderFacade.getAiDefaultModel(environment.intValue(), surface);
    }
```

- [ ] **Step 4: Add the schema**

Append to `ai-provider-catalog.graphqls`:

```graphql
extend type Query {
    aiDefaultModel(environment: ID!, surface: AiChatSurface!): AiDefaultModel
}

type AiDefaultModel {
    provider: String!
    model: String!
}

enum AiChatSurface {
    COPILOT
    HUB
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:test --tests "*AiProviderCatalogGraphQlControllerTest"`
Expected: PASS

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-provider-catalog.graphqls \
        server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiProviderCatalogGraphQlController.java \
        server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/test/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiProviderCatalogGraphQlControllerTest.java
git commit -m "732 Add surface-aware aiDefaultModel GraphQL query"
```

---

## Task 5: Frontend — GraphQL operation + codegen

**Files:**
- Create: `client/src/graphql/platform/ai-providers/aiDefaultModel.graphql`
- Modify (generated): `client/src/shared/middleware/graphql.ts`, `graphql-types.ts`

- [ ] **Step 1: Create the operation**

```graphql
query aiDefaultModel($environment: ID!, $surface: AiChatSurface!) {
    aiDefaultModel(environment: $environment, surface: $surface) {
        provider
        model
    }
}
```

- [ ] **Step 2: Regenerate**

Run: `cd client && npx graphql-codegen`
Expected: completes; `useAiDefaultModelQuery` + `AiChatSurface` enum generated.

- [ ] **Step 3: Note the generated enum member names**

Run: `cd client && grep -n "enum AiChatSurface" src/shared/middleware/graphql-types.ts`
Expected: an enum like `export enum AiChatSurface { Copilot = 'COPILOT', Hub = 'HUB' }`. Note the exact
member identifiers (`AiChatSurface.Copilot` / `AiChatSurface.Hub`) — used in Task 7.

- [ ] **Step 4: Commit (operation + generated separately)**

```bash
git add client/src/graphql/platform/ai-providers/aiDefaultModel.graphql
git commit -m "732 client - Add aiDefaultModel GraphQL operation"
git add client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "732 client - Regenerate GraphQL types for aiDefaultModel"
```

---

## Task 6: Frontend — `ModelPicker` default props (TDD)

**Files:**
- Modify: `client/src/shared/components/ai/model-picker/ModelPicker.tsx`
- Test: `client/src/shared/components/ai/model-picker/ModelPicker.test.tsx`

- [ ] **Step 1: Write the failing tests**

Add to `ModelPicker.test.tsx`:

```tsx
    it('shows the configured default model in the trigger when nothing is selected', () => {
        render(
            <ModelPicker
                defaultModel="gpt-4o"
                defaultProvider="ai.provider.openAi"
                environment={1}
                onChange={vi.fn()}
                selectedModel={null}
                selectedProvider={null}
            />
        );

        expect(screen.getByText('GPT-4o')).toBeInTheDocument();
        expect(screen.queryByText('Select model')).not.toBeInTheDocument();
    });

    it('falls back to "Select model" when no default and no selection', () => {
        render(<ModelPicker environment={1} onChange={vi.fn()} selectedModel={null} selectedProvider={null} />);

        expect(screen.getByText('Select model')).toBeInTheDocument();
    });
```

- [ ] **Step 2: Run to verify failure**

Run: `cd client && npx vitest run src/shared/components/ai/model-picker/ModelPicker.test.tsx -t "configured default"`
Expected: FAIL — props not accepted; trigger shows "Select model".

- [ ] **Step 3: Add the props to `ModelPickerPropsI`** (alphabetical, after `agentDefaultProvider`)

```tsx
    /** Deployment-configured default (provider key + model name). Display-only: shown when no explicit
     * selection exists; the picker still sends null so the server resolves the default. */
    defaultModel?: string | null;
    defaultProvider?: string | null;
```

- [ ] **Step 4: Destructure** (alphabetical, after `agentDefaultProvider`)

```tsx
    defaultModel,
    defaultProvider,
```

- [ ] **Step 5: Add the precedence branch in `triggerContent`** (between the `agentDefault*` block and the final `return`)

```tsx
        if (defaultProvider && defaultModel) {
            const provider = providers.find((candidate) => candidate.key === defaultProvider);
            const model = provider?.models.find((candidate) => candidate.name === defaultModel);

            return {icon: provider?.icon ?? null, label: model?.label || defaultModel};
        }

        return {icon: null, label: workspaceDefaultLabel ?? 'Select model'};
```

And add `defaultModel`, `defaultProvider` to the `triggerContent` `useMemo` deps (alphabetical):

```tsx
    }, [agentDefaultModel, agentDefaultProvider, defaultModel, defaultProvider, providers, selectedModel, selectedProvider, workspaceDefaultLabel]);
```

- [ ] **Step 6: Run tests**

Run: `cd client && npx vitest run src/shared/components/ai/model-picker/ModelPicker.test.tsx`
Expected: PASS

- [ ] **Step 7: Lint/typecheck + commit**

```bash
cd client && npx eslint src/shared/components/ai/model-picker/ModelPicker.tsx src/shared/components/ai/model-picker/ModelPicker.test.tsx && npx tsc --noEmit
cd /Volumes/Data/bytechef/bytechef
git add client/src/shared/components/ai/model-picker/ModelPicker.tsx client/src/shared/components/ai/model-picker/ModelPicker.test.tsx
git commit -m "732 client - Add display-only configured-default props to ModelPicker"
```

---

## Task 7: Frontend — wire composers with surface

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubHomePanel.tsx`
- Modify: `client/src/pages/automation/ai-hub/AiHubPanel.tsx`
- Modify: `client/src/shared/components/copilot/CopilotPanel.tsx`

Each composer already reads `currentEnvironmentId`. Add the import, the query (gated
`currentEnvironmentId >= 0`), and the two props. Use the generated `AiChatSurface` enum member names
from Task 5 Step 3 (shown below as `AiChatSurface.Hub` / `AiChatSurface.Copilot` — adjust if codegen
produced different casing).

- [ ] **Step 1: AiHubHomePanel (surface HUB)**

Add imports:

```tsx
import {AiChatSurface} from '@/shared/middleware/graphql-types';
import {useAiDefaultModelQuery} from '@/shared/middleware/graphql';
```

After the `currentEnvironmentId` line:

```tsx
    const {data: defaultModelData} = useAiDefaultModelQuery(
        {environment: currentEnvironmentId >= 0 ? String(currentEnvironmentId) : '', surface: AiChatSurface.Hub},
        {enabled: currentEnvironmentId >= 0}
    );
```

In `<ModelPicker>` (alphabetical, before `environment`):

```tsx
                                    defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                    defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
```

- [ ] **Step 2: AiHubPanel (surface HUB)**

Same imports + query as Step 1. In `<ModelPicker>` (which already passes `agentDefault*`), add
(alphabetical, after `agentDefaultProvider`, before `environment`):

```tsx
                                defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
```

(`agentDefault*` still wins over `default*` per the precedence in Task 6.)

- [ ] **Step 3: CopilotPanel (surface COPILOT)**

Add the same imports. After the `currentEnvironmentId` line:

```tsx
    const {data: defaultModelData} = useAiDefaultModelQuery(
        {environment: currentEnvironmentId >= 0 ? String(currentEnvironmentId) : '', surface: AiChatSurface.Copilot},
        {enabled: currentEnvironmentId >= 0}
    );
```

In `<ModelPicker>` (alphabetical, before `environment`):

```tsx
                                    defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                    defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
```

- [ ] **Step 4: Lint + typecheck + tests**

Run: `cd client && npm run check`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubHomePanel.tsx \
        client/src/pages/automation/ai-hub/AiHubPanel.tsx \
        client/src/shared/components/copilot/CopilotPanel.tsx
git commit -m "732 client - Show configured default model in AI Hub and Copilot composers"
```

---

## Task 8: Part 3 — gate AI Hub UI on `ai.hub.enabled`

**Files:**
- Modify: `client/src/shared/stores/useApplicationInfoStore.tsx`
- Test: `client/src/shared/stores/tests/useApplicationInfoStore.test.ts`
- Modify: `client/src/App.tsx`

`info.ai.hub.enabled` is already exposed by actuator (`application.yml:16-17`); only the client needs
to read and use it.

- [ ] **Step 1: Write the failing store test**

Add to `useApplicationInfoStore.test.ts` a case that mocks `/actuator/info` returning
`ai: { hub: { enabled: 'true' }, copilot: {enabled:'false'}, gateway:{enabled:'false'}, knowledgeBase:{enabled:'false'} }`
(mirror the existing test's fetch mock) and asserts `applicationInfoStore.getState().ai.hub.enabled === true`.
Follow the existing test's structure for mocking `fetch` and calling `getApplicationInfo()`.

- [ ] **Step 2: Run to verify failure**

Run: `cd client && npx vitest run src/shared/stores/tests/useApplicationInfoStore.test.ts`
Expected: FAIL — `ai.hub` does not exist on the typed state.

- [ ] **Step 3: Add `hub` to the interface** (`ApplicationInfoI.ai`, alphabetical: after `gateway`, before `knowledgeBase`)

```tsx
        gateway: {
            enabled: boolean;
        };
        hub: {
            enabled: boolean;
        };
        knowledgeBase: {
            enabled: boolean;
        };
```

- [ ] **Step 4: Add `hub` to initial state** (after `gateway`, before `knowledgeBase`)

```tsx
                    gateway: {
                        enabled: false,
                    },
                    hub: {
                        enabled: false,
                    },
                    knowledgeBase: {
                        enabled: false,
                    },
```

- [ ] **Step 5: Parse `hub` from the actuator JSON** (after `gateway`, before `knowledgeBase`)

```tsx
                                gateway: {
                                    enabled: json.ai.gateway?.enabled === 'true',
                                },
                                hub: {
                                    enabled: json.ai.hub?.enabled === 'true',
                                },
                                knowledgeBase: {
                                    enabled: json.ai.knowledgeBase?.enabled === 'true',
                                },
```

- [ ] **Step 6: Run the store test**

Run: `cd client && npx vitest run src/shared/stores/tests/useApplicationInfoStore.test.ts`
Expected: PASS

- [ ] **Step 7: Gate the AI Hub nav in `App.tsx`**

Change the nav gate (currently `ai.copilot.enabled`):

```tsx
        if (navItem.href === '/automation/ai-hub') {
            return edition === EditionType.EE && ai.hub.enabled;
        }
```

- [ ] **Step 8: Guard the route**

Run: `cd client && grep -n "ai-hub" src/App.tsx` and find where the `/automation/ai-hub` `<Route>` is
rendered. If a sibling EE feature (e.g. a route gated by `contextStoreEnabled`) wraps its route in a
conditional, mirror that pattern to render the AI Hub route only when `edition === EditionType.EE && ai.hub.enabled`.
If routes are unconditionally registered (nav-gated only), leave as-is — hiding the nav satisfies
"the whole AI Hub UI will not be visible"; note this in the commit message.

- [ ] **Step 9: Full client check**

Run: `cd client && npm run check`
Expected: PASS

- [ ] **Step 10: Commit**

```bash
git add client/src/shared/stores/useApplicationInfoStore.tsx \
        client/src/shared/stores/tests/useApplicationInfoStore.test.ts \
        client/src/App.tsx
git commit -m "732 client - Gate AI Hub UI on ai.hub.enabled instead of copilot"
```

---

## Task 9: Full verification

- [ ] **Step 1: Backend build/tests for touched modules**

Run: `./gradlew :server:libs:config:app-config:compileJava :server:ee:libs:platform:platform-configuration:platform-configuration-service:test :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Client full check**

Run: `cd client && npm run check`
Expected: PASS

- [ ] **Step 3: Manual smoke**

- Set `bytechef.ai.hub.provider: anthropic`, anthropic api-key (or anthropic enabled on AI Providers
  page), and `bytechef.ai.provider.chat.anthropic.options.model: claude-sonnet-4-6`. Open AI Hub home
  on Development → trigger shows "Claude Sonnet 4.6". Pick a different model → it sticks (last-used).
- Set `bytechef.ai.copilot.provider: openai` + openai default model → Copilot composer shows the
  OpenAI default.
- Set `bytechef.ai.hub.enabled: false` → AI Hub nav item disappears.

---

## Notes for the implementer

- `Provider` = `com.bytechef.component.ai.llm.Provider` (keys `ai.provider.anthropic`, `ai.provider.openAi`).
  `ChatProvider` = `ApplicationProperties.Ai.ChatProvider` (`OPENAI`/`ANTHROPIC`). `AiChatSurface` =
  `com.bytechef.ee.platform.configuration.dto.AiChatSurface` (`COPILOT`/`HUB`). Map between them in the facade.
- `hasConfigApiKey(Provider)` already exists (private) in `AiProviderFacadeImpl`; reuse it.
- Spring GraphQL binds the GraphQL `AiChatSurface` enum to the Java `AiChatSurface` enum by member name.
- Keep client object/prop keys alphabetical (ESLint `sort-keys` / import-destructure rules).
- EE-only backend (`@ConditionalOnEEVersion`); CE returns null for the query like the catalog query.
```
