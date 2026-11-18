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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressFBWarnings({
    "DMI_HARDCODED_ABSOLUTE_FILENAME", "ENV_USE_PROPERTY_INSTEAD_OF_ENV"
})
class ProcessTaskRunnerTest {

    private static final Duration PROCESS_DEATH_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration DRAIN_CEILING = Duration.ofSeconds(20);

    private static final Pattern WORKING_DIRECTORY_PATTERN = Pattern.compile("\\S*bytechef-run-\\S+");

    private static final Set<String> NON_DISCRIMINATING_NAMES = Set.of(
        "HOME", "IFS", "OPTIND", "PATH", "PPID", "PS1", "PS2", "PS4", "PWD", "OLDPWD", "SHLVL", "_");

    private final ProcessTaskRunner processTaskRunner = new ProcessTaskRunner(new ApplicationProperties());

    @Test
    void testGetTypeIsProcess() {
        assertThat(processTaskRunner.getType()).isEqualTo("process");
    }

    @Test
    void testShellCommandsRunAndCaptureStdout() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerResult result = processTaskRunner.run(commandsRequest(List.of("echo hello-from-shell")));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("hello-from-shell");
    }

    @Test
    void testNonZeroExitFailsWithTheStreamTailsInTheMessage() {
        assumeTrue(isExecutable("/bin/sh"));

        assertThatThrownBy(
            () -> processTaskRunner.run(
                commandsRequest(List.of("echo out-before-failure", "echo boom 1>&2", "exit 3"))))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("code 3")
                    .hasMessageContaining("out-before-failure")
                    .hasMessageContaining("boom");
    }

    @Test
    void testLargeStdoutDoesNotDeadlock() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerResult result = processTaskRunner.run(
            commandsRequest(List
                .of("i=0; while [ $i -lt 4000 ]; do echo 0123456789012345678901234567890123456789; i=$((i+1)); done")));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isNotEmpty();
    }

    @Test
    void testHostEnvironmentIsNotInherited() {
        assumeTrue(isExecutable("/bin/sh"));

        Optional<String> clearedName = findClearedHostVariableName();

        assumeTrue(clearedName.isPresent());

        String name = clearedName.get();
        String hostValue = System.getenv(name);

        TaskRunnerResult result = processTaskRunner.run(commandsRequest(List.of("echo \"[$" + name + "]\"")));

        assertThat(result.stdout()).contains("[]");
        assertThat(result.stdout()).doesNotContain(hostValue);
    }

    @Test
    void testDeclaredEnvironmentReachesTheProcess() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null, List.of("echo \"[$GREETING]\""), Map.of(), Map.of("GREETING", "hi"), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Duration.ofSeconds(30), Map.of(),
            mock(ActionContext.class));

        assertThat(processTaskRunner.run(request)
            .stdout()).contains("[hi]");
    }

    @Test
    void testDeclaredEnvironmentCannotOverrideTheContractVariables() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null, List.of("echo '{\"ok\": true}' > \"$BYTECHEF_OUTPUT_DIR/output.json\""), Map.of(),
            Map.of("BYTECHEF_OUTPUT_DIR", "/nonexistent-bytechef-output-dir"), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Duration.ofSeconds(30), Map.of(),
            mock(ActionContext.class));

        TaskRunnerResult result = processTaskRunner.run(request);

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isEqualTo(Map.of("ok", true));
    }

    @Test
    void testStandardInputIsClosedSoAReadSeesEndOfInput() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null, List.of("read answer", "echo \"[read-returned]\""), Map.of(), Map.of(), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Duration.ofSeconds(10), Map.of(),
            mock(ActionContext.class));

        TaskRunnerResult result = processTaskRunner.run(request);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("[read-returned]");
    }

    @Test
    void testTimeoutFailsWithATimedOutMessage() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null, List.of("sleep 30"), Map.of(), Map.of(), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Duration.ofSeconds(2), Map.of(),
            mock(ActionContext.class));

        assertThatThrownBy(() -> processTaskRunner.run(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("timed out");
    }

    @Test
    void testTimeoutDestroysTheProcessTree() throws InterruptedException {
        assumeTrue(isExecutable("/bin/sh"));

        String marker = uniqueSleepSeconds();

        TaskRunnerRequest request = sleepRequest(marker, Duration.ofSeconds(2));

        assertThatThrownBy(() -> processTaskRunner.run(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("timed out");

        awaitNoMarkerProcesses(marker);
    }

    @Test
    void testInterruptionDestroysTheProcessTree() throws InterruptedException {
        assumeTrue(isExecutable("/bin/sh"));

        String marker = uniqueSleepSeconds();

        TaskRunnerRequest request = sleepRequest(marker, Duration.ofMinutes(5));

        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        Thread runnerThread = new Thread(() -> {
            try {
                processTaskRunner.run(request);
            } catch (RuntimeException exception) {
                failure.set(exception);
            }
        });

        runnerThread.start();

        awaitMarkerProcesses(marker);

        runnerThread.interrupt();
        runnerThread.join(PROCESS_DEATH_TIMEOUT.toMillis());

        awaitNoMarkerProcesses(marker);

        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testAnExitedChildWhoseDescendantHoldsTheStreamsDoesNotHangForever() throws InterruptedException {
        assumeTrue(isExecutable("/bin/sh"));

        String marker = uniqueSleepSeconds();

        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null, List.of("sleep " + marker + " &", "echo still-captured"), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Duration.ofSeconds(2),
            Map.of(), mock(ActionContext.class));

        try {
            long startNanos = System.nanoTime();

            TaskRunnerResult result = processTaskRunner.run(request);

            Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

            assertThat(elapsed).isLessThan(DRAIN_CEILING);
            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout()).contains("still-captured");
        } finally {
            destroyMarkerProcesses(marker);

            awaitNoMarkerProcesses(marker);
        }
    }

    @Test
    void testTheDrainWaitIsBoundedWhenAStreamNeverReachesEndOfFile() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch drainedStarted = new CountDownLatch(1);

        Thread neverDrained = Thread.ofVirtual()
            .start(() -> awaitQuietly(release));

        Thread drained = Thread.ofVirtual()
            .start(drainedStarted::countDown);

        try {
            long startNanos = System.nanoTime();

            boolean bothDrained = ProcessTaskRunner.joinDrainThreads(
                neverDrained, drained, Duration.ofMillis(200));

            Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

            assertThat(bothDrained).isFalse();
            assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
            assertThat(drainedStarted.getCount()).isZero();
        } finally {
            release.countDown();
        }
    }

    @Test
    void testTheDrainBudgetIsWhatIsLeftOfTheTimeoutButNeverLessThanTheGrace() {
        Duration full = ProcessTaskRunner.drainBudget(Duration.ofSeconds(30), System.nanoTime());

        assertThat(full).isBetween(Duration.ofSeconds(25), Duration.ofSeconds(30));

        Duration exhausted = ProcessTaskRunner.drainBudget(
            Duration.ofSeconds(2), System.nanoTime() - Duration.ofSeconds(10)
                .toNanos());

        assertThat(exhausted).isPositive();
        assertThat(exhausted).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void testTheWorkingDirectoryIsRemovedAfterASuccessfulRun() throws IOException {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerResult result = processTaskRunner.run(commandsRequest(List.of("echo \"$BYTECHEF_WORKING_DIR\"")));

        assertWorkingDirectoryRemoved(result.stdout());
    }

    @Test
    void testTheWorkingDirectoryIsRemovedAfterANonZeroExit() throws IOException {
        assumeTrue(isExecutable("/bin/sh"));

        assertWorkingDirectoryRemoved(
            failureMessage(() -> processTaskRunner.run(
                commandsRequest(List.of("echo \"$BYTECHEF_WORKING_DIR\"", "exit 3")))));
    }

    @Test
    void testTheWorkingDirectoryIsRemovedAfterATimeout() throws IOException {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null, List.of("echo \"$BYTECHEF_WORKING_DIR\"", "sleep 30"), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Duration.ofSeconds(2),
            Map.of(), mock(ActionContext.class));

        assertWorkingDirectoryRemoved(failureMessage(() -> processTaskRunner.run(request)));
    }

    @Test
    void testTheJavaScriptBootstrapCarriesThePerformReturnValueToTheOutput() {
        assumeTrue(isOnPath("node"));

        TaskRunnerResult result = processTaskRunner.run(
            scriptRequest(
                "javascript", "function perform(input, context) { return {doubled: input.value * 2}; }",
                Map.of("value", 21)));

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isEqualTo(Map.of("doubled", 42));
    }

    @Test
    void testTheTruffleJavaScriptIdReachesTheSameBootstrap() {
        assumeTrue(isOnPath("node"));

        TaskRunnerResult result = processTaskRunner.run(
            scriptRequest(
                "js", "function perform(input, context) { return {doubled: input.value * 2}; }",
                Map.of("value", 21)));

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isEqualTo(Map.of("doubled", 42));
    }

    @Test
    void testTheJavaScriptBootstrapRaisesAFriendlyErrorForTheComponentBridge() {
        assumeTrue(isOnPath("node"));

        assertThatThrownBy(
            () -> processTaskRunner.run(
                scriptRequest(
                    "javascript", "function perform(input, context) { return context.component.example(); }",
                    Map.of())))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("context.component is not available under the process runner");
    }

    @Test
    void testThePythonBootstrapCarriesThePerformReturnValueToTheOutput() {
        assumeTrue(isOnPath("python3"));

        TaskRunnerResult result = processTaskRunner.run(
            scriptRequest(
                "python", "def perform(input, context):\n    return {\"doubled\": input[\"value\"] * 2}\n",
                Map.of("value", 21)));

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isEqualTo(Map.of("doubled", 42));
    }

    @Test
    void testThePythonBootstrapRaisesAFriendlyErrorForTheComponentBridge() {
        assumeTrue(isOnPath("python3"));

        assertThatThrownBy(
            () -> processTaskRunner.run(
                scriptRequest(
                    "python", "def perform(input, context):\n    return context.component.example()\n", Map.of())))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("context.component is not available under the process runner");
    }

    @Test
    void testAClearedEnvironmentPointsHomeAtTheWorkingDirectory() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerResult result = processTaskRunner.run(commandsRequest(List.of("echo \"[$HOME]\"")));

        assertThat(result.stdout()).contains("bytechef-run-");
    }

    @Test
    void testAnInheritedEnvironmentKeepsTheServerAccountsHome() {
        assumeTrue(isExecutable("/bin/sh"));

        String hostHome = System.getenv("HOME");

        assumeTrue(hostHome != null && !hostHome.isBlank());

        ProcessTaskRunner enabledTaskRunner = new ProcessTaskRunner(
            applicationProperties(Map.of("inherit-environment-enabled", "true")));

        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null, List.of("echo \"[$HOME]\""), Map.of(), Map.of(), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of("inheritEnvironment", true)),
            Duration.ofSeconds(30), Map.of(), mock(ActionContext.class));

        assertThat(enabledTaskRunner.run(request)
            .stdout()).contains("[" + hostHome + "]");
    }

    @Test
    void testValidateRejectsAnEmptyInterpreterOverride() {
        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null, List.of("echo hi"), Map.of(), Map.of(), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of("interpreter", "   ")),
            Duration.ofSeconds(5), Map.of(), mock(ActionContext.class));

        assertThatThrownBy(() -> processTaskRunner.validate(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidateRejectsALanguageTheBootstrapCannotExecute() {
        TaskRunnerRequest request = new TaskRunnerRequest(
            "ruby", "puts 1", List.of(), Map.of(), Map.of(), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of("interpreter", "ruby")),
            Duration.ofSeconds(5), Map.of(), mock(ActionContext.class));

        assertThatThrownBy(() -> processTaskRunner.validate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ruby");
    }

    @Test
    void testValidateRejectsInheritEnvironmentWhenTheOperatorHasNotEnabledIt() {
        assertThatThrownBy(() -> processTaskRunner.validate(inheritEnvironmentRequest()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.process.properties.inherit-environment-enabled");
    }

    @Test
    void testValidateAcceptsInheritEnvironmentWhenTheOperatorEnabledIt() {
        ProcessTaskRunner enabledTaskRunner = new ProcessTaskRunner(
            applicationProperties(Map.of("inherit-environment-enabled", "true")));

        enabledTaskRunner.validate(inheritEnvironmentRequest());
    }

    @Test
    void testInheritEnvironmentIsHiddenWhenTheOperatorHasNotEnabledIt() {
        assertThat(processTaskRunner.getProperties())
            .noneMatch(property -> "inheritEnvironment".equals(property.getName()));
    }

    @Test
    void testInheritEnvironmentIsOfferedWhenTheOperatorEnabledIt() {
        ProcessTaskRunner enabledTaskRunner = new ProcessTaskRunner(
            applicationProperties(Map.of("inherit-environment-enabled", "true")));

        assertThat(enabledTaskRunner.getProperties())
            .anyMatch(property -> "inheritEnvironment".equals(property.getName()));
    }

    @Test
    void testCapabilitiesExcludeTheComponentBridge() {
        assertThat(processTaskRunner.getCapabilities())
            .doesNotContain(com.bytechef.platform.component.runner.TaskRunnerCapability.COMPONENT_BRIDGE);
    }

    @Test
    void testGetPropertiesReturnsFreshInstances() {
        assertThat(processTaskRunner.getProperties()
            .getFirst())
                .isNotSameAs(
                    processTaskRunner.getProperties()
                        .getFirst());
    }

    private static ApplicationProperties applicationProperties(Map<String, String> runnerProperties) {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(runnerProperties);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("process", runner);

        script.setRunners(runners);

        return applicationProperties;
    }

    private static void awaitMarkerProcesses(String marker) throws InterruptedException {
        await(() -> countMarkerProcesses(marker) > 0);

        assumeTrue(countMarkerProcesses(marker) > 0, "process command lines are not observable on this host");
    }

    private static void awaitNoMarkerProcesses(String marker) throws InterruptedException {
        await(() -> countMarkerProcesses(marker) == 0);

        assertThat(countMarkerProcesses(marker)).isZero();
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + PROCESS_DEATH_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline && !condition.getAsBoolean()) {
            Thread.sleep(50);
        }
    }

    private static TaskRunnerRequest commandsRequest(List<String> commands) {
        return new TaskRunnerRequest(
            "shell", null, commands, Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Duration.ofSeconds(30), Map.of(), mock(ActionContext.class));
    }

    private static long countMarkerProcesses(String marker) {
        return markerProcessHandles(marker).count();
    }

    private static Optional<String> findClearedHostVariableName() {
        Map<String, String> hostEnvironment = System.getenv();

        return hostEnvironment.entrySet()
            .stream()
            .filter(entry -> {
                String name = entry.getKey();

                return !NON_DISCRIMINATING_NAMES.contains(name) && !name.startsWith("BYTECHEF_") &&
                    isShellIdentifier(name);
            })
            .filter(entry -> {
                String value = entry.getValue();

                return value != null && !value.isBlank();
            })
            .map(Map.Entry::getKey)
            .sorted()
            .findFirst();
    }

    private static TaskRunnerRequest inheritEnvironmentRequest() {
        return new TaskRunnerRequest(
            "shell", null, List.of("echo hi"), Map.of(), Map.of(), Map.of(), List.of(),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of("inheritEnvironment", true)),
            Duration.ofSeconds(5), Map.of(), mock(ActionContext.class));
    }

    private static boolean isExecutable(String path) {
        return Files.isExecutable(Path.of(path));
    }

    private static boolean isShellIdentifier(String name) {
        if (name.isEmpty()) {
            return false;
        }

        char first = name.charAt(0);

        if (first != '_' && !Character.isLetter(first)) {
            return false;
        }

        return name.chars()
            .allMatch(character -> character == '_' || Character.isLetterOrDigit(character));
    }

    private static TaskRunnerRequest sleepRequest(String marker, Duration timeout) {
        return new TaskRunnerRequest(
            "shell", null, List.of("sleep " + marker + " &", "sleep " + marker), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), timeout, Map.of(),
            mock(ActionContext.class));
    }

    private static String uniqueSleepSeconds() {
        long uniqueSuffix = System.nanoTime() % 1_000_000L;

        return String.valueOf(9_000_000L + uniqueSuffix);
    }

    private static void assertWorkingDirectoryRemoved(String text) throws IOException {
        Matcher matcher = WORKING_DIRECTORY_PATTERN.matcher(text);

        assertThat(matcher.find()).isTrue();

        Path workingDirectoryPath = Path.of(matcher.group());

        assertThat(workingDirectoryPath).doesNotExist();
        assertThat(TaskRunnerWorkingDirectoryTest.bytechefRunDirectories()).doesNotContain(workingDirectoryPath);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();
        }
    }

    private static void destroyMarkerProcesses(String marker) {
        markerProcessHandles(marker).forEach(ProcessHandle::destroyForcibly);
    }

    private static String failureMessage(Runnable runnable) {
        Throwable throwable = catchThrowable(runnable::run);

        assertThat(throwable).isNotNull();

        return String.valueOf(throwable.getMessage());
    }

    private static boolean isOnPath(String command) {
        String path = System.getenv("PATH");

        if (path == null) {
            return false;
        }

        return Stream.of(path.split(File.pathSeparator))
            .filter(entry -> !entry.isBlank())
            .map(entry -> Path.of(entry)
                .resolve(command))
            .anyMatch(Files::isExecutable);
    }

    private static Stream<ProcessHandle> markerProcessHandles(String marker) {
        return ProcessHandle.allProcesses()
            .filter(processHandle -> {
                ProcessHandle.Info info = processHandle.info();

                Optional<String> commandLine = info.commandLine();

                return commandLine.map(line -> line.contains(marker))
                    .orElse(false);
            });
    }

    private static TaskRunnerRequest scriptRequest(String languageId, String script, Map<String, ?> input) {
        return new TaskRunnerRequest(
            languageId, script, List.of(), input, Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Duration.ofSeconds(30), Map.of(), mock(ActionContext.class));
    }
}
