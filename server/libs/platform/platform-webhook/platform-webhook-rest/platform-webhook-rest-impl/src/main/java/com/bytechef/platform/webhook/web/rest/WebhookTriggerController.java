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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Webhook trigger controller that handles incoming webhook requests from external services.
 *
 * @author Ivica Cardic
 */
@RestController
@CrossOrigin
@ConditionalOnCoordinator
public class WebhookTriggerController extends AbstractWebhookTriggerController {

    private final WebhookWorkflowExecutor webhookWorkflowExecutor;

    @SuppressFBWarnings("EI")
    public WebhookTriggerController(
        ApplicationProperties applicationProperties, TempFileStorage tempFileStorage,
        WebhookWorkflowExecutor webhookWorkflowExecutor) {

        super(applicationProperties.getPublicUrl(), tempFileStorage, webhookWorkflowExecutor);

        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
    }

    /**
     * Executes a workflow based on the provided webhook trigger. Supports HEAD, GET, and POST HTTP methods for
     * triggering different behaviors within the workflow.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for this endpoint. Webhook callbacks from
     * external services cannot include CSRF tokens. Security is maintained through webhook secret validation and
     * signature verification as implemented by individual webhook trigger components.
     *
     * @param id                  the unique identifier of the workflow execution, extracted from the path variable.
     * @param httpServletRequest  the HTTP request object containing client request details and metadata.
     * @param httpServletResponse the HTTP response object to send responses back to the client.
     * @return a {@link ResponseEntity} object representing the outcome of the workflow execution.
     */
    @SuppressFBWarnings("SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING")
    @RequestMapping(
        method = {
            RequestMethod.HEAD, RequestMethod.GET, RequestMethod.POST
        },
        value = "/webhooks/{id}")
    public ResponseEntity<?> executeWorkflow(
        @PathVariable String id, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        return TenantContext.callWithTenantId(workflowExecutionId.getTenantId(), () -> {
            ResponseEntity<?> responseEntity;

            boolean head = Objects.equals(httpServletRequest.getMethod(), RequestMethod.HEAD.name());
            boolean disabled = webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId);

            if (head || disabled) {
                WebhookTriggerFlags webhookTriggerFlags =
                    webhookWorkflowExecutor.getWebhookTriggerFlags(workflowExecutionId);

                WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

                if (webhookTriggerFlags.workflowSyncOnEnableValidation()) {
                    responseEntity = doValidateOnEnable(workflowExecutionId, webhookRequest);
                } else if (disabled && (!head || webhookTriggerFlags.workflowSyncExecution())) {
                    responseEntity = ResponseEntity.status(HttpStatus.GONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("detail", "Workflow is disabled."));
                } else {
                    responseEntity = ResponseEntity.ok()
                        .build();
                }
            } else {
                responseEntity = doProcessTrigger(workflowExecutionId, null, httpServletRequest, httpServletResponse);
            }

            return responseEntity;
        });
    }

    /**
     * Handles Server-Sent Events (SSE) streaming for workflow execution based on the webhook trigger. This method
     * configures a REST endpoint to stream events in real time to the client.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for this endpoint. Webhook callbacks from
     * external services cannot include CSRF tokens. Security is maintained through webhook secret validation and
     * signature verification as implemented by individual webhook trigger components.
     *
     * @param id                 the unique identifier of the workflow execution, extracted from the path variable.
     * @param httpServletRequest the HTTP request object containing client request details and metadata.
     * @return an {@link SseEmitter} object that streams events to the client.
     */
    @SuppressFBWarnings("SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING")
    @RequestMapping(
        method = {
            RequestMethod.GET, RequestMethod.POST
        }, value = "/webhooks/{id}/sse",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseStreamWorkflow(@PathVariable String id, HttpServletRequest httpServletRequest)
        throws Exception {

        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        return TenantContext.callWithTenantId(workflowExecutionId.getTenantId(), () -> {
            SseEmitterStreamBridge bridge = new SseEmitterStreamBridge(emitter);

            WebhookTriggerFlags webhookTriggerFlags = webhookWorkflowExecutor.getWebhookTriggerFlags(
                workflowExecutionId);
            WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

            webhookWorkflowExecutor.stream(workflowExecutionId, webhookRequest, bridge);

            return emitter;
        });
    }

    private ResponseEntity<?> doValidateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        WebhookValidateResponse response = webhookWorkflowExecutor.validateOnEnable(
            workflowExecutionId, webhookRequest);

        return ResponseEntity.status(response.status())
            .headers(
                response.headers() == null
                    ? null
                    : HttpHeaders.readOnlyHttpHeaders(new MultiValueMapAdapter<>(response.headers())))
            .body(response.body());
    }
}
