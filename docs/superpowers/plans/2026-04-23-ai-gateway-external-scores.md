# AI Gateway — External Scores Ingestion API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose `POST /api/ai-gateway/v1/traces/{traceId}/scores`, `POST /api/ai-gateway/v1/spans/{spanId}/scores`, and `POST /api/ai-gateway/v1/scores/batch` (1000-entry cap) so external evaluators (RAGAS, LangSmith, DeepEval, internal judge microservices) can attach scores to existing traces/spans, persisted with `source = EXTERNAL` on the existing `AiEvalScore` entity.

**Architecture:** Reuses the existing `AiEvalScore` entity + service layer. Appends `EXTERNAL` to `AiEvalScoreSource` (ordinal 3). Adds `metadata` (JSONB) + `sourceIdentifier` (TEXT, the free-text evaluator tag like `"ragas@0.2.3"`) columns. New facade (`AiExternalScoreFacade`) handles workspace-boundary validation (403 on cross-workspace writes) and emits `ScoreRecordedEvent`. New controller wraps the facade in `automation-ai-gateway-public-rest`. Two new Micrometer counters via the established `ObjectProvider<MeterRegistry>` pattern.

**Tech Stack:** Java 25 · Spring Boot 4 · Spring Data JDBC · Liquibase · Micrometer · Testcontainers · JUnit 5 · AssertJ

**Corresponds to:** §7 of `docs/superpowers/specs/2026-04-21-ai-gateway-gaps-spec.md`

**Depends on:** Nothing hard. Independent of Spec A. Reuses `AiEvalScore`, `AiEvalScoreService`, `AiObservabilityTraceService.getTrace()`, `AiGatewayApiKeyAuthenticationProvider`.

---

## File Structure

### Modified files (existing)

```
server/ee/libs/automation/automation-ai/automation-ai-gateway/
├── automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/
│   ├── domain/AiEvalScore.java                        # add metadata + sourceIdentifier
│   ├── domain/AiEvalScoreSource.java                  # append EXTERNAL (ordinal 3)
│   └── service/AiObservabilitySpanService.java        # add getSpan(Long) method
├── automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/
│   ├── service/AiObservabilitySpanServiceImpl.java    # impl getSpan(Long)
│   └── repository/AiObservabilitySpanRepository.java  # add findById if missing
└── automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/
    └── master.xml                                     # register new changeset
```

### New files

```
server/ee/libs/automation/automation-ai/automation-ai-gateway/
├── automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/
│   ├── dto/AiExternalScoreRequest.java                # record
│   ├── dto/AiExternalScoreBatchRequest.java           # record
│   ├── dto/AiExternalScoreBatchItem.java              # record
│   ├── dto/AiExternalScoreResult.java                 # record (scoreId, accepted, reason)
│   ├── dto/AiExternalScoreBatchResult.java            # record (acceptedCount, rejectedCount, failures)
│   ├── event/AiScoreRecordedEvent.java                # ApplicationEvent (record)
│   └── facade/AiExternalScoreFacade.java              # SPI
├── automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/
│   ├── facade/AiExternalScoreFacadeImpl.java          # converts request → AiEvalScore, validates workspace boundary
│   └── exception/AiScoreWorkspaceBoundaryException.java # runtime exc → mapped to 403
├── automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/
│   └── 00000000000005_ai_eval_score_external.xml      # add metadata + source_identifier columns
├── automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/
│   └── AiExternalScoreController.java                 # 3 endpoints
└── automation-ai-gateway-remote-client/src/main/java/com/bytechef/ee/automation/ai/gateway/remote/client/facade/
    └── RemoteAiExternalScoreFacadeClient.java         # @ConditionalOnEEVersion stub
```

No new Gradle modules. All code lives in existing `-api`, `-service`, `-public-rest`, `-remote-client` submodules.

---

## Conventions

- **License:** all files under `server/ee/**` use the ByteChef Enterprise license block + `@version ee` Javadoc (never Apache 2.0).
- **Enums:** `AiEvalScoreSource` appended (`EXTERNAL` at ordinal 3), persisted as INT.
- **Metrics:** new counters via `ObjectProvider<MeterRegistry>` per the pattern in `AiObservabilityOtlpIngestFacadeImpl`.
- **Cross-workspace 403 (not 404):** per spec §12.2 resolution — surface the boundary.
- **Commit message format:** server-side convention `<short imperative summary>` + Co-Authored-By trailer.

---

## Task 1: Append `EXTERNAL` to `AiEvalScoreSource` enum + Liquibase migration for `metadata` + `source_identifier`

**Files:**
- Modify: `.../automation-ai-gateway-api/.../domain/AiEvalScoreSource.java`
- Create: `.../automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000005_ai_eval_score_external.xml`
- Modify: master changelog include

