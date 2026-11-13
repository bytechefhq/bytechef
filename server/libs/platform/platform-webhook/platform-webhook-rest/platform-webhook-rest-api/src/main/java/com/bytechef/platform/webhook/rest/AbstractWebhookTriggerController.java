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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.rest.util.AsyncWebhookResponseUtils;
import com.bytechef.platform.webhook.rest.util.RedirectValidatorUtils;
import com.bytechef.platform.webhook.rest.util.WebhookRequestUtils;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMapAdapter;
import tools.jackson.core.type.TypeReference;

/**
 * Abstract controller for handling webhook triggers. Redirect URLs from synchronous workflow responses are validated to
 * prevent open redirect vulnerabilities - only relative paths and same-host redirects are allowed. Immediate responses
 * of asynchronous triggers come from the trigger configuration, never from the request, so they may also redirect to
 * external http(s) hosts.
 *
 * @author Ivica Cardic
 */
public abstract class AbstractWebhookTriggerController {

    public static final String HEADER_WORKFLOW_EXECUTION_ID = "X-ByteChef-Workflow-Execution-Id";
    public static final String HEADER_PUBLIC_URL = "X-ByteChef-Public-Url";

    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    private static final Logger log = LoggerFactory.getLogger(AbstractWebhookTriggerController.class);

    private String publicUrl;
    private final TempFileStorage tempFileStorage;
    private final WebhookWorkflowExecutor webhookWorkflowExecutor;

    protected AbstractWebhookTriggerController(
        TempFileStorage tempFileStorage, WebhookWorkflowExecutor webhookWorkflowExecutor) {

        this.tempFileStorage = tempFileStorage;
        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
    }

    protected AbstractWebhookTriggerController(
        String publicUrl, TempFileStorage tempFileStorage, WebhookWorkflowExecutor webhookWorkflowExecutor) {

        this.publicUrl = publicUrl;
        this.tempFileStorage = tempFileStorage;
        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
    }

