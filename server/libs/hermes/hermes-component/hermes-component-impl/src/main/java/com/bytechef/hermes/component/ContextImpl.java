
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

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.event.EventPublisher;
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
import java.util.Map;
import java.util.Optional;

import static com.bytechef.hermes.component.definition.ConnectionDefinition.CONNECTION_ID;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements Context {

    static {
        MapValueUtils.addConverter(new FileEntryConverter());
    }

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final TaskExecution taskExecution;

    @SuppressFBWarnings("EI")
    public ContextImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        EventPublisher eventPublisher, FileStorageService fileStorageService, TaskExecution taskExecution) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.taskExecution = taskExecution;
    }

    @Override
    public Optional<Connection> fetchConnection() {
        return Optional.ofNullable(MapValueUtils.getLong(taskExecution.getParameters(), CONNECTION_ID))
            .map(connectionService::getConnection)
            .map(this::toContextConnection);
    }

    @Override
    public InputStream getFileStream(FileEntry fileEntry) {
        return fileStorageService.getFileStream(new com.bytechef.hermes.file.storage.domain.FileEntry(
            fileEntry.getName(), fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getUrl()));
    }

    @Override
    public void publishProgressEvent(int progress) {
        eventPublisher.publishEvent(new TaskProgressedWorkflowEvent(taskExecution.getId(), progress));
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

    private static class FileEntryConverter implements Converter<Map<?, ?>, FileEntry> {

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
        private final InputParameters parameters;

        public ContextConnection(
            com.bytechef.hermes.connection.domain.Connection connection,
            ConnectionDefinitionService connectionDefinitionService) {

            this.connection = connection;
            this.connectionDefinitionService = connectionDefinitionService;
            this.parameters = new InputParametersImpl(connection.getParameters());
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
        public InputParameters getParameters() {
            return parameters;
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
