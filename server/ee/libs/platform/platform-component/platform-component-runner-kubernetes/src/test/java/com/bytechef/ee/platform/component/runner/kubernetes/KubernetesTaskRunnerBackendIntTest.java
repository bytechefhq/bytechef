/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.platform.component.runner.external.RemoteTaskRunner;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.Deadline;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Timeout(value = 15, unit = TimeUnit.MINUTES)
class KubernetesTaskRunnerBackendIntTest {

    private static final String SHELL_IMAGE = "busybox:1.36";

    private static K3sContainer k3sContainer;
    private static Map<String, Object> connectionParameters;
    private static KubernetesClient kubernetesClient;

    @BeforeAll
    static void startCluster() {
        assumeTrue(
            DockerClientFactory.instance()
                .isDockerAvailable(),
            "no docker daemon, so k3s cannot start");

        k3sContainer = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.31.4-k3s1"));

        k3sContainer.start();

        Config config = Config.fromKubeconfig(k3sContainer.getKubeConfigYaml());

        connectionParameters = new HashMap<>();

        connectionParameters.put("masterUrl", config.getMasterUrl());
        connectionParameters.put("caCertData", config.getCaCertData());
        connectionParameters.put("clientCertData", config.getClientCertData());
        connectionParameters.put("clientKeyData", config.getClientKeyData());
        connectionParameters.put("namespace", "default");

        kubernetesClient = new KubernetesClientBuilder().withConfig(config)
            .build();
    }

    @AfterAll
    static void stopCluster() {
        if (kubernetesClient != null) {
            kubernetesClient.close();
        }

        if (k3sContainer != null) {
            k3sContainer.stop();
        }
    }

    @Test
    void testAPythonScriptReturnsItsValue() {
        TaskRunnerResult result = runner(() -> "python-key").run(
            scriptRequest(
                """
                    def perform(input, context):
                        return {"doubled": input["value"] * 2}
                    """,
                Map.of("value", 21), Map.of(), actionContext(null, new LinkedHashMap<>())));

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isEqualTo(Map.of("doubled", 42));
    }

    @Test
    void testAnInputFileGoesInAndAnOutputFileComesBack() {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerResult result = runner(() -> "files-key").run(
            commandsRequest(
                List.of(
                    "cat \"$BYTECHEF_WORKING_DIR/greeting.txt\" > \"$BYTECHEF_OUTPUT_DIR/echoed.txt\"",
                    "echo collected"),
                Map.of("greeting.txt", "hello from the host"), List.of("*.txt"), Map.of("image", SHELL_IMAGE),
                actionContext(null, storedFiles)));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("collected");
        assertThat(new String(storedFiles.get("echoed.txt"), StandardCharsets.UTF_8)).isEqualTo("hello from the host");
    }

    @Test
    void testAFailingCommandReportsItsCodeAndStderr() {
        RemoteTaskRunner runner = runner(() -> "failing-key");

        TaskRunnerRequest request = commandsRequest(
            List.of("echo 'the interpreter did not like that' >&2", "exit 7"), Map.of(), List.of(),
            Map.of("image", SHELL_IMAGE), actionContext(null, new LinkedHashMap<>()));

        assertThatThrownBy(() -> runner.run(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("code 7")
            .hasMessageContaining("the interpreter did not like that");
    }

    @Test
    void testAJobThatOutlivesItsDeadlineIsKilledAndRemoved() throws InterruptedException {
        RemoteTaskRunner runner = runner(() -> "timeout-key");

        TaskRunnerRequest request = commandsRequest(
            List.of("sleep 300"), Map.of(), List.of(), Map.of("image", SHELL_IMAGE, "waitUntilCompletion", "PT20S"),
            actionContext(null, new LinkedHashMap<>()));

        assertThatThrownBy(() -> runner.run(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("timed out");

        assertThat(awaitNoPods("timeout-key")).isTrue();
    }

    @Test
    void testAnInterruptedRunIsResumedByTheNextRunWithTheSameKey() throws InterruptedException {
        TaskRunnerRequest first = commandsRequest(
            List.of("sleep 15", "echo '{\"ok\": true}' > \"$BYTECHEF_OUTPUT_DIR/output.json\"", "echo done"),
            Map.of(), List.of(), Map.of("image", SHELL_IMAGE), actionContext(42L, new LinkedHashMap<>()));

        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                runner(() -> "unused").run(first);
            } catch (RuntimeException exception) {
                failure.set(exception);
            }
        });

        thread.start();

        assertThat(awaitMainRunning()).isTrue();

        thread.interrupt();
        thread.join(Duration.ofSeconds(30)
            .toMillis());

        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);

        TaskRunnerRequest second = commandsRequest(
            List.of("echo never-runs"), Map.of(), List.of(), Map.of("image", SHELL_IMAGE),
            actionContext(42L, new LinkedHashMap<>()));

        try {
            TaskRunnerResult result = runner(() -> "unused").run(second);

            assertThat(result.output()).isEqualTo(Map.of("ok", true));
            assertThat(result.stdout()).contains("done")
                .doesNotContain("never-runs");
        } finally {
            kubernetesClient.pods()
                .inNamespace("default")
                .withLabel("bytechef.io/managed-by", "bytechef")
                .withGracePeriod(0)
                .delete();
        }
    }

