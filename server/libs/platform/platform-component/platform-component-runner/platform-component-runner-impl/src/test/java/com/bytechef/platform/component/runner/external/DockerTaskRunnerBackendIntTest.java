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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Timeout(value = 10, unit = TimeUnit.MINUTES)
class DockerTaskRunnerBackendIntTest {

    private static final String IMAGE = "python:3.12-slim";

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private static final Duration PULL_TIMEOUT = Duration.ofMinutes(5);

    @BeforeAll
    static void assumeADaemonAnswersAndHoldsTheImage() {
        assumeTrue(isDaemonReachable(), "no docker daemon answered, so the docker runner cannot be exercised");
        assumeTrue(
            pullImage(), "the docker registry did not hand over " + IMAGE + ", so the docker runner cannot be " +
                "exercised");
    }

    @Test
    void testAnInputFileReachesTheContainerAndAnOutputFileComesBack() {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerRequest request = commandsRequest(
            List.of(
                "cat \"$BYTECHEF_WORKING_DIR/greeting.txt\" > \"$BYTECHEF_OUTPUT_DIR/echoed.txt\"",
                "echo collected"),
            Map.of("greeting.txt", "hello from the host"), List.of("*.txt"), Map.of("image", IMAGE), storedFiles);

        TaskRunnerResult taskRunnerResult = taskRunner().run(request);

        assertThat(taskRunnerResult.exitCode()).isZero();
        assertThat(taskRunnerResult.stdout()).contains("collected");
        assertThat(taskRunnerResult.outputFiles()).containsOnlyKeys("echoed.txt");
        assertThat(storedFiles).containsOnlyKeys("echoed.txt");
        assertThat(new String(storedFiles.get("echoed.txt"), StandardCharsets.UTF_8)).isEqualTo("hello from the host");
    }

    @Test
    void testOnlyTheOutputFilesMatchingAGlobAreCollected() {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerRequest request = commandsRequest(
            List.of(
                "echo 'a,b' > \"$BYTECHEF_OUTPUT_DIR/report.csv\"",
                "mkdir -p \"$BYTECHEF_OUTPUT_DIR/nested\"",
                "echo 'c,d' > \"$BYTECHEF_OUTPUT_DIR/nested/deep.csv\"",
                "echo ignore me > \"$BYTECHEF_OUTPUT_DIR/notes.txt\""),
            Map.of(), List.of("*.csv"), Map.of("image", IMAGE), storedFiles);

        TaskRunnerResult taskRunnerResult = taskRunner().run(request);

        assertThat(taskRunnerResult.outputFiles()).containsOnlyKeys("report.csv");
        assertThat(storedFiles).containsOnlyKeys("report.csv");
    }

