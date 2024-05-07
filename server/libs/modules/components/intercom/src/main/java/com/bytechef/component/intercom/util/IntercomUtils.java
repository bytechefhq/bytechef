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

import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.intercom.constant.IntercomConstants.BASE_URL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.TypeReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IntercomUtils {

    public static Map<String, String> getContactRole(String id, ActionContext context) {
        Map<String, Object> body = context.http(http -> http.get(BASE_URL + "/contacts/" + id))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, String> contactMap = new LinkedHashMap<>();

        Object type = body.get("role");

        contactMap.put("type", (String) type);
        contactMap.put("id", id);

        return contactMap;
    }

    public static Map<String, String> getAdminId(ActionContext context) {

        Map<String, Object> body = context.http(http -> http.get(BASE_URL + "/admins"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});

        Map<String, String> adminMap = new LinkedHashMap<>();

        if (body != null && body.get("admins") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    adminMap.put("type", "admin");
                    adminMap.put("id", (String) map.get("id"));
                }
            }
        }
        return adminMap;
    }
}
