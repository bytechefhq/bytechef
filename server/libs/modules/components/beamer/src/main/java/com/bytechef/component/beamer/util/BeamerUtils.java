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

package com.bytechef.component.beamer.util;

import static com.bytechef.component.beamer.constant.BeamerConstants.ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.TITLE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BeamerUtils {

    public static List<Option<String>> getPostsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context.http(http -> http.get("/posts"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, TITLE, ID);
    }

    public static List<Option<String>> getFeatureRequestsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context.http(http -> http.get("/requests"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, TITLE, ID);
    }

    private static List<Option<String>> getOptions(List<Map<String, Object>> body, String label, String value) {
        List<Option<String>> options = new ArrayList<>();

        for (Object item : body) {
            if (item instanceof Map<?, ?> map) {
                options
                    .add(option(getTitleofPost((LinkedHashMap) map, label), String.valueOf(map.get(value))));
            }
        }
        return options;
    }

    private static String getTitleofPost(LinkedHashMap post, String label) {
        ArrayList postMap = (ArrayList) post.get("translations");
        LinkedHashMap<String, String> translations = (LinkedHashMap<String, String>) postMap.getFirst();
        return translations.get(label);
    }

    public static List<Option<String>> getPostCategoryOptions(
        Parameters parameters, Parameters parameters1, Map<String, String> stringStringMap, String s,
        ActionContext context) {
        List<Option<String>> options = new ArrayList<>();

        for (BeamerPostCategory category : BeamerPostCategory.values()) {
            options.add(option(category.name(), category.getCategoryName()));
        }

        return options;
    }
}
