# Guardrails OpenNLP Detector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional `platform-ai-guardrails-opennlp` module that detects unstructured PII (person, organization, location) with operator-supplied Apache OpenNLP models, behind the existing `SensitiveDataDetector` SPI.

**Architecture:** One Spring bean holding N pre-loaded `TokenNameFinderModel`s, registered only when the feature is enabled and at least one model is configured. Models load eagerly at construction so a bad path fails startup rather than degrading to silent non-coverage. `detect()` tokenizes once, runs each model behind its own try/catch, filters by confidence, and maps token-index spans back to character offsets.

**Tech Stack:** Java 25, Spring Boot 4, Gradle (Kotlin DSL), Apache OpenNLP 2.5.11 (`opennlp-tools`, pure Java), JUnit 5, AssertJ.

**Spec:** `docs/superpowers/specs/2026-08-24-guardrails-opennlp-detector-design.md`

## Global Constraints

- **EE license header on every new file** — `server/ee/` uses the ByteChef Enterprise header, NOT Apache 2.0. Copy it verbatim from a neighbouring file. Spotless keys off header CONTENT (`@version ee` in the class javadoc), not path. **Exception: `ApplicationProperties.java` is CE** (`server/libs/config/app-config`) — do not put an EE header on it, and do not add `@version ee` to it.
- **`@version ee` Javadoc tag on every new class** under `server/ee/`.
- **Ship no model files.** No `.bin` may be committed. No test may hit the network.
- **The new module depends on `platform-ai-guardrails-api` only** — never on `-service`. If you find yourself needing `-service`, stop and report: that would break the SPI's central claim.
- **OpenNLP version is exactly `2.5.11`.** Maven Central's search index reports `2.5.9` as newest — it lags; `repo1.maven.org` serves `2.5.11`. `3.0.0-M3` is a MILESTONE and must not be used.
- **Change nothing in `-api`, `-service`, the engine, or any existing detector.**
- **Blank line before control statements** (`if`, `for`, `while`, `try`, `switch`) except immediately after an opening `{`. No blank line before a class's closing `}`. Blank line after a variable modification a following statement uses.
- **No abbreviated variable names**, including lambda parameters and loop variables. **No `_` prefix on private methods.**
- **Test method names camelCase with no underscores**, including private helpers. Unit test classes end in `Test`, never `IntTest`.
- **Run `./gradlew spotlessApply` before every commit.**
- **Never judge a Gradle run piped into `tail`/`grep`** — redirect to a file, check `$?` on its own line, then grep the file for `^> Task .* FAILED`. **SpotBugs XML is disabled in this repo — read `build/reports/spotbugs/main.html`, never the XML.**

**Gradle project path for the new module:**
`:server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp`

**SPI types available from `platform-ai-guardrails-api`** (package `com.bytechef.ee.platform.ai.guardrails.detector`, all already committed — do not modify):

- `enum SensitiveKind { PII, SECRET }`
- `record SensitiveSpan(SensitiveKind kind, String category, int start, int end, double confidence)` with `static of(kind, category, start, end)` (confidence 1.0), `placeholder()` → `"[REDACTED_" + category + "]"`, `length()`, `overlaps(other)`. Compact constructor validates: `category` matches `[A-Z][A-Z0-9_]*`, `start >= 0`, `end > start`, `confidence` finite in `[0.0, 1.0]`.
- `interface SensitiveDataDetector` with `String name()`, `List<SensitiveSpan> detect(String text)`, `default boolean streamSafe() { return true; }`

---

## File Structure

**Created — new module** `server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-opennlp/`

| File | Responsibility |
|---|---|
| `build.gradle.kts` | Module dependencies |
| `src/main/java/.../opennlp/OpenNlpGuardrailsProperties.java` | `@ConfigurationProperties` binding |
| `src/main/java/.../opennlp/OpenNlpSensitiveDataDetector.java` | The detector: model holding, tokenization, span mapping, threshold |
| `src/main/java/.../opennlp/OpenNlpGuardrailsConfiguration.java` | Conditional bean registration |
| `src/test/java/.../opennlp/TrainedTestModels.java` | Trains throwaway models in memory for tests |
| `src/test/java/.../opennlp/OpenNlpSensitiveDataDetectorTest.java` | Detector behaviour |
| `src/test/java/.../opennlp/OpenNlpGuardrailsConfigurationTest.java` | Registration conditions |

Package for all of the above: `com.bytechef.ee.platform.ai.guardrails.opennlp`.

**Modified**

| File | Change |
|---|---|
| `settings.gradle.kts` | `include(...)` the new module |
| `gradle/libs.versions.toml` | Declare `opennlp-tools` |
| `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` | Add `Guardrails` subtree under `Ai` |
| `server/libs/config/app-config/src/test/java/com/bytechef/config/ApplicationPropertiesGuardrailsBindingTest.java` | New: strict-binding regression test (module already has a test source set) |
| `docs/agents/ai-guardrails.md` | Document the detector |
| `CLAUDE.md` | One-line pointer |

---

## Task 1: Module scaffolding, dependency, and config binding

**Files:**
- Modify: `settings.gradle.kts` (between the `-graphql` and `-service` includes, ~line 785)
- Modify: `gradle/libs.versions.toml` (`[libraries]` section)
- Create: `server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-opennlp/build.gradle.kts`
- Create: `.../platform-ai-guardrails-opennlp/src/main/java/com/bytechef/ee/platform/ai/guardrails/opennlp/OpenNlpGuardrailsProperties.java`
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`
- Test: `server/libs/config/app-config/src/test/java/com/bytechef/config/ApplicationPropertiesGuardrailsBindingTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `OpenNlpGuardrailsProperties` with `isEnabled()`, `getTokenizerModel()` (`@Nullable Resource`), `getMinConfidence()` (`double`), `getEntityModels()` (`Map<String, Resource>`); `ApplicationProperties.Ai.Guardrails.OpenNlp` mirroring the same keys.

