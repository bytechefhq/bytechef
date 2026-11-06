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
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AssetFileFacade {

    AssetFile createFromUpload(
        Long workspaceId, int environment, String filename, String contentType, InputStream data);

    AssetFile createFromAi(
        Long workspaceId, int environment, String filename, String contentType, String content,
        @Nullable AssetFileFormat format, @Nullable String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt);

    AssetFile createBinaryFromAi(
        Long workspaceId, int environment, String filename, String contentType, byte[] data,
        @Nullable AssetFileFormat format, @Nullable String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt);

    void delete(Long id);

    long getMaxFileSizeBytes();

    InputStream downloadContent(Long id);

    List<AssetFile> findAllByWorkspaceIdAndEnvironment(Long workspaceId, int environment, List<Long> tagIds);

    AssetFile findById(Long id);

    AssetFile findByIdInWorkspace(Long id, Long workspaceId);

    AssetFile rename(Long id, String newName);

    AssetFile updateDescription(Long id, @Nullable String description);

    List<AssetFileVersion> getVersions(Long id);

    AssetFile restoreVersion(Long id, Long versionId);

    String enablePublicLink(Long id);

    void disablePublicLink(Long id);

    AssetFile updateContent(Long id, String contentType, InputStream data);

    AssetFile cloneToEnvironment(Long id, Long workspaceId, int targetEnvironmentId, @Nullable String newName);
}
