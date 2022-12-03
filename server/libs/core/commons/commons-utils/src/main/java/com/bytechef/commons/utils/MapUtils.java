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

package com.bytechef.commons.utils;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class MapUtils {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final DefaultConversionService conversionService = new DefaultConversionService();

    public static Object get(Map<String, Object> map, String key) {
        return map.get(key);
    }

    public static <T> T get(Map<String, Object> map, String key, Class<T> returnType) {
        Object value = get(map, key);

        if (value == null) {
            return null;
        }

        return conversionService.convert(value, returnType);
    }

    public static <T> T get(Map<String, Object> map, String key, Class<T> returnType, T defaultValue) {
        Object value = get(map, key);

        if (value == null) {
            return defaultValue;
        }

        return conversionService.convert(value, returnType);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] getArray(Map<String, Object> map, String key, Class<T> elementType) {
        Object value = get(map, key);

        if (value.getClass().isArray() && value.getClass().getComponentType().equals(elementType)) {
            return (T[]) value;
        }

        List<T> list = getList(map, key, elementType);

        return list.toArray((T[]) Array.newInstance(elementType, 0));
    }

    public static Boolean getBoolean(Map<String, Object> map, String key) {
        return get(map, key, Boolean.class);
    }

    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Boolean value = getBoolean(map, key);
        return value != null ? value : defaultValue;
    }

    public static Date getDate(Map<String, Object> map, String key) {
        Object value = get(map, key);

        if (value instanceof String) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);

            try {
                return dateFormat.parse((String) value);
            } catch (ParseException parseException) {
                throw new RuntimeException(parseException);
            }
        }

        return (Date) value;
    }

    public static Double getDouble(Map<String, Object> map, String key) {
        return get(map, key, Double.class);
    }

    public static Double getDouble(Map<String, Object> map, String key, double defaultValue) {
        return get(map, key, Double.class, defaultValue);
    }

    public static Duration getDuration(Map<String, Object> map, String key) {
        String value = getString(map, key);

        if (value == null) {
            return null;
        }

        return Duration.parse("PT" + value);
    }

    public static Duration getDuration(Map<String, Object> map, String key, String defaultDuration) {
        Duration value = getDuration(map, key);

        return value != null ? value : Duration.parse("PT" + defaultDuration);
    }

    public static Float getFloat(Map<String, Object> map, String key) {
        return get(map, key, Float.class);
    }

    public static float getFloat(Map<String, Object> map, String key, float defaultValue) {
        return get(map, key, Float.class, defaultValue);
    }

    public static Integer getInteger(Map<String, Object> map, String key) {
        return get(map, key, Integer.class);
    }

    public static int getInteger(Map<String, Object> map, String key, int defaultValue) {
        return get(map, key, Integer.class, defaultValue);
    }

    public static <T> List<T> getList(Map<String, Object> map, String key, Class<T> elementType) {
        List<?> list = get(map, key, List.class);

        if (list == null) {
            return Collections.emptyList();
        }

        List<T> typedList = new ArrayList<>();

        for (Object item : list) {
            typedList.add(conversionService.convert(item, elementType));
        }

        return Collections.unmodifiableList(typedList);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(Map<String, Object> map, String key, ParameterizedTypeReference<T> elementType) {
        return getList(map, key, (Class<T>) ((ParameterizedType) elementType.getType()).getRawType());
    }

    public static <T> List<T> getList(Map<String, Object> map, String key, Class<T> elementType, List<T> defaultValue) {
        List<T> list = getList(map, key, elementType);

        return list != null ? list : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(
            Map<String, Object> map, String key, ParameterizedTypeReference<T> elementType, List<T> defaultValue) {
        return getList(map, key, (Class<T>) ((ParameterizedType) elementType.getType()).getRawType(), defaultValue);
    }

    public static Long getLong(Map<String, Object> map, String key) {
        return get(map, key, Long.class);
    }

    public static long getLong(Map<String, Object> map, String key, long defaultValue) {
        return get(map, key, Long.class, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Map<String, Object> value = (Map<String, Object>) get(map, key);

        if (value == null) {
            return null;
        }

        return Collections.unmodifiableMap(value);
    }

    public static Map<String, Object> getMap(Map<String, Object> map, String key, Map<String, Object> defaultValue) {
        Map<String, Object> value = getMap(map, key);

        return value != null ? value : defaultValue;
    }

    public static String getRequiredString(Map<String, Object> map, String key) {
        String value = getString(map, key);

        Assert.notNull(value, "Unknown key: " + key);

        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getRequired(Map<String, Object> map, String key) {
        T value = (T) get(map, key);

        Assert.notNull(value, "Unknown key: " + key);

        return value;
    }

    public static <T> T getRequired(Map<String, Object> map, String key, Class<T> returnType) {
        T value = get(map, key, returnType);

        Assert.notNull(value, "Unknown key: " + key);

        return value;
    }

    public static String getString(Map<String, Object> map, String key) {
        return conversionService.convert(get(map, key), String.class);
    }

    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);

        return value != null ? value : defaultValue;
    }
}
