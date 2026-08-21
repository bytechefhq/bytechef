# Embedding-Provider Status UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tell users proactively when the Knowledge Base can't embed — an "Embeddings" capability badge on the AI Providers settings page (OpenAI only), and an environment-aware banner on the Knowledge Bases list page (with a link to AI Providers) when no embedding provider is active.

**Architecture:** Backend adds (1) a `supportsEmbeddings` field to the AI Providers REST model (OpenAPI-generated) driving the badge, and (2) a `knowledgeBaseEmbeddingActive(environment)` GraphQL query driving the banner, backed by a CE SPI interface with an EE impl over `ProviderApiKeyResolver` (CE defaults to active). Frontend renders a `Badge` and an `Alert` banner.

**Tech Stack:** Java 25, Spring Boot 4, Spring GraphQL, OpenAPI generator (spring + typescript-fetch), React 19 + TS, graphql-codegen, vitest.

## Global Constraints

- EE source files (`server/ee/...`) use the **ByteChef Enterprise license header** + a `@version ee` Javadoc tag. CE files (`server/libs/...`) use the Apache 2.0 header.
- Server: `./gradlew spotlessApply` before each commit; pass `checkstyle/PMD/SpotBugs`. Unit test classes end in `Test`; test methods camelCase, no underscores; clear names; one blank line before control statements.
- Client: run `cd client && npm run check` (lint + typecheck + tests) before each client commit. Conventions: interface names end in `Props`/`I`; lucide icons imported with `Icon` suffix; use `twMerge` (not `cn`); object keys in ascending order (sort-keys); named imports sorted alphabetically.
- Embedding-capable = exactly what `CatalogEmbeddingModelFactory.resolveFactory` supports today: **`Provider.OPEN_AI` only**.
- Badge marks **capability** (`supportsEmbeddings`), NOT active state — show it regardless of `enabled`.
- Branch `0_732` receives parallel user commits — make FRESH commits, never amend; `git add` ONLY the files each task lists.
- `Provider` enum: `com.bytechef.component.ai.llm.Provider` (has `OPEN_AI`, `getKey()` → `"ai.provider.openAi"`, `getId()`, `getLabel()`).
- AI Providers settings route path (for the banner link): `/automation/settings/ai-providers`.

---

## File Structure

Backend:
- Modify `server/ee/libs/platform/platform-configuration/platform-configuration-rest/openapi.yaml` — add `supportsEmbeddings` to the `AiProvider` schema (regenerates `AiProviderModel.java` + TS client).
- Modify `.../platform-configuration-service/.../facade/AiProviderFacadeImpl.java` + its `AiProviderDTO` record — compute/carry `supportsEmbeddings`.
- Modify the `AiProviderDTO`→`AiProviderModel` converter (locate in Task 1).
- Create `server/libs/platform/platform-configuration/platform-configuration-api/.../ai/EmbeddingProviderStatusProvider.java` — CE SPI interface.
- Create `server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/.../catalog/CatalogEmbeddingProviderStatusProvider.java` — EE impl.
- Modify `server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/.../knowledge-base.graphqls` + `KnowledgeBaseGraphQlController.java` — add the query.

Frontend:
- Regenerated: `client/src/ee/shared/middleware/platform/configuration/models/AiProvider.ts` (via OpenAPI regen in Task 1).
- Modify `client/src/ee/pages/settings/platform/ai-providers/components/AiProviderList.tsx` — badge.
- Create `client/src/graphql/automation/knowledge-base/knowledgeBaseEmbeddingActive.graphql` (+ regen `graphql.ts`).
- Modify `client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx` — banner + link.

---

## Task 1: Backend — `supportsEmbeddings` on the AI Providers model

**Files:**
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-rest/openapi.yaml` (`AiProvider` schema)
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImpl.java` (+ its `AiProviderDTO`)
- Modify: the `AiProviderDTO`→`AiProviderModel` converter (locate)
- Test: `.../platform-configuration-service/src/test/java/.../facade/AiProviderFacadeImplTest.java` (create if absent; else add a test)

**Interfaces:**
- Produces: `AiProviderModel.supportsEmbeddings` (boolean, REST + TS client); `AiProviderDTO.supportsEmbeddings()`.

