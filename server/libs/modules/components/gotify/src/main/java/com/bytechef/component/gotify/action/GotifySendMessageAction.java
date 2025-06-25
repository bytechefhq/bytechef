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

package com.bytechef.component.gotify.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.gotify.constant.GotifyConstants.EXTRAS;
import static com.bytechef.component.gotify.constant.GotifyConstants.EXTRA_INFO_KEY;
import static com.bytechef.component.gotify.constant.GotifyConstants.EXTRA_INFO_VALUE;
import static com.bytechef.component.gotify.constant.GotifyConstants.MESSAGE;
import static com.bytechef.component.gotify.constant.GotifyConstants.PRIORITY;
import static com.bytechef.component.gotify.constant.GotifyConstants.SUB_NAMESPACE;
import static com.bytechef.component.gotify.constant.GotifyConstants.TITLE;
import static com.bytechef.component.gotify.constant.GotifyConstants.TOP_NAMESPACE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GotifySendMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendMessage")
        .title("Send Message")
        .description("Sends a message to the server.")
        .properties(
            string(TITLE)
                .label("Message Title")
                .description("The title of the message that will be sent.")
                .required(false),
            string(MESSAGE)
                .label("Message Content")
                .description("The content message that will be sent.")
                .required(true),
            integer(PRIORITY)
                .label("Priority")
                .description(
                    "The priority of the message. If unset, then the default priority of the application will be used.")
                .required(false),
            array(EXTRAS)
                .label("Extras")
                .description("The extra data sent along the message.")
                .required(false)
                .items(
                    object("extra")
                        .label("Extra")
                        .properties(
                            string(TOP_NAMESPACE)
                                .label("Top Namespace")
                                .description("The top namespace of the message extra information.")
                                .required(true),
                            string(SUB_NAMESPACE)
                                .label("Sub Namespace")
                                .description("The sub namespace of the message extra information.")
                                .required(true),
                            string(EXTRA_INFO_KEY)
                                .label("Extra Information Key")
                                .description("Key of the extra information.")
                                .required(true),
                            string(EXTRA_INFO_VALUE)
                                .label("Extra Information Value")
                                .description("The extra data sent along the message.")
                                .required(true))))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("id")
                            .description("ID of the message that was sent."),
                        integer("appid")
                            .description("ID of the app that sent the message."),
                        string("message")
                            .description("Content of the message that was sent."),
                        string("title")
                            .description("Title of the message that was sent."),
                        integer("priority")
                            .description("Priority of the message that was sent."),
                        object("extras")
                            .description("Extras of the message that was sent.")
                            .properties(
                                object("top_namespace")
                                    .description("Outer namespace of extra information.")
                                    .properties(
                                        object("sub_namespace")
                                            .description("Inner namespace of extra information.")
                                            .properties(
                                                string("extra_info")
                                                    .description("Extra information.")))),
                        string("date")
                            .description("Date when the message was sent."))))
        .perform(GotifySendMessageAction::perform);

    private GotifySendMessageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/message"))
            .body(
                Body.of(
                    MESSAGE, inputParameters.getRequiredString(MESSAGE),
                    PRIORITY, inputParameters.getInteger(PRIORITY),
                    TITLE, inputParameters.getString(TITLE),
                    EXTRAS, getExtras(inputParameters.getList(EXTRAS, new TypeReference<>() {}))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Object getExtras(List<Map<String, String>> extrasList) {
        if (extrasList == null || extrasList.isEmpty()) {
            return null;
        }

        Map<String, Map<String, Map<String, String>>> extras = new HashMap<>();

        for (Map<String, String> extra : extrasList) {
            String topNamespace = extra.get(TOP_NAMESPACE);
            String subNamespace = extra.get(SUB_NAMESPACE);
            String extraInfoKey = extra.get(EXTRA_INFO_KEY);
            String extraInfoValue = extra.get(EXTRA_INFO_VALUE);

            extras.put(topNamespace, Map.of(subNamespace, Map.of(extraInfoKey, extraInfoValue)));
        }

        return extras;
    }
}
