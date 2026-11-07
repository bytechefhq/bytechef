/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.chat.repository.AiHubChatAssetFileRepository;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
public class AiHubChatAssetFileServiceImpl implements AiHubChatAssetFileService {

    private final AiHubChatAssetFileRepository chatAssetFileRepository;
    private final Clock clock;

    @SuppressFBWarnings("EI")
    public AiHubChatAssetFileServiceImpl(AiHubChatAssetFileRepository chatAssetFileRepository) {
        this.chatAssetFileRepository = chatAssetFileRepository;
        this.clock = Clock.systemUTC();
    }

    @Override
    public void deleteRelationship(long chatId, long assetFileId, AiHubChatAssetFileRelationship relationship) {
        chatAssetFileRepository.deleteByChatIdAndAssetFileIdAndRelationship(
            chatId, assetFileId, relationship.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubChatAssetFile> findByChatId(long chatId) {
        return chatAssetFileRepository.findByChatIdOrderByCreatedAtDesc(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubChatAssetFile> findAuthoredByAssetFileId(long assetFileId) {
        return chatAssetFileRepository.findAuthoredByAssetFileId(assetFileId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findDistinctAssetFileIdsByChatId(long chatId) {
        return chatAssetFileRepository.findDistinctAssetFileIdsByChatId(chatId);
    }

    @Override
    public AiHubChatAssetFile recordAuthorship(long chatId, long assetFileId) {
        Optional<AiHubChatAssetFile> existingOptional = chatAssetFileRepository.findAuthoredByAssetFileId(assetFileId);

        if (existingOptional.isPresent()) {
            AiHubChatAssetFile existing = existingOptional.get();

            if (existing.getChatId() == chatId) {
                return existing;
            }

            throw new AuthorshipAlreadyAssignedException(assetFileId, existing.getChatId());
        }

        return insertRow(chatId, assetFileId, AiHubChatAssetFileRelationship.AUTHORED);
    }

    @Override
    public AiHubChatAssetFile recordRelationship(
        long chatId, long assetFileId, AiHubChatAssetFileRelationship relationship) {

        if (relationship == AiHubChatAssetFileRelationship.AUTHORED) {
            throw new IllegalArgumentException("Use recordAuthorship to record an AUTHORED relationship");
        }

        Optional<AiHubChatAssetFile> existingOptional = chatAssetFileRepository
            .findByChatIdAndAssetFileIdAndRelationship(chatId, assetFileId, relationship.ordinal());

        return existingOptional.orElseGet(() -> insertRow(chatId, assetFileId, relationship));
    }

    private AiHubChatAssetFile insertRow(long chatId, long assetFileId, AiHubChatAssetFileRelationship relationship) {
        AiHubChatAssetFile chatAssetFile = new AiHubChatAssetFile();

        chatAssetFile.setAssetFileId(assetFileId);
        chatAssetFile.setChatId(chatId);
        chatAssetFile.setCreatedAt(LocalDateTime.now(clock));
        chatAssetFile.setRelationship(relationship);

        return chatAssetFileRepository.save(chatAssetFile);
    }
}
