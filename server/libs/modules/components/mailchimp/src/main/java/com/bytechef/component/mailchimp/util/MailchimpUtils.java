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

package com.bytechef.component.mailchimp.util;

import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.AUTHORIZATION;
import static com.bytechef.hermes.definition.DefinitionDSL.option;

import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Context.Http;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.definition.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class MailchimpUtils {

    public static String getMailChimpServer(String accessToken, Context context) {
        Map<?, ?> response = context.http(http -> http.get("https://login.mailchimp.com/oauth2/metadata")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .header(AUTHORIZATION, "OAuth " + accessToken)
            .execute()
            .getBody());

        if (!response.containsKey("dc")) {
            throw new ComponentExecutionException(
                "%s: %s".formatted(response.get("error"), response.get("error_description")));
        }

        return (String) response.get("dc");
    }

    @SuppressWarnings("unchecked")
    public static ComponentOptionsFunction getListIdOptions() {
        return (inputParameters, connectionParameters, searchText, context) -> {
            String accessToken = connectionParameters.getRequiredString(ACCESS_TOKEN);

            String url = "https://%s.api.mailchimp.com/3.0/lists".formatted(getMailChimpServer(accessToken, context));

            Map<String, ?> response = context
                .http(http -> http.get(url))
                .queryParameters(
                    Map.of(
                        "fields", List.of("lists.id,lists.name,total_items"),
                        "count", List.of("1000")))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody();

            context.logger(logger -> logger.debug("Response for url='%s': %s".formatted(url, response)));

            List<Option<?>> options = new ArrayList<>();

            for (Map<?, ?> list : (List<Map<?, ?>>) response.get("lists")) {
                options.add(option((String) list.get("name"), list.get("id")));
            }

            return options;
        };
    }
}
