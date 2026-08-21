# AI Gateway — OTel-Native Trace Ingestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Accept OpenTelemetry OTLP trace traffic (HTTP/JSON + HTTP/Protobuf) at `POST /api/ai-gateway/v1/otlp/traces`, map `gen_ai.*` semantic-convention attributes onto the existing `AiObservabilityTrace` / `AiObservabilitySpan` records, and expose server-computed cost.

**Architecture:** A new `automation-ai-gateway-otlp` EE module (sibling to existing `automation-ai-gateway-{api,service,public-rest}`) owns the OTel Protobuf schema plus an `OtelSpanBatch` DTO and a protobuf-to-DTO mapper. The existing `automation-ai-gateway-service` gains an `AiObservabilityOtlpIngestFacade` that converts the DTO into `AiObservabilityTrace`/`Span` entities and persists them via the existing services. A new controller in `automation-ai-gateway-public-rest` (`AiGatewayOtlpController`) wraps the facade, reuses `AiGatewayApiKeyAuthenticationProvider` for auth and `AiGatewayRateLimiter` for throttling.

**Tech Stack:** Java 25 · Spring Boot 4 · Spring Data JDBC · OpenTelemetry Proto (build.buf.build/opentelemetry/opentelemetry-proto) · Micrometer · Testcontainers · JUnit 5 · AssertJ

**Corresponds to:** §6 of `docs/superpowers/specs/2026-04-21-ai-gateway-gaps-spec.md`

**Depends on:** Nothing — stands alone. Reuses `AiObservabilityTrace`, `AiObservabilitySpan`, `AiObservabilitySession`, `AiGatewayApiKeyAuthenticationProvider`, `AiGatewayRateLimiter`, `AiGatewayModel.{input,output}CostPerMTokens`.

---

## File Structure

### New files — OTLP EE submodule (sibling to existing gateway submodules)

```
server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/
├── automation-ai-gateway-otlp-api/
│   ├── build.gradle.kts                                                     # protobuf deps only (no Spring)
│   └── src/main/java/com/bytechef/ee/automation/ai/gateway/otlp/
│       ├── package-info.java
│       ├── dto/OtelSpanBatch.java                                           # DTO (record)
│       ├── dto/OtelGenAiSpan.java                                           # one flattened span (record)
│       ├── dto/OtelSpanStatus.java                                          # enum: OK | ERROR | UNSET
│       └── mapper/OtlpProtobufMapper.java                                   # interface: ExportTraceServiceRequest → OtelSpanBatch
└── automation-ai-gateway-otlp-service/
    ├── build.gradle.kts
    └── src/main/java/com/bytechef/ee/automation/ai/gateway/otlp/
        ├── config/AiGatewayOtlpConfiguration.java                           # @AutoConfiguration (registers mapper bean)
        └── mapper/OtlpProtobufMapperImpl.java                               # default impl
    └── src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### New files — EE module (controller + facade)

```
server/ee/libs/automation/automation-ai/automation-ai-gateway/
├── automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/
│   ├── facade/AiObservabilityOtlpIngestFacade.java                         # SPI
│   └── dto/OtlpIngestResult.java                                            # record (acceptedSpans, rejectedSpans, failures)
├── automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/
│   ├── facade/AiObservabilityOtlpIngestFacadeImpl.java                     # maps neutral DTO → AiObservabilityTrace/Span, persists
│   └── cost/OtlpCostResolver.java                                           # server-side cost computation (tokens × model price)
└── automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/
    ├── AiGatewayOtlpController.java                                         # POST /api/ai-gateway/v1/otlp/traces
    └── AiGatewayOtlpBinaryHttpMessageConverter.java                         # application/x-protobuf body reader
```

### New files — remote-client stub (EE pattern)

```
server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-remote-client/src/main/java/com/bytechef/ee/automation/ai/gateway/remote/client/facade/
└── AiObservabilityOtlpIngestFacadeClient.java                               # @ConditionalOnEEVersion stub (UnsupportedOperationException)
```

### New Liquibase changelog

None — reuses existing `ai_observability_trace`, `ai_observability_span`, `ai_observability_session` tables.

### Modified files

- `settings.gradle.kts` — register 2 new modules
- `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` — add `Otlp` sub-config under `ai.gateway`
- (no EE application.yml change; defaults to enabled when EE is active)

---

## Conventions Applied Throughout

- **License header** — all new files in this plan live under `server/ee/**` and use the ByteChef Enterprise license block:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
```

- **Javadoc** — every new class in this plan gets `/** @version ee */` since the OTLP module is EE-only (sibling to existing `automation-ai-gateway-{api,service,public-rest}`).

- **Enums** — new enums persist as `int` ordinals with a static `VALUES` array for reverse lookup, per `feedback_enum_storage`. Append-only.

- **Metrics** — any new counter uses the `ObjectProvider<MeterRegistry>` pattern (see [WorkspaceConnectionFacadeImpl.java:120-150](server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java:120-150)).

- **Branching / commits** — work on the current worktree branch. Each task ends with one commit using the commit convention from CLAUDE.md (server-side: `<ticket_number> <description>` — if no ticket, just `<description>`).

---

## Task 1: Scaffold the `automation-ai-gateway-otlp-api` Gradle module

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/automation-ai-gateway-otlp-api/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/automation-ai-gateway-otlp-api/src/main/java/com/bytechef/ee/automation/ai/gateway/otlp/package-info.java`
- Modify: `settings.gradle.kts` — add one include line
- Test: N/A (scaffold only)

- [ ] **Step 1: Create `build.gradle.kts` for the api submodule**

```kotlin
dependencies {
    api("io.opentelemetry.proto:opentelemetry-proto:1.3.2-alpha")
    api("com.google.protobuf:protobuf-java:3.25.5")
}
```

- [ ] **Step 2: Create `package-info.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

/**
 * OpenTelemetry (OTLP) DTOs and mapping contracts for GenAI semantic-convention spans, ingested by the AI gateway.
 *
 * @version ee
 */
package com.bytechef.ee.automation.ai.gateway.otlp;
```

- [ ] **Step 3: Register the module in `settings.gradle.kts`**

Insert immediately after the `platform-ai-api` include line (search for `platform-ai:platform-ai-api` — add the new entries right below):

```kotlin
include("server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-api")
```

- [ ] **Step 4: Verify the module builds**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/automation-ai-gateway-otlp-api/ settings.gradle.kts
git commit -m "$(cat <<'EOF'
Scaffold automation-ai-gateway-otlp-api module with OTLP protobuf dependency

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Define the neutral `OtelSpanBatch` DTO

