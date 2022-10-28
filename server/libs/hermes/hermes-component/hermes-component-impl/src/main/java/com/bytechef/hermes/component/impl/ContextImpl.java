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
import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements Context {
    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final TaskExecution taskExecution;

    public ContextImpl(
            ConnectionService connectionService,
            EventPublisher eventPublisher,
            FileStorageService fileStorageService,
            TaskExecution taskExecution) {
        this.connectionService = connectionService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.taskExecution = taskExecution;
    }

    @Override
    public Optional<ConnectionParameters> fetchConnection() {
        if (taskExecution.containsKey(CONNECTION_ID)) {
            return connectionService
                    .fetchConnection(taskExecution.getString(CONNECTION_ID))
                    .map(ConnectionParametersImpl::new);
        }

        return Optional.empty();
    }

    @Override
    public ConnectionParameters getConnection() {
        return new ConnectionParametersImpl(
                connectionService.getConnection(taskExecution.getRequiredString(CONNECTION_ID)));
    }

    @Override
    public InputStream getFileStream(FileEntry fileEntry) {
        return fileStorageService.getFileStream(
                com.bytechef.hermes.file.storage.domain.FileEntry.of(fileEntry.toMap()));
    }

    @Override
    public void publishProgressEvent(int progress) {
        eventPublisher.publishEvent(new TaskProgressedWorkflowEvent(taskExecution.getId(), progress));
    }

    @Override
    public String readFileToString(FileEntry fileEntry) {
        return fileStorageService.readFileToString(
                com.bytechef.hermes.file.storage.domain.FileEntry.of(fileEntry.toMap()));
    }

    @Override
    public FileEntry storeFileContent(String fileName, String data) {
        return new FileEntryImpl(fileStorageService.storeFileContent(fileName, data));
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        try {
            return new FileEntryImpl(fileStorageService.storeFileContent(fileName, inputStream));
        } catch (Exception exception) {
            throw new ActionExecutionException("Unable to store file " + fileName, exception);
        }
    }
}