- [ ] **Step 1: Append `EXTERNAL` to the enum**

Read the existing file first; then apply:

```java
public enum AiEvalScoreSource {
    MANUAL,      // ordinal 0 — existing
    API,         // ordinal 1 — existing
    LLM_JUDGE,   // ordinal 2 — existing
    EXTERNAL     // ordinal 3 — NEW: scores from external evaluators via the Scores API
}
```

**Critical:** do NOT reorder; append only. This is enforced by `feedback_enum_storage` memory.

- [ ] **Step 2: Create the Liquibase changeset**

Write `00000000000005_ai_eval_score_external.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">

    <changeSet id="20260423000001" author="ivicac" context="ee">
        <addColumn tableName="ai_eval_score">
            <column name="metadata" type="CLOB"/>
            <column name="source_identifier" type="VARCHAR(255)"/>
        </addColumn>

        <createIndex tableName="ai_eval_score" indexName="idx_ai_eval_score_source_identifier">
            <column name="source_identifier"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

Rationale: `CLOB` is used for the `metadata` JSON blob for Postgres compatibility matching sibling changelogs. `VARCHAR(255)` for `source_identifier` matches typical evaluator-identifier lengths (`ragas@0.2.3`, `internal-judge-v1`). Index on `source_identifier` supports §7.2's "indexed for filtering" requirement.

- [ ] **Step 3: Register in master changelog**

Locate the `master.xml` or equivalent include file used by the AI gateway module (likely at `.../automation-ai-gateway-service/src/main/resources/config/liquibase/master.xml` — verify the actual path by reading the file that includes `00000000000004_ai_eval_init.xml`). Append an `<include>` for the new file.

- [ ] **Step 4: Rebuild + verify Liquibase accepts**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:build
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test
```

Integration tests under Testcontainers should still pass — the migration applies cleanly against a fresh DB.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/ \
        server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/
git commit -m "$(cat <<'EOF'
Add AiEvalScoreSource.EXTERNAL and metadata/source_identifier columns

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Extend `AiEvalScore` domain with `metadata` and `sourceIdentifier` fields

**Files:**
- Modify: `.../domain/AiEvalScore.java`

- [ ] **Step 1: Add the fields + getters/setters + builder updates**

Read `AiEvalScore.java` first. Locate the fields block. Add (preserve ordering + style from neighbouring fields):

```java
private String metadata;

private String sourceIdentifier;
```

Add matching `getMetadata() / setMetadata(String)` + `getSourceIdentifier() / setSourceIdentifier(String)` in the same alphabetical slot used by other getters/setters. Match existing Javadoc style (usually none for straightforward getters — follow what the file uses).

If the class has static factory methods (`numeric(...)`, `bool(...)`, `categorical(...)`) — add overloads that accept `metadata` and `sourceIdentifier`:

```java
public static AiEvalScore numeric(
    Long workspaceId, Long traceId, Long spanId, String name, BigDecimal value,
    String comment, AiEvalScoreSource source, String sourceIdentifier, String metadata) {
    AiEvalScore score = numeric(workspaceId, traceId, spanId, name, value, comment, source);
    score.setSourceIdentifier(sourceIdentifier);
    score.setMetadata(metadata);
    return score;
}
```

Mirror for `bool()` and `categorical()` if they exist. Keep the original 3-arg versions intact.

- [ ] **Step 2: Verify `compileJava` still passes**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava
```

- [ ] **Step 3: Add unit tests if the class has a test**

Grep for `AiEvalScoreTest.java`. If it exists, add one test that round-trips the new fields. If not, skip.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiEvalScore.java
git commit -m "$(cat <<'EOF'
Add metadata and sourceIdentifier fields to AiEvalScore

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Add `getSpan(Long)` to `AiObservabilitySpanService`

**Files:**
- Modify: `.../service/AiObservabilitySpanService.java` (interface)
- Modify: `.../service/AiObservabilitySpanServiceImpl.java`
- Modify: `.../repository/AiObservabilitySpanRepository.java` if needed

**Context:** Spec B §7.4 requires workspace-boundary validation. For span-level scores (`POST /spans/{spanId}/scores`), the facade must fetch the span, walk to its trace, and verify `trace.workspaceId == caller.workspaceId`. Today `AiObservabilitySpanService` has no `getSpan(Long)` — only `getSpansByTrace(Long)`.

- [ ] **Step 1: Add to the interface**

```java
/**
 * @throws java.util.NoSuchElementException if no span exists with the given id.
 */
