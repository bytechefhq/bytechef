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

package com.bytechef.component.definition;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents a read-oriented, type-aware view over a set of workflow parameters. In addition to the raw {@link Map}
 * contract, it exposes typed accessors that convert stored values into common Java types, optional default-value and
 * required (non-null) variants, and dotted-path lookups for reaching values nested inside maps and lists.
 *
 * @author Ivica Cardic
 */
public interface Parameters extends Map<String, Object> {

    /**
     * Determines whether a value exists at the given dotted path.
     *
     * @param path the dotted path to test (for example, {@code "address.city"})
     * @return {@code true} if a value is present at the path; {@code false} otherwise
     */
    boolean containsPath(String path);

    /**
     * Retrieves the value mapped to the given key, converted to the requested type.
     *
     * @param key        the parameter key
     * @param returnType the type the value should be converted to
     * @param <T>        the requested value type
     * @return the converted value, or {@code null} if the key is absent
     */
    <T> T get(String key, Class<T> returnType);

    /**
     * Retrieves the value mapped to the given key, converted to the requested type, falling back to a default when the
     * key is absent.
     *
     * @param key          the parameter key
     * @param returnType   the type the value should be converted to
     * @param defaultValue the value to return when the key is absent
     * @param <T>          the requested value type
     * @return the converted value, or {@code defaultValue} if the key is absent
     */
    <T> T get(String key, Class<T> returnType, T defaultValue);

    /**
     * Retrieves the value mapped to the given key as an array.
     *
     * @param key the parameter key
     * @return the value as an {@code Object} array, or {@code null} if the key is absent
     */
    Object[] getArray(String key);

    /**
     * Retrieves the value mapped to the given key as an array, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param defaultValue the array to return when the key is absent
     * @return the value as an {@code Object} array, or {@code defaultValue} if the key is absent
     */
    Object[] getArray(String key, Object[] defaultValue);