**Why the binding test exists** (spec §5): `ApplicationProperties` is `@ConfigurationProperties(prefix = "bytechef", ignoreUnknownFields = false)`. Strict binding fails on keys that are **present in a property source but unbound**. This module's keys are present by construction — it is inert until an operator sets `enabled: true` — and the module is optional, so apps that do not carry it would see the keys with no bean to bind them. `bytechef.licence.*` broke every app's context this exact way once.

- [ ] **Step 1: Write the failing test**

Create `ApplicationPropertiesGuardrailsBindingTest.java`:

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

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Guards the strict-binding contract for the OpenNLP guardrails properties. `ApplicationProperties` binds the whole
 * `bytechef` tree with `ignoreUnknownFields = false`, so a key that is PRESENT in configuration but has no matching
 * field fails the application context — for every app, not just the one that owns the feature. The OpenNLP detector
 * lives in an optional module and is inert until an operator sets `enabled`, so its keys are present-by-construction
 * in deployments whose apps may not carry the module at all.
 */
class ApplicationPropertiesGuardrailsBindingTest {

    @Test
    void testOpenNlpGuardrailsKeysBindWithoutUnboundElements() {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(
            Map.of(
                "bytechef.ai.guardrails.opennlp.enabled", "true",
                "bytechef.ai.guardrails.opennlp.min-confidence", "0.9",
                "bytechef.ai.guardrails.opennlp.tokenizer-model", "classpath:tokenizer.bin",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON", "classpath:person.bin"));

        Binder binder = new Binder(source);

        assertThatCode(
            () -> binder.bind(
                "bytechef", Bindable.of(ApplicationProperties.class), new NoUnboundElementsBindHandler()))
                    .doesNotThrowAnyException();
    }
}
```

**`app-config` needs no build-file change.** It already declares `spring-boot-autoconfigure` (which brings `Binder` and `NoUnboundElementsBindHandler` transitively), `assertj-core` and `junit-jupiter` — verified. Do not add dependencies to it.

**Note on the license header:** `app-config` is a CE module using the **Apache 2.0** header shown above — verified against `ApplicationProperties.java`, not assumed. Do not use the EE header here and do not add `@version ee`. The module already has a test source set (`ApplicationPropertiesTest.java`, `AiProviderApiKeyTest.java`), so no scaffolding is needed.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:libs:config:app-config:test --tests 'com.bytechef.config.ApplicationPropertiesGuardrailsBindingTest' > /tmp/o1.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/o1.log
```

Expected: FAIL — `UnboundConfigurationPropertiesException` naming `bytechef.ai.guardrails.opennlp.*`, because `ApplicationProperties.Ai` has no `guardrails` field.

If instead it PASSES, stop and report: the strict-binding premise this task rests on does not hold, and Task 1's `ApplicationProperties` change may be unnecessary.

- [ ] **Step 3: Add the `Guardrails` subtree to `ApplicationProperties.Ai`**

Inside `public static class Ai`, the nested-class fields are alphabetical (`autoMemory`, `brave`, `copilot`, `firecrawl`, `gateway`, `hub`, …). Add the field between `gateway` and `hub`:

```java
        private Guardrails guardrails = new Guardrails();
```

Add its getter/setter beside the neighbouring ones, following the file's existing style exactly, and add the nested class alongside the other `Ai` nested classes:

```java
        public static class Guardrails {

            private OpenNlp openNlp = new OpenNlp();

            public OpenNlp getOpenNlp() {
                return openNlp;
            }

            public void setOpenNlp(OpenNlp openNlp) {
                this.openNlp = openNlp;
            }

            public static class OpenNlp {

                private boolean enabled;

                private Map<String, String> entityModels = new LinkedHashMap<>();

                private double minConfidence = 0.85;

                private String tokenizerModel;

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public Map<String, String> getEntityModels() {
                    return entityModels;
                }

                public void setEntityModels(Map<String, String> entityModels) {
                    this.entityModels = entityModels;
                }

                public double getMinConfidence() {
                    return minConfidence;
                }

                public void setMinConfidence(double minConfidence) {
                    this.minConfidence = minConfidence;
                }

                public String getTokenizerModel() {
                    return tokenizerModel;
                }

                public void setTokenizerModel(String tokenizerModel) {
                    this.tokenizerModel = tokenizerModel;
                }
            }
        }
```

Model paths are `String` here, not `Resource` — `ApplicationProperties` is a plain binding target and must not depend on the module's types. The module's own properties class does the `Resource` conversion.

Add `java.util.LinkedHashMap` and `java.util.Map` imports if not already present.

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:libs:config:app-config:test --tests 'com.bytechef.config.ApplicationPropertiesGuardrailsBindingTest' > /tmp/o1.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/o1.log || echo "no failed tasks"
```

Expected: exit=0.

- [ ] **Step 5: Register the module in `settings.gradle.kts`**

Insert so the four guardrails includes stay alphabetical (`-api`, `-graphql`, `-opennlp`, `-service`):

```kotlin
include("server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp")
```

- [ ] **Step 6: Declare the dependency in `gradle/libs.versions.toml`**

In the `[libraries]` section, alphabetically among the `org-apache-*` entries, matching the inline-coordinate style already used by `org-apache-poi-poi-ooxml`:

```toml
org-apache-opennlp-opennlp-tools = "org.apache.opennlp:opennlp-tools:2.5.11"
```

- [ ] **Step 7: Create the module's `build.gradle.kts`**

```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(libs.org.apache.opennlp.opennlp.tools)
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework:spring-test")
    testImplementation(project(":server:libs:test:test-support"))
}
```

- [ ] **Step 8: Create `OpenNlpGuardrailsProperties.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code bytechef.ai.guardrails.opennlp.*}.
 *
 * <p>
 * Model locations are Spring resource strings, so {@code file:} and {@code classpath:} both work without this module
 * implementing path handling of its own. Entity-model keys are {@code SensitiveSpan} categories, not a separate
 * vocabulary — a key of {@code PERSON} produces {@code [REDACTED_PERSON]} with no mapping table anywhere.
 * </p>
 *
 * <p>
 * The same keys are mirrored on {@code ApplicationProperties.Ai.Guardrails.OpenNlp}. That is not redundancy: the
 * central binder declares the whole {@code bytechef} tree with {@code ignoreUnknownFields = false}, so an
 * operator-set key with no field there fails every app's context, including apps that do not carry this module.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.ai.guardrails.opennlp")
