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

package com.bytechef.component.jira.unifiedapi;

import static com.bytechef.component.definition.ComponentDsl.unifiedApi;
import static com.bytechef.component.definition.UnifiedApiDefinition.Category.TICKETING;

import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.jira.unified.adapter.JiraAccountAdapter;
import com.bytechef.component.jira.unified.adapter.JiraContactAdapter;
import com.bytechef.component.jira.unified.mapper.JiraAccountMapper;
import com.bytechef.component.jira.unified.mapper.JiraContactMapper;

/**
 * @author Ivica Cardic
 */
public class JiraUnifiedApi {

    public static final UnifiedApiDefinition UNIFIED_API_DEFINITION = unifiedApi(TICKETING)
        .providerAdapters(new JiraAccountAdapter(), new JiraContactAdapter())
        .providerMappers(new JiraAccountMapper(), new JiraContactMapper());
}
