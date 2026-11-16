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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseDocumentNotFoundException;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseDocumentOwnershipResolverTest {

    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);
    private final WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService =
        mock(WorkspaceKnowledgeBaseService.class);

    private final KnowledgeBaseDocumentOwnershipResolver knowledgeBaseDocumentOwnershipResolver =
        new KnowledgeBaseDocumentOwnershipResolver(knowledgeBaseDocumentService, workspaceKnowledgeBaseService);

    @Test
    void testResourceTypeMatchesThePreAuthorizeToken() {
        assertThat(knowledgeBaseDocumentOwnershipResolver.resourceType()).isEqualTo("KnowledgeBaseDocument");
    }

    @Test
    void testResolveOwnerOfAMissingDocumentIsUnknown() {
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(1302L))
            .thenThrow(new KnowledgeBaseDocumentNotFoundException(1302L));

        assertThat(knowledgeBaseDocumentOwnershipResolver.resolveOwner(1302L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerOfADocumentWithoutAKnowledgeBaseIsUnknown() {
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(1302L)).thenReturn(new KnowledgeBaseDocument());

        assertThat(knowledgeBaseDocumentOwnershipResolver.resolveOwner(1302L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerOfADocumentWhoseKnowledgeBaseHasNoWorkspaceIsUnknown() {
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(1302L)).thenReturn(newKnowledgeBaseDocument());
        when(workspaceKnowledgeBaseService.fetchWorkspaceId(1301L)).thenReturn(Optional.empty());

        assertThat(knowledgeBaseDocumentOwnershipResolver.resolveOwner(1302L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceOfTheDocumentKnowledgeBase() {
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(1302L)).thenReturn(newKnowledgeBaseDocument());
        when(workspaceKnowledgeBaseService.fetchWorkspaceId(1301L)).thenReturn(Optional.of(7L));

        assertThat(knowledgeBaseDocumentOwnershipResolver.resolveOwner(1302L))
            .isEqualTo(ResourceOwner.ofWorkspace(7L));
    }

    private static KnowledgeBaseDocument newKnowledgeBaseDocument() {
        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setKnowledgeBaseId(1301L);

        return knowledgeBaseDocument;
    }
}
