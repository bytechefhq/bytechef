/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
@Transactional
public class AiHubChatAssetFileFacadeImpl implements AiHubChatAssetFileFacade {

    private static final String ASSET_FILE_RESOURCE_TYPE = "asset_file";

    private final ObjectProvider<AssetFileFacade> assetFileFacadeProvider;
    private final AiHubChatAssetFileService chatAssetFileService;
    private final AiHubChatService chatService;

    @SuppressFBWarnings("EI")
    public AiHubChatAssetFileFacadeImpl(
        ObjectProvider<AssetFileFacade> assetFileFacadeProvider, AiHubChatAssetFileService chatAssetFileService,
        AiHubChatService chatService) {

        this.assetFileFacadeProvider = assetFileFacadeProvider;
        this.chatAssetFileService = chatAssetFileService;
        this.chatService = chatService;
    }

    @Override
    public AiHubChatAssetFile attachAssetFile(
        long chatId, long requesterWorkspaceId, long requesterUserId, long assetFileId) {

        chatService.getViewable(chatId, requesterWorkspaceId, requesterUserId);

        verifyAssetFileVisible(assetFileId, requesterWorkspaceId);

        return chatAssetFileService.recordRelationship(chatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);
    }

    @Override
    public void detachAssetFile(long chatId, long requesterWorkspaceId, long requesterUserId, long assetFileId) {
        chatService.getViewable(chatId, requesterWorkspaceId, requesterUserId);

        chatAssetFileService.deleteRelationship(chatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);
    }

    private void verifyAssetFileVisible(long assetFileId, long workspaceId) {
        AssetFileFacade assetFileFacade = assetFileFacadeProvider.getIfAvailable();

        if (assetFileFacade == null) {
            throw new NotFoundException(ASSET_FILE_RESOURCE_TYPE, assetFileId, "Asset files are not available");
        }

        try {
            assetFileFacade.findByIdInWorkspace(assetFileId, workspaceId);
        } catch (AccessDeniedException | AssetFileNotFoundException exception) {
            throw new NotFoundException(ASSET_FILE_RESOURCE_TYPE, assetFileId, "Asset file not found");
        }
    }
}
