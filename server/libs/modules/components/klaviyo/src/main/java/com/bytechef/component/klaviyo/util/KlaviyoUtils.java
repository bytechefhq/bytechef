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

package com.bytechef.component.klaviyo.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class KlaviyoUtils {

    public static List<Option<String>> getProfileIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, ?> body = context
            .http(http -> http.get("/api/profiles"))
            .headers(Map.of(
                "accept", List.of("application/vnd.api+json"),
                "revision", List.of("2025-04-15")))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("data") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map && map.get("attributes") instanceof Map<?, ?> attributes) {
                    options.add(option((String) attributes.get("email"), (String) map.get("id")));
                }
            }
        }
        return options;
    }

    public static String getProfileEmail(Context context, String profileId) {

        Map<String, ?> body = getProfile(context, profileId);

        if (body.get("data") instanceof Map<?, ?> map && map.get("attributes") instanceof Map<?, ?> attributes) {
            return (String) attributes.get("email");
        }
        return null;
    }

    public static String getProfilePhoneNumber(Context context, String profileId) {

        Map<String, ?> body = getProfile(context, profileId);

        if (body.get("data") instanceof Map<?, ?> map && map.get("attributes") instanceof Map<?, ?> attributes) {
            return (String) attributes.get("phone_number");
        }
        return null;
    }

    private static Map<String, ?> getProfile(Context context, String profileId) {
        return context
            .http(http -> http.get("/api/profiles/" + profileId))
            .headers(Map.of(
                "accept", List.of("application/vnd.api+json"),
                "revision", List.of("2025-04-15")))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private KlaviyoUtils() {
    }
}