public class OpenNlpGuardrailsProperties {

    private boolean enabled;

    private Map<String, String> entityModels = new LinkedHashMap<>();

    private double minConfidence = 0.85;

    private String tokenizerModel;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getEntityModels() {
        return entityModels;
    }

    public void setEntityModels(Map<String, String> entityModels) {
        this.entityModels = entityModels;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public String getTokenizerModel() {
        return tokenizerModel;
    }

    public void setTokenizerModel(String tokenizerModel) {
        this.tokenizerModel = tokenizerModel;
    }
}
```

- [ ] **Step 9: Verify the module resolves**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:compileJava > /tmp/o1b.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/o1b.log || echo "no failed tasks"
```

Expected: exit=0. If the `libs.org.apache.opennlp.opennlp.tools` accessor does not resolve, check the exact catalog key you added — Gradle maps `-` to `.` in accessors.

- [ ] **Step 10: Commit**

```bash
git add settings.gradle.kts gradle/libs.versions.toml server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-opennlp server/libs/config/app-config
git commit -m "Add the OpenNLP guardrails module skeleton and its config binding"
```

---

## Task 2: Trained test models and eager model loading

**Files:**
- Create: `.../platform-ai-guardrails-opennlp/src/test/java/com/bytechef/ee/platform/ai/guardrails/opennlp/TrainedTestModels.java`
- Create: `.../src/main/java/com/bytechef/ee/platform/ai/guardrails/opennlp/OpenNlpSensitiveDataDetector.java`
- Test: `.../src/test/java/com/bytechef/ee/platform/ai/guardrails/opennlp/OpenNlpSensitiveDataDetectorTest.java`

**Interfaces:**
- Consumes: `SensitiveDataDetector`, `SensitiveSpan`, `SensitiveKind` (from `-api`); `OpenNlpGuardrailsProperties` (Task 1).
- Produces:
  - `TrainedTestModels.personModelResource()` → `Resource` (a serialized model recognising a couple of names)
  - `TrainedTestModels.organizationModelResource()` → `Resource`
  - `OpenNlpSensitiveDataDetector(Map<String, Resource> entityModelResources, @Nullable Resource tokenizerModelResource, double minConfidence)`
  - `name()` → `"opennlp-ner"`

**The models are trained in memory.** The module ships no `.bin`, so tests build one from a handful of annotated sentences. The resulting model is poor at general NER and entirely adequate for exercising the adapter.

- [ ] **Step 1: Write the test fixture**

Create `TrainedTestModels.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import opennlp.tools.namefind.BioCodec;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ObjectStreamUtils;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ModelUtil;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Trains throwaway OpenNLP name-finder models in memory, so the adapter can be tested end to end without shipping a
 * model file or touching the network. The models are deliberately tiny — they recognise only the names they were
 * trained on — which is all that is needed to exercise tokenization, offset mapping, and thresholding.
 *
 * @version ee
 */
final class TrainedTestModels {

    private TrainedTestModels() {
    }

    static Resource personModelResource() {
        return modelResource(
            "person",
            sample(new String[] {
                "Ada", "Lovelace", "wrote", "the", "note", "."
            }, 0, 2),
            sample(new String[] {
                "Alan", "Turing", "read", "the", "note", "."
            }, 0, 2),
            sample(new String[] {
                "Ada", "Lovelace", "and", "Alan", "Turing", "met", "."
            }, 0, 2, 3, 5));
    }

    static Resource organizationModelResource() {
        return modelResource(
            "organization",
            sample(new String[] {
                "Acme", "Corp", "shipped", "it", "."
            }, 0, 2),
            sample(new String[] {
                "Globex", "shipped", "it", "."
            }, 0, 1),
            sample(new String[] {
                "Acme", "Corp", "bought", "Globex", "."
            }, 0, 2, 3, 4));
    }

    /**
     * Builds one training sample. Span bounds are TOKEN indices, given as start/end pairs, end exclusive.
     */
    private static NameSample sample(String[] tokens, int... spanBounds) {
        List<Span> spans = new ArrayList<>();

        for (int index = 0; index < spanBounds.length; index += 2) {
            spans.add(new Span(spanBounds[index], spanBounds[index + 1]));
        }

        return new NameSample(tokens, spans.toArray(new Span[0]), true);
    }

    private static Resource modelResource(String type, NameSample... samples) {
        TrainingParameters trainingParameters = ModelUtil.createDefaultTrainingParameters();

        trainingParameters.put(TrainingParameters.CUTOFF_PARAM, 0);
        trainingParameters.put(TrainingParameters.ITERATIONS_PARAM, 150);

        try (ObjectStream<NameSample> sampleStream = ObjectStreamUtils.createObjectStream(samples)) {
            TokenNameFinderModel model = NameFinderME.train(
                "eng", type, sampleStream, trainingParameters,
                TokenNameFinderFactory.create(null, null, Collections.emptyMap(), new BioCodec()));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            model.serialize(outputStream);

            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not train the test model for type " + type, exception);
        }
    }
}
```

**If `NameFinderME.train` fails on this tiny corpus**, increase `ITERATIONS_PARAM` or add one or two more samples of the same shape. Do NOT switch to loading a `.bin` from disk — no model file may enter the repository.

- [ ] **Step 2: Write the failing test**

Create `OpenNlpSensitiveDataDetectorTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveSpan;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @version ee
 */
class OpenNlpSensitiveDataDetectorTest {

    @Test
    void testNameIsStable() {
        assertThat(detector(Map.of("PERSON", TrainedTestModels.personModelResource())).name())
            .isEqualTo("opennlp-ner");
    }

    @Test
    void testLoadsEveryConfiguredModelAtConstruction() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of(
                "PERSON", TrainedTestModels.personModelResource(),
                "ORGANIZATION", TrainedTestModels.organizationModelResource()));

        List<SensitiveSpan> spans = detector.detect("Ada Lovelace wrote the note.");

        assertThat(spans).isNotEmpty();
    }

    @Test
    void testMissingModelResourceFailsConstruction() {
        assertThatThrownBy(
            () -> detector(Map.of("PERSON", new ClassPathResource("models/does-not-exist.bin"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PERSON");
    }

    @Test
    void testCorruptModelResourceFailsConstruction() {
        assertThatThrownBy(
            () -> detector(Map.of("PERSON", new org.springframework.core.io.ByteArrayResource("not a model".getBytes())))
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PERSON");
    }

    @Test
    void testRejectsCategoryThatIsNotAValidSpanCategory() {
        assertThatThrownBy(() -> detector(Map.of("lower-case", TrainedTestModels.personModelResource())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lower-case");
    }

    private static OpenNlpSensitiveDataDetector detector(Map<String, Resource> entityModelResources) {
        return new OpenNlpSensitiveDataDetector(entityModelResources, null, 0.0);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:test > /tmp/o2.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|error:' /tmp/o2.log | head
```

Expected: FAIL — `OpenNlpSensitiveDataDetector` does not exist.

- [ ] **Step 4: Write the detector's loading half**

Create `OpenNlpSensitiveDataDetector.java` with construction only; `detect` returns an empty list for now (Task 3 fills it in):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveDataDetector;
import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveSpan;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Detects unstructured personally-identifiable data — person names, organizations, locations — using
 * operator-supplied Apache OpenNLP name-finder models. This module ships none: Apache distributes no NER models, so
 * the detector is inert until an operator configures at least one.
 *
 * <p>
 * <b>Models are loaded eagerly</b> in this constructor. A missing, unreadable, or corrupt model fails application
 * startup rather than being caught later by the engine's fail-open detector policy, which would hand the operator a
 * guardrail that silently protects nothing.
 * </p>
 *
 * <p>
 * <b>Thread safety.</b> {@link TokenNameFinderModel} is thread-safe and is held for the process lifetime;
 * {@code NameFinderME} is NOT, and is therefore constructed per {@code detect} call (see Task 3). Caching the finder
 * instead is the natural-looking optimization and is wrong — its state is per-document, so concurrent calls interleave
 * and produce spans at positions that never held an entity.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenNlpSensitiveDataDetector implements SensitiveDataDetector {

    private static final Logger log = LoggerFactory.getLogger(OpenNlpSensitiveDataDetector.class);

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final List<EntityModel> entityModels;
    private final double minConfidence;
    private final Supplier<Tokenizer> tokenizerSupplier;

    public OpenNlpSensitiveDataDetector(
        Map<String, Resource> entityModelResources, @Nullable Resource tokenizerModelResource, double minConfidence) {

        this.entityModels = loadEntityModels(entityModelResources);
        this.minConfidence = minConfidence;
        this.tokenizerSupplier = loadTokenizerSupplier(tokenizerModelResource);

        log.info(
            "OpenNLP sensitive-data detector loaded {} model(s) with a minimum confidence of {}",
            entityModels.size(), minConfidence);
    }

    @Override
    public String name() {
        return "opennlp-ner";
    }

    @Override
    public List<SensitiveSpan> detect(String text) {
        return List.of();
    }

    private static List<EntityModel> loadEntityModels(Map<String, Resource> entityModelResources) {
        List<EntityModel> loaded = new ArrayList<>(entityModelResources.size());

        for (Map.Entry<String, Resource> entry : entityModelResources.entrySet()) {
            String category = entry.getKey();

            if (!CATEGORY_PATTERN.matcher(category)
                .matches()) {

                throw new IllegalArgumentException(
                    "OpenNLP entity-model category must match [A-Z][A-Z0-9_]*, got: " + category);
            }

            loaded.add(new EntityModel(category, loadEntityModel(category, entry.getValue())));
        }

        return List.copyOf(loaded);
    }

    private static TokenNameFinderModel loadEntityModel(String category, Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new TokenNameFinderModel(inputStream);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                "Could not load the OpenNLP model configured for category " + category + " from " +
                    resource.getDescription(),
                exception);
        }
    }

    private static Supplier<Tokenizer> loadTokenizerSupplier(@Nullable Resource tokenizerModelResource) {
        if (tokenizerModelResource == null) {
            return () -> SimpleTokenizer.INSTANCE;
        }

        try (InputStream inputStream = tokenizerModelResource.getInputStream()) {
            TokenizerModel tokenizerModel = new TokenizerModel(inputStream);

            // TokenizerME is not thread-safe, so a fresh one is built per call, exactly as NameFinderME is.
            return () -> new TokenizerME(tokenizerModel);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                "Could not load the OpenNLP tokenizer model from " + tokenizerModelResource.getDescription(),
                exception);
        }
    }

    private record EntityModel(String category, TokenNameFinderModel model) {
    }
}
```

- [ ] **Step 5: Run test to verify the loading tests pass**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:test > /tmp/o2.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/o2.log || echo "no failed tasks"
grep -E 'Results:' /tmp/o2.log
```

