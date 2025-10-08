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

package com.bytechef.component.notion.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.notion.constant.NotionConstants.CONTENT;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.TEXT;
import static com.bytechef.component.notion.constant.NotionConstants.TITLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionUtils {

    private NotionUtils() {
    }

    public static OptionsFunction<String> gePageOrDatabaseIdOptions(boolean isPage) {
        return (inputParameters, connectionParameters, arrayIndex, searchText, context) -> {
            String objectType = isPage ? "page" : "database";

            Map<String, Object> body = context
                .http(http -> http.post("/search"))
                .body(Http.Body.of("filter", Map.of("property", "object", "value", objectType)))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            List<Option<String>> options = new ArrayList<>();

            Object resultsObject = body.get("results");

            if (isPage) {
                if (resultsObject instanceof List<?> results) {
                    for (Object result : results) {
                        if (result instanceof Map<?, ?> resultMap &&
                            resultMap.get("properties") instanceof Map<?, ?> properties &&
                            properties.get(TITLE) instanceof Map<?, ?> title) {

                            options.add(getOption((String) resultMap.get(ID), title));
                        }
                    }
                }
            } else {
                if (resultsObject instanceof List<?> results) {
                    for (Object object : results) {
                        if (object instanceof Map<?, ?> map) {
                            options.add(getOption((String) map.get(ID), map));
                        }
                    }
                }
            }

            return options;
        };
    }

    private static Option<String> getOption(String id, Map<?, ?> titleMap) {
        if (titleMap.get(TITLE) instanceof List<?> list) {
            Object titleItem = list.getFirst();

            if (titleItem instanceof Map<?, ?> title && title.get(TEXT) instanceof Map<?, ?> text) {
                return option((String) text.get(CONTENT), id);
            }
        }

        return null;
    }
}
