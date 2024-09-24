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

package com.bytechef.component.codeworkflow;

import static com.bytechef.component.codeworkflow.constant.CodeWorkflowConstants.CODE_WORKFLOW;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.codeworkflow.action.CodeWorkflowPerformAction;
import com.bytechef.component.codeworkflow.task.CodeWorkflowTaskExecutor;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(CODE_WORKFLOW + "_v1_ComponentHandler")
public class CodeWorkflowComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public CodeWorkflowComponentHandler(CodeWorkflowTaskExecutor codeWorkflowTaskExecutor) {
        this.componentDefinition = new CodeWorkflowComponentDefinition(
            component(CODE_WORKFLOW)
                .title("Code Workflow")
                .description("Execute code workflow tasks.")
                .actions(new CodeWorkflowPerformAction(codeWorkflowTaskExecutor).actionDefinition));
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