Expected: `testLoadsEveryConfiguredModelAtConstruction` FAILS (detect returns empty — Task 3 fixes it); every other test passes. That single expected failure is fine at this point; do not implement `detect` yet to make it green.

If a different test fails, fix that before continuing.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-opennlp
git commit -m "Load OpenNLP entity models eagerly, failing startup on a bad one"
```

---

## Task 3: `detect()` — tokenization, span mapping, and the confidence threshold

**Files:**
- Modify: `.../opennlp/OpenNlpSensitiveDataDetector.java` (replace the stub `detect`)
- Test: `.../opennlp/OpenNlpSensitiveDataDetectorTest.java` (add cases)

**Interfaces:**
- Consumes: everything from Task 2.
- Produces: a working `detect(String)` returning `SensitiveSpan`s with `kind == SensitiveKind.PII`, the configured category, character offsets, and the model's probability as `confidence`.

**The offset arithmetic is the risk.** `NameFinderME.find` returns spans in TOKEN indices; `SensitiveSpan` needs CHARACTER offsets. `Span.getEnd()` is exclusive, so the last token of an entity is at `getEnd() - 1`. Getting this wrong redacts the wrong characters — silently, and plausibly, since the output still looks redacted.

- [ ] **Step 1: Write the failing tests**

Add to `OpenNlpSensitiveDataDetectorTest.java`:

```java
    @Test
    void testMapsAMultiTokenEntityToItsCharacterOffsets() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        String text = "Hello Ada Lovelace wrote the note.";

        List<SensitiveSpan> spans = detector.detect(text);

        assertThat(spans).isNotEmpty();

        SensitiveSpan span = spans.getFirst();

        assertThat(text.substring(span.start(), span.end())).isEqualTo("Ada Lovelace");
        assertThat(span.category()).isEqualTo("PERSON");
        assertThat(span.kind()).isEqualTo(SensitiveKind.PII);
    }

    @Test
    void testSpansCarryTheModelProbabilityAsConfidence() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        List<SensitiveSpan> spans = detector.detect("Ada Lovelace wrote the note.");

        assertThat(spans).isNotEmpty();
        assertThat(spans.getFirst()
            .confidence()).isBetween(0.0, 1.0);
    }

    @Test
    void testSpansBelowMinConfidenceAreDropped() {
        Map<String, Resource> models = Map.of("PERSON", TrainedTestModels.personModelResource());

        List<SensitiveSpan> permissive =
            new OpenNlpSensitiveDataDetector(models, null, 0.0).detect("Ada Lovelace wrote the note.");
        List<SensitiveSpan> strict =
            new OpenNlpSensitiveDataDetector(models, null, 1.01).detect("Ada Lovelace wrote the note.");

        assertThat(permissive).isNotEmpty();
        assertThat(strict).isEmpty();
    }

    @Test
    void testTextWithNoEntitiesYieldsNoSpans() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        assertThat(detector.detect("the note was filed yesterday")).isEmpty();
    }

    @Test
    void testEmptyTextYieldsNoSpans() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        assertThat(detector.detect("")).isEmpty();
    }
