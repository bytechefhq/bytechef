
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

package com.bytechef.hermes.component.definition.factory;

import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ContextImpl;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextFactory {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final DataStorageService dataStorageService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public ContextFactory(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        DataStorageService dataStorageService, EventPublisher eventPublisher, FileStorageService fileStorageService) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.dataStorageService = dataStorageService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    public ActionContext createActionContext(Map<String, Long> connectionIdMap, Long taskExecutionId) {
        return createContextImpl(connectionIdMap, taskExecutionId);
    }

    public ActionContext createActionContext(Map<String, Long> connectionIdMap) {
        return createContextImpl(connectionIdMap, null);
    }

    public TriggerContext createTriggerContext(Map<String, Long> connectionIdMap) {
        return createContextImpl(connectionIdMap, null);
    }

    private ContextImpl createContextImpl(Map<String, Long> connectionIdMap, Long taskExecutionId) {
        return new ContextImpl(
            connectionIdMap, taskExecutionId, connectionDefinitionService, connectionService, dataStorageService,
            fileStorageService, eventPublisher);
    }
}
