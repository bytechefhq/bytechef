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

package com.bytechef.component.clickup.util;

import static com.bytechef.component.clickup.constant.ClickupConstants.FOLDER_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.NAME;
import static com.bytechef.component.clickup.constant.ClickupConstants.SPACE_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.WORKSPACE_ID;
import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class ClickupUtils {

    private ClickupUtils() {
    }

    public static List<Option<String>> getAllListIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();
        String folderId = inputParameters.getString(FOLDER_ID);
        if (folderId != null) {
            options.addAll(getListsWithinFolder(context, folderId));
        }

        options.addAll(getFolderlessLists(context, inputParameters.getRequiredString(SPACE_ID)));

        return options;
    }

    private static List<Option<String>> getListsWithinFolder(ActionContext context, String folderId) {

        return getOptions(fetchDataFromHttpEndpoint(context, "/folder/" + folderId + "/list"), "lists");
    }

    private static List<Option<String>> getFolderlessLists(ActionContext context, String spaceId) {
        return getOptions(fetchDataFromHttpEndpoint(context, "/space/" + spaceId + "/list"), "lists");
    }

    public static List<Option<String>> getFolderIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        return getOptions(
            fetchDataFromHttpEndpoint(context, "/space/" + inputParameters.getRequiredString(SPACE_ID) + "/folder"),
            "folders");
    }

    public static List<Option<String>> getSpaceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        return getOptions(
            fetchDataFromHttpEndpoint(context, "/team/" + inputParameters.getRequiredString(WORKSPACE_ID) + "/space"),
            "spaces");
    }

    public static List<Option<String>> getWorkspaceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getOptions(fetchDataFromHttpEndpoint(context, "/team"), "teams");
    }

    private static List<Option<String>> getOptions(Map<String, List<Map<String, Object>>> body, String resource) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body.get(resource)) {
            options.add(option((String) map.get(NAME), (String) map.get(ID)));
        }

        return options;
    }

    private static Map<String, List<Map<String, Object>>> fetchDataFromHttpEndpoint(Context context, String path) {
        return context
            .http(http -> http.get(path))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static String subscribeWebhook(
        String webhookUrl, TriggerContext context, String workspaceId, String eventType) {

        Map<String, Object> body = context.http(http -> http.post("/team/" + workspaceId + "/webhook"))
            .body(
                Http.Body.of(
                    "endpoint", webhookUrl,
                    "events", List.of(eventType)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get(ID);
    }

    public static void unsubscribeWebhook(TriggerContext context, String webhookId) {
        context.http(http -> http.delete("/webhook/" + webhookId))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

    public static Map<String, Object> getCreatedObject(
        WebhookBody body, TriggerContext context, String id, String path) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return context
            .http(http -> http.get(path + content.get(id)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
