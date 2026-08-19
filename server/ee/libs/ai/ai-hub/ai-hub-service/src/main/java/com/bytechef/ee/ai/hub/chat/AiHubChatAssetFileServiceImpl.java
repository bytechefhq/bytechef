/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.chat.repository.AiHubChatAssetFileRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link AiHubChatAssetFileService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Transactional
public class AiHubChatAssetFileServiceImpl implements AiHubChatAssetFileService {

    private final AiHubChatAssetFileRepository chatAssetFileRepository;
    private final Clock clock;

    public AiHubChatAssetFileServiceImpl(
        AiHubChatAssetFileRepository chatAssetFileRepository) {

        this.chatAssetFileRepository = chatAssetFileRepository;
        this.clock = Clock.systemUTC();
    }

    @Override
    public AiHubChatAssetFile recordAuthorship(long chatId, long assetFileId) {
        Optional<AiHubChatAssetFile> existing =
            chatAssetFileRepository.findAuthoredByAssetFileId(
                assetFileId);

        if (existing.isPresent()) {
            AiHubChatAssetFile existingRow = existing.get();

            if (existingRow.getChatId() == chatId) {
                return existingRow;
            }

            throw new AuthorshipAlreadyAssignedException(assetFileId, existingRow.getChatId());
        }

        return insertRow(chatId, assetFileId, AiHubChatAssetFileRelationship.AUTHORED);
    }

    @Override
    public AiHubChatAssetFile recordRelationship(
        long chatId, long assetFileId, AiHubChatAssetFileRelationship relationship) {

        if (relationship == AiHubChatAssetFileRelationship.AUTHORED) {
            throw new IllegalArgumentException(
                "Use recordAuthorship(...) for AUTHORED; recordRelationship is for non-authorship relationships");
        }

        Optional<AiHubChatAssetFile> existing = chatAssetFileRepository
            .findByChatIdAndAssetFileIdAndRelationship(chatId, assetFileId, relationship.ordinal());

        return existing.orElseGet(() -> insertRow(chatId, assetFileId, relationship));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubChatAssetFile> findByChatId(long chatId) {
        return chatAssetFileRepository.findByChatIdOrderByCreatedAtDesc(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findDistinctAssetFileIdsByChatId(long chatId) {
        return chatAssetFileRepository.findDistinctAssetFileIdsByChatId(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubChatAssetFile> findAuthoredByAssetFileId(long assetFileId) {
        return chatAssetFileRepository.findAuthoredByAssetFileId(assetFileId);
    }

    private AiHubChatAssetFile insertRow(
        long chatId, long assetFileId, AiHubChatAssetFileRelationship relationship) {

        AiHubChatAssetFile row = new AiHubChatAssetFile();

        row.setChatId(chatId);
        row.setAssetFileId(assetFileId);
        row.setRelationship(relationship);
        row.setCreatedAt(LocalDateTime.now(clock));

        return chatAssetFileRepository.save(row);
    }
}
