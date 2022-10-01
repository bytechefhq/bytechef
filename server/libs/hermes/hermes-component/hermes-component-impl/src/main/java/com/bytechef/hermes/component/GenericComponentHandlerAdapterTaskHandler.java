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
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.component.definition.Action;
import com.bytechef.hermes.component.impl.ContextImpl;
import com.bytechef.hermes.component.impl.ExecutionParametersImpl;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class GenericComponentHandlerAdapterTaskHandler implements TaskHandler<Object> {

    private final Action action;
    private final ConnectionService connectionService;
    private final GenericComponentHandler componentHandler;

    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI2")
    public GenericComponentHandlerAdapterTaskHandler(
            Action action,
            ConnectionService connectionService,
            GenericComponentHandler componentHandler,
            EventPublisher eventPublisher,
            FileStorageService fileStorageService) {
        this.action = action;
        this.connectionService = connectionService;
        this.componentHandler = componentHandler;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
        Context context = new ContextImpl(connectionService, eventPublisher, fileStorageService, taskExecution);

        return componentHandler.handle(action, context, new ExecutionParametersImpl(taskExecution));
    }
}
