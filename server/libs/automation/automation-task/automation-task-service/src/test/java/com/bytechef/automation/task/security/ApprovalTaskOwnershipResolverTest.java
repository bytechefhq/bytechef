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

package com.bytechef.automation.task.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.repository.ApprovalTaskRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskOwnershipResolverTest {

    private static final long APPROVAL_TASK_ID = 3L;
    private static final long ASSIGNEE_ID = 17L;
    private static final long JOB_ID = 21L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_ID = 5L;
    private static final long WORKSPACE_ID = 9L;

    private final ApprovalTaskRepository approvalTaskRepository = mock(ApprovalTaskRepository.class);
    private final PrincipalJobService principalJobService = mock(PrincipalJobService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ApprovalTaskOwnershipResolver resolver = new ApprovalTaskOwnershipResolver(
        approvalTaskRepository, principalJobService, projectDeploymentService, projectService);

    @Test
    void testResourceTypeMatchesTheTokenTheApprovalTaskGuardsName() {
        assertThat(resolver.resourceType()).isEqualTo("ApprovalTask");
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceOfTheJobsProjectAndTheAssignee() {
        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId(String.valueOf(JobResumeId.of(JOB_ID)))
            .assigneeId(ASSIGNEE_ID)
            .build();

        stubJobChain(approvalTask);

        assertThat(resolver.resolveOwner(APPROVAL_TASK_ID))
            .isEqualTo(ResourceOwner.of(OptionalLong.of(WORKSPACE_ID), OptionalLong.of(ASSIGNEE_ID)));
    }

    @Test
    void testResolveOwnerReturnsOnlyTheWorkspaceForAnUnassignedTask() {
        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId(String.valueOf(JobResumeId.of(JOB_ID)))
            .build();

        stubJobChain(approvalTask);

        assertThat(resolver.resolveOwner(APPROVAL_TASK_ID)).isEqualTo(ResourceOwner.ofWorkspace(WORKSPACE_ID));
    }

    @Test
    void testResolveOwnerReturnsOnlyTheAssigneeForATaskRaisedForNoJob() {
        ApprovalTask approvalTask = ApprovalTask.builder()
            .assigneeId(ASSIGNEE_ID)
            .build();

        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.of(approvalTask));

        assertThat(resolver.resolveOwner(APPROVAL_TASK_ID)).isEqualTo(ResourceOwner.ofUser(ASSIGNEE_ID));

        verifyNoInteractions(principalJobService, projectDeploymentService, projectService);
    }

    @Test
    void testResolveOwnerFailsClosedForAMalformedJobResumeId() {
        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId("not-a-job-resume-id")
            .build();

        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.of(approvalTask));

        assertThat(resolver.resolveOwner(APPROVAL_TASK_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedForAJobWithNoDeploymentPrincipal() {
        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId(String.valueOf(JobResumeId.of(JOB_ID)))
            .build();

        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.of(approvalTask));
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(APPROVAL_TASK_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedForAnUnknownTask() {
        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(APPROVAL_TASK_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedForANonNumericId() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }

    private void stubJobChain(ApprovalTask approvalTask) {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.of(approvalTask));
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(PROJECT_DEPLOYMENT_ID));
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));
        when(projectDeployment.getProjectId()).thenReturn(PROJECT_ID);
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project));
    }
}
