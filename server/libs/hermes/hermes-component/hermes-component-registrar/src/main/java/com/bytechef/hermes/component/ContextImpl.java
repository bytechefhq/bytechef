
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

import static com.bytechef.hermes.connection.constant.ConnectionConstants.CONNECTION_ID;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.TaskProgressedWorkflowEvent;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.LocalConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;

import java.io.InputStream;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements Context {

    private final LocalConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final TaskExecution taskExecution;

    public ContextImpl(
        LocalConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService,
        EventPublisher eventPublisher,
        FileStorageService fileStorageService,
        TaskExecution taskExecution) {

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
            .map(connectionDefinitionService::toContextConnection);
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
            throw new ComponentExecutionException("Unable to store file " + fileName, exception);
        }
    }

    private static FileEntry getFileEntry(com.bytechef.hermes.file.storage.domain.FileEntry fileEntry) {
        return fileEntry.toContextFileEntry();
    }
}
