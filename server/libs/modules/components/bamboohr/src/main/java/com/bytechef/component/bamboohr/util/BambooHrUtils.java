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

package com.bytechef.component.bamboohr.util;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.NAME;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BambooHrUtils {

    public static List<Option<String>> getEmployeeIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, Object> body = context
            .http(http -> http.get("/employees/directory"))
            .header("accept", "application/json")
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("employees") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    String id = (String) map.get(ID);

                    options.add(option(map.get("displayName") + " - " + id, id));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getEmployeeFilesIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, Object> body = context
            .http(http -> http.get("/employees/%s/files/view".formatted(inputParameters.getRequiredString(ID))))
            .header("accept", "application/xml")
            .configuration(responseType(Http.ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("category") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getFieldOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/meta/fields"))
            .header("accept", "application/json")
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body) {
            if (map.get("alias") != null) {
                options.add(option((String) map.get(NAME), (String) map.get("alias")));
            }
        }

        return options;
    }

    public static OptionsFunction<String> getOptions(String targetName) {
        return (inputParameters, connectionParameters, arrayIndex, searchText, context) -> {

            List<Map<String, Object>> body = context
                .http(http -> http.get("/meta/lists"))
                .header("accept", "application/json")
                .configuration(responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            List<Option<String>> options = new ArrayList<>();

            for (Map<String, Object> entry : body) {
                if (targetName.equals(entry.get(NAME))) {
                    if (entry.get("options") instanceof List<?> list) {
                        for (Object object : list) {
                            if (object instanceof Map<?, ?> map) {
                                options.add(option((String) map.get(NAME), (String) map.get(NAME)));
                            }
                        }
                    }

                    break;
                }
            }

            return options;
        };
    }

    private BambooHrUtils() {
    }
}
