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
import com.bytechef.component.definition.unified.accounting.model.AccountUnifiedInputModel;
import com.bytechef.component.definition.unified.accounting.model.AccountUnifiedOutputModel;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import java.util.List;

/**
 * Maps accounting account data between ByteChef's unified account models and a provider's native account models.
 * Implementations convert a {@link AccountUnifiedInputModel} into the provider-native input shape and a provider-native
 * output shape into a {@link AccountUnifiedOutputModel}.
 *
 * @param <OI> the provider-native account input model type
 * @param <OO> the provider-native account output model type
 *
 * @author Ivica Cardic
 */
public interface ProviderAccountMapper<OI extends ProviderInputModel, OO extends ProviderOutputModel>
    extends ProviderModelMapper<AccountUnifiedInputModel, AccountUnifiedOutputModel, OI, OO> {

    /**
     * Converts a unified account input model into the provider-native input model, applying the supplied custom field
     * mappings.
     *
     * @param inputModel          the unified account input model to convert
     * @param customFieldMappings the mappings between unified custom fields and provider-specific fields
     * @return the provider-native account input model
     */
    @Override
    OI desunify(AccountUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Converts a provider-native account output model into the unified account output model, applying the supplied
     * custom field mappings.
     *
     * @param outputModel         the provider-native account output model to convert
     * @param customFieldMappings the mappings between provider-specific fields and unified custom fields
     * @return the unified account output model
     */
    @Override
    AccountUnifiedOutputModel unify(OO outputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Returns the accounting model type handled by this mapper, always {@link AccountingModelType#ACCOUNT}.
     *
     * @return {@link AccountingModelType#ACCOUNT}
     */
    @Override
    default AccountingModelType getModelType() {
        return AccountingModelType.ACCOUNT;
    }
}
