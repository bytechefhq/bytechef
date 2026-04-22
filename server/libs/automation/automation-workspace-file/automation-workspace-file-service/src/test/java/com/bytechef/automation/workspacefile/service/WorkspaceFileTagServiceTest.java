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

package com.bytechef.automation.workspacefile.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.repository.WorkspaceFileRepository;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceFileTagServiceTest {

    @Mock
    private WorkspaceFileRepository workspaceFileRepository;

    @Mock
    private TagService tagService;

    private WorkspaceFileTagService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceFileTagServiceImpl(workspaceFileRepository, tagService);
    }

    @Test
    void testUpdateTagsAssignsResolvedTagIdsAndSaves() {
        WorkspaceFile workspaceFile = new WorkspaceFile();

        workspaceFile.setId(5L);
        workspaceFile.setName("spec.md");

        Tag existingTag = new Tag("existing");

        existingTag.setId(10L);

        when(workspaceFileRepository.findById(5L)).thenReturn(Optional.of(workspaceFile));
        when(workspaceFileRepository.save(any(WorkspaceFile.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateTags(5L, List.of(existingTag));

        verify(workspaceFileRepository).save(argThat(saved -> saved.getTagIds()
            .contains(10L)));
    }

    @Test
    void testUpdateTagsThrowsWhenWorkspaceFileMissing() {
        when(workspaceFileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTags(99L, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
