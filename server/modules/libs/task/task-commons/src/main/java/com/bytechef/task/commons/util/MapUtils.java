/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.commons.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Ivica Cardic
 */
public class MapUtils {

    public static <T> Map<String, ?> of(Map<String, T> map, Function<T, ?> valueProcessor) {
        Map<String, Object> resultMap = new HashMap<>(map);

        if (valueProcessor != null) {
            for (Map.Entry<String, T> entry : map.entrySet()) {
                resultMap.put(entry.getKey(), valueProcessor.apply(entry.getValue()));
            }
        }

        return resultMap;
    }

    public static <T> Map<String, ?> of(List<T> values) {
        return of(values, null);
    }

    public static <T> Map<String, ?> of(List<T> values, Function<T, ?> valueProcessor) {
        Map<String, Object> map = new HashMap<>();

        int count = 1;

        for (T value : values) {
            map.put("column_" + count++, valueProcessor == null ? value : valueProcessor.apply(value));
        }

        return map;
    }
}
