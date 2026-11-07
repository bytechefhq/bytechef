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

package com.bytechef.platform.mcp.server;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
public final class McpApps {

    public static final String RESOURCE_MIME_TYPE = "text/html;profile=mcp-app";

    private static final Logger log = LoggerFactory.getLogger(McpApps.class);

    private McpApps() {
    }

    public static Map<String, Object> getToolMeta(String resourceUri) {
        return Map.of("ui", Map.of("resourceUri", resourceUri));
    }

    public static List<McpServerFeatures.AsyncResourceSpecification> getResourceSpecifications(
        String resourceUri, String title, String description) {

        return getResourceSpecifications(resourceUri, title, description, null);
    }

    public static List<McpServerFeatures.AsyncResourceSpecification> getResourceSpecifications(
        String resourceUri, String title, String description, @Nullable String publicUrl) {

        String name = resourceUri.substring(resourceUri.lastIndexOf('/') + 1);
        String classpathLocation = "mcp-apps/" + name + ".html";

        ClassLoader classLoader = McpApps.class.getClassLoader();

        if (classLoader.getResource(classpathLocation) == null) {
            log.warn(
                "MCP App HTML not found at classpath:{}; the {} resource will not be served. Build it via its "
                    + "Gradle task (npm run build in mcp-apps/{}), then rebuild this module.",
                classpathLocation, resourceUri, name);

            return List.of();
        }

        String html;

        try (InputStream inputStream = classLoader.getResourceAsStream(classpathLocation)) {
            html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }

        McpSchema.Resource.Builder resourceBuilder = McpSchema.Resource.builder(resourceUri, title)
            .description(description)
            .mimeType(RESOURCE_MIME_TYPE);

        if (publicUrl != null && !publicUrl.isBlank()) {
            String normalizedPublicUrl = publicUrl.strip()
                .replaceAll("/+$", "");

            html = html.replace(
                "<head>",
                "<head><script>window.__BYTECHEF_ICON_BASE_URL__="
                    + toJsStringLiteral(normalizedPublicUrl + "/icons") + ";</script>");

            resourceBuilder.meta(
                Map.of("ui", Map.of("csp", Map.of("resourceDomains", List.of(toOrigin(normalizedPublicUrl))))));
        }

        McpSchema.ReadResourceResult readResourceResult = McpSchema.ReadResourceResult.builder(
            List.of(
                McpSchema.TextResourceContents.builder(resourceUri, html)
                    .mimeType(RESOURCE_MIME_TYPE)
                    .build()))
            .build();

        return List.of(new McpServerFeatures.AsyncResourceSpecification(
            resourceBuilder.build(), (exchange, readResourceRequest) -> Mono.just(readResourceResult)));
    }

    private static String toJsStringLiteral(String value) {
        return "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("<", "\\u003c") + "\"";
    }

    private static String toOrigin(String url) {
        URI uri = URI.create(url);

        return uri.getScheme() + "://" + uri.getAuthority();
    }
}
