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

package com.bytechef.platform.webhook.web.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowContinuationHelperTest {

    private static final String TENANT_ID = "tenant1";

    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
    private final PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);
    private final WorkflowContinuationHelper workflowContinuationHelper = new WorkflowContinuationHelper(
        jobPrincipalAccessorRegistry, principalJobFacade);

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testCreateContinuationJobLooksUpTheWorkflowInTheTenantOfTheId() {
        WorkflowExecutionId workflowExecutionId = TenantContext.callWithTenantId(
            TENANT_ID, () -> WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger_1"));

        List<String> tenantIds = new ArrayList<>();

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return jobPrincipalAccessor;
            });
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return "workflow-id";
            });
        when(jobPrincipalAccessor.getInputMap(1L, "workflow-uuid"))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return Map.of();
            });
        when(principalJobFacade.createJob(any(), anyLong(), eq(PlatformType.AUTOMATION)))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return 2L;
            });

        workflowContinuationHelper.createContinuationJob(workflowExecutionId.toString(), Map.of("callSid", "call-1"));

        assertThat(tenantIds).containsExactly(TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }
}
