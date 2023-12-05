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

package com.bytechef.hermes.webhook.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.StreamUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.component.registry.dto.WebhookTriggerFlags;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest.WebhookBodyImpl;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.constants.FileEntryConstants;
import com.bytechef.hermes.webhook.executor.WebhookExecutor;
import com.bytechef.hermes.webhook.web.rest.exception.WorkflowNotEnabledException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final FileStorageService fileStorageService;
    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final ObjectMapper objectMapper;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WebhookExecutor webhookExecutor;
    private final WorkflowService workflowService;
    private final XmlMapper xmlMapper;

    @SuppressFBWarnings("EI")
    public WebhookController(
        FileStorageService fileStorageService, InstanceAccessorRegistry instanceAccessorRegistry,
        ObjectMapper objectMapper,
        TriggerDefinitionService triggerDefinitionService, WebhookExecutor webhookExecutor,
        WorkflowService workflowService, XmlMapper xmlMapper) {

        this.fileStorageService = fileStorageService;
        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.objectMapper = objectMapper;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookExecutor = webhookExecutor;
        this.workflowService = workflowService;
        this.xmlMapper = xmlMapper;
    }

    @RequestMapping(
        method = {
            RequestMethod.GET, RequestMethod.POST
        },
        value = "/webhooks/{id}")
    public ResponseEntity<?> webhooks(@PathVariable String id, HttpServletRequest httpServletRequest)
        throws Exception {

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(
            workflowExecutionId.getType());

        if (!instanceAccessor.isWorkflowEnabled(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId())) {

            throw new WorkflowNotEnabledException(
                "Workflow id=%s is not enabled".formatted(workflowExecutionId.getWorkflowId()));
        }

        WebhookBodyImpl body = null;
        String mediaType = httpServletRequest.getContentType();
        Map<String, String[]> headers = getHeaderMap(httpServletRequest);
        Map<String, String[]> parameters = httpServletRequest.getParameterMap();
        ResponseEntity<?> responseEntity;

        OperationType operationType = getComponentOperation(workflowExecutionId);

        WebhookTriggerFlags webhookTriggerFlags = triggerDefinitionService.getWebhookTriggerFlags(
            operationType.componentName(), operationType.componentVersion(),
            operationType.componentOperationName());

        if (mediaType != null) {
            if (mediaType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                MultiValueMap<String, Object> multipartFormDataMap = new LinkedMultiValueMap<>();

                for (Part part : httpServletRequest.getParts()) {
                    List<Object> value = multipartFormDataMap.getOrDefault(part.getName(), new ArrayList<>());

                    if (part.getContentType() == null) {
                        value.add(StreamUtils.copyToString(part.getInputStream(), StandardCharsets.UTF_8));
                    } else {
                        value.add(
                            fileStorageService.storeFileContent(
                                FileEntryConstants.FILES_DIR, part.getSubmittedFileName(), part.getInputStream()));
                    }

                    multipartFormDataMap.put(part.getName(), value);
                }

                body = new WebhookBodyImpl(
                    multipartFormDataMap, ContentType.FORM_DATA, httpServletRequest.getContentType());

                UriComponents uriComponents = getUriComponents(httpServletRequest);

                parameters = toMap(uriComponents.getQueryParams());
            } else if (mediaType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
                Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();

                UriComponents uriComponents = getUriComponents(httpServletRequest);

                MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();

                for (String queryParam : queryParams.keySet()) {
                    parameterMap.remove(queryParam);
                }

                body = new WebhookBodyImpl(
                    parameterMap, ContentType.FORM_URL_ENCODED, httpServletRequest.getContentType());
                parameters = toMap(queryParams);
            } else if (mediaType.startsWith(MimeTypeUtils.MIME_APPLICATION_JSON)) {
                Object content;

                if (webhookTriggerFlags.webhookRawBody()) {
                    content = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);
                } else {
                    content = JsonUtils.read(
                        StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8),
                        objectMapper);
                }

                body = new WebhookBodyImpl(content, ContentType.JSON, httpServletRequest.getContentType());
            } else if (mediaType.startsWith(MimeTypeUtils.MIME_APPLICATION_XML)) {
                Object content;

                if (webhookTriggerFlags.webhookRawBody()) {
                    content = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);
                } else {
                    content = XmlUtils.read(
                        StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8),
                        xmlMapper);
                }

                body = new WebhookBodyImpl(content, ContentType.XML, httpServletRequest.getContentType());
            } else if (mediaType.startsWith("application/")) {
                body = new WebhookBodyImpl(
                    fileStorageService.storeFileContent(
                        FileEntryConstants.FILES_DIR, getFilename(httpServletRequest.getContentType()),
                        httpServletRequest.getInputStream()),
                    ContentType.BINARY, httpServletRequest.getContentType());
            } else {
                body = new WebhookBodyImpl(
                    StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8),
                    ContentType.RAW, httpServletRequest.getContentType());
            }
        }

        WebhookRequest webhookRequest = new WebhookRequest(
            headers, parameters, body, WebhookMethod.valueOf(httpServletRequest.getMethod()));

        if (logger.isDebugEnabled()) {
            logger.debug(
                "webhooks: id={}, webhookRequest={}, webhookTriggerFlags={}", id, webhookRequest, webhookTriggerFlags);
        }

        if (webhookTriggerFlags.workflowSyncExecution()) {
            responseEntity = ResponseEntity.ok(webhookExecutor.execute(workflowExecutionId, webhookRequest));
        } else if (webhookTriggerFlags.workflowSyncValidation()) {
            if (webhookExecutor.validateAndExecuteAsync(workflowExecutionId, webhookRequest)) {
                responseEntity = getResponseEntity(ResponseEntity.ok());
            } else {
                responseEntity = getResponseEntity(ResponseEntity.badRequest());
            }
        } else {
            webhookExecutor.executeAsync(workflowExecutionId, webhookRequest);

            responseEntity = getResponseEntity(ResponseEntity.ok());
        }

        return responseEntity;
    }

    private OperationType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return OperationType.ofType(workflowTrigger.getType());
    }

    private static String getFilename(String mimeTypeString) {
        MimeType mimeType = MimeTypeUtils.parseMimeType(mimeTypeString);

        String subtype = mimeType.getSubtype();

        return "file." + subtype.toLowerCase();
    }

    private static Map<String, String[]> getHeaderMap(HttpServletRequest httpServletRequest) {
        Map<String, String[]> headerMap = new HashMap<>();
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headerMap.put(headerName, toArray(httpServletRequest.getHeaders(headerName)));
        }

        return headerMap;
    }

    private static <T> ResponseEntity<T> getResponseEntity(ResponseEntity.BodyBuilder bodyBuilder) {
        return bodyBuilder.build();
    }

    private static UriComponents getUriComponents(HttpServletRequest httpServletRequest) {
        return UriComponentsBuilder
            .fromHttpRequest(new ServletServerHttpRequest(httpServletRequest))
            .build();
    }

    private static String[] toArray(Enumeration<String> enumeration) {
        List<String> list = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }

        return list.toArray(new String[0]);
    }

    private static Map<String, String[]> toMap(MultiValueMap<String, String> multiValueMap) {
        Map<String, String[]> resultMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : multiValueMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            String[] arrayOfValues = new String[values.size()];

            resultMap.put(key, values.toArray(arrayOfValues));
        }

        return resultMap;
    }
}
