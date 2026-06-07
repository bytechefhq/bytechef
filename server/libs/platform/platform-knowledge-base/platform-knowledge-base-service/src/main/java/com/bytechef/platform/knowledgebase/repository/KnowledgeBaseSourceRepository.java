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
 * Spring Data JDBC repository for {@link KnowledgeBaseSource}. Workspace scoping has been moved out of the source row
 * itself — workspace membership is enforced by the {@code workspace_knowledge_base_source} relation table managed by
 * the automation-side {@code WorkspaceKnowledgeBaseSourceRepository}. This repository owns only the source-level
 * lifecycle queries (e.g. all enabled sources across knowledge bases, used by the background sync scheduler).
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
}
