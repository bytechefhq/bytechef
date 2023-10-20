
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
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;

import java.util.Map;

import static com.bytechef.component.datamapper.constant.DataMapperConstants.FIELD_KEY;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.FIELD_VALUE;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.hermes.definition.DefinitionDSL.array;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapListToObjectAction {

    public static final ActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapListToObject")
        .title("Map list to object")
        .description("Map an array of key value objects to a single object.")
        .properties(
            array(INPUT)
                .label("Input")
                .description("An array of key value objects.")
                .required(true),
            string(FIELD_KEY)
                .label("Field Key")
                .description("The value's key will serve as the key in the newly created object.")
                .required(true),
            string(FIELD_VALUE)
                .label("Field Key")
                .description("The value of the key will become the value in the new object.")
                .required(true))
        .outputSchema(getOutputSchemaFunction(), object())
        .execute(DataMapperMapListToObjectAction::execute);

    protected static Object execute(ActionContext context, Map<String, ?> inputParameters) {
        // TODO
        return null;
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }
}
