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

package com.bytechef.component.zoominfo.util;

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
 * @author Marija Horvat
 */
public class ZoominfoUtils {

    public static List<Option<String>> getCompanyFieldOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/lookup/outputfields/company/enrich"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body) {
            if ((boolean) map.get("accessGranted")) {
                options.add(option((String) map.get("fieldName"), (String) map.get("fieldName")));
            }
        }

        return options;
    }

    public static List<Option<String>> getContactFieldOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/lookup/outputfields/contact/enrich"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body) {
            if ((boolean) map.get("accessGranted")) {
                options.add(option((String) map.get("fieldName"), (String) map.get("fieldName")));
            }
        }

        return options;
    }

    private ZoominfoUtils() {
    }
}
