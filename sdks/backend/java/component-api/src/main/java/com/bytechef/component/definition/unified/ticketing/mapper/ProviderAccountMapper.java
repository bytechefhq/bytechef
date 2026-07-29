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
import com.bytechef.component.definition.unified.ticketing.model.AccountUnifiedInputModel;
import com.bytechef.component.definition.unified.ticketing.model.AccountUnifiedOutputModel;
import java.util.List;

/**
 * Maps ticketing account models between ByteChef's unified shape and a provider's native shape. It converts a
 * {@link AccountUnifiedInputModel} into the provider input model {@code OI} when sending data to a provider, and a
 * provider output model {@code OO} into an {@link AccountUnifiedOutputModel} when reading data back.
 *
 * @param <OI> the provider-native input model type produced by {@link #desunify}
 * @param <OO> the provider-native output model type consumed by {@link #unify}
 *
 * @author Ivica Cardic
 */
public interface ProviderAccountMapper<OI extends ProviderInputModel, OO extends ProviderOutputModel>
    extends ProviderModelMapper<AccountUnifiedInputModel, AccountUnifiedOutputModel, OI, OO> {

    /**
     * Converts a unified account input model into the provider's native input model, applying the supplied custom field
     * mappings to translate unified fields onto provider-specific fields.
     *
     * @param inputModel          the unified account input model to convert
     * @param customFieldMappings the mappings between unified and provider-specific custom fields
     * @return the provider-native input model
     */
    @Override
    OI desunify(AccountUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Converts a provider's native account output model into the unified output model, applying the supplied custom
     * field mappings to translate provider-specific fields onto unified fields.
     *
     * @param outputModel         the provider-native account output model to convert
     * @param customFieldMappings the mappings between unified and provider-specific custom fields
     * @return the unified account output model
     */
    @Override
    AccountUnifiedOutputModel unify(OO outputModel, List<CustomFieldMapping> customFieldMappings);

    /**
     * Returns the model type handled by this mapper, always {@link TicketingModelType#ACCOUNT}.
     *
     * @return {@link TicketingModelType#ACCOUNT}
     */
    @Override
    default TicketingModelType getModelType() {
        return TicketingModelType.ACCOUNT;
    }
}
