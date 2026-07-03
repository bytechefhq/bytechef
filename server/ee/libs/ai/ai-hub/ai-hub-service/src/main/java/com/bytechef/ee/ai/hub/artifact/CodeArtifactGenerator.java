/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.task.AiHubTaskAssetFileService;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Persists a code file (Python, TypeScript, Java, etc.). Like markdown the LLM has already produced the final source,
 * the generator just writes it. Distinct from markdown because the viewer dispatches to the Monaco editor with language
 * detection rather than the markdown render — that distinction is what {@code format=CODE} encodes.
 *
 * <p>
 * MIME type is best-effort, derived from the filename extension. Unknown extensions fall back to {@code text/plain};
 * Monaco's own language detection then takes over on the client.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class CodeArtifactGenerator extends AbstractTextArtifactGenerator {

    /**
     * Extension → MIME-type lookup for the most common languages. Not exhaustive — that's intentional. A new entry here
     * only changes the {@code asset_file.mime_type} column; the viewer relies on Monaco's own language detection for
     * syntax highlighting, so a missing entry is at most a wrong content-type on download (recoverable). Adding every
     * language under the sun would tip this list into maintenance burden territory without a corresponding win.
     */
    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
        Map.entry("py", "text/x-python"),
        Map.entry("ts", "application/typescript"),
        Map.entry("tsx", "application/typescript"),
        Map.entry("js", "application/javascript"),
        Map.entry("jsx", "application/javascript"),
        Map.entry("java", "text/x-java-source"),
        Map.entry("kt", "text/x-kotlin"),
        Map.entry("rb", "text/x-ruby"),
        Map.entry("go", "text/x-go"),
        Map.entry("rs", "text/x-rust"),
        Map.entry("c", "text/x-c"),
        Map.entry("cpp", "text/x-c++"),
        Map.entry("h", "text/x-c"),
        Map.entry("hpp", "text/x-c++"),
        Map.entry("cs", "text/x-csharp"),
        Map.entry("php", "application/x-php"),
        Map.entry("sh", "application/x-sh"),
        Map.entry("sql", "application/sql"),
        Map.entry("html", "text/html"),
        Map.entry("css", "text/css"),
        Map.entry("yaml", "application/yaml"),
        Map.entry("yml", "application/yaml"),
        Map.entry("xml", "application/xml"));

    public CodeArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubTaskAssetFileService taskAssetFileService) {

        super(assetFileFacade, taskAssetFileService);
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.CODE;
    }

    @Override
    protected String mimeType(GenerationRequest request) {
        String filename = request.filename();
        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "text/plain";
        }

        String extension = filename.substring(dotIndex + 1)
            .toLowerCase();

        return EXTENSION_TO_MIME.getOrDefault(extension, "text/plain");
    }

    /**
     * Code intentionally has no default extension — the LLM must include one (.py, .ts, etc.) so the viewer can pick a
     * language for syntax highlighting. Returning null leaves the filename unchanged; if the filename is extension-less
     * the file still persists, just with {@code mime_type=text/plain}.
     */
    @Override
    protected String defaultExtension() {
        return null;
    }
}
