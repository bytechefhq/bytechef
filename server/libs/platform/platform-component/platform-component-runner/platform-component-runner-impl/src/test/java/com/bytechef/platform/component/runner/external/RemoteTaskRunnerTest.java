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

package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.bytechef.tenant.TenantContext;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
            .value()).isEqualTo("public-100-7");
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
    void testAFailedJobsStderrFileReachesTheFailureMessage() {
        FakeBackend backend = new FakeBackend();

        backend.exit = RemoteJobExit.of(1);
        backend.stderrFile = "Traceback: boom";

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of(), 7L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("code 1")
            .hasMessageContaining("Traceback: boom");
    }

    @Test
    void testAFailedCollectDoesNotHideTheExitCode() {
        FakeBackend backend = new FakeBackend();

        backend.exit = RemoteJobExit.of(1);
        backend.collectFails = true;

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of(), 7L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("code 1");
    }

    @Test
    void testAFailingCleanupDoesNotReplaceTheOriginalFailure() {
        FakeBackend backend = new FakeBackend();

        backend.exit = RemoteJobExit.of(3);
        backend.cleanupFails = true;

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of(), 7L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("code 3");
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
        backend.existingKey = "public-100-7";

        runner(backend).run(request(Map.of(), 7L));

        assertThat(backend.submittedSpec).isNull();
        assertThat(backend.awaitedHandle.id()).isEqualTo("already-running");
    }

    @Test
    void testTheKeyCombinesTheTenantTheJobAndTheTaskExecution() {
        FakeBackend backend = new FakeBackend();

        TenantContext.runWithTenantId("acme", () -> runner(backend).run(request(Map.of(), 7L)));

        assertThat(backend.submittedSpec.key()
            .value()).isEqualTo("acme-100-7");
        assertThat(backend.submittedSpec.key()
            .jobId()).isEqualTo(100L);
    }

    @Test
    void testAMissingJobIdIsWrittenAsNoneInTheKey() {
        FakeBackend backend = new FakeBackend();

        runner(backend).run(request(Map.of(), 7L, Map.of(), null));

        assertThat(backend.submittedSpec.key()
            .value()).isEqualTo("public-none-7");
    }

    @Test
    void testTheSameTaskExecutionIdUnderAnotherTenantNeverReattaches() {
        FakeBackend backend = new FakeBackend();

        TenantContext.runWithTenantId("tenant1", () -> runner(backend).run(request(Map.of("delete", false), 7L)));

        String firstKey = backend.submittedSpec.key()
            .value();

        backend.existing = new RemoteJobHandle("job-1");
        backend.existingKey = firstKey;
        backend.submittedSpec = null;

        TenantContext.runWithTenantId("tenant2", () -> runner(backend).run(request(Map.of(), 7L)));

        assertThat(backend.findCalls).isEqualTo(2);
        assertThat(backend.submittedSpec).isNotNull();
        assertThat(backend.submittedSpec.key()
            .value()).isNotEqualTo(firstKey)
                .isEqualTo("tenant2-100-7");
        assertThat(firstKey).isEqualTo("tenant1-100-7");
    }

    @Test
    void testAFinishedJobWithoutAnExitCodeIsAFailureThatHonoursDelete() {
        FakeBackend backend = new FakeBackend();

        backend.exit = new RemoteJobExit(true, null, null);

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of("delete", false), 7L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no exit code");

        assertThat(backend.cleanupModes).containsExactly(CleanupMode.DETACH);
    }

    @Test
    void testResumeOffNeverLooksForAnExistingJob() {
        FakeBackend backend = new FakeBackend();

        backend.existing = new RemoteJobHandle("already-running");
        backend.existingKey = "public-100-7";

        runner(backend).run(request(Map.of("resume", false), 7L));

        assertThat(backend.findCalls).isZero();
        assertThat(backend.submittedSpec).isNotNull();
    }

    @Test
    void testANullTaskExecutionIdGetsARandomKeyAndNeverResumes() {
        FakeBackend backend = new FakeBackend();

        backend.existing = new RemoteJobHandle("already-running");
        backend.existingKey = "random-1";

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
        AtomicBoolean interruptRestored = new AtomicBoolean();

        Thread thread = new Thread(() -> {
            try {
                runner.run(request);
            } catch (RuntimeException exception) {
                failure.set(exception);
                interruptRestored.set(Thread.currentThread()
                    .isInterrupted());
            }
        });

        thread.start();

        assertThat(backend.awaitReached.await(10, TimeUnit.SECONDS)).isTrue();

        thread.interrupt();
        thread.join(Duration.ofSeconds(10)
            .toMillis());

        assertThat(thread.isAlive()).isFalse();
        assertThat(interruptRestored.get()).isTrue();
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(backend.cleanupModes).containsExactly(expected);
    }

    @Test
    void testTheContextCarriesTheRunnerParametersAndTheTransferCeiling() {
        FakeBackend backend = new FakeBackend();

        RemoteTaskRunner runner = new RemoteTaskRunner(
            backend, applicationProperties(Map.of("max-transfer-bytes", "3MB")), () -> "k");

        runner.run(request(Map.of("namespace", "jobs"), 7L));

        assertThat(backend.lastContext.runnerParameters()
            .getString("namespace")).isEqualTo("jobs");
        assertThat(backend.lastContext.maxTransferBytes()).isEqualTo(3L * 1024 * 1024);
    }

    @Test
    void testTheContextCarriesTheEffectiveCompletionBudget() {
        FakeBackend backend = new FakeBackend();

        RemoteTaskRunner runner = new RemoteTaskRunner(
            backend, applicationProperties(Map.of("max-wait", "PT20M")), () -> "k");

        runner.run(request(Map.of("waitUntilCompletion", "PT2H"), 7L));

        assertThat(backend.lastContext.completionBudget()).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    void testALargeStderrFileReachesTheSinkWithoutBreakingTheTail() {
        FakeBackend backend = new FakeBackend();

        backend.exit = RemoteJobExit.of(1);
        backend.stderrFile = "x".repeat(300_000) + "END-OF-TRACEBACK";

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of(), 7L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("END-OF-TRACEBACK");
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

        return request(runnerParameters, taskExecutionId, connections, taskExecutionId == null ? null : 100L);
    }

    private static TaskRunnerRequest request(
        Map<String, ?> runnerParameters, @Nullable Long taskExecutionId,
        Map<String, ComponentConnection> connections, @Nullable Long jobId) {

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getTaskExecutionId()).thenReturn(taskExecutionId);
        when(actionContext.getJobId()).thenReturn(jobId);

        return new TaskRunnerRequest(
            "python", SCRIPT, List.of(), Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(runnerParameters), null, connections, actionContext);
    }

    static final class FakeBackend implements RemoteTaskRunnerBackend {

        private final CountDownLatch awaitReached = new CountDownLatch(1);
        private final List<CleanupMode> cleanupModes = new ArrayList<>();

        private @Nullable RemoteJobContext lastContext;
        private @Nullable RemoteJobHandle existing;
        private @Nullable String existingKey;
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
        private boolean cleanupFails;
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
            lastContext = context;
            findCalls++;

            if (existingKey == null || !existingKey.equals(key.value())) {
                return Optional.empty();
            }

            return Optional.ofNullable(existing);
        }

        @Override
        public RemoteJobHandle submit(
            RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
            throws IOException {

            lastContext = context;
            submittedSpec = spec;
            submittedEntries = ContainerArchiveTest.entryNames(inputArchive);

            return new RemoteJobHandle("job-1");
        }

        @Override
        public RemoteJobExit await(
            RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
            RemoteJobContext context) throws InterruptedException {

            lastContext = context;
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
        public @Nullable InputStream
            collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
                throws IOException {

            lastContext = context;

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
            lastContext = context;
            cleanupModes.add(cleanupMode);

            if (cleanupFails) {
                throw new IllegalStateException("cleanup broke");
            }
        }
    }
}
