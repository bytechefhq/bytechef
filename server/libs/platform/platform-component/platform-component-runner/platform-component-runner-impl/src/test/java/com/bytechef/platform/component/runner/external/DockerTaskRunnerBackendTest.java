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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.WaitResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DockerTaskRunnerBackendTest {

    private static final String CONTAINER_ID = "container-42";
    private static final String IMAGE = "python:3.12-slim";
    private static final String SCRIPT = "def perform(input, context):\n    return 1\n";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);

    private final DockerTaskRunnerBackend backend = new DockerTaskRunnerBackend();

    @Test
    void testGetTypeIsDocker() {
        assertThat(backend.getType()).isEqualTo("docker");
    }

    @Test
    void testGetTitleIsDocker() {
        assertThat(backend.getTitle()).isEqualTo("Docker");
    }

    @Test
    void testCapabilitiesCoverEverythingButTheComponentBridge() {
        RemoteTaskRunner taskRunner = new RemoteTaskRunner(backend, applicationProperties(Map.of()));

        Set<TaskRunnerCapability> capabilities = taskRunner.getCapabilities();

        assertThat(capabilities).containsExactlyInAnyOrder(
            TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMMANDS, TaskRunnerCapability.INPUT_FILES,
            TaskRunnerCapability.OUTPUT_FILES, TaskRunnerCapability.ENVIRONMENT);
    }

    @Test
    void testGetPropertiesOffersEveryDockerSetting() {
        assertThat(propertyNames(backend, Map.of())).containsExactlyInAnyOrder(
            "image", "entrypoint", "user", "cpu", "memory", "networkMode", "extraHosts", "privileged", "credentials",
            "killGracePeriod");
    }

    @Test
    void testTheImagePropertyIsOptionalBecauseTheLanguageSuppliesADefault() {
        ValueProperty<?> imageProperty = backend.getProperties(Map.of())
            .stream()
            .filter(property -> "image".equals(property.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(imageProperty.getRequired()).isFalse();
    }

    @Test
    void testGetPropertiesReturnsFreshInstances() {
        assertThat(backend.getProperties(Map.of())
            .getFirst())
                .isNotSameAs(
                    backend.getProperties(Map.of())
                        .getFirst());
    }

    @Test
    void testAMissingImageFallsBackToTheLanguagesDefaultImage() {
        backend.validate(request(Map.of()), context(Map.of(), null));
        backend.validate(request(Map.of("image", "   ")), context(Map.of(), null));
    }

    @Test
    void testEachLanguageHasItsOwnDefaultImage() {
        Map<String, String> defaultImagesByLanguageId = Map.of(
            "shell", "ubuntu:24.04", "python", "python:3.12-slim", "javascript", "node:22-alpine", "js",
            "node:22-alpine");

        for (Map.Entry<String, String> entry : defaultImagesByLanguageId.entrySet()) {
            TestDaemon testDaemon = new TestDaemon();

            TaskRunnerRequest request = new TaskRunnerRequest(
                entry.getKey(), null, List.of("true"), Map.of(), Map.of(), Map.of(), List.of(),
                ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), TEST_TIMEOUT, Map.of(),
                mock(ActionContext.class));

            testDaemon.newTaskRunner()
                .run(request);

            verify(testDaemon.dockerClient).createContainerCmd(entry.getValue());
        }
    }

    @Test
    void testADefaultedImageIsTheImageTheDaemonIsHanded() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT, Map.of(), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.dockerClient).createContainerCmd(IMAGE);
    }

    @Test
    void testValidateAcceptsANetworkModeThatIsNotHost() {
        backend.validate(request(Map.of("image", IMAGE, "networkMode", "none")), context(Map.of(), null));
        backend.validate(request(Map.of("image", IMAGE, "networkMode", "bridge")), context(Map.of(), null));
    }

    @Test
    void testValidateRejectsALanguageTheBootstrapCannotExecute() {
        TaskRunnerRequest request = new TaskRunnerRequest(
            "ruby", "puts 1", List.of(), Map.of(), Map.of(), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of("image", IMAGE)),
            TEST_TIMEOUT, Map.of(), mock(ActionContext.class));

        RemoteTaskRunner taskRunner = new RemoteTaskRunner(backend, applicationProperties(Map.of()));

        assertThatThrownBy(() -> taskRunner.validate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ruby");
    }

    @Test
    void testValidateRejectsAMalformedMemoryLimit() {
        assertThatThrownBy(
            () -> backend.validate(request(Map.of("image", IMAGE, "memory", "plenty")), context(Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plenty");
    }

    @Test
    void testValidateRejectsANonPositiveCpuLimit() {
        assertThatThrownBy(() -> backend.validate(request(Map.of("image", IMAGE, "cpu", 0)), context(Map.of(), null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testASuccessfulRunReturnsTheOutputTheContainerWroteAndRemovesTheContainer() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.stdout = "hello-from-container";

        TaskRunnerResult result = testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("hello-from-container");
        assertThat(result.output()).isEqualTo(Map.of("ok", true));
        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testANonZeroExitFailsWithBothStreamTailsAndRemovesTheContainer() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.exitCode = 3;
        testDaemon.stdout = "out-before-failure";
        testDaemon.stderr = "boom";

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest(SCRIPT)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("code 3")
            .hasMessageContaining("out-before-failure")
            .hasMessageContaining("boom");

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testATimeoutRemovesTheContainer() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.containerExits = false;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, Duration.ofMillis(200));

        assertThatThrownBy(() -> taskRunner.run(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("timed out");

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testATimeoutReportsTheStreamTailsTheContainerHadAlreadyWritten() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.containerExits = false;
        testDaemon.stdout = "out-before-timeout";
        testDaemon.stderr = "err-before-timeout";

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, Duration.ofMillis(200));

        assertThatThrownBy(() -> taskRunner.run(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("timed out")
            .hasMessageContaining("out-before-timeout")
            .hasMessageContaining("err-before-timeout");
    }

    @Test
    void testAnInterruptionRemovesTheContainer() throws InterruptedException {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.containerExits = false;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        Thread runnerThread = new Thread(() -> {
            try {
                taskRunner.run(scriptRequest(SCRIPT, Duration.ofMinutes(5)));
            } catch (RuntimeException exception) {
                failure.set(exception);
            }
        });

        runnerThread.start();

        assertThat(testDaemon.waitReached.await(10, TimeUnit.SECONDS)).isTrue();

        runnerThread.interrupt();
        runnerThread.join(Duration.ofSeconds(10)
            .toMillis());

        assertThat(runnerThread.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testAFailedPullNeverCreatesAContainer() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = false;
        testDaemon.pullFails = true;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest(SCRIPT)))
            .isInstanceOf(RuntimeException.class);

        verify(testDaemon.dockerClient, never()).createContainerCmd(anyString());

        assertThat(testDaemon.removedContainerIds).isEmpty();
    }

    @Test
    void testAPullThatNeverCompletesFailsWithinTheTimeout() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = false;
        testDaemon.pullCompletes = false;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, Duration.ofMillis(200));

        assertThatThrownBy(() -> taskRunner.run(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("timed out");

        verify(testDaemon.dockerClient, never()).createContainerCmd(anyString());
    }

    @Test
    void testTheContainerIsRemovedWhenCopyingTheWorkingDirectoryInFails() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.inputCopyFails = true;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest(SCRIPT)))
            .isInstanceOf(RuntimeException.class);

        verify(testDaemon.dockerClient, never()).startContainerCmd(anyString());

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testTheContainerIsRemovedWhenTheLogStreamCannotBeAttached() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.logAttachFails = true;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest(SCRIPT)))
            .isInstanceOf(RuntimeException.class);

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testTheContainerIsRemovedWhenCollectingTheOutputFails() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.outputCopyFails = true;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest(SCRIPT)))
            .isInstanceOf(RuntimeException.class);

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testAMissingOutputDirectoryIsNotAFailure() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.outputPresent = false;

        TaskRunnerResult result = testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isNull();
        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testTheContainerRunsTheLanguagesDefaultInterpreterOnTheSourceFile() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        verify(testDaemon.createContainerCmd).withEntrypoint(List.of("python3"));
        verify(testDaemon.createContainerCmd).withCmd("/tmp/script.py");
    }

    @Test
    void testAnEntrypointOverrideReplacesTheInterpreter() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT,
            Map.of("image", IMAGE, "entrypoint", List.of("python3", "-u")), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.createContainerCmd).withEntrypoint(List.of("python3", "-u"));
        verify(testDaemon.createContainerCmd).withCmd("/tmp/script.py");
    }

    @Test
    void testTheContractVariablesNameTheContainersOwnPaths() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(testDaemon.environment).contains(
            "BYTECHEF_WORKING_DIR=/tmp", "BYTECHEF_INPUT_FILE=/tmp/input.json", "BYTECHEF_OUTPUT_DIR=/tmp/output");
    }

    @Test
    void testADeclaredEnvironmentEntryCannotOverrideTheContractVariables() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT, Map.of("image", IMAGE),
            Map.of("BYTECHEF_OUTPUT_DIR", "/nowhere", "GREETING", "hi"));

        testDaemon.newTaskRunner()
            .run(request);

        assertThat(testDaemon.environment).contains("BYTECHEF_OUTPUT_DIR=/tmp/output", "GREETING=hi");
        assertThat(testDaemon.environment).doesNotContain("BYTECHEF_OUTPUT_DIR=/nowhere");
    }

    @Test
    void testTheCpuAndMemoryLimitsReachTheHostConfig() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT,
            Map.of("image", IMAGE, "cpu", 1.5, "memory", "512m", "networkMode", "none"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        HostConfig hostConfig = testDaemon.hostConfig;

        assertThat(hostConfig.getNanoCPUs()).isEqualTo(1_500_000_000L);
        assertThat(hostConfig.getMemory()).isEqualTo(512L * 1024 * 1024);
        assertThat(hostConfig.getNetworkMode()).isEqualTo("none");
    }

    @Test
    void testTheContainersHomeIsItsWorkingDirectory() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(testDaemon.environment).contains("HOME=/tmp");
    }

    @Test
    void testADeclaredHomeStillWins() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT, Map.of("image", IMAGE),
            Map.of("HOME", "/home/somebody"));

        testDaemon.newTaskRunner()
            .run(request);

        assertThat(testDaemon.environment).contains("HOME=/home/somebody");
        assertThat(testDaemon.environment).doesNotContain("HOME=/tmp");
    }

    @Test
    void testTheContainerCarriesNoBindMounts() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        HostConfig hostConfig = testDaemon.hostConfig;

        assertThat(hostConfig.getBinds()).isNullOrEmpty();
    }

    @Test
    void testThePullIsSkippedWhenTheImageIsAlreadyPresent() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = true;

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        verify(testDaemon.dockerClient, never()).pullImageCmd(anyString());
    }

    @Test
    void testTheAlwaysPolicyPullsEvenWhenTheImageIsPresent() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = true;

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT,
            Map.of("image", IMAGE, "pullPolicy", "ALWAYS"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.dockerClient).pullImageCmd(IMAGE);
    }

    @Test
    void testTheNeverPolicyDoesNotEvenAskWhetherTheImageIsPresent() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = false;

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT,
            Map.of("image", IMAGE, "pullPolicy", "NEVER"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.dockerClient, never()).pullImageCmd(anyString());
        verify(testDaemon.dockerClient, never()).inspectImageCmd(anyString());
    }

    @Test
    void testAPerTaskHostIsRejectedUnlessTheOperatorAllowedIt() {
        assertThatThrownBy(
            () -> backend.validate(request(Map.of("host", "tcp://elsewhere:2375")), context(Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytechef.script.runners.docker.properties.allow-task-host");

        backend.validate(
            request(Map.of("host", "tcp://elsewhere:2375")), context(Map.of("allow-task-host", "true"), null));
    }

    @Test
    void testTheHostPropertyIsOfferedOnlyWhenTheOperatorAllowedIt() {
        assertThat(propertyNames(backend, Map.of())).doesNotContain("host");
        assertThat(propertyNames(backend, Map.of("allow-task-host", "true"))).contains("host");
    }

    @Test
    void testAnAllowedPerTaskHostReachesTheClientFactory() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of("allow-task-host", "true", "host", "unix:///operator.sock");

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT, Map.of("host", "tcp://elsewhere:2375"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        assertThat(testDaemon.hosts).isNotEmpty()
            .allMatch("tcp://elsewhere:2375"::equals);
    }

    @Test
    void testTheOperatorsHostIsUsedWhenTheTaskNamesNone() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of("host", "unix:///operator.sock");

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(testDaemon.hosts).isNotEmpty()
            .allMatch("unix:///operator.sock"::equals);
    }

    @Test
    void testVolumesAreRejectedUnlessTheOperatorAllowedThem() {
        assertThatThrownBy(
            () -> backend.validate(request(Map.of("volumes", List.of("/data:/data:ro"))), context(Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytechef.script.runners.docker.properties.allow-volume-mounts");

        assertThat(propertyNames(backend, Map.of())).doesNotContain("volumes");
        assertThat(propertyNames(backend, Map.of("allow-volume-mounts", "true"))).contains("volumes");
    }

    @Test
    void testAnAllowedVolumeBecomesABind() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of("allow-volume-mounts", "true");

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT, Map.of("volumes", List.of("/data:/data:ro")), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        assertThat(testDaemon.hostConfig.getBinds()).hasSize(1);
    }

    @Test
    void testPrivilegedReachesTheHostConfig() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT, TEST_TIMEOUT, Map.of("privileged", true), Map.of()));

        assertThat(testDaemon.hostConfig.getPrivileged()).isTrue();
    }

    @Test
    void testANamedNetworkIsPassedThrough() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT, TEST_TIMEOUT, Map.of("networkMode", "bytechef_default"), Map.of()));

        assertThat(testDaemon.hostConfig.getNetworkMode()).isEqualTo("bytechef_default");
    }

    @Test
    void testNoLimitsAreSetWhenTheWorkflowNamedNone() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(testDaemon.hostConfig.getNanoCPUs()).isNull();
        assertThat(testDaemon.hostConfig.getMemory()).isNull();
        assertThat(testDaemon.hostConfig.getPidsLimit()).isNull();
    }

    @Test
    void testRegistryCredentialsReachThePull() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = false;

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, TEST_TIMEOUT,
            Map.of("credentials", Map.of("registry", "ghcr.io", "username", "u", "password", "p")), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        ArgumentCaptor<AuthConfig> authConfigArgumentCaptor = ArgumentCaptor.forClass(AuthConfig.class);

        verify(testDaemon.pullImageCmd).withAuthConfig(authConfigArgumentCaptor.capture());

        AuthConfig authConfig = authConfigArgumentCaptor.getValue();

        assertThat(authConfig.getRegistryAddress()).isEqualTo("ghcr.io");
        assertThat(authConfig.getUsername()).isEqualTo("u");
        assertThat(authConfig.getPassword()).isEqualTo("p");
    }

    @Test
    void testTheContainerCarriesTheResumeLabels() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT));

        assertThat(testDaemon.labels)
            .containsEntry("io.bytechef.managed-by", "bytechef")
            .containsEntry("io.bytechef.task-execution-id", "test-key");
    }

    @Test
    void testFindReturnsARunningContainerWithTheLabel() {
        TestDaemon testDaemon = new TestDaemon();

        Container container = mock(Container.class);

        when(container.getId()).thenReturn("c-9");
        when(container.getState()).thenReturn("running");

        testDaemon.existingContainers = List.of(container);

        testDaemon.newTaskRunner();

        Optional<RemoteJobHandle> handle = new DockerTaskRunnerBackend((host, timeout) -> testDaemon.dockerClient)
            .find(new RemoteJobKey("7", null), context(Map.of(), null));

        assertThat(handle).map(RemoteJobHandle::id)
            .contains("c-9");

        ListContainersCmd listContainersCmd = testDaemon.dockerClient.listContainersCmd();

        verify(listContainersCmd).withShowAll(true);
        verify(listContainersCmd).withLabelFilter(
            Map.of("io.bytechef.task-execution-id", "7", "io.bytechef.managed-by", "bytechef"));
    }

    @Test
    void testFindRemovesAContainerThatWasCreatedButNeverStarted() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.existingContainers = List.of(container("c-1", "created", Map.of()));

        testDaemon.newTaskRunner();

        Optional<RemoteJobHandle> handle = new DockerTaskRunnerBackend((host, timeout) -> testDaemon.dockerClient)
            .find(new RemoteJobKey("7", null), context(Map.of(), null));

        assertThat(handle).isEmpty();
        assertThat(testDaemon.removedContainerIds).containsExactly("c-1");
    }

    @Test
    void testFindPrefersARunningContainerOverAnExitedOne() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.existingContainers = List.of(
            container("c-exited", "exited", Map.of()), container("c-running", "running", Map.of()));

        testDaemon.newTaskRunner();

        Optional<RemoteJobHandle> handle = new DockerTaskRunnerBackend((host, timeout) -> testDaemon.dockerClient)
            .find(new RemoteJobKey("7", null), context(Map.of(), null));

        assertThat(handle).map(RemoteJobHandle::id)
            .contains("c-running");
    }

    @Test
    void testFindIgnoresADeadContainer() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.existingContainers = List.of(container("c-dead", "dead", Map.of()));

        testDaemon.newTaskRunner();

        Optional<RemoteJobHandle> handle = new DockerTaskRunnerBackend((host, timeout) -> testDaemon.dockerClient)
            .find(new RemoteJobKey("7", null), context(Map.of(), null));

        assertThat(handle).isEmpty();
        assertThat(testDaemon.removedContainerIds).isEmpty();
    }

    @Test
    void testFindCarriesTheKillGracePeriodLabelOntoTheHandle() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.existingContainers = List.of(
            container("c-9", "running", Map.of("io.bytechef.kill-grace-period", "PT7S")));

        testDaemon.newTaskRunner();

        Optional<RemoteJobHandle> handle = new DockerTaskRunnerBackend((host, timeout) -> testDaemon.dockerClient)
            .find(new RemoteJobKey("7", null), context(Map.of(), null));

        assertThat(handle).map(RemoteJobHandle::attributes)
            .contains(Map.of("killGracePeriod", "PT7S"));
    }

    @Test
    void testATimedOutContainerIsGivenItsKillGracePeriodBeforeItIsRemoved() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.containerExits = false;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        TaskRunnerRequest request = scriptRequest(
            SCRIPT, Duration.ofMillis(200), Map.of("image", IMAGE, "killGracePeriod", "PT5S"), Map.of());

        assertThatThrownBy(() -> taskRunner.run(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("timed out");

        verify(testDaemon.dockerClient).stopContainerCmd(CONTAINER_ID);
        verify(testDaemon.stopContainerCmd).withTimeout(5);

        assertThat(testDaemon.labels).containsEntry("io.bytechef.kill-grace-period", "PT5S");
        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testATimedOutContainerWithoutAKillGracePeriodIsForcedAtOnce() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.containerExits = false;

        RemoteTaskRunner taskRunner = testDaemon.newTaskRunner();

        TaskRunnerRequest request = scriptRequest(SCRIPT, Duration.ofMillis(200));

        assertThatThrownBy(() -> taskRunner.run(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("timed out");

        verify(testDaemon.dockerClient, never()).stopContainerCmd(anyString());

        assertThat(testDaemon.labels).doesNotContainKey("io.bytechef.kill-grace-period");
        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testAFinishedContainerIsRemovedWithoutBeingStopped() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest(SCRIPT, TEST_TIMEOUT, Map.of("image", IMAGE, "killGracePeriod", "PT5S"), Map.of()));

        verify(testDaemon.dockerClient, never()).stopContainerCmd(anyString());

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testAKillGracePeriodExtendsTheCleanupClientsResponseTimeout() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner();

        List<Duration> responseTimeouts = new ArrayList<>();

        new DockerTaskRunnerBackend((host, timeout) -> {
            responseTimeouts.add(timeout);

            return testDaemon.dockerClient;
        }).cleanup(
            new RemoteJobHandle("c-9", Map.of("killGracePeriod", "PT60S")), CleanupMode.KILL_AND_DELETE,
            context(Map.of(), null));

        verify(testDaemon.stopContainerCmd).withTimeout(60);

        assertThat(responseTimeouts).singleElement()
            .satisfies(timeout -> assertThat(timeout).isGreaterThanOrEqualTo(Duration.ofSeconds(90)));
        assertThat(testDaemon.removedContainerIds).containsExactly("c-9");
    }

    @Test
    void testDetachLeavesTheContainerAlone() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner();

        new DockerTaskRunnerBackend((host, timeout) -> testDaemon.dockerClient)
            .cleanup(new RemoteJobHandle("c-9"), CleanupMode.DETACH, context(Map.of(), null));

        assertThat(testDaemon.removedContainerIds).isEmpty();
    }

    private static Container container(String id, String state, Map<String, String> labels) {
        Container container = mock(Container.class);

        when(container.getId()).thenReturn(id);
        when(container.getState()).thenReturn(state);
        when(container.getLabels()).thenReturn(labels);

        return container;
    }

    private static ApplicationProperties applicationProperties(Map<String, String> runnerProperties) {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(runnerProperties);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("docker", runner);

        script.setRunners(runners);

        return applicationProperties;
    }

    private static List<String> propertyNames(
        DockerTaskRunnerBackend backend, Map<String, String> operatorProperties) {

        return backend.getProperties(operatorProperties)
            .stream()
            .map(ValueProperty::getName)
            .toList();
    }

    private static RemoteJobContext context(Map<String, String> operatorProperties, @Nullable String taskHost) {
        return new RemoteJobContext(
            null, operatorProperties, taskHost, ParametersFactory.create(Map.of()), 512L * 1024 * 1024,
            Duration.ofHours(1));
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters) {
        return request("python", runnerParameters);
    }

    private static TaskRunnerRequest request(String languageId, Map<String, ?> runnerParameters) {
        return new TaskRunnerRequest(
            languageId, SCRIPT, List.of(), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), TEST_TIMEOUT,
            Map.of(), mock(ActionContext.class));
    }

    private static TaskRunnerRequest scriptRequest(String script) {
        return scriptRequest(script, TEST_TIMEOUT);
    }

    private static TaskRunnerRequest scriptRequest(String script, Duration timeout) {
        return scriptRequest(script, timeout, Map.of("image", IMAGE), Map.of());
    }

    private static TaskRunnerRequest scriptRequest(
        String script, Duration timeout, Map<String, ?> runnerParameters, Map<String, String> env) {

        return new TaskRunnerRequest(
            "python", script, List.of(), Map.of(), env, Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(runnerParameters), timeout, Map.of(), mock(ActionContext.class));
    }

    private static final class TestDaemon {

        private final DockerClient dockerClient = mock(DockerClient.class);
        private final CreateContainerCmd createContainerCmd = mock(CreateContainerCmd.class, RETURNS_SELF);
        private final CountDownLatch waitReached = new CountDownLatch(1);
        private final List<String> removedContainerIds = new ArrayList<>();
        private final List<String> hosts = new ArrayList<>();

        private int exitCode;
        private boolean containerExits = true;
        private boolean imagePresent = true;
        private boolean pullCompletes = true;
        private boolean pullFails;
        private boolean outputPresent = true;
        private boolean inputCopyFails;
        private boolean logAttachFails;
        private boolean outputCopyFails;
        private String stdout = "";
        private String stderr = "";

        private List<String> environment = List.of();
        private List<Container> existingContainers = List.of();
        private HostConfig hostConfig;
        private Map<String, String> labels = Map.of();
        private PullImageCmd pullImageCmd;
        private StopContainerCmd stopContainerCmd;
        private Map<String, String> runnerProperties = Map.of();

        private RemoteTaskRunner newTaskRunner() {
            stubInspectImage();
            stubPullImage();
            stubCreateContainer();
            stubStartAndCopyIn();
            stubLogs();
            stubWait();
            stubCopyOut();
            stubRemoveContainer();
            stubStopContainer();
            stubListContainers();

            return new RemoteTaskRunner(
                new DockerTaskRunnerBackend((host, timeout) -> {
                    hosts.add(host);

                    return dockerClient;
                }),
                applicationProperties(runnerProperties), () -> "test-key");
        }

        private void stubListContainers() {
            ListContainersCmd listContainersCmd = mock(ListContainersCmd.class, RETURNS_SELF);

            when(listContainersCmd.exec()).thenReturn(existingContainers);

            when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        }

        private void stubInspectImage() {
            InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class, RETURNS_SELF);

            if (imagePresent) {
                when(inspectImageCmd.exec()).thenReturn(mock(InspectImageResponse.class));
            } else {
                when(inspectImageCmd.exec()).thenThrow(new NotFoundException("no such image"));
            }

            when(dockerClient.inspectImageCmd(anyString())).thenReturn(inspectImageCmd);
        }

        private void stubPullImage() {
            pullImageCmd = mock(PullImageCmd.class, RETURNS_SELF);

            when(pullImageCmd.exec(any())).thenAnswer(invocation -> {
                ResultCallback<PullResponseItem> callback = invocation.getArgument(0);

                if (pullFails) {
                    callback.onError(new IllegalStateException("registry refused the pull"));
                } else if (pullCompletes) {
                    callback.onComplete();
                }

                return callback;
            });

            when(dockerClient.pullImageCmd(anyString())).thenReturn(pullImageCmd);
        }

        private void stubCreateContainer() {
            CreateContainerResponse createContainerResponse = new CreateContainerResponse();

            createContainerResponse.setId(CONTAINER_ID);

            when(createContainerCmd.exec()).thenReturn(createContainerResponse);

            when(createContainerCmd.withEnv(anyList())).thenAnswer(invocation -> {
                environment = invocation.getArgument(0);

                return createContainerCmd;
            });

            when(createContainerCmd.withLabels(any())).thenAnswer(invocation -> {
                labels = invocation.getArgument(0);

                return createContainerCmd;
            });

            when(createContainerCmd.withHostConfig(any())).thenAnswer(invocation -> {
                hostConfig = invocation.getArgument(0);

                return createContainerCmd;
            });

            when(dockerClient.createContainerCmd(anyString())).thenReturn(createContainerCmd);
        }

        private void stubStartAndCopyIn() {
            when(dockerClient.startContainerCmd(anyString())).thenReturn(mock(StartContainerCmd.class, RETURNS_SELF));

            CopyArchiveToContainerCmd copyArchiveToContainerCmd = mock(
                CopyArchiveToContainerCmd.class, RETURNS_SELF);

            if (inputCopyFails) {
                when(copyArchiveToContainerCmd.exec()).thenThrow(new IllegalStateException("daemon refused the copy"));
            }

            when(dockerClient.copyArchiveToContainerCmd(anyString())).thenReturn(copyArchiveToContainerCmd);
        }

        private void stubLogs() {
            LogContainerCmd logContainerCmd = mock(LogContainerCmd.class, RETURNS_SELF);

            if (logAttachFails) {
                when(logContainerCmd.exec(any()))
                    .thenThrow(new IllegalStateException("daemon refused the log stream"));

                when(dockerClient.logContainerCmd(anyString())).thenReturn(logContainerCmd);

                return;
            }

            when(logContainerCmd.exec(any())).thenAnswer(invocation -> {
                ResultCallback<Frame> callback = invocation.getArgument(0);

                if (!stdout.isEmpty()) {
                    callback.onNext(new Frame(StreamType.STDOUT, stdout.getBytes(StandardCharsets.UTF_8)));
                }

                if (!stderr.isEmpty()) {
                    callback.onNext(new Frame(StreamType.STDERR, stderr.getBytes(StandardCharsets.UTF_8)));
                }

                callback.onComplete();

                return callback;
            });

            when(dockerClient.logContainerCmd(anyString())).thenReturn(logContainerCmd);
        }

        private void stubWait() {
            WaitContainerCmd waitContainerCmd = mock(WaitContainerCmd.class, RETURNS_SELF);

            when(waitContainerCmd.exec(any())).thenAnswer(invocation -> {
                ResultCallback<WaitResponse> callback = invocation.getArgument(0);

                waitReached.countDown();

                if (containerExits) {
                    WaitResponse waitResponse = mock(WaitResponse.class);

                    when(waitResponse.getStatusCode()).thenReturn(exitCode);

                    callback.onNext(waitResponse);
                    callback.onComplete();
                }

                return callback;
            });

            when(dockerClient.waitContainerCmd(anyString())).thenReturn(waitContainerCmd);
        }

        private void stubCopyOut() {
            CopyArchiveFromContainerCmd copyArchiveFromContainerCmd = mock(
                CopyArchiveFromContainerCmd.class, RETURNS_SELF);

            if (outputCopyFails) {
                when(copyArchiveFromContainerCmd.exec()).thenThrow(new IllegalStateException("daemon went away"));
            } else if (outputPresent) {
                when(copyArchiveFromContainerCmd.exec()).thenAnswer(invocation -> outputArchive());
            } else {
                when(copyArchiveFromContainerCmd.exec()).thenThrow(new NotFoundException("no such directory"));
            }

            when(dockerClient.copyArchiveFromContainerCmd(anyString(), anyString()))
                .thenReturn(copyArchiveFromContainerCmd);
        }

        private void stubRemoveContainer() {
            when(dockerClient.removeContainerCmd(anyString())).thenAnswer(invocation -> {
                String containerId = invocation.getArgument(0);

                RemoveContainerCmd removeContainerCmd = mock(RemoveContainerCmd.class, RETURNS_SELF);

                when(removeContainerCmd.exec()).thenAnswer(execInvocation -> {
                    removedContainerIds.add(containerId);

                    return null;
                });

                return removeContainerCmd;
            });
        }

        private void stubStopContainer() {
            stopContainerCmd = mock(StopContainerCmd.class, RETURNS_SELF);

            when(dockerClient.stopContainerCmd(anyString())).thenReturn(stopContainerCmd);
        }

        private InputStream outputArchive() throws IOException {
            Path path = Files.createTempDirectory("bytechef-docker-test-");

            try {
                Path outputPath = Files.createDirectory(path.resolve("output"));

                Files.writeString(outputPath.resolve("output.json"), "{\"ok\": true}", StandardCharsets.UTF_8);

                try (InputStream inputStream = ContainerArchive.toTar(path)) {
                    return new ByteArrayInputStream(inputStream.readAllBytes());
                }
            } finally {
                deleteRecursively(path);
            }
        }

        private static void deleteRecursively(Path path) throws IOException {
            try (Stream<Path> paths = Files.walk(path)) {
                List<Path> candidates = paths.sorted(Comparator.reverseOrder())
                    .toList();

                for (Path candidate : candidates) {
                    Files.deleteIfExists(candidate);
                }
            }
        }
    }
}
