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

package com.bytechef.component.productboard.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.productboard.constant.ProductboardConstants.DATA;
import static com.bytechef.component.productboard.constant.ProductboardConstants.ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class ProductboardUtils extends AbstractProductboardUtils {

    private ProductboardUtils() {
    }

    public static WebhookEnableOutput createSubscription(
        String webhookUrl, String workflowExecutionId, TriggerContext context, String eventType) {

        Map<String, Object> body = context.http(http -> http.post("/webhooks"))
            .header("X-Version", "1")
            .body(Http.Body.of(
                DATA, Map.of(
                    "name", "Webhook for " + workflowExecutionId,
                    "events", List.of(Map.of("eventType", eventType)),
                    "notification", Map.of("url", webhookUrl, "version", 1))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(DATA) instanceof Map<?, ?> map) {
            return new WebhookEnableOutput(Map.of(ID, map.get(ID)), null);
        }

        throw new ProviderException("Failed to create webhook.");
    }

    public static List<Option<String>> getFeatureIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context.http(http -> http.get("/features"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(DATA) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getNoteIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, ?> body = context.http(http -> http.get("/notes"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(DATA) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("title"), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static void deleteSubscription(TriggerContext context, String webhookId) {
        context.http(http -> http.delete("/webhooks/%s".formatted(webhookId)))
            .header("X-Version", "1")
            .execute();
    }

    public static Object getContent(WebhookBody body) {
        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get(DATA);
    }

    public static WebhookValidateResponse webhookValidateOnEnable(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        Map<String, List<String>> map = parameters.toMap();

        List<String> validationToken = map.get("validationToken");

        return new WebhookValidateResponse(validationToken.getFirst(), 200);
    }
}
