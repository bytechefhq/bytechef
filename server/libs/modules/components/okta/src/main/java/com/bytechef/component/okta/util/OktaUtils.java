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

package com.bytechef.component.okta.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.okta.constant.OktaConstants.ID;
import static com.bytechef.component.okta.constant.OktaConstants.LABEL;
import static com.bytechef.component.okta.constant.OktaConstants.LIMIT;
import static com.bytechef.component.okta.constant.OktaConstants.NAME;
import static com.bytechef.component.okta.constant.OktaConstants.PROFILE;
import static com.bytechef.component.okta.constant.OktaConstants.Q;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class OktaUtils {

    private static final String AFTER = "after";
    private static final int PAGE_SIZE = 200;

    private OktaUtils() {
    }

    public static String encodePathSegment(String pathSegment) {
        String encodedPathSegment = URLEncoder.encode(pathSegment, StandardCharsets.UTF_8);

        return encodedPathSegment.replace("+", "%20");
    }

    public static List<Option<String>> getApplicationIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> application : getItems(context, "/apps", getSearchQueryParameters(searchText), null)) {
            options.add(option((String) application.get(LABEL), (String) application.get(ID)));
        }

        return options;
    }

    public static List<Option<String>> getGroupIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> group : getItems(context, "/groups", getSearchQueryParameters(searchText), null)) {
            String groupName = (String) group.get(ID);

            if (group.get(PROFILE) instanceof Map<?, ?> profile && profile.get(NAME) instanceof String name) {
                groupName = name;
            }

            options.add(option(groupName, (String) group.get(ID)));
        }

        return options;
    }

    public static List<Map<String, Object>> getItems(
        Context context, String path, Map<String, List<String>> queryParameters, Integer limit) {

        List<Map<String, Object>> items = new ArrayList<>();
        String cursor = null;

        do {
            int pageSize = limit == null ? PAGE_SIZE : Math.min(PAGE_SIZE, limit - items.size());

            Map<String, List<String>> pageQueryParameters = new HashMap<>(queryParameters);

            pageQueryParameters.put(LIMIT, List.of(String.valueOf(pageSize)));

            if (cursor != null) {
                pageQueryParameters.put(AFTER, List.of(cursor));
            }

            Http.Response response = context.http(http -> http.get(path))
                .queryParameters(pageQueryParameters)
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute();

            List<Map<String, Object>> page = response.getBody(new TypeReference<>() {});

            if (page == null || page.isEmpty()) {
                break;
            }

            items.addAll(page);

            cursor = getNextCursor(response.getHeader("Link"));
        } while (cursor != null && (limit == null || items.size() < limit));

        if (limit != null && items.size() > limit) {
            return new ArrayList<>(items.subList(0, limit));
        }

        return items;
    }

    public static String getNextCursor(List<String> linkHeaders) {
        if (linkHeaders == null) {
            return null;
        }

        for (String linkHeader : linkHeaders) {
            for (String link : linkHeader.split(",")) {
                if (!link.contains("rel=\"next\"")) {
                    continue;
                }

                int urlStart = link.indexOf('<');
                int urlEnd = link.indexOf('>');

                if (urlStart < 0 || urlEnd <= urlStart) {
                    continue;
                }

                String url = link.substring(urlStart + 1, urlEnd);

                int queryStart = url.indexOf('?');

                if (queryStart < 0) {
                    continue;
                }

                for (String queryParameter : url.substring(queryStart + 1)
                    .split("&")) {

                    if (queryParameter.startsWith(AFTER + "=")) {
                        return URLDecoder.decode(queryParameter.substring(AFTER.length() + 1), StandardCharsets.UTF_8);
                    }
                }
            }
        }

        return null;
    }

    public static Map<String, List<String>> getQueryParameters(Parameters inputParameters, String... names) {
        Map<String, List<String>> queryParameters = new HashMap<>();

        for (String name : names) {
            String value = inputParameters.getString(name);

            if (value != null && !value.isBlank()) {
                queryParameters.put(name, List.of(value));
            }
        }

        return queryParameters;
    }

    public static String normalizeDomain(String domain) {
        String normalizedDomain = domain.trim();

        if (normalizedDomain.startsWith("https://")) {
            normalizedDomain = normalizedDomain.substring("https://".length());
        } else if (normalizedDomain.startsWith("http://")) {
            normalizedDomain = normalizedDomain.substring("http://".length());
        }

        while (normalizedDomain.endsWith("/")) {
            normalizedDomain = normalizedDomain.substring(0, normalizedDomain.length() - 1);
        }

        return normalizedDomain;
    }

    private static Map<String, List<String>> getSearchQueryParameters(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return Map.of();
        }

        return Map.of(Q, List.of(searchText));
    }
}
