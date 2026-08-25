# Temporal Task-Output Type Preservation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A value that was temporal when a task returned it is temporal when a later expression reads it, so workflow authors never call `parseDate` on something that was never a string.

**Architecture:** Task outputs and job contexts are persisted as untyped JSON, which erases `java.time` and `java.sql` temporal types to strings. A codec in `commons-util` tags temporal values on write and reconstructs them on read, normalizing every instant-bearing type to `ZonedDateTime` at UTC — the type `parseDate` already returns, and therefore the one that makes comparisons evaluate. The same normalization is applied to the synchronous in-memory cache so sync and async agree. The structured `dateTime` condition operand is fixed in the same change because it re-flattens the reconstructed value with `MapUtils.getString`.

**Tech Stack:** Java 25, Spring Boot 4, Gradle, JUnit 5, Jackson (via `JsonUtils`), SpEL (via `SpelEvaluator`).

**Spec:** `docs/superpowers/specs/2026-08-25-temporal-task-output-type-preservation-design.md`

## Global Constraints

- Java 25. Existing code style: one blank line before control statements, one blank line between a variable modification and the statement using it, no trailing blank line before a class's closing brace.
- No inline code comments explaining rationale — rationale goes in the commit message. Javadoc on a new public type is fine.
- Descriptive variable names. No single letters, no abbreviations (`exception` not `e`, `zonedDateTime` not `zdt`).
- Unit test classes end in `Test`, integration test classes end in `IntTest`. Test method names are camelCase with no underscores; the rule applies to private helpers too.
- Run `./gradlew spotlessApply` before every commit. Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep for `^> Task .* FAILED`.
- Commit message convention: `5575 <description>` for server-side changes.
- Reconstruction zone is **UTC**, always. Never the JVM default zone.

---

### Task 1: `TemporalValueUtils.normalize`

Recursively converts instant-bearing temporal values to `ZonedDateTime` at UTC, leaving everything else alone. This is the shared operation both the durable codec (Task 3) and the sync cache (Task 4) apply, so it lands first and on its own.

**Files:**
- Create: `server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/TemporalValueUtils.java`
- Test: `server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/TemporalValueUtilsTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `public static @Nullable Object TemporalValueUtils.normalize(@Nullable Object value)`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TemporalValueUtilsTest {

    private static final ZonedDateTime EXPECTED = ZonedDateTime.parse("2026-08-26T00:00:00Z");

    @Test
    public void testNormalizeInstantBearingTypesToUtcZonedDateTime() {
        assertEquals(EXPECTED, TemporalValueUtils.normalize(Timestamp.from(Instant.parse("2026-08-26T00:00:00Z"))));
        assertEquals(EXPECTED, TemporalValueUtils.normalize(Instant.parse("2026-08-26T00:00:00Z")));
        assertEquals(EXPECTED, TemporalValueUtils.normalize(java.util.Date.from(Instant.parse("2026-08-26T00:00:00Z"))));
        assertEquals(EXPECTED, TemporalValueUtils.normalize(OffsetDateTime.parse("2026-08-26T02:00:00+02:00")));
        assertEquals(EXPECTED, TemporalValueUtils.normalize(ZonedDateTime.parse("2026-08-26T02:00:00+02:00")));
    }

    @Test
    public void testNormalizeLeavesValuesThatFixNoInstant() {
        assertEquals(LocalDate.of(2026, 8, 26), TemporalValueUtils.normalize(LocalDate.of(2026, 8, 26)));
        assertEquals(
            LocalDateTime.of(2026, 8, 26, 0, 0), TemporalValueUtils.normalize(LocalDateTime.of(2026, 8, 26, 0, 0)));
        assertEquals(LocalTime.of(10, 30), TemporalValueUtils.normalize(LocalTime.of(10, 30)));
    }

    @Test
    public void testNormalizeMapsSqlDateAndTimeToTheirOwnTypes() {
        assertEquals(LocalDate.of(2026, 8, 26), TemporalValueUtils.normalize(java.sql.Date.valueOf("2026-08-26")));
        assertEquals(LocalTime.of(10, 30), TemporalValueUtils.normalize(java.sql.Time.valueOf("10:30:00")));
    }

    @Test
    public void testNormalizeRecursesIntoListsAndMaps() {
        Object normalized = TemporalValueUtils.normalize(
            List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")))));

        assertEquals(List.of(Map.of("APPLYDATE", EXPECTED)), normalized);
    }

    @Test
    public void testNormalizeLeavesNonTemporalValuesUntouched() {
        assertEquals("2026-08-26T00:00:00.000Z", TemporalValueUtils.normalize("2026-08-26T00:00:00.000Z"));
        assertEquals(42, TemporalValueUtils.normalize(42));
        assertNull(TemporalValueUtils.normalize(null));
    }

    @Test
    public void testNormalizeIsIdempotent() {
        Object once = TemporalValueUtils.normalize(Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        assertEquals(once, TemporalValueUtils.normalize(once));
    }

    @Test
    public void testNormalizePreservesTheInstantWhenChangingZone() {
        ZonedDateTime normalized = (ZonedDateTime) TemporalValueUtils.normalize(
            ZonedDateTime.parse("2026-08-26T02:00:00+02:00"));

        assertEquals(ZoneOffset.UTC, normalized.getZone());
        assertEquals(Instant.parse("2026-08-26T00:00:00Z"), normalized.toInstant());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:core:commons:commons-util:test --tests '*TemporalValueUtilsTest' > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED|error:" /tmp/t.log`
Expected: FAIL — `TemporalValueUtils` does not exist, compilation error.

- [ ] **Step 3: Write minimal implementation**

```java
package com.bytechef.commons.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Normalizes temporal values so that every value fixing a point on the timeline is represented as a
 * {@link ZonedDateTime} at UTC, which is the type {@code parseDate} returns and therefore the one workflow
 * expressions can compare against.
 *
 * @author Ivica Cardic
 */
public final class TemporalValueUtils {

    private TemporalValueUtils() {
    }

    public static @Nullable Object normalize(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case java.sql.Date sqlDate -> sqlDate.toLocalDate();
            case java.sql.Time sqlTime -> sqlTime.toLocalTime();
            case Date date -> date.toInstant()
                .atZone(ZoneOffset.UTC);
            case Instant instant -> instant.atZone(ZoneOffset.UTC);
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant()
                .atZone(ZoneOffset.UTC);
            case ZonedDateTime zonedDateTime -> zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
            case LocalDate localDate -> localDate;
            case LocalDateTime localDateTime -> localDateTime;
            case LocalTime localTime -> localTime;
            case OffsetTime offsetTime -> offsetTime;
            case Map<?, ?> map -> normalizeMap(map);
            case List<?> list -> normalizeList(list);
            default -> value;
        };
    }

    private static Map<String, @Nullable Object> normalizeMap(Map<?, ?> map) {
        Map<String, @Nullable Object> normalizedMap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            normalizedMap.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
        }

        return normalizedMap;
    }

    private static List<@Nullable Object> normalizeList(List<?> list) {
        List<@Nullable Object> normalizedList = new ArrayList<>(list.size());

        for (Object item : list) {
            normalizedList.add(normalize(item));
        }

        return normalizedList;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:core:commons:commons-util:spotlessApply && ./gradlew :server:libs:core:commons:commons-util:test --tests '*TemporalValueUtilsTest' > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/TemporalValueUtils.java server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/TemporalValueUtilsTest.java
git commit -m "5575 Normalize instant-bearing temporal values to ZonedDateTime at UTC"
```

---

### Task 2: `TemporalValueUtils.tag` / `untag`

