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

import static com.bytechef.component.definition.ComponentDsl.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ValueProperty;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class TaskRunnerPropertyFactoryTest {

    private static final Set<TaskRunnerCapability> INLINE_SCRIPT = Set.of(TaskRunnerCapability.INLINE_SCRIPT);

    @Test
    public void testStampsDisplayConditionOnRunnerProperties() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry(newTaskRunner("graalvm", "GraalVM", "mode"));

        ModifiableObjectProperty objectProperty = TaskRunnerPropertyFactory.taskRunnerProperty(
            taskRunnerRegistry, INLINE_SCRIPT);

        List<? extends ValueProperty<?>> properties = objectProperty.getProperties().orElseThrow();

        ValueProperty<?> modeProperty = properties.stream()
            .filter(property -> "mode".equals(property.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(modeProperty.getDisplayCondition())
            .contains("taskRunner.type == 'graalvm'");
    }

    @Test
    public void testAssemblesMultipleRunnersInRegistryOrderEachWithItsOwnDisplayCondition() {
        TaskRunner dockerTaskRunner = newTaskRunner("docker", "Docker", "image");
        TaskRunner graalVmTaskRunner = newTaskRunner("graalvm", "GraalVM", "mode");

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(dockerTaskRunner, graalVmTaskRunner);

        ModifiableObjectProperty objectProperty = TaskRunnerPropertyFactory.taskRunnerProperty(
            taskRunnerRegistry, INLINE_SCRIPT);

        List<? extends ValueProperty<?>> properties = objectProperty.getProperties().orElseThrow();

        Property.StringProperty typeProperty = (Property.StringProperty) properties.get(0);

        assertThat(typeProperty.getOptions().orElseThrow())
            .extracting(Option::getValue)
            .containsExactly("docker", "graalvm");

        assertThat(properties)
            .extracting(ValueProperty::getName)
            .containsExactly("type", "image", "mode");

        ValueProperty<?> imageProperty = properties.get(1);
        ValueProperty<?> modeProperty = properties.get(2);

        assertThat(imageProperty.getDisplayCondition()).contains("taskRunner.type == 'docker'");
        assertThat(modeProperty.getDisplayCondition()).contains("taskRunner.type == 'graalvm'");
    }

    @Test
    public void testRejectsARunnerReturningCachedProperties() {
        TaskRunner cachingTaskRunner = mock(TaskRunner.class);

        List<ModifiableStringProperty> cachedProperties = List.of(string("mode").label("Mode"));

        when(cachingTaskRunner.getType()).thenReturn("caching");
        when(cachingTaskRunner.getTitle()).thenReturn("Caching");
        when(cachingTaskRunner.getCapabilities()).thenReturn(INLINE_SCRIPT);
        when(cachingTaskRunner.getProperties()).thenAnswer(invocation -> cachedProperties);

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(cachingTaskRunner);

        assertThatThrownBy(() -> TaskRunnerPropertyFactory.taskRunnerProperty(taskRunnerRegistry, INLINE_SCRIPT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("caching")
            .hasMessageContaining("freshly constructed");
    }

    @Test
    public void testEmptyRegistryProducesAnOptionalSelectWithNoOptionsAndNoDefault() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry();

        Property.StringProperty typeProperty = getTypeProperty(taskRunnerRegistry);

        assertThat(typeProperty.getRequired()).isFalse();
        assertThat(typeProperty.getOptions().orElseThrow()).isEmpty();
        assertThat(typeProperty.getDefaultValue()).isEmpty();
    }

    @Test
    public void testNonEmptyRegistryProducesARequiredSelect() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry(newTaskRunner("graalvm", "GraalVM", "mode"));

        Property.StringProperty typeProperty = getTypeProperty(taskRunnerRegistry);

        assertThat(typeProperty.getRequired()).isTrue();
    }

    @Test
    public void testDefaultsToGraalVmWhenGraalVmIsAmongTheEnabledRunnersButNotFirst() {
        TaskRunner dockerTaskRunner = newTaskRunner("docker", "Docker", "image");
        TaskRunner graalVmTaskRunner = newTaskRunner("graalvm", "GraalVM", "mode");

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(dockerTaskRunner, graalVmTaskRunner);

        Property.StringProperty typeProperty = getTypeProperty(taskRunnerRegistry);

        assertThat(typeProperty.getDefaultValue()).contains("graalvm");
    }

    @Test
    public void testDefaultsToTheFirstEnabledRunnerWhenGraalVmIsNotEnabled() {
        TaskRunner dockerTaskRunner = newTaskRunner("docker", "Docker", "image");
        TaskRunner processTaskRunner = newTaskRunner("process", "Process", "commands");

        TaskRunnerRegistry taskRunnerRegistry = newRegistry(dockerTaskRunner, processTaskRunner);

        Property.StringProperty typeProperty = getTypeProperty(taskRunnerRegistry);

        assertThat(typeProperty.getDefaultValue()).contains("docker");
    }

    private static Property.StringProperty getTypeProperty(TaskRunnerRegistry taskRunnerRegistry) {
        ModifiableObjectProperty objectProperty = TaskRunnerPropertyFactory.taskRunnerProperty(
            taskRunnerRegistry, INLINE_SCRIPT);

        return (Property.StringProperty) objectProperty.getProperties().orElseThrow()
            .getFirst();
    }

    private static TaskRunner newTaskRunner(String type, String title, String propertyName) {
        TaskRunner taskRunner = mock(TaskRunner.class);

        when(taskRunner.getType()).thenReturn(type);
        when(taskRunner.getTitle()).thenReturn(title);
        when(taskRunner.getCapabilities()).thenReturn(INLINE_SCRIPT);
        when(taskRunner.getProperties())
            .thenAnswer(invocation -> List.of(string(propertyName).label(propertyName)));

        return taskRunner;
    }

    private static TaskRunnerRegistry newRegistry(TaskRunner... taskRunners) {
        TaskRunnerRegistry taskRunnerRegistry = mock(TaskRunnerRegistry.class);

        when(taskRunnerRegistry.getTaskRunners(any()))
            .thenAnswer(invocation -> {
                Set<TaskRunnerCapability> requiredCapabilities = invocation.getArgument(0);

                return List.of(taskRunners)
                    .stream()
                    .filter(taskRunner -> taskRunner.getCapabilities()
                        .containsAll(requiredCapabilities))
                    .toList();
            });

        return taskRunnerRegistry;
    }

    private static TaskRunner inProcessRunner() {
        return newCapableTaskRunner(
            "graalvm", Set.of(TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMPONENT_BRIDGE));
    }

    private static TaskRunner externalRunner() {
        return newCapableTaskRunner(
            "external",
            Set.of(
                TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMMANDS, TaskRunnerCapability.INPUT_FILES,
                TaskRunnerCapability.OUTPUT_FILES, TaskRunnerCapability.ENVIRONMENT));
    }

    private static TaskRunner environmentOnlyRunner() {
        return newCapableTaskRunner(
            "environment-only", Set.of(TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.ENVIRONMENT));
    }

    private static TaskRunner newCapableTaskRunner(String type, Set<TaskRunnerCapability> capabilities) {
        TaskRunner taskRunner = mock(TaskRunner.class);

        when(taskRunner.getType()).thenReturn(type);
        when(taskRunner.getCapabilities()).thenReturn(capabilities);

        return taskRunner;
    }

    @Test
    void testExternalPropertiesAreOmittedWhenNoRunnerDeclaresTheCapability() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry(inProcessRunner());

        List<ModifiableValueProperty<?, ?>> properties =
            TaskRunnerPropertyFactory.externalProperties(taskRunnerRegistry);

        assertThat(properties).isEmpty();
    }

    @Test
    void testExternalPropertiesAppearWhenARunnerDeclaresTheCapability() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry(externalRunner());

        List<String> names = TaskRunnerPropertyFactory.externalProperties(taskRunnerRegistry)
            .stream()
            .map(property -> property.getName())
            .toList();

        assertThat(names).containsExactly("env", "inputFiles", "outputFiles", "timeout");
    }

    @Test
    void testEachExternalPropertyIsConditionedOnTheRunnersThatSupportIt() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry(externalRunner());

        ModifiableValueProperty<?, ?> envProperty = TaskRunnerPropertyFactory.externalProperties(taskRunnerRegistry)
            .getFirst();

        assertThat(envProperty.getDisplayCondition())
            .hasValueSatisfying(condition -> assertThat(condition).contains("taskRunner.type == 'external'"));
    }

    @Test
    void testEnvIsGatedOnEnvironmentSpecificallyNotOnInputFilesOrOutputFiles() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry(environmentOnlyRunner());

        List<String> names = TaskRunnerPropertyFactory.externalProperties(taskRunnerRegistry)
            .stream()
            .map(property -> property.getName())
            .toList();

        assertThat(names).containsExactly("env", "timeout");
    }

    @Test
    void testTimeoutIsConditionedOnTheExternalRunnerAndNotOnGraalVm() {
        TaskRunnerRegistry taskRunnerRegistry = newRegistry(inProcessRunner(), externalRunner());

        List<ModifiableValueProperty<?, ?>> properties =
            TaskRunnerPropertyFactory.externalProperties(taskRunnerRegistry);

        ModifiableValueProperty<?, ?> timeoutProperty = properties.stream()
            .filter(property -> "timeout".equals(property.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(timeoutProperty.getDisplayCondition())
            .hasValueSatisfying(condition -> {
                assertThat(condition).contains("taskRunner.type == 'external'");
                assertThat(condition).doesNotContain("taskRunner.type == 'graalvm'");
            });
    }
}
