/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.knowledgebase.service;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * CRUD service for {@link KnowledgeBaseSource}. Workspace scoping is the nullable
 * {@code knowledge_base_source.workspace_id} column; the automation-side service is the one that decides which
 * workspace a caller may see. Lifecycle helpers ({@link #updateStatus}, {@link #updateLastSyncMetadata},
 * {@link #setEnabled}) wrap a load + mutate + save round-trip so callers don't have to reach for the repository
 * directly to flip a status flag.
 *
 * @author Ivica Cardic
 */
public interface KnowledgeBaseSourceService {

    KnowledgeBaseSource create(KnowledgeBaseSource source);

    KnowledgeBaseSource update(KnowledgeBaseSource source);

    void delete(Long id);

    KnowledgeBaseSource get(Long id);

    Optional<KnowledgeBaseSource> fetch(Long id);

    List<KnowledgeBaseSource> getAllByIds(List<Long> ids);

    List<KnowledgeBaseSource> getAllEnabledByIds(List<Long> ids);

    List<KnowledgeBaseSource> getAllByWorkspaceId(long workspaceId);

    List<KnowledgeBaseSource> getAllEnabledByWorkspaceId(long workspaceId);

    List<KnowledgeBaseSource> findAllByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * Every source whose generated sync workflow is {@code workflowId}. Used by the workflow-delete cascade.
     */
    List<KnowledgeBaseSource> findAllByWorkflowId(String workflowId);

    /**
     * Returns every enabled source across all knowledge bases. Used by the background sync scheduler; not workspace
     * scoped — callers that need workspace scoping go through the automation-side
     * {@code WorkspaceKnowledgeBaseSourceService} instead.
     */
    List<KnowledgeBaseSource> findAllActiveAcrossKnowledgeBases();

    /**
     * Updates status + (optionally) lastSyncRunAt + lastSyncJobExecutionId in one round-trip. Used by the listener's
     * COMPLETED / FAILED branches.
     */
    void updateStatus(
        Long id, KnowledgeBaseSourceStatus status, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId);

    /**
     * Updates lastSyncRunAt + lastSyncJobExecutionId without touching status. Used by the listener's COMPLETED +
     * PARTIAL branch — bumps timestamps without flipping status (a {@code BUILDING_PREVIEW} source stays
     * {@code BUILDING_PREVIEW} until a {@code FULL_REPLACE} proves the source is ready; a {@code READY} source stays
     * {@code READY}).
     */
    void updateLastSyncMetadata(Long id, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId);

    void setEnabled(Long id, boolean enabled);
}