    @Test
    void testThePullSecretIsOwnedByItsPodAndGoesWithIt() throws Exception {
        Map<String, ?> runnerParameters = Map.of(
            "image", SHELL_IMAGE, "credentials",
            Map.of("registry", "registry.invalid", "username", "user", "password", "not-a-real-password"));

        KubernetesTaskRunnerBackend backend = new KubernetesTaskRunnerBackend(
            Fabric8KubernetesOperations::new, Duration.ofMillis(500));

        RemoteJobContext context = new RemoteJobContext(
            new ComponentConnection("kubernetes", 1, 1L, new HashMap<>(connectionParameters), null), Map.of(), null,
            ParametersFactory.create(runnerParameters), 1024L * 1024, Duration.ofMinutes(5));

        RemoteJobSpec spec = new RemoteJobSpec(
            new RemoteJobKey("owner-key", null), "shell", "script.sh", List.of("sh"), Map.of(), "/bytechef/work",
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(runnerParameters));

        RemoteJobHandle handle = backend.submit(
            spec, new ByteArrayInputStream(new byte[1024]), Deadline.after(Duration.ofMinutes(5)), context);

        try {
            String secretName = handle.attributes()
                .get("pullSecret");

            Pod pod = kubernetesClient.pods()
                .inNamespace("default")
                .withName(handle.id())
                .get();

            Secret secret = kubernetesClient.secrets()
                .inNamespace("default")
                .withName(secretName)
                .get();

            assertThat(secret.getMetadata()
                .getOwnerReferences()).singleElement()
                    .satisfies(ownerReference -> {
                        assertThat(ownerReference.getKind()).isEqualTo("Pod");
                        assertThat(ownerReference.getName()).isEqualTo(handle.id());
                        assertThat(ownerReference.getUid()).isEqualTo(pod.getMetadata()
                            .getUid());
                    });

            kubernetesClient.pods()
                .inNamespace("default")
                .withName(handle.id())
                .withGracePeriod(0)
                .delete();

            assertThat(awaitNoSecret(secretName)).isTrue();
        } finally {
            backend.cleanup(handle, CleanupMode.DELETE, context);
        }
    }

    private static boolean awaitNoSecret(String secretName) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofMinutes(2)
            .toNanos();

        while (System.nanoTime() < deadline) {
            if (kubernetesClient.secrets()
                .inNamespace("default")
                .withName(secretName)
                .get() == null) {

                return true;
            }

            Thread.sleep(500);
        }

