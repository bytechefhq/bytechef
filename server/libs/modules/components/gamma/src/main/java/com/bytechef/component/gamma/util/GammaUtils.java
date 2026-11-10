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

package com.bytechef.component.gamma.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.gamma.constant.GammaConstants.ID;
import static com.bytechef.component.gamma.constant.GammaConstants.NAME;

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
public class GammaUtils {

    private static final int THEMES_PER_PAGE = 50;

    private GammaUtils() {
    }

    public static List<Option<String>> getThemeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String cursor = null;

        do {
            String currentCursor = cursor;

            Map<String, Object> response = context.http(http -> http.get("/themes"))
                .queryParameters(getThemeQueryParameters(currentCursor))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (response == null) {
                break;
            }

            if (response.get("data") instanceof List<?> themes) {
                for (Object theme : themes) {
                    if (theme instanceof Map<?, ?> themeMap) {
                        options.add(option((String) themeMap.get(NAME), (String) themeMap.get(ID)));
                    }
                }
            }

            cursor = null;

            if (Boolean.TRUE.equals(response.get("hasMore")) &&
                response.get("nextCursor") instanceof String nextCursor && !nextCursor.isBlank()) {

                cursor = nextCursor;
            }
        } while (cursor != null);

        return options;
    }

    private static Object[] getThemeQueryParameters(String cursor) {
        if (cursor == null) {
            return new Object[] {
                "limit", String.valueOf(THEMES_PER_PAGE)
            };
        }

        return new Object[] {
            "limit", String.valueOf(THEMES_PER_PAGE), "after", cursor
        };
    }
}
