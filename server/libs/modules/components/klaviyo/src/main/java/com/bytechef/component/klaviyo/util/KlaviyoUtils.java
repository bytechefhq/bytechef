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
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ATTRIBUTES;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.DATA;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.EMAIL;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PHONE_NUMBER;

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
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/api/profiles"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(DATA) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get(ATTRIBUTES) instanceof Map<?, ?> attributes) {
                    options.add(option((String) attributes.get(EMAIL), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static String getProfileEmail(Context context, String profileId) {
        return getProfileAttribute(context, profileId, EMAIL);
    }

    public static String getProfilePhoneNumber(Context context, String profileId) {
        return getProfileAttribute(context, profileId, PHONE_NUMBER);
    }

    private static String getProfileAttribute(Context context, String profileId, String attributeName) {
        Map<String, ?> profileData = context
            .http(http -> http.get("/api/profiles/" + profileId))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (profileData.get(DATA) instanceof Map<?, ?> dataMap &&
            dataMap.get(ATTRIBUTES) instanceof Map<?, ?> attributes) {

            return (String) attributes.get(attributeName);
        }

        return null;
    }

    private KlaviyoUtils() {
    }
}
