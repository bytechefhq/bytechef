# models.dev Model Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Populate `ai_gateway_model` rows automatically from the models.dev open model database, so pricing, context windows, and capabilities stop being hand-typed and stop drifting.

**Architecture:** A standalone CE module (`platform-ai-model-catalog`) parses a bundled copy of `https://models.dev/api.json` into typed records and refreshes it on a schedule. A separate EE reconciler in `platform-ai-gateway-service` maps `AiGatewayProviderType` onto models.dev provider ids and writes rows. The gateway's cost and routing code is untouched — it keeps reading the same columns, which now contain better data.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Data JDBC, Jackson 3 (`tools.jackson`), Gradle Kotlin DSL, JUnit 5 + AssertJ + Mockito, Liquibase, GraphQL (Spring for GraphQL), React 19 + TypeScript.

**Spec:** `docs/superpowers/specs/2026-08-10-models-dev-model-catalog-design.md`

## Global Constraints

- **Licensing:** Files under `server/libs/` use the Apache 2.0 header. Files under `server/ee/` use the ByteChef Enterprise license header **and** a `@version ee` Javadoc tag. Copy the header verbatim from a neighbouring file in the same tree.
- **Jackson is version 3**: import from `tools.jackson.databind`, never `com.fasterxml.jackson`. `JsonNode.properties()` returns `Set<Map.Entry<String, JsonNode>>`; `JsonNode.propertyNames()` returns `Collection<String>`.
- **`@Nullable` is `org.jspecify.annotations.Nullable`.** JSpecify is on every module's classpath via the root `build.gradle.kts` — do not add it to any `build.gradle.kts`.
- **Money is `BigDecimal`.** Never parse a cost rate through `double` or `float`.
- **Java style (from CLAUDE.md):** one blank line before `if`/`for`/`while`/`switch`/`try` (except immediately after an opening `{`); one blank line between a variable modification and the next statement that uses it; no blank line before a class's closing `}`; no `_` prefix on private methods; descriptive variable names, never single letters.
- **Test naming:** unit tests end in `Test`, integration tests end in `IntTest`. Test method names are camelCase with no underscores (`testParseModelWithoutCost`, never `testParse_NoCost`).
- **Before every commit:** `./gradlew spotlessApply`. Never judge a Gradle run piped through `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep the file.
- **Commit messages:** no ticket prefix for this plan's commits — use `<description>` for server and `client - <description>` for client, matching the spec and plan commits already on this branch. (The repo convention is normally `<ticket> <description>`; this work has no ticket.) Every commit command shown in the tasks below reflects this.

---

### Task 1: Catalog API module — records and interface

Creates the CE `-api` module: the record shapes and the `ModelCatalog` interface. The two `fromWireValue` helpers carry the spec's leniency rule (unknown upstream vocabulary must never fail a parse) and are the only logic here worth testing.

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-api/build.gradle.kts`
- Create: `server/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-api/src/main/java/com/bytechef/platform/ai/model/catalog/ModelCatalog.java`
- Create: `.../catalog/CatalogProvider.java`, `CatalogModel.java`, `Modalities.java`, `Limit.java`, `Cost.java`, `CostTier.java`
- Modify: `settings.gradle.kts` (add two `include(...)` lines near line 194, beside `server:libs:platform:platform-ai:platform-ai-api`)
- Test: `.../platform-ai-model-catalog-api/src/test/java/com/bytechef/platform/ai/model/catalog/CatalogModelTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `ModelCatalog` (`fetchModel(String, String) -> Optional<CatalogModel>`, `getModels(String) -> List<CatalogModel>`, `getProviders() -> List<CatalogProvider>`, `getLoadedAt() -> Instant`); records `CatalogProvider(String id, String name, String doc, Map<String, CatalogModel> models)`, `CatalogModel(...)`, `Modalities(List<Modality> input, List<Modality> output)`, `Limit(Integer context, Integer input, Integer output)`, `Cost(BigDecimal input, BigDecimal output, BigDecimal cacheRead, BigDecimal cacheWrite, BigDecimal reasoning, BigDecimal inputAudio, BigDecimal outputAudio, List<CostTier> tiers)`, `CostTier(Integer contextSize, BigDecimal input, BigDecimal output, BigDecimal cacheRead, BigDecimal cacheWrite)`; enums `CatalogModel.Status{ACTIVE,BETA,DEPRECATED}` with `Status.fromWireValue(String)`, `Modalities.Modality{TEXT,IMAGE,AUDIO,VIDEO,PDF}` with `Modality.fromWireValue(String)`.

- [ ] **Step 1: Register the `-api` module in `settings.gradle.kts`**

Add beside the existing `platform-ai` entries (around line 194):

```kotlin
include("server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api")
```

Register **only** `-api` here. Gradle validates every included project's directory on every invocation and fails configuration for the whole repository if one is missing — and git does not track empty directories, so an `include` whose module has no committed files breaks a fresh checkout even when the local working tree looks fine. Task 2 adds the `-service` include in the same commit that creates its `build.gradle.kts`.

- [ ] **Step 2: Create the `-api` build file**

`server/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-api/build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```

The api module is pure records — no Spring, no Jackson. That is deliberate: it keeps the catalog vocabulary loadable by anything.

- [ ] **Step 3: Write the failing test**

`CatalogModelTest.java` (Apache 2.0 header):

```java
package com.bytechef.platform.ai.model.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import org.junit.jupiter.api.Test;

class CatalogModelTest {

    @Test
    void testStatusFromWireValueMapsKnownValues() {
        assertThat(Status.fromWireValue("deprecated")).isEqualTo(Status.DEPRECATED);
        assertThat(Status.fromWireValue("beta")).isEqualTo(Status.BETA);
    }

    @Test
    void testStatusFromWireValueDefaultsToActive() {
        assertThat(Status.fromWireValue(null)).isEqualTo(Status.ACTIVE);
        assertThat(Status.fromWireValue("")).isEqualTo(Status.ACTIVE);
        assertThat(Status.fromWireValue("retired-next-tuesday")).isEqualTo(Status.ACTIVE);
    }

    @Test
    void testModalityFromWireValueMapsKnownValues() {
        assertThat(Modality.fromWireValue("text")).isEqualTo(Modality.TEXT);
        assertThat(Modality.fromWireValue("PDF")).isEqualTo(Modality.PDF);
    }

    @Test
    void testModalityFromWireValueReturnsNullForUnknownValue() {
        assertThat(Modality.fromWireValue("hologram")).isNull();
        assertThat(Modality.fromWireValue(null)).isNull();
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api:test > /tmp/t1.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/t1.log | head
```

Expected: FAIL — `CatalogModel` and `Modalities` do not exist.

- [ ] **Step 5: Write the records**

`Modalities.java`:

```java
package com.bytechef.platform.ai.model.catalog;

import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Input and output modalities a model accepts and produces.
 *
 * @author Ivica Cardic
 */
public record Modalities(List<Modality> input, List<Modality> output) {

    public enum Modality {

        TEXT, IMAGE, AUDIO, VIDEO, PDF;

        /**
         * Maps an upstream modality string onto an enum constant, returning {@code null} for anything unrecognized.
         * models.dev is community-maintained and adds vocabulary without warning; callers drop nulls rather than
         * failing, so one new modality upstream cannot blank the catalog on the next refresh.
         */
        public static @Nullable Modality fromWireValue(@Nullable String wireValue) {
            if (wireValue == null) {
                return null;
            }

            for (Modality modality : values()) {
                if (modality.name()
                    .equals(wireValue.toUpperCase(Locale.ROOT))) {

                    return modality;
                }
            }

            return null;
        }
    }
}
```

`CatalogModel.java`:

```java
package com.bytechef.platform.ai.model.catalog;

import java.time.LocalDate;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * A single model as published by models.dev.
 *
 * <p>
 * {@code cost} is nullable rather than a non-null record with null fields: 21 of the 249 models across the providers
 * the AI gateway maps publish no pricing block at all, and "priced at zero" must stay distinguishable from "no pricing
 * published" — the OTLP cost resolver already treats that distinction as load-bearing.
 *
 * @author Ivica Cardic
 */
public record CatalogModel(
    String id, String name, @Nullable String description, @Nullable String family, boolean attachment,
    boolean reasoning, boolean toolCall, boolean structuredOutput, boolean temperature, boolean openWeights,
    @Nullable String knowledge, @Nullable LocalDate releaseDate, @Nullable LocalDate lastUpdated, Status status,
    Modalities modalities, Limit limit, @Nullable Cost cost) {

    public enum Status {

        ACTIVE, BETA, DEPRECATED;

        /**
         * Maps an upstream status string onto an enum constant. Absent, blank, and unrecognized values all map to
         * {@link #ACTIVE} — upstream omits the field for 197 of the 249 mapped models, and an unknown value must not
         * fail the parse.
         */
        public static Status fromWireValue(@Nullable String wireValue) {
            if (wireValue == null || wireValue.isBlank()) {
                return ACTIVE;
            }

            for (Status status : values()) {
                if (status.name()
                    .equals(wireValue.toUpperCase(Locale.ROOT))) {

                    return status;
                }
            }

            return ACTIVE;
        }
    }
}
```

`Limit.java`:

```java
package com.bytechef.platform.ai.model.catalog;

import org.jspecify.annotations.Nullable;

/**
 * Token limits. All three are nullable — upstream publishes {@code input} for only a subset of models.
 *
 * @author Ivica Cardic
 */
public record Limit(@Nullable Integer context, @Nullable Integer input, @Nullable Integer output) {
}
```

`CostTier.java`:

```java
package com.bytechef.platform.ai.model.catalog;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * A context-size pricing tier. {@code contextSize} is the threshold above which these rates apply.
 *
 * @author Ivica Cardic
 */
public record CostTier(
    @Nullable Integer contextSize, @Nullable BigDecimal input, @Nullable BigDecimal output,
    @Nullable BigDecimal cacheRead, @Nullable BigDecimal cacheWrite) {
}
```

`Cost.java`:

```java
package com.bytechef.platform.ai.model.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Per-million-token rates in USD.
 *
 * <p>
 * {@code tiers} carries context-size-dependent pricing (23 of the 249 mapped models charge more above a context
 * threshold). The AI gateway's two flat cost columns cannot express this and use the base {@code input} / {@code
 * output} rates; the tiers are modeled so a future gateway change is a consumer change rather than a re-parse.
 *
 * @author Ivica Cardic
 */
public record Cost(
    @Nullable BigDecimal input, @Nullable BigDecimal output, @Nullable BigDecimal cacheRead,
    @Nullable BigDecimal cacheWrite, @Nullable BigDecimal reasoning, @Nullable BigDecimal inputAudio,
    @Nullable BigDecimal outputAudio, List<CostTier> tiers) {
}
```

`CatalogProvider.java`:

```java
package com.bytechef.platform.ai.model.catalog;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A models.dev provider and its models, keyed by model id.
 *
 * @author Ivica Cardic
 */
public record CatalogProvider(String id, String name, @Nullable String doc, Map<String, CatalogModel> models) {
}
```

`ModelCatalog.java`:

```java
package com.bytechef.platform.ai.model.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Read access to the models.dev catalog: provider and model metadata, pricing, limits, and capabilities.
 *
 * <p>
 * Provider ids are models.dev's own string ids ({@code "anthropic"}, {@code "azure"}), never a ByteChef enum. Keeping
 * the module ignorant of any particular consumer's provider vocabulary is what lets the AI gateway, and later the LLM
 * component model dropdowns, share one catalog.
 *
 * @author Ivica Cardic
 */
public interface ModelCatalog {

