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

package com.bytechef.platform.component.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.config.ApplicationProperties;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class TaskRunnerRegistryTest {

    private ListAppender<ILoggingEvent> logAppender;

    @SuppressWarnings("PMD")
    private ch.qos.logback.classic.Logger taskRunnerRegistryLogger;

    @BeforeEach
    public void setUp() {
        taskRunnerRegistryLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(TaskRunnerRegistryImpl.class);
        logAppender = new ListAppender<>();

        logAppender.start();
        taskRunnerRegistryLogger.addAppender(logAppender);
    }

    @AfterEach
    public void tearDown() {
        taskRunnerRegistryLogger.detachAppender(logAppender);
    }

    @Test
    public void testReturnsEnabledRunner() {
        TaskRunner taskRunner = newTaskRunner("graalvm", Set.of(TaskRunnerCapability.INLINE_SCRIPT));

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(List.of(taskRunner), Map.of("graalvm", true));

        assertThat(taskRunnerRegistry.getTaskRunner("graalvm")).isSameAs(taskRunner);
    }

    @Test
    public void testThrowsForDisabledRunner() {
        TaskRunner taskRunner = newTaskRunner("process", Set.of(TaskRunnerCapability.COMMANDS));

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(List.of(taskRunner), Map.of("process", false));

        assertThatThrownBy(() -> taskRunnerRegistry.getTaskRunner("process"))
            .isInstanceOf(TaskRunnerNotEnabledException.class)
            .hasMessageContaining("bytechef.script.runners.process.enabled=true");
    }

    @Test
    public void testThrowsForRunnerAbsentFromConfiguration() {
        TaskRunner taskRunner = newTaskRunner("docker", Set.of(TaskRunnerCapability.COMMANDS));

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(List.of(taskRunner), Map.of());

        assertThatThrownBy(() -> taskRunnerRegistry.getTaskRunner("docker"))
            .isInstanceOf(TaskRunnerNotEnabledException.class);
    }

    @Test
    public void testThrowsForUnregisteredRunner() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry(List.of(), Map.of("kubernetes", true));

        assertThatThrownBy(() -> taskRunnerRegistry.getTaskRunner("kubernetes"))
            .isInstanceOf(TaskRunnerNotEnabledException.class);
    }

    @Test
    public void testFiltersByCapabilityAndOrdersByType() {
        TaskRunner dockerTaskRunner = newTaskRunner("docker", Set.of(TaskRunnerCapability.COMMANDS));
        TaskRunner graalVmTaskRunner = newTaskRunner("graalvm", Set.of(TaskRunnerCapability.INLINE_SCRIPT));
        TaskRunner processTaskRunner = newTaskRunner("process", Set.of(TaskRunnerCapability.COMMANDS));

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(
            List.of(processTaskRunner, graalVmTaskRunner, dockerTaskRunner),
            Map.of("docker", true, "graalvm", true, "process", true));

        List<TaskRunner> taskRunners = taskRunnerRegistry.getTaskRunners(Set.of(TaskRunnerCapability.COMMANDS));

        assertThat(taskRunners).containsExactly(dockerTaskRunner, processTaskRunner);
    }

    @Test
    public void testExcludesDisabledRunnersFromCapabilityLookup() {
        TaskRunner dockerTaskRunner = newTaskRunner("docker", Set.of(TaskRunnerCapability.COMMANDS));
        TaskRunner processTaskRunner = newTaskRunner("process", Set.of(TaskRunnerCapability.COMMANDS));

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(
            List.of(dockerTaskRunner, processTaskRunner), Map.of("process", true));

        assertThat(taskRunnerRegistry.getTaskRunners(Set.of(TaskRunnerCapability.COMMANDS)))
            .containsExactly(processTaskRunner);
    }

    @Test
    public void testLogEnabledRunnersWarnsForTrustedGraalvm() {
        TaskRunner taskRunner = newTaskRunner("graalvm", Set.of(TaskRunnerCapability.INLINE_SCRIPT));

        TaskRunnerRegistryImpl taskRunnerRegistry = newRegistryImpl(
            List.of(taskRunner), true, Map.of("trusted-enabled", "true"));

        taskRunnerRegistry.logEnabledRunners();

        assertThat(logAppender.list).hasSize(1);

        ILoggingEvent event = logAppender.list.get(0);

        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
            .contains("Trusted GraalVM")
            .contains("full")
            .contains("host access");
    }

    @Test
    public void testLogEnabledRunnersSilentForUntrustedGraalvm() {
        TaskRunner taskRunner = newTaskRunner("graalvm", Set.of(TaskRunnerCapability.INLINE_SCRIPT));

        TaskRunnerRegistryImpl taskRunnerRegistry = newRegistryImpl(List.of(taskRunner), true, Map.of());

        taskRunnerRegistry.logEnabledRunners();

        assertThat(logAppender.list).isEmpty();
    }

    @Test
    public void testLogEnabledRunnersWarnsForEnabledNonGraalvmRunner() {
        TaskRunner taskRunner = newTaskRunner("process", Set.of(TaskRunnerCapability.COMMANDS));

        TaskRunnerRegistryImpl taskRunnerRegistry = newRegistryImpl(List.of(taskRunner), true, Map.of());

        taskRunnerRegistry.logEnabledRunners();

        assertThat(logAppender.list).hasSize(1);

        ILoggingEvent event = logAppender.list.get(0);

        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
            .contains("process")
            .contains("sandbox");
    }

    @Test
    public void testLogEnabledRunnersSilentForDisabledRunner() {
        TaskRunner taskRunner = newTaskRunner("docker", Set.of(TaskRunnerCapability.COMMANDS));

        TaskRunnerRegistryImpl taskRunnerRegistry = newRegistryImpl(List.of(taskRunner), false, Map.of());

        taskRunnerRegistry.logEnabledRunners();

        assertThat(logAppender.list).isEmpty();
    }

    private static TaskRunnerRegistryImpl newRegistryImpl(
        List<TaskRunner> taskRunners, boolean enabled, Map<String, String> properties) {

        ApplicationProperties applicationProperties = new ApplicationProperties();

        for (TaskRunner taskRunner : taskRunners) {
            ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

            runner.setEnabled(enabled);
            runner.setProperties(properties);

            applicationProperties.getScript()
                .getRunners()
                .put(taskRunner.getType(), runner);
        }

        return new TaskRunnerRegistryImpl(taskRunners, applicationProperties);
    }

    private static TaskRunner newTaskRunner(String type, Set<TaskRunnerCapability> capabilities) {
        TaskRunner taskRunner = mock(TaskRunner.class);

        when(taskRunner.getType()).thenReturn(type);
        when(taskRunner.getCapabilities()).thenReturn(capabilities);

        return taskRunner;
    }

    private static TaskRunnerRegistry newRegistry(List<TaskRunner> taskRunners, Map<String, Boolean> enabledByType) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        for (Map.Entry<String, Boolean> entry : enabledByType.entrySet()) {
            ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

            runner.setEnabled(entry.getValue());

            applicationProperties.getScript()
                .getRunners()
                .put(entry.getKey(), runner);
        }

        return new TaskRunnerRegistryImpl(taskRunners, applicationProperties);
    }
}
