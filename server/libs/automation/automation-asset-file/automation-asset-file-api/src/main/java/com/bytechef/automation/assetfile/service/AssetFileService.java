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
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface AssetFileService {

    AssetFile create(AssetFile assetFile, Long workspaceId);

    void delete(Long id);

    Optional<AssetFile> fetchByWorkspaceIdAndEnvironmentAndName(Long workspaceId, int environment, String name);

    List<AssetFile> findAllByWorkspaceIdAndEnvironment(Long workspaceId, int environment, List<Long> tagIds);

    AssetFile findById(Long id);

    /**
     * Total asset-file bytes across the whole tenant (all workspaces, all environments) — the quantity the plan's
     * {@code maxStorageBytes} limit is compared against.
     */
    long sumSizeBytes();

    long sumSizeBytesByWorkspaceIdAndEnvironment(Long workspaceId, int environment);

    AssetFile update(AssetFile assetFile);
}
