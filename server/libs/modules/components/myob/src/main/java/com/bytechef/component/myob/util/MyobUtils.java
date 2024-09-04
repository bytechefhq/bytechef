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

package com.bytechef.component.myob.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.myob.constant.MyobConstants.COMPANY_FILE;
import static com.bytechef.component.myob.constant.MyobConstants.COMPANY_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.FIRST_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.LAST_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.UID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MyobUtils {

    private MyobUtils() {
    }

    public static List<Option<String>> getCompanyFileOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("https://api.myob.com/accountright"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> item : body) {
            options.add(option((String) item.get("Name"), (String) item.get("Uri")));
        }

        return options;
    }

    public static List<Option<String>> getCustomerOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get(inputParameters.getRequiredString(COMPANY_FILE) + "/Contact/Customer"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> item : body.get("Items")) {
            options.add(option(item.get(FIRST_NAME) + " " + item.get(LAST_NAME), (String) item.get(UID)));
        }

        return options;
    }

    public static List<Option<String>> getSupplierOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get(inputParameters.getRequiredString(COMPANY_FILE) + "/Contact/Supplier"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> item : body.get("Items")) {
            options.add(option((String) item.get(COMPANY_NAME), (String) item.get(UID)));
        }

        return options;
    }

}
