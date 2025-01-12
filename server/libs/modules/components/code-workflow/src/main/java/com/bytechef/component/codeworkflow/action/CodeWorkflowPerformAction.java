/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.codeworkflow.action;

import static com.bytechef.component.codeworkflow.constant.CodeWorkflowConstants.PERFORM;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.codeworkflow.action.definition.CodeWorkflowPerformActionDefinition;
import com.bytechef.component.codeworkflow.task.CodeWorkflowTaskExecutor;

/**
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
