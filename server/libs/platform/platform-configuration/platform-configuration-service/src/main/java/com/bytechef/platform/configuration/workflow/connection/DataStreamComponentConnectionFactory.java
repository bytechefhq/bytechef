/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.workflow.connection;

import com.bytechef.platform.component.definition.DataStreamComponentDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.DataStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(1)
public class DataStreamComponentConnectionFactory
    implements ComponentConnectionFactory, ComponentConnectionFactoryResolver {

    private final ComponentDefinitionService componentDefinitionService;

    public DataStreamComponentConnectionFactory(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<ComponentConnection> create(
        String workflowNodeName, Map<String, ?> extensions, ComponentDefinition componentDefinition) {

        return getWorkflowConnections(DataStream.of(extensions), workflowNodeName);
    }

    @Override
    public Optional<ComponentConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        return Optional.ofNullable(
            StringUtils.startsWith(componentDefinition.getName(), DataStreamComponentDefinition.DATA_STREAM)
                ? this
                : null);
    }

    private List<ComponentConnection> getWorkflowConnections(DataStream dataStream, String workflowNodeName) {
        List<ComponentConnection> componentConnections = new ArrayList<>();

        if (dataStream != null) {
            if (dataStream.source() != null) {
                ComponentConnection componentConnection = getWorkflowConnection(
                    workflowNodeName,
                    StringUtils.lowerCase(DataStreamComponentDefinition.ComponentType.SOURCE.name()),
                    dataStream.source());

                if (componentConnection != null) {
                    componentConnections.add(componentConnection);
                }
            }

            if (dataStream.destination() != null) {
                ComponentConnection componentConnection = getWorkflowConnection(
                    workflowNodeName,
                    StringUtils.lowerCase(DataStreamComponentDefinition.ComponentType.DESTINATION.name()),
                    dataStream.destination());

                if (componentConnection != null) {
                    componentConnections.add(componentConnection);
                }
            }
        }

        return componentConnections;
    }

    private ComponentConnection getWorkflowConnection(
        String workflowNodeName, String workflowConnectionKey, DataStream.DataStreamComponent dataStreamComponent) {

        ComponentConnection componentConnection = null;

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            dataStreamComponent.componentName(), dataStreamComponent.componentVersion());

        if (componentDefinition.getConnection() != null) {
            componentConnection =
                ComponentConnection.of(workflowNodeName, workflowConnectionKey, componentDefinition.getName(),
                    componentDefinition.getVersion(), componentDefinition.isConnectionRequired());
        }

        return componentConnection;
    }
}
