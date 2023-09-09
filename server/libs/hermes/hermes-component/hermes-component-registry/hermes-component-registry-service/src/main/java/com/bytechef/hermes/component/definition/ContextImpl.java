
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

package com.bytechef.hermes.component.definition;

import com.bytechef.data.storage.service.DataStorageService;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.execution.event.TaskProgressedEvent;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.execution.constants.FileEntryConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements ActionDefinition.ActionContext, TriggerDefinition.TriggerContext {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final Map<String, Long> connectionIdMap;
    private final ConnectionService connectionService;
    private final DataStorageService dataStorageService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final Long taskExecutionId;

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
    public Optional<Connection> fetchConnection(String taskConnectionKey) {
        return fetchConnection(connectionIdMap.get(taskConnectionKey)).map(this::toContextConnection);
    }

    @Override
    public <T> Optional<T> fetchValue(String context, int scope, long scopeId, String key) {
        return dataStorageService.fetch(context, scope, scopeId, key);
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
    public Connection getConnection(String workflowConnectionKey) {
        return toContextConnection(connectionService.getConnection(connectionIdMap.get(workflowConnectionKey)));
    }

    @Override
    public <T> T getValue(String context, int scope, long scopeId, String key) {
        return dataStorageService.get(context, scope, scopeId, key);
    }

    @Override
    public InputStream getFileStream(Context.FileEntry fileEntry) {
        return fileStorageService.getFileStream(
            FileEntryConstants.DOCUMENTS_DIR, ((ContextFileEntryImpl) fileEntry).getFileEntry());
    }

    @Override
    public void publishActionProgressEvent(int progress) {
        eventPublisher.publishEvent(new TaskProgressedEvent(taskExecutionId, progress));
    }

    @Override
    public String readFileToString(Context.FileEntry fileEntry) {
        return fileStorageService.readFileToString(
            FileEntryConstants.DOCUMENTS_DIR, ((ContextFileEntryImpl) fileEntry).getFileEntry());
    }

    @Override
    public void setValue(String context, int scope, long scopeId, String key, Object value) {
        dataStorageService.put(context, scope, scopeId, key, value);
    }

    @Override
    public FileEntry storeFileContent(String fileName, String data) {
        return new ContextFileEntryImpl(
            fileStorageService.storeFileContent(FileEntryConstants.DOCUMENTS_DIR, fileName, data));
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        try {
            return new ContextFileEntryImpl(
                fileStorageService.storeFileContent(FileEntryConstants.DOCUMENTS_DIR, fileName, inputStream));
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to store file " + fileName, exception);
        }
    }

    private Optional<com.bytechef.hermes.connection.domain.Connection> fetchConnection(Long connectionId) {
        return connectionId == null
            ? Optional.empty()
            : Optional.of(connectionService.getConnection(connectionId));
    }

    private Connection toContextConnection(com.bytechef.hermes.connection.domain.Connection connection) {
        return new ContextConnectionImpl(connection, connectionDefinitionService);
    }
}
