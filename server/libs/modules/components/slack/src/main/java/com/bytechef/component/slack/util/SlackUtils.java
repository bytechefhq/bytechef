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

package com.bytechef.component.slack.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.ID;
import static com.bytechef.component.slack.constant.SlackConstants.NAME;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class SlackUtils {

    private SlackUtils() {
    }

    public static Object sendMessage(
        String channel, String text, List<Map<String, Object>> blocks, ActionContext actionContext) {

        Map<String, Object> body = actionContext
            .http(http -> http.post("/chat.postMessage"))
            .body(
                Http.Body.of(
                    CHANNEL, channel,
                    TEXT, text,
                    "blocks", blocks))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if ((boolean) body.get("ok")) {
            return body;
        } else {
            throw new ProviderException((String) body.get("error"));
        }
    }

    public static List<Option<String>> getChannelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("/conversations.list"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("channels") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getUserOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("/users.list"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("members") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }
}
