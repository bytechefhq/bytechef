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

package com.bytechef.component.sendfox.util;

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
public class SendfoxUtils extends AbstractSendfoxUtils {
    private SendfoxUtils() {
    }

    public static List<Option<String>> getEmailOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> body = context.http(http -> http.get("/contacts"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("data") instanceof List<?> contacts) {
            for (Object contactObject : contacts) {
                if (contactObject instanceof Map<?, ?> contact) {
                    String email = (String) contact.get("email");

                    options.add(option(email, email));
                }
            }
        }

        return options;
    }

    public static List<Option<Long>> getListsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<Long>> options = new ArrayList<>();

        Map<String, Object> body = context.http(http -> http.get("/lists"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("data") instanceof List<?> lists) {
            for (Object listObject : lists) {
                if (listObject instanceof Map<?, ?> list) {
                    options.add(option((String) list.get("name"), ((Integer) list.get("id")).intValue()));
                }
            }
        }

        return options;
    }
}
