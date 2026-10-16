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

import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class,
    ProjectDeploymentGraphQlController.class
})
@GraphQlTest(
    controllers = ProjectDeploymentGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.inspection.enabled=false",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@AutomationConfigurationGraphQlConfigurationSharedMocks
class ProjectDeploymentGraphQlControllerIntTest {

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ProjectDeploymentService projectDeploymentService;

    @Test
    void testWorkspaceProjectDeploymentsListsTheNonEmbeddedDeploymentsTheServiceReturns() {
        when(environmentService.getEnvironment(2L)).thenReturn(Environment.PRODUCTION);
        when(projectDeploymentService.getProjectDeployments(false, Environment.PRODUCTION, null, null, 1L))
            .thenReturn(List.of(projectDeployment(10L, 100L, "Orders")));

        graphQlTester
            .document("""
                query {
                    workspaceProjectDeployments(workspaceId: 1, environmentId: 2) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("workspaceProjectDeployments[*].name")
            .entityList(String.class)
            .containsExactly("Orders");
    }

    private static ProjectDeployment projectDeployment(long id, long projectId, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setId(id);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);

        return projectDeployment;
    }
}
