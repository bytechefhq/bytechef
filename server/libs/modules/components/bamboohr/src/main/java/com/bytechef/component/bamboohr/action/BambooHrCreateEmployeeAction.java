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

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYEE_NUMBER;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYMENT_STATUS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIRST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.HIRE_DATE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.JOB_TITLE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LAST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LOCATION;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.bamboohr.util.BambooHrUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BambooHrCreateEmployeeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEmployee")
        .title("Create Employee")
        .description("Add a new employee.")
        .properties(
            string(FIRST_NAME)
                .label("First Name")
                .description("The first name of the employee.")
                .required(true),
            string(LAST_NAME)
                .label("Last Name")
                .description("The last name of the employee.")
                .required(true),
            string(EMPLOYEE_NUMBER)
                .label("Employee Number")
                .description("The employee number of the employee.")
                .required(false),
            string(JOB_TITLE)
                .label("Job Title")
                .description("The job title of the employee.")
                .options(BambooHrUtils.getOptions("Job Title"))
                .required(false),
            string(LOCATION)
                .label("Location")
                .description("The employee's current location.")
                .options(BambooHrUtils.getOptions("Location"))
                .required(false),
            string(EMPLOYMENT_STATUS)
                .label("Employee Status")
                .description("The employment status of the employee.")
                .options(BambooHrUtils.getOptions("Employment Status"))
                .required(false),
            date(HIRE_DATE)
                .label("Hire Date")
                .description("The date the employee was hired.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("url")
                            .description("The URL to view the employee in the web app."),
                        string(ID)
                            .description("The ID of the employee."))))
        .perform(BambooHrCreateEmployeeAction::perform);

    private BambooHrCreateEmployeeAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Http.Response response = context
            .http(http -> http.post("/employees/"))
            .body(Http.Body.of(
                FIRST_NAME, inputParameters.getRequiredString(FIRST_NAME),
                LAST_NAME, inputParameters.getRequiredString(LAST_NAME),
                EMPLOYEE_NUMBER, inputParameters.getString(EMPLOYEE_NUMBER),
                JOB_TITLE, inputParameters.getString(JOB_TITLE),
                LOCATION, inputParameters.getString(LOCATION),
                EMPLOYMENT_STATUS, inputParameters.getString(EMPLOYMENT_STATUS),
                HIRE_DATE, inputParameters.getString(HIRE_DATE)))
            .execute();

        List<String> location = response.getHeader("location");

        if (location != null && !location.isEmpty()) {
            String url = location.getFirst();
            String id = url.substring(url.lastIndexOf("/") + 1);

            return Map.of("url", url, ID, id);
        }

        return null;
    }
}