The JSON tag format. `tag` normalizes first, so only five reconstructed types ever reach disk and `untag` has five cases rather than nine.

**Files:**
- Modify: `server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/TemporalValueUtils.java`
- Test: `server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/TemporalValueUtilsTest.java`

**Interfaces:**
- Consumes: `TemporalValueUtils.normalize` from Task 1.
- Produces: `public static @Nullable Object tag(@Nullable Object value)` and `public static @Nullable Object untag(@Nullable Object value)`. Tag shape: a two-entry `Map` with keys `@bytechefType` (one of `ZONED_DATE_TIME`, `LOCAL_DATE`, `LOCAL_DATE_TIME`, `LOCAL_TIME`, `OFFSET_TIME`) and `@bytechefValue` (the ISO-8601 text).

- [ ] **Step 1: Write the failing test**

Append to `TemporalValueUtilsTest`:

```java
    @Test
    public void testTagRoundTripsEveryReconstructedType() {
        List<Object> values = List.of(
            ZonedDateTime.parse("2026-08-26T00:00:00Z"), LocalDate.of(2026, 8, 26),
            LocalDateTime.of(2026, 8, 26, 0, 0), LocalTime.of(10, 30),
            java.time.OffsetTime.parse("10:30:00+02:00"));

        for (Object value : values) {
            assertEquals(value, TemporalValueUtils.untag(TemporalValueUtils.tag(value)), String.valueOf(value));
        }
    }

    @Test
    public void testTagNormalizesBeforeTagging() {
        Object tagged = TemporalValueUtils.tag(Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        assertEquals(
            Map.of("@bytechefType", "ZONED_DATE_TIME", "@bytechefValue", "2026-08-26T00:00:00Z"), tagged);
        assertEquals(ZonedDateTime.parse("2026-08-26T00:00:00Z"), TemporalValueUtils.untag(tagged));
    }

    @Test
    public void testTagRecursesIntoListsAndMaps() {
        Object tagged = TemporalValueUtils.tag(
            List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")))));

        assertEquals(
            List.of(Map.of("APPLYDATE", ZonedDateTime.parse("2026-08-26T00:00:00Z"))),
            TemporalValueUtils.untag(tagged));
    }

    @Test
    public void testUntagLeavesUntaggedDataUnchanged() {
        Object legacy = List.of(Map.of("APPLYDATE", "2026-08-26T00:00:00.000Z"));

        assertEquals(legacy, TemporalValueUtils.untag(legacy));
    }

    @Test
    public void testUntagTreatsLookalikeMapsAsPlainData() {
        Map<String, Object> unknownType = Map.of("@bytechefType", "NOT_A_TYPE", "@bytechefValue", "x");
        Map<String, Object> extraKey = Map.of(
            "@bytechefType", "ZONED_DATE_TIME", "@bytechefValue", "2026-08-26T00:00:00Z", "extra", 1);
        Map<String, Object> unparseableValue = Map.of(
            "@bytechefType", "ZONED_DATE_TIME", "@bytechefValue", "not-a-date");

        assertEquals(unknownType, TemporalValueUtils.untag(unknownType));
        assertEquals(extraKey, TemporalValueUtils.untag(extraKey));
        assertEquals(unparseableValue, TemporalValueUtils.untag(unparseableValue));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:core:commons:commons-util:test --tests '*TemporalValueUtilsTest' > /tmp/t.log 2>&1; echo $?; grep -E "cannot find symbol|^> Task .* FAILED" /tmp/t.log`
Expected: FAIL — `tag`/`untag` do not exist.

- [ ] **Step 3: Write minimal implementation**

Add to `TemporalValueUtils` (imports: `java.time.format.DateTimeParseException`, `java.util.function.Function`):

```java
    private static final String TYPE_KEY = "@bytechefType";
    private static final String VALUE_KEY = "@bytechefValue";

    private static final Map<String, Function<String, Object>> PARSERS = Map.of(
        "ZONED_DATE_TIME", ZonedDateTime::parse,
        "LOCAL_DATE", LocalDate::parse,
        "LOCAL_DATE_TIME", LocalDateTime::parse,
        "LOCAL_TIME", LocalTime::parse,
        "OFFSET_TIME", OffsetTime::parse);

    public static @Nullable Object tag(@Nullable Object value) {
        return tagNormalized(normalize(value));
    }

    public static @Nullable Object untag(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Map<?, ?> map -> untagMap(map);
            case List<?> list -> {
                List<@Nullable Object> untaggedList = new ArrayList<>(list.size());

                for (Object item : list) {
                    untaggedList.add(untag(item));
                }

                yield untaggedList;
            }
            default -> value;
        };
    }

    private static @Nullable Object tagNormalized(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case ZonedDateTime zonedDateTime -> tagOf("ZONED_DATE_TIME", zonedDateTime.toString());
            case LocalDate localDate -> tagOf("LOCAL_DATE", localDate.toString());
            case LocalDateTime localDateTime -> tagOf("LOCAL_DATE_TIME", localDateTime.toString());
            case LocalTime localTime -> tagOf("LOCAL_TIME", localTime.toString());
            case OffsetTime offsetTime -> tagOf("OFFSET_TIME", offsetTime.toString());
            case Map<?, ?> map -> {
                Map<String, @Nullable Object> taggedMap = new LinkedHashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    taggedMap.put(String.valueOf(entry.getKey()), tagNormalized(entry.getValue()));
                }

                yield taggedMap;
            }
            case List<?> list -> {
                List<@Nullable Object> taggedList = new ArrayList<>(list.size());

                for (Object item : list) {
                    taggedList.add(tagNormalized(item));
                }

                yield taggedList;
            }
            default -> value;
        };
    }

    private static Map<String, Object> tagOf(String type, String value) {
        Map<String, Object> tag = new LinkedHashMap<>();

        tag.put(TYPE_KEY, type);
        tag.put(VALUE_KEY, value);

        return tag;
    }

    private static Object untagMap(Map<?, ?> map) {
        Object reconstructed = reconstruct(map);

        if (reconstructed != null) {
            return reconstructed;
        }

        Map<String, @Nullable Object> untaggedMap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            untaggedMap.put(String.valueOf(entry.getKey()), untag(entry.getValue()));
        }

        return untaggedMap;
    }

    private static @Nullable Object reconstruct(Map<?, ?> map) {
        if (map.size() != 2 || !map.containsKey(TYPE_KEY) || !map.containsKey(VALUE_KEY)) {
            return null;
        }

        if (!(map.get(TYPE_KEY) instanceof String type) || !(map.get(VALUE_KEY) instanceof String value)) {
            return null;
        }

        Function<String, Object> parser = PARSERS.get(type);

        if (parser == null) {
            return null;
        }

        try {
            return parser.apply(value);
        } catch (DateTimeParseException dateTimeParseException) {
            return null;
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:core:commons:commons-util:spotlessApply && ./gradlew :server:libs:core:commons:commons-util:check > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/TemporalValueUtils.java server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/TemporalValueUtilsTest.java
git commit -m "5575 Add a JSON tag format for temporal values"
```

---

### Task 3: Wire the codec into the durable storage

**Files:**
- Modify: `server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/src/main/java/com/bytechef/atlas/file/storage/TaskFileStorageImpl.java:60-106`
- Test: `server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/src/test/java/com/bytechef/atlas/file/storage/TaskFileStorageTest.java`

**Interfaces:**
- Consumes: `TemporalValueUtils.tag` / `untag` from Task 2.
- Produces: no signature change. `readTaskExecutionOutput`, `readContextValue` and `readJobOutputs` now return reconstructed temporals for anything written after this change.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.atlas.file.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
public class TaskFileStorageTest {

    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

