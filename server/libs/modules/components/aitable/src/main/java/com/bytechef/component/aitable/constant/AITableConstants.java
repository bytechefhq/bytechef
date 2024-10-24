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

package com.bytechef.component.aitable.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.component.aitable.util.AITableUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableDynamicPropertiesProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;

/**
 * @author Monika Domiter
 */
public class AITableConstants {

    public static final String DATA = "data";
    public static final String DATASHEET_ID = "datasheetId";
    public static final String FIELDS = "fields";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String MAX_RECORDS = "maxRecords";
    public static final String RECORD_ID = "recordId";
    public static final String RECORD_IDS = "recordIds";
    public static final String RECORDS = "records";
    public static final String SPACE_ID = "spaceId";
    public static final String TYPE = "type";

    public static final ModifiableStringProperty DATASHEET_ID_PROPERTY = string(DATASHEET_ID)
        .label("Datasheet")
        .description("AITable Datasheet")
        .optionsLookupDependsOn(SPACE_ID)
        .options((ActionOptionsFunction<String>) AITableUtils::getDatasheetIdOptions)
        .required(true);

    public static final ModifiableDynamicPropertiesProperty FIELDS_DYNAMIC_PROPERTY = dynamicProperties(FIELDS)
        .propertiesLookupDependsOn(DATASHEET_ID)
        .properties(AITableUtils::createPropertiesForRecord);

    public static final ModifiableObjectProperty OUTPUT_PROPERTY = object()
        .properties(
            integer("code"),
            bool("success"),
            object("data")
                .properties(
                    array(RECORDS)
                        .items(
                            object()
                                .properties(
                                    string(RECORD_ID),
                                    object(FIELDS)
                                        .additionalProperties(
                                            array(), bool(), date(), dateTime(), integer(), nullable(),
                                            number(), object(), string(), time())))),
            string("message"));

    public static final ModifiableStringProperty SPACE_ID_PROPERTY = string(SPACE_ID)
        .label("Space")
        .options((ActionOptionsFunction<String>) AITableUtils::getSpaceIdOptions)
        .required(true);

    private AITableConstants() {
    }
}
