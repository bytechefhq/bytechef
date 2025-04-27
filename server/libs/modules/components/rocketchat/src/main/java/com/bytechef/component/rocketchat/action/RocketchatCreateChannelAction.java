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

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.CHANNEL_RESPONSE_PROPERTY;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.EXCLUDE_SELF;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.MEMBERS;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.NAME;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.READ_ONLY;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.rocketchat.util.RocketchatUtils;

/**
 * @author Marija Horvat
 */
public class RocketchatCreateChannelAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createChannel")
        .title("Create Channel")
        .description("Create a public channel.")
        .properties(
            string(NAME)
                .label("Channel Name")
                .description("The name of the channel.")
                .required(true),
            array(MEMBERS)
                .label("Members")
                .description("An array of the users to be added to the channel when it is created.")
                .items(string())
                .options((ActionOptionsFunction<String>) RocketchatUtils::getUsersOptions)
                .required(false),
            bool(READ_ONLY)
                .label("Read Only")
                .description("Set if the channel is read only or not.")
                .defaultValue(false)
                .required(false),
            bool(EXCLUDE_SELF)
                .label("Exclude Self")
                .description(
                    "If set to true, the user calling the endpoint is not automatically added as a member of the channel.")
                .defaultValue(false)
                .required(false))
        .output(outputSchema(CHANNEL_RESPONSE_PROPERTY))
        .perform(RocketchatCreateChannelAction::perform);

    private RocketchatCreateChannelAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post("/channels.create"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    MEMBERS, inputParameters.getList(MEMBERS),
                    READ_ONLY, inputParameters.getBoolean(READ_ONLY),
                    EXCLUDE_SELF, inputParameters.getBoolean(EXCLUDE_SELF)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
