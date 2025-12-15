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

package com.bytechef.component.nocodb.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.nocodb.util.NocoDbUtils;

/**
 * @author Monika Kušter
 */
public class NocoDbConstants {

    public static final String BASE_ID = "baseId";
    public static final String BASE_URL = "baseUrl";
    public static final String FIELDS = "fields";
    public static final String RECORDS = "records";
    public static final String RECORD_ID = "recordId";
    public static final String SORT = "sort";
    public static final String TABLE_COLUMNS = "tableColumns";
    public static final String TABLE_ID = "tableId";
    public static final String TITLE = "title";
    public static final String WHERE = "where";
    public static final String WORKSPACE_ID = "workspaceId";

    public static final ModifiableStringProperty BASE_ID_PROPERTY = string(BASE_ID)
        .label("Base ID")
        .description("ID of the base.")
        .options((OptionsFunction<String>) NocoDbUtils::getBaseIdOptions)
        .optionsLookupDependsOn(WORKSPACE_ID)
        .required(false);

    public static final ModifiableStringProperty TABLE_ID_PROPERTY = string(TABLE_ID)
        .label("Table ID")
        .description("ID of the table.")
        .options((OptionsFunction<String>) NocoDbUtils::getTableIdOptions)
        .optionsLookupDependsOn(BASE_ID, WORKSPACE_ID)
        .required(true);

    public static final ModifiableStringProperty WORKSPACE_ID_PROPERTY = string(WORKSPACE_ID)
        .label("Workspace ID")
        .description("ID of the workspace.")
        .options((OptionsFunction<String>) NocoDbUtils::getWorkspaceIdOptions)
        .required(false);

    private NocoDbConstants() {
    }
}
