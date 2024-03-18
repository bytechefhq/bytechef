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

package com.bytechef.component.freshsales.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.CREATE_ACCOUNT;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.PHONE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.WEBSITE;
import static com.bytechef.component.freshsales.util.FreshsalesUtils.getHeaders;
import static com.bytechef.component.freshsales.util.FreshsalesUtils.getUrl;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;

/**
 * @author Monika Domiter
 */
public class FreshsalesCreateAccountAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_ACCOUNT)
        .title("Create account")
        .description("Creates a new account")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the account")
                .required(true),
            string(WEBSITE)
                .label("Website")
                .description("Website of the account")
                .controlType(Property.ControlType.URL)
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("Phone number of the account")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    number("id"),
                    string(NAME),
                    string(WEBSITE),
                    string(PHONE)))
        .perform(FreshsalesCreateAccountAction::perform);

    private FreshsalesCreateAccountAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(getUrl(connectionParameters, "sales_accounts")))
            .headers(getHeaders(connectionParameters))
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
