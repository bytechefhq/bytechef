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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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
 * @author Ivica Cardic
 */
@RestController
@CrossOrigin
@ConditionalOnCoordinator
public class WebhookTriggerController extends AbstractWebhookTriggerController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookTriggerController.class);

    private final WebhookWorkflowExecutor webhookWorkflowExecutor;

    @SuppressFBWarnings("EI")
    public WebhookTriggerController(
        ApplicationProperties applicationProperties, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        TempFileStorage tempFileStorage, TriggerDefinitionService triggerDefinitionService,
        WebhookWorkflowExecutor webhookWorkflowExecutor, WorkflowService workflowService) {

        super(
            jobPrincipalAccessorRegistry, applicationProperties.getPublicUrl(), tempFileStorage,
            triggerDefinitionService, webhookWorkflowExecutor, workflowService);

        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
    }

    /**
     * Executes a workflow based on the provided webhook trigger. Supports HEAD, GET, and POST HTTP methods for
     * triggering different behaviors within the workflow.
     *
     * @param id                  the unique identifier of the workflow execution, extracted from the path variable.
     * @param httpServletRequest  the HTTP request object containing client request details and metadata.
     * @param httpServletResponse the HTTP response object to send responses back to the client.
     * @return a {@link ResponseEntity} object representing the outcome of the workflow execution.
     */
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

            if (Objects.equals(httpServletRequest.getMethod(), RequestMethod.HEAD.name()) ||
                isWorkflowDisabled(workflowExecutionId)) {

                WebhookTriggerFlags webhookTriggerFlags = getWebhookTriggerFlags(workflowExecutionId);

                WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

                if (webhookTriggerFlags.workflowSyncOnEnableValidation()) {
                    responseEntity = doValidateOnEnable(workflowExecutionId, webhookRequest);
                } else {
                    responseEntity = ResponseEntity.ok()
                        .build();
                }
            } else {
                responseEntity = doProcessTrigger(
                    workflowExecutionId, null, httpServletRequest, httpServletResponse);
            }

            return responseEntity;
        });
    }

    /**
     * Handles Server-Sent Events (SSE) streaming for workflow execution based on the webhook trigger. This method
     * configures a REST endpoint to stream events in real time to the client.
     *
     * @param id                 the unique identifier of the workflow execution, extracted from the path variable.
     * @param httpServletRequest the HTTP request object containing client request details and metadata.
     * @return an {@link SseEmitter} object that streams events to the client.
     */
    @RequestMapping(
        method = {
            RequestMethod.GET, RequestMethod.POST
        }, value = "/webhooks/{id}/sse",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseStreamWorkflow(@PathVariable String id, HttpServletRequest httpServletRequest)
        throws Exception {

        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        WebhookTriggerFlags webhookTriggerFlags = getWebhookTriggerFlags(workflowExecutionId);
        WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

        CompletableFuture<Void> future = webhookWorkflowExecutor.executeAsync(
            workflowExecutionId, webhookRequest, new WebhookSseStreamBridge(emitter));

        future.whenComplete((unused, throwable) -> {
            if (throwable != null) {
                sendEvent(emitter, "error", Objects.toString(throwable.getMessage(), "An error occurred"));
            }

            emitter.complete();
        });

        return emitter;
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

    private static void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(name)
                    .data(data instanceof String ? JsonUtils.write(data) : data));
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }
    }

    /**
     * Bridge that broadcasts streamed payloads to SSE clients for webhook workflow execution.
     */
    private static class WebhookSseStreamBridge implements SseStreamBridge {

        private final SseEmitter emitter;

        private WebhookSseStreamBridge(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onEvent(Object payload) {
            if (payload instanceof Map<?, ?> map && map.containsKey("event")) {
                String event = (String) map.get("event");
                Object data = ((Map<String, Object>) payload).entrySet()
                    .stream()
                    .filter(entry -> !"event".equals(entry.getKey()))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(null);

                sendEvent(emitter, event, data);
            } else {
                sendEvent(emitter, "stream", payload);
            }
        }

        @Override
        public void onComplete() {
            try {
                emitter.complete();
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            try {
                sendEvent(emitter, "error", Objects.toString(throwable.getMessage(), "An error occurred"));
            } finally {
                try {
                    emitter.complete();
                } catch (Exception exception) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(exception.getMessage(), exception);
                    }
                }
            }
        }
    }
}
