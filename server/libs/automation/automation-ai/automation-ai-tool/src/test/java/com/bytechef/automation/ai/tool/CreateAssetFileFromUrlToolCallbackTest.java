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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class CreateAssetFileFromUrlToolCallbackTest {

    private static final Set<String> ALLOWED_TEST_HOSTS = Set.of("files.example.com");

    private final AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);

    @Test
    void testGetToolDefinitionName() {
        CreateAssetFileFromUrlToolCallback toolCallback =
            new CreateAssetFileFromUrlToolCallback(assetFileFacade, null);

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("createAssetFileFromUrl");
    }

    @Test
    void testCallStoresTextResponseThroughCreateFromAi() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(7L);
        assetFile.setName("report.csv");

        when(assetFileFacade.createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(assetFile);

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/csv; charset=utf-8", "id,name\n1,alpha\n".getBytes(StandardCharsets.UTF_8)),
            64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/report.csv\"}", workspaceToolContext());

        assertThat(result).contains("\"id\":7")
            .contains("report.csv");

        verify(assetFileFacade).createFromAi(
            eq(1L), anyInt(), eq("report.csv"), eq("text/csv"), eq("id,name\n1,alpha\n"), any(), any(), any(),
            any());
    }

    @Test
    void testCallStoresBinaryResponseThroughCreateBinaryFromAi() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(9L);
        assetFile.setName("chart.png");

        when(assetFileFacade.createBinaryFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(assetFile);

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "image/png", new byte[] {
                1, 2, 3
            }), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/chart.png\"}", workspaceToolContext());

        assertThat(result).contains("\"id\":9");

        verify(assetFileFacade).createBinaryFromAi(
            eq(1L), anyInt(), eq("chart.png"), eq("image/png"), eq(new byte[] {
                1, 2, 3
            }), any(), any(), any(), any());
    }

    @Test
    void testCallHonoursExplicitFilename() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(11L);
        assetFile.setName("notes.md");

        when(assetFileFacade.createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(assetFile);

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/markdown", "# hi".getBytes(StandardCharsets.UTF_8)), 64 * 1024 * 1024);

        toolCallback.call(
            "{\"url\":\"https://files.example.com/raw\",\"filename\":\"notes.md\"}", workspaceToolContext());

        verify(assetFileFacade).createFromAi(
            anyLong(), anyInt(), eq("notes.md"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCallRejectsPrivateAddress() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/plain", "secret".getBytes(StandardCharsets.UTF_8)), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"http://127.0.0.1:8080/admin\"}", workspaceToolContext());

        // The specific SSRF reason (loopback / CGNAT / unresolvable host / …) must NOT reach the model — that
        // would let a caller enumerate internal hostnames one refusal at a time.
        assertThat(result).contains(CreateAssetFileFromUrlToolCallback.REFUSED_URL_MESSAGE)
            .doesNotContainIgnoringCase("loopback");

        verify(assetFileFacade, never()).createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testProductionConstructorRejectsPrivateAddress() {
        // Uses the real (public) constructor and its real HttpClient — a literal IP address needs no DNS lookup,
        // so UrlValidator refuses it before any network call is made, keeping this deterministic and offline.
        CreateAssetFileFromUrlToolCallback toolCallback =
            new CreateAssetFileFromUrlToolCallback(assetFileFacade, null);

        String result = toolCallback.call(
            "{\"url\":\"http://127.0.0.1:8080/admin\"}", workspaceToolContext());

        assertThat(result).contains(CreateAssetFileFromUrlToolCallback.REFUSED_URL_MESSAGE)
            .doesNotContainIgnoringCase("loopback");

        verify(assetFileFacade, never()).createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCallChecksWorkspaceContextBeforeUrlValidation() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/plain", "secret".getBytes(StandardCharsets.UTF_8)), 64 * 1024 * 1024);

        // No workspace context AND a private-address URL: if URL validation ran first this would return the
        // generic refusal message instead, silently skipping the workspace check.
        String result = toolCallback.call(
            "{\"url\":\"http://127.0.0.1:8080/admin\"}", new ToolContext(Map.of()));

        assertThat(result).contains("Workspace context unavailable");
    }

    @Test
    void testCallSurfacesQuotaLimitInsteadOfOpaqueExceptionName() throws Exception {
        when(assetFileFacade.createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AssetFileQuotaExceededException("File size 999 exceeds per-file limit 100", 999, 100));

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/csv", "id,name\n1,alpha\n".getBytes(StandardCharsets.UTF_8)), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/report.csv\"}", workspaceToolContext());

        assertThat(result).contains("100")
            .contains("999")
            .doesNotContain("AssetFileQuotaExceededException");
    }

    @Test
    void testResolveMaxDownloadBytesUsesFacadeQuotaWhenSmallerThanCeiling() {
        AssetFileFacade smallQuotaFacade = mock(AssetFileFacade.class);

        when(smallQuotaFacade.getMaxFileSizeBytes()).thenReturn(1024L);

        assertThat(CreateAssetFileFromUrlToolCallback.resolveMaxDownloadBytes(smallQuotaFacade)).isEqualTo(1024);
    }

    @Test
    void testResolveMaxDownloadBytesFallsBackToCeilingWhenQuotaIsUnlimited() {
        AssetFileFacade unlimitedQuotaFacade = mock(AssetFileFacade.class);

        when(unlimitedQuotaFacade.getMaxFileSizeBytes()).thenReturn(-1L);

        assertThat(CreateAssetFileFromUrlToolCallback.resolveMaxDownloadBytes(unlimitedQuotaFacade))
            .isEqualTo(CreateAssetFileFromUrlToolCallback.MAX_DOWNLOAD_BYTES);
    }

    @Test
    void testResolveMaxDownloadBytesFallsBackToCeilingWhenQuotaExceedsIt() {
        AssetFileFacade largeQuotaFacade = mock(AssetFileFacade.class);

        when(largeQuotaFacade.getMaxFileSizeBytes()).thenReturn(Long.MAX_VALUE);

        assertThat(CreateAssetFileFromUrlToolCallback.resolveMaxDownloadBytes(largeQuotaFacade))
            .isEqualTo(CreateAssetFileFromUrlToolCallback.MAX_DOWNLOAD_BYTES);
    }

    @Test
    void testDefaultHttpClientNeverFollowsRedirects() {
        // Asserted directly on the object the production constructor actually builds — asserting only via a
        // mocked HttpClient (as the call()-level redirect test below does) would stay green even if this flipped
        // to Redirect.ALWAYS, since the mock never consults the real setting.
        HttpClient httpClient = CreateAssetFileFromUrlToolCallback.newDefaultHttpClient();

        assertThat(httpClient.followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
    }

    @Test
    void testCallRejectsUnsupportedMimeType() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "application/x-msdownload", new byte[] {
                4
            }), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/setup.exe\"}", workspaceToolContext());

        assertThat(result).contains("Unsupported mime type");
    }

    @Test
    void testCallRejectsOversizePayload() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/plain", "abcdefghij".getBytes(StandardCharsets.UTF_8)), 4);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/big.txt\"}", workspaceToolContext());

        assertThat(result).contains("exceeds");
    }

    @Test
    void testCallRejectsInvalidUtf8DeclaredAsText() throws Exception {
        byte[] invalidUtf8 = {
            (byte) 0xFF, (byte) 0xFE, 0x00
        };

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/plain", invalidUtf8), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/bad.txt\"}", workspaceToolContext());

        assertThat(result).contains("not valid UTF-8");

        verify(assetFileFacade, never()).createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCallRejectsSlowDripPastReadDeadline() throws Exception {
        // A response that sends a single chunk after sleeping past a millisecond-scale deadline reproduces the
        // "slow drip" failure mode the deadline guards against, without the test actually waiting out the real
        // 60-second production value. The deadline is checked only after read() returns, so one slow read is
        // enough to trip it.
        HttpResponse<InputStream> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(new SlowInputStream("partial".getBytes(StandardCharsets.UTF_8), 200));
        when(response.headers()).thenReturn(
            HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (name, value) -> true));

        HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn((HttpResponse) response);

        CreateAssetFileFromUrlToolCallback toolCallback = new CreateAssetFileFromUrlToolCallback(
            assetFileFacade, null, httpClient, 64 * 1024 * 1024, ALLOWED_TEST_HOSTS, Duration.ofMillis(10));

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/slow.txt\"}", workspaceToolContext());

        assertThat(result).contains("10ms read deadline");

        verify(assetFileFacade, never()).createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCallRejectsRedirect() throws Exception {
        HttpResponse<InputStream> response = stubResponse(302, "text/plain", new byte[0]);

        when(response.headers()).thenReturn(
            HttpHeaders.of(Map.of("location", List.of("https://elsewhere.example.com/x")), (name, value) -> true));

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(response, 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/moved\"}", workspaceToolContext());

        assertThat(result).contains("redirect");
    }

    @Test
    void testCallWithoutWorkspaceContextReturnsError() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/plain", "hello".getBytes(StandardCharsets.UTF_8)), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/a.txt\"}", new ToolContext(Map.of()));

        assertThat(result).contains("Workspace context unavailable");
    }

    @Test
    void testCallRecordsTextArtifactKind() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(21L);
        assetFile.setName("report.csv");

        when(assetFileFacade.createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(assetFile);

        ToolArtifactRecorder artifactRecorder = mock(ToolArtifactRecorder.class);

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            artifactRecorder,
            stubResponse(200, "text/csv", "id,name\n1,alpha\n".getBytes(StandardCharsets.UTF_8)),
            64 * 1024 * 1024);

        toolCallback.call(
            "{\"url\":\"https://files.example.com/report.csv\"}", workspaceToolContextWithThreadId("thread-1"));

        verify(artifactRecorder).record(eq("thread-1"), any(), eq("FILE_CREATED"), eq("21"), eq("report.csv"));
    }

    @Test
    void testCallRecordsBinaryArtifactKind() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(22L);
        assetFile.setName("chart.png");

        when(assetFileFacade.createBinaryFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(assetFile);

        ToolArtifactRecorder artifactRecorder = mock(ToolArtifactRecorder.class);

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            artifactRecorder,
            stubResponse(200, "image/png", new byte[] {
                1, 2, 3
            }), 64 * 1024 * 1024);

        toolCallback.call(
            "{\"url\":\"https://files.example.com/chart.png\"}", workspaceToolContextWithThreadId("thread-2"));

        verify(artifactRecorder).record(eq("thread-2"), any(), eq("BINARY_FILE_CREATED"), eq("22"), eq("chart.png"));
    }

    private CreateAssetFileFromUrlToolCallback newToolCallback(
        HttpResponse<InputStream> response, int maxDownloadBytes) throws Exception {

        return newToolCallback(null, response, maxDownloadBytes);
    }

    @SuppressWarnings("unchecked")
    private CreateAssetFileFromUrlToolCallback newToolCallback(
        ToolArtifactRecorder artifactRecorder, HttpResponse<InputStream> response, int maxDownloadBytes)
        throws Exception {

        HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn((HttpResponse) response);

        return new CreateAssetFileFromUrlToolCallback(
            assetFileFacade, artifactRecorder, httpClient, maxDownloadBytes, ALLOWED_TEST_HOSTS);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<InputStream> stubResponse(int statusCode, String contentType, byte[] body)
        throws IOException {

        HttpResponse<InputStream> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(new ByteArrayInputStream(body));
        when(response.headers()).thenReturn(
            HttpHeaders.of(Map.of("content-type", List.of(contentType)), (name, value) -> true));

        return response;
    }

    private ToolContext workspaceToolContext() {
        return new ToolContext(
            Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 1L));
    }

    private ToolContext workspaceToolContextWithThreadId(String threadId) {
        return new ToolContext(
            Map.of(
                AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 1L,
                AutomationToolInvocationContext.TOOL_CONTEXT_THREAD_ID_KEY, threadId));
    }

    /**
     * An {@link InputStream} whose first (and only) chunk read sleeps {@code sleepMillis} before returning, so a test
     * can trip the read-deadline guard deterministically instead of actually waiting out a real slow-drip response.
     */
    private static final class SlowInputStream extends InputStream {

        private final byte[] data;
        private final long sleepMillis;
        private boolean served;

        private SlowInputStream(byte[] data, long sleepMillis) {
            this.data = data;
            this.sleepMillis = sleepMillis;
        }

        @Override
        public int read(byte[] destination, int offset, int length) throws IOException {
            if (served) {
                return -1;
            }

            served = true;

            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread()
                    .interrupt();

                throw new IOException("Interrupted while simulating a slow response", exception);
            }

            int copyLength = Math.min(length, data.length);

            System.arraycopy(data, 0, destination, offset, copyLength);

            return copyLength;
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException("readBounded() always reads via the byte[] overload");
        }
    }
}
