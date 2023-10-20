
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

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public final class MapValueUtils {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final DefaultConversionService conversionService = new DefaultConversionService();

    private MapValueUtils() {
    }

    public static Object get(Map<String, ?> map, String key) {
        Assert.notNull(map, "'map' must not be null");

        return map.get(key);
    }

    public static <T> T get(Map<String, Object> map, String key, Class<T> returnType) {
        Object value = get(map, key);

        if (value == null) {
            return null;
        }

        return conversionService.convert(value, returnType);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String, Object> map, String key, ParameterizedTypeReference<T> returnType) {
        return get(map, key, (Class<T>) ((ParameterizedType) returnType.getType()).getRawType());
    }

    public static <T> T get(Map<String, Object> map, String key, Class<T> returnType, T defaultValue) {
        T value = get(map, key, returnType);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(
        Map<String, Object> map, String key, ParameterizedTypeReference<T> returnType, T defaultValue) {

        return get(map, key, (Class<T>) ((ParameterizedType) returnType.getType()).getRawType(), defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] getArray(Map<String, Object> map, String key, Class<T> elementType) {
        Object value = get(map, key);

        if (value.getClass()
            .isArray()
            && value.getClass()
                .getComponentType()
                .equals(elementType)) {
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

    public static Date getDate(Map<String, Object> map, String key, Date defaultValue) {
        Date date = getDate(map, key);

        if (date == null) {
            date = defaultValue;
        }

        return date;
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

    public static Duration getDuration(Map<String, Object> map, String key, Duration defaultDuration) {
        Duration value = getDuration(map, key);

        return value != null ? value : defaultDuration;
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

    public static <T> List<T> getList(Map<String, Object> map, String key, Class<T> elementType, List<T> defaultValue) {
        List<T> list = getList(map, key, elementType);

        return list != null ? list : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(Map<String, Object> map, String key, ParameterizedTypeReference<T> elementType) {

        return getList(map, key, (Class<T>) ResolvableType.forType(elementType)
            .getRawClass());
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(
        Map<String, Object> map, String key, ParameterizedTypeReference<T> elementType, List<T> defaultValue) {

        List<T> list = getList(map, key, (Class<T>) ResolvableType.forType(elementType)
            .getRawClass(), defaultValue);

        return list != null ? list : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(
        Map<String, Object> map, String key, List<Class<?>> elementTypes, List<Object> defaultValue) {

        List<Object> list = get(map, key, List.class);

        if (list == null) {
            list = defaultValue;
        } else {
            list = list.stream()
                .map(value -> {
                    for (Class<?> elementType : elementTypes) {
                        if (conversionService.canConvert(value.getClass(), elementType)) {
                            value = conversionService.convert(value, elementType);
                        }
                    }

                    return value;
                })
                .toList();
        }

        return list;
    }

    public static LocalDate getLocalDate(Map<String, Object> map, String key) {
        Object value = get(map, key);

        if (value instanceof String) {
            return LocalDate.parse((String) value, DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        }

        return (LocalDate) value;
    }

    public static LocalDate getLocalDate(Map<String, Object> map, String key, LocalDate defaultValue) {
        LocalDate localDate = getLocalDate(map, key);

        if (localDate == null) {
            localDate = defaultValue;
        }

        return localDate;
    }

    public static LocalDateTime getLocalDateTime(Map<String, Object> map, String key) {
        Object value = get(map, key);

        if (value instanceof String) {
            return LocalDateTime.parse((String) value, DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        }

        return (LocalDateTime) value;
    }

    public static LocalDateTime getLocalDateTime(Map<String, Object> map, String key, LocalDateTime defaultValue) {
        LocalDateTime localDateTime = getLocalDateTime(map, key);

        if (localDateTime == null) {
            localDateTime = defaultValue;
        }

        return localDateTime;
    }

    public static Long getLong(Map<String, Object> map, String key) {
        return get(map, key, Long.class);
    }

    public static long getLong(Map<String, Object> map, String key, long defaultValue) {
        return get(map, key, Long.class, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static <V> Map<String, V> getMap(Map<String, Object> map, String key) {
        Map<String, V> value = (Map<String, V>) get(map, key, Map.class);

        if (value == null) {
            return null;
        }

        return Collections.unmodifiableMap(value);
    }

    public static <V> Map<String, V> getMap(Map<String, Object> map, String key, Map<String, V> defaultValue) {
        Map<String, V> value = getMap(map, key);

        return value != null ? value : defaultValue;
    }

    public static Map<String, Object> getMap(
        Map<String, Object> map, String key, List<Class<?>> valueTypes, Map<String, Object> defaultValue) {
        Map<String, Object> mapValue = getMap(map, key);

        if (mapValue == null) {
            mapValue = defaultValue;
        } else {
            mapValue = mapValue.entrySet()
                .stream()
                .map(entry -> {
                    for (Class<?> valueType : valueTypes) {
                        Object value = entry.getValue();

                        if (value != null && conversionService.canConvert(value.getClass(), valueType)) {
                            entry = Map.entry(entry.getKey(), conversionService.convert(entry.getValue(), valueType));
                        }
                    }

                    return entry;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return mapValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getRequired(Map<String, ?> map, String key) {
        T value = (T) get(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <T> T getRequired(Map<String, Object> map, String key, Class<T> returnType) {
        T value = get(map, key, returnType);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Boolean getRequiredBoolean(Map<String, Object> map, String key) {
        Boolean value = getBoolean(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Date getRequiredDate(Map<String, Object> map, String key) {
        Date value = getDate(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Double getRequiredDouble(Map<String, Object> map, String key) {
        Double value = getDouble(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Float getRequiredFloat(Map<String, Object> map, String key) {
        Float value = getFloat(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static Integer getRequiredInteger(Map<String, Object> map, String key) {
        Integer value = getInteger(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <T> List<T> getRequiredList(Map<String, Object> map, String key, Class<T> elementType) {
        List<T> value = getList(map, key, elementType);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <T> List<T> getRequiredList(
        Map<String, Object> map, String key, ParameterizedTypeReference<T> elementType) {
        List<T> value = getList(map, key, elementType);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static LocalDate getRequiredLocalDate(Map<String, Object> map, String key) {
        LocalDate value = getLocalDate(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static LocalDateTime getRequiredLocalDateTime(Map<String, Object> map, String key) {
        LocalDateTime value = getLocalDateTime(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static <V> Map<String, V> getRequiredMap(Map<String, Object> map, String key) {
        Map<String, V> value = getMap(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static String getRequiredString(Map<String, Object> map, String key) {
        String value = getString(map, key);

        Assert.notNull(value, "Unknown value for : " + key);

        return value;
    }

    public static String getString(Map<String, Object> map, String key) {
        return get(map, key, String.class);
    }

    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);

        return value != null ? value : defaultValue;
    }

    public static void addConverter(Converter<?, ?> converter) {
        conversionService.addConverter(converter);
    }
}
