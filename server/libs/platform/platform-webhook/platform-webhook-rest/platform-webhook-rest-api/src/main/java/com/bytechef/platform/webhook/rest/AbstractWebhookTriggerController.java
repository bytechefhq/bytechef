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
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.component.trigger.WebhookRequest.WebhookBodyImpl;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.executor.constant.WebhookConstants;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ForwardedHeaderUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;

/**
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
            webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);
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

                responseEntity = processWebhookResponse(httpServletResponse, responseMap);
            } else {
                responseEntity = ResponseEntity.ok(outputs);
            }
        } else if (webhookTriggerFlags.workflowSyncValidation()) {
            responseEntity = validateAndExecuteAsync(workflowExecutionId, webhookRequest);
        } else {
            webhookWorkflowExecutor.execute(workflowExecutionId, webhookRequest);

            responseEntity = ResponseEntity.ok()
                .build();
        }

        return responseEntity;
    }

    public WebhookRequest getWebhookRequest(
        HttpServletRequest httpServletRequest, WebhookTriggerFlags webhookTriggerFlags)
        throws IOException, ServletException {

        WebhookBodyImpl body = null;
        String contentType = httpServletRequest.getContentType();
        Map<String, List<String>> headers = getHeaderMap(httpServletRequest);
        Map<String, List<String>> parameters;

        if (contentType == null) {
            parameters = MapUtils.toMap(httpServletRequest.getParameterMap());
        } else {
            BodyAndParameters bodyAndParameters = getBodyAndParameters(
                httpServletRequest, contentType, webhookTriggerFlags);

            body = bodyAndParameters.body;
            parameters = bodyAndParameters.parameters;
        }

        return new WebhookRequest(headers, parameters, body, WebhookMethod.valueOf(httpServletRequest.getMethod()));
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

    private BodyAndParameters getBodyAndParameters(
        HttpServletRequest httpServletRequest, String contentType, WebhookTriggerFlags webhookTriggerFlags)
        throws IOException, ServletException {

        WebhookBodyImpl body;
        Map<String, List<String>> parameters;

        if (contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            MultiValueMap<String, Object> multipartFormDataMap = new LinkedMultiValueMap<>();

            for (Part part : httpServletRequest.getParts()) {
                List<Object> value = multipartFormDataMap.getOrDefault(part.getName(), new ArrayList<>());

                if (part.getContentType() == null) {
                    try (InputStream inputStream = part.getInputStream()) {
                        value.add(StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8));
                    }
                } else {
                    value.add(tempFileStorage.storeFileContent(part.getSubmittedFileName(), part.getInputStream()));
                }

                multipartFormDataMap.put(part.getName(), value);
            }

            body = new WebhookBodyImpl(
                multipartFormDataMap, ContentType.FORM_DATA, httpServletRequest.getContentType(), null);
            parameters = MapUtils.toMap(httpServletRequest.getParameterMap());
        } else if (contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            Map<String, String[]> parameterMap = new HashMap<>(httpServletRequest.getParameterMap());

            UriComponents uriComponents = getUriComponents(httpServletRequest);

            MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();

            for (String queryParam : queryParams.keySet()) {
                parameterMap.remove(queryParam);
            }

            body = new WebhookBodyImpl(
                parseFormUrlencodedParams(parameterMap), ContentType.FORM_URL_ENCODED,
                httpServletRequest.getContentType(), null);
            parameters = new HashMap<>(queryParams);
        } else if (contentType.startsWith(MimeTypeUtils.MIME_APPLICATION_JSON)) {
            try (InputStream inputStream = httpServletRequest.getInputStream()) {
                Object content;
                String rawContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);

                if (webhookTriggerFlags.webhookRawBody()) {
                    content = rawContent;
                } else {
                    content = JsonUtils.read(rawContent);
                }

                body = new WebhookBodyImpl(content, ContentType.JSON, httpServletRequest.getContentType(), rawContent);
                parameters = MapUtils.toMap(httpServletRequest.getParameterMap());
            }
        } else if (contentType.startsWith(MimeTypeUtils.MIME_APPLICATION_XML)) {
            try (InputStream inputStream = httpServletRequest.getInputStream()) {
                Object content;
                String rawContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);

                if (webhookTriggerFlags.webhookRawBody()) {
                    content = rawContent;
                } else {
                    content = XmlUtils.read(rawContent);
                }

                body = new WebhookBodyImpl(content, ContentType.XML, httpServletRequest.getContentType(), rawContent);
                parameters = MapUtils.toMap(httpServletRequest.getParameterMap());
            }
        } else if (contentType.startsWith("application/")) {
            body = new WebhookBodyImpl(
                tempFileStorage.storeFileContent(
                    getFilename(httpServletRequest.getContentType()), httpServletRequest.getInputStream()),
                ContentType.BINARY, httpServletRequest.getContentType(), null);
            parameters = MapUtils.toMap(httpServletRequest.getParameterMap());
        } else {
            try (InputStream inputStream = httpServletRequest.getInputStream()) {
                String rawContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);

                body = new WebhookBodyImpl(rawContent, ContentType.RAW, httpServletRequest.getContentType(), null);
                parameters = MapUtils.toMap(httpServletRequest.getParameterMap());
            }
        }

        return new BodyAndParameters(body, parameters);
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(getWorkflowId(workflowExecutionId));

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private String getFilename(String mimeTypeString) {
        MimeType mimeType = org.springframework.util.MimeTypeUtils.parseMimeType(mimeTypeString);

        String subtype = mimeType.getSubtype();

        return "file." + subtype.toLowerCase();
    }

    private Map<String, List<String>> getHeaderMap(HttpServletRequest httpServletRequest) {
        Map<String, List<String>> headerMap = new HashMap<>();
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            headerMap.put(headerName, CollectionUtils.toList(httpServletRequest.getHeaders(headerName)));
        }

        return headerMap;
    }

    private UriComponents getUriComponents(HttpServletRequest httpServletRequest) {
        ServletServerHttpRequest servletServerHttpRequest = new ServletServerHttpRequest(httpServletRequest);

        UriComponentsBuilder uriComponentsBuilder = ForwardedHeaderUtils.adaptFromForwardedHeaders(
            servletServerHttpRequest.getURI(), servletServerHttpRequest.getHeaders());

        return uriComponentsBuilder.build();
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

    @SuppressWarnings("unchecked")
    private Map<String, ?> parseFormUrlencodedParams(Map<String, String[]> parameterMap) {
        Map<String, Object> multiMap = new HashMap<>();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            // Split the key on [
            String[] keys = key.split("\\[");

            Map<String, Object> currentMap = multiMap;

            for (int i = 0; i < keys.length; i++) {
                String currentKey = keys[i];

                // Remove any trailing ]
                if (currentKey.endsWith("]")) {
                    currentKey = currentKey.substring(0, currentKey.length() - 1);
                }

                if (i == keys.length - 1) {
                    // If we're at the last key, add the value

                    List<Object> convertedValues = Arrays.stream(values)
                        .map(string -> (string == null || string.isBlank()) ? null : ConvertUtils.convertString(string))
                        .toList();

                    currentMap.put(
                        currentKey,
                        convertedValues.isEmpty()
                            ? null
                            : convertedValues.size() == 1 ? convertedValues.getFirst() : convertedValues);
                } else {
                    // Otherwise, add a new map if one doesn't already exist
                    currentMap.putIfAbsent(currentKey, new HashMap<String, Object>());

                    currentMap = (Map<String, Object>) currentMap.get(currentKey);
                }
            }
        }

        return multiMap;
    }

    private ResponseEntity<Object> processWebhookResponse(
        HttpServletResponse httpServletResponse, Map<?, ?> responseMap) throws IOException {

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

                bodyBuilder.contentType(MediaType.asMediaType(MimeType.valueOf(fileEntry.getMimeType())));

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
                responseEntity = ResponseEntity.noContent()
                    .build();

                httpServletResponse.sendRedirect(String.valueOf(webhookResponse.getBody()));

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

    private record BodyAndParameters(WebhookBodyImpl body, Map<String, List<String>> parameters) {
    }
}
