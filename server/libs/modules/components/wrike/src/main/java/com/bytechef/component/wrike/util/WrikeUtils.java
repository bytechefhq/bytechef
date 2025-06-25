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

package com.bytechef.component.wrike.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.wrike.constant.WrikeConstants.DATA;
import static com.bytechef.component.wrike.constant.WrikeConstants.FIRST_NAME;
import static com.bytechef.component.wrike.constant.WrikeConstants.ID;
import static com.bytechef.component.wrike.constant.WrikeConstants.PARENT;
import static com.bytechef.component.wrike.constant.WrikeConstants.TITLE;

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
public class WrikeUtils {

    public static List<Option<String>> getContactIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> response = context.http(http -> http.get("/contacts"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (response.get(DATA) instanceof List<?> contacts) {
            for (Object contactObject : contacts) {
                if (contactObject instanceof Map<?, ?> contact) {
                    options.add(option((String) contact.get(FIRST_NAME), (String) contact.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getParentIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String uri = inputParameters.getString(PARENT) == null ? "folders" : inputParameters.getString(PARENT);

        Map<String, Object> response = context.http(http -> http.get("/%s".formatted(uri)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.get(DATA) instanceof List<?> parents) {
            for (Object parent : parents) {
                if (parent instanceof Map<?, ?> parentMap) {
                    Object scope = parentMap.get("scope");

                    if (!scope.equals("RbFolder")) {
                        options.add(option((String) parentMap.get(TITLE), (String) parentMap.get(ID)));
                    }
                }
            }
        }

        return options;
    }
}
