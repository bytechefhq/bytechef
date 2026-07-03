/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task.repository;

import com.bytechef.ee.ai.hub.task.AiHubTaskArtifact;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiHubTaskArtifact} rows.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubTaskArtifactRepository
    extends CrudRepository<AiHubTaskArtifact, Long> {

    List<AiHubTaskArtifact> findByTaskIdOrderByCreatedAtDesc(long taskId, Limit limit);

    long countByTaskId(long taskId);

    /**
     * Idempotency lookup for the user-driven "attach a reference" flow. Returns the existing artifact row for the given
     * task + kind + artifactId tuple if one was previously recorded, so the recorder can skip duplicate inserts when
     * the user re-attaches the same file/workflow/etc.
     */
    Optional<AiHubTaskArtifact> findFirstByTaskIdAndKindAndArtifactId(
        long taskId, int kind, String artifactId);

    /**
     * Cross-kind idempotency lookup for workflow artifacts. A single workflow maps to one sidebar row regardless of
     * whether it was created, updated, or merely referenced this turn, so the dedup key is {@code (taskId, artifactId)}
     * spanning all three workflow kinds (WORKFLOW_CREATED / WORKFLOW_UPDATED / WORKFLOW_REFERENCED ordinals).
     */
    Optional<AiHubTaskArtifact> findFirstByTaskIdAndArtifactIdAndKindIn(
        long taskId, String artifactId, Collection<Integer> kinds);
}
