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

package com.bytechef.component.mailerlite.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.DATA;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.EMAIL;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.EVENTS;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.ID;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.NAME;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.TRIGGER_NAME;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.URL;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MailerLiteUtils {

    protected static final ContextFunction<Http, Executor> GET_GROUPS_CONTEXT_FUNCTION =
        http -> http.get("/groups");

    protected static final ContextFunction<Http, Executor> GET_SUBSCRIBERS_CONTEXT_FUNCTION =
        http -> http.get("/subscribers");

    public static Map<String, Object> getContent(WebhookBody body) {

        return body.getContent(new TypeReference<>() {});
    }

    public static List<Option<String>> getGroupIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> body = context.http(GET_GROUPS_CONTEXT_FUNCTION)
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body.get(DATA), NAME);
    }

    public static List<Option<String>> getSubscriberIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> body = context.http(GET_SUBSCRIBERS_CONTEXT_FUNCTION)
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body.get(DATA), EMAIL);
    }

    public static String subscribeWebhook(
        String triggerName, String events, String webhookUrl, TriggerContext context) {

        Map<String, Object> body = context
            .http(http -> http.post("/webhooks"))
            .body(
                Body.of(
                    Map.of(
                        TRIGGER_NAME, triggerName,
                        EVENTS, List.of(events),
                        URL, webhookUrl)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("data") instanceof Map<?, ?> data) {
            return (String) data.get(ID);
        }

        throw new ProviderException("Failed to subscribe to webhook.");
    }

    public static void unsubscribeWebhook(String webhookId, TriggerContext context) {
        context.http(http -> http.delete("/webhooks/" + webhookId))
            .execute();
    }

    private static List<Option<String>> getOptions(Object data, String label) {
        List<Option<String>> options = new ArrayList<>();

        if (data instanceof List<?> dataList) {
            for (Object optionObject : dataList) {
                if (optionObject instanceof Map<?, ?> option) {
                    options.add(option((String) option.get(label), (String) option.get(ID)));
                }
            }
        }

        return options;
    }
}
