/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.definition.ActionContext.FileEntry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface Parameters extends Map<String, Object> {

    <T> T get(String key, Class<T> returnType);

    <T> T get(String key, Class<T> returnType, T defaultValue);

    Object[] getArray(String key);

    Object[] getArray(String key, Object[] defaultValue);

    <T> T[] getArray(String key, Class<T> elementType);

    <T> T[] getArray(String key, Class<T> elementType, T[] defaultValue);

    Boolean getBoolean(String key);

    boolean getBoolean(String key, boolean defaultValue);

    Date getDate(String key);

    Date getDate(String key, Date defaultValue);

    Double getDouble(String key);

    double getDouble(String key, double defaultValue);

    Duration getDuration(String key);

    Duration getDuration(String key, Duration defaultDuration);

    FileEntry getFileEntry(String key);

    List<FileEntry> getFileEntries(String key, List<FileEntry> defaultValue);

    Float getFloat(String key);

    float getFloat(String key, float defaultValue);

    Integer getInteger(String key);

    int getInteger(String key, int defaultValue);

    List<?> getList(String key);

    List<?> getList(String key, List<?> defaultValue);

    <T> List<T> getList(String key, Class<T> elementType);

    <T> List<T> getList(String key, Context.TypeReference<T> elementTypeReference);

    <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue);

    List<?> getList(String key, Class<?>[] elementTypes);

    List<?> getList(String key, List<Class<?>> elementTypes, List<?> defaultValue);

    <T> List<T> getList(String rows, Context.TypeReference<T> typeReference, List<T> defaultValue);

    LocalDate getLocalDate(String key);

    LocalDate getLocalDate(String key, LocalDate defaultValue);

    LocalDateTime getLocalDateTime(String key);

    LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue);

    LocalTime getLocalTime(String key);

    LocalTime getLocalTime(String key, LocalTime defaultValue);

    Long getLong(String key);

    long getLong(String key, long defaultValue);

    Map<String, ?> getMap(String key);

    Map<String, ?> getMap(String key, Map<String, ?> defaultValue);

    <V> Map<String, V> getMap(String key, Class<V> valueType);

    <V> Map<String, V> getMap(String key, Context.TypeReference<V> valueTypeReference);

    <V> Map<String, V> getMap(String key, Class<V> valueType, Map<String, V> defaultValue);

    <V> Map<String, V> getMap(String key, Context.TypeReference<V> valueTypeReference, Map<String, V> defaultValue);

    Map<String, ?> getMap(String key, List<Class<?>> valueTypes);

    Map<String, ?> getMap(String key, List<Class<?>> valueTypes, Map<String, ?> defaultValue);

    Object getRequired(String key);

    <T> T getRequired(String key, Class<T> returnType);

    Object[] getRequiredArray(String key);

    <T> T[] getRequiredArray(String key, Class<T> elementType);

    boolean getRequiredBoolean(String key);

    Date getRequiredDate(String key);

    double getRequiredDouble(String key);

    FileEntry getRequiredFileEntry(String key);

    float getRequiredFloat(String key);

    int getRequiredInteger(String key);

    List<?> getRequiredList(String key);

    <T> List<T> getRequiredList(String key, Class<T> elementType);

    <T> List<T> getRequiredList(String key, Context.TypeReference<T> elementTypeReference);

    LocalDate getRequiredLocalDate(String key);

    LocalDateTime getRequiredLocalDateTime(String key);

    LocalTime getRequiredLocalTime(String key);

    long getRequiredLong(String key);

    Map<String, ?> getRequiredMap(String key);

    <V> Map<String, V> getRequiredMap(String key, Class<V> valueType);

    <V> Map<String, V> getRequiredMap(String key, Context.TypeReference<V> valueTypeReference);

    String getRequiredString(String key);

    String getString(String key);

    String getString(String key, String defaultValue);
}
