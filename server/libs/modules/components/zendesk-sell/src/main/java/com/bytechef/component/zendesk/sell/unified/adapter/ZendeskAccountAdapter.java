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

package com.bytechef.component.zendesk.sell.unified.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.unified.crm.adapter.ProviderAccountAdapter;
import com.bytechef.component.zendesk.sell.unified.model.ZendeskAccountInputModel;
import com.bytechef.component.zendesk.sell.unified.model.ZendeskAccountOutputModel;

/**
 * @author Ivica Cardic
 */
public class ZendeskAccountAdapter
    implements ProviderAccountAdapter<ZendeskAccountInputModel, ZendeskAccountOutputModel> {

    @Override
    public String create(ZendeskAccountInputModel inputModel, Parameters connectionParameters, Context context) {
        return null;
    }

    @Override
    public void delete(String id, Parameters connectionParameters, Context context) {

    }

    @Override
    public ZendeskAccountOutputModel get(String id, Parameters connectionParameters, Context context) {
        return null;
    }

    @Override
    public Page<ZendeskAccountOutputModel> getPage(
        Parameters connectionParameters, Parameters cursorParameters, Context context) {

        return null;
    }

    @Override
    public void update(
        String id, ZendeskAccountInputModel inputModel, Parameters connectionParameters, Context context) {
    }
}
