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

package com.bytechef.automation.knowledgebase.facade;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceKnowledgeBaseFacade} interface that handles workspace knowledge base
 * operations.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
public class WorkspaceKnowledgeBaseFacadeImpl implements WorkspaceKnowledgeBaseFacade {

    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService;

    @SuppressFBWarnings("EI")
    public WorkspaceKnowledgeBaseFacadeImpl(
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, KnowledgeBaseService knowledgeBaseService,
        WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService) {

        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.workspaceKnowledgeBaseService = workspaceKnowledgeBaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getWorkspaceKnowledgeBases(Long workspaceId) {
        List<WorkspaceKnowledgeBase> workspaceKnowledgeBases =
            workspaceKnowledgeBaseService.getWorkspaceKnowledgeBases(workspaceId);

        return workspaceKnowledgeBases.stream()
            .map(workspaceKnowledgeBase -> knowledgeBaseService.getKnowledgeBase(
                workspaceKnowledgeBase.getKnowledgeBaseId()))
            .toList();
    }

    @Override
    public KnowledgeBase createWorkspaceKnowledgeBase(KnowledgeBase knowledgeBase, Long workspaceId) {
        KnowledgeBase createdKnowledgeBase = knowledgeBaseService.createKnowledgeBase(knowledgeBase);

        workspaceKnowledgeBaseService.assignKnowledgeBaseToWorkspace(createdKnowledgeBase.getId(), workspaceId);

        return createdKnowledgeBase;
    }

    @Override
    public void deleteWorkspaceKnowledgeBase(Long knowledgeBaseId) {
        List<KnowledgeBaseDocument> documents = knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseId);

        for (KnowledgeBaseDocument document : documents) {
            knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(document.getId());
        }

        workspaceKnowledgeBaseService.removeKnowledgeBaseFromWorkspace(knowledgeBaseId);

        knowledgeBaseService.deleteKnowledgeBase(knowledgeBaseId);
    }
}
