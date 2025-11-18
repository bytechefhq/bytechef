/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.task.handler;

import com.bytechef.atlas.worker.task.handler.DynamicTaskHandlerProvider;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.definition.WorkflowNodeType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order
public class ComponentDynamicTaskHandlerProvider implements DynamicTaskHandlerProvider {

    private final ActionDefinitionFacade actionDefinitionFacade;

    public ComponentDynamicTaskHandlerProvider(ActionDefinitionFacade actionDefinitionFacade) {
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @Override
    public TaskHandler<?> getTaskHandler(String type) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return new ComponentTaskHandler(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), actionDefinitionFacade);
    }
}
