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

package com.bytechef.platform.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseTagFacadeTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private KnowledgeBaseTagFacadeImpl knowledgeBaseTagFacade;

    @Mock
    private TagService tagService;

    @Test
    void testGetTagsReadsTheKnowledgeBasesThroughTheService() {
        Tag tag = new Tag(10L, "a");

        when(knowledgeBaseService.getKnowledgeBases(List.of(1L)))
            .thenReturn(List.of(createKnowledgeBase(1L, List.of(10L))));
        when(tagService.getTags(List.of(10L))).thenReturn(List.of(tag));

        assertThat(knowledgeBaseTagFacade.getTags(List.of(1L))).containsExactly(tag);
    }

    @Test
    void testGetTagsSkipsTheTagLookupWhenNoKnowledgeBaseHasTags() {
        when(knowledgeBaseService.getKnowledgeBases(List.of(1L)))
            .thenReturn(List.of(createKnowledgeBase(1L, List.of())));

        assertThat(knowledgeBaseTagFacade.getTags(List.of(1L))).isEmpty();

        verify(tagService, never()).getTags(anyList());
    }

    @Test
    void testGetTagsByKnowledgeBaseIdResolvesTagsPerKnowledgeBase() {
        Tag tag = new Tag(10L, "a");

        when(knowledgeBaseService.getKnowledgeBases(List.of(1L, 2L)))
            .thenReturn(List.of(createKnowledgeBase(1L, List.of(10L)), createKnowledgeBase(2L, List.of())));
        when(tagService.getTags(List.of(10L))).thenReturn(List.of(tag));

        Map<Long, List<Tag>> tagsByKnowledgeBaseId = knowledgeBaseTagFacade.getTagsByKnowledgeBaseId(List.of(1L, 2L));

        assertThat(tagsByKnowledgeBaseId).containsOnlyKeys(1L, 2L);
        assertThat(tagsByKnowledgeBaseId.get(1L)).containsExactly(tag);
        assertThat(tagsByKnowledgeBaseId.get(2L)).isEmpty();
    }

    @Test
    void testUpdateTagsSavesNewTagsAndStoresAllTagIds() {
        Tag existingTag = new Tag(10L, "existing");
        Tag newTag = new Tag("new");
        Tag savedTag = new Tag(11L, "new");

        when(tagService.save(List.of(newTag))).thenReturn(List.of(savedTag));

        knowledgeBaseTagFacade.updateTags(1L, List.of(existingTag, newTag));

        verify(knowledgeBaseService).updateKnowledgeBaseTagIds(1L, List.of(10L, 11L));
    }

    @Test
    void testUpdateTagsWithNoTagsClearsTheTagIds() {
        knowledgeBaseTagFacade.updateTags(1L, List.of());

        verify(knowledgeBaseService).updateKnowledgeBaseTagIds(1L, List.of());
        verify(tagService, never()).save(anyList());
    }

    private static KnowledgeBase createKnowledgeBase(long id, List<Long> tagIds) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(id);
        knowledgeBase.setTagIds(tagIds);

        return knowledgeBase;
    }
}
