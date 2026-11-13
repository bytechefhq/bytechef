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

package com.bytechef.component.webhook.util;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.webhook.constant.WebhookConstants.BODY;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.REDIRECT_URL;
import static com.bytechef.component.webhook.constant.WebhookConstants.RESPONSE_TYPE;
import static com.bytechef.component.webhook.constant.WebhookConstants.STATUS_CODE;

import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Optional immediate response of the asynchronous Webhook triggers. The response is sent to the caller before the
 * workflow runs and is built only from the trigger configuration.
 *
 * @author Ivica Cardic
 */
public class WebhookResponseUtils {

    private static final String RESPONSE_WITH_STATUS_CONDITION = "contains({'%s','%s','%s'}, %s)".formatted(
        ResponseType.JSON.name(), ResponseType.RAW.name(), ResponseType.NO_DATA.name(), RESPONSE_TYPE);

    private enum ResponseType {
        DEFAULT, REDIRECT, JSON, RAW, NO_DATA
    }

    private WebhookResponseUtils() {
    }

    public static @Nullable WebhookResponse getWebhookResponse(Parameters inputParameters, TriggerContext context) {
        ResponseType responseType = inputParameters.get(RESPONSE_TYPE, ResponseType.class, ResponseType.DEFAULT);

        Map<String, String> headers = inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of());
        int statusCode = inputParameters.getInteger(STATUS_CODE, 200);

        return switch (responseType) {
            case DEFAULT -> null;
            case REDIRECT -> WebhookResponse.redirect(inputParameters.getRequiredString(REDIRECT_URL));
            case JSON -> WebhookResponse.json(inputParameters.getRequired(BODY), headers, statusCode);
            case RAW -> WebhookResponse.raw(inputParameters.getRequiredString(BODY), headers, statusCode);
            case NO_DATA -> WebhookResponse.noData(headers, statusCode);
        };
    }

    /**
     * Returns the given trigger properties followed by the optional immediate response properties.
     */
    public static Property[] withResponseProperties(Property... properties) {
        List<Property> allProperties = new ArrayList<>(List.of(properties));

        allProperties.add(
            string(RESPONSE_TYPE)
                .label("Response Type")
                .description(
                    "The response sent to the caller right away, before the workflow runs. Default sends an empty " +
                        "HTTP 200 response.")
                .options(
                    option("Default", ResponseType.DEFAULT.name()),
                    option("Redirect", ResponseType.REDIRECT.name()),
                    option("JSON", ResponseType.JSON.name()),
                    option("Raw", ResponseType.RAW.name()),
                    option("No Data", ResponseType.NO_DATA.name()))
                .defaultValue(ResponseType.DEFAULT.name()));
        allProperties.add(
            string(REDIRECT_URL)
                .label("Redirect URL")
                .description(
                    "The http or https URL the caller is redirected to, on any host. It can use workflow inputs, " +
                        "but not the incoming request.")
                .displayCondition("%s == '%s'".formatted(RESPONSE_TYPE, ResponseType.REDIRECT.name()))
                .required(true));
        allProperties.add(
            object(BODY)
                .label("Body")
                .description("The JSON body of the response.")
                .displayCondition("%s == '%s'".formatted(RESPONSE_TYPE, ResponseType.JSON.name()))
                .required(true)
                .placeholder("Add property"));
        allProperties.add(
            string(BODY)
                .label("Body")
                .description("The body of the response. Set a Content-Type header to send HTML.")
                .displayCondition("%s == '%s'".formatted(RESPONSE_TYPE, ResponseType.RAW.name()))
                .required(true));
        allProperties.add(
            object(HEADERS)
                .label("Headers")
                .description("The headers of the response. Location and Set-Cookie are not allowed.")
                .additionalProperties(string())
                .displayCondition(RESPONSE_WITH_STATUS_CONDITION)
                .placeholder("Add header"));
        allProperties.add(
            integer(STATUS_CODE)
                .label("Status Code")
                .description("The status code of the response. Redirect status codes (3xx) are not allowed.")
                .defaultValue(200)
                .displayCondition(RESPONSE_WITH_STATUS_CONDITION));

        return allProperties.toArray(Property[]::new);
    }
}