    @Test
    public void testTaskExecutionOutputPreservesTemporalValues() {
        Object output = List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z"))));

        FileEntry fileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 1L, output);

        assertEquals(
            List.of(Map.of("APPLYDATE", ZonedDateTime.parse("2026-08-26T00:00:00Z"))),
            taskFileStorage.readTaskExecutionOutput(fileEntry));
    }

    @Test
    public void testTaskExecutionOutputLeavesStringsAsStrings() {
        Object output = Map.of("latestModifyDate", "2026-08-24T22:00:00Z");

        FileEntry fileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 2L, output);

        assertEquals(output, taskFileStorage.readTaskExecutionOutput(fileEntry));
    }

    @Test
    public void testContextValuePreservesTemporalValues() {
        Map<String, ?> context = Map.of("dbDate", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        FileEntry fileEntry = taskFileStorage.storeContextValue(1L, Context.Classname.JOB, context);

        assertEquals(
            Map.of("dbDate", ZonedDateTime.parse("2026-08-26T00:00:00Z")),
            taskFileStorage.readContextValue(fileEntry));
    }

    @Test
    public void testJobOutputsPreserveTemporalValues() {
        Map<String, ?> outputs = Map.of("result", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        FileEntry fileEntry = taskFileStorage.storeJobOutputs(1L, outputs);

        assertEquals(
            Map.of("result", ZonedDateTime.parse("2026-08-26T00:00:00Z")),
            taskFileStorage.readJobOutputs(fileEntry));
    }
}
```

Add to `server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/build.gradle.kts`:

```kotlin
    testImplementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    testImplementation(project(":server:libs:test:test-support"))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:atlas:atlas-file-storage:atlas-file-storage-impl:test --tests '*TaskFileStorageTest' > /tmp/t.log 2>&1; echo $?; grep -E "expected:|^> Task .* FAILED" /tmp/t.log`
Expected: FAIL — the temporal assertions get the ISO `String` back instead of a `ZonedDateTime`.

- [ ] **Step 3: Write minimal implementation**

In `TaskFileStorageImpl`, add `import com.bytechef.commons.util.TemporalValueUtils;` and wrap the three read methods and the four store methods:

```java
    @Override
    public Map<String, ?> readContextValue(FileEntry fileEntry) {
        Map<String, ?> value = JsonUtils.read(
            CompressionUtils.decompressToString(fileStorageService.readFileToBytes(CONTEXT_FILES_DIR, fileEntry)),
            new TypeReference<>() {});

        return (Map<String, ?>) TemporalValueUtils.untag(value);
    }

    @Override
    public Map<String, ?> readJobOutputs(FileEntry fileEntry) {
        Map<String, ?> value = JsonUtils.read(
            CompressionUtils.decompressToString(fileStorageService.readFileToBytes(JOB_FILES_DIR, fileEntry)),
            new TypeReference<>() {});

        return (Map<String, ?>) TemporalValueUtils.untag(value);
    }

    @Override
    public Object readTaskExecutionOutput(FileEntry fileEntry) {
        Object value = JsonUtils.read(
            CompressionUtils.decompressToString(
                fileStorageService.readFileToBytes(TASK_EXECUTION_FILES_DIR, fileEntry)),
            Object.class);

        return TemporalValueUtils.untag(value);
    }
```

and in each store method replace the serialized argument, e.g.:

```java
    @Override
    public FileEntry storeTaskExecutionOutput(long jobId, long taskExecutionId, Object output) {
        return fileStorageService.storeFileContent(
            TASK_EXECUTION_FILES_DIR, taskExecutionId + ".json",
            CompressionUtils.compress(JsonUtils.write(TemporalValueUtils.tag(output))));
    }
```

Apply the same `TemporalValueUtils.tag(...)` wrap in `storeContextValue` (both overloads) and `storeJobOutputs`. Add `@SuppressWarnings("unchecked")` to the two `Map` read methods for the cast.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:atlas:atlas-file-storage:atlas-file-storage-impl:spotlessApply && ./gradlew :server:libs:atlas:atlas-file-storage:atlas-file-storage-impl:check > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/atlas/atlas-file-storage/atlas-file-storage-impl
git commit -m "5575 Preserve temporal types across task-output and context storage"
```

---

### Task 4: Normalize on the synchronous cache path

`InMemoryTaskFileStorage` caches the original object, so sync executions never cross JSON. Without this task, a `Timestamp` reaches expressions in test mode while a `ZonedDateTime` reaches them in production.

**Files:**
- Modify: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/file/storage/InMemoryTaskFileStorage.java`
- Test: `server/libs/platform/platform-job-sync/src/test/java/com/bytechef/platform/job/sync/file/storage/InMemoryTaskFileStorageTest.java`

**Interfaces:**
- Consumes: `TemporalValueUtils.normalize` from Task 1; `TaskFileStorageImpl` from Task 3 as the durable delegate.
- Produces: no signature change.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.job.sync.file.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
public class InMemoryTaskFileStorageTest {

    private final TaskFileStorage taskFileStorage = new InMemoryTaskFileStorage(
        new TaskFileStorageImpl(new Base64FileStorageService()));

    @Test
    public void testCachedReadReturnsTheSameTypeAsTheDurableRead() {
        Object output = List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z"))));

        FileEntry fileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 1L, output);

        assertEquals(
            List.of(Map.of("APPLYDATE", ZonedDateTime.parse("2026-08-26T00:00:00Z"))),
            taskFileStorage.readTaskExecutionOutput(fileEntry));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-job-sync:test --tests '*InMemoryTaskFileStorageTest' > /tmp/t.log 2>&1; echo $?; grep -E "expected:|^> Task .* FAILED" /tmp/t.log`
Expected: FAIL — the cache returns the original `Timestamp`, not a `ZonedDateTime`.

- [ ] **Step 3: Write minimal implementation**

Add `import com.bytechef.commons.util.TemporalValueUtils;` and normalize inside the private `cache` helper so every store path is covered by one edit:

```java
    private void cache(FileEntry fileEntry, @Nullable Object value) {
        if (value != null) {
            jobDataStorage.put(fileEntry.getUrl(), TemporalValueUtils.normalize(value));
        }
    }
```

If the existing `cache` helper has a different shape, keep its shape and wrap only the value being put.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-job-sync:spotlessApply && ./gradlew :server:libs:platform:platform-job-sync:check > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-job-sync
git commit -m "5575 Normalize temporal values on the synchronous cache path"
```

---

### Task 5: `Parse` accepts already-temporal arguments

Without this, every workflow written against the old string behaviour — including the documented #5575 workaround `parseDate(${oracleSql_1[0].APPLYDATE}, "yyyy-MM-dd'T'HH:mm:ss.SSSX")` — starts throwing `ClassCastException`.

**Files:**
- Modify: `server/libs/core/evaluator/evaluator-impl/src/main/java/com/bytechef/evaluator/Parse.java:44-59`
- Test: `server/libs/core/evaluator/evaluator-impl/src/test/java/com/bytechef/evaluator/SpelEvaluatorTest.java`

**Interfaces:**
- Consumes: `TemporalValueUtils.normalize` from Task 1.
- Produces: no signature change. `parseDate`/`parseDateTime` accept a `String`, a `Temporal` or a `java.util.Date`.

- [ ] **Step 1: Write the failing test**

Append to `SpelEvaluatorTest`:

```java
    @Test
    public void testParseDateAcceptsAnAlreadyTemporalArgument() {
        Map<String, Object> context = Map.of("dbDate", java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"));

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDate(${dbDate}, \"yyyy-MM-dd'T'HH:mm:ss.SSSX\")"), context);

        assertEquals(java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"), MapUtils.get(map, "value"));
    }

    @Test
    public void testParseDateStillAcceptsAString() {
        Map<String, Object> context = Map.of("dbDate", "2026-08-26T00:00:00.000Z");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDate(${dbDate}, \"yyyy-MM-dd'T'HH:mm:ss.SSSX\")"), context);

        assertEquals(java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"), MapUtils.get(map, "value"));
    }

    @Test
    public void testParseDateTimeAcceptsAnAlreadyTemporalArgument() {
        Map<String, Object> context = Map.of("dbDate", java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"));

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("value", "=parseDateTime(${dbDate})"), context);

        assertEquals(LocalDateTime.of(2026, 8, 26, 0, 0), MapUtils.get(map, "value"));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:core:evaluator:evaluator-impl:test --tests '*SpelEvaluatorTest' > /tmp/t.log 2>&1; echo $?; grep -E "ClassCastException|^> Task .* FAILED" /tmp/t.log`
Expected: FAIL with `ClassCastException: class java.time.ZonedDateTime cannot be cast to class java.lang.String`.

- [ ] **Step 3: Write minimal implementation**

Replace the body of `Parse.execute`:

```java
    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        Object argument = TemporalValueUtils.normalize(arguments[0]);

        if (argument instanceof ZonedDateTime zonedDateTime) {
            return new TypedValue(
                type == Type.DATE ? zonedDateTime : zonedDateTime.toLocalDateTime());
        }

        if (argument instanceof LocalDateTime localDateTime) {
            return new TypedValue(
                type == Type.DATE ? localDateTime.atZone(ZoneOffset.UTC) : localDateTime);
        }

        if (argument instanceof LocalDate localDate) {
            return new TypedValue(
                type == Type.DATE ? localDate : localDate.atStartOfDay());
        }

        String text = (String) argument;

        if (type == Type.DATE) {
            if (arguments.length == 2) {
                return new TypedValue(ZonedDateTime.parse(text, DateTimeFormatter.ofPattern((String) arguments[1])));
            }

            return new TypedValue(LocalDate.parse(text));
        }

        if (arguments.length == 2) {
            return new TypedValue(LocalDateTime.parse(text, DateTimeFormatter.ofPattern((String) arguments[1])));
        }

        return new TypedValue(LocalDateTime.parse(text));
    }
```

Add imports `com.bytechef.commons.util.TemporalValueUtils` and `java.time.ZoneOffset`. Add `implementation(project(":server:libs:core:commons:commons-util"))` to `server/libs/core/evaluator/evaluator-impl/build.gradle.kts` if it is not already present.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:core:evaluator:evaluator-impl:spotlessApply && ./gradlew :server:libs:core:evaluator:evaluator-impl:check > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/core/evaluator/evaluator-impl
git commit -m "5575 Let parseDate and parseDateTime accept already-temporal arguments"
```

---

### Task 6: Consumer audit

Roughly 30 classes read through `readTaskExecutionOutput` / `readContextValue` and may now see a `ZonedDateTime` where they saw a `String`. This task finds and fixes any that would break. It is a real task with a mechanical procedure, not a review note.

**Files:**
- Modify: whichever consumers the procedure identifies (expected: none, or very few)
- Test: add a regression test beside any consumer that needed a change

**Interfaces:**
- Consumes: Tasks 3 and 4.
- Produces: nothing new.

- [ ] **Step 1: Enumerate the consumers**

```bash
grep -rn "readTaskExecutionOutput\|readContextValue" server/libs server/ee --include=*.java | grep -v "/test/" > /tmp/consumers.txt
wc -l /tmp/consumers.txt
```

Expected: about 69 lines. Record the list.

- [ ] **Step 2: Classify each consumer**

For each file in the list, read the code that touches the returned value and put it in one bucket:

- **Serializes to JSON for HTTP, a DTO, or a model tool result** — safe. Jackson renders `ZonedDateTime` as an ISO-8601 string, so the client payload is unchanged. Expected for `ProjectWorkflowExecutionFacadeImpl`, `IntegrationWorkflowExecutionFacadeImpl`, `SseStreamApplicationEventListener`, `AutomationMcpToolFacade`, `AutomationA2AServerFacade`, `EmbeddedMcpToolFacade`.
- **Puts the value into an evaluation context** — safe and intended. Expected for `DefaultTaskCompletionHandler` and the task dispatchers.
- **Casts to `String`, calls a `String` method, or passes it to something typed `String`** — **must be fixed**. Fix by accepting `Object` and calling `String.valueOf(...)` at the point text is genuinely required.

- [ ] **Step 3: Prove the risky bucket is empty or fixed**

```bash
cut -d: -f1 /tmp/consumers.txt | sort -u | while read -r file; do
  grep -Hn "(String)\|\.equals(\|\.startsWith(\|\.contains(\|String\.valueOf" "$file" | grep -i "output\|context" || true
done
```

Inspect every hit by hand. Fix any that would receive a task-output or context value.

- [ ] **Step 4: Run the full check on the touched modules**

Run: `./gradlew --continue :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:check :server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-service:check :server:libs:platform:platform-coordinator:check > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add -u
git commit -m "5575 Fix consumers assuming task-output values are strings"
```

If nothing needed changing, skip the commit and record that in the task notes instead of committing an empty change.

---

### Task 7: Sample flow — a temporal output compares without `parseDate`

The reported workflow, end to end on the real atlas engine. The IntTest harness wires `TaskFileStorageImpl` directly, so the JSON round-trip really happens.

**Files:**
- Create: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-test-int-support/src/main/java/com/bytechef/platform/workflow/task/dispatcher/test/task/handler/TestTemporalTaskHandler.java`
- Create: `server/libs/modules/task-dispatchers/condition/src/test/resources/workflows/condition_v1-temporalOutput-noParseDate.yaml`
- Modify: `server/libs/modules/task-dispatchers/condition/src/test/java/com/bytechef/task/dispatcher/condition/ConditionTaskDispatcherIntTest.java`

**Interfaces:**
- Consumes: Tasks 3 and 5.
- Produces: `TestTemporalTaskHandler` — a `TaskHandler<Object>` returning `Timestamp.from(Instant.parse(<the "value" parameter>))`, for reuse by Tasks 8, 11 and 12.

- [ ] **Step 1: Write the test handler and the sample flow**

`TestTemporalTaskHandler.java`:

```java
package com.bytechef.platform.workflow.task.dispatcher.test.task.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/**
 * Returns a {@link Timestamp} so integration tests can exercise a task whose output is genuinely temporal, the way a
 * JDBC query action's {@code rs.getObject} result is.
 *
 * @author Ivica Cardic
 */
public class TestTemporalTaskHandler implements TaskHandler<Object> {

    @Override
    public Object handle(TaskExecution taskExecution) {
        Map<String, ?> parameters = taskExecution.getParameters();

        return Timestamp.from(Instant.parse((String) parameters.get("value")));
    }
}
```

`condition_v1-temporalOutput-noParseDate.yaml`:

```yaml
---
label: "If Task Temporal Output Without parseDate"
inputs:
- name: "dbDate"
  type: "string"
  required: true
- name: "restDate"
  type: "string"
  required: true
tasks:
- name: "dbDate"
  type: "temporal/v1/set"
  parameters:
    value: "${dbDate}"
- name: "condition_1"
  type: "condition/v1"
  parameters:
    rawExpression: true
    expression: "=${dbDate} > parseDate(${restDate}, \"yyyy-MM-dd'T'HH:mm:ssX\")"
    caseTrue:
    - name: "comparisonResult"
      type: "var/v1/set"
      parameters:
        value: "true branch"
    caseFalse:
    - name: "comparisonResult"
      type: "var/v1/set"
      parameters:
        value: "false branch"
```

Add the test method to `ConditionTaskDispatcherIntTest`:

```java
    @Test
    public void testDispatchTemporalOutputComparesWithoutParseDate() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-temporalOutput-noParseDate".getBytes(StandardCharsets.UTF_8)),
            Map.of("dbDate", "2026-08-26T00:00:00Z", "restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("comparisonResult"));

        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-temporalOutput-noParseDate".getBytes(StandardCharsets.UTF_8)),
            Map.of("dbDate", "2026-08-23T00:00:00Z", "restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("comparisonResult"));
    }
```

and extend the handler map:

```java
    private Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of("var/v1/set", testVarTaskHandler, "temporal/v1/set", new TestTemporalTaskHandler());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run this before Task 3 is applied. If you are already past Task 3, prove the test is meaningful by temporarily restoring the pre-change file, running the test, then restoring your version:

```bash
git show <commit-before-task-3>:server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/src/main/java/com/bytechef/atlas/file/storage/TaskFileStorageImpl.java > server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/src/main/java/com/bytechef/atlas/file/storage/TaskFileStorageImpl.java
# run the test, observe the failure, then:
git checkout -- server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/src/main/java/com/bytechef/atlas/file/storage/TaskFileStorageImpl.java
```

**Never run `git stash` in this repository.** The stash is shared across every worktree of this clone, so stashing here can destroy another session's uncommitted work.

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:testIntegration --tests '*ConditionTaskDispatcherIntTest' > /tmp/t.log 2>&1; echo $?; grep -E "EL1013E|^> Task .* FAILED" /tmp/t.log`
Expected before the fix: FAIL mentioning `Cannot compare instances of`.

- [ ] **Step 3: No implementation needed**

The behaviour comes from Tasks 3 and 5. If the test fails here, the defect is in those tasks, not in this one.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:spotlessApply && ./gradlew :server:libs:modules:task-dispatchers:condition:testIntegration --tests '*ConditionTaskDispatcherIntTest' > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-test-int-support server/libs/modules/task-dispatchers/condition
git commit -m "5575 Add a sample flow comparing a temporal output without parseDate"
```

---

### Task 8: Sample flow — an ISO-8601 string stays a string

The other half of the requirement. Without this, nothing stops a later change from converting every date-shaped string and breaking the legitimate `parseDate` on the Liferay value.

**Files:**
- Create: `server/libs/modules/task-dispatchers/condition/src/test/resources/workflows/condition_v1-temporalOutput-stringStaysString.yaml`
- Modify: `server/libs/modules/task-dispatchers/condition/src/test/java/com/bytechef/task/dispatcher/condition/ConditionTaskDispatcherIntTest.java`

**Interfaces:**
- Consumes: Task 7's handler map wiring.
- Produces: nothing.

- [ ] **Step 1: Write the sample flow and the test**

`condition_v1-temporalOutput-stringStaysString.yaml`:

```yaml
---
label: "If Task String Output Stays A String"
inputs:
- name: "restDate"
  type: "string"
  required: true
tasks:
- name: "restDate"
  type: "var/v1/set"
  parameters:
    value: "${restDate}"
- name: "condition_1"
  type: "condition/v1"
  parameters:
    rawExpression: true
    expression: "=parseDate(${restDate}, \"yyyy-MM-dd'T'HH:mm:ssX\") > parseDate('2026-08-24T22:00:00Z', \"yyyy-MM-dd'T'HH:mm:ssX\")"
    caseTrue:
    - name: "stringResult"
      type: "var/v1/set"
      parameters:
        value: "true branch"
    caseFalse:
    - name: "stringResult"
      type: "var/v1/set"
      parameters:
        value: "false branch"
```

```java
    @Test
    public void testDispatchStringOutputStaysAString() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-temporalOutput-stringStaysString".getBytes(StandardCharsets.UTF_8)),
            Map.of("restDate", "2026-08-26T00:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("stringResult"));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:testIntegration --tests '*testDispatchStringOutputStaysAString*' > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected before the flow exists: FAIL — workflow not found.

- [ ] **Step 3: No implementation needed**

This is a regression guard over behaviour Tasks 3 and 5 must already have.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:testIntegration --tests '*ConditionTaskDispatcherIntTest' > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/modules/task-dispatchers/condition
git commit -m "5575 Add a sample flow pinning that string outputs stay strings"
```

---

### Task 9: `DateTimeOperandParser`

The parser the structured `dateTime` condition operand needs. Separate from `TemporalValueUtils` because this one parses arbitrary user and API text leniently, whereas the codec only ever reads text it wrote itself.

**Files:**
- Create: `server/libs/modules/task-dispatchers/condition/src/main/java/com/bytechef/task/dispatcher/condition/util/DateTimeOperandParser.java`
- Test: `server/libs/modules/task-dispatchers/condition/src/test/java/com/bytechef/task/dispatcher/condition/util/DateTimeOperandParserTest.java`

**Interfaces:**
- Consumes: `TemporalValueUtils.normalize` from Task 1.
- Produces: `static ZonedDateTime DateTimeOperandParser.parse(@Nullable Object operand, String operandName)`. Throws `IllegalArgumentException` naming `operandName` and showing the value when it cannot parse.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.task.dispatcher.condition.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

public class DateTimeOperandParserTest {

    private static final ZonedDateTime EXPECTED = ZonedDateTime.parse("2026-08-26T00:00:00Z");

    @Test
    public void testParseOffsetBearingStrings() {
        assertEquals(EXPECTED, DateTimeOperandParser.parse("2026-08-26T00:00:00Z", "value1"));
        assertEquals(EXPECTED, DateTimeOperandParser.parse("2026-08-26T00:00:00.000Z", "value1"));
        assertEquals(EXPECTED, DateTimeOperandParser.parse("2026-08-26T02:00:00+02:00", "value1"));
    }

    @Test
    public void testParseZoneLessValuesAsUtc() {
        assertEquals(EXPECTED, DateTimeOperandParser.parse("2026-08-26T00:00:00", "value1"));
        assertEquals(EXPECTED, DateTimeOperandParser.parse("2026-08-26", "value1"));
    }

    @Test
    public void testParseAlreadyTemporalOperands() {
        assertEquals(EXPECTED, DateTimeOperandParser.parse(EXPECTED, "value1"));
        assertEquals(EXPECTED, DateTimeOperandParser.parse(Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")), "value1"));
    }

    @Test
    public void testParseFailsNamingTheOperandAndValue() {
        IllegalArgumentException emptyException = assertThrows(
            IllegalArgumentException.class, () -> DateTimeOperandParser.parse("", "value1"));
        IllegalArgumentException unparseableException = assertThrows(
            IllegalArgumentException.class, () -> DateTimeOperandParser.parse("not-a-date", "value2"));
        IllegalArgumentException nullException = assertThrows(
            IllegalArgumentException.class, () -> DateTimeOperandParser.parse(null, "value1"));

        assertTrue(emptyException.getMessage().contains("value1"));
        assertTrue(unparseableException.getMessage().contains("value2"));
        assertTrue(unparseableException.getMessage().contains("not-a-date"));
        assertTrue(nullException.getMessage().contains("value1"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:test --tests '*DateTimeOperandParserTest' > /tmp/t.log 2>&1; echo $?; grep -E "cannot find symbol|^> Task .* FAILED" /tmp/t.log`
Expected: FAIL — `DateTimeOperandParser` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.bytechef.task.dispatcher.condition.util;

import com.bytechef.commons.util.TemporalValueUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.jspecify.annotations.Nullable;

/**
 * Parses a {@code dateTime} condition operand into a {@link ZonedDateTime} at UTC. Operands reach this class either as
 * a reconstructed temporal value or as text from an API response or the editor's date picker.
 *
 * @author Ivica Cardic
 */
final class DateTimeOperandParser {

    private DateTimeOperandParser() {
    }

    static ZonedDateTime parse(@Nullable Object operand, String operandName) {
        Object normalized = TemporalValueUtils.normalize(operand);

        if (normalized instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime;
        }

        if (normalized instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneOffset.UTC);
        }

        if (normalized instanceof LocalDate localDate) {
            return localDate.atStartOfDay(ZoneOffset.UTC);
        }

        if (normalized instanceof String text && !text.isBlank()) {
            return parseText(text, operandName);
        }

        throw new IllegalArgumentException(
            "Condition operand " + operandName + " is not a date: " + normalized);
    }

    private static ZonedDateTime parseText(String text, String operandName) {
        try {
            return ZonedDateTime.parse(text);
        } catch (DateTimeParseException zonedException) {
            try {
                return LocalDateTime.parse(text)
                    .atZone(ZoneOffset.UTC);
            } catch (DateTimeParseException localDateTimeException) {
                try {
                    return LocalDate.parse(text)
                        .atStartOfDay(ZoneOffset.UTC);
                } catch (DateTimeParseException localDateException) {
                    throw new IllegalArgumentException(
                        "Condition operand " + operandName + " is not a date: " + text, localDateException);
                }
            }
        }
    }
}
```

Add `implementation(project(":server:libs:core:commons:commons-util"))` to `server/libs/modules/task-dispatchers/condition/build.gradle.kts` if absent. Because the class is package-private, the test must live in package `com.bytechef.task.dispatcher.condition.util`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:spotlessApply && ./gradlew :server:libs:modules:task-dispatchers:condition:test --tests '*DateTimeOperandParserTest' > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/modules/task-dispatchers/condition
git commit -m "5575 Add a lenient parser for dateTime condition operands"
```

---

### Task 10: Wire the operand parser into `ConditionTaskUtils`

**Files:**
- Modify: `server/libs/modules/task-dispatchers/condition/src/main/java/com/bytechef/task/dispatcher/condition/util/ConditionTaskUtils.java:163-180`
- Test: `server/libs/modules/task-dispatchers/condition/src/test/java/com/bytechef/task/dispatcher/condition/util/ConditionTaskUtilsTest.java`

**Interfaces:**
- Consumes: `DateTimeOperandParser.parse` from Task 9.
- Produces: no signature change. `resolveCase` handles `dateTime` operands that are offset-bearing strings or reconstructed temporals.

- [ ] **Step 1: Write the failing test**

Append to `ConditionTaskUtilsTest` (follow the file's existing helper for building a `TaskExecution`; if it has none, build one with `TaskExecution.builder().workflowTask(new WorkflowTask(Map.of(...))).build()` matching the pattern used by the existing `dateTime` tests):

```java
    @Test
    public void testResolveCaseWithOffsetBearingDateTimeOperands() {
        assertTrue(
            resolveDateTimeCase("AFTER", "2026-08-26T00:00:00.000Z", "2026-08-24T22:00:00Z"));
        assertFalse(
            resolveDateTimeCase("AFTER", "2026-08-23T00:00:00.000Z", "2026-08-24T22:00:00Z"));
    }

    @Test
    public void testResolveCaseWithAlreadyTemporalDateTimeOperands() {
        assertTrue(
            resolveDateTimeCase(
                "AFTER", ZonedDateTime.parse("2026-08-26T00:00:00Z"), ZonedDateTime.parse("2026-08-24T22:00:00Z")));
    }

    @Test
    public void testResolveCaseWithZoneLessDateTimeOperandsIsUnchanged() {
        assertTrue(resolveDateTimeCase("AFTER", "2026-08-26T00:00:00", "2026-08-24T22:00:00"));
        assertTrue(resolveDateTimeCase("BEFORE", "2026-08-24T22:00:00", "2026-08-26T00:00:00"));
    }
```

with a private helper in the same class, built on the file's existing `buildConditionsTask(List<List<Map<String, ?>>>)` helper at `ConditionTaskUtilsTest.java:201` rather than a second task builder:

```java
    private static boolean resolveDateTimeCase(String operation, Object value1, Object value2) {
        return ConditionTaskUtils.resolveCase(
            buildConditionsTask(
                List.of(
                    List.of(
                        Map.of(
                            "type", "dateTime", "operation", operation, "value1", value1, "value2", value2)))));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:test --tests '*ConditionTaskUtilsTest' > /tmp/t.log 2>&1; echo $?; grep -E "DateTimeParseException|^> Task .* FAILED" /tmp/t.log`
Expected: FAIL with `DateTimeParseException` on the offset-bearing operands.

- [ ] **Step 3: Write minimal implementation**

In `ConditionTaskUtils.getConditionExpressions`, read the operands untyped and parse with the new class:

```java
            Object value1 = MapUtils.get(condition, ConditionTaskDispatcherConstants.VALUE_1);
            Object value2 = MapUtils.get(condition, ConditionTaskDispatcherConstants.VALUE_2);

            String replacement1;
            String replacement2;

            if (operandType.equals(ConditionTaskDispatcherConstants.DATE_TIME)) {
                String variableName1 = "dt" + variables.size();
                String variableName2 = "dt" + (variables.size() + 1);

                variables.put(variableName1, DateTimeOperandParser.parse(value1, "value1"));
                variables.put(variableName2, DateTimeOperandParser.parse(value2, "value2"));

                replacement1 = "#" + variableName1;
                replacement2 = "#" + variableName2;
            } else if (operandType.equals(ConditionTaskDispatcherConstants.STRING)) {
                replacement1 = EncodingUtils.urlEncode(String.valueOf(value1 == null ? "" : value1));
                replacement2 = EncodingUtils.urlEncode(String.valueOf(value2 == null ? "" : value2));
            } else {
                replacement1 = String.valueOf(value1 == null ? "" : value1);
                replacement2 = String.valueOf(value2 == null ? "" : value2);
            }
```

The `dateTime` templates are unchanged — `ZonedDateTime` has both `isAfter` and `isBefore`, and they remain instance methods, so `SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods()` still resolves them.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:spotlessApply && ./gradlew :server:libs:modules:task-dispatchers:condition:check > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines. Every pre-existing `ConditionTaskUtilsTest` case must still pass — that is the no-regression claim.

- [ ] **Step 5: Commit**

```bash
git add server/libs/modules/task-dispatchers/condition
git commit -m "5575 Accept offset-bearing and temporal dateTime condition operands"
```

---

### Task 11: Sample flow — structured `dateTime` condition over task outputs

**Files:**
- Create: `server/libs/modules/task-dispatchers/condition/src/test/resources/workflows/condition_v1-dateTime-temporalOutput.yaml`
- Modify: `server/libs/modules/task-dispatchers/condition/src/test/java/com/bytechef/task/dispatcher/condition/ConditionTaskDispatcherIntTest.java`

**Interfaces:**
- Consumes: Tasks 3, 9 and 10; `TestTemporalTaskHandler` from Task 7.
- Produces: nothing.

- [ ] **Step 1: Write the sample flow and the test**

`condition_v1-dateTime-temporalOutput.yaml`:

```yaml
---
label: "If Task Date Time Condition Over Task Outputs"
inputs:
- name: "dbDate"
  type: "string"
  required: true
- name: "restDate"
  type: "string"
  required: true
tasks:
- name: "dbDate"
  type: "temporal/v1/set"
  parameters:
    value: "${dbDate}"
- name: "restDate"
  type: "var/v1/set"
  parameters:
    value: "${restDate}"
- name: "condition_1"
  type: "condition/v1"
  parameters:
    conditions:
    - - type: "dateTime"
        value1: "${dbDate}"
        operation: "AFTER"
        value2: "${restDate}"
    caseTrue:
    - name: "dateTimeResult"
      type: "var/v1/set"
      parameters:
        value: "true branch"
    caseFalse:
    - name: "dateTimeResult"
      type: "var/v1/set"
      parameters:
        value: "false branch"
```

```java
    @Test
    public void testDispatchDateTimeConditionOverTaskOutputs() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-dateTime-temporalOutput".getBytes(StandardCharsets.UTF_8)),
            Map.of("dbDate", "2026-08-26T00:00:00Z", "restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("dateTimeResult"));

        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-dateTime-temporalOutput".getBytes(StandardCharsets.UTF_8)),
            Map.of("dbDate", "2026-08-23T00:00:00Z", "restDate", "2026-08-24T22:00:00Z"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("dateTimeResult"));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run this before Task 10 is applied. If you are already past it, restore the pre-change `ConditionTaskUtils.java` with `git show <commit-before-task-10>:<path> > <path>`, run the test, then `git checkout -- <path>`. Never use `git stash` here.

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:testIntegration --tests '*testDispatchDateTimeConditionOverTaskOutputs*' > /tmp/t.log 2>&1; echo $?; grep -E "DateTimeParseException|^> Task .* FAILED" /tmp/t.log`
Expected before the fix: FAIL with `DateTimeParseException`.

- [ ] **Step 3: No implementation needed**

The behaviour comes from Tasks 9 and 10.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:modules:task-dispatchers:condition:testIntegration --tests '*ConditionTaskDispatcherIntTest' > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/modules/task-dispatchers/condition
git commit -m "5575 Add a sample flow for the dateTime condition over task outputs"
```

---

### Task 12: The client payload is unchanged

Pins the claim the consumer audit rests on: a stored `Timestamp` reaches an HTTP consumer as the same ISO-8601 text it does today.

**Files:**
- Create: `server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/src/test/java/com/bytechef/atlas/file/storage/TaskFileStorageSerializationTest.java`

**Interfaces:**
- Consumes: Task 3.
- Produces: nothing.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.atlas.file.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
public class TaskFileStorageSerializationTest {

    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

    @Test
    public void testReSerializedOutputMatchesWhatTheClientReceivedBefore() {
        Object output = List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z"))));

        FileEntry fileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 1L, output);

        assertEquals(
            "[{\"APPLYDATE\":\"2026-08-26T00:00:00Z\"}]",
            JsonUtils.write(taskFileStorage.readTaskExecutionOutput(fileEntry)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:atlas:atlas-file-storage:atlas-file-storage-impl:test --tests '*TaskFileStorageSerializationTest' > /tmp/t.log 2>&1; echo $?; grep -E "expected:|^> Task .* FAILED" /tmp/t.log`
Expected: FAIL, with the assertion message revealing the exact serialized form. Then decide by what the actual value is:

- **Tag keys (`@bytechefType`) appear** — the codec is leaking into the read path. Fix Task 3; do not touch this test.
- **Only the fractional-second precision differs** (`00:00:00Z` versus `00:00:00.000Z`) — pin the expected value to the observed one and state the observed string in the commit message. This is Jackson's rendering, not a defect.
- **The field is absent, reordered, or a different shape** — stop and investigate before changing anything.

- [ ] **Step 3: No implementation needed**

If the tag leaks into the serialized form, fix `TaskFileStorageImpl` rather than weakening this test.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:atlas:atlas-file-storage:atlas-file-storage-impl:check > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/atlas/atlas-file-storage/atlas-file-storage-impl
git commit -m "5575 Pin the serialized form a client receives for a temporal output"
```

---

### Task 13: Full verification

**Files:** none.

**Interfaces:**
- Consumes: every prior task.
- Produces: nothing.

- [ ] **Step 1: Format everything**

Run: `./gradlew spotlessApply > /tmp/t.log 2>&1; echo $?`
Expected: exit 0.

- [ ] **Step 2: Compile the whole server**

Run: `./gradlew compileJava compileTestJava --continue > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 3: Run the unit suite**

Run: `./gradlew check --continue > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: no FAILED lines other than the pre-existing `WorkflowNodeDynamicPropertiesFacadeTest > testGetClusterElementDynamicPropertiesEvaluatesParametersLeniently` failure, which is unrelated to this work and fails identically on a clean tree.

- [ ] **Step 4: Run the integration suite for the touched dispatchers**

Run: `./gradlew --continue :server:libs:modules:task-dispatchers:condition:testIntegration :server:libs:platform:platform-job-sync:test > /tmp/t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t.log`
Expected: exit 0, no FAILED lines.

- [ ] **Step 5: Report**

State which suites ran, the exit codes observed, and any failure that was judged pre-existing along with the evidence for that judgement. Do not claim completion without pasting the command output.

---

### Task 14: Extract the codec and extend it to numeric types

Splits `tag`/`untag` out of `TemporalValueUtils` — which would otherwise misdescribe itself — and adds numeric types. `normalize` stays where it is, temporal-only, because `InMemoryTaskFileStorage` and `Parse` consume it and numbers need no normalization.

**Files:**
- Create: `server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/ValueTagUtils.java`
- Modify: `server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/TemporalValueUtils.java` — remove `tag`, `untag`, `tagNormalized`, `tagOf`, `untagMap`, `reconstruct`, `TYPE_KEY`, `VALUE_KEY`, `PARSERS`. Keep `normalize` and its private helpers exactly as they are.
- Create: `server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/ValueTagUtilsTest.java`
- Modify: `server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/TemporalValueUtilsTest.java` — move the tag/untag tests into `ValueTagUtilsTest`, leave the `normalize` tests
- Modify: `server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/src/main/java/com/bytechef/atlas/file/storage/TaskFileStorageImpl.java` — `TemporalValueUtils.tag/untag` become `ValueTagUtils.tag/untag`

**Interfaces:**
- Consumes: `TemporalValueUtils.normalize`.
- Produces: `public static @Nullable Object ValueTagUtils.tag(@Nullable Object value)` and `untag(@Nullable Object value)`. Same two-key shape as before: `@bytechefType` / `@bytechefValue`.

**Type names and their round-trip functions.** The five temporal names are unchanged. Six numeric names are added:

| `@bytechefType` | Java type | written with | read with |
|---|---|---|---|
| `BIG_DECIMAL` | `java.math.BigDecimal` | `toPlainString()` | `new BigDecimal(text)` |
| `BIG_INTEGER` | `java.math.BigInteger` | `toString()` | `new BigInteger(text)` |
| `LONG` | `Long` | `toString()` | `Long.parseLong(text)` |
| `FLOAT` | `Float` | `toString()` | `Float.parseFloat(text)` |
| `SHORT` | `Short` | `toString()` | `Short.parseShort(text)` |
| `BYTE` | `Byte` | `toString()` | `Byte.parseByte(text)` |

`BigDecimal` uses `toPlainString()` rather than `toString()` so a large or small scale is never written in scientific notation, and so scale is preserved — `new BigDecimal("1.10")` and `new BigDecimal("1.1")` are NOT `.equals()`, and the round-trip must keep them distinct.

`Integer`, `Double`, `Boolean` and `String` are deliberately NOT tagged: they already round-trip as themselves, and tagging them would grow every payload for nothing.

**`normalize` must NOT be changed to touch numbers.** It exists to make temporal values mutually comparable. `tag` still calls it first, so temporal values are still normalized before tagging; numeric values pass through it untouched and are tagged as-is.

- [ ] **Step 1: Write the failing test**

Create `ValueTagUtilsTest` covering, at minimum: a round-trip case per numeric type above; that `BigDecimal("1.10")` survives with its scale intact and is NOT equal to `BigDecimal("1.1")`; that `Integer`, `Double`, `Boolean` and `String` are returned untagged by `tag`; that numbers nested inside `Map` and `List` round-trip; that untagged legacy JSON is unchanged by `untag`; and that a map with a known type name but an unparseable value (`{"@bytechefType": "LONG", "@bytechefValue": "not-a-number"}`) is returned as plain data rather than throwing.

Move the existing tag/untag tests from `TemporalValueUtilsTest` into this class unchanged — they must keep passing verbatim.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:core:commons:commons-util:test --tests '*ValueTagUtilsTest' > /tmp/t14.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t14.log`
Expected: FAIL — `ValueTagUtils` does not exist.

- [ ] **Step 3: Write minimal implementation**

Move the six private members listed above into `ValueTagUtils` unchanged, then extend two places:

- `tagNormalized` gains a case per numeric type, each producing `tagOf(<name>, <written form>)`.
- `PARSERS` gains an entry per numeric type using the read function above.

**One correctness detail that will bite otherwise:** `reconstruct` currently catches only `DateTimeParseException`. `Long.parseLong`, `new BigDecimal(...)` and the rest throw `NumberFormatException`, which is not a `DateTimeParseException`. Broaden the catch so an unparseable tagged value is returned as plain data for numeric types too, exactly as it already is for temporal ones. Do not let a malformed stored value throw out of `untag`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:core:commons:commons-util:spotlessApply && ./gradlew --continue :server:libs:core:commons:commons-util:check :server:libs:atlas:atlas-file-storage:atlas-file-storage-impl:check :server:libs:platform:platform-job-sync:check :server:libs:core:evaluator:evaluator-impl:check > /tmp/t14.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t14.log`
Expected: exit 0, no FAILED lines. All four modules must pass — the extraction changes a class three of them depend on.

- [ ] **Step 5: Commit**

```bash
git add server/libs/core/commons/commons-util server/libs/atlas/atlas-file-storage/atlas-file-storage-impl
git commit -m "5575 Extract the value tag codec and extend it to numeric types"
```

---

### Task 15: Consumer audit for numeric types

The audit in Task 6 cleared temporal types only. Tagging numbers changes what consumers receive in the opposite direction — a `Long` that used to arrive as `Integer` now arrives as `Long`, a `BigDecimal` that arrived as `Double` now arrives as `BigDecimal` — so a consumer casting to `Integer` or `Double` breaks where it did not before.

**Files:** whichever consumers the procedure identifies (expected: few, possibly none)

**Interfaces:**
- Consumes: Task 14.
- Produces: nothing.

- [ ] **Step 1: Enumerate**

```bash
grep -rn "readTaskExecutionOutput\|readContextValue\|readJobOutputs" server/libs server/ee --include=*.java | grep -v "/test/" | cut -d: -f1 | sort -u > /tmp/consumers15.txt
wc -l < /tmp/consumers15.txt
```
Expected: 34 files, matching Task 6's enumeration.

- [ ] **Step 2: Search for numeric assumptions**

For each file, look for anything that would now receive a different box type:
```bash
while read -r file; do
  grep -Hn "(Integer)\|(Double)\|(Number)\|intValue()\|doubleValue()\|instanceof Integer\|instanceof Double" "$file" || true
done < /tmp/consumers15.txt
```
Inspect every hit by hand and decide whether the value it touches can come from a storage read. `((Number) value).intValue()` is SAFE — `Long`, `BigDecimal` and `Integer` are all `Number`. `(Integer) value` is BROKEN if the value can now be a `Long`. `(Double) value` is BROKEN if it can now be a `BigDecimal`.

- [ ] **Step 3: Fix anything broken**

Prefer widening to `Number` and calling the accessor (`.intValue()`, `.doubleValue()`) over casting to a concrete box type. Add a regression test beside each fix.

- [ ] **Step 4: Verify**

Run the full check for every module you modified, redirecting to a file and checking `$?` on its own line.

- [ ] **Step 5: Commit**

```bash
git add -u
git commit -m "5575 Fix consumers assuming a concrete numeric box type"
```
If nothing needed changing, make no commit and say so in the report with the per-file evidence.
### Task 16: Preserve value types for trigger outputs

Trigger outputs go through `TriggerFileStorage`, a separate class from `TaskFileStorage`, and it is still untagged. `TriggerCompletionHandler` feeds a trigger's output into the job it creates, so it reaches the workflow context and expressions read it as `${trigger_1}`. A polling trigger returning a DB row therefore loses its `Timestamp` and `BigDecimal` types exactly as task outputs did before this plan.

**Files:**
- Modify: `server/libs/platform/platform-file-storage/platform-file-storage-impl/src/main/java/com/bytechef/platform/file/storage/TriggerFileStorageImpl.java` — `readTriggerExecutionOutput` wraps with `ValueTagUtils.untag`, `storeTriggerExecutionOutput` wraps its serialized argument with `ValueTagUtils.tag`
- Test: `server/libs/platform/platform-file-storage/platform-file-storage-impl/src/test/java/com/bytechef/platform/file/storage/TriggerFileStorageTest.java`
- Modify: that module's `build.gradle.kts` if the test needs `file-storage-base64-service` / `test-support`, mirroring what `atlas-file-storage-impl` already declares

**Interfaces:**
- Consumes: `ValueTagUtils.tag` / `untag`.
- Produces: no signature change.

- [ ] **Step 1: Write the failing test**

Mirror `TaskFileStorageTest`: a trigger output containing a `java.sql.Timestamp` reads back as `ZonedDateTime` at UTC; one containing a `BigDecimal` reads back as an equal `BigDecimal` and not a `Double`; an ISO-8601 **string** output reads back unchanged as a `String`; and legacy untagged JSON reads back exactly as today.

- [ ] **Step 2: Run test to verify it fails**

Run the module's `test` task filtered to the new class, redirecting to a file and checking `$?` on its own line.
Expected: FAIL — the temporal and `BigDecimal` assertions get a `String` and a `Double`.

- [ ] **Step 3: Write minimal implementation**

Wrap both methods exactly as `TaskFileStorageImpl` does — `ValueTagUtils.tag(output)` on the way in, `ValueTagUtils.untag(...)` on the way out. Change nothing else in the class.

- [ ] **Step 4: Audit the trigger-output consumers**

```bash
grep -rn "readTriggerExecutionOutput" server/libs server/ee --include=*.java | grep -v "/test/" | cut -d: -f1 | sort -u
```
Expect 5 files. For each, classify as SAFE (serializes onward, or feeds the evaluation context) or BROKEN (casts to `String`, `Integer` or `Double`, or calls a type-specific method). Fix anything broken by widening to `Number` and using its accessor, or accepting `Object` and converting where text is genuinely required.

- [ ] **Step 5: Verify and commit**

Run the full `check` for every module touched, redirecting to a file and checking `$?` on its own line, then grep `^> Task .* FAILED`.

```bash
git add server/libs/platform/platform-file-storage
git commit -m "5575 Preserve value types across trigger-output storage"
```
