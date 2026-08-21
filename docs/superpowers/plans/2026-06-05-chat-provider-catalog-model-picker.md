# Chat-Provider Catalog Model Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the composer's AI-Gateway-driven LLM picker (in both AI Hub and Copilot) with a catalog-driven picker that lists all chat-capable providers with icons, shows models only for active providers, links inactive providers to Settings, shows the exact selected provider+model, and actually resolves catalog selections at runtime.

**Architecture:** Three phases. (1) A new USER-safe GraphQL query `aiProviderCatalog(environment)` projects the platform `Provider` catalog (icons, enabled, Spring-AI model lists) without exposing API keys. (2) A new `CatalogChatModelFactory` + `CatalogChatClientResolver` build a Spring-AI `ChatModel` from the catalog's platform API key by reusing each component's existing `CHAT_MODEL` lambda; both `AiHubChatClientResolver` and `CopilotChatClientResolver` try it before the gateway path. (3) The shared `ModelPicker` is rewritten to consume the new query (icons, active/inactive, "Choose model by ID", exact-trigger, last-used seeding); both composers already render this one component.

**Tech Stack:** Java 25 / Spring Boot 4 (EE modules), Spring GraphQL, Spring AI; React 19 / TypeScript, GraphQL codegen, Zustand, react-inlinesvg, Vitest.

**Spec:** `docs/superpowers/specs/2026-06-05-chat-provider-catalog-model-picker-design.md`

---

## Implementation deviations from this plan (recorded post-build, 2026-06-05)

These are the places where the as-built code diverged from the plan as written. Documented honestly so the plan matches reality:

1. **GraphQL host module.** The plan tentatively placed the controller/schema in `platform-configuration-graphql`, but that module is **CE-only** and can't depend on the EE `AiProviderFacade`. The query was hosted in the EE module `automation-ai-gateway-graphql` (already codegen-globbed), package `com.bytechef.ee.automation.ai.gateway.web.graphql`. No `codegen.ts` change was needed (that module's `.graphqls` glob already existed), so Task 3.1 only added the client operation + regenerated.

2. **Provider key mapping.** `Provider`'s constructor is `Provider(int id, String name, String key, String label)`: `getName()` is the component-match (e.g. `"openAi"`), **`getKey()` is the PropertyService storage key AND the catalog wire value (e.g. `"ai.provider.openAi"`)**, `getLabel()` is the display name. The DTO emits `key=getKey()`, `name=getLabel()`. The wire `userSelectedLlmProvider` is therefore `"ai.provider.openAi"`; it cannot collide with the gateway namespace (`"OPENAI"`).

3. **VERTEX_GEMINI matching bug fix.** The pre-existing `provider.getName().contains(componentDefinition.getName())` match is case-sensitive and dropped Gemini (`"vertexGemini".contains("gemini")` is false). `getAiProviderCatalog` uses a case-insensitive **longest-substring** match instead (also prevents AZURE_OPEN_AI cross-matching an `"openai"` component). The pre-existing `getAiProviders` method was left unchanged (out of scope).

4. **Fourth `ModelPicker` caller + optional default sentinel (plan gap).** The plan enumerated only three callers; a **fourth** exists: `AiHubPersonalAgentForm.tsx` (the personal-agent config form). That form genuinely needs a "Use workspace default" sentinel to clear an agent's override. So the rewrite did **not** remove the default sentinel outright — it made it **optional**, gated on the re-added optional prop `workspaceDefaultLabel?: string`: composers omit it (exact-provider behavior per the spec); the personal-agent form passes it (sentinel restored). The form was updated to also pass the new `environment` prop. No last-used seeding in the form (it persists the agent's own default, not a per-conversation pick).

