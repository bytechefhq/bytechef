/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.task.AiHubTaskAssetFileService;
import com.bytechef.ee.ai.hub.task.AuthorshipAlreadyAssignedException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared base for the text passthrough generators (markdown, code, csv, json). The LLM has already produced the final
 * content as part of its tool-call argument; the generator's only job is to persist it as an {@code asset_file} row,
 * link the row to the task via the {@code AUTHORED} join, and report back.
 *
 * <p>
 * The DOCX/PPTX/CHART generators do not extend this class — they have nontrivial materialisation steps (POI document
 * assembly, chart rendering) and live in their own classes. CSV and JSON also extend this for now since their
 * "structured output" path produces a final string before persistence; the schema validation step they layer on top
 * lives in their own subclasses.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public abstract class AbstractTextArtifactGenerator implements ArtifactGenerator {

    private static final Logger log = LoggerFactory.getLogger(AbstractTextArtifactGenerator.class);

    private final AssetFileFacade assetFileFacade;
    private final AiHubTaskAssetFileService taskAssetFileService;

    protected AbstractTextArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubTaskAssetFileService taskAssetFileService) {

        this.assetFileFacade = assetFileFacade;
        this.taskAssetFileService = taskAssetFileService;
    }

    @Override
    public final GenerationResult generate(GenerationRequest request) {
        validate(request);

        String resolvedFilename = ensureExtension(request.filename());
        String contentType = mimeType(request);
        String content = transformPayload(request);

        AssetFile saved = assetFileFacade.createFromAi(
            request.workspaceId(),
            request.environmentId(),
            resolvedFilename,
            contentType,
            content,
            format(),
            resolveMetadataJson(request),
            request.generatedByAgentSource(),
            request.generatedFromPrompt());

        boolean linked = false;
        Long taskId = request.taskId();

        if (taskId != null) {
            try {
                taskAssetFileService.recordAuthorship(taskId, saved.getId());
                linked = true;
            } catch (AuthorshipAlreadyAssignedException exception) {
                // The asset_file is already authored by a *different* task. The generator-supplied filename
                // resolution should have prevented collisions (resolveUniqueName is workspace-scoped) so this branch
                // really only fires on a TOCTOU race or a bug. Logging at WARN keeps the audit trail; the file was
                // persisted and is reachable by id, just not joined to *this* task.
                log.warn(
                    "Generated asset_file {} already has a different authoring task; skipping AUTHORED join "
                        + "for task {}",
                    saved.getId(), taskId, exception);
            }
        }

        return new GenerationResult(saved.getId(), saved.getName(), format(), linked);
    }

    /**
     * The MIME type to persist. Subclasses override to advertise a stable type that matches their format —
     * {@code text/markdown} for MARKDOWN, the language-derived type for CODE, etc. The default returns
     * {@code text/plain} so a partial implementation does not silently mis-label rows.
     */
    protected String mimeType(GenerationRequest request) {
        return "text/plain";
    }

    /**
     * Default extension for this format. Subclasses override; the default of {@code null} is a no-op, leaving the
     * filename unchanged. Used by {@link #ensureExtension(String)} so the LLM passing {@code "report"} produces
     * {@code "report.md"} for markdown.
     */
    protected String defaultExtension() {
        return null;
    }

    /**
     * Format-specific syntactic validation hook. Runs before the persist step so a malformed payload surfaces as a tool
     * error rather than landing in the user's workspace. Default no-op for formats whose payload is unstructured text
     * (markdown, code) — CSV/JSON override to enforce row consistency / well-formedness. Throws
     * {@link IllegalArgumentException} on a violation; the tool callback layer translates that into a structured error
     * message the LLM can recover from.
     */
    protected void validate(GenerationRequest request) {
    }

    /**
     * Optional payload transformation hook. Runs after validation but before the persist step. Subclasses override to
     * modify the content before it is written to the {@code asset_file} row — for example,
     * {@code HtmlArtifactGenerator} injects a Content-Security-Policy meta tag here. The default returns the payload
     * unmodified.
     */
    protected String transformPayload(GenerationRequest request) {
        return request.payload();
    }

    /**
     * Resolves the {@code metadata_json} value persisted on the {@code asset_file} row. Default is the caller-supplied
     * value from the request — used by markdown/code/csv/json which have no separate spec from their payload. The chart
     * generator overrides to return the payload itself so the viewer can read the chart spec from {@code metadata_json}
     * without re-parsing the file content.
     */
    protected String resolveMetadataJson(GenerationRequest request) {
        return request.metadataJson();
    }

    private String ensureExtension(String filename) {
        String ext = defaultExtension();

        if (ext == null || ext.isBlank()) {
            return filename;
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            // Caller already supplied an extension. Trust it — overriding here would let a CSV-format request that
            // names itself ".tsv" land as ".tsv.csv" which is more confusing than the rare mismatch.
            return filename;
        }

        String trimmed = ext.startsWith(".") ? ext.substring(1) : ext;

        return filename + '.' + trimmed;
    }

    /**
     * Format identity is declared by the concrete subclass. Defined as an abstract method on the
     * {@link ArtifactGenerator} interface; redeclared here as a reminder that every subclass needs a unique format.
     */
    @Override
    public abstract AssetFileFormat format();
}
