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

package com.bytechef.platform.component.definition;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public final class ParametersImpl implements Parameters {

    private final Map<String, Object> map;

    @SuppressWarnings("unchecked")
    public ParametersImpl(Map<String, ?> map) {
        this.map = map == null ? Collections.emptyMap() : (Map<String, Object>) Collections.unmodifiableMap(map);
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsPath(String path) {
        return MapUtils.containsPath(map, path);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public <T> T get(String key, Class<T> returnType) {
        return MapUtils.get(map, key, returnType);
    }

    @Override
    public <T> T get(String key, Class<T> returnType, T defaultValue) {
        return MapUtils.get(map, key, returnType, defaultValue);
    }

    @Override
    public Object[] getArray(String key) {
        return MapUtils.getArray(map, key);
    }

    @Override
    public Object[] getArray(String key, Object[] defaultValue) {
        return MapUtils.getArray(map, key, defaultValue);
    }

    @Override
    public Object[] getArray(String key, List<?> defaultValue) {
        return MapUtils.getArray(map, key, defaultValue.toArray(new Object[0]));
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType) {
        return MapUtils.getArray(map, key, elementType);
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType, T[] defaultValue) {
        return MapUtils.getArray(map, key, elementType, defaultValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] getArray(String key, Class<T> elementType, List<T> defaultValue) {
        return (T[]) MapUtils.getArray(map, key, defaultValue.toArray(new Object[0]));
    }

    @Override
    public Boolean getBoolean(String key) {
        return MapUtils.getBoolean(map, key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return MapUtils.getBoolean(map, key, defaultValue);
    }

    @Override
    public Date getDate(String key) {
        return MapUtils.getDate(map, key);
    }

    @Override
    public Date getDate(String key, Date defaultValue) {
        return MapUtils.getDate(map, key, defaultValue);
    }

    @Override
    public Double getDouble(String key) {
        return MapUtils.getDouble(map, key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return MapUtils.getDouble(map, key, defaultValue);
    }

    @Override
    public Duration getDuration(String key) {
        return MapUtils.getDuration(map, key);
    }

    @Override
    public Duration getDuration(String key, Duration defaultValue) {
        return MapUtils.getDuration(map, key, defaultValue);
    }

    @Override
    public FileEntry getFileEntry(String key) {
        return MapUtils.get(map, key, FileEntry.class);
    }

    @Override
    public List<FileEntry> getFileEntries(String key) {
        return getList(key, FileEntry.class);
    }

    @Override
    public List<FileEntry> getFileEntries(String key, List<FileEntry> defaultValue) {
        return getList(key, FileEntry.class, defaultValue);
    }

    @Override
    public Float getFloat(String key) {
        return MapUtils.getFloat(map, key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return MapUtils.getFloat(map, key, defaultValue);
    }

    @Override
    public <T> T getFromPath(String path, Class<T> elementType) {
        return MapUtils.getFromPath(map, path, elementType);
    }

    @Override
    public <T> T getFromPath(String path, Class<T> elementType, T defaultValue) {
        return MapUtils.getFromPath(map, path, elementType, defaultValue);
    }

    @Override
    public <T> T getFromPath(String path, com.bytechef.component.definition.TypeReference<T> elementTypeReference) {
        return MapUtils.getFromPath(map, path, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return elementTypeReference.getType();
            }
        });
    }

    @Override
    public <T> T getFromPath(
        String path, com.bytechef.component.definition.TypeReference<T> elementTypeReference, T defaultValue) {
        return MapUtils.getFromPath(map, path, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return elementTypeReference.getType();
            }
        }, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return MapUtils.getInteger(map, key);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return MapUtils.getInteger(map, key, defaultValue);
    }

    @Override
    public List<?> getList(String key) {
        return MapUtils.getList(map, key);
    }

    @Override
    public List<?> getList(String key, List<?> defaultValue) {
        return MapUtils.getList(map, key, defaultValue);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        return MapUtils.getList(map, key, elementType);
    }

    @Override
    public <T> List<T> getList(String key, com.bytechef.component.definition.TypeReference<T> elementTypeReference) {
        return MapUtils.getList(map, key, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return elementTypeReference.getType();
            }
        });
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
        return MapUtils.getList(map, key, elementType, defaultValue);
    }

    @Override
    public <T> List<T> getList(
        String key, com.bytechef.component.definition.TypeReference<T> elementTypeReference, List<T> defaultValue) {
        return MapUtils.getList(map, key, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return elementTypeReference.getType();
            }
        }, defaultValue);
    }

    @Override
    public List<?> getList(String key, Class<?>[] elementTypes) {
        return MapUtils.getList(map, key, elementTypes);
    }

    @Override
    public List<?> getList(String key, List<Class<?>> elementTypes, List<?> defaultValue) {
        return MapUtils.getList(map, key, elementTypes, defaultValue);
    }

    @Override
    public LocalDate getLocalDate(String key) {
        return MapUtils.getLocalDate(map, key);
    }

    @Override
    public LocalDate getLocalDate(String key, LocalDate defaultValue) {
        return MapUtils.getLocalDate(map, key, defaultValue);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key) {
        return MapUtils.getLocalDateTime(map, key);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
        return MapUtils.getLocalDateTime(map, key, defaultValue);
    }

    @Override
    public LocalTime getLocalTime(String key) {
        return MapUtils.getLocalTime(map, key);
    }

    @Override
    public LocalTime getLocalTime(String key, LocalTime defaultValue) {
        return MapUtils.getLocalTime(map, key, defaultValue);
    }

    @Override
    public Long getLong(String key) {
        return MapUtils.getLong(map, key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return MapUtils.getLong(map, key, defaultValue);
    }

    @Override
    public Map<String, ?> getMap(String key) {
        return MapUtils.getMap(map, key);
    }

    @Override
    public Map<String, ?> getMap(String key, Map<String, ?> defaultValue) {
        return MapUtils.getMap(map, key, defaultValue);
    }

    @Override
    public <V> Map<String, V> getMap(String key, Class<V> valueType) {
        return MapUtils.getMap(map, key, valueType);
    }

    @Override
    public <V> Map<String, V>
        getMap(String key, com.bytechef.component.definition.TypeReference<V> valueTypeReference) {
        return MapUtils.getMap(map, key, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return valueTypeReference.getType();
            }
        });
    }

    @Override
    public <V> Map<String, V> getMap(String key, Class<V> valueType, Map<String, V> defaultValue) {
        return MapUtils.getMap(map, key, valueType, defaultValue);
    }

    @Override
    public <V> Map<String, V>
        getMap(
            String key, com.bytechef.component.definition.TypeReference<V> valueTypeReference,
            Map<String, V> defaultValue) {
        return MapUtils.getMap(map, key, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return valueTypeReference.getType();
            }
        }, defaultValue);
    }

    @Override
    public Map<String, ?> getMap(String key, List<Class<?>> valueTypes) {
        return MapUtils.getMap(map, key, valueTypes);
    }

    @Override
    public Map<String, ?> getMap(String key, List<Class<?>> valueTypes, Map<String, ?> defaultValue) {
        return MapUtils.getMap(map, key, valueTypes, defaultValue);
    }

    @Override
    public Map<String, ?> getMapFromPath(String path, List<Class<?>> valueTypes) {
        return MapUtils.getMapFromPath(map, path, valueTypes);
    }

    @Override
    public Map<String, ?> getMapFromPath(String path, List<Class<?>> valueTypes, Map<String, ?> defaultValue) {
        return MapUtils.getMapFromPath(map, path, valueTypes, defaultValue);
    }

    @Override
    public Object getRequired(String key) {
        return MapUtils.getRequired(map, key);
    }

    @Override
    public <T> T getRequired(String key, Class<T> returnType) {
        return MapUtils.getRequired(map, key, returnType);
    }

    @Override
    public Object[] getRequiredArray(String key) {
        return MapUtils.getRequiredArray(map, key);
    }

    @Override
    public <T> T[] getRequiredArray(String key, Class<T> elementType) {
        return MapUtils.getRequiredArray(map, key, elementType);
    }

    @Override
    public boolean getRequiredBoolean(String key) {
        return MapUtils.getRequiredBoolean(map, key);
    }

    @Override
    public Date getRequiredDate(String key) {
        return MapUtils.getRequiredDate(map, key);
    }

    @Override
    public double getRequiredDouble(String key) {
        return MapUtils.getRequiredDouble(map, key);
    }

    @Override
    public FileEntry getRequiredFileEntry(String key) {
        return MapUtils.getRequired(map, key, FileEntry.class);
    }

    @Override
    public float getRequiredFloat(String key) {
        return MapUtils.getRequiredFloat(map, key);
    }

    @Override
    public <T> T getRequiredFromPath(String path, Class<T> elementType) {
        return MapUtils.getRequiredFromPath(map, path, elementType);
    }

    @Override
    public <T> T getRequiredFromPath(
        String path, com.bytechef.component.definition.TypeReference<T> elementTypeReference) {

        return MapUtils.getFromPath(map, path, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return elementTypeReference.getType();
            }
        });
    }

    @Override
    public int getRequiredInteger(String key) {
        return MapUtils.getRequiredInteger(map, key);
    }

    @Override
    public List<?> getRequiredList(String key) {
        return MapUtils.getRequiredList(map, key);
    }

    @Override
    public <T> List<T> getRequiredList(String key, Class<T> elementType) {
        return MapUtils.getRequiredList(map, key, elementType);
    }

    @Override
    public <T> List<T>
        getRequiredList(String key, com.bytechef.component.definition.TypeReference<T> elementTypeReference) {
        return MapUtils.getRequiredList(map, key, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return elementTypeReference.getType();
            }
        });
    }

    @Override
    public LocalDate getRequiredLocalDate(String key) {
        return MapUtils.getRequiredLocalDate(map, key);
    }

    @Override
    public LocalDateTime getRequiredLocalDateTime(String key) {
        return MapUtils.getRequiredLocalDateTime(map, key);
    }

    @Override
    public LocalTime getRequiredLocalTime(String key) {
        return MapUtils.getRequiredLocalTime(map, key);
    }

    @Override
    public long getRequiredLong(String key) {
        return MapUtils.getRequiredLong(map, key);
    }

    @Override
    public Map<String, ?> getRequiredMap(String key) {
        return MapUtils.getRequiredMap(map, key);
    }

    @Override
    public <V> Map<String, V> getRequiredMap(String key, Class<V> valueType) {
        return MapUtils.getRequiredMap(map, key, valueType);
    }

    @Override
    public <V> Map<String, V>
        getRequiredMap(String key, com.bytechef.component.definition.TypeReference<V> valueTypeReference) {
        return MapUtils.getRequiredMap(map, key, new TypeReference<>() {

            @Override
            @NonNull
            public Type getType() {
                return valueTypeReference.getType();
            }
        });
    }

    @Override
    public String getRequiredString(String key) {
        return MapUtils.getRequiredString(map, key);
    }

    @Override
    public String getString(String key) {
        return MapUtils.getString(map, key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return MapUtils.getString(map, key, defaultValue);
    }

    @Override
    public Map<String, ?> toMap() {
        return map;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    @NonNull
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    @NonNull
    public Collection<Object> values() {
        return map.values();
    }

    @Override
    public String toString() {
        return "ParameterMapImpl{" +
            "map=" + map +
            '}';
    }
}
