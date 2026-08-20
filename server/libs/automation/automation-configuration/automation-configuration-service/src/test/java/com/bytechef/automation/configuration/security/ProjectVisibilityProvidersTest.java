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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.io.Serializable;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pins the inheritance invariant: a project's children carry no visibility of their own, so every provider here answers
 * with the OWNING PROJECT's record under the {@code "Project"} type. A child that returned its own id would look up
 * grants that do not exist and hide a project its owner had shared.
 *
 * @author Ivica Cardic
 */
class ProjectVisibilityProvidersTest {

    private static final long PROJECT_ID = 5L;
    private static final long PROJECT_WORKFLOW_ID = 8L;
    private static final long PROJECT_DEPLOYMENT_ID = 9L;
    private static final String WORKFLOW_ID = "wf-1";

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectWorkflowRepository projectWorkflowRepository = mock(ProjectWorkflowRepository.class);
    private final ProjectDeploymentRepository projectDeploymentRepository = mock(ProjectDeploymentRepository.class);

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();

        project.setId(PROJECT_ID);
        project.setVisibility(ResourceVisibility.PRIVATE);

        // created_by is @CreatedBy-managed; the test seeds it the way the persistence layer would
        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(Optional.of(project));

        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(PROJECT_ID);
        when(projectWorkflowRepository.findById(PROJECT_WORKFLOW_ID)).thenReturn(Optional.of(projectWorkflow));

        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);

        when(projectDeployment.getProjectId()).thenReturn(PROJECT_ID);
        when(projectDeploymentRepository.findById(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.of(projectDeployment));
    }

    @Test
    void testProjectProviderReturnsOwnRecord() {
        ProjectVisibilityProvider provider = new ProjectVisibilityProvider(projectRepository);

        assertThat(provider.resourceType()).isEqualTo("Project");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(PROJECT_ID)).contains(projectRecord());
        assertThat(provider.fetchVisibility(999L)).isEmpty();
    }

    @Test
    void testWorkflowProviderInheritsProjectRecordUnderStringId() {
        WorkflowVisibilityProvider provider = new WorkflowVisibilityProvider(projectRepository);

        assertThat(provider.resourceType()).isEqualTo("Workflow");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility((Serializable) WORKFLOW_ID)).contains(projectRecord());
        assertThat(provider.fetchVisibility(1L)).as("workflow ids are strings; a long is not a workflow")
            .isEmpty();
        assertThat(provider.fetchVisibility((Serializable) "unknown")).isEmpty();
    }

    @Test
    void testProjectWorkflowProviderInheritsProjectRecord() {
        ProjectWorkflowVisibilityProvider provider =
            new ProjectWorkflowVisibilityProvider(projectRepository, projectWorkflowRepository);

        assertThat(provider.resourceType()).isEqualTo("ProjectWorkflow");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(PROJECT_WORKFLOW_ID)).contains(projectRecord());
        assertThat(provider.fetchVisibility(999L)).as("an unknown project workflow must fail closed")
            .isEmpty();
    }

    @Test
    void testProjectDeploymentProviderInheritsProjectRecord() {
        ProjectDeploymentVisibilityProvider provider =
            new ProjectDeploymentVisibilityProvider(projectDeploymentRepository, projectRepository);

        assertThat(provider.resourceType()).isEqualTo("ProjectDeployment");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(PROJECT_DEPLOYMENT_ID)).contains(projectRecord());
        assertThat(provider.fetchVisibility(999L)).as("an unknown deployment must fail closed")
            .isEmpty();
    }

    private static VisibilityRecord projectRecord() {
        return new VisibilityRecord(PROJECT_ID, ResourceVisibility.PRIVATE, "ivica");
    }
}
