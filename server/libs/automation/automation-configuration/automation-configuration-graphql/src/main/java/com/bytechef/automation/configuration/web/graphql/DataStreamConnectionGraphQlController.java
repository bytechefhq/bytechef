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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.web.graphql.dto.DataStreamCompatibleConnectionDTO;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class DataStreamConnectionGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(DataStreamConnectionGraphQlController.class);

    private final ComponentDefinitionService componentDefinitionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public DataStreamConnectionGraphQlController(
        ComponentDefinitionService componentDefinitionService, WorkspaceConnectionFacade workspaceConnectionFacade) {

        this.componentDefinitionService = componentDefinitionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @QueryMapping
    public List<DataStreamCompatibleConnectionDTO> dataStreamCompatibleConnections(
        @Argument Long workspaceId, @Argument Long environmentId) {

        List<ConnectionDTO> connections = workspaceConnectionFacade.getConnections(
            workspaceId, null, null, environmentId, null);

        List<DataStreamCompatibleConnectionDTO> compatible = new ArrayList<>();

        for (ConnectionDTO connection : connections) {
            String componentName = connection.componentName();

            try {
                int componentVersion = resolveAuthoritativeComponentVersion(
                    componentName, connection.connectionVersion());

                if (!hasItemReaderClusterElement(componentName, componentVersion)) {
                    continue;
                }

                compatible.add(new DataStreamCompatibleConnectionDTO(
                    connection.id(), connection.name(), componentName, componentVersion));
            } catch (IllegalArgumentException e) {
                log.warn(
                    "Skipping connection id={} name='{}' componentName='{}' connectionVersion={}: {}",
                    connection.id(), connection.name(), componentName, connection.connectionVersion(), e.getMessage());
            }
        }

        return compatible;
    }

    private int resolveAuthoritativeComponentVersion(String componentName, int fallbackConnectionVersion) {
        ComponentDefinition componentDefinition = componentDefinitionService.getConnectionComponentDefinition(
            componentName, fallbackConnectionVersion);

        return componentDefinition.getVersion();
    }

    private boolean hasItemReaderClusterElement(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);

        for (ClusterElementDefinition clusterElement : componentDefinition.getClusterElements()) {
            ClusterElementType type = clusterElement.getType();

            if (ItemReader.SOURCE.equals(type)) {
                return true;
            }
        }

        return false;
    }
}
