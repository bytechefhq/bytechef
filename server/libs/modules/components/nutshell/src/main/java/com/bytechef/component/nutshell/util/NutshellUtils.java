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

package com.bytechef.component.nutshell.util;

import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.nutshell.constant.NutshellConstants.DESCRIPTION;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAIL;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAILS;
import static com.bytechef.component.nutshell.constant.NutshellConstants.ID;
import static com.bytechef.component.nutshell.constant.NutshellConstants.NAME;
import static com.bytechef.component.nutshell.constant.NutshellConstants.PHONE;
import static com.bytechef.component.nutshell.constant.NutshellConstants.PHONES;
import static com.bytechef.component.nutshell.constant.NutshellConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.ComponentDsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NutshellUtils {

    public static Object createEntityBasedOnType(
        Parameters inputParameters, ActionContext actionContext, boolean isCompanyEntity) {

        Map<String, Object> companyMap = createEntityMap(inputParameters);

        return actionContext
            .http(http -> http.post(isCompanyEntity ? "/accounts" : "/contacts"))
            .body(Http.Body.of(isCompanyEntity ? "accounts" : "contacts", List.of(companyMap)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> createEntityMap(Parameters inputParameters) {
        Map<String, Object> map = new HashMap<>();

        map.put(NAME, inputParameters.getRequiredString(NAME));
        map.put(DESCRIPTION, inputParameters.getString(DESCRIPTION, ""));

        addToListIfPresent(inputParameters, EMAIL, EMAILS, map);
        addToListIfPresent(inputParameters, PHONE, PHONES, map);

        return map;
    }

    private static void
        addToListIfPresent(Parameters inputParameters, String key, String mapKey, Map<String, Object> map) {
        String value = inputParameters.getString(key);

        if (value != null) {
            map.put(mapKey, List.of(Map.of(VALUE, value)));
        }
    }

    public static void addIfPresent(Parameters inputParameters, String key, String mapKey, Map<String, Object> map) {
        String value = inputParameters.getString(key);

        if (value != null) {
            map.put(mapKey, Map.of(key, value));
        }
    }

    public static List<Option<String>> getUserOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringMap, String s,
        Context context) {

        List<Option<String>> options = new ArrayList<>();
        Map<String, ?> body = context.http(http -> http.get("/users"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("users") instanceof List<?> userList) {
            options = getOptions(userList, NAME, ID);
        }

        return options;

    }

    private static List<Option<String>> getOptions(List<?> itemList, String label, String value) {
        List<Option<String>> options = new ArrayList<>();

        for (Object item : itemList) {
            if (item instanceof Map<?, ?> map) {
                options.add(ComponentDsl.option((String) map.get(label), String.valueOf(map.get(value))));
            }
        }
        return options;
    }
}
