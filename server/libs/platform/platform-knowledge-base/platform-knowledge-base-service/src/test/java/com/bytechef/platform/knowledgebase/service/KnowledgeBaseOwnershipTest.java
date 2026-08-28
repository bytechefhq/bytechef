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

package com.bytechef.platform.knowledgebase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.OwnerType;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.owner.Owner;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseOwnershipTest {

    private static final Owner ACCOUNT_A = Owner.connectedUser(1L);

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @InjectMocks
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    @Test
    void testAnOwnedKnowledgeBaseIsRefusedToAnotherAccount() {
        when(knowledgeBaseRepository.findById(7L)).thenReturn(Optional.of(ownedBy(2L)));

        RuntimeException exception = assertThrows(
            RuntimeException.class, () -> knowledgeBaseService.getKnowledgeBase(7L, Optional.of(ACCOUNT_A)));

        // Same message as a genuinely missing knowledge base, so ids cannot be probed for existence.
        assertEquals("KnowledgeBase not found: 7", exception.getMessage());
    }

    @Test
    void testAnOwnedKnowledgeBaseIsReadableByItsOwner() {
        when(knowledgeBaseRepository.findById(7L)).thenReturn(Optional.of(ownedBy(1L)));

        assertNotNull(knowledgeBaseService.getKnowledgeBase(7L, Optional.of(ACCOUNT_A)));
    }

    @Test
    void testAnUnownedKnowledgeBaseIsReadableByAnyAccount() {
        when(knowledgeBaseRepository.findById(7L)).thenReturn(Optional.of(new KnowledgeBase()));

        assertNotNull(knowledgeBaseService.getKnowledgeBase(7L, Optional.of(ACCOUNT_A)));
    }

    @Test
    void testAnAdminWithNoOwnerReadsAnything() {
        when(knowledgeBaseRepository.findById(7L)).thenReturn(Optional.of(ownedBy(2L)));

        assertNotNull(knowledgeBaseService.getKnowledgeBase(7L, Optional.empty()));
    }

    @Test
    void testListingHidesOtherAccountsKnowledgeBases() {
        when(knowledgeBaseRepository.findAllByEnvironment(2))
            .thenReturn(List.of(ownedBy(1L), ownedBy(2L), new KnowledgeBase()));

        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases(2, Optional.of(ACCOUNT_A));

        assertEquals(2, knowledgeBases.size());
    }

    private static KnowledgeBase ownedBy(long ownerId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setOwnerType(OwnerType.CONNECTED_USER);

        return knowledgeBase;
    }
}
