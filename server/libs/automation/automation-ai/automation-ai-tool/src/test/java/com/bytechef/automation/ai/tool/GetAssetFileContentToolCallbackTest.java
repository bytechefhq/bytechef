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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class GetAssetFileContentToolCallbackTest {

    @Mock
    private AssetFileFacade facade;

    @Test
    void testCallHappyPath() {
        AssetFile file = new AssetFile();

        file.setId(123L);
        file.setName("spec.md");
        file.setMimeType("text/markdown");
        file.setSizeBytes(11L);
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L));

        when(facade.findByIdInWorkspace(123L, 7L)).thenReturn(file);
        when(facade.downloadContent(123L))
            .thenReturn(new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)));

        GetAssetFileContentToolCallback callback =
            new GetAssetFileContentToolCallback(facade);

        String result = callback.call("{\"id\":123}", toolContext);

        assertThat(result).contains("\"id\":123");
        assertThat(result).contains("\"name\":\"spec.md\"");
        assertThat(result).contains("\"content\":\"hello world\"");
    }

    @Test
    void testCallReturnsErrorWhenNoWorkspaceContext() {

        GetAssetFileContentToolCallback callback =
            new GetAssetFileContentToolCallback(facade);

        String result = callback.call("{\"id\":123}");

        assertThat(result).contains("\"error\"");
        assertThat(result).contains("workspace context");
    }

    @Test
    void testCallReturnsErrorWhenFileBelongsToDifferentWorkspace() {
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L));

        when(facade.findByIdInWorkspace(123L, 7L))
            .thenThrow(new IllegalArgumentException("Asset file 123 not found in workspace 7"));

        GetAssetFileContentToolCallback callback =
            new GetAssetFileContentToolCallback(facade);

        String result = callback.call("{\"id\":123}", toolContext);

        assertThat(result).contains("\"error\"");
        assertThat(result).contains("not found in workspace 7");
    }
}
