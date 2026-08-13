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

package com.bytechef.automation.assetfile.service;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.domain.AssetFileVersion;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AssetFileFacade {

    AssetFile createFromUpload(
        Long workspaceId, int environment, String filename, String contentType, InputStream data);

    /**
     * Persists a text artifact emitted by an AI tool. {@code format} and {@code metadataJson} are the artifact
     * classification fields used by the Files viewer to dispatch render mode (markdown / code / csv / json) and to
     * carry generator-specific spec data (e.g. chart spec). Both are {@code null} for legacy callers that did not
     * classify; new {@code ArtifactGenerator} implementations always supply at least {@code format}.
     */
    AssetFile createFromAi(
        Long workspaceId, int environment, String filename, String contentType, String content,
        @Nullable AssetFileFormat format, @Nullable String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt);

    /**
     * Binary counterpart to {@link #createFromAi} for copilot-produced non-text assets (images, pptx, and other binary
     * formats). The caller supplies the raw bytes directly — no character-encoding step is applied. Quota and
     * size-limit checks are identical to the text path (byte count is already known).
     */
    AssetFile createBinaryFromAi(
        Long workspaceId, int environment, String filename, String contentType, byte[] data,
        @Nullable AssetFileFormat format, @Nullable String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt);

    void delete(Long id);

    /**
     * Returns the configured per-file size quota in bytes (the same limit {@code createFromAi} and
     * {@code createBinaryFromAi} enforce), or a negative value when no per-file limit is configured. Lets a caller that
     * buffers bytes before calling one of the {@code create*} methods — e.g. a tool that downloads a URL server-side —
     * bound its own buffer to the effective limit instead of discovering the quota only after fully downloading and
     * being rejected.
     */
    long getMaxFileSizeBytes();

    InputStream downloadContent(Long id);

    List<AssetFile> findAllByWorkspaceIdAndEnvironment(Long workspaceId, int environment, List<Long> tagIds);

    AssetFile findById(Long id);

    /**
     * Loads an asset file but only when it belongs to the supplied {@code workspaceId}. Authorization gate for callers
     * that hold a workspace context (the AI tool callbacks): if a tool is invoked with a file id that resolves to a
     * different workspace, this method throws
     * {@link com.bytechef.automation.assetfile.exception.AssetFileNotFoundException} so the caller cannot exfiltrate a
     * file from another tenant by guessing ids. The same exception is used for "file not found" since the caller cannot
     * distinguish the two safely (a 404-vs-403 oracle is itself a leak); the tool callback converts this to a typed
     * tool-error.
     */
    AssetFile findByIdInWorkspace(Long id, Long workspaceId);

    /**
     * Returns the workspace id that owns the given asset file. Used by the REST layer to translate a file id into a
     * workspace before checking caller membership: download/replace endpoints take only the file id in the path, so
     * resolving the workspace at the controller level is the only way to apply a workspace-membership gate without
     * forcing every client to thread the workspace through the URL.
     *
     * @throws com.bytechef.automation.assetfile.exception.AssetFileNotFoundException when the id does not resolve to a
     *                                                                                workspace asset file
     */
    Long getOwningWorkspaceId(Long id);

    AssetFile rename(Long id, String newName);

    /**
     * Updates the free-text description of an asset file. A {@code null} value clears the description.
     */
    AssetFile updateDescription(Long id, @Nullable String description);

    /**
     * Lists the content versions of an asset file, newest first. Versions are snapshots of PRIOR content — the current
     * content lives on the {@link AssetFile} row itself and is not represented as a version.
     */
    List<AssetFileVersion> getVersions(Long id);

    /**
     * Restores a previous content version: the current content is snapshotted as a new version (so a restore is itself
     * undoable) and the version's bytes are copied into a fresh blob that becomes the current content. The version row
     * being restored is left untouched.
     *
     * @throws com.bytechef.automation.assetfile.exception.AssetFileNotFoundException when the version does not exist or
     *                                                                                belongs to a different file
     */
    AssetFile restoreVersion(Long id, Long versionId);

    /**
     * Enables (or returns the existing) durable public link for the file. The returned token grants anonymous download
     * of the file's content for as long as the link stays enabled and the operator-level
     * {@code bytechef.asset-file.sharing.public-link-enabled} switch is on.
     *
     * @throws IllegalStateException when public-link sharing is disabled by the operator
     */
    String enablePublicLink(Long id);

    void disablePublicLink(Long id);

    /**
     * Resolves a public-link token to its asset file. Returns empty when the token is unknown OR when the operator has
     * switched public sharing off — an existing link stops resolving the moment the kill-switch flips.
     */
    Optional<AssetFile> fetchByPublicLinkToken(String token);

    /**
     * Mints a short-lived HMAC-signed download token for the file's current content, redeemable anonymously at the
     * signed asset-file download endpoint. TTL comes from the platform-wide signed-URL configuration.
     *
     * @throws IllegalStateException when no signing infrastructure is configured
     */
    String createSignedDownloadToken(Long id);

    AssetFile updateContent(Long id, String contentType, InputStream data);

    /**
     * Clones an existing asset file into the target environment within the same workspace, copying the underlying bytes
     * verbatim. The clone is recorded as {@link com.bytechef.automation.assetfile.domain.AssetFileSource#USER_UPLOAD}
     * regardless of the source's origin: promoting an AI-generated file to PROD means the user has reviewed and is
     * shipping it, so attributing the destination as a user upload is the honest audit trail. AI metadata
     * ({@code generatedByAgentSource}, {@code generatedFromPrompt}, {@code format}, {@code metadataJson}) is dropped
     * for the same reason — those fields describe the genesis of the bytes, not the lineage of the row.
     *
     * <p>
     * Same-workspace, cross-environment scope: the {@code workspaceId} carries over. The source file must belong to the
     * supplied workspace; if not, {@link com.bytechef.automation.assetfile.exception.AssetFileNotFoundException} is
     * thrown so a forged id from another workspace cannot bypass the boundary.
     * </p>
     *
     * @param id                  source asset file id
     * @param workspaceId         owning workspace — verified against the source for defense in depth
     * @param targetEnvironmentId target environment ordinal
     *                            ({@link com.bytechef.platform.configuration.domain.Environment#ordinal()})
     * @param newName             optional name override; falls back to the source filename when null
     * @return the persisted clone
     */
    AssetFile cloneToEnvironment(Long id, Long workspaceId, int targetEnvironmentId, @Nullable String newName);
}
