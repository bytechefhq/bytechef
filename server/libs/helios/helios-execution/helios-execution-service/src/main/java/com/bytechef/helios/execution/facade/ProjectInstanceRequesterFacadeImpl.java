
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.execution.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.hermes.configuration.connection.WorkflowConnection;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class ProjectInstanceRequesterFacadeImpl implements ProjectInstanceRequesterFacade {

    private final ConnectionService connectionService;
    private final JobFacade jobFacade;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final TriggerLifecycleFacade triggerLifecycleFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceRequesterFacadeImpl(
        ConnectionService connectionService, JobFacade jobFacade,
        ProjectInstanceService projectInstanceService, ProjectInstanceWorkflowService projectInstanceWorkflowService,
        TriggerLifecycleFacade triggerLifecycleFacade, WorkflowService workflowService) {

        this.connectionService = connectionService;
        this.jobFacade = jobFacade;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
        this.workflowService = workflowService;
    }

    // Propagation.NEVER is set because of sending job messages via queue in monolith mode, where it can happen
    // the case where a job is finished and completion task executed, but the transaction is not yet committed and
    // the job id is missing.
    @Override
    @Transactional(propagation = Propagation.NEVER)
    @SuppressFBWarnings("NP")
    public long createJob(long projectInstanceId, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            projectInstanceId, workflowId);

        long jobId = jobFacade.createJob(new JobParameters(workflowId, projectInstanceWorkflow.getInputs()));

        projectInstanceWorkflowService.addJob(Objects.requireNonNull(projectInstanceWorkflow.getId()), jobId);

        return jobId;
    }

    @Override
    @Transactional
    public void enableProjectInstance(long projectInstanceId, boolean enabled) {
        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(projectInstanceId);

        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            if (!projectInstanceWorkflow.isEnabled()) {
                continue;
            }

            if (enabled) {
                enableWorkflowTrigger(projectInstanceWorkflow);
            } else {
                disableWorkflowTrigger(projectInstanceWorkflow);
            }
        }

        projectInstanceService.updateEnabled(projectInstanceId, enabled);
    }

    @Override
    @Transactional
    public void enableProjectInstanceWorkflow(long projectInstanceId, String workflowId, boolean enabled) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            projectInstanceId, workflowId);

        if (enabled) {
            enableWorkflowTrigger(projectInstanceWorkflow);
        } else {
            disableWorkflowTrigger(projectInstanceWorkflow);
        }

        projectInstanceWorkflowService.updateEnabled(projectInstanceWorkflow.getId(), enabled);
    }

    private void disableWorkflowTrigger(ProjectInstanceWorkflow projectInstanceWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            triggerLifecycleFacade.executeTriggerDisable(
                workflowTrigger,
                WorkflowExecutionId.of(
                    workflow.getId(), projectInstanceWorkflow.getProjectInstanceId(), ProjectConstants.PROJECT,
                    workflowTrigger),
                getConnection(workflowTrigger));
        }
    }

    private void enableWorkflowTrigger(ProjectInstanceWorkflow projectInstanceWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            triggerLifecycleFacade.executeTriggerEnable(
                workflowTrigger,
                WorkflowExecutionId.of(
                    workflow.getId(), projectInstanceWorkflow.getProjectInstanceId(), ProjectConstants.PROJECT,
                    workflowTrigger),
                getConnection(workflowTrigger));
        }
    }

    private Connection getConnection(WorkflowConnection workflowConnection) {
        return workflowConnection.getConnectionId()
            .map(connectionService::getConnection)
            .orElseGet(() -> getConnection(workflowConnection.getKey(), workflowConnection.getOperationName()));
    }

    private Connection getConnection(WorkflowTrigger workflowTrigger) {
        return WorkflowConnection.of(workflowTrigger)
            .values()
            .stream()
            .findFirst()
            .map(this::getConnection)
            .orElse(null);
    }

    private Connection getConnection(String workflowConnectionKey, String taskName) {
        ProjectInstanceWorkflowConnection projectInstanceWorkflowConnection =
            projectInstanceWorkflowService.getProjectInstanceWorkflowConnection(workflowConnectionKey, taskName);

        return connectionService.getConnection(projectInstanceWorkflowConnection.getConnectionId());
    }
}
