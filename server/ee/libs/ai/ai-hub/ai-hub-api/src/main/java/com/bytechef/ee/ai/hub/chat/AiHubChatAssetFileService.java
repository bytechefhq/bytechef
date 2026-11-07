/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatAssetFileService {

    void deleteRelationship(long chatId, long assetFileId, AiHubChatAssetFileRelationship relationship);

    List<AiHubChatAssetFile> findByChatId(long chatId);

    Optional<AiHubChatAssetFile> findAuthoredByAssetFileId(long assetFileId);

    List<Long> findDistinctAssetFileIdsByChatId(long chatId);

    AiHubChatAssetFile recordAuthorship(long chatId, long assetFileId);

    AiHubChatAssetFile recordRelationship(long chatId, long assetFileId, AiHubChatAssetFileRelationship relationship);
}
