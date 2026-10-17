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

package com.bytechef.platform.security.web.mcp.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Ivica Cardic
 */
public final class McpProtectedResourceUrls {

    public static final String AUTOMATION_SCOPE = "mcp:automation";
    public static final String MANAGEMENT_SCOPE = "mcp:management";

    private static final Pattern AUTOMATION_PATH_PATTERN = Pattern.compile("^/api/automation/[^/]+/mcp/?$");
    private static final Pattern MANAGEMENT_PATH_PATTERN = Pattern.compile("^/api/management(?:/mcp|/[^/]+/mcp)/?$");

    private McpProtectedResourceUrls() {
    }

    public static String resolveResourceUrl(@Nullable String publicUrl, String requestBaseUrl, String path) {
        String baseUrl = StringUtils.isBlank(publicUrl) ? requestBaseUrl : publicUrl;

        return StringUtils.removeEnd(baseUrl, "/") + path;
    }

    public static String resolveResourceUrl(@Nullable String publicUrl, HttpServletRequest request) {
        return resolveResourceUrl(publicUrl, getRequestBaseUrl(request), getPathWithinApplication(request));
    }

    public static String getRequestBaseUrl(HttpServletRequest request) {
        return UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
            .replacePath(request.getContextPath())
            .replaceQuery(null)
            .fragment(null)
            .build()
            .toUriString();
    }

    public static String getPathWithinApplication(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        return requestUri.substring(request.getContextPath()
            .length());
    }

    public static String toPublicResourceUrl(@Nullable String publicUrl, String resourceUrl) {
        if (StringUtils.isBlank(publicUrl)) {
            return resourceUrl;
        }

        URI resourceUri = URI.create(resourceUrl);

        return StringUtils.removeEnd(publicUrl, "/") + resourceUri.getRawPath();
    }

    @Nullable
    public static String getRequiredScope(String path) {
        if (AUTOMATION_PATH_PATTERN.matcher(path)
            .matches()) {

            return AUTOMATION_SCOPE;
        }

        if (MANAGEMENT_PATH_PATTERN.matcher(path)
            .matches()) {

            return MANAGEMENT_SCOPE;
        }

        return null;
    }
}
