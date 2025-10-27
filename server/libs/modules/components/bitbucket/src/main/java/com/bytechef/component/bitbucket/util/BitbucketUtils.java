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

package com.bytechef.component.bitbucket.util;

import static com.bytechef.component.bitbucket.constant.BitbucketConstants.KEY;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.NAME;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.PAGE;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.SLUG;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.VALUES;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.WORKSPACE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BitbucketUtils extends AbstractBitbucketUtils {

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getPaginationList(Context context, String url) {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> response;

        do {
            int page = 0;

            response = context.http(http -> http.get(url))
                .queryParameter(PAGE, String.valueOf(++page))
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (response.get(VALUES) instanceof List<?> values) {
                for (Object value : values) {
                    if (value instanceof Map<?, ?> valueMap)
                        list.add((Map<String, Object>) valueMap);
                }
            }

        } while (response.get("next") != null);

        return list;
    }

    private static List<Option<String>> getPaginationValues(
        Context context, String startUrl, String optionLabel, String optionValue) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> response;

        do {
            int page = 0;

            response = context.http(http -> http.get(startUrl))
                .queryParameter(PAGE, String.valueOf(++page))
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (response.get(VALUES) instanceof List<?> values) {
                for (Object value : values) {
                    if (value instanceof Map<?, ?> valueMap)
                        options.add(option((String) valueMap.get(optionLabel), (String) valueMap.get(optionValue)));
                }
            }

        } while (response.get("next") != null);

        return options;
    }

    public static List<Option<String>> getRepositoryOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginationValues(
            context, "/repositories/%s".formatted(inputParameters.getRequiredString(WORKSPACE)), NAME, SLUG);
    }

    public static List<Option<String>> getKeyOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginationValues(
            context, "/workspaces/%s/projects".formatted(inputParameters.getRequiredString(WORKSPACE)), NAME, KEY);
    }

    public static List<Option<String>> getWorkspaceOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginationValues(context, "/workspaces", NAME, NAME);
    }

    private BitbucketUtils() {
    }
}
