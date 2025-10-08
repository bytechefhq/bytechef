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
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.exception.ClusterElementDefinitionErrorType;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.TokenRefreshHelper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ClusterElementDefinitionFacadeImpl implements ClusterElementDefinitionFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;
    private final TokenRefreshHelper tokenRefreshHelper;

    @SuppressFBWarnings("EI")
    public ClusterElementDefinitionFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService,
        ContextFactory contextFactory, TokenRefreshHelper tokenRefreshHelper) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
        this.tokenRefreshHelper = tokenRefreshHelper;
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ClusterElementContext clusterElementContext = contextFactory.createClusterElementContext(
            componentName, componentVersion, clusterElementName, componentConnection, true);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, clusterElementContext,
            ClusterElementDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES,
            (componentConnection1, clusterElementContext1) -> clusterElementDefinitionService.executeDynamicProperties(
                componentName, componentVersion, clusterElementName, propertyName, inputParameters,
                lookupDependsOnPaths, componentConnection1, clusterElementContext1),
            componentConnection1 -> contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, componentConnection1, true));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ClusterElementContext clusterElementContext = contextFactory.createClusterElementContext(
            componentName, componentVersion, clusterElementName, componentConnection, true);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, clusterElementContext,
            ClusterElementDefinitionErrorType.EXECUTE_OPTIONS,
            (componentConnection1, clusterElementContext1) -> clusterElementDefinitionService.executeOptions(
                componentName, componentVersion, clusterElementName, propertyName, inputParameters,
                lookupDependsOnPaths, searchText, componentConnection1, clusterElementContext1),
            componentConnection1 -> contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, componentConnection1, true));
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String clusterElementName, int statusCode, Object body) {

        return clusterElementDefinitionService.executeProcessErrorResponse(
            componentName, componentVersion, clusterElementName, statusCode, body,
            contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, null, false));
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable Long connectionId) {

        return executeTool(
            componentName, componentVersion, clusterElementName, inputParameters,
            getComponentConnection(connectionId), false);
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        ClusterElementContext clusterElementContext = contextFactory.createClusterElementContext(
            componentName, componentVersion, clusterElementName, componentConnection, editorEnvironment);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, clusterElementContext,
            ClusterElementDefinitionErrorType.EXECUTE_PERFORM,
            (componentConnection1, clusterElementContext1) -> clusterElementDefinitionService.executeTool(
                componentName, componentVersion, clusterElementName, inputParameters, componentConnection1,
                clusterElementContext1),
            componentConnection1 -> contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, componentConnection1, editorEnvironment));
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters) {

        return clusterElementDefinitionService.executeWorkflowNodeDescription(
            componentName, componentVersion, clusterElementName, inputParameters,
            contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, null, true));
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
