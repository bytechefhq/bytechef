# Catalog-resolved Knowledge Base Embedding Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the server boot without a statically-configured embedding provider, and resolve the OpenAI embedding API key at call time from the UI-activated provider, so the Knowledge Base starts working once a provider is activated — no restart.

**Architecture:** Introduce an always-present, lazily-resolving `EmbeddingModel` bean (`CatalogEmbeddingModel`, EE) that reads the current environment from a new `EnvironmentContext` ThreadLocal, resolves the OpenAI API key via a shared `ProviderApiKeyResolver` (extracted from the chat resolver), builds the embedding model with a `CatalogEmbeddingModelFactory` (mirroring `CatalogChatModelFactory`), and delegates. The KB / Copilot / ContextStore vector-store configs are unchanged; they just always find an `EmbeddingModel`.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI 2.0.0, Spring Data JDBC, JUnit 5 + Mockito + AssertJ.

## Global Constraints

- EE source files (`server/ee/...`) use the **ByteChef Enterprise license header** (not Apache 2.0) and carry a `@version ee` Javadoc tag. CE files (`server/libs/...`) use the Apache 2.0 header.
- Run `./gradlew spotlessApply` before every commit; code must pass `checkstyleMain/Test`, `pmdMain/Test`, `spotbugsMain/Test`.
- Test class names: unit tests end in `Test` (never `IntTest`); test method names are camelCase without underscores.
- No method-name `_` prefixes; clear, descriptive variable names (no single letters).
- One blank line before control statements and after a variable modification that precedes its use (per CLAUDE.md Java style).
- Embedding model name and dimensions stay in `application.yml`
  (`bytechef.ai.provider.embedding.openai.options.model: text-embedding-3-small`,
  `spring.ai.vectorstore.pgvector.dimensions: 1536`). This plan does NOT change them.
- `Environment` enum (`com.bytechef.platform.configuration.domain.Environment`) values are `DEVELOPMENT, STAGING, PRODUCTION` (ordinals 0,1,2). The `EnvironmentContext` default-when-unset is `PRODUCTION`.

---

## File Structure

New:
- `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/context/EnvironmentContext.java` — ThreadLocal environment holder (CE, Apache header).
- `server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/ProviderApiKeyResolver.java` — shared API-key resolver (EE).
- `.../catalog/CatalogEmbeddingModelFactory.java` — builds an `EmbeddingModel` for a catalog provider (EE).
- `.../catalog/CatalogEmbeddingModel.java` — always-present `@Primary` delegating `EmbeddingModel` (EE).
- Matching `*Test.java` for each.

Modified:
- `server/libs/modules/components/ai/llm/open-ai/.../cluster/OpenAiEmbedding.java` — expose a public `EMBEDDING_MODEL` constant (Apache header, unchanged).
- `.../catalog/CatalogChatClientResolverImpl.java` — delegate key resolution to `ProviderApiKeyResolver` (no behavior change).
- `server/libs/platform/platform-knowledge-base/platform-knowledge-base-worker/.../etl/KnowledgeBaseVectorStoreWriter.java` — set `EnvironmentContext` around `vectorStore.add(...)`.
- `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/.../facade/KnowledgeBaseFacadeImpl.java` — set `EnvironmentContext` around `similaritySearch(...)`; inject `KnowledgeBaseService`.

Unchanged (verified): the four pgvector configs keep injecting `EmbeddingModel`.

---

## Task 1: `EnvironmentContext` ThreadLocal holder

**Files:**
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/context/EnvironmentContext.java`
- Test: `server/libs/platform/platform-configuration/platform-configuration-api/src/test/java/com/bytechef/platform/configuration/context/EnvironmentContextTest.java`

**Interfaces:**
- Produces: `EnvironmentContext.set(Environment)`, `EnvironmentContext.set(int ordinal)`, `Environment EnvironmentContext.getCurrentEnvironment()`, `EnvironmentContext.clear()`.

- [ ] **Step 1: Verify test deps exist**

Run: `grep -nE "junit|assertj" server/libs/platform/platform-configuration/platform-configuration-api/build.gradle.kts`
Expected: `junit-jupiter` and `assertj-core` present. If absent, add to `build.gradle.kts`:
```kotlin
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
```

- [ ] **Step 2: Write the failing test**

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

package com.bytechef.platform.configuration.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EnvironmentContextTest {

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testDefaultsToProductionWhenUnset() {
        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testReturnsSetEnvironment() {
        EnvironmentContext.set(Environment.STAGING);

        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.STAGING);
    }

    @Test
    void testSetByOrdinal() {
        EnvironmentContext.set(0);

        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testClearResetsToDefault() {
        EnvironmentContext.set(Environment.DEVELOPMENT);
        EnvironmentContext.clear();

        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:test --tests "com.bytechef.platform.configuration.context.EnvironmentContextTest"`
