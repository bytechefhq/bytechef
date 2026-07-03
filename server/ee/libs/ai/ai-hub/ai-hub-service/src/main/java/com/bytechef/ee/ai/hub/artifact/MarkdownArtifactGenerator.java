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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Persists a markdown report. The LLM has already streamed the content into chat as part of its tool-call argument;
 * this generator simply writes the final value to {@code asset_file} with {@code format=MARKDOWN} so the viewer routes
 * to the markdown render mode.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class MarkdownArtifactGenerator extends AbstractTextArtifactGenerator {

    public MarkdownArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubTaskAssetFileService taskAssetFileService) {

        super(assetFileFacade, taskAssetFileService);
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.MARKDOWN;
    }

    @Override
    protected String mimeType(GenerationRequest request) {
        return "text/markdown";
    }

    @Override
    protected String defaultExtension() {
        return "md";
    }
}
