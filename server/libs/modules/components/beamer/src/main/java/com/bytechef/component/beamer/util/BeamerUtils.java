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

package com.bytechef.component.beamer.util;

import static com.bytechef.component.beamer.constant.BeamerConstants.ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.TITLE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nikolina Spehar
 */
public class BeamerUtils {

    protected static final ContextFunction<Http, Executor> GET_POSTS_CONTEXT_FUNCTION =
        http -> http.get("/posts");

    protected static final ContextFunction<Http, Executor> GET_FEATURE_REQUESTS_CONTEXT_FUNCTION =
        http -> http.get("/requests");

    public static List<Option<String>> getPostsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context.http(GET_POSTS_CONTEXT_FUNCTION)
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getFeatureRequestsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context.http(GET_FEATURE_REQUESTS_CONTEXT_FUNCTION)
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    private static List<Option<String>> getOptions(List<Map<String, Object>> body) {
        List<Option<String>> options = new ArrayList<>();

        for (Object item : body) {
            if (item instanceof Map<?, ?> map && map.get("translations") instanceof List<?> translations &&
                translations.getFirst() instanceof Map<?, ?> translationMap) {

                options.add(option((String) translationMap.get(TITLE), String.valueOf(map.get(ID))));
            }
        }

        return options;
    }

    public static List<Option<String>> getPostCategoryOptions() {
        return Arrays.stream(BeamerPostCategory.values())
            .map(category -> option(category.getLabel(), category.getValue()))
            .collect(Collectors.toList());
    }
}
