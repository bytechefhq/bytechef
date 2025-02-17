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

package com.bytechef.component.apollo.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class ApolloUtils {

    private ApolloUtils() {
    }

    public static ActionOptionsFunction<String> getOptions(String path, String resources) {
        return (inputParameters, connectionParameters, arrayIndex, searchText, actionContext) -> {
            Map<String, ?> body = actionContext.http(http -> http.get(path))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            List<Option<String>> options = new ArrayList<>();

            if (body.get(resources) instanceof List<?> users) {
                for (Object user : users) {
                    if (user instanceof Map<?, ?> map) {
                        options.add(option((String) map.get("name"), (String) map.get("id")));
                    }
                }
            }

            return options;
        };
    }
}
