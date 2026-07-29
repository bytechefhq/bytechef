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
import com.bytechef.component.definition.unified.crm.model.AccountUnifiedInputModel;
import com.bytechef.component.definition.unified.crm.model.AccountUnifiedOutputModel;
import java.util.List;

/**
 * Maps CRM account data between ByteChef's unified account models and a provider's native account models.
 * Implementations translate the normalized {@link AccountUnifiedInputModel} into a provider-native input model when
 * writing to the provider, and translate a provider-native output model back into the normalized
 * {@link AccountUnifiedOutputModel} when reading, applying any configured custom-field mappings in both directions.
 *
 * @param <OI> the provider-native account input model type produced by {@link #desunify}
 * @param <OO> the provider-native account output model type consumed by {@link #unify}
 *
 * @author Ivica Cardic
 */
public interface ProviderAccountMapper<OI extends ProviderInputModel, OO extends ProviderOutputModel>
    extends ProviderModelMapper<AccountUnifiedInputModel, AccountUnifiedOutputModel, OI, OO> {

    /**
     * Converts a unified account input model into the provider-native input model, applying the supplied custom-field
     * mappings so that unified custom fields are written to their provider-specific counterparts.
     *
     * @param inputModel          the unified account input model to convert
     * @param customFieldMappings the mappings between unified and provider-specific custom fields
     * @return the provider-native account input model
     */
    @Override
    OI desunify(AccountUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Converts a provider-native account output model into the unified account output model, applying the supplied
     * custom-field mappings so that provider-specific fields surface as unified custom fields.
     *
     * @param outputModel         the provider-native account output model to convert
     * @param customFieldMappings the mappings between unified and provider-specific custom fields
     * @return the unified account output model
     */
    @Override
    AccountUnifiedOutputModel unify(OO outputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Returns the CRM model type handled by this mapper, which is always {@link CrmModelType#ACCOUNT}.
     *
     * @return {@link CrmModelType#ACCOUNT}
     */
    @Override
    default CrmModelType getModelType() {
        return CrmModelType.ACCOUNT;
    }
}
