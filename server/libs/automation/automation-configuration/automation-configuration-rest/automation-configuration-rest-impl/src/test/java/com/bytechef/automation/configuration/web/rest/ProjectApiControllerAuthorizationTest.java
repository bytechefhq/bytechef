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

package com.bytechef.automation.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.web.rest.model.ProjectVersionModel;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;

/**
 * Pins that the read surfaces of this controller reach their data through the facade layer, which is where this
 * codebase puts authorization.
 *
 * <p>
 * The controller carries no {@code @PreAuthorize} of its own and is not meant to: the API facade is the HTTP surface
 * and owns the gate. That convention has one failure mode, and {@code getProjectVersions} was an instance of it — a
 * controller method that calls a <em>service</em> directly compiles fine, reads as ordinary delegation, and silently
 * serves any project's version history to any authenticated caller, {@code PRIVATE} projects and other workspaces
 * included. Asserting that the facade was called is not enough on its own; the assertion that {@link ProjectService}
 * was never touched is what makes a revert to the service fail here rather than pass.
 *
 * @author Ivica Cardic
 */
class ProjectApiControllerAuthorizationTest {

    private static final long PROJECT_ID = 42L;

    private final ConversionService conversionService = mock(ConversionService.class);
    private final ProjectFacade projectFacade = mock(ProjectFacade.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private final ProjectApiController projectApiController = new ProjectApiController(
        conversionService, projectFacade, projectService);

    @Test
    void testGetProjectVersionsReadsThroughTheGuardedFacade() {
        ProjectVersion projectVersion = new ProjectVersion(
            1, ProjectVersion.Status.PUBLISHED.ordinal(), Instant.EPOCH, "First release");

        ProjectVersionModel projectVersionModel = new ProjectVersionModel();

        when(projectFacade.getProjectVersions(PROJECT_ID)).thenReturn(List.of(projectVersion));
        when(conversionService.convert(projectVersion, ProjectVersionModel.class)).thenReturn(projectVersionModel);

        ResponseEntity<List<ProjectVersionModel>> responseEntity = projectApiController.getProjectVersions(PROJECT_ID);

        assertThat(responseEntity.getBody()).containsExactly(projectVersionModel);

        verify(projectFacade).getProjectVersions(PROJECT_ID);

        // The whole point of the fix: the history must not be read off the unguarded service.
        verifyNoInteractions(projectService);
    }

    /**
     * {@code exportProject} does read the project row off the service, for the download's filename — but only after
     * {@code ProjectFacade.exportProject} has run its own {@code WORKFLOW_VIEW} gate. Pinning the order keeps a later
     * edit from hoisting the filename lookup above the gated call, which would reintroduce the same ungated by-id read
     * in a different method.
     */
    @Test
    void testExportProjectRunsTheGatedFacadeCallBeforeTouchingTheService() {
        Project project = new Project();

        project.setName("Reports");

        when(projectFacade.exportProject(PROJECT_ID)).thenReturn(new byte[0]);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        projectApiController.exportProject(PROJECT_ID);

        InOrder inOrder = inOrder(projectFacade, projectService);

        inOrder.verify(projectFacade)
            .exportProject(PROJECT_ID);
        inOrder.verify(projectService)
            .getProject(PROJECT_ID);
    }
}
