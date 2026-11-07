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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class BrowserVoiceSessionTokenServiceTest {

    private static final String TENANT_ID = "tenant_a";

    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
    private final List<String> tenantIdsSeen = new CopyOnWriteArrayList<>();
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final String webhookId = TenantContext.callWithTenantId(
        TENANT_ID, () -> WorkflowExecutionId.of(PlatformType.AUTOMATION, 7L, "wf-uuid", "trigger_1")
            .toString());

    private BrowserVoiceSessionTokenService tokenService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        ObjectProvider<ContextFactory> contextFactoryProvider = mock(ObjectProvider.class);

        when(contextFactoryProvider.getIfAvailable()).thenReturn(mock(ContextFactory.class));

        tokenService = new BrowserVoiceSessionTokenService(
            jobPrincipalAccessorRegistry, new VoiceMetricsRecorder(mock(ObjectProvider.class)), workflowService,
            contextFactoryProvider);

        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(7L, "wf-uuid")).thenAnswer(invocation -> {
            tenantIdsSeen.add(TenantContext.getCurrentTenantId());

            return "wf-1";
        });
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
    }

    @Test
    void testIssueMintsATokenForAnEnabledVoiceTrigger() {
        when(jobPrincipalAccessor.isWorkflowEnabled(7L, "wf-uuid")).thenReturn(true);

        BrowserVoiceSessionTokenService.Token token = tokenService.issue(webhookId);

        assertThat(token.token()).isNotBlank();
        assertThat(tokenService.consume(token.token(), webhookId)).isTrue();
    }

    @Test
    void testIssueRefusesADisabledWorkflow() {
        when(jobPrincipalAccessor.isWorkflowEnabled(7L, "wf-uuid")).thenReturn(false);

        assertThatThrownBy(() -> tokenService.issue(webhookId))
            .isInstanceOf(BrowserVoiceSessionTokenService.InvalidWebhookException.class)
            .hasMessageContaining("disabled");
    }

    @Test
    void testIssueResolvesTheWorkflowInTheWebhooksTenant() {
        when(jobPrincipalAccessor.isWorkflowEnabled(7L, "wf-uuid")).thenAnswer(invocation -> {
            tenantIdsSeen.add(TenantContext.getCurrentTenantId());

            return true;
        });

        tokenService.issue(webhookId);

        assertThat(tenantIdsSeen).containsExactly(TENANT_ID, TENANT_ID);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testIssueRefusesWhereVoiceSessionsCannotRun() {
        ObjectProvider<ContextFactory> missingContextFactory = mock(ObjectProvider.class);

        when(missingContextFactory.getIfAvailable()).thenReturn(null);
        when(jobPrincipalAccessor.isWorkflowEnabled(7L, "wf-uuid")).thenReturn(true);

        BrowserVoiceSessionTokenService nodeWithoutVoice = new BrowserVoiceSessionTokenService(
            jobPrincipalAccessorRegistry, new VoiceMetricsRecorder(mock(ObjectProvider.class)), workflowService,
            missingContextFactory);

        assertThatThrownBy(() -> nodeWithoutVoice.issue(webhookId))
            .isInstanceOf(BrowserVoiceSessionTokenService.VoiceUnavailableException.class)
            .hasMessageContaining("not available on this node");
    }

    @Test
    void testIssueRefusesAnUnparseableWebhookId() {
        assertThatThrownBy(() -> tokenService.issue("not-a-webhook-id"))
            .isInstanceOf(BrowserVoiceSessionTokenService.InvalidWebhookException.class);
    }
}
