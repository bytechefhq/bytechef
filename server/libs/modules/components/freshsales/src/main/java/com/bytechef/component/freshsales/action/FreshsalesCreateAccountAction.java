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

package com.bytechef.component.freshsales.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.ID;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.PHONE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.WEBSITE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Monika Domiter
 */
public class FreshsalesCreateAccountAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createAccount")
        .title("Create Account")
        .description("Creates a new account.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the account.")
                .required(true),
            string(WEBSITE)
                .label("Website")
                .description("Website of the account.")
                .controlType(Property.ControlType.URL)
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("Phone number of the account.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("sales_account")
                            .properties(
                                number(ID)
                                    .description("ID of the account."),
                                string(NAME)
                                    .description("Name of the account."),
                                string(WEBSITE)
                                    .description("Website of the account."),
                                string(PHONE)
                                    .description("Website of the account.")))))
        .perform(FreshsalesCreateAccountAction::perform);

    private FreshsalesCreateAccountAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post("/sales_accounts"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    WEBSITE, inputParameters.getString(WEBSITE),
                    PHONE, inputParameters.getString(PHONE)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
