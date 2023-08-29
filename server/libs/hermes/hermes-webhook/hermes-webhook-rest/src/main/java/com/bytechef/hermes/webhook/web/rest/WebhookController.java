
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.autoconfigure.annotation.ConditionalOnEnabled;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.hermes.component.util.XmlUtils;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.webhook.executor.WebhookExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnEnabled("coordinator")
public class WebhookController {

    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final WebhookExecutor webhookExecutor;

    @SuppressFBWarnings("EI")
    public WebhookController(
        FileStorageService fileStorageService, ObjectMapper objectMapper, WebhookExecutor webhookExecutor) {

        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.webhookExecutor = webhookExecutor;
    }

    @RequestMapping(
        method = {
            RequestMethod.GET, RequestMethod.POST
        },
        value = "/webhooks/{id}")
    public ResponseEntity<?> webhooks(@PathVariable String id, HttpServletRequest httpServletRequest)
        throws Exception {

        WebhookBody body = null;
        String mediaType = httpServletRequest.getContentType();
        Map<String, String[]> headers = getHeaderMap(httpServletRequest);
        Map<String, String[]> parameters = httpServletRequest.getParameterMap();
        ResponseEntity<?> responseEntity;
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        if (mediaType != null && !mediaType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            if (mediaType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                Map<String, Object> multipartFormDataMap = new HashMap<>();

                for (Part part : httpServletRequest.getParts()) {
                    if (isBinaryPart(part)) {
                        multipartFormDataMap.put(
                            part.getName(),
                            fileStorageService.storeFileContent(part.getName(), part.getInputStream()));
                    } else {
                        multipartFormDataMap.put(
                            part.getName(),
                            StreamUtils.copyToString(part.getInputStream(), StandardCharsets.UTF_8));
                    }
                }

                body = new WebhookBodyImpl(
                    multipartFormDataMap, ContentType.FORM_DATA, httpServletRequest.getContentType());
            } else if (mediaType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
                Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();

                UriComponents uriComponents = UriComponentsBuilder
                    .fromHttpRequest(new ServletServerHttpRequest(httpServletRequest))
                    .build();

                MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();

                for (String queryParam : queryParams.keySet()) {
                    parameterMap.remove(queryParam);
                }

                body = new WebhookBodyImpl(
                    parameterMap, ContentType.FORM_URL_ENCODED, httpServletRequest.getContentType());
                parameters = toMap(queryParams);
            } else if (mediaType.startsWith(MimeTypeUtils.APPLICATION_JSON_VALUE)) {
                Object content;

                if (workflowExecutionId.isWebhookRawBody()) {
                    content = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);
                } else {
                    content = JsonUtils.read(
                        StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8),
                        objectMapper);
                }

                body = new WebhookBodyImpl(content, ContentType.JSON, httpServletRequest.getContentType());
            } else if (mediaType.startsWith(MimeTypeUtils.APPLICATION_XML_VALUE)) {
                Object content;

                if (workflowExecutionId.isWebhookRawBody()) {
                    content = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);
                } else {
                    content = XmlUtils.read(
                        StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8));
                }

                body = new WebhookBodyImpl(content, ContentType.XML, httpServletRequest.getContentType());
            } else if (mediaType.startsWith("application/")) {
                body = new WebhookBodyImpl(
                    fileStorageService.storeFileContent(
                        "file." + toFileExtension(httpServletRequest.getContentType()),
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

        if (workflowExecutionId.isWorkflowSyncExecution()) {
            responseEntity = ResponseEntity.ok(webhookExecutor.execute(workflowExecutionId, webhookRequest));
        } else if (workflowExecutionId.isWorkflowSyncValidation()) {
            if (webhookExecutor.validateAndExecute(workflowExecutionId, webhookRequest)) {
                responseEntity = ResponseEntity.ok()
                    .build();
            } else {
                responseEntity = ResponseEntity.badRequest()
                    .build();
            }
        } else {
            webhookExecutor.executeAsync(workflowExecutionId, webhookRequest);

            responseEntity = ResponseEntity.ok()
                .build();
        }

        return responseEntity;
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

    private static String[] toArray(Enumeration<String> enumeration) {
        ArrayList<String> arrayList = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            arrayList.add(enumeration.nextElement());
        }

        return arrayList.toArray(new String[0]);
    }

    private static String toFileExtension(String mimeTypeString) {
        MimeType mimeType = MimeTypeUtils.parseMimeType(mimeTypeString);

        String subtype = mimeType.getSubtype();

        return subtype.toLowerCase();
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

    private boolean isBinaryPart(Part part) {
        String contentType = part.getContentType();

        return contentType != null && contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private record WebhookParameters(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
    }

    private record WebhookBodyImpl(Object content, ContentType contentType, String mimeType) implements WebhookBody {
    }

}
