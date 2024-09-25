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

package com.bytechef.component.xero.unified;

import static com.bytechef.component.definition.ComponentDsl.unifiedApi;
import static com.bytechef.component.definition.UnifiedApiDefinition.Category.ACCOUNTING;

import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.xero.unified.adapter.XeroAccountAdapter;
import com.bytechef.component.xero.unified.adapter.XeroContactAdapter;
import com.bytechef.component.xero.unified.mapper.XeroAccountMapper;
import com.bytechef.component.xero.unified.mapper.XeroContactMapper;

/**
 * @author Ivica Cardic
 */
public class XeroUnifiedApi {

    public static final UnifiedApiDefinition UNIFIED_API_DEFINITION = unifiedApi(ACCOUNTING)
        .providerAdapters(new XeroAccountAdapter(), new XeroContactAdapter())
        .providerMappers(new XeroAccountMapper(), new XeroContactMapper());
}
