/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.component.trigger.WebhookRequest.WebhookBodyImpl;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.tenant.TenantContext;
import com.bytechef.platform.tenant.util.TenantUtils;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.ForwardedHeaderUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}"
    + ApiPlatformHandlerController.API_PLATFORM_ROOT_PATH + "/**")
public class ApiPlatformHandlerController {

    private static final Logger logger = LoggerFactory.getLogger(ApiPlatformHandlerController.class);

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    protected static final String API_PLATFORM_ROOT_PATH = "/o";

    private final ApiCollectionService apiCollectionService;
    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ApiPlatformHandlerExecutor apiPlatformHandlerExecutor;
    private final String basePath;
    private final FilesFileStorage filesFileStorage;

    @SuppressFBWarnings("EI")
    public ApiPlatformHandlerController(
        @Value("${openapi.openAPIDefinition.base-path.automation:}") String basePath,
        ApiCollectionService apiCollectionService, ApiCollectionEndpointService apiCollectionEndpointService,
        ApiPlatformHandlerExecutor apiPlatformHandlerExecutor, FilesFileStorage filesFileStorage) {

        this.apiCollectionService = apiCollectionService;
        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiPlatformHandlerExecutor = apiPlatformHandlerExecutor;
        this.basePath = basePath;
        this.filesFileStorage = filesFileStorage;
    }

    @DeleteMapping(produces = "application/json")
    public Object handleDeleteMethod(final HttpServletRequest request) {
        return null;
    }

    @GetMapping(produces = "application/json")
    public Object handleGetMethod(final HttpServletRequest request) {
        return doHandle(request);
    }

    @PatchMapping(produces = "application/json")
    public Object handlePatchMethod(final HttpServletRequest request) {
        return null;
    }

    @PostMapping(produces = "application/json")
    public Object handlePostMethod(final HttpServletRequest request) {
        return null;
    }

    @PutMapping(produces = "application/json")
    public Object handlePutMethod(final HttpServletRequest request) {
        return null;
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

    private Object doHandle(HttpServletRequest request) {
        return TenantUtils.callWithTenantId(TenantContext.getCurrentTenantId(), () -> {
            Map<String, List<String>> variables;

            String requestURI = request.getRequestURI();

            String path = requestURI.replace(basePath + API_PLATFORM_ROOT_PATH, "");

            ApiCollectionEndpoint apiCollectionEndpoint = getApiCollectionEndpoint(path, getEnvironment(request));

            ApiCollection apiCollection = apiCollectionService.getApiCollection(
                apiCollectionEndpoint.getApiCollectionId());

            variables = PATH_MATCHER
                .extractUriTemplateVariables(
                    getPathPattern(apiCollection.getCollectionVersion(), apiCollectionEndpoint.getPath()), path)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

            // TODO fetch from New API Request Trigger
            WebhookTriggerFlags webhookTriggerFlags = new WebhookTriggerFlags(false, true, false, false);

            WebhookRequest webhookRequest = getWebhookRequest(request, webhookTriggerFlags);

            webhookRequest = new WebhookRequest(
                webhookRequest.headers(), MapUtils.concat(webhookRequest.parameters(), variables),
                webhookRequest.body(), webhookRequest.method());

            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                AppType.AUTOMATION, apiCollection.getProjectInstanceId(),
                apiCollectionEndpoint.getWorkflowReferenceCode(), "trigger_1");

            // TODO return response from ResponseToAPIRequest action

            return apiPlatformHandlerExecutor.executeSync(workflowExecutionId, webhookRequest);
        });
    }

    private ApiCollectionEndpoint getApiCollectionEndpoint(String path, Environment environment) {
        for (ApiCollection apiCollection : apiCollectionService.getApiCollections(null, environment, null, null)) {
            List<ApiCollectionEndpoint> apiCollectionEndpoints = apiCollectionEndpointService.getApiEndpoints(
                apiCollection.getId());

            for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpoints) {
                if (PATH_MATCHER.match(
                    getPathPattern(apiCollection.getCollectionVersion(), apiCollectionEndpoint.getPath()), path)) {

                    return apiCollectionEndpoint;
                }
            }
        }

        throw new IllegalArgumentException("No API Collection endpoint found for request uri: " + path);
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
                    value.add(StreamUtils.copyToString(part.getInputStream(), StandardCharsets.UTF_8));
                } else {
                    value.add(
                        filesFileStorage.storeFileContent(part.getSubmittedFileName(), part.getInputStream()));
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
            Object content;
            String rawContent = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);

            if (webhookTriggerFlags.webhookRawBody()) {
                content = rawContent;
            } else {
                content = JsonUtils.read(rawContent);
            }

            body = new WebhookBodyImpl(content, ContentType.JSON, httpServletRequest.getContentType(), rawContent);
        } else if (contentType.startsWith(MimeTypeUtils.MIME_APPLICATION_XML)) {
            Object content;
            String rawContent = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);

            if (webhookTriggerFlags.webhookRawBody()) {
                content = rawContent;
            } else {
                content = XmlUtils.read(rawContent);
            }

            body = new WebhookBodyImpl(content, ContentType.XML, httpServletRequest.getContentType(), rawContent);
        } else if (contentType.startsWith("application/")) {
            body = new WebhookBodyImpl(
                filesFileStorage.storeFileContent(
                    getFilename(httpServletRequest.getContentType()), httpServletRequest.getInputStream()),
                ContentType.BINARY, httpServletRequest.getContentType(), null);
        } else {
            String rawContent = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);

            body = new WebhookBodyImpl(
                rawContent, ContentType.RAW, httpServletRequest.getContentType(), null);
        }

        return new BodyAndParameters(body, parameters);
    }

    protected Environment getEnvironment(HttpServletRequest request) {
        String environment = request.getHeader("x-environment");

        if (StringUtils.isNotBlank(environment)) {
            return Environment.valueOf(environment.toUpperCase());
        }

        return Environment.PRODUCTION;
    }

    private static String getFilename(String mimeTypeString) {
        MimeType mimeType = org.springframework.util.MimeTypeUtils.parseMimeType(mimeTypeString);

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

    private static String getPathPattern(int collectionVersion, String apiCollectionEndpointPath) {
        return "/v" + collectionVersion + "/" + apiCollectionEndpointPath;
    }

    private static UriComponents getUriComponents(HttpServletRequest httpServletRequest) {
        ServletServerHttpRequest servletServerHttpRequest = new ServletServerHttpRequest(httpServletRequest);

        UriComponentsBuilder uriComponentsBuilder = ForwardedHeaderUtils.adaptFromForwardedHeaders(
            servletServerHttpRequest.getURI(), servletServerHttpRequest.getHeaders());

        return uriComponentsBuilder.build();
    }

    private WebhookRequest getWebhookRequest(
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
            headers, parameters, body, TriggerDefinition.WebhookMethod.valueOf(httpServletRequest.getMethod()));
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

                    List<Object> convertedValues = Arrays
                        .stream(values)
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

    private Map<String, List<String>> toMap(Map<String, String[]> map) {
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
