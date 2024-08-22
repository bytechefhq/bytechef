/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.registry.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.AppType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractTaskHandler implements TaskHandler<Object> {

    private final String componentName;
    private final int componentVersion;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final String actionName;

    @SuppressFBWarnings("EI")
    protected AbstractTaskHandler(
        String componentName, int componentVersion, String actionName, ActionDefinitionFacade actionDefinitionFacade) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.actionName = actionName;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
        Map<String, Long> connectIdMap = MapUtils.getMap(
            taskExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

        try {
            return actionDefinitionFacade.executePerform(
                componentName, componentVersion, actionName,
                MapUtils.get(taskExecution.getMetadata(), MetadataConstants.TYPE, AppType.class),
                MapUtils.getLong(taskExecution.getMetadata(), MetadataConstants.INSTANCE_ID),
                MapUtils.getLong(taskExecution.getMetadata(), MetadataConstants.INSTANCE_WORKFLOW_ID),
                Validate.notNull(taskExecution.getJobId(), "jobId"), taskExecution.getParameters(), connectIdMap);
        } catch (Exception e) {
            throw new TaskExecutionException(e.getMessage(), e);
        }
    }
}
