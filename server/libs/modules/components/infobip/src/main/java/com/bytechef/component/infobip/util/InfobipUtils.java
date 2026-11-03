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

package com.bytechef.component.infobip.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONFIGURATION_KEY;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESTINATIONS;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.KEYWORD;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGES;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.NUMBER;
import static com.bytechef.component.infobip.constant.InfobipConstants.SENDER;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEMPLATE_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static com.bytechef.component.infobip.constant.InfobipConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Monika Kušter
 */
public class InfobipUtils {

    public static List<? extends ValueProperty<?>> createPlaceholderProperties(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext actionContext) {

        if (!inputParameters.containsKey(TEMPLATE_NAME) || !inputParameters.containsKey(FROM)) {
            return List.of();
        }

        List<ValueProperty<?>> properties = new ArrayList<>();

        for (Map<String, Object> map : getTemplates(inputParameters.getRequiredString(FROM), actionContext)) {
            Object name = map.get(NAME);

            if (name.equals(inputParameters.getRequiredString(TEMPLATE_NAME)) &&
                map.get("structure") instanceof Map<?, ?> structure &&
                structure.get("body") instanceof Map<?, ?> body) {

                String text = (String) body.get(TEXT);

                Pattern pattern = Pattern.compile("\\{\\{(.*?)}}");
                Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    properties.add(
                        string("_" + matcher.group(1))
                            .label(matcher.group(1))
                            .required(true));
                }
            }
        }

        return properties;
    }

    public static List<Map<String, Object>> getTemplates(String sender, Context context) {
        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get("/whatsapp/2/senders/%s/templates".formatted(sender)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get("templates");
    }

    public static List<Option<String>> getTemplateOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        if (!inputParameters.containsKey(FROM)) {
            return List.of();
        }

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : getTemplates(inputParameters.getRequiredString(FROM), context)) {
            String name = (String) map.get(NAME);

            options.add(option(name, name));
        }

        return options;
    }

    public static WebhookEnableOutput getWebhookEnableOutput(
        String number, String channel, String keyword, String webhookUrl, TriggerContext triggerContext) {

        return getWebhookEnableOutput(NUMBER, number, channel, keyword, webhookUrl, triggerContext);
    }

    /**
     * Creates an Infobip inbound message configuration that forwards messages to the given webhook URL.
     *
     * @param identifierKey the request field that identifies the receiving address: {@code number} for SMS and
     *                      WhatsApp, {@code resource} for channels such as Viber Bots that use a sender ID
     */
    public static WebhookEnableOutput getWebhookEnableOutput(
        String identifierKey, String identifier, String channel, String keyword, String webhookUrl,
        TriggerContext triggerContext) {

        Map<String, Object> body =
            triggerContext.http(http -> http.post("/resource-management/1/inbound-message-configurations"))
                .body(
                    Http.Body.of(
                        new HashMap<>() {
                            {
                                put("channel", channel);
                                put("forwarding", Map.of("type", "HTTP_FORWARD", "url", webhookUrl));

                                if (keyword != null) {
                                    put(KEYWORD, keyword);
                                }

                                put(identifierKey, identifier);
                            }
                        }))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(CONFIGURATION_KEY, (String) body.get(CONFIGURATION_KEY)), null);
    }

    public static void unsubscribeWebhook(String configurationKey, TriggerContext triggerContext) {
        try {
            triggerContext.http(http -> http.delete(
                "/resource-management/1/inbound-message-configurations/%s".formatted(configurationKey)))
                .execute();
        } catch (ProviderException.NotFoundException notFoundException) {
            triggerContext.log(log -> log.debug(
                "Infobip inbound message configuration {} no longer exists", configurationKey));
        }
    }

    /**
     * Sends one Viber text message through an Infobip messages endpoint. Viber Bots and Viber Business Messages accept
     * the same request body and return the same response, and differ only in the endpoint path.
     */
    public static Map<String, Object> sendViberTextMessage(
        String path, Parameters inputParameters, ActionContext actionContext) {

        Map<String, Object> message = Map.of(
            SENDER, inputParameters.getRequiredString(SENDER),
            DESTINATIONS, List.of(Map.of(TO, inputParameters.getRequiredString(TO))),
            CONTENT, Map.of(TYPE, "TEXT", TEXT, inputParameters.getRequiredString(TEXT)));

        return actionContext
            .http(http -> http.post(path))
            .body(Http.Body.of(MESSAGES, List.of(message)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
