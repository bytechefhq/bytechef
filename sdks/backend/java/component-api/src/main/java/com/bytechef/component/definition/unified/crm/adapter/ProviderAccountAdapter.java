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

package com.bytechef.component.definition.unified.crm.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.crm.CrmModelType;
import com.bytechef.component.definition.unified.crm.model.ProviderAccountInputModel;
import com.bytechef.component.definition.unified.crm.model.ProviderAccountOutputModel;

/**
 * Adapter for provider account models.
 *
 * @author Ivica Cardic
 */
public interface ProviderAccountAdapter<PAI extends ProviderAccountInputModel, PAO extends ProviderAccountOutputModel>
    extends ProviderModelAdapter<PAI, PAO> {

    @Override
    String create(PAI inputModel, Parameters connectionParameters, Context context);

    @Override
    void delete(String id, Parameters connectionParameters, Context context);

    @Override
    PAO get(String id, Parameters connectionParameters, Context context);

    @Override
    default CrmModelType getModelType() {
        return CrmModelType.ACCOUNT;
    }

    @Override
    Page<PAO> getPage(Parameters connectionParameters, Parameters cursorParameters, Context context);

    @Override
    void update(String id, PAI inputModel, Parameters connectionParameters, Context context);
}
