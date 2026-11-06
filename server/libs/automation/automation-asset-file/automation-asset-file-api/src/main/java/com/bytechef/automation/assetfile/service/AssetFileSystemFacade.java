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
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface AssetFileSystemFacade {

    AssetFile createFromUpload(
        Long workspaceId, int environment, String filename, String contentType, InputStream data);

    List<AssetFile> findAllByWorkspaceIdAndEnvironment(Long workspaceId, int environment, List<Long> tagIds);

    AssetFile findByIdInWorkspace(Long id, Long workspaceId);

    AssetFile renameInWorkspace(Long id, Long workspaceId, String newName);

    void deleteInWorkspace(Long id, Long workspaceId);

    InputStream downloadContentInWorkspace(Long id, Long workspaceId);

    AssetFile updateContentInWorkspace(Long id, Long workspaceId, String contentType, InputStream data);

    Optional<AssetFile> fetchByPublicLinkToken(String token);
}