AiObservabilitySpan getSpan(long id);
```

- [ ] **Step 2: Add to the impl**

`AiObservabilitySpanRepository` already extends `ListCrudRepository<AiObservabilitySpan, Long>`, so `findById(Long)` is inherited. No repository change needed.

```java
@Override
public AiObservabilitySpan getSpan(long id) {
    return aiObservabilitySpanRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("AiObservabilitySpan id=" + id + " not found"));
}
```

- [ ] **Step 3: Unit test**

Append to `AiObservabilitySpanServiceImplTest` (or the equivalent):

```java
@Test
void testGetSpanThrowsWhenMissing() {
    when(aiObservabilitySpanRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> aiObservabilitySpanServiceImpl.getSpan(99L))
        .isInstanceOf(NoSuchElementException.class);
}

@Test
void testGetSpanReturnsRow() {
    AiObservabilitySpan span = new AiObservabilitySpan(42L, AiObservabilitySpanType.GENERATION);
    ReflectionTestUtils.setField(span, "id", 7L);
    when(aiObservabilitySpanRepository.findById(7L)).thenReturn(Optional.of(span));

    assertThat(aiObservabilitySpanServiceImpl.getSpan(7L)).isSameAs(span);
}
```

- [ ] **Step 4: Verify + commit**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:check
```

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/ \
        server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/
git commit -m "$(cat <<'EOF'
Add AiObservabilitySpanService.getSpan(long) for workspace-boundary checks

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Define request/result DTOs

**Files (all new, records):**
- `.../automation-ai-gateway-api/.../dto/AiExternalScoreRequest.java`
- `.../dto/AiExternalScoreBatchRequest.java`
- `.../dto/AiExternalScoreBatchItem.java`
- `.../dto/AiExternalScoreResult.java`
- `.../dto/AiExternalScoreBatchResult.java`

- [ ] **Step 1: `AiExternalScoreRequest.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Request body for {@code POST /traces/{traceId}/scores} and {@code POST /spans/{spanId}/scores}.
 *
 * <p>{@code value} is expected to be a {@link BigDecimal} for NUMERIC / BOOLEAN (0 or 1) and a
 * {@link String} for CATEGORICAL. The controller validates this conforms before handing off.
 *
 * @version ee
 */
public record AiExternalScoreRequest(
    String name,
    Object value,
    AiEvalScoreDataType dataType,
    String comment,
    String source,
    Map<String, Object> metadata) {

    public AiExternalScoreRequest {
        Validate.isTrue(StringUtils.isNotBlank(name), "name must not be blank");
        Validate.notNull(dataType, "dataType must not be null");
        Validate.notNull(value, "value must not be null");

        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }
}
```

- [ ] **Step 2: `AiExternalScoreBatchItem.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * One row of the batch score request. Exactly one of {@code traceId} / {@code spanId} must be non-null.
 *
 * @version ee
 */
public record AiExternalScoreBatchItem(
    Long traceId,
    Long spanId,
    String name,
    Object value,
    AiEvalScoreDataType dataType,
    String comment,
    String source,
    Map<String, Object> metadata) {

    public AiExternalScoreBatchItem {
        Validate.isTrue(
            (traceId == null) != (spanId == null),
            "Exactly one of traceId or spanId must be non-null");

        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }
}
```

- [ ] **Step 3: `AiExternalScoreBatchRequest.java`**

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
 * Batch body for {@code POST /scores/batch}. Enforces a 1000-entry cap per §7.2 of the AI gateway gaps spec.
 *
 * @version ee
 */
public record AiExternalScoreBatchRequest(List<AiExternalScoreBatchItem> scores) {

    public static final int MAX_BATCH_SIZE = 1000;

    public AiExternalScoreBatchRequest {
        Validate.notNull(scores, "scores must not be null");
        Validate.isTrue(
            scores.size() <= MAX_BATCH_SIZE,
            "scores size (%d) exceeds cap (%d)", scores.size(), MAX_BATCH_SIZE);
        scores = List.copyOf(scores);
    }
}
```

- [ ] **Step 4: `AiExternalScoreResult.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

/**
 * Response for single-score endpoints. {@code rejectionReason} is null on accepted writes.
 *
 * @version ee
 */
public record AiExternalScoreResult(Long scoreId, boolean accepted, String rejectionReason) {

    public static AiExternalScoreResult accepted(Long scoreId) {
        return new AiExternalScoreResult(scoreId, true, null);
    }

    public static AiExternalScoreResult rejected(String reason) {
        return new AiExternalScoreResult(null, false, reason);
    }
}
```

- [ ] **Step 5: `AiExternalScoreBatchResult.java`**

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
 * Summary of a batch write: counts + a per-failure detail list (empty on success).
 *
 * @version ee
 */
public record AiExternalScoreBatchResult(int acceptedCount, int rejectedCount, List<String> rejectionReasons) {

    public AiExternalScoreBatchResult {
        Validate.isTrue(acceptedCount >= 0 && rejectedCount >= 0, "counts must be >= 0");
        Validate.notNull(rejectionReasons, "rejectionReasons must not be null");
        rejectionReasons = List.copyOf(rejectionReasons);
    }
}
```

