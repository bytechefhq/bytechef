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

import com.bytechef.component.definition.DataStreamItemReader;
import com.bytechef.component.definition.DataStreamItemWriter;
import com.bytechef.component.definition.UnifiedApiDefinition.Category;
import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.platform.component.definition.DataStreamComponentDefinition.ComponentType;
import com.bytechef.platform.component.domain.ComponentDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface ComponentDefinitionService {

    Optional<ComponentDefinition> fetchComponentDefinition(@NonNull String name, Integer version);

    ComponentDefinition getComponentDefinition(@NonNull String name, Integer version);

    List<ComponentDefinition> getComponentDefinitions();

    List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include);

    List<ComponentDefinition> getComponentDefinitionVersions(@NonNull String name);

    ComponentDefinition getConnectionComponentDefinition(@NonNull String name, int connectionVersion);

    List<ComponentDefinition> getDataStreamComponentDefinitions(@NonNull ComponentType componentType);

    DataStreamItemReader getDataStreamItemReader(@NonNull String componentName, int componentVersion);

    DataStreamItemWriter getDataStreamItemWriter(@NonNull String componentName, int componentVersion);

    List<ComponentDefinition> getUnifiedApiComponentDefinitions(Category category);

    ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> getUnifiedApiProviderModelAdapter(
        @NonNull String componentName, @NonNull Category category, @NonNull ModelType modelType);

    ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel>
        getUnifiedApiProviderModelMapper(
            @NonNull String componentName, @NonNull Category category, @NonNull ModelType modelType);

    boolean hasComponentDefinition(@NonNull String name, Integer version);
}
