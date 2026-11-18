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
 * @author Ivica Cardic
 * @version ee
 */
interface KubernetesOperations extends AutoCloseable {

    Secret createSecret(Secret secret);

    Pod createPod(Pod pod);

    @Nullable
    Pod getPod(String namespace, String name);

    List<Pod> listPods(String namespace, Map<String, String> labels);

    void deletePod(String namespace, String name, Duration gracePeriod);

    void deleteSecret(String namespace, String name);

    void patchSecretOwner(String namespace, String secretName, Pod owner);

    ExecResult exec(
        String namespace, String podName, String container, List<String> command, @Nullable InputStream stdin,
        OutputStream stdout, Duration timeout) throws IOException, InterruptedException;

    LogStream followLogs(String namespace, String podName, String container, OutputStream outputStream);

    @Nullable
    String lastWarningEvent(String namespace, String podName);

    @Override
    void close();

    record ExecResult(int exitCode, String stderr) {
    }

    interface LogStream extends Closeable {

        boolean awaitCompletion(Duration timeout) throws InterruptedException;

        @Override
        void close();
    }
}
