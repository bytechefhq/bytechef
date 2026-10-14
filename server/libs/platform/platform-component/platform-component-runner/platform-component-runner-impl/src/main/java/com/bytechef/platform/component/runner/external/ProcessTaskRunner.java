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

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.INHERIT_ENVIRONMENT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.INTERPRETER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.polyglot.GuestLoggingOutputStream;
import com.bytechef.platform.component.runner.TaskRunner;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerOperatorFlag;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI")
public class ProcessTaskRunner implements TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessTaskRunner.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final int DRAIN_BUFFER_SIZE = 8 * 1024;
    private static final String INHERIT_ENVIRONMENT_ENABLED = "inherit-environment-enabled";

    private static final Duration DRAIN_GRACE = Duration.ofSeconds(5);

    private static final Map<ExternalLanguage, String> DEFAULT_INTERPRETERS = Map.of(
        ExternalLanguage.JAVASCRIPT, "node",
        ExternalLanguage.PYTHON, "python3",
        ExternalLanguage.SHELL, "/bin/sh");

    private final ApplicationProperties applicationProperties;

    public ProcessTaskRunner(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getType() {
        return PROCESS;
    }

    @Override
    public String getTitle() {
        return "Process";
    }

    @Override
    public List<? extends ModifiableValueProperty<?, ?>> getProperties() {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(
            string(INTERPRETER)
                .label("Interpreter")
                .description(
                    "The executable the source is handed to. Defaults to the interpreter for the action's language.")
                .required(false));

        if (isInheritEnvironmentEnabled()) {
            properties.add(
                bool(INHERIT_ENVIRONMENT)
                    .label("Inherit Environment")
                    .description(
                        "Passes the server process's own environment to the execution. Off by default, because that " +
                            "environment holds the server's credentials.")
                    .defaultValue(false)
                    .required(false));
        }

        return properties;
    }

    @Override
    public Set<TaskRunnerCapability> getCapabilities() {
        return Set.of(
            TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMMANDS, TaskRunnerCapability.INPUT_FILES,
            TaskRunnerCapability.OUTPUT_FILES, TaskRunnerCapability.ENVIRONMENT);
    }

    @Override
    public void validate(TaskRunnerRequest request) {
        String interpreter = request.runnerParameters()
            .getString(INTERPRETER);

        if (interpreter != null && interpreter.isBlank()) {
            throw new IllegalArgumentException("The process runner's interpreter must not be blank");
        }

        String languageId = request.languageId();

        if (!TaskRunnerBootstrap.isSupported(languageId)) {
            throw new IllegalArgumentException(
                "Language '%s' cannot be executed by an external task runner".formatted(languageId));
        }

        if (isInheritEnvironmentRequested(request) && !isInheritEnvironmentEnabled()) {
            throw new IllegalArgumentException(
                "The process runner's '%s' option is not enabled. An operator must enable it with %s=true."
                    .formatted(
                        INHERIT_ENVIRONMENT,
                        TaskRunnerOperatorFlag.getPropertyName(PROCESS, INHERIT_ENVIRONMENT_ENABLED)));
        }

        resolveInterpreter(request);
    }

    @Override
    public TaskRunnerResult run(TaskRunnerRequest request) {
        validate(request);

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request)) {
            return run(request, workingDirectory);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException("Interrupted while running the process task runner", exception);
        }
    }

    @SuppressFBWarnings("COMMAND_INJECTION")
    private TaskRunnerResult run(TaskRunnerRequest request, TaskRunnerWorkingDirectory workingDirectory)
        throws IOException, InterruptedException {

        String languageId = request.languageId();
        String script = request.script();

        String source = script != null
            ? TaskRunnerBootstrap.append(languageId, script, PROCESS)
            : String.join(System.lineSeparator(), request.commands()) + System.lineSeparator();

        Path sourcePath = workingDirectory.writeSourceFile(
            TaskRunnerBootstrap.sourceFileName(languageId), source);

        ProcessBuilder processBuilder = new ProcessBuilder(
            resolveInterpreter(request), sourcePath.toString());

        processBuilder.directory(
            workingDirectory.getPath()
                .toFile());

        applyEnvironment(processBuilder, request, workingDirectory);

        Process process = processBuilder.start();

        closeStandardInput(process);

        BoundedOutputCapture stdout = new BoundedOutputCapture();
        BoundedOutputCapture stderr = new BoundedOutputCapture();

        Thread stdoutThread = drain(process.getInputStream(), stdout, line -> {
            if (log.isInfoEnabled()) {
                log.info("[process] {}", line);
            }
        });
        Thread stderrThread = drain(process.getErrorStream(), stderr, line -> log.warn("[process] {}", line));

        Duration requestTimeout = request.timeout();
        Duration timeout = requestTimeout == null ? DEFAULT_TIMEOUT : requestTimeout;

        boolean exited;

        long waitStartNanos = System.nanoTime();

        try {
            exited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            if (process.isAlive()) {
                destroyTree(process);
            }
        }

        boolean drained = joinDrainThreads(stdoutThread, stderrThread, drainBudget(timeout, waitStartNanos));

        if (!drained) {
            destroyTree(process);

            log.warn(
                "The process task runner stopped waiting for the child's output streams after {}; a descendant that " +
                    "outlived the child still holds them open, so the captured output may be incomplete",
                timeout.plus(DRAIN_GRACE));
        }

        if (!exited) {
            throw new IllegalStateException(
                "The process task runner timed out after %s; stdout tail: %s; stderr tail: %s".formatted(
                    timeout, stdout.get(), stderr.get()));
        }

        int exitCode = process.exitValue();

        if (exitCode != 0) {
            throw new IllegalStateException(
                "The process exited with code %d; stdout tail: %s; stderr tail: %s".formatted(
                    exitCode, stdout.get(), stderr.get()));
        }

        return new TaskRunnerResult(
            TaskRunnerOutputs.readOutputJson(workingDirectory), exitCode, stdout.get(), stderr.get(),
            TaskRunnerOutputs.collectOutputFiles(request, workingDirectory));
    }

    static Duration drainBudget(Duration timeout, long waitStartNanos) {
        Duration remaining = timeout.minus(Duration.ofNanos(System.nanoTime() - waitStartNanos));

        return remaining.compareTo(DRAIN_GRACE) > 0 ? remaining : DRAIN_GRACE;
    }

    static boolean joinDrainThreads(Thread stdoutThread, Thread stderrThread, Duration budget)
        throws InterruptedException {

        long deadlineNanos = System.nanoTime() + budget.toNanos();

        boolean stdoutDrained = join(stdoutThread, deadlineNanos);
        boolean stderrDrained = join(stderrThread, deadlineNanos);

        return stdoutDrained && stderrDrained;
    }

    private static boolean join(Thread thread, long deadlineNanos) throws InterruptedException {
        long remainingNanos = deadlineNanos - System.nanoTime();

        if (remainingNanos > 0) {
            thread.join(Duration.ofNanos(remainingNanos));
        }

        return !thread.isAlive();
    }

    private static void applyEnvironment(
        ProcessBuilder processBuilder, TaskRunnerRequest request, TaskRunnerWorkingDirectory workingDirectory) {

        Map<String, String> environment = processBuilder.environment();

        boolean inherit = isInheritEnvironmentRequested(request);

        if (!inherit) {
            String path = environment.get("PATH");

            environment.clear();

            if (path != null) {
                environment.put("PATH", path);
            }

            environment.put(
                "HOME", workingDirectory.getPath()
                    .toString());
        }

        environment.putAll(request.env());
        environment.putAll(workingDirectory.getEnvironment());
    }

    private static void closeStandardInput(Process process) {
        try {
            OutputStream outputStream = process.getOutputStream();

            outputStream.close();
        } catch (IOException exception) {
            log.warn("Could not close the child process's standard input", exception);
        }
    }

    private static Thread drain(InputStream inputStream, BoundedOutputCapture capture, Consumer<String> lineConsumer) {
        return Thread.ofVirtual()
            .start(() -> {
                byte[] buffer = new byte[DRAIN_BUFFER_SIZE];

                try (InputStream stream = inputStream;
                    GuestLoggingOutputStream loggingOutputStream = new GuestLoggingOutputStream(lineConsumer)) {

                    int read = stream.read(buffer);

                    while (read != -1) {
                        capture.append(buffer, read);
                        loggingOutputStream.write(buffer, 0, read);

                        read = stream.read(buffer);
                    }
                } catch (IOException exception) {
                    capture.append(new byte[0], 0);
                }
            });
    }

    private static void destroyTree(Process process) {
        List<ProcessHandle> descendants = process.descendants()
            .toList();

        for (ProcessHandle descendant : descendants) {
            descendant.destroyForcibly();
        }

        process.destroyForcibly();
    }

    private boolean isInheritEnvironmentEnabled() {
        return TaskRunnerOperatorFlag.isEnabled(applicationProperties, PROCESS, INHERIT_ENVIRONMENT_ENABLED);
    }

    private static boolean isInheritEnvironmentRequested(TaskRunnerRequest request) {
        return request.runnerParameters()
            .getBoolean(INHERIT_ENVIRONMENT, false);
    }

    private static String resolveInterpreter(TaskRunnerRequest request) {
        String interpreter = request.runnerParameters()
            .getString(INTERPRETER);

        if (interpreter != null && !interpreter.isBlank()) {
            return interpreter;
        }

        String languageId = request.languageId();

        String defaultInterpreter = DEFAULT_INTERPRETERS.get(ExternalLanguage.of(languageId));

        if (defaultInterpreter == null) {
            throw new IllegalArgumentException(
                "The process runner has no default interpreter for language '%s'; set one on the task runner"
                    .formatted(languageId));
        }

        return defaultInterpreter;
    }
}
