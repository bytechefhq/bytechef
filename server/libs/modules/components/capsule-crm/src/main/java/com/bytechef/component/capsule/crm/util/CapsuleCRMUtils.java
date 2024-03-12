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

package com.bytechef.component.capsule.crm.util;

import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.BASE_URL;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.FIRST_NAME_PROPERTY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.LAST_NAME_PROPERTY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.NAME_PROPERTY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class CapsuleCRMUtils {

    private CapsuleCRMUtils() {
    }

    public static List<Property.ValueProperty<?>> createNameProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String type = inputParameters.getRequiredString(TYPE);

        if (type.equals("person")) {
            return List.of(FIRST_NAME_PROPERTY, LAST_NAME_PROPERTY);
        } else {
            return List.of(NAME_PROPERTY);
        }
    }

    public static List<Option<String>> getCountryOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, List<Map<String, Object>>> body =
            context.http(http -> http.get(BASE_URL + "/countries"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new Context.TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body.getOrDefault("countries", List.of())) {
            options.add(option(String.valueOf(map.get("name")), String.valueOf(map.get("name"))));
        }

        return options;
    }

}
