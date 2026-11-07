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

package com.bytechef.automation.knowledgebase.service;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface WorkspaceKnowledgeBaseSourceService {

    Optional<Long> fetchWorkspaceIdByKnowledgeBaseSourceId(Long knowledgeBaseSourceId);

    List<KnowledgeBaseSource> getAllSourcesByWorkspaceId(Long workspaceId);

    List<KnowledgeBaseSource> getAllSourcesByWorkspaceId(Long workspaceId, Long environmentId);

    List<KnowledgeBaseSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId);

    List<KnowledgeBaseSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId, Long environmentId);
}
