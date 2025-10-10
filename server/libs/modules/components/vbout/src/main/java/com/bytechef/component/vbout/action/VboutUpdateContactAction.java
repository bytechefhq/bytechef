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

package com.bytechef.component.vbout.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vbout.constant.VboutConstants.EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.ID;
import static com.bytechef.component.vbout.constant.VboutConstants.LIST_ID;
import static com.bytechef.component.vbout.constant.VboutConstants.STATUS;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.vbout.util.VboutUtils;

/**
 * @author Marija Horvat
 */
public class VboutUpdateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateContact")
        .title("Update Contact")
        .description("Updates a contact in a selected email list.")
        .properties(
            string(LIST_ID)
                .label("List ID")
                .description("The ID of the list with contact.")
                .options((OptionsFunction<String>) VboutUtils::getListIdOptions)
                .required(true),
            string(ID)
                .label("Contact ID")
                .description("The ID of the contact.")
                .optionsLookupDependsOn(LIST_ID)
                .options((OptionsFunction<String>) VboutUtils::getContactIdOptions)
                .required(true),
            string(EMAIL)
                .label("Email")
                .description("The updated email of the contact.")
                .required(false),
            string(STATUS)
                .label("Status")
                .description("The updated status of the contact.")
                .options(
                    option("Active", "active"),
                    option("Disactive", "disactive"))
                .required(false))
        .perform(VboutUpdateContactAction::perform);

    private VboutUpdateContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.post("/emailMarketing/EditContact"))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(
                ID, inputParameters.getRequiredString(ID),
                EMAIL, inputParameters.getString(EMAIL),
                STATUS, inputParameters.getString(STATUS))
            .execute();

        return null;
    }
}
