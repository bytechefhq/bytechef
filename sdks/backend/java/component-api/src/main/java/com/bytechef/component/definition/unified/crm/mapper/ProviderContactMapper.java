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

package com.bytechef.component.definition.unified.crm.mapper;

import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.crm.CrmModelType;
import com.bytechef.component.definition.unified.crm.model.ContactUnifiedInputModel;
import com.bytechef.component.definition.unified.crm.model.ContactUnifiedOutputModel;
import java.util.List;

/**
 * Maps CRM contact data between ByteChef's unified contact models and a provider's native contact models.
 * Implementations translate the normalized {@link ContactUnifiedInputModel} into a provider-native input model when
 * writing to the provider, and translate a provider-native output model back into the normalized
 * {@link ContactUnifiedOutputModel} when reading, applying any configured custom-field mappings in both directions.
 *
 * @param <OI> the provider-native contact input model type produced by {@link #desunify}
 * @param <OO> the provider-native contact output model type consumed by {@link #unify}
 *
 * @author Ivica Cardic
 */
public interface ProviderContactMapper<OI extends ProviderInputModel, OO extends ProviderOutputModel>
    extends ProviderModelMapper<ContactUnifiedInputModel, ContactUnifiedOutputModel, OI, OO> {

    /**
     * Converts a unified contact input model into the provider-native input model, applying the supplied custom-field
     * mappings so that unified custom fields are written to their provider-specific counterparts.
     *
     * @param inputModel          the unified contact input model to convert
     * @param customFieldMappings the mappings between unified and provider-specific custom fields
     * @return the provider-native contact input model
     */
    @Override
    OI desunify(ContactUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Converts a provider-native contact output model into the unified contact output model, applying the supplied
     * custom-field mappings so that provider-specific fields surface as unified custom fields.
     *
     * @param outputModel         the provider-native contact output model to convert
     * @param customFieldMappings the mappings between unified and provider-specific custom fields
     * @return the unified contact output model
     */
    @Override
    ContactUnifiedOutputModel unify(OO outputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Returns the CRM model type handled by this mapper, which is always {@link CrmModelType#CONTACT}.
     *
     * @return {@link CrmModelType#CONTACT}
     */
    @Override
    default CrmModelType getModelType() {
        return CrmModelType.CONTACT;
    }
}
