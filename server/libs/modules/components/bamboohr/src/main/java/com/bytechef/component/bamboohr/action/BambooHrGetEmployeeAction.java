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

package com.bytechef.component.bamboohr.action;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIELDS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.bamboohr.util.BambooHrUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;

/**
 * @author Marija Horvat
 */
public class BambooHrGetEmployeeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getEmployee")
        .title("Get Employee")
        .description("Get employee data, based on employee ID.")
        .properties(
            string(ID)
                .label("Employee ID")
                .description("The ID of the employee.")
                .options((OptionsFunction<String>) BambooHrUtils::getEmployeeIdOptions)
                .required(true),
            array(FIELDS)
                .description("Fields you want to get from employee. See documentation for available fields.")
                .items(string())
                .options((OptionsFunction<String>) BambooHrUtils::getFieldOptions)
                .required(true))
        .output()
        .perform(BambooHrGetEmployeeAction::perform);

    private BambooHrGetEmployeeAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<String> fields = inputParameters.getRequiredList(FIELDS, String.class);
        String queryParameter = String.join(",", fields);

        return context
            .http(http -> http.get("/employees/" + inputParameters.getRequiredString(ID)))
            .queryParameter(FIELDS, queryParameter)
            .header("accept", "application/json")
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
