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

import static com.bytechef.component.bambooHR.constant.BambooHRConstants.EMPLOYEE_NUMBER;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.EMPLOYMENT_STATUS;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.FIRST_NAME;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.HIRE_DATE;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.JOB_TITLE;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.LAST_NAME;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.LOCATION;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.bambooHR.util.BambooHRUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

public class BambooHRCreateEmployeeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEmployee")
        .title("Create Employee")
        .description("Add a new employee.")
        .properties(
            string(EMPLOYEE_NUMBER)
                .label("Employee Number")
                .description("The employee number of the employee.")
                .required(true),
            string(FIRST_NAME)
                .label("First Name")
                .description("The first name of the employee.")
                .required(true),
            string(LAST_NAME)
                .label("Last Name")
                .description("The last name of the employee.")
                .required(true),
            string(JOB_TITLE)
                .label("Job Title")
                .description("The job title of the employee.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BambooHRUtils::getJobTitleOptions)
                .required(false),
            string(LOCATION)
                .label("Location")
                .description("The employee's current location.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BambooHRUtils::getLocationOptions)
                .required(false),
            string(EMPLOYMENT_STATUS)
                .label("Employee Status")
                .description("The employment status of the employee.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BambooHRUtils::getEmploymentStatusOptions)
                .required(false),
            date(HIRE_DATE)
                .label("Hire Date")
                .description("The date the employee was hired.")
                .required(false))
        .perform(BambooHRCreateEmployeeAction::perform);

    private BambooHRCreateEmployeeAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post("/employees/"))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .body(Context.Http.Body.of(
                EMPLOYEE_NUMBER, inputParameters.getRequiredString(EMPLOYEE_NUMBER),
                FIRST_NAME, inputParameters.getRequiredString(FIRST_NAME),
                LAST_NAME, inputParameters.getRequiredString(LAST_NAME),
                JOB_TITLE, inputParameters.getString(JOB_TITLE),
                LOCATION, inputParameters.getString(LOCATION),
                EMPLOYMENT_STATUS, inputParameters.getString(EMPLOYMENT_STATUS),
                HIRE_DATE, inputParameters.getString(HIRE_DATE)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
