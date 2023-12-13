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

package com.bytechef.component.datamapper.action;

import static com.bytechef.component.datamapper.constant.DataMapperConstants.FIELD_KEY;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.VALUE_KEY;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaResponse;
import com.bytechef.hermes.component.definition.ParameterMap;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapObjectsToListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapObjectsToList")
        .title("Map objects to list")
        .description("Transform an object or array of objects into an array of key-value pairs.")
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
                .displayCondition("type === 1")
                .required(true),
            string(FIELD_KEY)
                .label("Field key")
                .description("The key name to which keys should be mapped."),
            string(VALUE_KEY)
                .label("Value key")
                .description("The key name to which values should be mapped."))
        .outputSchema(getOutputSchemaFunction())
        .perform(DataMapperMapObjectsToListAction::perform);

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        // TODO
        return null;
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (inputParameters, connection, context) -> {
            if (inputParameters.getInteger(TYPE, 1) == 1) {
                return new OutputSchemaResponse(object());
            } else {
                return new OutputSchemaResponse(array());
            }
        };
    }
}
