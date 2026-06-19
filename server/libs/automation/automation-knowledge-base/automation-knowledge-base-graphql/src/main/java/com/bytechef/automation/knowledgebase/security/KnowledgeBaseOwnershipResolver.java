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

package com.bytechef.automation.knowledgebase.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps a knowledge-base id to its owning workspace via the {@code workspace_knowledge_base} relation. Knowledge bases
 * are collaborative, workspace-scoped resources: EE authorizes by the caller's workspace role; CE is permissive (shared
 * within the single workspace). Fails closed when the knowledge base is not mapped to any workspace.
 *
 * @author Ivica Cardic
 */
@Component
public class KnowledgeBaseOwnershipResolver implements ResourceOwnershipResolver {

    private final WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseOwnershipResolver(WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository) {
        this.workspaceKnowledgeBaseRepository = workspaceKnowledgeBaseRepository;
    }

    @Override
    public String resourceType() {
        return "KnowledgeBase";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        List<WorkspaceKnowledgeBase> workspaceKnowledgeBases =
            workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(id);

        if (workspaceKnowledgeBases.isEmpty()) {
            return ResourceOwner.unknown();
        }

        WorkspaceKnowledgeBase workspaceKnowledgeBase = workspaceKnowledgeBases.getFirst();

        return ResourceOwner.ofWorkspace(workspaceKnowledgeBase.getWorkspaceId());
    }
}