- [ ] **Step 6: Add DTO test for batch cap**

```java
@Test
void testBatchRejectsOverCap() {
    List<AiExternalScoreBatchItem> items = IntStream.range(0, 1001)
        .mapToObj(i -> new AiExternalScoreBatchItem(
            1L, null, "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
        .toList();

    assertThatThrownBy(() -> new AiExternalScoreBatchRequest(items))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cap");
}
```

- [ ] **Step 7: Verify + commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:check
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/
git commit -m "$(cat <<'EOF'
Add AiExternalScoreRequest/BatchRequest/Result DTOs

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Define `AiScoreRecordedEvent` + `AiScoreWorkspaceBoundaryException`

**Files:**
- Create: `.../automation-ai-gateway-api/.../event/AiScoreRecordedEvent.java`
- Create: `.../automation-ai-gateway-service/.../exception/AiScoreWorkspaceBoundaryException.java`

- [ ] **Step 1: `AiScoreRecordedEvent.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.event;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreSource;
import java.time.Instant;

/**
 * Fired after a score write commits. Consumers include: audit log, metrics aggregators.
 * Reuses Spring's {@link org.springframework.context.ApplicationEventPublisher} pattern already used by
 * {@code AiGatewayTraceCompletedEvent}, {@code AiGatewayBudgetExceededEvent}.
 *
 * @version ee
 */
public record AiScoreRecordedEvent(
    Long scoreId,
    Long workspaceId,
    Long traceId,
    Long spanId,
    String name,
    AiEvalScoreDataType dataType,
    AiEvalScoreSource source,
    String sourceIdentifier,
    Instant occurredAt) {}
```

- [ ] **Step 2: `AiScoreWorkspaceBoundaryException.java`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.exception;

/**
 * Thrown when a score write targets a trace or span outside the caller's workspace. Mapped to HTTP 403 by
 * {@link AiGatewayExceptionHandler} — the boundary is surfaced, not hidden behind a 404 (§12.2 resolution).
 *
 * @version ee
 */
public class AiScoreWorkspaceBoundaryException extends RuntimeException {

    public AiScoreWorkspaceBoundaryException(String message) {
        super(message);
    }
}
```

- [ ] **Step 3: Verify + commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava

git add server/ee/libs/automation/automation-ai/automation-ai-gateway/
git commit -m "$(cat <<'EOF'
Add AiScoreRecordedEvent + AiScoreWorkspaceBoundaryException

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Add `AiExternalScoreFacade` SPI + `AiExternalScoreFacadeImpl`

**Files:**
- Create: `.../automation-ai-gateway-api/.../facade/AiExternalScoreFacade.java`
- Create: `.../automation-ai-gateway-service/.../facade/AiExternalScoreFacadeImpl.java`
- Test: `.../automation-ai-gateway-service/.../facade/AiExternalScoreFacadeImplTest.java`

- [ ] **Step 1: Interface (SPI)**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreResult;

/**
 * Ingests scores from external evaluators. Validates that every target trace/span belongs to the caller's
 * workspace — throws {@code AiScoreWorkspaceBoundaryException} (mapped to 403) on mismatch.
 *
 * @version ee
 */
public interface AiExternalScoreFacade {

    AiExternalScoreResult recordTraceScore(Long workspaceId, Long traceId, AiExternalScoreRequest request);

    AiExternalScoreResult recordSpanScore(Long workspaceId, Long spanId, AiExternalScoreRequest request);

    AiExternalScoreBatchResult recordBatch(Long workspaceId, AiExternalScoreBatchRequest request);
}
```

- [ ] **Step 2: Implementation — write the failing test first**

Create `AiExternalScoreFacadeImplTest.java` (in the `-service` test directory):