    /**
     * Retrieves the value mapped to the given key as an array, falling back to the given list when the key is absent.
     *
     * @param key          the parameter key
     * @param defaultValue the list whose elements form the fallback array when the key is absent
     * @return the value as an {@code Object} array, or an array derived from {@code defaultValue} if the key is absent
     */
    Object[] getArray(String key, List<?> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a typed array whose elements are converted to the given element
     * type.
     *
     * @param key         the parameter key
     * @param elementType the type each array element should be converted to
     * @param <T>         the array element type
     * @return the typed array, or {@code null} if the key is absent
     */
    <T> T[] getArray(String key, Class<T> elementType);

    /**
     * Retrieves the value mapped to the given key as a typed array, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param elementType  the type each array element should be converted to
     * @param defaultValue the array to return when the key is absent
     * @param <T>          the array element type
     * @return the typed array, or {@code defaultValue} if the key is absent
     */
    <T> T[] getArray(String key, Class<T> elementType, T[] defaultValue);

    /**
     * Retrieves the value mapped to the given key as a typed array, falling back to the given list when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param elementType  the type each array element should be converted to
     * @param defaultValue the list whose elements form the fallback array when the key is absent
     * @param <T>          the array element type
     * @return the typed array, or an array derived from {@code defaultValue} if the key is absent
     */
    <T> T[] getArray(String key, Class<T> elementType, List<T> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link Boolean}.
     *
     * @param key the parameter key
     * @return the boolean value, or {@code null} if the key is absent
     */
    Boolean getBoolean(String key);

    /**
     * Retrieves the value mapped to the given key as a primitive boolean, falling back to a default when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the boolean value, or {@code defaultValue} if the key is absent
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link Date}.
     *
     * @param key the parameter key
     * @return the date value, or {@code null} if the key is absent
     */
    Date getDate(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link Date}, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the date value, or {@code defaultValue} if the key is absent
     */
    Date getDate(String key, Date defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link Double}.
     *
     * @param key the parameter key
     * @return the double value, or {@code null} if the key is absent
     */
    Double getDouble(String key);

    /**
     * Retrieves the value mapped to the given key as a primitive double, falling back to a default when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the double value, or {@code defaultValue} if the key is absent
     */
    double getDouble(String key, double defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link Duration}.
     *
     * @param key the parameter key
     * @return the duration value, or {@code null} if the key is absent
     */
    Duration getDuration(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link Duration}, falling back to a default when the key is
     * absent.
     *
     * @param key             the parameter key
     * @param defaultDuration the value to return when the key is absent
     * @return the duration value, or {@code defaultDuration} if the key is absent
     */
    Duration getDuration(String key, Duration defaultDuration);

    /**
     * Retrieves the value mapped to the given key as a {@link FileEntry}.
     *
     * @param key the parameter key
     * @return the file entry, or {@code null} if the key is absent
     */
    FileEntry getFileEntry(String key);

    /**
     * Retrieves the value mapped to the given key as a list of {@link FileEntry} instances.
     *
     * @param key the parameter key
     * @return the list of file entries, or {@code null} if the key is absent
     */
    List<FileEntry> getFileEntries(String key);

    /**
     * Retrieves the value mapped to the given key as a list of {@link FileEntry} instances, falling back to a default
     * when the key is absent.
     *
     * @param key          the parameter key
     * @param defaultValue the list to return when the key is absent
     * @return the list of file entries, or {@code defaultValue} if the key is absent
     */
    List<FileEntry> getFileEntries(String key, List<FileEntry> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link Float}.
     *
     * @param key the parameter key
     * @return the float value, or {@code null} if the key is absent
     */
    Float getFloat(String key);

    /**
     * Retrieves the value mapped to the given key as a primitive float, falling back to a default when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the float value, or {@code defaultValue} if the key is absent
     */
    float getFloat(String key, float defaultValue);

    /**
     * Retrieves the value located at the given dotted path, converted to the requested type.
     *
     * @param path        the dotted path to the value
     * @param elementType the type the value should be converted to
     * @param <T>         the requested value type
     * @return the converted value, or {@code null} if nothing exists at the path
     */
    <T> T getFromPath(String path, Class<T> elementType);

    /**
     * Retrieves the value located at the given dotted path, converted to the requested type, falling back to a default
     * when nothing exists at the path.
     *
     * @param path         the dotted path to the value
     * @param elementType  the type the value should be converted to
     * @param defaultValue the value to return when nothing exists at the path
     * @param <T>          the requested value type
     * @return the converted value, or {@code defaultValue} if nothing exists at the path
     */
    <T> T getFromPath(String path, Class<T> elementType, T defaultValue);

    /**
     * Retrieves the value located at the given dotted path, converted using the supplied {@link TypeReference} to
     * preserve generic type information.
     *
     * @param path                 the dotted path to the value
     * @param elementTypeReference the type reference describing the target type
     * @param <T>                  the requested value type
     * @return the converted value, or {@code null} if nothing exists at the path
     */
    <T> T getFromPath(String path, TypeReference<T> elementTypeReference);

    /**
     * Retrieves the value located at the given dotted path, converted using the supplied {@link TypeReference}, falling
     * back to a default when nothing exists at the path.
     *
     * @param path                 the dotted path to the value
     * @param elementTypeReference the type reference describing the target type
     * @param defaultValue         the value to return when nothing exists at the path
     * @param <T>                  the requested value type
     * @return the converted value, or {@code defaultValue} if nothing exists at the path
     */
    <T> T getFromPath(String path, TypeReference<T> elementTypeReference, T defaultValue);

    /**
     * Retrieves the value mapped to the given key as an {@link Integer}.
     *
     * @param key the parameter key
     * @return the integer value, or {@code null} if the key is absent
     */
    Integer getInteger(String key);

    /**
     * Retrieves the value mapped to the given key as a primitive int, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the integer value, or {@code defaultValue} if the key is absent
     */
    int getInteger(String key, int defaultValue);

    /**
     * Retrieves the value mapped to the given key as a list.
     *
     * @param key the parameter key
     * @return the list value, or {@code null} if the key is absent
     */
    List<?> getList(String key);

    /**
     * Retrieves the value mapped to the given key as a list, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param defaultValue the list to return when the key is absent
     * @return the list value, or {@code defaultValue} if the key is absent
     */
    List<?> getList(String key, List<?> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a list whose elements are converted to the given element type.
     *
     * @param key         the parameter key
     * @param elementType the type each element should be converted to
     * @param <T>         the list element type
     * @return the typed list, or {@code null} if the key is absent
     */
    <T> List<T> getList(String key, Class<T> elementType);

    /**
     * Retrieves the value mapped to the given key as a list whose elements are converted using the supplied
     * {@link TypeReference} to preserve generic type information.
     *
     * @param key                  the parameter key
     * @param elementTypeReference the type reference describing the element type
     * @param <T>                  the list element type
     * @return the typed list, or {@code null} if the key is absent
     */
    <T> List<T> getList(String key, TypeReference<T> elementTypeReference);

    /**
     * Retrieves the value mapped to the given key as a typed list, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param elementType  the type each element should be converted to
     * @param defaultValue the list to return when the key is absent
     * @param <T>          the list element type
     * @return the typed list, or {@code defaultValue} if the key is absent
     */
    <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a list whose elements may be converted to any of the supplied
     * element types.
     *
     * @param key          the parameter key
     * @param elementTypes the candidate types the elements may be converted to
     * @return the list value, or {@code null} if the key is absent
     */
    List<?> getList(String key, Class<?>[] elementTypes);

    /**
     * Retrieves the value mapped to the given key as a list whose elements may be converted to any of the supplied
     * element types, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param elementTypes the candidate types the elements may be converted to
     * @param defaultValue the list to return when the key is absent
     * @return the list value, or {@code defaultValue} if the key is absent
     */
    List<?> getList(String key, List<Class<?>> elementTypes, List<?> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a typed list, converted using the supplied {@link TypeReference},
     * falling back to a default when the key is absent.
     *
     * @param rows          the parameter key
     * @param typeReference the type reference describing the element type
     * @param defaultValue  the list to return when the key is absent
     * @param <T>           the list element type
     * @return the typed list, or {@code defaultValue} if the key is absent
     */
    <T> List<T> getList(String rows, TypeReference<T> typeReference, List<T> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalDate}.
     *
     * @param key the parameter key
     * @return the local date value, or {@code null} if the key is absent
     */
    LocalDate getLocalDate(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalDate}, falling back to a default when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the local date value, or {@code defaultValue} if the key is absent
     */
    LocalDate getLocalDate(String key, LocalDate defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalDateTime}.
     *
     * @param key the parameter key
     * @return the local date-time value, or {@code null} if the key is absent
     */
    LocalDateTime getLocalDateTime(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalDateTime}, falling back to a default when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the local date-time value, or {@code defaultValue} if the key is absent
     */
    LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalTime}.
     *
     * @param key the parameter key
     * @return the local time value, or {@code null} if the key is absent
     */
    LocalTime getLocalTime(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalTime}, falling back to a default when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the local time value, or {@code defaultValue} if the key is absent
     */
    LocalTime getLocalTime(String key, LocalTime defaultValue);

    /**
     * Retrieves the value mapped to the given key as a {@link Long}.
     *
     * @param key the parameter key
     * @return the long value, or {@code null} if the key is absent
     */
    Long getLong(String key);

    /**
     * Retrieves the value mapped to the given key as a primitive long, falling back to a default when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the long value, or {@code defaultValue} if the key is absent
     */
    long getLong(String key, long defaultValue);

    /**
     * Retrieves the value mapped to the given key as a map.
     *
     * @param key the parameter key
     * @return the map value, or {@code null} if the key is absent
     */
    Map<String, ?> getMap(String key);

    /**
     * Retrieves the value mapped to the given key as a map, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param defaultValue the map to return when the key is absent
     * @return the map value, or {@code defaultValue} if the key is absent
     */
    Map<String, ?> getMap(String key, Map<String, ?> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a map whose values are converted to the given value type.
     *
     * @param key       the parameter key
     * @param valueType the type each map value should be converted to
     * @param <V>       the map value type
     * @return the typed map, or {@code null} if the key is absent
     */
    <V> Map<String, V> getMap(String key, Class<V> valueType);

    /**
     * Retrieves the value mapped to the given key as a map whose values are converted using the supplied
     * {@link TypeReference} to preserve generic type information.
     *
     * @param key                the parameter key
     * @param valueTypeReference the type reference describing the value type
     * @param <V>                the map value type
     * @return the typed map, or {@code null} if the key is absent
     */
    <V> Map<String, V> getMap(String key, TypeReference<V> valueTypeReference);

    /**
     * Retrieves the value mapped to the given key as a typed map, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param valueType    the type each map value should be converted to
     * @param defaultValue the map to return when the key is absent
     * @param <V>          the map value type
     * @return the typed map, or {@code defaultValue} if the key is absent
     */
    <V> Map<String, V> getMap(String key, Class<V> valueType, Map<String, V> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a typed map, converted using the supplied {@link TypeReference},
     * falling back to a default when the key is absent.
     *
     * @param key                the parameter key
     * @param valueTypeReference the type reference describing the value type
     * @param defaultValue       the map to return when the key is absent
     * @param <V>                the map value type
     * @return the typed map, or {@code defaultValue} if the key is absent
     */
    <V> Map<String, V> getMap(String key, TypeReference<V> valueTypeReference, Map<String, V> defaultValue);

    /**
     * Retrieves the value mapped to the given key as a map whose values may be converted to any of the supplied value
     * types.
     *
     * @param key        the parameter key
     * @param valueTypes the candidate types the map values may be converted to
     * @return the map value, or {@code null} if the key is absent
     */
    Map<String, ?> getMap(String key, List<Class<?>> valueTypes);

    /**
     * Retrieves the value mapped to the given key as a map whose values may be converted to any of the supplied value
     * types, falling back to a default when the key is absent.
     *
     * @param key          the parameter key
     * @param valueTypes   the candidate types the map values may be converted to
     * @param defaultValue the map to return when the key is absent
     * @return the map value, or {@code defaultValue} if the key is absent
     */
    Map<String, ?> getMap(String key, List<Class<?>> valueTypes, Map<String, ?> defaultValue);

    /**
     * Retrieves the value located at the given dotted path as a map whose values may be converted to any of the
     * supplied value types.
     *
     * @param path       the dotted path to the value
     * @param valueTypes the candidate types the map values may be converted to
     * @return the map value, or {@code null} if nothing exists at the path
     */
    Map<String, ?> getMapFromPath(String path, List<Class<?>> valueTypes);

    /**
     * Retrieves the value located at the given dotted path as a map whose values may be converted to any of the
     * supplied value types, falling back to a default when nothing exists at the path.
     *
     * @param path         the dotted path to the value
     * @param valueTypes   the candidate types the map values may be converted to
     * @param defaultValue the map to return when nothing exists at the path
     * @return the map value, or {@code defaultValue} if nothing exists at the path
     */
    Map<String, ?> getMapFromPath(String path, List<Class<?>> valueTypes, Map<String, ?> defaultValue);

    /**
     * Retrieves the value mapped to the given key, requiring it to be present.
     *
     * @param key the parameter key
     * @return the value mapped to the key
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    Object getRequired(String key);

    /**
     * Retrieves the value mapped to the given key converted to the requested type, requiring it to be present.
     *
     * @param key        the parameter key
     * @param returnType the type the value should be converted to
     * @param <T>        the requested value type
     * @return the converted value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    <T> T getRequired(String key, Class<T> returnType);

    /**
     * Retrieves the value mapped to the given key as an array, requiring it to be present.
     *
     * @param key the parameter key
     * @return the value as an {@code Object} array
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    Object[] getRequiredArray(String key);

    /**
     * Retrieves the value mapped to the given key as a typed array, requiring it to be present.
     *
     * @param key         the parameter key
     * @param elementType the type each array element should be converted to
     * @param <T>         the array element type
     * @return the typed array
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    <T> T[] getRequiredArray(String key, Class<T> elementType);

    /**
     * Retrieves the value mapped to the given key as a primitive boolean, requiring it to be present.
     *
     * @param key the parameter key
     * @return the boolean value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    boolean getRequiredBoolean(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link Date}, requiring it to be present.
     *
     * @param key the parameter key
     * @return the date value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    Date getRequiredDate(String key);

    /**
     * Retrieves the value mapped to the given key as a primitive double, requiring it to be present.
     *
     * @param key the parameter key
     * @return the double value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    double getRequiredDouble(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link FileEntry}, requiring it to be present.
     *
     * @param key the parameter key
     * @return the file entry
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    FileEntry getRequiredFileEntry(String key);

    /**
     * Retrieves the value mapped to the given key as a primitive float, requiring it to be present.
     *
     * @param key the parameter key
     * @return the float value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    float getRequiredFloat(String key);

    /**
     * Retrieves the value located at the given dotted path converted to the requested type, requiring it to be present.
     *
     * @param path        the dotted path to the value
     * @param elementType the type the value should be converted to
     * @param <T>         the requested value type
     * @return the converted value
     * @throws IllegalStateException if nothing exists at the path
     */
    <T> T getRequiredFromPath(String path, Class<T> elementType);

    /**
     * Retrieves the value located at the given dotted path converted using the supplied {@link TypeReference},
     * requiring it to be present.
     *
     * @param path                 the dotted path to the value
     * @param elementTypeReference the type reference describing the target type
     * @param <T>                  the requested value type
     * @return the converted value
     * @throws IllegalStateException if nothing exists at the path
     */
    <T> T getRequiredFromPath(String path, TypeReference<T> elementTypeReference);

    /**
     * Retrieves the value mapped to the given key as a primitive int, requiring it to be present.
     *
     * @param key the parameter key
     * @return the integer value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    int getRequiredInteger(String key);

    /**
     * Retrieves the value mapped to the given key as a list, requiring it to be present.
     *
     * @param key the parameter key
     * @return the list value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    List<?> getRequiredList(String key);

    /**
     * Retrieves the value mapped to the given key as a typed list, requiring it to be present.
     *
     * @param key         the parameter key
     * @param elementType the type each element should be converted to
     * @param <T>         the list element type
     * @return the typed list
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    <T> List<T> getRequiredList(String key, Class<T> elementType);

    /**
     * Retrieves the value mapped to the given key as a typed list, converted using the supplied {@link TypeReference},
     * requiring it to be present.
     *
     * @param key                  the parameter key
     * @param elementTypeReference the type reference describing the element type
     * @param <T>                  the list element type
     * @return the typed list
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    <T> List<T> getRequiredList(String key, TypeReference<T> elementTypeReference);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalDate}, requiring it to be present.
     *
     * @param key the parameter key
     * @return the local date value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    LocalDate getRequiredLocalDate(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalDateTime}, requiring it to be present.
     *
     * @param key the parameter key
     * @return the local date-time value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    LocalDateTime getRequiredLocalDateTime(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link LocalTime}, requiring it to be present.
     *
     * @param key the parameter key
     * @return the local time value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    LocalTime getRequiredLocalTime(String key);

    /**
     * Retrieves the value mapped to the given key as a primitive long, requiring it to be present.
     *
     * @param key the parameter key
     * @return the long value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    long getRequiredLong(String key);

    /**
     * Retrieves the value mapped to the given key as a map, requiring it to be present.
     *
     * @param key the parameter key
     * @return the map value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    Map<String, ?> getRequiredMap(String key);

    /**
     * Retrieves the value mapped to the given key as a typed map, requiring it to be present.
     *
     * @param key       the parameter key
     * @param valueType the type each map value should be converted to
     * @param <V>       the map value type
     * @return the typed map
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    <V> Map<String, V> getRequiredMap(String key, Class<V> valueType);

    /**
     * Retrieves the value mapped to the given key as a typed map, converted using the supplied {@link TypeReference},
     * requiring it to be present.
     *
     * @param key                the parameter key
     * @param valueTypeReference the type reference describing the value type
     * @param <V>                the map value type
     * @return the typed map
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    <V> Map<String, V> getRequiredMap(String key, TypeReference<V> valueTypeReference);

    /**
     * Retrieves the value mapped to the given key as a {@link String}, requiring it to be present.
     *
     * @param key the parameter key
     * @return the string value
     * @throws IllegalStateException if the key is absent or its value is {@code null}
     */
    String getRequiredString(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link String}.
     *
     * @param key the parameter key
     * @return the string value, or {@code null} if the key is absent
     */
    String getString(String key);

    /**
     * Retrieves the value mapped to the given key as a {@link String}, falling back to a default when the key is
     * absent.
     *
     * @param key          the parameter key
     * @param defaultValue the value to return when the key is absent
     * @return the string value, or {@code defaultValue} if the key is absent
     */
    String getString(String key, String defaultValue);

    /**
     * Returns the parameters as a plain, immutable {@link Map} view.
     *
     * @return a map containing all parameter entries
     */
    Map<String, ?> toMap();
}