    Optional<CatalogModel> fetchModel(String providerId, String modelId);

    /**
     * Returns when the in-memory catalog was populated — the bundled snapshot's load time, or the time of the last
     * successful refresh. Operators use this to tell a live catalog from one pinned to the shipped snapshot.
     */
    Instant getLoadedAt();

    List<CatalogModel> getModels(String providerId);

    List<CatalogProvider> getProviders();
}
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api:test > /tmp/t1.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t1.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add settings.gradle.kts server/libs/platform/platform-ai/platform-ai-model-catalog
git commit -m "Add model catalog API records and ModelCatalog interface"
```

---

### Task 2: models.dev JSON parser

Parses the published `api.json` into the Task 1 records. This is the module's only real logic, so it gets the heaviest test coverage — every leniency rule in the spec is pinned here.

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-service/build.gradle.kts`
- Create: `.../platform-ai-model-catalog-service/src/main/java/com/bytechef/platform/ai/model/catalog/modelsdev/ModelsDevParser.java`
- Test: `.../platform-ai-model-catalog-service/src/test/java/com/bytechef/platform/ai/model/catalog/modelsdev/ModelsDevParserTest.java`
- Test fixture: `.../platform-ai-model-catalog-service/src/test/resources/model-catalog/test-api.json`

**Interfaces:**
- Consumes: every record from Task 1.
- Produces: `ModelsDevParser.parse(InputStream) -> Map<String, CatalogProvider>` (static, keyed by provider id).

- [ ] **Step 1: Register the `-service` module and create its build file**

Add to `settings.gradle.kts`, immediately after the `-api` include Task 1 added:

```kotlin
include("server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service")
```

The include and the `build.gradle.kts` below must land in the **same commit** — an include pointing at a directory with no committed files fails Gradle configuration repo-wide on a fresh checkout.

`server/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-service/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")

    api(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
```

- [ ] **Step 2: Write the test fixture**

`src/test/resources/model-catalog/test-api.json` — deliberately exercises every edge the spec names:

```json
{
  "acme": {
    "id": "acme",
    "name": "Acme",
    "doc": "https://acme.example/docs",
    "models": {
      "acme-large": {
        "id": "acme-large",
        "name": "Acme Large",
        "description": "Flagship",
        "family": "acme",
        "attachment": true,
        "reasoning": true,
        "tool_call": true,
        "structured_output": true,
        "temperature": true,
        "open_weights": false,
        "knowledge": "2025-01",
        "release_date": "2025-06-01",
        "last_updated": "2025-09-15",
        "modalities": {"input": ["text", "image", "hologram"], "output": ["text"]},
        "limit": {"context": 200000, "output": 64000},
        "cost": {
          "input": 3,
          "output": 15,
          "cache_read": 0.003625,
          "tiers": [
            {"input": 6, "output": 22.5, "tier": {"type": "context", "size": 200000}}
          ],
          "context_over_200k": {"input": 999, "output": 999}
        }
      },
      "acme-free": {
        "id": "acme-free",
        "name": "Acme Free",
        "attachment": false,
        "reasoning": false,
        "tool_call": false,
        "open_weights": true,
        "modalities": {"input": ["text"], "output": ["text"]},
        "limit": {"context": 8192}
      },
      "acme-old": {
        "id": "acme-old",
        "name": "Acme Old",
        "status": "deprecated",
        "attachment": false,
        "reasoning": false,
        "tool_call": false,
        "modalities": {"input": ["text"], "output": ["text"]},
        "limit": {"context": 4096},
        "cost": {"input": 1, "output": 2}
      },
      "acme-vision": {
        "id": "acme-vision",
        "name": "Acme Vision",
        "status": "invented-status",
        "attachment": true,
        "reasoning": false,
        "tool_call": false,
        "modalities": {"input": ["text"], "output": ["image"]},
        "limit": {"context": 4096},
        "cost": {"input": 1, "output": 2}
      }
    }
  }
}
```

- [ ] **Step 3: Write the failing test**

`ModelsDevParserTest.java` (Apache 2.0 header):

```java
package com.bytechef.platform.ai.model.catalog.modelsdev;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelsDevParserTest {

    private static Map<String, CatalogProvider> parseFixture() {
        try (InputStream inputStream = ModelsDevParserTest.class.getResourceAsStream(
            "/model-catalog/test-api.json")) {

            return ModelsDevParser.parse(inputStream);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void testParseReadsProviderMetadata() {
        CatalogProvider provider = parseFixture().get("acme");

        assertThat(provider.id()).isEqualTo("acme");
        assertThat(provider.name()).isEqualTo("Acme");
        assertThat(provider.doc()).isEqualTo("https://acme.example/docs");
        assertThat(provider.models()).hasSize(4);
    }

    @Test
    void testParseReadsCostAsBigDecimal() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-large");

        assertThat(model.cost()).isNotNull();
        assertThat(model.cost()
            .input()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(model.cost()
            .output()).isEqualByComparingTo(new BigDecimal("15"));
        assertThat(model.cost()
            .cacheRead()).isEqualByComparingTo(new BigDecimal("0.003625"));
    }

    @Test
    void testParseReadsTiersAndIgnoresContextOver200k() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-large");

        assertThat(model.cost()
            .tiers()).hasSize(1);
        assertThat(model.cost()
            .tiers()
            .getFirst()
            .contextSize()).isEqualTo(200000);
        assertThat(model.cost()
            .tiers()
            .getFirst()
            .input()).isEqualByComparingTo(new BigDecimal("6"));
    }

    @Test
    void testParseReadsDatesAndLimits() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-large");

        assertThat(model.releaseDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(model.lastUpdated()).isEqualTo(LocalDate.of(2025, 9, 15));
        assertThat(model.limit()
            .context()).isEqualTo(200000);
        assertThat(model.limit()
            .output()).isEqualTo(64000);
        assertThat(model.limit()
            .input()).isNull();
    }

    @Test
    void testParseDropsUnknownModality() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-large");

        assertThat(model.modalities()
            .input()).containsExactly(Modality.TEXT, Modality.IMAGE);
    }

    @Test
    void testParseModelWithoutCostYieldsNullCost() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-free");

        assertThat(model.cost()).isNull();
        assertThat(model.openWeights()).isTrue();
        assertThat(model.structuredOutput()).isFalse();
    }

    @Test
    void testParseReadsDeprecatedStatus() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-old");

        assertThat(model.status()).isEqualTo(Status.DEPRECATED);
    }

    @Test
    void testParseMapsUnknownStatusToActive() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-vision");

        assertThat(model.status()).isEqualTo(Status.ACTIVE);
        assertThat(model.modalities()
            .output()).containsExactly(Modality.IMAGE);
    }

    /**
     * Documents how complete the leniency is: a provider whose value is a bare string, not an object, still yields a
     * usable (empty) entry rather than aborting the parse or dropping its healthy siblings. That is the correct
     * outcome on a refresh path, where one malformed upstream contribution must never blank the catalog.
     */
    @Test
    void testParseToleratesGarbageProviderAndKeepsSiblings() {
        String json = """
            {"good": {"id": "good", "name": "Good", "models": {}},
             "bad": "this-should-be-an-object"}
            """;

        Map<String, CatalogProvider> providers = ModelsDevParser.parse(
            new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThat(providers).containsOnlyKeys("good", "bad");
        assertThat(providers.get("bad")
            .models()).isEmpty();
    }

    @Test
    void testParseSkipsModelWhoseFieldThrows() {
        String json = """
            {"acme": {"id": "acme", "name": "Acme", "models": {
                "good": {"id": "good", "name": "Good", "modalities": {"input": ["text"], "output": ["text"]},
                         "limit": {"context": 100}},
                "bad": {"id": "bad", "name": "Bad", "cost": {"input": 1e999999999}}
            }}}
            """;

        Map<String, CatalogProvider> providers = ModelsDevParser.parse(
            new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThat(providers.get("acme")
            .models()).containsKey("good");
    }
}
```

`1e999999999` overflows `BigDecimal` construction, which is the realistic shape of a per-model parse failure: a field that is syntactically valid JSON but semantically unrepresentable. The assertion checks only that the healthy sibling survives — whether `bad` is dropped or degrades depends on where the throw lands, and pinning that would over-specify the parser.

- [ ] **Step 4: Run the test to verify it fails**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service:test > /tmp/t2.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/t2.log | head
```

Expected: FAIL — `ModelsDevParser` does not exist.

- [ ] **Step 5: Write the parser**

`ModelsDevParser.java` (Apache 2.0 header):

```java
package com.bytechef.platform.ai.model.catalog.modelsdev;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.Cost;
import com.bytechef.platform.ai.model.catalog.CostTier;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Parses the models.dev {@code api.json} document into {@link CatalogProvider} records.
 *
 * <p>
 * Parsing is deliberately lenient. models.dev is community-maintained: new capability flags, modalities, and status
 * values appear without notice, and a single malformed contribution reaches the published document before anyone
 * notices. A strict parse would let any of those blank the entire catalog on the next scheduled refresh, so unknown
 * vocabulary is dropped and an unparseable model is skipped while its siblings survive.
 *
 * @author Ivica Cardic
 */
public final class ModelsDevParser {

