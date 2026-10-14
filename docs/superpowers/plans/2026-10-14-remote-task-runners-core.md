# Remote Task Runners — Plan 1: CE Runner Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the CE half of the remote-runner design: the connection SPI, the remote backend interface, the `RemoteTaskRunner` orchestrator with Kestra-style `resume`/`delete`, Docker as the first backend, Kestra-aligned Process/Docker properties, and the save-time derivation of a runner's connection.

**Architecture:** `RemoteTaskRunner implements TaskRunner` wraps a `RemoteTaskRunnerBackend` and owns the local working directory, tar streams, deadlines, log capture and cleanup. Backends only submit/await/collect/cleanup against their API. The registry wraps every backend bean in an orchestrator. A `WorkflowDefinitionPreprocessor` SPI in atlas lets the runner module write `connections.taskRunner` into a task whenever it is saved.

**Tech Stack:** Java 25, Spring Boot 4, docker-java, Jackson 3 (`tools.jackson`), JUnit 5, AssertJ, Mockito, Testcontainers (existing Docker IntTest).

**Spec:** `docs/superpowers/specs/2026-10-14-remote-task-runners-design.md` (sections 2–5, 8–11). Plans 2 (Kubernetes) and 3 (AWS Batch) build on this one.

## Global Constraints

- Branch `0_p_script_runners` is **unpushed**; history may be rewritten. Back it up before the rebase task.
- Commit messages are **subject only**: `git commit -m "<subject>"`. No body, no `Co-Authored-By`, no `Generated with` line.
- Fold small changes into existing commits with `git commit --fixup=<sha>`; place each file at its **last-touch commit or later**, never earlier (`git log -1 --format=%h HEAD -- <path>`). The final task replays them.
- Every commit must compile at its own position. The final task verifies this with `git rebase --exec`.
- Operator setting defaults live in `server/apps/server-app/src/main/resources/config/application-bytechef.yml`, never as Java field initializers on `ApplicationProperties`.
- No `_` in test method names (Checkstyle); test classes end in `Test`/`IntTest`; one test class per source class.
- Run `./gradlew spotlessApply` on touched modules before each commit; `./gradlew <module>:check` before the final rebase.
- No prose comments in committed code; short interface Javadoc is fine.
- Runner type IDs: `graalvm`, `process`, `docker` (CE). Remote working directory base is chosen by the backend (`/tmp` for Docker); layout under it is fixed: `input.json`, `output/`, `.stderr`.
- Use `EncodingUtils`, never `java.util.Base64` directly (not needed here, but applies if touched).
- `JsonUtils` in tests needs `@ExtendWith(ObjectMapperSetupExtension.class)`.

## Review Focus

1. **Editor "Test" button with no task execution ID** — must get a random key and `resume=false`; two concurrent test clicks must never attach to each other's container. Pinned in Task 6 (`RemoteTaskRunnerTest.testANullTaskExecutionIdGetsARandomKeyAndNeverResumes`).
2. **Graceful shutdown while a job runs** — interrupt must DETACH (leave the container) when `resume=true`, so the redelivered task re-attaches; it must KILL when `resume=false`. Pinned in Task 6 (`testAnInterruptionDetachesWhenResumeIsOn`, `…KillsWhenResumeIsOff`).
3. **`waitUntilCompletion` above the operator's `max-wait`** — must be clamped, never honoured, so a worker permit cannot be held for days. Pinned in Task 6 (`testWaitUntilCompletionIsClampedToTheOperatorsMaxWait`).
4. **Stored workflow naming a per-task Docker `host` while `allow-task-host` is off** — must be rejected at `validate`, not silently use the operator's daemon. Pinned in Task 8 (`DockerTaskRunnerBackendTest.testAPerTaskHostIsRejectedUnlessTheOperatorAllowedIt`).
5. **Saving a workflow whose runner is currently disabled** — the existing `connections.taskRunner` entry must be left untouched. Pinned in Task 10 (`TaskRunnerConnectionWorkflowPreprocessorTest.testADisabledRunnerLeavesTheConnectionAlone`).

## File Structure

| Path | Responsibility |
|---|---|
| `…/platform-component-runner-api/…/runner/TaskRunnerConnection.java` | Record: which connection component a runner needs |
| `…/runner/TaskRunner.java` | `default Optional<TaskRunnerConnection> getConnection()` |
| `…/runner/remote/RemoteTaskRunnerBackend.java` | Backend SPI (submit/await/collect/cleanup over tar streams) |
| `…/runner/remote/RemoteJobKey.java`, `RemoteJobSpec.java`, `RemoteJobContext.java`, `RemoteJobHandle.java`, `RemoteJobExit.java`, `RemoteLogSink.java`, `CleanupMode.java`, `RemotePullPolicy.java`, `Deadline.java` | SPI value types |
| `…/runner/TaskRunnerConstants.java` | New property names |
| `app-config …/ApplicationProperties.java` | `Script.RunnerLimits` |
| `…/platform-component-runner-impl/…/runner/TaskRunnerLimits.java` | Resolves `max-wait`, `max-log-bytes`, `max-transfer-bytes` (global + per-runner) |
| `…/runner/external/BoundedOutputCapture.java` | Gets a `(long maxBytes)` constructor |
| `…/runner/external/RemoteTaskRunner.java` | Orchestrator |
| `…/runner/external/RemoteTaskRunnerSettings.java` | Parses the shared properties of one request |
| `…/runner/TaskRunnerRegistryImpl.java` | Wraps backends in `RemoteTaskRunner` |
| `…/runner/external/DockerTaskRunnerBackend.java` | Docker backend (replaces `DockerTaskRunner.java`) |
| `…/runner/external/DockerOperatorSettings.java` | Shrinks to `host`, `allow-task-host`, `allow-volume-mounts` |
| `…/runner/external/ProcessTaskRunner.java` | `inheritEnvironment` becomes a plain property |
| `platform-component-api …/definition/ActionContextAware.java`, `platform-component-context-service …/ActionContextImpl.java`, `ContextFactoryImpl.java` | `@Nullable Long getTaskExecutionId()` |
| `atlas-configuration-api …/workflow/preprocessor/WorkflowDefinitionPreprocessor.java` | SPI |
| `atlas-configuration-api …/workflow/mapper/WorkflowDefinitionCodec.java` | Read/write a definition map in JSON or YAML |
| `atlas-configuration-api …/util/WorkflowTaskUtils.java` | `isWorkflowTaskMap` made public |
| `atlas-configuration-service …/WorkflowServiceImpl.java`, `atlas-configuration-config …/WorkflowConfiguration.java` | Apply preprocessors on create/update |
| `…/platform-component-runner-impl/…/runner/TaskRunnerConnectionWorkflowPreprocessor.java` | Writes `connections.taskRunner` |
| `script`/`commands` `README.mdx`, `commands_v1.json`, `script_v1.json` | Docs and definition snapshots |

Module path prefix used below: `R_API` = `server/libs/platform/platform-component/platform-component-runner/platform-component-runner-api`, `R_IMPL` = `server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl`, package root `com.bytechef.platform.component.runner`.

Gradle shorthands: `:R_API` = `:server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api`, `:R_IMPL` = the same with `-impl`.

## Commit map

| Task | Commit |
|---|---|
| 1 | fixup → `Add the task runner SPI` (`25cc7a7f4ff`) |
| 2 | new: `Add the task runner limits` |
| 3 | new: `Add the remote task runner backend SPI` |
| 4 | fixup → `Add bounded output capture for external executions` (`9681da0e575`) |
| 5 | new: `Expose the task execution id on the action context` |
| 6 | new: `Add the remote task runner orchestrator` |
| 7 | new: `Offer the process runner's inherit environment option to every workflow` |
| 8 | new: `Run the Docker runner through the remote task runner orchestrator` |
| 9 | new: `Add the workflow definition preprocessor` |
| 10 | new: `Derive the task runner connection when a workflow is saved` |
| 11 | rebase: place the fixups, verify every commit builds |

Why Docker is a **new** commit rather than a fold into `0f0319c892a`: `ContainerArchive` is born in `0f0319c892a`, and the orchestrator needs it, so the orchestrator cannot sit below that commit. Folding would require an add/add conflict resolution in the middle of the replay. The spec's §11 is updated to this map in Task 11.

---
### Task 1: Connection SPI on `TaskRunner`

**Files:**
- Create: `R_API/src/main/java/com/bytechef/platform/component/runner/TaskRunnerConnection.java`
- Modify: `R_API/src/main/java/com/bytechef/platform/component/runner/TaskRunner.java`
- Test: `R_API/src/test/java/com/bytechef/platform/component/runner/TaskRunnerConnectionTest.java`

