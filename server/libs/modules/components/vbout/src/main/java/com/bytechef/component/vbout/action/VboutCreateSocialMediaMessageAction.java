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

package com.bytechef.component.vbout.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vbout.constant.VboutConstants.CHANNEL;
import static com.bytechef.component.vbout.constant.VboutConstants.CHANNEL_ID;
import static com.bytechef.component.vbout.constant.VboutConstants.MESSAGE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.vbout.util.VboutUtils;

/**
 * @author Marija Horvat
 */
public class VboutCreateSocialMediaMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSocialMediaMessage")
        .title("Create Social Media Message")
        .description("Post a message to one of your social media channel.")
        .properties(
            string(MESSAGE)
                .label("Message")
                .description("The post message to be sent.")
                .required(true),
            string(CHANNEL)
                .label("Channel")
                .description("The channel which the post will be sent to.")
                .options(
                    option("Facebook", "facebook"),
                    option("Twitter", "twitter"),
                    option("Linkedin", "linkedin"))
                .required(true),
            string(CHANNEL_ID)
                .label("Social Media Account")
                .description("The social media account which will create the post.")
                .optionsLookupDependsOn(CHANNEL)
                .options((OptionsFunction<String>) VboutUtils::getChannelIdOptions)
                .required(true))
        .perform(VboutCreateSocialMediaMessageAction::perform);

    private VboutCreateSocialMediaMessageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.post("/socialMedia/AddPost"))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(
                MESSAGE, inputParameters.getRequiredString(MESSAGE),
                CHANNEL, inputParameters.getRequiredString(CHANNEL),
                CHANNEL_ID, inputParameters.getRequiredString(CHANNEL_ID))
            .execute();

        return null;
    }
}
