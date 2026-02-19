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

package com.bytechef.platform.webhook.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.RedirectValidator;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.executor.constant.WebhookConstants;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMapAdapter;
import tools.jackson.core.type.TypeReference;

/**
 * Abstract controller for handling webhook triggers. Redirect URLs from workflow responses are validated to prevent
 * open redirect vulnerabilities - only relative paths and same-host redirects are allowed.
 *
 * @author Ivica Cardic
 */
public abstract class AbstractWebhookTriggerController {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWebhookTriggerController.class);

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private String publicUrl;
    private final TempFileStorage tempFileStorage;
    private final TriggerDefinitionService triggerDefinitionService;
    private WebhookWorkflowExecutor webhookWorkflowExecutor;
    private final WorkflowService workflowService;

    protected AbstractWebhookTriggerController(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TempFileStorage tempFileStorage,
        TriggerDefinitionService triggerDefinitionService, WorkflowService workflowService) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.tempFileStorage = tempFileStorage;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowService = workflowService;
    }

    protected AbstractWebhookTriggerController(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, String publicUrl, TempFileStorage tempFileStorage,
        TriggerDefinitionService triggerDefinitionService, WebhookWorkflowExecutor webhookWorkflowExecutor,
        WorkflowService workflowService) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.publicUrl = publicUrl;
        this.tempFileStorage = tempFileStorage;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
        this.workflowService = workflowService;
    }

    protected ResponseEntity<Object> doProcessTrigger(
        WorkflowExecutionId workflowExecutionId, @Nullable WebhookRequest webhookRequest,
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws IOException, ServletException {

        ResponseEntity<Object> responseEntity;
        WebhookTriggerFlags webhookTriggerFlags = getWebhookTriggerFlags(workflowExecutionId);

        if (webhookRequest == null) {
            webhookRequest = WebhookRequestUtils.getWebhookRequest(
                httpServletRequest, tempFileStorage, webhookTriggerFlags);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                "doProcessTrigger: id={}, webhookRequest={}, webhookTriggerFlags={}", workflowExecutionId,
                webhookRequest, webhookTriggerFlags);
        }

        if (webhookTriggerFlags.workflowSyncExecution()) {
            Object outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, webhookRequest);

            if (outputs instanceof Map<?, ?> responseMap &&
                responseMap.containsKey(WebhookConstants.WEBHOOK_RESPONSE)) {

                responseEntity = processWebhookResponse(httpServletRequest, httpServletResponse, responseMap);
            } else {
                responseEntity = ResponseEntity.ok(outputs);
            }
        } else if (webhookTriggerFlags.workflowSyncValidation()) {
            responseEntity = validateAndExecuteAsync(workflowExecutionId, webhookRequest);
        } else {
            webhookWorkflowExecutor.executeAsync(workflowExecutionId, webhookRequest);

            responseEntity = ResponseEntity.ok()
                .build();
        }

        return responseEntity;
    }

    protected WebhookRequest getWebhookRequest(
        HttpServletRequest httpServletRequest, WebhookTriggerFlags webhookTriggerFlags)
        throws IOException, ServletException {

        return WebhookRequestUtils.getWebhookRequest(httpServletRequest, tempFileStorage, webhookTriggerFlags);
    }

    protected WebhookTriggerFlags getWebhookTriggerFlags(WorkflowExecutionId workflowExecutionId) {
        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId);

        return triggerDefinitionService.getWebhookTriggerFlags(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());
    }

    protected boolean isWorkflowDisabled(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return !jobPrincipalAccessor.isWorkflowEnabled(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private Object checkBody(Object body) {
        if (body instanceof Map) {
            walkThroughMap((Map<?, ?>) body);
        } else if (body instanceof List) {
            walkThroughList((List<?>) body);
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private String convertToFileEntryUrl(Map<?, ?> map) {
        FileEntry fileEntry = new FileEntry((Map<String, ?>) map);

        return publicUrl + "/file-entries/%s/content".formatted(fileEntry.toId());
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(getWorkflowId(workflowExecutionId));

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        String workflowId;

        if (workflowExecutionId.getJobPrincipalId() == -1) {
            workflowId = jobPrincipalAccessor.getLastWorkflowId(workflowExecutionId.getWorkflowUuid());
        } else {
            workflowId = jobPrincipalAccessor.getWorkflowId(
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
        }

        return workflowId;
    }

    @SuppressFBWarnings(
        value = "UNVALIDATED_REDIRECT",
        justification = "Redirect URL is validated by RedirectValidator.isValidRedirect() before use")
    private ResponseEntity<Object> processWebhookResponse(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Map<?, ?> responseMap)
        throws IOException {

        ResponseEntity<Object> responseEntity;

        @SuppressWarnings("unchecked")
        WebhookResponse webhookResponse = MapUtils.getRequired(
            (Map<String, ?>) responseMap, WebhookConstants.WEBHOOK_RESPONSE, new TypeReference<>() {});

        Map<String, String> headers = webhookResponse.getHeaders();

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpServletResponse.addHeader(entry.getKey(), entry.getValue());
        }

        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.status(webhookResponse.getStatusCode());

        switch (webhookResponse.getType()) {
            case BINARY:
                @SuppressWarnings("unchecked")
                FileEntry fileEntry = new FileEntry((Map<String, ?>) webhookResponse.getBody());

                bodyBuilder.contentType(
                    fileEntry.getMimeType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.asMediaType(MimeType.valueOf(fileEntry.getMimeType())));

                responseEntity = bodyBuilder.body(new InputStreamResource(tempFileStorage.getInputStream(fileEntry)));

                break;
            case JSON:
                bodyBuilder.contentType(MediaType.APPLICATION_JSON);

                responseEntity = bodyBuilder.body(checkBody(webhookResponse.getBody()));

                break;
            case RAW:
                bodyBuilder.contentType(MediaType.TEXT_PLAIN);

                responseEntity = bodyBuilder.body(String.valueOf(webhookResponse.getBody()));

                break;
            case REDIRECT:
                String redirectUrl = String.valueOf(webhookResponse.getBody());
                String serverHost = httpServletRequest.getServerName();

                if (RedirectValidator.isValidRedirect(redirectUrl, serverHost)) {
                    responseEntity = ResponseEntity.noContent()
                        .build();

                    httpServletResponse.sendRedirect(redirectUrl);
                } else {
                    logger.warn("Blocked potentially unsafe redirect URL: {}", redirectUrl);

                    responseEntity = ResponseEntity.badRequest()
                        .body("Invalid redirect URL");
                }

                break;
            default:
                responseEntity = bodyBuilder.build();
        }

        return responseEntity;
    }

    private ResponseEntity<Object> validateAndExecuteAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        TriggerDefinition.WebhookValidateResponse response = webhookWorkflowExecutor.validateAndExecuteAsync(
            workflowExecutionId, webhookRequest);

        return ResponseEntity.status(response.status())
            .headers(
                response.headers() == null
                    ? null
                    : HttpHeaders.readOnlyHttpHeaders(new MultiValueMapAdapter<>(response.headers())))
            .body(response.body());
    }

    @SuppressWarnings("unchecked")
    private void walkThroughMap(Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> nestedMap) {
                if (FileEntry.isFileEntryMap(nestedMap)) {
                    String fileEntryUrl = convertToFileEntryUrl(nestedMap);

                    ((Map<Object, Object>) map).put(entry.getKey(), fileEntryUrl);
                }

                walkThroughMap(nestedMap);
            } else if (entry.getValue() instanceof List) {
                walkThroughList((List<?>) entry.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void walkThroughList(List<?> list) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);

            if (item instanceof Map<?, ?> map) {
                if (FileEntry.isFileEntryMap(map)) {
                    String fileEntryUrl = convertToFileEntryUrl(map);

                    ((List<Object>) list).set(i, fileEntryUrl);
                }

                walkThroughMap(map);
            } else if (item instanceof List) {
                walkThroughList((List<?>) item);
            }
        }
    }
}
