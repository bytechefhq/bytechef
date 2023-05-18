
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

/**
 * @author Ivica Cardic
 */
public final class MapValueReader implements MapValueUtils.MapValueReader {

    @Override
    public boolean containsKey(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.containsKey(map, key);
    }

    @Override
    public Object get(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.get(map, key);
    }

    @Override
    public <T> T get(Map<String, ?> map, String key, Class<T> returnType) {
        return com.bytechef.commons.util.MapValueUtils.get(map, key, returnType);
    }

    @Override
    public <T> T get(Map<String, ?> map, String key, Class<T> returnType, T defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.get(map, key, returnType, defaultValue);
    }

    @Override
    public Object[] getArray(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getArray(map, key);
    }

    @Override
    public Object[] getArray(Map<String, ?> map, String key, Object[] defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getArray(map, key, defaultValue);
    }

    @Override
    public <T> T[] getArray(Map<String, ?> map, String key, Class<T> elementType) {
        return com.bytechef.commons.util.MapValueUtils.getArray(map, key, elementType);
    }

    @Override
    public <T> T[] getArray(Map<String, ?> map, String key, Class<T> elementType, T[] defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getArray(map, key, elementType, defaultValue);
    }

    @Override
    public Boolean getBoolean(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getBoolean(map, key);
    }

    @Override
    public boolean getBoolean(Map<String, ?> map, String key, boolean defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getBoolean(map, key, defaultValue);
    }

    @Override
    public Date getDate(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getDate(map, key);
    }

    @Override
    public Date getDate(Map<String, ?> map, String key, Date defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getDate(map, key, defaultValue);
    }

    @Override
    public Double getDouble(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getDouble(map, key);
    }

    @Override
    public double getDouble(Map<String, ?> map, String key, double defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getDouble(map, key, defaultValue);
    }

    @Override
    public Duration getDuration(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getDuration(map, key);
    }

    @Override
    public Duration getDuration(Map<String, ?> map, String key, Duration defaultDuration) {
        return com.bytechef.commons.util.MapValueUtils.getDuration(map, key, defaultDuration);
    }

    @Override
    public Float getFloat(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getFloat(map, key);
    }

    @Override
    public float getFloat(Map<String, ?> map, String key, float defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getFloat(map, key, defaultValue);
    }

    @Override
    public Integer getInteger(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getInteger(map, key);
    }

    @Override
    public int getInteger(Map<String, ?> map, String key, int defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getInteger(map, key, defaultValue);
    }

    @Override
    public List<?> getList(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getList(map, key);
    }

    @Override
    public List<?> getList(Map<String, ?> map, String key, List<?> defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getList(map, key, defaultValue);
    }

    @Override
    public <T> List<T> getList(Map<String, ?> map, String key, Class<T> elementType) {
        return com.bytechef.commons.util.MapValueUtils.getList(map, key, elementType);
    }

    @Override
    public <T> List<T> getList(Map<String, ?> map, String key, Class<T> elementType, List<T> defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getList(map, key, elementType, defaultValue);
    }

    @Override
    public List<?> getList(Map<String, ?> map, String key, Class<?>[] elementTypes) {
        return com.bytechef.commons.util.MapValueUtils.getList(map, key, elementTypes);
    }

    @Override
    public List<?> getList(Map<String, ?> map, String key, List<Class<?>> elementTypes, List<?> defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getList(map, key, elementTypes, defaultValue);
    }

    @Override
    public LocalDate getLocalDate(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getLocalDate(map, key);
    }

    @Override
    public LocalDate getLocalDate(Map<String, ?> map, String key, LocalDate defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getLocalDate(map, key, defaultValue);
    }

    @Override
    public LocalDateTime getLocalDateTime(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getLocalDateTime(map, key);
    }

    @Override
    public LocalDateTime getLocalDateTime(Map<String, ?> map, String key, LocalDateTime defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getLocalDateTime(map, key, defaultValue);
    }

    @Override
    public LocalTime getLocalTime(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getLocalTime(map, key);
    }

    @Override
    public LocalTime getLocalTime(Map<String, ?> map, String key, LocalTime defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getLocalTime(map, key, defaultValue);
    }

    @Override
    public Long getLong(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getLong(map, key);
    }

    @Override
    public long getLong(Map<String, ?> map, String key, long defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getLong(map, key, defaultValue);
    }

    @Override
    public Map<String, ?> getMap(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getMap(map, key);
    }

    @Override
    public Map<String, ?> getMap(Map<String, ?> map, String key, Map<String, ?> defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getMap(map, key, defaultValue);
    }

    @Override
    public <V> Map<String, V> getMap(Map<String, ?> map, String key, Class<V> valueType) {
        return com.bytechef.commons.util.MapValueUtils.getMap(map, key, valueType);
    }

    @Override
    public <V> Map<String, V> getMap(Map<String, ?> map, String key, Class<V> valueType, Map<String, V> defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getMap(map, key, valueType, defaultValue);
    }

    @Override
    public Map<String, ?> getMap(Map<String, ?> map, String key, List<Class<?>> valueTypes) {
        return com.bytechef.commons.util.MapValueUtils.getMap(map, key, valueTypes);
    }

    @Override
    public Map<String, ?> getMap(
        Map<String, ?> map, String key, List<Class<?>> valueTypes, Map<String, ?> defaultValue) {

        return com.bytechef.commons.util.MapValueUtils.getMap(map, key, valueTypes, defaultValue);
    }

    @Override
    public Object getRequired(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequired(map, key);
    }

    @Override
    public <T> T getRequired(Map<String, ?> map, String key, Class<T> returnType) {
        return com.bytechef.commons.util.MapValueUtils.getRequired(map, key, returnType);
    }

    @Override
    public Object[] getRequiredArray(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredArray(map, key);
    }

    @Override
    public <T> T[] getRequiredArray(Map<String, ?> map, String key, Class<T> elementType) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredArray(map, key, elementType);
    }

    @Override
    public Boolean getRequiredBoolean(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredBoolean(map, key);
    }

    @Override
    public Date getRequiredDate(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredDate(map, key);
    }

    @Override
    public Double getRequiredDouble(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredDouble(map, key);
    }

    @Override
    public Float getRequiredFloat(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredFloat(map, key);
    }

    @Override
    public Integer getRequiredInteger(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredInteger(map, key);
    }

    @Override
    public List<?> getRequiredList(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredList(map, key);
    }

    @Override
    public <T> List<T> getRequiredList(Map<String, ?> map, String key, Class<T> elementType) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredList(map, key, elementType);
    }

    @Override
    public LocalDate getRequiredLocalDate(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredLocalDate(map, key);
    }

    @Override
    public LocalDateTime getRequiredLocalDateTime(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredLocalDateTime(map, key);
    }

    @Override
    public LocalTime getRequiredLocalTime(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredLocalTime(map, key);
    }

    @Override
    public Map<String, ?> getRequiredMap(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredMap(map, key);
    }

    @Override
    public <V> Map<String, V> getRequiredMap(Map<String, ?> map, String key, Class<V> valueType) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredMap(map, key, valueType);
    }

    @Override
    public String getRequiredString(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getRequiredString(map, key);
    }

    @Override
    public String getString(Map<String, ?> map, String key) {
        return com.bytechef.commons.util.MapValueUtils.getString(map, key);
    }

    @Override
    public String getString(Map<String, ?> map, String key, String defaultValue) {
        return com.bytechef.commons.util.MapValueUtils.getString(map, key, defaultValue);
    }
}
