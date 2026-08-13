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
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class CreateBinaryAssetFileToolCallbackTest {

    @Mock
    private AssetFileFacade facade;

    private CreateBinaryAssetFileToolCallback callback;

    @BeforeEach
    void setUp() {
        callback = new CreateBinaryAssetFileToolCallback(facade);
    }

    @Test
    void testToolDefinitionExposesCreateBinaryAssetFileName() {
        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("createBinaryAssetFile");
    }

    @Test
    void testCallSavesBinaryAndReturnsAssetMetadata() {
        byte[] data = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        String base64 = Base64.getEncoder()
            .encodeToString(data);
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 5L,
            AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 1,
            AutomationToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, "generate a logo"));

        AssetFile saved = new AssetFile();

        saved.setId(99L);
        saved.setName("logo.png");
        saved.setSizeBytes(data.length);

        when(
            facade.createBinaryFromAi(
                eq(5L), anyInt(), eq("logo.png"), eq("image/png"), eq(data),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq((short) 1), eq("generate a logo")))
                    .thenReturn(saved);

        String input = "{\"filename\":\"logo.png\",\"mimeType\":\"image/png\",\"base64Content\":\"%s\"}"
            .formatted(base64);

        String result = callback.call(input, toolContext);

        assertThat(result).contains("\"id\":99");
        assertThat(result).contains("\"name\":\"logo.png\"");
        assertThat(result).contains("\"sizeBytes\":%d".formatted(data.length));
        assertThat(result).contains("downloadUrl");
    }

    @Test
    void testCallReturnsErrorOnDisallowedMimeType() {
        String input =
            "{\"filename\":\"archive.zip\",\"mimeType\":\"application/zip\",\"base64Content\":\"dGVzdA==\"}";

        String result = callback.call(input);

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("mime");

        verify(facade, never()).createBinaryFromAi(
            org.mockito.ArgumentMatchers.any(), anyInt(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testCallSavesPdfBinaryAndReturnsAssetMetadata() {
        byte[] data = new byte[] {
            0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34
        };
        String base64 = Base64.getEncoder()
            .encodeToString(data);
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 5L,
            AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 1,
            AutomationToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, "save the report"));

        AssetFile saved = new AssetFile();

        saved.setId(101L);
        saved.setName("report.pdf");
        saved.setSizeBytes(data.length);

        when(
            facade.createBinaryFromAi(
                eq(5L), anyInt(), eq("report.pdf"), eq("application/pdf"), eq(data),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq((short) 1), eq("save the report")))
                    .thenReturn(saved);

        String input = "{\"filename\":\"report.pdf\",\"mimeType\":\"application/pdf\",\"base64Content\":\"%s\"}"
            .formatted(base64);

        String result = callback.call(input, toolContext);

        assertThat(result).contains("\"id\":101");
        assertThat(result).contains("\"name\":\"report.pdf\"");
        assertThat(result).contains("\"sizeBytes\":%d".formatted(data.length));
    }

    @Test
    void testCallSavesGifBinaryAndReturnsAssetMetadata() {
        byte[] data = new byte[] {
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61
        };
        String base64 = Base64.getEncoder()
            .encodeToString(data);
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 5L,
            AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 1,
            AutomationToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, "attach a gif"));

        AssetFile saved = new AssetFile();

        saved.setId(102L);
        saved.setName("celebration.gif");
        saved.setSizeBytes(data.length);

        when(
            facade.createBinaryFromAi(
                eq(5L), anyInt(), eq("celebration.gif"), eq("image/gif"), eq(data),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq((short) 1), eq("attach a gif")))
                    .thenReturn(saved);

        String input = "{\"filename\":\"celebration.gif\",\"mimeType\":\"image/gif\",\"base64Content\":\"%s\"}"
            .formatted(base64);

        String result = callback.call(input, toolContext);

        assertThat(result).contains("\"id\":102");
        assertThat(result).contains("\"name\":\"celebration.gif\"");
        assertThat(result).contains("\"sizeBytes\":%d".formatted(data.length));
    }

    @Test
    void testCallRejectsOversizeBase64BeforeDecoding() {
        // Regression guard: a hallucinating model could try to push a multi-hundred-MB base64 payload that
        // decodes to enough bytes to exhaust the JVM heap. The 64 MiB pre-decode bound is a hard floor and
        // MUST NOT be moved after Base64.decode() — this test pins both the rejection and the fact that the
        // facade is NEVER invoked. We use an internal callback whose bound is overridden to a small value so
        // the test can drive the rejection path without allocating multi-megabyte buffers.
        CreateBinaryAssetFileToolCallback smallBoundCallback =
            new CreateBinaryAssetFileToolCallback(facade, null, /* maxBinaryBytes */ 6);

        // 6 raw bytes -> max base64 length is 6 * 4 / 3 = 8. "AAAAAAAAA" (9 chars) exceeds the bound and
        // must be rejected pre-decode.
        String input =
            "{\"filename\":\"big.png\",\"mimeType\":\"image/png\",\"base64Content\":\"AAAAAAAAA\"}";

        String result = smallBoundCallback.call(input);

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("maximum allowed size");

        verify(facade, never()).createBinaryFromAi(
            org.mockito.ArgumentMatchers.any(), anyInt(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testOversizeRejectionFiresBeforeDecodeAttempt() {
        // Stronger than testCallRejectsOversizeBase64BeforeDecoding: feed a string that exceeds the bound AND
        // is not valid base64. If the size check fires first (correct), the result is the
        // "maximum allowed size" error. If a regression moves the size check after Base64.decode(), the
        // decoder would throw IllegalArgumentException first and the result would be the "invalid base64"
        // error instead. The error string is therefore the canary for check ordering.
        CreateBinaryAssetFileToolCallback smallBoundCallback =
            new CreateBinaryAssetFileToolCallback(facade, null, /* maxBinaryBytes */ 6);

        // 9 chars > 8 (the pre-decode max for a 6-byte cap) AND contains '!' which would cause
        // Base64.getDecoder().decode() to throw IllegalArgumentException.
        String input =
            "{\"filename\":\"big.png\",\"mimeType\":\"image/png\",\"base64Content\":\"!!!!!!!!!\"}";

        String result = smallBoundCallback.call(input);

        assertThat(result).containsIgnoringCase("maximum allowed size");
        assertThat(result).doesNotContainIgnoringCase("invalid base64");
    }

    @Test
    void testMaxBinaryBytesConstantPinnedAt64MiB() {
        // This test pins the literal cap. Without it, a refactor that quietly lowered
        // MAX_BINARY_BYTES to (say) 1 MiB would still pass testCallRejectsOversizeBase64BeforeDecoding
        // because that test injects its own small bound. The cap is a documented security boundary
        // (review C5: pre-decode rejection of multi-hundred-MB base64) and changes to it must be
        // visible to anyone reading the test history.
        //
        // Combined-coverage contract: this test pins the *value* of MAX_BINARY_BYTES at 64 MiB;
        // testBoundaryAtExactlyMaxBytes / testBoundaryJustOverMaxBytes pin the *inequality direction*
        // (using bound=6 to keep allocations tiny — the inequality predicate is the same code path
        // regardless of the specific bound value). A regression that altered EITHER would fail at least
        // one of these tests. We deliberately do not allocate ~85 MiB to test "just over the literal cap"
        // — that would burn ~300 MiB of heap during the test for an assertion the small-bound tests
        // already cover via the same predicate.
        assertThat(CreateBinaryAssetFileToolCallback.MAX_BINARY_BYTES).isEqualTo(64 * 1024 * 1024);
    }

    @Test
    void testBoundaryAtExactlyMaxBytes() {
        // Boundary test: a payload that decodes to EXACTLY the cap must succeed. A regression that flips the
        // comparison from `>` to `>=` (or `<=` to `<`) on the pre-decode size check would cause this test to
        // fail with the "maximum allowed size" error instead of reaching the facade.
        int bound = 6;
        byte[] data = new byte[bound];

        // Deterministic content so the assertion below can match exactly.
        for (int i = 0; i < bound; i++) {
            data[i] = (byte) i;
        }

        String base64 = Base64.getEncoder()
            .encodeToString(data); // 6 bytes -> 8 base64 chars, well within bound math.
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 5L,
            AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 1));

        AssetFile saved = new AssetFile();

        saved.setId(201L);
        saved.setName("at-cap.png");
        saved.setSizeBytes(bound);

        when(
            facade.createBinaryFromAi(
                eq(5L), anyInt(), eq("at-cap.png"), eq("image/png"), eq(data),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq((short) 1), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(saved);

        CreateBinaryAssetFileToolCallback smallBoundCallback =
            new CreateBinaryAssetFileToolCallback(facade, null, /* maxBinaryBytes */ bound);

        String input = ("{\"filename\":\"at-cap.png\","
            + "\"mimeType\":\"image/png\","
            + "\"base64Content\":\"%s\"}").formatted(base64);

        String result = smallBoundCallback.call(input, toolContext);

        assertThat(result).contains("\"id\":201");
        assertThat(result).doesNotContainIgnoringCase("maximum allowed size");
    }

    @Test
    void testBoundaryJustOverMaxBytes() {
        // Mirror of testBoundaryAtExactlyMaxBytes: a payload one byte over the cap MUST be rejected. Together
        // these two pin the inequality direction at the documented cap rather than relying on the small-bound
        // override only seeing "much smaller" vs "much larger".
        int bound = 6;
        byte[] data = new byte[bound + 1];

        String base64 = Base64.getEncoder()
            .encodeToString(data);

        CreateBinaryAssetFileToolCallback smallBoundCallback =
            new CreateBinaryAssetFileToolCallback(facade, null, /* maxBinaryBytes */ bound);

        String input = ("{\"filename\":\"over-cap.png\","
            + "\"mimeType\":\"image/png\","
            + "\"base64Content\":\"%s\"}").formatted(base64);

        String result = smallBoundCallback.call(input);

        assertThat(result).containsIgnoringCase("maximum allowed size");

        verify(facade, never()).createBinaryFromAi(
            org.mockito.ArgumentMatchers.any(), anyInt(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testCallReturnsErrorOnInvalidBase64() {
        String input =
            "{\"filename\":\"image.png\",\"mimeType\":\"image/png\",\"base64Content\":\"!!!not-base64!!!\"}";

        String result = callback.call(input);

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("base64");

        verify(facade, never()).createBinaryFromAi(
            org.mockito.ArgumentMatchers.any(), anyInt(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testCallSurfacesQuotaLimitInsteadOfOpaqueExceptionName() {
        byte[] data = new byte[] {
            1, 2, 3, 4
        };
        String base64 = Base64.getEncoder()
            .encodeToString(data);

        when(
            facade.createBinaryFromAi(
                org.mockito.ArgumentMatchers.any(), anyInt(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                    .thenThrow(
                        new AssetFileQuotaExceededException("File size 999 exceeds per-file limit 100", 999, 100));

        String input = "{\"filename\":\"banner.png\",\"mimeType\":\"image/png\",\"base64Content\":\"%s\"}"
            .formatted(base64);

        String result = callback.call(input, new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 5L)));

        assertThat(result).contains("100")
            .contains("999")
            .doesNotContain("AssetFileQuotaExceededException");
    }

    @Test
    void testCallPrefersToolContextOverProvider() {
        byte[] data = new byte[] {
            1, 2, 3, 4
        };
        String base64 = Base64.getEncoder()
            .encodeToString(data);

        AssetFile saved = new AssetFile();

        saved.setId(7L);
        saved.setName("banner.png");
        saved.setSizeBytes(data.length);

        when(
            facade.createBinaryFromAi(
                eq(42L), anyInt(), eq("banner.png"), eq("image/png"), eq(data),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq((short) 3), eq("tool-context prompt")))
                    .thenReturn(saved);

        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L,
            AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 3,
            AutomationToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, "tool-context prompt"));

        String input = "{\"filename\":\"banner.png\",\"mimeType\":\"image/png\",\"base64Content\":\"%s\"}"
            .formatted(base64);

        String result = callback.call(input, toolContext);

        assertThat(result).contains("\"id\":7");

    }
}
