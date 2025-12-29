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

package com.bytechef.commons.util;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Array;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
public class MapUtils {

    private static final Logger logger = LoggerFactory.getLogger(MapUtils.class);

    private static ObjectMapper objectMapper;

    public static <K> Map<K, ?> append(Map<K, ?> map, K key, Map<K, ?> values) {
        Validate.notNull(key, "'key' must not be null");
        Validate.notNull(values, "'values' must not be null");

        Map<K, Object> submap = new HashMap<>(getMap(map, key, Map.of()));

        submap.putAll(values);

        Map<K, Object> newMap = new HashMap<>(map);

        newMap.put(key, submap);

        return Collections.unmodifiableMap(newMap);
    }

    public static <K, V> Map<K, V> concat(Map<K, V> map1, Map<K, V> map2) {
        Validate.notNull(map1, "'map1' must not be null");
        Validate.notNull(map2, "'map2' must not be null");

        return Stream
            .concat(
                stream(map1)
                    .filter(entry -> entry.getValue() != null),
                stream(map2)
                    .filter(entry -> entry.getValue() != null))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
    }

    public static <K> boolean containsKey(Map<K, ?> map, String key) {
        Validate.notNull(map, "'map' must not be null");

        return map.containsKey(key);
    }

    public static boolean containsPath(Map<String, ?> map, String path) {
        try {
            JsonPath.read(map, path);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    public static boolean isEmpty(Map<String, ?> map) {
        return map == null || map.isEmpty();
    }

    public static <K> Object get(Map<K, ?> map, K key) {
        Validate.notNull(map, "'map' must not be null");

        return map.get(key);
    }

    public static <K, T> @Nullable T get(Map<K, ?> map, K key, Class<T> returnType) {
        Object value = get(map, key);

        if (value == null) {
            return null;
        }

        return convert(value, returnType);
    }

    public static <K, T> T get(Map<K, ?> map, K key, Class<T> returnType, T defaultValue) {
        T value = get(map, key, returnType);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    public static <K, T> @Nullable T get(Map<K, ?> map, K key, TypeReference<T> elementTypeRef) {
        Validate.notNull(map, "'map' must not be null");

        Object value = get(map, key);

        if (value == null) {
            return null;
        }

        return convert(value, elementTypeRef);
    }

    public static <K, T> T get(Map<K, ?> map, K key, TypeReference<T> elementTypeRef, T defaultValue) {
        Validate.notNull(map, "'map' must not be null");

        Object value = get(map, key);

        if (value == null) {
            return defaultValue;
        }

        return convert(value, elementTypeRef);
    }

    public static <K> @Nullable Object[] getArray(Map<K, ?> map, K key) {
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

    public static <K> Object[] getArray(Map<K, ?> map, K key, Object[] defaultValue) {
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
    public static <K, T> @Nullable T[] getArray(Map<K, ?> map, K key, Class<T> elementType) {
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
    public static <K, T> T[] getArray(Map<K, ?> map, K key, Class<T> elementType, T[] defaultValue) {
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

    public static <K> Boolean getBoolean(Map<K, ?> map, K key) {
        return get(map, key, Boolean.class);
    }

    public static <K> boolean getBoolean(Map<K, ?> map, K key, boolean defaultValue) {
        Boolean value = getBoolean(map, key);

        return value != null ? value : defaultValue;
    }

    public static <K> Date getDate(Map<K, ?> map, K key) {
        return get(map, key, Date.class);
    }

    public static <K> Date getDate(Map<K, ?> map, K key, Date defaultValue) {
        Date date = getDate(map, key);

        if (date == null) {
            date = defaultValue;
        }

        return date;
    }

    public static <K> Double getDouble(Map<K, ?> map, K key) {
        return get(map, key, Double.class);
    }

    public static <K> Double getDouble(Map<K, ?> map, K key, double defaultValue) {
        return get(map, key, Double.class, defaultValue);
    }

    public static <K> Duration getDuration(Map<K, ?> map, K key) {
        return get(map, key, Duration.class);
    }

    public static <K> Duration getDuration(Map<K, ?> map, K key, Duration defaultDuration) {
        Duration value = getDuration(map, key);

        return value != null ? value : defaultDuration;
    }

    public static <K> Float getFloat(Map<K, ?> map, K key) {
        return get(map, key, Float.class);
    }

    public static <K> float getFloat(Map<K, ?> map, K key, float defaultValue) {
        return get(map, key, Float.class, defaultValue);
    }

    public static <K, T> T getFromPath(Map<K, ?> map, String path, Class<T> elementType) {
        Object value = readFromPath(map, path);

        return convert(value, elementType);
    }

    public static <K, T> T getFromPath(Map<K, ?> map, String path, Class<T> elementType, T defaultValue) {
        Object value = readFromPath(map, path);

        if (value == null) {
            return defaultValue;
        }

        return convert(value, elementType);
    }

    public static <K, T> T getFromPath(Map<K, ?> map, String path, TypeReference<T> typeReference) {
        Object value = readFromPath(map, path);

        return convert(value, typeReference);
    }

    public static <K, T> T getFromPath(Map<K, ?> map, String path, TypeReference<T> typeReference, T defaultValue) {
        Object value = readFromPath(map, path);

        if (value == null) {
            return defaultValue;
        }

        return convert(value, typeReference);
    }

    public static <K> Integer getInteger(Map<K, ?> map, K key) {
        return get(map, key, Integer.class);
    }

    public static <K> int getInteger(Map<K, ?> map, K key, int defaultValue) {
        return get(map, key, Integer.class, defaultValue);
    }

    public static <K> List<?> getList(Map<K, ?> map, K key) {
        return get(map, key, List.class);
    }

    public static <K> List<?> getList(Map<K, ?> map, K key, List<?> defaultValue) {
        List<?> list = get(map, key, List.class);

        return list == null ? defaultValue : list;
    }

    public static <K, T> @Nullable List<T> getList(Map<K, ?> map, K key, Class<T> elementType) {
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

    public static <K, T> List<T> getList(Map<K, ?> map, K key, Class<T> elementType, List<T> defaultValue) {
        List<T> list = getList(map, key, elementType);

        return list != null ? list : defaultValue;
    }

    public static <K> List<?> getList(Map<K, ?> map, K key, Class<?>[] elementTypes) {
        List<?> list = get(map, key, List.class);

        if (list != null) {
            list = list.stream()
                .map(entryValue -> convert(entryValue, Arrays.asList(elementTypes)))
                .toList();
        }

        return list;
    }

    public static <K> List<?> getList(
        Map<K, ?> map, K key, List<Class<?>> elementTypes, List<?> defaultValue) {

        List<?> list = get(map, key, List.class);

        if (list == null) {
            list = defaultValue;
        } else {
            list = list.stream()
                .map(entryValue -> convert(entryValue, elementTypes))
                .toList();
        }

        return list;
    }

    public static <K, T> @Nullable List<T> getList(Map<K, ?> map, K key, TypeReference<T> elementTypeRef) {
        List<?> list = getList(map, key);

        if (list == null) {
            return null;
        }

        return CollectionUtils.map(list, item -> convert(item, elementTypeRef));
    }

    public static <K, T> List<T> getList(Map<K, ?> map, K key, TypeReference<T> elementTypeRef, List<T> defaultValue) {
        List<?> list = getList(map, key);

        if (list == null) {
            return defaultValue;
        }

        return CollectionUtils.map(list, item -> convert(item, elementTypeRef));
    }

    public static <K> LocalDate getLocalDate(Map<K, ?> map, K key) {
        return get(map, key, LocalDate.class);
    }

    public static <K> LocalDate getLocalDate(Map<K, ?> map, K key, LocalDate defaultValue) {
        LocalDate localDate = getLocalDate(map, key);

        if (localDate == null) {
            localDate = defaultValue;
        }

        return localDate;
    }

    public static <K> LocalDateTime getLocalDateTime(Map<K, ?> map, K key) {
        return get(map, key, LocalDateTime.class);
    }

    public static <K> LocalDateTime getLocalDateTime(Map<K, ?> map, K key, LocalDateTime defaultValue) {
        LocalDateTime localDateTime = getLocalDateTime(map, key);

        if (localDateTime == null) {
            localDateTime = defaultValue;
        }

        return localDateTime;
    }

    public static <K> LocalTime getLocalTime(Map<K, ?> map, K key) {
        return get(map, key, LocalTime.class);
    }

    public static <K> LocalTime getLocalTime(Map<K, ?> map, K key, LocalTime defaultValue) {
        LocalTime localTime = getLocalTime(map, key);

        if (localTime == null) {
            localTime = defaultValue;
        }

        return localTime;
    }

    public static <K> Long getLong(Map<K, ?> map, K key) {
        return get(map, key, Long.class);
    }

    public static <K> long getLong(Map<K, ?> map, K key, long defaultValue) {
        return get(map, key, Long.class, defaultValue);
    }

    public static <K1, K2> @Nullable Map<K2, ?> getMap(Map<K1, ?> map, K1 key) {
        @SuppressWarnings("unchecked")
        Map<K2, ?> value = get(map, key, Map.class);

        if (value == null) {
            return null;
        }

        return Collections.unmodifiableMap(toMap(value, entry -> (K2) entry.getKey(), Map.Entry::getValue));
    }

    public static <K1, K2, V> @Nullable Map<K2, V> getMap(Map<K1, ?> map, K1 key, TypeReference<V> elementTypeRef) {
        Map<K2, ?> resultMap = getMap(map, key);

        if (resultMap == null) {
            return null;
        }

        return toMap(resultMap, Map.Entry::getKey, entry -> convert(entry.getValue(), elementTypeRef));
    }

    public static <K1, K2> Map<K2, ?> getMap(Map<K1, ?> map, K1 key, Map<K2, ?> defaultValue) {
        @SuppressWarnings("unchecked")
        Map<K2, ?> value = get(map, key, Map.class);

        if (value == null) {
            return Collections.unmodifiableMap(defaultValue);
        }

        return Collections.unmodifiableMap(toMap(value, entry -> (K2) entry.getKey(), Map.Entry::getValue));
    }

    public static <K1, K2, V> @Nullable Map<K2, V> getMap(Map<K1, ?> map, K1 key, Class<V> valueType) {
        @SuppressWarnings("unchecked")
        Map<K2, ?> value = get(map, key, Map.class);

        if (value == null) {
            return null;
        }

        return Collections.unmodifiableMap(
            toMap(value, entry -> (K2) entry.getKey(), entry -> convert(entry.getValue(), valueType)));
    }

    public static <K1, K2, V> Map<K2, V> getMap(
        Map<K1, ?> map, K1 key, Class<V> valueType, Map<K2, V> defaultValue) {

        Map<K2, V> value = getMap(map, key, valueType);

        return value == null ? Collections.unmodifiableMap(defaultValue) : value;
    }

    public static <K1, K2, V> Map<K2, V> getMap(
        Map<K1, ?> map, K1 key, TypeReference<V> elementTypeRef, Map<K2, V> defaultValue) {

        if (!map.containsKey(key)) {
            return defaultValue;
        }

        return getMap(map, key, elementTypeRef);
    }

    public static <K1, K2> Map<K2, ?> getMap(Map<K1, ?> map, K1 key, List<Class<?>> valueTypes) {
        Map<K2, ?> mapValue = getMap(map, key);

        if (mapValue != null) {
            mapValue = toMap(mapValue, Map.Entry::getKey, entry -> convert(entry.getValue(), valueTypes));
        }

        return mapValue;
    }

    public static <K1, K2> Map<K2, ?> getMap(
        Map<K1, ?> map, K1 key, List<Class<?>> valueTypes, Map<K2, ?> defaultValue) {

        Map<K2, ?> mapValue = getMap(map, key);

        if (mapValue == null) {
            mapValue = Collections.unmodifiableMap(defaultValue);
        } else {
            mapValue = toMap(mapValue, Map.Entry::getKey, entry -> convert(entry.getValue(), valueTypes));
        }

        return mapValue;
    }

    public static <K1, K2> Map<K2, ?> getMapFromPath(Map<K1, ?> map, String path, List<Class<?>> valueTypes) {
        Map<K2, ?> mapValue = getFromPath(map, path, new TypeReference<>() {});

        if (mapValue != null) {
            mapValue = toMap(mapValue, Map.Entry::getKey, entry -> convert(entry.getValue(), valueTypes));
        }

        return mapValue;
    }

    public static <K1, K2> Map<K2, ?> getMapFromPath(
        Map<K1, ?> map, String path, List<Class<?>> valueTypes, Map<K2, ?> defaultValue) {

        Map<K2, ?> mapValue = getFromPath(map, path, new TypeReference<>() {});

        if (mapValue == null) {
            mapValue = Collections.unmodifiableMap(defaultValue);
        } else {
            mapValue = toMap(mapValue, Map.Entry::getKey, entry -> convert(entry.getValue(), valueTypes));
        }

        return mapValue;
    }

    public static <K> Object getRequired(Map<K, ?> map, K key) {
        Object value = get(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K, T> T getRequired(Map<K, ?> map, K key, Class<T> returnType) {
        T value = get(map, key, returnType);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K, T> T getRequired(Map<K, ?> map, K key, TypeReference<T> returnTypeRef) {
        T value = get(map, key, returnTypeRef);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> Object[] getRequiredArray(Map<K, ?> map, K key) {
        Object[] value = getArray(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K, T> T[] getRequiredArray(Map<K, ?> map, K key, Class<T> elementType) {
        T[] value = getArray(map, key, elementType);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> Boolean getRequiredBoolean(Map<K, ?> map, K key) {
        Boolean value = getBoolean(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> Date getRequiredDate(Map<K, ?> map, K key) {
        Date value = getDate(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> Double getRequiredDouble(Map<K, ?> map, K key) {
        Double value = getDouble(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> Float getRequiredFloat(Map<K, ?> map, K key) {
        Float value = getFloat(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <T> T getRequiredFromPath(Map<String, ?> map, String path, Class<T> elementType) {
        Object value = readFromPath(map, path);

        Validate.notNull(value, "Unknown value for : " + path);

        return convert(value, elementType);
    }

    public static <T> T getRequiredFromPath(Map<String, ?> map, String path, TypeReference<T> typeReference) {
        Object value = readFromPath(map, path);

        return convert(value, typeReference);
    }

    public static <K> Integer getRequiredInteger(Map<K, ?> map, K key) {
        Integer value = getInteger(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> List<?> getRequiredList(Map<K, ?> map, K key) {
        List<?> value = getList(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K, T> List<T> getRequiredList(Map<K, ?> map, K key, Class<T> elementType) {
        List<T> value = getList(map, key, elementType);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K, T> List<T> getRequiredList(Map<K, ?> map, K key, TypeReference<T> elementTypeRef) {
        List<T> value = getList(map, key, elementTypeRef);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> LocalDate getRequiredLocalDate(Map<K, ?> map, K key) {
        LocalDate value = getLocalDate(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> LocalTime getRequiredLocalTime(Map<K, ?> map, K key) {
        LocalTime value = getLocalTime(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> LocalDateTime getRequiredLocalDateTime(Map<K, ?> map, K key) {
        LocalDateTime value = getLocalDateTime(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K1, K2, V> Map<K2, V> getRequiredMap(Map<K1, ?> map, K1 key, Class<V> valueType) {
        Map<K2, V> value = getMap(map, key, valueType);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K1, K2, V> Map<K2, V> getRequiredMap(
        Map<K1, ?> map, K1 key, TypeReference<V> elementTypeRef) {

        Map<K2, V> value = getMap(map, key, elementTypeRef);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K1, K2> Map<K2, ?> getRequiredMap(Map<K1, ?> map, K1 key) {
        Map<K2, ?> value = getMap(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static long getRequiredLong(Map<String, ?> map, String key) {
        Long value = getLong(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> String getRequiredString(Map<K, ?> map, K key) {
        String value = getString(map, key);

        Validate.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <K> String getString(Map<K, ?> map, K key) {
        return get(map, key, String.class);
    }

    public static <K> String getString(Map<K, ?> map, K key, String defaultValue) {
        String value = getString(map, key);

        return value != null ? value : defaultValue;
    }

    @SuppressFBWarnings("EI")
    public static void setObjectMapper(ObjectMapper objectMapper) {
        MapUtils.objectMapper = objectMapper;
    }

    public static int size(Map<?, ?> map) {
        Validate.notNull(map, "'map' must not be null");

        return map.size();
    }

    public static <K, V> Stream<Map.Entry<K, V>> stream(Map<K, V> map) {
        Validate.notNull(map, "'map' must not be null");

        Set<Map.Entry<K, V>> entry = map.entrySet();

        return entry.stream();
    }

    public static Map<String, List<String>> toMap(Map<String, String[]> map) {
        return map.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> Arrays.asList(entry.getValue())));
    }

    public static <K, V, T> Map<K, V> toMap(
        List<T> list, Function<T, ? extends K> keyMapper, Function<T, ? extends V> valueMapper) {

        return list.stream()
            .collect(Collectors.toMap(keyMapper, valueMapper));
    }

    public static <K, V, K1, V1> Map<K1, V1> toMap(
        Map<K, V> map, Function<Map.Entry<K, V>, ? extends K1> keyMapper,
        Function<Map.Entry<K, V>, ? extends V1> valueMapper) {

        Map<K1, V1> newMap = new HashMap<>();

        for (Map.Entry<K, V> entry : map.entrySet()) {
            newMap.put(keyMapper.apply(entry), valueMapper.apply(entry));
        }

        return newMap;
    }

    public static String toString(Map<String, ?> map) {
        return map.keySet()
            .stream()
            .map(key -> {
                Object value = map.get(key);
                Class<?> valueClass = value.getClass();

                if (map.get(key) instanceof Collection<?> collection) {
                    value = CollectionUtils.toString(collection);
                } else if (valueClass.isArray()) {
                    value = CollectionUtils.toString(Arrays.asList(getArray(value)));
                }

                return key + "=" + value;
            })
            .collect(Collectors.joining(", ", "{", "}"));
    }

    private static <T> T convert(Object value, Class<T> elementType) {
        // Special handling for Instant: attempts the ISO_INSTANT format first, then falls back to
        // ISO_LOCAL_DATE_TIME assuming UTC timezone if no timezone information is present
        if (elementType == Instant.class) {
            if (value == null) {
                return null;
            }

            if (value instanceof Instant) {
                return elementType.cast(value);
            }

            if (value instanceof String string) {
                try {
                    // Try strict ISO_INSTANT first (expects 'Z' or offset)
                    return elementType.cast(Instant.parse(string));
                } catch (DateTimeParseException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "Failed to parse Instant using ISO_INSTANT from value '{}'; attempting ISO_LOCAL_DATE_TIME assuming UTC",
                            string, e);
                    }
                    try {
                        LocalDateTime localDateTime = LocalDateTime.parse(
                            string, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                        return elementType.cast(localDateTime.toInstant(ZoneOffset.UTC));
                    } catch (DateTimeParseException e2) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                "Failed to parse Instant using ISO_LOCAL_DATE_TIME from value '{}'; falling back to ObjectMapper.convertValue",
                                string, e2);
                        }
                    }
                }
            }
        }

        return objectMapper.convertValue(value, elementType);
    }

    private static <T> T convert(Object value, TypeReference<T> elementTypeRef) {
        return objectMapper.convertValue(value, elementTypeRef);
    }

    private static Object convert(Object value, List<Class<?>> elementTypes) {
        for (Class<?> elementType : elementTypes) {
            try {
                value = objectMapper
                    .rebuild()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build()
                    .convertValue(value, elementType);
            } catch (Exception e) {
                if (logger.isTraceEnabled()) {
                    logger.trace(e.getMessage(), e);
                }
            }

            if (value.getClass() == elementType) {
                break;
            }
        }

        return value;
    }

    private static Object[] getArray(Object value) {
        Object[] outputArray;

        if (value instanceof Object[]) {
            outputArray = (Object[]) value;
        } else {
            int length = Array.getLength(value);

            Object[] array = new Object[length];

            for (int i = 0; i < length; ++i) {
                array[i] = Array.get(value, i);
            }

            outputArray = array;
        }

        return outputArray;
    }

    private static Object readFromPath(Object map, String path) {
        Object value = null;

        try {
            value = JsonPath.read(map, path);
        } catch (PathNotFoundException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage());
            }
        }
        return value;
    }
}
