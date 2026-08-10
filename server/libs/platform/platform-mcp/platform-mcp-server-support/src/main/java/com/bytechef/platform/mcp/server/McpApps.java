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
 * Serves an MCP App widget — the workflow editor, or a read-only data table / code workflow / custom component / file
 * viewer — as a static, tenant-independent {@code ui://bytechef/<name>} resource. Each widget is a single
 * self-contained HTML file (built from {@code mcp-apps/<name>} and bundled into this module's resources by the matching
 * {@code build<Name>} Gradle task) that MCP Apps hosts render in a sandboxed iframe. A widget is a pure projection of
 * the backing tool's {@code structuredContent} and re-renders on every pushed result.
 *
 * <p>
 * Widgets that render component icons need the deployment's public URL, which the single-file bundle cannot know at
 * build time: passing one injects the icon base URL into the HTML and grants that origin in the resource CSP (maps to
 * {@code img-src}). Widgets that render only rows / source / text pass none and stay fully offline.
 * </p>
 *
 * <p>
 * The resources are static and tenant-independent, so each is served to every session of every ByteChef MCP server that
 * registers it. When the bundled HTML is absent (server built without Node.js), servers start normally and simply serve
 * no UI resource.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class McpApps {

    // MCP Apps UI resources are identified by this profile mime type (SEP-1865 / @modelcontextprotocol/ext-apps
    // RESOURCE_MIME_TYPE); hosts that don't support MCP Apps ignore the resource.
    public static final String RESOURCE_MIME_TYPE = "text/html;profile=mcp-app";

    private static final Logger log = LoggerFactory.getLogger(McpApps.class);

    private McpApps() {
    }

    /**
     * Tool {@code _meta} marking a tool as backed by the given MCP App (modern nested {@code ui.resourceUri} format per
     * the MCP Apps extension). Hosts that see this metadata fetch and render the resource alongside the tool result.
     */
    public static Map<String, Object> getToolMeta(String resourceUri) {
        return Map.of("ui", Map.of("resourceUri", resourceUri));
    }

    /**
     * Serves a widget that needs no deployment-specific origin.
     *
     * @param resourceUri the {@code ui://bytechef/<name>} identifier; its last segment names the classpath bundle
     *                    ({@code mcp-apps/<name>.html})
     */
    public static List<McpServerFeatures.AsyncResourceSpecification> getResourceSpecifications(
        String resourceUri, String title, String description) {

        return getResourceSpecifications(resourceUri, title, description, null);
    }

    /**
     * @param resourceUri the {@code ui://bytechef/<name>} identifier; its last segment names the classpath bundle
     *                    ({@code mcp-apps/<name>.html})
     * @param publicUrl   the deployment's public base URL ({@code bytechef.public-url}); when present, the widget is
     *                    pointed at this deployment's anonymous component-icon endpoint and the resource CSP allows
     *                    that origin for static resources (maps to {@code img-src}). When absent, the widget stays
     *                    fully offline and renders fallback glyphs instead of component icons.
     */
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

            // The single-file bundle cannot know the deployment URL at build time; inject the icon base for
            // ComponentImage (window.__BYTECHEF_ICON_BASE_URL__) at serve time instead.
            html = html.replace(
                "<head>",
                "<head><script>window.__BYTECHEF_ICON_BASE_URL__="
                    + toJsStringLiteral(normalizedPublicUrl + "/icons/components") + ";</script>");

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
