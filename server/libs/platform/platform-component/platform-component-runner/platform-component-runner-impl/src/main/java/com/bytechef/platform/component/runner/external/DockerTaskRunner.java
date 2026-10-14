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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.CPU;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.DOCKER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENTRYPOINT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.EXTRA_HOSTS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.IMAGE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.MEMORY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.NETWORK_MODE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PULL_POLICY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.USER;

import com.bytechef.component.definition.ComponentDsl.ModifiableOption;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.polyglot.GuestLoggingOutputStream;
import com.bytechef.platform.component.runner.TaskRunner;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerOperatorFlag;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.WaitResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

/**
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI")
public class DockerTaskRunner implements TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(DockerTaskRunner.class);

    private static final String CONTAINER_DIRECTORY = "/tmp";
    private static final String CONTAINER_OUTPUT_DIRECTORY = CONTAINER_DIRECTORY + "/output";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration LOG_DRAIN_GRACE = Duration.ofSeconds(5);

    private static final long NANO_CPUS_PER_CPU = 1_000_000_000L;

    private static final Pattern MEMORY_PATTERN = Pattern.compile("^(\\d+)\\s*([bkmg])?$", Pattern.CASE_INSENSITIVE);

    private static final Map<ExternalLanguage, List<String>> DEFAULT_ENTRYPOINTS = Map.of(
        ExternalLanguage.JAVASCRIPT, List.of("node"),
        ExternalLanguage.PYTHON, List.of("python3"),
        ExternalLanguage.SHELL, List.of("/bin/sh"));

    private static final Map<ExternalLanguage, String> DEFAULT_IMAGES = Map.of(
        ExternalLanguage.JAVASCRIPT, "node:22-alpine",
        ExternalLanguage.PYTHON, "python:3.12-slim",
        ExternalLanguage.SHELL, "ubuntu:24.04");

    private final ApplicationProperties applicationProperties;
    private final BiFunction<DockerOperatorSettings, Duration, DockerClient> dockerClientFactory;

    @Autowired
    public DockerTaskRunner(ApplicationProperties applicationProperties) {
        this(applicationProperties, DockerTaskRunner::createDockerClient);
    }

    DockerTaskRunner(
        ApplicationProperties applicationProperties,
        BiFunction<DockerOperatorSettings, Duration, DockerClient> dockerClientFactory) {

        this.applicationProperties = applicationProperties;
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
    public List<? extends ModifiableValueProperty<?, ?>> getProperties() {
        DockerOperatorSettings dockerOperatorSettings = DockerOperatorSettings.of(applicationProperties);

        List<String> allowedImages = dockerOperatorSettings.getAllowedImages();

        List<ModifiableOption<String>> imageOptions = allowedImages.stream()
            .map(image -> option(image, image))
            .toList();

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(
            string(IMAGE)
                .label("Image")
                .description(
                    "The container image the task runs in. Defaults to the image for the action's language. Only " +
                        "images an operator has allowed can be run.")
                .options(imageOptions)
                .required(false));

        properties.add(
            string(PULL_POLICY)
                .label("Pull Policy")
                .description("When the image is pulled from its registry.")
                .options(
                    option("Always", PullPolicy.ALWAYS.name()),
                    option("If Not Present", PullPolicy.IF_NOT_PRESENT.name()),
                    option("Never", PullPolicy.NEVER.name()))
                .defaultValue(PullPolicy.IF_NOT_PRESENT.name())
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
                .description(
                    "How many CPUs the container may use, as a decimal fraction of one CPU. Defaults to the operator's "
                        +
                        "allowance, and cannot exceed the operator's maximum.")
                .required(false));

        properties.add(
            string(MEMORY)
                .label("Memory")
                .description(
                    "How much memory the container may use, in bytes or with a b, k, m or g suffix. Defaults to the " +
                        "operator's allowance, and cannot exceed the operator's maximum.")
                .required(false));

        List<ModifiableOption<String>> networkModeOptions = NetworkMode.allowedBy(dockerOperatorSettings)
            .stream()
            .map(networkMode -> option(networkMode.getLabel(), networkMode.getValue()))
            .toList();

        properties.add(
            string(NETWORK_MODE)
                .label("Network Mode")
                .description("The network the container is attached to.")
                .options(networkModeOptions)
                .required(false));

        properties.add(
            array(EXTRA_HOSTS)
                .label("Extra Hosts")
                .description("Additional entries for the container's hosts file, each written as host:ip.")
                .items(string())
                .required(false));

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
        String languageId = request.languageId();

        if (!TaskRunnerBootstrap.isSupported(languageId)) {
            throw new IllegalArgumentException(
                "Language '%s' cannot be executed by an external task runner".formatted(languageId));
        }

        DockerOperatorSettings dockerOperatorSettings = DockerOperatorSettings.of(applicationProperties);

        String image = resolveImage(request);

        if (!dockerOperatorSettings.isImageAllowed(image)) {
            throw new IllegalArgumentException(
                "Image '%s' is not allowed. An operator must add it to %s.".formatted(
                    image,
                    TaskRunnerOperatorFlag.getPropertyName(DOCKER, DockerOperatorSettings.ALLOWED_IMAGES_PROPERTY)));
        }

        resolveNetworkMode(request, dockerOperatorSettings);
        resolvePullPolicy(request);
        resolveEntrypoint(request);
        resolveNanoCpus(request, dockerOperatorSettings);
        resolveMemoryBytes(request, dockerOperatorSettings);
        resolvePidsLimit(dockerOperatorSettings);
    }

    private static NetworkMode resolveNetworkMode(
        TaskRunnerRequest request, DockerOperatorSettings dockerOperatorSettings) {

        List<NetworkMode> allowedNetworkModes = NetworkMode.allowedBy(dockerOperatorSettings);

        String value = getNetworkMode(request);

        if (value == null) {
            if (allowedNetworkModes.isEmpty()) {
                throw new IllegalArgumentException(
                    "The docker runner has no network mode it may use. An operator must name one in %s.".formatted(
                        TaskRunnerOperatorFlag.getPropertyName(
                            DOCKER, DockerOperatorSettings.ALLOWED_NETWORK_MODES_PROPERTY)));
            }

            return allowedNetworkModes.getFirst();
        }

        NetworkMode networkMode = NetworkMode.of(value);

        if (networkMode == NetworkMode.HOST && !dockerOperatorSettings.isHostNetworkAllowed()) {
            throw new IllegalArgumentException(
                "The docker runner's '%s' network mode is not enabled. An operator must enable it with %s=true."
                    .formatted(
                        NetworkMode.HOST.getValue(),
                        TaskRunnerOperatorFlag.getPropertyName(
                            DOCKER, DockerOperatorSettings.ALLOW_HOST_NETWORK_PROPERTY)));
        }

        if (networkMode == null || !allowedNetworkModes.contains(networkMode)) {
            List<String> allowedValues = allowedNetworkModes.stream()
                .map(NetworkMode::getValue)
                .toList();

            throw new IllegalArgumentException(
                "The docker runner cannot use the network mode '%s'; it accepts %s.".formatted(
                    value, String.join(", ", allowedValues)));
        }

        return networkMode;
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

            throw new IllegalStateException("Interrupted while running the docker task runner", exception);
        }
    }

    private TaskRunnerResult run(TaskRunnerRequest request, TaskRunnerWorkingDirectory workingDirectory)
        throws IOException, InterruptedException {

        Duration requestTimeout = request.timeout();
        Duration timeout = requestTimeout == null ? DEFAULT_TIMEOUT : requestTimeout;

        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        String languageId = request.languageId();
        String script = request.script();

        String source = script != null
            ? TaskRunnerBootstrap.append(languageId, script, DOCKER)
            : String.join(System.lineSeparator(), request.commands()) + System.lineSeparator();

        String sourceFileName = TaskRunnerBootstrap.sourceFileName(languageId);

        workingDirectory.writeSourceFile(sourceFileName, source);

        DockerOperatorSettings dockerOperatorSettings = DockerOperatorSettings.of(applicationProperties);

        String image = resolveImage(request);

        try (DockerClient dockerClient = dockerClientFactory.apply(dockerOperatorSettings, timeout)) {
            pullImage(dockerClient, image, request, timeout, deadlineNanos);

            checkDeadline(deadlineNanos, timeout, "creating the container");

            String containerId = createContainer(
                dockerClient, image, request, dockerOperatorSettings, workingDirectory, sourceFileName);

            try {
                return run(dockerClient, containerId, request, workingDirectory, timeout, deadlineNanos);
            } finally {
                removeContainer(dockerClient, containerId);
            }
        }
    }

    private static TaskRunnerResult run(
        DockerClient dockerClient, String containerId, TaskRunnerRequest request,
        TaskRunnerWorkingDirectory workingDirectory, Duration timeout, long deadlineNanos)
        throws IOException, InterruptedException {

        copyWorkingDirectoryIn(dockerClient, containerId, workingDirectory);

        checkDeadline(deadlineNanos, timeout, "starting the container");

        try (StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId)) {
            startContainerCmd.exec();
        }

        BoundedOutputCapture stdout = new BoundedOutputCapture();
        BoundedOutputCapture stderr = new BoundedOutputCapture();

        LogCallback logCallback = attachLogs(dockerClient, containerId, stdout, stderr);

        ExitResult exitResult;

        try {
            exitResult = awaitExit(dockerClient, containerId, deadlineNanos);
        } finally {
            drainLogs(logCallback);
        }

        if (!exitResult.exited()) {
            throw new IllegalStateException(
                "The docker task runner timed out after %s; stdout tail: %s; stderr tail: %s".formatted(
                    timeout, stdout.get(), stderr.get()));
        }

        Integer statusCode = exitResult.statusCode();

        if (statusCode == null) {
            throw new IllegalStateException(
                "The container reported no exit code; stdout tail: %s; stderr tail: %s".formatted(
                    stdout.get(), stderr.get()));
        }

        int exitCode = statusCode;

        if (exitCode != 0) {
            throw new IllegalStateException(
                "The container exited with code %d; stdout tail: %s; stderr tail: %s".formatted(
                    exitCode, stdout.get(), stderr.get()));
        }

        checkDeadline(deadlineNanos, timeout, "collecting the output");

        copyOutputOut(dockerClient, containerId, workingDirectory);

        return new TaskRunnerResult(
            TaskRunnerOutputs.readOutputJson(workingDirectory), exitCode, stdout.get(), stderr.get(),
            TaskRunnerOutputs.collectOutputFiles(request, workingDirectory));
    }

    private static LogCallback attachLogs(
        DockerClient dockerClient, String containerId, BoundedOutputCapture stdout, BoundedOutputCapture stderr) {

        LogCallback logCallback = new LogCallback(stdout, stderr);

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

    private static ExitResult awaitExit(DockerClient dockerClient, String containerId, long deadlineNanos)
        throws IOException, InterruptedException {

        try (WaitCallback waitCallback = new WaitCallback();
            WaitContainerCmd waitContainerCmd = dockerClient.waitContainerCmd(containerId)) {

            waitContainerCmd.exec(waitCallback);

            boolean exited = waitCallback.awaitCompletion(remainingMillis(deadlineNanos), TimeUnit.MILLISECONDS);

            return new ExitResult(exited, waitCallback.getStatusCode());
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
        DockerClient dockerClient, String image, TaskRunnerRequest request, Duration timeout, long deadlineNanos)
        throws IOException, InterruptedException {

        PullPolicy pullPolicy = resolvePullPolicy(request);

        if (pullPolicy == PullPolicy.NEVER) {
            return;
        }

        if (pullPolicy == PullPolicy.IF_NOT_PRESENT && isImagePresent(dockerClient, image)) {
            return;
        }

        checkDeadline(deadlineNanos, timeout, "pulling the image");

        try (PullImageResultCallback pullImageResultCallback = new PullImageResultCallback();
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image)) {

            pullImageCmd.exec(pullImageResultCallback);

            boolean pulled = pullImageResultCallback.awaitCompletion(
                remainingMillis(deadlineNanos), TimeUnit.MILLISECONDS);

            if (!pulled) {
                throw new IllegalStateException(
                    "The docker task runner timed out after %s pulling image '%s'".formatted(timeout, image));
            }
        }
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
        DockerClient dockerClient, String image, TaskRunnerRequest request,
        DockerOperatorSettings dockerOperatorSettings, TaskRunnerWorkingDirectory workingDirectory,
        String sourceFileName) {

        try (CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image)) {
            createContainerCmd.withHostConfig(createHostConfig(request, dockerOperatorSettings));
            createContainerCmd.withWorkingDir(CONTAINER_DIRECTORY);
            createContainerCmd.withEntrypoint(resolveEntrypoint(request));
            createContainerCmd.withCmd(CONTAINER_DIRECTORY + "/" + sourceFileName);
            createContainerCmd.withEnv(createEnvironment(request, workingDirectory));
            createContainerCmd.withAttachStdout(true);
            createContainerCmd.withAttachStderr(true);

            createContainerCmd.withTty(false);

            String user = getString(request, USER);

            if (user != null) {
                createContainerCmd.withUser(user);
            }

            CreateContainerResponse createContainerResponse = createContainerCmd.exec();

            return createContainerResponse.getId();
        }
    }

    private static HostConfig createHostConfig(
        TaskRunnerRequest request, DockerOperatorSettings dockerOperatorSettings) {

        HostConfig hostConfig = HostConfig.newHostConfig();

        NetworkMode networkMode = resolveNetworkMode(request, dockerOperatorSettings);

        hostConfig.withNetworkMode(networkMode.getValue());
        hostConfig.withNanoCPUs(resolveNanoCpus(request, dockerOperatorSettings));
        hostConfig.withMemory(resolveMemoryBytes(request, dockerOperatorSettings));
        hostConfig.withPidsLimit(resolvePidsLimit(dockerOperatorSettings));

        List<String> extraHosts = request.runnerParameters()
            .getList(EXTRA_HOSTS, String.class, List.of());

        if (!extraHosts.isEmpty()) {
            hostConfig.withExtraHosts(extraHosts.toArray(new String[0]));
        }

        return hostConfig;
    }

    private static List<String> createEnvironment(
        TaskRunnerRequest request, TaskRunnerWorkingDirectory workingDirectory) {

        Map<String, String> environment = new LinkedHashMap<>();

        environment.put("HOME", CONTAINER_DIRECTORY);

        environment.putAll(request.env());

        String hostPath = workingDirectory.getPath()
            .toString();

        for (Map.Entry<String, String> entry : workingDirectory.getEnvironment()
            .entrySet()) {

            environment.put(entry.getKey(), toContainerPath(hostPath, entry.getValue()));
        }

        return environment.entrySet()
            .stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .toList();
    }

    private static String toContainerPath(String hostPath, String value) {
        if (!value.startsWith(hostPath)) {
            return value;
        }

        String relative = value.substring(hostPath.length())
            .replace('\\', '/');

        return CONTAINER_DIRECTORY + relative;
    }

    private static void copyWorkingDirectoryIn(
        DockerClient dockerClient, String containerId, TaskRunnerWorkingDirectory workingDirectory)
        throws IOException {

        Path path = workingDirectory.getPath();

        try (InputStream inputStream = ContainerArchive.toTar(path);
            CopyArchiveToContainerCmd copyArchiveToContainerCmd = dockerClient.copyArchiveToContainerCmd(containerId)) {

            copyArchiveToContainerCmd.withRemotePath(CONTAINER_DIRECTORY);
            copyArchiveToContainerCmd.withTarInputStream(inputStream);

            copyArchiveToContainerCmd.exec();
        }
    }

    private static void copyOutputOut(
        DockerClient dockerClient, String containerId, TaskRunnerWorkingDirectory workingDirectory)
        throws IOException {

        try (CopyArchiveFromContainerCmd copyArchiveFromContainerCmd = dockerClient.copyArchiveFromContainerCmd(
            containerId, CONTAINER_OUTPUT_DIRECTORY)) {

            try (InputStream inputStream = copyArchiveFromContainerCmd.exec()) {
                ContainerArchive.extractTar(inputStream, workingDirectory.getPath());
            }
        } catch (NotFoundException exception) {
            log.warn("The container left no {} directory, so the execution produced no output",
                CONTAINER_OUTPUT_DIRECTORY, exception);
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

    private static void checkDeadline(long deadlineNanos, Duration timeout, String stage) {
        if (System.nanoTime() - deadlineNanos >= 0) {
            throw new IllegalStateException(
                "The docker task runner timed out after %s while %s".formatted(timeout, stage));
        }
    }

    private static long remainingMillis(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();

        return remainingNanos > 0 ? TimeUnit.NANOSECONDS.toMillis(remainingNanos) : 0;
    }

    private static String resolveImage(TaskRunnerRequest request) {
        String image = getString(request, IMAGE);

        if (image != null) {
            return image;
        }

        String languageId = request.languageId();

        String defaultImage = DEFAULT_IMAGES.get(ExternalLanguage.of(languageId));

        if (defaultImage == null) {
            throw new IllegalArgumentException(
                "The docker runner has no default image for language '%s'; name one on the task runner"
                    .formatted(languageId));
        }

        return defaultImage;
    }

    @Nullable
    private static String getNetworkMode(TaskRunnerRequest request) {
        return getString(request, NETWORK_MODE);
    }

    @Nullable
    private static String getString(TaskRunnerRequest request, String name) {
        String value = request.runnerParameters()
            .getString(name);

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<String> resolveEntrypoint(TaskRunnerRequest request) {
        List<String> entrypoint = request.runnerParameters()
            .getList(ENTRYPOINT, String.class, List.of());

        if (!entrypoint.isEmpty()) {
            return entrypoint;
        }

        String languageId = request.languageId();

        List<String> defaultEntrypoint = DEFAULT_ENTRYPOINTS.get(ExternalLanguage.of(languageId));

        if (defaultEntrypoint == null) {
            throw new IllegalArgumentException(
                "The docker runner has no default entrypoint for language '%s'; set one on the task runner"
                    .formatted(languageId));
        }

        return defaultEntrypoint;
    }

    private static PullPolicy resolvePullPolicy(TaskRunnerRequest request) {
        String pullPolicy = getString(request, PULL_POLICY);

        if (pullPolicy == null) {
            return PullPolicy.IF_NOT_PRESENT;
        }

        for (PullPolicy candidate : PullPolicy.values()) {
            if (candidate.name()
                .equalsIgnoreCase(pullPolicy)) {

                return candidate;
            }
        }

        throw new IllegalArgumentException(
            "The docker runner does not know the pull policy '%s'".formatted(pullPolicy));
    }

    private static long resolveNanoCpus(TaskRunnerRequest request, DockerOperatorSettings dockerOperatorSettings) {
        long maxNanoCpus = parseOperatorNanoCpus(
            dockerOperatorSettings.getMaxCpu(), DockerOperatorSettings.MAX_CPU_PROPERTY);

        Double cpu = request.runnerParameters()
            .getDouble(CPU);

        if (cpu == null) {
            long defaultNanoCpus = parseOperatorNanoCpus(
                dockerOperatorSettings.getDefaultCpu(), DockerOperatorSettings.DEFAULT_CPU_PROPERTY);

            return Math.min(defaultNanoCpus, maxNanoCpus);
        }

        if (cpu <= 0) {
            throw new IllegalArgumentException("The docker runner's cpu limit must be greater than zero");
        }

        long nanoCpus = Math.round(cpu * NANO_CPUS_PER_CPU);

        if (nanoCpus > maxNanoCpus) {
            throw new IllegalArgumentException(
                "The docker runner's cpu limit '%s' is above the maximum an operator allows. An operator must raise %s."
                    .formatted(
                        cpu,
                        TaskRunnerOperatorFlag.getPropertyName(DOCKER, DockerOperatorSettings.MAX_CPU_PROPERTY)));
        }

        return nanoCpus;
    }

    private static long parseOperatorNanoCpus(String value, String propertyName) {
        double cpu;

        try {
            cpu = Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "The docker runner cannot read '%s' as a cpu limit; an operator must correct %s.".formatted(
                    value, TaskRunnerOperatorFlag.getPropertyName(DOCKER, propertyName)),
                exception);
        }

        if (!(cpu > 0)) {
            throw new IllegalArgumentException(
                "The docker runner's %s must be greater than zero.".formatted(
                    TaskRunnerOperatorFlag.getPropertyName(DOCKER, propertyName)));
        }

        return Math.round(cpu * NANO_CPUS_PER_CPU);
    }

    private static long resolveMemoryBytes(TaskRunnerRequest request, DockerOperatorSettings dockerOperatorSettings) {
        long maxMemoryBytes = parseOperatorMemoryBytes(
            dockerOperatorSettings.getMaxMemory(), DockerOperatorSettings.MAX_MEMORY_PROPERTY);

        String memory = getString(request, MEMORY);

        if (memory == null) {
            long defaultMemoryBytes = parseOperatorMemoryBytes(
                dockerOperatorSettings.getDefaultMemory(), DockerOperatorSettings.DEFAULT_MEMORY_PROPERTY);

            return Math.min(defaultMemoryBytes, maxMemoryBytes);
        }

        long memoryBytes = parseMemoryBytes(memory);

        if (memoryBytes > maxMemoryBytes) {
            throw new IllegalArgumentException(
                ("The docker runner's memory limit '%s' is above the maximum an operator allows. An operator must " +
                    "raise %s.").formatted(
                        memory,
                        TaskRunnerOperatorFlag.getPropertyName(DOCKER, DockerOperatorSettings.MAX_MEMORY_PROPERTY)));
        }

        return memoryBytes;
    }

    private static long parseOperatorMemoryBytes(String value, String propertyName) {
        try {
            return parseMemoryBytes(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "The docker runner cannot read '%s' as a memory limit; an operator must correct %s.".formatted(
                    value, TaskRunnerOperatorFlag.getPropertyName(DOCKER, propertyName)),
                exception);
        }
    }

    private static long resolvePidsLimit(DockerOperatorSettings dockerOperatorSettings) {
        String value = dockerOperatorSettings.getPidsLimit();

        long pidsLimit;

        try {
            pidsLimit = Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "The docker runner cannot read '%s' as a pids limit; an operator must correct %s.".formatted(
                    value,
                    TaskRunnerOperatorFlag.getPropertyName(DOCKER, DockerOperatorSettings.PIDS_LIMIT_PROPERTY)),
                exception);
        }

        if (pidsLimit <= 0) {
            throw new IllegalArgumentException(
                "The docker runner's %s must be greater than zero; a container is never left unbounded.".formatted(
                    TaskRunnerOperatorFlag.getPropertyName(DOCKER, DockerOperatorSettings.PIDS_LIMIT_PROPERTY)));
        }

        return pidsLimit;
    }

    private static long parseMemoryBytes(String memory) {
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

    private static DockerClient createDockerClient(DockerOperatorSettings dockerOperatorSettings, Duration timeout) {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        String host = dockerOperatorSettings.getHost();

        if (!host.isBlank()) {
            builder.withDockerHost(host);
        }

        DefaultDockerClientConfig dockerClientConfig = builder.build();

        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.getDockerHost())
            .sslConfig(dockerClientConfig.getSSLConfig())
            .responseTimeout(timeout)
            .build();

        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }

    private record ExitResult(boolean exited, @Nullable Integer statusCode) {
    }

    private enum NetworkMode {

        BRIDGE("Bridge", "bridge", false),
        NONE("None", "none", false),
        HOST("Host", "host", true);

        private final String label;
        private final String value;
        private final boolean operatorGated;

        NetworkMode(String label, String value, boolean operatorGated) {
            this.label = label;
            this.value = value;
            this.operatorGated = operatorGated;
        }

        static List<NetworkMode> allowedBy(DockerOperatorSettings dockerOperatorSettings) {
            return Arrays.stream(values())
                .filter(networkMode -> dockerOperatorSettings.isNetworkModeAllowed(networkMode.value))
                .filter(networkMode -> !networkMode.operatorGated || dockerOperatorSettings.isHostNetworkAllowed())
                .toList();
        }

        @Nullable
        static NetworkMode of(String value) {
            for (NetworkMode networkMode : values()) {
                if (networkMode.value.equalsIgnoreCase(value)) {
                    return networkMode;
                }
            }

            return null;
        }

        String getLabel() {
            return label;
        }

        String getValue() {
            return value;
        }
    }

    private enum PullPolicy {

        ALWAYS, IF_NOT_PRESENT, NEVER
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

        private final BoundedOutputCapture stdout;
        private final BoundedOutputCapture stderr;
        private final GuestLoggingOutputStream stdoutLoggingOutputStream;
        private final GuestLoggingOutputStream stderrLoggingOutputStream;

        LogCallback(BoundedOutputCapture stdout, BoundedOutputCapture stderr) {
            this.stdout = stdout;
            this.stderr = stderr;

            this.stdoutLoggingOutputStream = new GuestLoggingOutputStream(line -> {
                if (log.isInfoEnabled()) {
                    log.info("[docker] {}", line);
                }
            });
            this.stderrLoggingOutputStream = new GuestLoggingOutputStream(line -> log.warn("[docker] {}", line));
        }

        @Override
        public void onNext(Frame frame) {
            byte[] payload = frame.getPayload();

            if (payload == null) {
                return;
            }

            StreamType streamType = frame.getStreamType();

            if (streamType == StreamType.STDERR) {
                append(stderr, stderrLoggingOutputStream, payload);
            } else {
                append(stdout, stdoutLoggingOutputStream, payload);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                stdoutLoggingOutputStream.close();
                stderrLoggingOutputStream.close();
            }
        }

        private static void append(
            BoundedOutputCapture capture, GuestLoggingOutputStream loggingOutputStream, byte[] payload) {

            capture.append(payload, payload.length);

            try {
                loggingOutputStream.write(payload, 0, payload.length);
            } catch (IOException exception) {
                log.debug("Could not tee the container's output to the log", exception);
            }
        }
    }
}
