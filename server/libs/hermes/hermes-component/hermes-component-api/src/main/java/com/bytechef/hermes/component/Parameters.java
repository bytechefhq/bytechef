
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

package com.bytechef.hermes.component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface Parameters {

    /**
     * Determined if the given task contains the given key.
     *
     * @param key The key to check for existance.
     * @return <code>true</code> if the key exists.<code>false</code> otherwise.
     */
    boolean containsKey(String key);

    /**
     * Return the object associated with the given key.
     *
     * @param key The key associated with the desired value.
     * @return The object or <code>null</code> if no object is associated with the given key.
     */
    Object get(String key);

    /**
     * Return the value associated with the given key -- converting to the desired return type.
     *
     * @param key        The key associated with the desired value.
     * @param returnType The type to return the value as -- converting as neccessary.
     * @return The value associated with the given key.
     */
    <T> T get(String key, Class<T> returnType);

    /**
     * Return the value associated with the given key -- converting to the desired return type.
     *
     * @param key          The key associated with the desired value.
     * @param returnType   The type to return the value as -- converting as neccessary.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The value associated with the given key or the default value.
     */
    <T> T get(String key, Class<T> returnType, T defaultValue);

    /**
     * Return the array value associated with the given key.
     *
     * @param key The key associated with the desired value.
     * @return The array value.
     */
    <T> T[] getArray(String key, Class<T> elementType);

    /**
     * Return the {@link Boolean} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The {@link Integer} value associated with the given key -- converting as needed.
     */
    Boolean getBoolean(String key);

    /**
     * Return the {@link Boolean} value associated with the given key -- converting as necessary.
     *
     * @param key          The key associated with the desired value.
     * @param defaultValue The default value if no value is associated with the key
     * @return The {@link Integer} value associated with the given key -- converting as needed.
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Return the {@link Date} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The {@link Date} value associated with the given key -- converting as needed.
     */
    Date getDate(String key);

    /**
     * Return the {@link Date} value associated with the given key -- converting as necessary.
     *
     * @param key          The key associated with the desired value.
     * @param defaultValue The default value if no value is associated with the key
     * @return The {@link Date} value associated with the given key -- converting as needed.
     */
    Date getDate(String key, Date defaultValue);

    /**
     * Return the {@link Double} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The {@link Double} value associated with the given key -- converting as needed.
     */
    Double getDouble(String key);

    /**
     * Return the {@link Double} value associated with the given key -- converting as necessary.
     *
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The {@link Double} value associated with the given key -- converting as needed.
     */
    double getDouble(String key, double defaultValue);

    /**
     * Return the {@link Duration} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The {@link Duration} value associated with the given key -- converting as needed.
     */
    Duration getDuration(String key);

    /**
     * Return the {@link Duration} value associated with the given key -- converting as necessary.
     *
     * @param key             The key associated with the desired value.
     * @param defaultDuration The default duration
     * @return The {@link Duration} value associated with the given key -- converting as needed.
     */
    Duration getDuration(String key, Duration defaultDuration);

    /**
     * Return the {@link Float} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The {@link Float} value associated with the given key -- converting as needed.
     */
    Float getFloat(String key);

    /**
     * Return the {@link Float} value associated with the given key -- converting as necessary.
     *
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The {@link Float} value associated with the given key -- converting as needed.
     */
    float getFloat(String key, float defaultValue);

    /**
     * Return the {@link Integer} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The {@link Integer} value associated with the given key -- converting as needed.
     */
    Integer getInteger(String key);

    /**
     * Return the {@link Integer} value associated with the given key -- converting as necessary.
     *
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The {@link Integer} value associated with the given key -- converting as needed.
     */
    int getInteger(String key, int defaultValue);

    /**
     * Return the {@link List} of items associated with the given key.
     *
     * @param key         The key associated with the desired value.
     * @param elementType The type of the list elements.
     * @return The list of items
     */
    <T> List<T> getList(String key, Class<T> elementType);

    /**
     * Return the {@link List} of items associated with the given key.
     *
     * @param key          The key associated with the desired value.
     * @param elementType  The type of the list elements.
     * @param defaultValue The list value to return none was not found for the given key or if the value is null.
     * @return The list of items
     */
    <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue);

    List<Object> getList(String key, List<Class<?>> elementTypes, List<Object> defaultValue);

    LocalDate getLocalDate(String key);

    LocalDate getLocalDate(String key, LocalDate defaultValue);

    LocalDateTime getLocalDateTime(String key);

    LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue);

    /**
     * Return the {@link Long} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The {@link Long} value associated with the given key -- converting as needed.
     */
    Long getLong(String key);

    /**
     * Return the {@link Long} value associated with the given key -- converting as necessary.
     *
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The {@link Long} value associated with the given key -- converting as needed.
     */
    long getLong(String key, long defaultValue);

    /**
     * Return the {@link Map} value associated with the given key.
     *
     * @param key The key associated with the desired value.
     * @return The {@link Map} value associated with the given key.
     */
    <V> Map<String, V> getMap(String key);

    /**
     * Return the {@link Map} value associated with the given key.
     *
     * @param key          The key associated with the desired value.
     * @param defaultValue The default map to return if none is associated with the key.
     * @return The {@link Map} value associated with the given key.
     */
    <V> Map<String, V> getMap(String key, Map<String, V> defaultValue);

    Map<String, Object> getMap(String key, List<Class<?>> valueTypes, Map<String, Object> defaultValue);

    /**
     * Return the value associated with the given key or throws an exception if no value is associated with the given
     * key.
     *
     * @param key The key associated with the desired value.
     * @return The value associated with the given key.
     * @throws IllegalArgumentException if no value is associated with the given key.
     */
    <T> T getRequired(String key);

    /**
     * Return the value -- converting to the desired return type -- associated with the given key or throws an exception
     * if no value is associated with the given key.
     *
     * @param key        The key associated with the desired value.
     * @param returnType The type to return the value as -- converting as necessary.
     * @return The value associated with the given key.
     */
    <T> T getRequired(String key, Class<T> returnType);

    Boolean getRequiredBoolean(String key);

    Date getRequiredDate(String key);

    Double getRequiredDouble(String key);

    Float getRequiredFloat(String key);

    Integer getRequiredInteger(String key);

    LocalDate getRequiredLocalDate(String key);

    LocalDateTime getRequiredLocalDateTime(String key);

    /**
     * Return the {@link String} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The string value associated with the given key -- converting to {@link String} as needed.
     * @throws IllegalArgumentException if no value is associated with the given key.
     */
    String getRequiredString(String key);

    /**
     * Return the {@link String} value associated with the given key -- converting as necessary.
     *
     * @param key The key associated with the desired value.
     * @return The string value associated with the given key -- converting to {@link String} as needed.
     */
    String getString(String key);

    /**
     * Return the {@link String} value associated with the given key -- converting as necessary.
     *
     * @param key          The key associated with the desired value.
     * @param defaultValue The value to return if a value was not found for the given key or if the value is null.
     * @return The string value associated with the given key -- converting to {@link String} as needed.
     */
    String getString(String key, String defaultValue);
}
