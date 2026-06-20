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

package com.bytechef.automation.task.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ApprovalTaskFacadeTest {

    @Mock
    private ApprovalTaskService approvalTaskService;

    @Mock
    private ApprovalTokens approvalTokens;

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Test
    void testCreateApprovalTaskUnwrapsSignedTokenAndPersistsInnerToken() {
        String innerToken = Base64.getEncoder()
            .encodeToString(("public:123:" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
        String signedToken = "v1.9999999999.payload.signature";

        when(approvalTokens.resolveInnerToken(signedToken)).thenReturn(Optional.of(innerToken));
        when(principalJobService.getJobPrincipalId(123L, PlatformType.AUTOMATION)).thenReturn(7L);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        when(projectDeploymentService.getProjectDeployment(7L)).thenReturn(projectDeployment);

        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId(signedToken)
            .build();

        when(approvalTaskService.create(approvalTask)).thenReturn(approvalTask);

        ApprovalTaskFacadeImpl approvalTaskFacade = new ApprovalTaskFacadeImpl(
            approvalTaskService, approvalTokens, principalJobService, projectDeploymentService);

        ApprovalTask result = approvalTaskFacade.createApprovalTask(approvalTask);

        assertThat(result.getJobResumeId()).isEqualTo(innerToken);
        assertThat(result.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testCreateApprovalTaskAcceptsLegacyUnsignedInnerToken() {
        String innerToken = Base64.getEncoder()
            .encodeToString(("public:456:" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));

        when(approvalTokens.resolveInnerToken(innerToken)).thenReturn(Optional.of(innerToken));
        when(principalJobService.getJobPrincipalId(456L, PlatformType.AUTOMATION)).thenReturn(9L);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.STAGING);

        when(projectDeploymentService.getProjectDeployment(9L)).thenReturn(projectDeployment);

        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId(innerToken)
            .build();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        when(approvalTaskService.create(approvalTaskArgumentCaptor.capture())).thenReturn(approvalTask);

        ApprovalTaskFacadeImpl approvalTaskFacade = new ApprovalTaskFacadeImpl(
            approvalTaskService, approvalTokens, principalJobService, projectDeploymentService);

        approvalTaskFacade.createApprovalTask(approvalTask);

        assertThat(approvalTaskArgumentCaptor.getValue()
            .getJobResumeId()).isEqualTo(innerToken);
    }
}
