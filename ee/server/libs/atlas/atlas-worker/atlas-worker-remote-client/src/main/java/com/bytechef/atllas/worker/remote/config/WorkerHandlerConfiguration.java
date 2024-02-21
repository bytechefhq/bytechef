/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atllas.worker.remote.config;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.constant.MetadataConstants;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class WorkerHandlerConfiguration {

    @Bean
    TaskHandlerRegistry taskHandlerRegistry(ActionDefinitionFacade actionDefinitionFacade) {
        return type -> (TaskHandler<?>) taskExecution -> {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

            Map<String, Long> connectIdMap = MapUtils.getMap(
                taskExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

            return actionDefinitionFacade.executePerform(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName(),
                MapUtils.getInteger(taskExecution.getMetadata(), MetadataConstants.TYPE),
                MapUtils.getLong(taskExecution.getMetadata(), MetadataConstants.INSTANCE_ID),
                MapUtils.getString(taskExecution.getMetadata(), MetadataConstants.WORKFLOW_ID),
                Validate.notNull(taskExecution.getJobId(), "jobId"), taskExecution.getParameters(),
                OptionalUtils.orElse(CollectionUtils.findFirst(connectIdMap.values()), null));
        };
    }
}
