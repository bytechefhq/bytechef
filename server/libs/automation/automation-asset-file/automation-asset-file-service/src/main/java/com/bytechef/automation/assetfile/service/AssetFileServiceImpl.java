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
import com.bytechef.automation.assetfile.domain.WorkspaceAssetFile;
import com.bytechef.automation.assetfile.repository.AssetFileRepository;
import com.bytechef.automation.assetfile.repository.WorkspaceAssetFileRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AssetFileServiceImpl implements AssetFileService {

    private final AssetFileRepository assetFileRepository;
    private final WorkspaceAssetFileRepository workspaceAssetFileRepository;

    public AssetFileServiceImpl(
        AssetFileRepository assetFileRepository,
        WorkspaceAssetFileRepository workspaceAssetFileRepository) {

        this.assetFileRepository = assetFileRepository;
        this.workspaceAssetFileRepository = workspaceAssetFileRepository;
    }

    @Override
    public AssetFile create(AssetFile assetFile, Long workspaceId) {
        Assert.notNull(workspaceId, "workspaceId is required");

        assetFile.validate();

        AssetFile savedAssetFile = assetFileRepository.save(assetFile);

        workspaceAssetFileRepository.save(new WorkspaceAssetFile(savedAssetFile.getId(), workspaceId));

        return savedAssetFile;
    }

    @Override
    public void delete(Long id) {
        assetFileRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssetFile> fetchByWorkspaceIdAndEnvironmentAndName(
        Long workspaceId, int environment, String name) {

        return assetFileRepository.findAllByWorkspaceIdAndEnvironment(workspaceId, environment)
            .stream()
            .filter(assetFile -> Objects.equals(assetFile.getName(), name))
            .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetFile> findAllByWorkspaceIdAndEnvironment(
        Long workspaceId, int environment, List<Long> tagIds) {

        if (tagIds == null || tagIds.isEmpty()) {
            return assetFileRepository.findAllByWorkspaceIdAndEnvironment(workspaceId, environment);
        }

        return assetFileRepository.findAllByWorkspaceIdAndEnvironmentAndTagIdsIn(workspaceId, environment, tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetFile findById(Long id) {
        return assetFileRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AssetFile %d not found".formatted(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public long sumSizeBytesByWorkspaceIdAndEnvironment(Long workspaceId, int environment) {
        return assetFileRepository.sumSizeBytesByWorkspaceIdAndEnvironment(workspaceId, environment);
    }

    @Override
    public AssetFile update(AssetFile assetFile) {
        assetFile.validate();

        return assetFileRepository.save(assetFile);
    }
}
