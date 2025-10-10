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

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.CATEGORY_ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FILE_ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.SHARE_WITH_EMPLOYEE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
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
public class BambooHrUpdateEmployeeFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateEmployeeFile")
        .title("Update Employee File")
        .description("Update an employee file.")
        .properties(
            string(ID)
                .label("Employee ID")
                .description("The ID of the employee.")
                .options((OptionsFunction<String>) BambooHrUtils::getEmployeeIdOptions)
                .required(true),
            string(FILE_ID)
                .label("File ID")
                .description("The ID of the employee file being updated.")
                .options((OptionsFunction<String>) BambooHrUtils::getEmployeeFilesIdOptions)
                .optionsLookupDependsOn(ID)
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
        .perform(BambooHrUpdateEmployeeFileAction::perform);

    private BambooHrUpdateEmployeeFileAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context
            .http(http -> http.post(
                "/employees/" + inputParameters.getRequiredString(ID) + "/files/" +
                    inputParameters.getRequiredString(FILE_ID)))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getString(NAME),
                    CATEGORY_ID, inputParameters.getString(CATEGORY_ID),
                    SHARE_WITH_EMPLOYEE, inputParameters.getString(SHARE_WITH_EMPLOYEE)))
            .execute();

        return null;
    }
}
