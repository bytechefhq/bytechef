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

package com.bytechef.automation.knowledgebase.web.graphql;

import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacade;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseSourceService;
import com.bytechef.automation.knowledgebase.web.graphql.dto.CreateKnowledgeBaseSourceGraphQlInput;
import com.bytechef.automation.knowledgebase.web.graphql.dto.KnowledgeBaseSourceFilter;
import com.bytechef.automation.knowledgebase.web.graphql.dto.UpdateKnowledgeBaseSourceGraphQlInput;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the Knowledge Base source surface. Mutations route through
 * {@link WorkspaceKnowledgeBaseSourceFacade} — the same code path used by the AI Hub chat-define tools and the future
 * UI, so there is no parallel implementation to maintain. Workspace-scoped reads flow through
 * {@link WorkspaceKnowledgeBaseSourceService} which joins the {@code workspace_knowledge_base_source} relation table
 * with the platform-side source service. The {@code workspaceId} GraphQL field on {@link KnowledgeBaseSource} is
 * resolved via {@link SchemaMapping} since the entity itself no longer carries {@code workspace_id} after the
 * KB-to-platform pivot.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class KnowledgeBaseSourceGraphQlController {

    private final KnowledgeBaseSourceService knowledgeBaseSourceService;
    private final WorkspaceKnowledgeBaseSourceFacade workspaceKnowledgeBaseSourceFacade;
    private final WorkspaceKnowledgeBaseSourceService workspaceKnowledgeBaseSourceService;

    @SuppressFBWarnings("EI")
    KnowledgeBaseSourceGraphQlController(
        KnowledgeBaseSourceService knowledgeBaseSourceService,
        WorkspaceKnowledgeBaseSourceFacade workspaceKnowledgeBaseSourceFacade,
        WorkspaceKnowledgeBaseSourceService workspaceKnowledgeBaseSourceService) {

        this.knowledgeBaseSourceService = knowledgeBaseSourceService;
        this.workspaceKnowledgeBaseSourceFacade = workspaceKnowledgeBaseSourceFacade;
        this.workspaceKnowledgeBaseSourceService = workspaceKnowledgeBaseSourceService;
    }

    @QueryMapping
    public KnowledgeBaseSource knowledgeBaseSource(@Argument Long id) {
        return knowledgeBaseSourceService.fetch(id)
            .orElse(null);
    }

    @QueryMapping
    public List<KnowledgeBaseSource> knowledgeBaseSources(
        @Argument Long workspaceId, @Argument Long environmentId, @Argument KnowledgeBaseSourceFilter filter) {

        if (filter != null && Boolean.TRUE.equals(filter.enabled())) {
            return workspaceKnowledgeBaseSourceService.getAllEnabledSourcesByWorkspaceId(workspaceId, environmentId);
        }

        return workspaceKnowledgeBaseSourceService.getAllSourcesByWorkspaceId(workspaceId, environmentId);
    }

    @SchemaMapping(typeName = "KnowledgeBaseSource", field = "workspaceId")
    public Long workspaceId(KnowledgeBaseSource source) {
        return workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(source.getId())
            .orElse(null);
    }

    @MutationMapping
    public KnowledgeBaseSource createKnowledgeBaseSource(@Argument CreateKnowledgeBaseSourceGraphQlInput input) {
        return workspaceKnowledgeBaseSourceFacade.create(input.workspaceId(), input.toFacadeInput());
    }

    @MutationMapping
    public KnowledgeBaseSource updateKnowledgeBaseSource(
        @Argument Long id, @Argument UpdateKnowledgeBaseSourceGraphQlInput input) {

        Long workspaceId = workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(id)
            .orElseThrow(() -> new IllegalStateException(
                "KnowledgeBaseSource " + id + " has no owning workspace"));

        return workspaceKnowledgeBaseSourceFacade.update(workspaceId, id, input.toFacadeInput());
    }

    @MutationMapping
    public boolean deleteKnowledgeBaseSource(@Argument Long id) {
        Long workspaceId = workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(id)
            .orElseThrow(() -> new IllegalStateException(
                "KnowledgeBaseSource " + id + " has no owning workspace"));

        workspaceKnowledgeBaseSourceFacade.delete(workspaceId, id);

        return true;
    }

    @MutationMapping
    public Long refreshKnowledgeBaseSource(@Argument Long id) {
        Long workspaceId = workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(id)
            .orElseThrow(() -> new IllegalStateException(
                "KnowledgeBaseSource " + id + " has no owning workspace"));

        return workspaceKnowledgeBaseSourceFacade.refreshNow(workspaceId, id);
    }

    @MutationMapping
    public KnowledgeBaseSource setKnowledgeBaseSourceEnabled(@Argument Long id, @Argument boolean enabled) {
        Long workspaceId = workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(id)
            .orElseThrow(() -> new IllegalStateException(
                "KnowledgeBaseSource " + id + " has no owning workspace"));

        workspaceKnowledgeBaseSourceFacade.setEnabled(workspaceId, id, enabled);

        return knowledgeBaseSourceService.get(id);
    }
}
