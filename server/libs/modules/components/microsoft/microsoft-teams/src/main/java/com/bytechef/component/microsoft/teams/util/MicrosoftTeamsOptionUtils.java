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

package com.bytechef.component.microsoft.teams.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_URL;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.E_TAG;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.NAME;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.VALUE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.WEB_DAV_URL;
import static com.bytechef.microsoft.commons.MicrosoftUtils.getOptions;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftTeamsOptionUtils {

    private MicrosoftTeamsOptionUtils() {
    }

    public static List<Map<String, String>> getAttachmentsList(List<String> fileIds, Context context) {
        List<Map<String, String>> attachmetsList = new ArrayList<>();

        for (String fileId : fileIds) {
            attachmetsList.add(getAttachments(fileId, context));
        }

        return attachmetsList;
    }

    private static Map<String, String> getAttachments(String fileId, Context context) {
        Map<String, String> body = context
            .http(http -> http.get("https://graph.microsoft.com/v1.0/me/drive/items/" + fileId))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .queryParameter("$select", "id,name,webUrl,webDavUrl,@microsoft.graph.downloadUrl,etag")
            .execute()
            .getBody(new TypeReference<>() {});

        String eTag = body.get(E_TAG);
        String id = eTag.substring(eTag.indexOf('{') + 1, eTag.indexOf('}'));

        return Map.of(
            ID, id,
            CONTENT_TYPE, "reference",
            CONTENT_URL, body.get(WEB_DAV_URL),
            NAME, body.get(NAME));
    }

    public static List<Option<String>> getChatIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();
        String nextLink = "/chats";

        while (nextLink != null) {
            String currentLink = nextLink;

            Map<String, Object> body = context.http(http -> http.get(currentLink))
                .queryParameters("$expand", "members")
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get(VALUE) instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {

                        List<String> chatMembers = new ArrayList<>();

                        if (map.get("members") instanceof List<?> members) {
                            for (Object member : members) {
                                if (member instanceof Map<?, ?> memberMap) {
                                    chatMembers.add((String) memberMap.get(DISPLAY_NAME));
                                }
                            }
                        }

                        String chatName = getChatName((String) map.get("topic"), chatMembers);

                        options.add(option(map.get("chatType") + " chat: " + chatName, (String) map.get(ID)));
                    }
                }
            }

            nextLink = (String) body.get("@odata.nextLink");
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
            "/teams/" + inputParameters.getRequiredString(TEAM_ID) + "/channels"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(context, body, DISPLAY_NAME, ID);
    }

    public static String getHtmlAttachmentsTag(List<Map<String, String>> attachments) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map<String, String> attachment : attachments) {
            stringBuilder.append("<attachment id=%s></attachment>".formatted(attachment.get(ID)));
        }

        return stringBuilder.toString();
    }

    public static List<Option<String>> getTeamIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context.http(http -> http.get("/me/joinedTeams"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(context, body, DISPLAY_NAME, ID);
    }
}
