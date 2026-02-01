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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class KnowledgeBaseGraphQlController {

    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseFacade knowledgeBaseFacade;
    private final KnowledgeBaseService knowledgeBaseService;
    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;

    @SuppressFBWarnings("EI")
    KnowledgeBaseGraphQlController(
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, KnowledgeBaseFacade knowledgeBaseFacade,
        KnowledgeBaseService knowledgeBaseService, WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade) {

        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseFacade = knowledgeBaseFacade;
        this.knowledgeBaseService = knowledgeBaseService;
        this.workspaceKnowledgeBaseFacade = workspaceKnowledgeBaseFacade;
    }

    @SchemaMapping(typeName = "KnowledgeBase", field = "documents")
    List<KnowledgeBaseDocument> documents(KnowledgeBase knowledgeBase) {
        return knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBase.getId());
    }

    @QueryMapping
    List<KnowledgeBase> knowledgeBases(@Argument Long workspaceId) {
        return workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(workspaceId);
    }

    @QueryMapping
    KnowledgeBase knowledgeBase(@Argument Long id) {
        return knowledgeBaseService.getKnowledgeBase(id);
    }

    @QueryMapping
    List<KnowledgeBaseDocumentChunk> searchKnowledgeBase(
        @Argument Long id, @Argument String query, @Argument String metadataFilters) {

        return knowledgeBaseFacade.searchKnowledgeBase(id, query, metadataFilters);
    }

    @MutationMapping
    KnowledgeBase createKnowledgeBase(@Argument KnowledgeBase knowledgeBase, @Argument Long workspaceId) {
        return workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(knowledgeBase, workspaceId);
    }

    @MutationMapping
    KnowledgeBase updateKnowledgeBase(@Argument Long id, @Argument KnowledgeBase knowledgeBase) {
        return knowledgeBaseService.updateKnowledgeBase(id, knowledgeBase);
    }

    @MutationMapping
    boolean deleteKnowledgeBase(@Argument Long id) {
        workspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(id);

        return true;
    }
}
