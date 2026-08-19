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

package com.bytechef.ee.ai.hub.artifact;

import org.jspecify.annotations.Nullable;

/**
 * Bundled input passed to {@link ArtifactGenerator#generate}. Bundling the long ids and the heterogeneous
 * generator-specific fields into one record keeps the {@link ArtifactGenerator} signature stable as new generators land
 * — adding a generator never widens the interface.
 *
 * <p>
 * {@code workspaceId}, {@code userId}, and {@code environmentId} are sourced from the calling agent's
 * {@code AiHubToolInvocationContext}; the tool callback layer is the single point of trust for these. Generators must
 * not accept their own copies via tool-input JSON — a tampered tool-input that overrode {@code workspaceId} would let
 * the LLM author files into a workspace it cannot reach.
 * </p>
 *
 * @param workspaceId            the workspace owning the resulting {@code asset_file} row; required.
 * @param userId                 the user the chat turn is authenticated as; required for audit attribution but does not
 *                               appear on the {@code asset_file} row directly (workspace ownership is the access key).
 * @param environmentId          the environment ordinal the resulting row is partitioned to. Inherited from the chat
 *                               surface — the environment selector at the top of AI Hub decides this.
 * @param chatId                 the chat id to link the file to via {@code (chatId, fileId,
 *                                AUTHORED)}. {@code null} on first-turn races where the chat row does not yet exist;
 *                               the file still persists in that case but with no join.
 * @param generatedByAgentSource the AG-UI agent source ordinal that produced this artifact (e.g. AI Hub). Persisted on
 *                               the {@code asset_file} row for cross-agent provenance reporting.
 * @param generatedFromPrompt    the user's last prompt text. Persisted on the {@code asset_file} row so the audit view
 *                               can surface "what did the user ask that produced this file?" without joining back to
 *                               the chat message log.
 * @param filename               the desired file name. Generators may extend it with a format-specific extension if the
 *                               caller didn't include one (e.g. {@code "report"} → {@code "report.md"}).
 * @param payload                the generator-specific payload. For text generators this is the literal content; for
 *                               binary generators (DOCX/PPTX/CHART) this is a structured JSON spec the generator
 *                               parses. Generator-specific schema lives in each implementation's javadoc.
 * @param metadataJson           optional structured metadata persisted to {@code asset_file.metadata_json}. Chart
 *                               generators store their spec here so the viewer can re-render without going back to the
 *                               LLM. {@code null} for text formats that have no separate spec.
 */
public record GenerationRequest(
    long workspaceId,
    long userId,
    int environmentId,
    @Nullable Long chatId,
    short generatedByAgentSource,
    @Nullable String generatedFromPrompt,
    String filename,
    String payload,
    @Nullable String metadataJson) {

    /**
     * Compact constructor pinning the non-empty invariants the registry and downstream facade both rely on. A blank
     * {@code filename} would create an unaddressable file in the listing UI; a null {@code payload} would violate the
     * facade's content-required contract. Both are caller bugs — we fail fast here so the error surface is the tool
     * callback (which translates to a {@code toolError}) rather than a cryptic database constraint violation later.
     */
    public GenerationRequest {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename is required");
        }

        if (payload == null) {
            throw new IllegalArgumentException("payload is required");
        }
    }
}