```

Add the import `com.bytechef.ee.platform.ai.guardrails.detector.SensitiveKind;`.

**Note on `testSpansBelowMinConfidenceAreDropped`:** `1.01` is deliberately above the maximum possible probability, so the assertion does not depend on how confident this particular tiny model happens to be. Do not "tighten" it to a value inside `[0, 1]` — that makes the test depend on training luck.

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:test > /tmp/o3.log 2>&1; echo "exit=$?"; grep -E 'Results:|expected' /tmp/o3.log | head
```

Expected: the new tests FAIL because `detect` returns an empty list.

- [ ] **Step 3: Implement `detect`**

Replace the stub with:

```java
    @Override
    public List<SensitiveSpan> detect(String text) {
        Tokenizer tokenizer = tokenizerSupplier.get();

        Span[] tokenPositions = tokenizer.tokenizePos(text);

        if (tokenPositions.length == 0) {
            return List.of();
        }

        String[] tokens = new String[tokenPositions.length];

        for (int index = 0; index < tokenPositions.length; index++) {
            tokens[index] = tokenPositions[index].getCoveredText(text)
                .toString();
        }

        List<SensitiveSpan> spans = new ArrayList<>();

        for (EntityModel entityModel : entityModels) {
            spans.addAll(findSpans(entityModel, tokens, tokenPositions));
        }

        return spans;
    }

    private List<SensitiveSpan> findSpans(EntityModel entityModel, String[] tokens, Span[] tokenPositions) {
        // NameFinderME is not thread-safe and this detector is shared across every concurrent request, so a fresh one
        // is built per call. It wraps the already-parsed model, so construction is cheap. This also removes the
        // clearAdaptiveData() obligation a cached finder would carry, whose omission would leak one request's entity
        // context into the next.
        NameFinderME nameFinder = new NameFinderME(entityModel.model());

        Span[] entities = nameFinder.find(tokens);
        double[] probabilities = nameFinder.probs(entities);

        List<SensitiveSpan> spans = new ArrayList<>(entities.length);

        for (int index = 0; index < entities.length; index++) {
            Span entity = entities[index];

            if (entity.getStart() < 0 || entity.getEnd() > tokenPositions.length ||
                entity.getStart() >= entity.getEnd()) {

                throw new IllegalStateException(
                    "OpenNLP model for category " + entityModel.category() + " returned an out-of-range token span");
            }

            double confidence = clampProbability(probabilities[index]);

            if (confidence < minConfidence) {
                continue;
            }

            // getEnd() is exclusive, so the entity's last token is at getEnd() - 1.
            int start = tokenPositions[entity.getStart()].getStart();
            int end = tokenPositions[entity.getEnd() - 1].getEnd();

            spans.add(new SensitiveSpan(SensitiveKind.PII, entityModel.category(), start, end, confidence));
        }

        return spans;
    }

    private static double clampProbability(double probability) {
        if (!Double.isFinite(probability)) {
            return 0.0;
        }

        return Math.min(1.0, Math.max(0.0, probability));
    }
```

Add imports: `com.bytechef.ee.platform.ai.guardrails.detector.SensitiveKind`, `opennlp.tools.namefind.NameFinderME`, `opennlp.tools.util.Span`.

`clampProbability` exists because `SensitiveSpan`'s compact constructor rejects a confidence outside `[0.0, 1.0]` or non-finite — a model returning `1.0000000001` through floating-point accumulation would otherwise throw out of a detector whose whole job is to fail softly.

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:test > /tmp/o3.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/o3.log || echo "no failed tasks"
grep -E 'Results:' /tmp/o3.log
```

Expected: exit=0, all tests pass including Task 2's `testLoadsEveryConfiguredModelAtConstruction`.

**If `testMapsAMultiTokenEntityToItsCharacterOffsets` fails on the substring assertion**, the offset mapping is wrong — do not adjust the expected string. Check the `getEnd() - 1`.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-opennlp
git commit -m "Map OpenNLP token spans to character offsets and apply the confidence floor"
```

