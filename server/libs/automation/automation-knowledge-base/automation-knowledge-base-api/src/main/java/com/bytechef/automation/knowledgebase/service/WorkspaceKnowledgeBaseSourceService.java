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

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-scoped service over the {@link WorkspaceKnowledgeBaseSource} relation table. Provides the workspace-aware
 * lookups for KB sources — the platform-side source service no longer carries any workspace knowledge after the
 * platform-relation refactor.
 *
 * <p>
 * Environment scoping: the {@code environmentId} overloads accept an environment for forward compatibility with the
 * Phase 15 UI. The relation table does not carry an environment column today (sources own a single
 * {@code ProjectDeploymentWorkflow} pinned to {@code DEVELOPMENT}); the parameter is reserved so callers can pass the
 * active environment without further GraphQL surface changes when per-environment scoping lands.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceKnowledgeBaseSourceService {

    WorkspaceKnowledgeBaseSource registerSourceForWorkspace(Long knowledgeBaseSourceId, Long workspaceId);

    void unregisterSource(Long knowledgeBaseSourceId);

    Optional<Long> fetchWorkspaceIdByKnowledgeBaseSourceId(Long knowledgeBaseSourceId);

    List<KnowledgeBaseSource> getAllSourcesByWorkspaceId(Long workspaceId);

    List<KnowledgeBaseSource> getAllSourcesByWorkspaceId(Long workspaceId, Long environmentId);

    List<KnowledgeBaseSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId);

    List<KnowledgeBaseSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId, Long environmentId);
}
