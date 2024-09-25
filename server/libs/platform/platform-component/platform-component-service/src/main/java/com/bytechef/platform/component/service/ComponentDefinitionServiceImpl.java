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
import com.bytechef.component.definition.DataStreamDefinition;
import com.bytechef.component.definition.DataStreamItemReader;
import com.bytechef.component.definition.DataStreamItemWriter;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition.Category;
import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.definition.DataStreamComponentDefinition.ComponentType;
import com.bytechef.platform.component.domain.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("componentDefinitionService")
public class ComponentDefinitionServiceImpl implements ComponentDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public ComponentDefinitionServiceImpl(ComponentDefinitionRegistry componentDefinitionRegistry) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public ComponentDefinition getComponentDefinition(@NonNull String name, Integer version) {
        com.bytechef.component.definition.ComponentDefinition componentDefinition =
            componentDefinitionRegistry.getComponentDefinition(name, version);

        return new ComponentDefinition(componentDefinition);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .map(ComponentDefinition::new)
            .toList();
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include) {

        List<ComponentDefinition> components = getComponentDefinitions()
            .stream()
            .filter(filter(actionDefinitions, connectionDefinitions, triggerDefinitions, include))
            .distinct()
            .toList();

        if (include != null && !include.isEmpty()) {
            components = new ArrayList<>(components);

            components.sort(Comparator.comparing(component -> include.indexOf(component.getName())));
        }

        return components;
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitionVersions(@NonNull String name) {
        return componentDefinitionRegistry.getComponentDefinitions(name)
            .stream()
            .map(ComponentDefinition::new)
            .toList();
    }

    @Override
    public List<ComponentDefinition> getDataStreamComponentDefinitions(@NonNull ComponentType componentType) {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> {
                if (componentType == ComponentType.SOURCE) {
                    return componentDefinition.getDataStream()
                        .flatMap(DataStreamDefinition::getReader)
                        .isPresent();
                } else {
                    return componentDefinition.getDataStream()
                        .flatMap(DataStreamDefinition::getWriter)
                        .isPresent();
                }
            })
            .map(ComponentDefinition::new)
            .toList();
    }

    @Override
    public DataStreamItemReader getDataStreamItemReader(@NonNull String componentName, int componentVersion) {
        com.bytechef.component.definition.ComponentDefinition componentDefinition =
            componentDefinitionRegistry.getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getDataStream()
            .flatMap(DataStreamDefinition::getReader)
            .orElseThrow(() -> new IllegalArgumentException(
                "Data stream item reader for component: %s, version: %d not found".formatted(
                    componentName, componentVersion)))
            .getDataStreamItemReader()
            .get();
    }

    @Override
    public DataStreamItemWriter getDataStreamItemWriter(@NonNull String componentName, int componentVersion) {
        com.bytechef.component.definition.ComponentDefinition componentDefinition =
            componentDefinitionRegistry.getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getDataStream()
            .flatMap(DataStreamDefinition::getWriter)
            .orElseThrow(() -> new IllegalArgumentException(
                "Data stream item writer with component: %s, version: %d not found".formatted(
                    componentName, componentVersion)))
            .getDataStreamItemWriter()
            .get();
    }

    @Override
    public List<ComponentDefinition> getUnifiedApiComponentDefinitions(Category category) {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getUnifiedApi()))
            .map(ComponentDefinition::new)
            .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public
        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel>
        getUnifiedApiProviderModelMapper(
            @NonNull String componentName, @NonNull Category category, @NonNull ModelType modelType) {

        return (ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel>) componentDefinitionRegistry
            .getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getUnifiedApi()) &&
                componentName.equals(componentDefinition.getName()))
            .map(com.bytechef.component.definition.ComponentDefinition::getUnifiedApi)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unified API provider model mapper for component: %s not found".formatted(componentName)))
            .filter(unifiedApiDefinition -> unifiedApiDefinition.getCategory() == category)
            .map(UnifiedApiDefinition::getProviderMappers)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unified API provider model mapper for category: %s not found".formatted(category)))
            .stream()
            .filter(providerModelMapper -> providerModelMapper.getModelType() == modelType)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unified API provider model mapper for model type: %s not found".formatted(modelType)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel>
        getUnifiedApiProviderModelAdapter(
            @NonNull String componentName, @NonNull Category category, @NonNull ModelType modelType) {

        return (ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel>) componentDefinitionRegistry
            .getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getUnifiedApi()) &&
                componentName.equals(componentDefinition.getName()))
            .map(com.bytechef.component.definition.ComponentDefinition::getUnifiedApi)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unified API provider model adapter for component: %s not found".formatted(componentName)))
            .filter(unifiedApiDefinition -> unifiedApiDefinition.getCategory() == category)
            .map(UnifiedApiDefinition::getProviderAdapters)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unified API provider model adapter for category: %s not found".formatted(category)))
            .stream()
            .filter(providerModelAdapter -> providerModelAdapter.getModelType() == modelType)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unified API provider model adapter for model type: %s not found".formatted(modelType)));
    }

    @Override
    public boolean hasComponentDefinition(@NonNull String name, Integer version) {
        return componentDefinitionRegistry.hasComponentDefinition(name, version);
    }

    private static Predicate<ComponentDefinition> filter(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include) {

        return componentDefinition -> {
            if (include != null && include.contains(componentDefinition.getName())) {
                return true;
            }

            if (actionDefinitions != null && !CollectionUtils.isEmpty(componentDefinition.getActions())) {
                return true;
            }

            if (connectionDefinitions != null && componentDefinition.getConnection() != null) {
                return true;
            }

            if (triggerDefinitions != null && !CollectionUtils.isEmpty(componentDefinition.getTriggers())) {
                return true;
            }

            if (include == null && actionDefinitions == null && connectionDefinitions == null &&
                triggerDefinitions == null) {

                return true;
            }

            return false;
        };
    }
}
