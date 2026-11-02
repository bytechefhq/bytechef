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
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.test.service.TestJobRegistry;
import com.bytechef.platform.workflow.test.service.TestJobRegistry.TestJob;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Test jobs never reach the persistent job store, so these resolvers are what let a workspace editor attach to, stop,
 * and read the logs of their own test runs.
 *
 * @author Ivica Cardic
 */
class TestJobOwnershipResolverTest {

    private static final long JOB_ID = 5L;
    private static final long WORKSPACE_ID = 42L;
    private static final String WORKFLOW_ID = "workflow-uuid";

    private final ProjectService projectService = mock(ProjectService.class);
    private final TestJobRegistry testJobRegistry = mock(TestJobRegistry.class);

    private ObjectProvider<TestJobRegistry> testJobRegistryProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        testJobRegistryProvider = mock(ObjectProvider.class);

        when(testJobRegistryProvider.getIfAvailable()).thenReturn(testJobRegistry);
    }

    @Test
    void testResolvesTheWorkspaceOfTheProjectTheTestedWorkflowBelongsTo() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(testJobRegistry.fetchTestJob(JOB_ID)).thenReturn(Optional.of(new TestJob(WORKFLOW_ID, 0)));
        when(projectService.fetchWorkflowProject(WORKFLOW_ID)).thenReturn(Optional.of(project));

        TestJobOwnershipResolver resolver = new TestJobOwnershipResolver(projectService, testJobRegistryProvider);

        assertThat(resolver.resolveOwner(JOB_ID)
            .workspaceId()).hasValue(WORKSPACE_ID);
    }

    @Test
    void testAnUnknownTestJobHasNoOwner() {
        when(testJobRegistry.fetchTestJob(JOB_ID)).thenReturn(Optional.empty());

        TestJobOwnershipResolver resolver = new TestJobOwnershipResolver(projectService, testJobRegistryProvider);

        assertThat(resolver.resolveOwner(JOB_ID)
            .workspaceId()).isEmpty();
    }

    @Test
    void testADeploymentWithoutEditorTestsHasNoOwner() {
        when(testJobRegistryProvider.getIfAvailable()).thenReturn(null);

        TestJobOwnershipResolver resolver = new TestJobOwnershipResolver(projectService, testJobRegistryProvider);

        assertThat(resolver.resolveOwner(JOB_ID)
            .workspaceId()).isEmpty();
    }

    @Test
    void testReportsTheEnvironmentTheTestRanIn() {
        when(testJobRegistry.fetchTestJob(JOB_ID))
            .thenReturn(Optional.of(new TestJob(WORKFLOW_ID, Environment.PRODUCTION.ordinal())));

        TestJobEnvironmentResolver resolver = new TestJobEnvironmentResolver(testJobRegistryProvider);

        assertThat(resolver.fetchEnvironment(JOB_ID)).contains(Environment.PRODUCTION);
    }
}