    private static final Logger log = LoggerFactory.getLogger(ModelsDevParser.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ModelsDevParser() {
    }

    public static Map<String, CatalogProvider> parse(InputStream inputStream) {
        JsonNode rootJsonNode = OBJECT_MAPPER.readTree(inputStream);

        Map<String, CatalogProvider> providers = new LinkedHashMap<>();

        for (Map.Entry<String, JsonNode> providerEntry : rootJsonNode.properties()) {
            String providerId = providerEntry.getKey();

            try {
                providers.put(providerId, parseProvider(providerId, providerEntry.getValue()));
            } catch (RuntimeException exception) {
                log.debug("Skipping unparseable models.dev provider '{}'", providerId, exception);
            }
        }

        return Map.copyOf(providers);
    }

    private static CatalogProvider parseProvider(String providerId, JsonNode providerJsonNode) {
        Map<String, CatalogModel> models = new LinkedHashMap<>();

        JsonNode modelsJsonNode = providerJsonNode.path("models");

        for (Map.Entry<String, JsonNode> modelEntry : modelsJsonNode.properties()) {
            String modelId = modelEntry.getKey();

            try {
                models.put(modelId, parseModel(modelId, modelEntry.getValue()));
            } catch (RuntimeException exception) {
                log.debug("Skipping unparseable models.dev model '{}/{}'", providerId, modelId, exception);
            }
        }

        return new CatalogProvider(
            text(providerJsonNode, "id", providerId), text(providerJsonNode, "name", providerId),
            nullableText(providerJsonNode, "doc"), Map.copyOf(models));
    }

    private static CatalogModel parseModel(String modelId, JsonNode modelJsonNode) {
        return new CatalogModel(
            text(modelJsonNode, "id", modelId), text(modelJsonNode, "name", modelId),
            nullableText(modelJsonNode, "description"), nullableText(modelJsonNode, "family"),
            bool(modelJsonNode, "attachment"), bool(modelJsonNode, "reasoning"), bool(modelJsonNode, "tool_call"),
            bool(modelJsonNode, "structured_output"), bool(modelJsonNode, "temperature"),
            bool(modelJsonNode, "open_weights"), nullableText(modelJsonNode, "knowledge"),
            date(modelJsonNode, "release_date"), date(modelJsonNode, "last_updated"),
            Status.fromWireValue(nullableText(modelJsonNode, "status")),
            parseModalities(modelJsonNode.path("modalities")), parseLimit(modelJsonNode.path("limit")),
            parseCost(modelJsonNode.path("cost")));
    }

    private static Modalities parseModalities(JsonNode modalitiesJsonNode) {
        return new Modalities(
            parseModalityList(modalitiesJsonNode.path("input")), parseModalityList(modalitiesJsonNode.path("output")));
    }

    private static List<Modality> parseModalityList(JsonNode modalityListJsonNode) {
        List<Modality> modalities = new ArrayList<>();

        for (JsonNode modalityJsonNode : modalityListJsonNode) {
            Modality modality = Modality.fromWireValue(modalityJsonNode.asString(null));

            if (modality != null) {
                modalities.add(modality);
            }
        }

        return List.copyOf(modalities);
    }

    private static Limit parseLimit(JsonNode limitJsonNode) {
        return new Limit(
            integer(limitJsonNode, "context"), integer(limitJsonNode, "input"), integer(limitJsonNode, "output"));
    }

    private static @Nullable Cost parseCost(JsonNode costJsonNode) {
        if (costJsonNode.isMissingNode() || costJsonNode.isNull()) {
            return null;
        }

        return new Cost(
            decimal(costJsonNode, "input"), decimal(costJsonNode, "output"), decimal(costJsonNode, "cache_read"),
            decimal(costJsonNode, "cache_write"), decimal(costJsonNode, "reasoning"),
            decimal(costJsonNode, "input_audio"), decimal(costJsonNode, "output_audio"),
            parseTiers(costJsonNode.path("tiers")));
    }

    /**
     * Reads the structured {@code cost.tiers} array and deliberately ignores the flat {@code cost.context_over_200k}
     * key that upstream publishes alongside it. Both describe the same thing, but the flat key hardcodes a threshold
     * into its own name and cannot describe a model tiered at 272k tokens — which several GPT-5.x entries are.
     */
    private static List<CostTier> parseTiers(JsonNode tiersJsonNode) {
        List<CostTier> tiers = new ArrayList<>();

        for (JsonNode tierJsonNode : tiersJsonNode) {
            tiers.add(
                new CostTier(
                    integer(tierJsonNode.path("tier"), "size"), decimal(tierJsonNode, "input"),
                    decimal(tierJsonNode, "output"), decimal(tierJsonNode, "cache_read"),
                    decimal(tierJsonNode, "cache_write")));
        }

        return List.copyOf(tiers);
    }

    private static boolean bool(JsonNode jsonNode, String fieldName) {
        return jsonNode.path(fieldName)
            .asBoolean(false);
    }

    /**
     * Reads a numeric rate through its text form so the {@link BigDecimal} carries the published digits exactly. Going
     * via {@code double} would round rates such as {@code 0.003625} before they ever reach a cost calculation.
     */
    private static @Nullable BigDecimal decimal(JsonNode jsonNode, String fieldName) {
        JsonNode valueJsonNode = jsonNode.path(fieldName);

        if (!valueJsonNode.isNumber()) {
            return null;
        }

        return new BigDecimal(valueJsonNode.asString());
    }

    private static @Nullable LocalDate date(JsonNode jsonNode, String fieldName) {
        String value = nullableText(jsonNode, fieldName);

        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static @Nullable Integer integer(JsonNode jsonNode, String fieldName) {
        JsonNode valueJsonNode = jsonNode.path(fieldName);

        if (!valueJsonNode.isNumber()) {
            return null;
        }

        return valueJsonNode.asInt();
    }

    private static @Nullable String nullableText(JsonNode jsonNode, String fieldName) {
        JsonNode valueJsonNode = jsonNode.path(fieldName);

        if (!valueJsonNode.isString()) {
            return null;
        }

        return valueJsonNode.asString();
    }

    private static String text(JsonNode jsonNode, String fieldName, String fallback) {
        String value = nullableText(jsonNode, fieldName);

        return value == null ? fallback : value;
    }
}
```

**Note on the skip-unparseable test:** `limit.context` of `"not-a-number"` is a string, so `integer()` returns null rather than throwing — that alone does not skip the model. Make the `bad` model actually unparseable by giving it a `modalities` value that is a string rather than an object (`"modalities": "text"`), which makes `parseModalityList` iterate a non-array and `parseModalities` still succeed... Instead, verify the skip path by making `parseModel` throw: use `"release_date": {"nested": true}` — `nullableText` returns null and `date` returns null, still no throw. **Use this fixture instead for that one test:** replace the `bad` model with `"bad": []` (a JSON array where an object is required). `properties()` on an array node yields nothing and `text(...)` falls back, so still no throw. **Simplest reliable trigger:** assert on a provider whose `models` node is a string. Adjust the test to:

```java
    @Test
    void testParseSkipsUnparseableProviderAndKeepsSiblings() {
        String json = """
            {"good": {"id": "good", "name": "Good", "models": {}},
             "bad": "this-should-be-an-object"}
            """;

        Map<String, CatalogProvider> providers = ModelsDevParser.parse(
            new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThat(providers).containsOnlyKeys("good", "bad");
        assertThat(providers.get("bad")
            .models()).isEmpty();
    }
```

This documents the actual behavior: leniency is so complete that a garbage provider degrades to an empty model map rather than being dropped. That is the correct outcome for a refresh path — record it as the test's intent, and replace `testParseSkipsUnparseableModelAndKeepsSiblings` with this test.

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service:test > /tmp/t2.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t2.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/platform/platform-ai/platform-ai-model-catalog
git commit -m "Add lenient models.dev api.json parser"
```

---

### Task 3: Bundled snapshot and lazy in-memory catalog

Adds the shipped `api.json`, the loader that reads it from the classpath, and the `ModelCatalog` implementation that serves reads from a swappable in-memory map. Parsing is lazy so a 3.6 MB parse never lands on the startup path.

**Files:**
- Create: `.../platform-ai-model-catalog-service/src/main/resources/config/model-catalog/models-dev-api.json` (downloaded, ~3.6 MB)
- Create: `.../modelsdev/ModelsDevSnapshotLoader.java`
- Create: `.../catalog/service/ModelCatalogImpl.java`
- Modify: `.../platform-ai-model-catalog-service/build.gradle.kts` (add the `refreshModelsDevSnapshot` task)
- Test: `.../src/test/java/com/bytechef/platform/ai/model/catalog/service/ModelCatalogImplTest.java`
- Test: `.../src/test/java/com/bytechef/platform/ai/model/catalog/modelsdev/ModelsDevSnapshotLoaderTest.java`

**Interfaces:**
- Consumes: `ModelsDevParser.parse(InputStream)`, all Task 1 records.
- Produces: `ModelsDevSnapshotLoader.load() -> Map<String, CatalogProvider>` and its constant `SNAPSHOT_RESOURCE`; `ModelCatalogImpl implements ModelCatalog` with constructor `ModelCatalogImpl(ModelsDevSnapshotLoader)` and `replaceCatalog(Map<String, CatalogProvider>, Instant)`.

- [ ] **Step 1: Download the snapshot**

```bash
mkdir -p server/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-service/src/main/resources/config/model-catalog
curl -sS -o server/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-service/src/main/resources/config/model-catalog/models-dev-api.json https://models.dev/api.json
ls -la server/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-service/src/main/resources/config/model-catalog/models-dev-api.json
```

Expected: a file of roughly 3.6 MB. Store it **verbatim** — do not reformat or prune. A verbatim copy is what makes a future refresh commit diffable against upstream.

- [ ] **Step 2: Write the failing tests**

`ModelsDevSnapshotLoaderTest.java`:

```java
package com.bytechef.platform.ai.model.catalog.modelsdev;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards the shipped snapshot itself. A bad {@code refreshModelsDevSnapshot} commit — a truncated download, an error
 * page saved as JSON, an upstream rename of a provider id — fails here rather than reaching a deployment.
 */
class ModelsDevSnapshotLoaderTest {

    @Test
    void testBundledSnapshotParses() {
        Map<String, CatalogProvider> providers = new ModelsDevSnapshotLoader().load();

        assertThat(providers).hasSizeGreaterThan(100);
    }

    @Test
    void testBundledSnapshotContainsEveryGatewayMappedProvider() {
        Map<String, CatalogProvider> providers = new ModelsDevSnapshotLoader().load();

        assertThat(providers).containsKeys(
            "anthropic", "azure", "cohere", "deepseek", "google", "groq", "mistral", "openai");
    }

    @Test
    void testBundledSnapshotHasPricedModels() {
        Map<String, CatalogProvider> providers = new ModelsDevSnapshotLoader().load();

        assertThat(providers.get("anthropic")
            .models()).isNotEmpty();
        assertThat(
            providers.get("anthropic")
                .models()
                .values()
                .stream()
                .anyMatch(model -> model.cost() != null)).isTrue();
    }
}
```

`ModelCatalogImplTest.java`:

```java
package com.bytechef.platform.ai.model.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevSnapshotLoader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelCatalogImplTest {

    @Mock
    private ModelsDevSnapshotLoader modelsDevSnapshotLoader;

    private static CatalogModel model(String id) {
        return new CatalogModel(
            id, id, null, null, false, false, false, false, false, false, null, null, null, Status.ACTIVE,
            new Modalities(List.of(), List.of()), new Limit(null, null, null), null);
    }

    private static Map<String, CatalogProvider> providers(String providerId, String modelId) {
        return Map.of(providerId, new CatalogProvider(providerId, providerId, null, Map.of(modelId, model(modelId))));
    }

    @Test
    void testFetchModelReturnsModel() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        assertThat(modelCatalog.fetchModel("acme", "acme-large")).isPresent();
    }

    @Test
    void testFetchModelReturnsEmptyForUnknownProviderOrModel() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        assertThat(modelCatalog.fetchModel("nope", "acme-large")).isEmpty();
        assertThat(modelCatalog.fetchModel("acme", "nope")).isEmpty();
    }

    @Test
    void testGetModelsReturnsEmptyListForUnknownProvider() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        assertThat(modelCatalog.getModels("nope")).isEmpty();
        assertThat(modelCatalog.getModels("acme")).hasSize(1);
    }

    @Test
    void testSnapshotIsLoadedLazilyAndOnlyOnce() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        verify(modelsDevSnapshotLoader, times(0)).load();

        modelCatalog.getProviders();
        modelCatalog.getProviders();

        verify(modelsDevSnapshotLoader, times(1)).load();
    }

    @Test
    void testReplaceCatalogSwapsContentAndAdvancesLoadedAt() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        Instant before = modelCatalog.getLoadedAt();
        Instant refreshedAt = before.plusSeconds(60);

        modelCatalog.replaceCatalog(providers("beta", "beta-small"), refreshedAt);

        assertThat(modelCatalog.fetchModel("acme", "acme-large")).isEmpty();
        assertThat(modelCatalog.fetchModel("beta", "beta-small")).isPresent();
        assertThat(modelCatalog.getLoadedAt()).isEqualTo(refreshedAt);
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service:test > /tmp/t3.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/t3.log | head
```

Expected: FAIL — `ModelsDevSnapshotLoader` and `ModelCatalogImpl` do not exist.

- [ ] **Step 4: Write the loader**

`ModelsDevSnapshotLoader.java` (Apache 2.0 header):

```java
package com.bytechef.platform.ai.model.catalog.modelsdev;

import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Loads the bundled models.dev snapshot from the classpath.
 *
 * <p>
 * A missing or unreadable snapshot throws rather than degrading to an empty catalog: it is a packaging error, not a
 * runtime condition, and silently serving nothing would make every model look uncatalogued.
 *
 * @author Ivica Cardic
 */
public class ModelsDevSnapshotLoader {

    static final String SNAPSHOT_RESOURCE = "/config/model-catalog/models-dev-api.json";

    public Map<String, CatalogProvider> load() {
        try (InputStream inputStream = ModelsDevSnapshotLoader.class.getResourceAsStream(SNAPSHOT_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Bundled models.dev snapshot not found at " + SNAPSHOT_RESOURCE);
            }

            return ModelsDevParser.parse(inputStream);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to read bundled models.dev snapshot", ioException);
        }
    }
}
```

- [ ] **Step 5: Write the catalog implementation**

`ModelCatalogImpl.java` (Apache 2.0 header):

```java
package com.bytechef.platform.ai.model.catalog.service;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevSnapshotLoader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Serves catalog reads from an in-memory map that {@link com.bytechef.platform.ai.model.catalog.modelsdev
 * .ModelsDevRefresher} may replace wholesale.
 *
 * <p>
 * The bundled snapshot is parsed <em>lazily</em>, on first read rather than in the constructor. The document is 3.6 MB
 * and most deployments will not touch the catalog on their first request; server startup is already sensitive to
 * classpath-wide work, and paying a multi-megabyte parse on every boot for a feature that may go unused is the wrong
 * trade.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ModelCatalogImpl implements ModelCatalog {

    private final ModelsDevSnapshotLoader modelsDevSnapshotLoader;

    private volatile @Nullable Catalog catalog;

    public ModelCatalogImpl(ModelsDevSnapshotLoader modelsDevSnapshotLoader) {
        this.modelsDevSnapshotLoader = modelsDevSnapshotLoader;
    }

    @Override
    public Optional<CatalogModel> fetchModel(String providerId, String modelId) {
        CatalogProvider provider = getCatalog().providers()
            .get(providerId);

        if (provider == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            provider.models()
                .get(modelId));
    }

    @Override
    public Instant getLoadedAt() {
        return getCatalog().loadedAt();
    }

    @Override
    public List<CatalogModel> getModels(String providerId) {
        CatalogProvider provider = getCatalog().providers()
            .get(providerId);

        if (provider == null) {
            return List.of();
        }

        return List.copyOf(
            provider.models()
                .values());
    }

    @Override
    public List<CatalogProvider> getProviders() {
        return List.copyOf(
            getCatalog().providers()
                .values());
    }

    /**
     * Replaces the whole catalog atomically. Callers must never install a partial or empty map — a read served from
     * half a catalog is indistinguishable from a model genuinely not existing.
     */
    public void replaceCatalog(Map<String, CatalogProvider> providers, Instant loadedAt) {
        catalog = new Catalog(Map.copyOf(providers), loadedAt);
    }

    private Catalog getCatalog() {
        Catalog currentCatalog = catalog;

        if (currentCatalog == null) {
            synchronized (this) {
                currentCatalog = catalog;

                if (currentCatalog == null) {
                    currentCatalog = new Catalog(modelsDevSnapshotLoader.load(), Instant.now());

                    catalog = currentCatalog;
                }
            }
        }

        return currentCatalog;
    }

    private record Catalog(Map<String, CatalogProvider> providers, Instant loadedAt) {
    }
}
```

- [ ] **Step 6: Add the snapshot refresh Gradle task**

Append to `.../platform-ai-model-catalog-service/build.gradle.kts`:

```kotlin
tasks.register("refreshModelsDevSnapshot") {
    description = "Re-downloads the bundled models.dev api.json snapshot. Run manually, then commit the result."
    group = "build"
    notCompatibleWithConfigurationCache("Performs a network fetch on demand")

    doLast {
        val target = file("src/main/resources/config/model-catalog/models-dev-api.json")

        target.parentFile.mkdirs()

        java.net.URI("https://models.dev/api.json").toURL().openStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }

        logger.lifecycle("Wrote ${target.length()} bytes to $target")
    }
}
```

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service:test > /tmp/t3.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t3.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/platform/platform-ai/platform-ai-model-catalog
git commit -m "Add bundled models.dev snapshot and lazy in-memory catalog"
```

---

### Task 4: Scheduled refresh and Spring wiring

Adds the refresher, the configuration properties, and the autoconfiguration that publishes `ModelCatalog` as a bean. The refresher's contract is one-directional fail-soft: it either installs a complete catalog or leaves the existing one alone.

**Files:**
- Create: `.../catalog/config/ModelCatalogProperties.java`
- Create: `.../catalog/config/ModelCatalogConfiguration.java`
- Create: `.../modelsdev/ModelsDevRefresher.java`
- Create: `.../platform-ai-model-catalog-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `.../src/test/java/com/bytechef/platform/ai/model/catalog/modelsdev/ModelsDevRefresherTest.java`

**Interfaces:**
- Consumes: `ModelCatalogImpl.replaceCatalog(Map, Instant)`, `ModelsDevParser.parse(InputStream)`, `ModelsDevSnapshotLoader.load()`.
- Produces: Spring bean `ModelCatalog` (backed by `ModelCatalogImpl`); `ModelsDevRefresher.refresh()`; properties under `bytechef.ai.model-catalog.refresh`.

- [ ] **Step 1: Write the failing test**

`ModelsDevRefresherTest.java`:

```java
package com.bytechef.platform.ai.model.catalog.modelsdev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.service.ModelCatalogImpl;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelsDevRefresherTest {

    private static final String VALID_BODY = """
        {"acme": {"id": "acme", "name": "Acme", "models": {
            "acme-large": {"id": "acme-large", "name": "Acme Large",
                           "modalities": {"input": ["text"], "output": ["text"]},
                           "limit": {"context": 100}, "cost": {"input": 1, "output": 2}}}}}
        """;

    @Mock
    private ModelCatalogImpl modelCatalog;

    @Test
    void testRefreshInstallsFetchedCatalog() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> VALID_BODY.getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        verify(modelCatalog).replaceCatalog(any(), any(Instant.class));
    }

    @Test
    void testRefreshKeepsCurrentCatalogWhenFetchThrows() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(modelCatalog, () -> {
            throw new IllegalStateException("connection reset");
        });

        refresher.refresh();

        verify(modelCatalog, never()).replaceCatalog(any(), any(Instant.class));
    }

    @Test
    void testRefreshKeepsCurrentCatalogWhenBodyIsMalformed() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> "<html>502 Bad Gateway</html>".getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        verify(modelCatalog, never()).replaceCatalog(any(), any(Instant.class));
    }

    @Test
    void testRefreshKeepsCurrentCatalogWhenBodyParsesToZeroProviders() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> "{}".getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        verify(modelCatalog, never()).replaceCatalog(any(), any(Instant.class));
    }

    @Test
    void testFetchedCatalogRetainsParsedContent() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> VALID_BODY.getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        org.mockito.ArgumentCaptor<Map<String, CatalogProvider>> captor =
            org.mockito.ArgumentCaptor.forClass(Map.class);

        verify(modelCatalog).replaceCatalog(captor.capture(), any(Instant.class));

        assertThat(captor.getValue()).containsKey("acme");
    }
}
```

Note the seam: the refresher takes a `BodySupplier` rather than a `RestClient`, so the test drives every failure mode without HTTP.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service:test > /tmp/t4.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/t4.log | head
```

Expected: FAIL — `ModelsDevRefresher` does not exist.

- [ ] **Step 3: Write the properties class**

`ModelCatalogProperties.java` (Apache 2.0 header):

```java
package com.bytechef.platform.ai.model.catalog.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.ai.model-catalog")
public class ModelCatalogProperties {

    private Refresh refresh = new Refresh();

    public Refresh getRefresh() {
        return refresh;
    }

    public void setRefresh(Refresh refresh) {
        this.refresh = refresh;
    }

    public static class Refresh {

        private boolean enabled = true;
        private Duration interval = Duration.ofDays(1);
        private String url = "https://models.dev/api.json";

        public Duration getInterval() {
            return interval;
        }

        public String getUrl() {
            return url;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
```

- [ ] **Step 4: Write the refresher**

`ModelsDevRefresher.java` (Apache 2.0 header):

```java
package com.bytechef.platform.ai.model.catalog.modelsdev;

import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.service.ModelCatalogImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically replaces the in-memory catalog with a freshly fetched copy of the models.dev document.
 *
 * <p>
 * Fail-soft in one direction only: every failure path — connection error, non-2xx, malformed body, a body that parses
 * to zero providers — logs and leaves the existing catalog untouched. The refresher never installs an empty or partial
 * catalog, so a deployment with blocked egress serves the bundled snapshot indefinitely and emits one warn line per
 * interval, rather than degrading into a catalog that silently answers nothing.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public class ModelsDevRefresher {

    private static final Logger log = LoggerFactory.getLogger(ModelsDevRefresher.class);

    private final BodySupplier bodySupplier;
    private final ModelCatalogImpl modelCatalog;

    public ModelsDevRefresher(ModelCatalogImpl modelCatalog, BodySupplier bodySupplier) {
        this.bodySupplier = bodySupplier;
        this.modelCatalog = modelCatalog;
    }

    @Scheduled(
        initialDelayString = "${bytechef.ai.model-catalog.refresh.interval:P1D}",
        fixedDelayString = "${bytechef.ai.model-catalog.refresh.interval:P1D}")
    public void refresh() {
        Map<String, CatalogProvider> providers;

        try {
            byte[] body = bodySupplier.get();

            providers = ModelsDevParser.parse(new ByteArrayInputStream(body));
        } catch (RuntimeException exception) {
            log.warn("models.dev catalog refresh failed; keeping the current catalog", exception);

            return;
        }

        if (providers.isEmpty()) {
            log.warn("models.dev catalog refresh returned zero providers; keeping the current catalog");

            return;
        }

        modelCatalog.replaceCatalog(providers, Instant.now());

        log.info("models.dev catalog refreshed with {} providers", providers.size());
    }

    /**
     * The fetch seam. Isolating the network call behind a supplier keeps every failure mode unit-testable without an
     * HTTP stack.
     */
    @FunctionalInterface
    public interface BodySupplier {

        byte[] get();
    }
}
```

- [ ] **Step 5: Write the autoconfiguration**

`ModelCatalogConfiguration.java` (Apache 2.0 header):

```java
package com.bytechef.platform.ai.model.catalog.config;

import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevRefresher;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevSnapshotLoader;
import com.bytechef.platform.ai.model.catalog.service.ModelCatalogImpl;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration
@EnableConfigurationProperties(ModelCatalogProperties.class)
public class ModelCatalogConfiguration {

    @Bean
    ModelCatalogImpl modelCatalog(ModelsDevSnapshotLoader modelsDevSnapshotLoader) {
        return new ModelCatalogImpl(modelsDevSnapshotLoader);
    }

    @Bean
    ModelsDevSnapshotLoader modelsDevSnapshotLoader() {
        return new ModelsDevSnapshotLoader();
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "bytechef.ai.model-catalog.refresh", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    ModelsDevRefresher modelsDevRefresher(ModelCatalogImpl modelCatalog, ModelCatalogProperties properties) {
        String url = properties.getRefresh()
            .getUrl();

        RestClient restClient = RestClient.builder()
            .build();

        return new ModelsDevRefresher(
            modelCatalog,
            () -> restClient.get()
                .uri(url)
                .retrieve()
                .body(byte[].class));
    }
}
```

`ModelCatalogImpl` is exposed as the concrete type because `ModelsDevRefresher` needs `replaceCatalog`; it also satisfies every `ModelCatalog` injection point.

- [ ] **Step 6: Register the autoconfiguration**

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.bytechef.platform.ai.model.catalog.config.ModelCatalogConfiguration
```

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service:test > /tmp/t4.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t4.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/platform/platform-ai/platform-ai-model-catalog
git commit -m "Add scheduled models.dev catalog refresh with fail-soft semantics"
```

---

### Task 5: Pinning on `ai_gateway_model`

Adds the `catalog_pinned` column and the rule that an admin edit to a catalog-owned field pins the row. The comparison lives in `AiGatewayModelServiceImpl.update` because that is the single choke point both the platform GraphQL controller and the workspace facade pass through.

**Files:**
- Modify: `server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/resources/config/liquibase/changelog/platform/ai/gateway/00000000000001_ai_gateway_init.xml` (changeSet `20260507000001`, inside `createTable tableName="ai_gateway_model"`, after the `capabilities` column at line 74)
- Modify: `.../platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway/domain/AiGatewayModel.java`
- Modify: `.../platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway/service/AiGatewayModelService.java`
- Modify: `.../platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/service/AiGatewayModelServiceImpl.java`
- Test: `.../platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/service/AiGatewayModelPinningTest.java`

**Interfaces:**
- Consumes: existing `AiGatewayModel`, `AiGatewayModelRepository`.
- Produces: `AiGatewayModel.isCatalogPinned()` / `setCatalogPinned(boolean)`; `AiGatewayModelService.updateFromCatalog(AiGatewayModel) -> AiGatewayModel` (writes catalog-owned fields **without** pinning); `AiGatewayModelService.unpin(long)`.

- [ ] **Step 1: Add the Liquibase column**

The module is unreleased — `git ls-tree -r --name-only v0.31.2 | grep platform-ai-gateway` returns nothing — so the column goes directly into the init changeset rather than a new one. Insert after the `capabilities` column:

```xml
            <column name="catalog_pinned" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
```

Verify the edit did not desync build output:

```bash
rm -rf server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/build/resources
grep -n "catalog_pinned" server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/resources/config/liquibase/changelog/platform/ai/gateway/00000000000001_ai_gateway_init.xml
```

- [ ] **Step 2: Write the failing test**

`AiGatewayModelPinningTest.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.platform.ai.gateway.repository.AiGatewayModelRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayModelPinningTest {

    @Mock
    private AiGatewayModelDeploymentService aiGatewayModelDeploymentService;

    @Mock
    private AiGatewayModelRepository aiGatewayModelRepository;

    private AiGatewayModelServiceImpl aiGatewayModelService;

    private static AiGatewayModel existingModel() {
        AiGatewayModel model = new AiGatewayModel(1L, "gpt-5.1");

        model.setAlias("fast");
        model.setCapabilities("reasoning,tool_call");
        model.setContextWindow(400000);
        model.setInputCostPerMTokens(new BigDecimal("1.25"));
        model.setOutputCostPerMTokens(new BigDecimal("10.00"));

        return model;
    }

    private static AiGatewayModel incomingCopyOf(AiGatewayModel source) {
        AiGatewayModel model = new AiGatewayModel(source.getProviderId(), source.getName());

        model.setAlias(source.getAlias());
        model.setCapabilities(source.getCapabilities());
        model.setContextWindow(source.getContextWindow());
        model.setEnabled(source.isEnabled());
        model.setInputCostPerMTokens(source.getInputCostPerMTokens());
        model.setOutputCostPerMTokens(source.getOutputCostPerMTokens());

        return model;
    }

    @BeforeEach
    void setUp() {
        aiGatewayModelService = new AiGatewayModelServiceImpl(
            aiGatewayModelDeploymentService, aiGatewayModelRepository);
    }

    @Test
    void testUpdatePinsRowWhenCostChanges() {
        AiGatewayModel existing = existingModel();

        when(aiGatewayModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiGatewayModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiGatewayModel incoming = incomingCopyOf(existing);

        incoming.setInputCostPerMTokens(new BigDecimal("0.99"));

        AiGatewayModel updated = aiGatewayModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isTrue();
    }

    @Test
    void testUpdateDoesNotPinWhenOnlyAliasChanges() {
        AiGatewayModel existing = existingModel();

        when(aiGatewayModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiGatewayModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiGatewayModel incoming = incomingCopyOf(existing);

        incoming.setAlias("renamed");

        AiGatewayModel updated = aiGatewayModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isFalse();
    }

    @Test
    void testUpdateDoesNotPinWhenClientRoundTripsIdenticalValues() {
        AiGatewayModel existing = existingModel();

        when(aiGatewayModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiGatewayModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiGatewayModel updated = aiGatewayModelService.update(incomingCopyOf(existing));

        assertThat(updated.isCatalogPinned()).isFalse();
    }

    @Test
    void testUpdateFromCatalogNeverPins() {
        AiGatewayModel existing = existingModel();

        when(aiGatewayModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiGatewayModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiGatewayModel incoming = incomingCopyOf(existing);

        incoming.setInputCostPerMTokens(new BigDecimal("2.50"));

        AiGatewayModel updated = aiGatewayModelService.updateFromCatalog(incoming);

        assertThat(updated.isCatalogPinned()).isFalse();
        assertThat(updated.getInputCostPerMTokens()).isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    void testUnpinClearsTheFlag() {
        AiGatewayModel existing = existingModel();

        existing.setCatalogPinned(true);

        when(aiGatewayModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiGatewayModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        aiGatewayModelService.unpin(1L);

        assertThat(existing.isCatalogPinned()).isFalse();
    }
}
```

Add the import `com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelDeploymentService`. The two-argument constructor and the `findById(...).orElseThrow(...)` lookup inside `getModel` match the existing implementation exactly; the deployment service mock is unused by these tests but required by the constructor.

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*AiGatewayModelPinningTest*' > /tmp/t5.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/t5.log | head
```

Expected: FAIL — `isCatalogPinned`, `updateFromCatalog`, and `unpin` do not exist.

- [ ] **Step 4: Add the domain field**

In `AiGatewayModel.java`, add the column field alphabetically (after `capabilities`):

```java
    @Column("catalog_pinned")
    private boolean catalogPinned;
```

Add the accessors beside the other `is`/`set` pairs:

```java
    /**
     * Whether an administrator has explicitly overridden a catalog-owned field on this row. The catalog reconciler
     * skips pinned rows, so a negotiated rate survives indefinitely. Cleared by
     * {@link com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelService#unpin(long)}.
     */
    public boolean isCatalogPinned() {
        return catalogPinned;
    }

    public void setCatalogPinned(boolean catalogPinned) {
        this.catalogPinned = catalogPinned;
    }
```

Also add `catalogPinned` to `toString()`.

- [ ] **Step 5: Extend the service interface**

In `AiGatewayModelService.java`:

```java
    /**
     * Applies catalog-sourced values without pinning the row. The reconciler must not route through
     * {@link #update(AiGatewayModel)} — that method treats any change to a catalog-owned field as an administrator
     * override, so a reconcile would pin every row it touched and immediately stop managing it.
     */
    AiGatewayModel updateFromCatalog(AiGatewayModel model);

    /**
     * Clears the catalog-override flag, handing the row back to the reconciler. The next reconcile overwrites its
     * catalog-owned fields.
     */
    void unpin(long id);
```

- [ ] **Step 6: Implement pinning in the service**

In `AiGatewayModelServiceImpl.java`, replace `update` and add the two new methods:

```java
    @Override
    public AiGatewayModel update(AiGatewayModel model) {
        Validate.notNull(model, "'model' must not be null");

        AiGatewayModel existingModel = getModel(model.getId());

        if (catalogOwnedFieldChanged(existingModel, model)) {
            existingModel.setCatalogPinned(true);
        }

        return applyAndSave(existingModel, model);
    }

    @Override
    public AiGatewayModel updateFromCatalog(AiGatewayModel model) {
        Validate.notNull(model, "'model' must not be null");

        return applyAndSave(getModel(model.getId()), model);
    }

    @Override
    public void unpin(long id) {
        AiGatewayModel model = getModel(id);

        model.setCatalogPinned(false);

        aiGatewayModelRepository.save(model);
    }

    private AiGatewayModel applyAndSave(AiGatewayModel existingModel, AiGatewayModel model) {
        existingModel.setAlias(model.getAlias());
        existingModel.setCapabilities(model.getCapabilities());
        existingModel.setContextWindow(model.getContextWindow());
        existingModel.setEnabled(model.isEnabled());
        existingModel.setInputCostPerMTokens(model.getInputCostPerMTokens());
        existingModel.setName(model.getName());
        existingModel.setOutputCostPerMTokens(model.getOutputCostPerMTokens());

        return aiGatewayModelRepository.save(existingModel);
    }

    /**
     * Compares rather than merely detecting presence. GraphQL clients round-trip the whole object, so a save that
     * touched only the alias resends all four catalog-owned fields unchanged — treating that as an override would pin
     * essentially every row the first time anyone renamed one.
     */
    private static boolean catalogOwnedFieldChanged(AiGatewayModel existingModel, AiGatewayModel model) {
        return valueChanged(existingModel.getContextWindow(), model.getContextWindow())
            || valueChanged(existingModel.getCapabilities(), model.getCapabilities())
            || decimalChanged(existingModel.getInputCostPerMTokens(), model.getInputCostPerMTokens())
            || decimalChanged(existingModel.getOutputCostPerMTokens(), model.getOutputCostPerMTokens());
    }

    /**
     * {@link BigDecimal#equals} distinguishes {@code 1.5} from {@code 1.50}; a rate re-serialized by a client with a
     * different scale is the same money and must not pin the row.
     */
    private static boolean decimalChanged(@Nullable BigDecimal existingValue, @Nullable BigDecimal value) {
        if (existingValue == null || value == null) {
            return existingValue != value;
        }

        return existingValue.compareTo(value) != 0;
    }

    private static boolean valueChanged(@Nullable Object existingValue, @Nullable Object value) {
        return !Objects.equals(existingValue, value);
    }
```

Add imports for `java.math.BigDecimal`, `java.util.Objects`, and `org.jspecify.annotations.Nullable` if not already present.

- [ ] **Step 7: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*AiGatewayModelPinningTest*' > /tmp/t5.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t5.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 8: Compile the whole server to catch interface implementors**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/compile.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/compile.log | head
```

Adding methods to `AiGatewayModelService` breaks any other implementor (for example a remote-client stub). Fix by adding stubs that throw `UnsupportedOperationException`, per the EE remote-client convention in CLAUDE.md.

- [ ] **Step 9: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/platform/platform-ai/platform-ai-gateway
git commit -m "Pin ai_gateway_model rows on administrator override of catalog-owned fields"
```

---

### Task 6: Provider mapping and capabilities encoding

Two small pure helpers the reconciler translates through. Both are fully unit-testable and define contracts the next task depends on.

**Files:**
- Create: `.../platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/catalog/AiGatewayModelsDevProviderIds.java`
- Create: `.../catalog/CapabilitiesEncoder.java`
- Test: `.../src/test/java/com/bytechef/ee/platform/ai/gateway/catalog/AiGatewayModelsDevProviderIdsTest.java`
- Test: `.../src/test/java/com/bytechef/ee/platform/ai/gateway/catalog/CapabilitiesEncoderTest.java`

**Interfaces:**
- Consumes: `AiGatewayProviderType`, `CatalogModel`, `Modalities.Modality`.
- Produces: `AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType) -> String`; `CapabilitiesEncoder.encode(CatalogModel) -> String`.

- [ ] **Step 1: Add the module dependency**

In `.../platform-ai-gateway-service/build.gradle.kts`, add to the `implementation` block:

```kotlin
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))
```

- [ ] **Step 2: Write the failing tests**

`AiGatewayModelsDevProviderIdsTest.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiGatewayModelsDevProviderIdsTest {

    @Test
    void testResolveProviderIdMapsEveryKnownType() {
        assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.ANTHROPIC))
            .isEqualTo("anthropic");
        assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.AZURE_OPENAI))
            .isEqualTo("azure");
        assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.COHERE))
            .isEqualTo("cohere");
        assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.DEEPSEEK))
            .isEqualTo("deepseek");
        assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.GOOGLE_GEMINI))
            .isEqualTo("google");
        assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.GROQ))
            .isEqualTo("groq");
        assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.MISTRAL))
            .isEqualTo("mistral");
        assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.OPENAI))
            .isEqualTo("openai");
    }

    /**
     * Fails the build when someone appends a provider type without mapping it, rather than letting that provider
     * silently reconcile nothing at runtime.
     */
    @Test
    void testEveryProviderTypeHasAMapping() {
        for (AiGatewayProviderType type : AiGatewayProviderType.values()) {
            assertThat(AiGatewayModelsDevProviderIds.resolveProviderId(type))
                .as("no models.dev provider id mapped for %s", type)
                .isNotBlank();
        }
    }
}
```

`CapabilitiesEncoderTest.java`:

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class CapabilitiesEncoderTest {

    private static CatalogModel model(
        boolean attachment, boolean reasoning, boolean toolCall, boolean structuredOutput, boolean temperature,
        List<Modality> inputModalities) {

        return new CatalogModel(
            "m", "M", null, null, attachment, reasoning, toolCall, structuredOutput, temperature, false, null, null,
            null, Status.ACTIVE, new Modalities(inputModalities, List.of(Modality.TEXT)),
            new Limit(null, null, null), null);
    }

    @Test
    void testEncodeSortsTokensAlphabetically() {
        String capabilities = CapabilitiesEncoder.encode(
            model(true, true, true, true, true, List.of(Modality.TEXT, Modality.IMAGE)));

        assertThat(capabilities).isEqualTo(
            "attachment,reasoning,structured_output,temperature,tool_call,vision");
    }

    @Test
    void testEncodeEmitsVisionOnlyForImageInput() {
        assertThat(CapabilitiesEncoder.encode(model(false, false, false, false, false, List.of(Modality.TEXT))))
            .isEmpty();
        assertThat(CapabilitiesEncoder.encode(model(false, false, false, false, false, List.of(Modality.IMAGE))))
            .isEqualTo("vision");
    }

    @Test
    void testEncodeFitsTheColumn() {
        String capabilities = CapabilitiesEncoder.encode(
            model(true, true, true, true, true, List.of(Modality.IMAGE)));

        assertThat(capabilities.length()).isLessThanOrEqualTo(256);
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*catalog*' > /tmp/t6.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/t6.log | head
```

Expected: FAIL — neither class exists.

- [ ] **Step 4: Write the mapping**

`AiGatewayModelsDevProviderIds.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;

/**
 * Maps the gateway's provider types onto models.dev provider ids.
 *
 * <p>
 * The switch is deliberately exhaustive with no {@code default} arm: appending a value to {@link
 * AiGatewayProviderType} then breaks compilation here, which is the point. A {@code default} returning null would let
 * a new provider ship silently reconciling nothing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AiGatewayModelsDevProviderIds {

    private AiGatewayModelsDevProviderIds() {
    }

    static String resolveProviderId(AiGatewayProviderType type) {
        return switch (type) {
            case ANTHROPIC -> "anthropic";
            case AZURE_OPENAI -> "azure";
            case COHERE -> "cohere";
            case DEEPSEEK -> "deepseek";
            case GOOGLE_GEMINI -> "google";
            case GROQ -> "groq";
            case MISTRAL -> "mistral";
            case OPENAI -> "openai";
        };
    }
}
```

Because the class and method are package-private, both test classes must sit in the same package — they do.

- [ ] **Step 5: Write the encoder**

`CapabilitiesEncoder.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encodes a catalog model's capability flags into {@code ai_gateway_model.capabilities}.
 *
 * <p>
 * That column is a free-form {@code VARCHAR(256)} that nothing in the codebase parses, so this class defines its
 * format and is its only writer. Tokens are sorted so repeated reconciles of an unchanged model produce a
 * byte-identical string — otherwise every sweep would look like a change and rewrite every row.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class CapabilitiesEncoder {

    private CapabilitiesEncoder() {
    }

    static String encode(CatalogModel model) {
        List<String> tokens = new ArrayList<>();

        if (model.attachment()) {
            tokens.add("attachment");
        }

        if (model.reasoning()) {
            tokens.add("reasoning");
        }

        if (model.structuredOutput()) {
            tokens.add("structured_output");
        }

        if (model.temperature()) {
            tokens.add("temperature");
        }

        if (model.toolCall()) {
            tokens.add("tool_call");
        }

        if (model.modalities()
            .input()
            .contains(Modality.IMAGE)) {

            tokens.add("vision");
        }

        Collections.sort(tokens);

        return String.join(",", tokens);
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*catalog*' > /tmp/t6.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t6.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/platform/platform-ai/platform-ai-gateway
git commit -m "Add models.dev provider mapping and capabilities encoder"
```

---

### Task 7: The reconciler

Implements the spec's rules table, including the asymmetric insert filter and idempotence.

**Files:**
- Create: `.../platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway/catalog/AiGatewayModelCatalogReconciler.java` (interface)
- Create: `.../platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/catalog/AiGatewayModelCatalogReconcilerImpl.java`
- Test: `.../platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/catalog/AiGatewayModelCatalogReconcilerTest.java`

**Interfaces:**
- Consumes: `AiGatewayProviderService.getEnabledProviders()`, `AiGatewayModelService.getModelsByProviderId(long)` / `create(AiGatewayModel)` / `updateFromCatalog(AiGatewayModel)`, `ModelCatalog.getModels(String)`, `AiGatewayModelsDevProviderIds.resolveProviderId`, `CapabilitiesEncoder.encode`.
- Produces: interface `AiGatewayModelCatalogReconciler` with `void reconcile()`, implemented by `AiGatewayModelCatalogReconcilerImpl(AiGatewayModelService, AiGatewayProviderService, ModelCatalog)`.

The interface lives in `-api` so the automation-side facade can trigger an on-demand reconcile (Task 9) without `automation-ai-gateway-service` depending on `platform-ai-gateway-service`. This is the same api/service split every other gateway service uses.

- [ ] **Step 1: Write the failing test**

`AiGatewayModelCatalogReconcilerTest.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.Cost;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayModelCatalogReconcilerTest {

    @Mock
    private AiGatewayModelService aiGatewayModelService;

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    @Mock
    private ModelCatalog modelCatalog;

    private AiGatewayModelCatalogReconciler reconciler;

    private static CatalogModel catalogModel(String id, Status status, Modality outputModality) {
        return new CatalogModel(
            id, id, null, null, false, false, true, false, true, false, null, null, null, status,
            new Modalities(List.of(Modality.TEXT), List.of(outputModality)), new Limit(400000, null, 128000),
            new Cost(
                new BigDecimal("1.25"), new BigDecimal("10"), null, null, null, null, null, List.of()));
    }

    private static AiGatewayProvider enabledProvider() {
        AiGatewayProvider provider = new AiGatewayProvider("OpenAI", AiGatewayProviderType.OPENAI, "sk-test");

        setId(provider, 7L);

        return provider;
    }

    private static AiGatewayModel existingRow(String name, boolean pinned) {
        AiGatewayModel model = new AiGatewayModel(7L, name);

        model.setCatalogPinned(pinned);
        setId(model, 42L);

        return model;
    }

    private static void setId(Object target, Long id) {
        try {
            java.lang.reflect.Field field = target.getClass()
                .getDeclaredField("id");

            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @BeforeEach
    void setUp() {
        reconciler = new AiGatewayModelCatalogReconcilerImpl(
            aiGatewayModelService, aiGatewayProviderService, modelCatalog);
    }

    @Test
    void testInsertsMissingModelWithCatalogValues() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        ArgumentCaptor<AiGatewayModel> captor = ArgumentCaptor.forClass(AiGatewayModel.class);

        verify(aiGatewayModelService).create(captor.capture());

        AiGatewayModel created = captor.getValue();

        assertThat(created.getName()).isEqualTo("gpt-5.1");
        assertThat(created.isEnabled()).isTrue();
        assertThat(created.getContextWindow()).isEqualTo(400000);
        assertThat(created.getInputCostPerMTokens()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(created.getOutputCostPerMTokens()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(created.getCapabilities()).isEqualTo("temperature,tool_call");
    }

    @Test
    void testUpdatesUnpinnedExistingRow() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(List.of(existingRow("gpt-5.1", false)));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiGatewayModelService).updateFromCatalog(any());
        verify(aiGatewayModelService, never()).create(any());
    }

    @Test
    void testSkipsPinnedRow() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(List.of(existingRow("gpt-5.1", true)));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiGatewayModelService, never()).updateFromCatalog(any());
        verify(aiGatewayModelService, never()).create(any());
    }

    @Test
    void testSkipsUncataloguedRowSilently() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(
            List.of(existingRow("ft:gpt-4o:acme:x", false)));
        when(modelCatalog.getModels("openai")).thenReturn(List.of());

        reconciler.reconcile();

        verify(aiGatewayModelService, never()).updateFromCatalog(any());
        verify(aiGatewayModelService, never()).create(any());
    }

    @Test
    void testSkipsDisabledProvider() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of());

        reconciler.reconcile();

        verify(aiGatewayModelService, never()).getModelsByProviderId(anyLong());
        verify(aiGatewayModelService, never()).create(any());
    }

    @Test
    void testDoesNotInsertDeprecatedModel() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-4", Status.DEPRECATED, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiGatewayModelService, never()).create(any());
    }

    @Test
    void testStillUpdatesDeprecatedModelThatAlreadyHasARow() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(List.of(existingRow("gpt-4", false)));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-4", Status.DEPRECATED, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiGatewayModelService).updateFromCatalog(any());
    }

    @Test
    void testDoesNotInsertNonTextOutputModel() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("dall-e-3", Status.ACTIVE, Modality.IMAGE)));

        reconciler.reconcile();

        verify(aiGatewayModelService, never()).create(any());
    }

    @Test
    void testInsertsModelWithoutCostAsNullRates() {
        CatalogModel unpriced = new CatalogModel(
            "acme-preview", "Acme Preview", null, null, false, false, false, false, false, false, null, null, null,
            Status.ACTIVE, new Modalities(List.of(Modality.TEXT), List.of(Modality.TEXT)),
            new Limit(8192, null, null), null);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(List.of(unpriced));

        reconciler.reconcile();

        ArgumentCaptor<AiGatewayModel> captor = ArgumentCaptor.forClass(AiGatewayModel.class);

        verify(aiGatewayModelService).create(captor.capture());

        AiGatewayModel created = captor.getValue();

        assertThat(created.getInputCostPerMTokens()).isNull();
        assertThat(created.getOutputCostPerMTokens()).isNull();
        assertThat(created.getContextWindow()).isEqualTo(8192);
    }

    /**
     * A daily sweep that rewrote every row would bump {@code last_modified_date} and the optimistic-locking version on
     * hundreds of rows for no reason, making "when did this model's pricing actually change?" unanswerable.
     */
    @Test
    void testSecondReconcileAgainstUnchangedCatalogWritesNothing() {
        AiGatewayModel alreadyCurrent = existingRow("gpt-5.1", false);

        alreadyCurrent.setCapabilities("temperature,tool_call");
        alreadyCurrent.setContextWindow(400000);
        alreadyCurrent.setInputCostPerMTokens(new BigDecimal("1.25"));
        alreadyCurrent.setOutputCostPerMTokens(new BigDecimal("10"));

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenReturn(List.of(alreadyCurrent));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiGatewayModelService, never()).updateFromCatalog(any());
    }

    @Test
    void testContinuesToNextProviderWhenOneFails() {
        AiGatewayProvider failing = new AiGatewayProvider("OpenAI", AiGatewayProviderType.OPENAI, "sk-a");
        AiGatewayProvider healthy = new AiGatewayProvider("Anthropic", AiGatewayProviderType.ANTHROPIC, "sk-b");

        setId(failing, 7L);
        setId(healthy, 8L);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(failing, healthy));
        when(aiGatewayModelService.getModelsByProviderId(7L)).thenThrow(new IllegalStateException("db down"));
        when(aiGatewayModelService.getModelsByProviderId(8L)).thenReturn(List.of());
        when(modelCatalog.getModels("anthropic")).thenReturn(
            List.of(catalogModel("claude-sonnet-4-6", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiGatewayModelService).create(any());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*AiGatewayModelCatalogReconcilerTest*' > /tmp/t7.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/t7.log | head
```

Expected: FAIL — `AiGatewayModelCatalogReconciler` does not exist.

- [ ] **Step 3: Write the interface**

`platform-ai-gateway-api/.../catalog/AiGatewayModelCatalogReconciler.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

/**
 * Populates {@code ai_gateway_model} rows from the models.dev catalog.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayModelCatalogReconciler {

    void reconcile();
}
```

- [ ] **Step 4: Write the implementation**

`platform-ai-gateway-service/.../catalog/AiGatewayModelCatalogReconcilerImpl.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.Cost;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Populates {@code ai_gateway_model} rows from the models.dev catalog.
 *
 * <p>
 * Insert is filtered and update is not, deliberately. Without the filter, enabling a provider would insert video and
 * image generation models the gateway cannot route, plus every deprecated model upstream still lists. But a deprecated
 * model a deployment is <em>already routing to</em> costs real money, so an existing row keeps being repriced no matter
 * what upstream marks it.
 *
 * <p>
 * Rows the catalog does not know — Azure deployment names, fine-tunes, models newer than the bundled snapshot — are
 * skipped silently and keep whatever rates were entered by hand.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public class AiGatewayModelCatalogReconcilerImpl implements AiGatewayModelCatalogReconciler {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayModelCatalogReconcilerImpl.class);

    private final AiGatewayModelService aiGatewayModelService;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final ModelCatalog modelCatalog;

    public AiGatewayModelCatalogReconcilerImpl(
        AiGatewayModelService aiGatewayModelService, AiGatewayProviderService aiGatewayProviderService,
        ModelCatalog modelCatalog) {

        this.aiGatewayModelService = aiGatewayModelService;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.modelCatalog = modelCatalog;
    }

    @Override
    public void reconcile() {
        for (AiGatewayProvider provider : aiGatewayProviderService.getEnabledProviders()) {
            try {
                reconcileProvider(provider);
            } catch (RuntimeException exception) {
                log.warn(
                    "models.dev reconcile failed for provider '{}' (id={}); continuing", provider.getName(),
                    provider.getId(), exception);
            }
        }
    }

    private void reconcileProvider(AiGatewayProvider provider) {
        String providerId = AiGatewayModelsDevProviderIds.resolveProviderId(provider.getType());

        List<CatalogModel> catalogModels = modelCatalog.getModels(providerId);

        if (catalogModels.isEmpty()) {
            return;
        }

        Map<String, AiGatewayModel> existingModels = new HashMap<>();

        for (AiGatewayModel model : aiGatewayModelService.getModelsByProviderId(provider.getId())) {
            existingModels.put(model.getName(), model);
        }

        for (CatalogModel catalogModel : catalogModels) {
            AiGatewayModel existingModel = existingModels.get(catalogModel.id());

            if (existingModel == null) {
                if (insertable(catalogModel)) {
                    aiGatewayModelService.create(toNewModel(provider.getId(), catalogModel));
                }

                continue;
            }

            if (existingModel.isCatalogPinned() || upToDate(existingModel, catalogModel)) {
                continue;
            }

            applyCatalogValues(existingModel, catalogModel);

            aiGatewayModelService.updateFromCatalog(existingModel);
        }
    }

    /**
     * Skips rows the catalog would write identically. Without this the daily sweep rewrites every row, bumping
     * {@code last_modified_date} and the optimistic-locking version across the whole table and destroying the audit
     * signal for when a model's pricing genuinely moved.
     */
    private static boolean upToDate(AiGatewayModel model, CatalogModel catalogModel) {
        return sameDecimal(model.getInputCostPerMTokens(), baseRate(catalogModel.cost(), true))
            && sameDecimal(model.getOutputCostPerMTokens(), baseRate(catalogModel.cost(), false))
            && Objects.equals(
                model.getContextWindow(),
                catalogModel.limit()
                    .context())
            && Objects.equals(model.getCapabilities(), CapabilitiesEncoder.encode(catalogModel));
    }

    private static boolean sameDecimal(
        @Nullable java.math.BigDecimal existingValue, @Nullable java.math.BigDecimal value) {

        if (existingValue == null || value == null) {
            return existingValue == value;
        }

        return existingValue.compareTo(value) == 0;
    }

    /**
     * The gateway routes text completions. A model that cannot emit text is not routable through it, and a deprecated
     * model is not something to newly adopt — but see the class Javadoc for why neither condition applies to updates.
     */
    private static boolean insertable(CatalogModel catalogModel) {
        return catalogModel.modalities()
            .output()
            .contains(Modality.TEXT) && catalogModel.status() != Status.DEPRECATED;
    }

    private static AiGatewayModel toNewModel(Long providerId, CatalogModel catalogModel) {
        AiGatewayModel model = new AiGatewayModel(providerId, catalogModel.id());

        applyCatalogValues(model, catalogModel);

        return model;
    }

    /**
     * Writes exactly the four catalog-owned fields. {@code alias}, {@code enabled}, and {@code defaultRoutingPolicyId}
     * belong to the administrator and are never touched here.
     */
    private static void applyCatalogValues(AiGatewayModel model, CatalogModel catalogModel) {
        model.setCapabilities(CapabilitiesEncoder.encode(catalogModel));
        model.setContextWindow(
            catalogModel.limit()
                .context());
        model.setInputCostPerMTokens(baseRate(catalogModel.cost(), true));
        model.setOutputCostPerMTokens(baseRate(catalogModel.cost(), false));
    }

    /**
     * Returns the base-tier rate. For the 23 tiered models upstream publishes, above-threshold traffic is under-costed
     * against this rate — a known limitation of the gateway's two flat cost columns, which could not express tiering
     * before this reconciler existed either.
     */
    private static @Nullable java.math.BigDecimal baseRate(@Nullable Cost cost, boolean input) {
        if (cost == null) {
            return null;
        }

        return input ? cost.input() : cost.output();
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*AiGatewayModelCatalogReconcilerTest*' > /tmp/t7.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t7.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/platform/platform-ai/platform-ai-gateway
git commit -m "Add ai_gateway_model catalog reconciler"
```

---

### Task 8: Reconciler scheduling and app wiring

Turns the reconciler into a bean that runs on startup and on a schedule, and puts the CE catalog module on server-app's classpath.

**Files:**
- Create: `.../platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/catalog/AiGatewayModelCatalogReconcilerConfiguration.java`
- Modify: `server/apps/server-app/build.gradle.kts` (add the two catalog modules near line 280)
- Modify: `.../platform-ai-gateway-service/build.gradle.kts` (add the `-service` module so the autoconfiguration is present)

**Interfaces:**
- Consumes: `AiGatewayModelCatalogReconciler.reconcile()`, `ModelCatalog` bean.
- Produces: Spring bean `AiGatewayModelCatalogReconciler`; a startup trigger and a `@Scheduled` sweep.

- [ ] **Step 1: Add the module dependencies**

In `server/apps/server-app/build.gradle.kts`, beside the other `platform-ai` entries:

```kotlin
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service"))
```

In `.../platform-ai-gateway-service/build.gradle.kts`, add:

```kotlin
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service"))
```

- [ ] **Step 2: Write the configuration**

`AiGatewayModelCatalogReconcilerConfiguration.java` (Enterprise header, `@version ee`):

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayModelCatalogReconcilerConfiguration {

    @Bean
    AiGatewayModelCatalogReconciler aiGatewayModelCatalogReconciler(
        AiGatewayModelService aiGatewayModelService, AiGatewayProviderService aiGatewayProviderService,
        ModelCatalog modelCatalog) {

        return new AiGatewayModelCatalogReconcilerImpl(aiGatewayModelService, aiGatewayProviderService, modelCatalog);
    }

    @Bean
    AiGatewayModelCatalogReconcilerScheduler aiGatewayModelCatalogReconcilerScheduler(
        AiGatewayModelCatalogReconciler reconciler) {

        return new AiGatewayModelCatalogReconcilerScheduler(reconciler);
    }
}
```

- [ ] **Step 3: Write the scheduler**

`AiGatewayModelCatalogReconcilerScheduler.java` (Enterprise header, `@version ee`), in the same package:

```java
package com.bytechef.ee.platform.ai.gateway.catalog;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Triggers {@link AiGatewayModelCatalogReconciler} once the application is ready and daily thereafter.
 *
 * <p>
 * The startup run is {@link Async} on purpose: it means a fresh deployment that has configured a provider has a
 * populated model list without waiting a day or finding a button, while the multi-megabyte catalog parse and the
 * resulting writes stay off the startup thread.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public class AiGatewayModelCatalogReconcilerScheduler {

    private final AiGatewayModelCatalogReconciler reconciler;

    public AiGatewayModelCatalogReconcilerScheduler(AiGatewayModelCatalogReconciler reconciler) {
        this.reconciler = reconciler;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        reconciler.reconcile();
    }

    @Scheduled(initialDelayString = "P1D", fixedDelayString = "P1D")
    public void reconcileOnSchedule() {
        reconciler.reconcile();
    }
}
```

- [ ] **Step 4: Compile and run the gateway module's tests**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test :server:apps:server-app:compileJava --continue > /tmp/t8.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t8.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/platform/platform-ai/platform-ai-gateway server/apps/server-app/build.gradle.kts
git commit -m "Schedule ai_gateway_model catalog reconciliation on startup and daily"
```

---

### Task 9: GraphQL surface for pinning and on-demand reconcile

Exposes `catalogPinned`, the unpin mutation, and the on-demand reconcile trigger the spec calls for alongside the startup and daily runs.

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-model.graphqls`
- Modify: `.../automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayModelGraphQlController.java`
- Modify: `.../automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayModelFacade.java`
- Modify: `.../automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayModelFacadeImpl.java`

**Interfaces:**
- Consumes: `AiGatewayModelService.unpin(long)`, `AiGatewayModel.isCatalogPinned()`, `AiGatewayModelCatalogReconciler.reconcile()`.
- Produces: GraphQL field `AiGatewayModel.catalogPinned: Boolean!`; mutations `unpinAiGatewayModel(id: ID!): AiGatewayModel` and `reconcileAiGatewayModelCatalog: Boolean`; `AiGatewayModelFacade.unpin(long) -> AiGatewayModel` and `AiGatewayModelFacade.reconcileCatalog()`.

- [ ] **Step 1: Extend the schema**

In `ai-gateway-model.graphqls`, add to `type AiGatewayModel` (keeping alphabetical order, after `capabilities`):

```graphql
    catalogPinned: Boolean!
```

And to `extend type Mutation`:

```graphql
    reconcileAiGatewayModelCatalog: Boolean
    unpinAiGatewayModel(id: ID!): AiGatewayModel
```

- [ ] **Step 2: Add the facade methods**

In `AiGatewayModelFacade` (api):

```java
    /**
     * Runs a models.dev reconcile immediately, rather than waiting for the daily sweep. Used after enabling a provider
     * or after an operator refreshes the bundled snapshot.
     */
    void reconcileCatalog();

    /**
     * Clears the catalog-override flag so the reconciler resumes managing this row.
     */
    AiGatewayModel unpin(long id);
```

In `AiGatewayModelFacadeImpl`, matching the `ADMIN` guard every other method on this class carries. The class is package-private with a `@SuppressFBWarnings("EI")` constructor — add the reconciler as a second constructor parameter:

```java
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void reconcileCatalog() {
        aiGatewayModelCatalogReconciler.reconcile();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayModel unpin(long id) {
        aiGatewayModelService.unpin(id);

        return aiGatewayModelService.getModel(id);
    }
```

Add `implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-api"))` to `automation-ai-gateway-service/build.gradle.kts` if it is not already present — the reconciler interface lives there, which is exactly why Task 7 put it in `-api`.

- [ ] **Step 3: Add the controller mutations**

In `AiGatewayModelGraphQlController`:

```java
    @MutationMapping
    public boolean reconcileAiGatewayModelCatalog() {
        aiGatewayModelFacade.reconcileCatalog();

        return true;
    }

    @MutationMapping
    public AiGatewayModel unpinAiGatewayModel(@Argument long id) {
        return aiGatewayModelFacade.unpin(id);
    }
```

- [ ] **Step 4: Compile and run the module's tests**

```bash
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:test :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --continue > /tmp/t9.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/t9.log | head
```

Expected: PASS, `exit=0`.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/automation/automation-ai/automation-ai-gateway
git commit -m "Expose catalogPinned and unpinAiGatewayModel over GraphQL"
```

---

### Task 10: Client — catalog badge and unpin action

Surfaces the flag in the models table so an administrator can tell a catalog-managed row from an overridden one.

**Files:**
- Modify: `client/src/graphql/automation/ai-gateway/aiGatewayModels.graphql`
- Modify: `client/src/pages/automation/ai/gateway/components/models/AiGatewayModels.tsx`
- Regenerate: `client/src/shared/middleware/graphql.ts` (and `graphql-types.ts`)

**Interfaces:**
- Consumes: GraphQL field `catalogPinned`, mutation `unpinAiGatewayModel`.
- Produces: a "Source" column rendering `Catalog` or `Overridden`.

- [ ] **Step 1: Add the field to every operation**

In `aiGatewayModels.graphql`, add `catalogPinned` to the selection set of **all four** operations that select model fields (`aiGatewayModels`, `aiGatewayModelsByProvider`, `createAiGatewayModel`, `updateAiGatewayModel`), keeping the existing alphabetical ordering — it goes immediately after `capabilities`.

Then append the mutation:

```graphql
mutation unpinAiGatewayModel($id: ID!) {
    unpinAiGatewayModel(id: $id) {
        alias
        capabilities
        catalogPinned
        contextWindow
        createdDate
        enabled
        id
        inputCostPerMTokens
        lastModifiedDate
        name
        outputCostPerMTokens
        providerId
        version
    }
}
```

- [ ] **Step 2: Regenerate the GraphQL client**

```bash
cd client && npx graphql-codegen > /tmp/codegen.log 2>&1; echo "exit=$?"; cd ..
```

Expected: `exit=0` and a modified `client/src/shared/middleware/graphql.ts`.

- [ ] **Step 3: Add the Source column**

In `AiGatewayModels.tsx`, add a header cell after the `Enabled` header (line 124):

```tsx
                                    <th className="pb-2 font-medium">Source</th>
```

And the matching body cell after the `Enabled` cell (after line 171):

```tsx
                                            <td className="py-3">
                                                <span
                                                    className={twMerge(
                                                        'rounded-full px-2 py-0.5 text-xs font-medium',
                                                        model.catalogPinned
                                                            ? 'bg-surface-warning-secondary text-content-warning-primary'
                                                            : 'bg-surface-neutral-secondary text-content-neutral-primary'
                                                    )}
                                                >
                                                    {model.catalogPinned ? 'Overridden' : 'Catalog'}
                                                </span>
                                            </td>
```

Both tokens already exist in this design system — `client/src/ee/pages/automation/ai-hub/tools/TaskToolChips.tsx:45` uses the same pair on a chip.

- [ ] **Step 4: Run the client checks**

```bash
cd client && npm run check > /tmp/client.log 2>&1; echo "exit=$?"; cd ..
grep -E "error|✖" /tmp/client.log | head
```

Expected: `exit=0`.

- [ ] **Step 5: Format and commit**

```bash
cd client && npm run format > /tmp/format.log 2>&1; echo "exit=$?"; cd ..
git add client/src/graphql/automation/ai-gateway/aiGatewayModels.graphql client/src/shared/middleware client/src/pages/automation/ai/gateway
git commit -m "client - Show catalog source on AI gateway models"
```

---

## Final verification

- [ ] **Full server check**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
./gradlew check --continue > /tmp/check.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/check.log | head -20
```

- [ ] **Full client check**

```bash
cd client && npm run check > /tmp/client-final.log 2>&1; echo "exit=$?"; cd ..
```

- [ ] **Manual smoke test**

Start infrastructure and the server, enable the gateway, create an OpenAI provider, and confirm the models table populates without typing a single rate:

```bash
cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd ..
./gradlew -p server/apps/server-app bootRun --args="--spring.profiles.active=dev --bytechef.ai.gateway.enabled=true"
```

Then in the UI: AI Gateway → Providers → add an OpenAI provider → Models. Expect roughly 45 rows with populated context windows, costs, and capabilities, each showing the `Catalog` badge. Edit one model's input cost, save, and confirm its badge flips to `Overridden`.
