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

package com.bytechef.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

/**
 * Unit test for the MCP App workflow editor decoration in {@link ManagementMcpServerConfiguration}: the workflow
 * get/create/update tools gain {@code _meta.ui.resourceUri} (and {@code readOnlyHint} on the read tool), and their
 * results carry the nested workflow definition as {@code structuredContent.definition}.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ManagementMcpServerWorkflowEditorTest {

    private static final String DEFINITION_JSON =
        "{\"label\":\"Test\",\"triggers\":[{\"name\":\"trigger_1\",\"type\":\"manual/v1/manual\"}],\"tasks\":[]}";

    @Test
    void testAttachWorkflowEditorUiDecoratesOnlyWorkflowTools() {
        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = ManagementMcpServerConfiguration
            .attachWorkflowEditorUi(
                List.of(
                    toolSpecification("getWorkflow"), toolSpecification("createProjectWorkflow"),
                    toolSpecification("updateWorkflow"), toolSpecification("deleteWorkflow")));

        Map<String, Object> expectedUi = Map.of("resourceUri", "ui://bytechef/workflow-editor");

        for (McpServerFeatures.AsyncToolSpecification toolSpecification : toolSpecifications) {
            McpSchema.Tool tool = toolSpecification.tool();

            if ("deleteWorkflow".equals(tool.name())) {
                assertThat(tool.meta()).isNull();
            } else {
                assertThat(tool.meta()).containsEntry("ui", expectedUi);
            }
        }
    }

    @Test
    void testAttachWorkflowEditorUiMarksGetWorkflowReadOnly() {
        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = ManagementMcpServerConfiguration
            .attachWorkflowEditorUi(List.of(toolSpecification("getWorkflow"), toolSpecification("updateWorkflow")));

        McpSchema.Tool getWorkflowTool = toolSpecifications.getFirst()
            .tool();

        assertThat(getWorkflowTool.annotations()).isNotNull();
        assertThat(getWorkflowTool.annotations()
            .readOnlyHint()).isTrue();

        McpSchema.Tool updateWorkflowTool = toolSpecifications.getLast()
            .tool();

        assertThat(updateWorkflowTool.annotations()).isNull();
    }

    @Test
    void testWithDefinitionStructuredContentReadsDefinitionFromResult() {
        McpSchema.CallToolResult callToolResult = ManagementMcpServerConfiguration.withDefinitionStructuredContent(
            new McpSchema.CallToolRequest("getWorkflow", Map.of("workflowId", "abc")),
            textResult("{\"id\":\"abc\",\"name\":\"Test\",\"definition\":" + escape(DEFINITION_JSON) + "}"));

        assertDefinitionStructuredContent(callToolResult);
    }

    @Test
    void testWithDefinitionStructuredContentFallsBackToRequestArgument() {
        McpSchema.CallToolResult callToolResult = ManagementMcpServerConfiguration.withDefinitionStructuredContent(
            new McpSchema.CallToolRequest("createProjectWorkflow", Map.of("projectId", 1, "definition",
                DEFINITION_JSON)),
            textResult("{\"id\":42,\"project_id\":1,\"workflow_id\":\"abc\"}"));

        assertDefinitionStructuredContent(callToolResult);
    }

    @Test
    void testWithDefinitionStructuredContentLeavesErrorAndNonJsonResultsUntouched() {
        McpSchema.CallToolResult errorResult = McpSchema.CallToolResult.builder()
            .content(List.of(McpSchema.TextContent.builder("boom")
                .build()))
            .isError(true)
            .build();

        assertThat(ManagementMcpServerConfiguration.withDefinitionStructuredContent(
            new McpSchema.CallToolRequest("updateWorkflow", Map.of()), errorResult)).isSameAs(errorResult);

        McpSchema.CallToolResult plainTextResult = textResult("not json at all");

        assertThat(ManagementMcpServerConfiguration.withDefinitionStructuredContent(
            new McpSchema.CallToolRequest("updateWorkflow", Map.of()), plainTextResult)).isSameAs(plainTextResult);
    }

    @Test
    void testAttachMcpAppUiDecoratesContributedDescriptorTool() {
        McpAppUiDescriptor descriptor = new McpAppUiDescriptor(
            "ui://bytechef/data-table-viewer", text -> Map.of("rows", List.of()));

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications =
            ManagementMcpServerConfiguration.attachMcpAppUi(
                List.of(toolSpecification("queryDataTable"), toolSpecification("otherTool")),
                Map.of("queryDataTable", descriptor));

        McpSchema.Tool queryDataTableTool = toolSpecifications.getFirst()
            .tool();

        assertThat(queryDataTableTool.meta())
            .containsEntry("ui", Map.of("resourceUri", "ui://bytechef/data-table-viewer"));
        assertThat(queryDataTableTool.annotations()
            .readOnlyHint()).isTrue();

        assertThat(toolSpecifications.getLast()
            .tool()
            .meta()).isNull();
    }

    @Test
    void testWithShapedStructuredContentAppliesShaper() {
        McpAppUiDescriptor descriptor = new McpAppUiDescriptor(
            "ui://bytechef/file-viewer", text -> Map.of("content", "hello"));

        McpSchema.CallToolResult callToolResult = ManagementMcpServerConfiguration.withShapedStructuredContent(
            textResult("{\"name\":\"a.txt\",\"content\":\"hello\"}"), descriptor);

        assertThat(callToolResult.structuredContent()).isEqualTo(Map.of("content", "hello"));
    }

    @Test
    void testWithShapedStructuredContentLeavesErrorAndNullShapeUntouched() {
        McpAppUiDescriptor descriptor = new McpAppUiDescriptor("ui://bytechef/file-viewer", text -> null);

        McpSchema.CallToolResult nullShapeResult = textResult("{}");

        assertThat(ManagementMcpServerConfiguration.withShapedStructuredContent(nullShapeResult, descriptor))
            .isSameAs(nullShapeResult);

        McpSchema.CallToolResult errorResult = McpSchema.CallToolResult.builder()
            .content(List.of(McpSchema.TextContent.builder("boom")
                .build()))
            .isError(true)
            .build();

        assertThat(ManagementMcpServerConfiguration.withShapedStructuredContent(
            errorResult, new McpAppUiDescriptor("ui://bytechef/file-viewer", text -> Map.of("x", "y"))))
                .isSameAs(errorResult);
    }

    @SuppressWarnings("unchecked")
    private static void assertDefinitionStructuredContent(McpSchema.CallToolResult callToolResult) {
        assertThat(callToolResult.structuredContent()).isInstanceOf(Map.class);

        Map<String, Object> structuredContent = (Map<String, Object>) callToolResult.structuredContent();

        assertThat(structuredContent).containsKey("definition");

        Map<String, Object> definition = (Map<String, Object>) structuredContent.get("definition");

        assertThat(definition).containsEntry("label", "Test");
        assertThat(definition.get("triggers")).isInstanceOf(List.class);
    }

    private static String escape(String json) {
        return "\"" + json.replace("\"", "\\\"") + "\"";
    }

    private static McpSchema.CallToolResult textResult(String text) {
        return McpSchema.CallToolResult.builder()
            .content(List.of(McpSchema.TextContent.builder(text)
                .build()))
            .build();
    }

    private static McpServerFeatures.AsyncToolSpecification toolSpecification(String toolName) {
        McpSchema.Tool tool = McpSchema.Tool.builder(toolName, Map.of("type", "object"))
            .description(toolName + " description")
            .build();

        return new McpServerFeatures.AsyncToolSpecification(
            tool,
            (exchange, callToolRequest) -> Mono.just(McpSchema.CallToolResult.builder()
                .content(List.of())
                .build()));
    }
}