---

## Task 4: Per-model isolation, stream safety, and concurrency

**Files:**
- Modify: `.../opennlp/OpenNlpSensitiveDataDetector.java`
- Test: `.../opennlp/OpenNlpSensitiveDataDetectorTest.java`

**Interfaces:**
- Consumes: everything from Task 3.
- Produces: `streamSafe()` returning `false`; `detect` no longer propagates one model's failure.

- [ ] **Step 1: Write the failing tests**

```java
    @Test
    void testIsNotStreamSafe() {
        assertThat(detector(Map.of("PERSON", TrainedTestModels.personModelResource())).streamSafe())
            .isFalse();
    }

    @Test
    void testOneFailingModelDoesNotSuppressAnother() throws Exception {
        OpenNlpSensitiveDataDetector detector = new OpenNlpSensitiveDataDetector(
            Map.of(
                "PERSON", TrainedTestModels.personModelResource(),
                "ORGANIZATION", TrainedTestModels.organizationModelResource()),
            null, 0.0);

        breakOneModel(detector, "PERSON");

        List<SensitiveSpan> spans = detector.detect("Acme Corp shipped it.");

        assertThat(spans).isNotEmpty();
        assertThat(spans).allMatch(span -> "ORGANIZATION".equals(span.category()));
    }

    @Test
    void testConcurrentDetectionReturnsCorrectSpans() throws Exception {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        String text = "Hello Ada Lovelace wrote the note.";

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        try {
            List<Future<String>> futures = new ArrayList<>();

            for (int attempt = 0; attempt < 200; attempt++) {
                futures.add(executorService.submit(() -> {
                    List<SensitiveSpan> spans = detector.detect(text);

                    if (spans.isEmpty()) {
                        return "<none>";
                    }

                    SensitiveSpan span = spans.getFirst();

                    return text.substring(span.start(), span.end());
                }));
            }

            for (Future<String> future : futures) {
                assertThat(future.get()).isEqualTo("Ada Lovelace");
            }
        } finally {
            executorService.shutdownNow();
        }
    }
```

And the helper that corrupts one loaded model, placed with the other private helpers:

```java
    /**
     * Replaces one already-loaded model with a stand-in whose use throws, so per-model isolation can be exercised
     * without a corrupt model file. Reflection is used deliberately: the field is private because nothing in
     * production should replace a loaded model, and the alternative — a test-only setter on the production class —
     * would be worse.
     */
    private static void breakOneModel(OpenNlpSensitiveDataDetector detector, String category) throws Exception {
        Field field = OpenNlpSensitiveDataDetector.class.getDeclaredField("entityModels");

        field.setAccessible(true);

        List<?> entityModels = (List<?>) field.get(detector);
        List<Object> rewritten = new ArrayList<>();

        for (Object entityModel : entityModels) {
            Method categoryMethod = entityModel.getClass()
                .getDeclaredMethod("category");

            categoryMethod.setAccessible(true);

            if (category.equals(categoryMethod.invoke(entityModel))) {
                Constructor<?> constructor = entityModel.getClass()
                    .getDeclaredConstructors()[0];

                constructor.setAccessible(true);

                rewritten.add(constructor.newInstance(category, null));
            } else {
                rewritten.add(entityModel);
            }
        }

        field.set(detector, List.copyOf(rewritten));
    }
```

Imports for the test: `java.lang.reflect.Constructor`, `java.lang.reflect.Field`, `java.lang.reflect.Method`, `java.util.ArrayList`, `java.util.concurrent.ExecutorService`, `java.util.concurrent.Executors`, `java.util.concurrent.Future`.

Passing `null` as the model makes `new NameFinderME(null)` throw, which is exactly the runtime failure the isolation must contain.

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:test > /tmp/o4.log 2>&1; echo "exit=$?"; grep -E 'Results:|expected' /tmp/o4.log | head
```

Expected: `testIsNotStreamSafe` fails (the SPI default is `true`), and `testOneFailingModelDoesNotSuppressAnother` fails with the model's exception propagating out of `detect`.

- [ ] **Step 3: Add `streamSafe()` and per-model isolation**

Add the override:

```java
    /**
     * Named-entity recognition is not local: run over a bounded lookahead window that starts mid-sentence, it gives
     * different and worse answers than over the whole text. Returning {@code false} keeps this detector out of the
     * streaming redactor rather than letting it contribute unreliable spans there. Streamed completions therefore get
     * regex redaction only; batch response scanning and all request-direction scanning still cover NER.
     */
    @Override
    public boolean streamSafe() {
        return false;
    }
```

Wrap the per-model call in `detect`:

```java
        for (EntityModel entityModel : entityModels) {
            try {
                spans.addAll(findSpans(entityModel, tokens, tokenPositions));
            } catch (RuntimeException exception) {
                // Contain the failure to the model that caused it. The engine's SensitiveDataRedactor discards a
                // detector's ENTIRE batch when detect() throws, so letting this propagate would silently cost every
                // other category its spans too.
                log.warn(
                    "OpenNLP model for category '{}' failed; continuing without its spans", entityModel.category(),
                    exception);
            }
        }
```

`findSpans` already builds its own list and returns it only on success, so a model failing part-way contributes nothing rather than a partial set.

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:test > /tmp/o4.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/o4.log || echo "no failed tasks"
grep -E 'Results:' /tmp/o4.log
```

Expected: exit=0.

- [ ] **Step 5: Prove the concurrency test has teeth**

Temporarily hoist the `NameFinderME` into a field constructed once in `findSpans`' place (i.e. cache it per `EntityModel`), re-run **only** `testConcurrentDetectionReturnsCorrectSpans`, and confirm it FAILS or becomes flaky. Then revert to the per-call construction and confirm it passes again.

Record in your report which failure you observed. If the cached version passes reliably, say so — the test is then weaker than intended and the controller needs to know rather than being told it holds.

```bash
git diff --stat server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-opennlp
```

