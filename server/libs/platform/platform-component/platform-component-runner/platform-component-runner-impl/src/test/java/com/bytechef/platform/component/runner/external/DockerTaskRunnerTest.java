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
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property.StringProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerCapability;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DockerTaskRunnerTest {

    private static final String ALLOWED_IMAGE = "python:3.12-slim";
    private static final String CONTAINER_ID = "container-42";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);

    private final DockerTaskRunner dockerTaskRunner = new DockerTaskRunner(
        applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE)));

    @Test
    void testGetTypeIsDocker() {
        assertThat(dockerTaskRunner.getType()).isEqualTo("docker");
    }

    @Test
    void testGetTitleIsDocker() {
        assertThat(dockerTaskRunner.getTitle()).isEqualTo("Docker");
    }

    @Test
    void testCapabilitiesCoverEverythingButTheComponentBridge() {
        Set<TaskRunnerCapability> capabilities = dockerTaskRunner.getCapabilities();

        assertThat(capabilities).containsExactlyInAnyOrder(
            TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMMANDS, TaskRunnerCapability.INPUT_FILES,
            TaskRunnerCapability.OUTPUT_FILES, TaskRunnerCapability.ENVIRONMENT);
    }

    @Test
    void testGetPropertiesOffersEveryDockerSetting() {
        assertThat(propertyNames(dockerTaskRunner)).containsExactlyInAnyOrder(
            "image", "pullPolicy", "entrypoint", "user", "cpu", "memory", "networkMode", "extraHosts");
    }

    @Test
    void testTheImagePropertyIsOfferedEvenWhenTheAllowlistIsEmpty() {
        DockerTaskRunner emptyAllowlistTaskRunner = new DockerTaskRunner(applicationProperties(Map.of()));

        assertThat(propertyNames(emptyAllowlistTaskRunner)).contains("image");
    }

    @Test
    void testTheImagePropertyIsOptionalBecauseTheLanguageSuppliesADefault() {
        ValueProperty<?> imageProperty = dockerTaskRunner.getProperties()
            .stream()
            .filter(property -> "image".equals(property.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(imageProperty.getRequired()).isFalse();
    }

    @Test
    void testTheImagePropertyOffersTheAllowedImagesAsOptions() {
        DockerTaskRunner twoImageTaskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE + ", node:22-alpine")));

        assertThat(optionValues(twoImageTaskRunner, "image")).containsExactly(ALLOWED_IMAGE, "node:22-alpine");
    }

    @Test
    void testTheNetworkModeSelectHidesHostUnlessTheOperatorAllowedIt() {
        assertThat(optionValues(dockerTaskRunner, "networkMode")).doesNotContain("host");
    }

    @Test
    void testTheNetworkModeSelectOffersHostWhenTheOperatorAllowedIt() {
        DockerTaskRunner hostNetworkTaskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "allow-host-network", "true")));

        assertThat(optionValues(hostNetworkTaskRunner, "networkMode")).contains("host");
    }

    @Test
    void testGetPropertiesReturnsFreshInstances() {
        assertThat(dockerTaskRunner.getProperties()
            .getFirst())
                .isNotSameAs(
                    dockerTaskRunner.getProperties()
                        .getFirst());
    }

    @Test
    void testValidateAcceptsAnAllowedImage() {
        dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE)));
    }

    @Test
    void testValidateRejectsAnImageOutsideTheAllowlist() {
        assertThatThrownBy(() -> dockerTaskRunner.validate(request(Map.of("image", "ubuntu:24.04"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ubuntu:24.04")
            .hasMessageContaining("bytechef.script.runners.docker.properties.allowed-images");
    }

    @Test
    void testValidateRejectsAPrefixOfAnAllowedImage() {
        assertThatThrownBy(() -> dockerTaskRunner.validate(request(Map.of("image", "python:3.12"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.allowed-images");
    }

    @Test
    void testAnEmptyAllowlistPermitsNothing() {
        DockerTaskRunner emptyAllowlistTaskRunner = new DockerTaskRunner(applicationProperties(Map.of()));

        assertThatThrownBy(() -> emptyAllowlistTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.allowed-images");
    }

    @Test
    void testAMissingImageFallsBackToTheLanguagesDefaultImage() {
        dockerTaskRunner.validate(request(Map.of()));
        dockerTaskRunner.validate(request(Map.of("image", "   ")));
    }

    @Test
    void testEachLanguageHasItsOwnDefaultImage() {
        Map<String, String> defaultImagesByLanguageId = Map.of(
            "shell", "ubuntu:24.04", "python", "python:3.12-slim", "javascript", "node:22-alpine", "js",
            "node:22-alpine");

        for (Map.Entry<String, String> entry : defaultImagesByLanguageId.entrySet()) {
            DockerTaskRunner taskRunner = new DockerTaskRunner(
                applicationProperties(Map.of("allowed-images", entry.getValue())));

            taskRunner.validate(request(entry.getKey(), Map.of()));
        }
    }

    @Test
    void testADefaultedImageStillHasToBeOnTheAllowlist() {
        DockerTaskRunner otherImageTaskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", "ubuntu:24.04")));

        assertThatThrownBy(() -> otherImageTaskRunner.validate(request(Map.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(ALLOWED_IMAGE)
            .hasMessageContaining("bytechef.script.runners.docker.properties.allowed-images");
    }

    @Test
    void testAnEmptyAllowlistPermitsNotEvenTheDefaultImage() {
        DockerTaskRunner emptyAllowlistTaskRunner = new DockerTaskRunner(applicationProperties(Map.of()));

        assertThatThrownBy(() -> emptyAllowlistTaskRunner.validate(request(Map.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.allowed-images");
    }

    @Test
    void testADefaultedImageIsTheImageTheDaemonIsHanded() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", TEST_TIMEOUT, Map.of(), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.dockerClient).createContainerCmd(ALLOWED_IMAGE);
    }

    @Test
    void testValidateRejectsHostNetworkingUnlessTheOperatorEnabledIt() {
        assertThatThrownBy(
            () -> dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "host"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytechef.script.runners.docker.properties.allow-host-network");
    }

    @Test
    void testValidateAcceptsHostNetworkingWhenTheOperatorEnabledIt() {
        DockerTaskRunner hostNetworkTaskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "allow-host-network", "true")));

        hostNetworkTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "host")));
    }

    @Test
    void testValidateAcceptsANetworkModeThatIsNotHost() {
        dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "none")));
        dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "bridge")));
    }

    @Test
    void testValidateRejectsANamedDockerNetwork() {
        assertThatThrownBy(
            () -> dockerTaskRunner.validate(
                request(Map.of("image", ALLOWED_IMAGE, "networkMode", "bytechef_default"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bytechef_default")
                    .hasMessageContaining("bridge")
                    .hasMessageContaining("none");
    }

    @Test
    void testValidateRejectsJoiningAnotherContainersNetworkNamespace() {
        assertThatThrownBy(
            () -> dockerTaskRunner.validate(
                request(Map.of("image", ALLOWED_IMAGE, "networkMode", "container:bytechef"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("container:bytechef");
    }

    @Test
    void testValidateRejectsAContainerNamespaceThatCouldCarryHostNetworkingTransitively() {
        DockerTaskRunner hostNetworkTaskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "allow-host-network", "true")));

        for (DockerTaskRunner taskRunner : List.of(dockerTaskRunner, hostNetworkTaskRunner)) {
            assertThatThrownBy(
                () -> taskRunner.validate(
                    request(Map.of("image", ALLOWED_IMAGE, "networkMode", "container:9f3d163e"))))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("container:9f3d163e");
        }
    }

    @Test
    void testTheAcceptedNetworkModesAreExactlyTheOnesTheSelectOffers() {
        DockerTaskRunner hostNetworkTaskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "allow-host-network", "true")));

        assertThat(optionValues(dockerTaskRunner, "networkMode")).containsExactly("bridge", "none");
        assertThat(optionValues(hostNetworkTaskRunner, "networkMode")).containsExactly("bridge", "none", "host");

        for (DockerTaskRunner taskRunner : List.of(dockerTaskRunner, hostNetworkTaskRunner)) {
            for (String networkMode : optionValues(taskRunner, "networkMode")) {
                taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", networkMode)));
            }
        }

        assertThatThrownBy(
            () -> dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "host"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidateRejectsALanguageTheBootstrapCannotExecute() {
        TaskRunnerRequest request = new TaskRunnerRequest(
            "ruby", "puts 1", List.of(), Map.of(), Map.of(), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of("image", ALLOWED_IMAGE)),
            TEST_TIMEOUT, Map.of(), mock(ActionContext.class));

        assertThatThrownBy(() -> dockerTaskRunner.validate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ruby");
    }

    @Test
    void testValidateRejectsAnUnknownPullPolicy() {
        assertThatThrownBy(
            () -> dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "pullPolicy", "SOMETIMES"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SOMETIMES");
    }

    @Test
    void testValidateRejectsAMalformedMemoryLimit() {
        assertThatThrownBy(
            () -> dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "memory", "plenty"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plenty");
    }

    @Test
    void testValidateRejectsANonPositiveCpuLimit() {
        assertThatThrownBy(() -> dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "cpu", 0))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testASuccessfulRunReturnsTheOutputTheContainerWroteAndRemovesTheContainer() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.stdout = "hello-from-container";

        TaskRunnerResult result = testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

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

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest("def perform(input, context):\n    return 1\n")))
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

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", Duration.ofMillis(200));

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

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", Duration.ofMillis(200));

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

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        Thread runnerThread = new Thread(() -> {
            try {
                taskRunner.run(scriptRequest("def perform(input, context):\n    return 1\n", Duration.ofMinutes(5)));
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

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest("def perform(input, context):\n    return 1\n")))
            .isInstanceOf(RuntimeException.class);

        verify(testDaemon.dockerClient, never()).createContainerCmd(anyString());

        assertThat(testDaemon.removedContainerIds).isEmpty();
    }

    @Test
    void testAPullThatNeverCompletesFailsWithinTheTimeout() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = false;
        testDaemon.pullCompletes = false;

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", Duration.ofMillis(200));

        assertThatThrownBy(() -> taskRunner.run(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("timed out");

        verify(testDaemon.dockerClient, never()).createContainerCmd(anyString());
    }

    @Test
    void testTheContainerIsRemovedWhenCopyingTheWorkingDirectoryInFails() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.inputCopyFails = true;

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest("def perform(input, context):\n    return 1\n")))
            .isInstanceOf(RuntimeException.class);

        verify(testDaemon.dockerClient, never()).startContainerCmd(anyString());

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testTheContainerIsRemovedWhenTheLogStreamCannotBeAttached() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.logAttachFails = true;

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest("def perform(input, context):\n    return 1\n")))
            .isInstanceOf(RuntimeException.class);

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testTheContainerIsRemovedWhenCollectingTheOutputFails() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.outputCopyFails = true;

        DockerTaskRunner taskRunner = testDaemon.newTaskRunner();

        assertThatThrownBy(() -> taskRunner.run(scriptRequest("def perform(input, context):\n    return 1\n")))
            .isInstanceOf(RuntimeException.class);

        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testAMissingOutputDirectoryIsNotAFailure() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.outputPresent = false;

        TaskRunnerResult result = testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isNull();
        assertThat(testDaemon.removedContainerIds).containsExactly(CONTAINER_ID);
    }

    @Test
    void testTheContainerRunsTheLanguagesDefaultInterpreterOnTheSourceFile() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        verify(testDaemon.createContainerCmd).withEntrypoint(List.of("python3"));
        verify(testDaemon.createContainerCmd).withCmd("/tmp/script.py");
    }

    @Test
    void testAnEntrypointOverrideReplacesTheInterpreter() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", TEST_TIMEOUT,
            Map.of("image", ALLOWED_IMAGE, "entrypoint", List.of("python3", "-u")), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.createContainerCmd).withEntrypoint(List.of("python3", "-u"));
        verify(testDaemon.createContainerCmd).withCmd("/tmp/script.py");
    }

    @Test
    void testTheContractVariablesNameTheContainersOwnPaths() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        assertThat(testDaemon.environment).contains(
            "BYTECHEF_WORKING_DIR=/tmp", "BYTECHEF_INPUT_FILE=/tmp/input.json", "BYTECHEF_OUTPUT_DIR=/tmp/output");
    }

    @Test
    void testADeclaredEnvironmentEntryCannotOverrideTheContractVariables() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", TEST_TIMEOUT, Map.of("image", ALLOWED_IMAGE),
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
            "def perform(input, context):\n    return 1\n", TEST_TIMEOUT,
            Map.of("image", ALLOWED_IMAGE, "cpu", 1.5, "memory", "512m", "networkMode", "none"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        HostConfig hostConfig = testDaemon.hostConfig;

        assertThat(hostConfig.getNanoCPUs()).isEqualTo(1_500_000_000L);
        assertThat(hostConfig.getMemory()).isEqualTo(512L * 1024 * 1024);
        assertThat(hostConfig.getNetworkMode()).isEqualTo("none");
    }

    @Test
    void testAForkBombShapedRequestStillGetsABoundedContainer() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = commandsRequest(List.of(":(){ :|:& };:"), Map.of("image", ALLOWED_IMAGE));

        testDaemon.newTaskRunner()
            .run(request);

        HostConfig hostConfig = testDaemon.hostConfig;

        assertThat(hostConfig.getPidsLimit()).isEqualTo(512L);
        assertThat(hostConfig.getNanoCPUs()).isEqualTo(1_000_000_000L);
        assertThat(hostConfig.getMemory()).isEqualTo(512L * 1024 * 1024);
    }

    @Test
    void testThePidsLimitIsSetEvenWhenTheWorkflowNamedItsOwnLimits() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", TEST_TIMEOUT,
            Map.of("image", ALLOWED_IMAGE, "cpu", 1.5, "memory", "512m"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        assertThat(testDaemon.hostConfig.getPidsLimit()).isEqualTo(512L);
    }

    @Test
    void testTheOperatorsPidsLimitReachesTheHostConfig() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of("allowed-images", ALLOWED_IMAGE, "pids-limit", "64");

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        assertThat(testDaemon.hostConfig.getPidsLimit()).isEqualTo(64L);
    }

    @Test
    void testAnOperatorCannotAskForAnUnboundedPidsLimit() {
        DockerTaskRunner taskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "pids-limit", "-1")));

        assertThatThrownBy(() -> taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.pids-limit");
    }

    @Test
    void testAMalformedOperatorCeilingRefusesTheExecutionRatherThanBecomingUnlimited() {
        Map<String, String> malformedCeilings = Map.of(
            "max-memory", "plenty", "max-cpu", "lots", "pids-limit", "many");

        for (Map.Entry<String, String> entry : malformedCeilings.entrySet()) {
            DockerTaskRunner taskRunner = new DockerTaskRunner(
                applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, entry.getKey(), entry.getValue())));

            assertThatThrownBy(() -> taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytechef.script.runners.docker.properties." + entry.getKey());
        }
    }

    @Test
    void testTheOperatorsDefaultCpuAndMemoryReachAContainerThatAskedForNeither() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of(
            "allowed-images", ALLOWED_IMAGE, "default-cpu", "0.5", "default-memory", "256m");

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        HostConfig hostConfig = testDaemon.hostConfig;

        assertThat(hostConfig.getNanoCPUs()).isEqualTo(500_000_000L);
        assertThat(hostConfig.getMemory()).isEqualTo(256L * 1024 * 1024);
    }

    @Test
    void testACpuRequestAboveTheOperatorsMaximumIsRejected() {
        DockerTaskRunner taskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "max-cpu", "2")));

        assertThatThrownBy(() -> taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "cpu", 8))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-cpu");

        taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "cpu", 2)));
    }

    @Test
    void testAMemoryRequestAboveTheOperatorsMaximumIsRejected() {
        DockerTaskRunner taskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "max-memory", "1g")));

        assertThatThrownBy(() -> taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "memory", "2g"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-memory");

        taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "memory", "1g")));
    }

    @Test
    void testTheMaximumsApplyEvenWhenTheOperatorNamedNone() {
        assertThatThrownBy(() -> dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "cpu", 64))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-cpu");

        assertThatThrownBy(() -> dockerTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "memory", "64g"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-memory");
    }

    @Test
    void testAnOperatorDefaultAboveTheirOwnMaximumIsHeldToTheMaximum() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of(
            "allowed-images", ALLOWED_IMAGE, "default-cpu", "8", "max-cpu", "2", "default-memory", "8g", "max-memory",
            "1g");

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        HostConfig hostConfig = testDaemon.hostConfig;

        assertThat(hostConfig.getNanoCPUs()).isEqualTo(2_000_000_000L);
        assertThat(hostConfig.getMemory()).isEqualTo(1024L * 1024 * 1024);
    }

    @Test
    void testTheContainersHomeIsItsWorkingDirectory() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        assertThat(testDaemon.environment).contains("HOME=/tmp");
    }

    @Test
    void testADeclaredHomeStillWins() {
        TestDaemon testDaemon = new TestDaemon();

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", TEST_TIMEOUT, Map.of("image", ALLOWED_IMAGE),
            Map.of("HOME", "/home/somebody"));

        testDaemon.newTaskRunner()
            .run(request);

        assertThat(testDaemon.environment).contains("HOME=/home/somebody");
        assertThat(testDaemon.environment).doesNotContain("HOME=/tmp");
    }

    @Test
    void testAnUnsetNetworkModeStillResolvesToBridgeByDefault() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        assertThat(testDaemon.hostConfig.getNetworkMode()).isEqualTo("bridge");
    }

    @Test
    void testAnOperatorAllowingOnlyNoneGetsNoneForAWorkflowThatNamedNoMode() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.runnerProperties = Map.of(
            "allowed-images", ALLOWED_IMAGE, "allowed-network-modes", "none");

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        assertThat(testDaemon.hostConfig.getNetworkMode()).isEqualTo("none");
    }

    @Test
    void testAnOperatorAllowingOnlyNoneRejectsARequestForBridge() {
        DockerTaskRunner taskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "allowed-network-modes", "none")));

        assertThatThrownBy(
            () -> taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "bridge"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bridge");

        assertThat(optionValues(taskRunner, "networkMode")).containsExactly("none");

        taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "none")));
    }

    @Test
    void testHostStillNeedsItsOwnFlagWhateverTheModeAllowlistSays() {
        DockerTaskRunner listedButUnflaggedTaskRunner = new DockerTaskRunner(
            applicationProperties(
                Map.of("allowed-images", ALLOWED_IMAGE, "allowed-network-modes", "none,bridge,host")));

        assertThatThrownBy(
            () -> listedButUnflaggedTaskRunner.validate(
                request(Map.of("image", ALLOWED_IMAGE, "networkMode", "host"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bytechef.script.runners.docker.properties.allow-host-network");

        assertThat(optionValues(listedButUnflaggedTaskRunner, "networkMode")).doesNotContain("host");

        DockerTaskRunner flaggedTaskRunner = new DockerTaskRunner(
            applicationProperties(Map.of("allowed-images", ALLOWED_IMAGE, "allow-host-network", "true")));

        flaggedTaskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "host")));
    }

    @Test
    void testTheModeAllowlistCanTakeHostAwayFromAnOperatorWhoFlaggedIt() {
        DockerTaskRunner taskRunner = new DockerTaskRunner(
            applicationProperties(
                Map.of(
                    "allowed-images", ALLOWED_IMAGE, "allow-host-network", "true", "allowed-network-modes",
                    "none,bridge")));

        assertThat(optionValues(taskRunner, "networkMode")).containsExactly("bridge", "none");

        assertThatThrownBy(() -> taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE, "networkMode", "host"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("host");
    }

    @Test
    void testAModeAllowlistNamingNothingKnownRefusesEveryExecution() {
        DockerTaskRunner taskRunner = new DockerTaskRunner(
            applicationProperties(
                Map.of("allowed-images", ALLOWED_IMAGE, "allowed-network-modes", "bytechef_default")));

        assertThatThrownBy(() -> taskRunner.validate(request(Map.of("image", ALLOWED_IMAGE))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.allowed-network-modes");
    }

    @Test
    void testTheContainerCarriesNoBindMounts() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        HostConfig hostConfig = testDaemon.hostConfig;

        assertThat(hostConfig.getBinds()).isNullOrEmpty();
    }

    @Test
    void testThePullIsSkippedWhenTheImageIsAlreadyPresent() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = true;

        testDaemon.newTaskRunner()
            .run(scriptRequest("def perform(input, context):\n    return 1\n"));

        verify(testDaemon.dockerClient, never()).pullImageCmd(anyString());
    }

    @Test
    void testTheAlwaysPolicyPullsEvenWhenTheImageIsPresent() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = true;

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", TEST_TIMEOUT,
            Map.of("image", ALLOWED_IMAGE, "pullPolicy", "ALWAYS"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.dockerClient).pullImageCmd(ALLOWED_IMAGE);
    }

    @Test
    void testTheNeverPolicyDoesNotEvenAskWhetherTheImageIsPresent() {
        TestDaemon testDaemon = new TestDaemon();

        testDaemon.imagePresent = false;

        TaskRunnerRequest request = scriptRequest(
            "def perform(input, context):\n    return 1\n", TEST_TIMEOUT,
            Map.of("image", ALLOWED_IMAGE, "pullPolicy", "NEVER"), Map.of());

        testDaemon.newTaskRunner()
            .run(request);

        verify(testDaemon.dockerClient, never()).pullImageCmd(anyString());
        verify(testDaemon.dockerClient, never()).inspectImageCmd(anyString());
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

    private static List<String> optionValues(DockerTaskRunner taskRunner, String propertyName) {
        StringProperty stringProperty = taskRunner.getProperties()
            .stream()
            .filter(property -> propertyName.equals(property.getName()))
            .map(StringProperty.class::cast)
            .findFirst()
            .orElseThrow();

        return stringProperty.getOptions().orElseThrow()
            .stream()
            .map(Option::getValue)
            .toList();
    }

    private static List<String> propertyNames(DockerTaskRunner taskRunner) {
        return taskRunner.getProperties()
            .stream()
            .map(ValueProperty::getName)
            .toList();
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters) {
        return request("python", runnerParameters);
    }

    private static TaskRunnerRequest request(String languageId, Map<String, ?> runnerParameters) {
        return new TaskRunnerRequest(
            languageId, "def perform(input, context):\n    return 1\n", List.of(), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), TEST_TIMEOUT,
            Map.of(), mock(ActionContext.class));
    }

    private static TaskRunnerRequest commandsRequest(List<String> commands, Map<String, ?> runnerParameters) {
        return new TaskRunnerRequest(
            "shell", null, commands, Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(runnerParameters), TEST_TIMEOUT, Map.of(), mock(ActionContext.class));
    }

    private static TaskRunnerRequest scriptRequest(String script) {
        return scriptRequest(script, TEST_TIMEOUT);
    }

    private static TaskRunnerRequest scriptRequest(String script, Duration timeout) {
        return scriptRequest(script, timeout, Map.of("image", ALLOWED_IMAGE), Map.of());
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
        private HostConfig hostConfig;
        private Map<String, String> runnerProperties = Map.of("allowed-images", ALLOWED_IMAGE);

        private DockerTaskRunner newTaskRunner() {
            stubInspectImage();
            stubPullImage();
            stubCreateContainer();
            stubStartAndCopyIn();
            stubLogs();
            stubWait();
            stubCopyOut();
            stubRemoveContainer();

            return new DockerTaskRunner(applicationProperties(runnerProperties), (settings, timeout) -> dockerClient);
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
            PullImageCmd pullImageCmd = mock(PullImageCmd.class, RETURNS_SELF);

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
            RemoveContainerCmd removeContainerCmd = mock(RemoveContainerCmd.class, RETURNS_SELF);

            when(removeContainerCmd.exec()).thenAnswer(invocation -> {
                removedContainerIds.add(CONTAINER_ID);

                return null;
            });

            when(dockerClient.removeContainerCmd(anyString())).thenAnswer(invocation -> removeContainerCmd);
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
