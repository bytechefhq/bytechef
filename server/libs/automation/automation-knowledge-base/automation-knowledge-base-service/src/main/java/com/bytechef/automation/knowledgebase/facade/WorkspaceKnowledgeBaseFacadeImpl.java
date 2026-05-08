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
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.configuration.domain.Environment;
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
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class WorkspaceKnowledgeBaseFacadeImpl implements WorkspaceKnowledgeBaseFacade {

    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseService knowledgeBaseService;

    @SuppressFBWarnings("EI")
    public WorkspaceKnowledgeBaseFacadeImpl(
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, KnowledgeBaseService knowledgeBaseService) {

        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getWorkspaceKnowledgeBases(Long workspaceId, long environmentId) {
        return knowledgeBaseService.getKnowledgeBases(workspaceId, (int) environmentId);
    }

    @Override
    public KnowledgeBase createWorkspaceKnowledgeBase(
        KnowledgeBase knowledgeBase, Long workspaceId, long environmentId) {

        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            throw new IllegalArgumentException("Invalid environmentId: " + environmentId);
        }

        knowledgeBase.setEnvironment(environments[(int) environmentId]);
        knowledgeBase.setWorkspaceId(workspaceId);

        return knowledgeBaseService.createKnowledgeBase(knowledgeBase);
    }

    @Override
    public void deleteWorkspaceKnowledgeBase(Long knowledgeBaseId) {
        List<KnowledgeBaseDocument> documents = knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseId);

        for (KnowledgeBaseDocument document : documents) {
            knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(document.getId());
        }

        knowledgeBaseService.deleteKnowledgeBase(knowledgeBaseId);
    }
}
