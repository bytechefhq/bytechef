
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

package com.bytechef.hermes.component.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class MapUtils {

    static MapReader mapReader;

    static {
        ServiceLoader<MapReader> loader = ServiceLoader.load(MapReader.class);

        mapReader = loader.findFirst()
            .orElse(null);

        if (mapReader == null) {
            System.err.println("MapReader instance is not available");
        }
    }

    private MapUtils() {
    }

    /**
     * Determined if the given task contains the given key.
     *
     * @param map
     * @param key The key to check for existence.
     * @return <code>true</code> if the key exists.<code>false</code> otherwise.
     */
    public static boolean containsKey(Map<String, ?> map, String key) {
        return mapReader.containsKey(map, key);
    }

    /**
     * Return the object associated with the given key.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The object or <code>null</code> if no object is associated with the given key.
     */
    public static Object get(Map<String, ?> map, String key) {
        return mapReader.get(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @param returnType
     * @return
     * @param <T>
     */
    public static <T> T get(Map<String, ?> map, String key, Class<T> returnType) {
        return mapReader.get(map, key, returnType);
    }

    /**
     *
     * @param map
     * @param key
     * @param returnType
     * @param defaultValue
     * @return
     * @param <T>
     */
    public static <T> T get(Map<String, ?> map, String key, Class<T> returnType, T defaultValue) {
        return mapReader.get(map, key, returnType, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static Object[] getArray(Map<String, ?> map, String key) {
        return mapReader.getArray(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @param defaultValue
     * @return
     */
    public static Object[] getArray(Map<String, ?> map, String key, Object[] defaultValue) {
        return mapReader.getArray(map, key, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @param elementType
     * @return
     * @param <T>
     */
    public static <T> T[] getArray(Map<String, ?> map, String key, Class<T> elementType) {
        return mapReader.getArray(map, key, elementType);
    }

    /**
     *
     * @param map
     * @param key
     * @param elementType
     * @return
     * @param <T>
     */
    public static <T> T[] getArray(Map<String, ?> map, String key, Class<T> elementType, T[] defaultValue) {
        return mapReader.getArray(map, key, elementType, defaultValue);
    }

    /**
     * Return the {@link Boolean} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The {@link Integer} value associated with the given key -- converting as needed.
     */
    public static Boolean getBoolean(Map<String, ?> map, String key) {
        return mapReader.getBoolean(map, key);
    }

    /**
     * Return the {@link Boolean} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The default value if no value is associated with the key
     * @return The {@link Integer} value associated with the given key -- converting as needed.
     */
    public static boolean getBoolean(Map<String, ?> map, String key, boolean defaultValue) {
        return mapReader.getBoolean(map, key, defaultValue);
    }

    /**
     * Return the {@link Date} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The {@link Date} value associated with the given key -- converting as needed.
     */
    public static Date getDate(Map<String, ?> map, String key) {
        return mapReader.getDate(map, key);
    }

    /**
     * Return the {@link Date} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The default value if no value is associated with the key
     * @return The {@link Date} value associated with the given key -- converting as needed.
     */
    public static Date getDate(Map<String, ?> map, String key, Date defaultValue) {
        return mapReader.getDate(map, key, defaultValue);
    }

    /**
     * Return the {@link Double} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The {@link Double} value associated with the given key -- converting as needed.
     */
    public static Double getDouble(Map<String, ?> map, String key) {
        return mapReader.getDouble(map, key);
    }

    /**
     * Return the {@link Double} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The {@link Double} value associated with the given key -- converting as needed.
     */
    public static double getDouble(Map<String, ?> map, String key, double defaultValue) {
        return mapReader.getDouble(map, key, defaultValue);
    }

    /**
     * Return the {@link Duration} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The {@link Duration} value associated with the given key -- converting as needed.
     */
    public static Duration getDuration(Map<String, ?> map, String key) {
        return mapReader.getDuration(map, key);
    }

    /**
     * Return the {@link Duration} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key             The key associated with the desired value.
     * @param defaultDuration The default duration
     * @return The {@link Duration} value associated with the given key -- converting as needed.
     */
    public static Duration getDuration(Map<String, ?> map, String key, Duration defaultDuration) {
        return mapReader.getDuration(map, key, defaultDuration);
    }

    /**
     * Return the {@link Float} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The {@link Float} value associated with the given key -- converting as needed.
     */
    public static Float getFloat(Map<String, ?> map, String key) {
        return mapReader.getFloat(map, key);
    }

    /**
     * Return the {@link Float} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The {@link Float} value associated with the given key -- converting as needed.
     */
    public static float getFloat(Map<String, ?> map, String key, float defaultValue) {
        return mapReader.getFloat(map, key, defaultValue);
    }

    /**
     * Return the {@link Integer} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The {@link Integer} value associated with the given key -- converting as needed.
     */
    public static Integer getInteger(Map<String, ?> map, String key) {
        return mapReader.getInteger(map, key);
    }

    /**
     * Return the {@link Integer} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The {@link Integer} value associated with the given key -- converting as needed.
     */
    public static int getInteger(Map<String, ?> map, String key, int defaultValue) {
        return mapReader.getInteger(map, key, defaultValue);
    }

    /**
     * Return the {@link List} of items associated with the given key.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The list of items
     */
    public static List<?> getList(Map<String, ?> map, String key) {
        return mapReader.getList(map, key);
    }

    /**
     * Return the {@link List} of items associated with the given key.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The list value to return none was not found for the given key or if the value is null.
     * @return The list of items
     */
    public static List<?> getList(Map<String, ?> map, String key, List<?> defaultValue) {
        return mapReader.getList(map, key, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @param elementType
     * @return
     * @param <T>
     */
    public static <T> List<T> getList(Map<String, ?> map, String key, Class<T> elementType) {
        return mapReader.getList(map, key, elementType);
    }

    /**
     *
     * @param map
     * @param key
     * @param elementType
     * @param defaultValue
     * @return
     * @param <T>
     */
    public static <T> List<T> getList(Map<String, ?> map, String key, Class<T> elementType, List<T> defaultValue) {
        return mapReader.getList(map, key, elementType, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @param elementTypes
     * @return
     */
    public static List<?> getList(Map<String, ?> map, String key, Class<?>[] elementTypes) {
        return mapReader.getList(map, key, elementTypes);
    }

    /**
     *
     * @param map
     * @param key
     * @param elementTypes
     * @param defaultValue
     * @return
     */
    public static List<?> getList(Map<String, ?> map, String key, List<Class<?>> elementTypes, List<?> defaultValue) {
        return mapReader.getList(map, key, elementTypes, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static LocalDate getLocalDate(Map<String, ?> map, String key) {
        return mapReader.getLocalDate(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @param defaultValue
     * @return
     */
    public static LocalDate getLocalDate(Map<String, ?> map, String key, LocalDate defaultValue) {
        return mapReader.getLocalDate(map, key, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static LocalDateTime getLocalDateTime(Map<String, ?> map, String key) {
        return mapReader.getLocalDateTime(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @param defaultValue
     * @return
     */
    public static LocalDateTime getLocalDateTime(Map<String, ?> map, String key, LocalDateTime defaultValue) {
        return mapReader.getLocalDateTime(map, key, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static LocalTime getLocalTime(Map<String, ?> map, String key) {
        return mapReader.getLocalTime(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @param defaultValue
     * @return
     */
    public static LocalTime getLocalTime(Map<String, ?> map, String key, LocalTime defaultValue) {
        return mapReader.getLocalTime(map, key, defaultValue);
    }

    /**
     * Return the {@link Long} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The {@link Long} value associated with the given key -- converting as needed.
     */
    public static Long getLong(Map<String, ?> map, String key) {
        return mapReader.getLong(map, key);
    }

    /**
     * Return the {@link Long} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The {@link Long} value associated with the given key -- converting as needed.
     */
    public static long getLong(Map<String, ?> map, String key, long defaultValue) {
        return mapReader.getLong(map, key, defaultValue);
    }

    /**
     * Return the {@link Map} value associated with the given key.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The {@link Map} value associated with the given key.
     */
    public static Map<String, ?> getMap(Map<String, ?> map, String key) {
        return mapReader.getMap(map, key);
    }

    /**
     * Return the {@link Map} value associated with the given key.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The default map to return if none is associated with the key.
     * @return The {@link Map} value associated with the given key.
     */
    public static Map<String, ?> getMap(Map<String, ?> map, String key, Map<String, ?> defaultValue) {
        return mapReader.getMap(map, key, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @param valueType
     * @return
     * @param <V>
     */
    public static <V> Map<String, V> getMap(Map<String, ?> map, String key, Class<V> valueType) {
        return mapReader.getMap(map, key, valueType);
    }

    /**
     *
     * @param map
     * @param key
     * @param valueType
     * @param defaultValue
     * @return
     * @param <V>
     */
    public static <V> Map<String, V> getMap(
        Map<String, ?> map, String key, Class<V> valueType, Map<String, V> defaultValue) {

        return mapReader.getMap(map, key, valueType, defaultValue);
    }

    /**
     *
     * @param map
     * @param key
     * @param valueTypes
     * @return
     */
    public static Map<String, ?> getMap(Map<String, ?> map, String key, List<Class<?>> valueTypes) {
        return mapReader.getMap(map, key, valueTypes);
    }

    /**
     *
     * @param map
     * @param key
     * @param valueTypes
     * @param defaultValue
     * @return
     */
    public static Map<String, ?> getMap(
        Map<String, ?> map, String key, List<Class<?>> valueTypes, Map<String, ?> defaultValue) {

        return mapReader.getMap(map, key, valueTypes, defaultValue);
    }

    /**
     * Return the value associated with the given key or throws an exception if no value is associated with the given
     * key.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The value associated with the given key.
     * @throws IllegalArgumentException if no value is associated with the given key.
     */
    public static Object getRequired(Map<String, ?> map, String key) {
        return mapReader.getRequired(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @param elementType
     * @return
     * @param <T>
     */
    public static <T> T getRequired(Map<String, ?> map, String key, Class<T> elementType) {
        return mapReader.getRequired(map, key, elementType);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static Object[] getRequiredArray(Map<String, ?> map, String key) {
        return mapReader.getRequiredArray(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @param elementType
     * @return
     * @param <T>
     */
    public static <T> T[] getRequiredArray(Map<String, ?> map, String key, Class<T> elementType) {
        return mapReader.getRequiredArray(map, key, elementType);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static Boolean getRequiredBoolean(Map<String, ?> map, String key) {
        return mapReader.getRequiredBoolean(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static Date getRequiredDate(Map<String, ?> map, String key) {
        return mapReader.getRequiredDate(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static Double getRequiredDouble(Map<String, ?> map, String key) {
        return mapReader.getRequiredDouble(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static Float getRequiredFloat(Map<String, ?> map, String key) {
        return mapReader.getRequiredFloat(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static Integer getRequiredInteger(Map<String, ?> map, String key) {
        return mapReader.getRequiredInteger(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static LocalDate getRequiredLocalDate(Map<String, ?> map, String key) {
        return mapReader.getRequiredLocalDate(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static LocalDateTime getRequiredLocalDateTime(Map<String, ?> map, String key) {
        return mapReader.getRequiredLocalDateTime(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static LocalTime getRequiredLocalTime(Map<String, ?> map, String key) {
        return mapReader.getRequiredLocalTime(map, key);
    }

    public static List<?> getRequiredList(Map<String, ?> map, String key) {
        return mapReader.getRequiredList(map, key);
    }

    public static <T> List<T> getRequiredList(Map<String, ?> map, String key, Class<T> elementType) {
        return mapReader.getRequiredList(map, key, elementType);
    }

    /**
     *
     * @param map
     * @param key
     * @return
     */
    public static Map<String, ?> getRequiredMap(Map<String, ?> map, String key) {
        return mapReader.getRequiredMap(map, key);
    }

    /**
     *
     * @param map
     * @param key
     * @param valueType
     * @return
     * @param <V>
     */
    public static <V> Map<String, V> getRequiredMap(Map<String, ?> map, String key, Class<V> valueType) {
        return mapReader.getRequiredMap(map, key, valueType);
    }

    /**
     * Return the {@link String} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The string value associated with the given key -- converting to {@link String} as needed.
     * @throws IllegalArgumentException if no value is associated with the given key.
     */
    public static String getRequiredString(Map<String, ?> map, String key) {
        return mapReader.getRequiredString(map, key);
    }

    /**
     * Return the {@link String} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key The key associated with the desired value.
     * @return The string value associated with the given key -- converting to {@link String} as needed.
     */
    public static String getString(Map<String, ?> map, String key) {
        return mapReader.getString(map, key);
    }

    /**
     * Return the {@link String} value associated with the given key -- converting as necessary.
     *
     * @param map
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The string value associated with the given key -- converting to {@link String} as needed.
     */
    public static String getString(Map<String, ?> map, String key, String defaultValue) {
        return mapReader.getString(map, key, defaultValue);
    }

    interface MapReader {

        boolean containsKey(Map<String, ?> map, String key);

        Object get(Map<String, ?> map, String key);

        <T> T get(Map<String, ?> map, String key, Class<T> returnType);

        <T> T get(Map<String, ?> map, String key, Class<T> returnType, T defaultValue);

        Object[] getArray(Map<String, ?> map, String key);

        Object[] getArray(Map<String, ?> map, String key, Object[] defaultValue);

        <T> T[] getArray(Map<String, ?> map, String key, Class<T> elementType);

        <T> T[] getArray(Map<String, ?> map, String key, Class<T> elementType, T[] defaultValue);

        Boolean getBoolean(Map<String, ?> map, String key);

        boolean getBoolean(Map<String, ?> map, String key, boolean defaultValue);

        Date getDate(Map<String, ?> map, String key);

        Date getDate(Map<String, ?> map, String key, Date defaultValue);

        Double getDouble(Map<String, ?> map, String key);

        double getDouble(Map<String, ?> map, String key, double defaultValue);

        Duration getDuration(Map<String, ?> map, String key);

        Duration getDuration(Map<String, ?> map, String key, Duration defaultValue);

        Float getFloat(Map<String, ?> map, String key);

        float getFloat(Map<String, ?> map, String key, float defaultValue);

        Integer getInteger(Map<String, ?> map, String key);

        int getInteger(Map<String, ?> map, String key, int defaultValue);

        List<?> getList(Map<String, ?> map, String key);

        List<?> getList(Map<String, ?> map, String key, List<?> defaultValue);

        <T> List<T> getList(Map<String, ?> map, String key, Class<T> elementType);

        <T> List<T> getList(Map<String, ?> map, String key, Class<T> elementType, List<T> defaultValue);

        List<?> getList(Map<String, ?> map, String key, Class<?>[] elementTypes);

        List<?> getList(Map<String, ?> map, String key, List<Class<?>> elementTypes, List<?> defaultValue);

        LocalDate getLocalDate(Map<String, ?> map, String key);

        LocalDate getLocalDate(Map<String, ?> map, String key, LocalDate defaultValue);

        LocalDateTime getLocalDateTime(Map<String, ?> map, String key);

        LocalDateTime getLocalDateTime(Map<String, ?> map, String key, LocalDateTime defaultValue);

        LocalTime getLocalTime(Map<String, ?> map, String key);

        LocalTime getLocalTime(Map<String, ?> map, String key, LocalTime defaultValue);

        Long getLong(Map<String, ?> map, String key);

        long getLong(Map<String, ?> map, String key, long defaultValue);

        Map<String, ?> getMap(Map<String, ?> map, String key);

        Map<String, ?> getMap(Map<String, ?> map, String key, Map<String, ?> defaultValue);

        <V> Map<String, V> getMap(Map<String, ?> map, String key, Class<V> valueType);

        <V> Map<String, V> getMap(Map<String, ?> map, String key, Class<V> valueType, Map<String, V> defaultValue);

        Map<String, ?> getMap(Map<String, ?> map, String key, List<Class<?>> valueTypes);

        Map<String, ?> getMap(Map<String, ?> map, String key, List<Class<?>> valueTypes, Map<String, ?> defaultValue);

        Object getRequired(Map<String, ?> map, String key);

        <T> T getRequired(Map<String, ?> map, String key, Class<T> returnType);

        Object[] getRequiredArray(Map<String, ?> map, String key);

        <T> T[] getRequiredArray(Map<String, ?> map, String key, Class<T> elementType);

        Boolean getRequiredBoolean(Map<String, ?> map, String key);

        Date getRequiredDate(Map<String, ?> map, String key);

        Double getRequiredDouble(Map<String, ?> map, String key);

        Float getRequiredFloat(Map<String, ?> map, String key);

        Integer getRequiredInteger(Map<String, ?> map, String key);

        List<?> getRequiredList(Map<String, ?> map, String key);

        <T> List<T> getRequiredList(Map<String, ?> map, String key, Class<T> elementType);

        LocalDate getRequiredLocalDate(Map<String, ?> map, String key);

        LocalDateTime getRequiredLocalDateTime(Map<String, ?> map, String key);

        LocalTime getRequiredLocalTime(Map<String, ?> map, String key);

        Map<String, ?> getRequiredMap(Map<String, ?> map, String key);

        <V> Map<String, V> getRequiredMap(Map<String, ?> map, String key, Class<V> valueType);

        String getRequiredString(Map<String, ?> map, String key);

        String getString(Map<String, ?> map, String key);

        String getString(Map<String, ?> map, String key, String defaultValue);
    }
}
