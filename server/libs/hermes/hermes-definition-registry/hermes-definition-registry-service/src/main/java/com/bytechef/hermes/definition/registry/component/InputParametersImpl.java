
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

package com.bytechef.hermes.definition.registry.component;

import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.InputParameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public class InputParametersImpl implements InputParameters {

    private final Map<String, Object> parameters;

    @SuppressFBWarnings("EI2")
    public InputParametersImpl(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean containsKey(String key) {
        return parameters.containsKey(key);
    }

    @Override
    public Object get(String key) {
        return MapValueUtils.get(parameters, key);
    }

    @Override
    public <T> T get(String key, Class<T> returnType) {
        return MapValueUtils.get(parameters, key, returnType);
    }

    @Override
    public <T> T get(String key, Class<T> returnType, T defaultValue) {
        return MapValueUtils.get(parameters, key, returnType, defaultValue);
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType) {
        return MapValueUtils.getArray(parameters, key, elementType);
    }

    @Override
    public Boolean getBoolean(String key) {
        return MapValueUtils.getBoolean(parameters, key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return MapValueUtils.getBoolean(parameters, key, defaultValue);
    }

    @Override
    public Date getDate(String key) {
        return MapValueUtils.getDate(parameters, key);
    }

    @Override
    public Date getDate(String key, Date defaultValue) {
        return MapValueUtils.getDate(parameters, key, defaultValue);
    }

    @Override
    public Double getDouble(String key) {
        return MapValueUtils.getDouble(parameters, key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return MapValueUtils.getDouble(parameters, key, defaultValue);
    }

    @Override
    public Float getFloat(String key) {
        return MapValueUtils.getFloat(parameters, key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return MapValueUtils.getFloat(parameters, key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return MapValueUtils.getInteger(parameters, key);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return MapValueUtils.getInteger(parameters, key, defaultValue);
    }

    @Override
    public Set<String> getKeys() {
        return parameters.keySet();
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        return MapValueUtils.getList(parameters, key, elementType);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
        return MapValueUtils.getList(parameters, key, elementType, defaultValue);
    }

    @Override
    public List<Object> getList(String key, List<Class<?>> elementTypes, List<Object> defaultValue) {
        return MapValueUtils.getList(parameters, key, elementTypes, defaultValue);
    }

    @Override
    public Long getLong(String key) {
        return MapValueUtils.getLong(parameters, key);
    }

    @Override
    public Duration getDuration(String key) {
        return MapValueUtils.getDuration(parameters, key);
    }

    @Override
    public Duration getDuration(String key, Duration defaultValue) {
        return null;
    }

    @Override
    public LocalDate getLocalDate(String key) {
        return MapValueUtils.getLocalDate(parameters, key);
    }

    @Override
    public LocalDate getLocalDate(String key, LocalDate defaultValue) {
        return MapValueUtils.getLocalDate(parameters, key, defaultValue);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key) {
        return MapValueUtils.getLocalDateTime(parameters, key);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
        return MapValueUtils.getLocalDateTime(parameters, key, defaultValue);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return MapValueUtils.getLong(parameters, key, defaultValue);
    }

    @Override
    public <V> Map<String, V> getMap(String key) {
        return MapValueUtils.getMap(parameters, key);
    }

    @Override
    public <V> Map<String, V> getMap(String key, Map<String, V> defaultValue) {
        return MapValueUtils.getMap(parameters, key, defaultValue);
    }

    @Override
    public Map<String, Object> getMap(String key, List<Class<?>> valueTypes, Map<String, Object> defaultValue) {
        return MapValueUtils.getMap(parameters, key, valueTypes, defaultValue);
    }

    @Override
    public Object getRequired(String key) {
        return MapValueUtils.getRequired(parameters, key);
    }

    @Override
    public <T> T getRequired(String key, Class<T> returnType) {
        return MapValueUtils.getRequired(parameters, key, returnType);
    }

    @Override
    public Boolean getRequiredBoolean(String key) {
        return MapValueUtils.getRequiredBoolean(parameters, key);
    }

    @Override
    public Date getRequiredDate(String key) {
        return MapValueUtils.getRequiredDate(parameters, key);
    }

    @Override
    public Double getRequiredDouble(String key) {
        return MapValueUtils.getRequiredDouble(parameters, key);
    }

    @Override
    public Float getRequiredFloat(String key) {
        return MapValueUtils.getRequiredFloat(parameters, key);
    }

    @Override
    public Integer getRequiredInteger(String key) {
        return MapValueUtils.getRequiredInteger(parameters, key);
    }

    @Override
    public LocalDate getRequiredLocalDate(String key) {
        return MapValueUtils.getRequiredLocalDate(parameters, key);
    }

    @Override
    public LocalDateTime getRequiredLocalDateTime(String key) {
        return MapValueUtils.getRequiredLocalDateTime(parameters, key);
    }

    @Override
    public String getRequiredString(String key) {
        return MapValueUtils.getRequiredString(parameters, key);
    }

    @Override
    public String getString(String key) {
        return MapValueUtils.getString(parameters, key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return MapValueUtils.getString(parameters, key, defaultValue);
    }

    @Override
    public String toString() {
        return "ParametersImpl{" +
            "parameters=" + parameters +
            '}';
    }
}
