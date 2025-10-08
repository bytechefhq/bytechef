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

package com.bytechef.platform.component.service;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ClusterElementDefinitionService {

    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementNameName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        ComponentConnection componentConnection, ClusterElementContext context);

    List<Option> executeOptions(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        ComponentConnection componentConnection, ClusterElementContext context);

    ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String clusterElementName, int statusCode, Object body,
        ClusterElementContext context);

    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, ClusterElementContext context);

    String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        ClusterElementContext context);

    <T> T getClusterElement(String componentName, int componentVersion, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(String componentName, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName);

    List<ClusterElementDefinition> getClusterElementDefinitions(ClusterElementType clusterElementType);

    List<ClusterElementDefinition> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType);

    ClusterElementType getClusterElementType(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName);

    List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName);
}
