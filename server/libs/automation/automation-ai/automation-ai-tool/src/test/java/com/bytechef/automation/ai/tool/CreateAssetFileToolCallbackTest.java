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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ObjectMapperSetupExtension.class)
class CreateAssetFileToolCallbackTest {

    @Mock
    private AssetFileFacade facade;

    private CreateAssetFileToolCallback callback;

    @BeforeEach
    void setUp() {
        callback = new CreateAssetFileToolCallback(facade);
    }

    @Test
    void testCallHappyPath() {
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L,
            AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 0,
            AutomationToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, "write a runbook"));

        AssetFile saved = new AssetFile();

        saved.setId(42L);
        saved.setName("runbook.md");
        saved.setSizeBytes(10L);

        when(
            facade.createFromAi(
                eq(7L), anyInt(), eq("runbook.md"), eq("text/markdown"), eq("# runbook"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq((short) 0), eq("write a runbook")))
                    .thenReturn(saved);

        String input = "{\"filename\":\"runbook.md\",\"mimeType\":\"text/markdown\",\"content\":\"# runbook\"}";

        String result = callback.call(input, toolContext);

        assertThat(result).contains("\"id\":42");
        assertThat(result).contains("\"name\":\"runbook.md\"");
    }

    @Test
    void testCallRejectsDisallowedMime() {
        String input = "{\"filename\":\"x.png\",\"mimeType\":\"image/png\",\"content\":\"...\"}";

        String result = callback.call(input);

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("mime");
    }

    @Test
    void testCallPrefersToolContextOverProvider() {
        AssetFile saved = new AssetFile();

        saved.setId(5L);
        saved.setName("doc.md");
        saved.setSizeBytes(3L);

        when(
            facade.createFromAi(
                eq(88L), anyInt(), eq("doc.md"), eq("text/markdown"), eq("body"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq((short) 7), eq("tool-context prompt")))
                    .thenReturn(saved);

        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 88L,
            AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 7,
            AutomationToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, "tool-context prompt"));

        String input = "{\"filename\":\"doc.md\",\"mimeType\":\"text/markdown\",\"content\":\"body\"}";

        String result = callback.call(input, toolContext);

        assertThat(result).contains("\"id\":5");

    }

    @Test
    void testCallReturnsErrorWhenWorkspaceContextMissing() {

        String input = "{\"filename\":\"doc.md\",\"mimeType\":\"text/markdown\",\"content\":\"body\"}";

        String result = callback.call(input);

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("workspace context unavailable");
    }

    @Test
    void testMaxContentBytesPinnedAt64MiB() {
        assertThat(CreateAssetFileToolCallback.MAX_CONTENT_BYTES).isEqualTo(64L * 1024L * 1024L);
    }

    @Test
    void testCallRejectsPayloadWhereByteEstimateExceedsCap() {
        CreateAssetFileToolCallback smallBoundCallback = new CreateAssetFileToolCallback(
            facade, null, 10L);

        String input = "{\"filename\":\"doc.md\",\"mimeType\":\"text/markdown\",\"content\":\"abc\"}";

        String result = smallBoundCallback.call(input);

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("too large");

        verify(facade, never()).createFromAi(
            org.mockito.ArgumentMatchers.any(), anyInt(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testCallSurfacesQuotaLimitInsteadOfOpaqueExceptionName() {
        when(
            facade.createFromAi(
                org.mockito.ArgumentMatchers.any(), anyInt(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                    .thenThrow(
                        new AssetFileQuotaExceededException("File size 999 exceeds per-file limit 100", 999, 100));

        String input = "{\"filename\":\"runbook.md\",\"mimeType\":\"text/markdown\",\"content\":\"# runbook\"}";

        String result = callback.call(input, new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)));

        assertThat(result).contains("100")
            .contains("999")
            .doesNotContain("AssetFileQuotaExceededException");
    }
}
