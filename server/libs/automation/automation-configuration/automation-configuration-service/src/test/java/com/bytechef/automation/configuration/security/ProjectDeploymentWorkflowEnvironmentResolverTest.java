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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * An empty answer here is not a denial — {@code hasResourceScope} falls back to the environment-unaware check — so the
 * cases that matter are that a known row reports its deployment's environment, and that the empty answers are only
 * reached for ids that cannot name a row at all.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentWorkflowEnvironmentResolverTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 77L;

    private final ProjectDeploymentRepository projectDeploymentRepository = mock(ProjectDeploymentRepository.class);
    private final ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository =
        mock(ProjectDeploymentWorkflowRepository.class);
    private final ProjectDeploymentWorkflowEnvironmentResolver resolver =
        new ProjectDeploymentWorkflowEnvironmentResolver(
            projectDeploymentRepository, projectDeploymentWorkflowRepository);

    @Test
    void testResourceTypeMatchesTheTokenTheDeploymentWorkflowGuardNames() {
        assertThat(resolver.resourceType()).isEqualTo("ProjectDeploymentWorkflow");
    }

    @Test
    void testFetchEnvironmentReturnsTheEnvironmentOfTheRowsDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        givenRowBelongingToDeployment();

        when(projectDeploymentRepository.findById(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.of(projectDeployment));

        assertThat(resolver.fetchEnvironment(PROJECT_DEPLOYMENT_WORKFLOW_ID)).contains(Environment.PRODUCTION);
    }

    @Test
    void testFetchEnvironmentIsEmptyForAnUnknownRow() {
        when(projectDeploymentWorkflowRepository.findById(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(PROJECT_DEPLOYMENT_WORKFLOW_ID)).isEmpty();
    }

    @Test
    void testFetchEnvironmentIsEmptyForANonNumericId() {
        assertThat(resolver.fetchEnvironment("not-a-number")).isEmpty();
    }

    private void givenRowBelongingToDeployment() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        when(projectDeploymentWorkflowRepository.findById(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(Optional.of(projectDeploymentWorkflow));
    }
}
