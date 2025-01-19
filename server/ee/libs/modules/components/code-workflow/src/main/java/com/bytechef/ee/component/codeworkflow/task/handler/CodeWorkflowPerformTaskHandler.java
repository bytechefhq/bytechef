/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task.handler;

import static com.bytechef.ee.component.codeworkflow.constant.CodeWorkflowConstants.CODE_WORKFLOW;
import static com.bytechef.ee.component.codeworkflow.constant.CodeWorkflowConstants.PERFORM;

import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.workflow.worker.task.handler.AbstractTaskHandler;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component(CODE_WORKFLOW + "/v1/" + PERFORM)
public class CodeWorkflowPerformTaskHandler extends AbstractTaskHandler {

    public CodeWorkflowPerformTaskHandler(ActionDefinitionFacade actionDefinitionFacade) {
        super(CODE_WORKFLOW, 1, PERFORM, actionDefinitionFacade);
    }
}
