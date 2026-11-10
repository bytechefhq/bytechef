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

package com.bytechef.component.loops.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.loops.constant.LoopsConstants.ID;
import static com.bytechef.component.loops.constant.LoopsConstants.NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class LoopsUtils {

    private static final int CAMPAIGNS_PER_PAGE = 50;

    private LoopsUtils() {
    }

    public static List<Option<String>> getCampaignOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String cursor = null;

        do {
            String currentCursor = cursor;

            Map<String, Object> response = context.http(http -> http.get("/campaigns"))
                .queryParameters(getCampaignQueryParameters(currentCursor))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (response == null) {
                break;
            }

            if (response.get("data") instanceof List<?> campaigns) {
                for (Object campaign : campaigns) {
                    if (campaign instanceof Map<?, ?> campaignMap) {
                        options.add(option((String) campaignMap.get(NAME), (String) campaignMap.get(ID)));
                    }
                }
            }

            cursor = null;

            if (response.get("pagination") instanceof Map<?, ?> pagination &&
                pagination.get("nextCursor") instanceof String nextCursor && !nextCursor.isBlank()) {

                cursor = nextCursor;
            }
        } while (cursor != null);

        return options;
    }

    public static List<Option<String>> getMailingListOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Map<String, Object>> response = context.http(http -> http.get("/lists"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> item : response) {
            options.add(option((String) item.get(NAME), (String) item.get(ID)));
        }

        return options;
    }

    private static Object[] getCampaignQueryParameters(String cursor) {
        if (cursor == null) {
            return new Object[] {
                "perPage", String.valueOf(CAMPAIGNS_PER_PAGE)
            };
        }

        return new Object[] {
            "perPage", String.valueOf(CAMPAIGNS_PER_PAGE), "cursor", cursor
        };
    }
}
