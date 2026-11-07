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

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class AssetFileViewerMcpContributorConfigurationTest {

    private final AssetFileViewerMcpContributorConfiguration configuration =
        new AssetFileViewerMcpContributorConfiguration();

    @Test
    void testContributesNoToolsButTheFileViewerDescriptor() {
        McpServerToolCallbackContributor contributor = configuration.assetFileViewerMcpContributor();

        assertThat(contributor.getToolCallbacks()).isEmpty();
        assertThat(contributor.getMcpAppUiDescriptors()
            .get("getAssetFileContent")
            .resourceUri()).isEqualTo(McpAppResources.FILE_VIEWER_URI);
    }

    @Test
    void testFileShaperExtractsViewerFields() {
        McpServerToolCallbackContributor contributor = configuration.assetFileViewerMcpContributor();

        Map<String, Object> structuredContent = contributor.getMcpAppUiDescriptors()
            .get("getAssetFileContent")
            .structuredContentShaper()
            .apply("{\"id\":1,\"name\":\"a.txt\",\"mimeType\":\"text/plain\",\"content\":\"hello\"}");

        assertThat(structuredContent).containsEntry("name", "a.txt")
            .containsEntry("mimeType", "text/plain")
            .containsEntry("content", "hello")
            .doesNotContainKey("id");
    }
}
