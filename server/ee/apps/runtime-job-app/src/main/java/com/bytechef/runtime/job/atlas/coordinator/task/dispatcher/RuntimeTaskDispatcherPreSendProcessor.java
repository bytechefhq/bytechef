/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.runtime.job.platform.connection.ConnectionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RuntimeTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final ApplicationArguments applicationArguments;

    public RuntimeTaskDispatcherPreSendProcessor(ApplicationArguments applicationArguments) {
        this.applicationArguments = applicationArguments;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        WorkflowTask workflowTask = taskExecution.getWorkflowTask();

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        String componentName = workflowNodeType.name();

        String name = workflowTask.getName();

        List<String> connections = applicationArguments.getOptionValues("connections");

        if (connections == null) {
            connections = List.of();
        }

        Map<String, Map<String, Object>> connectionParameters = connections.stream()
            .map(value -> JsonUtils.read(value, new TypeReference<Map<String, Map<String, Object>>>() {}))
            .reduce(new HashMap<>(), (a, b) -> {
                a.putAll(b);

                return a;
            });

        Map<String, ?> parameters = connectionParameters.get(workflowTask.getName());

        if (parameters == null) {
            name = componentName;
            parameters = connectionParameters.get(componentName);
        }

        if (parameters != null) {
            long connectionId = ConnectionContext.putConnectionParameters(name, parameters);

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
