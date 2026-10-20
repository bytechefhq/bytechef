/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfiguration;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiCollectionRepository;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ApiCollectionIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ApiCollectionIntTestConfigurationSharedMocks
class ApiCollectionFacadeIntTest {

    @Autowired
    private ApiCollectionFacade apiCollectionFacade;

    @Autowired
    private ApiCollectionRepository apiCollectionRepository;

    @Autowired
    private ApiCollectionService apiCollectionService;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Project project;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        project = Project.builder()
            .name("test-project")
            .workspaceId(workspace.getId())
            .build();

        project.publish("v1");

        project = projectRepository.save(project);
    }

    @AfterEach
    void afterEach() {
        apiCollectionRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testCreateApiCollectionStoresIt() {
        ApiCollectionDTO apiCollectionDTO = apiCollectionFacade.createApiCollection(
            getApiCollectionDTO("test-collection"));

        assertThat(apiCollectionService.getApiCollection(apiCollectionDTO.id()))
            .satisfies(apiCollection -> {
                assertThat(apiCollection.getName()).isEqualTo("test-collection");
                assertThat(apiCollection.getProjectDeploymentId()).isEqualTo(apiCollectionDTO.projectDeploymentId());
            });
    }

    private ApiCollectionDTO getApiCollectionDTO(String name) {
        return new ApiCollectionDTO(
            1, "test-context-path", null, null, "test description", false, List.of(), Environment.DEVELOPMENT, null,
            null, null, name, null, project.getId(), null, 0, 1, List.of(), 0);
    }
}
