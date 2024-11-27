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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.NAME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365OptionUtils {

    private MicrosoftOutlook365OptionUtils() {
    }

    public static List<Option<String>> getCalendarOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) {

        Map<String, Object> body = actionContext
            .http(http -> http.get("/calendars"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(actionContext, body, NAME, ID);
    }

    public static List<Option<String>> getCategoryOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) {

        Map<String, Object> body = actionContext
            .http(http -> http.get("/outlook/masterCategories"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(actionContext, body, "displayName", "displayName");
    }

    public static List<Option<String>> getEventOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) {

        Map<String, Object> body = actionContext
            .http(http -> http.get("/calendars/" + inputParameters.getRequiredString(CALENDAR) + "/events"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(actionContext, body, SUBJECT, ID);
    }

    public static List<Option<String>> getMessageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) {

        Map<String, Object> body = actionContext.http(http -> http.get("/messages"))
            .queryParameters("$top", 100)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(actionContext, body, ID, ID);
    }

    private static List<Option<String>> getOptions(
        ActionContext actionContext, Map<String, Object> body, String label, String value) {

        List<Option<String>> options = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> map) {

                    options.add(option((String) map.get(label), (String) map.get(value)));
                }
            }
        }

        List<Map<?, ?>> categoriesFromNextPage =
            MicrosoftOutlook365Utils.getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), actionContext);

        for (Map<?, ?> map : categoriesFromNextPage) {
            options.add(option((String) map.get(label), (String) map.get(value)));
        }

        return options;
    }
}
