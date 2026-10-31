/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 * @version ee
 */
final class FakeKubernetesOperations implements KubernetesOperations {

    static final String POD_NAME = "bytechef-run-abc12";
    static final String POD_UID = "6f1c2c4e-0d7b-4c55-9d43-1f2a3b4c5d6e";
    static final String SECRET_NAME = "bytechef-pull-xyz34";

    final Deque<Pod> podSnapshots = new ArrayDeque<>();
    final Deque<Integer> execResults = new ArrayDeque<>();
    final List<ExecCall> execCalls = new ArrayList<>();
    final Map<String, Duration> deletedPods = new LinkedHashMap<>();
    final List<String> deletedSecrets = new ArrayList<>();
    final List<Secret> createdSecrets = new ArrayList<>();
    final List<Pod> createdPods = new ArrayList<>();
    final List<Duration> requestTimeouts = new ArrayList<>();
    final List<SecretOwner> secretOwners = new ArrayList<>();

    List<Pod> listedPods = List.of();
    String logText = "";
    byte[] collectBytes = new byte[0];
    @Nullable
    String warningEvent;
    @Nullable
    RuntimeException failCreatePod;
    @Nullable
    RuntimeException failCreateSecret;
    @Nullable
    Exception failExec;
    boolean interruptGetPod;
    @Nullable
    RuntimeException failPatchSecretOwner;
    boolean failDeletePod;
    int closeCount;
    int closedLogs;
    @Nullable
    Map<String, String> listedLabels;
    @Nullable
    String listedNamespace;
    @Nullable
    Pod lastServed;

    KubernetesOperationsFactory factory() {
        return (connectionSettings, requestTimeout) -> {
            requestTimeouts.add(requestTimeout);

            return this;
        };
    }

    @Override
    public Secret createSecret(Secret secret) {
        if (failCreateSecret != null) {
            throw failCreateSecret;
        }

        createdSecrets.add(secret);

        return new SecretBuilder(secret).editMetadata()
            .withName(SECRET_NAME)
            .endMetadata()
            .build();
    }

    @Override
    public Pod createPod(Pod pod) {
        if (failCreatePod != null) {
            throw failCreatePod;
        }

        createdPods.add(pod);

        return new PodBuilder(pod).editMetadata()
            .withName(POD_NAME)
            .withUid(POD_UID)
            .endMetadata()
            .build();
    }

    @Override
    public @Nullable Pod getPod(String namespace, String name) {
        if (interruptGetPod) {
            throw interruptedCall();
        }

        if (podSnapshots.size() > 1) {
            lastServed = podSnapshots.poll();
        } else {
            lastServed = podSnapshots.peek();
        }

        return lastServed;
    }

    @Override
    public List<Pod> listPods(String namespace, Map<String, String> labels) {
        listedNamespace = namespace;
        listedLabels = labels;

        return listedPods;
    }

    @Override
    public void deletePod(String namespace, String name, Duration gracePeriod) {
        failWhenInterrupted();

        if (failDeletePod) {
            throw new KubernetesClientException("boom");
        }

        deletedPods.put(name, gracePeriod);
    }

    @Override
    public void deleteSecret(String namespace, String name) {
        failWhenInterrupted();

        deletedSecrets.add(name);
    }

    @Override
    public void patchSecretOwner(String namespace, String secretName, Pod owner) {
        if (failPatchSecretOwner != null) {
            throw failPatchSecretOwner;
        }

        secretOwners.add(
            new SecretOwner(
                secretName, owner.getMetadata()
                    .getName(),
                owner.getMetadata()
                    .getUid()));
    }

    @Override
    public ExecResult exec(
        String namespace, String podName, String container, List<String> command, @Nullable InputStream stdin,
        OutputStream stdout, Duration timeout) throws IOException {

        if (failExec instanceof IOException ioException) {
            throw ioException;
        }

        if (failExec instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        byte[] input = stdin == null ? null : stdin.readAllBytes();

        execCalls.add(new ExecCall(container, command, input));

        if (String.join(" ", command)
            .contains("tar -cf -")) {

            stdout.write(collectBytes);
        }

        Integer exitCode = execResults.poll();

        return new ExecResult(exitCode == null ? 0 : exitCode, exitCode == null || exitCode == 0 ? "" : "tar: error");
    }

    @Override
    public LogStream followLogs(String namespace, String podName, String container, OutputStream outputStream) {
        try {
            outputStream.write(logText.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }

        return new LogStream() {

            @Override
            public boolean awaitCompletion(Duration timeout) {
                return true;
            }

            @Override
            public void close() {
                closedLogs++;
            }
        };
    }

    @Override
    public @Nullable String lastWarningEvent(String namespace, String podName) {
        return warningEvent;
    }

    @Override
    public void close() {
        closeCount++;
    }

    /**
     * Fails like fabric8 does when the calling thread is interrupted: it re-sets the flag and throws.
     */
    private static KubernetesClientException interruptedCall() {
        Thread.currentThread()
            .interrupt();

        return new KubernetesClientException("interrupted", new InterruptedIOException("interrupted"));
    }

    private static void failWhenInterrupted() {
        if (Thread.currentThread()
            .isInterrupted()) {

            throw new KubernetesClientException("interrupted", new InterruptedIOException("interrupted"));
        }
    }

    record SecretOwner(String secretName, String podName, @Nullable String podUid) {
    }

    record ExecCall(String container, List<String> command, byte @Nullable [] stdin) {
    }

    static Pod pod(
        @Nullable ContainerState init, @Nullable ContainerState main, @Nullable ContainerState output,
        Map<String, String> annotations) {

        List<ContainerStatus> initStatuses = new ArrayList<>();
        List<ContainerStatus> statuses = new ArrayList<>();

        if (init != null) {
            initStatuses.add(status("bytechef-init", init));
        }

        if (main != null) {
            statuses.add(status("main", main));
        }

        if (output != null) {
            statuses.add(status("bytechef-output", output));
        }

        return new PodBuilder().withNewMetadata()
            .withName(POD_NAME)
            .withNamespace("jobs")
            .withAnnotations(annotations)
            .endMetadata()
            .withNewStatus()
            .withPhase("Running")
            .withInitContainerStatuses(initStatuses)
            .withContainerStatuses(statuses)
            .endStatus()
            .build();
    }

    static ContainerState running() {
        return new ContainerStateBuilder().withNewRunning()
            .endRunning()
            .build();
    }

    static ContainerState waiting(String reason, String message) {
        return new ContainerStateBuilder().withNewWaiting()
            .withReason(reason)
            .withMessage(message)
            .endWaiting()
            .build();
    }

    static ContainerState terminated(int exitCode, String reason) {
        return new ContainerStateBuilder().withNewTerminated()
            .withExitCode(exitCode)
            .withReason(reason)
            .endTerminated()
            .build();
    }

    private static ContainerStatus status(String name, ContainerState state) {
        return new ContainerStatusBuilder().withName(name)
            .withState(state)
            .build();
    }
}
