/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.ee.ai.hub.task.repository.AiHubTaskAssetFileRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link AiHubTaskAssetFileService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Transactional
public class AiHubTaskAssetFileServiceImpl implements AiHubTaskAssetFileService {

    private final AiHubTaskAssetFileRepository taskAssetFileRepository;
    private final Clock clock;

    public AiHubTaskAssetFileServiceImpl(
        AiHubTaskAssetFileRepository taskAssetFileRepository) {

        this.taskAssetFileRepository = taskAssetFileRepository;
        this.clock = Clock.systemUTC();
    }

    @Override
    public AiHubTaskAssetFile recordAuthorship(long taskId, long assetFileId) {
        Optional<AiHubTaskAssetFile> existing =
            taskAssetFileRepository.findAuthoredByAssetFileId(
                assetFileId);

        if (existing.isPresent()) {
            AiHubTaskAssetFile existingRow = existing.get();

            if (existingRow.getTaskId() == taskId) {
                return existingRow;
            }

            throw new AuthorshipAlreadyAssignedException(assetFileId, existingRow.getTaskId());
        }

        return insertRow(taskId, assetFileId, AiHubTaskAssetFileRelationship.AUTHORED);
    }

    @Override
    public AiHubTaskAssetFile recordRelationship(
        long taskId, long assetFileId, AiHubTaskAssetFileRelationship relationship) {

        if (relationship == AiHubTaskAssetFileRelationship.AUTHORED) {
            throw new IllegalArgumentException(
                "Use recordAuthorship(...) for AUTHORED; recordRelationship is for non-authorship relationships");
        }

        Optional<AiHubTaskAssetFile> existing = taskAssetFileRepository
            .findByTaskIdAndAssetFileIdAndRelationship(taskId, assetFileId, relationship.ordinal());

        return existing.orElseGet(() -> insertRow(taskId, assetFileId, relationship));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubTaskAssetFile> findByTaskId(long taskId) {
        return taskAssetFileRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findDistinctAssetFileIdsByTaskId(long taskId) {
        return taskAssetFileRepository.findDistinctAssetFileIdsByTaskId(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubTaskAssetFile> findAuthoredByAssetFileId(long assetFileId) {
        return taskAssetFileRepository.findAuthoredByAssetFileId(assetFileId);
    }

    private AiHubTaskAssetFile insertRow(
        long taskId, long assetFileId, AiHubTaskAssetFileRelationship relationship) {

        AiHubTaskAssetFile row = new AiHubTaskAssetFile();

        row.setTaskId(taskId);
        row.setAssetFileId(assetFileId);
        row.setRelationship(relationship);
        row.setCreatedAt(LocalDateTime.now(clock));

        return taskAssetFileRepository.save(row);
    }
}
