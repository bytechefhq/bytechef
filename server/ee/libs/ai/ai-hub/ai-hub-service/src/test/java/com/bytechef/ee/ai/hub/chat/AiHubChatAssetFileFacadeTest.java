/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatAssetFileFacadeTest {

    private static final long ASSET_FILE_ID = 99L;
    private static final long CHAT_ID = 42L;
    private static final long USER_ID = 10L;
    private static final long WORKSPACE_ID = 7L;

    private final AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
    private final AiHubChatAssetFileService chatAssetFileService = mock(AiHubChatAssetFileService.class);
    private final AiHubChatService chatService = mock(AiHubChatService.class);

    private AiHubChatAssetFileFacade chatAssetFileFacade;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        ObjectProvider<AssetFileFacade> assetFileFacadeProvider = mock(ObjectProvider.class);

        when(assetFileFacadeProvider.getIfAvailable()).thenReturn(assetFileFacade);

        chatAssetFileFacade = new AiHubChatAssetFileFacadeImpl(
            assetFileFacadeProvider, chatAssetFileService, chatService);
    }

    @Test
    void testAttachAssetFileRecordsAttachedRelationship() {
        AiHubChatAssetFile chatAssetFile = new AiHubChatAssetFile();

        when(assetFileFacade.findByIdInWorkspace(ASSET_FILE_ID, WORKSPACE_ID)).thenReturn(new AssetFile());
        when(chatAssetFileService.recordRelationship(
            CHAT_ID, ASSET_FILE_ID, AiHubChatAssetFileRelationship.ATTACHED)).thenReturn(chatAssetFile);

        AiHubChatAssetFile attached = chatAssetFileFacade.attachAssetFile(
            CHAT_ID, WORKSPACE_ID, USER_ID, ASSET_FILE_ID);

        assertThat(attached).isSameAs(chatAssetFile);

        verify(chatService).getViewable(CHAT_ID, WORKSPACE_ID, USER_ID);
    }

    @Test
    void testAttachAssetFileOfAWorkspaceTheCallerIsNotAMemberOfIsNotFound() {
        when(assetFileFacade.findByIdInWorkspace(ASSET_FILE_ID, WORKSPACE_ID))
            .thenThrow(new AccessDeniedException("Workspace is not accessible to the current user"));

        assertThatThrownBy(() -> chatAssetFileFacade.attachAssetFile(CHAT_ID, WORKSPACE_ID, USER_ID, ASSET_FILE_ID))
            .isInstanceOf(NotFoundException.class);

        verify(chatAssetFileService, never()).recordRelationship(anyLong(), anyLong(), any());
    }

    @Test
    void testAttachAssetFileOfAnotherWorkspaceIsNotFound() {
        when(assetFileFacade.findByIdInWorkspace(ASSET_FILE_ID, WORKSPACE_ID))
            .thenThrow(new AssetFileNotFoundException("Asset file 99 not found in workspace 7"));

        assertThatThrownBy(() -> chatAssetFileFacade.attachAssetFile(CHAT_ID, WORKSPACE_ID, USER_ID, ASSET_FILE_ID))
            .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(chatAssetFileService);
    }

    @Test
    void testAttachAssetFileToAChatTheCallerCannotSeeIsNotFound() {
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID))
            .thenThrow(new NotFoundException("ai_hub_chat", CHAT_ID));

        assertThatThrownBy(() -> chatAssetFileFacade.attachAssetFile(CHAT_ID, WORKSPACE_ID, USER_ID, ASSET_FILE_ID))
            .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(assetFileFacade, chatAssetFileService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAttachAssetFileWithoutTheAssetFileModuleIsNotFound() {
        ObjectProvider<AssetFileFacade> emptyProvider = mock(ObjectProvider.class);

        AiHubChatAssetFileFacade facadeWithoutAssetFiles = new AiHubChatAssetFileFacadeImpl(
            emptyProvider, chatAssetFileService, chatService);

        assertThatThrownBy(
            () -> facadeWithoutAssetFiles.attachAssetFile(CHAT_ID, WORKSPACE_ID, USER_ID, ASSET_FILE_ID))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(chatAssetFileService);
    }

    @Test
    void testDetachAssetFileDeletesAttachedRelationship() {
        chatAssetFileFacade.detachAssetFile(CHAT_ID, WORKSPACE_ID, USER_ID, ASSET_FILE_ID);

        verify(chatService).getViewable(CHAT_ID, WORKSPACE_ID, USER_ID);
        verify(chatAssetFileService).deleteRelationship(
            CHAT_ID, ASSET_FILE_ID, AiHubChatAssetFileRelationship.ATTACHED);
    }

    @Test
    void testDetachAssetFileFromAChatTheCallerCannotSeeIsNotFound() {
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID))
            .thenThrow(new NotFoundException("ai_hub_chat", CHAT_ID));

        assertThatThrownBy(() -> chatAssetFileFacade.detachAssetFile(CHAT_ID, WORKSPACE_ID, USER_ID, ASSET_FILE_ID))
            .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(chatAssetFileService);
    }
}