- [ ] **Step 1: Add the schema field**

In `openapi.yaml`, inside the `AiProvider` schema `properties:` block, add:
```yaml
    supportsEmbeddings:
      description: "Whether this AI provider can be used for embeddings."
      type: "boolean"
      readOnly: true
```

- [ ] **Step 2: Add the field to `AiProviderDTO` and compute it in the facade**

In `AiProviderFacadeImpl.java`, extend the `AiProviderDTO` record to include a trailing `boolean supportsEmbeddings`, then set it where the DTO is constructed in `getAiProviders(int environment)`:
```java
            return new AiProviderDTO(
                provider.getId(), provider.getLabel(), componentDefinition.getIcon(), apiKey, enabled,
                supportsEmbeddings(provider));
```
Add the helper (next to `hasConfigApiKey`):
```java
    /**
     * Mirrors {@code CatalogEmbeddingModelFactory.resolveFactory}: only OpenAI is wired for embeddings today.
     */
    private static boolean supportsEmbeddings(Provider provider) {
        return provider == Provider.OPEN_AI;
    }
```
Update the `AiProviderDTO` record declaration to:
```java
public record AiProviderDTO(int id, String name, String icon, String apiKey, boolean enabled,
    boolean supportsEmbeddings) {
}
```
(Find every other `new AiProviderDTO(...)` construction in the codebase and add the trailing arg; grep `new AiProviderDTO(` first.)

- [ ] **Step 3: Map the new field in the converter**

Find the converter that turns `AiProviderDTO` into `AiProviderModel`:
```bash
grep -rn "AiProviderModel" server/ee/libs/platform/platform-configuration --include=*.java | grep -iE "converter|mapper|convert\(" | grep -v test
```
If it is a MapStruct `@Mapper`, regeneration in Step 4 maps the same-named field automatically. If it is a manual `Converter<AiProviderDTO, AiProviderModel>`, add `aiProviderModel.setSupportsEmbeddings(aiProviderDTO.supportsEmbeddings());` to it.

- [ ] **Step 4: Regenerate the OpenAPI Java model + TS client**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-rest:generateOpenAPI`
Expected: `AiProviderModel.java` now has `supportsEmbeddings`; `client/src/ee/shared/middleware/platform/configuration/models/AiProvider.ts` now has `readonly supportsEmbeddings?: boolean;`.

- [ ] **Step 5: Write the failing test**

Create/extend `AiProviderFacadeImplTest` (mirror the module's existing facade-test style; if none exists, use a plain unit test mocking `componentDefinitionService` + `propertyService`). Minimum assertion:
```java
    @Test
    void testOnlyOpenAiSupportsEmbeddings() {
        List<AiProviderDTO> providers = aiProviderFacade.getAiProviders(2);

        AiProviderDTO openAi = providers.stream()
            .filter(provider -> provider.id() == Provider.OPEN_AI.getId())
            .findFirst()
            .orElseThrow();

        assertThat(openAi.supportsEmbeddings()).isTrue();
        assertThat(providers.stream()
            .filter(provider -> provider.id() != Provider.OPEN_AI.getId())
            .allMatch(provider -> !provider.supportsEmbeddings())).isTrue();
    }
```
(If the facade's collaborators are awkward to mock, instead unit-test a package-private extracted `supportsEmbeddings(Provider)` directly — assert true for OPEN_AI, false for ANTHROPIC.)

- [ ] **Step 6: Run test (RED → GREEN)**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test --tests "*AiProviderFacadeImplTest"`
Expected: GREEN after Step 2.

- [ ] **Step 7: Format, build, commit**

```bash
./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:spotlessApply
git add server/ee/libs/platform/platform-configuration/platform-configuration-rest/openapi.yaml \
  server/ee/libs/platform/platform-configuration/platform-configuration-rest/src/main/java/com/bytechef/ee/platform/configuration/web/rest/model/AiProviderModel.java \
  server/ee/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImpl.java \
  client/src/ee/shared/middleware/platform/configuration/models/AiProvider.ts \
  server/ee/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImplTest.java
# Also add the converter file if you modified one in Step 3.
git commit -m "$(printf '%s\n\n%s' '- Add supportsEmbeddings flag to AI Providers model' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 2: Backend — embedding-active SPI (CE interface + EE impl)

**Files:**
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/ai/EmbeddingProviderStatusProvider.java` (CE interface)
- Create: `server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogEmbeddingProviderStatusProvider.java` (EE impl)
- Test: `.../platform-ai-agent-service/src/test/java/.../catalog/CatalogEmbeddingProviderStatusProviderTest.java`

