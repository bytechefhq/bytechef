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

package com.bytechef.automation.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import java.time.Instant;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public interface ProjectWorkflowExecutionFacade {

    WorkflowExecutionDTO getWorkflowExecution(long id);

    /**
     * The execution view of a trigger execution that produced no job - a failed webhook or poll - with no job and the
     * trigger execution filled in.
     */
    WorkflowExecutionDTO getTriggerExecutionWorkflowExecution(long triggerExecutionId);

    TaskExecutionDTO getWorkflowExecutionTaskExecution(long id, long taskExecutionId);

    default Page<WorkflowExecutionDTO> getWorkflowExecutions(
        Boolean embedded, Long environmentId, Status jobStatus, Instant jobStartDate, Instant jobEndDate,
        Long projectId, Long projectDeploymentId, String workflowId, Integer projectVersion, long workspaceId,
        int pageNumber) {

        return getWorkflowExecutions(
            embedded, environmentId, jobStatus, jobStartDate, jobEndDate, projectId, projectDeploymentId, null,
            workflowId, projectVersion, workspaceId, pageNumber);
    }

    /**
     * Returns a page of workflow executions. A non-null {@code connectedUserId} restricts the page to the executions of
     * the connected user's automation workflows, both those copied into the connected user's own project and those
     * referenced from catalog projects.
     */
    Page<WorkflowExecutionDTO> getWorkflowExecutions(
        Boolean embedded, Long environmentId, Status jobStatus, Instant jobStartDate, Instant jobEndDate,
        Long projectId, Long projectDeploymentId, Long connectedUserId, String workflowId, Integer projectVersion,
        long workspaceId, int pageNumber);
}
