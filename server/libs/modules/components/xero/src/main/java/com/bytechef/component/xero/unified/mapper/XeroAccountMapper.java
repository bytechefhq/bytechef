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

package com.bytechef.component.xero.unified.mapper;

import com.bytechef.component.definition.unified.crm.mapper.ProviderAccountMapper;
import com.bytechef.component.definition.unified.crm.model.AccountUnifiedInputModel;
import com.bytechef.component.definition.unified.crm.model.AccountUnifiedOutputModel;
import com.bytechef.component.xero.unified.model.XeroAccountInputModel;
import com.bytechef.component.xero.unified.model.XeroAccountOutputModel;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class XeroAccountMapper
    implements ProviderAccountMapper<XeroAccountInputModel, XeroAccountOutputModel> {

    @Override
    public XeroAccountInputModel desunify(
        AccountUnifiedInputModel inputModel, List<CustomFieldMapping> customFieldMappings) {

        return null;
    }

    @Override
    public AccountUnifiedOutputModel unify(
        XeroAccountOutputModel outputModel, List<CustomFieldMapping> customFieldMappings) {

        return null;
    }
}
