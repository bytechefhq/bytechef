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

package com.bytechef.component.definition.unified.base.mapper;

import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import java.util.List;

/**
 * Mapper to convert between a provider model and unified model.
 *
 * @param <UI>
 * @param <UO>
 * @param <PI>
 * @param <PO>
 *
 * @author Ivica Cardic
 */
public interface ProviderModelMapper<UI extends UnifiedInputModel, UO extends UnifiedOutputModel, PI extends ProviderInputModel, PO extends ProviderOutputModel> {

    PI desunify(UI inputModel, List<CustomFieldMapping> customFieldMappings);

    UO unify(PO outputModel, List<CustomFieldMapping> customFieldMappings);

    ModelType getModelType();

    record CustomFieldMapping(String unifiedField, String providerField) {
    }
}
