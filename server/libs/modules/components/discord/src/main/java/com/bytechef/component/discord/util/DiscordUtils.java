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

package com.bytechef.component.discord.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.discord.constant.DiscordConstants.BASE_URL;
import static com.bytechef.component.discord.constant.DiscordConstants.GUILD_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.RECIPIENT_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class DiscordUtils {

    private DiscordUtils() {
    }

    public static List<Option<String>> getChannelIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Map<String, Object>> body = context.http(http -> http
            .get(BASE_URL + "/guilds/" + inputParameters.getRequiredString(GUILD_ID) + "/channels"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static Map<String, Object> getDMChannel(Parameters inputParameters, ActionContext actionContext) {
        return actionContext.http(http -> http.post(BASE_URL + "/users/@me/channels"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(RECIPIENT_ID, inputParameters.getRequired(RECIPIENT_ID)))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static List<Option<String>> getGuildIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Map<String, Object>> body = context.http(http -> http.get(BASE_URL + "/users/@me/guilds"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getGuildMemberIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Map<String, ?>> body = context.http(http -> http
            .get(BASE_URL + "/guilds/" + inputParameters.getRequiredString(GUILD_ID) + "/members"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .queryParameter("limit", "1000")
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> member : body) {
            if (member.get("user") instanceof Map<?, ?> map) {
                options.add(option((String) map.get("username"), (String) map.get("id")));
            }
        }

        return options;
    }

    private static List<Option<String>> getOptions(List<Map<String, Object>> body) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body) {
            options.add(option((String) map.get("name"), (String) map.get("id")));
        }

        return options;
    }
}
