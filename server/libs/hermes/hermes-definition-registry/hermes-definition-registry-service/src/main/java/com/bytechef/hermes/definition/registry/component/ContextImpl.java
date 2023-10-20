
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

import com.bytechef.commons.typeconverter.TypeConverter;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.event.TaskProgressedWorkflowEvent;
import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.TriggerContext;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements ActionContext, TriggerContext {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final DataStorageService dataStorageService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final Long taskExecutionId;
    private final Map<String, Long> connectionIdMap;

    @SuppressFBWarnings("EI")
    public ContextImpl(
        ConnectionDefinitionService connectionDefinitionService, Map<String, Long> connectionIdMap,
        ConnectionService connectionService, DataStorageService dataStorageService, EventPublisher eventPublisher,
        FileStorageService fileStorageService, Long taskExecutionId) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionIdMap = connectionIdMap;
        this.connectionService = connectionService;
        this.dataStorageService = dataStorageService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.taskExecutionId = taskExecutionId;
    }

    @Override
    public Optional<Connection> fetchConnection() {
        Collection<Long> connectionIds = connectionIdMap.values();

        Iterator<Long> iterator = connectionIds.iterator();

        if (iterator.hasNext()) {
            return fetchConnection(iterator.next()).map(this::toContextConnection);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> fetchValue(DataStorageScope scope, long scopeId, String key) {
        return dataStorageService.fetchValue(scope, scopeId, key);
    }

    @Override
    public Optional<Connection> fetchConnection(String key) {
        return fetchConnection(connectionIdMap.get(key)).map(this::toContextConnection);
    }

    @Override
    public Connection getConnection() {
        Collection<Long> connectionIds = connectionIdMap.values();

        Iterator<Long> iterator = connectionIds.iterator();

        if (!iterator.hasNext()) {
            throw new IllegalStateException("Connection is not defined");
        }

        return toContextConnection(connectionService.getConnection(iterator.next()));
    }

    @Override
    public Connection getConnection(String key) {
        return toContextConnection(connectionService.getConnection(connectionIdMap.get(key)));
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
    public void saveValue(DataStorageScope scope, long scopeId, String key, Object value) {
        dataStorageService.save(scope, scopeId, key, value);
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

    private Optional<com.bytechef.hermes.connection.domain.Connection> fetchConnection(
        Long connectionId) {

        return connectionId == null
            ? Optional.empty()
            : Optional.of(connectionService.getConnection(connectionId));
    }

    private FileEntry toContextFileEntry(com.bytechef.hermes.file.storage.domain.FileEntry fileEntry) {
        return new ContextFileEntry(fileEntry);
    }

    private Connection toContextConnection(com.bytechef.hermes.connection.domain.Connection connection) {
        return new ContextConnectionImpl(
            connection.getAuthorizationName(), connection.getComponentName(), connectionDefinitionService,
            connection.getConnectionVersion(), connection.getParameters());
    }

    public static class ContextFileEntryConverter implements TypeConverter.Conversion<FileEntry> {

        public Object[] getTypeKeys() {
            return new Object[] {
                FileEntry.class
            };
        }

        @SuppressWarnings("unchecked")
        public FileEntry convert(Object value, Object typeKey) {
            Map<String, ?> source = (Map<String, ?>) value;

            return new ContextFileEntry(
                (String) source.get("extension"),
                (String) source.get("mimeType"),
                (String) source.get("name"),
                (String) source.get("url"));
        }
    }

    private static class ContextFileEntry implements FileEntry {

        private final String extension;
        private final String mimeType;
        private final String name;
        private final String url;

        private ContextFileEntry(String extension, String mimeType, String name, String url) {
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
