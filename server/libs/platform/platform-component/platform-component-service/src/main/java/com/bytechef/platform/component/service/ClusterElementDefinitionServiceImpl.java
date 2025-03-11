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
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementTypeName) {

        ClusterElementType clusterElementType = getClusterElementType(
            componentName, componentVersion, clusterElementTypeName);

        return getClusterElementDefinition(componentName, componentVersion, clusterElementType);
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        Assert.isTrue(
            !clusterElementType.multipleElements(),
            "Cluster element type " + clusterElementType.name() + " contains multiple elements");

        List<? extends com.bytechef.component.definition.ClusterElementDefinition<?>> clusterElementDefinitions =
            getClusterElementDefinitions(componentName, componentVersion, clusterElementType);

        return new ClusterElementDefinition(clusterElementDefinitions.getFirst(), componentName, componentVersion);
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, ClusterElementType clusterElementType, String name) {

        List<? extends com.bytechef.component.definition.ClusterElementDefinition<?>> clusterElementDefinitions =
            getClusterElementDefinitions(componentName, componentVersion, clusterElementType);

        return clusterElementDefinitions.stream()
            .filter(clusterElementDefinition -> name.equals(clusterElementDefinition.getName()))
            .findFirst()
            .map(clusterElementDefinition -> new ClusterElementDefinition(
                clusterElementDefinition, componentName, componentVersion))
            .orElseThrow(() -> new IllegalArgumentException(
                "Cluster element definition " + name + " not found in component " + componentName));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getClusterElementObject(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        Assert.isTrue(
            !clusterElementType.multipleElements(),
            "Cluster element type " + clusterElementType.name() + " contains multiple elements");
        Assert.isTrue(
            clusterElementType.local(),
            "Cluster element type " + clusterElementType.name() + " is not local");

        List<? extends com.bytechef.component.definition.ClusterElementDefinition<?>> clusterElementDefinitions =
            getClusterElementDefinitions(componentName, componentVersion, clusterElementType);

        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition =
            clusterElementDefinitions.getFirst();

        return (T) clusterElementDefinition.getObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getClusterElementObject(
        String componentName, int componentVersion, ClusterElementType clusterElementType, String name) {

        Assert.isTrue(
            clusterElementType.local(),
            "Cluster element type " + clusterElementType.name() + " is not local");

        List<? extends com.bytechef.component.definition.ClusterElementDefinition<?>> clusterElementDefinitions =
            getClusterElementDefinitions(componentName, componentVersion, clusterElementType);

        return (T) clusterElementDefinitions.stream()
            .filter(clusterElementDefinition -> name.equals(clusterElementDefinition.getName()))
            .findFirst()
            .map(com.bytechef.component.definition.ClusterElementDefinition::getObject)
            .orElseThrow(() -> new IllegalArgumentException(
                "Cluster element definition " + name + " not found in component " + componentName));
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        ClusterElementType clusterElementType = getClusterElementType(
            rootComponentName, rootComponentVersion, clusterElementTypeName);

        return getRootClusterElementDefinitions(rootComponentName, rootComponentVersion, clusterElementType);
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, ClusterElementType clusterElementType) {

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

    private List<? extends com.bytechef.component.definition.ClusterElementDefinition<?>> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getClusterElements(), List.of())
            .stream()
            .map(
                clusterElementDefinition -> (com.bytechef.component.definition.ClusterElementDefinition<?>) clusterElementDefinition)
            .filter(clusterElementDefinition -> clusterElementType.equals(clusterElementDefinition.getType()))
            .toList();
    }

    private ClusterElementType getClusterElementType(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        ClusterRootComponentDefinition rootComponentDefinition =
            (ClusterRootComponentDefinition) componentDefinitionRegistry.getComponentDefinition(
                rootComponentName, rootComponentVersion);

        return rootComponentDefinition.getElementTypes()
            .stream()
            .filter(curClusterElementType -> Objects.equals(curClusterElementType.name(), clusterElementTypeName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Cluster element type %s not found in root component %s".formatted(
                    clusterElementTypeName, rootComponentName)));
    }
}
