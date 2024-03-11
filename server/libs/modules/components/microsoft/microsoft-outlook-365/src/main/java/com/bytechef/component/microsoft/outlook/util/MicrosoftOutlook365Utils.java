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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365Utils {

    private MicrosoftOutlook365Utils() {
    }

    public static List<Option<String>> getCategoryOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("https://graph.microsoft.com/v1.0/me/outlook/masterCategories"))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});

        ArrayList<LinkedHashMap<String, String>> value = (ArrayList<LinkedHashMap<String, String>>) body.get("value");

        List<Option<String>> options = new ArrayList<>();

        for (LinkedHashMap<String, String> linkedHashMap : value) {
            String displayName = linkedHashMap.get("displayName");

            options.add(option(displayName, displayName));
        }

        return options;
    }

    public static List<Option<String>> getMessageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("https://graph.microsoft.com/v1.0/me/messages"))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});

        ArrayList<LinkedHashMap<String, String>> value = (ArrayList<LinkedHashMap<String, String>>) body.get("value");

        List<Option<String>> options = new ArrayList<>();

        for (LinkedHashMap<String, String> linkedHashMap : value) {
            String id = linkedHashMap.get("id");

            options.add(option(id, id));
        }

        return options;
    }

}