```java
package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpanType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.automation.ai.gateway.event.AiScoreRecordedEvent;
import com.bytechef.ee.automation.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalScoreService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySpanService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 */
class AiExternalScoreFacadeImplTest {

    @Test
    void testRecordTraceScorePersistsAndEmitsEvent() {
        AiEvalScoreService evalScoreService = mock(AiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(42L, AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace, "id", 1L);
        when(traceService.getTrace(1L)).thenReturn(trace);

        AiEvalScore persistedScore = new AiEvalScore();
        ReflectionTestUtils.setField(persistedScore, "id", 99L);
        when(evalScoreService.create(any(AiEvalScore.class))).thenReturn(persistedScore);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            evalScoreService, traceService, spanService, publisher, staticProvider(new SimpleMeterRegistry()));

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", new BigDecimal("0.87"), AiEvalScoreDataType.NUMERIC,
            null, "ragas@0.2.3", Map.of("run_id", "abc"));

        AiExternalScoreResult result = facade.recordTraceScore(42L, 1L, request);

        assertThat(result.accepted()).isTrue();
        assertThat(result.scoreId()).isEqualTo(99L);

        verify(publisher).publishEvent(any(AiScoreRecordedEvent.class));
    }

    @Test
    void testRecordTraceScoreRejectsCrossWorkspace() {
        AiEvalScoreService evalScoreService = mock(AiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(99L, AiObservabilityTraceSource.API);  // workspace 99
        ReflectionTestUtils.setField(trace, "id", 1L);
        when(traceService.getTrace(1L)).thenReturn(trace);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            evalScoreService, traceService, spanService,
            mock(ApplicationEventPublisher.class), staticProvider(new SimpleMeterRegistry()));

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of());

        assertThatThrownBy(() -> facade.recordTraceScore(42L, 1L, request))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class);
    }

    @Test
    void testRecordSpanScoreUsesSpanTraceWorkspace() {
        AiEvalScoreService evalScoreService = mock(AiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        AiObservabilitySpan span = new AiObservabilitySpan(7L, AiObservabilitySpanType.GENERATION);
        ReflectionTestUtils.setField(span, "id", 5L);
        when(spanService.getSpan(5L)).thenReturn(span);

        AiObservabilityTrace trace = new AiObservabilityTrace(42L, AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace, "id", 7L);
        when(traceService.getTrace(7L)).thenReturn(trace);

        AiEvalScore persistedScore = new AiEvalScore();
        ReflectionTestUtils.setField(persistedScore, "id", 100L);
        when(evalScoreService.create(any(AiEvalScore.class))).thenReturn(persistedScore);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            evalScoreService, traceService, spanService, publisher, staticProvider(new SimpleMeterRegistry()));

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of());

        AiExternalScoreResult result = facade.recordSpanScore(42L, 5L, request);

        assertThat(result.accepted()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> staticProvider(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        return provider;
    }
}
```

Run the test → it fails because `AiExternalScoreFacadeImpl` doesn't exist yet.

- [ ] **Step 3: Implement the facade**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchItem;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.automation.ai.gateway.event.AiScoreRecordedEvent;
import com.bytechef.ee.automation.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalScoreService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySpanService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Component
public class AiExternalScoreFacadeImpl implements AiExternalScoreFacade {

    private static final String METRIC_RECORDED = "bytechef_ai_score_recorded";
    private static final String METRIC_VALUE = "bytechef_ai_score_value";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiEvalScoreService aiEvalScoreService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MeterRegistry meterRegistry;