        return false;
    }

    private static boolean awaitMainRunning() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofMinutes(5)
            .toNanos();

        while (System.nanoTime() < deadline) {
            for (Pod pod : kubernetesClient.pods()
                .inNamespace("default")
                .withLabel("bytechef.io/managed-by", "bytechef")
                .list()
                .getItems()) {

                if (pod.getStatus()
                    .getContainerStatuses()
                    .stream()
                    .anyMatch(status -> "main".equals(status.getName()) && status.getState()
                        .getRunning() != null)) {

                    return true;
                }
            }

            Thread.sleep(500);
        }

        return false;
    }

    private static boolean awaitNoPods(String key) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofMinutes(2)
            .toNanos();

        while (System.nanoTime() < deadline) {
            boolean anyLeft = kubernetesClient.pods()
                .inNamespace("default")
                .withLabel("bytechef.io/managed-by", "bytechef")
                .list()
                .getItems()
                .stream()
                .anyMatch(pod -> key.equals(pod.getMetadata()
                    .getAnnotations()
                    .get("bytechef.io/key"))
                    && pod.getMetadata()
                        .getDeletionTimestamp() == null);

            if (!anyLeft) {
                return true;
            }

            Thread.sleep(500);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static ActionContextAware actionContext(Long taskExecutionId, Map<String, byte[]> storedFiles) {
        ActionContextAware actionContext = mock(ActionContextAware.class);
        File file = mock(File.class);

        when(actionContext.getTaskExecutionId()).thenReturn(taskExecutionId);
        when(actionContext.getJobId()).thenReturn(taskExecutionId == null ? null : 900L);

        ArgumentCaptor<ContextFunction<File, ?>> fileFunctionArgumentCaptor =
            ArgumentCaptor.forClass(ContextFunction.class);

        try {
            when(actionContext.file(fileFunctionArgumentCaptor.capture()))
                .thenAnswer(invocation -> fileFunctionArgumentCaptor.getValue()
                    .apply(file));
            when(file.storeContent(anyString(), any(InputStream.class)))
                .thenAnswer(invocation -> {
                    String fileName = invocation.getArgument(0);
                    InputStream inputStream = invocation.getArgument(1);

                    storedFiles.put(fileName, inputStream.readAllBytes());

                    return mock(FileEntry.class);
                });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not arrange the storing action context", exception);
        }

        return actionContext;
    }

    @Test
    void testADeclaredConnectionReachesThePodAndIsMasked() {
        RemoteTaskRunner runner = runner(() -> "connection-key");

        Map<String, ComponentConnection> connections = new HashMap<>(connections());

        connections.put("slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null));

        TaskRunnerRequest request = commandsRequest(
            List.of(
                "test \"$BYTECHEF_CONNECTION_SLACK_TOKEN\" = 'xoxb-123456' && echo matched",
                "echo \"token=$BYTECHEF_CONNECTION_SLACK_TOKEN\""),
            Map.of(), List.of(), Map.of("image", SHELL_IMAGE), actionContext(null, new LinkedHashMap<>()),
            connections);

        TaskRunnerResult taskRunnerResult = runner.run(request);

        assertThat(taskRunnerResult.stdout()).contains("matched")
            .contains("token=******")
            .doesNotContain("xoxb-123456");
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("kubernetes", runner);

        applicationProperties.getScript()
            .setRunners(runners);
        applicationProperties.getScript()
            .setInjectConnections(true);

        return applicationProperties;
    }

    private static TaskRunnerRequest commandsRequest(
        List<String> commands, Map<String, ?> inputFiles, List<String> outputFilePatterns,
        Map<String, ?> runnerParameters, ActionContextAware actionContext) {

        return commandsRequest(
            commands, inputFiles, outputFilePatterns, runnerParameters, actionContext, connections());
    }

    private static TaskRunnerRequest commandsRequest(
        List<String> commands, Map<String, ?> inputFiles, List<String> outputFilePatterns,
        Map<String, ?> runnerParameters, ActionContextAware actionContext,
        Map<String, ComponentConnection> connections) {

        return new TaskRunnerRequest(
            "shell", null, commands, Map.of(), Map.of(), inputFiles, outputFilePatterns,
            ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), null, connections,
            actionContext);
    }

    private static Map<String, ComponentConnection> connections() {
        return Map.of(
            "taskRunner", new ComponentConnection("kubernetes", 1, 1L, new HashMap<>(connectionParameters), null));
    }

    private static RemoteTaskRunner runner(Supplier<String> keySupplier) {
        return new RemoteTaskRunner(
            new KubernetesTaskRunnerBackend(Fabric8KubernetesOperations::new, Duration.ofMillis(500)),
            applicationProperties(), keySupplier);
    }

    private static TaskRunnerRequest scriptRequest(
        String script, Map<String, ?> input, Map<String, ?> runnerParameters, ActionContextAware actionContext) {

        return new TaskRunnerRequest(
            "python", script, List.of(), input, Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(runnerParameters), null, connections(), actionContext);
    }
}
