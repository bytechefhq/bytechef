/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ArtifactGeneratorRegistryTest {

    @Test
    void testGenerateDispatchesToFormatMatchingGenerator() {
        StubGenerator markdown = new StubGenerator(AssetFileFormat.MARKDOWN, 1L);
        StubGenerator code = new StubGenerator(AssetFileFormat.CODE, 2L);

        ArtifactGeneratorRegistry registry = new ArtifactGeneratorRegistry(List.of(markdown, code));

        GenerationRequest request = new GenerationRequest(
            1L, 10L, 0, null, (short) 0, null, "report.md", "# hello", null);

        GenerationResult result = registry.generate(AssetFileFormat.MARKDOWN, request);

        assertThat(result.assetFileId()).isEqualTo(1L);
        assertThat(markdown.invocations).isEqualTo(1);
        assertThat(code.invocations).isEqualTo(0);
    }

    @Test
    void testGenerateThrowsWhenNoGeneratorRegistered() {
        // Probe-oracle defense: a tool input requesting an unsupported format must fail with a structured error
        // rather than silently no-oping or throwing an opaque NPE — the LLM needs a typed signal it can use to
        // suggest a different format.
        ArtifactGeneratorRegistry registry = new ArtifactGeneratorRegistry(List.of());

        GenerationRequest request = new GenerationRequest(
            1L, 10L, 0, null, (short) 0, null, "report.md", "x", null);

        assertThatThrownBy(() -> registry.generate(AssetFileFormat.MARKDOWN, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No ArtifactGenerator registered for format 'MARKDOWN'");
    }

    @Test
    void testConstructionRejectsDuplicateFormat() {
        // Two generators competing for the same format would silently shadow at runtime — the loop ordering decides
        // which wins. Failing at wire time means the misconfiguration shows up on application startup with a clear
        // message that names both classes, instead of as an unreproducible "wrong generator runs" runtime bug.
        StubGenerator first = new StubGenerator(AssetFileFormat.MARKDOWN, 1L);
        StubGenerator second = new StubGenerator(AssetFileFormat.MARKDOWN, 2L);

        assertThatThrownBy(() -> new ArtifactGeneratorRegistry(List.of(first, second)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate ArtifactGenerator registration for format 'MARKDOWN'");
    }

    @Test
    void testSupportedFormatsExposesRegisteredFormats() {
        StubGenerator markdown = new StubGenerator(AssetFileFormat.MARKDOWN, 1L);
        StubGenerator code = new StubGenerator(AssetFileFormat.CODE, 2L);

        ArtifactGeneratorRegistry registry = new ArtifactGeneratorRegistry(List.of(markdown, code));

        assertThat(registry.supportedFormats()).containsExactlyInAnyOrder(
            AssetFileFormat.MARKDOWN, AssetFileFormat.CODE);
    }

    private static class StubGenerator implements ArtifactGenerator {

        private final AssetFileFormat format;
        private final long assetFileId;
        int invocations;

        StubGenerator(AssetFileFormat format, long assetFileId) {
            this.format = format;
            this.assetFileId = assetFileId;
        }

        @Override
        public AssetFileFormat format() {
            return format;
        }

        @Override
        public GenerationResult generate(GenerationRequest request) {
            invocations++;

            return new GenerationResult(assetFileId, request.filename(), format, request.taskId() != null);
        }
    }
}
