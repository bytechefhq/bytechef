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

package com.bytechef.component.scrapingbee.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ScrapingBeeTestUtils {

    private ScrapingBeeTestUtils() {
    }

    public static Map<String, Object> toQueryParameterMap(Object[] keyValueArray) {
        return toQueryParameterMap(Arrays.asList(keyValueArray));
    }

    public static Map<String, Object> toQueryParameterMap(List<Object> keyValueList) {
        Map<String, Object> queryParameters = new HashMap<>();

        for (int index = 0; index < keyValueList.size(); index += 2) {
            Object value = keyValueList.get(index + 1);

            if (value != null) {
                queryParameters.put(String.valueOf(keyValueList.get(index)), value);
            }
        }

        return queryParameters;
    }
}
