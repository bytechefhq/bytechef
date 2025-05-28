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
import static com.bytechef.component.infobip.constant.InfobipConstants.CONFIGURATION_KEY;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.KEYWORD;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.NUMBER;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class InfobipUtils {

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

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : getTemplates(inputParameters.getRequiredString(FROM), context)) {
            options.add(option((String) map.get(NAME), (String) map.get(NAME)));
        }

        return options;
    }

    public static WebhookEnableOutput getWebhookEnableOutput(
        String number, String channel, String keyword, String webhookUrl, TriggerContext triggerContext) {

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

                                put(NUMBER, number);
                            }
                        }))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(CONFIGURATION_KEY, (String) body.get(CONFIGURATION_KEY)), null);
    }

    public static void unsubscribeWebhook(String configurationKey, TriggerContext triggerContext) {
        triggerContext.http(http -> http.delete(
            "/resource-management/1/inbound-message-configurations/%s".formatted(configurationKey)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }
}
