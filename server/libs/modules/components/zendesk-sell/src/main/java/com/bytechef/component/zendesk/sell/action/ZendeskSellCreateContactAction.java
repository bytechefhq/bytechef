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

package com.bytechef.component.zendesk.sell.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.BASE_URL;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.CREATE_CONTACT;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.DATA;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.EMAIL;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.FIRST_NAME;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.IS_ORGANIZATION;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.LAST_NAME;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.NAME;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.TITLE;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.WEBSITE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Monika Domiter
 */
public class ZendeskSellCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CONTACT)
        .title("Create contact")
        .description("Creates new contact. A contact may represent a single individual or an organization.")
        .properties(
            bool(IS_ORGANIZATION)
                .label("Is contact represent an organization?")
                .description("Is contact represent an organization or a single individual?")
                .required(true),
            string(NAME)
                .label("Name")
                .description("The name of the organisation.")
                .displayCondition("%s == true".formatted(IS_ORGANIZATION))
                .required(false),
            string(FIRST_NAME)
                .label("First name")
                .description("The first name of the person.")
                .displayCondition("%s == false".formatted(IS_ORGANIZATION))
                .required(false),
            string(LAST_NAME)
                .label("Last name")
                .description("The last name of the person.")
                .displayCondition("%s == false".formatted(IS_ORGANIZATION))
                .required(true),
            string(TITLE)
                .label("Title")
                .required(false),
            string(WEBSITE)
                .label("Website")
                .required(false),
            string(EMAIL)
                .label("Email")
                .controlType(ControlType.EMAIL)
                .required(false))
        .outputSchema(
            object()
                .properties(
                    object(DATA)
                        .properties(
                            string("id"),
                            bool(IS_ORGANIZATION),
                            string(TITLE),
                            string(WEBSITE),
                            string(EMAIL)),
                    object("meta")
                        .properties(
                            integer("version"),
                            string("type"))))
        .perform(ZendeskSellCreateContactAction::perform);

    private ZendeskSellCreateContactAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/contacts"))
            .body(
                Http.Body.of(
                    DATA,
                    new Object[] {
                        IS_ORGANIZATION, inputParameters.getRequiredBoolean(IS_ORGANIZATION),
                        NAME, inputParameters.getString(NAME),
                        FIRST_NAME, inputParameters.getString(FIRST_NAME),
                        LAST_NAME, inputParameters.getString(LAST_NAME),
                        TITLE, inputParameters.getString(TITLE),
                        WEBSITE, inputParameters.getString(WEBSITE),
                        EMAIL, inputParameters.getString(EMAIL),
                    }))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
