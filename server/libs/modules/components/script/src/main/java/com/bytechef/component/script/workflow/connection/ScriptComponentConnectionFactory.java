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

package com.bytechef.component.script.workflow.connection;

import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;

import com.bytechef.platform.component.definition.ScriptComponentDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerConnectionUtils;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactory;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Strings;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
class ScriptComponentConnectionFactory
    implements ComponentConnectionFactory, ComponentConnectionFactoryResolver {

    private final ComponentDefinitionService componentDefinitionService;
    private final TaskRunnerRegistry taskRunnerRegistry;

    public ScriptComponentConnectionFactory(
        ComponentDefinitionService componentDefinitionService, TaskRunnerRegistry taskRunnerRegistry) {

        this.componentDefinitionService = componentDefinitionService;
        this.taskRunnerRegistry = taskRunnerRegistry;
    }

    @Override
    public List<ComponentConnection> create(
        String workflowNodeName, Map<String, ?> parameters, Map<String, ?> extensions,
        ComponentDefinition componentDefinition) {

        List<ComponentConnection> declaredComponentConnections = ComponentConnection.of(
            extensions, workflowNodeName,
            (name, version) -> {
                ComponentDefinition curComponentDefinition = componentDefinitionService.getComponentDefinition(
                    name, version);

                return curComponentDefinition.isConnectionRequired();
            });

        List<ComponentConnection> componentConnections = new ArrayList<>();

        for (ComponentConnection declaredComponentConnection : declaredComponentConnections) {
            if (!TASK_RUNNER.equals(declaredComponentConnection.key())) {
                componentConnections.add(declaredComponentConnection);
            }
        }

        Optional<TaskRunnerConnection> taskRunnerConnection = TaskRunnerConnectionUtils.fetchConnection(
            parameters, taskRunnerRegistry);

        if (taskRunnerConnection.isPresent()) {
            TaskRunnerConnection connection = taskRunnerConnection.get();

            componentConnections.add(
                ComponentConnection.of(
                    workflowNodeName, TASK_RUNNER, connection.componentName(), connection.componentVersion(), true));
        }

        return componentConnections;
    }

    @Override
    public Optional<ComponentConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        return Optional.ofNullable(
            Strings.CS.startsWith(componentDefinition.getName(), ScriptComponentDefinition.SCRIPT) ? this : null);
    }
}
