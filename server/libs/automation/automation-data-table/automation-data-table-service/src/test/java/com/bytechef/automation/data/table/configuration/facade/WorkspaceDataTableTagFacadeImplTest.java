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

package com.bytechef.automation.data.table.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
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
class WorkspaceDataTableTagFacadeImplTest {

    @Mock
    private DataTableService dataTableService;

    @Mock
    private DataTableTagService dataTableTagService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private WorkspaceDataTableFacadeImpl workspaceDataTableFacade;

    @Test
    void testGetDataTableTagsScopesToWorkspace() {
        when(dataTableService.getWorkspaceDataTables(5L))
            .thenReturn(List.of(new DataTable(1L, "orders"), new DataTable(2L, "customers")));
        when(dataTableTagService.getTagIds(List.of(1L, 2L))).thenReturn(List.of(10L, 11L));
        when(tagService.getTags(List.of(10L, 11L))).thenReturn(List.of(new Tag("a"), new Tag("b")));

        List<Tag> tags = workspaceDataTableFacade.getDataTableTags(5L);

        assertThat(tags).hasSize(2);

        verify(dataTableTagService).getTagIds(List.of(1L, 2L));
    }

    @Test
    void testGetDataTableTagsSkipsTagLookupWhenNoTagIds() {
        when(dataTableService.getWorkspaceDataTables(5L)).thenReturn(List.of(new DataTable(1L, "orders")));
        when(dataTableTagService.getTagIds(List.of(1L))).thenReturn(List.of());

        List<Tag> tags = workspaceDataTableFacade.getDataTableTags(5L);

        assertThat(tags).isEmpty();

        verify(tagService, never()).getTags(anyList());
    }

    @Test
    void testGetTagsByTableIdResolvesTagsForWorkspaceTables() {
        Tag firstTag = new Tag(10L, "a");
        Tag secondTag = new Tag(11L, "b");

        when(dataTableTagService.getTagIdsByTableId())
            .thenReturn(Map.of(1L, List.of(10L, 11L), 2L, List.of(), 3L, List.of(12L)));
        when(dataTableService.getWorkspaceDataTables(5L))
            .thenReturn(List.of(new DataTable(1L, "orders"), new DataTable(2L, "customers")));
        when(tagService.getTags(List.of(10L, 11L))).thenReturn(List.of(firstTag, secondTag));

        Map<Long, List<Tag>> tagsByTableId = workspaceDataTableFacade.getTagsByTableId(5L);

        assertThat(tagsByTableId).containsOnlyKeys(1L, 2L);
        assertThat(tagsByTableId.get(1L)).containsExactly(firstTag, secondTag);
        assertThat(tagsByTableId.get(2L)).isEmpty();
    }

    @Test
    void testUpdateTagsSavesNewTagsAndStoresAllTagIds() {
        Tag existingTag = new Tag(10L, "existing");
        Tag newTag = new Tag("new");
        Tag savedTag = new Tag(11L, "new");

        when(tagService.save(List.of(newTag))).thenReturn(List.of(savedTag));

        workspaceDataTableFacade.updateTags(1L, List.of(existingTag, newTag));

        verify(dataTableTagService).updateTagIds(1L, List.of(10L, 11L));
    }

    @Test
    void testUpdateTagsWithNoTagsClearsTagIds() {
        workspaceDataTableFacade.updateTags(1L, List.of());

        verify(dataTableTagService).updateTagIds(1L, List.of());
        verify(tagService, never()).save(anyList());
    }
}
