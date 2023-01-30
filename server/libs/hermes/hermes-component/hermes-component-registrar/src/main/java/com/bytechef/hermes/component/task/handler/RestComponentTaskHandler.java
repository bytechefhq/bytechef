
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

package com.bytechef.hermes.component.task.handler;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.RestComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.impl.ContextImpl;
import com.bytechef.hermes.component.rest.RestClient;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class RestComponentTaskHandler implements TaskHandler<Object> {

    private static final RestClient REST_CLIENT = new RestClient();

    private final ActionDefinition actionDefinition;
    private final ConnectionDefinition connectionDefinition;
    private final ConnectionService connectionService;
    private final RestComponentHandler restComponentHandler;

    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI2")
    public RestComponentTaskHandler(
        ActionDefinition actionDefinition, ConnectionDefinition connectionDefinition,
        ConnectionService connectionService, RestComponentHandler restComponentHandler, EventPublisher eventPublisher,
        FileStorageService fileStorageService) {

        this.actionDefinition = actionDefinition;
        this.connectionDefinition = connectionDefinition;
        this.connectionService = connectionService;
        this.restComponentHandler = restComponentHandler;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
        Context context = new ContextImpl(
            connectionDefinition, connectionService, eventPublisher, fileStorageService, taskExecution);

        try {
            return restComponentHandler.postExecute(
                actionDefinition, REST_CLIENT.execute(actionDefinition, context, taskExecution));
        } catch (Exception e) {
            throw new TaskExecutionException(e.getMessage(), e);
        }
    }
}
