
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

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapObjectsToListAction {

    private static final String INPUT = "input";
    private static final String FIELD_KEY = "fieldKey";
    private static final String VALUE_KEY = "valueKey";

    public static final ActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapObjectsToList")
        .title("Map objects to list")
        .description("Transform an object or an array of objects into an array of key-value pairs.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("Object containing one or more properties.")
                .required(true),
            string(FIELD_KEY)
                .label("Field key")
                .description("The name of the key to map keys to."),
            string(VALUE_KEY)
                .label("Value key")
                .description("The name of the key to map keys to."))
        .execute(DataMapperMapObjectsToListAction::execute);

    protected static Object execute(ActionContext context, InputParameters inputParameters) {
        return null;
    }
}
