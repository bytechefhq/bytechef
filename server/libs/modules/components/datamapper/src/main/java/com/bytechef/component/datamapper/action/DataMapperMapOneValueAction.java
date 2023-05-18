
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
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL;

import java.util.Map;

import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;

import static com.bytechef.hermes.definition.DefinitionDSL.nullable;
import static com.bytechef.hermes.definition.DefinitionDSL.number;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapOneValueAction {

    private static final String MAPPINGS = "mappings";
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String VALUE = "value";
    private static final String DEFAULT_VALUE = "defaultValue";

    public static final ActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapOneValue")
        .title("Map one value")
        .description(
            "The action maps a given value by matching it with the defined mappings, and it returns the outcome of the mapping. In case there is no mapping specified for the value, it returns the default value, and if there is no default defined, it returns null.")
        .properties(
            oneOf(VALUE).types(array(), bool(), number(), object(), string(), nullable())
                .label("Value")
                .description("The value you want to map."),
            oneOf(DEFAULT_VALUE).types(array(), bool(), number(), object(), string(), nullable())
                .label("Default value")
                .description("If no mapping exists, map this value by default."),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "A list of \"mappings\" for the value. If the value specified matches the \"From\", then the connector will return the \"To\" value.")
                .items(
                    object().properties(
                        string(FROM)
                            .label("From"),
                        string(TO)
                            .label("To")))
                .required(true)

        )
        .execute(DataMapperMapOneValueAction::execute);

    protected static Object execute(ActionContext context, Map<String, ?> inputParameters) {
        return null;
    }
}
