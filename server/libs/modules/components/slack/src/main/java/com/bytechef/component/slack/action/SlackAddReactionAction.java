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

package com.bytechef.component.slack.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.ERROR;
import static com.bytechef.component.slack.constant.SlackConstants.NAME;
import static com.bytechef.component.slack.constant.SlackConstants.OK;
import static com.bytechef.component.slack.constant.SlackConstants.TIMESTAMP;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.slack.util.SlackUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class SlackAddReactionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addReaction")
        .title("Add Reaction")
        .description("Adds a reaction to a message.")
        .properties(
            string(CHANNEL)
                .label("Channel ID")
                .description("ID of the channel, private group, or IM channel where the message is located.")
                .options((OptionsFunction<String>) SlackUtils::getChannelIdOptions)
                .required(true),
            string(NAME)
                .label("Emoji Name")
                .description("Reaction (emoji) name to add.")
                .exampleValue("thumbsup")
                .required(true),
            string(TIMESTAMP)
                .label("Timestamp")
                .description("Timestamp of the message to add reaction to.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool(OK)
                            .description("Indicates if the reaction was successfully added."),
                        string("warning"),
                        object("responseMetadata")
                            .properties(
                                array("messages")
                                    .items(string())))))
        .perform(SlackAddReactionAction::perform);

    private SlackAddReactionAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, Object> body = actionContext
            .http(http -> http.post("/reactions.add"))
            .body(
                Http.Body.of(
                    CHANNEL, inputParameters.getRequiredString(CHANNEL),
                    NAME, inputParameters.getRequiredString(NAME),
                    TIMESTAMP, inputParameters.getRequiredString(TIMESTAMP)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if ((boolean) body.get(OK)) {
            return body;
        } else {
            throw new ProviderException((String) body.get(ERROR));
        }
    }
}
