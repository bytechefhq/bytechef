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

package com.bytechef.component.mailerlite.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.GROUP_ID;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.SUBSCRIBER_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
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
                .label("Subscriber")
                .description("ID of the user that will be added to the selected group.")
                .options((OptionsFunction<String>) MailerLiteUtils::getSubscriberIdOptions)
                .required(true),
            string(GROUP_ID)
                .label("Group ID")
                .description("ID of the group to which the user will be added.")
                .options((OptionsFunction<String>) MailerLiteUtils::getGroupIdOptions)
                .required(true))
        .perform(MailerLiteRemoveSubscriberFromGroupAction::perform);

    private MailerLiteRemoveSubscriberFromGroupAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.delete(
            "/subscribers/%s/groups/%s".formatted(
                inputParameters.getRequiredString(SUBSCRIBER_ID), inputParameters.getRequiredString(GROUP_ID))))
            .execute();

        return null;
    }
}