Expected: FAIL — `EnvironmentContext` does not exist (compile error).

- [ ] **Step 4: Write the implementation**

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

package com.bytechef.platform.configuration.context;

import com.bytechef.platform.configuration.domain.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-bound current {@link Environment}, parallel to {@code TenantContext}. Set by callers around operations that
 * need the environment but cannot receive it as a parameter (e.g. Spring AI {@code VectorStore.add()} →
 * {@code EmbeddingModel.embed()}, where the embedding API key is environment-scoped). Defaults to
 * {@link Environment#PRODUCTION} when unset.
 *
 * @author Ivica Cardic
 */
public final class EnvironmentContext {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentContext.class);

    private static final ThreadLocal<Environment> CONTEXT = new ThreadLocal<>();

    private EnvironmentContext() {
    }

    public static void set(Environment environment) {
        CONTEXT.set(environment);
    }

    public static void set(int ordinal) {
        Environment[] environments = Environment.values();

        if (ordinal < 0 || ordinal >= environments.length) {
            throw new IllegalArgumentException("Invalid environment ordinal: " + ordinal);
        }

        CONTEXT.set(environments[ordinal]);
    }

    public static Environment getCurrentEnvironment() {
        Environment environment = CONTEXT.get();

        if (environment == null) {
            logger.warn("No environment set in EnvironmentContext; defaulting to {}", Environment.PRODUCTION);

            return Environment.PRODUCTION;
        }

        return environment;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:test --tests "com.bytechef.platform.configuration.context.EnvironmentContextTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Format and commit**

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:spotlessApply
git add server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/context/EnvironmentContext.java \
  server/libs/platform/platform-configuration/platform-configuration-api/src/test/java/com/bytechef/platform/configuration/context/EnvironmentContextTest.java \
  server/libs/platform/platform-configuration/platform-configuration-api/build.gradle.kts
git commit -m "$(printf '%s\n\n%s' '- Add EnvironmentContext thread-local for environment-scoped embedding resolution' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 2: Extract `ProviderApiKeyResolver` from the chat resolver

**Files:**
- Create: `server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/ProviderApiKeyResolver.java`
- Modify: `.../catalog/CatalogChatClientResolverImpl.java` (replace private `resolveApiKey`/`configApiKey` with delegation)
- Test: `.../catalog/ProviderApiKeyResolverTest.java`

**Interfaces:**
- Consumes: `com.bytechef.platform.configuration.service.PropertyService`, `com.bytechef.config.ApplicationProperties`, `com.bytechef.component.ai.llm.Provider`.
- Produces: `@Nullable String ProviderApiKeyResolver.resolve(Provider provider, int environment)` — returns the enabled property's `apiKey`, else the static config key, else `null`.

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProviderApiKeyResolverTest {

    private final PropertyService propertyService = mock(PropertyService.class);
    private final ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);
    private final ProviderApiKeyResolver resolver =
        new ProviderApiKeyResolver(propertyService, applicationProperties);

    @Test
    void testReturnsEnabledPropertyApiKey() {
        Property property = mock(Property.class);

        when(property.isEnabled()).thenReturn(true);
        when(property.get("apiKey")).thenReturn("sk-from-ui");
        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 2L))
            .thenReturn(Optional.of(property));

        assertThat(resolver.resolve(Provider.OPEN_AI, 2)).isEqualTo("sk-from-ui");
    }

    @Test
    void testFallsBackToConfigWhenNoEnabledProperty() {
        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 2L))
            .thenReturn(Optional.empty());
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn("sk-from-config");

        assertThat(resolver.resolve(Provider.OPEN_AI, 2)).isEqualTo("sk-from-config");
    }

    @Test
    void testReturnsNullWhenNeitherPresent() {
        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 2L))
            .thenReturn(Optional.empty());
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn(null);

        assertThat(resolver.resolve(Provider.OPEN_AI, 2)).isNull();
    }
}
```

Add the missing import `import static org.mockito.Mockito.RETURNS_DEEP_STUBS;` at the top.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test --tests "com.bytechef.ee.platform.ai.agent.catalog.ProviderApiKeyResolverTest"`
Expected: FAIL — `ProviderApiKeyResolver` does not exist.

