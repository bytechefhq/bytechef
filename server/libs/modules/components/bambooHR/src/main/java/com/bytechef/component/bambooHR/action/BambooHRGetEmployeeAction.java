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

package com.bytechef.component.bambooHR.action;

import static com.bytechef.component.bambooHR.constant.BambooHRConstants.ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BambooHRGetEmployeeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getEmployee")
        .title("Get Employee")
        .description("Get employee data, based on employee ID.")
        .properties(
            string(ID)
                .label("Employee ID")
                .description("The ID of the employee.")
                .required(true),
            array("fields")
                .description("Fields you want to get from employee. See documentation for available fields.")
                .items(string())
                .required(true))
        .output()
        .perform(BambooHRGetEmployeeAction::perform);

    private BambooHRGetEmployeeAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object[] inputArray = inputParameters.getArray("fields");
        StringBuilder queryParameter = new StringBuilder();
        for (int i = 0; i < inputArray.length; i++) {
            queryParameter.append(inputArray[i]);
            if (i != inputArray.length - 1) {
                queryParameter.append(",");
            }
        }

        return context
            .http(http -> http.get("/employees/" + inputParameters.getRequiredString(ID)))
            .queryParameters(Map.of("fields", List.of(queryParameter.toString())))
            .headers(Map.of("accept", List.of("application/json")))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
