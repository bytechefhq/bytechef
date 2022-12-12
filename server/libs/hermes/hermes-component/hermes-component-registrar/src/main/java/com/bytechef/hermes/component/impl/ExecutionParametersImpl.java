
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

package com.bytechef.hermes.component.impl;

import com.bytechef.commons.utils.MapUtils;
import com.bytechef.hermes.component.ExecutionParameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ExecutionParametersImpl implements ExecutionParameters {

    private final Map<String, Object> parameters;

    @SuppressFBWarnings("EI2")
    public ExecutionParametersImpl(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean containsKey(String key) {
        return parameters.containsKey(key);
    }

    @Override
    public Object get(String key) {
        return MapUtils.get(parameters, key);
    }

    @Override
    public <T> T get(String key, Class<T> returnType) {
        return MapUtils.get(parameters, key, returnType);
    }

    @Override
    public <T> T get(String key, Class<T> returnType, T defaultValue) {
        return MapUtils.get(parameters, key, returnType, defaultValue);
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType) {
        return MapUtils.getArray(parameters, key, elementType);
    }

    @Override
    public Boolean getBoolean(String key) {
        return MapUtils.getBoolean(parameters, key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return MapUtils.getBoolean(parameters, key, defaultValue);
    }

    @Override
    public Date getDate(String key) {
        return MapUtils.getDate(parameters, key);
    }

    @Override
    public Date getDate(String key, Date defaultValue) {
        return MapUtils.getDate(parameters, key, defaultValue);
    }

    @Override
    public Double getDouble(String key) {
        return MapUtils.getDouble(parameters, key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return MapUtils.getDouble(parameters, key, defaultValue);
    }

    @Override
    public Float getFloat(String key) {
        return MapUtils.getFloat(parameters, key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return MapUtils.getFloat(parameters, key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return MapUtils.getInteger(parameters, key);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return MapUtils.getInteger(parameters, key, defaultValue);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        return MapUtils.getList(parameters, key, elementType);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
        return MapUtils.getList(parameters, key, elementType, defaultValue);
    }

    @Override
    public List<Object> getList(String key, List<Class<?>> elementTypes, List<Object> defaultValue) {
        return MapUtils.getList(parameters, key, elementTypes, defaultValue);
    }

    @Override
    public Long getLong(String key) {
        return MapUtils.getLong(parameters, key);
    }

    @Override
    public Duration getDuration(String key) {
        return MapUtils.getDuration(parameters, key);
    }

    @Override
    public Duration getDuration(String key, Duration defaultValue) {
        return null;
    }

    @Override
    public LocalDate getLocalDate(String key) {
        return MapUtils.getLocalDate(parameters, key);
    }

    @Override
    public LocalDate getLocalDate(String key, LocalDate defaultValue) {
        return MapUtils.getLocalDate(parameters, key, defaultValue);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key) {
        return MapUtils.getLocalDateTime(parameters, key);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
        return MapUtils.getLocalDateTime(parameters, key, defaultValue);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return MapUtils.getLong(parameters, key, defaultValue);
    }

    @Override
    public <V> Map<String, V> getMap(String key) {
        return MapUtils.getMap(parameters, key);
    }

    @Override
    public <V> Map<String, V> getMap(String key, Map<String, V> defaultValue) {
        return MapUtils.getMap(parameters, key, defaultValue);
    }

    @Override
    public Map<String, Object> getMap(String key, List<Class<?>> valueTypes, Map<String, Object> defaultValue) {
        return MapUtils.getMap(parameters, key, valueTypes, defaultValue);
    }

    @Override
    public Object getRequired(String key) {
        return MapUtils.getRequired(parameters, key);
    }

    @Override
    public <T> T getRequired(String key, Class<T> returnType) {
        return MapUtils.getRequired(parameters, key, returnType);
    }

    @Override
    public Boolean getRequiredBoolean(String key) {
        return MapUtils.getRequiredBoolean(parameters, key);
    }

    @Override
    public Date getRequiredDate(String key) {
        return MapUtils.getRequiredDate(parameters, key);
    }

    @Override
    public Double getRequiredDouble(String key) {
        return MapUtils.getRequiredDouble(parameters, key);
    }

    @Override
    public Float getRequiredFloat(String key) {
        return MapUtils.getRequiredFloat(parameters, key);
    }

    @Override
    public Integer getRequiredInteger(String key) {
        return MapUtils.getRequiredInteger(parameters, key);
    }

    @Override
    public LocalDate getRequiredLocalDate(String key) {
        return MapUtils.getRequiredLocalDate(parameters, key);
    }

    @Override
    public LocalDateTime getRequiredLocalDateTime(String key) {
        return MapUtils.getRequiredLocalDateTime(parameters, key);
    }

    @Override
    public String getRequiredString(String key) {
        return MapUtils.getRequiredString(parameters, key);
    }

    @Override
    public String getString(String key) {
        return MapUtils.getString(parameters, key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return MapUtils.getString(parameters, key, defaultValue);
    }
}
