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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowContinuationHelperTest {

    private static final String TENANT_ID = "tenant_a";

    @Test
    void testContinuationJobReadsAndCreatesInTheWebhooksTenant() {
        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
        PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);
        List<String> tenantIdsSeen = new CopyOnWriteArrayList<>();
        String webhookId = TenantContext.callWithTenantId(
            TENANT_ID, () -> WorkflowExecutionId.of(PlatformType.AUTOMATION, 7L, "wf-uuid", "trigger_1")
                .toString());

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(7L, "wf-uuid")).thenAnswer(invocation -> {
            tenantIdsSeen.add(TenantContext.getCurrentTenantId());

            return "wf-1";
        });
        when(jobPrincipalAccessor.getInputMap(7L, "wf-uuid")).thenAnswer(invocation -> {
            tenantIdsSeen.add(TenantContext.getCurrentTenantId());

            return Map.of();
        });
        when(principalJobFacade.createJob(any(), eq(7L), eq(PlatformType.AUTOMATION))).thenAnswer(invocation -> {
            tenantIdsSeen.add(TenantContext.getCurrentTenantId());

            return 1L;
        });

        new WorkflowContinuationHelper(jobPrincipalAccessorRegistry, principalJobFacade)
            .createContinuationJob(webhookId, Map.of("endReason", "client_closed"));

        assertThat(tenantIdsSeen).containsExactly(TENANT_ID, TENANT_ID, TENANT_ID);
    }
}