**Interfaces:**
- Consumes: `ProviderApiKeyResolver.resolve(Provider, int)` (EE, exists).
- Produces: `boolean EmbeddingProviderStatusProvider.isEmbeddingActive(int environment)`.

- [ ] **Step 1: Create the CE SPI interface**

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

package com.bytechef.platform.configuration.ai;

/**
 * Reports whether an embedding-capable AI provider is currently usable for an environment. Implemented in EE (where the
 * embedding API key is resolved at runtime from the activated provider); absent in CE, where callers treat embeddings
 * as always active because the server cannot boot with the Knowledge Base enabled and no embedding key configured.
 *
 * @author Ivica Cardic
 */
public interface EmbeddingProviderStatusProvider {

    boolean isEmbeddingActive(int environment);
}
```

- [ ] **Step 2: Write the failing EE-impl test**

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
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogEmbeddingProviderStatusProviderTest {

    private final ProviderApiKeyResolver providerApiKeyResolver = mock(ProviderApiKeyResolver.class);
    private final CatalogEmbeddingProviderStatusProvider statusProvider =
        new CatalogEmbeddingProviderStatusProvider(providerApiKeyResolver);

    @Test
    void testActiveWhenKeyResolves() {
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, 2)).thenReturn("sk-test");

        assertThat(statusProvider.isEmbeddingActive(2)).isTrue();
    }

    @Test
    void testInactiveWhenNoKey() {
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, 2)).thenReturn(null);

        assertThat(statusProvider.isEmbeddingActive(2)).isFalse();
    }

    @Test
    void testInactiveWhenBlankKey() {
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, 2)).thenReturn("   ");

        assertThat(statusProvider.isEmbeddingActive(2)).isFalse();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test --tests "*CatalogEmbeddingProviderStatusProviderTest"`
Expected: FAIL — class does not exist.

- [ ] **Step 4: Create the EE impl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * EE {@link EmbeddingProviderStatusProvider}: embeddings are active for an environment when the OpenAI API key resolves
 * (from the activated provider property, falling back to static config) — the same predicate the runtime embedding
 * model uses.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CatalogEmbeddingProviderStatusProvider implements EmbeddingProviderStatusProvider {

    private final ProviderApiKeyResolver providerApiKeyResolver;

    @SuppressFBWarnings("EI")
    public CatalogEmbeddingProviderStatusProvider(ProviderApiKeyResolver providerApiKeyResolver) {
        this.providerApiKeyResolver = providerApiKeyResolver;
    }

    @Override
    public boolean isEmbeddingActive(int environment) {
        String apiKey = providerApiKeyResolver.resolve(Provider.OPEN_AI, environment);

        return apiKey != null && !apiKey.isBlank();
    }
}
```
Verify `com.bytechef.platform.annotation.ConditionalOnEEVersion` is on this module's classpath; if not, drop the annotation (the module is EE-only, so the bean is already absent in CE) and rely on module presence.

- [ ] **Step 5: Run test (GREEN) + confirm `platform-configuration-api` is a dependency**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:test --tests "*CatalogEmbeddingProviderStatusProviderTest"`
Expected: PASS (3 tests). If compilation fails on the `EmbeddingProviderStatusProvider` import, confirm `platform-ai-agent-service/build.gradle.kts` depends on `platform-configuration-api` (it does per the existing `CatalogChatClientResolverImpl` imports).

- [ ] **Step 6: Format, commit**

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:spotlessApply :server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service:spotlessApply
git add server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/ai/EmbeddingProviderStatusProvider.java \
  server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/main/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogEmbeddingProviderStatusProvider.java \
  server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service/src/test/java/com/bytechef/ee/platform/ai/agent/catalog/CatalogEmbeddingProviderStatusProviderTest.java
