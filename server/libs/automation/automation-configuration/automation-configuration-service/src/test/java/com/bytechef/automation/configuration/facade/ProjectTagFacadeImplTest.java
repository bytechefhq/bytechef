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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectTagFacadeImplTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ProjectTagFacadeImpl projectTagFacade;

    @Test
    void testGetProjectTagsScopesToWorkspace() {
        Project project = new Project();

        project.setTagIds(List.of(10L, 11L));

        when(projectService.getWorkspaceProjectIds(5L)).thenReturn(List.of(1L, 2L));
        when(projectService.getProjects(List.of(1L, 2L))).thenReturn(List.of(project));
        when(tagService.getTags(List.of(10L, 11L))).thenReturn(List.of(new Tag("a"), new Tag("b")));

        List<Tag> tags = projectTagFacade.getProjectTags(5L);

        assertThat(tags).hasSize(2);
    }
}
