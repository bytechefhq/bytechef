/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat.repository;

import com.bytechef.ee.ai.hub.chat.AiHubChatArtifact;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiHubChatArtifact} rows.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatArtifactRepository
    extends CrudRepository<AiHubChatArtifact, Long> {

    List<AiHubChatArtifact> findByChatIdOrderByCreatedAtDesc(long chatId, Limit limit);

    long countByChatId(long chatId);

    /**
     * Idempotency lookup for the user-driven "attach a reference" flow. Returns the existing artifact row for the given
     * chat + kind + artifactId tuple if one was previously recorded, so the recorder can skip duplicate inserts when
     * the user re-attaches the same file/workflow/etc.
     */
    Optional<AiHubChatArtifact> findFirstByChatIdAndKindAndArtifactId(
        long chatId, int kind, String artifactId);

    /**
     * Cross-kind idempotency lookup for workflow artifacts. A single workflow maps to one sidebar row regardless of
     * whether it was created, updated, or merely referenced this turn, so the dedup key is {@code (chatId, artifactId)}
     * spanning all three workflow kinds (WORKFLOW_CREATED / WORKFLOW_UPDATED / WORKFLOW_REFERENCED ordinals).
     */
    Optional<AiHubChatArtifact> findFirstByChatIdAndArtifactIdAndKindIn(
        long chatId, String artifactId, Collection<Integer> kinds);
}
