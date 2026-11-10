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

package com.bytechef.component.greenhouse.util;

import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public class GreenhouseUtils {

    static final String CURSOR = "cursor";
    static final String IDS = "ids";
    static final int MAX_PER_PAGE = 500;
    static final String PER_PAGE = "per_page";

    private static final Pattern NEXT_CURSOR_PATTERN = Pattern.compile(
        "<[^>]*[?&]cursor=([^&>]+)[^>]*>\\s*;\\s*rel=\"next\"");

    private GreenhouseUtils() {
    }

    public static Map<String, Object> getRecordById(String path, String id, String recordName, Context context) {
        List<Map<String, Object>> records = context.http(http -> http.get(path))
            .queryParameter(IDS, id)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (records == null || records.isEmpty()) {
            throw new ProviderException("Greenhouse %s with ID %s was not found.".formatted(recordName, id));
        }

        return records.getFirst();
    }

    public static List<Map<String, Object>> getAllRecords(
        String path, Map<String, List<String>> queryParameters, Context context) {

        List<Map<String, Object>> allRecords = new ArrayList<>();

        Map<String, List<String>> pageQueryParameters = new HashMap<>(queryParameters);

        pageQueryParameters.put(PER_PAGE, List.of(String.valueOf(MAX_PER_PAGE)));

        while (pageQueryParameters != null) {
            Map<String, List<String>> currentQueryParameters = pageQueryParameters;

            Response response = context.http(http -> http.get(path))
                .queryParameters(currentQueryParameters)
                .configuration(responseType(ResponseType.JSON))
                .execute();

            List<Map<String, Object>> records = response.getBody(new TypeReference<>() {});

            if (records != null) {
                allRecords.addAll(records);
            }

            String nextCursor = getNextCursor(response.getHeader("link"));

            pageQueryParameters = nextCursor == null ? null : Map.of(CURSOR, List.of(nextCursor));
        }

        return allRecords;
    }

    static String getNextCursor(List<String> linkHeaders) {
        if (linkHeaders == null) {
            return null;
        }

        for (String linkHeader : linkHeaders) {
            if (linkHeader == null) {
                continue;
            }

            Matcher matcher = NEXT_CURSOR_PATTERN.matcher(linkHeader);

            if (matcher.find()) {
                return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
            }
        }

        return null;
    }
}
