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

import static com.bytechef.component.bambooHR.constant.BambooHRConstants.EMPLOYMENT_STATUS;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.FIRST_NAME;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.HIRE_DATE;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.ID;
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

/**
 * @author Marija Horvat
 */
public class BambooHRUpdateEmployeeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateEmployee")
        .title("Update Employee")
        .description("Update an employee, based on employee ID.")
        .properties(
            string(ID)
                .label("Employee ID")
                .description("The ID of the employee.")
                .required(true),
            string(FIRST_NAME)
                .label("Updated First Name")
                .description("The updated first name of the employee.")
                .required(false),
            string(LAST_NAME)
                .label("Updated Last Name")
                .description("The updated last name of the employee.")
                .required(false),
            string(JOB_TITLE)
                .label("Updated Job Title")
                .description("The updated job title of the employee.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BambooHRUtils::getJobTitleOptions)
                .required(false),
            string(LOCATION)
                .label("Updated Location")
                .description("The updated employee's current location.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BambooHRUtils::getLocationOptions)
                .required(false),
            string(EMPLOYMENT_STATUS)
                .label("Updated Employee Status")
                .description("The updated employment status of the employee.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BambooHRUtils::getEmploymentStatusOptions)
                .required(false),
            date(HIRE_DATE)
                .label("Updated Hire Date")
                .description("The updated date the employee was hired.")
                .required(false))
        .perform(BambooHRUpdateEmployeeAction::perform);

    private BambooHRUpdateEmployeeAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post("/employees/" + inputParameters.getRequiredString(ID)))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .body(Context.Http.Body.of(
                FIRST_NAME, inputParameters.getString(FIRST_NAME),
                LAST_NAME, inputParameters.getString(LAST_NAME),
                JOB_TITLE, inputParameters.getString(JOB_TITLE),
                LOCATION, inputParameters.getString(LOCATION),
                EMPLOYMENT_STATUS, inputParameters.getString(EMPLOYMENT_STATUS),
                HIRE_DATE, inputParameters.getString(HIRE_DATE)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
