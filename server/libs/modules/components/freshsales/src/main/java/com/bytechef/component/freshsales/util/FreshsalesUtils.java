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

package com.bytechef.component.freshsales.util;

import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class FreshsalesUtils {

    private FreshsalesUtils() {
    }

    public static Map<String, List<String>> getHeaders(Parameters connectionParameters) {
        return Map.of(
            "Authorization", List.of("Token token=" + connectionParameters.getRequiredString(KEY)));
    }

    public static String getUrl(Parameters connectionParameters, String resource) {
        return "https://" + connectionParameters.getRequiredString(USERNAME) + ".myfreshworks.com/crm/sales/api/"
            + resource;
    }
}
