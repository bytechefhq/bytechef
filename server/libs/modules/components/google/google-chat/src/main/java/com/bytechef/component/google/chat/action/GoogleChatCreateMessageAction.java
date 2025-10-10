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

package com.bytechef.component.google.chat.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.chat.constant.GoogleChatConstants.SPACE;
import static com.bytechef.component.google.chat.constant.GoogleChatConstants.TEXT;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.chat.util.GoogleChatUtils;
import com.bytechef.google.commons.GoogleUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleChatCreateMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createMessage")
        .title("Create Message")
        .description("Creates a new message in selected space.")
        .properties(
            string(SPACE)
                .label("Space")
                .description("Space in which the message will be created.")
                .options((OptionsFunction<String>) GoogleChatUtils::getSpaceOptions)
                .required(true),
            string(TEXT)
                .label("Message Text")
                .description("Text of the message.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("name")
                            .description("Name of the message that was created."),
                        object("sender")
                            .description("Sender of the message.")
                            .properties(
                                string("name")
                                    .description("Name of the sender of the message."),
                                string("type")
                                    .description("Type of the sender of the message (BOT or HUMAN).")),
                        string("createTime")
                            .description("Time when the message was created."),
                        string("text")
                            .description("Text of the message."),
                        object("thread")
                            .properties(
                                string("name")
                                    .description("Thread to which the message was sent.")),
                        object("space")
                            .properties(
                                string("name")
                                    .description("Space to which the message was sent.")),
                        string("argumentText"),
                        string("formattedText"))))
        .perform(GoogleChatCreateMessageAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleChatCreateMessageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "https://chat.googleapis.com/v1/" + inputParameters.getRequiredString(SPACE) + "/messages"))
            .configuration(responseType(Http.ResponseType.JSON))
            .body(
                Body.of(
                    Map.of(
                        TEXT, inputParameters.getRequiredString(TEXT))))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
