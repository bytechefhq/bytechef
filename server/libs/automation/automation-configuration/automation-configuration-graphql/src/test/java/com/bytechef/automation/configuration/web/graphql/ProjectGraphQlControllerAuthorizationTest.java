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

package com.bytechef.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.tag.service.TagService;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins that both project read surfaces reach their rows through the facade layer, which is where this codebase puts
 * authorization.
 *
 * <p>
 * The controller carries no {@code @PreAuthorize} of its own and is not meant to. That convention has one failure mode,
 * and both of these queries were instances of it: they assembled their answers here, out of {@link ProjectService},
 * {@link PermissionService} and {@link ProjectVisibilityFilter} directly, past the facade — so the by-id read grew a
 * hand-written {@code hasResourceScope} check with a hand-thrown {@code AccessDeniedException}, and the listing grew
 * the whole two-halves narrowing, neither of which anything but this controller could inherit or audit.
 *
 * <p>
 * Verifying that the facade was called is not enough on its own: a controller could call the facade and still keep a
 * local check beside it. {@link #testTheControllerCannotReachPastTheFacade()} is what closes that — those three
 * collaborators are no longer constructor parameters at all, so there is nothing left here to run a check with.
 *
 * @author Ivica Cardic
 */
class ProjectGraphQlControllerAuthorizationTest {

    private static final long PROJECT_ID = 1L;

    private final CategoryService categoryService = mock(CategoryService.class);
    private final ProjectFacade projectFacade = mock(ProjectFacade.class);
    private final TagService tagService = mock(TagService.class);

    private final ProjectGraphQlController projectGraphQlController = new ProjectGraphQlController(
        categoryService, projectFacade, tagService);

    @Test
    void testProjectReadsThroughTheGuardedFacade() {
        Project project = project(PROJECT_ID);

        when(projectFacade.getProjectRow(PROJECT_ID)).thenReturn(project);

        assertThat(projectGraphQlController.project(PROJECT_ID)).isSameAs(project);

        verify(projectFacade).getProjectRow(PROJECT_ID);

        verifyNoInteractions(categoryService, tagService);
    }

    @Test
    void testProjectsReadThroughTheGuardedFacade() {
        Project project = project(PROJECT_ID);

        when(projectFacade.getProjectRows()).thenReturn(List.of(project));

        assertThat(projectGraphQlController.projects()).containsExactly(project);

        verify(projectFacade).getProjectRows();

        verifyNoInteractions(categoryService, tagService);
    }

    @Test
    void testTheControllerCannotReachPastTheFacade() {
        Constructor<?>[] constructors = ProjectGraphQlController.class.getConstructors();

        assertThat(constructors).hasSize(1);

        assertThat(constructors[0].getParameterTypes())
            .describedAs(
                "a collaborator that can answer an authorization question is a collaborator this controller can run " +
                    "one with — the facade is the only thing it may reach for a project")
            .doesNotContain(PermissionService.class, ProjectService.class, ProjectVisibilityFilter.class);
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);

        return project;
    }
}
