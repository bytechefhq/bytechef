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

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link McpApps}. A widget HTML bundle on the classpath (test fixtures here for
 * {@code data-table-viewer} and {@code workflow-editor}; the built widget bundles in production) is served as its
 * {@code ui://bytechef/<name>} resource; an absent bundle yields no resource. Passing a public URL adds the icon base
 * injection and the CSP grant that icon-rendering widgets need.
 *
 * @author Ivica Cardic
 */
class McpAppsTest {

    private static final String DATA_TABLE_VIEWER_URI = "ui://bytechef/data-table-viewer";
    private static final String WORKFLOW_EDITOR_URI = "ui://bytechef/workflow-editor";

    @Test
    void testGetResourceSpecificationsServesBundledHtml() {
        List<McpServerFeatures.AsyncResourceSpecification> specifications = McpApps.getResourceSpecifications(
            DATA_TABLE_VIEWER_URI, "Data Table Viewer", "Read-only data table");

        assertThat(specifications).hasSize(1);

        McpSchema.Resource resource = specifications.getFirst()
            .resource();

        assertThat(resource.uri()).isEqualTo(DATA_TABLE_VIEWER_URI);
        assertThat(resource.mimeType()).isEqualTo("text/html;profile=mcp-app");

        McpSchema.ReadResourceResult readResourceResult = specifications.getFirst()
            .readHandler()
            .apply(null, new McpSchema.ReadResourceRequest(DATA_TABLE_VIEWER_URI))
            .block();

        assertThat(readResourceResult).isNotNull();

        McpSchema.TextResourceContents textResourceContents = (McpSchema.TextResourceContents) readResourceResult
            .contents()
            .getFirst();

        assertThat(textResourceContents.text()).contains("Data Table Viewer Fixture");
    }

    @Test
    void testAbsentBundleReturnsNoResource() {
        assertThat(McpApps.getResourceSpecifications(
            "ui://bytechef/nonexistent-viewer", "Missing Viewer", "No bundle on the classpath")).isEmpty();
    }

    @Test
    void testGetResourceSpecificationsWithoutPublicUrlAddsNoIconBaseUrlOrCsp() {
        List<McpServerFeatures.AsyncResourceSpecification> specifications = McpApps.getResourceSpecifications(
            WORKFLOW_EDITOR_URI, "ByteChef Workflow Editor", "Read-only workflow canvas");

        assertThat(specifications).hasSize(1);

        McpSchema.Resource resource = specifications.getFirst()
            .resource();

        assertThat(resource.uri()).isEqualTo(WORKFLOW_EDITOR_URI);
        assertThat(resource.mimeType()).isEqualTo("text/html;profile=mcp-app");
        assertThat(resource.meta()).isNull();

        McpSchema.ReadResourceResult readResourceResult = specifications.getFirst()
            .readHandler()
            .apply(null, new McpSchema.ReadResourceRequest(WORKFLOW_EDITOR_URI))
            .block();

        assertThat(readResourceResult).isNotNull();
        assertThat(readResourceResult.contents()).hasSize(1);

        McpSchema.TextResourceContents textResourceContents = (McpSchema.TextResourceContents) readResourceResult
            .contents()
            .getFirst();

        assertThat(textResourceContents.uri()).isEqualTo(WORKFLOW_EDITOR_URI);
        assertThat(textResourceContents.mimeType()).isEqualTo("text/html;profile=mcp-app");
        assertThat(textResourceContents.text()).contains("<!doctype html>");
        assertThat(textResourceContents.text()).doesNotContain("__BYTECHEF_ICON_BASE_URL__");
    }

    @Test
    void testGetResourceSpecificationsInjectsIconBaseUrlAndCspForPublicUrl() {
        List<McpServerFeatures.AsyncResourceSpecification> specifications = McpApps.getResourceSpecifications(
            WORKFLOW_EDITOR_URI, "ByteChef Workflow Editor", "Read-only workflow canvas",
            "https://bytechef.example.com:8443/");

        assertThat(specifications).hasSize(1);

        McpSchema.Resource resource = specifications.getFirst()
            .resource();

        assertThat(resource.meta()).containsEntry(
            "ui",
            Map.of("csp", Map.of("resourceDomains", List.of("https://bytechef.example.com:8443"))));

        McpSchema.ReadResourceResult readResourceResult = specifications.getFirst()
            .readHandler()
            .apply(null, new McpSchema.ReadResourceRequest(WORKFLOW_EDITOR_URI))
            .block();

        assertThat(readResourceResult).isNotNull();

        McpSchema.TextResourceContents textResourceContents = (McpSchema.TextResourceContents) readResourceResult
            .contents()
            .getFirst();

        assertThat(textResourceContents.text()).contains(
            "window.__BYTECHEF_ICON_BASE_URL__=\"https://bytechef.example.com:8443/icons/components\";");
    }

    @Test
    void testGetToolMetaUsesModernNestedFormat() {
        Map<String, Object> toolMeta = McpApps.getToolMeta(WORKFLOW_EDITOR_URI);

        assertThat(toolMeta).containsEntry("ui", Map.of("resourceUri", WORKFLOW_EDITOR_URI));
    }
}
