/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.ee.component.codeworkflow.constant.CodeWorkflowConstants.PERFORM;

import com.bytechef.ee.component.codeworkflow.action.definition.CodeWorkflowPerformActionDefinition;
import com.bytechef.ee.component.codeworkflow.task.CodeWorkflowTaskExecutor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowPerformAction {

    public final CodeWorkflowPerformActionDefinition actionDefinition;

    public CodeWorkflowPerformAction(CodeWorkflowTaskExecutor codeWorkflowTaskExecutor) {
        actionDefinition = new CodeWorkflowPerformActionDefinition(
            action(PERFORM)
                .title("Perform Code Workflow Task")
                .description("Perform code workflow task.")
                .properties(
                    string("workflowName")
                        .label("Workflow Name"),
                    integer("workflowVersion")
                        .label("Workflow Version"),
                    string("taskName")
                        .label("Task Name")),
            codeWorkflowTaskExecutor);
    }
}
