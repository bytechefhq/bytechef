/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.oas.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.task.handler.ComponentTaskHandler;

/**
 * @author Ivica Cardic
 */
public class OpenApiComponentTaskHandler extends ComponentTaskHandler {

    private final String actionName;
    private final OpenApiComponentHandler openApiComponentHandler;

    public OpenApiComponentTaskHandler(
        String actionName, ActionDefinitionFacade actionDefinitionFacade,
        OpenApiComponentHandler openApiComponentHandler) {

        super(
            openApiComponentHandler.getName(), openApiComponentHandler.getVersion(), actionName,
            actionDefinitionFacade);

        this.actionName = actionName;
        this.openApiComponentHandler = openApiComponentHandler;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
        try {
            Object result = super.handle(taskExecution);

            if (result instanceof Response response) {
                return openApiComponentHandler.postExecute(actionName, response);
            } else {
                return result;
            }
        } catch (Exception e) {
            throw new TaskExecutionException(e.getMessage(), e);
        }
    }
}
