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

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins that the workspace deployment listing reaches its rows through the facade layer, which is where this codebase
 * puts authorization.
 *
 * <p>
 * The controller carries no {@code @PreAuthorize} of its own and is not meant to. That convention has one failure mode,
 * and this query was an instance of it: it read the rows straight off {@code ProjectDeploymentService}, past the facade
 * — which is why it shipped with neither the workspace gate nor the two filters its REST twin had all along, and
 * returned every deployment row in any workspace. Asserting that the facade was called is not enough on its own; the
 * assertion that {@link ProjectService} is never touched is what makes a revert to a locally assembled listing fail
 * here rather than pass.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentGraphQlControllerAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final TagService tagService = mock(TagService.class);

    private final ProjectDeploymentGraphQlController projectDeploymentGraphQlController =
        new ProjectDeploymentGraphQlController(
            environmentService, projectDeploymentFacade, projectService, tagService);

    @Test
    void testWorkspaceProjectDeploymentsReadsThroughTheGuardedFacade() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(10L);

        when(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, ENVIRONMENT_ID, null, null))
            .thenReturn(List.of(projectDeployment));

        List<ProjectDeployment> projectDeployments = projectDeploymentGraphQlController.workspaceProjectDeployments(
            WORKSPACE_ID, ENVIRONMENT_ID, null, null);

        assertThat(projectDeployments).containsExactly(projectDeployment);

        verify(projectDeploymentFacade).getWorkspaceProjectDeployments(WORKSPACE_ID, ENVIRONMENT_ID, null, null);

        verifyNoInteractions(environmentService, projectService, tagService);
    }
}
