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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("unifiedApiService")
public class UnifiedApiDefinitionServiceImpl implements UnifiedApiDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    public UnifiedApiDefinitionServiceImpl(@Lazy ComponentDefinitionRegistry componentDefinitionRegistry) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public List<ComponentDefinition>
        getUnifiedApiComponentDefinitions(UnifiedApiDefinition.UnifiedApiCategory category) {
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
            String componentName, UnifiedApiDefinition.UnifiedApiCategory category, ModelType modelType) {

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
            String componentName, UnifiedApiCategory category, ModelType modelType) {

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
}
