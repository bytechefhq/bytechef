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

package com.bytechef.component.bambooHR.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BambooHRUtils {

    public static List<Option<String>> getLocationOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/meta/lists"))
            .headers(Map.of("accept", List.of("application/json")))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, "Location");
    }

    public static List<Option<String>> getJobTitleOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/meta/lists"))
            .headers(Map.of("accept", List.of("application/json")))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, "Job Title");
    }

    public static List<Option<String>> getEmploymentStatusOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/meta/lists"))
            .headers(Map.of("accept", List.of("application/json")))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, "Employment Status");
    }

    private static List<Option<String>> getOptions(List<Map<String, Object>> body, String targetName) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> entry : body) {
            if (targetName.equals(entry.get("name"))) {
                Object optionsObj = entry.get("options");
                if (optionsObj instanceof List<?>) {
                    List<Map<String, Object>> extractedOptions = (List<Map<String, Object>>) optionsObj;
                    for (Map<String, Object> stringObjectMap : extractedOptions) {
                        options.add(option((String) stringObjectMap.get("name"), (String) stringObjectMap.get("name")));
                    }
                }
                break;
            }
        }
        return options;
    }

    private BambooHRUtils() {
    }
}
