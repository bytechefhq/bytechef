/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.component.trigger.WebhookRequest.WebhookBodyImpl;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.webhook.executor.WorkflowExecutor;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * @author Ivica Cardic
 */
public abstract class AbstractWebhookTriggerController {

    private final FilesFileStorage filesFileStorage;
    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowExecutor workflowExecutor;
    private final WorkflowService workflowService;

    private static final Logger logger = LoggerFactory.getLogger(AbstractWebhookTriggerController.class);

    protected AbstractWebhookTriggerController(
        FilesFileStorage filesFileStorage, InstanceAccessorRegistry instanceAccessorRegistry,
        TriggerDefinitionService triggerDefinitionService, WorkflowExecutor workflowExecutor,
        WorkflowService workflowService) {

        this.filesFileStorage = filesFileStorage;
        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowExecutor = workflowExecutor;
        this.workflowService = workflowService;
    }

    protected ResponseEntity<Object> doProcessTrigger(
        WorkflowExecutionId workflowExecutionId, HttpServletRequest httpServletRequest)
        throws IOException, ServletException {

        ResponseEntity<Object> responseEntity;
        WebhookTriggerFlags webhookTriggerFlags = getWebhookTriggerFlags(workflowExecutionId);

        WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "webhooks: id={}, webhookRequest={}, webhookTriggerFlags={}", workflowExecutionId, webhookRequest,
                webhookTriggerFlags);
        }

        if (webhookTriggerFlags.workflowSyncExecution()) {
            responseEntity = ResponseEntity.ok(workflowExecutor.executeSync(workflowExecutionId, webhookRequest));
        } else if (webhookTriggerFlags.workflowSyncValidation()) {
            responseEntity = doValidateAndExecuteAsync(workflowExecutionId, webhookRequest);
        } else {
            workflowExecutor.execute(workflowExecutionId, webhookRequest);

            responseEntity = ResponseEntity.ok()
                .build();
        }

        return responseEntity;
    }

    protected WebhookRequest getWebhookRequest(
        HttpServletRequest httpServletRequest, WebhookTriggerFlags webhookTriggerFlags)
        throws IOException, ServletException {

        WebhookBodyImpl body = null;
        String contentType = httpServletRequest.getContentType();
        Map<String, List<String>> headers = getHeaderMap(httpServletRequest);
        Map<String, List<String>> parameters = toMap(httpServletRequest.getParameterMap());

        if (contentType != null) {
            BodyAndParameters bodyAndParameters = getBodyAndParameters(
                httpServletRequest, contentType, parameters, webhookTriggerFlags);

            body = bodyAndParameters.body;
            parameters = bodyAndParameters.parameters;
        }

        return new WebhookRequest(
            headers, parameters, body, WebhookMethod.valueOf(httpServletRequest.getMethod()));
    }

    protected WebhookTriggerFlags getWebhookTriggerFlags(WorkflowExecutionId workflowExecutionId) {
        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId);

        return triggerDefinitionService.getWebhookTriggerFlags(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName());
    }

    protected boolean isWorkflowEnabled(WorkflowExecutionId workflowExecutionId) {
        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(workflowExecutionId.getType());

        return instanceAccessor.isWorkflowEnabled(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowReferenceCode());
    }

    private Object convertString(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(str);
        }

        try {
            return LocalDateTime.parse(str);
        } catch (DateTimeParseException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        try {
            return LocalDate.parse(str);
        } catch (DateTimeParseException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        return str;
    }

    private ResponseEntity<Object> doValidateAndExecuteAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        TriggerDefinition.WebhookValidateResponse response = workflowExecutor.validateAndExecuteAsync(
            workflowExecutionId, webhookRequest);

        return ResponseEntity.status(response.status())
            .headers(
                response.headers() == null
                    ? null
                    : HttpHeaders.readOnlyHttpHeaders(new MultiValueMapAdapter<>(response.headers())))
            .body(response.body());
    }

    private BodyAndParameters getBodyAndParameters(
        HttpServletRequest httpServletRequest, String contentType, Map<String, List<String>> parameters,
        WebhookTriggerFlags webhookTriggerFlags) throws IOException, ServletException {

        WebhookBodyImpl body;

        if (contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            MultiValueMap<String, Object> multipartFormDataMap = new LinkedMultiValueMap<>();

            for (Part part : httpServletRequest.getParts()) {
                List<Object> value = multipartFormDataMap.getOrDefault(part.getName(), new ArrayList<>());

                if (part.getContentType() == null) {
                    try (InputStream inputStream = part.getInputStream()) {
                        value.add(StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8));
                    }
                } else {
                    value.add(filesFileStorage.storeFileContent(part.getSubmittedFileName(), part.getInputStream()));
                }

                multipartFormDataMap.put(part.getName(), value);
            }

            body = new WebhookBodyImpl(
                multipartFormDataMap, ContentType.FORM_DATA, httpServletRequest.getContentType(), null);

            UriComponents uriComponents = getUriComponents(httpServletRequest);

            parameters = toMap(uriComponents.getQueryParams());
        } else if (contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();

            UriComponents uriComponents = getUriComponents(httpServletRequest);

            MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();

            for (String queryParam : queryParams.keySet()) {
                parameterMap.remove(queryParam);
            }

            body = new WebhookBodyImpl(
                parseFormUrlencodedParams(parameterMap), ContentType.FORM_URL_ENCODED,
                httpServletRequest.getContentType(), null);
            parameters = toMap(queryParams);
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
            }
        } else if (contentType.startsWith("application/")) {
            body = new WebhookBodyImpl(
                filesFileStorage.storeFileContent(
                    getFilename(httpServletRequest.getContentType()), httpServletRequest.getInputStream()),
                ContentType.BINARY, httpServletRequest.getContentType(), null);
        } else {
            try (InputStream inputStream = httpServletRequest.getInputStream()) {
                String rawContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);

                body = new WebhookBodyImpl(rawContent, ContentType.RAW, httpServletRequest.getContentType(), null);
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

            headerMap.put(headerName, toList(httpServletRequest.getHeaders(headerName)));
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
        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(workflowExecutionId.getType());

        return instanceAccessor.getWorkflowId(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowReferenceCode());
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
                        .map(string -> (string == null || string.isBlank()) ? null : convertString(string))
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

    private static List<String> toList(Enumeration<String> enumeration) {
        List<String> list = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }

        return list;
    }

    private static Map<String, List<String>> toMap(Map<String, String[]> map) {
        return map.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> Arrays.asList(entry.getValue())));
    }

    private static Map<String, List<String>> toMap(MultiValueMap<String, String> multiValueMap) {
        return new HashMap<>(multiValueMap);
    }

    private record BodyAndParameters(WebhookBodyImpl body, Map<String, List<String>> parameters) {
    }
}
