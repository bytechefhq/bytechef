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

package com.bytechef.component.rocketchat.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.POST_MESSAGE_RESPONSE_PROPERTY;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.ROOM_ID;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.TEXT;
import static com.bytechef.component.rocketchat.util.RocketchatUtils.sendMessage;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.rocketchat.util.RocketchatUtils;

/**
 * @author Marija Horvat
 */
public class RocketchatSendDirectMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendDirectMessage")
        .title("Send Direct Message")
        .description("Send messages to users on your workspace.")
        .properties(
            string(ROOM_ID)
                .label("Username")
                .description("Username to send the direct message to.")
                .options((OptionsFunction<String>) RocketchatUtils::getUsersOptions)
                .required(true),
            string(TEXT)
                .label("Message")
                .description("The message to send.")
                .required(true))
        .output(outputSchema(POST_MESSAGE_RESPONSE_PROPERTY))
        .perform(RocketchatSendDirectMessageAction::perform);

    private RocketchatSendDirectMessageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return sendMessage(
            "@" + inputParameters.getRequiredString(ROOM_ID), inputParameters.getRequiredString(TEXT), context);
    }
}
