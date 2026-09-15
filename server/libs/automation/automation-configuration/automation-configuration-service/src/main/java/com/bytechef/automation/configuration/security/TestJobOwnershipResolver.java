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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.workflow.test.service.TestJobRegistry;
import com.bytechef.platform.workflow.test.service.TestJobRegistry.TestJob;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Maps an editor test job id to its owning workspace: test job &rarr; the workflow it ran (from
 * {@link TestJobRegistry}) &rarr; owning {@link Project} &rarr; {@code project.workspace_id}. Fails closed for an
 * unknown job, a workflow that belongs to no project, and a deployment that does not run editor tests at all.
 *
 * @author Ivica Cardic
 */
@Component
public class TestJobOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectService projectService;
    private final ObjectProvider<TestJobRegistry> testJobRegistryProvider;

    @SuppressFBWarnings("EI")
    public TestJobOwnershipResolver(
        ProjectService projectService, ObjectProvider<TestJobRegistry> testJobRegistryProvider) {

        this.projectService = projectService;
        this.testJobRegistryProvider = testJobRegistryProvider;
    }

    @Override
    public String resourceType() {
        return "TestJob";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        TestJobRegistry testJobRegistry = testJobRegistryProvider.getIfAvailable();

        if (testJobRegistry == null) {
            return ResourceOwner.unknown();
        }

        return testJobRegistry.fetchTestJob(id)
            .map(TestJob::workflowId)
            .flatMap(projectService::fetchWorkflowProject)
            .map(Project::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
