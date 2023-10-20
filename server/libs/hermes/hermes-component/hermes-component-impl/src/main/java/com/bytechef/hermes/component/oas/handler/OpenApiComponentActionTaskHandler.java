
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

package com.bytechef.hermes.component.oas.handler;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.oas.OpenApiClient;
import com.bytechef.hermes.component.util.ComponentContextSupplier;
import com.bytechef.hermes.constant.MetadataConstants;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class OpenApiComponentActionTaskHandler implements TaskHandler<Object> {

    private static final OpenApiClient OPEN_API_CLIENT = new OpenApiClient();

    private final ActionDefinition actionDefinition;
    private final ContextFactory contextFactory;
    private final OpenApiComponentHandler openApiComponentHandler;

    @SuppressFBWarnings("EI2")
    public OpenApiComponentActionTaskHandler(
        ActionDefinition actionDefinition,
        ContextFactory contextFactory, OpenApiComponentHandler openApiComponentHandler) {

        this.actionDefinition = actionDefinition;
        this.contextFactory = contextFactory;
        this.openApiComponentHandler = openApiComponentHandler;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
        ActionContext context = contextFactory.createActionContext(
            MapValueUtils.getMap(taskExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class),
            taskExecution.getId());

        return ComponentContextSupplier.get(
            context, openApiComponentHandler.getDefinition(),
            () -> {
                try {
                    return openApiComponentHandler.postExecute(
                        actionDefinition, OPEN_API_CLIENT.execute(actionDefinition, taskExecution));
                } catch (Exception e) {
                    throw new TaskExecutionException(e.getMessage(), e);
                }
            });
    }
}
