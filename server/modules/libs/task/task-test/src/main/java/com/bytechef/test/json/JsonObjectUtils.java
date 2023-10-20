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

package com.bytechef.test.json;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.json.JSONObject;

/**
 * @author Ivica Cardic
 */
public class JsonObjectUtils {

    public static JSONObject of(String json) {
        return new JSONObject(json);
    }

    public static JSONObject of(Object item) {
        return new JSONObject(item);
    }

    static JSONObject of() {
        return new JSONObject();
    }

    public static <V> JSONObject of(String k1, V v1) {
        return new JSONObject().put(k1, v1);
    }

    public static <V> JSONObject of(String k1, V v1, String k2, V v2) {
        return new JSONObject().put(k1, v1).put(k2, v2);
    }

    public static <V> JSONObject of(String k1, V v1, String k2, V v2, String k3, V v3) {
        return new JSONObject().put(k1, v1).put(k2, v2).put(k3, v3);
    }

    public static <V> JSONObject of(String k1, V v1, String k2, V v2, String k3, V v3, String k4, V v4) {
        return new JSONObject().put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4);
    }

    public static <V> JSONObject of(
            String k1, V v1, String k2, V v2, String k3, V v3, String k4, V v4, String k5, V v5) {
        return new JSONObject().put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).put(k5, v5);
    }

    public static <V> JSONObject of(
            String k1, V v1, String k2, V v2, String k3, V v3, String k4, V v4, String k5, V v5, String k6, V v6) {
        return new JSONObject()
                .put(k1, v1)
                .put(k2, v2)
                .put(k3, v3)
                .put(k4, v4)
                .put(k5, v5)
                .put(k6, v6);
    }

    public static <V> JSONObject of(
            String k1,
            V v1,
            String k2,
            V v2,
            String k3,
            V v3,
            String k4,
            V v4,
            String k5,
            V v5,
            String k6,
            V v6,
            String k7,
            V v7) {
        return new JSONObject()
                .put(k1, v1)
                .put(k2, v2)
                .put(k3, v3)
                .put(k4, v4)
                .put(k5, v5)
                .put(k6, v6)
                .put(k7, v7);
    }

    public static <V> JSONObject of(
            String k1,
            V v1,
            String k2,
            V v2,
            String k3,
            V v3,
            String k4,
            V v4,
            String k5,
            V v5,
            String k6,
            V v6,
            String k7,
            V v7,
            String k8,
            V v8) {
        return new JSONObject()
                .put(k1, v1)
                .put(k2, v2)
                .put(k3, v3)
                .put(k4, v4)
                .put(k5, v5)
                .put(k6, v6)
                .put(k7, v7)
                .put(k8, v8);
    }

    public static <V> JSONObject of(
            String k1,
            V v1,
            String k2,
            V v2,
            String k3,
            V v3,
            String k4,
            V v4,
            String k5,
            V v5,
            String k6,
            V v6,
            String k7,
            V v7,
            String k8,
            V v8,
            String k9,
            V v9) {
        return new JSONObject()
                .put(k1, v1)
                .put(k2, v2)
                .put(k3, v3)
                .put(k4, v4)
                .put(k5, v5)
                .put(k6, v6)
                .put(k7, v7)
                .put(k8, v8)
                .put(k9, v9);
    }

    public static <V> JSONObject of(
            String k1,
            V v1,
            String k2,
            V v2,
            String k3,
            V v3,
            String k4,
            V v4,
            String k5,
            V v5,
            String k6,
            V v6,
            String k7,
            V v7,
            String k8,
            V v8,
            String k9,
            V v9,
            String k10,
            V v10) {
        return new JSONObject()
                .put(k1, v1)
                .put(k2, v2)
                .put(k3, v3)
                .put(k4, v4)
                .put(k5, v5)
                .put(k6, v6)
                .put(k7, v7)
                .put(k8, v8)
                .put(k9, v9)
                .put(k10, v10);
    }

    public static JSONObject of(String key, Map<?, ?> value) {
        return new JSONObject().put(key, value);
    }

    public static JSONObject of(String key, Collection<?> value) {
        return new JSONObject().put(key, value);
    }

    public static JSONObject of(Map<?, ?> map) {
        return new JSONObject(map);
    }

    public static <T> JSONObject of(Map<String, T> map, Function<T, ?> valueProcessor) {
        JSONObject JSONObject;

        if (valueProcessor == null) {
            JSONObject = of(map);
        } else {
            JSONObject = new JSONObject();

            for (Map.Entry<String, T> entry : map.entrySet()) {
                JSONObject.put(entry.getKey(), valueProcessor.apply(entry.getValue()));
            }
        }

        return JSONObject;
    }

    public static <T> JSONObject of(List<T> values) {
        return of(values, null);
    }

    public static <T> JSONObject of(List<T> values, Function<T, ?> valueProcessor) {
        JSONObject jsonObject = new JSONObject();

        int count = 1;

        for (T value : values) {
            jsonObject.put("column_" + count++, valueProcessor == null ? value : valueProcessor.apply(value));
        }

        return jsonObject;
    }

    public static Map<String, Object> toMap(String json) {
        JSONObject jsonObject = new JSONObject(json);

        return jsonObject.toMap();
    }
}
