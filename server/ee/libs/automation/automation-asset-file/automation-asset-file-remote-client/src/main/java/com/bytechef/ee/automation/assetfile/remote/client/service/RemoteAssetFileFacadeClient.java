/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.assetfile.remote.client.service;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.io.InputStream;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAssetFileFacadeClient implements AssetFileFacade {

    @Override
    public AssetFile createFromUpload(
        Long workspaceId, int environment, String filename, String contentType, InputStream data) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile createFromAi(
        Long workspaceId, int environment, String filename, String contentType, String content,
        AssetFileFormat format, String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile createBinaryFromAi(
        Long workspaceId, int environment, String filename, String contentType, byte[] data,
        AssetFileFormat format, String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream downloadContent(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AssetFile> findAllByWorkspaceIdAndEnvironment(
        Long workspaceId, int environment, List<Long> tagIds) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile findById(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile findByIdInWorkspace(Long id, Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getOwningWorkspaceId(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile rename(Long id, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile updateContent(Long id, String contentType, InputStream data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile cloneToEnvironment(Long id, Long workspaceId, int targetEnvironmentId, @Nullable String newName) {
        throw new UnsupportedOperationException();
    }
}
