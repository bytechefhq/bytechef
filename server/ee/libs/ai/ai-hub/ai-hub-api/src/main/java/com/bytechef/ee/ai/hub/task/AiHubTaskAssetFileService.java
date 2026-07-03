/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import java.util.List;
import java.util.Optional;

/**
 * Records and queries the join rows linking AI Hub tasks to {@code asset_file} rows.
 *
 * <p>
 * The service treats {@link #recordRelationship(long, long, AiHubTaskAssetFileRelationship)} as idempotent: calling it
 * twice for the same triple is a no-op. The dedicated {@link #recordAuthorship(long, long)} method exists because
 * AUTHORED has a partial unique constraint and surfaces a different exception
 * ({@link com.bytechef.ee.ai.hub.task.AuthorshipAlreadyAssignedException}) when violated, which the caller can act on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubTaskAssetFileService {

    /**
     * Records that {@code taskId} authored {@code assetFileId}. Throws
     * {@link com.bytechef.ee.ai.hub.task.AuthorshipAlreadyAssignedException} when the asset_file already has a
     * different authoring task.
     *
     * <p>
     * Idempotent for the same task: if {@code taskId} is already the recorded author, returns the existing row without
     * inserting a duplicate.
     */
    AiHubTaskAssetFile recordAuthorship(long taskId, long assetFileId);

    /**
     * Records a non-authorship relationship (ATTACHED, MENTIONED, READ_BY_TOOL). Idempotent — re-recording the same
     * relationship returns the existing row instead of throwing on the unique constraint.
     *
     * @throws IllegalArgumentException when called with {@link AiHubTaskAssetFileRelationship#AUTHORED} — use
     *                                  {@link #recordAuthorship(long, long)} for that.
     */
    AiHubTaskAssetFile recordRelationship(
        long taskId, long assetFileId, AiHubTaskAssetFileRelationship relationship);

    /**
     * Returns the join rows for the task, newest first, including duplicates if a file has multiple relationship types.
     * Use {@link #findDistinctAssetFileIdsByTaskId(long)} when the listing intent is "files this task has touched" with
     * one row per file.
     */
    List<AiHubTaskAssetFile> findByTaskId(long taskId);

    /**
     * Returns deduplicated asset_file ids referenced (any relationship type) by the task. Drives the task files panel;
     * the caller hydrates the {@code AssetFile} rows separately to keep this service decoupled from the
     * {@code automation-asset-file} module's facade.
     */
    List<Long> findDistinctAssetFileIdsByTaskId(long taskId);

    /**
     * Returns the AUTHORED row for the asset_file, if any. The partial unique index guarantees zero or one match.
     */
    Optional<AiHubTaskAssetFile> findAuthoredByAssetFileId(long assetFileId);
}
