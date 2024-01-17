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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_EMPTY_STRINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_NULLS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_UNMAPPED;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.REQUIRED_FIELDS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapObjectsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapObjects")
        .title("Map objects")
        .description("Transform the fields of an object and assign them new keys.")
        .properties(
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(INPUT)
                .label("Input")
                .description("The object containing one or more properties.")
                .displayCondition("type === 1")
                .required(true),
            array(INPUT)
                .label("Input")
                .description("The array containing one or more properties.")
                .displayCondition("type === 2")
                .required(true),
            array(MAPPINGS)
                .label("Mapping")
                .description(
                    "The collection of of \"mappings\"  where the \"From\" key as the key and the value as the key that needs mapping. For nested keys, it supports dot notation, where the new mapped path can be used for nested mapping.")
                .items(
                    object()
                        .properties(
                            string(FROM)
                                .label("From"),
                            string(TO)
                                .label("To")))
                .required(true),
            bool(INCLUDE_UNMAPPED)
                .label("Include Unmapped")
                .description("Should fields from the original object that do not have mappings be included?")
                .defaultValue(true),
            bool(INCLUDE_NULLS)
                .label("Include Nulls")
                .description("Should fields that have null values be included?")
                .defaultValue(true),
            bool(INCLUDE_EMPTY_STRINGS)
                .label("Include empty strings")
                .description("Should fields with empty string values be included?")
                .defaultValue(true),
            array(REQUIRED_FIELDS)
                .label("Required fields")
                .description("A list of fields that are required on the mapped object.")
                .items(string()))
        .outputSchema()
        .perform(DataMapperMapObjectsAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        // TODO
        return null;
    }
}
