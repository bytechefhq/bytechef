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

package com.bytechef.component.greenhouse.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.APPLICATION_ID;
import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.APPLICATION_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.greenhouse.util.GreenhouseUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GreenhouseGetApplicationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getApplication")
        .title("Get Application")
        .description("Get an application by ID, including its status, current stage, job and answers.")
        .properties(
            string(APPLICATION_ID)
                .label("Application ID")
                .description("The ID of the application to get.")
                .required(true))
        .output(outputSchema(APPLICATION_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/greenhouse_v1#get-application")
        .perform(GreenhouseGetApplicationAction::perform);

    private GreenhouseGetApplicationAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return GreenhouseUtils.getRecordById("/applications", inputParameters.getRequiredString(APPLICATION_ID),
            "application", context);
    }
}
