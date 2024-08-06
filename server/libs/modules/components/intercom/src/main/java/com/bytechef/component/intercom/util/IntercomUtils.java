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

package com.bytechef.component.intercom.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.intercom.constant.IntercomConstants.ID;
import static com.bytechef.component.intercom.constant.IntercomConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IntercomUtils {

    protected static final ContextFunction<Http, Http.Executor> GET_ADMINS_CONTEXT_FUNCTION =
        http -> http.get("/admins");

    protected static final ContextFunction<Http, Http.Executor> GET_CONTACTS_CONTEXT_FUNCTION =
        http -> http.get("/contacts");

    private IntercomUtils() {
    }

    public static Map<String, String> getAdminId(ActionContext context) {

        Map<String, Object> body = context.http(GET_ADMINS_CONTEXT_FUNCTION)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, String> adminMap = new LinkedHashMap<>();

        if (body != null && body.get("admins") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    adminMap.put(TYPE, "admin");
                    adminMap.put(ID, (String) map.get(ID));
                }
            }
        }
        return adminMap;
    }

    public static List<Option<String>> getContactIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context.http(GET_CONTACTS_CONTEXT_FUNCTION)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body != null && body.get("data") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static Map<String, String> getContactRole(String id, ActionContext context) {
        Map<String, Object> body = context.http(http -> http.get("/contacts/" + id))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, String> contactMap = new LinkedHashMap<>();

        Object type = body.get("role");

        contactMap.put(TYPE, (String) type);
        contactMap.put(ID, id);

        return contactMap;
    }
}
