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

package com.bytechef.automation.configuration.teardown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectDeploymentTenantTeardownContributorTest {

    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentTenantTeardownContributor contributor =
        new ProjectDeploymentTenantTeardownContributor(projectDeploymentFacade, projectDeploymentService);

    @Test
    void testDisablesEveryEnabledDeployment() {
        when(projectDeploymentService.getProjectDeployments())
            .thenReturn(List.of(createProjectDeployment(1L, true), createProjectDeployment(2L, false)));

        assertThat(contributor.onDeletionRequested("000001")).isEmpty();

        verify(projectDeploymentFacade).enableProjectDeployment(1L, false);
        verify(projectDeploymentFacade, never()).enableProjectDeployment(2L, false);
    }

    @Test
    void testFailureOnOneDeploymentDoesNotStopTheNext() {
        when(projectDeploymentService.getProjectDeployments())
            .thenReturn(List.of(createProjectDeployment(1L, true), createProjectDeployment(3L, true)));

        doThrow(new IllegalStateException("webhook unregister failed"))
            .when(projectDeploymentFacade)
            .enableProjectDeployment(1L, false);

        assertThat(contributor.onDeletionRequested("000001"))
            .containsExactly("projectDeployment 1: webhook unregister failed");

        verify(projectDeploymentFacade).enableProjectDeployment(3L, false);
    }

    private static ProjectDeployment createProjectDeployment(long id, boolean enabled) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(enabled);
        projectDeployment.setId(id);

        return projectDeployment;
    }
}
