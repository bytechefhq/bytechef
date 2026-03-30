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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.component.trigger.WebhookRequest.WebhookBodyImpl;
import com.bytechef.platform.file.storage.TempFileStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ForwardedHeaderUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for extracting {@link WebhookRequest} data from {@link HttpServletRequest}.
 *
 * @author Ivica Cardic
 */
public class WebhookRequestUtils {

    private WebhookRequestUtils() {
    }

    public static WebhookRequest getWebhookRequest(
        HttpServletRequest httpServletRequest, TempFileStorage tempFileStorage) throws IOException, ServletException {

        return getWebhookRequest(
            httpServletRequest, tempFileStorage, new WebhookTriggerFlags(false, false, false, false));
    }

    public static WebhookRequest getWebhookRequest(
        HttpServletRequest httpServletRequest, TempFileStorage tempFileStorage,
        WebhookTriggerFlags webhookTriggerFlags) throws IOException, ServletException {

        WebhookBodyImpl body = null;
        String contentType = httpServletRequest.getContentType();
        Map<String, List<String>> headers = getHeaderMap(httpServletRequest);
        Map<String, List<String>> parameters;

        if (contentType == null) {
            parameters = MapUtils.toMap(httpServletRequest.getParameterMap());
        } else {
            BodyAndParameters bodyAndParameters = getBodyAndParameters(
                httpServletRequest, contentType, webhookTriggerFlags, tempFileStorage);

            body = bodyAndParameters.body;
            parameters = bodyAndParameters.parameters;
        }

        return new WebhookRequest(headers, parameters, body, WebhookMethod.valueOf(httpServletRequest.getMethod()));
    }

    public static Map<String, List<String>> getHeaderMap(HttpServletRequest httpServletRequest) {
        Map<String, List<String>> headerMap = new HashMap<>();
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            headerMap.put(headerName, CollectionUtils.toList(httpServletRequest.getHeaders(headerName)));
        }

        return headerMap;
    }

    public static UriComponents getUriComponents(HttpServletRequest httpServletRequest) {
        ServletServerHttpRequest servletServerHttpRequest = new ServletServerHttpRequest(httpServletRequest);

        UriComponentsBuilder uriComponentsBuilder = ForwardedHeaderUtils.adaptFromForwardedHeaders(
            servletServerHttpRequest.getURI(), servletServerHttpRequest.getHeaders());

        return uriComponentsBuilder.build();
    }

    static String getFilename(String mimeTypeString) {
        MimeType mimeType = org.springframework.util.MimeTypeUtils.parseMimeType(mimeTypeString);

        String subtype = mimeType.getSubtype();

        return "file." + subtype.toLowerCase();
    }

    @SuppressWarnings("unchecked")
    static Map<String, ?> parseMap(Map<String, ?> map) {
        Map<String, Object> multiMap = new HashMap<>();

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Split the key on [ or .
            String[] keys = key.split("\\[|\\.");

            Map<String, Object> currentMap = multiMap;

            for (int i = 0; i < keys.length; i++) {
                String currentKey = keys[i];

                // Remove any trailing ]
                if (currentKey.endsWith("]")) {
                    currentKey = currentKey.substring(0, currentKey.length() - 1);
                }

                if (i == keys.length - 1) {
                    // If we're at the last key, add the value

                    List<Object> values;

                    if (value instanceof Object[] objects) {
                        values = Arrays.stream(objects)
                            .map(WebhookRequestUtils::convertValue)
                            .toList();
                    } else if (value instanceof List<?> list) {
                        values = list.stream()
                            .map(WebhookRequestUtils::convertValue)
                            .toList();
                    } else {
                        values = List.of(convertValue(value));
                    }

                    currentMap.put(
                        currentKey, values.isEmpty() ? null : values.size() == 1 ? values.getFirst() : values);
                } else {
                    // Otherwise, add a new map if one doesn't already exist
                    currentMap.putIfAbsent(currentKey, new HashMap<String, Object>());

                    currentMap = (Map<String, Object>) currentMap.get(currentKey);
                }
            }
        }

        return multiMap;
    }

    private static Object convertValue(Object value) {
        if (value instanceof String string) {
            if (string.isBlank()) {
                return string;
            }

            return ConvertUtils.convertString(string);
        }

        return value;
    }

    private static BodyAndParameters getBodyAndParameters(
        HttpServletRequest httpServletRequest, String contentType, WebhookTriggerFlags webhookTriggerFlags,
        TempFileStorage tempFileStorage) throws IOException, ServletException {

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
                parseMap(multipartFormDataMap), ContentType.FORM_DATA, httpServletRequest.getContentType(), null);
            parameters = MapUtils.toMap(httpServletRequest.getParameterMap());
        } else if (contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            Map<String, String[]> parameterMap = new HashMap<>(httpServletRequest.getParameterMap());

            UriComponents uriComponents = getUriComponents(httpServletRequest);

            MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();

            for (String queryParam : queryParams.keySet()) {
                parameterMap.remove(queryParam);
            }

            body = new WebhookBodyImpl(
                parseMap(parameterMap), ContentType.FORM_URL_ENCODED, httpServletRequest.getContentType(), null);
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

    private record BodyAndParameters(WebhookBodyImpl body, Map<String, List<String>> parameters) {
    }
}
