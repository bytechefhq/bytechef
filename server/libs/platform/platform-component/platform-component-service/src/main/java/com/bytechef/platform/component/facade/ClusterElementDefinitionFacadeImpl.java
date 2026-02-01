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

package com.bytechef.platform.component.facade;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementContext.ClusterElementFunction;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.datastream.ClusterElementResolverFunction;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ClusterElementDefinitionFacadeImpl implements ClusterElementDefinitionFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;

    @SuppressFBWarnings("EI")
    public ClusterElementDefinitionFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService,
        ContextFactory contextFactory) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return clusterElementDefinitionService.executeDynamicProperties(
            componentName, componentVersion, clusterElementName, propertyName, inputParameters,
            lookupDependsOnPaths, componentConnection);
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, Map<String, ?> extensions, List<String> lookupDependsOnPaths,
        String searchText, Long connectionId, Map<String, Long> clusterElementConnectionIds,
        Map<String, Map<String, ?>> clusterElementInputParameters) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        Map<String, ComponentConnection> clusterElementConnections = clusterElementConnectionIds.entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .collect(
                java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, entry -> getComponentConnection(entry.getValue())));

        ClusterElementResolverFunction clusterElementResolver = createClusterElementResolver(
            extensions, clusterElementConnections, clusterElementInputParameters);

        return clusterElementDefinitionService.executeOptions(
            componentName, componentVersion, clusterElementName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText, componentConnection, clusterElementResolver);
    }

    @Override
    public Object executeTool(
        String componentName, String clusterElementName, Map<String, ?> inputParameters, @Nullable Long connectionId) {

        return clusterElementDefinitionService.executeTool(
            componentName, clusterElementName, inputParameters, getComponentConnection(connectionId), false);
    }

    private ClusterElementResolverFunction createClusterElementResolver(
        Map<String, ?> extensions, Map<String, ComponentConnection> clusterElementConnections,
        Map<String, Map<String, ?>> clusterElementInputParameters) {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        return new ClusterElementResolverFunction() {
            @Override
            public <T> T resolve(
                ClusterElementType clusterElementType, ClusterElementFunction<T> clusterElementFunction) {

                ClusterElement clusterElement = clusterElementMap.fetchClusterElement(clusterElementType)
                    .orElse(null);

                if (clusterElement == null) {
                    return null;
                }

                Object clusterElementObject = clusterElementDefinitionService.getClusterElement(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName());

                ComponentConnection clusterElementConnection = clusterElementConnections.get(
                    clusterElement.getWorkflowNodeName());

                ClusterElementContext context = contextFactory.createClusterElementContext(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName(), clusterElementConnection, true);

                Map<String, ?> inputParameters = clusterElementInputParameters.getOrDefault(
                    clusterElement.getWorkflowNodeName(), Map.of());

                return clusterElementFunction.apply(
                    clusterElementObject, ParametersFactory.create(inputParameters),
                    ParametersFactory.create(
                        clusterElementConnection == null ? Map.of() : clusterElementConnection.getParameters()),
                    context);
            }
        };
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());
        }

        return componentConnection;
    }
}
