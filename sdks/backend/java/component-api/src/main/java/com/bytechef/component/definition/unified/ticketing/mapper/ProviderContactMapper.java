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

package com.bytechef.component.definition.unified.ticketing.mapper;

import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.ticketing.TicketingModelType;
import com.bytechef.component.definition.unified.ticketing.model.ContactUnifiedInputModel;
import com.bytechef.component.definition.unified.ticketing.model.ContactUnifiedOutputModel;
import java.util.List;

/**
 * Maps ticketing contact models between ByteChef's unified shape and a provider's native shape. It converts a
 * {@link ContactUnifiedInputModel} into the provider input model {@code OI} when sending data to a provider, and a
 * provider output model {@code OO} into a {@link ContactUnifiedOutputModel} when reading data back.
 *
 * @param <OI> the provider-native input model type produced by {@link #desunify}
 * @param <OO> the provider-native output model type consumed by {@link #unify}
 *
 * @author Ivica Cardic
 */
public interface ProviderContactMapper<OI extends ProviderInputModel, OO extends ProviderOutputModel>
    extends ProviderModelMapper<ContactUnifiedInputModel, ContactUnifiedOutputModel, OI, OO> {

    /**
     * Converts a unified contact input model into the provider's native input model, applying the supplied custom field
     * mappings to translate unified fields onto provider-specific fields.
     *
     * @param inputModel          the unified contact input model to convert
     * @param customFieldMappings the mappings between unified and provider-specific custom fields
     * @return the provider-native input model
     */
    @Override
    OI desunify(ContactUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Converts a provider's native contact output model into the unified output model, applying the supplied custom
     * field mappings to translate provider-specific fields onto unified fields.
     *
     * @param outputModel         the provider-native contact output model to convert
     * @param customFieldMappings the mappings between unified and provider-specific custom fields
     * @return the unified contact output model
     */
    @Override
    ContactUnifiedOutputModel unify(OO outputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Returns the model type handled by this mapper, always {@link TicketingModelType#CONTACT}.
     *
     * @return {@link TicketingModelType#CONTACT}
     */
    @Override
    default TicketingModelType getModelType() {
        return TicketingModelType.CONTACT;
    }
}