    public AiExternalScoreFacadeImpl(
        AiEvalScoreService aiEvalScoreService,
        AiObservabilityTraceService aiObservabilityTraceService,
        AiObservabilitySpanService aiObservabilitySpanService,
        ApplicationEventPublisher applicationEventPublisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.aiEvalScoreService = aiEvalScoreService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    @Transactional
    public AiExternalScoreResult recordTraceScore(Long workspaceId, Long traceId, AiExternalScoreRequest request) {
        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(traceId);

        assertSameWorkspace(workspaceId, trace.getWorkspaceId(), "trace " + traceId);

        AiEvalScore score = buildScore(workspaceId, traceId, null, request);

        return persistAndEmit(score, request, workspaceId);
    }

    @Override
    @Transactional
    public AiExternalScoreResult recordSpanScore(Long workspaceId, Long spanId, AiExternalScoreRequest request) {
        AiObservabilitySpan span = aiObservabilitySpanService.getSpan(spanId);
        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(span.getTraceId());

        assertSameWorkspace(workspaceId, trace.getWorkspaceId(), "span " + spanId);

        AiEvalScore score = buildScore(workspaceId, span.getTraceId(), spanId, request);

        return persistAndEmit(score, request, workspaceId);
    }

    @Override
    @Transactional
    public AiExternalScoreBatchResult recordBatch(Long workspaceId, AiExternalScoreBatchRequest request) {
        int accepted = 0;
        int rejected = 0;
        List<String> reasons = new ArrayList<>();

        for (AiExternalScoreBatchItem item : request.scores()) {
            AiExternalScoreRequest itemRequest = new AiExternalScoreRequest(
                item.name(), item.value(), item.dataType(), item.comment(), item.source(), item.metadata());

            try {
                if (item.traceId() != null) {
                    recordTraceScore(workspaceId, item.traceId(), itemRequest);
                } else {
                    recordSpanScore(workspaceId, item.spanId(), itemRequest);
                }

                accepted++;
            } catch (AiScoreWorkspaceBoundaryException | IllegalArgumentException exception) {
                rejected++;
                reasons.add(
                    (item.traceId() != null ? "trace " + item.traceId() : "span " + item.spanId())
                        + ": " + exception.getMessage());
            }
        }

        return new AiExternalScoreBatchResult(accepted, rejected, reasons);
    }

    private AiEvalScore buildScore(Long workspaceId, Long traceId, Long spanId, AiExternalScoreRequest request) {
        AiEvalScore score = new AiEvalScore();

        score.setWorkspaceId(workspaceId);
        score.setTraceId(traceId);

        if (spanId != null) {
            score.setSpanId(spanId);
        }

        score.setName(request.name());
        score.setComment(request.comment());
        score.setDataType(request.dataType());
        score.setSource(AiEvalScoreSource.EXTERNAL);
        score.setSourceIdentifier(request.source());
        score.setMetadata(serializeMetadata(request.metadata()));

        Object value = request.value();

        switch (request.dataType()) {
            case NUMERIC -> score.setValue(toBigDecimal(value));
            case BOOLEAN -> score.setValue(toBigDecimal(value));
            case CATEGORICAL -> score.setStringValue(String.valueOf(value));
        }

        return score;
    }

    private AiExternalScoreResult persistAndEmit(AiEvalScore score, AiExternalScoreRequest request, Long workspaceId) {
        AiEvalScore persisted = aiEvalScoreService.create(score);

        applicationEventPublisher.publishEvent(new AiScoreRecordedEvent(
            persisted.getId(),
            workspaceId,
            persisted.getTraceId(),
            persisted.getSpanId(),
            persisted.getName(),
            persisted.getDataType(),
            persisted.getSource(),
            persisted.getSourceIdentifier(),
            Instant.now()));

        incrementCounters(persisted, request);

        return AiExternalScoreResult.accepted(persisted.getId());
    }

    private void assertSameWorkspace(Long callerWorkspaceId, Long targetWorkspaceId, String targetLabel) {
        if (!callerWorkspaceId.equals(targetWorkspaceId)) {
            throw new AiScoreWorkspaceBoundaryException(
                "Workspace " + callerWorkspaceId + " cannot write scores to " + targetLabel
                    + " (belongs to workspace " + targetWorkspaceId + ")");
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }

        return new BigDecimal(String.valueOf(value));
    }

    private static String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException jsonProcessingException) {
            throw new IllegalArgumentException("metadata is not serializable as JSON", jsonProcessingException);
        }
    }

    private void incrementCounters(AiEvalScore score, AiExternalScoreRequest request) {
        if (meterRegistry == null) {
            return;
        }

        Counter.builder(METRIC_RECORDED)
            .tag("source", score.getSource().name())
            .tag("data_type", score.getDataType().name())
            .tag("workspace", String.valueOf(score.getWorkspaceId()))
            .register(meterRegistry)
            .increment();

        if (score.getDataType() == AiEvalScoreDataType.NUMERIC && score.getValue() != null) {
            DistributionSummary.builder(METRIC_VALUE)
                .tag("name", score.getName())
                .register(meterRegistry)
                .record(score.getValue().doubleValue());
        }
    }
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --tests AiExternalScoreFacadeImplTest
```

Expected: 3 tests pass.

- [ ] **Step 5: Full check + commit**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:check
```

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/ \
        server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/ \
        server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/
git commit -m "$(cat <<'EOF'
Add AiExternalScoreFacade for external score ingestion

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Add `AiExternalScoreController` with 3 endpoints

**Files:**
- Create: `.../automation-ai-gateway-public-rest/.../AiExternalScoreController.java`
- Test: `.../automation-ai-gateway-public-rest/src/test/.../AiExternalScoreControllerTest.java`

- [ ] **Step 1: Write the failing `@WebMvcTest`**