Expected after reverting: no diff for `OpenNlpSensitiveDataDetector.java` beyond the intended Task 4 changes.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-opennlp
git commit -m "Contain per-model failures and keep NER out of the streaming path"
```

---

## Task 5: Spring wiring and conditional registration

**Files:**
- Create: `.../opennlp/OpenNlpGuardrailsConfiguration.java`
- Test: `.../opennlp/OpenNlpGuardrailsConfigurationTest.java`

**Interfaces:**
- Consumes: `OpenNlpGuardrailsProperties` (Task 1), `OpenNlpSensitiveDataDetector` (Tasks 2–4).
- Produces: an `OpenNlpSensitiveDataDetector` bean, present only when `enabled=true` AND `entity-models` is non-empty.

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveDataDetector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.Resource;

/**
 * @version ee
 */
class OpenNlpGuardrailsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OpenNlpGuardrailsConfiguration.class));

    @Test
    void testNoDetectorWhenDisabled() {
        contextRunner
            .withPropertyValues(
                "bytechef.ai.guardrails.opennlp.enabled=false",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=" + personModelPath())
            .run(context -> assertThat(context).doesNotHaveBean(SensitiveDataDetector.class));
    }

    @Test
    void testNoDetectorWhenEnabledButNoModelsConfigured() {
        contextRunner
            .withPropertyValues("bytechef.ai.guardrails.opennlp.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(SensitiveDataDetector.class));
    }

    @Test
    void testDetectorRegisteredWhenEnabledWithAModel() {
        contextRunner
            .withPropertyValues(
                "bytechef.ai.guardrails.opennlp.enabled=true",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=" + personModelPath())
            .run(context -> {
                assertThat(context).hasSingleBean(SensitiveDataDetector.class);
                assertThat(context.getBean(SensitiveDataDetector.class)
                    .name()).isEqualTo("opennlp-ner");
            });
    }

    @Test
    void testBadModelPathFailsTheContext() {
        contextRunner
            .withPropertyValues(
                "bytechef.ai.guardrails.opennlp.enabled=true",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=file:/does/not/exist.bin")
            .run(context -> assertThat(context).hasFailed());
    }

    /**
     * Writes the in-memory trained model to a temp file, because the configuration resolves models from resource
     * STRINGS and a ByteArrayResource has no string form.
     */
    private static String personModelPath() {
        try {
            Resource resource = TrainedTestModels.personModelResource();

            Path path = Files.createTempFile("bytechef-opennlp-person", ".bin");

            Files.write(path, resource.getInputStream()
                .readAllBytes());

            path.toFile()
                .deleteOnExit();

            return "file:" + path;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not materialize the test model", exception);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:test --tests '*OpenNlpGuardrailsConfigurationTest' > /tmp/o5.log 2>&1; echo "exit=$?"; grep -E 'error:|^> Task .* FAILED' /tmp/o5.log | head
```

Expected: FAIL — `OpenNlpGuardrailsConfiguration` does not exist.

- [ ] **Step 3: Write the configuration**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

