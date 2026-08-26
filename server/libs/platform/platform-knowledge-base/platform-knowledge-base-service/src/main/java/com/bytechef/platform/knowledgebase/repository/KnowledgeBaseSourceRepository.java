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

package com.bytechef.platform.knowledgebase.repository;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for {@link KnowledgeBaseSource}. Workspace membership is the nullable
 * {@code knowledge_base_source.workspace_id} column; a source with a null workspace matches no workspace-scoped query.
 * The repository also owns the workspace-agnostic lifecycle queries (e.g. all enabled sources across knowledge bases,
 * used by the background sync scheduler).
 *
 * @author Ivica Cardic
 */
@Repository
public interface KnowledgeBaseSourceRepository
    extends PagingAndSortingRepository<KnowledgeBaseSource, Long>, ListCrudRepository<KnowledgeBaseSource, Long> {

    List<KnowledgeBaseSource> findAllByEnabled(boolean enabled);

    List<KnowledgeBaseSource> findAllByKnowledgeBaseId(Long knowledgeBaseId);

    List<KnowledgeBaseSource> findAllByIdIn(List<Long> ids);

    List<KnowledgeBaseSource> findAllByIdInAndEnabled(List<Long> ids, boolean enabled);

    List<KnowledgeBaseSource> findAllByWorkflowId(String workflowId);

    List<KnowledgeBaseSource> findAllByWorkspaceId(long workspaceId);

    List<KnowledgeBaseSource> findAllByWorkspaceIdAndEnabled(long workspaceId, boolean enabled);
}
