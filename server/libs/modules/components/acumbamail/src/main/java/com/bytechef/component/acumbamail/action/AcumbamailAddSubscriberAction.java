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

package com.bytechef.component.acumbamail.action;

import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.EMAIL;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.LIST_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.acumbamail.util.AcumbamailUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marija Horvat
 */
public class AcumbamailAddSubscriberAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addSubscriber")
        .title("Add Subscriber")
        .description("Add a subscriber to a list.")
        .properties(
            integer(LIST_ID)
                .label("List Id")
                .description("List identifier.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) AcumbamailUtils::getListsIdOptions)
                .required(true),
            string(EMAIL)
                .label("Email")
                .description("Subscriber email address.")
                .required(true))
        .output()
        .perform(AcumbamailAddSubscriberAction::perform);

    private AcumbamailAddSubscriberAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/addSubscriber/"))
            .queryParameters(
                "auth_token", connectionParameters.getString("access_token")
                    .strip(),
                LIST_ID, inputParameters.getRequiredString(LIST_ID),
                EMAIL, inputParameters.getRequiredString(EMAIL))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
