/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.runtime.platform.connection.ConnectionContext;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RuntimeTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private static final String CONNECTION_ENV_VARIABLE = "BYTECHEF_CONNECTION_%s_PARAMETERS";

    private final Environment environment;

    public RuntimeTaskDispatcherPreSendProcessor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        WorkflowTask workflowTask = taskExecution.getWorkflowTask();

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        String componentName = workflowNodeType.componentName();

        String name = workflowTask.getName();
        String parameters = environment.getProperty(CONNECTION_ENV_VARIABLE.formatted(workflowTask.getName()));

        if (parameters == null) {
            name = componentName;
            parameters = environment.getProperty(CONNECTION_ENV_VARIABLE.formatted(componentName.toUpperCase()));
        }

        if (parameters != null) {
            long connectionId = ConnectionContext.putConnectionParameters(name, JsonUtils.readMap(parameters));

            Map<String, Long> connectionIdMap = Map.of(workflowTask.getName(), connectionId);

            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        return taskExecution;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        return true;
    }
}
