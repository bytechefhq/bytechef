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

package com.bytechef.component.definition.unified.crm.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.crm.CrmModelType;
import com.bytechef.component.definition.unified.crm.model.ProviderContactInputModel;
import com.bytechef.component.definition.unified.crm.model.ProviderContactOutputModel;

/**
 * Adapter for provider contact.
 *
 * @author Ivica Cardic
 */
public interface ProviderContactAdapter<PCI extends ProviderContactInputModel, PCO extends ProviderContactOutputModel>
    extends ProviderModelAdapter<PCI, PCO> {

    @Override
    String create(PCI inputModel, Parameters connectionParameters, Context context);

    @Override
    void delete(String id, Parameters connectionParameters, Context context);

    @Override
    PCO get(String id, Parameters connectionParameters, Context context);

    @Override
    default CrmModelType getModelType() {
        return CrmModelType.CONTACT;
    }

    @Override
    Page<PCO> getPage(Parameters connectionParameters, Parameters cursorParameters, Context context);

    @Override
    void update(String id, PCI inputModel, Parameters connectionParameters, Context context);
}
