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

package com.bytechef.component.microsoft.teams.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.BASE_URL;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.VALUE;
import static com.bytechef.component.microsoft.teams.util.MicrosoftTeamsUtils.getChatMembers;

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
public class MicrosoftTeamsOptionUtils {

    private MicrosoftTeamsOptionUtils() {
    }

    public static List<Option<String>> getChatIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context.http(http -> http.get(BASE_URL + "/chats"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if ((item instanceof Map<?, ?> map)) {
                    List<String> members = getChatMembers(context, map);

                    String chatName = getChatName((String) map.get("topic"), members);

                    options.add(option(map.get("chatType") + " chat: " + chatName, (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    private static String getChatName(String topic, List<String> members) {
        String membersString = members.isEmpty() ? "No title" : String.join(",", members);

        return topic == null ? membersString : topic;
    }

    public static List<Option<String>> getChannelIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context.http(http -> http.get(
            BASE_URL + "/teams/" + inputParameters.getRequiredString(TEAM_ID) + "/channels"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getTeamIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context.http(http -> http.get(BASE_URL + "/me/joinedTeams"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    private static List<Option<String>> getOptions(Map<String, Object> body) {
        List<Option<String>> options = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if ((item instanceof Map<?, ?> map)) {
                    options.add(option((String) map.get(DISPLAY_NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }
}
