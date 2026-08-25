# Guardrails Sensitive-Data Detector SPI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `AiGuardrails`' sequential `String.replaceAll` redaction chain with a detect → resolve → apply pipeline behind a bean-contributed `SensitiveDataDetector` SPI, fixing a partial-secret leak on the way.

**Architecture:** Three new types in `platform-ai-guardrails-api` (`SensitiveKind`, `SensitiveSpan`, `SensitiveDataDetector`) define the contract. `SensitiveDataRedactor` in `-service` runs every registered detector over the original text, resolves overlapping candidate spans by a total order (secrets beat PII, then longer, then earlier, then category), and applies the winners right-to-left. Two built-in detectors hold the regex patterns that live in `AiGuardrails` today. `AiGuardrails` and `StreamingResponseRedactor` become consumers of the redactor rather than owners of the patterns.

**Tech Stack:** Java 25, Spring Boot 4, Gradle (Kotlin DSL), JUnit 5, AssertJ, Mockito, Micrometer. **No new third-party dependency is added by this plan.**

**Spec:** `docs/superpowers/specs/2026-08-24-guardrails-sensitive-data-detector-spi-design.md`

## Global Constraints

- **EE license header on every new file** — `server/ee/` uses the ByteChef Enterprise header, not Apache 2.0. Spotless keys off the header CONTENT (`@version ee` in the class javadoc), not the path. Copy the header verbatim from an existing neighbouring file.
- **`@version ee` Javadoc tag on every new class** under `server/ee/`.
- **`platform-ai-guardrails-api` must stay dependency-free.** Its `build.gradle.kts` declares only `testImplementation` entries. Nothing added in Task 1 may introduce a compile dependency — no Spring, no Micrometer, no commons.
- **No new third-party dependency anywhere.** No OpenNLP, ONNX, or Presidio (spec §4).
- **Blank line before control statements** (`if`, `for`, `while`, `try`, `switch`) except immediately after an opening `{`. Blank line after a variable modification that a following statement uses. No blank line before a class's closing `}`.
- **No abbreviated variable names.** `span`, `detector`, `candidate` — never `s`, `d`, `c`. Applies to lambda parameters and loop variables.
- **No `_` prefix on private methods.**
- **Test class naming:** unit tests end in `Test` (never `IntTest`), method names are camelCase with no underscores, and this rule covers private helper methods in test sources too.
- **Placeholder strings must not change.** `[REDACTED_EMAIL]`, `[REDACTED_SSN]`, `[REDACTED_CC]`, `[REDACTED_PHONE]`, `[REDACTED_IP]`, `[REDACTED_SECRET]`.
- **Run `./gradlew spotlessApply` before every commit.**
- **Never judge a Gradle run piped into `tail`/`grep`.** Redirect to a file, check `$?` on its own line, then grep the file for `^> Task .* FAILED`.

**Gradle project paths used throughout:**

- `:server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api`
- `:server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service`
- `:server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service`

**Package convention:** new types go in `com.bytechef.ee.platform.ai.guardrails.detector` in BOTH `-api` and `-service`. A package split across those two modules is the established convention here — `com.bytechef.ee.platform.ai.guardrails.service` already exists in both.

---

## File Structure

**Created — `platform-ai-guardrails-api`** (`src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/`)

| File | Responsibility |
|---|---|
| `SensitiveKind.java` | Closed two-value enum (`PII`, `SECRET`) mapping onto the two existing policy toggles |
| `SensitiveSpan.java` | Validated `[start, end)` range + kind + open category + confidence; derives its own placeholder |
| `SensitiveDataDetector.java` | The SPI: `name()`, `detect(String)`, `streamSafe()` |

**Created — `platform-ai-guardrails-service`** (`src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/`)

| File | Responsibility |
|---|---|
| `SensitiveDataRedactor.java` | Runs detectors (fail-open), resolves overlaps, applies replacements |
| `RegexPiiDetector.java` | The five PII patterns lifted out of `AiGuardrails` |
| `RegexSecretDetector.java` | The nine secret patterns lifted out of `AiGuardrails` |
| `SensitiveDataDetectors.java` | `builtIn()` factory, for the legacy constructor and tests |

**Modified**

| File | Change |
|---|---|
| `AiGuardrails.java` | Patterns removed; `redact*` become instance methods over `SensitiveDataRedactor`; gains a detector-taking constructor |
| `StreamingResponseRedactor.java` | Takes a `SensitiveDataRedactor`; uses only stream-safe detectors |
| `AiGatewayGuardrails.java` | Three dead `static` delegates deleted; three call sites become instance calls |
| `docs/agents/ai-guardrails.md` | Documents the SPI, the resolution rule, and the §3 fix |

---

## Task 1: SPI types in `platform-ai-guardrails-api`