    @Test
    void testANonZeroExitFailsTheTaskWithTheStderrTail() {
        TaskRunnerRequest request = commandsRequest(
            List.of("echo 'the interpreter did not like that' >&2", "exit 7"), Map.of(), List.of(),
            Map.of("image", IMAGE), new LinkedHashMap<>());

        RemoteTaskRunner remoteTaskRunner = taskRunner();

        assertThatThrownBy(() -> remoteTaskRunner.run(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("code 7")
            .hasMessageContaining("the interpreter did not like that");
    }

    @Test
    void testAPerformReturnValueComesBackAsTheResultsVars() {
        TaskRunnerRequest request = scriptRequest(
            """
                def perform(input, context):
                    return {"greeting": "hi", "doubled": input["value"] * 2}
                """,
            Map.of("value", 21), Map.of("image", IMAGE));

        TaskRunnerResult taskRunnerResult = taskRunner().run(request);

        assertThat(taskRunnerResult.exitCode()).isZero();
        assertThat(taskRunnerResult.output()).isEqualTo(Map.of("greeting", "hi", "doubled", 42));
    }

    @Test
    void testARequestNamingNoImageRunsInTheLanguagesDefaultImage() {
        TaskRunnerRequest request = scriptRequest(
            """
                def perform(input, context):
                    return {"ran": True}
                """,
            Map.of(), Map.of());

        TaskRunnerResult taskRunnerResult = taskRunner().run(request);

        assertThat(taskRunnerResult.exitCode()).isZero();
        assertThat(taskRunnerResult.output()).isEqualTo(Map.of("ran", true));
    }

    @Test
    void testAContainerRunningAsANonRootUserCanWriteItsOutput() {
        TaskRunnerRequest request = scriptRequest(
            """
                def perform(input, context):
                    return {"ran": True}
                """,
            Map.of(), Map.of("image", IMAGE, "user", "1000:1000"));

        TaskRunnerResult taskRunnerResult = taskRunner().run(request);

        assertThat(taskRunnerResult.exitCode()).isZero();
        assertThat(taskRunnerResult.output()).isEqualTo(Map.of("ran", true));
    }

    @Test
    void testTheContainersHomeIsItsWorkingDirectoryEvenForANumericUser() {
        TaskRunnerRequest request = commandsRequest(
            List.of("echo \"$HOME\"", "touch \"$HOME/cache-probe\""), Map.of(), List.of(),
            Map.of("image", IMAGE, "user", "1000:1000"), new LinkedHashMap<>());

        TaskRunnerResult taskRunnerResult = taskRunner().run(request);

        assertThat(taskRunnerResult.exitCode()).isZero();
        assertThat(taskRunnerResult.stdout()).contains("/tmp");
    }

    @Test
    void testAContainerLeftBehindIsReattachedByTheNextRunWithTheSameKey() {
        RemoteTaskRunner first = new RemoteTaskRunner(
            new DockerTaskRunnerBackend(), applicationProperties(), () -> "resume-key");

        TaskRunnerRequest detached = commandsRequest(
            List.of("sleep 2", "echo '{\"ok\": true}' > \"$BYTECHEF_OUTPUT_DIR/output.json\"", "echo done"),
            Map.of(), List.of(), Map.of("image", IMAGE, "delete", false),
            resumableActionContext(42L));

        try {
            TaskRunnerResult firstResult = first.run(detached);

            assertThat(firstResult.exitCode()).isZero();

            TaskRunnerRequest reattach = commandsRequest(
                List.of("echo never-runs"), Map.of(), List.of(), Map.of("image", IMAGE),
                resumableActionContext(42L));

            RemoteTaskRunner second = new RemoteTaskRunner(
                new DockerTaskRunnerBackend(), applicationProperties(), () -> "unused");

            TaskRunnerResult secondResult = second.run(reattach);

            assertThat(secondResult.stdout()).contains("done")
                .doesNotContain("never-runs");
            assertThat(secondResult.output()).isEqualTo(Map.of("ok", true));
        } finally {
            removeContainersLabelled("io.bytechef.task-execution-id", "public-none-42");
        }
    }

    private static void removeContainersLabelled(String label, String value) {
        try (DockerClient dockerClient = dockerClient(Duration.ofSeconds(30));
            ListContainersCmd listContainersCmd = dockerClient.listContainersCmd()) {

            listContainersCmd.withShowAll(true);
            listContainersCmd.withLabelFilter(Map.of(label, value));

            for (Container container : listContainersCmd.exec()) {
                try (RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(container.getId())) {
                    removeContainerCmd.withForce(true);

                    removeContainerCmd.exec();
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove the containers labelled " + label, exception);
        }
    }

    private static DockerClient dockerClient(Duration responseTimeout) {
        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .build();

        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.getDockerHost())
            .sslConfig(dockerClientConfig.getSSLConfig())
            .responseTimeout(responseTimeout)
            .build();

        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }

    @Test
    void testTheConnectionsFileReachesTheContainerAndIsMasked() {
        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null,
            List.of(
                "grep -q 'xoxb-123456' \"$BYTECHEF_CONNECTIONS_FILE\" && echo matched",
                "echo \"file=$(cat \"$BYTECHEF_CONNECTIONS_FILE\")\"",
                "echo \"[${BYTECHEF_CONNECTION_SLACK_TOKEN:-}]\""),
            Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of("image", IMAGE)), TIMEOUT,
            Map.of("slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null)),
            storingActionContext(new LinkedHashMap<>(), null));

        TaskRunnerResult taskRunnerResult = taskRunner().run(request);

        assertThat(taskRunnerResult.stdout()).contains("matched")
            .contains("file=******")
            .contains("[]")
            .doesNotContain("xoxb-123456");
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(Map.of());

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("docker", runner);

        script.setRunners(runners);
        script.setInjectConnections(true);

        return applicationProperties;
    }

    private static TaskRunnerRequest commandsRequest(
        List<String> commands, Map<String, ?> inputFiles, List<String> outputFilePatterns,
        Map<String, ?> runnerParameters, Map<String, byte[]> storedFiles) {

        return commandsRequest(
            commands, inputFiles, outputFilePatterns, runnerParameters, storingActionContext(storedFiles, null));
    }

    private static TaskRunnerRequest commandsRequest(
        List<String> commands, Map<String, ?> inputFiles, List<String> outputFilePatterns,
        Map<String, ?> runnerParameters, ActionContext actionContext) {

        return new TaskRunnerRequest(
            "shell", null, commands, Map.of(), Map.of(), inputFiles, outputFilePatterns,
            ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), TIMEOUT, Map.of(),
            actionContext);
    }

    private static boolean isDaemonReachable() {
        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .build();

        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.getDockerHost())
            .sslConfig(dockerClientConfig.getSSLConfig())
            .responseTimeout(Duration.ofSeconds(10))
            .build();

        try (DockerClient dockerClient = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient)) {
            dockerClient.pingCmd()
                .exec();

            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean pullImage() {
        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .build();

        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.getDockerHost())
            .sslConfig(dockerClientConfig.getSSLConfig())
            .responseTimeout(PULL_TIMEOUT)
            .build();

        try (DockerClient dockerClient = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback()) {

            dockerClient.pullImageCmd(IMAGE)
                .exec(pullImageResultCallback);

            return pullImageResultCallback.awaitCompletion(PULL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            return false;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static TaskRunnerRequest scriptRequest(
        String script, Map<String, ?> input, Map<String, ?> runnerParameters) {

        return new TaskRunnerRequest(
            "python", script, List.of(), input, Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(runnerParameters), TIMEOUT, Map.of(),
            storingActionContext(new LinkedHashMap<>(), null));
    }

    private static ActionContext resumableActionContext(long taskExecutionId) {
        return storingActionContext(new LinkedHashMap<>(), taskExecutionId);
    }

    @SuppressWarnings("unchecked")
    private static ActionContext storingActionContext(
        Map<String, byte[]> storedFiles, @Nullable Long taskExecutionId) {

        ActionContextAware actionContext = mock(ActionContextAware.class);
        File file = mock(File.class);

        when(actionContext.getTaskExecutionId()).thenReturn(taskExecutionId);
        when(actionContext.getJobId()).thenReturn(null);

        ArgumentCaptor<ContextFunction<File, ?>> fileFunctionArgumentCaptor =
            ArgumentCaptor.forClass(ContextFunction.class);

        try {
            when(actionContext.file(fileFunctionArgumentCaptor.capture()))
                .thenAnswer(invocation -> {
                    ContextFunction<File, ?> fileFunction = fileFunctionArgumentCaptor.getValue();

                    return fileFunction.apply(file);
                });
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

    private static RemoteTaskRunner taskRunner() {
        return new RemoteTaskRunner(new DockerTaskRunnerBackend(), applicationProperties(), () -> "int-test");
    }
}
