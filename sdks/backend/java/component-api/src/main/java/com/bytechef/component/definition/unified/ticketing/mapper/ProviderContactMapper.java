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

package com.bytechef.component.definition.unified.ticketing.mapper;

import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.ticketing.TicketingModelType;
import com.bytechef.component.definition.unified.ticketing.model.ContactUnifiedInputModel;
import com.bytechef.component.definition.unified.ticketing.model.ContactUnifiedOutputModel;
import java.util.List;

/**
 * Mapper for contact provider models.
 *
 * @param <OI>
 * @param <OO>
 *
 * @author Ivica Cardic
 */
public interface ProviderContactMapper<OI extends ProviderInputModel, OO extends ProviderOutputModel>
    extends ProviderModelMapper<ContactUnifiedInputModel, ContactUnifiedOutputModel, OI, OO> {

    @Override
    OI desunify(ContactUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings);

    @Override
    ContactUnifiedOutputModel unify(OO outputModel, List<CustomFieldMapping> customFieldMappings);

    @Override
    default TicketingModelType getModelType() {
        return TicketingModelType.CONTACT;
    }
}
