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

package com.bytechef.component.intercom.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.intercom.constant.IntercomConstants.CONTACT_OUTPUT_PROPERTY;
import static com.bytechef.component.intercom.constant.IntercomConstants.ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.intercom.util.IntercomUtils;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class IntercomGetContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getContact")
        .title("Get Contact")
        .description("Get a single Contact")
        .properties(
            string(ID)
                .label("Contact ID")
                .required(true)
                .options((OptionsFunction<String>) IntercomUtils::getContactIdOptions))
        .output(outputSchema(CONTACT_OUTPUT_PROPERTY))
        .perform(IntercomGetContactAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get("/contacts/" + inputParameters.getRequiredString(ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private IntercomGetContactAction() {
    }
}
