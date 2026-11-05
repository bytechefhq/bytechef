/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The cluster calls the Kubernetes runner makes, kept narrow so the runner can be tested without a cluster.
 *
 * @author Ivica Cardic
 * @version ee
 */
interface KubernetesOperations extends AutoCloseable {

    /**
     * Creates the secret and returns it as stored, with its generated name.
     */
    Secret createSecret(Secret secret);

    /**
     * Creates the pod and returns it as stored, with its generated name.
     */
    Pod createPod(Pod pod);

    @Nullable
    Pod getPod(String namespace, String name);

    List<Pod> listPods(String namespace, Map<String, String> labels);

    void deletePod(String namespace, String name, Duration gracePeriod);

    void deleteSecret(String namespace, String name);

    /**
     * Makes {@code owner} the owner of the secret, so the cluster's garbage collector deletes the secret with the pod.
     */
    void patchSecretOwner(String namespace, String secretName, Pod owner);

    /**
     * Runs a command in a container, streaming {@code stdin} (when given) to it and its stdout to {@code stdout}, and
     * waits up to {@code timeout} for it to exit.
     */
    ExecResult exec(
        String namespace, String podName, String container, List<String> command, @Nullable InputStream stdin,
        OutputStream stdout, Duration timeout) throws IOException, InterruptedException;

    /**
     * Follows a container's log into {@code outputStream} until the container ends or the stream is closed.
     */
    LogStream followLogs(String namespace, String podName, String container, OutputStream outputStream);

    /**
     * Returns the message of the most recent warning event about the pod, such as a scheduling failure.
     */
    @Nullable
    String lastWarningEvent(String namespace, String podName);

    @Override
    void close();

    record ExecResult(int exitCode, String stderr) {
    }

    interface LogStream extends Closeable {

        /**
         * Waits up to {@code timeout} for the log to reach its end; returns whether it did.
         */
        boolean awaitCompletion(Duration timeout) throws InterruptedException;

        @Override
        void close();
    }
}
