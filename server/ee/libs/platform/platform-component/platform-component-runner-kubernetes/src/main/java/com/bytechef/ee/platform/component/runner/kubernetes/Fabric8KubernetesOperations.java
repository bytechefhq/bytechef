/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ContainerResource;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link KubernetesOperations} over the fabric8 client.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class Fabric8KubernetesOperations implements KubernetesOperations {

    private static final Logger log = LoggerFactory.getLogger(Fabric8KubernetesOperations.class);

    private static final int MAX_STDERR_BYTES = 8 * 1024;

    private final KubernetesClient kubernetesClient;

    Fabric8KubernetesOperations(KubernetesConnectionSettings connectionSettings, Duration requestTimeout) {
        this.kubernetesClient = new KubernetesClientBuilder().withConfig(connectionSettings.toConfig(requestTimeout))
            .build();
    }

    @Override
    public Secret createSecret(Secret secret) {
        return kubernetesClient.secrets()
            .inNamespace(secret.getMetadata()
                .getNamespace())
            .resource(secret)
            .create();
    }

    @Override
    public Pod createPod(Pod pod) {
        return kubernetesClient.pods()
            .inNamespace(pod.getMetadata()
                .getNamespace())
            .resource(pod)
            .create();
    }

    @Override
    public @Nullable Pod getPod(String namespace, String name) {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(name)
            .get();
    }

    @Override
    public List<Pod> listPods(String namespace, Map<String, String> labels) {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems();
    }

    @Override
    public void deletePod(String namespace, String name, Duration gracePeriod) {
        kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(name)
            .withGracePeriod(gracePeriod.toSeconds())
            .delete();
    }

    @Override
    public void deleteSecret(String namespace, String name) {
        kubernetesClient.secrets()
            .inNamespace(namespace)
            .withName(name)
            .delete();
    }

    @Override
    public void patchSecretOwner(String namespace, String secretName, Pod owner) {
        ObjectMeta ownerMetadata = owner.getMetadata();

        OwnerReference ownerReference = new OwnerReferenceBuilder().withApiVersion("v1")
            .withKind("Pod")
            .withName(ownerMetadata.getName())
            .withUid(ownerMetadata.getUid())
            .withBlockOwnerDeletion(false)
            .withController(false)
            .build();

        String patch = kubernetesClient.getKubernetesSerialization()
            .asJson(Map.of("metadata", Map.of("ownerReferences", List.of(ownerReference))));

        kubernetesClient.secrets()
            .inNamespace(namespace)
            .withName(secretName)
            .patch(PatchContext.of(PatchType.STRATEGIC_MERGE), patch);
    }

    @Override
    public ExecResult exec(
        String namespace, String podName, String container, List<String> command, @Nullable InputStream stdin,
        OutputStream stdout, Duration timeout) throws IOException, InterruptedException {

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        FailureRecordingOutputStream recordingStdout = new FailureRecordingOutputStream(stdout);

        ContainerResource containerResource = kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(podName)
            .inContainer(container);

        String[] commandArray = command.toArray(new String[0]);

        try (ExecWatch execWatch = stdin == null
            ? containerResource.writingOutput(recordingStdout)
                .writingError(stderr)
                .exec(commandArray)
            : containerResource.redirectingInput()
                .writingOutput(recordingStdout)
                .writingError(stderr)
                .exec(commandArray)) {

            if (stdin != null) {
                try (OutputStream input = execWatch.getInput()) {
                    stdin.transferTo(input);
                }
            }

            CompletableFuture.anyOf(execWatch.exitCode(), recordingStdout.failure)
                .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (recordingStdout.failure.isDone()) {
                throw recordingStdout.failure.join();
            }

            Integer exitCode = execWatch.exitCode()
                .join();

            return new ExecResult(exitCode == null ? -1 : exitCode, tail(stderr));
        } catch (CompletionException | ExecutionException exception) {
            throw new IOException("The command failed in container " + container, exception.getCause());
        } catch (TimeoutException exception) {
            throw new IOException(
                "The command did not finish within %s in container %s".formatted(timeout, container), exception);
        }
    }

    @Override
    public LogStream followLogs(String namespace, String podName, String container, OutputStream outputStream) {
        LogWatch logWatch = kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(podName)
            .inContainer(container)
            .watchLog(outputStream);

        return new LogStream() {

            @Override
            public boolean awaitCompletion(Duration timeout) throws InterruptedException {
                try {
                    logWatch.onClose()
                        .toCompletableFuture()
                        .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

                    return true;
                } catch (ExecutionException | TimeoutException exception) {
                    log.debug("The log of {} did not reach its end", podName, exception);

                    return false;
                }
            }

            @Override
            public void close() {
                logWatch.close();
            }
        };
    }

    @Override
    public @Nullable String lastWarningEvent(String namespace, String podName) {
        try {
            return kubernetesClient.v1()
                .events()
                .inNamespace(namespace)
                .withField("involvedObject.name", podName)
                .list()
                .getItems()
                .stream()
                .filter(event -> "Warning".equals(event.getType()))
                .max(Comparator.comparing(Fabric8KubernetesOperations::eventTime))
                .map(Event::getMessage)
                .orElse(null);
        } catch (KubernetesClientException exception) {
            log.debug("Could not read the events of {}", podName, exception);

            return null;
        }
    }

    @Override
    public void close() {
        kubernetesClient.close();
    }

    private static String eventTime(Event event) {
        String lastTimestamp = event.getLastTimestamp();

        return lastTimestamp == null ? "" : lastTimestamp;
    }

    private static String tail(ByteArrayOutputStream stderr) {
        byte[] bytes = stderr.toByteArray();

        int start = Math.max(0, bytes.length - MAX_STDERR_BYTES);

        return new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
    }

    /**
     * Keeps the first write failure of the sink, which fabric8 only logs, so {@link #exec} can report it.
     */
    private static final class FailureRecordingOutputStream extends FilterOutputStream {

        private final CompletableFuture<IOException> failure = new CompletableFuture<>();

        FailureRecordingOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        public void write(int value) throws IOException {
            if (failure.isDone()) {
                return;
            }

            try {
                out.write(value);
            } catch (IOException exception) {
                failure.complete(exception);

                throw exception;
            }
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            if (failure.isDone()) {
                return;
            }

            try {
                out.write(buffer, offset, length);
            } catch (IOException exception) {
                failure.complete(exception);

                throw exception;
            }
        }
    }
}
