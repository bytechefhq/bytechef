
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

import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public class ContextConnectionImpl implements Context.Connection {

    private final String authorizationName;
    private final String componentName;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final int connectionVersion;
    private final InputParameters inputParameters;
    private final Map<String, Object> parameters;

    @SuppressFBWarnings("EI")
    public ContextConnectionImpl(
        String authorizationName, String componentName, ConnectionDefinitionService connectionDefinitionService,
        int connectionVersion, Map<String, Object> parameters) {

        this.authorizationName = authorizationName;
        this.componentName = componentName;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionVersion = connectionVersion;
        this.parameters = parameters;

        this.inputParameters = new InputParametersImpl(parameters);
    }

    @Override
    public void applyAuthorization(AuthorizationContext authorizationContext) {
        connectionDefinitionService.executeAuthorizationApply(
            componentName, connectionVersion, parameters, authorizationName, authorizationContext);
    }

    @Override
    public Optional<String> fetchBaseUri() {
        return connectionDefinitionService.fetchBaseUri(
            componentName, connectionVersion, parameters);
    }

    @Override
    public String getBaseUri() {
        return fetchBaseUri().orElseThrow();
    }

    @Override
    public boolean containsKey(String key) {
        return inputParameters.containsKey(key);
    }

    @Override
    public Object get(String key) {
        return inputParameters.get(key);
    }

    @Override
    public <T> T get(String key, Class<T> returnType) {
        return inputParameters.get(key, returnType);
    }

    @Override
    public <T> T get(String key, Class<T> returnType, T defaultValue) {
        return inputParameters.get(key, returnType, defaultValue);
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType) {
        return inputParameters.getArray(key, elementType);
    }

    @Override
    public Boolean getBoolean(String key) {
        return inputParameters.getBoolean(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return inputParameters.getBoolean(key, defaultValue);
    }

    @Override
    public Date getDate(String key) {
        return inputParameters.getDate(key);
    }

    @Override
    public Date getDate(String key, Date defaultValue) {
        return inputParameters.getDate(key, defaultValue);
    }

    @Override
    public Double getDouble(String key) {
        return inputParameters.getDouble(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return inputParameters.getDouble(key, defaultValue);
    }

    @Override
    public Duration getDuration(String key) {
        return inputParameters.getDuration(key);
    }

    @Override
    public Duration getDuration(String key, Duration defaultDuration) {
        return inputParameters.getDuration(key, defaultDuration);
    }

    @Override
    public Float getFloat(String key) {
        return inputParameters.getFloat(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return inputParameters.getFloat(key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return inputParameters.getInteger(key);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return inputParameters.getInteger(key, defaultValue);
    }

    @Override
    public Set<String> getKeys() {
        return inputParameters.getKeys();
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        return inputParameters.getList(key, elementType);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
        return inputParameters.getList(key, elementType, defaultValue);
    }

    @Override
    public List<Object> getList(String key, List<Class<?>> elementTypes, List<Object> defaultValue) {
        return inputParameters.getList(key, elementTypes, defaultValue);
    }

    @Override
    public LocalDate getLocalDate(String key) {
        return inputParameters.getLocalDate(key);
    }

    @Override
    public LocalDate getLocalDate(String key, LocalDate defaultValue) {
        return inputParameters.getLocalDate(key, defaultValue);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key) {
        return inputParameters.getLocalDateTime(key);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
        return inputParameters.getLocalDateTime(key, defaultValue);
    }

    @Override
    public Long getLong(String key) {
        return inputParameters.getLong(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return inputParameters.getLong(key, defaultValue);
    }

    @Override
    public <V> Map<String, V> getMap(String key) {
        return inputParameters.getMap(key);
    }

    @Override
    public <V> Map<String, V> getMap(String key, Map<String, V> defaultValue) {
        return inputParameters.getMap(key, defaultValue);
    }

    @Override
    public Map<String, Object> getMap(String key, List<Class<?>> valueTypes, Map<String, Object> defaultValue) {
        return inputParameters.getMap(key, valueTypes, defaultValue);
    }

    @Override
    public <T> T getRequired(String key) {
        return inputParameters.getRequired(key);
    }

    @Override
    public <T> T getRequired(String key, Class<T> returnType) {
        return inputParameters.getRequired(key, returnType);
    }

    @Override
    public Boolean getRequiredBoolean(String key) {
        return inputParameters.getRequiredBoolean(key);
    }

    @Override
    public Date getRequiredDate(String key) {
        return inputParameters.getRequiredDate(key);
    }

    @Override
    public Double getRequiredDouble(String key) {
        return inputParameters.getRequiredDouble(key);
    }

    @Override
    public Float getRequiredFloat(String key) {
        return inputParameters.getRequiredFloat(key);
    }

    @Override
    public Integer getRequiredInteger(String key) {
        return inputParameters.getRequiredInteger(key);
    }

    @Override
    public LocalDate getRequiredLocalDate(String key) {
        return inputParameters.getRequiredLocalDate(key);
    }

    @Override
    public LocalDateTime getRequiredLocalDateTime(String key) {
        return inputParameters.getRequiredLocalDateTime(key);
    }

    @Override
    public String getRequiredString(String key) {
        return inputParameters.getRequiredString(key);
    }

    @Override
    public String getString(String key) {
        return inputParameters.getString(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return inputParameters.getString(key, defaultValue);
    }

    @Override
    public String toString() {
        return "ContextConnection{" +
            "authorizationName='" + authorizationName + '\'' +
            ", componentName='" + componentName + '\'' +
            ", connectionDefinitionService=" + connectionDefinitionService +
            ", connectionVersion=" + connectionVersion +
            ", parameters=" + parameters +
            '}';
    }
}
