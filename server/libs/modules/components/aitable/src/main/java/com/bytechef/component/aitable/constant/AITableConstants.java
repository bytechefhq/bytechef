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

package com.bytechef.component.aitable.constant;

import static com.bytechef.component.aitable.constant.FieldType.ATTACHMENT;
import static com.bytechef.component.aitable.constant.FieldType.AUTO_NUMBER;
import static com.bytechef.component.aitable.constant.FieldType.BUTTON;
import static com.bytechef.component.aitable.constant.FieldType.CASCADER;
import static com.bytechef.component.aitable.constant.FieldType.CREATED_BY;
import static com.bytechef.component.aitable.constant.FieldType.CREATED_TIME;
import static com.bytechef.component.aitable.constant.FieldType.FORMULA;
import static com.bytechef.component.aitable.constant.FieldType.LAST_MODIFIED_BY;
import static com.bytechef.component.aitable.constant.FieldType.LAST_MODIFIED_TIME;
import static com.bytechef.component.aitable.constant.FieldType.MAGIC_LOOK_UP;
import static com.bytechef.component.aitable.constant.FieldType.ONE_WAY_LINK;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.aitable.util.AITableUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableDynamicPropertiesProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import java.util.List;

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
        .label("Datasheet ID")
        .description("AITable Datasheet ID")
        .optionsLookupDependsOn(SPACE_ID)
        .options((OptionsFunction<String>) AITableUtils::getDatasheetIdOptions)
        .required(true);

    public static final ModifiableDynamicPropertiesProperty FIELDS_DYNAMIC_PROPERTY = dynamicProperties(FIELDS)
        .propertiesLookupDependsOn(DATASHEET_ID)
        .properties(AITableUtils::createPropertiesForRecord);

    public static final ModifiableStringProperty SPACE_ID_PROPERTY = string(SPACE_ID)
        .label("Space ID")
        .options((OptionsFunction<String>) AITableUtils::getSpaceIdOptions)
        .required(false);

    public static final List<FieldType> NOT_SUPPORTED_FIELD_TYPES = List.of(
        ATTACHMENT, AUTO_NUMBER, BUTTON, CASCADER, CREATED_BY, CREATED_TIME, FORMULA, LAST_MODIFIED_TIME,
        LAST_MODIFIED_BY, MAGIC_LOOK_UP, ONE_WAY_LINK);

    private AITableConstants() {
    }
}
