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

package com.bytechef.component.jira.util;

import static com.bytechef.component.jira.constant.JiraConstants.CONTENT;
import static com.bytechef.component.jira.constant.JiraConstants.DESCRIPTION;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUETYPE;
import static com.bytechef.component.jira.constant.JiraConstants.NAME;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.TEXT;
import static com.bytechef.component.jira.constant.JiraConstants.TYPE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class JiraUtils {

    private JiraUtils() {
    }

    public static void addDescriptionField(Map<String, Object> project, String description) {
        if (description != null) {
            project.put(DESCRIPTION, Map.of(
                CONTENT, List.of(
                    Map.of(
                        CONTENT, List.of(
                            Map.of(
                                TEXT, description,
                                TYPE, TEXT)),
                        TYPE, "paragraph")),
                TYPE, "doc",
                "version", 1));
        }
    }

    public static String getProjectName(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> body = context
            .http(http -> http.get("/project/" + inputParameters.getRequiredString(PROJECT)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get(NAME);
    }

    public static Integer subscribeWebhook(
        Parameters inputParameters, String webhookUrl, TriggerContext context, String event) {

        StringBuilder jqlFilter = new StringBuilder(PROJECT + " = " + inputParameters.getRequiredString(PROJECT));

        if (inputParameters.getString(ISSUETYPE) != null) {
            jqlFilter
                .append(" AND ")
                .append(ISSUETYPE)
                .append(" = ")
                .append(inputParameters.getString(ISSUETYPE));
        }

        Map<String, ?> body = context
            .http(http -> http.post("/webhook"))
            .body(Http.Body.of(
                "url", webhookUrl,
                "webhooks", List.of(
                    Map.of(
                        "events", List.of(event),
                        "jqlFilter", jqlFilter.toString()))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("webhookRegistrationResult") instanceof List<?> list) {
            Object object = list.getFirst();

            if (object instanceof Map<?, ?> map) {
                return (Integer) map.get("createdWebhookId");
            }
        }

        throw new ProviderException("Failed to start Jira webhook.");
    }

    public static void unsubscribeWebhook(Parameters outputParameters, TriggerContext context) {

        context
            .http(http -> http.delete("/webhook"))
            .body(Http.Body.of("webhookIds", List.of(outputParameters.getInteger(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }
}
