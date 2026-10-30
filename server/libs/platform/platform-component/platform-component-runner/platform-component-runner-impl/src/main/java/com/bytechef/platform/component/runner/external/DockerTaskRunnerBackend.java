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
import com.bytechef.component.definition.Property.ControlType;
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
public class DockerTaskRunnerBackend implements RemoteTaskRunnerBackend {

    static final String LABEL_JOB_ID = "io.bytechef.job-id";
    static final String LABEL_KILL_GRACE_PERIOD = "io.bytechef.kill-grace-period";
    static final String LABEL_MANAGED_BY = "io.bytechef.managed-by";
    static final String LABEL_TASK_EXECUTION_ID = "io.bytechef.task-execution-id";
    static final String MANAGED_BY_BYTECHEF = "bytechef";

    private static final Logger log = LoggerFactory.getLogger(DockerTaskRunnerBackend.class);

    private static final String CONTAINER_DIRECTORY = "/tmp";
    private static final Duration CLIENT_GRACE = Duration.ofSeconds(30);
    private static final Duration LOG_DRAIN_GRACE = Duration.ofSeconds(5);
    private static final long NANO_CPUS_PER_CPU = 1_000_000_000L;
    private static final String STATE_CREATED = "created";
    private static final String STATE_EXITED = "exited";
    private static final String STATE_RUNNING = "running";

    private static final Pattern MEMORY_PATTERN = Pattern.compile("^(\\d+)\\s*([bkmg])?$", Pattern.CASE_INSENSITIVE);

    private static final Map<ExternalLanguage, String> DEFAULT_IMAGES = Map.of(
        ExternalLanguage.BASH, "ubuntu:24.04",
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
                        .controlType(ControlType.PASSWORD)
                        .required(false),
                    string(REGISTRY_TOKEN).label("Registry Token")
                        .controlType(ControlType.PASSWORD)
                        .required(false))
                .required(false));
        properties.add(
            string(KILL_GRACE_PERIOD)
                .label("Kill Grace Period")
                .description(
                    "How long a still-running container is given to stop on its own when it is killed - on a " +
                        "timeout or a non-resumable shutdown - before it is forced, as an ISO-8601 duration. Zero " +
                        "forces it at once.")
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
                    HOST,
                    TaskRunnerOperatorFlag.getPropertyName(DOCKER, DockerOperatorSettings.ALLOW_TASK_HOST_PROPERTY)));
        }

        List<String> volumes = parameters.getList(VOLUMES, String.class, List.of());

        if (!volumes.isEmpty() && !settings.isVolumeMountAllowed()) {
            throw new IllegalArgumentException(
                "The docker runner's '%s' option is not enabled. An operator must enable it with %s=true.".formatted(
                    VOLUMES,
                    TaskRunnerOperatorFlag.getPropertyName(DOCKER,
                        DockerOperatorSettings.ALLOW_VOLUME_MOUNTS_PROPERTY)));
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
            listContainersCmd.withLabelFilter(
                Map.of(LABEL_TASK_EXECUTION_ID, key.value(), LABEL_MANAGED_BY, MANAGED_BY_BYTECHEF));

            Container running = null;
            Container exited = null;

            for (Container container : listContainersCmd.exec()) {
                String state = container.getState();

                if (state == null) {
                    continue;
                }

                switch (state.toLowerCase(Locale.ROOT)) {
                    case STATE_RUNNING -> running = running == null ? container : running;
                    case STATE_EXITED -> exited = exited == null ? container : exited;
                    case STATE_CREATED -> removeContainer(dockerClient, container.getId());
                    default -> log.debug("Ignoring the container {} in state {}", container.getId(), state);
                }
            }

            Container resumable = running != null ? running : exited;

            if (resumable == null) {
                return Optional.empty();
            }

            return Optional.of(new RemoteJobHandle(resumable.getId(), handleAttributes(resumable)));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not list the docker runner's containers", exception);
        }
    }

    private static Map<String, String> handleAttributes(Container container) {
        Map<String, String> labels = container.getLabels();

        if (labels == null) {
            return Map.of();
        }

        String killGracePeriod = labels.get(LABEL_KILL_GRACE_PERIOD);

        return killGracePeriod == null ? Map.of() : Map.of(KILL_GRACE_PERIOD, killGracePeriod);
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

            return new RemoteJobHandle(
                containerId, Map.of(KILL_GRACE_PERIOD, resolveKillGracePeriod(parameters).toString()));
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
        } catch (RuntimeException exception) {
            dockerClient.close();

            throw exception;
        }
    }

    @Override
    public void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context) {
        if (cleanupMode == CleanupMode.DETACH) {
            return;
        }

        try {
            Duration killGracePeriod = cleanupMode == CleanupMode.KILL_AND_DELETE
                ? handleKillGracePeriod(handle) : Duration.ZERO;

            try (DockerClient dockerClient = client(context, CLIENT_GRACE.plus(killGracePeriod))) {
                if (!killGracePeriod.isZero()) {
                    stopContainer(dockerClient, handle.id(), killGracePeriod);
                }

                removeContainer(dockerClient, handle.id());
            }
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

    private static Duration handleKillGracePeriod(RemoteJobHandle handle) {
        String value = handle.attributes()
            .get(KILL_GRACE_PERIOD);

        return value == null ? Duration.ZERO : Duration.parse(value);
    }

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
            createContainerCmd.withLabels(labels(spec.key(), resolveKillGracePeriod(parameters)));
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

    private static Map<String, String> labels(RemoteJobKey key, Duration killGracePeriod) {
        Map<String, String> labels = new LinkedHashMap<>();

        labels.put(LABEL_MANAGED_BY, MANAGED_BY_BYTECHEF);
        labels.put(LABEL_TASK_EXECUTION_ID, key.value());

        Long jobId = key.jobId();

        if (jobId != null) {
            labels.put(LABEL_JOB_ID, String.valueOf(jobId));
        }

        if (!killGracePeriod.isZero()) {
            labels.put(LABEL_KILL_GRACE_PERIOD, killGracePeriod.toString());
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

        try (
            CopyArchiveToContainerCmd copyArchiveToContainerCmd = dockerClient.copyArchiveToContainerCmd(containerId)) {
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
            throw new IllegalArgumentException(
                "The docker runner's %s must not be negative".formatted(KILL_GRACE_PERIOD));
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
