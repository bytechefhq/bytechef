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

package com.bytechef.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * End-to-end proof that {@link ProjectService#updateErrorWorkflow(long, Long)} actually persists the error-workflow
 * reference, unlike {@link ProjectService#update(Project)} which silently discards it. A facade-level test with a
 * mocked {@link ProjectService} cannot catch this class of regression since the mock has no notion of what the real
 * service persists.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class ProjectErrorWorkflowServiceIntTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @AfterEach
    public void afterEach() {
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testErrorProjectWorkflowIdRoundTripsThroughService() {
        Project project = projectRepository.save(
            Project.builder()
                .name("test-project")
                .workspaceId(Validate.notNull(workspace.getId(), "id"))
                .build());

        long id = Validate.notNull(project.getId(), "id");

        projectService.updateErrorWorkflow(id, 42L);

        Project reloaded = OptionalUtils.get(projectRepository.findById(id));

        assertThat(reloaded.getErrorProjectWorkflowId()).isEqualTo(42L);
    }

    @Test
    void testErrorProjectWorkflowIdCanBeClearedThroughService() {
        Project project = projectRepository.save(
            Project.builder()
                .name("test-project-2")
                .workspaceId(Validate.notNull(workspace.getId(), "id"))
                .build());

        long id = Validate.notNull(project.getId(), "id");

        projectService.updateErrorWorkflow(id, 42L);

        Project withReference = OptionalUtils.get(projectRepository.findById(id));

        assertThat(withReference.getErrorProjectWorkflowId()).isEqualTo(42L);

        projectService.updateErrorWorkflow(id, null);

        Project cleared = OptionalUtils.get(projectRepository.findById(id));

        assertThat(cleared.getErrorProjectWorkflowId()).isNull();
    }
}
