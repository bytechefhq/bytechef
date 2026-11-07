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

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidatorUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @author Ivica Cardic
 */
public class CreateAssetFileFromUrlToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CreateAssetFileFromUrlToolCallback.class);

    static final int MAX_DOWNLOAD_BYTES = 64 * 1024 * 1024;

    static final String REFUSED_URL_MESSAGE = "Refused: the URL must be a public http(s) address";

    static final Duration DEFAULT_READ_DEADLINE = Duration.ofSeconds(60);

    private static final Set<String> TEXT_MIME_TYPES = Set.of(
        "text/markdown", "text/csv", "text/plain", "application/json",
        "text/javascript", "text/x-python", "text/x-java",
        "text/html", "text/css", "text/yaml", "application/xml");

    private static final Set<String> BINARY_MIME_TYPES = Set.of(
        "image/png", "image/jpeg", "image/webp", "image/gif",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private static final String DESCRIPTION = """
        Download a file from a public http(s) URL and save it into the user's workspace files. Use this
        when the user asks you to fetch, grab, or import something from a link — the bytes are downloaded
        on the server, so large files cost nothing in this conversation. Supported content types are the
        text formats (markdown, csv, plain, json, xml, html, css, yaml, and source code) plus png, jpeg,
        webp, gif, pdf, and pptx. Text content must be UTF-8 encoded; any charset parameter on the
        Content-Type header is ignored, so a text file served as a non-UTF-8 charset (e.g. windows-1252)
        is refused rather than mis-decoded. URLs that redirect are refused: read the error and retry with
        the final URL. Private, loopback, and internal addresses are refused.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "url": {"type": "string", "description": "Public http or https URL to download"},
                    "filename": {"type": "string", "description": "Optional filename override; defaults to the last URL path segment"},
                    "description": {"type": "string", "description": "Optional short description"}
                },
                "required": ["url"]
            }""";

    private final AssetFileFacade facade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;
    private final HttpClient httpClient;
    private final int maxDownloadBytes;
    private final Set<String> allowedHosts;
    private final Duration readDeadline;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateAssetFileFromUrlToolCallback(
        AssetFileFacade facade, @Nullable ToolArtifactRecorder artifactRecorder) {

        this(
            facade, artifactRecorder, newDefaultHttpClient(), resolveMaxDownloadBytesSafely(facade), Set.of(),
            DEFAULT_READ_DEADLINE);
    }

    static int resolveMaxDownloadBytes(AssetFileFacade facade) {
        long configuredLimit = facade.getMaxFileSizeBytes();

        if (configuredLimit < 0) {
            return MAX_DOWNLOAD_BYTES;
        }

        return (int) Math.min(MAX_DOWNLOAD_BYTES, configuredLimit);
    }

    private static int resolveMaxDownloadBytesSafely(AssetFileFacade facade) {
        try {
            return resolveMaxDownloadBytes(facade);
        } catch (UnsupportedOperationException exception) {
            log.warn(
                "AssetFileFacade#getMaxFileSizeBytes() is unsupported by {} — falling back to the {} byte "
                    + "memory-safety ceiling for createAssetFileFromUrl downloads instead of the facade's "
                    + "configured per-file quota",
                facade.getClass()
                    .getName(),
                MAX_DOWNLOAD_BYTES);

            return MAX_DOWNLOAD_BYTES;
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    CreateAssetFileFromUrlToolCallback(
        AssetFileFacade facade, @Nullable ToolArtifactRecorder artifactRecorder, HttpClient httpClient,
        int maxDownloadBytes, Set<String> allowedHosts) {

        this(facade, artifactRecorder, httpClient, maxDownloadBytes, allowedHosts, DEFAULT_READ_DEADLINE);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    CreateAssetFileFromUrlToolCallback(
        AssetFileFacade facade, @Nullable ToolArtifactRecorder artifactRecorder, HttpClient httpClient,
        int maxDownloadBytes, Set<String> allowedHosts, Duration readDeadline) {

        this.facade = facade;
        this.artifactRecorder = artifactRecorder;
        this.httpClient = httpClient;
        this.maxDownloadBytes = maxDownloadBytes;
        this.allowedHosts = Set.copyOf(allowedHosts);
        this.readDeadline = readDeadline;
    }

    static HttpClient newDefaultHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createAssetFileFromUrl")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            CreateAssetFileFromUrlInput input = JsonUtils.read(toolInput, CreateAssetFileFromUrlInput.class);

            String url = input.url();

            if (url == null || url.isBlank()) {
                return toolError("url is required");
            }

            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            try {
                UrlValidatorUtils.validate(url, allowedHosts);
            } catch (UrlValidationException exception) {
                log.warn("createAssetFileFromUrl refused url='{}': {}", url, exception.getMessage());

                return toolError(REFUSED_URL_MESSAGE);
            }

            URI uri;

            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException exception) {
                return toolError("Malformed url: " + exception.getMessage());
            }

            DownloadResult downloadResult = download(uri);

            if (downloadResult.errorMessage() != null) {
                return toolError(downloadResult.errorMessage());
            }

            String mimeType = downloadResult.mimeType();
            boolean textMimeType = downloadResult.textMimeType();
            byte[] data = downloadResult.data();

            String filename = resolveFilename(input.filename(), uri);
            int environment = AutomationToolInvocationContext.resolveEnvironmentOrDefault(invocationContext);

            AssetFile created;

            if (textMimeType) {
                String content;

                try {
                    content = decodeStrictUtf8(data);
                } catch (CharacterCodingException exception) {
                    return toolError(
                        "Declared mime type %s but the downloaded bytes are not valid UTF-8 text"
                            .formatted(mimeType));
                }

                created = facade.createFromAi(
                    workspaceId, environment, filename, mimeType, content, null, null,
                    invocationContext.sourceOrdinal(), invocationContext.lastUserPrompt());
            } else {
                created = facade.createBinaryFromAi(
                    workspaceId, environment, filename, mimeType, data, null, null,
                    invocationContext.sourceOrdinal(), invocationContext.lastUserPrompt());
            }

            String threadId = invocationContext.threadId();

            if (artifactRecorder != null && threadId != null) {
                String artifactKind = textMimeType ? "FILE_CREATED" : "BINARY_FILE_CREATED";

                artifactRecorder.record(
                    threadId, invocationContext.userId(), artifactKind, String.valueOf(created.getId()),
                    created.getName());
            }

            return JsonUtils.write(
                new CreateAssetFileFromUrlOutput(
                    created.getId(),
                    created.getName(),
                    "/api/automation/internal/asset-files/%d/content".formatted(created.getId()),
                    created.getSizeBytes()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IOException exception) {
            return toolError("Download failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            return toolError("Download was interrupted");
        } catch (AssetFileQuotaExceededException exception) {
            return toolError(
                "Download exceeds the allowed file size limit of " + exception.getLimit()
                    + " bytes (attempted " + exception.getAttempted() + " bytes)");
        } catch (RuntimeException exception) {
            log.warn("createAssetFileFromUrl failed: {}", exception.toString(), exception);

            return toolError("createAssetFileFromUrl failed (" + exception.getClass()
                .getSimpleName() + ")");
        }
    }

    private DownloadResult download(URI uri) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<InputStream> response =
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        int statusCode = response.statusCode();

        try (InputStream inputStream = response.body()) {
            if (statusCode >= 300 && statusCode < 400) {
                String location = response.headers()
                    .firstValue("location")
                    .orElse("(no Location header)");

                return DownloadResult.error(
                    "The URL issued a redirect to %s. Redirects are not followed; retry with the final URL."
                        .formatted(location));
            }

            if (statusCode < 200 || statusCode >= 300) {
                return DownloadResult.error("Download failed with HTTP status " + statusCode);
            }

            Optional<String> declaredLength = response.headers()
                .firstValue("content-length");

            if (declaredLength.isPresent()) {
                long declaredBytes;

                try {
                    declaredBytes = Long.parseLong(declaredLength.get());
                } catch (NumberFormatException exception) {
                    declaredBytes = -1L;
                }

                if (declaredBytes > maxDownloadBytes) {
                    return DownloadResult.error(
                        "Download exceeds the maximum allowed size of " + maxDownloadBytes + " bytes");
                }
            }

            String mimeType = response.headers()
                .firstValue("content-type")
                .map(CreateAssetFileFromUrlToolCallback::normalizeMimeType)
                .orElse("application/octet-stream");

            boolean textMimeType = TEXT_MIME_TYPES.contains(mimeType);

            if (!textMimeType && !BINARY_MIME_TYPES.contains(mimeType)) {
                return DownloadResult.error("Unsupported mime type: %s".formatted(mimeType));
            }

            byte[] data = readBounded(inputStream, maxDownloadBytes, readDeadline);

            if (data == null) {
                return DownloadResult.error(
                    "Download exceeds the maximum allowed size of " + maxDownloadBytes + " bytes");
            }

            return DownloadResult.success(mimeType, textMimeType, data);
        }
    }

    private static String decodeStrictUtf8(byte[] data) throws CharacterCodingException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(data));

        return charBuffer.toString();
    }

    private static byte @Nullable [] readBounded(InputStream inputStream, int maxBytes, Duration readDeadline)
        throws IOException {

        long deadlineNanos = System.nanoTime() + readDeadline.toNanos();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long total = 0;
        int bytesRead;

        while ((bytesRead = inputStream.read(chunk)) != -1) {
            total += bytesRead;

            if (total > maxBytes) {
                return null;
            }

            buffer.write(chunk, 0, bytesRead);

            if (System.nanoTime() - deadlineNanos > 0) {
                throw new IOException(
                    "Download exceeded the " + readDeadline.toMillis() + "ms read deadline");
            }
        }

        return buffer.toByteArray();
    }

    private static String normalizeMimeType(String contentTypeHeader) {
        int separatorIndex = contentTypeHeader.indexOf(';');
        String mimeType = separatorIndex < 0 ? contentTypeHeader : contentTypeHeader.substring(0, separatorIndex);

        return mimeType.trim()
            .toLowerCase(Locale.ROOT);
    }

    private static String resolveFilename(@Nullable String requestedFilename, URI uri) {
        if (requestedFilename != null && !requestedFilename.isBlank()) {
            return requestedFilename.trim();
        }

        String path = uri.getPath();

        if (path != null) {
            int slashIndex = path.lastIndexOf('/');
            String lastSegment = slashIndex < 0 ? path : path.substring(slashIndex + 1);

            if (!lastSegment.isBlank()) {
                return lastSegment;
            }
        }

        return "download";
    }

    private String toolError(String message) {
        try {
            return JsonUtils.write(Map.of("error", message));
        } catch (JacksonException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateAssetFileFromUrlInput(String url, String filename, String description) {
    }

    public record CreateAssetFileFromUrlOutput(long id, String name, String downloadUrl, long sizeBytes) {
    }

    private record DownloadResult(@Nullable String errorMessage, String mimeType, boolean textMimeType, byte[] data) {

        static DownloadResult error(String errorMessage) {
            return new DownloadResult(errorMessage, "", false, new byte[0]);
        }

        static DownloadResult success(String mimeType, boolean textMimeType, byte[] data) {
            return new DownloadResult(null, mimeType, textMimeType, data);
        }
    }
}
