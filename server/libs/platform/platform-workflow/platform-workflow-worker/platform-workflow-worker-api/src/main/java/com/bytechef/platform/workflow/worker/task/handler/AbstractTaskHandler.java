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

package com.bytechef.platform.workflow.worker.task.handler;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

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

        Map<String, ?> continueParameters = extractContinueParameters(taskExecution);
        Instant suspendExpiresAt = extractSuspendExpiresAt(taskExecution);

        try {
            WorkflowTask workflowTask = taskExecution.getWorkflowTask();

            return actionDefinitionFacade.executePerform(
                componentName, componentVersion, actionName,
                MapUtils.getLong(taskExecution.getMetadata(), MetadataConstants.JOB_PRINCIPAL_ID),
                MapUtils.getLong(taskExecution.getMetadata(), MetadataConstants.JOB_PRINCIPAL_WORKFLOW_ID),
                Validate.notNull(taskExecution.getJobId(), "jobId"),
                MapUtils.getString(taskExecution.getMetadata(), MetadataConstants.WORKFLOW_ID),
                taskExecution.getParameters(), connectIdMap, workflowTask.getExtensions(),
                MapUtils.getLong(taskExecution.getMetadata(), MetadataConstants.ENVIRONMENT_ID),
                MapUtils.get(taskExecution.getMetadata(), MetadataConstants.TYPE, PlatformType.class),
                MapUtils.getBoolean(taskExecution.getMetadata(), MetadataConstants.EDITOR_ENVIRONMENT, false),
                continueParameters, suspendExpiresAt);
        } catch (Exception e) {
            throw new TaskExecutionException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ?> extractContinueParameters(TaskExecution taskExecution) {
        Map<String, ?> suspendData = MapUtils.getMap(
            taskExecution.getMetadata(), MetadataConstants.SUSPEND, Map.of());

        if (suspendData.isEmpty()) {
            return null;
        }

        Object continueParametersValue = suspendData.get("continueParameters");

        if (continueParametersValue instanceof Map) {
            return (Map<String, ?>) continueParametersValue;
        }

        return null;
    }

    private static @Nullable Instant extractSuspendExpiresAt(TaskExecution taskExecution) {
        Map<String, ?> suspendData = MapUtils.getMap(
            taskExecution.getMetadata(), MetadataConstants.SUSPEND, Map.of());

        if (suspendData.isEmpty()) {
            return null;
        }

        Object expiresAtValue = suspendData.get("expiresAt");

        if (expiresAtValue instanceof Number expiresAtNumber) {
            return Instant.ofEpochMilli(expiresAtNumber.longValue());
        }

        return null;
    }
}