- [ ] **Step 3: Write the implementation (move logic verbatim from `CatalogChatClientResolverImpl`)**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves a catalog provider's API key for an environment: the UI-activated platform property
 * ({@link Property#isEnabled()}) wins, falling back to the statically-configured key. Shared by the chat client
 * resolver and the embedding model so the two cannot diverge.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ProviderApiKeyResolver {

    private final PropertyService propertyService;
    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI")
    public ProviderApiKeyResolver(PropertyService propertyService, ApplicationProperties applicationProperties) {
        this.propertyService = propertyService;
        this.applicationProperties = applicationProperties;
    }

    public @Nullable String resolve(Provider provider, int environment) {
        Optional<Property> property = propertyService.fetchProperty(
            provider.getKey(), Scope.PLATFORM, null, (long) environment);

        return property
            .filter(Property::isEnabled)
            .map(enabledProperty -> enabledProperty.get("apiKey"))
            .map(Object::toString)
            .filter(apiKey -> !apiKey.isBlank())
            .orElseGet(() -> configApiKey(provider));
    }

    private @Nullable String configApiKey(Provider provider) {
        ApplicationProperties.Ai.Provider configProvider = applicationProperties.getAi()
            .getProvider();

        return switch (provider) {
            case OPEN_AI -> configProvider.getOpenAi()
                .getApiKey();
            case ANTHROPIC -> configProvider.getAnthropic()
                .getApiKey();
            case MISTRAL -> configProvider.getMistral()
                .getApiKey();
            case VERTEX_GEMINI -> configProvider.getVertexGemini()
                .getApiKey();
            case GROQ -> configProvider.getGroq()
                .getApiKey();
            case PERPLEXITY -> configProvider.getPerplexity()
                .getApiKey();
            case NVIDIA -> configProvider.getNvidia()
                .getApiKey();
            case DEEPSEEK -> configProvider.getDeepSeek()
                .getApiKey();
            default -> null;
        };
    }
}
```

- [ ] **Step 4: Refactor `CatalogChatClientResolverImpl` to delegate**

In `CatalogChatClientResolverImpl.java`:
1. Add a `private final ProviderApiKeyResolver providerApiKeyResolver;` field; add it to the constructor parameters and assignment.
2. Replace the body of `resolveApiKey(provider, environment)` usage: delete the private `resolveApiKey` and `configApiKey` methods, and at the call site replace `String apiKey = resolveApiKey(provider, environment);` with:
```java
        String apiKey = providerApiKeyResolver.resolve(provider, environment);
```
3. Remove now-unused imports (`Property`, `Property.Scope`, `Optional`, `ApplicationProperties` if no longer referenced — keep `ApplicationProperties` only if still used elsewhere in the class; after this change it is not, so remove it and drop it from the constructor).

- [ ] **Step 4b: Update the existing `CatalogChatClientResolverTest` for the new constructor**

The constructor signature changed (removed `ApplicationProperties`, added `ProviderApiKeyResolver`), so the existing test no longer compiles. Open `.../catalog/CatalogChatClientResolverTest.java` and:
1. Replace the `ApplicationProperties` mock/field with a `ProviderApiKeyResolver providerApiKeyResolver = mock(ProviderApiKeyResolver.class);`.
2. Update the `new CatalogChatClientResolverImpl(...)` construction to pass `(propertyService_or_removed, catalogChatModelFactory, providerApiKeyResolver)` — match the new parameter order you defined in Step 2 (if `PropertyService` is no longer used directly by the impl, drop it from the impl constructor and the test too).
3. Replace any test stubbing that drove key resolution through `PropertyService`/`ApplicationProperties` with `when(providerApiKeyResolver.resolve(provider, environment)).thenReturn("sk-...")` (and `thenReturn(null)` for the "disabled/absent" cases). The key-resolution unit coverage now lives in `ProviderApiKeyResolverTest`; this test only needs the resolver to return a key or null.

- [ ] **Step 5: Run the new test plus the existing chat resolver tests (parity)**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test --tests "com.bytechef.ee.platform.ai.agent.catalog.*"`
Expected: PASS — `ProviderApiKeyResolverTest` green AND existing `CatalogChatClientResolverTest` still green (no behavior change).

- [ ] **Step 6: Format and commit**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/ProviderApiKeyResolver.java \
  server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogChatClientResolverImpl.java \
  server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/test/java/com/bytechef/ee/platform/ai/agent/catalog/ProviderApiKeyResolverTest.java \
  server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/test/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogChatClientResolverTest.java
git commit -m "$(printf '%s\n\n%s' '- Extract ProviderApiKeyResolver shared by chat and embedding resolution' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: `CatalogEmbeddingModelFactory` + expose `OpenAiEmbedding.EMBEDDING_MODEL`

**Files:**
- Modify: `server/libs/modules/components/ai/llm/open-ai/src/main/java/com/bytechef/component/ai/llm/openai/cluster/OpenAiEmbedding.java`
- Create: `.../catalog/CatalogEmbeddingModelFactory.java`
- Test: `.../catalog/CatalogEmbeddingModelFactoryTest.java`

**Interfaces:**
- Consumes: `com.bytechef.component.ai.llm.Provider`, `com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction`, `OpenAiEmbedding.EMBEDDING_MODEL`.
- Produces: `@Nullable org.springframework.ai.embedding.EmbeddingModel CatalogEmbeddingModelFactory.createEmbeddingModel(Provider provider, String model, String apiKey)`.

- [ ] **Step 1: Expose a public constant on `OpenAiEmbedding`** (mirrors `OpenAiChatAction.CHAT_MODEL`)

In `OpenAiEmbedding.java`, add a public constant and reuse it in the cluster element definition:
```java
    public static final EmbeddingFunction EMBEDDING_MODEL = OpenAiEmbedding::apply;

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<EmbeddingFunction>clusterElement("embedding")
            .title("OpenAI Embedding")
            .description("OpenAI embedding.")
            .type(EmbeddingFunction.EMBEDDING)
            .object(() -> EMBEDDING_MODEL)
            .properties(
                string(MODEL)
                    .label("Model")
                    .description("ID of the model to use.")
                    .required(true)
                    .options(EMBEDDING_MODELS));
```
(`apply` stays `protected static`; the method reference resolves within the class.)

- [ ] **Step 2: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.llm.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogEmbeddingModelFactoryTest {

    private final CatalogEmbeddingModelFactory factory = new CatalogEmbeddingModelFactory();

    @Test
    void testBuildsOpenAiEmbeddingModel() {
        EmbeddingModel embeddingModel = factory.createEmbeddingModel(
            Provider.OPEN_AI, "text-embedding-3-small", "sk-test");

        assertThat(embeddingModel).isNotNull();
    }

    @Test
    void testReturnsNullForProviderWithoutEmbeddingSupport() {
        assertThat(factory.createEmbeddingModel(Provider.ANTHROPIC, "irrelevant", "sk-test")).isNull();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test --tests "com.bytechef.ee.platform.ai.agent.catalog.CatalogEmbeddingModelFactoryTest"`
Expected: FAIL — `CatalogEmbeddingModelFactory` does not exist.

- [ ] **Step 4: Write the implementation (mirrors `CatalogChatModelFactory`)**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.openai.cluster.OpenAiEmbedding;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * Builds a Spring-AI {@link EmbeddingModel} for a catalog {@link Provider} + model name + platform API key, by reusing
 * the LLM component's existing embedding lambda. Mirrors {@code CatalogChatModelFactory}. Returns {@code null} for
 * providers that do not expose an embedding model.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class CatalogEmbeddingModelFactory {

    public @Nullable EmbeddingModel createEmbeddingModel(Provider provider, String model, String apiKey) {
        EmbeddingFunction embeddingFunction = resolveFactory(provider);

        if (embeddingFunction == null) {
            return null;
        }

        Parameters inputParameters = ParametersFactory.create(Map.of(MODEL, model));
        Parameters connectionParameters = ParametersFactory.create(Map.of(TOKEN, apiKey));

        return embeddingFunction.apply(inputParameters, connectionParameters);
    }

    private static @Nullable EmbeddingFunction resolveFactory(Provider provider) {
        return switch (provider) {
            case OPEN_AI -> OpenAiEmbedding.EMBEDDING_MODEL;
            // Only OpenAI exposes embedding config today (bytechef.ai.provider.embedding.openai). Extend here when
            // another provider gains embedding configuration.
            default -> null;
        };
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test --tests "com.bytechef.ee.platform.ai.agent.catalog.CatalogEmbeddingModelFactoryTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Format and commit**

```bash
./gradlew :server:libs:modules:components:ai:llm:open-ai:spotlessApply :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:spotlessApply
git add server/libs/modules/components/ai/llm/open-ai/src/main/java/com/bytechef/component/ai/llm/openai/cluster/OpenAiEmbedding.java \
  server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogEmbeddingModelFactory.java \
  server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/test/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogEmbeddingModelFactoryTest.java
git commit -m "$(printf '%s\n\n%s' '- Add CatalogEmbeddingModelFactory reusing the OpenAI embedding lambda' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 4: `CatalogEmbeddingModel` — always-present, lazily-resolving `@Primary` bean

**Files:**
- Create: `.../catalog/CatalogEmbeddingModel.java`
- Test: `.../catalog/CatalogEmbeddingModelTest.java`

**Interfaces:**
- Consumes: `ProviderApiKeyResolver.resolve(Provider, int)`, `CatalogEmbeddingModelFactory.createEmbeddingModel(Provider, String, String)`, `EnvironmentContext.getCurrentEnvironment()`, `ApplicationProperties.getAi().getProvider().getEmbedding().getOpenAi().getOptions().getModel()`.
- Produces: a Spring `@Primary` bean of type `org.springframework.ai.embedding.EmbeddingModel` named `catalogEmbeddingModel`.

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogEmbeddingModelTest {

    private final ProviderApiKeyResolver providerApiKeyResolver = mock(ProviderApiKeyResolver.class);
    private final CatalogEmbeddingModelFactory catalogEmbeddingModelFactory =
        mock(CatalogEmbeddingModelFactory.class);
    private final ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

    private final CatalogEmbeddingModel catalogEmbeddingModel = new CatalogEmbeddingModel(
        providerApiKeyResolver, catalogEmbeddingModelFactory, applicationProperties);

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testDelegatesEmbedWhenKeyResolved() {
        stubModelName();
        EmbeddingModel delegate = mock(EmbeddingModel.class);
        Document document = new Document("hello");

        when(delegate.embed(document)).thenReturn(new float[] {0.1f});
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, Environment.PRODUCTION.ordinal()))
            .thenReturn("sk-test");
        when(catalogEmbeddingModelFactory.createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test"))
            .thenReturn(delegate);

        float[] result = catalogEmbeddingModel.embed(document);

        assertThat(result).containsExactly(0.1f);
    }

    @Test
    void testThrowsActionableErrorWhenNoKey() {
        stubModelName();

        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, Environment.PRODUCTION.ordinal()))
            .thenReturn(null);

        assertThatThrownBy(() -> catalogEmbeddingModel.embed(new Document("hello")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No embedding provider is activated")
            .hasMessageContaining("PRODUCTION")
            .hasMessageContaining("bytechef.ai.provider.openai.api-key");
    }

    @Test
    void testReadsEnvironmentFromContext() {
        stubModelName();
        EmbeddingModel delegate = mock(EmbeddingModel.class);
        Document document = new Document("hello");

        EnvironmentContext.set(Environment.STAGING);

        when(delegate.embed(document)).thenReturn(new float[] {0.2f});
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, Environment.STAGING.ordinal()))
            .thenReturn("sk-staging");
        when(catalogEmbeddingModelFactory.createEmbeddingModel(
            Provider.OPEN_AI, "text-embedding-3-small", "sk-staging"))
            .thenReturn(delegate);

        catalogEmbeddingModel.embed(document);

        verify(providerApiKeyResolver).resolve(Provider.OPEN_AI, Environment.STAGING.ordinal());
    }

    @Test
    void testCachesDelegatePerEnvironmentAndKey() {
        stubModelName();
        EmbeddingModel delegate = mock(EmbeddingModel.class);

        when(delegate.embed(org.mockito.ArgumentMatchers.any(Document.class))).thenReturn(new float[] {0.3f});
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, Environment.PRODUCTION.ordinal()))
            .thenReturn("sk-test");
        when(catalogEmbeddingModelFactory.createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test"))
            .thenReturn(delegate);

        catalogEmbeddingModel.embed(new Document("a"));
        catalogEmbeddingModel.embed(new Document("b"));

        verify(catalogEmbeddingModelFactory, times(1))
            .createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test");
    }

    private void stubModelName() {
        when(applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOpenAi()
            .getOptions()
            .getModel()).thenReturn("text-embedding-3-small");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test --tests "com.bytechef.ee.platform.ai.agent.catalog.CatalogEmbeddingModelTest"`
Expected: FAIL — `CatalogEmbeddingModel` does not exist.

- [ ] **Step 3: Write the implementation**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Always-present {@link EmbeddingModel} that resolves the underlying provider model at call time from the
 * UI-activated provider for the current {@link EnvironmentContext} environment. Lets the application boot without a
 * statically-configured embedding provider; the Knowledge Base starts working as soon as a provider is activated.
 *
 * <p>
 * Resolution: the embedding model name is config-pinned ({@code bytechef.ai.provider.embedding.openai}); only the API
 * key is dynamic, resolved via {@link ProviderApiKeyResolver} (activated property → static config). Built models are
 * cached per {@code (environment, apiKey)} so rotating or disabling a key naturally yields a cache miss.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Primary
public class CatalogEmbeddingModel implements EmbeddingModel {

    private final ProviderApiKeyResolver providerApiKeyResolver;
    private final CatalogEmbeddingModelFactory catalogEmbeddingModelFactory;
    private final ApplicationProperties applicationProperties;
    private final Map<String, EmbeddingModel> delegateCache = new ConcurrentHashMap<>();

    @SuppressFBWarnings("EI")
    public CatalogEmbeddingModel(
        ProviderApiKeyResolver providerApiKeyResolver, CatalogEmbeddingModelFactory catalogEmbeddingModelFactory,
        ApplicationProperties applicationProperties) {

        this.providerApiKeyResolver = providerApiKeyResolver;
        this.catalogEmbeddingModelFactory = catalogEmbeddingModelFactory;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return resolveDelegate().call(request);
    }

    @Override
    public float[] embed(Document document) {
        return resolveDelegate().embed(document);
    }

    @Override
    public float[] embed(String text) {
        return resolveDelegate().embed(text);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return resolveDelegate().embed(texts);
    }

    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
        return resolveDelegate().embed(documents, options, batchingStrategy);
    }

    @Override
    public int dimensions() {
        return resolveDelegate().dimensions();
    }

    private EmbeddingModel resolveDelegate() {
        Environment environment = EnvironmentContext.getCurrentEnvironment();

        String model = applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOpenAi()
            .getOptions()
            .getModel();

        String apiKey = providerApiKeyResolver.resolve(Provider.OPEN_AI, environment.ordinal());

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "No embedding provider is activated for environment " + environment + ". Activate the OpenAI " +
                    "provider in the UI (or set bytechef.ai.provider.openai.api-key) so the Knowledge Base can embed " +
                    "documents.");
        }

        return delegateCache.computeIfAbsent(
            environment.ordinal() + ":" + apiKey,
            ignoredKey -> catalogEmbeddingModelFactory.createEmbeddingModel(Provider.OPEN_AI, model, apiKey));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test --tests "com.bytechef.ee.platform.ai.agent.catalog.CatalogEmbeddingModelTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Run the module `check` (Checkstyle/PMD/SpotBugs)**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Format and commit**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogEmbeddingModel.java \
  server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/test/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogEmbeddingModelTest.java
git commit -m "$(printf '%s\n\n%s' '- Add CatalogEmbeddingModel resolving the embedding key from the activated provider' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 5: Set `EnvironmentContext` around ingestion writes

**Files:**
- Modify: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-worker/src/main/java/com/bytechef/platform/knowledgebase/worker/etl/KnowledgeBaseVectorStoreWriter.java`
- Test: `.../etl/KnowledgeBaseVectorStoreWriterTest.java`
- Possibly modify: `.../platform-knowledge-base-worker/build.gradle.kts` (ensure `platform-configuration-api` dependency)

**Interfaces:**
- Consumes: `EnvironmentContext.set(int)`, `EnvironmentContext.clear()`. The existing `write(...)`/`writeChunk(...)` already receive `long environmentId`.

- [ ] **Step 1: Verify the worker can see `platform-configuration-api`**

Run: `grep -n "platform-configuration-api" server/libs/platform/platform-knowledge-base/platform-knowledge-base-worker/build.gradle.kts`
If absent, add to the `dependencies { }` block:
```kotlin
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
```

- [ ] **Step 2: Write the failing test**

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

package com.bytechef.platform.knowledgebase.worker.etl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

class KnowledgeBaseVectorStoreWriterTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final KnowledgeBaseVectorStoreWriter writer = new KnowledgeBaseVectorStoreWriter(vectorStore);

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testSetsEnvironmentDuringAddAndClearsAfter() {
        AtomicReference<Environment> observed = new AtomicReference<>();

        doAnswer(invocation -> {
            observed.set(EnvironmentContext.getCurrentEnvironment());

            return null;
        }).when(vectorStore)
            .add(org.mockito.ArgumentMatchers.anyList());

        writer.write(List.of(new Document("hello")), 1L, 2L, Environment.STAGING.ordinal(), List.of());

        assertThat(observed.get()).isEqualTo(Environment.STAGING);
        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-worker:test --tests "com.bytechef.platform.knowledgebase.worker.etl.KnowledgeBaseVectorStoreWriterTest"`
Expected: FAIL — environment is `PRODUCTION` (default) during `add`, not `STAGING`.

- [ ] **Step 4: Wrap the `add` calls in `write` and `writeChunk`**

In `KnowledgeBaseVectorStoreWriter.java`, add the import:
```java
import com.bytechef.platform.configuration.context.EnvironmentContext;
```
In `write(...)`, replace `vectorStore.add(sanitizedDocuments);` with:
```java
        EnvironmentContext.set((int) environmentId);

        try {
            vectorStore.add(sanitizedDocuments);
        } finally {
            EnvironmentContext.clear();
        }
```
In `writeChunk(...)`, replace `vectorStore.add(List.of(sanitizedDocument));` with:
```java
        EnvironmentContext.set((int) environmentId);

        try {
            vectorStore.add(List.of(sanitizedDocument));
        } finally {
            EnvironmentContext.clear();
        }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-worker:test --tests "com.bytechef.platform.knowledgebase.worker.etl.KnowledgeBaseVectorStoreWriterTest"`
Expected: PASS.

- [ ] **Step 6: Format and commit**

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-worker:spotlessApply
git add server/libs/platform/platform-knowledge-base/platform-knowledge-base-worker/src/main/java/com/bytechef/platform/knowledgebase/worker/etl/KnowledgeBaseVectorStoreWriter.java \
  server/libs/platform/platform-knowledge-base/platform-knowledge-base-worker/src/test/java/com/bytechef/platform/knowledgebase/worker/etl/KnowledgeBaseVectorStoreWriterTest.java \
  server/libs/platform/platform-knowledge-base/platform-knowledge-base-worker/build.gradle.kts
git commit -m "$(printf '%s\n\n%s' '- Bind environment context around Knowledge Base ingestion writes' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 6: Set `EnvironmentContext` around similarity search

**Files:**
- Modify: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/java/com/bytechef/platform/knowledgebase/facade/KnowledgeBaseFacadeImpl.java`
- Test: `.../facade/KnowledgeBaseFacadeImplTest.java` (create if absent)

**Interfaces:**
- Consumes: `KnowledgeBaseService.getKnowledgeBase(Long)` → `KnowledgeBase.getEnvironment()` (returns `Environment`), `EnvironmentContext.set(Environment)/clear()`.

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

package com.bytechef.platform.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.knowledgebase.storage.KnowledgeBaseFileStorage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import tools.jackson.databind.ObjectMapper;

class KnowledgeBaseFacadeImplTest {

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService =
        mock(KnowledgeBaseDocumentChunkService.class);
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage = mock(KnowledgeBaseFileStorage.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final VectorStore vectorStore = mock(VectorStore.class, RETURNS_DEEP_STUBS);

    private final KnowledgeBaseFacadeImpl facade = new KnowledgeBaseFacadeImpl(
        knowledgeBaseDocumentChunkService, knowledgeBaseFileStorage, knowledgeBaseService, objectMapper, vectorStore);

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testSetsEnvironmentDuringSearchAndClearsAfter() {
        AtomicReference<Environment> observed = new AtomicReference<>();
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setEnvironment(Environment.STAGING);

        when(knowledgeBaseService.getKnowledgeBase(1L)).thenReturn(knowledgeBase);
        doAnswer(invocation -> {
            observed.set(EnvironmentContext.getCurrentEnvironment());

            return List.of();
        }).when(vectorStore)
            .similaritySearch(org.mockito.ArgumentMatchers.any(org.springframework.ai.vectorstore.SearchRequest.class));

        facade.searchKnowledgeBase(1L, "query", null);

        assertThat(observed.get()).isEqualTo(Environment.STAGING);
        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
```

(If `KnowledgeBaseFacadeImpl`'s constructor is package-private, this test in the same package can call it. Verify the constructor parameter order matches Step 3.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test --tests "com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacadeImplTest"`
Expected: FAIL — constructor does not yet take `KnowledgeBaseService`, and environment is not set.

- [ ] **Step 3: Add `KnowledgeBaseService` and wrap the search**

In `KnowledgeBaseFacadeImpl.java`:
1. Add imports:
```java
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
```
2. Add field `private final KnowledgeBaseService knowledgeBaseService;` and insert it into the constructor parameter list (second-to-last, before `ObjectMapper objectMapper` to keep services grouped) and assign it.
3. In `searchKnowledgeBase(...)`, wrap the `vectorStore.similaritySearch(...)` call:
```java
        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);

        Environment environment = knowledgeBase.getEnvironment();

        EnvironmentContext.set(environment);

        List<Document> documents;

        try {
            documents = vectorStore.similaritySearch(searchRequest);
        } finally {
            EnvironmentContext.clear();
        }
```
(Keep the subsequent `documents.stream()...` mapping unchanged.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test --tests "com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacadeImplTest"`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:spotlessApply
git add server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/java/com/bytechef/platform/knowledgebase/facade/KnowledgeBaseFacadeImpl.java \
  server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/test/java/com/bytechef/platform/knowledgebase/facade/KnowledgeBaseFacadeImplTest.java
git commit -m "$(printf '%s\n\n%s' '- Bind environment context around Knowledge Base similarity search' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 7: Full-build verification and spec correction

**Files:**
- Modify: `docs/superpowers/specs/2026-06-27-catalog-embedding-model-design.md` (fix the default-environment note)

- [ ] **Step 1: Correct the spec's environment default**

In the spec, the "Environment propagation" decision and `EnvironmentContext` component description say the default is `PRODUCTION (ordinal 0)`. `PRODUCTION` is ordinal **2**. Change both mentions to read "defaults to `PRODUCTION`" (drop the incorrect "ordinal 0").

- [ ] **Step 2: Compile the whole server app**

Run: `./gradlew :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL (the new `@Primary` `CatalogEmbeddingModel` resolves; the four pgvector configs still compile unchanged).

- [ ] **Step 3: Run the affected module tests together**

Run:
```bash
./gradlew \
  :server:libs:platform:platform-configuration:platform-configuration-api:test \
  :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test \
  :server:libs:platform:platform-knowledge-base:platform-knowledge-base-worker:test \
  :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test \
  :server:ee:libs:config:tenant-multi-knowledge-base-config:test
```
Expected: BUILD SUCCESSFUL; the pre-existing `MultiTenantKnowledgeBasePgVectorConfigurationTest` and KB tests stay green.

- [ ] **Step 4: Commit the spec fix**

```bash
git add docs/superpowers/specs/2026-06-27-catalog-embedding-model-design.md
git commit -m "$(printf '%s\n\n%s' '- Correct environment default note in catalog-embedding-model spec' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Notes / deferred (documented, not silently dropped)

- **Copilot and ContextStore** vector stores also inject `EmbeddingModel`. With the `@Primary` `CatalogEmbeddingModel` present they now boot without a static key too, but they do **not** set `EnvironmentContext`, so they resolve the key at the default `PRODUCTION` environment. Wiring their per-entity environment (if they have one) is a follow-up; their previously-shipped fail-fast guards remain valid for CE.
- **CE single-tenant** keeps the static `openAiEmbeddingModel` bean + the fail-fast guard (no UI provider catalog in CE). `CatalogEmbeddingModel` lives in an EE module and is absent from CE builds.
- **Key rotation**: the `(environment, apiKey)` cache key means a rotated/disabled key produces a cache miss (and, if disabled, an actionable error). No explicit eviction hook is wired; add one mirroring `AiGatewayProviderServiceImpl.update()` only if stale-entry growth becomes a concern.
```