5. **Excluded providers (final).** Hugging Face is removed (Spring AI no longer supports its chat model) and Azure OpenAI is removed (its chat model needs a per-deployment endpoint the platform catalog doesn't store). Both are dropped from the `CHAT_PROVIDERS` set in `AiProviderFacadeImpl`, so they don't appear in the picker at all (rather than appearing and silently falling back). `CatalogChatModelFactory` keeps a defensive `default -> null` for them. The catalog therefore offers 8 chat providers: Anthropic, Groq, Mistral, NVIDIA, OpenAI, Vertex Gemini, Perplexity, DeepSeek.

---

## Background facts (verified — do not re-derive)

- **Catalog provider enum:** `com.bytechef.component.ai.llm.Provider` — constructor `Provider(int id, String name, String key, String label)`. Getters: `getId()`, `getName()` (component name match key, e.g. `"anthropic"`), `getKey()` (stable storage key, e.g. `"ai.provider.openAi"`), `getLabel()` (display, e.g. `"Anthropic"`). Chat-capable entries offered by the catalog (8): ANTHROPIC, GROQ, MISTRAL, NVIDIA, OPEN_AI, VERTEX_GEMINI, PERPLEXITY, DEEPSEEK. **Excluded:** STABILITY (image-only), HUGGING_FACE (Spring AI dropped support), AZURE_OPEN_AI (needs a deployment endpoint the catalog doesn't store).
- **`getKey()` is the wire value** sent as `userSelectedLlmProvider` (e.g. `"ai.provider.openAi"`). Gateway resolvers match `AiGatewayProviderType.name()` (e.g. `"OPENAI"`) — different namespace, so catalog and gateway tiers never collide.
- **Each provider component exposes** `public static final com.bytechef.component.ai.llm.ChatModel CHAT_MODEL` in its `*ChatAction` class (e.g. `OpenAiChatAction.CHAT_MODEL`). The interface method is `org.springframework.ai.chat.model.ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters, boolean responseFormatRequired)`. The lambdas read `inputParameters.getRequiredString(MODEL)` (`LLMConstants.MODEL = "model"`) and `connectionParameters.getString(TOKEN)` (`com.bytechef.component.definition.Authorization.TOKEN`).
- **Build `Parameters` via** `com.bytechef.platform.component.definition.ParametersFactory.create(Map<String, ?> map)`.
- **No registry** maps `Provider` → `CHAT_MODEL`; the new `CatalogChatModelFactory` is that registry (a `switch` on `Provider`).
- **Model option lists** live on each chat action's `model` `StringProperty.getOptions()` (`Option` has `getValue()`/`getLabel()`); providers with a free-form model field have no options.
- **`PropertyService.getProperties(List<String> keys, Scope scope, Long scopeId, Long environmentId)`** returns `Property` rows; `property.get("apiKey")` and `property.isEnabled()` read the per-provider state. `Scope.PLATFORM`, `scopeId=null`, `environmentId=(long) environment`.
- **`AiProviderFacadeImpl`** (`platform-configuration-service`, EE) already walks `Provider.values()` + `ComponentDefinitionService.getComponentDefinitions()` for icons and `PropertyService` for enabled/apiKey. The new read method extends this class.
- **GraphQL controller pattern:** `@Controller @ConditionalOnEEVersion @ConditionalOnProperty(... "bytechef.ai.gateway.enabled" = "true") @ConditionalOnCoordinator`; `@QueryMapping @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")`. Schema `.graphqls` uses `extend type Query { ... }`.
- **Client wire-through is unchanged:** `ModelPicker.onChange(provider, model)` → existing stores → existing `buildStateToSend`/`stateToSend` → `userSelectedLlmProvider`/`userSelectedLlmModel`. Only the picker's data source and rendering change, plus last-used seeding.
- **Settings route:** `/automation/settings/ai-providers`.

**EE license header + `@version ee`:** every new file under `server/ee/` uses the ByteChef Enterprise license header and a `@version ee` Javadoc tag (Spotless picks the header by `@version ee` content). New files under `server/libs/` (CE) use the Apache header.

---

## File Structure

**Phase 1 — Server query**
- Modify: `server/ee/libs/platform/.../platform-configuration-api/.../facade/AiProviderFacade.java` — add `getAiProviderCatalog(int)`.
- Create: `server/ee/libs/platform/.../platform-configuration-api/.../dto/AiProviderCatalogItemDTO.java` — USER-safe DTO (no apiKey).
- Modify: `server/ee/libs/platform/.../platform-configuration-service/.../facade/AiProviderFacadeImpl.java` — implement it.
- Create: `server/ee/libs/platform/.../platform-configuration-graphql/.../web/graphql/AiProviderCatalogGraphQlController.java` — USER query.
- Create: `server/ee/libs/platform/.../platform-configuration-graphql/src/main/resources/graphql/ai-provider-catalog.graphqls`.
- Tests: `AiProviderFacadeImplCatalogTest.java`, `AiProviderCatalogGraphQlControllerTest.java`.

**Phase 2 — Server runtime resolver**
- Create: `server/ee/libs/platform/.../platform-ai-hub-service/.../agent/CatalogChatModelFactory.java` (or nearest shared EE module reachable by both resolvers — see Task 2.1).
- Create: `.../agent/CatalogChatClientResolver.java` — builds `ChatClient` from catalog key + model.
- Modify: `AiHubChatClientResolver.java` — try catalog tier before gateway.
- Modify: `CopilotChatClientResolver.java` — try catalog tier before gateway.
- Tests: `CatalogChatModelFactoryTest.java`, `CatalogChatClientResolverTest.java`, resolver wiring tests.

**Phase 3 — Client**
- Create: `client/src/graphql/platform/ai-providers/aiProviderCatalog.graphql`.
- Modify: `client/codegen.ts` — add the schema path.
- Create: `client/src/shared/components/ai/model-picker/lastUsedModel.ts` — last-used persistence.
- Rewrite: `client/src/shared/components/ai/model-picker/ModelPicker.tsx`.
- Modify: `AiHubHomePanel.tsx`, `AiHubPanel.tsx`, `CopilotPanel.tsx` — seed from last-used; pass `environment`.
- Tests: `ModelPicker.test.tsx`, `lastUsedModel.test.ts`.

---

# Phase 1 — Server: USER-safe `aiProviderCatalog` query

### Task 1.1: USER-safe DTO

**Files:**
- Create: `server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/dto/AiProviderCatalogItemDTO.java`

- [ ] **Step 1: Create the DTO**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * USER-safe projection of a chat-capable AI provider from the platform catalog. Deliberately omits the
 * API key (unlike {@link AiProviderDTO}) so it can be returned to non-admin chat users.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AiProviderCatalogItemDTO(
    String key, String name, String icon, boolean enabled, boolean supportsModelById, List<Model> models) {

    @SuppressFBWarnings("EI")
    public record Model(String name, String label) {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/dto/AiProviderCatalogItemDTO.java
git commit -m "732 Add AiProviderCatalogItemDTO for USER-safe chat provider catalog"
```

---

### Task 1.2: Facade method (test first)

**Files:**
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacade.java`
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImpl.java`
- Test: `server/ee/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImplCatalogTest.java`

- [ ] **Step 1: Add the interface method**

In `AiProviderFacade.java`, add (keep alphabetical with existing methods):

```java
    List<AiProviderCatalogItemDTO> getAiProviderCatalog(int environment);
```

Add the import `import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;`.

- [ ] **Step 2: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiProviderFacadeImplCatalogTest {

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private PropertyService propertyService;

    @Test
    void testGetAiProviderCatalogExcludesStabilityAndOmitsApiKey() {
        ComponentDefinition openAi = mockComponentDefinition("openAi", "<svg>openai</svg>");
        ComponentDefinition anthropic = mockComponentDefinition("anthropic", "<svg>anthropic</svg>");
        ComponentDefinition stability = mockComponentDefinition("stability", "<svg>stability</svg>");

        when(componentDefinitionService.getComponentDefinitions())
            .thenReturn(List.of(openAi, anthropic, stability));
        when(propertyService.getProperties(any(), eq(Scope.PLATFORM), eq(null), anyLong()))
            .thenReturn(List.of());

        AiProviderFacadeImpl facade = new AiProviderFacadeImpl(componentDefinitionService, propertyService);

        List<AiProviderCatalogItemDTO> catalog = facade.getAiProviderCatalog(1);

        assertThat(catalog)
            .extracting(AiProviderCatalogItemDTO::key)
            .contains("ai.provider.openAi", "ai.provider.anthropic")
            .doesNotContain("ai.provider.stability");
    }

    private static ComponentDefinition mockComponentDefinition(String name, String icon) {
        ComponentDefinition componentDefinition = org.mockito.Mockito.mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn(name);

        return componentDefinition;
    }
}
```

> NOTE: the exact `ComponentDefinition` action/property API for reading model options is fleshed out in Step 4. The test above pins the two load-bearing behaviors (Stability excluded; the DTO type carries no apiKey field by construction). Extend with a model-options assertion once Step 4's helper exists.

- [ ] **Step 3: Run test, verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test --tests '*AiProviderFacadeImplCatalogTest*'`
Expected: FAIL (method `getAiProviderCatalog` does not exist).

- [ ] **Step 4: Implement `getAiProviderCatalog` in `AiProviderFacadeImpl`**

Add this method (mirrors the existing `getAiProviders` traversal; reuses the same icon + property lookup). Add a private helper to read the chat action's `model` options and a chat-capable set.

```java
    private static final java.util.Set<Provider> CHAT_PROVIDERS = java.util.EnumSet.of(
        Provider.ANTHROPIC, Provider.AZURE_OPEN_AI, Provider.GROQ, Provider.HUGGING_FACE, Provider.MISTRAL,
        Provider.NVIDIA, Provider.OPEN_AI, Provider.VERTEX_GEMINI, Provider.PERPLEXITY, Provider.DEEPSEEK);

    @Override
    @Transactional(readOnly = true)
    public List<AiProviderCatalogItemDTO> getAiProviderCatalog(int environment) {
        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

        List<Property> properties = propertyService.getProperties(
            CHAT_PROVIDERS.stream()
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null, (long) environment);

        return CHAT_PROVIDERS.stream()
            .map(provider -> {
                ComponentDefinition componentDefinition = componentDefinitions.stream()
                    .filter(curComponentDefinition -> {
                        String name = provider.getName();

                        return name.contains(curComponentDefinition.getName());
                    })
                    .findFirst()
                    .orElse(null);

                if (componentDefinition == null) {
                    return null;
                }

                Property property = properties.stream()
                    .filter(curProperty -> curProperty.getKey()
                        .equals(provider.getKey()))
                    .findFirst()
                    .orElse(null);

                boolean enabled = property != null && property.isEnabled();

                List<AiProviderCatalogItemDTO.Model> models = readChatModels(componentDefinition);

                return new AiProviderCatalogItemDTO(
                    provider.getKey(), provider.getLabel(), componentDefinition.getIcon(), enabled,
                    models.isEmpty(), models);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private static List<AiProviderCatalogItemDTO.Model> readChatModels(ComponentDefinition componentDefinition) {
        // The chat action is the component's "ask"/chat action; its "model" property carries the option list
        // for providers that enumerate models (OpenAI/Anthropic/Mistral/Gemini). Free-form providers (Groq,
        // Perplexity, NVIDIA, Azure, DeepSeek, HuggingFace) have no options -> empty list -> supportsModelById.
        return componentDefinition.getActions()
            .stream()
            .flatMap(actionDefinition -> actionDefinition.getProperties()
                .stream())
            .filter(property -> "model".equals(property.getName()))
            .filter(property -> property instanceof com.bytechef.platform.component.domain.StringProperty)
            .map(property -> (com.bytechef.platform.component.domain.StringProperty) property)
            .findFirst()
            .map(stringProperty -> {
                List<? extends com.bytechef.platform.component.domain.Option> options = stringProperty.getOptions();

                if (options == null) {
                    return List.<AiProviderCatalogItemDTO.Model>of();
                }

                return options.stream()
                    .map(option -> new AiProviderCatalogItemDTO.Model(
                        String.valueOf(option.getValue()), option.getLabel()))
                    .toList();
            })
            .orElseGet(List::of);
    }
```

Add imports: `com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO`, `java.util.List` (already present), `java.util.Objects` (already present).

> If `ComponentDefinition.getActions()` / `StringProperty.getOptions()` / `Option.getValue()/getLabel()` signatures differ from the above when you open the files, adapt the calls — the contract you need is "from the component definition, find the chat action's `model` property and read its options (value+label)". Confirm the exact `domain` package types under `platform-component-api/.../platform/component/domain/`.

- [ ] **Step 5: Flesh out the test's model assertion**

Extend `AiProviderFacadeImplCatalogTest` with a case where a mocked `ComponentDefinition` for `openAi` returns an action whose `model` `StringProperty` has two options, and assert the returned item's `models` has those two `{name,label}` pairs and `supportsModelById == false`; and a free-form provider yields empty `models` + `supportsModelById == true`. Use Mockito to stub `getActions()`/`getProperties()`/`getOptions()` per the confirmed domain API.

- [ ] **Step 6: Run tests, verify pass**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test --tests '*AiProviderFacadeImplCatalogTest*'`
Expected: PASS.

- [ ] **Step 7: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacade.java \
        server/ee/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImpl.java \
        server/ee/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImplCatalogTest.java
git commit -m "732 Add getAiProviderCatalog facade method (chat providers, models, no apiKey)"
```

---

### Task 1.3: GraphQL schema + controller

**Files:**
- Create: `server/ee/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/ai-provider-catalog.graphqls`
- Create: `server/ee/libs/platform/platform-configuration/platform-configuration-graphql/src/main/java/com/bytechef/ee/platform/configuration/web/graphql/AiProviderCatalogGraphQlController.java`
- Test: `.../platform-configuration-graphql/src/test/java/com/bytechef/ee/platform/configuration/web/graphql/AiProviderCatalogGraphQlControllerTest.java`

> Confirm the graphql module path exists (`platform-configuration-graphql`). If the platform-configuration domain has no graphql module yet, place the controller + schema in the nearest existing EE graphql module that the client codegen already scans and that is coordinator-scoped; update the codegen path in Task 3.1 accordingly. The automation-ai-gateway-graphql module is a valid fallback host.

- [ ] **Step 1: Create the schema**

```graphql
extend type Query {
    aiProviderCatalog(environment: ID!): [AiProviderCatalogItem!]!
}

type AiProviderCatalogItem {
    key: String!
    name: String!
    icon: String
    enabled: Boolean!
    supportsModelById: Boolean!
    models: [AiProviderModel!]!
}

type AiProviderModel {
    name: String!
    label: String!
}
```

- [ ] **Step 2: Write the failing controller test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiProviderCatalogGraphQlControllerTest {

    @Mock
    private AiProviderFacade aiProviderFacade;

    @Test
    void testAiProviderCatalogDelegatesToFacade() {
        AiProviderCatalogItemDTO item = new AiProviderCatalogItemDTO(
            "ai.provider.openAi", "Open AI", "<svg/>", true, false, List.of(new AiProviderCatalogItemDTO.Model("gpt-5", "GPT-5")));

        when(aiProviderFacade.getAiProviderCatalog(2)).thenReturn(List.of(item));

        AiProviderCatalogGraphQlController controller = new AiProviderCatalogGraphQlController(aiProviderFacade);

        List<AiProviderCatalogItemDTO> result = controller.aiProviderCatalog(2L);

        assertThat(result).singleElement()
            .extracting(AiProviderCatalogItemDTO::key)
            .isEqualTo("ai.provider.openAi");
    }
}
```

- [ ] **Step 3: Run test, verify fail**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-graphql:test --tests '*AiProviderCatalogGraphQlControllerTest*'`
Expected: FAIL (controller class missing).

- [ ] **Step 4: Create the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL query exposing the chat-capable AI provider catalog (USER-safe: no API keys) for the composer
 * model picker. Distinct from the ADMIN-only AI Providers REST surface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AiProviderCatalogGraphQlController {

    private final AiProviderFacade aiProviderFacade;

    @SuppressFBWarnings("EI")
    AiProviderCatalogGraphQlController(AiProviderFacade aiProviderFacade) {
        this.aiProviderFacade = aiProviderFacade;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiProviderCatalogItemDTO> aiProviderCatalog(@Argument Long environment) {
        return aiProviderFacade.getAiProviderCatalog(environment.intValue());
    }
}
```

> GraphQL field-to-record mapping: GraphQL `AiProviderCatalogItem`/`AiProviderModel` fields map by name to `AiProviderCatalogItemDTO`/`...Model` record accessors. No extra `@SchemaMapping` needed.

- [ ] **Step 5: Run test, verify pass**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-graphql:test --tests '*AiProviderCatalogGraphQlControllerTest*'`
Expected: PASS.

- [ ] **Step 6: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/ai-provider-catalog.graphqls \
        server/ee/libs/platform/platform-configuration/platform-configuration-graphql/src/main/java/com/bytechef/ee/platform/configuration/web/graphql/AiProviderCatalogGraphQlController.java \
        server/ee/libs/platform/platform-configuration/platform-configuration-graphql/src/test/java/com/bytechef/ee/platform/configuration/web/graphql/AiProviderCatalogGraphQlControllerTest.java
git commit -m "732 Add aiProviderCatalog USER GraphQL query"
```

---

# Phase 2 — Server: catalog-based runtime resolver

### Task 2.1: `CatalogChatModelFactory`

Builds a Spring-AI `ChatModel` for a catalog `Provider` key + model + apiKey, by reusing each component's `CHAT_MODEL` lambda.

**Files:**
- Create (host module reachable by BOTH resolvers — `platform-ai-hub-service` is depended on by AI Hub; Copilot depends on its own service. Place the factory in a module both can see. The safe host is a new small interface in `platform-ai-hub-api` with impl in a module both depend on, OR duplicate-free: put the factory in `platform-ai-gateway`/a shared `platform-ai` service module. **Confirm the dependency graph and pick the lowest common module.** This plan assumes host module `server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service` since both resolvers already depend on `platform-ai-gateway-*`.)
  - `.../platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/catalog/CatalogChatModelFactory.java`
- Test: `.../platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/catalog/CatalogChatModelFactoryTest.java`
- Modify: that module's `build.gradle.kts` — add `implementation` deps on the chat component modules whose `CHAT_MODEL` is referenced (open-ai, anthropic, mistral, gemini, groq, perplexity, nvidia, deepseek) plus `ai/llm` and `platform-component-api`.

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.llm.Provider;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogChatModelFactoryTest {

    private final CatalogChatModelFactory factory = new CatalogChatModelFactory();

    @Test
    void testCreateChatModelForOpenAiReturnsModel() {
        org.springframework.ai.chat.model.ChatModel chatModel =
            factory.createChatModel(Provider.OPEN_AI, "gpt-4o", "sk-test-key");

        assertThat(chatModel).isNotNull();
    }

    @Test
    void testCreateChatModelForCatalogOnlyConnectionProviderReturnsNull() {
        // Azure needs a deployment endpoint the catalog doesn't store; v1 returns null -> caller falls back.
        org.springframework.ai.chat.model.ChatModel chatModel =
            factory.createChatModel(Provider.AZURE_OPEN_AI, "gpt-4o", "sk-test-key");

        assertThat(chatModel).isNull();
    }
}
```

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*CatalogChatModelFactoryTest*'`
Expected: FAIL (class missing).

- [ ] **Step 3: Implement the factory**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.anthropic.action.AnthropicChatAction;
import com.bytechef.component.ai.llm.deepseek.action.DeepSeekChatAction;
import com.bytechef.component.ai.llm.gemini.action.GeminiChatAction;
import com.bytechef.component.ai.llm.groq.action.GroqChatAction;
import com.bytechef.component.ai.llm.mistral.action.MistralChatAction;
import com.bytechef.component.ai.llm.nvidia.action.NvidiaChatAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiChatAction;
import com.bytechef.component.ai.llm.perplexity.action.PerplexityChatAction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Builds a Spring-AI {@link org.springframework.ai.chat.model.ChatModel} for a catalog {@link Provider} +
 * model name + platform API key, by reusing each component's existing {@code CHAT_MODEL} lambda fed
 * synthetic {@link Parameters}.
 *
 * <p>
 * Providers whose {@code CHAT_MODEL} needs connection params beyond the API key (Azure: deployment
 * endpoint; HuggingFace: inference URL) cannot be built from the catalog alone and return {@code null}
 * (the caller falls back to the gateway/workspace default). All other chat providers' lambdas need only
 * {@code TOKEN} + {@code MODEL}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class CatalogChatModelFactory {

    public @Nullable org.springframework.ai.chat.model.ChatModel createChatModel(
        Provider provider, String model, String apiKey) {

        ChatModel chatModelFactory = resolveFactory(provider);

        if (chatModelFactory == null) {
            return null;
        }

        Parameters inputParameters = ParametersFactory.create(Map.of(MODEL, model));
        Parameters connectionParameters = ParametersFactory.create(Map.of(TOKEN, apiKey));

        return chatModelFactory.createChatModel(inputParameters, connectionParameters, false);
    }

    private static @Nullable ChatModel resolveFactory(Provider provider) {
        return switch (provider) {
            case OPEN_AI -> OpenAiChatAction.CHAT_MODEL;
            case ANTHROPIC -> AnthropicChatAction.CHAT_MODEL;
            case MISTRAL -> MistralChatAction.CHAT_MODEL;
            case VERTEX_GEMINI -> GeminiChatAction.CHAT_MODEL;
            case GROQ -> GroqChatAction.CHAT_MODEL;
            case PERPLEXITY -> PerplexityChatAction.CHAT_MODEL;
            case NVIDIA -> NvidiaChatAction.CHAT_MODEL;
            case DEEPSEEK -> DeepSeekChatAction.CHAT_MODEL;
            // AZURE_OPEN_AI + HUGGING_FACE need extra connection params not in the catalog -> not v1.
            default -> null;
        };
    }
}
```

> Verify each `*ChatAction` class name + package when wiring imports (e.g. the Gemini action may be `GeminiChatAction` under `...gemini.action`; Mistral `MistralChatAction`). Open each module's `action/` package to confirm the exact class and that `CHAT_MODEL` is `public static final`. Add only the modules you actually import to `build.gradle.kts`.

- [ ] **Step 4: Add Gradle deps**

In the host module's `build.gradle.kts`, add (names per `settings.gradle.kts`):

```kotlin
    implementation(project(":server:libs:modules:components:ai:llm:ai-llm"))
    implementation(project(":server:libs:modules:components:ai:llm:open-ai"))
    implementation(project(":server:libs:modules:components:ai:llm:anthropic"))
    implementation(project(":server:libs:modules:components:ai:llm:mistral"))
    implementation(project(":server:libs:modules:components:ai:llm:gemini"))
    implementation(project(":server:libs:modules:components:ai:llm:groq"))
    implementation(project(":server:libs:modules:components:ai:llm:perplexity"))
    implementation(project(":server:libs:modules:components:ai:llm:nvidia"))
    implementation(project(":server:libs:modules:components:ai:llm:deepseek"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
```

> Confirm exact Gradle project paths via `./gradlew :server:... projects` or by grepping `settings.gradle.kts` for each module dir name.

- [ ] **Step 5: Run, verify pass**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*CatalogChatModelFactoryTest*'`
Expected: PASS. (If `createChatModel` for OPEN_AI touches network at build time it should NOT — the lambda only constructs the client. If a provider's lambda eagerly validates the key, relax that test to assert no exception / non-null instead.)

- [ ] **Step 6: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/catalog/CatalogChatModelFactory.java \
        server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/catalog/CatalogChatModelFactoryTest.java \
        server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/build.gradle.kts
git commit -m "732 Add CatalogChatModelFactory reusing component CHAT_MODEL builders"
```

---

### Task 2.2: `CatalogChatClientResolver`

Turns `(environment, providerKey, model)` into a `ChatClient` using the factory + the platform API key.

**Files:**
- Create: `.../platform-ai-gateway-service/.../catalog/CatalogChatClientResolver.java`
- Test: `.../catalog/CatalogChatClientResolverTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogChatClientResolverTest {

    private final PropertyService propertyService = mock(PropertyService.class);
    private final CatalogChatModelFactory catalogChatModelFactory = mock(CatalogChatModelFactory.class);
    private final CatalogChatClientResolver resolver =
        new CatalogChatClientResolver(propertyService, catalogChatModelFactory);

    @Test
    void testResolveReturnsNullForUnknownProviderKey() {
        assertThat(resolver.resolve(1, "notAProvider", "x")).isNull();
    }

    @Test
    void testResolveReturnsNullWhenProviderDisabled() {
        Property property = mock(Property.class);

        when(property.isEnabled()).thenReturn(false);
        when(propertyService.fetchProperty(eq("ai.provider.openAi"), eq(Scope.PLATFORM), eq(null), anyLong()))
            .thenReturn(Optional.of(property));

        assertThat(resolver.resolve(1, "ai.provider.openAi", "gpt-4o")).isNull();
    }

    @Test
    void testResolveBuildsClientWhenEnabledWithKey() {
        Property property = mock(Property.class);

        when(property.isEnabled()).thenReturn(true);
        when(property.get("apiKey")).thenReturn("sk-test");
        when(propertyService.fetchProperty(eq("ai.provider.openAi"), eq(Scope.PLATFORM), eq(null), anyLong()))
            .thenReturn(Optional.of(property));
        when(catalogChatModelFactory.createChatModel(eq(Provider.OPEN_AI), eq("gpt-4o"), eq("sk-test")))
            .thenReturn(mock(org.springframework.ai.chat.model.ChatModel.class));

        assertThat(resolver.resolve(1, "ai.provider.openAi", "gpt-4o")).isNotNull();
    }
}
```

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*CatalogChatClientResolverTest*'`
Expected: FAIL (class missing).

- [ ] **Step 3: Implement the resolver**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

/**
 * Resolves an override {@link ChatClient} from the platform AI provider catalog: given a catalog provider
 * key (e.g. {@code "ai.provider.openAi"}) + model name, reads the environment-scoped platform API key and builds a
 * Spring-AI ChatModel via {@link CatalogChatModelFactory}. Returns {@code null} (caller falls back) when
 * the key is unknown, the provider is disabled, no API key is stored, or the factory can't build it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class CatalogChatClientResolver {

    private final PropertyService propertyService;
    private final CatalogChatModelFactory catalogChatModelFactory;

    @SuppressFBWarnings("EI")
    public CatalogChatClientResolver(
        PropertyService propertyService, CatalogChatModelFactory catalogChatModelFactory) {

        this.propertyService = propertyService;
        this.catalogChatModelFactory = catalogChatModelFactory;
    }

    public @Nullable ChatClient resolve(int environment, String providerKey, String model) {
        Provider provider = Arrays.stream(Provider.values())
            .filter(curProvider -> curProvider.getKey()
                .equals(providerKey))
            .findFirst()
            .orElse(null);

        if (provider == null) {
            return null;
        }

        Optional<Property> property =
            propertyService.fetchProperty(provider.getKey(), Scope.PLATFORM, null, (long) environment);

        if (property.isEmpty() || !property.get()
            .isEnabled()) {

            return null;
        }

        Object apiKey = property.get()
            .get("apiKey");

        if (apiKey == null) {
            return null;
        }

        ChatModel chatModel = catalogChatModelFactory.createChatModel(provider, model, apiKey.toString());

        if (chatModel == null) {
            return null;
        }

        return ChatClient.builder(chatModel)
            .defaultOptions(
                ChatOptions.builder()
                    .model(model))
            .build();
    }
}
```

- [ ] **Step 4: Run, verify pass**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*CatalogChatClientResolverTest*'`
Expected: PASS.

- [ ] **Step 5: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/catalog/CatalogChatClientResolver.java \
        server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/catalog/CatalogChatClientResolverTest.java
git commit -m "732 Add CatalogChatClientResolver building ChatClient from platform catalog key"
```

---

### Task 2.3: Wire catalog tier into AI Hub resolver

The catalog tier is tried first; on null it falls through to the existing gateway lookup unchanged. The catalog needs the `environment` — `AiHubStateKeys` already carries `environmentId` (the client sends it in `buildStateToSend`).

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/AiHubChatClientResolver.java`
- Test: same module — `AiHubChatClientResolverCatalogTest.java`

- [ ] **Step 1: Confirm the environment state key**

Open `com.bytechef.ee.platform.aihub.util.AiHubStateKeys` and confirm an `ENVIRONMENT_ID` (or similar) key the client populates (`buildStateToSend` sends `environmentId: String(currentEnvironmentId)`). Note its exact constant name for Step 3. If absent, add a `public static final String ENVIRONMENT_ID = "environmentId";` constant in `AiHubStateKeys`.

- [ ] **Step 2: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ee.platform.ai.gateway.catalog.CatalogChatClientResolver;
import com.bytechef.ee.platform.aihub.util.AiHubStateKeys;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatClientResolverCatalogTest {

    @Test
    void testCatalogSelectionResolvedBeforeGateway() {
        CatalogChatClientResolver catalogResolver = mock(CatalogChatClientResolver.class);
        ChatClient catalogClient = mock(ChatClient.class);

        when(catalogResolver.resolve(eq(3), eq("ai.provider.openAi"), eq("gpt-4o"))).thenReturn(catalogClient);

        AiHubChatClientResolver resolver = new AiHubChatClientResolver(
            mock(com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService.class),
            mock(com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService.class),
            mock(com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory.class),
            catalogResolver);

        State state = new State(Map.of(
            AiHubStateKeys.VERIFIED_WORKSPACE_ID, "10",
            AiHubStateKeys.ENVIRONMENT_ID, "3",
            AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, "ai.provider.openAi",
            AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, "gpt-4o"));

        assertThat(resolver.resolve(state)).isSameAs(catalogClient);
    }
}
```

> `State`'s constructor/usage: confirm how to build a `State` from a map in tests (mirror an existing `AiHubChatClientResolver` test if one exists). Adapt construction accordingly.

- [ ] **Step 3: Modify the resolver constructor + `resolve`**

Add the `CatalogChatClientResolver` dependency and try it before the gateway lookup. Insert after the `llmProvider/llmModel` are finalized (line ~128, right after the half-set fall-through and before `resolveProvider`):

```java
    private final CatalogChatClientResolver catalogChatClientResolver;
```

Update the constructor signature to accept and assign `catalogChatClientResolver` (keep `@SuppressFBWarnings("EI")`).

In `resolve`, after:

```java
        if (llmProvider == null || llmModel == null) {
            return null;
        }
```

insert:

```java
        Integer environment = asInteger(state.get(AiHubStateKeys.ENVIRONMENT_ID));

        if (environment != null) {
            ChatClient catalogClient = catalogChatClientResolver.resolve(environment, llmProvider, llmModel);

            if (catalogClient != null) {
                return catalogClient;
            }
        }
```

Add a private helper:

```java
    private static @Nullable Integer asInteger(@Nullable Object value) {
        Long parsed = asLong(value);

        return parsed == null ? null : parsed.intValue();
    }
```

- [ ] **Step 4: Update Spring wiring + Gradle dep**

`AiHubChatClientResolver` is `@Component`-injected, so Spring supplies the new constructor arg automatically once `platform-ai-gateway-service` (where `CatalogChatClientResolver` lives) is a dependency of `automation-ai-hub-service`. Confirm/add that `implementation(project(...))` dep in the module's `build.gradle.kts`.

- [ ] **Step 5: Run, verify pass**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests '*AiHubChatClientResolver*'`
Expected: PASS (new catalog test + existing resolver tests still green).

- [ ] **Step 6: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/...
git commit -m "732 Resolve AI Hub user-selected LLM via catalog before gateway"
```

---

### Task 2.4: Wire catalog tier into Copilot resolver

Same insertion in `CopilotChatClientResolver`. Copilot reads `workspaceId` from state; add reading `environmentId` similarly.

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/CopilotChatClientResolver.java`
- Test: `CopilotChatClientResolverCatalogTest.java`

- [ ] **Step 1: Write the failing test** (mirror Task 2.3 Step 2, using the Copilot constant `WORKSPACE_ID_KEY` and a new `ENVIRONMENT_ID_KEY = "environmentId"`; assert the catalog client is returned before the gateway path).

- [ ] **Step 2: Run, verify fail.**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests '*CopilotChatClientResolverCatalog*'`
Expected: FAIL.

- [ ] **Step 3: Implement.** Add constant:

```java
    static final String ENVIRONMENT_ID_KEY = "environmentId";
```

Add `private final CatalogChatClientResolver catalogChatClientResolver;` + constructor param + assignment. In `resolve`, after the `(llmProvider == null || llmModel == null)` guard and before `resolveProvider`, insert:

```java
        Long environment = asLong(state.get(ENVIRONMENT_ID_KEY));

        if (environment != null) {
            ChatClient catalogClient =
                catalogChatClientResolver.resolve(environment.intValue(), llmProvider, llmModel);

            if (catalogClient != null) {
                return catalogClient;
            }
        }
```

Confirm the Copilot client sends `environmentId` in `stateToSend` (Task 3.6 adds it if missing). Add the Gradle dep on `platform-ai-gateway-service` if not present.

- [ ] **Step 4: Run, verify pass.** Same gradle command. Expected: PASS.

- [ ] **Step 5: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/...
git commit -m "732 Resolve Copilot user-selected LLM via catalog before gateway"
```

---

# Phase 3 — Client: catalog-driven `ModelPicker`

### Task 3.1: GraphQL operation + codegen

**Files:**
- Create: `client/src/graphql/platform/ai-providers/aiProviderCatalog.graphql`
- Modify: `client/codegen.ts` — add the schema path (the `.graphqls` from Task 1.3)

- [ ] **Step 1: Create the operation**

```graphql
query aiProviderCatalog($environment: ID!) {
    aiProviderCatalog(environment: $environment) {
        key
        name
        icon
        enabled
        supportsModelById
        models {
            name
            label
        }
    }
}
```

- [ ] **Step 2: Add the schema path to `codegen.ts`**

In the `schema` array, add the path to the new schema file:

```typescript
        '../server/ee/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/*.graphqls',
```

> If a `platform-configuration-graphql` path is already globbed, no change is needed. If Task 1.3 hosted the controller in a different module, use that module's graphql resources glob instead.

- [ ] **Step 3: Regenerate types**

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` now exports `useAiProviderCatalogQuery` and `graphql-types.ts` has `AiProviderCatalogItem`/`AiProviderModel` types.

- [ ] **Step 4: Commit**

```bash
git add client/src/graphql/platform/ai-providers/aiProviderCatalog.graphql client/codegen.ts client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "732 client - Add aiProviderCatalog GraphQL query + codegen"
```

---

### Task 3.2: Last-used model persistence (test first)

**Files:**
- Create: `client/src/shared/components/ai/model-picker/lastUsedModel.ts`
- Test: `client/src/shared/components/ai/model-picker/lastUsedModel.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import {beforeEach, describe, expect, it} from 'vitest';

import {readLastUsedModel, writeLastUsedModel} from './lastUsedModel';

describe('lastUsedModel', () => {
    beforeEach(() => {
        localStorage.clear();
    });

    it('returns null when nothing stored', () => {
        expect(readLastUsedModel(42)).toBeNull();
    });

    it('round-trips a written selection per workspace', () => {
        writeLastUsedModel(42, 'openAi', 'gpt-4o');

        expect(readLastUsedModel(42)).toEqual({model: 'gpt-4o', provider: 'openAi'});
        expect(readLastUsedModel(43)).toBeNull();
    });

    it('clears when provider or model is null', () => {
        writeLastUsedModel(42, 'openAi', 'gpt-4o');
        writeLastUsedModel(42, null, null);

        expect(readLastUsedModel(42)).toBeNull();
    });
});
```

- [ ] **Step 2: Run, verify fail**

Run: `cd client && npx vitest run src/shared/components/ai/model-picker/lastUsedModel.test.ts`
Expected: FAIL (module missing).

- [ ] **Step 3: Implement**

```ts
/**
 * Per-workspace persistence of the user's last-used (provider, model) selection so a fresh conversation
 * or task seeds the picker with what they last ran, instead of the workspace default. Keyed by workspace
 * because providers/active-state are environment+workspace specific; the value is a stable provider key
 * (e.g. "ai.provider.openAi") + model name.
 */
interface LastUsedModelI {
    model: string;
    provider: string;
}

const storageKey = (workspaceId: number) => `bytechef.modelPicker.lastUsed.${workspaceId}`;

export const readLastUsedModel = (workspaceId: number): LastUsedModelI | null => {
    const raw = localStorage.getItem(storageKey(workspaceId));

    if (raw == null) {
        return null;
    }

    try {
        const parsed = JSON.parse(raw) as Partial<LastUsedModelI>;

        if (parsed.provider == null || parsed.model == null) {
            return null;
        }

        return {model: parsed.model, provider: parsed.provider};
    } catch {
        return null;
    }
};

export const writeLastUsedModel = (workspaceId: number, provider: string | null, model: string | null): void => {
    if (provider == null || model == null) {
        localStorage.removeItem(storageKey(workspaceId));

        return;
    }

    localStorage.setItem(storageKey(workspaceId), JSON.stringify({model, provider}));
};
```

- [ ] **Step 4: Run, verify pass**

Run: `cd client && npx vitest run src/shared/components/ai/model-picker/lastUsedModel.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/ai/model-picker/lastUsedModel.ts client/src/shared/components/ai/model-picker/lastUsedModel.test.ts
git commit -m "732 client - Add last-used model persistence helper"
```

---

### Task 3.3: Rewrite `ModelPicker` (catalog-driven)

This replaces the AI-Gateway data source with `useAiProviderCatalogQuery`, renders provider icons, branches active/inactive, adds "Choose model by ID", and shows the exact selected provider+model in the trigger. The personal-agents and workflow-chats cascades and all existing props are preserved; one new required prop `environment` is added.

**Files:**
- Rewrite: `client/src/shared/components/ai/model-picker/ModelPicker.tsx`
- Test: `client/src/shared/components/ai/model-picker/ModelPicker.test.tsx`

- [ ] **Step 1: Write failing tests**

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ModelPicker from './ModelPicker';

const {navigateMock, useAiProviderCatalogQueryMock} = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    useAiProviderCatalogQueryMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiProviderCatalogQuery: useAiProviderCatalogQueryMock,
}));

const catalog = [
    {
        enabled: true,
        icon: '<svg>openai</svg>',
        key: 'openAi',
        models: [{label: 'GPT-4o', name: 'gpt-4o'}],
        name: 'Open AI',
        supportsModelById: false,
    },
    {
        enabled: false,
        icon: '<svg>anthropic</svg>',
        key: 'anthropic',
        models: [],
        name: 'Anthropic',
        supportsModelById: true,
    },
];

describe('ModelPicker', () => {
    beforeEach(() => {
        navigateMock.mockReset();
        useAiProviderCatalogQueryMock.mockReturnValue({data: {aiProviderCatalog: catalog}});
        localStorage.clear();
    });

    it('lists active and inactive providers', () => {
        render(
            <ModelPicker
                environment={1}
                onChange={vi.fn()}
                selectedModel={null}
                selectedProvider={null}
                workspaceId={5}
            />
        );

        fireEvent.click(screen.getByLabelText('Select LLM provider and model'));

        expect(screen.getByText('Open AI')).toBeInTheDocument();
        expect(screen.getByText('Anthropic')).toBeInTheDocument();
    });

    it('shows exact selected provider+model in the trigger, not a default label', () => {
        render(
            <ModelPicker
                environment={1}
                onChange={vi.fn()}
                selectedModel="gpt-4o"
                selectedProvider="ai.provider.openAi"
                workspaceId={5}
            />
        );

        expect(screen.getByText('GPT-4o')).toBeInTheDocument();
        expect(screen.queryByText('Workspace default')).not.toBeInTheDocument();
    });

    it('navigates to AI Providers settings for an inactive provider', () => {
        render(
            <ModelPicker
                environment={1}
                onChange={vi.fn()}
                selectedModel={null}
                selectedProvider={null}
                workspaceId={5}
            />
        );

        fireEvent.click(screen.getByLabelText('Select LLM provider and model'));
        fireEvent.click(screen.getByText('Anthropic'));
        fireEvent.click(screen.getByText('Configure credentials'));

        expect(navigateMock).toHaveBeenCalledWith('/automation/settings/ai-providers');
    });
});
```

- [ ] **Step 2: Run, verify fail**

Run: `cd client && npx vitest run src/shared/components/ai/model-picker/ModelPicker.test.tsx`
Expected: FAIL.

- [ ] **Step 3: Rewrite `ModelPicker.tsx`**

Replace the whole file with the version below. It keeps the existing personal-agents/workflow-chats cascades and props (`agentDefault*`, `personalAgents`, `workflowChats`, callbacks, `iconOnly`, `layout`), removes the two AI-Gateway queries + the `workspaceDefaultLabel` sentinel display, and adds `environment`, catalog rendering, icons, model-by-id, and navigation.

```tsx
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {BotIcon, BrainCircuitIcon, ChevronDownIcon, PlusIcon, SettingsIcon, WorkflowIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const AI_PROVIDERS_SETTINGS_PATH = '/automation/settings/ai-providers';

export interface ModelPickerPersonalAgentI {
    id: number;
    name: string;
    title: string | null;
}

export interface ModelPickerWorkflowChatI {
    label: string;
    projectDeploymentId: string;
    workflowExecutionId: string;
}

export interface ModelPickerPropsI {
    agentDefaultModel?: string | null;
    agentDefaultProvider?: string | null;
    /** Environment whose catalog (enabled providers + models) drives the picker. */
    environment: number;
    iconOnly?: boolean;
    layout?: 'compact' | 'full';
    onChange: (provider: string | null, model: string | null) => void;
    onSelectPersonalAgent?: (agentId: number) => void;
    onSelectWorkflowChat?: (workflowExecutionId: string, projectDeploymentId: string, label: string) => void;
    personalAgents?: ModelPickerPersonalAgentI[];
    workflowChats?: ModelPickerWorkflowChatI[];
    selectedModel: string | null;
    selectedProvider: string | null;
    workspaceId: number;
}

const isPresent = <T,>(value: T | null): value is T => value != null;

const ModelPicker = ({
    agentDefaultModel,
    agentDefaultProvider,
    environment,
    iconOnly = false,
    layout = 'compact',
    onChange,
    onSelectPersonalAgent,
    onSelectWorkflowChat,
    personalAgents,
    selectedModel,
    selectedProvider,
    workflowChats,
    workspaceId,
}: ModelPickerPropsI) => {
    const [open, setOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [modelByIdProvider, setModelByIdProvider] = useState<string | null>(null);
    const [modelByIdValue, setModelByIdValue] = useState('');

    const navigate = useNavigate();

    const queryEnabled = environment > 0;

    const {data: catalogData} = useAiProviderCatalogQuery(
        {environment: environment > 0 ? String(environment) : ''},
        {enabled: queryEnabled}
    );

    const providers = useMemo(
        () => (catalogData?.aiProviderCatalog ?? []).filter(isPresent),
        [catalogData]
    );

    const sortedProviders = useMemo(() => {
        const query = searchQuery.trim().toLowerCase();
        const filtered = query
            ? providers.filter((provider) => provider.name.toLowerCase().includes(query))
            : providers;

        return [...filtered].sort((first, second) => first.name.localeCompare(second.name));
    }, [providers, searchQuery]);

    const showPersonalAgentsSection = onSelectPersonalAgent != null && (personalAgents?.length ?? 0) > 0;

    const sortedPersonalAgents = useMemo(() => {
        if (!showPersonalAgentsSection || personalAgents == null) {
            return [];
        }

        const query = searchQuery.trim().toLowerCase();
        const filtered = query
            ? personalAgents.filter(
                  (agent) =>
                      (agent.title ?? '').toLowerCase().includes(query) || agent.name.toLowerCase().includes(query)
              )
            : personalAgents;

        return [...filtered].sort((first, second) =>
            (first.title ?? first.name).localeCompare(second.title ?? second.name)
        );
    }, [personalAgents, searchQuery, showPersonalAgentsSection]);

    const showWorkflowChatsSection = onSelectWorkflowChat != null && (workflowChats?.length ?? 0) > 0;

    const sortedWorkflowChats = useMemo(() => {
        if (!showWorkflowChatsSection || workflowChats == null) {
            return [];
        }

        const query = searchQuery.trim().toLowerCase();
        const filtered = query
            ? workflowChats.filter((chat) => chat.label.toLowerCase().includes(query))
            : workflowChats;

        return [...filtered].sort((first, second) => first.label.localeCompare(second.label));
    }, [searchQuery, showWorkflowChatsSection, workflowChats]);

    const triggerContent = useMemo(() => {
        if (selectedProvider && selectedModel) {
            const provider = providers.find((candidate) => candidate.key === selectedProvider);
            const model = provider?.models.find((candidate) => candidate.name === selectedModel);

            return {icon: provider?.icon ?? null, label: model?.label || selectedModel};
        }

        if (agentDefaultProvider && agentDefaultModel) {
            const provider = providers.find((candidate) => candidate.key === agentDefaultProvider);
            const model = provider?.models.find((candidate) => candidate.name === agentDefaultModel);

            return {icon: provider?.icon ?? null, label: model?.label || agentDefaultModel};
        }

        return {icon: null, label: 'Select model'};
    }, [agentDefaultModel, agentDefaultProvider, providers, selectedModel, selectedProvider]);

    const closeMenu = () => {
        setOpen(false);
        setSearchQuery('');
        setModelByIdProvider(null);
        setModelByIdValue('');
    };

    const handleSelectModel = (providerKey: string, modelName: string) => {
        onChange(providerKey, modelName);
        closeMenu();
    };

    const handleConfigureCredentials = () => {
        navigate(AI_PROVIDERS_SETTINGS_PATH);
        closeMenu();
    };

    const handleSelectPersonalAgent = (agentId: number) => {
        onSelectPersonalAgent?.(agentId);
        closeMenu();
    };

    const handleSelectWorkflowChat = (chat: ModelPickerWorkflowChatI) => {
        onSelectWorkflowChat?.(chat.workflowExecutionId, chat.projectDeploymentId, chat.label);
        closeMenu();
    };

    const handleModelByIdSubmit = (providerKey: string) => {
        const trimmed = modelByIdValue.trim();

        if (trimmed.length > 0) {
            handleSelectModel(providerKey, trimmed);
        }
    };

    const triggerClassName = twMerge(
        'inline-flex items-center gap-1.5 rounded-md text-sm font-medium text-foreground transition-colors hover:text-accent-foreground focus-visible:ring-1 focus-visible:ring-ring focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50',
        layout === 'compact'
            ? 'h-7 border border-transparent bg-transparent px-2 hover:border-input hover:bg-accent'
            : 'h-9 w-full justify-between border border-input bg-background px-3 hover:bg-accent',
        iconOnly && 'size-7 justify-center border-0 bg-transparent px-0'
    );

    return (
        <DropdownMenu onOpenChange={(next) => (next ? setOpen(true) : closeMenu())} open={open}>
            <DropdownMenuTrigger asChild>
                <button
                    aria-label="Select LLM provider and model"
                    className={triggerClassName}
                    title={iconOnly ? triggerContent.label : undefined}
                    type="button"
                >
                    {triggerContent.icon ? (
                        <InlineSVG className="size-4 shrink-0" src={triggerContent.icon} />
                    ) : (
                        <BrainCircuitIcon className="size-4 shrink-0 text-muted-foreground" />
                    )}

                    {!iconOnly && <span className="truncate">{triggerContent.label}</span>}

                    {!iconOnly && layout === 'full' && (
                        <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" />
                    )}
                </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="start" className="w-72">
                <div className="px-2 py-1.5">
                    <input
                        aria-label="Search providers"
                        className="w-full rounded-sm border border-input bg-background px-2 py-1 text-sm placeholder:text-muted-foreground focus:ring-1 focus:ring-ring focus:outline-none"
                        onChange={(event) => setSearchQuery(event.target.value)}
                        onKeyDown={(event) => event.stopPropagation()}
                        placeholder="Search providers..."
                        type="text"
                        value={searchQuery}
                    />
                </div>

                <DropdownMenuSeparator />

                {showPersonalAgentsSection && (
                    <DropdownMenuSub>
                        <DropdownMenuSubTrigger>
                            <BotIcon className="text-muted-foreground" />

                            <span>Personal agents</span>
                        </DropdownMenuSubTrigger>

                        <DropdownMenuSubContent className="max-h-80 overflow-y-auto">
                            {sortedPersonalAgents.length === 0 ? (
                                <div className="px-2 py-1.5 text-sm text-muted-foreground">No matching agents.</div>
                            ) : (
                                sortedPersonalAgents.map((agent) => (
                                    <DropdownMenuItem key={agent.id} onSelect={() => handleSelectPersonalAgent(agent.id)}>
                                        <span className="truncate">{agent.title || agent.name}</span>
                                    </DropdownMenuItem>
                                ))
                            )}
                        </DropdownMenuSubContent>
                    </DropdownMenuSub>
                )}

                {showWorkflowChatsSection && (
                    <DropdownMenuSub>
                        <DropdownMenuSubTrigger>
                            <WorkflowIcon className="text-muted-foreground" />

                            <span>Workflow chats</span>
                        </DropdownMenuSubTrigger>

                        <DropdownMenuSubContent className="max-h-80 overflow-y-auto">
                            {sortedWorkflowChats.length === 0 ? (
                                <div className="px-2 py-1.5 text-sm text-muted-foreground">No matching workflows.</div>
                            ) : (
                                sortedWorkflowChats.map((chat) => (
                                    <DropdownMenuItem
                                        key={chat.workflowExecutionId}
                                        onSelect={() => handleSelectWorkflowChat(chat)}
                                    >
                                        <span className="truncate">{chat.label}</span>
                                    </DropdownMenuItem>
                                ))
                            )}
                        </DropdownMenuSubContent>
                    </DropdownMenuSub>
                )}

                {(showPersonalAgentsSection || showWorkflowChatsSection) && <DropdownMenuSeparator />}

                {sortedProviders.length === 0 ? (
                    <div className="px-2 py-1.5 text-sm text-muted-foreground">
                        {searchQuery.trim() ? 'No matching providers.' : 'No providers available.'}
                    </div>
                ) : (
                    sortedProviders.map((provider) => (
                        <DropdownMenuSub key={provider.key}>
                            <DropdownMenuSubTrigger>
                                <InlineSVG className="size-4 shrink-0" src={provider.icon ?? ''} />

                                <span className="truncate">{provider.name}</span>
                            </DropdownMenuSubTrigger>

                            <DropdownMenuSubContent className="max-h-80 overflow-y-auto">
                                {!provider.enabled ? (
                                    <DropdownMenuItem onSelect={handleConfigureCredentials}>
                                        <SettingsIcon className="text-muted-foreground" />

                                        <span>Configure credentials</span>
                                    </DropdownMenuItem>
                                ) : (
                                    <>
                                        {provider.models.map((model) => (
                                            <DropdownMenuItem
                                                key={model.name}
                                                onSelect={() => handleSelectModel(provider.key, model.name)}
                                            >
                                                <span className="truncate">{model.label || model.name}</span>
                                            </DropdownMenuItem>
                                        ))}

                                        {provider.models.length > 0 && <DropdownMenuSeparator />}

                                        {modelByIdProvider === provider.key ? (
                                            <div className="px-2 py-1.5">
                                                <input
                                                    aria-label="Model id"
                                                    autoFocus
                                                    className="w-full rounded-sm border border-input bg-background px-2 py-1 text-sm focus:ring-1 focus:ring-ring focus:outline-none"
                                                    onChange={(event) => setModelByIdValue(event.target.value)}
                                                    onKeyDown={(event) => {
                                                        event.stopPropagation();

                                                        if (event.key === 'Enter') {
                                                            handleModelByIdSubmit(provider.key);
                                                        }
                                                    }}
                                                    placeholder="model-id"
                                                    type="text"
                                                    value={modelByIdValue}
                                                />
                                            </div>
                                        ) : (
                                            <DropdownMenuItem
                                                onSelect={(event) => {
                                                    event.preventDefault();
                                                    setModelByIdProvider(provider.key);
                                                    setModelByIdValue('');
                                                }}
                                            >
                                                <PlusIcon className="text-muted-foreground" />

                                                <span>Choose model by ID</span>
                                            </DropdownMenuItem>
                                        )}
                                    </>
                                )}
                            </DropdownMenuSubContent>
                        </DropdownMenuSub>
                    ))
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ModelPicker;
```

- [ ] **Step 4: Run, verify pass**

Run: `cd client && npx vitest run src/shared/components/ai/model-picker/ModelPicker.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/ai/model-picker/ModelPicker.tsx client/src/shared/components/ai/model-picker/ModelPicker.test.tsx
git commit -m "732 client - Rewrite ModelPicker as catalog-driven with icons + configure credentials"
```

---

### Task 3.4: Update AI Hub home/draft picker usage

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubHomePanel.tsx`

- [ ] **Step 1: Pass `environment` + seed draft from last-used**

Add near the other store reads:

```tsx
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
```

(Import `useEnvironmentStore` from `@/shared/stores/useEnvironmentStore` if not already imported.)

Update the `<ModelPicker .../>` usage to pass `environment` and, when the draft selection is empty, seed from last-used. Replace the existing block with:

```tsx
                    <AiHubChatComposer
                        modelPicker={
                            currentWorkspaceId != null ? (
                                <ModelPicker
                                    environment={currentEnvironmentId}
                                    onChange={(provider, model) => {
                                        writeLastUsedModel(currentWorkspaceId, provider, model);
                                        setDraftLlmSelection(provider, model);
                                    }}
                                    selectedModel={
                                        draftLlmSelection?.model ??
                                        readLastUsedModel(currentWorkspaceId)?.model ??
                                        null
                                    }
                                    selectedProvider={
                                        draftLlmSelection?.provider ??
                                        readLastUsedModel(currentWorkspaceId)?.provider ??
                                        null
                                    }
                                    workspaceId={currentWorkspaceId}
                                />
                            ) : null
                        }
                    />
```

Add imports: `import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';`.

> Sort named imports alphabetically and keep object props in ascending key order (ESLint `sort-keys`).

- [ ] **Step 2: Typecheck + lint**

Run: `cd client && npm run check`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubHomePanel.tsx
git commit -m "732 client - Seed AI Hub draft model picker from last-used + pass environment"
```

---

### Task 3.5: Update AI Hub task picker usage

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubPanel.tsx`

- [ ] **Step 1: Pass `environment`, write last-used on change, seed task selection**

Add `const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);` (import the store if needed). Update the `<ModelPicker .../>` block:

```tsx
                            <ModelPicker
                                agentDefaultModel={personalAgent?.llmModel ?? null}
                                agentDefaultProvider={personalAgent?.llmProvider ?? null}
                                environment={currentEnvironmentId}
                                onChange={(provider, model) => {
                                    writeLastUsedModel(currentWorkspaceId, provider, model);
                                    setTaskLlmSelection(currentTaskId, provider, model);
                                }}
                                onSelectPersonalAgent={handleSelectPersonalAgent}
                                onSelectWorkflowChat={handleSelectWorkflowChat}
                                personalAgents={pickerAgents}
                                selectedModel={taskLlmSelection?.model ?? readLastUsedModel(currentWorkspaceId)?.model ?? null}
                                selectedProvider={
                                    taskLlmSelection?.provider ?? readLastUsedModel(currentWorkspaceId)?.provider ?? null
                                }
                                workflowChats={pickerWorkflowChats}
                                workspaceId={currentWorkspaceId}
                            />
```

Add the `lastUsedModel` import. (Note: `currentWorkspaceId` and `currentTaskId` are guaranteed non-null inside the existing `!isWorkflowChat && currentTaskId != null && currentWorkspaceId != null` guard.)

- [ ] **Step 2: Typecheck + lint**

Run: `cd client && npm run check`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubPanel.tsx
git commit -m "732 client - Seed AI Hub task model picker from last-used + pass environment"
```

---

### Task 3.6: Update Copilot picker usage + send `environmentId`

**Files:**
- Modify: `client/src/shared/components/copilot/CopilotPanel.tsx`
- Modify: `client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx`

- [ ] **Step 1: Pass `environment` + seed Copilot selection from last-used**

In `CopilotPanel.tsx`, add `const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);` and update the `leadingComposerActions`:

```tsx
                        leadingComposerActions={
                            currentWorkspaceId != null ? (
                                <ModelPicker
                                    environment={currentEnvironmentId}
                                    onChange={(provider, model) => {
                                        writeLastUsedModel(currentWorkspaceId, provider, model);
                                        setSelectedLlm(provider, model);
                                    }}
                                    selectedModel={selectedLlmModel ?? readLastUsedModel(currentWorkspaceId)?.model ?? null}
                                    selectedProvider={
                                        selectedLlmProvider ?? readLastUsedModel(currentWorkspaceId)?.provider ?? null
                                    }
                                    workspaceId={currentWorkspaceId}
                                />
                            ) : null
                        }
```

Add the `lastUsedModel` + `useEnvironmentStore` imports.

- [ ] **Step 2: Send `environmentId` in Copilot state**

In `CopilotRuntimeProvider.tsx`, add `environmentId` to `stateToSend` so the server catalog resolver (Task 2.4) has the environment. Read it from the environment store and include it:

```tsx
        const stateToSend = {
            ...contextWithoutError,
            ...useCopilotStateContributorRegistry.getState().contribute(),
            environmentId: String(environmentStore.getState().currentEnvironmentId ?? 0),
            ...(selectedLlmProvider != null && selectedLlmModel != null
                ? {userSelectedLlmModel: selectedLlmModel, userSelectedLlmProvider: selectedLlmProvider}
                : {}),
        };
```

Import the environment store the same way `AiHubRuntimeProvider` does (`import {environmentStore} from '@/shared/stores/useEnvironmentStore';` — confirm the exported vanilla store name). If the Copilot server resolver does not require `workspaceId` to be verified, no other change is needed.

- [ ] **Step 3: Typecheck + lint + tests**

Run: `cd client && npm run check`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add client/src/shared/components/copilot/CopilotPanel.tsx client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx
git commit -m "732 client - Catalog model picker in Copilot + send environmentId"
```

---

### Task 3.7: Full client check

- [ ] **Step 1: Run the full client gate**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests all PASS.

- [ ] **Step 2: Run the affected server checks**

Run:
```bash
./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test \
          :server:ee:libs:platform:platform-configuration:platform-configuration-graphql:test \
          :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test \
          :server:ee:libs:ai:ai-copilot:ai-copilot-service:test
```
Expected: PASS.

- [ ] **Step 3: Spotless + final commit (if Spotless changed anything)**

```bash
./gradlew spotlessApply
git add -A
git commit -m "732 Spotless formatting for chat-provider catalog model picker" || echo "nothing to format"
```

---

## Manual verification (post-implementation)

1. Start infra + server + client (per CLAUDE.md). Log in as a non-admin USER.
2. Open AI Hub composer → model picker: all chat providers listed with icons. Active providers expand to models + "Choose model by ID"; inactive show "Configure credentials" → clicking navigates to `/automation/settings/ai-providers`.
3. Pick a model under an active provider whose key is set in AI Providers settings (e.g. OpenAI). Trigger shows the exact provider icon + model. Send a message → confirm (server logs / response) the catalog model was used, not the workspace default.
4. Reload → the picker seeds the last-used model.
5. Repeat in the Copilot panel.
6. Confirm Azure/HuggingFace appear and, if selected, fall back gracefully (documented v1 limitation).

---

## Self-review notes (gaps to watch during execution)

- **Domain API shapes** (`ComponentDefinition.getActions()`, `StringProperty.getOptions()`, `Option.getValue/getLabel`) must be confirmed against `platform-component-api/.../platform/component/domain/` — adjust `readChatModels` accordingly (Task 1.2 Step 4 note).
- **Host module for the catalog factory/resolver** (Task 2.1) must be the lowest common module both resolvers can depend on; verify the Gradle graph before creating files there.
- **`*ChatAction` class names/packages** for each provider (Task 2.1 Step 3) must be confirmed when adding imports.
- **`AiHubStateKeys.ENVIRONMENT_ID`** existence (Task 2.3 Step 1) — add if missing; the AI Hub client already sends `environmentId`.
- **Vanilla environment store export** name for `CopilotRuntimeProvider` (Task 3.6 Step 2).
- **Spec coverage:** all 8 spec requirements map to tasks — list providers (3.3), models-only-when-active (3.3), configure-credentials→settings (3.3), exact trigger (3.3), last-used seeding (3.2/3.4-3.6), runtime resolution (2.1-2.4), no apiKey leak (1.1/1.2), both composers (3.4-3.6).
```
