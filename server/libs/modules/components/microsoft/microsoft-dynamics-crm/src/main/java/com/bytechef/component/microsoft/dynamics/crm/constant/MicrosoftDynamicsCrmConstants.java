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

package com.bytechef.component.microsoft.dynamics.crm.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.microsoft.dynamics.crm.util.MicrosoftDynamicsCrmUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftDynamicsCrmConstants {

    public static final String ENTITY_TYPE = "entityType";
    public static final String FIELDS = "fields";
    public static final String PROXY_URL = "proxyUrl";
    public static final String RECORD_ID = "recordId";
    public static final String TENANT_ID = "tenantId";

    public static final ModifiableStringProperty RECORD_ID_PROPERTY = string(RECORD_ID)
        .label("Record ID")
        .options((OptionsFunction<String>) MicrosoftDynamicsCrmUtils::getRecordIdOptions)
        .optionsLookupDependsOn(ENTITY_TYPE)
        .required(true);

    private MicrosoftDynamicsCrmConstants() {
    }
}
