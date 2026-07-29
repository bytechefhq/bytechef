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

package com.bytechef.component.definition.unified.accounting.mapper;

import com.bytechef.component.definition.unified.accounting.AccountingModelType;
import com.bytechef.component.definition.unified.accounting.model.ContactUnifiedInputModel;
import com.bytechef.component.definition.unified.accounting.model.ContactUnifiedOutputModel;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import java.util.List;

/**
 * Maps accounting contact data between ByteChef's unified contact models and a provider's native contact models.
 * Implementations convert a {@link ContactUnifiedInputModel} into the provider-native input shape and a provider-native
 * output shape into a {@link ContactUnifiedOutputModel}.
 *
 * @param <OI> the provider-native contact input model type
 * @param <OO> the provider-native contact output model type
 *
 * @author Ivica Cardic
 */
public interface ProviderContactMapper<OI extends ProviderInputModel, OO extends ProviderOutputModel>
    extends ProviderModelMapper<ContactUnifiedInputModel, ContactUnifiedOutputModel, OI, OO> {

    /**
     * Converts a unified contact input model into the provider-native input model, applying the supplied custom field
     * mappings.
     *
     * @param inputModel          the unified contact input model to convert
     * @param customFieldMappings the mappings between unified custom fields and provider-specific fields
     * @return the provider-native contact input model
     */
    @Override
    OI desunify(ContactUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Converts a provider-native contact output model into the unified contact output model, applying the supplied
     * custom field mappings.
     *
     * @param outputModel         the provider-native contact output model to convert
     * @param customFieldMappings the mappings between provider-specific fields and unified custom fields
     * @return the unified contact output model
     */
    @Override
    ContactUnifiedOutputModel unify(OO outputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Returns the accounting model type handled by this mapper, always {@link AccountingModelType#CONTACT}.
     *
     * @return {@link AccountingModelType#CONTACT}
     */
    @Override
    default AccountingModelType getModelType() {
        return AccountingModelType.CONTACT;
    }
}
