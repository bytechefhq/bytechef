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

package com.bytechef.component.neon.util;

import static com.bytechef.component.neon.constant.NeonConstants.FILTERS;

import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeonUtils {

    private NeonUtils() {
    }

    public static Map<String, List<String>> getFilterQueryParameters(Parameters inputParameters) {
        List<String> filters = inputParameters.getList(FILTERS, String.class, Collections.emptyList());

        Map<String, List<String>> queryParameters = new LinkedHashMap<>();

        for (String filter : filters) {
            int index = filter.indexOf('=');

            if (index < 0) {
                throw new IllegalArgumentException(
                    "Invalid filter \"" + filter + "\": expected the format column=operator.value, e.g. " +
                        "id=eq.1");
            }

            queryParameters
                .computeIfAbsent(filter.substring(0, index), key -> new ArrayList<>())
                .add(filter.substring(index + 1));
        }

        return queryParameters;
    }

    public static Object getFirstRow(Object body) {
        if (body instanceof List<?> rows) {
            return rows.isEmpty() ? null : rows.get(0);
        }

        return body;
    }
}
