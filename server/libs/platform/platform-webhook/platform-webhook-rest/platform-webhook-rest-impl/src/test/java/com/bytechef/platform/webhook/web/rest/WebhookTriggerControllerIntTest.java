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

package com.bytechef.platform.webhook.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Integration test for {@link WebhookTriggerController}'s synchronous-execution path. Exercises the controller
 * end-to-end through a standalone {@code MockMvc} setup wired against a stubbed {@link WebhookWorkflowExecutor}.
 *
 * <p>
 * The module's test harness cannot stand up a real synchronous webhook run: that requires a coordinator, a worker, a
 * message broker and a deployed workflow that produces a {@code WEBHOOK_RESPONSE}-tagged task output. Those moving
 * parts — and the broker-fed {@code JobCompletionAwaiter} that bridges them — are covered separately by
 * {@code JobCompletionAwaiterIntTest} (real broker + SSE listener) and {@code WebhookWorkflowExecutorTest} (the
 * post-hoc last-wins response selection). What this test pins is the unit of behaviour the controller itself added: it
 * returns a {@link CompletableFuture}, so Spring MVC releases the servlet thread (async-servlet) while the job runs,
 * then on completion either renders the tagged {@code WebhookResponse} or falls back to
 * {@code ResponseEntity.ok(outputs)}. The executor's future is stubbed to resolve exactly as the real distributed path
 * resolves it once the job reaches a terminal status.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class WebhookTriggerControllerIntTest {

    @Test
    public void testExecuteWorkflowRendersWebhookResponse() throws Exception {
        WebhookWorkflowExecutor webhookWorkflowExecutor = syncWebhookWorkflowExecutor();

        when(webhookWorkflowExecutor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(
                CompletableFuture.completedFuture(
                    Map.of(MetadataConstants.WEBHOOK_RESPONSE, WebhookResponse.json(Map.of("message", "ok")))));

        MockMvc mockMvc = mockMvc(webhookWorkflowExecutor);

        MvcResult mvcResult = startAsyncExecuteWorkflow(mockMvc);

        mockMvc
            .perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("ok"));
    }

    @Test
    public void testExecuteWorkflowReturnsRawOutputsWhenNoWebhookResponse() throws Exception {
        WebhookWorkflowExecutor webhookWorkflowExecutor = syncWebhookWorkflowExecutor();

        when(webhookWorkflowExecutor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Map.of("foo", "bar")));

        MockMvc mockMvc = mockMvc(webhookWorkflowExecutor);

        MvcResult mvcResult = startAsyncExecuteWorkflow(mockMvc);

        mockMvc
            .perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.foo").value("bar"));
    }

    /**
     * The path variable is an unsigned {@code WorkflowExecutionId} (base64 of a colon-joined string), so a caller
     * holding a webhook URL can edit any field in it — including {@code jobPrincipalId}, which {@code -1} turns into
     * the test-mode sentinel that resolves a workflow by uuid alone, ignoring the deployment's pinned version. The
     * controller must therefore consult {@code isWorkflowDisabled} and answer before dispatching, since an unknown
     * deployment reports "not enabled" (pinned by
     * {@code ProjectDeploymentJobPrincipalAccessorTest#testIsWorkflowEnabledFailsClosedForUnknownJobPrincipal}). Moving
     * execution ahead of that check, or treating a disabled workflow as merely a different response, would make the
     * tampered id reach workflow resolution.
     */
    @Test
    public void testExecuteWorkflowDoesNotExecuteWhenWorkflowDisabled() throws Exception {
        WebhookWorkflowExecutor webhookWorkflowExecutor = mock(WebhookWorkflowExecutor.class);

        when(webhookWorkflowExecutor.isWorkflowDisabled(any(WorkflowExecutionId.class))).thenReturn(true);
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));

        MockMvc mockMvc = mockMvc(webhookWorkflowExecutor);

        String tamperedId = WorkflowExecutionId
            .of(PlatformType.AUTOMATION, -1L, "workflow-uuid", "trigger_1")
            .toString();

        MvcResult mvcResult = mockMvc
            .perform(post("/webhooks/{id}", tamperedId))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc
            .perform(asyncDispatch(mvcResult))
            .andExpect(status().isGone());

        verify(webhookWorkflowExecutor, never()).executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class));
        verify(webhookWorkflowExecutor, never())
            .executeSyncJob(any(WorkflowExecutionId.class), any(WebhookRequest.class));
    }

    private static MvcResult startAsyncExecuteWorkflow(MockMvc mockMvc) throws Exception {
        String id = WorkflowExecutionId
            .of(PlatformType.AUTOMATION, 100L, "workflow-uuid", "trigger_1")
            .toString();

        return mockMvc
            .perform(post("/webhooks/{id}", id))
            .andExpect(request().asyncStarted())
            .andReturn();
    }

    private static MockMvc mockMvc(WebhookWorkflowExecutor webhookWorkflowExecutor) {
        WebhookTriggerController controller = new WebhookTriggerController(
            mock(ApplicationProperties.class), mock(FileEntryTokens.class), mock(TempFileStorage.class),
            webhookWorkflowExecutor);

        return MockMvcBuilders.standaloneSetup(controller)
            .build();
    }

    private static WebhookWorkflowExecutor syncWebhookWorkflowExecutor() {
        WebhookWorkflowExecutor webhookWorkflowExecutor = mock(WebhookWorkflowExecutor.class);

        when(webhookWorkflowExecutor.isWorkflowDisabled(any(WorkflowExecutionId.class))).thenReturn(false);
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));

        return webhookWorkflowExecutor;
    }
}