    protected ResponseEntity<Object> doProcessTrigger(
        WorkflowExecutionId workflowExecutionId, @Nullable WebhookRequest webhookRequest,
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws IOException, ServletException {

        ResponseEntity<Object> responseEntity;
        WebhookTriggerFlags webhookTriggerFlags = webhookWorkflowExecutor.getWebhookTriggerFlags(workflowExecutionId);

        if (webhookRequest == null) {
            webhookRequest = WebhookRequestUtils.getWebhookRequest(
                httpServletRequest, tempFileStorage, webhookTriggerFlags);
        }

        // Add workflowExecutionId and publicUrl as headers for trigger access
        webhookRequest = addPlatformHeaders(webhookRequest, workflowExecutionId);

        if (log.isDebugEnabled()) {
            log.debug(
                "doProcessTrigger: id={}, webhookRequest={}, webhookTriggerFlags={}", workflowExecutionId,
                webhookRequest, webhookTriggerFlags);
        }

        if (webhookTriggerFlags.workflowSyncExecution()) {
            WebhookValidateResponse webhookValidateResponse = webhookTriggerFlags.workflowSyncValidation()
                ? webhookWorkflowExecutor.validate(workflowExecutionId, webhookRequest) : WebhookValidateResponse.ok();

            if (webhookValidateResponse.status() == HttpStatus.OK.value()) {
                responseEntity = executeSync(
                    workflowExecutionId, webhookRequest.asValidated(), httpServletRequest, httpServletResponse);
            } else {
                responseEntity = toResponseEntity(webhookValidateResponse);
            }
        } else if (webhookTriggerFlags.workflowSyncValidation()) {
            responseEntity = validateAndExecuteAsync(
                workflowExecutionId, webhookRequest, webhookTriggerFlags, httpServletRequest, httpServletResponse);
        } else {
            responseEntity = executeAsync(
                workflowExecutionId, webhookRequest, webhookTriggerFlags, null, httpServletRequest,
                httpServletResponse);
        }

        return responseEntity;
    }

    protected WebhookRequest getWebhookRequest(
        HttpServletRequest httpServletRequest, WebhookTriggerFlags webhookTriggerFlags)
        throws IOException, ServletException {

        return WebhookRequestUtils.getWebhookRequest(httpServletRequest, tempFileStorage, webhookTriggerFlags);
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

    /**
     * Starts the asynchronous run and answers the caller. The configured immediate response is computed and checked
     * before the run starts, so a response that cannot be built answers 500 and nothing runs. Without a configured
     * response the caller gets the validation response, or an empty 200 when the trigger does not validate.
     */
    private ResponseEntity<Object> executeAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest,
        WebhookTriggerFlags webhookTriggerFlags, @Nullable WebhookValidateResponse webhookValidateResponse,
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {

        WebhookResponse webhookResponse = null;

        if (webhookTriggerFlags.webhookResponse()) {
            try {
                webhookResponse = webhookWorkflowExecutor.getWebhookResponse(workflowExecutionId);
            } catch (RuntimeException exception) {
                log.error("Failed to build the immediate webhook response for {}", workflowExecutionId, exception);

                return ResponseEntity.internalServerError()
                    .build();
            }

            if (webhookResponse != null && !AsyncWebhookResponseUtils.isAllowed(webhookResponse)) {
                log.error(
                    "Rejected the immediate webhook response for {}: type={}, statusCode={}", workflowExecutionId,
                    webhookResponse.getType(), webhookResponse.getStatusCode());

                return ResponseEntity.internalServerError()
                    .build();
            }
        }

        webhookWorkflowExecutor.executeAsync(workflowExecutionId, webhookRequest);

        if (webhookResponse != null) {
            return renderWebhookResponse(webhookResponse, true, httpServletRequest, httpServletResponse);
        }

        if (webhookValidateResponse != null) {
            return toResponseEntity(webhookValidateResponse);
        }

        return ResponseEntity.ok()
            .build();
    }

    @SuppressFBWarnings(
        value = "UNVALIDATED_REDIRECT",
        justification = "Async redirects are checked by AsyncWebhookResponseUtils.isAllowed() before the run starts; " +
            "sync redirects are validated by RedirectValidatorUtils.isValidRedirect() here")
    private ResponseEntity<Object> renderWebhookResponse(
        WebhookResponse webhookResponse, boolean async, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) throws IOException {

        ResponseEntity<Object> responseEntity;

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
                if (!async || !hasContentTypeHeader(headers)) {
                    bodyBuilder.contentType(MediaType.TEXT_PLAIN);
                } else {
                    bodyBuilder.header(CONTENT_SECURITY_POLICY, "sandbox");
                    bodyBuilder.header(X_CONTENT_TYPE_OPTIONS, "nosniff");
                }

                responseEntity = bodyBuilder.body(String.valueOf(webhookResponse.getBody()));

                break;
            case REDIRECT:
                String redirectUrl = String.valueOf(webhookResponse.getBody());
                String serverHost = httpServletRequest.getServerName();

                if (async || RedirectValidatorUtils.isValidRedirect(redirectUrl, serverHost)) {
                    responseEntity = ResponseEntity.noContent()
                        .build();

                    httpServletResponse.sendRedirect(redirectUrl);
                } else {
                    log.warn("Blocked potentially unsafe redirect URL: {}", redirectUrl);

                    responseEntity = ResponseEntity.badRequest()
                        .body("Invalid redirect URL");
                }

                break;
            default:
                responseEntity = bodyBuilder.build();
        }

        return responseEntity;
    }

    private ResponseEntity<Object> executeSync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest,
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {

        Object outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, webhookRequest);

        if (outputs instanceof Map<?, ?> responseMap && responseMap.containsKey(MetadataConstants.WEBHOOK_RESPONSE)) {
            @SuppressWarnings("unchecked")
            WebhookResponse webhookResponse = MapUtils.getRequired(
                (Map<String, ?>) responseMap, MetadataConstants.WEBHOOK_RESPONSE, new TypeReference<>() {});

            return renderWebhookResponse(webhookResponse, false, httpServletRequest, httpServletResponse);
        }

        return ResponseEntity.ok(outputs);
    }

    private static ResponseEntity<Object> toResponseEntity(WebhookValidateResponse webhookValidateResponse) {
        Map<String, List<String>> headers = webhookValidateResponse.headers();

        return ResponseEntity.status(webhookValidateResponse.status())
            .headers(headers == null ? null : HttpHeaders.readOnlyHttpHeaders(new MultiValueMapAdapter<>(headers)))
            .body(webhookValidateResponse.body());
    }

    private static boolean hasContentTypeHeader(Map<String, String> headers) {
        for (String headerName : headers.keySet()) {
            if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(headerName)) {
                return true;
            }
        }

        return false;
    }

    private ResponseEntity<Object> validateAndExecuteAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest,
        WebhookTriggerFlags webhookTriggerFlags, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) throws IOException {

        WebhookValidateResponse webhookValidateResponse = webhookWorkflowExecutor.validate(
            workflowExecutionId, webhookRequest);

        if (webhookValidateResponse.status() != HttpStatus.OK.value()) {
            return toResponseEntity(webhookValidateResponse);
        }

        return executeAsync(
            workflowExecutionId, webhookRequest.asValidated(), webhookTriggerFlags, webhookValidateResponse,
            httpServletRequest, httpServletResponse);
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

    private WebhookRequest addPlatformHeaders(WebhookRequest webhookRequest, WorkflowExecutionId workflowExecutionId) {
        Map<String, List<String>> headers = new HashMap<>(webhookRequest.headers());

        headers.put(HEADER_WORKFLOW_EXECUTION_ID, List.of(workflowExecutionId.toString()));

        if (publicUrl != null) {
            headers.put(HEADER_PUBLIC_URL, List.of(publicUrl));
        }

        return new WebhookRequest(
            headers, webhookRequest.parameters(), webhookRequest.body(), webhookRequest.method(),
            webhookRequest.validated());
    }
}
