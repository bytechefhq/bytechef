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

package com.bytechef.component.keap.util;

import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class KeapUtils {

    private KeapUtils() {
    }

    @SuppressWarnings("unchecked")
    public static OptionsDataSource.ActionOptionsFunction<String> getCompanyIdOptions() {
        return (inputParameters, connectionParameters, arrayIndex, searchText, context) -> {
            Map<String, ?> body = context
                .http(http -> http.get("https://api.infusionsoft.com/crm/rest/v1/companies"))
                .header("Content-Type", "application/json")
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
                .execute()
                .getBody(new Context.TypeReference<>() {});

            if (body.containsKey("error")) {
                throw new IllegalStateException((String) ((Map<?, ?>) body.get("error")).get("message"));
            }

            return getOptions((Map<String, List<Map<?, ?>>>) body, "companies");
        };
    }

    private static List<Option<String>> getOptions(Map<String, List<Map<?, ?>>> response, String name) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<?, ?> list : response.get(name)) {
            options.add(option((String) list.get("company_name"), (String) list.get("id")));
        }

        return options;
    }
}
