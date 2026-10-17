/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.DONE_MARKER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.INIT_CONTAINER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.MAIN_CONTAINER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.OUTPUT_CONTAINER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.READY_MARKER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.WORK_DIRECTORY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.KILL_GRACE_PERIOD;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
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
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Runs a task as a pod on the cluster the task's {@code kubernetes} connection names.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class KubernetesTaskRunnerBackend implements RemoteTaskRunnerBackend {

    static final String NAMESPACE_ATTRIBUTE = "namespace";
    static final String PULL_SECRET_ATTRIBUTE = "pullSecret";
    static final String TYPE = "kubernetes";

    private static final Logger log = LoggerFactory.getLogger(KubernetesTaskRunnerBackend.class);

    private static final Duration CLIENT_GRACE = Duration.ofSeconds(30);
    private static final String COLLECTING_THE_OUTPUTS = "collecting the outputs";
    private static final String DELIVERING_THE_INPUTS = "delivering the inputs";
    private static final String COLLECT_SCRIPT =
        "cd " + WORK_DIRECTORY + " && if [ -f .stderr ]; then tar -cf - output .stderr; else tar -cf - output; fi";
    private static final Set<String> FAIL_FAST_REASONS = Set.of(
        "ErrImagePull", "ImagePullBackOff", "InvalidImageName", "CreateContainerConfigError", "CreateContainerError");
    private static final Duration LOG_DRAIN_GRACE = Duration.ofSeconds(5);
    private static final int MAX_CAUSE_DEPTH = 16;

    private final KubernetesOperationsFactory operationsFactory;
    private final Duration pollInterval;

    @Autowired
    public KubernetesTaskRunnerBackend() {
        this(Fabric8KubernetesOperations::new, Duration.ofSeconds(1));
    }

    KubernetesTaskRunnerBackend(KubernetesOperationsFactory operationsFactory, Duration pollInterval) {
        this.operationsFactory = operationsFactory;
        this.pollInterval = pollInterval;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTitle() {
        return "Kubernetes";
    }

    @Override
    public String getRemoteWorkingDirectory() {
        return WORK_DIRECTORY;
    }

    @Override
    public List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties) {
        return KubernetesRunnerSettings.properties();
    }

    @Override
    public Optional<TaskRunnerConnection> getConnection() {
        return Optional.of(new TaskRunnerConnection(TYPE, 1));
    }

    @Override
    public void validate(TaskRunnerRequest request, RemoteJobContext context) {
        connectionSettings(context).validate();

        KubernetesRunnerSettings.of(request.languageId(), request.runnerParameters());
    }

    @Override
    public Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context) {
        KubernetesConnectionSettings connectionSettings = connectionSettings(context);

        String namespace = namespace(context.runnerParameters(), connectionSettings);

        try (KubernetesOperations operations = operationsFactory.create(connectionSettings, CLIENT_GRACE)) {
            RemoteJobHandle resumable = null;

            for (Pod pod : operations.listPods(namespace, KubernetesJobLabels.selector(key))) {
                Map<String, String> annotations = annotations(pod);

                if (!Objects.equals(annotations.get(KubernetesJobLabels.ANNOTATION_KEY), key.value())) {
                    continue;
                }

                if (resumable == null && isPastInputStage(pod) && isRunning(pod, OUTPUT_CONTAINER)) {
                    resumable = handle(pod, namespace);
                } else {
                    deleteQuietly(operations, namespace, pod.getMetadata()
                        .getName(), annotations.get(KubernetesJobLabels.ANNOTATION_PULL_SECRET), Duration.ZERO);
                }
            }

            return Optional.ofNullable(resumable);
        } catch (KubernetesClientException exception) {
            if (isInterrupt(exception)) {
                Thread.currentThread()
                    .interrupt();

                throw new IllegalStateException(
                    "The kubernetes runner was interrupted while looking for a job to resume", exception);
            }

            throw clientFailure("looking for a job to resume", exception);
        }
    }

    @Override
    public RemoteJobHandle submit(
        RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
        throws IOException, InterruptedException {

        KubernetesConnectionSettings connectionSettings = connectionSettings(context);
        KubernetesRunnerSettings runnerSettings = KubernetesRunnerSettings.of(
            spec.languageId(), spec.runnerParameters());

        String namespace = runnerSettings.namespaceOr(connectionSettings.namespace());

        try (KubernetesOperations operations = operationsFactory.create(
            connectionSettings, runningDeadline.remaining()
                .plus(CLIENT_GRACE))) {

            String pullSecretName = null;
            String podName = null;

            try {
                KubernetesRunnerSettings.RegistryCredentials credentials = runnerSettings.credentials();

                if (credentials != null) {
                    Secret secret = operations.createSecret(
                        KubernetesPodFactory.pullSecret(namespace, spec.key(), credentials));

                    pullSecretName = secret.getMetadata()
                        .getName();
                }

                Pod pod = createPod(operations, spec, runnerSettings, namespace, context, pullSecretName);

                podName = pod.getMetadata()
                    .getName();

                if (pullSecretName != null) {
                    patchSecretOwner(operations, namespace, pullSecretName, pod);
                }

                awaitInitContainer(operations, namespace, podName, runningDeadline);

                deliverInputs(operations, namespace, podName, inputArchive, runningDeadline);

                return handle(pod, namespace);
            } catch (KubernetesClientException exception) {
                discard(operations, namespace, podName, pullSecretName);

                throw failure("submitting the job", exception);
            } catch (IOException | InterruptedException | RuntimeException exception) {
                discard(operations, namespace, podName, pullSecretName);

                throw exception;
            }
        } catch (KubernetesClientException exception) {
            throw failure("submitting the job", exception);
        }
    }

    @Override
    public RemoteJobExit await(
        RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
        RemoteJobContext context) throws InterruptedException {

        String namespace = handleNamespace(handle);

        try (KubernetesOperations operations = operationsFactory.create(
            connectionSettings(context), completionDeadline.remaining()
                .plus(CLIENT_GRACE))) {

            RemoteJobExit evicted = awaitMainStarted(operations, namespace, handle.id(), runningDeadline);

            if (evicted != null) {
                return evicted;
            }

            KubernetesOperations.LogStream logStream = operations.followLogs(
                namespace, handle.id(), MAIN_CONTAINER, new SinkOutputStream(logSink));

            boolean finished = false;

            try {
                RemoteJobExit exit = awaitMainEnded(operations, namespace, handle.id(), completionDeadline);

                finished = exit.finished();

                return exit;
            } finally {
                try {
                    if (finished) {
                        logStream.awaitCompletion(LOG_DRAIN_GRACE);
                    }
                } finally {
                    logStream.close();
                }
            }
        } catch (KubernetesClientException exception) {
            throw failure("waiting for the job", exception);
        }
    }

    @Override
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public @Nullable InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
        throws IOException, InterruptedException {

        String namespace = handleNamespace(handle);

        Duration timeout = completionDeadline.remaining()
            .plus(CLIENT_GRACE);

        Path archivePath = Files.createTempFile("bytechef-kubernetes-output-" + UUID.randomUUID() + "-", ".tar");

        try (KubernetesOperations operations = operationsFactory.create(connectionSettings(context), timeout)) {
            KubernetesOperations.ExecResult execResult;

            try (OutputStream outputStream = new CeilingOutputStream(
                Files.newOutputStream(archivePath), Math.multiplyExact(context.maxTransferBytes(), 2L))) {

                execResult = exec(
                    operations, COLLECTING_THE_OUTPUTS, namespace, handle.id(), OUTPUT_CONTAINER,
                    List.of("sh", "-c", COLLECT_SCRIPT), null, outputStream, timeout);
            }

            if (execResult.exitCode() != 0) {
                throw new IOException(
                    "The kubernetes runner could not collect the outputs: tar exited with code %d: %s".formatted(
                        execResult.exitCode(), execResult.stderr()));
            }

            markDone(operations, namespace, handle.id());

            return selfDeleting(archivePath);
        } catch (KubernetesClientException exception) {
            Files.deleteIfExists(archivePath);

            throw failure(COLLECTING_THE_OUTPUTS, exception);
        } catch (IOException | InterruptedException | RuntimeException exception) {
            Files.deleteIfExists(archivePath);

            throw exception;
        }
    }

    @Override
    public void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context) {
        if (cleanupMode == CleanupMode.DETACH) {
            return;
        }

        Duration gracePeriod = cleanupMode == CleanupMode.KILL_AND_DELETE ? gracePeriod(handle) : Duration.ZERO;

        boolean interrupted = Thread.interrupted();

        try {
            try (KubernetesOperations operations = operationsFactory.create(
                connectionSettings(context), CLIENT_GRACE)) {

                deleteQuietly(
                    operations, handleNamespace(handle), handle.id(), handle.attributes()
                        .get(PULL_SECRET_ATTRIBUTE),
                    gracePeriod);
            }
        } catch (RuntimeException exception) {
            log.warn("Could not clean up the kubernetes job {}", handle.id(), exception);
        } finally {
            if (interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
        }
    }

    private static Duration gracePeriod(RemoteJobHandle handle) {
        try {
            return Duration.parse(handle.attributes()
                .getOrDefault(KILL_GRACE_PERIOD, "PT0S"));
        } catch (DateTimeParseException exception) {
            log.debug("Ignoring the malformed kill grace period of the kubernetes job {}", handle.id(), exception);

            return Duration.ZERO;
        }
    }

    private RemoteJobExit awaitMainEnded(
        KubernetesOperations operations, String namespace, String podName, Deadline completionDeadline)
        throws InterruptedException {

        while (true) {
            Pod pod = requirePod(operations, namespace, podName);

            RemoteJobExit evicted = evicted(pod);

            if (evicted != null) {
                return evicted;
            }

            ContainerState state = state(pod, MAIN_CONTAINER, false);

            if (state != null && state.getTerminated() != null) {
                Integer exitCode = state.getTerminated()
                    .getExitCode();

                String reason = state.getTerminated()
                    .getReason();

                return new RemoteJobExit(true, exitCode, "Completed".equals(reason) ? null : reason);
            }

            if (completionDeadline.isExpired()) {
                return RemoteJobExit.timedOut();
            }

            Thread.sleep(pollInterval.toMillis());
        }
    }

    @Nullable
    private RemoteJobExit awaitMainStarted(
        KubernetesOperations operations, String namespace, String podName, Deadline runningDeadline)
        throws InterruptedException {

        while (true) {
            Pod pod = requirePod(operations, namespace, podName);

            RemoteJobExit evicted = evicted(pod);

            if (evicted != null) {
                return evicted;
            }

            failFast(pod);

            ContainerState state = state(pod, MAIN_CONTAINER, false);

            if (state != null && (state.getRunning() != null || state.getTerminated() != null)) {
                return null;
            }

            if (runningDeadline.isExpired()) {
                throw notStarted(operations, namespace, podName, runningDeadline);
            }

            Thread.sleep(pollInterval.toMillis());
        }
    }

    private void awaitInitContainer(
        KubernetesOperations operations, String namespace, String podName, Deadline runningDeadline)
        throws InterruptedException {

        while (true) {
            Pod pod = requirePod(operations, namespace, podName);

            failFast(pod);

            ContainerState state = state(pod, INIT_CONTAINER, true);

            if (state != null && state.getRunning() != null) {
                return;
            }

            if (state != null && state.getTerminated() != null) {
                throw new IllegalStateException(
                    "The kubernetes runner's file sidecar ended before it received the inputs: %s".formatted(
                        state.getTerminated()
                            .getReason()));
            }

            if (runningDeadline.isExpired()) {
                throw notStarted(operations, namespace, podName, runningDeadline);
            }

            Thread.sleep(pollInterval.toMillis());
        }
    }

    private Pod createPod(
        KubernetesOperations operations, RemoteJobSpec spec, KubernetesRunnerSettings runnerSettings,
        String namespace, RemoteJobContext context, @Nullable String pullSecretName) throws InterruptedException {

        try {
            return operations.createPod(
                KubernetesPodFactory.pod(
                    spec, runnerSettings, namespace, context.completionBudget(), context.maxTransferBytes(),
                    pullSecretName));
        } catch (KubernetesClientException exception) {
            throw failure("creating the pod", exception);
        }
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private static void deliverInputs(
        KubernetesOperations operations, String namespace, String podName, InputStream inputArchive,
        Deadline runningDeadline) throws IOException, InterruptedException {

        Duration timeout = runningDeadline.remaining()
            .plus(CLIENT_GRACE);

        Path archivePath = Files.createTempFile("bytechef-kubernetes-input-" + UUID.randomUUID() + "-", ".tar");

        KubernetesOperations.ExecResult untar;

        try {
            try (OutputStream archiveOutputStream = Files.newOutputStream(archivePath)) {
                inputArchive.transferTo(archiveOutputStream);
            }

            long archiveSize = Files.size(archivePath);

            try (InputStream archiveInputStream = Files.newInputStream(archivePath)) {
                untar = exec(
                    operations, DELIVERING_THE_INPUTS, namespace, podName, INIT_CONTAINER,
                    List.of("sh", "-c", "head -c %d | tar -xf - -C %s".formatted(archiveSize, WORK_DIRECTORY)),
                    archiveInputStream, OutputStream.nullOutputStream(), timeout);
            }
        } finally {
            Files.deleteIfExists(archivePath);
        }

        if (untar.exitCode() != 0) {
            throw new IllegalStateException(
                "The kubernetes runner failed while %s: tar exited with code %d: %s".formatted(
                    DELIVERING_THE_INPUTS, untar.exitCode(), untar.stderr()));
        }

        KubernetesOperations.ExecResult ready = exec(
            operations, DELIVERING_THE_INPUTS, namespace, podName, INIT_CONTAINER, List.of("touch", READY_MARKER), null,
            OutputStream.nullOutputStream(), timeout);

        if (ready.exitCode() != 0) {
            throw new IllegalStateException(
                "The kubernetes runner failed while %s: the ready marker could not be written: %s".formatted(
                    DELIVERING_THE_INPUTS, ready.stderr()));
        }
    }

    private static void discard(
        KubernetesOperations operations, String namespace, @Nullable String podName, @Nullable String pullSecretName) {

        boolean interrupted = Thread.interrupted();

        try {
            if (podName != null) {
                deleteQuietly(operations, namespace, podName, null, Duration.ZERO);
            }

            if (pullSecretName != null) {
                deleteSecretQuietly(operations, namespace, pullSecretName);
            }
        } finally {
            if (interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
        }
    }

    private static KubernetesOperations.ExecResult exec(
        KubernetesOperations operations, String phase, String namespace, String podName, String container,
        List<String> command, @Nullable InputStream stdin, OutputStream stdout, Duration timeout)
        throws IOException, InterruptedException {

        try {
            return operations.exec(namespace, podName, container, command, stdin, stdout, timeout);
        } catch (IOException exception) {
            if (exception.getCause() instanceof KubernetesClientException clientException) {
                IllegalStateException failure = failure(phase, clientException);

                failure.addSuppressed(exception);

                throw failure;
            }

            if (isInterrupt(exception)) {
                throw interrupted(phase, exception);
            }

            throw exception;
        }
    }

    private static void patchSecretOwner(
        KubernetesOperations operations, String namespace, String secretName, Pod pod) {

        try {
            operations.patchSecretOwner(namespace, secretName, pod);
        } catch (KubernetesClientException exception) {
            if (isInterrupt(exception)) {
                throw exception;
            }

            log.warn(
                "Could not tie the kubernetes pull secret {} to its pod; it is deleted explicitly instead", secretName,
                exception);
        }
    }

    private static void deleteQuietly(
        KubernetesOperations operations, String namespace, String podName, @Nullable String pullSecretName,
        Duration gracePeriod) {

        try {
            operations.deletePod(namespace, podName, gracePeriod);
        } catch (RuntimeException exception) {
            log.warn("Could not delete the kubernetes pod {}", podName, exception);
        }

        if (pullSecretName != null) {
            deleteSecretQuietly(operations, namespace, pullSecretName);
        }
    }

    private static void deleteSecretQuietly(KubernetesOperations operations, String namespace, String secretName) {
        try {
            operations.deleteSecret(namespace, secretName);
        } catch (RuntimeException exception) {
            log.warn("Could not delete the kubernetes pull secret {}", secretName, exception);
        }
    }

    @Nullable
    private static RemoteJobExit evicted(Pod pod) {
        PodStatus status = pod.getStatus();

        if (status != null && "Failed".equals(status.getPhase()) && status.getReason() != null) {
            return new RemoteJobExit(true, null, "%s: %s".formatted(status.getReason(), status.getMessage()));
        }

        return null;
    }

    /**
     * Wraps a client error with its phase, or throws an {@link InterruptedException} (clearing the interrupt flag) when
     * the error stems from an interrupt, so the caller sees the interrupt rather than a failure.
     */
    private static IllegalStateException failure(String phase, KubernetesClientException exception)
        throws InterruptedException {

        if (isInterrupt(exception)) {
            throw interrupted(phase, exception);
        }

        return clientFailure(phase, exception);
    }

    private static IllegalStateException clientFailure(String phase, KubernetesClientException exception) {
        if (exception.getCode() <= 0) {
            return new IllegalStateException(
                "The kubernetes runner failed while %s: no response from the API server".formatted(phase), exception);
        }

        Status status = exception.getStatus();

        String reason = status == null || status.getReason() == null ? "" : " " + status.getReason();

        return new IllegalStateException(
            "The kubernetes runner failed while %s: HTTP %d%s".formatted(phase, exception.getCode(), reason),
            exception);
    }

    private static InterruptedException interrupted(String phase, Exception cause) {
        Thread.interrupted();

        InterruptedException interruptedException = new InterruptedException(
            "The kubernetes runner was interrupted while " + phase);

        interruptedException.initCause(cause);

        return interruptedException;
    }

    private static boolean isInterrupt(Throwable exception) {
        if (Thread.currentThread()
            .isInterrupted()) {

            return true;
        }

        Throwable cause = exception;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof InterruptedException ||
                (cause instanceof InterruptedIOException && !(cause instanceof SocketTimeoutException))) {

                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private static void failFast(Pod pod) {
        PodStatus status = pod.getStatus();

        if (status == null) {
            return;
        }

        List<ContainerStatus> containerStatuses = new ArrayList<>();

        if (status.getInitContainerStatuses() != null) {
            containerStatuses.addAll(status.getInitContainerStatuses());
        }

        if (status.getContainerStatuses() != null) {
            containerStatuses.addAll(status.getContainerStatuses());
        }

        for (ContainerStatus containerStatus : containerStatuses) {
            ContainerState state = containerStatus.getState();

            if (state != null && state.getWaiting() != null && FAIL_FAST_REASONS.contains(state.getWaiting()
                .getReason())) {

                throw new IllegalStateException(
                    "The kubernetes runner's container %s cannot start: %s: %s".formatted(
                        containerStatus.getName(), state.getWaiting()
                            .getReason(),
                        state.getWaiting()
                            .getMessage()));
            }
        }
    }

    private static Map<String, String> annotations(Pod pod) {
        Map<String, String> annotations = pod.getMetadata()
            .getAnnotations();

        return annotations == null ? Map.of() : annotations;
    }

    private static RemoteJobHandle handle(Pod pod, String namespace) {
        Map<String, String> annotations = annotations(pod);

        Map<String, String> attributes = new LinkedHashMap<>();

        attributes.put(NAMESPACE_ATTRIBUTE, namespace);
        attributes.put(
            KILL_GRACE_PERIOD, annotations.getOrDefault(KubernetesJobLabels.ANNOTATION_KILL_GRACE_PERIOD, "PT0S"));

        String pullSecretName = annotations.get(KubernetesJobLabels.ANNOTATION_PULL_SECRET);

        if (pullSecretName != null) {
            attributes.put(PULL_SECRET_ATTRIBUTE, pullSecretName);
        }

        return new RemoteJobHandle(pod.getMetadata()
            .getName(), attributes);
    }

    private static String handleNamespace(RemoteJobHandle handle) {
        String namespace = handle.attributes()
            .get(NAMESPACE_ATTRIBUTE);

        if (namespace == null) {
            throw new IllegalStateException("The kubernetes job handle %s names no namespace".formatted(handle.id()));
        }

        return namespace;
    }

    private static boolean isPastInputStage(Pod pod) {
        ContainerState state = state(pod, INIT_CONTAINER, true);

        return state != null && state.getTerminated() != null && Integer.valueOf(0)
            .equals(state.getTerminated()
                .getExitCode());
    }

    private static boolean isRunning(Pod pod, String containerName) {
        ContainerState state = state(pod, containerName, false);

        return state != null && state.getRunning() != null;
    }

    private static KubernetesConnectionSettings connectionSettings(RemoteJobContext context) {
        ComponentConnection connection = context.connection();

        if (connection == null) {
            throw new IllegalArgumentException(
                "The Kubernetes runner needs a connection; select one in the task's connections");
        }

        return KubernetesConnectionSettings.of(connection);
    }

    private static void markDone(KubernetesOperations operations, String namespace, String podName)
        throws InterruptedException {

        try {
            operations.exec(
                namespace, podName, OUTPUT_CONTAINER, List.of("touch", DONE_MARKER), null,
                OutputStream.nullOutputStream(), CLIENT_GRACE);
        } catch (IOException | RuntimeException exception) {
            log.debug("Could not mark the kubernetes pod {} done; cleanup removes it", podName, exception);
        }
    }

    private static String namespace(Parameters runnerParameters, KubernetesConnectionSettings connectionSettings) {
        String namespace = runnerParameters.getString(KubernetesRunnerSettings.NAMESPACE);

        return namespace == null || namespace.isBlank() ? connectionSettings.namespace() : namespace.trim();
    }

    private static IllegalStateException notStarted(
        KubernetesOperations operations, String namespace, String podName, Deadline runningDeadline) {

        String event = operations.lastWarningEvent(namespace, podName);

        return new IllegalStateException(
            "The kubernetes job did not start within %s%s".formatted(
                runningDeadline.getBudget(), event == null ? "" : ": " + event));
    }

    private static Pod requirePod(KubernetesOperations operations, String namespace, String podName) {
        Pod pod = operations.getPod(namespace, podName);

        if (pod == null) {
            throw new IllegalStateException("The kubernetes pod %s disappeared".formatted(podName));
        }

        return pod;
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private static InputStream selfDeleting(Path path) throws IOException {
        return new FilterInputStream(Files.newInputStream(path)) {

            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Files.deleteIfExists(path);
                }
            }
        };
    }

    @Nullable
    private static ContainerState state(Pod pod, String containerName, boolean initContainer) {
        PodStatus status = pod.getStatus();

        if (status == null) {
            return null;
        }

        List<ContainerStatus> containerStatuses = initContainer
            ? status.getInitContainerStatuses() : status.getContainerStatuses();

        if (containerStatuses == null) {
            return null;
        }

        for (ContainerStatus containerStatus : containerStatuses) {
            if (containerName.equals(containerStatus.getName())) {
                return containerStatus.getState();
            }
        }

        return null;
    }

    private static final class CeilingOutputStream extends FilterOutputStream {

        private final long ceiling;

        private long written;

        CeilingOutputStream(OutputStream outputStream, long ceiling) {
            super(outputStream);

            this.ceiling = ceiling;
        }

        @Override
        public void write(int value) throws IOException {
            count(1);

            out.write(value);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            count(length);

            out.write(buffer, offset, length);
        }

        private void count(int length) throws IOException {
            written += length;

            if (written > ceiling) {
                throw new IOException(
                    "The kubernetes job's outputs exceed the %d byte transfer ceiling".formatted(ceiling));
            }
        }
    }

    private static final class SinkOutputStream extends OutputStream {

        private final RemoteLogSink logSink;

        SinkOutputStream(RemoteLogSink logSink) {
            this.logSink = logSink;
        }

        @Override
        public void write(int value) {
            logSink.appendStdout(new byte[] {
                (byte) value
            }, 1);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) {
            byte[] copy = Arrays.copyOfRange(buffer, offset, offset + length);

            logSink.appendStdout(copy, length);
        }
    }
}
