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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.util.List;
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
class ListAssetFilesToolCallbackTest {

    @Mock
    private AssetFileFacade facade;

    @Test
    void testCallHappyPath() {
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 11L));

        AssetFile file = new AssetFile();

        file.setId(101L);
        file.setName("notes.md");
        file.setMimeType("text/markdown");
        file.setSizeBytes(42L);

        when(facade.findAllByWorkspaceIdAndEnvironment(eq(11L), anyInt(), isNull())).thenReturn(List.of(file));

        ListAssetFilesToolCallback callback = new ListAssetFilesToolCallback(facade);

        String result = callback.call("{}", toolContext);

        assertThat(result).contains("\"id\":101");
        assertThat(result).contains("\"name\":\"notes.md\"");
        assertThat(result).contains("\"mimeType\":\"text/markdown\"");
    }

    @Test
    void testCallPrefersToolContextOverProvider() {
        AssetFile file = new AssetFile();

        file.setId(202L);
        file.setName("spec.md");
        file.setMimeType("text/markdown");
        file.setSizeBytes(50L);

        when(facade.findAllByWorkspaceIdAndEnvironment(eq(42L), anyInt(), isNull())).thenReturn(List.of(file));

        ListAssetFilesToolCallback callback = new ListAssetFilesToolCallback(facade);

        ToolContext toolContext = new ToolContext(
            Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L));

        String result = callback.call("{}", toolContext);

        assertThat(result).contains("\"id\":202");

    }

    @Test
    void testCallReturnsErrorWhenWorkspaceContextMissing() {

        ListAssetFilesToolCallback callback = new ListAssetFilesToolCallback(facade);

        String result = callback.call("{}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("workspace context unavailable");
    }
}
