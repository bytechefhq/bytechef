
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
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;

import java.util.Map;

import static com.bytechef.component.datamapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.TO;
import static com.bytechef.hermes.definition.DefinitionDSL.array;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapKeysAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapKeys")
        .title("Map Keys")
        .description(
            "The action maps values of a given object to fresh keys and returns a new object containing those newly mapped keys.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("Object containing one or more properties.")
                .required(true),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "The collection of of \"mappings\" where \"From\" refers to a particular key from the Input, while \"To\" represents the name of a new key that is assigned the corresponding value of the \"From\" key.")
                .items(
                    object().properties(
                        string(FROM)
                            .label("From"),
                        string(TO)
                            .label("To")))
                .required(true))
        .outputSchema(getOutputSchemaFunction(), object())
        .perform(DataMapperMapKeysAction::perform);

    protected static Object perform(Map<String, ?> inputParameters, ActionContext context) {
        // TODO
        return null;
    }

    protected static OutputSchemaDataSource.OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }
}
