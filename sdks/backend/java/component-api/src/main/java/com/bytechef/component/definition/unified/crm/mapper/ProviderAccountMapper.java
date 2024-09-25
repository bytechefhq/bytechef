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

package com.bytechef.component.definition.unified.crm.mapper;

import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.crm.CrmModelType;
import com.bytechef.component.definition.unified.crm.model.AccountUnifiedInputModel;
import com.bytechef.component.definition.unified.crm.model.AccountUnifiedOutputModel;
import java.util.List;

/**
 * Mapper for account models.
 *
 * @param <OI>
 * @param <OO>
 *
 * @author Ivica Cardic
 */
public interface ProviderAccountMapper<OI extends ProviderInputModel, OO extends ProviderOutputModel>
    extends ProviderModelMapper<AccountUnifiedInputModel, AccountUnifiedOutputModel, OI, OO> {

    @Override
    OI desunify(AccountUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings);

    @Override
    AccountUnifiedOutputModel unify(OO outputModel, List<CustomFieldMapping> customFieldMappings);

    @Override
    default CrmModelType getModelType() {
        return CrmModelType.ACCOUNT;
    }
}
