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

package com.bytechef.platform.component.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.exception.ExecutionException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("clusterElementDefinitionService")
public class ClusterElementDefinitionServiceImpl implements ClusterElementDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    public ClusterElementDefinitionServiceImpl(ComponentDefinitionRegistry componentDefinitionRegistry) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, Context context) {

        SingleConnectionToolFunction toolFunction = getClusterElementObject(
            componentName, componentVersion, clusterElementName);

        try {
            return toolFunction.apply(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(
                    componentConnection == null ? Map.of() : componentConnection.getParameters()),
                context);
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(String componentName, String clusterElementName) {
        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, null);

        return getClusterElementDefinition(componentName, componentDefinition.getVersion(), clusterElementName);
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName) {

        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition =
            getComponentClusterElementDefinition(componentName, componentVersion, clusterElementName);

        return new ClusterElementDefinition(clusterElementDefinition, componentName, componentVersion);
    }

    @Override
    public List<ClusterElementDefinition> getClusterElementDefinitions(ClusterElementType clusterElementType) {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getClusterElements()))
            .flatMap(componentDefinition -> CollectionUtils.stream(
                OptionalUtils.get(componentDefinition.getClusterElements())
                    .stream()
                    .filter(clusterElementDefinition -> clusterElementType.equals(
                        clusterElementDefinition.getType()))
                    .map(clusterElementDefinition -> new ClusterElementDefinition(
                        clusterElementDefinition, componentDefinition.getName(), componentDefinition.getVersion()))
                    .toList()))
            .distinct()
            .toList();
    }

    @Override
    public List<ClusterElementDefinition> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getClusterElements(), List.of())
            .stream()
            .map(
                clusterElementDefinition -> (com.bytechef.component.definition.ClusterElementDefinition<?>) clusterElementDefinition)
            .filter(clusterElementDefinition -> clusterElementType == clusterElementDefinition.getType())
            .map(clusterElementDefinition -> new ClusterElementDefinition(
                clusterElementDefinition, componentName, componentVersion))
            .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getClusterElementObject(
        String componentName, int componentVersion, String clusterElementName) {

        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition =
            getComponentClusterElementDefinition(componentName, componentVersion, clusterElementName);

        return (T) clusterElementDefinition.getObject();
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        ClusterElementType clusterElementType = getClusterElementType(
            rootComponentName, rootComponentVersion, clusterElementTypeName);

        return getClusterElementDefinitions(clusterElementType);
    }

    private ClusterElementType getClusterElementType(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        ClusterRootComponentDefinition rootComponentDefinition =
            (ClusterRootComponentDefinition) componentDefinitionRegistry.getComponentDefinition(
                rootComponentName, rootComponentVersion);

        return rootComponentDefinition.getClusterElementType()
            .stream()
            .filter(curClusterElementType -> Objects.equals(curClusterElementType.name(), clusterElementTypeName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Cluster element type %s not found in root component %s".formatted(
                    clusterElementTypeName, rootComponentName)));
    }

    private com.bytechef.component.definition.ClusterElementDefinition<?> getComponentClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getClusterElements(), List.of())
            .stream()
            .map(
                clusterElementDefinition -> (com.bytechef.component.definition.ClusterElementDefinition<?>) clusterElementDefinition)
            .filter(clusterElementDefinition -> clusterElementName.equals(clusterElementDefinition.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Cluster element definition " + clusterElementName + " not found in component " + componentName));
    }
}
