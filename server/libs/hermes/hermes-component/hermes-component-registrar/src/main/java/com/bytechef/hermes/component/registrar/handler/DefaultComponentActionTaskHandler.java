
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

package com.bytechef.hermes.component.registrar.handler;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.util.ComponentContextSupplier;
import com.bytechef.hermes.constant.MetadataConstants;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentActionTaskHandler implements TaskHandler<Object> {

    protected final ComponentHandler componentHandler;

    private final ActionDefinition actionDefinition;
    private final ContextFactory contextFactory;

    @SuppressFBWarnings("EI2")
    public DefaultComponentActionTaskHandler(
        ActionDefinition actionDefinition, ComponentHandler componentHandler, ContextFactory contextFactory) {

        this.actionDefinition = actionDefinition;
        this.componentHandler = componentHandler;
        this.contextFactory = contextFactory;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
        ActionContext context = contextFactory.createActionContext(
            MapValueUtils.getMap(taskExecution.getMetadata(), MetadataConstants.CONNECTION_IDS), taskExecution.getId());

        return ComponentContextSupplier.get(
            context, componentHandler.getDefinition(),
            () -> {
                try {
                    return actionDefinition.getExecute()
                        .map(executeFunction -> executeFunction.apply(context, taskExecution.getParameters()))
                        .orElseGet(() -> componentHandler.handleAction(
                            actionDefinition, context, taskExecution.getParameters()));
                } catch (Exception e) {
                    throw new TaskExecutionException(e.getMessage(), e);
                }
            });
    }
}