git commit -m "$(printf '%s\n\n%s' '- Add EmbeddingProviderStatusProvider SPI with EE catalog impl' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: Backend — `knowledgeBaseEmbeddingActive` GraphQL query

**Files:**
- Modify: `server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/src/main/resources/graphql/automation/knowledge-base/knowledge-base.graphqls`
- Modify: `.../automation-knowledge-base-graphql/src/main/java/com/bytechef/automation/knowledgebase/web/graphql/KnowledgeBaseGraphQlController.java`
- Test: `.../KnowledgeBaseGraphQlControllerTest.java` (create if absent)

**Interfaces:**
- Consumes: `EmbeddingProviderStatusProvider.isEmbeddingActive(int)` (Task 2, optional bean — `ObjectProvider`, default true).
- Produces: GraphQL `knowledgeBaseEmbeddingActive(environment: Int!): Boolean!`.

- [ ] **Step 1: Add the schema query**

In `knowledge-base.graphqls`, inside `extend type Query { ... }`, add:
```graphql
    knowledgeBaseEmbeddingActive(environment: Int!): Boolean!
```

- [ ] **Step 2: Write the failing controller test**

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

package com.bytechef.automation.knowledgebase.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class KnowledgeBaseGraphQlControllerEmbeddingActiveTest {

    @Test
    @SuppressWarnings("unchecked")
    void testReturnsStatusFromProvider() {
        EmbeddingProviderStatusProvider statusProvider = mock(EmbeddingProviderStatusProvider.class);
        ObjectProvider<EmbeddingProviderStatusProvider> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(statusProvider);
        when(statusProvider.isEmbeddingActive(2)).thenReturn(false);

        assertThat(KnowledgeBaseGraphQlController.resolveEmbeddingActive(objectProvider, 2)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDefaultsToActiveWhenNoProvider() {
        ObjectProvider<EmbeddingProviderStatusProvider> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(null);

        assertThat(KnowledgeBaseGraphQlController.resolveEmbeddingActive(objectProvider, 2)).isTrue();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-graphql:test --tests "*KnowledgeBaseGraphQlControllerEmbeddingActiveTest"`
Expected: FAIL — method does not exist.

- [ ] **Step 4: Add the controller query + a testable static helper**

In `KnowledgeBaseGraphQlController.java`:
1. Add imports:
```java
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import org.springframework.beans.factory.ObjectProvider;
```
2. Add a field `private final ObjectProvider<EmbeddingProviderStatusProvider> embeddingProviderStatusProvider;`, add it to the constructor params + assignment (keep the existing `@SuppressFBWarnings("EI")` if present; add it if the class lacks it and SpotBugs complains).
3. Add the query method + a package-private static helper (so the default logic is unit-testable without Spring):
```java
    @QueryMapping
    public boolean knowledgeBaseEmbeddingActive(@Argument Integer environment) {
        return resolveEmbeddingActive(embeddingProviderStatusProvider, environment);
    }

    static boolean resolveEmbeddingActive(
        ObjectProvider<EmbeddingProviderStatusProvider> embeddingProviderStatusProvider, int environment) {

        EmbeddingProviderStatusProvider statusProvider = embeddingProviderStatusProvider.getIfAvailable();

        if (statusProvider == null) {
            return true;
        }

        return statusProvider.isEmbeddingActive(environment);
    }
```

- [ ] **Step 5: Run test (GREEN) + confirm dependency**

Run: `./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-graphql:test --tests "*KnowledgeBaseGraphQlControllerEmbeddingActiveTest"`
Expected: PASS (2 tests). The module already depends on `platform-configuration-api` (per the explore), so the SPI import resolves.

- [ ] **Step 6: Compile the server app (wires the EE bean into the CE query)**

Run: `./gradlew :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Format, commit**

```bash
./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-graphql:spotlessApply
git add server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/src/main/resources/graphql/automation/knowledge-base/knowledge-base.graphqls \
  server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/src/main/java/com/bytechef/automation/knowledgebase/web/graphql/KnowledgeBaseGraphQlController.java \
  server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/src/test/java/com/bytechef/automation/knowledgebase/web/graphql/KnowledgeBaseGraphQlControllerEmbeddingActiveTest.java
git commit -m "$(printf '%s\n\n%s' '- Add knowledgeBaseEmbeddingActive GraphQL query' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 4: Frontend — "Embeddings" badge on the AI Providers page

**Files:**
- Modify: `client/src/ee/pages/settings/platform/ai-providers/components/AiProviderList.tsx`
- Test: `client/src/ee/pages/settings/platform/ai-providers/components/AiProviderList.test.tsx` (create if absent)

**Interfaces:**
- Consumes: `AiProvider.supportsEmbeddings` (regenerated TS field from Task 1).

- [ ] **Step 1: Verify the regenerated model**

Run: `grep -n "supportsEmbeddings" client/src/ee/shared/middleware/platform/configuration/models/AiProvider.ts`
Expected: `readonly supportsEmbeddings?: boolean;` present (from Task 1). If missing, re-run Task 1 Step 4.

- [ ] **Step 2: Add the badge**

In `AiProviderList.tsx`, add `import Badge from '@/components/Badge/Badge';` (in correct alphabetical import order). Inside the provider name block (after the `<span className="text-sm font-semibold">{aiProvider.name}</span>` line), render the badge when the provider supports embeddings:
```tsx
<span className="flex items-center gap-2">
    <span className="text-sm font-semibold">{aiProvider.name}</span>

    {aiProvider.supportsEmbeddings && (
        <Badge label="Embeddings" styleType="secondary-outline" weight="semibold" />
    )}
</span>
```
(Adjust the wrapping to match the existing flex layout; keep the existing "Configure … credentials" subtitle untouched.)

- [ ] **Step 3: Write the test**

```tsx
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import AiProviderList from './AiProviderList';

// Mock the query hook + mutations the component uses; render two providers — one with
// supportsEmbeddings true (OpenAI), one false — and assert the badge appears once.
describe('AiProviderList', () => {
    it('renders the Embeddings badge only for embedding-capable providers', () => {
        // ...arrange providers [{name: 'Open AI', supportsEmbeddings: true, ...}, {name: 'Anthropic', supportsEmbeddings: false, ...}]
        render(<AiProviderList /* props/mocks */ />);

        expect(screen.getAllByText('Embeddings')).toHaveLength(1);
    });
});
```
Mirror an existing test in this folder for the exact mock setup (query hook, store mocks); if no sibling test exists, mock `useGetAiProvidersQuery` via `vi.mock` to return the two-provider array and assert badge count. Keep object keys ascending.

- [ ] **Step 4: Run client checks**

Run: `cd client && npx tsc --noEmit && npx eslint src/ee/pages/settings/platform/ai-providers/components/AiProviderList.tsx && npx vitest run src/ee/pages/settings/platform/ai-providers/components/AiProviderList.test.tsx`
Expected: typecheck clean, lint clean, test passes.

- [ ] **Step 5: Format, commit**

```bash
cd client && npm run format
git add client/src/ee/pages/settings/platform/ai-providers/components/AiProviderList.tsx \
  client/src/ee/pages/settings/platform/ai-providers/components/AiProviderList.test.tsx
git commit -m "$(printf '%s\n\n%s' '2026 client - Add Embeddings capability badge to AI Providers list' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 5: Frontend — Knowledge Bases list banner + link

**Files:**
- Create: `client/src/graphql/automation/knowledge-base/knowledgeBaseEmbeddingActive.graphql`
- Regenerated: `client/src/shared/middleware/graphql.ts` (via codegen)
- Modify: `client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx`
- Test: `client/src/pages/automation/knowledge-bases/KnowledgeBases.test.tsx` (create if absent)

**Interfaces:**
- Consumes: GraphQL `knowledgeBaseEmbeddingActive(environment)` (Task 3); `useEnvironmentStore` `currentEnvironmentId`; `Alert` (`@/components/ui/alert`).

- [ ] **Step 1: Add the GraphQL operation + regenerate**

Create `client/src/graphql/automation/knowledge-base/knowledgeBaseEmbeddingActive.graphql`:
```graphql
query knowledgeBaseEmbeddingActive($environment: Int!) {
    knowledgeBaseEmbeddingActive(environment: $environment)
}
```
Run: `cd client && npx graphql-codegen --config codegen.ts`
Expected: a `useKnowledgeBaseEmbeddingActiveQuery` hook appears in `client/src/shared/middleware/graphql.ts`.

- [ ] **Step 2: Add the banner to `KnowledgeBases.tsx`**

Add imports (alphabetical): `Alert, AlertDescription, AlertTitle` from `@/components/ui/alert`; `useEnvironmentStore` from its store path; `useKnowledgeBaseEmbeddingActiveQuery` from `@/shared/middleware/graphql`; `Link` from the router if used for SPA navigation (else an anchor to `/automation/settings/ai-providers`).

Read the environment and query:
```tsx
const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

const {data: embeddingActiveData} = useKnowledgeBaseEmbeddingActiveQuery({
    environment: currentEnvironmentId,
});

const embeddingActive = embeddingActiveData?.knowledgeBaseEmbeddingActive ?? true;
```
(`?? true` = fail-open so a query error never shows a false banner, per spec.)

Render the banner above the list/`EmptyList` (after the header, before the body content) when not active:
```tsx
{!embeddingActive && (
    <Alert variant="destructive">
        <AlertTitle>No embedding model is active</AlertTitle>

        <AlertDescription>
            Knowledge Base documents can&apos;t be processed until an embedding-capable AI provider is activated for
            this environment.{' '}

            <a className="font-medium underline" href="/automation/settings/ai-providers">
                Go to AI Providers
            </a>
        </AlertDescription>
    </Alert>
)}
```
Place it so it shows whether the list is empty or not (i.e., not inside the empty-state-only branch).

- [ ] **Step 3: Write the test**

```tsx
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

// Mock useKnowledgeBaseEmbeddingActiveQuery to return false → banner shows; true → hidden.
describe('KnowledgeBases embedding banner', () => {
    it('shows the banner when embedding is not active', () => {
        // vi.mock the query hook to return {data: {knowledgeBaseEmbeddingActive: false}}
        // render KnowledgeBases with required providers/mocks
        expect(screen.getByText('No embedding model is active')).toBeInTheDocument();
    });

    it('hides the banner when embedding is active', () => {
        // query hook returns {data: {knowledgeBaseEmbeddingActive: true}}
        expect(screen.queryByText('No embedding model is active')).not.toBeInTheDocument();
    });
});
```
Use `vi.hoisted` for any module-scope mock refs (per CLAUDE.md Vitest note). Mock the other hooks/stores the page needs (`useWorkspaceStore`, the KB list query) minimally.

- [ ] **Step 4: Run client checks**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass. (If `npm run check` is red on PRE-EXISTING unrelated files, fall back to per-file: `npx tsc --noEmit`, `npx eslint <changed files>`, `npx vitest run <changed test files>` — and note it.)

- [ ] **Step 5: Format, commit**

```bash
cd client && npm run format
git add client/src/graphql/automation/knowledge-base/knowledgeBaseEmbeddingActive.graphql \
  client/src/shared/middleware/graphql.ts \
  client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx \
  client/src/pages/automation/knowledge-bases/KnowledgeBases.test.tsx
git commit -m "$(printf '%s\n\n%s' '2026 client - Add embedding-inactive banner to Knowledge Bases list' 'Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Notes / decisions baked in

- **CE behavior**: the banner never shows in CE — a CE server with the Knowledge Base enabled cannot boot without a static embedding key (the fail-fast guard from the prior feature), and `knowledgeBaseEmbeddingActive` defaults to `true` when no EE `EmbeddingProviderStatusProvider` bean is present. This resolves the spec's "CE link behavior" open question: no special-casing needed.
- **Badge = capability**, shown whenever `supportsEmbeddings` (OpenAI), independent of `enabled`.
- **Banner = active-state**, driven by the authoritative `knowledgeBaseEmbeddingActive` query (not derived client-side from the AI Providers list), so CE works and the predicate isn't duplicated in the client.
- **Drift guard**: `supportsEmbeddings(Provider)` in the facade carries a comment pointing at `CatalogEmbeddingModelFactory.resolveFactory`; a future second embedding provider must update both (and the dimension/table constraints from the prior spec).
