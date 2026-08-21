# Central LLM Provider Registry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the per-provider `switch` + helper methods in the universal AI text/image action definitions with an `@AutoService` SPI + ServiceLoader registry, plus a generic API-key fallback accessor.

**Architecture:** Each LLM provider sub-module ships one `@AutoService(LLMModelProvider.class)` contributor declaring its `Provider` and its `ChatModel`/`ImageModel` capability. A `LLMModelRegistry` in base `ai:llm` builds `Provider → ChatModel` and `Provider → ImageModel` maps from `ServiceLoader`. The universal action definitions look up the registry and resolve the API-key fallback via a new `ApplicationProperties.Ai.Provider.getProviderApiKey(String)` method, deleting both switches and all per-provider helpers.

**Tech Stack:** Java 25, Spring Boot, Google AutoService (`com.google.auto.service.AutoService`), JUnit 5, Gradle.

## Global Constraints

- Java 25; modules under `server/libs/**` use the **Apache 2.0** license header (these are CE modules, NOT `server/ee/`).
- Java style: one blank line before control statements; one blank line after a variable modification that precedes its use; no trailing blank line before a class's closing brace; descriptive variable names (no single letters).
- Test method names: camelCase, no underscores (Checkstyle). Unit test classes end in `Test`.
- Run `./gradlew spotlessApply` before every commit; the formatter's output wins on layout.
- Provider enum keys are stable strings, e.g. `ai.provider.openAi`, `ai.provider.anthropic`, `ai.provider.azureOpenAi`, `ai.provider.deepseek`, `ai.provider.groq`, `ai.provider.mistral`, `ai.provider.nvidia`, `ai.provider.perplexity`, `ai.provider.stability`, `ai.provider.vertexGemini`.
- Use `org.jspecify.annotations.Nullable` for nullable annotations (matches `ChatModel`/`ImageModel`).

---

### Task 1: `LLMModelProvider` SPI + `LLMModelRegistry`

**Files:**
- Create: `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/LLMModelProvider.java`
- Create: `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/LLMModelRegistry.java`
- Test: `server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/LLMModelRegistryTest.java`

**Interfaces:**
- Produces:
  - `interface LLMModelProvider { Provider getProvider(); @Nullable default ChatModel getChatModel(); @Nullable default ImageModel getImageModel(); }`
  - `LLMModelRegistry.getChatModel(Provider) -> ChatModel` (throws `IllegalArgumentException` if none)
  - `LLMModelRegistry.getImageModel(Provider) -> ImageModel` (throws `IllegalArgumentException` if none)
  - package-private `LLMModelRegistry.buildChatModels(Iterable<LLMModelProvider>) -> Map<Provider, ChatModel>`
  - package-private `LLMModelRegistry.buildImageModels(Iterable<LLMModelProvider>) -> Map<Provider, ImageModel>`

- [ ] **Step 1: Write the failing test**

Create `LLMModelRegistryTest.java`:

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

