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

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseTagFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;
    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService;
    private final KnowledgeBaseFacade knowledgeBaseFacade;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseTagFacade knowledgeBaseTagFacade;
    private final WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService;

    @SuppressFBWarnings("EI")
    public WorkspaceKnowledgeBaseFacadeImpl(
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService, KnowledgeBaseFacade knowledgeBaseFacade,
        KnowledgeBaseService knowledgeBaseService, KnowledgeBaseTagFacade knowledgeBaseTagFacade,
        WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService) {

        this.knowledgeBaseDocumentChunkFacade = knowledgeBaseDocumentChunkFacade;
        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseDocumentTagService = knowledgeBaseDocumentTagService;
        this.knowledgeBaseFacade = knowledgeBaseFacade;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeBaseTagFacade = knowledgeBaseTagFacade;
        this.workspaceKnowledgeBaseService = workspaceKnowledgeBaseService;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'KNOWLEDGE_BASE_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getWorkspaceKnowledgeBases(Long workspaceId, long environmentId) {
        List<WorkspaceKnowledgeBase> workspaceKnowledgeBases =
            workspaceKnowledgeBaseService.getWorkspaceKnowledgeBases(workspaceId);

        return workspaceKnowledgeBases.stream()
            .map(workspaceKnowledgeBase -> knowledgeBaseService.getKnowledgeBase(
                workspaceKnowledgeBase.getKnowledgeBaseId()))
            .filter(knowledgeBase -> knowledgeBase.getEnvironmentId() == environmentId)
            .toList();
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'KNOWLEDGE_BASE_CREATE', #environmentId)")
    public KnowledgeBase createWorkspaceKnowledgeBase(
        KnowledgeBase knowledgeBase, Long workspaceId, long environmentId) {

        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            throw new IllegalArgumentException("Invalid environmentId: " + environmentId);
        }

        knowledgeBase.setEnvironment(environments[(int) environmentId]);

        KnowledgeBase createdKnowledgeBase = knowledgeBaseService.createKnowledgeBase(knowledgeBase);

        workspaceKnowledgeBaseService.assignKnowledgeBaseToWorkspace(createdKnowledgeBase.getId(), workspaceId);

        return createdKnowledgeBase;
    }

    @Override
    public KnowledgeBase cloneWorkspaceKnowledgeBase(
        Long knowledgeBaseId, Long workspaceId, long targetEnvironmentId, @Nullable String newName) {

        Environment[] environments = Environment.values();

        if (targetEnvironmentId < 0 || targetEnvironmentId >= environments.length) {
            throw new IllegalArgumentException("Invalid targetEnvironmentId: " + targetEnvironmentId);
        }

        // Defense in depth: ensure the source knowledge base actually belongs to the supplied workspace before
        // cloning. A forged knowledge base id from another workspace must not be reachable via the LLM-supplied
        // input. Membership is enforced via the workspace_knowledge_base relation table.
        boolean belongs = workspaceKnowledgeBaseService.getWorkspaceKnowledgeBases(workspaceId)
            .stream()
            .anyMatch(workspaceKnowledgeBase -> Objects.equals(workspaceKnowledgeBase.getKnowledgeBaseId(),
                knowledgeBaseId));

        if (!belongs) {
            throw new IllegalArgumentException(
                "Knowledge base " + knowledgeBaseId + " not found in workspace " + workspaceId);
        }

        KnowledgeBase source = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);

        // Build a fresh KnowledgeBase rather than mutating the source — JDBC repositories interpret a non-null id as
        // an UPDATE, which would silently rewrite the source row in PROD instead of inserting a new clone.
        KnowledgeBase clone = new KnowledgeBase();

        clone.setName(newName != null && !newName.isBlank() ? newName : source.getName());
        clone.setDescription(source.getDescription());
        clone.setMaxChunkSize(source.getMaxChunkSize());
        clone.setMinChunkSizeChars(source.getMinChunkSizeChars());
        clone.setOverlap(source.getOverlap());
        clone.setEnvironment(environments[(int) targetEnvironmentId]);

        // Documents intentionally NOT copied. Knowledge base ingestion fans out to async embedding jobs that hit
        // the configured embedding model — copying them in a single facade call would silently spin up a potentially
        // expensive batch of LLM calls, which the user did not consent to. The destination KB starts empty; the
        // caller can ingest documents explicitly with addKnowledgeBaseDocument.

        KnowledgeBase created = knowledgeBaseService.createKnowledgeBase(clone);

        workspaceKnowledgeBaseService.assignKnowledgeBaseToWorkspace(created.getId(), workspaceId);

        return created;
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_DELETE')")
    public void deleteWorkspaceKnowledgeBase(Long knowledgeBaseId) {
        List<KnowledgeBaseDocument> documents = knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseId);

        for (KnowledgeBaseDocument document : documents) {
            knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(document.getId());
        }

        workspaceKnowledgeBaseService.removeKnowledgeBaseFromWorkspace(knowledgeBaseId);

        knowledgeBaseService.deleteKnowledgeBase(knowledgeBaseId);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_EDIT')")
    public void deleteKnowledgeBaseDocument(long id) {
        knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBaseDocumentChunk', 'KNOWLEDGE_BASE_EDIT')")
    public void deleteKnowledgeBaseDocumentChunk(long id) {
        knowledgeBaseDocumentChunkFacade.deleteKnowledgeBaseDocumentChunk(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBase', 'KNOWLEDGE_BASE_VIEW')")
    @Transactional(readOnly = true)
    public KnowledgeBase getKnowledgeBase(long id) {
        return knowledgeBaseService.getKnowledgeBase(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_VIEW')")
    @Transactional(readOnly = true)
    public KnowledgeBaseDocument getKnowledgeBaseDocument(long id) {
        return knowledgeBaseDocumentService.getKnowledgeBaseDocument(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_VIEW')")
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunks(long id) {
        return knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_VIEW')")
    @Transactional(readOnly = true)
    public DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long id) {
        return knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(id);
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_VIEW')")
    @Transactional(readOnly = true)
    public List<String> getKnowledgeBaseDocumentTagNames(long knowledgeBaseId) {
        return knowledgeBaseDocumentTagService.getTagNamesByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_VIEW')")
    @Transactional(readOnly = true)
    public Map<Long, List<String>> getKnowledgeBaseDocumentTagNamesByDocument(long knowledgeBaseId) {
        return knowledgeBaseDocumentTagService.getTagNamesByKnowledgeBaseDocumentId(knowledgeBaseId);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBase', 'KNOWLEDGE_BASE_VIEW')")
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocumentChunk> searchKnowledgeBase(long id, String query, String metadataFilters) {
        return knowledgeBaseFacade.searchKnowledgeBase(id, query, metadataFilters);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBase', 'KNOWLEDGE_BASE_EDIT')")
    public KnowledgeBase updateKnowledgeBase(long id, KnowledgeBase knowledgeBase) {
        return knowledgeBaseService.updateKnowledgeBase(id, knowledgeBase);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'KnowledgeBaseDocumentChunk', 'KNOWLEDGE_BASE_EDIT')")
    public KnowledgeBaseDocumentChunk updateKnowledgeBaseDocumentChunk(long id, String content) {
        return knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(id, content);
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseDocumentId, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_EDIT')")
    public void updateKnowledgeBaseDocumentTags(long knowledgeBaseDocumentId, List<String> tagNames) {
        knowledgeBaseDocumentFacade.updateKnowledgeBaseDocumentTags(knowledgeBaseDocumentId, tagNames);
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_EDIT')")
    public void updateKnowledgeBaseTags(long knowledgeBaseId, List<Tag> tags) {
        knowledgeBaseTagFacade.updateTags(knowledgeBaseId, tags);
    }
}