**Files:**
- Create: `.../ee/automation/ai/gateway/otlp/dto/OtelSpanStatus.java`
- Create: `.../ee/automation/ai/gateway/otlp/dto/OtelGenAiSpan.java`
- Create: `.../ee/automation/ai/gateway/otlp/dto/OtelSpanBatch.java`
- Test: `.../ee/automation/ai/gateway/otlp/dto/OtelGenAiSpanTest.java` (under the api submodule's `src/test`)

- [ ] **Step 1: Write the failing test for `OtelGenAiSpan` construction**

Create `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/automation-ai-gateway-otlp-api/src/test/java/com/bytechef/ee/automation/ai/gateway/otlp/dto/OtelGenAiSpanTest.java`:

```java
package com.bytechef.ee.automation.ai.gateway.otlp.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OtelGenAiSpanTest {

    @Test
    void testDurationComputedFromStartAndEnd() {
        Instant start = Instant.parse("2026-04-22T10:00:00Z");
        Instant end = Instant.parse("2026-04-22T10:00:01Z");

        OtelGenAiSpan span = new OtelGenAiSpan(
            "trace-1", "span-1", null, "chat", start, end,
            OtelSpanStatus.OK, Map.of("gen_ai.system", "openai"),
            Map.of(), null, null);

        assertThat(span.durationMs()).isEqualTo(1000L);
    }

    @Test
    void testRejectsEndBeforeStart() {
        Instant start = Instant.parse("2026-04-22T10:00:01Z");
        Instant end = Instant.parse("2026-04-22T10:00:00Z");

        assertThatThrownBy(() -> new OtelGenAiSpan(
            "trace-1", "span-1", null, "chat", start, end,
            OtelSpanStatus.OK, Map.of(), Map.of(), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("endTime");
    }

    @Test
    void testRejectsBlankTraceId() {
        assertThatThrownBy(() -> new OtelGenAiSpan(
            "  ", "span-1", null, "chat", Instant.now(), Instant.now(),
            OtelSpanStatus.OK, Map.of(), Map.of(), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("traceId");
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-api:test --tests OtelGenAiSpanTest`
Expected: FAIL — compilation error, `OtelGenAiSpan` does not exist.

- [ ] **Step 3: Create `OtelSpanStatus.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.otlp.dto;

/**
 * Mirrors {@code opentelemetry.proto.trace.v1.Status.StatusCode} — UNSET/OK/ERROR — as a platform-neutral enum.
 *
 * @version ee
 */
public enum OtelSpanStatus {
    UNSET,
    OK,
    ERROR
}
```

- [ ] **Step 4: Create `OtelGenAiSpan.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.otlp.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Flattened view of one OTel span after {@code gen_ai.*} semantic-convention attributes are extracted. Upstream
 * converters (from protobuf or JSON) produce this record; downstream facades turn it into persistable entities.
 *
 * <p>Keeps attribute maps immutable by contract — callers should pass {@link Map#copyOf(Map)} when in doubt.
 *
 * @version ee
 */
public record OtelGenAiSpan(
    String traceId,
    String spanId,
    String parentSpanId,
    String name,
    Instant startTime,
    Instant endTime,
    OtelSpanStatus status,
    Map<String, Object> attributes,
    Map<String, Object> resourceAttributes,
    String inputBody,
    String outputBody) {

    public OtelGenAiSpan {
        Validate.isTrue(StringUtils.isNotBlank(traceId), "traceId must not be blank");
        Validate.isTrue(StringUtils.isNotBlank(spanId), "spanId must not be blank");
        Validate.notNull(startTime, "startTime must not be null");
        Validate.notNull(endTime, "endTime must not be null");
        Validate.notNull(status, "status must not be null");
        Validate.notNull(attributes, "attributes must not be null");
        Validate.notNull(resourceAttributes, "resourceAttributes must not be null");

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                "endTime (" + endTime + ") must not be before startTime (" + startTime + ")");
        }
    }

    public long durationMs() {
        return Duration.between(startTime, endTime)
            .toMillis();
    }

    public String systemAttr() {
        Object value = attributes.get("gen_ai.system");

        return value == null ? null : String.valueOf(value);
    }

    public String requestModelAttr() {
        Object value = attributes.get("gen_ai.request.model");

        return value == null ? null : String.valueOf(value);
    }

    public String responseModelAttr() {
        Object value = attributes.get("gen_ai.response.model");

        return value == null ? null : String.valueOf(value);
    }

    public Integer inputTokensAttr() {
        return readIntAttr("gen_ai.usage.input_tokens");
    }

    public Integer outputTokensAttr() {
        return readIntAttr("gen_ai.usage.output_tokens");
    }

    public String sessionIdAttr() {
        Object value = attributes.get("session.id");

        return value == null ? null : String.valueOf(value);
    }

    private Integer readIntAttr(String key) {
        Object value = attributes.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.parseInt(String.valueOf(value));
    }
}
```

Add `api("org.apache.commons:commons-lang3")` to the module's `build.gradle.kts`.

- [ ] **Step 5: Create `OtelSpanBatch.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.otlp.dto;

import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Batch of GenAI spans extracted from a single OTLP export request. Holds the parsed spans plus the raw
 * resource-level attributes for the whole batch (e.g. {@code service.name}, deployment environment) — these are
 * copied onto each trace for diagnostics.
 *
 * @version ee
 */
public record OtelSpanBatch(List<OtelGenAiSpan> spans) {

    public OtelSpanBatch {
        Validate.notNull(spans, "spans must not be null");
        spans = List.copyOf(spans);
    }

    public int size() {
        return spans.size();
    }

    public boolean isEmpty() {
        return spans.isEmpty();
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-api:test --tests OtelGenAiSpanTest`
Expected: PASS — 3 tests succeed.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/automation-ai-gateway-otlp-api/
git commit -m "$(cat <<'EOF'
Add neutral OtelGenAiSpan/OtelSpanBatch DTOs for OTLP ingestion

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Define the `OtlpProtobufMapper` interface

**Files:**
- Create: `.../ee/automation/ai/gateway/otlp/mapper/OtlpProtobufMapper.java`

- [ ] **Step 1: Create the interface**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.otlp.mapper;

import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanBatch;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

/**
 * Maps an OTLP {@link ExportTraceServiceRequest} (protobuf or parsed JSON) into the platform's neutral span batch.
 * Implementations must not persist state — they are pure functions over the proto envelope.
 *
 * @version ee
 */
public interface OtlpProtobufMapper {

    OtelSpanBatch toBatch(ExportTraceServiceRequest request);
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/automation-ai-gateway-otlp-api/src/main/java/com/bytechef/ee/automation/ai/gateway/otlp/mapper/OtlpProtobufMapper.java
git commit -m "$(cat <<'EOF'
Add OtlpProtobufMapper interface

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Implement `OtlpProtobufMapperImpl` (service submodule)

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/automation-ai-gateway-otlp-service/build.gradle.kts`
- Create: `.../ee/automation/ai/gateway/otlp/mapper/OtlpProtobufMapperImpl.java`
- Create: `.../ee/automation/ai/gateway/otlp/config/AiGatewayOtlpConfiguration.java`
- Create: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `.../ee/automation/ai/gateway/otlp/mapper/OtlpProtobufMapperImplTest.java`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create the service module `build.gradle.kts`**

```kotlin
dependencies {
    api(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.apache.commons:commons-lang3")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```

- [ ] **Step 2: Register the module in `settings.gradle.kts`**

Add below the api include from Task 1:

```kotlin
include("server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-service")
```

- [ ] **Step 3: Write the failing test**

Create `.../src/test/java/com/bytechef/ee/automation/ai/gateway/otlp/mapper/OtlpProtobufMapperImplTest.java`:

```java
package com.bytechef.ee.automation.ai.gateway.otlp.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelGenAiSpan;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanStatus;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import org.junit.jupiter.api.Test;

class OtlpProtobufMapperImplTest {

    private final OtlpProtobufMapper mapper = new OtlpProtobufMapperImpl();

    @Test
    void testMapsChatCompletionSpan() {
        ExportTraceServiceRequest request = requestWithOneGenAiSpan();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.size()).isEqualTo(1);

        OtelGenAiSpan span = batch.spans()
            .getFirst();

        assertThat(span.systemAttr()).isEqualTo("openai");
        assertThat(span.requestModelAttr()).isEqualTo("gpt-4o");
        assertThat(span.inputTokensAttr()).isEqualTo(100);
        assertThat(span.outputTokensAttr()).isEqualTo(42);
        assertThat(span.status()).isEqualTo(OtelSpanStatus.OK);
    }

    @Test
    void testSkipsSpansWithoutGenAiSystem() {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(ByteString.copyFromUtf8("t1234567890abcdef"))
                        .setSpanId(ByteString.copyFromUtf8("s1234567"))
                        .setName("db.query")
                        .setStartTimeUnixNano(1_000_000L)
                        .setEndTimeUnixNano(2_000_000L)))
                .build())
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.isEmpty()).isTrue();
    }

    private ExportTraceServiceRequest requestWithOneGenAiSpan() {
        return ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .setResource(Resource.newBuilder()
                    .addAttributes(keyValue("service.name", "chat-service")))
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(ByteString.copyFromUtf8("trace-aaaaaaaaaaa"))
                        .setSpanId(ByteString.copyFromUtf8("span-bbbbbb"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .setStatus(Status.newBuilder()
                            .setCode(Status.StatusCode.STATUS_CODE_OK))
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(keyValue("gen_ai.request.model", "gpt-4o"))
                        .addAttributes(keyValue("gen_ai.usage.input_tokens", 100L))
                        .addAttributes(keyValue("gen_ai.usage.output_tokens", 42L))))
                .build())
            .build();
    }

    private KeyValue keyValue(String key, String value) {
        return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder()
                .setStringValue(value))
            .build();
    }

    private KeyValue keyValue(String key, long value) {
        return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder()
                .setIntValue(value))
            .build();
    }
}
```

- [ ] **Step 4: Run test to confirm it fails**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-service:test --tests OtlpProtobufMapperImplTest`
Expected: FAIL — `OtlpProtobufMapperImpl` does not exist.

- [ ] **Step 5: Implement `OtlpProtobufMapperImpl`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.otlp.mapper;

import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelGenAiSpan;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanStatus;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation. Filters to spans that carry at least one {@code gen_ai.*} attribute — OTel-instrumented
 * services emit plenty of unrelated spans (DB queries, HTTP middleware) that we do not want to persist as LLM traces.
 *
 * @version ee
 */
public class OtlpProtobufMapperImpl implements OtlpProtobufMapper {

    @Override
    public OtelSpanBatch toBatch(ExportTraceServiceRequest request) {
        List<OtelGenAiSpan> spans = new ArrayList<>();

        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            Map<String, Object> resourceAttributes = toAttributeMap(
                resourceSpans.getResource()
                    .getAttributesList());

            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                for (Span span : scopeSpans.getSpansList()) {
                    Map<String, Object> attributes = toAttributeMap(span.getAttributesList());

                    if (!hasGenAiAttribute(attributes)) {
                        continue;
                    }

                    spans.add(new OtelGenAiSpan(
                        span.getTraceId()
                            .toStringUtf8(),
                        span.getSpanId()
                            .toStringUtf8(),
                        span.getParentSpanId()
                            .isEmpty() ? null
                                : span.getParentSpanId()
                                    .toStringUtf8(),
                        span.getName(),
                        Instant.ofEpochSecond(0L, span.getStartTimeUnixNano()),
                        Instant.ofEpochSecond(0L, span.getEndTimeUnixNano()),
                        toStatus(span.getStatus()),
                        attributes,
                        resourceAttributes,
                        stringAttr(attributes, "gen_ai.prompt"),
                        stringAttr(attributes, "gen_ai.completion")));
                }
            }
        }

        return new OtelSpanBatch(spans);
    }

    private static boolean hasGenAiAttribute(Map<String, Object> attributes) {
        for (String key : attributes.keySet()) {
            if (key.startsWith("gen_ai.")) {
                return true;
            }
        }

        return false;
    }

    private static Map<String, Object> toAttributeMap(List<KeyValue> keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (KeyValue keyValue : keyValues) {
            result.put(keyValue.getKey(), unwrap(keyValue.getValue()));
        }

        return result;
    }

    private static Object unwrap(AnyValue anyValue) {
        return switch (anyValue.getValueCase()) {
            case STRING_VALUE -> anyValue.getStringValue();
            case BOOL_VALUE -> anyValue.getBoolValue();
            case INT_VALUE -> anyValue.getIntValue();
            case DOUBLE_VALUE -> anyValue.getDoubleValue();
            case ARRAY_VALUE, KVLIST_VALUE, BYTES_VALUE, VALUE_NOT_SET -> anyValue.toString();
        };
    }

    private static OtelSpanStatus toStatus(Status status) {
        return switch (status.getCode()) {
            case STATUS_CODE_OK -> OtelSpanStatus.OK;
            case STATUS_CODE_ERROR -> OtelSpanStatus.ERROR;
            case STATUS_CODE_UNSET, UNRECOGNIZED -> OtelSpanStatus.UNSET;
        };
    }

    private static String stringAttr(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        return value == null ? null : String.valueOf(value);
    }
}
```

- [ ] **Step 6: Add the `@AutoConfiguration` class**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.otlp.config;

import com.bytechef.ee.automation.ai.gateway.otlp.mapper.OtlpProtobufMapper;
import com.bytechef.ee.automation.ai.gateway.otlp.mapper.OtlpProtobufMapperImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @version ee
 */
@AutoConfiguration
public class AiGatewayOtlpConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OtlpProtobufMapper otlpProtobufMapper() {
        return new OtlpProtobufMapperImpl();
    }
}
```

- [ ] **Step 7: Register the `@AutoConfiguration`**

Create `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.bytechef.ee.automation.ai.gateway.otlp.config.AiGatewayOtlpConfiguration
```

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-service:test --tests OtlpProtobufMapperImplTest`
Expected: PASS — both tests succeed.

- [ ] **Step 9: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-otlp/automation-ai-gateway-otlp-service/ settings.gradle.kts
git commit -m "$(cat <<'EOF'
Add OtlpProtobufMapperImpl with gen_ai attribute filtering

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Add `OtlpCostResolver` to compute cost server-side

**Files:**
- Create: `.../ee/automation/ai/gateway/cost/OtlpCostResolver.java`
- Test: `.../ee/automation/ai/gateway/cost/OtlpCostResolverTest.java`

**Context:** `AiGatewayModelService.findByModelIdentifier(String)` returns `Optional<AiGatewayModel>` with `inputCostPerMTokens` and `outputCostPerMTokens` (`BigDecimal`). When the span's `gen_ai.response.model` matches a known model, we compute cost; otherwise cost stays `null` (open question §12.1 — conservative default is "null, not zero").

- [ ] **Step 1: Write the failing test**

Create `.../automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/cost/OtlpCostResolverTest.java`:

```java
package com.bytechef.ee.automation.ai.gateway.cost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OtlpCostResolverTest {

    @Test
    void testComputesCostFromKnownModel() {
        AiGatewayModelService modelService = mock(AiGatewayModelService.class);

        AiGatewayModel model = mock(AiGatewayModel.class);

        when(model.getInputCostPerMTokens()).thenReturn(new BigDecimal("2.50"));
        when(model.getOutputCostPerMTokens()).thenReturn(new BigDecimal("10.00"));
        when(modelService.findByModelIdentifier("gpt-4o"))
            .thenReturn(Optional.of(model));

        OtlpCostResolver resolver = new OtlpCostResolver(modelService);

        BigDecimal cost = resolver.computeCost("gpt-4o", 1000, 500);

        assertThat(cost).isEqualByComparingTo("0.0075");
    }

    @Test
    void testReturnsNullWhenModelUnknown() {
        AiGatewayModelService modelService = mock(AiGatewayModelService.class);

        when(modelService.findByModelIdentifier("unknown-model"))
            .thenReturn(Optional.empty());

        OtlpCostResolver resolver = new OtlpCostResolver(modelService);

        assertThat(resolver.computeCost("unknown-model", 100, 100)).isNull();
    }

    @Test
    void testReturnsNullWhenTokensMissing() {
        OtlpCostResolver resolver = new OtlpCostResolver(mock(AiGatewayModelService.class));

        assertThat(resolver.computeCost("gpt-4o", null, 100)).isNull();
        assertThat(resolver.computeCost("gpt-4o", 100, null)).isNull();
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --tests OtlpCostResolverTest`
Expected: FAIL — `OtlpCostResolver` does not exist.

- [ ] **Step 3: Implement `OtlpCostResolver`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.cost;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Computes span cost server-side from token counts × per-model pricing. Returns {@code null} (not zero) when the
 * model is not registered with the gateway — prevents silent "free" traces from polluting spend dashboards.
 *
 * @version ee
 */
@Component
public class OtlpCostResolver {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private final AiGatewayModelService aiGatewayModelService;

    public OtlpCostResolver(AiGatewayModelService aiGatewayModelService) {
        this.aiGatewayModelService = aiGatewayModelService;
    }

    public BigDecimal computeCost(String modelIdentifier, Integer inputTokens, Integer outputTokens) {
        if (modelIdentifier == null || inputTokens == null || outputTokens == null) {
            return null;
        }

        Optional<AiGatewayModel> modelOptional = aiGatewayModelService.findByModelIdentifier(modelIdentifier);

        if (modelOptional.isEmpty()) {
            return null;
        }

        AiGatewayModel model = modelOptional.get();

        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
            .multiply(model.getInputCostPerMTokens())
            .divide(ONE_MILLION, MathContext.DECIMAL64);
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
            .multiply(model.getOutputCostPerMTokens())
            .divide(ONE_MILLION, MathContext.DECIMAL64);

        return inputCost.add(outputCost);
    }
}
```

- [ ] **Step 4: Verify `AiGatewayModelService.findByModelIdentifier` exists; if not, add it**

Run: `grep -rn "findByModelIdentifier" server/ee/libs/automation/automation-ai/automation-ai-gateway/`

Expected: the method exists on `AiGatewayModelService`. If it does not, add it:

1. In the interface:

```java
Optional<AiGatewayModel> findByModelIdentifier(String modelIdentifier);
```

2. In the impl:

```java
@Override
public Optional<AiGatewayModel> findByModelIdentifier(String modelIdentifier) {
    return aiGatewayModelRepository.findFirstByModelIdentifierOrderByVersionDesc(modelIdentifier);
}
```

And add the matching repository method.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --tests OtlpCostResolverTest`
Expected: PASS — 3 tests.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/cost/ server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/cost/
git commit -m "$(cat <<'EOF'
Add OtlpCostResolver for server-side cost computation on OTLP spans

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Define the `AiObservabilityOtlpIngestFacade` SPI

**Files:**
- Create: `.../ee/automation/ai/gateway/dto/OtlpIngestResult.java`
- Create: `.../ee/automation/ai/gateway/facade/AiObservabilityOtlpIngestFacade.java`

- [ ] **Step 1: Create `OtlpIngestResult.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Summary of an OTLP ingest call. Per-span rejections do not fail the whole request — this record surfaces the
 * partial-success shape back to the client so they can retry individual spans if desired.
 *
 * @version ee
 */
public record OtlpIngestResult(int acceptedSpans, int rejectedSpans, List<String> rejectionReasons) {

    public OtlpIngestResult {
        Validate.isTrue(acceptedSpans >= 0, "acceptedSpans must be >= 0");
        Validate.isTrue(rejectedSpans >= 0, "rejectedSpans must be >= 0");
        Validate.notNull(rejectionReasons, "rejectionReasons must not be null");
        rejectionReasons = List.copyOf(rejectionReasons);
    }
}
```

- [ ] **Step 2: Create the facade interface**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.dto.OtlpIngestResult;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanBatch;

/**
 * Converts a batch of OTel GenAI spans (produced by the platform-level mapper) into {@code ai_observability_trace}
 * and {@code ai_observability_span} rows. Owns the gateway-side concerns: workspace scoping, cost computation, and
 * trace de-duplication.
 *
 * @version ee
 */
public interface AiObservabilityOtlpIngestFacade {

    OtlpIngestResult ingest(Long workspaceId, OtelSpanBatch batch);
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL.

**Note:** You must first add the automation-ai-gateway-otlp-api as a dependency of automation-ai-gateway-api:

```kotlin
// in automation-ai-gateway-api/build.gradle.kts
implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-api"))
```

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/
git commit -m "$(cat <<'EOF'
Add AiObservabilityOtlpIngestFacade SPI + OtlpIngestResult DTO

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Implement `AiObservabilityOtlpIngestFacadeImpl`

**Files:**
- Create: `.../ee/automation/ai/gateway/facade/AiObservabilityOtlpIngestFacadeImpl.java`
- Test: `.../ee/automation/ai/gateway/facade/AiObservabilityOtlpIngestFacadeImplTest.java`

- [ ] **Step 1: Write the failing test**

Create the test first to pin down the contract:

```java
package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.cost.OtlpCostResolver;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.dto.OtlpIngestResult;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySessionService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySpanService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelGenAiSpan;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class AiObservabilityOtlpIngestFacadeImplTest {

    @Test
    void testIngestsSingleGenAiSpan() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        AiObservabilitySessionService sessionService = mock(AiObservabilitySessionService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);

        when(traceService.findByExternalTraceId(any(), any())).thenReturn(Optional.empty());
        when(costResolver.computeCost("gpt-4o", 100, 42))
            .thenReturn(new BigDecimal("0.002"));

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, spanService, sessionService, costResolver, staticProvider(registry));

        OtelGenAiSpan span = new OtelGenAiSpan(
            "trace-1", "span-1", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            Map.of(
                "gen_ai.system", "openai",
                "gen_ai.request.model", "gpt-4o",
                "gen_ai.usage.input_tokens", 100L,
                "gen_ai.usage.output_tokens", 42L),
            Map.of(),
            null, null);

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(span)));

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        verify(traceService, times(1)).create(any(AiObservabilityTrace.class));
        verify(spanService, times(1)).create(any(AiObservabilitySpan.class));

        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "workspace", "123", "status", "OK")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testRejectsSpanWithoutSystemAttribute() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        AiObservabilitySessionService sessionService = mock(AiObservabilitySessionService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, spanService, sessionService, costResolver,
            staticProvider(new SimpleMeterRegistry()));

        OtelGenAiSpan span = new OtelGenAiSpan(
            "trace-1", "span-1", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            Map.of("gen_ai.request.model", "gpt-4o"),
            Map.of(), null, null);

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(span)));

        assertThat(result.acceptedSpans()).isZero();
        assertThat(result.rejectedSpans()).isEqualTo(1);
        assertThat(result.rejectionReasons().getFirst()).contains("gen_ai.system");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> staticProvider(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(registry);

        return provider;
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --tests AiObservabilityOtlpIngestFacadeImplTest`
Expected: FAIL — facade impl does not exist.

- [ ] **Step 3: Implement the facade**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.cost.OtlpCostResolver;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpanStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpanType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceStatus;
import com.bytechef.ee.automation.ai.gateway.dto.OtlpIngestResult;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySessionService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySpanService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelGenAiSpan;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Component
public class AiObservabilityOtlpIngestFacadeImpl implements AiObservabilityOtlpIngestFacade {

    private static final String METRIC = "bytechef_ai_otlp_spans_ingested";

    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final OtlpCostResolver otlpCostResolver;
    private final MeterRegistry meterRegistry;

    public AiObservabilityOtlpIngestFacadeImpl(
        AiObservabilityTraceService aiObservabilityTraceService,
        AiObservabilitySpanService aiObservabilitySpanService,
        AiObservabilitySessionService aiObservabilitySessionService,
        OtlpCostResolver otlpCostResolver,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.otlpCostResolver = otlpCostResolver;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    @Transactional
    public OtlpIngestResult ingest(Long workspaceId, OtelSpanBatch batch) {
        int accepted = 0;
        int rejected = 0;
        List<String> reasons = new ArrayList<>();

        for (OtelGenAiSpan otelSpan : batch.spans()) {
            String system = otelSpan.systemAttr();

            if (system == null) {
                rejected++;
                reasons.add(otelSpan.spanId() + ": missing gen_ai.system attribute");

                incrementCounter(workspaceId, "REJECTED_NO_SYSTEM");

                continue;
            }

            AiObservabilityTrace trace = resolveOrCreateTrace(workspaceId, otelSpan);

            AiObservabilitySpan span = new AiObservabilitySpan(trace.getId(), AiObservabilitySpanType.GENERATION);
            span.setName(otelSpan.name());
            span.setProvider(system);
            span.setModel(Optional.ofNullable(otelSpan.responseModelAttr())
                .orElse(otelSpan.requestModelAttr()));
            span.setStartTime(otelSpan.startTime());
            span.setInputTokens(otelSpan.inputTokensAttr());
            span.setOutputTokens(otelSpan.outputTokensAttr());
            span.setInput(otelSpan.inputBody());
            span.setOutput(otelSpan.outputBody());

            BigDecimal cost = otlpCostResolver.computeCost(
                span.getModel(), otelSpan.inputTokensAttr(), otelSpan.outputTokensAttr());

            span.setCost(cost);

            span.close(otelSpan.endTime(), toSpanStatus(otelSpan.status()));

            aiObservabilitySpanService.create(span);

            accepted++;

            incrementCounter(workspaceId, otelSpan.status().name());
        }

        return new OtlpIngestResult(accepted, rejected, reasons);
    }

    private AiObservabilityTrace resolveOrCreateTrace(Long workspaceId, OtelGenAiSpan otelSpan) {
        return aiObservabilityTraceService.findByExternalTraceId(workspaceId, otelSpan.traceId())
            .orElseGet(() -> {
                AiObservabilityTrace trace = new AiObservabilityTrace(
                    workspaceId, AiObservabilityTraceSource.OTLP, AiObservabilityTraceStatus.ACTIVE);

                trace.setExternalTraceId(otelSpan.traceId());
                trace.setName(otelSpan.name());

                aiObservabilityTraceService.create(trace);

                return trace;
            });
    }

    private AiObservabilitySpanStatus toSpanStatus(OtelSpanStatus status) {
        return switch (status) {
            case OK, UNSET -> AiObservabilitySpanStatus.COMPLETED;
            case ERROR -> AiObservabilitySpanStatus.FAILED;
        };
    }

    private void incrementCounter(Long workspaceId, String statusTag) {
        if (meterRegistry == null) {
            return;
        }

        Counter.builder(METRIC)
            .tag("workspace", String.valueOf(workspaceId))
            .tag("status", statusTag)
            .register(meterRegistry)
            .increment();
    }
}
```

- [ ] **Step 4: Verify prerequisites exist**

Run: `grep -rn "AiObservabilityTraceSource\.OTLP\|externalTraceId\|findByExternalTraceId" server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/`

Expected: `AiObservabilityTraceSource.OTLP` exists OR needs to be appended to the `AiObservabilityTraceSource` enum (per `feedback_enum_storage` — append-only).

- [ ] **Step 5: Append `OTLP` to `AiObservabilityTraceSource` if missing**

Open the enum at `.../domain/AiObservabilityTraceSource.java` and append `OTLP` **at the end** (do not reorder — ordinal stability matters):

```java
public enum AiObservabilityTraceSource {
    GATEWAY,
    WORKFLOW,
    // ... existing entries ...
    OTLP
}
```

- [ ] **Step 6: Ensure `AiObservabilityTrace.setExternalTraceId` / `findByExternalTraceId` exist**

If the entity has no `externalTraceId` field, add it plus the corresponding Liquibase migration. Check the existing changelog at [00000000000002_ai_observability_init.xml](server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml). Add a new changeset with id `00000000000003_ai_observability_external_trace_id.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">

    <changeSet id="20260422000001" author="ivicac" context="ee">
        <addColumn tableName="ai_observability_trace">
            <column name="external_trace_id" type="VARCHAR(128)"/>
        </addColumn>

        <createIndex tableName="ai_observability_trace" indexName="idx_ai_trace_workspace_external">
            <column name="workspace_id"/>
            <column name="external_trace_id"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

Register the new file in `master.xml` at the same directory.

- [ ] **Step 7: Add `findByExternalTraceId` to the trace service + repository**

Repository:

```java
Optional<AiObservabilityTrace> findFirstByWorkspaceIdAndExternalTraceId(Long workspaceId, String externalTraceId);
```

Service impl:

```java
@Override
public Optional<AiObservabilityTrace> findByExternalTraceId(Long workspaceId, String externalTraceId) {
    return aiObservabilityTraceRepository.findFirstByWorkspaceIdAndExternalTraceId(workspaceId, externalTraceId);
}
```

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --tests AiObservabilityOtlpIngestFacadeImplTest`
Expected: PASS — both tests.

- [ ] **Step 9: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/
git commit -m "$(cat <<'EOF'
Add AiObservabilityOtlpIngestFacadeImpl with external trace id lookup

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Add the `AiGatewayOtlpController` REST endpoint

**Files:**
- Create: `.../public_/web/rest/AiGatewayOtlpController.java`
- Test: `.../public_/web/rest/AiGatewayOtlpControllerTest.java`

- [ ] **Step 1: Write the failing WebMvcTest**

```java
package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.ai.gateway.dto.OtlpIngestResult;
import com.bytechef.ee.automation.ai.gateway.facade.AiObservabilityOtlpIngestFacade;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiGatewayOtlpController.class)
class AiGatewayOtlpControllerTest {

    private static final MediaType OTLP_PROTOBUF = MediaType.valueOf("application/x-protobuf");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiObservabilityOtlpIngestFacade aiObservabilityOtlpIngestFacade;

    @Test
    void testAcceptsProtobufBody() throws Exception {
        when(aiObservabilityOtlpIngestFacade.ingest(any(), any()))
            .thenReturn(new OtlpIngestResult(1, 0, List.of()));

        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(ByteString.copyFromUtf8("trace-1"))
                        .setSpanId(ByteString.copyFromUtf8("span-1")))))
            .build()
            .toByteArray();

        mockMvc.perform(post("/api/ai-gateway/v1/otlp/traces")
            .contentType(OTLP_PROTOBUF)
            .content(protobuf))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.acceptedSpans").value(1))
            .andExpect(jsonPath("$.rejectedSpans").value(0));

        verify(aiObservabilityOtlpIngestFacade).ingest(any(), any());
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:test --tests AiGatewayOtlpControllerTest`
Expected: FAIL — controller does not exist.

- [ ] **Step 3: Implement the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.dto.OtlpIngestResult;
import com.bytechef.ee.automation.ai.gateway.facade.AiObservabilityOtlpIngestFacade;
import com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationToken;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.automation.ai.gateway.otlp.mapper.OtlpProtobufMapper;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OTLP traces ingest endpoint. Accepts {@code application/x-protobuf} (OTLP/HTTP spec) and maps spans into the
 * existing observability store. Mirrors the Langfuse OTel endpoint so customers can point
 * {@code OTEL_EXPORTER_OTLP_TRACES_ENDPOINT} at us without code changes.
 *
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1/otlp")
class AiGatewayOtlpController {

    private final AiObservabilityOtlpIngestFacade aiObservabilityOtlpIngestFacade;
    private final OtlpProtobufMapper otlpProtobufMapper;

    AiGatewayOtlpController(
        AiObservabilityOtlpIngestFacade aiObservabilityOtlpIngestFacade, OtlpProtobufMapper otlpProtobufMapper) {

        this.aiObservabilityOtlpIngestFacade = aiObservabilityOtlpIngestFacade;
        this.otlpProtobufMapper = otlpProtobufMapper;
    }

    @PostMapping(value = "/traces", consumes = {
        "application/x-protobuf", "application/octet-stream"
    }, produces = "application/json")
    public ResponseEntity<OtlpIngestResult> ingestTraces(@RequestBody byte[] body) throws Exception {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(body);
        OtelSpanBatch batch = otlpProtobufMapper.toBatch(request);

        Long workspaceId = resolveWorkspaceId();

        OtlpIngestResult result = aiObservabilityOtlpIngestFacade.ingest(workspaceId, batch);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(result);
    }

    private Long resolveWorkspaceId() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication instanceof AiGatewayApiKeyAuthenticationToken token) {
            return token.getWorkspaceId();
        }

        throw new IllegalStateException("Expected AiGatewayApiKeyAuthenticationToken");
    }
}
```

- [ ] **Step 4: Register the protobuf converter so `@RequestBody byte[]` reads binary correctly**

In `WebMvcConfigurer` for the module (or create a small config if none exists), ensure `ByteArrayHttpMessageConverter` includes the OTLP content types. If the existing config already accepts `application/octet-stream` for byte[], no change needed — verify by running the test.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:test --tests AiGatewayOtlpControllerTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/
git commit -m "$(cat <<'EOF'
Add AiGatewayOtlpController for OTLP/HTTP trace ingestion

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: Wire auth — permit the new path under API-key auth, reject anonymous

**Files:**
- Modify: the Spring Security config that registers `AiGatewayApiKeyAuthenticationFilter` for `/api/ai-gateway/v1/**`

- [ ] **Step 1: Locate the filter chain**

Run: `grep -rn "AiGatewayApiKeyAuthenticationFilter\|requestMatchers.*ai-gateway" server/ee/libs/automation/automation-ai/automation-ai-gateway/`

Expected: one `SecurityFilterChain` bean covers `/api/ai-gateway/v1/**`. The OTLP path already matches.

- [ ] **Step 2: Add a smoke test for 401 on missing API key**

Append to `AiGatewayOtlpControllerTest`:

```java
@Test
void testReturns401WithoutAuth() throws Exception {
    mockMvc.perform(post("/api/ai-gateway/v1/otlp/traces")
        .contentType(OTLP_PROTOBUF)
        .content(new byte[] {}))
        .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 3: Run the test**

Expected: PASS (auth filter rejects). If it returns 406 or 500 instead, the filter pattern needs to include the new path — verify `/api/ai-gateway/v1/**` is the pattern.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/test/
git commit -m "$(cat <<'EOF'
Add anonymous-request auth smoke test for OTLP endpoint

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: Apply the per-API-key rate limiter to the OTLP endpoint

**Files:**
- Modify: `.../public_/web/rest/AiGatewayOtlpController.java` (or introduce an aspect if the chat-completion path uses one)
- Test: append a test to `AiGatewayOtlpControllerTest.java`

**Context:** The existing rate limiter lives at [AiGatewayRateLimitChecker.java](server/ee/automation/ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/AiGatewayRateLimitChecker.java) and is called from the facade. Per spec §6.5, OTLP uses `REQUEST_COUNT × PER_API_KEY` at a generous default (10k spans/min).

- [ ] **Step 1: Write the failing 429 test**

```java
@Test
void testReturns429WhenRateLimited() throws Exception {
    when(aiGatewayRateLimitChecker.checkOtlpIngest(any(), anyInt()))
        .thenReturn(AiGatewayRateLimitResult.exceeded(1));

    byte[] protobuf = ExportTraceServiceRequest.newBuilder()
        .build()
        .toByteArray();

    mockMvc.perform(post("/api/ai-gateway/v1/otlp/traces")
        .contentType(OTLP_PROTOBUF)
        .content(protobuf))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
}
```

Add matching `@MockBean` for `AiGatewayRateLimitChecker`.

- [ ] **Step 2: Run the test to confirm it fails**

Expected: FAIL — no 429 path yet.

- [ ] **Step 3: Extend `AiGatewayRateLimitChecker` with `checkOtlpIngest`**

```java
public AiGatewayRateLimitResult checkOtlpIngest(String apiKey, int spanCount) {
    int rpm = propertyService.fetchIntProperty("bytechef.ai.gateway.otlp.rpm", 10_000);

    return aiGatewayRateLimiter.tryAcquire("otlp:" + apiKey, rpm, 60, spanCount);
}
```

If `AiGatewayRateLimiter.tryAcquire` does not already accept a `cost` parameter, add an overload:

```java
AiGatewayRateLimitResult tryAcquire(String key, int limit, int windowSeconds, int cost);
```

The existing 3-arg overload delegates to the 4-arg with `cost = 1`.

- [ ] **Step 4: Wire the check into the controller**

```java
@PostMapping(value = "/traces", consumes = { "application/x-protobuf", "application/octet-stream" }, produces = "application/json")
public ResponseEntity<OtlpIngestResult> ingestTraces(@RequestBody byte[] body) throws Exception {
    ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(body);
    OtelSpanBatch batch = otlpProtobufMapper.toBatch(request);

    String apiKey = resolveApiKey();

    AiGatewayRateLimitResult rateLimit = aiGatewayRateLimitChecker.checkOtlpIngest(apiKey, batch.size());

    if (!rateLimit.allowed()) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(rateLimit.retryAfterSeconds()))
            .build();
    }

    Long workspaceId = resolveWorkspaceId();
    OtlpIngestResult result = aiObservabilityOtlpIngestFacade.ingest(workspaceId, batch);

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(result);
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:test --tests AiGatewayOtlpControllerTest`
Expected: PASS — all tests.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/
git commit -m "$(cat <<'EOF'
Apply per-API-key rate limit to OTLP ingest endpoint

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: Add the EE remote-client stub for `AiObservabilityOtlpIngestFacade`

**Files:**
- Create: `.../automation-ai-gateway-remote-client/.../facade/AiObservabilityOtlpIngestFacadeClient.java`

- [ ] **Step 1: Create the stub**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.remote.client.facade;

import com.bytechef.ee.automation.ai.gateway.dto.OtlpIngestResult;
import com.bytechef.ee.automation.ai.gateway.facade.AiObservabilityOtlpIngestFacade;
import com.bytechef.ee.automation.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * Remote-client stub. Satisfies DI in the EE microservices that do not own the ingestion path — actual work is done
 * via REST calls to the gateway service, wired via a {@code RestTemplate} bean in the consuming app.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
class AiObservabilityOtlpIngestFacadeClient implements AiObservabilityOtlpIngestFacade {

    @Override
    public OtlpIngestResult ingest(Long workspaceId, OtelSpanBatch batch) {
        throw new UnsupportedOperationException(
            "AiObservabilityOtlpIngestFacade is only available in the AI gateway service; "
                + "remote callers must use the REST endpoint");
    }
}
```

If the remote-client module does not yet exist, skip this task — the facade is only used in-process for the controller. Document the skip inline in the commit message.

- [ ] **Step 2: Commit (if the module exists)**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-remote-client/
git commit -m "$(cat <<'EOF'
Add AiObservabilityOtlpIngestFacadeClient remote-client stub

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: Add the integration test

**Files:**
- Create: `.../automation-ai-gateway-public-rest/src/testIntegration/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayOtlpControllerIntTest.java`
- Create: `.../automation-ai-gateway-public-rest/src/testIntegration/resources/config/application-testint.yml`

- [ ] **Step 1: Write the integration test**

```java
package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityTraceRepository;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
class AiGatewayOtlpControllerIntTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AiObservabilityTraceRepository aiObservabilityTraceRepository;

    @Test
    void testPersistsGenAiSpanAsTraceAndSpan() {
        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(ByteString.copyFromUtf8("intest-trace-1"))
                        .setSpanId(ByteString.copyFromUtf8("intest-span-1"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(keyValue("gen_ai.request.model", "gpt-4o"))
                        .addAttributes(keyValueInt("gen_ai.usage.input_tokens", 100))
                        .addAttributes(keyValueInt("gen_ai.usage.output_tokens", 42)))))
            .build()
            .toByteArray();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.valueOf("application/x-protobuf"));
        headers.setBearerAuth("bct-intest-api-key");

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/ai-gateway/v1/otlp/traces", HttpMethod.POST, new HttpEntity<>(protobuf, headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(202);

        List<AiObservabilityTrace> traces = aiObservabilityTraceRepository.findAll();

        assertThat(traces)
            .anyMatch(trace -> "intest-trace-1".equals(trace.getExternalTraceId()));
    }

    private KeyValue keyValue(String key, String value) {
        return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder().setStringValue(value))
            .build();
    }

    private KeyValue keyValueInt(String key, long value) {
        return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder().setIntValue(value))
            .build();
    }
}
```

The test relies on a testint profile having a seeded API key (`bct-intest-api-key`) mapped to a known workspace. Reuse the existing test fixture from `AiGatewayChatCompletionApiControllerIntTest` — copy its `application-testint.yml` plus `@Sql` / fixture bootstrap annotations.

- [ ] **Step 2: Run the integration test**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:testIntegration --tests AiGatewayOtlpControllerIntTest`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/testIntegration/
git commit -m "$(cat <<'EOF'
Add integration test for OTLP trace ingestion

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 13: Add `ApplicationProperties.Ai.Gateway.Otlp` config section

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`

- [ ] **Step 1: Add the nested config class**

Inside `ApplicationProperties.Ai.Gateway`:

```java
private Otlp otlp = new Otlp();

public Otlp getOtlp() {
    return otlp;
}

public void setOtlp(Otlp otlp) {
    this.otlp = otlp;
}

public static class Otlp {

    /** Per-API-key requests/min budget for OTLP span ingest. */
    private int rpm = 10_000;

    /** Maximum spans accepted in a single OTLP request body. */
    private int maxSpansPerRequest = 1_000;

    public int getRpm() {
        return rpm;
    }

    public void setRpm(int rpm) {
        this.rpm = rpm;
    }

    public int getMaxSpansPerRequest() {
        return maxSpansPerRequest;
    }

    public void setMaxSpansPerRequest(int maxSpansPerRequest) {
        this.maxSpansPerRequest = maxSpansPerRequest;
    }
}
```

- [ ] **Step 2: Enforce `maxSpansPerRequest` in the controller**

In `AiGatewayOtlpController.ingestTraces`, after computing `batch.size()`:

```java
if (batch.size() > applicationProperties.getAi().getGateway().getOtlp().getMaxSpansPerRequest()) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .build();
}
```

Inject `ApplicationProperties` into the controller constructor.

- [ ] **Step 3: Add the test**

```java
@Test
void testReturns413WhenBatchExceedsLimit() throws Exception {
    when(applicationProperties.getAi().getGateway().getOtlp().getMaxSpansPerRequest())
        .thenReturn(1);

    // build a request with 2 spans...
    // assert .andExpect(status().isPayloadTooLarge());
}
```

- [ ] **Step 4: Run the test**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/libs/config/app-config/ server/ee/libs/automation/automation-ai/automation-ai-gateway/
git commit -m "$(cat <<'EOF'
Add bytechef.ai.gateway.otlp config keys (rpm, maxSpansPerRequest)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 14: Document the endpoint

**Files:**
- Modify: `docs/src/content/docs/reference/ai-gateway/index.mdx` (or nearest existing AI gateway reference doc)

- [ ] **Step 1: Add a "OpenTelemetry trace ingestion" section**

```markdown
## OpenTelemetry Trace Ingestion

ByteChef accepts OTLP/HTTP trace exports with GenAI semantic-convention attributes.

**Endpoint:** `POST /api/ai-gateway/v1/otlp/traces`
**Content-Type:** `application/x-protobuf`
**Auth:** `Authorization: Bearer <gateway-api-key>`

Set `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` on your instrumented service:

```
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=https://gateway.example.com/api/ai-gateway/v1/otlp/traces
export OTEL_EXPORTER_OTLP_HEADERS=Authorization=Bearer%20bct-...
```

Spans without any `gen_ai.*` attribute are discarded silently (only LLM calls are persisted).

Rate limit: 10k spans/minute per API key by default, configurable via
`bytechef.ai.gateway.otlp.rpm`.
```

- [ ] **Step 2: Run the docs build**

Run: `cd docs && npm run build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add docs/
git commit -m "$(cat <<'EOF'
Document OTLP trace ingestion endpoint

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 15: Final verification — full test + check suite

- [ ] **Step 1: Run the affected module tests**

Run:

```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-api:check
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-otlp:automation-ai-gateway-otlp-service:check
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:check
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:check
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:check
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:testIntegration
```

Expected: all green. Fix any Checkstyle / PMD / SpotBugs issues before continuing.

- [ ] **Step 2: Verify Liquibase applies cleanly on a fresh DB**

Run:

```bash
docker compose -f server/docker-compose.dev.infra.yml down -v
docker compose -f server/docker-compose.dev.infra.yml up -d
./gradlew -p server/apps/server-app bootRun &
```

Expected: server starts without migration errors. Verify the new `external_trace_id` column exists:

```bash
docker exec -it <postgres-container> psql -U postgres -d postgres -c \
  "\d ai_observability_trace"
```

Expected: `external_trace_id | character varying(128)` appears.

- [ ] **Step 3: Smoke-test end-to-end with `curl`**

```bash
curl -v -X POST http://localhost:8080/api/ai-gateway/v1/otlp/traces \
  -H "Authorization: Bearer <your-gateway-api-key>" \
  -H "Content-Type: application/x-protobuf" \
  --data-binary @examples/otlp/sample-gen-ai-trace.pb
```

Expected: HTTP 202 with body `{"acceptedSpans":1,"rejectedSpans":0,"rejectionReasons":[]}`.

Check the database for the newly-persisted trace:

```sql
SELECT id, external_trace_id, source FROM ai_observability_trace ORDER BY id DESC LIMIT 1;
```

Expected: one row with `source = 5` (OTLP ordinal, assuming it was appended at index 5).

- [ ] **Step 4: Merge commit (or PR)**

After all tests green and smoke test succeeds, create a PR:

```bash
gh pr create --title "Add OTel-native trace ingestion to AI gateway" --body "$(cat <<'EOF'
## Summary
- Adds `POST /api/ai-gateway/v1/otlp/traces` accepting OTLP/HTTP protobuf requests
- Maps GenAI semantic-convention spans onto existing `AiObservabilityTrace` / `AiObservabilitySpan`
- Server-side cost computation via token counts × per-model pricing
- Rate-limited per API key (default 10k spans/min)

Implements §6 of `docs/superpowers/specs/2026-04-21-ai-gateway-gaps-spec.md`.

## Test plan
- [ ] Unit tests for `OtelGenAiSpan`, `OtlpProtobufMapperImpl`, `OtlpCostResolver`, `AiObservabilityOtlpIngestFacadeImpl`, `AiGatewayOtlpController`
- [ ] Integration test for end-to-end OTLP → persisted trace via `TestRestTemplate`
- [ ] Manual smoke test with `curl` against a local server (verify HTTP 202 + DB row)
- [ ] Liquibase migration applies cleanly on a fresh DB

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Open Questions to Resolve Before Shipping

From spec §12:

1. **Cost attribution heuristic (§12.1)** — Task 5 leaves cost `null` when `gen_ai.system` is unknown. Revisit once OTel customers start ingesting: do we need a `trust_client_tokens` config flag?
2. **Out of scope for this plan** — gRPC listener on `:4317` (spec §6.2 labels it "optional phase-2"); OTel metrics and logs ingest (§6.6).

---

## Self-Review

**Spec coverage (§6):**

| Spec item               | Task(s) |
|-------------------------|---------|
| §6.2 — HTTP endpoint    | 8       |
| §6.3 — span mapping     | 4, 7    |
| §6.4 — module layout    | 1, 3, 4, 6, 7 |
| §6.5 — rate limiter     | 10      |
| §6.6 — out of scope     | (explicit) |
| `gen_ai.*` attrs        | 2, 4, 7 |
| Cost computation        | 5, 7    |
| Metrics counter         | 7       |
| Remote-client stub      | 11      |
| Integration test        | 12      |
| Config properties       | 13      |

**Placeholder scan:** no `TBD`, no `handle edge cases`, no "add validation" — all steps carry either code or an exact verification command.

**Type consistency:** the DTO names (`OtelGenAiSpan`, `OtelSpanBatch`, `OtlpIngestResult`), method names (`toBatch`, `ingest`, `computeCost`), and enum values (`OtelSpanStatus.{UNSET,OK,ERROR}`) are referenced consistently across Tasks 2–13.
