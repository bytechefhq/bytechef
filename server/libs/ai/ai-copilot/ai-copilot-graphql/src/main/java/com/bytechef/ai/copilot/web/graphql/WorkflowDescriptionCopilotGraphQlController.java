/*
 * Copyright 2025 ByteChef
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

package com.bytechef.ai.copilot.web.graphql;

import com.bytechef.ai.copilot.CopilotGenerationException;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.facade.WorkflowDescriptionCopilotFacade;
import com.bytechef.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class WorkflowDescriptionCopilotGraphQlController {

    private final WorkflowDescriptionCopilotFacade workflowDescriptionCopilotFacade;

    public WorkflowDescriptionCopilotGraphQlController(
        WorkflowDescriptionCopilotFacade workflowDescriptionCopilotFacade) {

        this.workflowDescriptionCopilotFacade = workflowDescriptionCopilotFacade;
    }

    @MutationMapping
    public GenerateWorkflowDescriptionPayload generateWorkflowDescription(
        @Argument GenerateWorkflowDescriptionInput input) {

        WorkflowDescriptionCopilotResult result;

        try {
            result = workflowDescriptionCopilotFacade.generateWorkflowDescription(
                new WorkflowDescriptionCopilotRequest(
                    input.workflowId(), input.workflowNodeName(), input.environmentId()));
        } catch (ChatModelUnavailableException | CopilotGenerationException exception) {
            throw new GraphQlBadRequestException(exception.getMessage(), exception);
        }

        return new GenerateWorkflowDescriptionPayload(result.value());
    }

    @SuppressFBWarnings("EI")
    public record GenerateWorkflowDescriptionInput(String workflowId, String workflowNodeName, long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record GenerateWorkflowDescriptionPayload(String value) {
    }
}
