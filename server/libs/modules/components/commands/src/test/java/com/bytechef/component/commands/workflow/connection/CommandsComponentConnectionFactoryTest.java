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

package com.bytechef.component.commands.workflow.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.runner.TaskRunner;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerNotEnabledException;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CommandsComponentConnectionFactoryTest {

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final TaskRunnerRegistry taskRunnerRegistry = mock(TaskRunnerRegistry.class);
    private final CommandsComponentConnectionFactory commandsComponentConnectionFactory =
        new CommandsComponentConnectionFactory(componentDefinitionService, taskRunnerRegistry);

    @Test
    void testResolveCommandsComponent() {
        Optional<ComponentConnectionFactory> componentConnectionFactory =
            commandsComponentConnectionFactory.resolve(componentDefinition("commands"));

        assertThat(componentConnectionFactory).contains(commandsComponentConnectionFactory);
    }

    @Test
    void testResolveOtherComponent() {
        Optional<ComponentConnectionFactory> componentConnectionFactory =
            commandsComponentConnectionFactory.resolve(componentDefinition("script"));

        assertThat(componentConnectionFactory).isEmpty();
    }

    @Test
    void testCreateReturnsTaskDeclaredConnections() {
        ComponentDefinition httpClientComponentDefinition = componentDefinition("httpClient");

        when(httpClientComponentDefinition.isConnectionRequired()).thenReturn(false);
        when(componentDefinitionService.getComponentDefinition("httpClient", 1))
            .thenReturn(httpClientComponentDefinition);

        Map<String, ?> extensions = Map.of(
            "connections", Map.of("echo", Map.of("componentName", "httpClient", "componentVersion", 1)));

        List<ComponentConnection> componentConnections = commandsComponentConnectionFactory.create(
            "commands_1", Map.of(), extensions, componentDefinition("commands"));

        assertThat(componentConnections).containsExactly(
            ComponentConnection.of("commands_1", "echo", "httpClient", 1, false));
    }

    @Test
    void testCreateAddsTheConnectionOfTheSelectedTaskRunner() {
        registerTaskRunner("kubernetes", new TaskRunnerConnection("kubernetes", 1));

        List<ComponentConnection> componentConnections = commandsComponentConnectionFactory.create(
            "commands_1", taskRunnerParameters("kubernetes"), Map.of(), componentDefinition("commands"));

        assertThat(componentConnections).containsExactly(
            ComponentConnection.of("commands_1", "taskRunner", "kubernetes", 1, true));
    }

    @Test
    void testCreateAddsNoConnectionForATaskRunnerWithoutOne() {
        registerTaskRunner("process", null);

        assertThat(
            commandsComponentConnectionFactory.create(
                "commands_1", taskRunnerParameters("process"), Map.of(), componentDefinition("commands")))
                    .isEmpty();
    }

    @Test
    void testCreateAddsNoConnectionForADisabledTaskRunner() {
        when(taskRunnerRegistry.getTaskRunner("kubernetes")).thenThrow(new TaskRunnerNotEnabledException("kubernetes"));

        assertThat(
            commandsComponentConnectionFactory.create(
                "commands_1", taskRunnerParameters("kubernetes"), Map.of(), componentDefinition("commands")))
                    .isEmpty();
    }

    @Test
    void testCreateIgnoresATaskRunnerEntryStoredInTheDefinition() {
        registerTaskRunner("process", null);

        Map<String, ?> extensions = Map.of(
            "connections",
            Map.of(
                "taskRunner",
                Map.of("componentName", "kubernetes", "componentVersion", 1, "authorizationRequired", true)));

        assertThat(
            commandsComponentConnectionFactory.create(
                "commands_1", taskRunnerParameters("process"), extensions, componentDefinition("commands")))
                    .isEmpty();
    }

    private void registerTaskRunner(String type, TaskRunnerConnection taskRunnerConnection) {
        TaskRunner taskRunner = mock(TaskRunner.class);

        when(taskRunner.getConnection()).thenReturn(Optional.ofNullable(taskRunnerConnection));
        when(taskRunnerRegistry.getTaskRunner(type)).thenReturn(taskRunner);
    }

    private static Map<String, ?> taskRunnerParameters(String type) {
        return Map.of("taskRunner", Map.of("type", type));
    }

    private static ComponentDefinition componentDefinition(String name) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn(name);

        return componentDefinition;
    }
}
