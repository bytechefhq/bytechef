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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365Utils {

    private MicrosoftOutlook365Utils() {
    }

    public static List<Option<String>> getCategoryOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("/outlook/masterCategories"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> map) {
                    String displayName = (String) map.get("displayName");

                    options.add(option(displayName, displayName));
                }
            }
        }

        List<Map<?, ?>> otherItems = getItemsFromNextPage(context, (String) body.get(ODATA_NEXT_LINK));

        for (Map<?, ?> map : otherItems) {
            String displayName = (String) map.get("displayName");

            options.add(option(displayName, displayName));
        }

        return options;
    }

    public static List<Option<String>> getMessageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> body = context.http(http -> http.get("/messages"))
            .queryParameters("$top", 100)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    String id = (String) map.get(ID);

                    options.add(option(id, id));
                }
            }
        }

        List<Map<?, ?>> otherItems = getItemsFromNextPage(context, (String) body.get(ODATA_NEXT_LINK));

        for (Map<?, ?> map : otherItems) {
            String id = (String) map.get(ID);

            options.add(option(id, id));
        }

        return options;
    }

    public static List<Map<?, ?>> getItemsFromNextPage(Context context, String link) {
        List<Map<?, ?>> otherItems = new ArrayList<>();

        while (link != null && !link.isEmpty()) {

            String finalLink = link;

            Map<String, Object> body = context.http(http -> http.get(finalLink))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get(VALUE) instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        otherItems.add(map);
                    }
                }
            }
            link = (String) body.get(ODATA_NEXT_LINK);
        }

        return otherItems;
    }

}
