/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.registry.handler;

import com.bytechef.atlas.worker.task.DynamicTaskHandlerFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order
public class ComponentDynamicTaskHandlerFactory implements DynamicTaskHandlerFactory {

    private final ActionDefinitionFacade actionDefinitionFacade;

    @SuppressFBWarnings("EI")
    public ComponentDynamicTaskHandlerFactory(ActionDefinitionFacade actionDefinitionFacade) {
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @Override
    public TaskHandler<?> getTaskHandler(String type) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return new ComponentTaskHandler(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName(), actionDefinitionFacade);
    }
}
