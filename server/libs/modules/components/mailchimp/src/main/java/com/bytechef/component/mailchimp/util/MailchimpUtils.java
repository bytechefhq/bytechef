
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

package com.bytechef.component.mailchimp.util;

import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.definition.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.hermes.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.hermes.component.util.HttpClientUtils.responseFormat;
import static com.bytechef.hermes.definition.DefinitionDSL.option;

/**
 * @author Ivica Cardic
 */
public class MailchimpUtils {
    public static String getMailChimpServer(String accessToken) {
        Map<?, ?> response = (Map<?, ?>) HttpClientUtils
            .get("https://login.mailchimp.com/oauth2/metadata")
            .configuration(responseFormat(HttpClientUtils.ResponseFormat.JSON))
            .headers(Map.of(AUTHORIZATION, List.of("OAuth " + accessToken)))
            .execute()
            .getBody();

        return (String) response.get("dc");
    }

    @SuppressWarnings("unchecked")
    public static ComponentOptionsFunction getListIdOptions() {
        return (connection, inputParameters) -> {
            Map<?, ?> response = ((Map<?, ?>) HttpClientUtils
                .get("https://%s.api.mailchimp.com/3.0/lists".formatted(
                    getMailChimpServer(connection.getRequiredString(ACCESS_TOKEN))))
                .queryParameters(
                    Map.of(
                        "fields", List.of("lists.id,lists.name,total_items"),
                        "count", List.of("1000")))
                .configuration(responseFormat(HttpClientUtils.ResponseFormat.JSON))
                .execute()
                .getBody());

            List<Option<?>> options = new ArrayList<>();

            ((List<Map<?, ?>>) response.get("lists")).forEach(list -> options.add(
                option((String) list.get("name"), list.get("id"))));

            return options;
        };
    }
}
