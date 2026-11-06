/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.assetfile.remote.client.service;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileSystemFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAssetFileSystemFacadeClient implements AssetFileSystemFacade {

    @Override
    public AssetFile createFromUpload(
        Long workspaceId, int environment, String filename, String contentType, InputStream data) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<AssetFile> findAllByWorkspaceIdAndEnvironment(Long workspaceId, int environment, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile findByIdInWorkspace(Long id, Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile renameInWorkspace(Long id, Long workspaceId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteInWorkspace(Long id, Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream downloadContentInWorkspace(Long id, Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssetFile updateContentInWorkspace(Long id, Long workspaceId, String contentType, InputStream data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<AssetFile> fetchByPublicLinkToken(String token) {
        throw new UnsupportedOperationException();
    }
}
