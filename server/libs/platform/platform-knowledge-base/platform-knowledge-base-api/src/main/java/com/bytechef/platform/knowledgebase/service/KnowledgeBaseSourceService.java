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

    List<KnowledgeBaseSource> findAllByWorkflowId(String workflowId);

    List<KnowledgeBaseSource> findAllActiveAcrossKnowledgeBases();

    void updateStatus(
        Long id, KnowledgeBaseSourceStatus status, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId);

    void updateLastSyncMetadata(Long id, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId);

    void setEnabled(Long id, boolean enabled);
}
