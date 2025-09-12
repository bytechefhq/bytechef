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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.web.graphql.config.ProjectConfigurationGraphQlTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    ProjectConfigurationGraphQlTestConfiguration.class,
    ProjectWorkflowGraphQlController.class,
})
@GraphQlTest(
    controllers = ProjectWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/",
    })
public class ProjectWorkflowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Test
    void testGetSharedWorkflow() {
        // Given: mock facade to return a SharedWorkflowDTO
        SharedWorkflowDTO sharedWorkflow = new SharedWorkflowDTO("Shared workflow description", true, 1, null);

        when(projectWorkflowFacade.getSharedWorkflow(anyString()))
            .thenReturn(sharedWorkflow);

        // When & Then
        this.graphQlTester
            .documentName("sharedWorkflow")
            .variable("workflowUuid", "123e4567-e89b-12d3-a456-426614174000")
            .execute()
            .path("sharedWorkflow.description")
            .entity(String.class)
            .isEqualTo("Shared workflow description")
            .path("sharedWorkflow.exported")
            .entity(Boolean.class)
            .isEqualTo(true);
    }
}
