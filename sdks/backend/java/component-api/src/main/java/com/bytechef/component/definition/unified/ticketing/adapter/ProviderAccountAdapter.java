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

package com.bytechef.component.definition.unified.ticketing.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.ticketing.TicketingModelType;
import com.bytechef.component.definition.unified.ticketing.model.ProviderAccountInputModel;
import com.bytechef.component.definition.unified.ticketing.model.ProviderAccountOutputModel;

/**
 * Adapter for provider account models.
 *
 * @author Ivica Cardic
 */
public interface ProviderAccountAdapter
    extends ProviderModelAdapter<ProviderAccountInputModel, ProviderAccountOutputModel> {

    @Override
    String create(ProviderAccountInputModel inputModel, Parameters connectionParameters, Context context);

    @Override
    void delete(String id, Parameters connectionParameters, Context context);

    @Override
    ProviderAccountOutputModel get(String id, Parameters connectionParameters, Context context);

    @Override
    default TicketingModelType getModelType() {
        return TicketingModelType.ACCOUNT;
    }

    @Override
    Page<ProviderAccountOutputModel> getPage(
        Parameters connectionParameters, Parameters cursorParameters, Context context);

    @Override
    void update(String id, ProviderAccountInputModel inputModel, Parameters connectionParameters, Context context);
}
