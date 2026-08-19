/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatAssetFileService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Persists a JSON file. The LLM has already produced the final JSON text; the generator's added work is one parse pass
 * to confirm the payload is well-formed before it lands. Malformed JSON from the model is the dominant failure mode for
 * "give me my data as JSON" prompts — the parse error message reaches the LLM via the tool error and lets it
 * self-correct on the next turn rather than the user discovering an unparseable file in their workspace.
 *
 * <p>
 * Pretty-printing is intentionally not applied here: when the LLM produces compact JSON we keep it compact; when it
 * produces multi-line JSON we keep that. The viewer (Monaco) handles formatting on read. Re-serialising would also
 * silently drop comments — JSON5 isn't supported by the parser and stripping is not a behaviour the user asked for.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class JsonArtifactGenerator extends AbstractTextArtifactGenerator {

    /**
     * Shared {@link JsonMapper}. Construction is non-trivial (module discovery, default config); reusing one instance
     * keeps validation cheap on the write hot path.
     */
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    public JsonArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubChatAssetFileService chatAssetFileService) {

        super(assetFileFacade, chatAssetFileService);
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.JSON;
    }

    @Override
    protected String mimeType(GenerationRequest request) {
        return "application/json";
    }

    @Override
    protected String defaultExtension() {
        return "json";
    }

    @Override
    protected void validate(GenerationRequest request) {
        String content = request.payload();

        if (content.isBlank()) {
            throw new IllegalArgumentException("JSON content is empty");
        }

        try {
            JSON_MAPPER.readTree(content);
        } catch (JacksonException exception) {
            // Surface the underlying parse error so the LLM can react to "unexpected token at line N column M" on
            // the next turn instead of seeing a generic "validation failed" string.
            throw new IllegalArgumentException(
                "JSON content is not well-formed: " + exception.getMessage(), exception);
        }
    }
}
