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

package com.bytechef.automation.assetfile.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

@ExtendWith(ObjectMapperSetupExtension.class)
class AssetFileViewerMcpContributorConfigurationTest {

    private final AssetFileViewerMcpContributorConfiguration configuration =
        new AssetFileViewerMcpContributorConfiguration();

    private final AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    @BeforeEach
    void beforeEach() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(42L, "Workspace 42")));
    }

    @Test
    void testContributesTheReadToolWorkspaceScoped() {
        List<ToolCallback> toolCallbacks = contributor().getToolCallbacks();

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactly("getAssetFileContent");
        assertThat(toolCallbacks)
            .allSatisfy(toolCallback -> {
                assertThat(toolCallback).isInstanceOf(WorkspaceScopedFlatToolCallback.class);
                assertThat(toolCallback.getToolDefinition()
                    .inputSchema()).contains("\"workspaceId\"", "\"environment\"");
            });
    }

    @Test
    void testContributesNoToolsWhenTheFacadeIsAbsent() {
        McpServerToolCallbackContributor contributor = configuration.assetFileViewerMcpContributor(
            provider(new StaticListableBeanFactory(), AssetFileFacade.class),
            provider(new StaticListableBeanFactory(Map.of("workspaceFacade", workspaceFacade)), WorkspaceFacade.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
        assertThat(contributor.getMcpAppUiDescriptors()).containsOnlyKeys("getAssetFileContent");
    }

    @Test
    void testGetAssetFileContentReadsAFileInTheRequestedWorkspace() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(5L);
        assetFile.setName("notes.txt");
        assetFile.setMimeType("text/plain");
        assetFile.setSizeBytes(5);

        when(assetFileFacade.findByIdInWorkspace(5L, 42L)).thenReturn(assetFile);
        when(assetFileFacade.downloadContent(5L)).thenReturn(
            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        String result = findToolCallback("getAssetFileContent").call("{\"workspaceId\":42,\"id\":5}");

        assertThat(result).contains("\"name\":\"notes.txt\"", "\"content\":\"hello\"");
    }

    @Test
    void testGetAssetFileContentRefusesAWorkspaceTheCallerCannotAccess() {
        String result = findToolCallback("getAssetFileContent").call("{\"workspaceId\":99,\"id\":5}");

        assertThat(result).contains("not accessible");

        verify(assetFileFacade, never()).findByIdInWorkspace(5L, 99L);
    }

    @Test
    void testContributesTheFileViewerDescriptor() {
        assertThat(contributor().getMcpAppUiDescriptors()
            .get("getAssetFileContent")
            .resourceUri()).isEqualTo(McpAppResources.FILE_VIEWER_URI);
    }

    @Test
    void testFileShaperExtractsViewerFields() {
        Map<String, Object> structuredContent = contributor().getMcpAppUiDescriptors()
            .get("getAssetFileContent")
            .structuredContentShaper()
            .apply("{\"id\":1,\"name\":\"a.txt\",\"mimeType\":\"text/plain\",\"content\":\"hello\"}");

        assertThat(structuredContent).containsEntry("name", "a.txt")
            .containsEntry("mimeType", "text/plain")
            .containsEntry("content", "hello")
            .doesNotContainKey("id");
    }

    private McpServerToolCallbackContributor contributor() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
            Map.of("assetFileFacade", assetFileFacade, "workspaceFacade", workspaceFacade));

        return configuration.assetFileViewerMcpContributor(
            provider(beanFactory, AssetFileFacade.class), provider(beanFactory, WorkspaceFacade.class));
    }

    private ToolCallback findToolCallback(String name) {
        return contributor().getToolCallbacks()
            .stream()
            .filter(toolCallback -> name.equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();
    }

    private static <T> ObjectProvider<T> provider(StaticListableBeanFactory beanFactory, Class<T> type) {
        return beanFactory.getBeanProvider(type);
    }
}
