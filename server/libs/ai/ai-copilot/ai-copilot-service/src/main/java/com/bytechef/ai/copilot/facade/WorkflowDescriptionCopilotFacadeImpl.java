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

package com.bytechef.ai.copilot.facade;

import com.bytechef.ai.copilot.workflow.WorkflowDescriptionCopilotGenerator;
import com.bytechef.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
class WorkflowDescriptionCopilotFacadeImpl implements WorkflowDescriptionCopilotFacade {

    private final WorkflowDescriptionCopilotGenerator workflowDescriptionCopilotGenerator;

    WorkflowDescriptionCopilotFacadeImpl(
        Optional<WorkflowDescriptionCopilotGenerator> workflowDescriptionCopilotGenerator) {
        this.workflowDescriptionCopilotGenerator = workflowDescriptionCopilotGenerator.orElse(null);
    }

    @Override
    @PreAuthorize("hasPermission(#request.workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public WorkflowDescriptionCopilotResult generateWorkflowDescription(WorkflowDescriptionCopilotRequest request) {
        if (workflowDescriptionCopilotGenerator == null) {
            throw new IllegalStateException("Workflow Description Copilot is not enabled");
        }

        return workflowDescriptionCopilotGenerator.generate(request);
    }
}
