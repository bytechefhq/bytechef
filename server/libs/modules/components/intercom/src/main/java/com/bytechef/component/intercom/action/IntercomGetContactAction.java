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

package com.bytechef.component.intercom.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.intercom.constant.IntercomConstants.CONTACT_NAME;
import static com.bytechef.component.intercom.constant.IntercomConstants.EMAIL;
import static com.bytechef.component.intercom.constant.IntercomConstants.GET_CONTACT;
import static com.bytechef.component.intercom.constant.IntercomConstants.NAME;
import static com.bytechef.component.intercom.constant.IntercomConstants.PHONE;
import static com.bytechef.component.intercom.constant.IntercomConstants.ROLE;
import static com.bytechef.component.intercom.constant.IntercomConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.intercom.util.IntercomUtils;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class IntercomGetContactAction {

    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action(GET_CONTACT)
        .title("Get Contact")
        .description("Get a single Contact")
        .properties(
            string(CONTACT_NAME)
                .label("Contact Name")
                .required(true)
                .options((ActionOptionsFunction<String>) IntercomUtils::getContactIdOptions))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(TYPE),
                        string(CONTACT_NAME),
                        string(ROLE),
                        string(EMAIL),
                        string(PHONE),
                        string(NAME))))
        .perform(IntercomGetContactAction::perform);

    public static Object
        perform(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.get("/contacts/" + inputParameters.getRequiredString(CONTACT_NAME)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private IntercomGetContactAction() {
    }
}
