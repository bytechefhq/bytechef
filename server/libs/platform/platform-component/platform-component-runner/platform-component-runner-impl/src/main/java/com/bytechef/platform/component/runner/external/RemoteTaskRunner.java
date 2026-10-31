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
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a task through a {@link RemoteTaskRunnerBackend}: owns the local working directory, the tar streams, the
 * deadlines, the log capture and the cleanup, so a backend only has to talk to its own API.
 */
@SuppressFBWarnings("EI")
public class RemoteTaskRunner implements TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(RemoteTaskRunner.class);

    private static final int STDERR_BUFFER_SIZE = 8 * 1024;

    private static final Map<ExternalLanguage, List<String>> DEFAULT_INTERPRETERS = Map.of(
        ExternalLanguage.BASH, List.of("bash"),
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

    public RemoteTaskRunner(
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

        TaskRunnerConnectionEnvironment connectionEnvironment = TaskRunnerConnectionEnvironment.of(
            request, applicationProperties);

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request);
            CapturingLogSink logSink = new CapturingLogSink(
                getType(), limits().getMaxLogBytes(), connectionEnvironment.secretMasker())) {

            return run(request, workingDirectory, logSink, connectionEnvironment.variables());
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
        TaskRunnerRequest request, TaskRunnerWorkingDirectory workingDirectory, CapturingLogSink logSink,
        Map<String, String> connectionVariables) throws IOException, InterruptedException {

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
            environment(request, backend.getRemoteWorkingDirectory(), connectionVariables),
            backend.getRemoteWorkingDirectory(),
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

            if (exitCode == null || exitCode != 0) {
                collectFailedJobOutput(handle, workingDirectory, logSink, completionDeadline, context, limits);
            }

            if (exitCode == null) {
                outcome = Outcome.FAILURE;

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
            try {
                backend.cleanup(handle, cleanupMode(outcome, resume, settings.delete()), context);
            } catch (RuntimeException exception) {
                log.warn("Could not clean up the {} job {}", type, handle.id(), exception);
            }
        }
    }

    static CleanupMode cleanupMode(Outcome outcome, boolean resume, boolean delete) {
        return switch (outcome) {
            case SUCCESS, FAILURE -> delete ? CleanupMode.DELETE : CleanupMode.DETACH;
            case TIMEOUT, ERROR -> CleanupMode.KILL_AND_DELETE;
            case INTERRUPTED -> resume ? CleanupMode.DETACH : CleanupMode.KILL_AND_DELETE;
        };
    }

    private void collectFailedJobOutput(
        RemoteJobHandle handle, TaskRunnerWorkingDirectory workingDirectory, CapturingLogSink logSink,
        Deadline completionDeadline, RemoteJobContext context, TaskRunnerLimits limits) throws InterruptedException {

        if (completionDeadline.isExpired()) {
            return;
        }

        try {
            collectOutput(handle, workingDirectory, logSink, completionDeadline, context, limits);
        } catch (IOException | RuntimeException exception) {
            log.debug("Could not collect the failed {} job's output", getType(), exception);
        }
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
            try (InputStream inputStream = Files.newInputStream(stderrPath, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[STDERR_BUFFER_SIZE];

                int read = inputStream.read(buffer);

                while (read != -1) {
                    logSink.appendStderr(buffer, read);

                    read = inputStream.read(buffer);
                }
            }
        }
    }

    private RemoteJobContext context(TaskRunnerRequest request) {
        ComponentConnection connection = request.componentConnections()
            .get(TASK_RUNNER);

        String taskHost = request.runnerParameters()
            .getString(HOST);

        TaskRunnerLimits limits = limits();

        return new RemoteJobContext(
            connection, operatorProperties(), taskHost == null || taskHost.isBlank() ? null : taskHost.trim(),
            request.runnerParameters(), limits.getMaxTransferBytes(),
            RemoteTaskRunnerSettings.of(request, limits, getType())
                .waitUntilCompletion());
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
                Long jobId = actionContextAware.getJobId();

                String value = "%s-%s-%d".formatted(
                    TenantContext.getCurrentTenantId(), jobId == null ? "none" : String.valueOf(jobId),
                    taskExecutionId);

                return new ResolvedKey(new RemoteJobKey(value, jobId), true);
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

    private static Map<String, String> environment(
        TaskRunnerRequest request, String remoteWorkingDirectory, Map<String, String> connectionVariables) {

        String directory = remoteWorkingDirectory.length() > 1 && remoteWorkingDirectory.endsWith("/")
            ? remoteWorkingDirectory.substring(0, remoteWorkingDirectory.length() - 1)
            : remoteWorkingDirectory;

        Map<String, String> environment = new LinkedHashMap<>();

        environment.put("HOME", directory);
        environment.putAll(connectionVariables);
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

        private final SecretMasker secretMasker;
        private final BoundedOutputCapture stdout;
        private final BoundedOutputCapture stderr;
        private final GuestLoggingOutputStream stdoutLoggingOutputStream;
        private final GuestLoggingOutputStream stderrLoggingOutputStream;

        CapturingLogSink(String type, long maxLogBytes, SecretMasker secretMasker) {
            this.secretMasker = secretMasker;
            this.stdout = new BoundedOutputCapture(maxLogBytes);
            this.stderr = new BoundedOutputCapture(maxLogBytes);
            this.stdoutLoggingOutputStream = new GuestLoggingOutputStream(line -> {
                if (log.isInfoEnabled()) {
                    log.info("[{}] {}", type, secretMasker.mask(line));
                }
            });
            this.stderrLoggingOutputStream =
                new GuestLoggingOutputStream(line -> log.warn("[{}] {}", type, secretMasker.mask(line)));
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
            return secretMasker.mask(stdout.get());
        }

        String stderr() {
            return secretMasker.mask(stderr.get());
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
