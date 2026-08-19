/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;

/**
 * Strategy for producing one persisted asset file from a tool-supplied {@link GenerationRequest}. Each implementation
 * handles one {@link AssetFileFormat} — markdown, code, csv, json, docx, pptx, chart, image — and is registered with
 * the {@code ArtifactGeneratorRegistry} which dispatches by format.
 *
 * <p>
 * Generators are <strong>synchronous</strong>. They run inside the agent's tool-call dispatch, persist the
 * {@code asset_file} row, link it to the chat via the {@code AUTHORED} join, and return the row id in
 * {@link GenerationResult}. The chat surface renders the resulting tool result as a clickable file chip via the
 * existing tool-call channel — there is intentionally no separate streaming protocol for generated artifacts (text
 * formats already stream through the chat token stream; binary formats are unrenderable until complete).
 * </p>
 *
 * <p>
 * <strong>Environment is implicit.</strong> The {@code workspaceId}, {@code userId}, and {@code environmentId} on the
 * incoming {@link GenerationRequest} come from the caller's {@code AiHubToolInvocationContext}, not from a separate
 * argument on the tool input. Generators must not accept their own environment argument — the chat surface is the
 * authoritative source for which environment a tool call lives in.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ArtifactGenerator {

    /**
     * The format this generator handles. Used by the registry to dispatch — exactly one generator may be registered per
     * format.
     */
    AssetFileFormat format();

    /**
     * Produces the artifact and persists it. The implementation is responsible for:
     * <ol>
     * <li>Materialising the artifact bytes from {@link GenerationRequest#payload()} (or generating fresh content from
     * the structured prompt).</li>
     * <li>Inserting an {@code asset_file} row via the platform facade with {@code source=AI_GENERATED}, the supplied
     * {@code generatedByAgentSource} ordinal, and {@code generatedFromPrompt} set to the user's last prompt.</li>
     * <li>Recording the {@code (chatId, assetFileId, AUTHORED)} join via
     * {@code AiHubChatAssetFileService.recordAuthorship} when {@code chatId} is non-null. When the request carries no
     * chat context (rare — first-turn race before the chat endpoint creates the row), the file still persists but is
     * not joined; the caller surfaces this back as {@code chatLinked=false}.</li>
     * </ol>
     */
    GenerationResult generate(GenerationRequest request);
}
