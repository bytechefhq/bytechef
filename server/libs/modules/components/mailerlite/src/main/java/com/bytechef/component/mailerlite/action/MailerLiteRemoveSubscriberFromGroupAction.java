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

package com.bytechef.component.mailerlite.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.GROUP_ID;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.SUBSCRIBER_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.mailerlite.util.MailerLiteUtils;

/**
 * @author Nikolina Spehar
 */
public class MailerLiteRemoveSubscriberFromGroupAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("removeSubscriberFromGroup")
        .title("Remove Subscriber from Group")
        .description("Remove selected subscriber from the group.")
        .properties(
            string(SUBSCRIBER_ID)
                .label("Subscriber Email")
                .description("User that will be added to the selected group.")
                .options((ActionOptionsFunction<String>) MailerLiteUtils::getSubscribers)
                .required(true),
            string(GROUP_ID)
                .label("Group")
                .description("Group to which the user will be added.")
                .options((ActionOptionsFunction<String>) MailerLiteUtils::getGroups)
                .required(true))
        .perform(MailerLiteRemoveSubscriberFromGroupAction::perform);

    private MailerLiteRemoveSubscriberFromGroupAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.delete(
            "/subscribers/" + inputParameters.getRequiredString(SUBSCRIBER_ID) + "/groups/"
                + inputParameters.getRequiredString(GROUP_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
