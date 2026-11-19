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

import static com.bytechef.component.commands.constant.CommandsConstants.COMMANDS;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactory;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Resolves the connections a Commands task declares in its workflow definition, the same way Script tasks do.
 *
 * @author Ivica Cardic
 */
@Component
@Order(2)
class CommandsComponentConnectionFactory implements ComponentConnectionFactory, ComponentConnectionFactoryResolver {

    private final ComponentDefinitionService componentDefinitionService;

    CommandsComponentConnectionFactory(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<ComponentConnection> create(
        String workflowNodeName, Map<String, ?> extensions, ComponentDefinition componentDefinition) {

        return ComponentConnection.of(
            extensions, workflowNodeName,
            (name, version) -> {
                ComponentDefinition connectionComponentDefinition =
                    componentDefinitionService.getComponentDefinition(name, version);

                return connectionComponentDefinition.isConnectionRequired();
            });
    }

    @Override
    public Optional<ComponentConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        return Optional.ofNullable(Objects.equals(componentDefinition.getName(), COMMANDS) ? this : null);
    }
}