package com.bytechef.component.ai.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LLMModelRegistryTest {

    private static final ChatModel CHAT_MODEL =
        (inputParameters, connectionParameters, responseFormatRequired) -> null;
    private static final ImageModel IMAGE_MODEL = (inputParameters, connectionParameters) -> null;

    @Test
    void testBuildChatModelsIncludesProvidersWithChatModel() {
        LLMModelProvider chatProvider = new LLMModelProvider() {

            @Override
            public Provider getProvider() {
                return Provider.OPEN_AI;
            }

            @Override
            public ChatModel getChatModel() {
                return CHAT_MODEL;
            }
        };

        Map<Provider, ChatModel> chatModels = LLMModelRegistry.buildChatModels(List.of(chatProvider));

        assertSame(CHAT_MODEL, chatModels.get(Provider.OPEN_AI));
    }

    @Test
    void testBuildChatModelsSkipsProvidersWithoutChatModel() {
        LLMModelProvider imageProvider = new LLMModelProvider() {

            @Override
            public Provider getProvider() {
                return Provider.STABILITY;
            }

            @Override
            public ImageModel getImageModel() {
                return IMAGE_MODEL;
            }
        };

        Map<Provider, ChatModel> chatModels = LLMModelRegistry.buildChatModels(List.of(imageProvider));

        assertFalse(chatModels.containsKey(Provider.STABILITY));
    }

    @Test
    void testBuildImageModelsIncludesProvidersWithImageModel() {
        LLMModelProvider imageProvider = new LLMModelProvider() {

            @Override
            public Provider getProvider() {
                return Provider.STABILITY;
            }

            @Override
            public ImageModel getImageModel() {
                return IMAGE_MODEL;
            }
        };

        Map<Provider, ImageModel> imageModels = LLMModelRegistry.buildImageModels(List.of(imageProvider));

        assertSame(IMAGE_MODEL, imageModels.get(Provider.STABILITY));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:components:ai:llm:test --tests "com.bytechef.component.ai.llm.LLMModelRegistryTest"`
Expected: FAIL — compilation error, `LLMModelProvider` / `LLMModelRegistry` do not exist.

- [ ] **Step 3: Create the SPI interface**

Create `LLMModelProvider.java`:

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

package com.bytechef.component.ai.llm;

import org.jspecify.annotations.Nullable;

/**
 * Self-registering provider of LLM chat and/or image models. Each LLM provider sub-module ships one
 * {@code @AutoService(LLMModelProvider.class)} implementation; the universal AI components discover them via
 * {@link LLMModelRegistry}.
 *
 * @author Ivica Cardic
 */
public interface LLMModelProvider {

    Provider getProvider();

    @Nullable
    default ChatModel getChatModel() {
        return null;
    }

    @Nullable
    default ImageModel getImageModel() {
        return null;
    }
}
```

- [ ] **Step 4: Create the registry**

Create `LLMModelRegistry.java`:

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

package com.bytechef.component.ai.llm;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Builds {@link Provider}-keyed maps of {@link ChatModel} and {@link ImageModel} from all
 * {@link LLMModelProvider} implementations discovered on the classpath via {@link ServiceLoader}.
 *
 * @author Ivica Cardic
 */
public final class LLMModelRegistry {

    private static final Map<Provider, ChatModel> CHAT_MODELS;
    private static final Map<Provider, ImageModel> IMAGE_MODELS;

    static {
        List<LLMModelProvider> llmModelProviders = new ArrayList<>();

        ServiceLoader.load(LLMModelProvider.class)
            .forEach(llmModelProviders::add);

        CHAT_MODELS = buildChatModels(llmModelProviders);
        IMAGE_MODELS = buildImageModels(llmModelProviders);
    }

    private LLMModelRegistry() {
    }

    public static ChatModel getChatModel(Provider provider) {
        ChatModel chatModel = CHAT_MODELS.get(provider);

        if (chatModel == null) {
            throw new IllegalArgumentException("No chat model registered for provider: " + provider);
        }

        return chatModel;
    }

    public static ImageModel getImageModel(Provider provider) {
        ImageModel imageModel = IMAGE_MODELS.get(provider);

        if (imageModel == null) {
            throw new IllegalArgumentException("No image model registered for provider: " + provider);
        }

        return imageModel;
    }

    static Map<Provider, ChatModel> buildChatModels(Iterable<LLMModelProvider> llmModelProviders) {
        Map<Provider, ChatModel> chatModels = new EnumMap<>(Provider.class);

        for (LLMModelProvider llmModelProvider : llmModelProviders) {
            ChatModel chatModel = llmModelProvider.getChatModel();

            if (chatModel != null) {
                chatModels.put(llmModelProvider.getProvider(), chatModel);
            }
        }

        return chatModels;
    }

    static Map<Provider, ImageModel> buildImageModels(Iterable<LLMModelProvider> llmModelProviders) {
        Map<Provider, ImageModel> imageModels = new EnumMap<>(Provider.class);

        for (LLMModelProvider llmModelProvider : llmModelProviders) {
            ImageModel imageModel = llmModelProvider.getImageModel();

            if (imageModel != null) {
                imageModels.put(llmModelProvider.getProvider(), imageModel);
            }
        }

        return imageModels;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:modules:components:ai:llm:spotlessApply :server:libs:modules:components:ai:llm:test --tests "com.bytechef.component.ai.llm.LLMModelRegistryTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/LLMModelProvider.java \
        server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/LLMModelRegistry.java \
        server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/LLMModelRegistryTest.java
git commit -m "Add LLMModelProvider SPI and LLMModelRegistry"
```

---

### Task 2: Generic API-key fallback accessor

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (the `Ai.Provider` class, around line 1262–1421)
- Test: `server/libs/config/app-config/src/test/java/com/bytechef/config/AiProviderApiKeyTest.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `ApplicationProperties.Ai.Provider.getProviderApiKey(String providerKey) -> @Nullable String`

- [ ] **Step 1: Write the failing test**

Create `AiProviderApiKeyTest.java`:

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

package com.bytechef.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.config.ApplicationProperties.Ai.Provider;
import org.junit.jupiter.api.Test;

class AiProviderApiKeyTest {

    @Test
    void testGetProviderApiKeyReturnsConfiguredKey() {
        Provider provider = new Provider();

        provider.getOpenAi()
            .setApiKey("sk-open-ai");

        assertEquals("sk-open-ai", provider.getProviderApiKey("ai.provider.openAi"));
    }

    @Test
    void testGetProviderApiKeyReturnsNullForUnknownKey() {
        Provider provider = new Provider();

        assertNull(provider.getProviderApiKey("ai.provider.unknown"));
    }
}
```

> Note: if `OpenAi.setApiKey(String)` is absent (it is a `@ConfigurationProperties` bean and should have one), add the standard setter alongside the existing `getApiKey()`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:config:app-config:test --tests "com.bytechef.config.AiProviderApiKeyTest"`
Expected: FAIL — `getProviderApiKey` does not exist.

- [ ] **Step 3: Add the method**

In `ApplicationProperties.java`, inside `public static class Provider` (after `getVertexGemini()`, before the setters block at ~line 1421), add:

```java
            @org.jspecify.annotations.Nullable
            public String getProviderApiKey(String providerKey) {
                return switch (providerKey) {
                    case "ai.provider.anthropic" -> anthropic.getApiKey();
                    case "ai.provider.azureOpenAi" -> azureOpenAi.getApiKey();
                    case "ai.provider.deepseek" -> deepSeek.getApiKey();
                    case "ai.provider.groq" -> groq.getApiKey();
                    case "ai.provider.mistral" -> mistral.getApiKey();
                    case "ai.provider.nvidia" -> nvidia.getApiKey();
                    case "ai.provider.openAi" -> openAi.getApiKey();
                    case "ai.provider.perplexity" -> perplexity.getApiKey();
                    case "ai.provider.stability" -> stability.getApiKey();
                    case "ai.provider.vertexGemini" -> vertexGemini.getApiKey();
                    default -> null;
                };
            }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:config:app-config:spotlessApply :server:libs:config:app-config:test --tests "com.bytechef.config.AiProviderApiKeyTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java \
        server/libs/config/app-config/src/test/java/com/bytechef/config/AiProviderApiKeyTest.java
git commit -m "Add generic getProviderApiKey accessor to Ai.Provider"
```

---

### Task 3: Text provider contributors + rewrite `AiTextActionDefinition`

**Files:**
- Create (9 contributors, one per text provider; azure & open-ai also declare image):
  - `…/ai/llm/anthropic/src/main/java/com/bytechef/component/ai/llm/anthropic/AnthropicModelProvider.java`
  - `…/ai/llm/azure-open-ai/src/main/java/com/bytechef/component/ai/llm/azure/openai/AzureOpenAiModelProvider.java`
  - `…/ai/llm/deepseek/src/main/java/com/bytechef/component/ai/llm/deepseek/DeepSeekModelProvider.java`
  - `…/ai/llm/gemini/src/main/java/com/bytechef/component/ai/llm/gemini/GeminiModelProvider.java`
  - `…/ai/llm/groq/src/main/java/com/bytechef/component/ai/llm/groq/GroqModelProvider.java`
  - `…/ai/llm/mistral/src/main/java/com/bytechef/component/ai/llm/mistral/MistralModelProvider.java`
  - `…/ai/llm/nvidia/src/main/java/com/bytechef/component/ai/llm/nvidia/NvidiaModelProvider.java`
  - `…/ai/llm/open-ai/src/main/java/com/bytechef/component/ai/llm/openai/OpenAiModelProvider.java`
  - `…/ai/llm/perplexity/src/main/java/com/bytechef/component/ai/llm/perplexity/PerplexityModelProvider.java`
- Modify: `…/ai/universal/universal-text/src/main/java/com/bytechef/component/ai/universal/text/action/definition/AiTextActionDefinition.java`
- Test: `…/ai/universal/universal-text/src/test/java/com/bytechef/component/ai/universal/text/LLMTextProviderRegistryIntTest.java`

**Interfaces:**
- Consumes: `LLMModelProvider`, `LLMModelRegistry.getChatModel(Provider)` (Task 1); `ApplicationProperties.Ai.Provider.getProviderApiKey(String)` (Task 2).
- Produces: each `XxxModelProvider` registered via `@AutoService(LLMModelProvider.class)`.

- [ ] **Step 1: Write the failing test**

Create `LLMTextProviderRegistryIntTest.java`:

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

package com.bytechef.component.ai.universal.text;

import static com.bytechef.component.ai.llm.Provider.ANTHROPIC;
import static com.bytechef.component.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.DEEPSEEK;
import static com.bytechef.component.ai.llm.Provider.GROQ;
import static com.bytechef.component.ai.llm.Provider.MISTRAL;
import static com.bytechef.component.ai.llm.Provider.NVIDIA;
import static com.bytechef.component.ai.llm.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.PERPLEXITY;
import static com.bytechef.component.ai.llm.Provider.VERTEX_GEMINI;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.groq.action.GroqChatAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class LLMTextProviderRegistryIntTest {

    @Test
    void testAllTextProvidersResolveToAChatModel() {
        List<Provider> textProviders = List.of(
            ANTHROPIC, AZURE_OPEN_AI, DEEPSEEK, GROQ, MISTRAL, NVIDIA, OPEN_AI, PERPLEXITY, VERTEX_GEMINI);

        for (Provider provider : textProviders) {
            assertNotNull(LLMModelRegistry.getChatModel(provider), "No chat model for " + provider);
        }
    }

    @Test
    void testGroqResolvesToItsOwnChatModel() {
        assertSame(GroqChatAction.CHAT_MODEL, LLMModelRegistry.getChatModel(GROQ));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:components:ai:universal:universal-text:test --tests "com.bytechef.component.ai.universal.text.LLMTextProviderRegistryIntTest"`
Expected: FAIL — `IllegalArgumentException: No chat model registered for provider: ANTHROPIC` (no contributors yet).

- [ ] **Step 3: Create the chat-only contributors**

`AnthropicModelProvider.java` (template — repeat for deepseek, gemini, groq, mistral, nvidia, perplexity, swapping package, class name, `Provider` constant, and `XxxChatAction`):

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

package com.bytechef.component.ai.llm.anthropic;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.LLMModelProvider;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.anthropic.action.AnthropicChatAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(LLMModelProvider.class)
public class AnthropicModelProvider implements LLMModelProvider {

    @Override
    public Provider getProvider() {
        return Provider.ANTHROPIC;
    }

    @Override
    public ChatModel getChatModel() {
        return AnthropicChatAction.CHAT_MODEL;
    }
}
```

Exact mapping for the 7 chat-only contributors:

| File / package | `getProvider()` | `getChatModel()` |
|---|---|---|
| `…/anthropic/AnthropicModelProvider` | `ANTHROPIC` | `AnthropicChatAction.CHAT_MODEL` |
| `…/deepseek/DeepSeekModelProvider` | `DEEPSEEK` | `DeepSeekChatAction.CHAT_MODEL` |
| `…/gemini/GeminiModelProvider` | `VERTEX_GEMINI` | `GeminiChatAction.CHAT_MODEL` |
| `…/groq/GroqModelProvider` | `GROQ` | `GroqChatAction.CHAT_MODEL` |
| `…/mistral/MistralModelProvider` | `MISTRAL` | `MistralChatAction.CHAT_MODEL` |
| `…/nvidia/NvidiaModelProvider` | `NVIDIA` | `NvidiaChatAction.CHAT_MODEL` |
| `…/perplexity/PerplexityModelProvider` | `PERPLEXITY` | `PerplexityChatAction.CHAT_MODEL` |

> The `…/gemini/` package is `com.bytechef.component.ai.llm.gemini`; its action import is `com.bytechef.component.ai.llm.gemini.action.GeminiChatAction`. The `…/azure-open-ai/` package is `com.bytechef.component.ai.llm.azure.openai`.

- [ ] **Step 4: Create the dual-capability contributors (azure + open-ai)**

`OpenAiModelProvider.java`:

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

package com.bytechef.component.ai.llm.openai;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.LLMModelProvider;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.openai.action.OpenAiChatAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiCreateImageAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(LLMModelProvider.class)
public class OpenAiModelProvider implements LLMModelProvider {

    @Override
    public Provider getProvider() {
        return Provider.OPEN_AI;
    }

    @Override
    public ChatModel getChatModel() {
        return OpenAiChatAction.CHAT_MODEL;
    }

    @Override
    public ImageModel getImageModel() {
        return OpenAiCreateImageAction.IMAGE_MODEL;
    }
}
```

`AzureOpenAiModelProvider.java` — same shape in package `com.bytechef.component.ai.llm.azure.openai`, returning `Provider.AZURE_OPEN_AI`, `AzureOpenAiChatAction.CHAT_MODEL` (import `…azure.openai.action.AzureOpenAiChatAction`), `AzureOpenAiCreateImageAction.IMAGE_MODEL` (import `…azure.openai.action.AzureOpenAiCreateImageAction`).

- [ ] **Step 5: Rewrite `AiTextActionDefinition`**

Replace the whole file body so that imports, `perform()`, token resolution, and registry lookup read as below; **delete** the commented Amazon Bedrock block, the `getChatModel(...)` switch, and all nine `getXxxChatModel` helpers:

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

package com.bytechef.component.ai.universal.text.action.definition;

import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.universal.text.action.AiTextAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marko Kriskovic
 */
public class AiTextActionDefinition extends AbstractActionDefinitionWrapper {

    private final ApplicationProperties.Ai.Provider aiProvider;
    private final AiTextAction aiTextAction;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public AiTextActionDefinition(
        ActionDefinition actionDefinition, ApplicationProperties.Ai.Provider aiProvider, AiTextAction aiTextAction,
        PropertyService propertyService) {

        super(actionDefinition);

        this.aiProvider = aiProvider;
        this.aiTextAction = aiTextAction;
        this.propertyService = propertyService;
    }

    @Override
    public Optional<BasePerformFunction> getPerform() {
        return Optional.of((PerformFunction) this::perform);
    }

    protected Object perform(Parameters inputParameters, Parameters connectionParameter, ActionContext context) {
        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        ActionContextAware actionContextAware = (ActionContextAware) context;

        Long environmentId = actionContextAware.getEnvironmentId();

        List<String> providerKeys = Arrays.stream(Provider.values())
            .map(Provider::getKey)
            .toList();

        List<String> activeProviderKeys = propertyService.getProperties(providerKeys, Scope.PLATFORM, null, environmentId)
            .stream()
            .filter(property -> property.getValue() != null && property.isEnabled())
            .map(Property::getKey)
            .toList();

        Provider provider = Provider.valueOf(inputParameters.getRequiredString(PROVIDER));

        String token = resolveToken(provider, activeProviderKeys, environmentId);

        Parameters modelInputParameters = aiTextAction.createParameters(inputParameters);

        modelConnectionParametersMap.put(TOKEN, token);

        ChatModel chatModel = LLMModelRegistry.getChatModel(provider);

        return chatModel.getResponse(
            modelInputParameters, ParametersFactory.create(modelConnectionParametersMap), context, false,
            modelInputParameters.containsPath("response.responseFormat"));
    }

    private String resolveToken(Provider provider, List<String> activeProviderKeys, Long environmentId) {
        String providerKey = provider.getKey();

        String token = activeProviderKeys.stream()
            .filter(providerKey::equals)
            .findFirst()
            .map(key -> propertyService.getProperty(key, Scope.PLATFORM, null, environmentId))
            .map(property -> (String) property.get("apiKey"))
            .orElse(null);

        if (token == null) {
            token = aiProvider.getProviderApiKey(providerKey);
        }

        return token;
    }
}
```

- [ ] **Step 6: Run the registry test and the existing handler snapshot test**

Run: `./gradlew :server:libs:modules:components:ai:universal:universal-text:spotlessApply :server:libs:modules:components:ai:universal:universal-text:test`
Expected: PASS — `LLMTextProviderRegistryIntTest` (2 tests) green; `AiTextComponentHandlerTest` snapshot test still green (definitions unchanged).

> If `AiTextComponentHandlerTest` fails on a stale snapshot, delete the JSON under `src/test/resources/definition/` AND `build/resources/test/definition/` for the AI text component, then rerun.

- [ ] **Step 7: Commit**

```bash
git add server/libs/modules/components/ai/llm/anthropic server/libs/modules/components/ai/llm/azure-open-ai \
        server/libs/modules/components/ai/llm/deepseek server/libs/modules/components/ai/llm/gemini \
        server/libs/modules/components/ai/llm/groq server/libs/modules/components/ai/llm/mistral \
        server/libs/modules/components/ai/llm/nvidia server/libs/modules/components/ai/llm/open-ai \
        server/libs/modules/components/ai/llm/perplexity \
        server/libs/modules/components/ai/universal/universal-text
git commit -m "Wire universal-text to LLMModelRegistry; fix Groq routing to its own chat model"
```

---

### Task 4: Image provider contributor + rewrite `AiImageActionDefinition`

**Files:**
- Create: `…/ai/llm/stability/src/main/java/com/bytechef/component/ai/llm/stability/StabilityModelProvider.java`
- Modify: `…/ai/universal/universal-image/src/main/java/com/bytechef/component/ai/universal/image/action/definition/AiImageActionDefinition.java`
- Test: `…/ai/universal/universal-image/src/test/java/com/bytechef/component/ai/universal/image/LLMImageProviderRegistryTest.java`

**Interfaces:**
- Consumes: `LLMModelRegistry.getImageModel(Provider)` (Task 1); `getProviderApiKey` (Task 2); the azure/open-ai dual contributors (Task 3).
- Produces: `StabilityModelProvider` registered via `@AutoService`.

- [ ] **Step 1: Write the failing test**

Create `LLMImageProviderRegistryTest.java`:

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

package com.bytechef.component.ai.universal.image;

import static com.bytechef.component.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.STABILITY;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.llm.Provider;
import java.util.List;
import org.junit.jupiter.api.Test;

class LLMImageProviderRegistryTest {

    @Test
    void testAllImageProvidersResolveToAnImageModel() {
        List<Provider> imageProviders = List.of(AZURE_OPEN_AI, OPEN_AI, STABILITY);

        for (Provider provider : imageProviders) {
            assertNotNull(LLMModelRegistry.getImageModel(provider), "No image model for " + provider);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:components:ai:universal:universal-image:test --tests "com.bytechef.component.ai.universal.image.LLMImageProviderRegistryTest"`
Expected: FAIL — `IllegalArgumentException: No image model registered for provider: STABILITY` (azure/open-ai already resolve from Task 3; Stability does not yet).

- [ ] **Step 3: Create the Stability contributor**

`StabilityModelProvider.java`:

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

package com.bytechef.component.ai.llm.stability;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.LLMModelProvider;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.stability.action.StabilityCreateImageAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(LLMModelProvider.class)
public class StabilityModelProvider implements LLMModelProvider {

    @Override
    public Provider getProvider() {
        return Provider.STABILITY;
    }

    @Override
    public ImageModel getImageModel() {
        return StabilityCreateImageAction.IMAGE_MODEL;
    }
}
```

- [ ] **Step 4: Rewrite `AiImageActionDefinition`**

Replace the file body; **delete** the three `getXxxImageModel` helpers and the `switch`:

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

package com.bytechef.component.ai.universal.image.action.definition;

import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.universal.image.action.AiImageAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marko Kriskovic
 */
public class AiImageActionDefinition extends AbstractActionDefinitionWrapper {

    private final AiImageAction aiImageAction;
    private final ApplicationProperties.Ai.Provider aiProvider;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public AiImageActionDefinition(
        ActionDefinition actionDefinition, ApplicationProperties.Ai.Provider aiProvider,
        AiImageAction aiImageAction, PropertyService propertyService) {

        super(actionDefinition);

        this.aiImageAction = aiImageAction;
        this.aiProvider = aiProvider;
        this.propertyService = propertyService;
    }

    @Override
    public Optional<BasePerformFunction> getPerform() {
        return Optional.of((PerformFunction) this::perform);
    }

    protected Object perform(Parameters inputParameters, Parameters connectionParameter, ActionContext context) {
        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        ActionContextAware actionContextAware = (ActionContextAware) context;

        Long environmentId = actionContextAware.getEnvironmentId();

        List<String> activeProviderKeys = propertyService.getProperties(
            Arrays.stream(Provider.values())
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null, environmentId)
            .stream()
            .filter(property -> property.getValue() != null && property.isEnabled())
            .map(Property::getKey)
            .toList();

        Provider provider = Provider.valueOf(inputParameters.getRequiredString(PROVIDER));

        String token = resolveToken(provider, activeProviderKeys, environmentId);

        modelConnectionParametersMap.put(TOKEN, token);

        Parameters modelInputParameters = aiImageAction.createParameters(inputParameters);
        Parameters modelConnectionParameters = ParametersFactory.create(modelConnectionParametersMap);

        ImageModel imageModel = LLMModelRegistry.getImageModel(provider);

        return imageModel.getResponse(modelInputParameters, modelConnectionParameters);
    }

    private String resolveToken(Provider provider, List<String> activeProviderKeys, Long environmentId) {
        String providerKey = provider.getKey();

        String token = activeProviderKeys.stream()
            .filter(providerKey::equals)
            .findFirst()
            .map(key -> propertyService.getProperty(key, Scope.PLATFORM, null, environmentId))
            .map(property -> (String) property.get("apiKey"))
            .orElse(null);

        if (token == null) {
            token = aiProvider.getProviderApiKey(providerKey);
        }

        return token;
    }
}
```

- [ ] **Step 5: Run the image registry test and existing handler snapshot test**

Run: `./gradlew :server:libs:modules:components:ai:universal:universal-image:spotlessApply :server:libs:modules:components:ai:universal:universal-image:test`
Expected: PASS — `LLMImageProviderRegistryTest` green; `AiImageComponentHandlerTest` snapshot still green.

> Stale-snapshot recovery: same as Task 3 — delete the AI image definition JSON under both `src/test/resources/definition/` and `build/resources/test/definition/`, rerun.

- [ ] **Step 6: Commit**

```bash
git add server/libs/modules/components/ai/llm/stability \
        server/libs/modules/components/ai/universal/universal-image
git commit -m "Wire universal-image to LLMModelRegistry"
```

---

### Task 5: Full check

**Files:** none (verification only).

- [ ] **Step 1: Run the AI module checks**

Run:
```bash
./gradlew :server:libs:modules:components:ai:llm:check \
          :server:libs:config:app-config:check \
          :server:libs:modules:components:ai:universal:universal-text:check \
          :server:libs:modules:components:ai:universal:universal-image:check
```
Expected: BUILD SUCCESSFUL (Checkstyle, PMD, SpotBugs, Spotless, tests all pass).

- [ ] **Step 2: Verify the old per-provider dispatch is gone**

Run: `grep -rn "getOpenAiChatModel\|getStabilityImageModel\|PerplexityChatAction.CHAT_MODEL" server/libs/modules/components/ai/universal`
Expected: no matches (helpers deleted; the Groq→Perplexity mis-route no longer exists).

- [ ] **Step 3: Commit (only if check produced formatting changes)**

```bash
git add -A server/libs/modules/components/ai server/libs/config/app-config
git commit -m "Apply formatting from full check"
```

---

## Self-Review

- **Spec coverage:** SPI (Task 1) ✓; ServiceLoader registry (Task 1) ✓; modality-intrinsic split (Task 1 `buildChatModels`/`buildImageModels` skip nulls) ✓; per-provider contributors text+image (Tasks 3–4) ✓; generic `getProviderApiKey` fallback (Task 2) ✓; both definitions rewritten, switches+helpers deleted (Tasks 3–4) ✓; Groq fix (Task 3) ✓; tests incl. registry, accessor, Groq regression, snapshot greenness (all tasks) ✓; migration order matches spec (SPI → contributors → accessor → rewrites) ✓.
- **Placeholder scan:** no TBD/TODO; all code blocks complete; the 7 chat-only contributors are given as one template + an exact mapping table (not "similar to Task N").
- **Type consistency:** `LLMModelProvider.getChatModel()/getImageModel()` (Task 1) match contributor `@Override`s (Tasks 3–4) and registry consumption; `getProviderApiKey(String)` (Task 2) matches `resolveToken` calls (Tasks 3–4); `LLMModelRegistry.getChatModel/getImageModel(Provider)` signatures consistent across consumers.
- **Known assumption flagged:** Task 2 Step 1 notes `OpenAi.setApiKey` must exist (standard for `@ConfigurationProperties`); add it if missing.
