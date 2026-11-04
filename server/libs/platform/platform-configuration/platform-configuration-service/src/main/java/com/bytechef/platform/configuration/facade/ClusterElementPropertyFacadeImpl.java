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

package com.bytechef.platform.configuration.facade;

import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ClusterElementPropertyFacadeImpl implements ClusterElementPropertyFacade {

    private static final String CONNECTION_ACCESS =
        "#connectionId == null or isTenantAdmin() or (hasPermission(#connectionId, 'Connection', 'WORKFLOW_VIEW') " +
            "and @mcpComponentConnectionSecurity.isMcpComponentConnection(#mcpComponentId, #connectionId))";

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @SuppressFBWarnings("EI")
    public ClusterElementPropertyFacadeImpl(ClusterElementDefinitionFacade clusterElementDefinitionFacade) {
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
    }

    @Override
    @PreAuthorize(CONNECTION_ACCESS)
    public List<Property> getClusterElementDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, long mcpComponentId,
        @Nullable Long connectionId) {

        return clusterElementDefinitionFacade.executeDynamicProperties(
            componentName, componentVersion, clusterElementName, propertyName, inputParameters, lookupDependsOnPaths,
            connectionId);
    }

    @Override
    @PreAuthorize(CONNECTION_ACCESS)
    public List<Option> getClusterElementOptions(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, long mcpComponentId,
        @Nullable Long connectionId) {

        return clusterElementDefinitionFacade.executeOptions(
            componentName, componentVersion, clusterElementName, propertyName, inputParameters, Map.of(),
            lookupDependsOnPaths, null, connectionId, Map.of(), Map.of());
    }
}
