
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

import static com.bytechef.hermes.connection.ConnectionConstants.CONNECTION_ID;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.TaskProgressedWorkflowEvent;
import com.bytechef.commons.utils.CollectionUtils;
import com.bytechef.commons.utils.MapValueUtils;
import com.bytechef.hermes.component.Connection;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements Context {

    static {
        MapValueUtils.addConverter(new Converter<Map<?, ?>, FileEntry>() {

            @Override
            public FileEntry convert(Map<?, ?> source) {
                return new FileEntryImpl(
                    (String) source.get("extension"),
                    (String) source.get("mimeType"),
                    (String) source.get("name"),
                    (String) source.get("url"));
            }
        });
    }

    private final ConnectionDefinition connectionDefinition;
    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final TaskExecution taskExecution;

    public ContextImpl(
        ConnectionDefinition connectionDefinition,
        ConnectionService connectionService,
        EventPublisher eventPublisher,
        FileStorageService fileStorageService,
        TaskExecution taskExecution) {

        this.connectionDefinition = connectionDefinition;
        this.connectionService = connectionService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.taskExecution = taskExecution;
    }

    @Override
    public Optional<Connection> fetchConnectionParameters() {
        Optional<Connection> connectionParametersOptional = Optional.empty();

        if (CollectionUtils.containsKey(taskExecution.getParameters(), CONNECTION_ID)) {
            connectionParametersOptional = Optional.of(getConnectionParameters());
        }

        return connectionParametersOptional;
    }

    @Override
    public ConnectionDefinition getConnectionDefinition() {
        return connectionDefinition;
    }

    @Override
    public Connection getConnectionParameters() {
        return new ConnectionImpl(connectionService.getConnection(
            MapValueUtils.getRequired(taskExecution.getParameters(), CONNECTION_ID)));
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
        return getFileEntry(fileStorageService.storeFileContent(fileName, data));
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        try {
            return getFileEntry(fileStorageService.storeFileContent(fileName, inputStream));
        } catch (Exception exception) {
            throw new ActionExecutionException("Unable to store file " + fileName, exception);
        }
    }

    private static FileEntry getFileEntry(com.bytechef.hermes.file.storage.domain.FileEntry fileEntry) {
        return new FileEntryImpl(fileEntry);
    }
}
