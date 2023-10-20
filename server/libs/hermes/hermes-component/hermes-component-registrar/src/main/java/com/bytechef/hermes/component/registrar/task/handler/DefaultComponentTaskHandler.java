
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

package com.bytechef.hermes.component.registrar.task.handler;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParametersImpl;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.ContextImpl;
import com.bytechef.hermes.component.util.ContextSupplier;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentTaskHandler implements TaskHandler<Object> {

    private final ActionDefinition actionDefinition;
    private final ConnectionDefinitionService connectionDefinitionService;
    protected final ComponentHandler componentHandler;
    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI2")
    public DefaultComponentTaskHandler(
        ActionDefinition actionDefinition, ConnectionDefinitionService connectionDefinitionService,
        ComponentHandler componentHandler, ConnectionService connectionService, EventPublisher eventPublisher,
        FileStorageService fileStorageService) {

        this.actionDefinition = actionDefinition;
        this.connectionDefinitionService = connectionDefinitionService;
        this.componentHandler = componentHandler;
        this.connectionService = connectionService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Object handle(TaskExecution taskExecution) {
        Context context = new ContextImpl(
            connectionDefinitionService, connectionService, eventPublisher, fileStorageService, taskExecution);

        return ContextSupplier.get(
            context,
            () -> actionDefinition.getExecute()
                .map(executeFunction -> executeFunction.apply(
                    context, new InputParametersImpl(taskExecution.getParameters())))
                .orElseGet(() -> componentHandler.handle(
                    actionDefinition, context, new InputParametersImpl(taskExecution.getParameters()))));
    }
}
