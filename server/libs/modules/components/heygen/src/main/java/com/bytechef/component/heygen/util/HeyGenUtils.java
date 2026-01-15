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

package com.bytechef.component.heygen.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.heygen.constant.HeyGenConstants.ID;
import static com.bytechef.component.heygen.constant.HeyGenConstants.NAME;
import static com.bytechef.component.heygen.constant.HeyGenConstants.TEMPLATE_ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class HeyGenUtils {

    public static List<Option<String>> getFolderIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        String nextPageToken = null;
        List<Option<String>> options = new ArrayList<>();

        do {
            Map<String, Map<?, ?>> body = context
                .http(http -> http.get("https://api.heygen.com/v1/folders"))
                .queryParameters("limit", 100, "token", nextPageToken)
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            Map<?, ?> data = body.get("data");

            if (data.get("folders") instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        String id = (String) map.get(ID);
                        String name = (String) map.get(NAME);

                        options.add(option(name, id));
                    }
                }
            }

            nextPageToken = (String) data.get("token");

        } while (nextPageToken != null);

        return options;
    }

    public static List<Option<String>> getLanguageOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, Map<?, ?>> body = context
            .http(http -> http.get("https://api.heygen.com/v2/video_translate/target_languages"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        Map<?, ?> data = body.get("data");

        if (data.get("languages") instanceof List<?> list) {
            for (Object language : list) {
                options.add(option((String) language, (String) language));
            }
        }

        return options;
    }

    public static List<Option<String>> getTemplateIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, Map<?, ?>> body = context
            .http(http -> http.get("https://api.heygen.com/v2/templates"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        Map<?, ?> data = body.get("data");

        if (data.get("templates") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    String id = (String) map.get(TEMPLATE_ID);
                    String name = (String) map.get(NAME);

                    options.add(option(name, id));
                }
            }
        }

        return options;
    }

    public static String addWebhook(String eventType, TriggerContext context, String webhookUrl) {

        Map<String, Object> body = context.http(http -> http.post("https://api.heygen.com/v1/webhook/endpoint.add"))
            .body(
                Body.of(
                    "url", webhookUrl,
                    "events", List.of(eventType)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("data") instanceof Map<?, ?> map) {
            return (String) map.get("endpoint_id");
        }

        throw new ProviderException("Failed to subscribe to webhook");
    }

    public static void deleteWebhook(TriggerContext context, String webhookId) {
        context.http(http -> http.delete("https://api.heygen.com/v1/webhook/endpoint.delete"))
            .queryParameter("endpoint_id", webhookId)
            .execute();
    }

    public static Object getContent(TriggerDefinition.WebhookBody body) {
        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get("event_data");
    }

    private HeyGenUtils() {
    }
}
