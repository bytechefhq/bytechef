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

import static com.bytechef.component.bambooHR.constant.BambooHRConstants.CATEGORY_ID;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.FILE_ID;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.ID;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.NAME;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.SHARE_WITH_EMPLOYEE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marija Horvat
 */
public class BambooHRUpdateEmployeeFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateEmployeeFile")
        .title("Update Employee File")
        .description("Update an employee file.")
        .properties(
            string(ID)
                .label("Employee ID")
                .description("The ID of the employee.")
                .required(true),
            string(FILE_ID)
                .label("File ID")
                .description("The ID of the employee file being updated.")
                .required(true),
            string(NAME)
                .label("Updated Name Of The File")
                .description("Use if you want to rename the file.")
                .required(false),
            string(CATEGORY_ID)
                .label("Updated Category ID")
                .description("Use if you want to move the file to a different category.")
                .required(false),
            bool(SHARE_WITH_EMPLOYEE)
                .label("Update Sharing The File")
                .description("Use if you want to update whether this file is shared or not.")
                .required(false))
        .perform(BambooHRUpdateEmployeeFileAction::perform);

    private BambooHRUpdateEmployeeFileAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post(
                "/employees/" + inputParameters.getRequiredString(ID)
                    + "/files/" + inputParameters.getRequiredString(FILE_ID)))
            .header("accept", "application/json")
            .configuration(responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getString(NAME),
                    CATEGORY_ID, inputParameters.getString(CATEGORY_ID),
                    SHARE_WITH_EMPLOYEE, inputParameters.getString(SHARE_WITH_EMPLOYEE)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
