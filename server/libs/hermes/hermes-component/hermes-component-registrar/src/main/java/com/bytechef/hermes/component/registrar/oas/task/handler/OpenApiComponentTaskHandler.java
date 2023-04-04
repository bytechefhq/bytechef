
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

package com.bytechef.hermes.component.registrar.oas.task.handler;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.ContextImpl;
import com.bytechef.hermes.component.registrar.oas.OpenApiClient;
import com.bytechef.hermes.component.util.ContextSupplier;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class OpenApiComponentTaskHandler implements TaskHandler<Object> {

    private static final OpenApiClient OPEN_API_CLIENT = new OpenApiClient();

    private final ActionDefinition actionDefinition;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final OpenApiComponentHandler openApiComponentHandler;

    @SuppressFBWarnings("EI2")
    public OpenApiComponentTaskHandler(
        ActionDefinition actionDefinition, ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService, EventPublisher eventPublisher, FileStorageService fileStorageService,
        OpenApiComponentHandler openApiComponentHandler) {

        this.actionDefinition = actionDefinition;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.openApiComponentHandler = openApiComponentHandler;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
        Context context = new ContextImpl(
            connectionDefinitionService, connectionService, eventPublisher, fileStorageService, taskExecution);

        return ContextSupplier.get(
            context,
            () -> {
                try {
                    return openApiComponentHandler.postExecute(
                        actionDefinition, OPEN_API_CLIENT.execute(actionDefinition, context, taskExecution));
                } catch (Exception e) {
                    throw new TaskExecutionException(e.getMessage(), e);
                }
            });
    }
}
