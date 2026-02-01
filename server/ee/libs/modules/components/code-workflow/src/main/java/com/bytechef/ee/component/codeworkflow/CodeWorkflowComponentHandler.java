/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.ee.component.codeworkflow.constant.CodeWorkflowConstants.CODE_WORKFLOW;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.codeworkflow.action.CodeWorkflowPerformAction;
import com.bytechef.ee.component.codeworkflow.task.CodeWorkflowTaskExecutor;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component(CODE_WORKFLOW + "_v1_ComponentHandler")
@ConditionalOnEEVersion
public class CodeWorkflowComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public CodeWorkflowComponentHandler(CodeWorkflowTaskExecutor codeWorkflowTaskExecutor) {
        this.componentDefinition = new CodeWorkflowComponentDefinition(
            component(CODE_WORKFLOW)
                .title("Code Workflow")
                .description("Execute code workflow tasks.")
                .icon("path:assets/code-workflow.svg")
                .actions(CodeWorkflowPerformAction.of(codeWorkflowTaskExecutor)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class CodeWorkflowComponentDefinition extends AbstractComponentDefinitionWrapper
        implements ComponentDefinition {

        public CodeWorkflowComponentDefinition(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
