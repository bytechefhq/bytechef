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

package com.bytechef.platform.workflow.webhook.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.StreamUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.component.definition.WorkflowNodeType;
import com.bytechef.platform.component.registry.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.component.registry.trigger.WebhookRequest;
import com.bytechef.platform.component.registry.trigger.WebhookRequest.WebhookBodyImpl;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.constants.FileEntryConstants;
import com.bytechef.platform.workflow.webhook.executor.WebhookExecutor;
import com.bytechef.platform.workflow.webhook.web.rest.exception.WorkflowNotEnabledException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.util.ForwardedHeaderUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnEndpoint
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final FileStorageService fileStorageService;
    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WebhookExecutor webhookExecutor;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WebhookController(
        FileStorageService fileStorageService, InstanceAccessorRegistry instanceAccessorRegistry,
        TriggerDefinitionService triggerDefinitionService, WebhookExecutor webhookExecutor,
        WorkflowService workflowService) {

        this.fileStorageService = fileStorageService;
        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookExecutor = webhookExecutor;
        this.workflowService = workflowService;
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
            Type.valueOf(workflowExecutionId.getType()));

        if (!instanceAccessor.isWorkflowEnabled(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId())) {

            throw new WorkflowNotEnabledException(
                "Workflow id=%s is not enabled".formatted(workflowExecutionId.getWorkflowId()));
        }

        WebhookBodyImpl body = null;
        String mediaType = httpServletRequest.getContentType();
        Map<String, List<String>> headers = getHeaderMap(httpServletRequest);
        Map<String, List<String>> parameters = toMap(httpServletRequest.getParameterMap());
        ResponseEntity<?> responseEntity;

        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId);

        WebhookTriggerFlags webhookTriggerFlags = triggerDefinitionService.getWebhookTriggerFlags(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName());

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
                        StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8));
                }

                body = new WebhookBodyImpl(content, ContentType.JSON, httpServletRequest.getContentType());
            } else if (mediaType.startsWith(MimeTypeUtils.MIME_APPLICATION_XML)) {
                Object content;

                if (webhookTriggerFlags.webhookRawBody()) {
                    content = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);
                } else {
                    content = XmlUtils.read(
                        StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8));
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
            responseEntity = ResponseEntity.ok(webhookExecutor.executeSync(workflowExecutionId, webhookRequest));
        } else if (webhookTriggerFlags.workflowSyncValidation()) {
            if (webhookExecutor.validateAndExecuteAsync(workflowExecutionId, webhookRequest)) {
                responseEntity = getResponseEntity(ResponseEntity.ok());
            } else {
                responseEntity = getResponseEntity(ResponseEntity.badRequest());
            }
        } else {
            webhookExecutor.execute(workflowExecutionId, webhookRequest);

            responseEntity = getResponseEntity(ResponseEntity.ok());
        }

        return responseEntity;
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private static String getFilename(String mimeTypeString) {
        MimeType mimeType = MimeTypeUtils.parseMimeType(mimeTypeString);

        String subtype = mimeType.getSubtype();

        return "file." + subtype.toLowerCase();
    }

    private static Map<String, List<String>> getHeaderMap(HttpServletRequest httpServletRequest) {
        Map<String, List<String>> headerMap = new HashMap<>();
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headerMap.put(headerName, toList(httpServletRequest.getHeaders(headerName)));
        }

        return headerMap;
    }

    private static <T> ResponseEntity<T> getResponseEntity(ResponseEntity.BodyBuilder bodyBuilder) {
        return bodyBuilder.build();
    }

    private static UriComponents getUriComponents(HttpServletRequest httpServletRequest) {
        ServletServerHttpRequest servletServerHttpRequest = new ServletServerHttpRequest(httpServletRequest);

        UriComponentsBuilder uriComponentsBuilder = ForwardedHeaderUtils.adaptFromForwardedHeaders(
            servletServerHttpRequest.getURI(), servletServerHttpRequest.getHeaders());

        return uriComponentsBuilder.build();
    }

    private static List<String> toList(Enumeration<String> enumeration) {
        List<String> list = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }

        return list;
    }

    private Map<String, List<String>> toMap(Map<String, String[]> map) {
        return map
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey, entry -> Arrays.asList(entry.getValue())));
    }

    private static Map<String, List<String>> toMap(MultiValueMap<String, String> multiValueMap) {
        return new HashMap<>(multiValueMap);
    }
}
