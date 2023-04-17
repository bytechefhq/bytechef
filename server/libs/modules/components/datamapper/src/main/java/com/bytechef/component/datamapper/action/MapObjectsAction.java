
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.datamapper.action;

import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL;

import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class MapObjectsAction {

    private static final String INPUT = "input";
    private static final String MAPPING = "mapping";
    private static final String INCLUDE_UNMAPPED = "includeUnmapped";
    private static final String INCLUDE_NULLS = "includeNulls";
    private static final String INCLUDE_EMPTY_STRINGS = "includeEmptyStrings";
    private static final String REQUIRED_FIELDS = "requiredFields";

    public static final ActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapObjects")
        .display(display("Map objects").description("Map object fields to new keys."))
        .properties(
            object(INPUT)
                .label("Input")
                .description("Object containing one or more properties.")
                .required(true),
            object(MAPPING)
                .label("Mapping")
                .description(
                    "An object that consists of key-value pairs defines the source key as the key and the value as the key that needs mapping. For nested keys, it supports dot notation, where the new mapped path can be used for nested mapping.")
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
        .execute(MapObjectsAction::execute);

    protected static Object execute(ActionContext context, InputParameters inputParameters) {
        return null;
    }
}
