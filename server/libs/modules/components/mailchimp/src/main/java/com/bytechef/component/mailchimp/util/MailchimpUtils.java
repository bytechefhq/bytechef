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

package com.bytechef.component.mailchimp.util;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class MailchimpUtils extends AbstractMailchimpUtils {

    private MailchimpUtils() {
    }

    public static String getMailChimpServer(String accessToken, Context context) {
        Map<?, ?> response = context.http(http -> http.get("https://login.mailchimp.com/oauth2/metadata")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .header(AUTHORIZATION, "OAuth " + accessToken)
            .execute()
            .getBody(new TypeReference<>() {}));

        if (!response.containsKey("dc")) {
            throw new IllegalStateException(
                "%s: %s".formatted(response.get("error"), response.get("error_description")));
        }

        return (String) response.get("dc");
    }

    public static List<Option<String>> getListIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, ?> response = context
            .http(http -> http.get("/lists"))
            .queryParameters(
                Map.of(
                    "fields", List.of("lists.id,lists.name,total_items"),
                    "count", List.of("1000")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (response.get("lists") instanceof List<?> lists) {
            for (Object list : lists) {
                if (list instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get("id")));
                }
            }
        }

        return options;
    }
}
