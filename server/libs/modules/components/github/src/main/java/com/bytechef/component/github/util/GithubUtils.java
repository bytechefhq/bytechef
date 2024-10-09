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

package com.bytechef.component.github.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class GithubUtils {

    private GithubUtils() {
    }

    public static Map<String, Object> getContent(WebhookBody body) {
        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        if (Objects.equals(content.get("action"), "opened")) {
            return content;
        }

        return Collections.emptyMap();
    }

    public static List<Option<String>> getIssueOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) + "/issues"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Object item : body) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("title"), String.valueOf(map.get("number"))));
            }
        }

        return options;
    }

    public static String getOwnerName(Context context) {
        Map<String, Object> body = context
            .http(http -> http.get("/user"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get("login");
    }

    public static List<Option<String>> getRepositoryOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context.http(http -> http.get("/user/repos"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Object item : body) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("name"), (String) map.get("name")));
            }
        }

        return options;
    }

    public static List<Option<String>> getCollaborators(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {
        List<Option<String>> collaborators = new ArrayList<>();
        List<Map<String, Object>> body = context
            .http(http -> http.get(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY)
                    + "/collaborators"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        for (Object item : body) {
            if (item instanceof Map<?, ?> map) {
                collaborators.add(option((String) map.get("name"), (String) map.get("login")));
            }
        }

        return collaborators;
    }

    public static Integer subscribeWebhook(String repositry, String event, String webhookUrl, TriggerContext context) {
        Map<String, Object> body = context
            .http(http -> http.post("/repos/" + getOwnerName(context) + "/" + repositry + "/hooks"))
            .body(
                Http.Body.of(
                    "events", List.of(event),
                    "config", Map.of("url", webhookUrl, "content_type", "json")))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (Integer) body.get(ID);
    }

    public static void unsubscribeWebhook(String repository, Integer webhookId, TriggerContext context) {
        context
            .http(http -> http.delete("/repos/" + getOwnerName(context) + "/" + repository + "/hooks/" + webhookId))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute();
    }
}
