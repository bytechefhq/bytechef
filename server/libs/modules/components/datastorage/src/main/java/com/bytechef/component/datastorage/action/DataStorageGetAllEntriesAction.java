
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

package com.bytechef.component.datastorage.action;

import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;

import java.util.Map;

import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.any;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class DataStorageGetAllEntriesAction {

    public static final ActionDefinition ACTION_DEFINITION = action("getAllEntries")
        .title("Get All Entries(Keys and Values)")
        .description(
            "Retrieve all the currently existing keys from storage, along with their values within the provided scope.")
        .properties(
            integer(SCOPE)
                .label("Scope")
                .description("The namespace to get keys from.")
                .options(SCOPE_OPTIONS)
                .required(true))
        .outputSchema(
            getOutputSchemaFunction(),
            object()
                .properties(
                    array("item")
                        .items(
                            dateTime("created"),
                            string("key"),
                            any("key"))))
        .execute(DataStorageGetAllEntriesAction::execute);

    protected static Object execute(ActionContext actionContext, Map<String, ?> inputParameters) {
        // TODO

        return null;
    }

    protected static OutputSchemaDataSource.OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }
}