```java
package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.automation.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.automation.ai.gateway.facade.AiExternalScoreFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 */
@WebMvcTest(AiExternalScoreController.class)
class AiExternalScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiExternalScoreFacade aiExternalScoreFacade;

    @Test
    void testRecordTraceScoreReturns200() throws Exception {
        when(aiExternalScoreFacade.recordTraceScore(eq(42L), eq(1L), any()))
            .thenReturn(AiExternalScoreResult.accepted(99L));

        mockMvc.perform(post("/api/ai-gateway/v1/traces/1/scores")
            .header("X-ByteChef-Workspace-Id", "42")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "faithfulness",
                    "value": 0.87,
                    "dataType": "NUMERIC",
                    "source": "ragas@0.2.3"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scoreId").value(99))
            .andExpect(jsonPath("$.accepted").value(true));

        verify(aiExternalScoreFacade).recordTraceScore(eq(42L), eq(1L), any());
    }

    @Test
    void testRecordSpanScoreReturns200() throws Exception {
        when(aiExternalScoreFacade.recordSpanScore(eq(42L), eq(5L), any()))
            .thenReturn(AiExternalScoreResult.accepted(101L));

        mockMvc.perform(post("/api/ai-gateway/v1/spans/5/scores")
            .header("X-ByteChef-Workspace-Id", "42")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "relevance",
                    "value": 1,
                    "dataType": "BOOLEAN",
                    "source": "internal-judge"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scoreId").value(101));
    }

    @Test
    void testReturns400WhenWorkspaceHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/ai-gateway/v1/traces/1/scores")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "faithfulness",
                    "value": 0.87,
                    "dataType": "NUMERIC",
                    "source": "ragas"
                }
                """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testReturns403OnCrossWorkspace() throws Exception {
        when(aiExternalScoreFacade.recordTraceScore(any(), any(), any()))
            .thenThrow(new AiScoreWorkspaceBoundaryException("cross-workspace"));

        mockMvc.perform(post("/api/ai-gateway/v1/traces/1/scores")
            .header("X-ByteChef-Workspace-Id", "42")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "faithfulness",
                    "value": 0.87,
                    "dataType": "NUMERIC",
                    "source": "ragas"
                }
                """))
            .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Implement the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.automation.ai.gateway.facade.AiExternalScoreFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * External Scores API — attaches scores to existing traces/spans from third-party evaluators.
 *
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
class AiExternalScoreController {

    private final AiExternalScoreFacade aiExternalScoreFacade;

    AiExternalScoreController(AiExternalScoreFacade aiExternalScoreFacade) {
        this.aiExternalScoreFacade = aiExternalScoreFacade;
    }

    @PostMapping("/traces/{traceId}/scores")
    public ResponseEntity<?> recordTraceScore(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("traceId") Long traceId,
        @RequestBody AiExternalScoreRequest request) {

        Long workspaceId = parseWorkspaceId(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        return ResponseEntity.ok(aiExternalScoreFacade.recordTraceScore(workspaceId, traceId, request));
    }

    @PostMapping("/spans/{spanId}/scores")
    public ResponseEntity<?> recordSpanScore(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("spanId") Long spanId,
        @RequestBody AiExternalScoreRequest request) {

        Long workspaceId = parseWorkspaceId(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        return ResponseEntity.ok(aiExternalScoreFacade.recordSpanScore(workspaceId, spanId, request));
    }

    @PostMapping("/scores/batch")
    public ResponseEntity<?> recordBatch(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @RequestBody AiExternalScoreBatchRequest request) {

        Long workspaceId = parseWorkspaceId(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        AiExternalScoreBatchResult result = aiExternalScoreFacade.recordBatch(workspaceId, request);

        return ResponseEntity.ok(result);
    }

    private static Long parseWorkspaceId(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException numberFormatException) {
            return null;
        }
    }

    private static ResponseEntity<?> missingWorkspaceResponse() {
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", "missing_workspace_id",
                "message", "Request must include header 'X-ByteChef-Workspace-Id' with a numeric workspace id."));
    }
}
```

- [ ] **Step 3: Add 403 handler to `AiGatewayExceptionHandler`**

Read the existing handler. Append:

```java
@ExceptionHandler(AiScoreWorkspaceBoundaryException.class)
public ResponseEntity<Map<String, String>> handleWorkspaceBoundary(AiScoreWorkspaceBoundaryException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "workspace_boundary", "message", exception.getMessage()));
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:test --tests AiExternalScoreControllerTest
```

Expected: 4 tests pass.

- [ ] **Step 5: Full check + commit**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:check
```

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/
git commit -m "$(cat <<'EOF'
Add AiExternalScoreController — POST /traces|spans/{id}/scores + /scores/batch

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Add EE remote-client stub

**Files:**
- Create: `.../automation-ai-gateway-remote-client/.../RemoteAiExternalScoreFacadeClient.java`

- [ ] **Step 1: Read a sibling stub for exact style**

Ref: `RemoteAiObservabilityOtlpIngestFacadeClient.java` — previously added in Spec A. Match its conventions.

- [ ] **Step 2: Write the stub**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.remote.client.facade;

import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.automation.ai.gateway.facade.AiExternalScoreFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAiExternalScoreFacadeClient implements AiExternalScoreFacade {

    @Override
    public AiExternalScoreResult recordTraceScore(Long workspaceId, Long traceId, AiExternalScoreRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiExternalScoreResult recordSpanScore(Long workspaceId, Long spanId, AiExternalScoreRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiExternalScoreBatchResult recordBatch(Long workspaceId, AiExternalScoreBatchRequest request) {
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 3: Verify + commit**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-remote-client:check
```

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-remote-client/
git commit -m "$(cat <<'EOF'
Add RemoteAiExternalScoreFacadeClient remote-client stub

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: Add facade-level integration test (real Postgres)

