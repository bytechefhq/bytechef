
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

package com.bytechef.hermes.definition.registry.component.factory;

import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.TriggerContext;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.definition.registry.component.ContextImpl;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ContextFactoryImpl implements ContextFactory {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final DataStorageService dataStorageService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public ContextFactoryImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        DataStorageService dataStorageService, EventPublisher eventPublisher, FileStorageService fileStorageService) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.dataStorageService = dataStorageService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public ActionContext createActionContext(Map<String, Long> connectionIdMap, Long taskExecutionId) {
        return createContext(connectionIdMap, taskExecutionId);
    }

    @Override
    public TriggerContext createTriggerContext(Map<String, Long> connectionIdMap) {
        return createContext(connectionIdMap, null);
    }

    private ContextImpl createContext(Map<String, Long> connectionIdMap, Long taskExecutionId) {
        return new ContextImpl(
            connectionDefinitionService, connectionIdMap, connectionService, dataStorageService, eventPublisher,
            fileStorageService, taskExecutionId);
    }
}
