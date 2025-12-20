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

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
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

    @SuppressFBWarnings("EI")
    public ClusterElementDefinitionFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectionService = connectionService;
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
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return clusterElementDefinitionService.executeOptions(
            componentName, componentVersion, clusterElementName, propertyName, inputParameters,
            lookupDependsOnPaths, searchText, componentConnection);
    }

    @Override
    public Object executeTool(
        String componentName, String clusterElementName, Map<String, ?> inputParameters, @Nullable Long connectionId) {

        return clusterElementDefinitionService.executeTool(
            componentName, clusterElementName, inputParameters, getComponentConnection(connectionId), false);
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
