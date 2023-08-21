
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

package com.bytechef.hermes.component.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentActionTaskHandler implements TaskHandler<Object> {

    private final String actionName;
    private final ActionDefinitionService actionDefinitionService;
    private final String componentName;
    private final int componentVersion;

    @SuppressFBWarnings("EI2")
    public DefaultComponentActionTaskHandler(
        String componentName, int componentVersion, String actionName,
        ActionDefinitionService actionDefinitionService) {

        this.actionName = actionName;
        this.actionDefinitionService = actionDefinitionService;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
    }

    @Override
    @SuppressFBWarnings("NP")
    public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
        try {
            return actionDefinitionService.executePerform(
                componentName, componentVersion, actionName, Objects.requireNonNull(taskExecution.getId()),
                taskExecution.getParameters(),
                MapUtils.getMap(taskExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class));
        } catch (Exception e) {
            throw new TaskExecutionException(e.getMessage(), e);
        }
    }
}
