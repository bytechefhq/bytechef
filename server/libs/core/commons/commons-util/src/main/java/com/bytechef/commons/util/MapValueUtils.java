
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

package com.bytechef.commons.util;

import com.bytechef.commons.typeconverter.TypeConverter;

import java.lang.reflect.Array;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public final class MapValueUtils {

    private MapValueUtils() {
    }

    public static Map<String, ?> append(Map<String, ?> map, String key, Map<String, ?> values) {
        Objects.requireNonNull(key, "'key' must not be null");
        Objects.requireNonNull(values, "'values' must not be null");

        Map<String, Object> submap = new HashMap<>(getMap(map, key, Map.of()));

        submap.putAll(values);

        Map<String, Object> newMap = new HashMap<>(map);

        newMap.put(key, submap);

        return Collections.unmodifiableMap(newMap);
    }

    public static boolean containsKey(Map<String, ?> map, String key) {
        Objects.requireNonNull(map, "'map' must not be null");

        return map.containsKey(key);
    }

    public static Object get(Map<String, ?> map, String key) {
        Objects.requireNonNull(map, "'map' must not be null");

        return map.get(key);
    }

    public static <T> T get(Map<String, ?> map, String key, Class<T> returnType) {
        Object value = get(map, key);

        if (value == null) {
            return null;
        }

        return convert(value, returnType);
    }

    public static <T> T get(Map<String, ?> map, String key, Class<T> returnType, T defaultValue) {
        T value = get(map, key, returnType);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    public static Object[] getArray(Map<String, ?> map, String key) {
        Object value = get(map, key);

        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            return (Object[]) value;
        }

        List<?> list = getList(map, key);

        if (list == null) {
            return null;
        }

        return list.toArray((Object[]) Array.newInstance(Object.class, 0));
    }

    public static Object[] getArray(Map<String, ?> map, String key, Object[] defaultValue) {
        Object value = get(map, key);

        if (value == null) {
            return defaultValue;
        }

        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            return (Object[]) value;
        }

        List<?> list = getList(map, key);

        if (list == null) {
            return defaultValue;
        }

        return list.toArray((Object[]) Array.newInstance(Object.class, 0));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] getArray(Map<String, ?> map, String key, Class<T> elementType) {
        Object value = get(map, key);

        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        Class<?> valueComponentType = valueClass.getComponentType();

        if (valueClass.isArray() && valueComponentType.equals(elementType)) {
            return (T[]) value;
        }

        List<T> list = getList(map, key, elementType);

        if (list == null) {
            return null;
        }

        return list.toArray((T[]) Array.newInstance(elementType, 0));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] getArray(Map<String, ?> map, String key, Class<T> elementType, T[] defaultValue) {
        Object value = get(map, key);

        if (value == null) {
            return defaultValue;
        }

        Class<?> valueClass = value.getClass();

        Class<?> valueComponentType = valueClass.getComponentType();

        if (valueClass.isArray() && valueComponentType.equals(elementType)) {
            return (T[]) value;
        }

        List<T> list = getList(map, key, elementType);

        if (list == null) {
            return defaultValue;
        }

        return list.toArray((T[]) Array.newInstance(elementType, 0));
    }

    public static Boolean getBoolean(Map<String, ?> map, String key) {
        return get(map, key, Boolean.class);
    }

    public static boolean getBoolean(Map<String, ?> map, String key, boolean defaultValue) {
        Boolean value = getBoolean(map, key);

        return value != null ? value : defaultValue;
    }

    public static Date getDate(Map<String, ?> map, String key) {
        return get(map, key, Date.class);
    }

    public static Date getDate(Map<String, ?> map, String key, Date defaultValue) {
        Date date = getDate(map, key);

        if (date == null) {
            date = defaultValue;
        }

        return date;
    }

    public static Double getDouble(Map<String, ?> map, String key) {
        return get(map, key, Double.class);
    }

    public static Double getDouble(Map<String, ?> map, String key, double defaultValue) {
        return get(map, key, Double.class, defaultValue);
    }

    public static Duration getDuration(Map<String, ?> map, String key) {
        return get(map, key, Duration.class);
    }

    public static Duration getDuration(Map<String, ?> map, String key, Duration defaultDuration) {
        Duration value = getDuration(map, key);

        return value != null ? value : defaultDuration;
    }

    public static Float getFloat(Map<String, ?> map, String key) {
        return get(map, key, Float.class);
    }

    public static float getFloat(Map<String, ?> map, String key, float defaultValue) {
        return get(map, key, Float.class, defaultValue);
    }

    public static Integer getInteger(Map<String, ?> map, String key) {
        return get(map, key, Integer.class);
    }

    public static int getInteger(Map<String, ?> map, String key, int defaultValue) {
        return get(map, key, Integer.class, defaultValue);
    }

    public static List<?> getList(Map<String, ?> map, String key) {
        return get(map, key, List.class);
    }

    public static List<?> getList(Map<String, ?> map, String key, List<?> defaultValue) {
        List<?> list = get(map, key, List.class);

        return list == null ? defaultValue : list;
    }

    public static <T> List<T> getList(Map<String, ?> map, String key, Class<T> elementType) {
        List<?> list = get(map, key, List.class);

        if (list == null) {
            return null;
        }

        List<T> typedList = new ArrayList<>();

        for (Object item : list) {
            typedList.add(convert(item, elementType));
        }

        return Collections.unmodifiableList(typedList);
    }

    public static <T> List<T> getList(Map<String, ?> map, String key, Class<T> elementType, List<T> defaultValue) {
        List<T> list = getList(map, key, elementType);

        return list != null ? list : defaultValue;
    }

    public static List<?> getList(Map<String, ?> map, String key, Class<?>[] elementTypes) {
        List<?> list = get(map, key, List.class);

        if (list != null) {
            list = list.stream()
                .map(value -> convert(value, Arrays.asList(elementTypes)))
                .toList();
        }

        return list;
    }

    public static List<?> getList(
        Map<String, ?> map, String key, List<Class<?>> elementTypes, List<?> defaultValue) {

        List<?> list = get(map, key, List.class);

        if (list == null) {
            list = defaultValue;
        } else {
            list = list.stream()
                .map(value -> convert(value, elementTypes))
                .toList();
        }

        return list;
    }

    public static LocalDate getLocalDate(Map<String, ?> map, String key) {
        return get(map, key, LocalDate.class);
    }

    public static LocalDate getLocalDate(Map<String, ?> map, String key, LocalDate defaultValue) {
        LocalDate localDate = getLocalDate(map, key);

        if (localDate == null) {
            localDate = defaultValue;
        }

        return localDate;
    }

    public static LocalDateTime getLocalDateTime(Map<String, ?> map, String key) {
        return get(map, key, LocalDateTime.class);
    }

    public static LocalDateTime getLocalDateTime(Map<String, ?> map, String key, LocalDateTime defaultValue) {
        LocalDateTime localDateTime = getLocalDateTime(map, key);

        if (localDateTime == null) {
            localDateTime = defaultValue;
        }

        return localDateTime;
    }

    public static LocalTime getLocalTime(Map<String, ?> map, String key) {
        return get(map, key, LocalTime.class);
    }

    public static LocalTime getLocalTime(Map<String, ?> map, String key, LocalTime defaultValue) {
        LocalTime localTime = getLocalTime(map, key);

        if (localTime == null) {
            localTime = defaultValue;
        }

        return localTime;
    }

    public static Long getLong(Map<String, ?> map, String key) {
        return get(map, key, Long.class);
    }

    public static long getLong(Map<String, ?> map, String key, long defaultValue) {
        return get(map, key, Long.class, defaultValue);
    }

    public static Map<String, ?> getMap(Map<String, ?> map, String key) {
        Map<?, ?> value = get(map, key, Map.class);

        if (value == null) {
            return null;
        }

        return Collections.unmodifiableMap(
            value.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> (String) entry.getKey(), Map.Entry::getValue)));
    }

    public static Map<String, ?> getMap(Map<String, ?> map, String key, Map<String, ?> defaultValue) {
        Map<?, ?> value = get(map, key, Map.class);

        if (value == null) {
            return Collections.unmodifiableMap(defaultValue);
        }

        return Collections.unmodifiableMap(
            value.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> (String) entry.getKey(), Map.Entry::getValue)));
    }

    public static <V> Map<String, V> getMap(Map<String, ?> map, String key, Class<V> valueType) {
        Map<?, ?> value = get(map, key, Map.class);

        if (value == null) {
            return null;
        }

        return Collections.unmodifiableMap(
            value.entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        entry -> (String) entry.getKey(),
                        entry -> convert(entry.getValue(), valueType))));
    }

    public static <V> Map<String, V> getMap(
        Map<String, ?> map, String key, Class<V> valueType, Map<String, V> defaultValue) {

        Map<String, V> value = getMap(map, key, valueType);

        return value == null ? Collections.unmodifiableMap(defaultValue) : value;
    }

    public static Map<String, ?> getMap(Map<String, ?> map, String key, List<Class<?>> valueTypes) {
        Map<String, ?> mapValue = getMap(map, key);

        if (mapValue != null) {
            mapValue = mapValue.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> convert(entry.getValue(), valueTypes)));
        }

        return mapValue;
    }

    public static Map<String, ?> getMap(
        Map<String, ?> map, String key, List<Class<?>> valueTypes, Map<String, ?> defaultValue) {

        Map<String, ?> mapValue = getMap(map, key);

        if (mapValue == null) {
            mapValue = Collections.unmodifiableMap(defaultValue);
        } else {
            mapValue = mapValue.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> convert(entry.getValue(), valueTypes)));
        }

        return mapValue;
    }

    public static Object getRequired(Map<String, ?> map, String key) {
        Object value = get(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <T> T getRequired(Map<String, ?> map, String key, Class<T> returnType) {
        T value = get(map, key, returnType);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Object[] getRequiredArray(Map<String, ?> map, String key) {
        Object[] value = getArray(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <T> T[] getRequiredArray(Map<String, ?> map, String key, Class<T> elementType) {
        T[] value = getArray(map, key, elementType);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Boolean getRequiredBoolean(Map<String, ?> map, String key) {
        Boolean value = getBoolean(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Date getRequiredDate(Map<String, ?> map, String key) {
        Date value = getDate(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Double getRequiredDouble(Map<String, ?> map, String key) {
        Double value = getDouble(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Float getRequiredFloat(Map<String, ?> map, String key) {
        Float value = getFloat(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Integer getRequiredInteger(Map<String, ?> map, String key) {
        Integer value = getInteger(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static List<?> getRequiredList(Map<String, ?> map, String key) {
        List<?> value = getList(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <T> List<T> getRequiredList(Map<String, ?> map, String key, Class<T> elementType) {
        List<T> value = getList(map, key, elementType);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static LocalDate getRequiredLocalDate(Map<String, ?> map, String key) {
        LocalDate value = getLocalDate(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static LocalTime getRequiredLocalTime(Map<String, ?> map, String key) {
        LocalTime value = getLocalTime(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static LocalDateTime getRequiredLocalDateTime(Map<String, ?> map, String key) {
        LocalDateTime value = getLocalDateTime(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <V> Map<String, V> getRequiredMap(Map<String, ?> map, String key, Class<V> valueType) {
        Map<String, V> value = getMap(map, key, valueType);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Map<String, ?> getRequiredMap(Map<String, ?> map, String key) {
        Map<String, ?> value = getMap(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static String getRequiredString(Map<String, ?> map, String key) {
        String value = getString(map, key);

        Objects.requireNonNull(value, "Unknown value for : " + key);

        return value;
    }

    public static String getString(Map<String, ?> map, String key) {
        return get(map, key, String.class);
    }

    public static String getString(Map<String, ?> map, String key, String defaultValue) {
        String value = getString(map, key);

        return value != null ? value : defaultValue;
    }

    private static <T> T convert(Object value, Class<T> elementType) {
        return TypeConverter.convert(elementType, value);
    }

    private static Object convert(Object value, List<Class<?>> elementTypes) {
        for (Class<?> elementType : elementTypes) {
            value = convert(value, elementType);

            if (value.getClass() == elementType) {
                break;
            }
        }

        return value;
    }
}