**Files:**
- Create: `server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-api/src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/SensitiveKind.java`
- Create: `.../platform-ai-guardrails-api/src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/SensitiveSpan.java`
- Create: `.../platform-ai-guardrails-api/src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/SensitiveDataDetector.java`
- Test: `.../platform-ai-guardrails-api/src/test/java/com/bytechef/ee/platform/ai/guardrails/detector/SensitiveSpanTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `SensitiveKind.{PII, SECRET}`; `SensitiveSpan(SensitiveKind kind, String category, int start, int end, double confidence)` with `SensitiveSpan.of(kind, category, start, end)`, `placeholder()`, `length()`, `overlaps(SensitiveSpan)`; `SensitiveDataDetector` with `String name()`, `List<SensitiveSpan> detect(String text)`, `default boolean streamSafe()`.

- [ ] **Step 1: Write the failing test**

Create `SensitiveSpanTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class SensitiveSpanTest {

    @Test
    void testPlaceholderIsDerivedFromCategory() {
        assertThat(SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 5)
            .placeholder()).isEqualTo("[REDACTED_EMAIL]");
        assertThat(SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 5)
            .placeholder()).isEqualTo("[REDACTED_SECRET]");
        assertThat(SensitiveSpan.of(SensitiveKind.PII, "PERSON", 0, 5)
            .placeholder()).isEqualTo("[REDACTED_PERSON]");
    }

    @Test
    void testOfDefaultsConfidenceToOne() {
        assertThat(SensitiveSpan.of(SensitiveKind.PII, "IP", 3, 9)
            .confidence()).isEqualTo(1.0);
    }

    @Test
    void testLengthIsEndMinusStart() {
        assertThat(SensitiveSpan.of(SensitiveKind.PII, "IP", 3, 9)
            .length()).isEqualTo(6);
    }

    @Test
    void testOverlapsIsHalfOpenSoTouchingSpansDoNotOverlap() {
        SensitiveSpan first = SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 5);
        SensitiveSpan touching = SensitiveSpan.of(SensitiveKind.PII, "IP", 5, 9);
        SensitiveSpan crossing = SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 9);

        assertThat(first.overlaps(touching)).isFalse();
        assertThat(touching.overlaps(first)).isFalse();
        assertThat(first.overlaps(crossing)).isTrue();
        assertThat(crossing.overlaps(first)).isTrue();
    }

    @Test
    void testRejectsInvalidCategory() {
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "lower", 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "", 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "9LEADING", 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsInvalidRange() {
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "EMAIL", -1, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 5, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 6, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsInvalidConfidence() {
        assertThatThrownBy(() -> new SensitiveSpan(SensitiveKind.PII, "EMAIL", 0, 5, 1.5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SensitiveSpan(SensitiveKind.PII, "EMAIL", 0, 5, Double.NaN))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testToStringCarriesNoCoveredText() {
        String rendered = SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 2, 40)
            .toString();

        assertThat(rendered).contains("SECRET");
        assertThat(rendered).contains("2");
        assertThat(rendered).contains("40");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api:test --tests 'com.bytechef.ee.platform.ai.guardrails.detector.SensitiveSpanTest' > /tmp/t1.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|error:' /tmp/t1.log | head
```

Expected: FAIL — compilation error, `SensitiveSpan` does not exist.

- [ ] **Step 3: Write `SensitiveKind.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

/**
 * The two categories of sensitive data the guardrail policy layer can toggle independently. Deliberately closed: these
 * two values map one-to-one onto the {@code redactPii} / {@code redactSecrets} settings that already exist on both
 * {@code AiGuardrailsWorkspaceSettings} and the gateway's {@code AiGatewayProjectSettings}, and a third value would
 * leave those toggles unable to decide which spans they govern. The open axis is
 * {@link SensitiveSpan#category()} — a detector reporting a new entity type varies the category, never the kind.
 *
 * <p>
 * Not persisted anywhere, so ordinal stability is not a concern here.
 * </p>
 *
 * @version ee
 */
public enum SensitiveKind {

    PII,
    SECRET
}
```

- [ ] **Step 4: Write `SensitiveSpan.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One region of sensitive data located in a source string, reported by a {@link SensitiveDataDetector}. Offsets are
 * UTF-16 indices into the string the detector was given, {@code start} inclusive and {@code end} exclusive.
 *
 * <p>
 * Two axes, deliberately. {@link #kind()} is closed and drives policy (which toggle governs this span);
 * {@link #category()} is an open, validated uppercase identifier and drives presentation — {@link #placeholder()}
 * derives {@code [REDACTED_<category>]} from it with no lookup table, which reproduces every placeholder the engine
 * emitted before the SPI existed and lets a detector added later report a new entity type without editing this module.
 * </p>
 *
 * <p>
 * A span never carries the text it covers, and {@link #toString()} therefore cannot leak it into a log line.
 * </p>
 *
 * @param kind       which policy toggle governs this span
 * @param category   uppercase identifier naming the entity type, matching {@code [A-Z][A-Z0-9_]*}
 * @param start      inclusive start offset
 * @param end        exclusive end offset, strictly greater than {@code start}
 * @param confidence detector confidence between {@code 0.0} and {@code 1.0}; deterministic detectors report
 *                   {@code 1.0}. Carried but not used by overlap resolution today — see the design spec's section 6.2
 *                   for why adding a probabilistic detector should not be a record-signature change.
 *
 * @version ee
 */
public record SensitiveSpan(SensitiveKind kind, String category, int start, int end, double confidence) {

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    public SensitiveSpan {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(category, "category must not be null");

        if (!CATEGORY_PATTERN.matcher(category)
            .matches()) {

            throw new IllegalArgumentException("category must match [A-Z][A-Z0-9_]*, got: " + category);
        }

        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0");
        }

        if (end <= start) {
            throw new IllegalArgumentException("end must be > start");
        }

        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }

    /**
     * Creates a span with full confidence, for deterministic detectors.
     *
     * @param kind     which policy toggle governs this span
     * @param category uppercase entity-type identifier
     * @param start    inclusive start offset
     * @param end      exclusive end offset
     * @return the span
     */
    public static SensitiveSpan of(SensitiveKind kind, String category, int start, int end) {
        return new SensitiveSpan(kind, category, start, end, 1.0);
    }

    /**
     * Returns the replacement text for this span, derived from {@link #category()}.
     *
     * @return the placeholder, e.g. {@code [REDACTED_EMAIL]}
     */
    public String placeholder() {
        return "[REDACTED_" + category + "]";
    }

    /**
     * Returns the number of characters this span covers.
     *
     * @return {@code end - start}
     */
    public int length() {
        return end - start;
    }

    /**
     * Returns whether this span shares at least one character position with {@code other}. Ranges are half-open, so
     * spans that merely touch ({@code this.end == other.start}) do not overlap and can both be applied.
     *
     * @param other the span to test against
     * @return {@code true} when the two ranges intersect
     */
    public boolean overlaps(SensitiveSpan other) {
        return start < other.end && end > other.start;
    }

    @Override
    public String toString() {
        return "SensitiveSpan[" + kind + "/" + category + "[" + start + "," + end + ") confidence=" + confidence + "]";
    }
}
```

- [ ] **Step 5: Write `SensitiveDataDetector.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import java.util.List;

/**
 * Locates sensitive data in text. Implementations are contributed as Spring beans and collected into
 * {@code SensitiveDataRedactor}; contributing one requires no change to the guardrails engine.
 *
 * <p>
 * Every detector is handed the ORIGINAL text and never another detector's output, so a detector's result cannot be
 * corrupted by one that happens to run before it. Overlaps between detectors are resolved centrally by a total order
 * over the spans themselves, which is why registration order cannot affect the redacted result.
 * </p>
 *
 * <p>
 * Implementations must be thread-safe and side-effect-free: one instance serves every workspace and every concurrent
 * request. A detector that throws is caught, logged, counted, and skipped for that call — the other detectors still
 * run. See the design spec's section 8 for the residual risk that fail-open policy accepts.
 * </p>
 *
 * @version ee
 */
public interface SensitiveDataDetector {

    /**
     * Returns a short stable identifier used in log lines and diagnostics.
     *
     * @return the detector name
     */
    String name();

    /**
     * Returns every sensitive region found in {@code text}. Spans may overlap each other and may be returned in any
     * order; the caller resolves and orders them. Offsets must lie within {@code text}.
     *
     * @param text the text to scan; never {@code null} and never empty
     * @return the spans found, empty when none
     */
    List<SensitiveSpan> detect(String text);

    /**
     * Returns whether this detector can be applied to an arbitrary substring of a document and give the same answer it
     * would give for the whole.
     *
     * <p>
     * A regex detector is local in this sense and the default is therefore {@code true}. A detector needing wider
     * context — sentence-level named-entity recognition, say — must return {@code false}: the streaming redactor scans
     * a bounded lookahead window, and feeding such a detector a window that starts mid-sentence produces different and
     * worse answers than feeding it the complete text. The streaming path skips detectors that return {@code false},
     * so that a detector which cannot honestly cover a stream is visibly absent from it rather than silently
     * contributing nothing usable.
     * </p>
     *
     * @return {@code true} when this detector is safe to run over a windowed fragment
     */
    default boolean streamSafe() {
        return true;
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api:test --tests 'com.bytechef.ee.platform.ai.guardrails.detector.SensitiveSpanTest' > /tmp/t1.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t1.log || echo "no failed tasks"
```

Expected: exit=0, all 7 tests pass.

- [ ] **Step 7: Verify `-api` gained no compile dependency**

```bash
cat server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-api/build.gradle.kts
```

Expected: unchanged — only `testImplementation` entries. If you had to add anything to make Task 1 compile, stop: something in the new types reaches outside the JDK, which the spec forbids.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-api
git commit -m "Add the sensitive-data detector SPI types"
```

---

## Task 2: `SensitiveDataRedactor` — detect, resolve, apply

**Files:**
- Create: `server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/SensitiveDataRedactor.java`
- Test: `.../platform-ai-guardrails-service/src/test/java/com/bytechef/ee/platform/ai/guardrails/detector/SensitiveDataRedactorTest.java`

**Interfaces:**
- Consumes: `SensitiveKind`, `SensitiveSpan`, `SensitiveDataDetector` (Task 1); `AiGuardrailMetrics#record(String)` (existing, `com.bytechef.ee.platform.ai.guardrails`).
- Produces:
  - `SensitiveDataRedactor(List<SensitiveDataDetector> detectors)`
  - `String redact(String text, Set<SensitiveKind> kinds, AiGuardrailMetrics metrics)` — `text` and `metrics` nullable
  - `List<SensitiveSpan> detectCandidates(String text, AiGuardrailMetrics metrics)` — `metrics` nullable
  - `SensitiveDataRedactor streamSafeView()`
  - package-private statics `resolve(List<SensitiveSpan>)` and `apply(String, List<SensitiveSpan>)`
  - `static final String DETECTOR_FAILED_EVENT = "detector_failed"` (package-private)

**Note on one addition beyond the spec:** `detectCandidates` validates that a returned span lies within the text and treats a violation as that detector's failure. The spec does not mention this. Without it, a contributed detector returning a bad offset throws `StringIndexOutOfBoundsException` from deep inside `apply`, crashing the turn — which contradicts spec §8's fail-open intent. Validating inside the per-detector `try` makes a malformed span behave exactly like a thrown exception.

- [ ] **Step 1: Write the failing test**

Create `SensitiveDataRedactorTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class SensitiveDataRedactorTest {

    private static final Set<SensitiveKind> BOTH = EnumSet.allOf(SensitiveKind.class);

    @Test
    void testAppliesNonOverlappingSpansLeftToRightInResult() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true,
                SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3),
                SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("[REDACTED_EMAIL] [REDACTED_IP]");
    }

    @Test
    void testSecretBeatsOverlappingPii() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21)),
            fixed("secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28)));

        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    @Test
    void testLongerSpanBeatsNestedSpanOfTheSameKind() {
        SensitiveDataRedactor redactor = redactor(
            fixed("outer", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 10)),
            fixed("inner", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 3, 6)));

        assertThat(redactor.redact("0123456789", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    @Test
    void testTouchingSpansBothSurvive() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true,
                SensitiveSpan.of(SensitiveKind.PII, "IP", 0, 5),
                SensitiveSpan.of(SensitiveKind.PII, "IP", 5, 10)));

        assertThat(redactor.redact("0123456789", BOTH, null)).isEqualTo("[REDACTED_IP][REDACTED_IP]");
    }

    @Test
    void testResultIsIndependentOfDetectorOrder() {
        SensitiveDataDetector piiDetector = fixed(
            "pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21));
        SensitiveDataDetector secretDetector = fixed(
            "secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28));

        String text = "xoxb-1234567890123456-abcdef";

        List<SensitiveDataDetector> detectors = new ArrayList<>(List.of(piiDetector, secretDetector));
        String expected = new SensitiveDataRedactor(detectors).redact(text, BOTH, null);

        for (int attempt = 0; attempt < 20; attempt++) {
            Collections.shuffle(detectors);

            assertThat(new SensitiveDataRedactor(detectors).redact(text, BOTH, null)).isEqualTo(expected);
        }
    }

    @Test
    void testKindFilterIsAppliedBeforeResolutionSoAPiiOnlyCallStillRedacts() {
        // The SECRET span would win the overlap, but a PII-only caller never sees it -- filtering candidates before
        // resolution is what keeps single-toggle output identical to the pre-SPI engine. Filtering after resolution
        // would leave this text untouched, which is a redaction regression.
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21)),
            fixed("secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28)));

        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", EnumSet.of(SensitiveKind.PII), null))
            .isEqualTo("xoxb-[REDACTED_CC]-abcdef");
    }

    @Test
    void testFailingDetectorIsSkippedAndOthersStillApply() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_hub");

        SensitiveDataRedactor redactor = redactor(
            throwing("broken"),
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact("abc def", BOTH, metrics)).isEqualTo("[REDACTED_EMAIL] def");

        Counter counter = meterRegistry.find(AiGuardrailMetrics.COUNTER_NAME)
            .tag("event", "detector_failed")
            .tag("surface", "ai_hub")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testFailingDetectorWithoutMetricsDoesNotThrow() {
        SensitiveDataRedactor redactor = redactor(throwing("broken"));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("abc def");
    }

    @Test
    void testSpanBeyondTextIsTreatedAsDetectorFailure() {
        SensitiveDataRedactor redactor = redactor(
            fixed("rogue", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 500)),
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("abc [REDACTED_IP]");
    }

    @Test
    void testStreamSafeViewExcludesNonStreamSafeDetectors() {
        SensitiveDataRedactor redactor = redactor(
            fixed("local", true, SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)),
            fixed("contextual", false, SensitiveSpan.of(SensitiveKind.PII, "PERSON", 0, 3)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("[REDACTED_PERSON] [REDACTED_IP]");
        assertThat(redactor.streamSafeView()
            .redact("abc def", BOTH, null)).isEqualTo("abc [REDACTED_IP]");
    }

    @Test
    void testNullAndEmptyTextAreReturnedUnchanged() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact(null, BOTH, null)).isNull();
        assertThat(redactor.redact("", BOTH, null)).isEmpty();
    }

    @Test
    void testEmptyKindSetRedactsNothing() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact("abc def", EnumSet.noneOf(SensitiveKind.class), null)).isEqualTo("abc def");
    }

    private static SensitiveDataRedactor redactor(SensitiveDataDetector... detectors) {
        return new SensitiveDataRedactor(List.of(detectors));
    }

    private static SensitiveDataDetector fixed(String name, boolean streamSafe, SensitiveSpan... spans) {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                return List.of(spans);
            }

            @Override
            public boolean streamSafe() {
                return streamSafe;
            }
        };
    }

    private static SensitiveDataDetector throwing(String name) {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                throw new IllegalStateException("detector is broken");
            }
        };
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test --tests 'com.bytechef.ee.platform.ai.guardrails.detector.SensitiveDataRedactorTest' > /tmp/t2.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|error:' /tmp/t2.log | head
```

Expected: FAIL — `SensitiveDataRedactor` does not exist.

- [ ] **Step 3: Write `SensitiveDataRedactor.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs every registered {@link SensitiveDataDetector} over a piece of text, resolves the overlapping spans they report
 * into one non-overlapping accepted set, and applies the winners as {@code [REDACTED_*]} placeholders.
 *
 * <p>
 * This replaces the sequential {@code String.replaceAll} chain the guardrail engine used previously, in which each
 * pattern rewrote the text the next pattern was about to scan. That chain leaked part of any secret whose body
 * contained a credit-card-shaped digit run: the PII pass claimed the digits first and destroyed the text the secret
 * pattern needed, so {@code xoxb-1234567890123456-abcdef} was emitted as {@code xoxb-[REDACTED_CC]-abcdef} with the
 * token's prefix and suffix intact. Detecting against the original text and resolving centrally fixes that.
 * </p>
 *
 * <p>
 * <b>Resolution order</b> is total, so the outcome cannot depend on detector registration order: SECRET before PII,
 * then longer before shorter, then earlier before later, then category ascending. Candidates are taken greedily in
 * that order and a candidate overlapping an already-accepted span is dropped. The length tiebreak also reproduces, for
 * a reason that does not depend on list position, the one ordering property the old chain got right — an enclosing
 * match swallows a nested one.
 * </p>
 *
 * <p>
 * <b>Failure is open, per detector.</b> A detector that throws (or reports a span outside the text) is logged,
 * counted as {@code detector_failed}, and skipped for that call; the others still run. A model-backed detector's
 * transient failure must not take down every AI surface in the product. The residual risk is that content the failed
 * detector would have redacted proceeds unredacted, and where the caller passes no {@link AiGuardrailMetrics} the log
 * line is the only signal.
 * </p>
 *
 * <p>
 * Immutable and thread-safe, provided every registered detector is.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SensitiveDataRedactor {

    static final String DETECTOR_FAILED_EVENT = "detector_failed";

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataRedactor.class);

    private static final Comparator<SensitiveSpan> RESOLUTION_ORDER = Comparator
        .comparingInt((SensitiveSpan span) -> span.kind() == SensitiveKind.SECRET ? 0 : 1)
        .thenComparing(
            Comparator.comparingInt(SensitiveSpan::length)
                .reversed())
        .thenComparingInt(SensitiveSpan::start)
        .thenComparing(SensitiveSpan::category);

    private final List<SensitiveDataDetector> detectors;

    public SensitiveDataRedactor(List<SensitiveDataDetector> detectors) {
        this.detectors = List.copyOf(detectors);
    }

    /**
     * Returns a redactor over only the {@link SensitiveDataDetector#streamSafe()} subset of this one's detectors, for
     * the streaming path which can offer a detector no more than a bounded lookahead window.
     *
     * @return a redactor restricted to stream-safe detectors, or {@code this} when every detector already is
     */
    public SensitiveDataRedactor streamSafeView() {
        List<SensitiveDataDetector> streamSafeDetectors = new ArrayList<>(detectors.size());

        for (SensitiveDataDetector detector : detectors) {
            if (detector.streamSafe()) {
                streamSafeDetectors.add(detector);
            } else {
                log.info(
                    "Detector '{}' is not stream-safe and is excluded from streaming response redaction",
                    detector.name());
            }
        }

        if (streamSafeDetectors.size() == detectors.size()) {
            return this;
        }

        return new SensitiveDataRedactor(streamSafeDetectors);
    }

    /**
     * Returns every candidate span reported by every detector, unresolved and possibly overlapping. The streaming
     * redactor needs the unresolved set: a span that loses an overlap still occupies characters, and a cut landing
     * inside one must still be pulled back.
     *
     * @param text    the text to scan
     * @param metrics the metrics instance to count detector failures through, or {@code null}
     * @return the candidate spans, empty when none or when {@code text} is null/empty
     */
    public List<SensitiveSpan> detectCandidates(@Nullable String text, @Nullable AiGuardrailMetrics metrics) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<SensitiveSpan> candidates = new ArrayList<>();

        for (SensitiveDataDetector detector : detectors) {
            collectSpans(detector, text, candidates, metrics);
        }

        return candidates;
    }

    /**
     * Returns {@code text} with every accepted span whose kind is in {@code kinds} replaced by its placeholder.
     *
     * <p>
     * The kind filter is applied to the CANDIDATES, before resolution. Filtering afterwards would let a span the
     * caller did not ask for consume an overlap and then be discarded, so a PII-only call over a secret containing a
     * digit run would return the text unredacted.
     * </p>
     *
     * @param text    the text to redact
     * @param kinds   the kinds the caller's policy has enabled
     * @param metrics the metrics instance to count detector failures through, or {@code null}
     * @return the redacted text, or {@code text} unchanged when nothing applies
     */
    public @Nullable String redact(
        @Nullable String text, Set<SensitiveKind> kinds, @Nullable AiGuardrailMetrics metrics) {

        if (text == null || text.isEmpty() || kinds.isEmpty()) {
            return text;
        }

        List<SensitiveSpan> candidates = filterByKind(detectCandidates(text, metrics), kinds);

        if (candidates.isEmpty()) {
            return text;
        }

        return apply(text, resolve(candidates));
    }

    /**
     * Returns the subset of {@code candidates} whose kind is in {@code kinds}. Public because {@code AiGuardrails}
     * sits in the parent package and drives the three stages separately, so that it can count which kinds actually
     * won before applying them.
     *
     * @param candidates the unresolved candidate spans
     * @param kinds      the kinds to keep
     * @return the matching candidates, in their original order
     */
    public static List<SensitiveSpan> filterByKind(List<SensitiveSpan> candidates, Set<SensitiveKind> kinds) {
        List<SensitiveSpan> filtered = new ArrayList<>(candidates.size());

        for (SensitiveSpan candidate : candidates) {
            if (kinds.contains(candidate.kind())) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    /**
     * Reduces overlapping candidates to a non-overlapping accepted set using the total order documented on this class.
     *
     * @param candidates the candidate spans, possibly overlapping and in any order
     * @return the accepted spans, none of which overlap another
     */
    public static List<SensitiveSpan> resolve(List<SensitiveSpan> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<SensitiveSpan> ordered = new ArrayList<>(candidates);

        ordered.sort(RESOLUTION_ORDER);

        List<SensitiveSpan> accepted = new ArrayList<>();

        for (SensitiveSpan candidate : ordered) {
            if (!overlapsAny(candidate, accepted)) {
                accepted.add(candidate);
            }
        }

        return accepted;
    }

    /**
     * Replaces every accepted span with its placeholder, working right to left so that each replacement leaves the
     * offsets of the spans not yet applied valid.
     *
     * @param text     the original text the spans were located in
     * @param accepted non-overlapping spans, in any order
     * @return the redacted text
     */
    public static String apply(String text, List<SensitiveSpan> accepted) {
        if (accepted.isEmpty()) {
            return text;
        }

        List<SensitiveSpan> ordered = new ArrayList<>(accepted);

        ordered.sort(
            Comparator.comparingInt(SensitiveSpan::start)
                .reversed());

        StringBuilder builder = new StringBuilder(text);

        for (SensitiveSpan span : ordered) {
            builder.replace(span.start(), span.end(), span.placeholder());
        }

        return builder.toString();
    }

    private void collectSpans(
        SensitiveDataDetector detector, String text, List<SensitiveSpan> candidates,
        @Nullable AiGuardrailMetrics metrics) {

        try {
            List<SensitiveSpan> spans = detector.detect(text);

            if (spans == null) {
                return;
            }

            for (SensitiveSpan span : spans) {
                if (span.end() > text.length()) {
                    throw new IllegalStateException(
                        "detector reported a span ending at " + span.end() + ", past the end of a " + text.length() +
                            "-character input");
                }
            }

            candidates.addAll(spans);
        } catch (RuntimeException exception) {
            log.warn("Sensitive-data detector '{}' failed; continuing without its spans", detector.name(), exception);

            if (metrics != null) {
                metrics.record(DETECTOR_FAILED_EVENT);
            }
        }
    }

    private static boolean overlapsAny(SensitiveSpan candidate, List<SensitiveSpan> accepted) {
        for (SensitiveSpan span : accepted) {
            if (candidate.overlaps(span)) {
                return true;
            }
        }

        return false;
    }
}
```

**Note:** `collectSpans` adds spans to `candidates` only after validating all of them, so a detector whose third span is malformed contributes none rather than a partial set.

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test --tests 'com.bytechef.ee.platform.ai.guardrails.detector.SensitiveDataRedactorTest' > /tmp/t2.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t2.log || echo "no failed tasks"
```

Expected: exit=0, all 12 tests pass.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-service
git commit -m "Add the sensitive-data span resolver and redactor"
```

---

## Task 3: The two built-in regex detectors

**Files:**
- Create: `.../platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/RegexPiiDetector.java`
- Create: `.../platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/RegexSecretDetector.java`
- Create: `.../platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails/detector/SensitiveDataDetectors.java`
- Test: `.../platform-ai-guardrails-service/src/test/java/com/bytechef/ee/platform/ai/guardrails/detector/RegexDetectorsTest.java`

**Interfaces:**
- Consumes: everything from Tasks 1 and 2.
- Produces: `RegexPiiDetector()` and `RegexSecretDetector()` (no-arg, `@Component`); `SensitiveDataDetectors.builtIn()` returning `List<SensitiveDataDetector>`.

**Source of the patterns:** copy them **verbatim** from `AiGuardrails.java` (the `EMAIL_PATTERN`…`IPV4_PATTERN` fields and the `SECRET_PATTERNS` list). Do not retype them; a single character changed here is a silent guardrail regression. Task 4 deletes the originals.

- [ ] **Step 1: Write the failing test**

Create `RegexDetectorsTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class RegexDetectorsTest {

    private static final Set<SensitiveKind> BOTH = EnumSet.allOf(SensitiveKind.class);

    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

    @Test
    void testDetectsEveryPiiCategory() {
        String redacted = redactor.redact(
            "Email me at jane.doe@example.com or call 415-555-0132. SSN 123-45-6789, card 4111 1111 1111 1111, " +
                "host 192.168.1.20.",
            BOTH, null);

        assertThat(redacted).contains("[REDACTED_EMAIL]");
        assertThat(redacted).contains("[REDACTED_SSN]");
        assertThat(redacted).contains("[REDACTED_CC]");
        assertThat(redacted).contains("[REDACTED_PHONE]");
        assertThat(redacted).contains("[REDACTED_IP]");
        assertThat(redacted).doesNotContain("jane.doe@example.com");
        assertThat(redacted).doesNotContain("123-45-6789");
    }

    @Test
    void testDetectsKnownSecretShapes() {
        String redacted = redactor.redact(
            "aws AKIAIOSFODNN7EXAMPLE gh ghp_1234567890abcdefghij1234567890abcdef openai " +
                "sk-abcdefghij1234567890ABCD jwt eyJhbGciOiJIUzI.eyJzdWIiOiIxMjM0.SflKxwRJSMeKKF2QT4 done",
            BOTH, null);

        assertThat(redacted).contains("[REDACTED_SECRET]");
        assertThat(redacted).doesNotContain("AKIAIOSFODNN7EXAMPLE");
        assertThat(redacted).doesNotContain("ghp_1234567890abcdefghij1234567890abcdef");
        assertThat(redacted).doesNotContain("sk-abcdefghij1234567890ABCD");
        assertThat(redacted).doesNotContain("eyJhbGciOiJIUzI");
    }

    @Test
    void testRedactsPemPrivateKeyBlockWhole() {
        String redacted = redactor.redact(
            "key:\n-----BEGIN RSA PRIVATE KEY-----\nMIIBOgIBAAJBAKj34Gkx...\n-----END RSA PRIVATE KEY-----\ntail",
            BOTH, null);

        assertThat(redacted).contains("[REDACTED_SECRET]");
        assertThat(redacted).doesNotContain("BEGIN RSA PRIVATE KEY");
        assertThat(redacted).contains("tail");
    }

    @Test
    void testLeavesCleanTextUnchanged() {
        String content = "Summarize the quarterly revenue report. The deployment succeeded.";

        assertThat(redactor.redact(content, BOTH, null)).isEqualTo(content);
    }

    /**
     * The bug this SPI was built to fix. The old sequential chain ran CREDIT_CARD before the secret patterns, so it
     * rewrote the digits the secret pattern needed and emitted "sk-proj-[REDACTED_CC]" -- disclosing that an OpenAI
     * key was present and leaking its prefix. Resolving spans against the original text redacts the whole secret.
     */
    @Test
    void testSecretContainingDigitRunIsRedactedWholeNotPartially() {
        assertThat(redactor.redact("sk-proj-1234567890123456", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    /**
     * Control cases: inputs with no PII/secret overlap must be byte-identical to what the old chain produced.
     */
    @Test
    void testNonOverlappingInputsMatchThePreSpiOutput() {
        assertThat(redactor.redact("card 4111 1111 1111 1111 ok", BOTH, null)).isEqualTo("card [REDACTED_CC] ok");
        assertThat(redactor.redact("mail me at bob@example.com please", BOTH, null))
            .isEqualTo("mail me at [REDACTED_EMAIL] please");
        assertThat(redactor.redact("contact sk-proj-abcdefghijklmnopqrstuvwx now", BOTH, null))
            .isEqualTo("contact [REDACTED_SECRET] now");
        assertThat(redactor.redact("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc123", BOTH, null))
            .isEqualTo("[REDACTED_SECRET]");
        assertThat(redactor.redact("bob@example.com called from 10.0.0.5 with 555-123-4567", BOTH, null))
            .isEqualTo("[REDACTED_EMAIL] called from [REDACTED_IP] with [REDACTED_PHONE]");
    }

    @Test
    void testBothDetectorsAreStreamSafe() {
        List<SensitiveDataDetector> detectors = SensitiveDataDetectors.builtIn();

        assertThat(detectors).hasSize(2);
        assertThat(detectors).allMatch(SensitiveDataDetector::streamSafe);
    }

    @Test
    void testKindsAreAssignedCorrectly() {
        assertThat(new RegexPiiDetector().detect("bob@example.com"))
            .allMatch(span -> span.kind() == SensitiveKind.PII);
        assertThat(new RegexSecretDetector().detect("AKIAIOSFODNN7EXAMPLE"))
            .allMatch(span -> span.kind() == SensitiveKind.SECRET);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test --tests 'com.bytechef.ee.platform.ai.guardrails.detector.RegexDetectorsTest' > /tmp/t3.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|error:' /tmp/t3.log | head
```

Expected: FAIL — `RegexPiiDetector` does not exist.

- [ ] **Step 3: Write `RegexPiiDetector.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Locates structured personally-identifiable data by regular expression: email addresses, US social-security numbers,
 * credit-card numbers, phone numbers and IPv4 addresses.
 *
 * <p>
 * Every pattern is linear and backtracking-safe — no nested optional quantifiers, so none can be driven into
 * catastrophic backtracking by hostile input. These are the exact patterns the guardrail engine applied before the
 * detector SPI existed; the category names are chosen so that {@link SensitiveSpan#placeholder()} derives the same
 * placeholder strings the engine emitted then.
 * </p>
 *
 * <p>
 * Regex matching is local, so this detector is stream-safe.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RegexPiiDetector implements SensitiveDataDetector {

    private static final Map<String, Pattern> PATTERNS = Map.of(
        "EMAIL", Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
        "SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
        "CC", Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b"),
        // Linear, backtracking-safe: a 3-3-4 grouping with a single required separator between groups.
        "PHONE", Pattern.compile("\\b\\d{3}[-.\\s]\\d{3}[-.\\s]\\d{4}\\b"),
        "IP", Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\b"));

    @Override
    public String name() {
        return "regex-pii";
    }

    @Override
    public List<SensitiveSpan> detect(String text) {
        List<SensitiveSpan> spans = new ArrayList<>();

        for (Map.Entry<String, Pattern> entry : PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue()
                .matcher(text);

            while (matcher.find()) {
                spans.add(SensitiveSpan.of(SensitiveKind.PII, entry.getKey(), matcher.start(), matcher.end()));
            }
        }

        return spans;
    }
}
```

**Note:** `Map` iteration order is unspecified, and that is fine — resolution imposes a total order on the spans, so the order they are produced in cannot affect the result. Task 2's `testResultIsIndependentOfDetectorOrder` is the guard for this property.

- [ ] **Step 4: Write `RegexSecretDetector.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Locates high-signal developer-secret shapes: PEM private-key blocks, cloud and provider API keys, and JSON web
 * tokens. Every span is reported under the single category {@code SECRET}, so they all redact to
 * {@code [REDACTED_SECRET]} — naming the provider in the placeholder would itself disclose which service a leaked
 * credential belonged to.
 *
 * <p>
 * Each pattern is anchored, fixed-length, or bounded by a single quantifier or literal terminator, so none can be
 * driven into catastrophic backtracking. Entropy and random-string detection deliberately live elsewhere (the
 * workflow layer's {@code SecretKeyDetectorUtils}) for callers who want them.
 * </p>
 *
 * <p>
 * Regex matching is local, so this detector is stream-safe.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RegexSecretDetector implements SensitiveDataDetector {

    private static final String CATEGORY = "SECRET";

    private static final List<Pattern> PATTERNS = List.of(
        // PEM private-key block (redact the whole block, not just the marker)
        Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
        // AWS access key id
        Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
        // GitHub personal/OAuth/app tokens (classic) and fine-grained PATs
        Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{36}\\b"),
        Pattern.compile("\\bgithub_pat_[A-Za-z0-9_]{22,}\\b"),
        // OpenAI API keys (incl. project-scoped)
        Pattern.compile("\\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\\b"),
        // Slack tokens
        Pattern.compile("\\bxox[baprs]-[A-Za-z0-9-]{10,}\\b"),
        // Stripe secret / restricted live keys
        Pattern.compile("\\b[sr]k_live_[0-9a-zA-Z]{24}\\b"),
        // Google API keys
        Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b"),
        // JSON Web Tokens (three base64url segments)
        Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"));

    @Override
    public String name() {
        return "regex-secret";
    }

    @Override
    public List<SensitiveSpan> detect(String text) {
        List<SensitiveSpan> spans = new ArrayList<>();

        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                spans.add(SensitiveSpan.of(SensitiveKind.SECRET, CATEGORY, matcher.start(), matcher.end()));
            }
        }

        return spans;
    }
}
```

- [ ] **Step 5: Write `SensitiveDataDetectors.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import java.util.List;

/**
 * Constructs the detectors the guardrail engine ships with, for callers that assemble a
 * {@link SensitiveDataRedactor} outside a Spring context — {@code AiGuardrails}' legacy constructor and unit tests.
 * In a running application the same two detectors are contributed as beans and injected instead; both detectors are
 * stateless, so the two paths are interchangeable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SensitiveDataDetectors {

    private SensitiveDataDetectors() {
    }

    /**
     * Returns the built-in regex detectors.
     *
     * @return the PII and secret detectors
     */
    public static List<SensitiveDataDetector> builtIn() {
        return List.of(new RegexPiiDetector(), new RegexSecretDetector());
    }
}
```

- [ ] **Step 6: Verify the patterns were copied, not retyped**

```bash
G=server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails
diff <(grep -oE 'Pattern\.compile\(".*"\)' "$G/AiGuardrails.java" | sort) \
     <(grep -hoE 'Pattern\.compile\(".*"\)' "$G"/detector/Regex*Detector.java | sort) \
  && echo "PATTERNS IDENTICAL"
```

Expected: `PATTERNS IDENTICAL`. If the diff shows anything, a pattern was altered in transit — fix it before continuing. (Multi-line `Pattern.compile` calls will not be captured by this grep; check those two by eye against the originals.)

- [ ] **Step 7: Run test to verify it passes**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test --tests 'com.bytechef.ee.platform.ai.guardrails.detector.*' > /tmp/t3.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t3.log || echo "no failed tasks"
```

Expected: exit=0. `testSecretContainingDigitRunIsRedactedWholeNotPartially` passing is the proof that the §3 bug is fixed.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-service
git commit -m "Add the built-in regex PII and secret detectors"
```

---

## Task 4: Wire `AiGuardrails` onto the redactor

**Files:**
- Modify: `.../platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails/AiGuardrails.java`
- Modify: `.../platform-ai-guardrails-service/src/test/java/com/bytechef/ee/platform/ai/guardrails/AiGuardrailsTest.java`

**Interfaces:**
- Consumes: `SensitiveDataRedactor`, `SensitiveDataDetectors.builtIn()`, `SensitiveKind` (Tasks 1–3).
- Produces:
  - `AiGuardrails#redactPii(String)`, `#redactSecrets(String)`, `#redactAll(String)` — now **instance** methods, same names and return types
  - `AiGuardrails#newStreamingResponseRedactor()` — new public no-arg overload, unconditional, used by Task 6
  - **Removed:** the six pattern fields, `buildAllSensitivePatterns()`, `sensitiveMatchRanges()`, and `SECRET_PLACEHOLDER`

**Why two constructors:** adding a required parameter to the existing constructor would break `new AiGuardrails(...)` in five test files across three other modules (`ai-hub-service` ×4, `automation-ai-gateway-service` ×1). Instead keep the current 11-argument signature, delegating to a new 12-argument one that takes the detector list, and put `@Autowired` on the new one so Spring is not left choosing between two candidates. `AiGuardrailMetrics` in this same module already uses exactly this pattern and documents why.

- [ ] **Step 1: Edit `AiGuardrails.java` — delete the pattern fields**

Delete these fields entirely: `EMAIL_PATTERN`, `SSN_PATTERN`, `CREDIT_CARD_PATTERN`, `PHONE_PATTERN`, `IPV4_PATTERN`, `SECRET_PATTERNS`, `ALL_SENSITIVE_PATTERNS`, and the private method `buildAllSensitivePatterns()`. Keep `SECRET_PLACEHOLDER`? No — delete it too; it is only used by the deleted `redactSecrets` body. Keep `BLOCKED_TERM_PLACEHOLDER` and `MODERATION_PLACEHOLDER`.

Remove the now-unused imports: `java.util.regex.Matcher`, `java.util.regex.Pattern`.

- [ ] **Step 2: Edit `AiGuardrails.java` — add the field and constructors**

Add the field beside the existing ones:

```java
    private final SensitiveDataRedactor sensitiveDataRedactor;
```

Replace the existing constructor with this pair (keep every existing `@Value` annotation and parameter exactly as-is):

```java
    /**
     * Legacy constructor retained so that callers assembling this engine by hand — unit tests in this and other
     * modules — keep compiling. Uses the built-in regex detectors, which is what those callers had before the
     * detector SPI existed. Spring uses the {@code @Autowired} constructor below instead, so contributed detector
     * beans participate.
     */
    public AiGuardrails(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGatewayInjectionClassifier injectionClassifier,
        @Nullable AiGatewayModerationClassifier moderationClassifier,
        @Nullable AiGuardrailMetrics metrics,
        @Value("${bytechef.ai.gateway.guardrails.pii-redaction-enabled:false}") boolean piiRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.secret-redaction-enabled:false}") boolean secretRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.blocked-terms:}") String blockedTerms,
        @Value("${bytechef.ai.gateway.guardrails.injection-detection-enabled:false}") boolean injectionDetectionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.moderation-enabled:false}") boolean moderationEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-enabled:false}") boolean responseScanEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-streaming-enabled:false}") boolean streamingResponseScanEnabled) {

        this(
            aiGuardrailsWorkspaceSettingsService, injectionClassifier, moderationClassifier, metrics,
            SensitiveDataDetectors.builtIn(), piiRedactionEnabled, secretRedactionEnabled, blockedTerms,
            injectionDetectionEnabled, moderationEnabled, responseScanEnabled, streamingResponseScanEnabled);
    }

    // Two constructors are declared, so Spring cannot pick an autowire candidate implicitly. @Autowired marks this one
    // as the container's entry point, so contributed SensitiveDataDetector beans reach the engine.
    @Autowired
    public AiGuardrails(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        @Nullable AiGatewayInjectionClassifier injectionClassifier,
        @Nullable AiGatewayModerationClassifier moderationClassifier,
        @Nullable AiGuardrailMetrics metrics,
        List<SensitiveDataDetector> sensitiveDataDetectors,
        @Value("${bytechef.ai.gateway.guardrails.pii-redaction-enabled:false}") boolean piiRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.secret-redaction-enabled:false}") boolean secretRedactionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.blocked-terms:}") String blockedTerms,
        @Value("${bytechef.ai.gateway.guardrails.injection-detection-enabled:false}") boolean injectionDetectionEnabled,
        @Value("${bytechef.ai.gateway.guardrails.moderation-enabled:false}") boolean moderationEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-enabled:false}") boolean responseScanEnabled,
        @Value("${bytechef.ai.gateway.guardrails.response-scan-streaming-enabled:false}") boolean streamingResponseScanEnabled) {

        this.aiGuardrailsWorkspaceSettingsService = aiGuardrailsWorkspaceSettingsService;
        this.globalBlockedTerms = parseBlockedTerms(blockedTerms);
        this.globalInjectionDetectionEnabled = injectionDetectionEnabled;
        this.globalModerationEnabled = moderationEnabled;
        this.globalPiiRedactionEnabled = piiRedactionEnabled;
        this.globalResponseScanEnabled = responseScanEnabled;
        this.globalSecretRedactionEnabled = secretRedactionEnabled;
        this.globalStreamingResponseScanEnabled = streamingResponseScanEnabled;
        this.injectionClassifier = injectionClassifier;
        this.moderationClassifier = moderationClassifier;
        this.metrics = metrics;
        this.sensitiveDataRedactor = new SensitiveDataRedactor(sensitiveDataDetectors);
    }
```

Add imports: `org.springframework.beans.factory.annotation.Autowired`, `java.util.EnumSet`, `java.util.Set` (already present), plus the four detector-package types.

- [ ] **Step 3: Edit `AiGuardrails.java` — replace the three redaction methods**

Replace `redactPii`, `redactSecrets`, `redactAll` and `sensitiveMatchRanges` with:

```java
    /**
     * Replaces personally-identifiable data in {@code content} with {@code [REDACTED_*]} placeholders.
     */
    public @Nullable String redactPii(@Nullable String content) {
        return sensitiveDataRedactor.redact(content, EnumSet.of(SensitiveKind.PII), metrics);
    }

    /**
     * Replaces recognised developer-secret shapes (cloud/provider API keys, tokens, JWTs, PEM private keys) in
     * {@code content} with a {@code [REDACTED_SECRET]} placeholder.
     */
    public @Nullable String redactSecrets(@Nullable String content) {
        return sensitiveDataRedactor.redact(content, EnumSet.of(SensitiveKind.SECRET), metrics);
    }

    /**
     * Applies both PII and secret redaction to {@code content} in ONE detection pass, resolving any overlap between
     * the two in favour of the secret. Used for response-direction scanning where both categories are masked
     * regardless of the request-direction toggles.
     */
    public @Nullable String redactAll(@Nullable String content) {
        return sensitiveDataRedactor.redact(content, EnumSet.allOf(SensitiveKind.class), metrics);
    }

```

**Delete `sensitiveMatchRanges` outright — do not replace it with an `AiGuardrails`-level equivalent.** Its only caller was `StreamingResponseRedactor`, and Task 5 gives that class its own `SensitiveDataRedactor` through its constructor, so it asks the redactor for candidates directly. An `AiGuardrails#detectCandidates` forwarder, or an accessor exposing the redactor, would have no caller the moment it was written. `AiGuardrails`' own streaming factory (Step 5) reaches the field directly, being in the same class.

- [ ] **Step 4: Edit `AiGuardrails.java` — single-pass `redactPiiAndSecrets`**

Replace the body of `redactPiiAndSecrets` with:

```java
    private String redactPiiAndSecrets(
        String content, EffectivePolicy policy, @Nullable AiGuardrailMetrics recordingMetrics) {

        Set<SensitiveKind> kinds = EnumSet.noneOf(SensitiveKind.class);

        if (policy.redactPii()) {
            kinds.add(SensitiveKind.PII);
        }

        if (policy.redactSecrets()) {
            kinds.add(SensitiveKind.SECRET);
        }

        if (kinds.isEmpty()) {
            return content;
        }

        List<SensitiveSpan> candidates = SensitiveDataRedactor.filterByKind(
            sensitiveDataRedactor.detectCandidates(content, recordingMetrics), kinds);

        if (candidates.isEmpty()) {
            return content;
        }

        List<SensitiveSpan> accepted = SensitiveDataRedactor.resolve(candidates);

        // Recorded from the accepted spans rather than by comparing strings, so the counters describe what was
        // actually redacted. Under the old chain an overlap could record pii_redacted for a match that the secret
        // pattern would have covered better; now exactly the winning kind is counted.
        if (containsKind(accepted, SensitiveKind.PII)) {
            record(recordingMetrics, "pii_redacted");
        }

        if (containsKind(accepted, SensitiveKind.SECRET)) {
            record(recordingMetrics, "secret_redacted");
        }

        return SensitiveDataRedactor.apply(content, accepted);
    }

    private static boolean containsKind(List<SensitiveSpan> spans, SensitiveKind kind) {
        for (SensitiveSpan span : spans) {
            if (span.kind() == kind) {
                return true;
            }
        }

        return false;
    }
```

`SensitiveDataRedactor.filterByKind` / `.resolve` / `.apply` were already declared `public` in Task 2 for exactly this caller — `AiGuardrails` sits in the parent package and needs the three stages separately so it can count which kinds actually won before applying them. No signature change is needed here.

- [ ] **Step 5: Edit `AiGuardrails.java` — the streaming factory**

Replace `newStreamingResponseRedactor(Long)`'s final `return new StreamingResponseRedactor();` with `return newStreamingResponseRedactor();`, and add:

```java
    /**
     * Returns a fresh streaming redactor over this engine's stream-safe detectors, with no policy check. For callers
     * that have already decided streaming scanning applies — the AI Gateway's project-level overlay.
     *
     * @return a fresh streaming redactor
     */
    public StreamingResponseRedactor newStreamingResponseRedactor() {
        return new StreamingResponseRedactor(sensitiveDataRedactor.streamSafeView());
    }
```

- [ ] **Step 6: Update `AiGuardrailsTest.java` static calls to instance calls**

The four redaction tests at lines ~42–90 call `AiGuardrails.redactPii(...)` / `.redactSecrets(...)` statically. Change each to use an instance. Add a field beside the existing ones:

```java
    private final AiGuardrails redactionGuardrails = guardrails(null, false, false, "", false, false);
```

then replace `AiGuardrails.redactPii(` with `redactionGuardrails.redactPii(` and `AiGuardrails.redactSecrets(` with `redactionGuardrails.redactSecrets(` in those four tests. **Do not change a single assertion** — the spec's claim is that these expectations still hold, and that is what this step verifies.

- [ ] **Step 7: Run the full guardrails module test suite**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test --continue > /tmp/t4.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t4.log || echo "no failed tasks"
```

Expected: exit=0 with every pre-existing test passing unmodified except for the four call-site edits in Step 6.

**If a metrics assertion fails**, read it carefully before changing it: per Step 4 the `pii_redacted` / `secret_redacted` counters now describe the accepted spans rather than "did the string change". On non-overlapping fixtures the two agree, so a failure here means a fixture does overlap — report it rather than adjusting the expectation silently.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-service
git commit -m "Redact through the detector SPI instead of a replaceAll chain"
```

---

## Task 5: `StreamingResponseRedactor` over stream-safe detectors

**Files:**
- Modify: `.../platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails/StreamingResponseRedactor.java`
- Modify: `.../platform-ai-guardrails-service/src/test/java/com/bytechef/ee/platform/ai/guardrails/StreamingResponseRedactorTest.java`

**Interfaces:**
- Consumes: `SensitiveDataRedactor` (Task 2), `AiGuardrails#newStreamingResponseRedactor()` (Task 4).
- Produces: `StreamingResponseRedactor(SensitiveDataRedactor)` public, `StreamingResponseRedactor(SensitiveDataRedactor, int window)` package-private. **The no-arg constructor is removed.**

- [ ] **Step 1: Write the failing test**

Append to `StreamingResponseRedactorTest.java` (keep every existing test; update their construction calls in Step 3):

```java
    @Test
    void testStreamedOutputEqualsWholeTextRedaction() {
        SensitiveDataRedactor sensitiveDataRedactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

        List<String> corpus = List.of(
            "contact bob@example.com about AKIAIOSFODNN7EXAMPLE today",
            "token sk-proj-abcdefghijklmnopqrstuvwx and card 4111 1111 1111 1111",
            "no sensitive content whatsoever in this sentence",
            "xoxb-1234567890123456-abcdef trailing text",
            "ip 10.0.0.5 phone 555-123-4567 ssn 123-45-6789");

        for (String text : corpus) {
            String expected = sensitiveDataRedactor.redact(text, EnumSet.allOf(SensitiveKind.class), null);

            for (int window : new int[] {
                8, 16, 64, 512
            }) {
                for (int chunkSize : new int[] {
                    1, 3, 7, 100
                }) {
                    StreamingResponseRedactor redactor =
                        new StreamingResponseRedactor(sensitiveDataRedactor, window);
                    StringBuilder emitted = new StringBuilder();

                    for (int index = 0; index < text.length(); index += chunkSize) {
                        emitted.append(
                            redactor.push(text.substring(index, Math.min(index + chunkSize, text.length()))));
                    }

                    emitted.append(redactor.flush());

                    assertThat(emitted.toString())
                        .as("text=%s window=%d chunkSize=%d", text, window, chunkSize)
                        .isEqualTo(expected);
                }
            }
        }
    }

    @Test
    void testNonStreamSafeDetectorDoesNotContributeToTheStream() {
        SensitiveDataDetector contextual = new SensitiveDataDetector() {

            @Override
            public String name() {
                return "contextual";
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                int index = text.indexOf("Ada");

                if (index < 0) {
                    return List.of();
                }

                return List.of(SensitiveSpan.of(SensitiveKind.PII, "PERSON", index, index + 3));
            }

            @Override
            public boolean streamSafe() {
                return false;
            }
        };

        SensitiveDataRedactor full = new SensitiveDataRedactor(List.of(contextual));

        assertThat(full.redact("Ada wrote it", EnumSet.allOf(SensitiveKind.class), null))
            .isEqualTo("[REDACTED_PERSON] wrote it");

        StreamingResponseRedactor redactor = new StreamingResponseRedactor(full.streamSafeView(), 4);

        String emitted = redactor.push("Ada wrote it") + redactor.flush();

        assertThat(emitted).isEqualTo("Ada wrote it");
    }
```

Add the imports the new tests need: `com.bytechef.ee.platform.ai.guardrails.detector.SensitiveDataDetector`, `...SensitiveDataDetectors`, `...SensitiveDataRedactor`, `...SensitiveKind`, `...SensitiveSpan`, `java.util.EnumSet`, `java.util.List`.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test --tests 'com.bytechef.ee.platform.ai.guardrails.StreamingResponseRedactorTest' > /tmp/t5.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|error:' /tmp/t5.log | head
```

Expected: FAIL — no constructor taking a `SensitiveDataRedactor`.

- [ ] **Step 3: Edit `StreamingResponseRedactor.java`**

Replace the constructors and the two redaction call sites:

```java
    private final StringBuilder carry = new StringBuilder();
    private final SensitiveDataRedactor sensitiveDataRedactor;
    private final int window;

    private boolean redacted;

    /**
     * @param sensitiveDataRedactor a redactor restricted to stream-safe detectors — see
     *                              {@link SensitiveDataRedactor#streamSafeView()}. A detector needing wider context
     *                              than the lookahead window would give different answers here than it gives over the
     *                              whole document, so it is excluded rather than silently mis-scanned.
     */
    public StreamingResponseRedactor(SensitiveDataRedactor sensitiveDataRedactor) {
        this(sensitiveDataRedactor, DEFAULT_WINDOW);
    }

    StreamingResponseRedactor(SensitiveDataRedactor sensitiveDataRedactor, int window) {
        this.sensitiveDataRedactor = sensitiveDataRedactor;
        this.window = window;
    }
```

In `push`, replace the ranges lookup and the redaction:

```java
        List<SensitiveSpan> candidates = sensitiveDataRedactor.detectCandidates(carry.toString(), null);

        // Pull the cut back to the start of any matched span it lands inside, to a fixpoint (an earlier span may
        // in turn straddle the pulled-back cut). This must consider CANDIDATES, not the resolved winners: a span
        // that loses an overlap still occupies characters, and a cut inside it would still split a value.
        boolean pulled = true;

        while (pulled) {
            pulled = false;

            for (SensitiveSpan candidate : candidates) {
                if (candidate.start() < safeCut && candidate.end() > safeCut) {
                    safeCut = candidate.start();
                    pulled = true;
                }
            }
        }
```

and

```java
        String emitted = sensitiveDataRedactor.redact(rawSegment, EnumSet.allOf(SensitiveKind.class), null);
```

In `flush`:

```java
        String remainder = sensitiveDataRedactor.redact(raw, EnumSet.allOf(SensitiveKind.class), null);
```

Update the class Javadoc's invariant sentence to name the new method: replace `{@code redactAll(fullStream)}` with `{@code redact(fullStream, both kinds)}`, and add a sentence noting that the guarantee covers only stream-safe detectors.

Imports to add: `com.bytechef.ee.platform.ai.guardrails.detector.SensitiveDataRedactor`, `...SensitiveKind`, `...SensitiveSpan`, `java.util.EnumSet`. Remove the now-unused `java.util.List` only if nothing else uses it (it is still used — keep it).

- [ ] **Step 4: Update the existing tests' construction calls**

Every `new StreamingResponseRedactor()` / `new StreamingResponseRedactor(window)` in the existing test file becomes `new StreamingResponseRedactor(builtInRedactor)` / `new StreamingResponseRedactor(builtInRedactor, window)`, with a field:

```java
    private final SensitiveDataRedactor builtInRedactor =
        new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());
```

Change no assertions.

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test --continue > /tmp/t5.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t5.log || echo "no failed tasks"
```

Expected: exit=0. The equivalence test running 80 window × chunk-size combinations per corpus entry is the real proof that the streaming invariant survived.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-service
git commit -m "Stream-redact through stream-safe detectors only"
```

---

## Task 6: `AiGatewayGuardrails` — drop the dead statics

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/guardrail/AiGatewayGuardrails.java`
- Modify: `.../automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/guardrail/AiGatewayGuardrailsTest.java`

**Interfaces:**
- Consumes: `AiGuardrails#redactPii/redactSecrets/redactAll` (now instance, Task 4), `AiGuardrails#newStreamingResponseRedactor()` (Task 4).
- Produces: nothing new. Three `public static` methods are removed.

- [ ] **Step 1: Confirm the statics are still unreferenced**

```bash
grep -rn 'AiGatewayGuardrails\.redact' --include='*.java' server
```

Expected: hits only in `AiGatewayGuardrailsTest.java`. If a production caller has appeared since the spec was written, stop and report — deleting them is only safe because there is none.

- [ ] **Step 2: Delete the three static delegates**

Remove `public static String redactPii(...)`, `public static String redactSecrets(...)` and `public static String redactAll(...)` (currently lines ~359–379), together with their Javadoc.

- [ ] **Step 3: Convert the three remaining static call sites to instance calls**

- In the response-scan method (~line 285): `scanned = AiGuardrails.redactAll(scanned);` → `scanned = aiGuardrails.redactAll(scanned);`
- In `applyProjectOverlay` (~line 405): `String redacted = AiGuardrails.redactPii(result);` → `String redacted = aiGuardrails.redactPii(result);`
- In `applyProjectOverlay` (~line 415): `String redacted = AiGuardrails.redactSecrets(result);` → `String redacted = aiGuardrails.redactSecrets(result);`

Remove the now-unused `import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;` **only if** the type is no longer referenced — it still is, as a field type. Keep it.

- [ ] **Step 4: Convert the streaming construction site (~line 352)**

`return new StreamingResponseRedactor();` → `return aiGuardrails.newStreamingResponseRedactor();`

Remove `import com.bytechef.ee.platform.ai.guardrails.StreamingResponseRedactor;` only if the type no longer appears — it does, as the method's return type. Keep it.

- [ ] **Step 5: Point the test's static calls at an instance**

In `AiGatewayGuardrailsTest`, the four assertions at lines ~47–94 call `AiGatewayGuardrails.redactPii/.redactSecrets`. The file already builds an `AiGuardrails` at line ~473. Route those four through an instance:

```java
    private final AiGuardrails redactionGuardrails = new AiGuardrails(
        mock(AiGuardrailsWorkspaceSettingsService.class), null, null, null, false, false, "", false, false, false,
        false);
```

then replace `AiGatewayGuardrails.redactPii(` with `redactionGuardrails.redactPii(` and `AiGatewayGuardrails.redactSecrets(` with `redactionGuardrails.redactSecrets(`. **Change no assertions.**

- [ ] **Step 6: Run the gateway module test suite**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --continue > /tmp/t6.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t6.log || echo "no failed tasks"
```

Expected: exit=0.

- [ ] **Step 7: Compile everything that could reference the removed statics**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t6c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t6c.log || echo "no failed tasks"
```

Expected: exit=0. This is the check that no other module referenced the deleted methods or the removed `StreamingResponseRedactor()` no-arg constructor.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service
git commit -m "Call guardrail redaction through the engine instance"
```

---

## Task 7: Documentation

**Files:**
- Modify: `docs/agents/ai-guardrails.md`

**Interfaces:**
- Consumes: everything above.
- Produces: nothing in code.

- [ ] **Step 1: Read the current document**

```bash
sed -n '1,80p' docs/agents/ai-guardrails.md
grep -n 'redactPii\|redactSecrets\|redactAll\|regex\|pattern' docs/agents/ai-guardrails.md
```

Any sentence describing redaction as a sequence of pattern replacements is now wrong and must change.

- [ ] **Step 2: Add a "Sensitive-data detectors" section**

Cover, in the document's existing voice:

- Redaction is **detect → resolve → apply** over the original text, not a chain of replacements.
- The SPI is `SensitiveDataDetector` in `platform-ai-guardrails-api`; contributing a detector is a bean, no engine change.
- Resolution order: SECRET before PII, then longer, then earlier, then category. **Registration order cannot affect the result** — say this explicitly, because it is the property that makes contributing a detector safe.
- `SensitiveSpan.category` is open and its placeholder is `[REDACTED_<CATEGORY>]`; `SensitiveKind` is closed because it drives the two policy toggles.
- The kind filter runs on candidates, before resolution, and why.
- `streamSafe()` — a non-local detector is excluded from the streaming path rather than mis-scanned there.
- Detector failure is fail-open, counted as `detector_failed`, and **not counted at all on paths carrying no metrics instance** (spec §8.1).
- The fixed bug: name it, with the `xoxb-…` before/after, so the changed behavior is discoverable by someone diffing outputs.

- [ ] **Step 3: Add `detector_failed` to the documented event list**

Find the list of `bytechef_ai_guardrail{event}` values in this file (or in `docs/agents/ai-gateway-guardrails.md`) and add `detector_failed`, noting it is emitted on the advisor-fronted paths only.

- [ ] **Step 4: Verify no stale claim survives**

```bash
grep -niE 'replaceAll|sequential|in order|one after' docs/agents/ai-guardrails.md
```

Expected: no hit describing redaction ordering. Fix any that remain.

- [ ] **Step 5: Commit**

```bash
git add docs/agents/ai-guardrails.md
git commit -m "docs - Document the sensitive-data detector SPI"
```

---

## Task 8: Full verification

- [ ] **Step 1: Format and run the complete check for the touched modules**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api:check \
          :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:check \
          :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:check \
          --continue > /tmp/t8.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t8.log || echo "no failed tasks"
```

Expected: exit=0. If SpotBugs reports something, read `build/reports/spotbugs/test.html` — the XML report is disabled in this repo and is stale.

- [ ] **Step 2: Compile the whole server**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t8c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t8c.log || echo "no failed tasks"
```

Expected: exit=0.

- [ ] **Step 3: Run the AI Hub tests that construct `AiGuardrails`**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --continue > /tmp/t8h.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t8h.log || echo "no failed tasks"
```

Expected: exit=0, with **no edits to those files** — the legacy constructor exists precisely so they keep compiling.

- [ ] **Step 4: Confirm the spec's headline claims hold**

Check each and report the result:

1. `platform-ai-guardrails-api/build.gradle.kts` still has no compile dependency.
2. `grep -rn 'opennlp' server/` returns nothing.
3. `grep -rn 'ALL_SENSITIVE_PATTERNS\|sensitiveMatchRanges' server/` returns nothing.
4. `grep -rn 'AiGatewayGuardrails\.redact' server/` returns nothing.
5. Every assertion in `AiGuardrailsTest`'s four redaction tests is unchanged from `git show HEAD~N`.

- [ ] **Step 5: Final commit if anything was fixed**

```bash
git add -A
git commit -m "Fix review findings in the sensitive-data detector SPI"
```

---

## Self-Review

**Spec coverage:**

| Spec section | Task |
|---|---|
| §5 types, §5.1 two axes | Task 1 |
| §6.1 detect, §6.2 resolve, §6.3 apply | Task 2 |
| §6.1 built-in detectors | Task 3 |
| §6.4 three entry points + filter-before-resolve | Tasks 2 (`redact`) and 4 (`redactPiiAndSecrets`) |
| §3 bug fix | Task 3 Step 1 (`testSecretContainingDigitRunIsRedactedWholeNotPartially`) |
| §7 streaming, §7.1 `streamSafe()` | Task 5 |
| §8 fail open, §8.1 best-effort metric | Task 2 (behavior), Task 7 (documented) |
| §9 call sites, §9.1 dead statics | Tasks 4, 5, 6 |
| §10 testing | Tasks 1, 2, 3, 5 |
| §4 non-goals (no new dependency) | Task 1 Step 7, Task 8 Step 4 |

No spec section is unimplemented.

**Deviations from the spec, both deliberate and both flagged in-plan:**

1. **Span-bounds validation** in `detectCandidates` (Task 2). Not in the spec; without it a malformed contributed span crashes the turn from inside `apply`, contradicting §8's fail-open intent.
2. **A legacy constructor overload** on `AiGuardrails` (Task 4). The spec says the constructor gains the detector list; adding it as an overload rather than a parameter keeps five test files in three unrelated modules compiling, and follows the `@Autowired`-on-one-of-two pattern `AiGuardrailMetrics` already uses in this module.

**Type consistency check:** `SensitiveSpan.of(kind, category, start, end)` is used identically in Tasks 1, 2, 3 and 5. `SensitiveDataRedactor.redact(text, kinds, metrics)` keeps the same three-argument shape everywhere. `filterByKind` / `resolve` / `apply` are declared `public static` in Task 2 and consumed unchanged in Task 4 — an earlier draft of this plan declared them package-private and widened them later, which was a contradiction between tasks; they are now final at first declaration.

One deliberate arity difference, called out so it does not read as a slip: the redactor exposes `detectCandidates(text, metrics)` while `AiGuardrails` exposes `detectCandidates(text)`. The engine's one-argument form supplies its own `metrics` field, so its callers — `StreamingResponseRedactor` among them — do not have to thread a metrics instance they do not hold.