/**
 * Registers the OpenNLP detector when an operator has both enabled it AND configured at least one model.
 *
 * <p>
 * The second condition matters: an enabled-but-empty configuration would otherwise register a detector that can never
 * contribute a span, which the engine would then call on every request for nothing. Absent is clearer than inert.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.guardrails.opennlp", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenNlpGuardrailsProperties.class)
public class OpenNlpGuardrailsConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.guardrails.opennlp", name = "entity-models")
    OpenNlpSensitiveDataDetector openNlpSensitiveDataDetector(
        OpenNlpGuardrailsProperties openNlpGuardrailsProperties, ResourceLoader resourceLoader) {

        Map<String, Resource> entityModelResources = new LinkedHashMap<>();

        Map<String, String> configuredModels = openNlpGuardrailsProperties.getEntityModels();

        for (Map.Entry<String, String> entry : configuredModels.entrySet()) {
            entityModelResources.put(entry.getKey(), resourceLoader.getResource(entry.getValue()));
        }

        return new OpenNlpSensitiveDataDetector(
            entityModelResources, tokenizerResource(openNlpGuardrailsProperties, resourceLoader),
            openNlpGuardrailsProperties.getMinConfidence());
    }

    private static @Nullable Resource tokenizerResource(
        OpenNlpGuardrailsProperties openNlpGuardrailsProperties, ResourceLoader resourceLoader) {

        String tokenizerModel = openNlpGuardrailsProperties.getTokenizerModel();

        if (!StringUtils.hasText(tokenizerModel)) {
            return null;
        }

        return resourceLoader.getResource(tokenizerModel);
    }
}
```

**If `@ConditionalOnProperty` with `name = "entity-models"` does not fire for a map property**, replace that annotation with an explicit guard: return the bean only when the map is non-empty, using `@Bean` plus a `@ConditionalOnExpression`, or register the bean unconditionally under the class-level condition and have `testNoDetectorWhenEnabledButNoModelsConfigured` assert on an empty-model context differently. Report which route you took and why — do not silently drop the empty-config requirement.

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:test > /tmp/o5.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/o5.log || echo "no failed tasks"
grep -E 'Results:' /tmp/o5.log
```

Expected: exit=0.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-opennlp
git commit -m "Register the OpenNLP detector only when enabled with a model"
```

---

## Task 6: Documentation

**Files:**
- Modify: `docs/agents/ai-guardrails.md`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: everything above.
- Produces: nothing in code.

- [ ] **Step 1: Read the target sections**

```bash
grep -nE '^#{2,3} ' docs/agents/ai-guardrails.md
grep -n 'SensitiveDataDetector' docs/agents/ai-guardrails.md | head
```

The detector SPI is described under the sensitive-data detectors section. Add the new module there, in the document's existing voice.

- [ ] **Step 2: Document the module in `docs/agents/ai-guardrails.md`**

Cover, in that section's voice:

- The module exists, is EE, is optional, and is **off by default**.
- **It ships no models, and Apache distributes none.** State this plainly — do not present the module as a feature that merely needs switching on. Anyone reaching for the legacy SourceForge 1.5 binaries is pointing a fifteen-year-old newswire model at chat and code text in a path that rewrites prompts irreversibly.
- The configuration block from spec §5, verbatim.
- Entity-model keys ARE `SensitiveSpan` categories — `PERSON` yields `[REDACTED_PERSON]` with no mapping table.
- Models load eagerly; a bad path fails startup, deliberately, because the engine's fail-open policy would otherwise turn a typo into a guardrail that silently protects nothing.
- `streamSafe()` is false, so **streamed completions get regex redaction only**; batch and request-direction scanning cover NER.
- `min-confidence` defaults to `0.85` and is the only false-positive control on a destructive pre-model path.
- The `ApplicationProperties` requirement and why it exists (spec §5) — this is the trap for whoever adds the next optional module with operator-settable properties.

- [ ] **Step 3: Add a pointer in `CLAUDE.md`**

In the "AI Guardrails (EE, standalone across surfaces)" section, two or three lines at most, recording the invariant rather than the implementation: an optional `platform-ai-guardrails-opennlp` module contributes NER through the SPI, ships no models, is off by default, and is not stream-safe. Point at `docs/agents/ai-guardrails.md` for detail. Do not restructure the section.

- [ ] **Step 4: Verify no overstatement survives**

```bash
grep -niE 'out of the box|just enable|works by default' docs/agents/ai-guardrails.md CLAUDE.md || echo "  none"
```

Expected: no hit that refers to the OpenNLP module.

- [ ] **Step 5: Commit**

```bash
git add docs/agents/ai-guardrails.md CLAUDE.md
git commit -m "docs - Document the optional OpenNLP guardrails detector"
```

---

## Task 7: Full verification

- [ ] **Step 1: Format and check the touched modules**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:check \
          :server:libs:config:app-config:check --continue > /tmp/o7.log 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' /tmp/o7.log || echo "no failed tasks"
```

Expected: exit=0. On a SpotBugs failure read `build/reports/spotbugs/main.html` — the XML report is disabled in this repo and is stale.

- [ ] **Step 2: Compile the whole server**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/o7c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/o7c.log || echo "no failed tasks"
```

Expected: exit=0.

- [ ] **Step 3: Confirm the guardrails engine is untouched**

```bash
git diff --name-only HEAD~6..HEAD | grep -E 'platform-ai-guardrails-(api|service|graphql)/' && echo "VIOLATION: engine modules changed" || echo "engine untouched, as designed"
```

Expected: `engine untouched, as designed`. If anything is listed, stop and report — the SPI's central claim is that a detector needs no engine change, and a violation is a finding about the SPI, not something to quietly accept.

- [ ] **Step 4: Confirm no model file was committed**

```bash
git diff --name-only HEAD~6..HEAD | grep -E '\.bin$' && echo "VIOLATION: model file committed" || echo "no model files, as designed"
```

Expected: `no model files, as designed`.

- [ ] **Step 5: Confirm the dependency is the intended version**

```bash
grep -n 'opennlp' gradle/libs.versions.toml
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp:dependencies --configuration runtimeClasspath > /tmp/o7d.log 2>&1
grep -E 'opennlp|onnx|native' /tmp/o7d.log | head
```

Expected: `opennlp-tools:2.5.11`, and **no** `onnxruntime` or other native dependency anywhere in the tree.

- [ ] **Step 6: Report**

State: the check/compile results, the four confirmations above, whether the concurrency test was proven to have teeth (Task 4 Step 5) and what failure was observed, and which route Task 5 Step 3 took for the empty-model condition.

---

## Self-Review

**Spec coverage:**

| Spec section | Task |
|---|---|
| §4 module, dependency, version 2.5.11 | Task 1 (steps 5–7, 9) |
| §5 configuration + `ApplicationProperties` strict binding | Task 1 (steps 1–4, 8) |
| §6 detector, §6.1 thread safety | Tasks 2, 3; concurrency proof in Task 4 step 5 |
| §6.2 span mapping | Task 3 |
| §6.3 `streamSafe()` false | Task 4 |
| §6.4 per-model isolation | Task 4 |
| §6.5 confidence threshold | Task 3 |
| §7 in-memory trained models | Task 2 (`TrainedTestModels`) |
| §8 who this is / is not for | Task 6 |
| D2 eager fail-fast | Task 2 (steps 2, 4), Task 5 (`testBadModelPathFailsTheContext`) |
| D6 register only when enabled AND non-empty | Task 5 |
| D7 `ApplicationProperties` subtree | Task 1 |
| §3 non-goals (no models, no ONNX/native) | Task 7 steps 4–5 |

No spec section is unimplemented.

**Placeholder scan:** no "TBD", "TODO", "similar to Task N", or "add appropriate error handling". The three places that say "if X does not work, do Y and report" (Task 2 step 1, Task 4 step 5, Task 5 step 3) are deliberate: each names a concrete alternative and requires the implementer to report which route they took, rather than leaving the outcome unspecified.

**Type consistency:** `OpenNlpSensitiveDataDetector(Map<String, Resource>, @Nullable Resource, double)` is used identically in Tasks 2, 3, 4 and 5. `TrainedTestModels.personModelResource()` / `.organizationModelResource()` return `Resource` everywhere. The private `EntityModel` record's accessor is `category()`/`model()` in both the production code (Task 2) and the reflection helper (Task 4). `name()` returns `"opennlp-ner"` in Task 2 and is asserted as that string in Tasks 2 and 5.

**One deliberate deviation from the spec, flagged rather than silent:** spec §5 asks for "an integration-level assertion that a context boots with these keys set, in an app that does not carry the module." Task 1 instead binds `ApplicationProperties` directly through Spring's `Binder` with `NoUnboundElementsBindHandler` — the same mechanism the app context uses — because booting a full app context in `app-config`'s test source set would require pulling an application module into it, and the binding test isolates the exact failure mode (`present-but-unbound`) without that weight. If the reviewer judges this insufficient, the heavier test belongs in an app module's existing `*IntTest`.
