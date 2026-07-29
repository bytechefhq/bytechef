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

package com.bytechef.component.definition.unified.base.mapper;

import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import java.util.List;

/**
 * Mapper that translates between unified models (the provider-agnostic representation exposed by the unified API) and
 * provider-specific models (the shape a concrete third-party API expects). Custom field mappings allow
 * provider-specific fields to be surfaced through the unified model's custom-field mechanism.
 *
 * @param <UI> the unified input model type
 * @param <UO> the unified output model type
 * @param <PI> the provider input model type
 * @param <PO> the provider output model type
 *
 * @author Ivica Cardic
 */
public interface ProviderModelMapper<UI extends UnifiedInputModel, UO extends UnifiedOutputModel, PI extends ProviderInputModel, PO extends ProviderOutputModel> {

    /**
     * Converts a unified input model into the provider-specific input model, applying the given custom field mappings.
     *
     * @param inputModel          the unified input model to convert
     * @param customFieldMappings the mappings from unified custom fields to provider fields
     * @return the corresponding provider input model
     */
    PI desunify(UI inputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Converts a provider-specific output model into the unified output model, applying the given custom field
     * mappings.
     *
     * @param outputModel         the provider output model to convert
     * @param customFieldMappings the mappings from provider fields to unified custom fields
     * @return the corresponding unified output model
     */
    UO unify(PO outputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Returns the unified model type that this mapper handles.
     *
     * @return the model type served by this mapper
     */
    ModelType getModelType();

    /**
     * Associates a field in the unified model with the corresponding field in the provider model, enabling custom
     * fields to be carried across the unification boundary.
     *
     * @param unifiedField  the field name in the unified model
     * @param providerField the field name in the provider model
     */
    record CustomFieldMapping(String unifiedField, String providerField) {
    }
}
