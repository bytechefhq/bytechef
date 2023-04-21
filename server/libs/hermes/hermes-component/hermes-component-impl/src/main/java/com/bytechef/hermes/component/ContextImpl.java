
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

import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.event.TaskProgressedWorkflowEvent;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.converter.Converter;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bytechef.hermes.component.definition.ConnectionDefinition.CONNECTION_ID;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements ActionContext, TriggerContext {

    static {
        MapValueUtils.addConverter(new ContextFileEntryConverter());
    }

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final Map<String, Object> parameters;
    private final Long taskExecutionId;

    @SuppressFBWarnings("EI")
    public ContextImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        EventPublisher eventPublisher, FileStorageService fileStorageService, Map<String, Object> parameters,
        Long taskExecutionId) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.parameters = parameters;
        this.taskExecutionId = taskExecutionId;
    }

    @Override
    public Optional<Connection> fetchConnection() {
        return Optional.ofNullable(MapValueUtils.getLong(parameters, CONNECTION_ID))
            .map(connectionService::getConnection)
            .map(this::toContextConnection);
    }

    @Override
    public Connection getConnection() {
        return toContextConnection(
            connectionService.getConnection(MapValueUtils.getLong(parameters, CONNECTION_ID)));
    }

    @Override
    public InputStream getFileStream(FileEntry fileEntry) {
        return fileStorageService.getFileStream(new com.bytechef.hermes.file.storage.domain.FileEntry(
            fileEntry.getName(), fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getUrl()));
    }

    @Override
    public void publishActionProgressEvent(int progress) {
        eventPublisher.publishEvent(new TaskProgressedWorkflowEvent(taskExecutionId, progress));
    }

    @Override
    public String readFileToString(FileEntry fileEntry) {
        return fileStorageService.readFileToString(new com.bytechef.hermes.file.storage.domain.FileEntry(
            fileEntry.getName(), fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getUrl()));
    }

    @Override
    public FileEntry storeFileContent(String fileName, String data) {
        return toContextFileEntry(fileStorageService.storeFileContent(fileName, data));
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        try {
            return toContextFileEntry(fileStorageService.storeFileContent(fileName, inputStream));
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to store file " + fileName, exception);
        }
    }

    private static FileEntry toContextFileEntry(com.bytechef.hermes.file.storage.domain.FileEntry fileEntry) {
        return new ContextFileEntry(fileEntry);
    }

    private Context.Connection toContextConnection(com.bytechef.hermes.connection.domain.Connection connection) {
        return new ContextConnection(connection, connectionDefinitionService);
    }

    private static class ContextFileEntryConverter implements Converter<Map<?, ?>, FileEntry> {

        @Override
        public Context.FileEntry convert(Map<?, ?> source) {
            return new ContextFileEntry(
                (String) source.get("extension"),
                (String) source.get("mimeType"),
                (String) source.get("name"),
                (String) source.get("url"));
        }
    }

    private static class ContextConnection implements Connection {

        private final com.bytechef.hermes.connection.domain.Connection connection;
        private final ConnectionDefinitionService connectionDefinitionService;
        private final InputParameters inputParameters;

        public ContextConnection(
            com.bytechef.hermes.connection.domain.Connection connection,
            ConnectionDefinitionService connectionDefinitionService) {

            this.connection = connection;
            this.connectionDefinitionService = connectionDefinitionService;
            this.inputParameters = new InputParametersImpl(connection.getParameters());
        }

        @Override
        public void applyAuthorization(AuthorizationContext authorizationContext) {
            connectionDefinitionService.executeAuthorizationApply(
                connection.getComponentName(), connection.getConnectionVersion(), connection.getParameters(),
                connection.getAuthorizationName(), authorizationContext);
        }

        @Override
        public Optional<String> fetchBaseUri() {
            return connectionDefinitionService.fetchBaseUri(
                connection.getComponentName(), connection.getConnectionVersion(), connection.getParameters());
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
        public Map<String, Object> getMap() {
            return inputParameters.getMap();
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
                ", connection=" + connection +
                '}';
        }
    }

    public static class ContextFileEntry implements FileEntry {

        private final String extension;
        private final String mimeType;
        private final String name;
        private final String url;

        public ContextFileEntry(String extension, String mimeType, String name, String url) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.name = name;
            this.url = url;
        }

        public ContextFileEntry(com.bytechef.hermes.file.storage.domain.FileEntry fileEntry) {
            this(fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getName(), fileEntry.getUrl());
        }

        @Override
        public String getExtension() {
            return extension;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "ContextFileEntry{" +
                "extension='" + extension + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
        }
    }
}
