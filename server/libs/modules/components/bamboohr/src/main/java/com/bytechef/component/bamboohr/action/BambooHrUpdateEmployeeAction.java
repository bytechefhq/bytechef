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

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYMENT_STATUS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIRST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.HIRE_DATE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.JOB_TITLE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LAST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LOCATION;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.bamboohr.util.BambooHrUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class BambooHrUpdateEmployeeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateEmployee")
        .title("Update Employee")
        .description("Update an employee, based on employee ID.")
        .properties(
            string(ID)
                .label("Employee ID")
                .description("The ID of the employee.")
                .options((OptionsFunction<String>) BambooHrUtils::getEmployeeIdOptions)
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
                .options(BambooHrUtils.getOptions("Job Title"))
                .required(false),
            string(LOCATION)
                .label("Updated Location")
                .description("The updated employee's current location.")
                .options(BambooHrUtils.getOptions("Location"))
                .required(false),
            string(EMPLOYMENT_STATUS)
                .label("Updated Employee Status")
                .description("The updated employment status of the employee.")
                .options(BambooHrUtils.getOptions("Employment Status"))
                .required(false),
            date(HIRE_DATE)
                .label("Updated Hire Date")
                .description("The updated date the employee was hired.")
                .required(false))
        .perform(BambooHrUpdateEmployeeAction::perform);

    private BambooHrUpdateEmployeeAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context
            .http(http -> http.post("/employees/" + inputParameters.getRequiredString(ID)))
            .body(
                Http.Body.of(
                    FIRST_NAME, inputParameters.getString(FIRST_NAME),
                    LAST_NAME, inputParameters.getString(LAST_NAME),
                    JOB_TITLE, inputParameters.getString(JOB_TITLE),
                    LOCATION, inputParameters.getString(LOCATION),
                    EMPLOYMENT_STATUS, inputParameters.getString(EMPLOYMENT_STATUS),
                    HIRE_DATE, inputParameters.getString(HIRE_DATE)))
            .execute();

        return null;
    }
}
