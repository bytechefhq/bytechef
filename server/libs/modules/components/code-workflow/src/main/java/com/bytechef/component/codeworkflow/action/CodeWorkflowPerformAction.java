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

import com.bytechef.component.codeworkflow.task.CodeWorkflowTaskExecutor;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParameterConnection;
import com.bytechef.platform.constant.ModeType;
import java.util.Map;
import java.util.Optional;

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

    public static class CodeWorkflowPerformActionDefinition extends AbstractActionDefinitionWrapper {

        private final CodeWorkflowTaskExecutor codeWorkflowTaskExecutor;

        public CodeWorkflowPerformActionDefinition(ActionDefinition actionDefinition,
            CodeWorkflowTaskExecutor codeWorkflowTaskExecutor) {
            super(actionDefinition);

            this.codeWorkflowTaskExecutor = codeWorkflowTaskExecutor;
        }

        @Override
        public Optional<PerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsPerformFunction) this::perform);
        }

        protected Object perform(
            Parameters inputParameters, Map<String, ? extends ParameterConnection> connectionParameters,
            Parameters extensions, ActionContext actionContext) {

            return codeWorkflowTaskExecutor.executePerform(
                inputParameters.getRequiredString("codeWorkflowContainerReference"),
                inputParameters.getRequiredString("workflowName"), inputParameters.getRequiredString("taskName"),
                inputParameters.getRequired(MetadataConstants.TYPE, ModeType.class));
        }
    }
}
