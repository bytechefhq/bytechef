
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.pipedrive.util;

import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.Body;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;
import com.bytechef.hermes.component.util.MapValueUtils;
import com.bytechef.hermes.definition.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.util.HttpClientUtils.responseFormat;
import static com.bytechef.hermes.definition.DefinitionDSL.option;

/**
 * @author Ivica Cardic
 */
public class PipedriveUtils {

    private static final Logger logger = LoggerFactory.getLogger(PipedriveUtils.class);

    public static String subscribeWebhook(String eventObject, String eventAction, String webhookUrl) {
        Map<?, ?> result = (Map<?, ?>) HttpClientUtils
            .post("/api/v1/webhooks")
            .body(
                Body.of(
                    Map.of(
                        "event_object", eventObject,
                        "event_action", eventAction,
                        "subscription_url", webhookUrl)))
            .configuration(responseFormat(ResponseFormat.JSON))
            .execute()
            .body();

        return (String) result.get("id");
    }

    public static void unsubscribeWebhook(String webhookId) {
        HttpClientUtils
            .delete("/api/v1/webhooks/%s".formatted(webhookId))
            .configuration(responseFormat(ResponseFormat.JSON))
            .execute();
    }

    public static ComponentOptionsFunction getOptions(String path, String dependsOn) {
        return (connection, inputParameters, searchText) -> {
            Map<String, ?> response = HttpClientUtils
                .get(path)
                .queryParameters(
                    dependsOn == null
                        ? Map.of()
                        : Map.of(dependsOn, List.of(MapValueUtils.getString(inputParameters, dependsOn, ""))))
                .configuration(responseFormat(HttpClientUtils.ResponseFormat.JSON))
                .execute()
                .getBody();

            if (logger.isDebugEnabled()) {
                logger.debug("Response for path='%s': %s".formatted(path, response));
            }

            List<Option<?>> options = new ArrayList<>();

            for (Map<?, ?> list : MapValueUtils.getRequiredList(response, "data", Map.class)) {
                options.add(option((String) list.get("name"), list.get("id")));
            }

            return options;
        };
    }
}