**Files:**
- Create: `.../automation-ai-gateway-service/src/test/java/.../facade/AiExternalScoreFacadeIntTest.java`

Mirror `AiObservabilityOtlpIngestFacadeIntTest` — use `@Import(AiExternalScoreFacadeImpl.class)`, use `@AiGatewayIntTest` or whatever scaffolding the sibling tests use, Testcontainers Postgres.

- [ ] **Step 1: Write the integration test**

The test should:
1. Seed a workspace + trace (reuse existing `AiObservabilityTraceService.create`).
2. Call `facade.recordTraceScore(workspaceId, traceId, request)` with realistic data.
3. Assert a row landed in `ai_eval_score` with source = EXTERNAL, correct `sourceIdentifier`, correct `metadata` JSON.
4. Assert a `AiScoreRecordedEvent` was published (use `ApplicationEvents` or a `@SpyBean`/`@MockitoBean` on the publisher — match the sibling int test style).
5. Cross-workspace rejection: seed a trace in workspace A, call `recordTraceScore(B, ...)` — expect `AiScoreWorkspaceBoundaryException`.

Structure the same way as `AiObservabilityOtlpIngestFacadeIntTest` at `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/` — read it first, use its annotations and test-config imports as the base. Do NOT invent a new int-test config.

- [ ] **Step 2: Run + verify**

```
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:testIntegration --tests AiExternalScoreFacadeIntTest
```

Expected: 2 tests pass (happy path + cross-workspace rejection).

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/AiExternalScoreFacadeIntTest.java
git commit -m "$(cat <<'EOF'
Add integration test for AiExternalScoreFacade

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: Document + final verification

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/README-external-scores.md`

- [ ] **Step 1: Write the README**

Structure matches the OTLP README from Spec A:
- Endpoint summary
- Request body schema (`name`, `value`, `dataType`, `source`, `metadata`)
- 3 endpoint examples (trace, span, batch)
- Error responses (400 missing workspace, 403 cross-workspace, 404 trace/span not found)
- Configuration notes
- Example `curl` commands

- [ ] **Step 2: Full verification across all touched modules**

```bash
./gradlew spotlessApply
./gradlew \
  :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:check \
  :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:check \
  :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:check \
  :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-remote-client:check
```

Expected: BUILD SUCCESSFUL across all 4 modules.

- [ ] **Step 3: Commit README**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/README-external-scores.md
git commit -m "$(cat <<'EOF'
Document External Scores API endpoints

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review

**Spec coverage (§7):**

| Requirement | Task |
|-------------|------|
| §7.2 — 3 endpoints (trace, span, batch) | 7 |
| §7.2 — 1000-entry batch cap | 4 |
| §7.2 — request body shape (name, value, dataType, comment, source, metadata) | 4 |
| §7.3 — `EXTERNAL` source ordinal (append-only) | 1 |
| §7.3 — reuse `AiEvalScore` | 2, 6 |
| §7.4 — auth via Bearer API key (existing filter) | inherited — endpoint covered by existing `/api/ai-gateway/v1/**` matcher |
| §7.4 — 403 on cross-workspace | 5, 7 |
| §7.5 — `ai.score.recorded` audit event | 5, 6 |
| §7.6 — `bytechef_ai_score_recorded` counter | 6 |
| §7.6 — `bytechef_ai_score_value` distribution summary | 6 |
| Remote-client stub | 8 |
| Integration test | 9 |
| Documentation | 10 |

**Placeholder scan:** no TBDs, no "handle edge cases", no "add validation" — all steps carry exact code or a specific command + expected output.

**Type consistency:** record names (`AiExternalScoreRequest`, `AiExternalScoreBatchRequest`, `AiExternalScoreResult`, `AiExternalScoreBatchResult`, `AiExternalScoreBatchItem`), facade method names (`recordTraceScore`, `recordSpanScore`, `recordBatch`), exception name (`AiScoreWorkspaceBoundaryException`), event name (`AiScoreRecordedEvent`) are referenced consistently across Tasks 4–9.

**Deviations from spec:**
- §7.5 says "Reuse existing `ConnectionAuditAspect` pattern". AI gateway already uses `ApplicationEventPublisher.publishEvent()` for audit-adjacent events (see `AiGatewayTraceCompletedEvent`). Following the gateway pattern instead of importing an aspect-based one from `platform-connection` — cleaner.
- §7.2 body has both `source` (free-text evaluator identifier) and the enum concept of `EXTERNAL` source. Disambiguated by naming: DB column `source_identifier` stores `"ragas@0.2.3"`; enum stays `source`.
- Used `X-ByteChef-Workspace-Id` header for workspace resolution (consistent with Spec A). Auth for Spec B still relies on the existing API-key filter, which doesn't populate workspace — same design constraint.