**Interfaces:**
- Produces: `record TaskRunnerConnection(String componentName, int componentVersion)`; `TaskRunner.getConnection()` returning `Optional<TaskRunnerConnection>` (default empty). Task 10 reads it.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.component.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TaskRunnerConnectionTest {

    @Test
    public void testHoldsTheComponentNameAndVersion() {
        TaskRunnerConnection connection = new TaskRunnerConnection("kubernetes", 1);

        assertThat(connection.componentName()).isEqualTo("kubernetes");
        assertThat(connection.componentVersion()).isEqualTo(1);
    }

    @Test
    public void testRejectsABlankComponentName() {
        assertThatThrownBy(() -> new TaskRunnerConnection(" ", 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("componentName");
    }

    @Test
    public void testRejectsAVersionBelowOne() {
        assertThatThrownBy(() -> new TaskRunnerConnection("aws", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("componentVersion");
    }

    @Test
    public void testARunnerDeclaresNoConnectionByDefault() {
        TaskRunner taskRunner = new TaskRunner() {

            @Override
            public String getType() {
                return "x";
            }

            @Override
            public String getTitle() {
                return "X";
            }

            @Override
            public List<? extends ModifiableValueProperty<?, ?>> getProperties() {
                return List.of();
            }

            @Override
            public Set<TaskRunnerCapability> getCapabilities() {
                return Set.of();
            }

            @Override
            public void validate(TaskRunnerRequest request) {
            }

            @Override
            public TaskRunnerResult run(TaskRunnerRequest request) {
                return TaskRunnerResult.ofOutput(null);
            }
        };

        assertThat(taskRunner.getConnection()).isEmpty();
    }
}
```

- [ ] **Step 2: Run it, expect compilation failure**

Run: `./gradlew :R_API:test --tests '*TaskRunnerConnectionTest' -q`
Expected: FAIL — `TaskRunnerConnection` does not exist, `getConnection()` undefined.

- [ ] **Step 3: Implement**

`TaskRunnerConnection.java` (Apache header like the neighbours):

```java
package com.bytechef.platform.component.runner;

/**
 * The connection component a task runner needs, wired under the task's {@code taskRunner} connection key.
 */
public record TaskRunnerConnection(String componentName, int componentVersion) {

    public TaskRunnerConnection {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("componentName must not be blank");
        }

        if (componentVersion < 1) {
            throw new IllegalArgumentException("componentVersion must be at least 1");
        }
    }
}
```

Add to `TaskRunner.java` (after `getCapabilities()`), plus `import java.util.Optional;`:

```java
    default Optional<TaskRunnerConnection> getConnection() {
        return Optional.empty();
    }
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :R_API:spotlessApply :R_API:test -q`
Expected: PASS.

- [ ] **Step 5: Commit as a fixup**

```bash
git add server/libs/platform/platform-component/platform-component-runner/platform-component-runner-api
git commit --fixup=25cc7a7f4ff
```

---

### Task 2: Operator runner limits

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (class `Script`)
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml`
- Create: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/TaskRunnerLimits.java`
- Test: `R_IMPL/src/test/java/com/bytechef/platform/component/runner/TaskRunnerLimitsTest.java`

**Interfaces:**
- Produces: `ApplicationProperties.Script.getRunnerLimits()` → `RunnerLimits` with `@Nullable Duration maxWait`, `@Nullable DataSize maxLogBytes`, `@Nullable DataSize maxTransferBytes`; `TaskRunnerLimits.of(ApplicationProperties, String runnerType)` with `getMaxWait()`, `getMaxLogBytes()`, `getMaxTransferBytes()`. Task 6 consumes it.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.component.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

public class TaskRunnerLimitsTest {

    @Test
    public void testBuiltInDefaultsApplyWhenNothingIsConfigured() {
        TaskRunnerLimits limits = TaskRunnerLimits.of(new ApplicationProperties(), "docker");

        assertThat(limits.getMaxWait()).isEqualTo(Duration.ofHours(1));
        assertThat(limits.getMaxLogBytes()).isEqualTo(1024L * 1024);
        assertThat(limits.getMaxTransferBytes()).isEqualTo(512L * 1024 * 1024);
    }

    @Test
    public void testGlobalLimitsAreRead() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script.RunnerLimits runnerLimits = applicationProperties.getScript()
            .getRunnerLimits();

        runnerLimits.setMaxWait(Duration.ofMinutes(20));
        runnerLimits.setMaxLogBytes(DataSize.ofKilobytes(64));
        runnerLimits.setMaxTransferBytes(DataSize.ofMegabytes(10));

        TaskRunnerLimits limits = TaskRunnerLimits.of(applicationProperties, "docker");

        assertThat(limits.getMaxWait()).isEqualTo(Duration.ofMinutes(20));
        assertThat(limits.getMaxLogBytes()).isEqualTo(64L * 1024);
        assertThat(limits.getMaxTransferBytes()).isEqualTo(10L * 1024 * 1024);
    }

    @Test
    public void testARunnersOwnPropertiesOverrideTheGlobalLimits() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getScript()
            .getRunnerLimits()
            .setMaxWait(Duration.ofMinutes(20));

        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(Map.of("max-wait", "PT5M", "max-log-bytes", "2MB", "max-transfer-bytes", "1GB"));

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("docker", runner);

        applicationProperties.getScript()
            .setRunners(runners);

        TaskRunnerLimits limits = TaskRunnerLimits.of(applicationProperties, "docker");

        assertThat(limits.getMaxWait()).isEqualTo(Duration.ofMinutes(5));
        assertThat(limits.getMaxLogBytes()).isEqualTo(2L * 1024 * 1024);
        assertThat(limits.getMaxTransferBytes()).isEqualTo(1024L * 1024 * 1024);

        assertThat(TaskRunnerLimits.of(applicationProperties, "process")
            .getMaxWait()).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    public void testAMalformedOverrideNamesItsKey() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(Map.of("max-wait", "soon"));

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("docker", runner);

        applicationProperties.getScript()
            .setRunners(runners);

        assertThatThrownBy(() -> TaskRunnerLimits.of(applicationProperties, "docker"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-wait");
    }
}
```

- [ ] **Step 2: Run it, expect compilation failure**

Run: `./gradlew :R_IMPL:test --tests '*TaskRunnerLimitsTest' -q`
Expected: FAIL — `getRunnerLimits()` and `TaskRunnerLimits` missing.

- [ ] **Step 3: Add `RunnerLimits` to `ApplicationProperties.Script`**

Inside `public static class Script`, add a field `private RunnerLimits runnerLimits = new RunnerLimits();` with getter/setter (keep fields alphabetical: `runnerLimits`, `runners`, `sandbox`), and the nested class after `Runner`. No field initializers inside `RunnerLimits`:

```java
        public static class RunnerLimits {

            private @Nullable Duration maxWait;

            private @Nullable DataSize maxLogBytes;

            private @Nullable DataSize maxTransferBytes;

            public @Nullable DataSize getMaxLogBytes() {
                return maxLogBytes;
            }

            public @Nullable DataSize getMaxTransferBytes() {
                return maxTransferBytes;
            }

            public @Nullable Duration getMaxWait() {
                return maxWait;
            }

            public void setMaxLogBytes(@Nullable DataSize maxLogBytes) {
                this.maxLogBytes = maxLogBytes;
            }

            public void setMaxTransferBytes(@Nullable DataSize maxTransferBytes) {
                this.maxTransferBytes = maxTransferBytes;
            }

            public void setMaxWait(@Nullable Duration maxWait) {
                this.maxWait = maxWait;
            }
        }
```

`ApplicationProperties` already imports `Duration`, `DataSize` and `org.jspecify.annotations.Nullable` (check the import block; add any that is missing).

- [ ] **Step 4: Add the yml defaults**

In `application-bytechef.yml`, under `script:` after the `runners:` block:

```yaml
    runner-limits:
      # Ceilings on what a remote task runner may cost this server: a held worker permit, captured log bytes, and
      # bytes moved in or out of the execution. A runner may override them under its own properties.
      max-wait: PT1H
      max-log-bytes: 1MB
      max-transfer-bytes: 512MB
```

- [ ] **Step 5: Implement `TaskRunnerLimits`**

```java
package com.bytechef.platform.component.runner;

import com.bytechef.config.ApplicationProperties;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.util.unit.DataSize;

public final class TaskRunnerLimits {

    public static final String MAX_LOG_BYTES_PROPERTY = "max-log-bytes";
    public static final String MAX_TRANSFER_BYTES_PROPERTY = "max-transfer-bytes";
    public static final String MAX_WAIT_PROPERTY = "max-wait";

    private static final Duration DEFAULT_MAX_WAIT = Duration.ofHours(1);
    private static final long DEFAULT_MAX_LOG_BYTES = 1024L * 1024;
    private static final long DEFAULT_MAX_TRANSFER_BYTES = 512L * 1024 * 1024;

    private final Duration maxWait;
    private final long maxLogBytes;
    private final long maxTransferBytes;

    private TaskRunnerLimits(Duration maxWait, long maxLogBytes, long maxTransferBytes) {
        this.maxWait = maxWait;
        this.maxLogBytes = maxLogBytes;
        this.maxTransferBytes = maxTransferBytes;
    }

    public static TaskRunnerLimits of(ApplicationProperties applicationProperties, String runnerType) {
        ApplicationProperties.Script script = applicationProperties.getScript();

        ApplicationProperties.Script.RunnerLimits runnerLimits = script.getRunnerLimits();

        Map<String, String> properties = runnerProperties(script, runnerType);

        Duration maxWait = parseDuration(properties.get(MAX_WAIT_PROPERTY), runnerType, MAX_WAIT_PROPERTY);

        if (maxWait == null) {
            maxWait = runnerLimits.getMaxWait();
        }

        Long maxLogBytes = parseBytes(properties.get(MAX_LOG_BYTES_PROPERTY), runnerType, MAX_LOG_BYTES_PROPERTY);

        if (maxLogBytes == null) {
            DataSize dataSize = runnerLimits.getMaxLogBytes();

            maxLogBytes = dataSize == null ? null : dataSize.toBytes();
        }

        Long maxTransferBytes = parseBytes(
            properties.get(MAX_TRANSFER_BYTES_PROPERTY), runnerType, MAX_TRANSFER_BYTES_PROPERTY);

        if (maxTransferBytes == null) {
            DataSize dataSize = runnerLimits.getMaxTransferBytes();

            maxTransferBytes = dataSize == null ? null : dataSize.toBytes();
        }

        return new TaskRunnerLimits(
            maxWait == null ? DEFAULT_MAX_WAIT : maxWait,
            maxLogBytes == null ? DEFAULT_MAX_LOG_BYTES : maxLogBytes,
            maxTransferBytes == null ? DEFAULT_MAX_TRANSFER_BYTES : maxTransferBytes);
    }

    public long getMaxLogBytes() {
        return maxLogBytes;
    }

    public long getMaxTransferBytes() {
        return maxTransferBytes;
    }

    public Duration getMaxWait() {
        return maxWait;
    }

    @Nullable
    private static Long parseBytes(@Nullable String value, String runnerType, String propertyName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return DataSize.parse(value.trim())
                .toBytes();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "Cannot read '%s' as a size; an operator must correct %s".formatted(
                    value, TaskRunnerOperatorFlag.getPropertyName(runnerType, propertyName)),
                exception);
        }
    }

    @Nullable
    private static Duration parseDuration(@Nullable String value, String runnerType, String propertyName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Duration.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "Cannot read '%s' as an ISO-8601 duration; an operator must correct %s".formatted(
                    value, TaskRunnerOperatorFlag.getPropertyName(runnerType, propertyName)),
                exception);
        }
    }

    private static Map<String, String> runnerProperties(ApplicationProperties.Script script, String runnerType) {
        Map<String, ApplicationProperties.Script.Runner> runners = script.getRunners();

        if (runners == null) {
            return Map.of();
        }

        ApplicationProperties.Script.Runner runner = runners.get(runnerType);

        if (runner == null || runner.getProperties() == null) {
            return Map.of();
        }

        return runner.getProperties();
    }
}
```

`R_IMPL/build.gradle.kts` needs `implementation("org.springframework:spring-core")` for `DataSize` if it is not already transitively on the compile classpath (it is, via `spring-context`; add it only if compilation complains).

- [ ] **Step 6: Run tests**

Run: `./gradlew :server:libs:config:app-config:spotlessApply :R_IMPL:spotlessApply :R_IMPL:test --tests '*TaskRunnerLimitsTest' -q`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add server/libs/config/app-config server/apps/server-app/src/main/resources/config/application-bytechef.yml server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl
git commit -m "Add the task runner limits"
```

---
### Task 3: Remote backend SPI types

**Files:**
- Create under `R_API/src/main/java/com/bytechef/platform/component/runner/remote/`: `RemoteTaskRunnerBackend.java`, `RemoteJobKey.java`, `RemoteJobSpec.java`, `RemoteJobContext.java`, `RemoteJobHandle.java`, `RemoteJobExit.java`, `RemoteLogSink.java`, `CleanupMode.java`, `RemotePullPolicy.java`, `Deadline.java`
- Modify: `R_API/src/main/java/com/bytechef/platform/component/runner/TaskRunnerConstants.java`
- Test: `R_API/src/test/java/com/bytechef/platform/component/runner/remote/RemoteJobSpecTest.java`, `DeadlineTest.java`

**Interfaces:**
- Produces (used by Tasks 6 and 8, and by Plans 2–3):

```java
public interface RemoteTaskRunnerBackend {
    String getType();
    String getTitle();
    String getRemoteWorkingDirectory();                       // e.g. "/tmp" or "/bytechef/work"
    List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties);
    Optional<TaskRunnerConnection> getConnection();
    void validate(TaskRunnerRequest request, RemoteJobContext context);
    Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context);
    RemoteJobHandle submit(RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context) throws IOException, InterruptedException;
    RemoteJobExit await(RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink, RemoteJobContext context) throws IOException, InterruptedException;
    @Nullable InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context) throws IOException, InterruptedException;
    void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context);
}
```
  `runningDeadline` bounds scheduling, image pull and start; `completionDeadline` bounds the whole job. `submit` receives a tar of the local working directory (entries relative to it: `input.json`, `<source>`, `output/`, input files) and returns once inputs are delivered and the job is running or queued. `collect` returns a tar whose entries are relative to the remote working directory (`output/...` and optionally `.stderr`), or `null` when the job produced no output directory. `cleanup` must not throw.

- [ ] **Step 1: Write the failing tests**

`RemoteJobSpecTest.java`:

```java
package com.bytechef.platform.component.runner.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class RemoteJobSpecTest {

    @Test
    public void testRemotePathsHangOffTheBackendsWorkingDirectory() {
        RemoteJobSpec spec = spec("/bytechef/work");

        assertThat(spec.remoteSourcePath()).isEqualTo("/bytechef/work/script.py");
        assertThat(spec.remoteInputFilePath()).isEqualTo("/bytechef/work/input.json");
        assertThat(spec.remoteOutputDirectory()).isEqualTo("/bytechef/work/output");
        assertThat(spec.remoteStderrPath()).isEqualTo("/bytechef/work/.stderr");
    }

    @Test
    public void testATrailingSlashOnTheWorkingDirectoryIsTolerated() {
        assertThat(spec("/tmp/").remoteSourcePath()).isEqualTo("/tmp/script.py");
    }

    @Test
    public void testTheWorkingDirectoryMustBeAbsolute() {
        assertThatThrownBy(() -> spec("work"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("absolute");
    }

    @Test
    public void testTheKeyBecomesTheLabelValue() {
        assertThat(new RemoteJobKey("1234", 9L).value()).isEqualTo("1234");
        assertThat(new RemoteJobKey("1234", null).jobId()).isNull();
        assertThatThrownBy(() -> new RemoteJobKey(" ", null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static RemoteJobSpec spec(String remoteWorkingDirectory) {
        return new RemoteJobSpec(
            new RemoteJobKey("1", null), "python", "script.py", List.of("python3"), Map.of(), remoteWorkingDirectory,
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(Map.of()));
    }
}
```

`DeadlineTest.java`:

```java
package com.bytechef.platform.component.runner.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class DeadlineTest {

    @Test
    public void testAFreshDeadlineHasTimeLeft() {
        Deadline deadline = Deadline.after(Duration.ofSeconds(30));

        assertThat(deadline.isExpired()).isFalse();
        assertThat(deadline.remainingMillis()).isBetween(29_000L, 30_000L);
        assertThat(deadline.remaining()).isPositive();
    }

    @Test
    public void testAnElapsedDeadlineReportsZeroRemaining() {
        Deadline deadline = Deadline.after(Duration.ZERO);

        assertThat(deadline.isExpired()).isTrue();
        assertThat(deadline.remainingMillis()).isZero();
    }

    @Test
    public void testCheckNamesTheStageAndTheBudget() {
        Deadline deadline = Deadline.after(Duration.ZERO);

        assertThatThrownBy(() -> deadline.check("docker", "pulling the image"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("docker")
            .hasMessageContaining("pulling the image")
            .hasMessageContaining("PT0S");
    }

    @Test
    public void testTheEarlierOfTwoDeadlinesWins() {
        Deadline later = Deadline.after(Duration.ofMinutes(10));
        Deadline earlier = Deadline.after(Duration.ofSeconds(5));

        assertThat(later.earliest(earlier)).isSameAs(earlier);
        assertThat(earlier.earliest(later)).isSameAs(earlier);
    }
}
```

- [ ] **Step 2: Run them, expect compilation failure**

Run: `./gradlew :R_API:test --tests '*remote*' -q`
Expected: FAIL — package does not exist.

- [ ] **Step 3: Create the types**

`CleanupMode.java`:

```java
package com.bytechef.platform.component.runner.remote;

public enum CleanupMode {

    DELETE,

    KILL_AND_DELETE,

    DETACH
}
```

`RemotePullPolicy.java`:

```java
package com.bytechef.platform.component.runner.remote;

import java.util.Locale;

public enum RemotePullPolicy {

    ALWAYS, IF_NOT_PRESENT, NEVER;

    public static RemotePullPolicy of(String value) {
        for (RemotePullPolicy candidate : values()) {
            if (candidate.name()
                .equalsIgnoreCase(value.trim())) {

                return candidate;
            }
        }

        throw new IllegalArgumentException("Unknown pull policy '%s'".formatted(value));
    }

    public String getLabel() {
        String lower = name().toLowerCase(Locale.ROOT)
            .replace('_', ' ');

        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
```

`RemoteJobKey.java`:

```java
package com.bytechef.platform.component.runner.remote;

import org.jspecify.annotations.Nullable;

/**
 * Identifies one task execution on the remote side; backends label the job with {@code value} so a redelivered task
 * can find it again.
 */
public record RemoteJobKey(String value, @Nullable Long jobId) {

    public RemoteJobKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
```

`RemoteJobHandle.java`:

```java
package com.bytechef.platform.component.runner.remote;

import java.util.Map;

/**
 * A backend's own identifier for a submitted job, for example a container id or a job id plus its S3 prefix.
 */
public record RemoteJobHandle(String id, Map<String, String> attributes) {

    public RemoteJobHandle(String id) {
        this(id, Map.of());
    }

    public RemoteJobHandle {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        attributes = Map.copyOf(attributes);
    }
}
```

`RemoteJobExit.java`:

```java
package com.bytechef.platform.component.runner.remote;

import org.jspecify.annotations.Nullable;

/**
 * How a remote job ended: {@code finished=false} means the deadline passed first.
 */
public record RemoteJobExit(boolean finished, @Nullable Integer exitCode, @Nullable String reason) {

    public static RemoteJobExit timedOut() {
        return new RemoteJobExit(false, null, null);
    }

    public static RemoteJobExit of(int exitCode) {
        return new RemoteJobExit(true, exitCode, null);
    }

    public static RemoteJobExit of(int exitCode, @Nullable String reason) {
        return new RemoteJobExit(true, exitCode, reason);
    }
}
```

`RemoteLogSink.java`:

```java
package com.bytechef.platform.component.runner.remote;

public interface RemoteLogSink {

    void appendStdout(byte[] buffer, int length);

    void appendStderr(byte[] buffer, int length);
}
```

`RemoteJobContext.java`:

```java
package com.bytechef.platform.component.runner.remote;

import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * What a backend may read besides the request: the task's runner connection and the operator's properties for this
 * runner type, plus the daemon or endpoint the task named through its {@code host} property when the backend has
 * one. Backends never read {@code ApplicationProperties} themselves.
 */
public record RemoteJobContext(
    @Nullable ComponentConnection connection, Map<String, String> operatorProperties, @Nullable String taskHost) {

    public RemoteJobContext {
        operatorProperties = Map.copyOf(operatorProperties);
    }

    public boolean isOperatorFlagEnabled(String propertyName) {
        return Boolean.parseBoolean(operatorProperties.get(propertyName));
    }
}
```

`RemoteJobSpec.java`:

```java
package com.bytechef.platform.component.runner.remote;

import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * Everything a backend needs to start one job. Remote paths are derived from the backend's working directory so every
 * runner shares the same layout underneath it.
 */
public record RemoteJobSpec(
    RemoteJobKey key, String languageId, String sourceFileName, List<String> interpreter,
    Map<String, String> environment, String remoteWorkingDirectory, RemotePullPolicy pullPolicy,
    Parameters runnerParameters) {

    public static final String INPUT_FILE_NAME = "input.json";
    public static final String OUTPUT_DIRECTORY_NAME = "output";
    public static final String STDERR_FILE_NAME = ".stderr";

    public RemoteJobSpec {
        if (!remoteWorkingDirectory.startsWith("/")) {
            throw new IllegalArgumentException("remoteWorkingDirectory must be absolute");
        }

        remoteWorkingDirectory = remoteWorkingDirectory.length() > 1 && remoteWorkingDirectory.endsWith("/")
            ? remoteWorkingDirectory.substring(0, remoteWorkingDirectory.length() - 1)
            : remoteWorkingDirectory;
        interpreter = List.copyOf(interpreter);
        environment = Map.copyOf(environment);
    }

    public String remoteInputFilePath() {
        return remoteWorkingDirectory + "/" + INPUT_FILE_NAME;
    }

    public String remoteOutputDirectory() {
        return remoteWorkingDirectory + "/" + OUTPUT_DIRECTORY_NAME;
    }

    public String remoteSourcePath() {
        return remoteWorkingDirectory + "/" + sourceFileName;
    }

    public String remoteStderrPath() {
        return remoteWorkingDirectory + "/" + STDERR_FILE_NAME;
    }
}
```

`Deadline.java`:

```java
package com.bytechef.platform.component.runner.remote;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class Deadline {

    private final long deadlineNanos;
    private final Duration budget;

    private Deadline(long deadlineNanos, Duration budget) {
        this.deadlineNanos = deadlineNanos;
        this.budget = budget;
    }

    public static Deadline after(Duration budget) {
        return new Deadline(System.nanoTime() + budget.toNanos(), budget);
    }

    public void check(String runnerType, String stage) {
        if (isExpired()) {
            throw new IllegalStateException(
                "The %s task runner timed out after %s while %s".formatted(runnerType, budget, stage));
        }
    }

    public Deadline earliest(Deadline other) {
        return other.deadlineNanos - deadlineNanos < 0 ? other : this;
    }

    public Duration getBudget() {
        return budget;
    }

    public boolean isExpired() {
        return System.nanoTime() - deadlineNanos >= 0;
    }

    public Duration remaining() {
        return Duration.ofMillis(remainingMillis());
    }

    public long remainingMillis() {
        long remainingNanos = deadlineNanos - System.nanoTime();

        return remainingNanos > 0 ? TimeUnit.NANOSECONDS.toMillis(remainingNanos) : 0;
    }
}
```

`RemoteTaskRunnerBackend.java` — exactly the interface shown under **Interfaces**, with imports `com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty`, `com.bytechef.platform.component.runner.TaskRunnerConnection`, `com.bytechef.platform.component.runner.TaskRunnerRequest`, `java.io.IOException`, `java.io.InputStream`, `java.util.List`, `java.util.Map`, `java.util.Optional`, `org.jspecify.annotations.Nullable`, and this Javadoc on the type: `Runs one task somewhere else. The orchestrator owns the local working directory, the deadlines and the log capture; a backend only moves a tar in, starts the job, reports how it ended, and moves a tar out.`

Add to `TaskRunnerConstants` (alphabetical within the runner-property group):

```java
    public static final String CREDENTIALS = "credentials";
    public static final String DELETE = "delete";
    public static final String HOST = "host";
    public static final String KILL_GRACE_PERIOD = "killGracePeriod";
    public static final String PASSWORD = "password";
    public static final String PRIVILEGED = "privileged";
    public static final String REGISTRY = "registry";
    public static final String REGISTRY_TOKEN = "registryToken";
    public static final String RESUME = "resume";
    public static final String USERNAME = "username";
    public static final String VOLUMES = "volumes";
    public static final String WAIT_UNTIL_COMPLETION = "waitUntilCompletion";
    public static final String WAIT_UNTIL_RUNNING = "waitUntilRunning";
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :R_API:spotlessApply :R_API:test -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-runner/platform-component-runner-api
git commit -m "Add the remote task runner backend SPI"
```

---

### Task 4: `BoundedOutputCapture` takes a byte ceiling

**Files:**
- Modify: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/external/BoundedOutputCapture.java`
- Test: `R_IMPL/src/test/java/com/bytechef/platform/component/runner/external/BoundedOutputCaptureTest.java` (exists; add tests)

**Interfaces:**
- Produces: `new BoundedOutputCapture(long maxBytes)`; the no-arg constructor keeps today's 64 KiB total. Task 6 uses the sized one with `TaskRunnerLimits.getMaxLogBytes()`.

- [ ] **Step 1: Add failing tests** to `BoundedOutputCaptureTest`

```java
    @Test
    void testASizedCaptureKeepsHalfAtEachEnd() {
        BoundedOutputCapture capture = new BoundedOutputCapture(8);

        byte[] bytes = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);

        capture.append(bytes, bytes.length);

        String text = capture.get();

        assertThat(text).startsWith("0123");
        assertThat(text).endsWith("CDEF");
        assertThat(text).contains("8 bytes elided");
    }

    @Test
    void testACeilingBelowTwoBytesIsRejected() {
        assertThatThrownBy(() -> new BoundedOutputCapture(1)).isInstanceOf(IllegalArgumentException.class);
    }
```

(add `import static org.assertj.core.api.Assertions.assertThatThrownBy;` and `java.nio.charset.StandardCharsets` if missing).

- [ ] **Step 2: Run, expect failure**

Run: `./gradlew :R_IMPL:test --tests '*BoundedOutputCaptureTest' -q`
Expected: FAIL — no such constructor.

- [ ] **Step 3: Implement**

Replace the static `HALF_LIMIT` usage with an instance field. The class becomes:

```java
    private static final long DEFAULT_MAX_BYTES = 64L * 1024;

    private final int halfLimit;
    private final ByteArrayOutputStream head = new ByteArrayOutputStream();
    private final byte[] tail;

    private int tailLength;
    private int tailPosition;
    private long totalLength;

    public BoundedOutputCapture() {
        this(DEFAULT_MAX_BYTES);
    }

    public BoundedOutputCapture(long maxBytes) {
        if (maxBytes < 2) {
            throw new IllegalArgumentException("maxBytes must be at least 2");
        }

        this.halfLimit = (int) Math.min(maxBytes / 2, Integer.MAX_VALUE - 8);
        this.tail = new byte[halfLimit];
    }
```

and every remaining `HALF_LIMIT` reference becomes `halfLimit` (`append`, `orderedTail`).

- [ ] **Step 4: Run tests**

Run: `./gradlew :R_IMPL:spotlessApply :R_IMPL:test --tests '*BoundedOutputCaptureTest' -q`
Expected: PASS.

- [ ] **Step 5: Commit as a fixup**

```bash
git add server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl
git commit --fixup=9681da0e575
```

---
### Task 5: Expose the task execution ID on the action context

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ActionContextAware.java`
- Modify: `server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/ActionContextImpl.java`
- Modify: `…/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/ContextFactoryImpl.java` (line ~104)
- Test: `…/platform-component-context-service/src/test/java/com/bytechef/platform/component/context/ActionContextImplTest.java` (exists; add tests)

**Interfaces:**
- Produces: `@Nullable Long ActionContextAware.getTaskExecutionId()` — `null` for the editor's Test button, the task execution ID otherwise. Task 6 reads it.

- [ ] **Step 1: Add failing tests** to `ActionContextImplTest` (adapt the builder call to the existing test's helpers if it already has a factory; otherwise use mocks as shown):

```java
    @Test
    void testTheTaskExecutionIdIsNullWhenNoneWasGiven() {
        ActionContextImpl actionContext = ActionContextImpl.builder(
            "component", 1, "action", true, mock(CacheManager.class), mock(DataStorage.class),
            mock(ApplicationEventPublisher.class), mock(HttpClientExecutor.class), mock(TempFileStorage.class), null)
            .taskExecutionId(null)
            .build();

        assertThat(actionContext.getTaskExecutionId()).isNull();
    }

    @Test
    void testTheTaskExecutionIdIsReturnedWhenGiven() {
        ActionContextImpl actionContext = ActionContextImpl.builder(
            "component", 1, "action", false, mock(CacheManager.class), mock(DataStorage.class),
            mock(ApplicationEventPublisher.class), mock(HttpClientExecutor.class), mock(TempFileStorage.class), null)
            .taskExecutionId(42L)
            .build();

        assertThat(actionContext.getTaskExecutionId()).isEqualTo(42L);
    }
```

- [ ] **Step 2: Run, expect failure**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:test --tests '*ActionContextImplTest' -q`
Expected: FAIL — `getTaskExecutionId()` undefined / `taskExecutionId(null)` does not compile.

- [ ] **Step 3: Implement**

`ActionContextAware.java` — add after `getJobId()`:

```java
    /**
     * Retrieves the identifier of the task execution this action runs under.
     *
     * @return the task execution ID, or {@code null} when the action runs outside a task execution, such as the
     *         editor's test run.
     */
    @Nullable
    Long getTaskExecutionId();
```

`ActionContextImpl.java`:
1. Field: `private final long taskExecutionId;` → `private final @Nullable Long taskExecutionId;`
2. In the private constructor, the `super(...)` argument `builder.taskExecutionId` → `builder.taskExecutionId == null ? 0L : builder.taskExecutionId` (the base `ContextImpl` keeps its primitive).
3. In `toClusterElementContext(...)` (the builder chain around line 147 that targets `ClusterElementContextImpl.Builder`): `.taskExecutionId(taskExecutionId)` → `.taskExecutionId(taskExecutionId == null ? 0L : taskExecutionId)`.
4. `Builder`: field `private @Nullable Long taskExecutionId;` and `Builder taskExecutionId(@Nullable Long taskExecutionId)`.
5. Add the getter next to `getJobId()`:

```java
    @Override
    public @Nullable Long getTaskExecutionId() {
        return taskExecutionId;
    }
```

`ContextFactoryImpl.java`: `.taskExecutionId(taskExecutionId != null ? taskExecutionId : 0)` → `.taskExecutionId(taskExecutionId)`.

- [ ] **Step 4: Run tests for both modules**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:spotlessApply :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:spotlessApply :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:test -q`
Expected: PASS. Also `./gradlew compileJava -q` at the root to catch any other `ActionContextAware` implementation (EE remote stubs or test doubles) that now needs the method; add `return null;` implementations where the compiler complains.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api server/libs/platform/platform-component/platform-component-context
git commit -m "Expose the task execution id on the action context"
```

---

### Task 6: `RemoteTaskRunner` orchestrator and registry wrapping

**Files:**
- Create: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/external/RemoteTaskRunnerSettings.java`
- Create: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/external/RemoteTaskRunner.java`
- Modify: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/TaskRunnerRegistryImpl.java`
- Modify: `R_IMPL/build.gradle.kts` (add `implementation(project(":server:libs:platform:platform-component:platform-component-api"))` if `ActionContextAware` is not already visible; it comes through the api module's `api(...)` dependency, so usually nothing to add)
- Modify: `server/libs/modules/components/commands/src/test/java/com/bytechef/component/commands/CommandsComponentHandlerTest.java`, `server/libs/modules/components/script/src/test/java/com/bytechef/component/script/ScriptComponentHandlerTest.java` (`createTaskRunnerRegistry()` gains a `List.of()` backends argument)
- Test: `R_IMPL/src/test/java/com/bytechef/platform/component/runner/external/RemoteTaskRunnerSettingsTest.java`, `RemoteTaskRunnerTest.java`; extend `R_IMPL/src/test/java/com/bytechef/platform/component/runner/TaskRunnerRegistryTest.java`

**Interfaces:**
- Consumes: Task 2 `TaskRunnerLimits`, Task 3 SPI types, Task 4 `BoundedOutputCapture(long)`, Task 5 `getTaskExecutionId()`, existing `TaskRunnerWorkingDirectory`, `TaskRunnerBootstrap`, `TaskRunnerOutputs`, `ContainerArchive`, `ExternalLanguage`.
- Produces: `public class RemoteTaskRunner implements TaskRunner` with constructors `(RemoteTaskRunnerBackend, ApplicationProperties)` and package-private `(RemoteTaskRunnerBackend, ApplicationProperties, Supplier<String> randomKeySupplier)`; `TaskRunnerRegistryImpl(List<TaskRunner>, List<RemoteTaskRunnerBackend>, ApplicationProperties)`; `record RemoteTaskRunnerSettings(boolean resume, boolean delete, Duration waitUntilRunning, Duration waitUntilCompletion, RemotePullPolicy pullPolicy)` with `static of(TaskRunnerRequest, TaskRunnerLimits, String type)` and `static List<ModifiableValueProperty<?, ?>> properties()`.

- [ ] **Step 1: Write the settings test**

```java
package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerLimits;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class RemoteTaskRunnerSettingsTest {

    private final TaskRunnerLimits limits = TaskRunnerLimits.of(new ApplicationProperties(), "docker");

    @Test
    void testDefaultsMatchKestra() {
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(request(Map.of(), null), limits, "docker");

        assertThat(settings.resume()).isTrue();
        assertThat(settings.delete()).isTrue();
        assertThat(settings.waitUntilRunning()).isEqualTo(Duration.ofMinutes(10));
        assertThat(settings.waitUntilCompletion()).isEqualTo(Duration.ofHours(1));
        assertThat(settings.pullPolicy()).isEqualTo(RemotePullPolicy.IF_NOT_PRESENT);
    }

    @Test
    void testTheTaskTimeoutTakesPrecedenceOverWaitUntilCompletion() {
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(
            request(Map.of("waitUntilCompletion", "PT30M"), Duration.ofSeconds(90)), limits, "docker");

        assertThat(settings.waitUntilCompletion()).isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    void testWaitUntilCompletionIsClampedToTheOperatorsMaxWait() {
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(
            request(Map.of("waitUntilCompletion", "PT48H"), null), limits, "docker");

        assertThat(settings.waitUntilCompletion()).isEqualTo(Duration.ofHours(1));

        RemoteTaskRunnerSettings timeoutSettings = RemoteTaskRunnerSettings.of(
            request(Map.of(), Duration.ofHours(5)), limits, "docker");

        assertThat(timeoutSettings.waitUntilCompletion()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void testWaitUntilRunningNeverExceedsWaitUntilCompletion() {
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(
            request(Map.of("waitUntilRunning", "PT20M"), Duration.ofMinutes(2)), limits, "docker");

        assertThat(settings.waitUntilRunning()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void testAMalformedDurationNamesTheProperty() {
        assertThatThrownBy(
            () -> RemoteTaskRunnerSettings.of(request(Map.of("waitUntilRunning", "ten minutes"), null), limits, "docker"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("waitUntilRunning")
                .hasMessageContaining("ten minutes");
    }

    @Test
    void testANonPositiveDurationIsRejected() {
        assertThatThrownBy(
            () -> RemoteTaskRunnerSettings.of(request(Map.of("waitUntilCompletion", "PT0S"), null), limits, "docker"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("waitUntilCompletion");
    }

    @Test
    void testAnUnknownPullPolicyIsRejected() {
        assertThatThrownBy(
            () -> RemoteTaskRunnerSettings.of(request(Map.of("pullPolicy", "SOMETIMES"), null), limits, "docker"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SOMETIMES");
    }

    @Test
    void testPropertiesAreFreshOnEveryCall() {
        assertThat(RemoteTaskRunnerSettings.properties()
            .getFirst()).isNotSameAs(
                RemoteTaskRunnerSettings.properties()
                    .getFirst());

        assertThat(RemoteTaskRunnerSettings.properties())
            .extracting(property -> property.getName())
            .containsExactly("pullPolicy", "resume", "delete", "waitUntilRunning", "waitUntilCompletion");
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters, @Nullable Duration timeout) {
        return new TaskRunnerRequest(
            "python", "def perform(input, context):\n    return 1\n", List.of(), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), timeout,
            Map.of(), mock(ActionContext.class));
    }
}
```

- [ ] **Step 2: Run, expect compilation failure**

Run: `./gradlew :R_IMPL:test --tests '*RemoteTaskRunnerSettingsTest' -q`
Expected: FAIL.

- [ ] **Step 3: Implement `RemoteTaskRunnerSettings`**

```java
package com.bytechef.platform.component.runner.external;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.DELETE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PULL_POLICY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.RESUME;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.WAIT_UNTIL_COMPLETION;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.WAIT_UNTIL_RUNNING;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.runner.TaskRunnerLimits;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.jspecify.annotations.Nullable;

record RemoteTaskRunnerSettings(
    boolean resume, boolean delete, Duration waitUntilRunning, Duration waitUntilCompletion,
    RemotePullPolicy pullPolicy) {

    private static final Duration DEFAULT_WAIT_UNTIL_RUNNING = Duration.ofMinutes(10);
    private static final Duration DEFAULT_WAIT_UNTIL_COMPLETION = Duration.ofHours(1);

    static RemoteTaskRunnerSettings of(TaskRunnerRequest request, TaskRunnerLimits limits, String type) {
        Parameters parameters = request.runnerParameters();

        Duration maxWait = limits.getMaxWait();

        Duration requestTimeout = request.timeout();

        Duration waitUntilCompletion = requestTimeout != null
            ? requestTimeout
            : parseDuration(parameters.getString(WAIT_UNTIL_COMPLETION), WAIT_UNTIL_COMPLETION, type,
                DEFAULT_WAIT_UNTIL_COMPLETION);

        if (waitUntilCompletion.compareTo(maxWait) > 0) {
            waitUntilCompletion = maxWait;
        }

        Duration waitUntilRunning = parseDuration(
            parameters.getString(WAIT_UNTIL_RUNNING), WAIT_UNTIL_RUNNING, type, DEFAULT_WAIT_UNTIL_RUNNING);

        if (waitUntilRunning.compareTo(waitUntilCompletion) > 0) {
            waitUntilRunning = waitUntilCompletion;
        }

        String pullPolicy = parameters.getString(PULL_POLICY);

        return new RemoteTaskRunnerSettings(
            parameters.getBoolean(RESUME, true), parameters.getBoolean(DELETE, true), waitUntilRunning,
            waitUntilCompletion,
            pullPolicy == null || pullPolicy.isBlank() ? RemotePullPolicy.IF_NOT_PRESENT : parsePullPolicy(pullPolicy, type));
    }

    static List<ModifiableValueProperty<?, ?>> properties() {
        return List.of(
            string(PULL_POLICY)
                .label("Pull Policy")
                .description("When the image is pulled from its registry.")
                .options(
                    option("Always", RemotePullPolicy.ALWAYS.name()),
                    option("If Not Present", RemotePullPolicy.IF_NOT_PRESENT.name()),
                    option("Never", RemotePullPolicy.NEVER.name()))
                .defaultValue(RemotePullPolicy.IF_NOT_PRESENT.name())
                .required(false),
            bool(RESUME)
                .label("Resume")
                .description(
                    "After a server restart, re-attach to the job this task execution already started instead of " +
                        "starting another one.")
                .defaultValue(true)
                .required(false),
            bool(DELETE)
                .label("Delete")
                .description(
                    "Remove the job and whatever it left behind once it has finished. Turn it off to inspect a " +
                        "failed job by hand.")
                .defaultValue(true)
                .required(false),
            string(WAIT_UNTIL_RUNNING)
                .label("Wait Until Running")
                .description(
                    "How long the job may take to be scheduled, pull its image and start, as an ISO-8601 duration.")
                .defaultValue("PT10M")
                .required(false),
            string(WAIT_UNTIL_COMPLETION)
                .label("Wait Until Completion")
                .description(
                    "How long the job may run before it is killed, as an ISO-8601 duration. The task's Timeout, " +
                        "when set, takes precedence; both are capped by the operator's max-wait.")
                .defaultValue("PT1H")
                .required(false));
    }

    private static Duration parseDuration(
        @Nullable String value, String propertyName, String type, Duration defaultValue) {

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        Duration duration;

        try {
            duration = Duration.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "The %s runner cannot read '%s' as the ISO-8601 duration %s expects".formatted(
                    type, value, propertyName),
                exception);
        }

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                "The %s runner's %s must be greater than zero".formatted(type, propertyName));
        }

        return duration;
    }

    private static RemotePullPolicy parsePullPolicy(String value, String type) {
        try {
            return RemotePullPolicy.of(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "The %s runner does not know the pull policy '%s'".formatted(type, value), exception);
        }
    }
}
```

- [ ] **Step 4: Run the settings test**

Run: `./gradlew :R_IMPL:spotlessApply :R_IMPL:test --tests '*RemoteTaskRunnerSettingsTest' -q`
Expected: PASS.

- [ ] **Step 5: Implement `RemoteTaskRunner`**

```java
package com.bytechef.platform.component.runner.external;

import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENTRYPOINT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.HOST;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.polyglot.GuestLoggingOutputStream;
import com.bytechef.platform.component.runner.TaskRunner;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerLimits;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.Deadline;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobExit;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemoteLogSink;
import com.bytechef.platform.component.runner.remote.RemoteTaskRunnerBackend;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a task through a {@link RemoteTaskRunnerBackend}: owns the local working directory, the tar streams, the
 * deadlines, the log capture and the cleanup, so a backend only has to talk to its own API.
 */
@SuppressFBWarnings("EI")
public class RemoteTaskRunner implements TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(RemoteTaskRunner.class);

    private static final Map<ExternalLanguage, List<String>> DEFAULT_INTERPRETERS = Map.of(
        ExternalLanguage.JAVASCRIPT, List.of("node"),
        ExternalLanguage.PYTHON, List.of("python3"),
        ExternalLanguage.SHELL, List.of("/bin/sh"));

    private final RemoteTaskRunnerBackend backend;
    private final ApplicationProperties applicationProperties;
    private final Supplier<String> randomKeySupplier;

    public RemoteTaskRunner(RemoteTaskRunnerBackend backend, ApplicationProperties applicationProperties) {
        this(backend, applicationProperties, () -> UUID.randomUUID()
            .toString());
    }

    RemoteTaskRunner(
        RemoteTaskRunnerBackend backend, ApplicationProperties applicationProperties,
        Supplier<String> randomKeySupplier) {

        this.backend = backend;
        this.applicationProperties = applicationProperties;
        this.randomKeySupplier = randomKeySupplier;
    }

    @Override
    public String getType() {
        return backend.getType();
    }

    @Override
    public String getTitle() {
        return backend.getTitle();
    }

    @Override
    public List<? extends ModifiableValueProperty<?, ?>> getProperties() {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>(
            backend.getProperties(operatorProperties()));

        properties.addAll(RemoteTaskRunnerSettings.properties());

        return properties;
    }

    @Override
    public Set<TaskRunnerCapability> getCapabilities() {
        return Set.of(
            TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMMANDS, TaskRunnerCapability.INPUT_FILES,
            TaskRunnerCapability.OUTPUT_FILES, TaskRunnerCapability.ENVIRONMENT);
    }

    @Override
    public Optional<TaskRunnerConnection> getConnection() {
        return backend.getConnection();
    }

    @Override
    public void validate(TaskRunnerRequest request) {
        String languageId = request.languageId();

        if (!TaskRunnerBootstrap.isSupported(languageId)) {
            throw new IllegalArgumentException(
                "Language '%s' cannot be executed by an external task runner".formatted(languageId));
        }

        RemoteJobContext context = context(request);

        if (backend.getConnection()
            .isPresent() && context.connection() == null) {

            throw new IllegalArgumentException(
                "The %s runner needs a connection; select one in the task's connections".formatted(getTitle()));
        }

        RemoteTaskRunnerSettings.of(request, limits(), getType());

        resolveInterpreter(request);

        backend.validate(request, context);
    }

    @Override
    public TaskRunnerResult run(TaskRunnerRequest request) {
        validate(request);

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request);
            CapturingLogSink logSink = new CapturingLogSink(getType(), limits().getMaxLogBytes())) {

            return run(request, workingDirectory, logSink);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException("Interrupted while running the %s task runner".formatted(getType()),
                exception);
        }
    }

    private TaskRunnerResult run(
        TaskRunnerRequest request, TaskRunnerWorkingDirectory workingDirectory, CapturingLogSink logSink)
        throws IOException, InterruptedException {

        String type = getType();

        TaskRunnerLimits limits = limits();
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(request, limits, type);
        RemoteJobContext context = context(request);

        ResolvedKey resolvedKey = resolveKey(request);

        boolean resume = settings.resume() && resolvedKey.resumable();

        Deadline completionDeadline = Deadline.after(settings.waitUntilCompletion());
        Deadline runningDeadline = Deadline.after(settings.waitUntilRunning())
            .earliest(completionDeadline);

        String languageId = request.languageId();
        String script = request.script();

        String source = script != null
            ? TaskRunnerBootstrap.append(languageId, script, type)
            : String.join(System.lineSeparator(), request.commands()) + System.lineSeparator();

        String sourceFileName = TaskRunnerBootstrap.sourceFileName(languageId);

        workingDirectory.writeSourceFile(sourceFileName, source);

        RemoteJobSpec spec = new RemoteJobSpec(
            resolvedKey.key(), languageId, sourceFileName, resolveInterpreter(request),
            environment(request, backend.getRemoteWorkingDirectory()), backend.getRemoteWorkingDirectory(),
            settings.pullPolicy(), request.runnerParameters());

        Optional<RemoteJobHandle> found = resume ? backend.find(resolvedKey.key(), context) : Optional.empty();

        RemoteJobHandle handle;

        if (found.isPresent()) {
            handle = found.get();

            log.info("Re-attached to the {} job {} for task execution {}", type, handle.id(), resolvedKey.key()
                .value());
        } else {
            checkInputSize(workingDirectory.getPath(), limits.getMaxTransferBytes(), type);

            try (InputStream inputArchive = ContainerArchive.toTar(workingDirectory.getPath())) {
                handle = backend.submit(spec, inputArchive, runningDeadline, context);
            }
        }

        Outcome outcome = Outcome.ERROR;

        try {
            RemoteJobExit exit = backend.await(handle, runningDeadline, completionDeadline, logSink, context);

            if (!exit.finished()) {
                outcome = Outcome.TIMEOUT;

                throw new IllegalStateException(
                    "The %s task runner timed out after %s; stdout tail: %s; stderr tail: %s".formatted(
                        type, completionDeadline.getBudget(), logSink.stdout(), logSink.stderr()));
            }

            Integer exitCode = exit.exitCode();

            if (exitCode == null) {
                throw new IllegalStateException(
                    "The %s job reported no exit code%s; stdout tail: %s; stderr tail: %s".formatted(
                        type, reasonSuffix(exit), logSink.stdout(), logSink.stderr()));
            }

            if (exitCode != 0) {
                outcome = Outcome.FAILURE;

                throw new IllegalStateException(
                    "The %s job exited with code %d%s; stdout tail: %s; stderr tail: %s".formatted(
                        type, exitCode, reasonSuffix(exit), logSink.stdout(), logSink.stderr()));
            }

            completionDeadline.check(type, "collecting the output");

            collectOutput(handle, workingDirectory, logSink, completionDeadline, context, limits);

            outcome = Outcome.SUCCESS;

            return new TaskRunnerResult(
                TaskRunnerOutputs.readOutputJson(workingDirectory), exitCode, logSink.stdout(), logSink.stderr(),
                TaskRunnerOutputs.collectOutputFiles(request, workingDirectory));
        } catch (InterruptedException exception) {
            outcome = Outcome.INTERRUPTED;

            throw exception;
        } finally {
            backend.cleanup(handle, cleanupMode(outcome, resume, settings.delete()), context);
        }
    }

    static CleanupMode cleanupMode(Outcome outcome, boolean resume, boolean delete) {
        return switch (outcome) {
            case SUCCESS, FAILURE -> delete ? CleanupMode.DELETE : CleanupMode.DETACH;
            case TIMEOUT, ERROR -> CleanupMode.KILL_AND_DELETE;
            case INTERRUPTED -> resume ? CleanupMode.DETACH : CleanupMode.KILL_AND_DELETE;
        };
    }

    private void collectOutput(
        RemoteJobHandle handle, TaskRunnerWorkingDirectory workingDirectory, CapturingLogSink logSink,
        Deadline completionDeadline, RemoteJobContext context, TaskRunnerLimits limits)
        throws IOException, InterruptedException {

        InputStream outputArchive = backend.collect(handle, completionDeadline, context);

        if (outputArchive == null) {
            log.warn("The {} job left no output directory, so the execution produced no output", getType());

            return;
        }

        try (InputStream inputStream = outputArchive) {
            ContainerArchive.extractTar(inputStream, workingDirectory.getPath(), limits.getMaxTransferBytes());
        }

        Path stderrPath = workingDirectory.getPath()
            .resolve(RemoteJobSpec.STDERR_FILE_NAME);

        if (Files.isRegularFile(stderrPath, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = Files.readAllBytes(stderrPath);

            logSink.appendStderr(bytes, bytes.length);
        }
    }

    private RemoteJobContext context(TaskRunnerRequest request) {
        ComponentConnection connection = request.componentConnections()
            .get(TASK_RUNNER);

        String taskHost = request.runnerParameters()
            .getString(HOST);

        return new RemoteJobContext(
            connection, operatorProperties(), taskHost == null || taskHost.isBlank() ? null : taskHost.trim());
    }

    private TaskRunnerLimits limits() {
        return TaskRunnerLimits.of(applicationProperties, getType());
    }

    private Map<String, String> operatorProperties() {
        Map<String, ApplicationProperties.Script.Runner> runners = applicationProperties.getScript()
            .getRunners();

        if (runners == null) {
            return Map.of();
        }

        ApplicationProperties.Script.Runner runner = runners.get(getType());

        if (runner == null || runner.getProperties() == null) {
            return Map.of();
        }

        return runner.getProperties();
    }

    private ResolvedKey resolveKey(TaskRunnerRequest request) {
        ActionContext actionContext = request.actionContext();

        if (actionContext instanceof ActionContextAware actionContextAware) {
            Long taskExecutionId = actionContextAware.getTaskExecutionId();

            if (taskExecutionId != null) {
                return new ResolvedKey(
                    new RemoteJobKey(String.valueOf(taskExecutionId), actionContextAware.getJobId()), true);
            }
        }

        return new ResolvedKey(new RemoteJobKey(randomKeySupplier.get(), null), false);
    }

    private List<String> resolveInterpreter(TaskRunnerRequest request) {
        List<String> entrypoint = request.runnerParameters()
            .getList(ENTRYPOINT, String.class, List.of());

        if (!entrypoint.isEmpty()) {
            return entrypoint;
        }

        String languageId = request.languageId();

        List<String> defaultInterpreter = DEFAULT_INTERPRETERS.get(ExternalLanguage.of(languageId));

        if (defaultInterpreter == null) {
            throw new IllegalArgumentException(
                "The %s runner has no default interpreter for language '%s'; set an entrypoint on the task runner"
                    .formatted(getType(), languageId));
        }

        return defaultInterpreter;
    }

    private static void checkInputSize(Path path, long maxTransferBytes, String type) throws IOException {
        long total;

        try (Stream<Path> paths = Files.walk(path)) {
            total = paths.filter(candidate -> Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS))
                .mapToLong(candidate -> {
                    try {
                        return Files.size(candidate);
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                })
                .sum();
        }

        if (total > maxTransferBytes) {
            throw new IllegalStateException(
                "The %s task runner's input files take %d bytes, above the %d byte transfer ceiling".formatted(
                    type, total, maxTransferBytes));
        }
    }

    private static Map<String, String> environment(TaskRunnerRequest request, String remoteWorkingDirectory) {
        String directory = remoteWorkingDirectory.length() > 1 && remoteWorkingDirectory.endsWith("/")
            ? remoteWorkingDirectory.substring(0, remoteWorkingDirectory.length() - 1)
            : remoteWorkingDirectory;

        Map<String, String> environment = new LinkedHashMap<>();

        environment.put("HOME", directory);
        environment.putAll(request.env());
        environment.put("BYTECHEF_WORKING_DIR", directory);
        environment.put("BYTECHEF_INPUT_FILE", directory + "/" + RemoteJobSpec.INPUT_FILE_NAME);
        environment.put("BYTECHEF_OUTPUT_DIR", directory + "/" + RemoteJobSpec.OUTPUT_DIRECTORY_NAME);

        return environment;
    }

    private static String reasonSuffix(RemoteJobExit exit) {
        String reason = exit.reason();

        return reason == null || reason.isBlank() ? "" : " (" + reason + ")";
    }

    enum Outcome {

        SUCCESS, FAILURE, TIMEOUT, INTERRUPTED, ERROR
    }

    private record ResolvedKey(RemoteJobKey key, boolean resumable) {
    }

    static final class CapturingLogSink implements RemoteLogSink, Closeable {

        private final BoundedOutputCapture stdout;
        private final BoundedOutputCapture stderr;
        private final GuestLoggingOutputStream stdoutLoggingOutputStream;
        private final GuestLoggingOutputStream stderrLoggingOutputStream;

        CapturingLogSink(String type, long maxLogBytes) {
            this.stdout = new BoundedOutputCapture(maxLogBytes);
            this.stderr = new BoundedOutputCapture(maxLogBytes);
            this.stdoutLoggingOutputStream = new GuestLoggingOutputStream(line -> {
                if (log.isInfoEnabled()) {
                    log.info("[{}] {}", type, line);
                }
            });
            this.stderrLoggingOutputStream = new GuestLoggingOutputStream(line -> log.warn("[{}] {}", type, line));
        }

        @Override
        public void appendStdout(byte[] buffer, int length) {
            append(stdout, stdoutLoggingOutputStream, buffer, length);
        }

        @Override
        public void appendStderr(byte[] buffer, int length) {
            append(stderr, stderrLoggingOutputStream, buffer, length);
        }

        @Override
        public void close() throws IOException {
            try {
                stdoutLoggingOutputStream.close();
            } finally {
                stderrLoggingOutputStream.close();
            }
        }

        String stdout() {
            return stdout.get();
        }

        String stderr() {
            return stderr.get();
        }

        private static void append(
            BoundedOutputCapture capture, GuestLoggingOutputStream loggingOutputStream, byte[] buffer, int length) {

            capture.append(buffer, length);

            try {
                loggingOutputStream.write(buffer, 0, length);
            } catch (IOException exception) {
                log.debug("Could not tee the job's output to the log", exception);
            }
        }
    }
}
```

- [ ] **Step 6: Wrap backends in the registry**

`TaskRunnerRegistryImpl` constructor becomes:

```java
    public TaskRunnerRegistryImpl(
        List<TaskRunner> taskRunners, List<RemoteTaskRunnerBackend> backends,
        ApplicationProperties applicationProperties) {

        List<TaskRunner> allTaskRunners = new ArrayList<>(taskRunners);

        for (RemoteTaskRunnerBackend backend : backends) {
            allTaskRunners.add(new RemoteTaskRunner(backend, applicationProperties));
        }

        Map<String, TaskRunner> taskRunnersByType = new LinkedHashMap<>();

        List<TaskRunner> sortedTaskRunners = allTaskRunners.stream()
            .sorted(Comparator.comparing(TaskRunner::getType))
            .toList();

        for (TaskRunner taskRunner : sortedTaskRunners) {
            taskRunnersByType.put(taskRunner.getType(), taskRunner);
        }

        this.taskRunnerMap = taskRunnersByType;
        this.applicationProperties = applicationProperties;
    }
```

(imports: `java.util.ArrayList`, `com.bytechef.platform.component.runner.external.RemoteTaskRunner`, `com.bytechef.platform.component.runner.remote.RemoteTaskRunnerBackend`.)

Update `TaskRunnerRegistryTest.newRegistryImpl` / `newRegistry` to pass `List.of()` as the second argument, and add:

```java
    @Test
    public void testWrapsEveryBackendInARemoteTaskRunner() {
        RemoteTaskRunnerBackend backend = mock(RemoteTaskRunnerBackend.class);

        when(backend.getType()).thenReturn("docker");

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);

        applicationProperties.getScript()
            .getRunners()
            .put("docker", runner);

        TaskRunnerRegistry taskRunnerRegistry = new TaskRunnerRegistryImpl(
            List.of(), List.of(backend), applicationProperties);

        assertThat(taskRunnerRegistry.getTaskRunner("docker")).isInstanceOf(RemoteTaskRunner.class);
    }
```

In `CommandsComponentHandlerTest.createTaskRunnerRegistry()` and `ScriptComponentHandlerTest`'s equivalent, insert `List.of(),` as the second constructor argument (the Docker runner stays in the first list until Task 8).

- [ ] **Step 7: Write the orchestrator test against a fake backend**

`R_IMPL/src/test/java/com/bytechef/platform/component/runner/external/RemoteTaskRunnerTest.java`:

```java
package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.Deadline;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobExit;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemoteLogSink;
import com.bytechef.platform.component.runner.remote.RemoteTaskRunnerBackend;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class RemoteTaskRunnerTest {

    private static final String SCRIPT = "def perform(input, context):\n    return 1\n";

    @Test
    void testASuccessfulRunReturnsTheOutputAndDeletesTheJob() {
        FakeBackend backend = new FakeBackend();

        backend.stdout = "hello";

        TaskRunnerResult result = runner(backend).run(request(Map.of(), 7L));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("hello");
        assertThat(result.output()).isEqualTo(Map.of("ok", true));
        assertThat(backend.cleanupModes).containsExactly(CleanupMode.DELETE);
        assertThat(backend.submittedSpec.key()
            .value()).isEqualTo("7");
        assertThat(backend.submittedSpec.environment()).containsEntry("BYTECHEF_INPUT_FILE", "/work/input.json")
            .containsEntry("HOME", "/work");
        assertThat(backend.submittedEntries).contains("input.json", "script.py", "output/");
    }

    @Test
    void testDeleteOffDetachesAfterSuccess() {
        FakeBackend backend = new FakeBackend();

        runner(backend).run(request(Map.of("delete", false), 7L));

        assertThat(backend.cleanupModes).containsExactly(CleanupMode.DETACH);
    }

    @Test
    void testANonZeroExitFailsWithBothTailsAndTheReason() {
        FakeBackend backend = new FakeBackend();

        backend.exit = RemoteJobExit.of(3, "OOMKilled");
        backend.stdout = "out-before";
        backend.stderr = "boom";

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of(), 7L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("code 3")
            .hasMessageContaining("OOMKilled")
            .hasMessageContaining("out-before")
            .hasMessageContaining("boom");

        assertThat(backend.cleanupModes).containsExactly(CleanupMode.DELETE);
    }

    @Test
    void testATimeoutKillsTheJob() {
        FakeBackend backend = new FakeBackend();

        backend.exit = RemoteJobExit.timedOut();

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of("waitUntilCompletion", "PT1S"), 7L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("timed out")
            .hasMessageContaining("PT1S");

        assertThat(backend.cleanupModes).containsExactly(CleanupMode.KILL_AND_DELETE);
    }

    @Test
    void testAFailureWhileCollectingKillsTheJob() {
        FakeBackend backend = new FakeBackend();

        backend.collectFails = true;

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of(), 7L))).isInstanceOf(RuntimeException.class);

        assertThat(backend.cleanupModes).containsExactly(CleanupMode.KILL_AND_DELETE);
    }

    @Test
    void testResumeReattachesToAnExistingJobInsteadOfSubmitting() {
        FakeBackend backend = new FakeBackend();

        backend.existing = new RemoteJobHandle("already-running");

        runner(backend).run(request(Map.of(), 7L));

        assertThat(backend.submittedSpec).isNull();
        assertThat(backend.awaitedHandle.id()).isEqualTo("already-running");
    }

    @Test
    void testResumeOffNeverLooksForAnExistingJob() {
        FakeBackend backend = new FakeBackend();

        backend.existing = new RemoteJobHandle("already-running");

        runner(backend).run(request(Map.of("resume", false), 7L));

        assertThat(backend.findCalls).isZero();
        assertThat(backend.submittedSpec).isNotNull();
    }

    @Test
    void testANullTaskExecutionIdGetsARandomKeyAndNeverResumes() {
        FakeBackend backend = new FakeBackend();

        backend.existing = new RemoteJobHandle("already-running");

        RemoteTaskRunner runner = new RemoteTaskRunner(backend, applicationProperties(Map.of()), () -> "random-1");

        runner.run(request(Map.of(), null));

        assertThat(backend.findCalls).isZero();
        assertThat(backend.submittedSpec.key()
            .value()).isEqualTo("random-1");
    }

    @Test
    void testAnInterruptionDetachesWhenResumeIsOn() throws InterruptedException {
        FakeBackend backend = new FakeBackend();

        backend.blockInAwait = true;

        assertInterruptedCleanup(runner(backend), request(Map.of(), 7L), backend, CleanupMode.DETACH);
    }

    @Test
    void testAnInterruptionKillsWhenResumeIsOff() throws InterruptedException {
        FakeBackend backend = new FakeBackend();

        backend.blockInAwait = true;

        assertInterruptedCleanup(
            runner(backend), request(Map.of("resume", false), 7L), backend, CleanupMode.KILL_AND_DELETE);
    }

    @Test
    void testWaitUntilCompletionIsClampedToTheOperatorsMaxWait() {
        FakeBackend backend = new FakeBackend();

        RemoteTaskRunner runner = new RemoteTaskRunner(
            backend, applicationProperties(Map.of("max-wait", "PT2S")), () -> "k");

        runner.run(request(Map.of("waitUntilCompletion", "PT48H"), 7L));

        assertThat(backend.completionBudget).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void testTheStderrFileFromTheOutputArchiveFeedsTheStderrTail() {
        FakeBackend backend = new FakeBackend();

        backend.stderrFile = "from-the-file";

        TaskRunnerResult result = runner(backend).run(request(Map.of(), 7L));

        assertThat(result.stderr()).contains("from-the-file");
    }

    @Test
    void testAMissingOutputArchiveIsNotAFailure() {
        FakeBackend backend = new FakeBackend();

        backend.outputPresent = false;

        TaskRunnerResult result = runner(backend).run(request(Map.of(), 7L));

        assertThat(result.output()).isNull();
    }

    @Test
    void testADeclaredConnectionIsRequiredWhenTheBackendWantsOne() {
        FakeBackend backend = new FakeBackend();

        backend.connection = new TaskRunnerConnection("kubernetes", 1);

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.validate(request(Map.of(), 7L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fake runner needs a connection");

        TaskRunnerRequest connected = request(
            Map.of(), 7L, Map.of("taskRunner", new ComponentConnection("kubernetes", 1, 5L, Map.of(), null)));

        runner.validate(connected);
    }

    @Test
    void testGetPropertiesAppendsTheSharedOnesAfterTheBackends() {
        FakeBackend backend = new FakeBackend();

        assertThat(runner(backend).getProperties())
            .extracting(ModifiableValueProperty::getName)
            .containsExactly("image", "pullPolicy", "resume", "delete", "waitUntilRunning", "waitUntilCompletion");
    }

    @Test
    void testCleanupModeTable() {
        assertThat(RemoteTaskRunner.cleanupMode(RemoteTaskRunner.Outcome.SUCCESS, true, true))
            .isEqualTo(CleanupMode.DELETE);
        assertThat(RemoteTaskRunner.cleanupMode(RemoteTaskRunner.Outcome.FAILURE, true, false))
            .isEqualTo(CleanupMode.DETACH);
        assertThat(RemoteTaskRunner.cleanupMode(RemoteTaskRunner.Outcome.TIMEOUT, true, false))
            .isEqualTo(CleanupMode.KILL_AND_DELETE);
        assertThat(RemoteTaskRunner.cleanupMode(RemoteTaskRunner.Outcome.ERROR, true, false))
            .isEqualTo(CleanupMode.KILL_AND_DELETE);
        assertThat(RemoteTaskRunner.cleanupMode(RemoteTaskRunner.Outcome.INTERRUPTED, true, true))
            .isEqualTo(CleanupMode.DETACH);
        assertThat(RemoteTaskRunner.cleanupMode(RemoteTaskRunner.Outcome.INTERRUPTED, false, true))
            .isEqualTo(CleanupMode.KILL_AND_DELETE);
    }

    private static void assertInterruptedCleanup(
        RemoteTaskRunner runner, TaskRunnerRequest request, FakeBackend backend, CleanupMode expected)
        throws InterruptedException {

        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                runner.run(request);
            } catch (RuntimeException exception) {
                failure.set(exception);
            }
        });

        thread.start();

        assertThat(backend.awaitReached.await(10, TimeUnit.SECONDS)).isTrue();

        thread.interrupt();
        thread.join(Duration.ofSeconds(10)
            .toMillis());

        assertThat(thread.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(backend.cleanupModes).containsExactly(expected);
    }

    private static ApplicationProperties applicationProperties(Map<String, String> runnerProperties) {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(runnerProperties);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("fake", runner);

        applicationProperties.getScript()
            .setRunners(runners);

        return applicationProperties;
    }

    private static RemoteTaskRunner runner(FakeBackend backend) {
        return new RemoteTaskRunner(backend, applicationProperties(Map.of()), () -> "random");
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters, @Nullable Long taskExecutionId) {
        return request(runnerParameters, taskExecutionId, Map.of());
    }

    private static TaskRunnerRequest request(
        Map<String, ?> runnerParameters, @Nullable Long taskExecutionId,
        Map<String, ComponentConnection> connections) {

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getTaskExecutionId()).thenReturn(taskExecutionId);
        when(actionContext.getJobId()).thenReturn(taskExecutionId == null ? null : 100L);

        return new TaskRunnerRequest(
            "python", SCRIPT, List.of(), Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(runnerParameters), null, connections, actionContext);
    }

    static final class FakeBackend implements RemoteTaskRunnerBackend {

        private final CountDownLatch awaitReached = new CountDownLatch(1);
        private final List<CleanupMode> cleanupModes = new ArrayList<>();

        private @Nullable RemoteJobHandle existing;
        private @Nullable RemoteJobSpec submittedSpec;
        private List<String> submittedEntries = List.of();
        private @Nullable RemoteJobHandle awaitedHandle;
        private @Nullable Duration completionBudget;
        private @Nullable TaskRunnerConnection connection;
        private RemoteJobExit exit = RemoteJobExit.of(0);
        private String stdout = "";
        private String stderr = "";
        private @Nullable String stderrFile;
        private boolean outputPresent = true;
        private boolean collectFails;
        private boolean blockInAwait;
        private int findCalls;

        @Override
        public String getType() {
            return "fake";
        }

        @Override
        public String getTitle() {
            return "Fake";
        }

        @Override
        public String getRemoteWorkingDirectory() {
            return "/work";
        }

        @Override
        public List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties) {
            return List.of(com.bytechef.component.definition.ComponentDsl.string("image"));
        }

        @Override
        public Optional<TaskRunnerConnection> getConnection() {
            return Optional.ofNullable(connection);
        }

        @Override
        public void validate(TaskRunnerRequest request, RemoteJobContext context) {
        }

        @Override
        public Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context) {
            findCalls++;

            return Optional.ofNullable(existing);
        }

        @Override
        public RemoteJobHandle submit(
            RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
            throws IOException {

            submittedSpec = spec;
            submittedEntries = ContainerArchiveTest.entryNames(inputArchive);

            return new RemoteJobHandle("job-1");
        }

        @Override
        public RemoteJobExit await(
            RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
            RemoteJobContext context) throws InterruptedException {

            awaitedHandle = handle;
            completionBudget = completionDeadline.getBudget();

            awaitReached.countDown();

            if (blockInAwait) {
                Thread.sleep(Duration.ofMinutes(5)
                    .toMillis());
            }

            if (!stdout.isEmpty()) {
                byte[] bytes = stdout.getBytes(StandardCharsets.UTF_8);

                logSink.appendStdout(bytes, bytes.length);
            }

            if (!stderr.isEmpty()) {
                byte[] bytes = stderr.getBytes(StandardCharsets.UTF_8);

                logSink.appendStderr(bytes, bytes.length);
            }

            return exit;
        }

        @Override
        public @Nullable InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
            throws IOException {

            if (collectFails) {
                throw new IOException("collect failed");
            }

            if (!outputPresent) {
                return null;
            }

            Path path = Files.createTempDirectory("bytechef-remote-test-");

            try {
                Path outputPath = Files.createDirectory(path.resolve("output"));

                Files.writeString(outputPath.resolve("output.json"), "{\"ok\": true}", StandardCharsets.UTF_8);

                if (stderrFile != null) {
                    Files.writeString(path.resolve(".stderr"), stderrFile, StandardCharsets.UTF_8);
                }

                try (InputStream inputStream = ContainerArchive.toTar(path)) {
                    return new ByteArrayInputStream(inputStream.readAllBytes());
                }
            } finally {
                try (Stream<Path> paths = Files.walk(path)) {
                    List<Path> candidates = paths.sorted(Comparator.reverseOrder())
                        .toList();

                    for (Path candidate : candidates) {
                        Files.deleteIfExists(candidate);
                    }
                }
            }
        }

        @Override
        public void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context) {
            cleanupModes.add(cleanupMode);
        }
    }
}
```

`ContainerArchiveTest.entryNames(InputStream)` does not exist yet; add this package-private static helper to `ContainerArchiveTest`:

```java
    static List<String> entryNames(InputStream inputStream) throws IOException {
        List<String> names = new ArrayList<>();

        try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream)) {
            TarArchiveEntry entry;

            while ((entry = tarArchiveInputStream.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }

        return names;
    }
```

- [ ] **Step 8: Run the orchestrator and registry tests**

Run: `./gradlew :R_IMPL:spotlessApply :R_IMPL:test -q`
Expected: PASS (the Docker tests still pass; they do not touch the orchestrator yet). If `testAnInterruption…` hangs, check that `await` is the method that blocks and that `run()` restores the interrupt flag after cleanup.

Then: `./gradlew :server:libs:modules:components:commands:test :server:libs:modules:components:script:test -q` — the handler tests must compile with the extra `List.of()` argument. Definition snapshots are unchanged in this task (the registry's runner list is the same).

- [ ] **Step 9: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-runner server/libs/modules/components/commands/src/test server/libs/modules/components/script/src/test
git commit -m "Add the remote task runner orchestrator"
```

---
### Task 7: Process runner — `inheritEnvironment` becomes a plain property (fold)

**Files:**
- Modify: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/external/ProcessTaskRunner.java`
- Modify: `R_IMPL/src/test/java/com/bytechef/platform/component/runner/external/ProcessTaskRunnerTest.java`
- Modify: `server/libs/modules/components/script/src/main/resources/README.mdx` (section "Inheriting The Host Environment")
- Modify: `server/libs/modules/components/commands/src/main/resources/README.mdx` (operator-configuration yml and the `process.inherit-environment-enabled` bullet)

**Interfaces:**
- Produces: the `inheritEnvironment` boolean property is always offered (default `false`) and honoured; the operator flag `inherit-environment-enabled` no longer exists.

- [ ] **Step 1: Change the tests first**

In `ProcessTaskRunnerTest`, delete `testValidateRejectsInheritEnvironmentWhenTheOperatorHasNotEnabledIt`, `testValidateAcceptsInheritEnvironmentWhenTheOperatorEnabledIt`, `testInheritEnvironmentIsHiddenWhenTheOperatorHasNotEnabledIt`, `testInheritEnvironmentIsOfferedWhenTheOperatorEnabledIt`. Change `testAnInheritedEnvironmentKeepsTheServerAccountsHome` to use `processTaskRunner` (the field built from `new ApplicationProperties()`) instead of `enabledTaskRunner`, and delete the now-unused `applicationProperties(...)` helper and `inheritEnvironmentRequest()` if nothing else uses them. Add:

```java
    @Test
    void testInheritEnvironmentIsAlwaysOfferedAndOffByDefault() {
        ValueProperty<?> property = processTaskRunner.getProperties()
            .stream()
            .filter(candidate -> "inheritEnvironment".equals(candidate.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(property.getDefaultValue()).contains(false);
    }
```

(import `com.bytechef.component.definition.Property.ValueProperty`).

- [ ] **Step 2: Run, expect failure**

Run: `./gradlew :R_IMPL:test --tests '*ProcessTaskRunnerTest' -q`
Expected: `testInheritEnvironmentIsAlwaysOfferedAndOffByDefault` and `testAnInheritedEnvironmentKeepsTheServerAccountsHome` FAIL.

- [ ] **Step 3: Implement**

In `ProcessTaskRunner`:
- delete the constant `INHERIT_ENVIRONMENT_ENABLED`, the method `isInheritEnvironmentEnabled()`, the `TaskRunnerOperatorFlag` import, and the `applicationProperties` field plus constructor parameter (the class then has a no-arg constructor; keep `@Component`);
- in `getProperties()`, drop the `if (isInheritEnvironmentEnabled())` guard so the `bool(INHERIT_ENVIRONMENT)` property is always added, with description `"Passes the server process's own environment to the execution. Off by default, because that environment holds the server's credentials."`;
- in `validate(...)`, delete the `isInheritEnvironmentRequested(request) && !isInheritEnvironmentEnabled()` block.

Update every `new ProcessTaskRunner(applicationProperties)` call site to `new ProcessTaskRunner()` (`CommandsComponentHandlerTest`, `ScriptComponentHandlerTest`, `ProcessTaskRunnerTest`). Check with `grep -rn "new ProcessTaskRunner(" server --include='*.java'`.

- [ ] **Step 4: Docs**

`script/README.mdx`, section `### Inheriting The Host Environment`, replace the second paragraph with:

> A workflow author can ask for the server's full environment instead, through the runner's **Inherit Environment** option. It is off by default; turning it on hands the child process everything the server process holds, including datasource passwords and cloud credentials, so use it only on a deployment where workflow authors are trusted with the server itself.

`commands/README.mdx`: in the operator-configuration yml remove the `properties:` / `inherit-environment-enabled: false` lines under `process:`; delete the `process.inherit-environment-enabled` bullet; in the sentence "Every key these three runners read" nothing else changes in this task (Docker keys are rewritten in Task 8).

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew :R_IMPL:spotlessApply :R_IMPL:test --tests '*ProcessTaskRunnerTest' :server:libs:modules:components:commands:test :server:libs:modules:components:script:test -q`
Expected: PASS.

This is a **new** commit, not a fold: the constructor change ripples into the handler tests, which later commits (`f93f1de5222`) rewrite, so a fixup below them would conflict and leave intermediate commits uncompilable.

```bash
git add server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl server/libs/modules/components/commands server/libs/modules/components/script
git commit -m "Offer the process runner's inherit environment option to every workflow"
```

---
### Task 8: Docker as a remote backend, Kestra-aligned

**Files:**
- Create: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/external/DockerTaskRunnerBackend.java`
- Delete: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/external/DockerTaskRunner.java`
- Rewrite: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/external/DockerOperatorSettings.java`
- Tests: create `DockerTaskRunnerBackendTest.java` and `DockerTaskRunnerBackendIntTest.java` (from the old `DockerTaskRunnerTest`/`DockerTaskRunnerIntTest`, which are deleted); rewrite `DockerOperatorSettingsTest.java`
- Modify: `CommandsComponentHandlerTest`, `ScriptComponentHandlerTest` (registry construction), definition snapshots `server/libs/modules/components/commands/src/test/resources/definition/commands_v1.json`, `server/libs/modules/components/script/src/test/resources/definition/script_v1.json`
- Modify: `commands/README.mdx`, `script/README.mdx` (Docker sections)

**Interfaces:**
- Consumes: Task 3 SPI, Task 6 orchestrator.
- Produces: `DockerTaskRunnerBackend` (`@Component`), constructors `()` and package-private `(BiFunction<String, Duration, DockerClient> dockerClientFactory)` where the `String` is the resolved daemon host (`""` = ambient); `DockerOperatorSettings.of(Map<String, String> properties)` with `getHost()`, `isTaskHostAllowed()`, `isVolumeMountAllowed()`; constants `ALLOW_TASK_HOST_PROPERTY = "allow-task-host"`, `ALLOW_VOLUME_MOUNTS_PROPERTY = "allow-volume-mounts"`, `HOST_PROPERTY = "host"`; labels `io.bytechef.managed-by`, `io.bytechef.task-execution-id`, `io.bytechef.job-id`.

- [ ] **Step 1: Rewrite `DockerOperatorSettingsTest`**

```java
package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DockerOperatorSettingsTest {

    @Test
    void testNothingConfiguredMeansTheAmbientDaemonAndEveryGateClosed() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(Map.of());

        assertThat(settings.getHost()).isEmpty();
        assertThat(settings.isTaskHostAllowed()).isFalse();
        assertThat(settings.isVolumeMountAllowed()).isFalse();
    }

    @Test
    void testTheOperatorsHostIsTrimmed() {
        assertThat(DockerOperatorSettings.of(Map.of("host", " tcp://daemon:2375 "))
            .getHost()).isEqualTo("tcp://daemon:2375");
    }

    @Test
    void testGatesOpenOnlyOnLiteralTrue() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            Map.of("allow-task-host", "TRUE", "allow-volume-mounts", "yes"));

        assertThat(settings.isTaskHostAllowed()).isTrue();
        assertThat(settings.isVolumeMountAllowed()).isFalse();
    }
}
```

- [ ] **Step 2: Rewrite `DockerOperatorSettings`**

```java
package com.bytechef.platform.component.runner.external;

import java.util.Map;

public final class DockerOperatorSettings {

    public static final String ALLOW_TASK_HOST_PROPERTY = "allow-task-host";
    public static final String ALLOW_VOLUME_MOUNTS_PROPERTY = "allow-volume-mounts";
    public static final String HOST_PROPERTY = "host";

    private final String host;
    private final boolean taskHostAllowed;
    private final boolean volumeMountAllowed;

    private DockerOperatorSettings(String host, boolean taskHostAllowed, boolean volumeMountAllowed) {
        this.host = host;
        this.taskHostAllowed = taskHostAllowed;
        this.volumeMountAllowed = volumeMountAllowed;
    }

    public static DockerOperatorSettings of(Map<String, String> properties) {
        String host = properties.get(HOST_PROPERTY);

        return new DockerOperatorSettings(
            host == null ? "" : host.trim(),
            Boolean.parseBoolean(properties.get(ALLOW_TASK_HOST_PROPERTY)),
            Boolean.parseBoolean(properties.get(ALLOW_VOLUME_MOUNTS_PROPERTY)));
    }

    public String getHost() {
        return host;
    }

    public boolean isTaskHostAllowed() {
        return taskHostAllowed;
    }

    public boolean isVolumeMountAllowed() {
        return volumeMountAllowed;
    }
}
```

Run: `./gradlew :R_IMPL:test --tests '*DockerOperatorSettingsTest' -q` → PASS (the old `DockerTaskRunner` no longer compiles; that is expected until Step 4; if Gradle refuses to compile the test source set, do Steps 3–4 first and come back).

- [ ] **Step 3: Delete the old runner and write the backend**

`git rm` `DockerTaskRunner.java`, `DockerTaskRunnerTest.java`, `DockerTaskRunnerIntTest.java` (the two tests are recreated under new names in Step 5).

`DockerTaskRunnerBackend.java`:

```java
package com.bytechef.platform.component.runner.external;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.CPU;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.CREDENTIALS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.DOCKER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENTRYPOINT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.EXTRA_HOSTS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.HOST;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.IMAGE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.KILL_GRACE_PERIOD;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.MEMORY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.NETWORK_MODE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PASSWORD;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PRIVILEGED;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.REGISTRY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.REGISTRY_TOKEN;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.USER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.USERNAME;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.VOLUMES;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerOperatorFlag;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.Deadline;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobExit;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemoteLogSink;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import com.bytechef.platform.component.runner.remote.RemoteTaskRunnerBackend;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.WaitResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings("EI")
public class DockerTaskRunnerBackend implements RemoteTaskRunnerBackend {

    static final String LABEL_JOB_ID = "io.bytechef.job-id";
    static final String LABEL_MANAGED_BY = "io.bytechef.managed-by";
    static final String LABEL_TASK_EXECUTION_ID = "io.bytechef.task-execution-id";

    private static final Logger log = LoggerFactory.getLogger(DockerTaskRunnerBackend.class);

    private static final String CONTAINER_DIRECTORY = "/tmp";
    private static final Duration CLIENT_GRACE = Duration.ofSeconds(30);
    private static final Duration LOG_DRAIN_GRACE = Duration.ofSeconds(5);
    private static final long NANO_CPUS_PER_CPU = 1_000_000_000L;
    private static final Set<String> RESUMABLE_STATES = Set.of("created", "running", "exited");

    private static final Pattern MEMORY_PATTERN = Pattern.compile("^(\\d+)\\s*([bkmg])?$", Pattern.CASE_INSENSITIVE);

    private static final Map<ExternalLanguage, String> DEFAULT_IMAGES = Map.of(
        ExternalLanguage.JAVASCRIPT, "node:22-alpine",
        ExternalLanguage.PYTHON, "python:3.12-slim",
        ExternalLanguage.SHELL, "ubuntu:24.04");

    private final BiFunction<String, Duration, DockerClient> dockerClientFactory;

    @Autowired
    public DockerTaskRunnerBackend() {
        this(DockerTaskRunnerBackend::createDockerClient);
    }

    DockerTaskRunnerBackend(BiFunction<String, Duration, DockerClient> dockerClientFactory) {
        this.dockerClientFactory = dockerClientFactory;
    }

    @Override
    public String getType() {
        return DOCKER;
    }

    @Override
    public String getTitle() {
        return "Docker";
    }

    @Override
    public String getRemoteWorkingDirectory() {
        return CONTAINER_DIRECTORY;
    }

    @Override
    public List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties) {
        DockerOperatorSettings settings = DockerOperatorSettings.of(operatorProperties);

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(
            string(IMAGE)
                .label("Image")
                .description("The container image the task runs in. Defaults to the image for the action's language.")
                .required(false));
        properties.add(
            array(ENTRYPOINT)
                .label("Entrypoint")
                .description(
                    "Replaces the interpreter the source is handed to. Defaults to the interpreter for the action's " +
                        "language; the source file is appended either way.")
                .items(string())
                .required(false));
        properties.add(
            string(USER)
                .label("User")
                .description("The user the container runs as, as a name or a numeric uid[:gid].")
                .required(false));
        properties.add(
            number(CPU)
                .label("CPU")
                .description("How many CPUs the container may use, as a decimal fraction of one CPU.")
                .required(false));
        properties.add(
            string(MEMORY)
                .label("Memory")
                .description("How much memory the container may use, in bytes or with a b, k, m or g suffix.")
                .required(false));
        properties.add(
            string(NETWORK_MODE)
                .label("Network Mode")
                .description("The network the container is attached to: bridge, none, host, or a named network.")
                .required(false));
        properties.add(
            array(EXTRA_HOSTS)
                .label("Extra Hosts")
                .description("Additional entries for the container's hosts file, each written as host:ip.")
                .items(string())
                .required(false));
        properties.add(
            bool(PRIVILEGED)
                .label("Privileged")
                .description("Gives the container every capability of the daemon's host.")
                .defaultValue(false)
                .required(false));
        properties.add(
            object(CREDENTIALS)
                .label("Registry Credentials")
                .description("How the image is pulled from a private registry.")
                .properties(
                    string(REGISTRY).label("Registry")
                        .required(false),
                    string(USERNAME).label("Username")
                        .required(false),
                    string(PASSWORD).label("Password")
                        .required(false),
                    string(REGISTRY_TOKEN).label("Registry Token")
                        .required(false))
                .required(false));
        properties.add(
            string(KILL_GRACE_PERIOD)
                .label("Kill Grace Period")
                .description(
                    "How long a still-running container is given to stop before it is killed, as an ISO-8601 " +
                        "duration. Zero kills it at once.")
                .defaultValue("PT0S")
                .required(false));

        if (settings.isTaskHostAllowed()) {
            properties.add(
                string(HOST)
                    .label("Docker Host")
                    .description(
                        "The Docker API this task talks to, such as unix:///var/run/docker.sock or " +
                            "tcp://daemon:2375. Defaults to the operator's daemon.")
                    .required(false));
        }

        if (settings.isVolumeMountAllowed()) {
            properties.add(
                array(VOLUMES)
                    .label("Volumes")
                    .description("Bind mounts, each written as host-path:container-path[:ro].")
                    .items(string())
                    .required(false));
        }

        return properties;
    }

    @Override
    public Optional<TaskRunnerConnection> getConnection() {
        return Optional.empty();
    }

    @Override
    public void validate(TaskRunnerRequest request, RemoteJobContext context) {
        DockerOperatorSettings settings = DockerOperatorSettings.of(context.operatorProperties());

        Parameters parameters = request.runnerParameters();

        resolveImage(request);
        resolveNanoCpus(parameters);
        resolveMemoryBytes(parameters);
        resolveKillGracePeriod(parameters);

        if (getString(parameters, HOST) != null && !settings.isTaskHostAllowed()) {
            throw new IllegalArgumentException(
                "The docker runner's '%s' option is not enabled. An operator must enable it with %s=true.".formatted(
                    HOST, TaskRunnerOperatorFlag.getPropertyName(DOCKER, DockerOperatorSettings.ALLOW_TASK_HOST_PROPERTY)));
        }

        List<String> volumes = parameters.getList(VOLUMES, String.class, List.of());

        if (!volumes.isEmpty() && !settings.isVolumeMountAllowed()) {
            throw new IllegalArgumentException(
                "The docker runner's '%s' option is not enabled. An operator must enable it with %s=true.".formatted(
                    VOLUMES,
                    TaskRunnerOperatorFlag.getPropertyName(DOCKER, DockerOperatorSettings.ALLOW_VOLUME_MOUNTS_PROPERTY)));
        }

        for (String volume : volumes) {
            Bind.parse(volume);
        }
    }

    @Override
    public Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context) {
        try (DockerClient dockerClient = client(context, CLIENT_GRACE);
            ListContainersCmd listContainersCmd = dockerClient.listContainersCmd()) {

            listContainersCmd.withShowAll(true);
            listContainersCmd.withLabelFilter(Map.of(LABEL_TASK_EXECUTION_ID, key.value()));

            for (Container container : listContainersCmd.exec()) {
                String state = container.getState();

                if (state != null && RESUMABLE_STATES.contains(state.toLowerCase(Locale.ROOT))) {
                    return Optional.of(new RemoteJobHandle(container.getId()));
                }
            }

            return Optional.empty();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not list the docker runner's containers", exception);
        }
    }

    @Override
    public RemoteJobHandle submit(
        RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
        throws IOException, InterruptedException {

        Parameters parameters = spec.runnerParameters();

        String image = resolveImage(spec.languageId(), parameters);

        try (DockerClient dockerClient = client(context, runningDeadline.remaining()
            .plus(CLIENT_GRACE))) {

            pullImage(dockerClient, image, spec.pullPolicy(), parameters, runningDeadline);

            runningDeadline.check(DOCKER, "creating the container");

            String containerId = createContainer(dockerClient, image, spec, parameters);

            try {
                copyWorkingDirectoryIn(dockerClient, containerId, inputArchive);

                runningDeadline.check(DOCKER, "starting the container");

                try (StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId)) {
                    startContainerCmd.exec();
                }
            } catch (IOException | RuntimeException exception) {
                removeContainer(dockerClient, containerId);

                throw exception;
            }

            return new RemoteJobHandle(containerId);
        }
    }

    @Override
    public RemoteJobExit await(
        RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
        RemoteJobContext context) throws IOException, InterruptedException {

        try (DockerClient dockerClient = client(context, completionDeadline.remaining()
            .plus(CLIENT_GRACE))) {

            LogCallback logCallback = attachLogs(dockerClient, handle.id(), logSink);

            try (WaitCallback waitCallback = new WaitCallback();
                WaitContainerCmd waitContainerCmd = dockerClient.waitContainerCmd(handle.id())) {

                waitContainerCmd.exec(waitCallback);

                boolean exited = waitCallback.awaitCompletion(
                    completionDeadline.remainingMillis(), TimeUnit.MILLISECONDS);

                if (!exited) {
                    return RemoteJobExit.timedOut();
                }

                Integer statusCode = waitCallback.getStatusCode();

                return new RemoteJobExit(true, statusCode, null);
            } finally {
                drainLogs(logCallback);
            }
        }
    }

    @Override
    public @Nullable InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
        throws IOException {

        DockerClient dockerClient = client(context, completionDeadline.remaining()
            .plus(CLIENT_GRACE));

        try {
            CopyArchiveFromContainerCmd copyArchiveFromContainerCmd = dockerClient.copyArchiveFromContainerCmd(
                handle.id(), CONTAINER_DIRECTORY + "/" + RemoteJobSpec.OUTPUT_DIRECTORY_NAME);

            InputStream inputStream = copyArchiveFromContainerCmd.exec();

            return new FilterInputStream(inputStream) {

                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        copyArchiveFromContainerCmd.close();
                        dockerClient.close();
                    }
                }
            };
        } catch (NotFoundException exception) {
            dockerClient.close();

            log.debug("The container {} left no output directory", handle.id(), exception);

            return null;
        } catch (IOException | RuntimeException exception) {
            dockerClient.close();

            throw exception;
        }
    }

    @Override
    public void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context) {
        if (cleanupMode == CleanupMode.DETACH) {
            return;
        }

        try (DockerClient dockerClient = client(context, CLIENT_GRACE)) {
            if (cleanupMode == CleanupMode.DELETE) {
                Duration killGracePeriod = handleKillGracePeriod(handle);

                if (!killGracePeriod.isZero()) {
                    stopContainer(dockerClient, handle.id(), killGracePeriod);
                }
            }

            removeContainer(dockerClient, handle.id());
        } catch (IOException | RuntimeException exception) {
            log.warn("Could not clean up the container {}", handle.id(), exception);
        }
    }

    private DockerClient client(RemoteJobContext context, Duration responseTimeout) {
        DockerOperatorSettings settings = DockerOperatorSettings.of(context.operatorProperties());

        String taskHost = context.taskHost();

        String host = taskHost != null && settings.isTaskHostAllowed() ? taskHost : settings.getHost();

        return dockerClientFactory.apply(host, responseTimeout);
    }
```

`handleKillGracePeriod(handle)` reads the grace period back from the handle's attributes; in `submit`, build the handle as `new RemoteJobHandle(containerId, Map.of(KILL_GRACE_PERIOD, resolveKillGracePeriod(parameters).toString()))` and implement:

```java
    private static Duration handleKillGracePeriod(RemoteJobHandle handle) {
        String value = handle.attributes()
            .get(KILL_GRACE_PERIOD);

        return value == null ? Duration.ZERO : Duration.parse(value);
    }
```

Continue the class with the private helpers:

```java
    private static LogCallback attachLogs(DockerClient dockerClient, String containerId, RemoteLogSink logSink) {
        LogCallback logCallback = new LogCallback(logSink);

        try (LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId)) {
            logContainerCmd.withFollowStream(true);
            logContainerCmd.withStdOut(true);
            logContainerCmd.withStdErr(true);
            logContainerCmd.withTailAll();

            logContainerCmd.exec(logCallback);
        } catch (RuntimeException exception) {
            closeQuietly(logCallback);

            throw exception;
        }

        return logCallback;
    }

    private static void closeQuietly(LogCallback logCallback) {
        try {
            logCallback.close();
        } catch (IOException | RuntimeException exception) {
            log.warn("Could not close the container's log stream", exception);
        }
    }

    private static void drainLogs(LogCallback logCallback) {
        try {
            boolean drained = logCallback.awaitCompletion(LOG_DRAIN_GRACE.toMillis(), TimeUnit.MILLISECONDS);

            if (!drained) {
                log.warn(
                    "The docker task runner stopped waiting for the container's log stream after {}; the captured " +
                        "output may be incomplete",
                    LOG_DRAIN_GRACE);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            log.debug("Interrupted while draining the container's log stream", exception);
        } catch (RuntimeException exception) {
            log.warn("Could not drain the container's log stream", exception);
        }

        closeQuietly(logCallback);
    }

    private static void pullImage(
        DockerClient dockerClient, String image, RemotePullPolicy pullPolicy, Parameters parameters,
        Deadline runningDeadline) throws IOException, InterruptedException {

        if (pullPolicy == RemotePullPolicy.NEVER) {
            return;
        }

        if (pullPolicy == RemotePullPolicy.IF_NOT_PRESENT && isImagePresent(dockerClient, image)) {
            return;
        }

        runningDeadline.check(DOCKER, "pulling the image");

        try (PullImageResultCallback pullImageResultCallback = new PullImageResultCallback();
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image)) {

            AuthConfig authConfig = authConfig(parameters);

            if (authConfig != null) {
                pullImageCmd.withAuthConfig(authConfig);
            }

            pullImageCmd.exec(pullImageResultCallback);

            boolean pulled = pullImageResultCallback.awaitCompletion(
                runningDeadline.remainingMillis(), TimeUnit.MILLISECONDS);

            if (!pulled) {
                throw new IllegalStateException(
                    "The docker task runner timed out after %s pulling image '%s'".formatted(
                        runningDeadline.getBudget(), image));
            }
        }
    }

    @Nullable
    private static AuthConfig authConfig(Parameters parameters) {
        Map<String, Object> credentials = parameters.getMap(CREDENTIALS, Object.class, Map.of());

        if (credentials.isEmpty()) {
            return null;
        }

        AuthConfig authConfig = new AuthConfig();

        Object registry = credentials.get(REGISTRY);
        Object username = credentials.get(USERNAME);
        Object password = credentials.get(PASSWORD);
        Object registryToken = credentials.get(REGISTRY_TOKEN);

        if (registry != null) {
            authConfig.withRegistryAddress(registry.toString());
        }

        if (username != null) {
            authConfig.withUsername(username.toString());
        }

        if (password != null) {
            authConfig.withPassword(password.toString());
        }

        if (registryToken != null) {
            authConfig.withRegistrytoken(registryToken.toString());
        }

        return authConfig;
    }

    private static boolean isImagePresent(DockerClient dockerClient, String image) {
        try (InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd(image)) {
            inspectImageCmd.exec();

            return true;
        } catch (NotFoundException exception) {
            log.debug("Image {} is not present on the daemon", image, exception);

            return false;
        }
    }

    private static String createContainer(
        DockerClient dockerClient, String image, RemoteJobSpec spec, Parameters parameters) {

        try (CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image)) {
            createContainerCmd.withHostConfig(createHostConfig(parameters));
            createContainerCmd.withWorkingDir(CONTAINER_DIRECTORY);
            createContainerCmd.withEntrypoint(spec.interpreter());
            createContainerCmd.withCmd(spec.remoteSourcePath());
            createContainerCmd.withEnv(
                spec.environment()
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList());
            createContainerCmd.withLabels(labels(spec.key()));
            createContainerCmd.withAttachStdout(true);
            createContainerCmd.withAttachStderr(true);
            createContainerCmd.withTty(false);

            String user = getString(parameters, USER);

            if (user != null) {
                createContainerCmd.withUser(user);
            }

            CreateContainerResponse createContainerResponse = createContainerCmd.exec();

            return createContainerResponse.getId();
        }
    }

    private static Map<String, String> labels(RemoteJobKey key) {
        Map<String, String> labels = new LinkedHashMap<>();

        labels.put(LABEL_MANAGED_BY, "bytechef");
        labels.put(LABEL_TASK_EXECUTION_ID, key.value());

        Long jobId = key.jobId();

        if (jobId != null) {
            labels.put(LABEL_JOB_ID, String.valueOf(jobId));
        }

        return labels;
    }

    private static HostConfig createHostConfig(Parameters parameters) {
        HostConfig hostConfig = HostConfig.newHostConfig();

        String networkMode = getString(parameters, NETWORK_MODE);

        if (networkMode != null) {
            hostConfig.withNetworkMode(networkMode);
        }

        Long nanoCpus = resolveNanoCpus(parameters);

        if (nanoCpus != null) {
            hostConfig.withNanoCPUs(nanoCpus);
        }

        Long memoryBytes = resolveMemoryBytes(parameters);

        if (memoryBytes != null) {
            hostConfig.withMemory(memoryBytes);
        }

        if (parameters.getBoolean(PRIVILEGED, false)) {
            hostConfig.withPrivileged(true);
        }

        List<String> extraHosts = parameters.getList(EXTRA_HOSTS, String.class, List.of());

        if (!extraHosts.isEmpty()) {
            hostConfig.withExtraHosts(extraHosts.toArray(new String[0]));
        }

        List<String> volumes = parameters.getList(VOLUMES, String.class, List.of());

        if (!volumes.isEmpty()) {
            hostConfig.withBinds(
                volumes.stream()
                    .map(Bind::parse)
                    .toList());
        }

        return hostConfig;
    }

    private static void copyWorkingDirectoryIn(DockerClient dockerClient, String containerId, InputStream inputArchive)
        throws IOException {

        try (CopyArchiveToContainerCmd copyArchiveToContainerCmd = dockerClient.copyArchiveToContainerCmd(containerId)) {
            copyArchiveToContainerCmd.withRemotePath(CONTAINER_DIRECTORY);
            copyArchiveToContainerCmd.withTarInputStream(inputArchive);

            copyArchiveToContainerCmd.exec();
        }
    }

    private static void stopContainer(DockerClient dockerClient, String containerId, Duration killGracePeriod) {
        try (StopContainerCmd stopContainerCmd = dockerClient.stopContainerCmd(containerId)) {
            stopContainerCmd.withTimeout((int) Math.max(1, killGracePeriod.toSeconds()));

            stopContainerCmd.exec();
        } catch (NotFoundException exception) {
            log.debug("The container {} is already gone", containerId, exception);
        } catch (RuntimeException exception) {
            log.debug("Could not stop the container {} gracefully", containerId, exception);
        }
    }

    private static void removeContainer(DockerClient dockerClient, String containerId) {
        try (RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containerId)) {
            removeContainerCmd.withForce(true);
            removeContainerCmd.withRemoveVolumes(true);

            removeContainerCmd.exec();
        } catch (RuntimeException exception) {
            log.warn("Could not remove the container {}", containerId, exception);
        }
    }

    private static String resolveImage(TaskRunnerRequest request) {
        return resolveImage(request.languageId(), request.runnerParameters());
    }

    private static String resolveImage(String languageId, Parameters parameters) {
        String image = getString(parameters, IMAGE);

        if (image != null) {
            return image;
        }

        String defaultImage = DEFAULT_IMAGES.get(ExternalLanguage.of(languageId));

        if (defaultImage == null) {
            throw new IllegalArgumentException(
                "The docker runner has no default image for language '%s'; name one on the task runner"
                    .formatted(languageId));
        }

        return defaultImage;
    }

    @Nullable
    private static String getString(Parameters parameters, String name) {
        String value = parameters.getString(name);

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Duration resolveKillGracePeriod(Parameters parameters) {
        String value = getString(parameters, KILL_GRACE_PERIOD);

        if (value == null) {
            return Duration.ZERO;
        }

        Duration duration;

        try {
            duration = Duration.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "The docker runner cannot read '%s' as the ISO-8601 duration %s expects".formatted(
                    value, KILL_GRACE_PERIOD),
                exception);
        }

        if (duration.isNegative()) {
            throw new IllegalArgumentException("The docker runner's %s must not be negative".formatted(KILL_GRACE_PERIOD));
        }

        return duration;
    }

    @Nullable
    private static Long resolveNanoCpus(Parameters parameters) {
        Double cpu = parameters.getDouble(CPU);

        if (cpu == null) {
            return null;
        }

        if (cpu <= 0) {
            throw new IllegalArgumentException("The docker runner's cpu limit must be greater than zero");
        }

        return Math.round(cpu * NANO_CPUS_PER_CPU);
    }

    @Nullable
    private static Long resolveMemoryBytes(Parameters parameters) {
        String memory = getString(parameters, MEMORY);

        if (memory == null) {
            return null;
        }

        Matcher matcher = MEMORY_PATTERN.matcher(memory);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "The docker runner cannot read '%s' as a memory limit; write bytes, or a number with a b, k, m or g suffix"
                    .formatted(memory));
        }

        long value;

        try {
            value = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "The docker runner's memory limit '%s' is too large".formatted(memory), exception);
        }

        long multiplier = memoryMultiplier(matcher.group(2));

        if (value <= 0 || value > Long.MAX_VALUE / multiplier) {
            throw new IllegalArgumentException(
                "The docker runner's memory limit '%s' is out of range".formatted(memory));
        }

        return value * multiplier;
    }

    private static long memoryMultiplier(@Nullable String suffix) {
        if (suffix == null) {
            return 1L;
        }

        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "k" -> 1024L;
            case "m" -> 1024L * 1024;
            case "g" -> 1024L * 1024 * 1024;
            default -> 1L;
        };
    }

    private static DockerClient createDockerClient(String host, Duration responseTimeout) {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        if (!host.isBlank()) {
            builder.withDockerHost(host);
        }

        DefaultDockerClientConfig dockerClientConfig = builder.build();

        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.getDockerHost())
            .sslConfig(dockerClientConfig.getSSLConfig())
            .responseTimeout(responseTimeout)
            .build();

        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }

    private static final class WaitCallback extends ResultCallback.Adapter<WaitResponse> {

        private final AtomicReference<Integer> statusCode = new AtomicReference<>();

        @Override
        public void onNext(WaitResponse waitResponse) {
            statusCode.set(waitResponse.getStatusCode());
        }

        @Nullable
        Integer getStatusCode() {
            return statusCode.get();
        }
    }

    private static final class LogCallback extends ResultCallback.Adapter<Frame> {

        private final RemoteLogSink logSink;

        LogCallback(RemoteLogSink logSink) {
            this.logSink = logSink;
        }

        @Override
        public void onNext(Frame frame) {
            byte[] payload = frame.getPayload();

            if (payload == null) {
                return;
            }

            if (frame.getStreamType() == StreamType.STDERR) {
                logSink.appendStderr(payload, payload.length);
            } else {
                logSink.appendStdout(payload, payload.length);
            }
        }
    }
}
```

Register the backend bean: `@Component` is enough (component scanning already picks up `DockerTaskRunner`'s package). In `CommandsComponentHandlerTest` / `ScriptComponentHandlerTest`, construct the registry as `new TaskRunnerRegistryImpl(List.of(new GraalVmTaskRunner(null, applicationProperties), new ProcessTaskRunner()), List.of(new DockerTaskRunnerBackend()), applicationProperties)`.

- [ ] **Step 4: Compile**

Run: `./gradlew :R_IMPL:spotlessApply :R_IMPL:compileJava -q`
Expected: clean. Fix any import the plan missed rather than changing behaviour.

- [ ] **Step 5: Recreate the tests**

`DockerTaskRunnerBackendTest.java` is the old `DockerTaskRunnerTest` with these changes:

1. Class name, and `@ExtendWith(ObjectMapperSetupExtension.class)` stays.
2. The field becomes `private final DockerTaskRunnerBackend backend = new DockerTaskRunnerBackend();` and the `validate`/`getProperties` tests call it with an explicit context/operator map:

```java
    private static RemoteJobContext context(Map<String, String> operatorProperties, @Nullable String taskHost) {
        return new RemoteJobContext(null, operatorProperties, taskHost);
    }
```

3. `TestDaemon.newTaskRunner()` returns the orchestrator around the backend, so the end-to-end tests keep their shape:

```java
        private RemoteTaskRunner newTaskRunner() {
            stubInspectImage();
            stubPullImage();
            stubCreateContainer();
            stubStartAndCopyIn();
            stubLogs();
            stubWait();
            stubCopyOut();
            stubRemoveContainer();
            stubListContainers();

            return new RemoteTaskRunner(
                new DockerTaskRunnerBackend((host, timeout) -> {
                    hosts.add(host);

                    return dockerClient;
                }),
                applicationProperties(runnerProperties), () -> "test-key");
        }

        private void stubListContainers() {
            ListContainersCmd listContainersCmd = mock(ListContainersCmd.class, RETURNS_SELF);

            when(listContainersCmd.exec()).thenReturn(existingContainers);

            when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        }
```

   with new `TestDaemon` fields `private final List<String> hosts = new ArrayList<>();` and `private List<Container> existingContainers = List.of();`, and `runnerProperties` defaulting to `Map.of()`. `applicationProperties(...)` keeps registering the runner under `"docker"`. Because `dockerClient.close()` is now called once per backend call, stub nothing for it (Mockito's default no-op is fine).

4. **Delete** these tests (allowlists, ceilings and network gating are gone): `testTheImagePropertyIsOfferedEvenWhenTheAllowlistIsEmpty`, `testTheImagePropertyOffersTheAllowedImagesAsOptions`, `testTheNetworkModeSelectHidesHostUnlessTheOperatorAllowedIt`, `testTheNetworkModeSelectOffersHostWhenTheOperatorAllowedIt`, `testValidateAcceptsAnAllowedImage`, `testValidateRejectsAnImageOutsideTheAllowlist`, `testValidateRejectsAPrefixOfAnAllowedImage`, `testAnEmptyAllowlistPermitsNothing`, `testADefaultedImageStillHasToBeOnTheAllowlist`, `testAnEmptyAllowlistPermitsNotEvenTheDefaultImage`, `testValidateRejectsHostNetworkingUnlessTheOperatorEnabledIt`, `testValidateAcceptsHostNetworkingWhenTheOperatorEnabledIt`, `testValidateRejectsANamedDockerNetwork`, `testValidateRejectsJoiningAnotherContainersNetworkNamespace`, `testValidateRejectsAContainerNamespaceThatCouldCarryHostNetworkingTransitively`, `testTheAcceptedNetworkModesAreExactlyTheOnesTheSelectOffers`, `testAForkBombShapedRequestStillGetsABoundedContainer`, `testThePidsLimitIsSetEvenWhenTheWorkflowNamedItsOwnLimits`, `testTheOperatorsPidsLimitReachesTheHostConfig`, `testAnOperatorCannotAskForAnUnboundedPidsLimit`, `testAMalformedOperatorCeilingRefusesTheExecutionRatherThanBecomingUnlimited`, `testTheOperatorsDefaultCpuAndMemoryReachAContainerThatAskedForNeither`, `testACpuRequestAboveTheOperatorsMaximumIsRejected`, `testAMemoryRequestAboveTheOperatorsMaximumIsRejected`, `testTheMaximumsApplyEvenWhenTheOperatorNamedNone`, `testAnOperatorDefaultAboveTheirOwnMaximumIsHeldToTheMaximum`, `testAnUnsetNetworkModeStillResolvesToBridgeByDefault`, `testAnOperatorAllowingOnlyNoneGetsNoneForAWorkflowThatNamedNoMode`, `testAnOperatorAllowingOnlyNoneRejectsARequestForBridge`, `testHostStillNeedsItsOwnFlagWhateverTheModeAllowlistSays`, `testTheModeAllowlistCanTakeHostAwayFromAnOperatorWhoFlaggedIt`, `testAModeAllowlistNamingNothingKnownRefusesEveryExecution`, `testValidateRejectsAnUnknownPullPolicy` (now covered by `RemoteTaskRunnerSettingsTest`).

5. **Keep** the rest, replacing `dockerTaskRunner.validate(request(...))` with `backend.validate(request(...), context(Map.of(), null))`, `propertyNames(dockerTaskRunner)` with `propertyNames(backend, Map.of())`, and `"image", ALLOWED_IMAGE` runner parameters as they are (the value is now just an image name, nothing is allow-listed). `testGetPropertiesOffersEveryDockerSetting` asserts `containsExactlyInAnyOrder("image", "entrypoint", "user", "cpu", "memory", "networkMode", "extraHosts", "privileged", "credentials", "killGracePeriod")`. `testATimeoutRemovesTheContainer` and `testAnInterruptionRemovesTheContainer` stay valid: the request's context is a plain `ActionContext` mock, so the key is not resumable and the orchestrator kills on interrupt.

6. **Add** these tests:

```java
    @Test
    void testAPerTaskHostIsRejectedUnlessTheOperatorAllowedIt() {
        assertThatThrownBy(
            () -> backend.validate(request(Map.of("host", "tcp://elsewhere:2375")), context(Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytechef.script.runners.docker.properties.allow-task-host");

        backend.validate(
            request(Map.of("host", "tcp://elsewhere:2375")), context(Map.of("allow-task-host", "true"), null));
    }

    @Test
    void testTheHostPropertyIsOfferedOnlyWhenTheOperatorAllowedIt() {
        assertThat(propertyNames(backend, Map.of())).doesNotContain("host");
        assertThat(propertyNames(backend, Map.of("allow-task-host", "true"))).contains("host");
    }

    @Test
    void testAnAllowedPerTaskHostReachesTheClientFactory() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of("allow-task-host", "true", "host", "unix:///operator.sock");

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT, Map.of("host", "tcp://elsewhere:2375"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        assertThat(testDaemon.hosts).isNotEmpty()
            .allMatch("tcp://elsewhere:2375"::equals);
    }

    @Test
    void testTheOperatorsHostIsUsedWhenTheTaskNamesNone() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of("host", "unix:///operator.sock");

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(testDaemon.hosts).isNotEmpty()
            .allMatch("unix:///operator.sock"::equals);
    }

    @Test
    void testVolumesAreRejectedUnlessTheOperatorAllowedThem() {
        assertThatThrownBy(
            () -> backend.validate(request(Map.of("volumes", List.of("/data:/data:ro"))), context(Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytechef.script.runners.docker.properties.allow-volume-mounts");

        assertThat(propertyNames(backend, Map.of())).doesNotContain("volumes");
        assertThat(propertyNames(backend, Map.of("allow-volume-mounts", "true"))).contains("volumes");
    }

    @Test
    void testAnAllowedVolumeBecomesABind() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of("allow-volume-mounts", "true");

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT, Map.of("volumes", List.of("/data:/data:ro")), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        assertThat(testDaemon.hostConfig.getBinds()).hasSize(1);
    }

    @Test
    void testPrivilegedReachesTheHostConfig() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT, TEST_TIMEOUT, Map.of("privileged", true), Map.of()));

        assertThat(testDaemon.hostConfig.getPrivileged()).isTrue();
    }

    @Test
    void testANamedNetworkIsPassedThrough() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT, TEST_TIMEOUT, Map.of("networkMode", "bytechef_default"), Map.of()));

        assertThat(testDaemon.hostConfig.getNetworkMode()).isEqualTo("bytechef_default");
    }

    @Test
    void testNoLimitsAreSetWhenTheWorkflowNamedNone() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(testDaemon.hostConfig.getNanoCPUs()).isNull();
        assertThat(testDaemon.hostConfig.getMemory()).isNull();
        assertThat(testDaemon.hostConfig.getPidsLimit()).isNull();
    }

    @Test
    void testRegistryCredentialsReachThePull() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = false;

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT,
            Map.of("credentials", Map.of("registry", "ghcr.io", "username", "u", "password", "p")), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.pullImageCmd).withAuthConfig(any());
    }

    @Test
    void testTheContainerCarriesTheResumeLabels() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(testDaemon.labels)
            .containsEntry("io.bytechef.managed-by", "bytechef")
            .containsEntry("io.bytechef.task-execution-id", "test-key");
    }

    @Test
    void testFindReturnsARunningContainerWithTheLabel() {
        TestDaemon testDaemon = new TestDaemon();

        Container container = mock(Container.class);

        when(container.getId()).thenReturn("c-9");
        when(container.getState()).thenReturn("running");

        testDaemon.existingContainers = List.of(container);

        testDaemon.newTaskRunner();

        Optional<RemoteJobHandle> handle = new DockerTaskRunnerBackend((host, timeout) -> testDaemon.dockerClient)
            .find(new RemoteJobKey("7", null), context(Map.of(), null));

        assertThat(handle).map(RemoteJobHandle::id)
            .contains("c-9");
    }

    @Test
    void testDetachLeavesTheContainerAlone() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner();

        new DockerTaskRunnerBackend((host, timeout) -> testDaemon.dockerClient)
            .cleanup(new RemoteJobHandle("c-9"), CleanupMode.DETACH, context(Map.of(), null));

        assertThat(testDaemon.removedContainerIds).isEmpty();
    }
```

   For `testRegistryCredentialsReachThePull` keep a `pullImageCmd` field on `TestDaemon` (the mock created in `stubPullImage`). For `testTheContainerCarriesTheResumeLabels`, capture labels in `stubCreateContainer`: `when(createContainerCmd.withLabels(any())).thenAnswer(invocation -> { labels = invocation.getArgument(0); return createContainerCmd; });` with `private Map<String, String> labels = Map.of();`. Add `private static final String SCRIPT = "def perform(input, context):\n    return 1\n";` and use it where the old file inlined the string. Helper signatures: `propertyNames(DockerTaskRunnerBackend backend, Map<String, String> operatorProperties)` calls `backend.getProperties(operatorProperties)`.

`DockerTaskRunnerBackendIntTest.java` is the old `DockerTaskRunnerIntTest` with: the class renamed; `taskRunner(String allowedImages)` replaced by

```java
    private static RemoteTaskRunner taskRunner() {
        return new RemoteTaskRunner(new DockerTaskRunnerBackend(), applicationProperties(), () -> "int-test");
    }
```

   where `applicationProperties()` registers an enabled `docker` runner with no properties; delete `testAnImageOutsideTheAllowlistIsRejected`; and add a resume round-trip:

```java
    @Test
    void testAContainerLeftBehindIsReattachedByTheNextRunWithTheSameKey() {
        RemoteTaskRunner first = new RemoteTaskRunner(
            new DockerTaskRunnerBackend(), applicationProperties(), () -> "resume-key");

        TaskRunnerRequest detached = commandsRequest(
            List.of("sleep 2", "echo '{\"ok\": true}' > \"$BYTECHEF_OUTPUT_DIR/output.json\"", "echo done"),
            Map.of(), List.of(), Map.of("image", IMAGE, "delete", false), new LinkedHashMap<>());

        TaskRunnerResult firstResult = first.run(detached);

        assertThat(firstResult.exitCode()).isZero();

        TaskRunnerRequest reattach = commandsRequest(
            List.of("echo never-runs"), Map.of(), List.of(), Map.of("image", IMAGE), new LinkedHashMap<>(),
            resumableActionContext(42L));

        RemoteTaskRunner second = new RemoteTaskRunner(
            new DockerTaskRunnerBackend(), applicationProperties(), () -> "unused");

        TaskRunnerResult secondResult = second.run(reattach);

        assertThat(secondResult.stdout()).doesNotContain("never-runs");
    }
```

   This needs the first run to use a resumable key too: make both requests use `resumableActionContext(42L)`, an `ActionContextAware` mock whose `getTaskExecutionId()` returns `42L` and whose `file(...)` behaves like `storingActionContext` (mock `ActionContextAware.class` instead of `ActionContext.class` inside a shared helper), and add an overload of `commandsRequest` that takes the context. After the test, the detached container from the first run has been removed by the second run's `DELETE` cleanup.

- [ ] **Step 6: Run the runner tests**

Run: `./gradlew :R_IMPL:spotlessApply :R_IMPL:test -q`
Expected: PASS. Then `./gradlew :R_IMPL:testIntegration -q` with a Docker daemon available: PASS (skipped with an assumption message when no daemon answers).

- [ ] **Step 7: Definition snapshots and handler tests**

`CommandsComponentHandlerTest.testEveryActionOffersTheDockerRunnerProperties` asserts `containsKeys(CPU, ENTRYPOINT, EXTRA_HOSTS, IMAGE, MEMORY, NETWORK_MODE, PULL_POLICY, USER, PRIVILEGED, CREDENTIALS, KILL_GRACE_PERIOD, RESUME, DELETE, WAIT_UNTIL_RUNNING, WAIT_UNTIL_COMPLETION)` (add the static imports). Then regenerate the snapshots:

```bash
rm server/libs/modules/components/commands/src/test/resources/definition/commands_v1.json server/libs/modules/components/script/src/test/resources/definition/script_v1.json
rm -rf server/libs/modules/components/commands/build/resources/test/definition server/libs/modules/components/script/build/resources/test/definition
./gradlew :server:libs:modules:components:commands:test :server:libs:modules:components:script:test -q
```

Expected: PASS, and both `.json` files are regenerated. Inspect the diff of each: the `taskRunner` property must now list `resume`, `delete`, `waitUntilRunning`, `waitUntilCompletion`, `privileged`, `credentials`, `killGracePeriod`, and no `host`/`volumes` (the test registry sets no operator flags). Note `image` no longer carries `options`.

- [ ] **Step 8: Docs**

`commands/README.mdx`:
- In `### Operator configuration`, replace the yml block and the bullet list so only these keys remain: `graalvm.enabled`/`trusted-enabled`, `process.enabled`, `docker.enabled` with `host: ""`, `allow-task-host: false`, `allow-volume-mounts: false`, and the new `runner-limits` block (`max-wait: PT1H`, `max-log-bytes: 1MB`, `max-transfer-bytes: 512MB`). Bullets: keep `graalvm.trusted-enabled` and `docker.host`; add `docker.allow-task-host` ("lets a workflow name its own Docker API through the runner's `host` property; whoever controls that daemon controls every container it starts, so leave it off unless workflow authors are trusted with the daemons they can reach") and `docker.allow-volume-mounts` ("offers the runner's `volumes` property; a host bind mount chosen by a workflow author is a container escape"); add a `runner-limits` bullet ("ceilings on what a remote execution may cost this server: the longest a worker waits on one job, the most log bytes it keeps, and the most bytes it moves in or out; any runner may override them under its own `properties`"). Delete the `allowed-images`, `allowed-network-modes`, `allow-host-network`, ceilings bullets and the "an operator upgrading…" sentence.
- In `### The Docker runner`, the property table becomes: `image` (optional, defaults below), `entrypoint`, `user`, `cpu`, `memory`, `networkMode` ("`bridge`, `none`, `host` or a named network; left blank the daemon's default applies"), `extraHosts`, `privileged`, `credentials` ("`registry`, `username`, `password`, `registryToken` for a private registry"), `killGracePeriod`, `host` ("only when the operator set `allow-task-host`"), `volumes` ("only when the operator set `allow-volume-mounts`; each entry `host-path:container-path[:ro]`"), and the shared properties `pullPolicy`, `resume`, `delete`, `waitUntilRunning`, `waitUntilCompletion` with the descriptions from `RemoteTaskRunnerSettings.properties()`.
- Delete the paragraphs **"The image allowlist is fail-closed…"**, **"Every container is bounded…"** with its table and "A malformed ceiling…", **"An operator chooses which networks…"** with its yml, and **"There is no `volumes` property."**; keep **"`HOME` is the working directory under both runners."** Add one paragraph: **"Resource limits are the workflow's own choice.** `cpu` and `memory` are applied exactly as written and nothing is applied when they are blank. The operator's only ceilings are the `runner-limits` above, which bound what the *server* spends on a job, not what the container may use."
- Add a paragraph **"A restart does not lose a running container."** explaining that every container carries `io.bytechef.task-execution-id`, that a redelivered task execution finds it and waits for it instead of starting another (`resume`), and that `delete: false` keeps the finished container for inspection.
- In the paragraph that says the generated page's registry "enables only `process`", leave the wording; it is still true.

`script/README.mdx`, `### Task Runners` table, Docker row: "Runs the script in a single-use container, on the operator's daemon or — when the operator allows it — on the daemon the task names. The task chooses the image, CPU, memory and network; the operator caps only how long the server waits and how much it transfers. Enable with `bytechef.script.runners.docker.enabled=true`. This is the same runner the **Commands** component uses; its README documents every property." In `### Language Support By Runner`, delete the sentence starting "This is the same two-layer arrangement the Docker runner's image and network-mode allowlists use".

- [ ] **Step 9: Commit**

Run `./gradlew :R_IMPL:check :server:libs:modules:components:commands:check :server:libs:modules:components:script:check -q` → PASS.

```bash
git add -A -- server/libs/platform/platform-component/platform-component-runner server/libs/modules/components/commands server/libs/modules/components/script
git commit -m "Run the Docker runner through the remote task runner orchestrator"
```

---
### Task 9: `WorkflowDefinitionPreprocessor` SPI in atlas

**Files:**
- Create: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/workflow/preprocessor/WorkflowDefinitionPreprocessor.java`
- Create: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/workflow/mapper/WorkflowDefinitionCodec.java`
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-service/src/main/java/com/bytechef/atlas/configuration/service/WorkflowServiceImpl.java`
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-config/src/main/java/com/bytechef/atlas/configuration/config/WorkflowConfiguration.java`
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/util/WorkflowTaskUtils.java` (`isWorkflowTaskMap` → `public static`)
- Tests: `…/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/workflow/mapper/WorkflowDefinitionCodecTest.java`; extend `…/atlas-configuration-service/src/test/java/com/bytechef/atlas/configuration/service/WorkflowServiceIntTest.java`

**Interfaces:**
- Produces: `interface WorkflowDefinitionPreprocessor { boolean process(Map<String, Object> workflowDefinition); }` (mutate in place, return `true` when changed); `WorkflowDefinitionCodec.read(String, Format)` → ordered `Map<String, Object>`, `WorkflowDefinitionCodec.write(Map<String, Object>, Format)`; `WorkflowServiceImpl(CacheManager, List<WorkflowCrudRepository>, List<WorkflowRepository>, List<WorkflowDefinitionPreprocessor>)` (the 3-arg constructor stays and delegates with `List.of()`); `WorkflowTaskUtils.isWorkflowTaskMap(Map<?, ?>)` public. Task 10 implements the SPI.

- [ ] **Step 1: Codec test**

```java
package com.bytechef.atlas.configuration.workflow.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
public class WorkflowDefinitionCodecTest {

    @Test
    public void testJsonRoundTripKeepsKeyOrder() {
        String definition = "{\"label\": \"L\", \"tasks\": [{\"name\": \"a\", \"type\": \"script/v1/python\"}]}";

        Map<String, Object> map = WorkflowDefinitionCodec.read(definition, Format.JSON);

        assertThat(map).isInstanceOf(LinkedHashMap.class);
        assertThat(map.keySet()).containsExactly("label", "tasks");

        String written = WorkflowDefinitionCodec.write(map, Format.JSON);

        assertThat(WorkflowDefinitionCodec.read(written, Format.JSON)).isEqualTo(map);
        assertThat(written).contains("\n");
    }

    @Test
    public void testYamlRoundTrip() {
        String definition = "label: L\ntasks:\n  - name: a\n    type: script/v1/python\n";

        Map<String, Object> map = WorkflowDefinitionCodec.read(definition, Format.YAML);

        assertThat(map).containsEntry("label", "L");
        assertThat(map.get("tasks")).isEqualTo(List.of(Map.of("name", "a", "type", "script/v1/python")));

        String written = WorkflowDefinitionCodec.write(map, Format.YAML);

        assertThat(WorkflowDefinitionCodec.read(written, Format.YAML)).isEqualTo(map);
    }
}
```

- [ ] **Step 2: Run, expect failure**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests '*WorkflowDefinitionCodecTest' -q`
Expected: FAIL — class missing.

- [ ] **Step 3: Implement the SPI and the codec**

`WorkflowDefinitionPreprocessor.java`:

```java
package com.bytechef.atlas.configuration.workflow.preprocessor;

import java.util.Map;

/**
 * Rewrites a workflow definition before it is saved. Implementations mutate the map in place and return whether they
 * changed anything, so an untouched definition is stored exactly as it was given.
 */
public interface WorkflowDefinitionPreprocessor {

    boolean process(Map<String, Object> workflowDefinition);
}
```

`WorkflowDefinitionCodec.java`:

```java
package com.bytechef.atlas.configuration.workflow.mapper;

import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.commons.util.JsonUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

public final class WorkflowDefinitionCodec {

    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    private WorkflowDefinitionCodec() {
    }

    public static Map<String, Object> read(String definition, Format format) {
        Map<String, Object> map = switch (format) {
            case JSON -> JsonUtils.read(definition, new TypeReference<LinkedHashMap<String, Object>>() {});
            case YAML -> YAML_OBJECT_MAPPER.readValue(definition, new TypeReference<LinkedHashMap<String, Object>>() {});
        };

        return map == null ? new LinkedHashMap<>() : map;
    }

    public static String write(Map<String, Object> definition, Format format) {
        return switch (format) {
            case JSON -> JsonUtils.writeWithDefaultPrettyPrinter(definition);
            case YAML -> YAML_OBJECT_MAPPER.writeValueAsString(definition);
        };
    }
}
```

`JsonUtils.read(String, TypeReference)` uses the Jackson 3 `tools.jackson.core.type.TypeReference`; if the project's `JsonUtils` signature expects a different `TypeReference` type, use that one. Make `WorkflowTaskUtils.isWorkflowTaskMap` `public static`.

- [ ] **Step 4: Apply preprocessors in `WorkflowServiceImpl`**

Add the field `private final List<WorkflowDefinitionPreprocessor> workflowDefinitionPreprocessors;`, keep the 3-arg constructor delegating to a new 4-arg one, and:

```java
    @Override
    public Workflow create(String definition, Format format, SourceType sourceType) {
        Assert.notNull(definition, "'definition' must not be null");
        Assert.notNull(format, "'format' must not be null");
        Assert.notNull(sourceType, "'sourceType' must not be null");

        final Workflow workflow = new Workflow(preprocess(definition, format), format);
        …
    }

    @Override
    public Workflow update(String id, String definition, int version) {
        Assert.notNull(id, "'id' must not be null");
        Assert.notNull(definition, "'definition' must not be null");

        final Workflow workflow = getWorkflow(id);

        String preprocessedDefinition = preprocess(definition, workflow.getFormat());

        return CollectionUtils.getFirstFilter(
            workflowCrudRepositories,
            workflowCrudRepository -> workflowCrudRepository.findById(id)
                .isPresent(),
            workflowCrudRepository -> update(preprocessedDefinition, version, workflow, workflowCrudRepository));
    }

    private String preprocess(String definition, Format format) {
        if (workflowDefinitionPreprocessors.isEmpty()) {
            return definition;
        }

        Map<String, Object> definitionMap = WorkflowDefinitionCodec.read(definition, format);

        boolean changed = false;

        for (WorkflowDefinitionPreprocessor workflowDefinitionPreprocessor : workflowDefinitionPreprocessors) {
            if (workflowDefinitionPreprocessor.process(definitionMap)) {
                changed = true;
            }
        }

        return changed ? WorkflowDefinitionCodec.write(definitionMap, format) : definition;
    }
```

(remove the stale `// TODO Validate definition…` line while there — `TODO` comments fail Checkstyle; a `Map` import is needed.)

`WorkflowConfiguration.workflowService(...)` gains a `List<WorkflowDefinitionPreprocessor> workflowDefinitionPreprocessors` parameter and passes it through (Spring injects an empty list when no bean exists).

- [ ] **Step 5: Integration test**

In `WorkflowServiceIntTest` register a preprocessor bean in `WorkflowConfigurationIntTestConfiguration`:

```java
        @Bean
        WorkflowDefinitionPreprocessor labelUpperCasingPreprocessor() {
            return workflowDefinition -> {
                Object label = workflowDefinition.get("label");

                if (label instanceof String string && !string.equals(string.toUpperCase())) {
                    workflowDefinition.put("label", string.toUpperCase());

                    return true;
                }

                return false;
            };
        }
```

pass it to `new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories, List.of(labelUpperCasingPreprocessor()))` in the `workflowService` bean, and add:

```java
    @Test
    public void testCreateRunsThePreprocessors() {
        Workflow workflow = workflowService.create(
            "{\"label\": \"lower\", \"tasks\": []}", Workflow.Format.JSON, Workflow.SourceType.JDBC);

        Assertions.assertEquals("LOWER", workflow.getLabel());
    }

    @Test
    public void testUpdateRunsThePreprocessorsAndLeavesAnUnchangedDefinitionVerbatim() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        Workflow updated = workflowService.update(
            Validate.notNull(workflow.getId(), "id"), "{\"label\": \"lower\",\"tasks\": []}", workflow.getVersion());

        Assertions.assertEquals("LOWER", updated.getLabel());

        String verbatim = "{\"label\": \"ALREADY\",\"tasks\": []}";

        Workflow untouched = workflowService.update(
            Validate.notNull(workflow.getId(), "id"), verbatim, updated.getVersion());

        Assertions.assertEquals(verbatim, untouched.getDefinition());
    }
```

The existing `testUpdate` asserts the definition is stored verbatim; its label `"Label"` is not all upper case, so change that test's definition to `{"label": "LABEL","tasks": []}` so it stays verbatim.

- [ ] **Step 6: Run**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:spotlessApply :server:libs:atlas:atlas-configuration:atlas-configuration-service:spotlessApply :server:libs:atlas:atlas-configuration:atlas-configuration-config:spotlessApply :server:libs:atlas:atlas-configuration:atlas-configuration-api:test :server:libs:atlas:atlas-configuration:atlas-configuration-service:testIntegration -q`
Expected: PASS (Docker needed for Testcontainers).

- [ ] **Step 7: Commit**

```bash
git add server/libs/atlas/atlas-configuration
git commit -m "Add the workflow definition preprocessor"
```

---

### Task 10: Derive the task runner connection on save

**Files:**
- Create: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/TaskRunnerConnectionWorkflowPreprocessor.java`
- Modify: `R_IMPL/build.gradle.kts` — add `implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))`
- Test: `R_IMPL/src/test/java/com/bytechef/platform/component/runner/TaskRunnerConnectionWorkflowPreprocessorTest.java`

**Interfaces:**
- Consumes: Task 1 `getConnection()`, Task 9 SPI and `WorkflowTaskUtils.isWorkflowTaskMap`, `TaskRunnerRegistry.getTaskRunner` (throws `TaskRunnerNotEnabledException` for unknown or disabled types).
- Produces: a `@Component` preprocessor that keeps `task.connections.taskRunner` in step with `task.parameters.taskRunner.type`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.component.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class TaskRunnerConnectionWorkflowPreprocessorTest {

    private final TaskRunnerRegistry taskRunnerRegistry = mock(TaskRunnerRegistry.class);
    private final TaskRunnerConnectionWorkflowPreprocessor preprocessor =
        new TaskRunnerConnectionWorkflowPreprocessor(taskRunnerRegistry);

    @Test
    public void testARunnerWithAConnectionGetsTheEntry() {
        registerRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        Map<String, Object> task = task("kubernetes");
        Map<String, Object> definition = definition(task);

        assertThat(preprocessor.process(definition)).isTrue();
        assertThat(connections(task)).containsEntry(
            "taskRunner", Map.of("componentName", "kubernetes", "componentVersion", 1, "authorizationRequired", true));
    }

    @Test
    public void testAnUnchangedEntryReportsNoChange() {
        registerRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        Map<String, Object> task = task("kubernetes");

        connections(task).put(
            "taskRunner",
            new LinkedHashMap<>(
                Map.of("componentName", "kubernetes", "componentVersion", 1, "authorizationRequired", true)));

        assertThat(preprocessor.process(definition(task))).isFalse();
    }

    @Test
    public void testARunnerWithoutAConnectionRemovesTheEntry() {
        registerRunner("docker", null);

        Map<String, Object> task = task("docker");

        connections(task).put("taskRunner", Map.of("componentName", "kubernetes", "componentVersion", 1));

        assertThat(preprocessor.process(definition(task))).isTrue();
        assertThat(task).doesNotContainKey("connections");
    }

    @Test
    public void testOtherConnectionsSurviveTheRemoval() {
        registerRunner("docker", null);

        Map<String, Object> task = task("docker");

        connections(task).put("taskRunner", Map.of("componentName", "kubernetes", "componentVersion", 1));
        connections(task).put("slack", Map.of("componentName", "slack", "componentVersion", 1));

        preprocessor.process(definition(task));

        assertThat(connections(task)).containsOnlyKeys("slack");
    }

    @Test
    public void testADisabledRunnerLeavesTheConnectionAlone() {
        when(taskRunnerRegistry.getTaskRunner(anyString())).thenThrow(new TaskRunnerNotEnabledException("kubernetes"));

        Map<String, Object> task = task("kubernetes");

        Map<String, Object> existing = Map.of("componentName", "kubernetes", "componentVersion", 1);

        connections(task).put("taskRunner", existing);

        assertThat(preprocessor.process(definition(task))).isFalse();
        assertThat(connections(task)).containsEntry("taskRunner", existing);
    }

    @Test
    public void testATaskWithoutARunnerIsLeftAlone() {
        Map<String, Object> task = new LinkedHashMap<>(Map.of("name", "t", "type", "slack/v1/send"));

        assertThat(preprocessor.process(definition(task))).isFalse();
        assertThat(task).doesNotContainKey("connections");
    }

    @Test
    public void testNestedTasksAreVisitedWherever TheySit() {
        registerRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        Map<String, Object> inner = task("kubernetes");

        Map<String, Object> loop = new LinkedHashMap<>();

        loop.put("name", "loop");
        loop.put("type", "loop/v1");
        loop.put("parameters", new LinkedHashMap<>(Map.of("iteratee", new ArrayList<>(List.of(inner)))));

        assertThat(preprocessor.process(definition(loop))).isTrue();
        assertThat(connections(inner)).containsKey("taskRunner");
    }

    @Test
    public void testAColumnDefinitionThatLooksLikeATaskIsNotATask() {
        registerRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        Map<String, Object> column = new LinkedHashMap<>(
            Map.of("name", "col", "type", "STRING", "parameters", Map.of("taskRunner", Map.of("type", "kubernetes"))));

        Map<String, Object> task = new LinkedHashMap<>(
            Map.of("name", "t", "type", "postgres/v1/create", "parameters", Map.of("columns", List.of(column))));

        assertThat(preprocessor.process(definition(task))).isFalse();
    }

    private void registerRunner(String type, TaskRunnerConnection connection) {
        TaskRunner taskRunner = mock(TaskRunner.class);

        when(taskRunner.getType()).thenReturn(type);
        when(taskRunner.getConnection()).thenReturn(Optional.ofNullable(connection));
        when(taskRunnerRegistry.getTaskRunner(type)).thenReturn(taskRunner);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> connections(Map<String, Object> task) {
        return (Map<String, Object>) task.computeIfAbsent("connections", key -> new LinkedHashMap<String, Object>());
    }

    private static Map<String, Object> definition(Map<String, Object> task) {
        Map<String, Object> definition = new LinkedHashMap<>();

        definition.put("label", "L");
        definition.put("tasks", new ArrayList<>(List.of(task)));

        return definition;
    }

    private static Map<String, Object> task(String runnerType) {
        Map<String, Object> task = new LinkedHashMap<>();

        task.put("name", "script");
        task.put("type", "script/v1/python");
        task.put("parameters", new LinkedHashMap<>(Map.of("taskRunner", new LinkedHashMap<>(Map.of("type", runnerType)))));

        return task;
    }
}
```

(Fix the typo in the method name `testNestedTasksAreVisitedWherever TheySit` → `testNestedTasksAreVisitedWhereverTheySit`.)

- [ ] **Step 2: Run, expect compilation failure**

Run: `./gradlew :R_IMPL:test --tests '*TaskRunnerConnectionWorkflowPreprocessorTest' -q`
Expected: FAIL.

- [ ] **Step 3: Implement**

```java
package com.bytechef.platform.component.runner;

import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;

import com.bytechef.atlas.configuration.util.WorkflowTaskUtils;
import com.bytechef.atlas.configuration.workflow.preprocessor.WorkflowDefinitionPreprocessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Keeps a task's {@code connections.taskRunner} entry in step with the runner it selected, so the editor offers the
 * runner's connection and the runner finds it at run time, whichever client saved the workflow.
 */
@Component
public class TaskRunnerConnectionWorkflowPreprocessor implements WorkflowDefinitionPreprocessor {

    static final String AUTHORIZATION_REQUIRED = "authorizationRequired";
    static final String COMPONENT_NAME = "componentName";
    static final String COMPONENT_VERSION = "componentVersion";
    static final String CONNECTIONS = "connections";
    static final String PARAMETERS = "parameters";

    private final TaskRunnerRegistry taskRunnerRegistry;

    public TaskRunnerConnectionWorkflowPreprocessor(TaskRunnerRegistry taskRunnerRegistry) {
        this.taskRunnerRegistry = taskRunnerRegistry;
    }

    @Override
    public boolean process(Map<String, Object> workflowDefinition) {
        return visit(workflowDefinition);
    }

    @SuppressWarnings("unchecked")
    private boolean visit(Object node) {
        boolean changed = false;

        if (node instanceof Map<?, ?> map) {
            if (WorkflowTaskUtils.isWorkflowTaskMap(map)) {
                changed = processTask((Map<String, Object>) map);
            }

            for (Object value : map.values()) {
                if (visit(value)) {
                    changed = true;
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                if (visit(item)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    @SuppressWarnings("unchecked")
    private boolean processTask(Map<String, Object> task) {
        String runnerType = runnerType(task);

        if (runnerType == null) {
            return false;
        }

        TaskRunner taskRunner;

        try {
            taskRunner = taskRunnerRegistry.getTaskRunner(runnerType);
        } catch (TaskRunnerNotEnabledException exception) {
            return false;
        }

        Optional<TaskRunnerConnection> connection = taskRunner.getConnection();

        Map<String, Object> connections = task.get(CONNECTIONS) instanceof Map<?, ?> map
            ? (Map<String, Object>) map
            : null;

        if (connection.isEmpty()) {
            if (connections == null || !connections.containsKey(TASK_RUNNER)) {
                return false;
            }

            connections.remove(TASK_RUNNER);

            if (connections.isEmpty()) {
                task.remove(CONNECTIONS);
            }

            return true;
        }

        TaskRunnerConnection taskRunnerConnection = connection.get();

        Map<String, Object> entry = new LinkedHashMap<>();

        entry.put(COMPONENT_NAME, taskRunnerConnection.componentName());
        entry.put(COMPONENT_VERSION, taskRunnerConnection.componentVersion());
        entry.put(AUTHORIZATION_REQUIRED, true);

        if (connections == null) {
            connections = new LinkedHashMap<>();

            task.put(CONNECTIONS, connections);
        }

        if (Objects.equals(connections.get(TASK_RUNNER), entry)) {
            return false;
        }

        connections.put(TASK_RUNNER, entry);

        return true;
    }

    private static String runnerType(Map<String, Object> task) {
        if (!(task.get(PARAMETERS) instanceof Map<?, ?> parameters)) {
            return null;
        }

        if (!(parameters.get(TASK_RUNNER) instanceof Map<?, ?> taskRunner)) {
            return null;
        }

        Object type = taskRunner.get(TYPE);

        if (!(type instanceof String string) || string.isBlank()) {
            return null;
        }

        return string;
    }
}
```

`isWorkflowTaskMap` rejects `{"name": "col", "type": "STRING"}` because `STRING` is not `component/vN[/op]`, which is what the last test pins.

- [ ] **Step 4: Run tests and the full module check**

Run: `./gradlew :R_IMPL:spotlessApply :R_IMPL:check -q`
Expected: PASS. Then boot-smoke the server (`./gradlew -p server/apps/server-app bootRun`, wait for "Started", stop) to confirm the preprocessor bean wires: with only CE runners enabled it is inert, so no workflow changes are expected.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-runner
git commit -m "Derive the task runner connection when a workflow is saved"
```

---
### Task 11: Place the fixups and verify every commit builds

**Files:** none (history only).

**Interfaces:** none.

- [ ] **Step 1: Back up and confirm the fixup targets still exist**

```bash
git branch -f backup/0_p_script_runners-pre-fold HEAD
git log --format='%h %s' HEAD | grep -E "^.{11} (fixup! |Add the task runner SPI|Add bounded output capture for external executions)"
```

Expected: exactly two `fixup!` subjects (Tasks 1 and 4) and their two targets. If `git log -1 --format=%h HEAD -- …/TaskRunner.java` (api) is not `25cc7a7f4ff`, the fixup for Task 1 must target that later commit instead; the same check for `BoundedOutputCapture.java` against `9681da0e575` (at the time of writing nothing touches either file later).

- [ ] **Step 2: Build the todo by SHA, not by subject**

`--autosquash` matches by subject text; the two targets here have unique subjects, but drive the todo by SHA anyway so a duplicate subject can never redirect a fixup. Save as `$SCRATCH/todo.py` (scratchpad, not the repo):

```python
import re, subprocess, sys

todo_path = sys.argv[1]
lines = open(todo_path).read().splitlines()

full = lambda sha: subprocess.check_output(["git", "rev-parse", sha]).decode().strip()

targets = {}   # fixup full sha -> target full sha
picks = []
fixups = []

for line in lines:
    m = re.match(r"^pick (\S+) (.*)$", line)
    if not m:
        continue
    sha, subject = m.group(1), m.group(2)
    if subject.startswith("fixup! "):
        fixups.append((full(sha), subject[len("fixup! "):]))
    else:
        picks.append((full(sha), subject))

out = []
placed = 0
for sha, subject in picks:
    out.append(f"pick {sha} {subject}")
    for fsha, fsubject in fixups:
        if fsubject == subject:
            out.append(f"fixup {fsha}")
            placed += 1

if placed != len(fixups):
    sys.exit(f"placed {placed} of {len(fixups)} fixups")

open(todo_path, "w").write("\n".join(out) + "\n")
```

- [ ] **Step 3: Replay with a build check on every commit**

Use the merge-base with `master` as the base so every branch commit is replayed (the script only reorders the two fixups). The `--exec` compiles the modules this plan touches at every commit:

```bash
BASE=$(git merge-base master HEAD)
GIT_SEQUENCE_EDITOR="python3 -I $SCRATCH/todo.py" GIT_EDITOR=true git rebase -i "$BASE" \
  --exec './gradlew -q :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api:compileTestJava :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:compileTestJava :server:libs:modules:components:commands:compileTestJava :server:libs:modules:components:script:compileTestJava :server:libs:atlas:atlas-configuration:atlas-configuration-service:compileTestJava --configure-on-demand'
```

Run this detached if the session may be interrupted (`nohup sh -c '…; echo exit=$? >> $SCRATCH/rebase.log' &`). On an `--exec` failure, fix the commit in place (`git commit --amend` is allowed here, it is your own replayed commit), then `git rebase --continue`. On a content conflict, resolve toward the fixup's intent, `git add`, `git rebase --continue`; check for rerere auto-resolutions (unmerged files with zero `<<<<<<<` markers) before trusting them.

- [ ] **Step 4: Verify losslessness**

```bash
git diff backup/0_p_script_runners-pre-fold HEAD --stat        # must be empty
git log --format=%s HEAD | grep -c '^fixup!'                   # must be 0
git rev-list --count master..HEAD                              # pre-fold count minus 2
./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api:check :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:check :server:libs:modules:components:commands:check :server:libs:modules:components:script:check :server:libs:atlas:atlas-configuration:atlas-configuration-api:check :server:libs:atlas:atlas-configuration:atlas-configuration-service:check :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:check -q
```

Expected: empty diff, zero fixups, the full `check` green.

- [ ] **Step 5: Update the spec's delivery plan**

In `docs/superpowers/specs/2026-10-14-remote-task-runners-design.md` §11, replace the table and the two paragraphs under it with the **Commit map** from the top of this plan plus one sentence: "Docker is reworked in a new commit rather than folded into `0f0319c892a`, because the orchestrator needs `ContainerArchive`, which that commit introduces." Also change §3.3's "A configuration class creates one `RemoteTaskRunner` bean for each backend bean" to "`TaskRunnerRegistryImpl` wraps every `RemoteTaskRunnerBackend` bean in a `RemoteTaskRunner`", §3.1's "Remote working-directory layout, the same for every backend: `/bytechef/work`…" to "the same relative layout (`input.json`, `output/`, `.stderr`) under a base directory each backend chooses (`/tmp` for Docker, `/bytechef/work` for Kubernetes and Batch)", the §3.1 signatures to the tar-stream form used in Task 3, and §4.2's "contributes these … ahead of the backend's own" to "after the backend's own". Commit:

```bash
git add docs/superpowers/specs/2026-10-14-remote-task-runners-design.md
git commit -m "Align the remote task runners design with the core implementation"
```

Do not push. Leave the `backup/…` ref in place until the user has reviewed the branch.
