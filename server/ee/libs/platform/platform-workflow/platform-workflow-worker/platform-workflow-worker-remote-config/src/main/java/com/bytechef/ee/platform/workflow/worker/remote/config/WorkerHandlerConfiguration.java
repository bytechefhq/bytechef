/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.config;

import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.registry.handler.AbstractTaskHandler;
import com.bytechef.platform.definition.WorkflowNodeType;
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
        return type -> {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

            return new ComponentTaskHandler(workflowNodeType, actionDefinitionFacade);
        };
    }

    private static class ComponentTaskHandler extends AbstractTaskHandler {

        public ComponentTaskHandler(WorkflowNodeType workflowNodeType, ActionDefinitionFacade actionDefinitionFacade) {
            super(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName(), actionDefinitionFacade);
        }
    }
}
